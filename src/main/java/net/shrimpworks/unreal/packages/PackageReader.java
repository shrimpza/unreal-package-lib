package net.shrimpworks.unreal.packages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import net.shrimpworks.unreal.packages.compression.ChunkChannel;
import net.shrimpworks.unreal.packages.compression.CompressedChunk;
import net.shrimpworks.unreal.packages.entities.NameNumber;

/**
 * Provides the means of directly accessing and parsing the content of package
 * files.
 * <p>
 * Manages the buffers and navigation and read operations within package files
 * required for parsing a package's contents.
 */
public class PackageReader implements Closeable {

	private static final int READ_BUFFER = 1024 * 8; // use an 8k read buffer

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray(); // used for hash encoding

	public final ReaderStats stats = new ReaderStats();

	private final SeekableByteChannel fileChannel;
	private final ByteBuffer buffer;

	private SeekableByteChannel channel;

	protected int version = 0;
	protected CompressedChunk[] chunks = null;

	private final boolean cacheChunks;
	private final Map<CompressedChunk, ChunkChannel> chunkCache = new HashMap<>();

	/**
	 * Creates a new package reader for an Unreal package, represented by the
	 * provided {@link FileChannel}.
	 *
	 * @param fileChannel unreal package file
	 * @param cacheChunks if true, decompressed chunks from compressed packages
	 *                    will be kept in memory for reuse, rather than
	 *                    discarded for potential garbage collection after
	 *                    moving to another chunk. this increases memory
	 *                    overhead but may improve read performance.
	 */
	public PackageReader(SeekableByteChannel fileChannel, boolean cacheChunks) {
		this.fileChannel = fileChannel;
		this.channel = fileChannel;

		this.cacheChunks = cacheChunks;
		this.buffer = ByteBuffer.allocateDirect(READ_BUFFER).order(ByteOrder.LITTLE_ENDIAN);
	}

	public PackageReader(Path packageFile, boolean cacheChunks) throws IOException {
		this(FileChannel.open(packageFile, StandardOpenOption.READ), cacheChunks);
	}

	public PackageReader(SeekableByteChannel fileChannel) {
		this(fileChannel, false);
	}

	public PackageReader(Path packageFile) throws IOException {
		this(FileChannel.open(packageFile, StandardOpenOption.READ), false);
	}

	@Override
	public void close() throws IOException {
		if (channel != null && channel.isOpen()) channel.close();
		fileChannel.close();
	}

	/**
	 * Calculate a hash of the file.
	 *
	 * @param alg hash algorithm, eg. SHA-1 or MD5
	 * @return string representation of file hash
	 */
	public String hash(String alg) {
		try {
			MessageDigest md = MessageDigest.getInstance(alg);

			fileChannel.position(0);
			buffer.clear();
			while (fileChannel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			return bytesToHex(md.digest()).toLowerCase();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate hash for package.", e);
		}
	}

	public void setChunks(CompressedChunk[] chunks) {
		this.chunks = chunks;
		this.stats.chunkCount = chunks.length;
	}

	// --- buffer positioning and management

	/**
	 * Return the total size of the package file.
	 *
	 * @return file size
	 */
	public long size() {
		try {
			return fileChannel.size();
		} catch (IOException e) {
			throw new IllegalStateException("Could not determine size of package.");
		}
	}

	/**
	 * Get the current read position within the current buffer.
	 * <p>
	 * Use {@link #currentPosition()} to find the current global
	 * read position.
	 *
	 * @return read position in buffer
	 */
	public int position() {
		return buffer.position();
	}

	/**
	 * Gets the current global read position, which may be within the current
	 * file for uncompressed packages or within compressed package headers, but
	 * may be relative to the current chunk for compressed packages.
	 *
	 * @return read position in package
	 */
	public int currentPosition() {
		try {
			if (channel instanceof ChunkChannel) {
				return ((ChunkChannel)channel).chunk.uncompressedOffset + (int)(channel.position() - buffer.remaining());
			} else {
				return (int)(channel.position() - buffer.remaining());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not determine current file position");
		}
	}

	/**
	 * Move to a position in the file, clear the buffer, and read from there.
	 *
	 * @param pos position in file
	 */
	public void moveTo(long pos) {
		moveTo(pos, false);
	}

	private void moveTo(long pos, boolean nonChunked) {
		moveTo(pos, nonChunked, false);
	}

	private void moveTo(long pos, boolean nonChunked, boolean keepChannel) {
		if (channel != fileChannel && nonChunked) channel = fileChannel;

		AtomicLong movePos = new AtomicLong(pos);

		// maybe we want to be inside a chunk actually
		if (!keepChannel && !nonChunked && chunks != null) {
			Optional<CompressedChunk> chunk = Arrays.stream(chunks)
													.filter(c -> pos >= c.uncompressedOffset
																 && pos < c.uncompressedOffset + c.uncompressedSize)
													.findFirst();
			chunk.ifPresent(compressedChunk -> {
				// we're already in the chunk, no need to re-read it
				if (!(channel instanceof ChunkChannel) || ((ChunkChannel)channel).chunk != compressedChunk) {
					channel = loadChunk(compressedChunk, cacheChunks);
				}

				movePos.set(pos - compressedChunk.uncompressedOffset);
			});
		}

		try {
			channel.position(movePos.get());

			buffer.clear();
			channel.read(buffer);
			buffer.flip();
		} catch (IOException e) {
			throw new IllegalStateException("Could not move to position " + pos + " within package file", e);
		} finally {
			stats.moveToCount++;
		}
	}

	/**
	 * Move to a position in the file, relative to the current position,
	 * and fill the buffer with data from that point on.
	 *
	 * @param amount amount to move forward by
	 */
	public void moveRelative(int amount) {
		try {
			// note: subtract remaining because the current position within the channel will align with the end of the last buffer fill
			moveTo(channel.position() - buffer.remaining() + amount, false, true);
		} catch (IOException e) {
			throw new IllegalStateException("Could not move by " + amount + " bytes within channel", e);
		} finally {
			stats.moveRelativeCount++;
		}
	}

	/**
	 * Ensure at least the specified number of bytes are available for
	 * subsequent read operations.
	 *
	 * @param minRemaining bytes
	 */
	public void ensureRemaining(int minRemaining) {
		try {
			if (buffer.capacity() < minRemaining) {
				throw new IllegalArgumentException("Impossible to fill buffer with " + minRemaining + " bytes");
			}

			if (buffer.remaining() < minRemaining) fillBuffer();
		} finally {
			stats.ensureRemainingCount++;
		}
	}

	/**
	 * Fill the read buffer with more data from the current position, retaining
	 * currently unread bytes in the buffer.
	 */
	public void fillBuffer() {
		try {
			buffer.compact();
			channel.read(buffer);
			buffer.flip();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read from package file", e);
		} finally {
			stats.fillBufferCount++;
		}
	}

	// --- read operations

	/**
	 * Reads a single byte at the current reader position and advances the
	 * position by one byte.
	 *
	 * @return a byte
	 */
	public byte readByte() {
		return buffer.get();
	}

	/**
	 * Reads a 2-byte signed short value from the current reader position and
	 * advances the position by two bytes.
	 *
	 * @return a signed short
	 */
	public short readShort() {
		return buffer.getShort();
	}

	/**
	 * Reads a 4-byte signed integer value from the current reader position and
	 * advances the reader position by 4 bytes.
	 *
	 * @return a singed integer
	 */
	public int readInt() {
		return buffer.getInt();
	}

	/**
	 * Reads an 8-byte signed long value from the current reader position and
	 * advances the reader position by 8 bytes.
	 *
	 * @return a singed long
	 */
	public long readLong() {
		return buffer.getLong();
	}

	/**
	 * Reads a 4-byte signed float value from the current reader position and
	 * advances the reader position by 4 bytes.
	 *
	 * @return a signed float
	 */
	public float readFloat() {
		return buffer.getFloat();
	}

	/**
	 * Read <code>length</code> bytes from the package, placing them into the
	 * destination byte array specified, at the <code>offset</code> within the
	 * destination array.
	 *
	 * @param dest   destination array
	 * @param offset position within destination to place read bytes
	 * @param length number of bytes to read
	 * @return number of bytes read
	 */
	public int readBytes(byte[] dest, int offset, int length) {
		int start = currentPosition(); //buffer.remaining();

		int read = 0;
		while (read < length) {
			if (buffer.remaining() < length) fillBuffer();
			int i = currentPosition();
			buffer.get(dest, offset + read, Math.min(buffer.remaining(), length - read));
			read += currentPosition() - i;
		}

		return currentPosition() - start;
	}

	/**
	 * Reads a "Compact Index" integer value.
	 * <p>
	 * Refer to package documentation and reference for description of the
	 * format.
	 * <p>
	 * For Unreal Engine 3 packages, compact indexes are no longer used,
	 * rather we use plain 32-bit/4-byte integers.
	 *
	 * @return an index value
	 */
	public int readIndex() {
		if (version == 0) throw new IllegalStateException("Version is not set");

		if (version > 178) return readInt();

		boolean negative = false;
		int num = 0;
		int len = 6;
		for (int i = 0; i < 5; i++) {
			boolean more;

			byte one = buffer.get();

			if (i == 0) {
				negative = (one & 0x80) > 0;
				more = (one & 0x40) > 0;
				num = one & 0x3F;
			} else if (i == 4) {
				num |= (one & 0x80) << len;
				more = false;
			} else {
				more = (one & 0x80) > 0;
				num |= (one & 0x7F) << len;
				len += 7;
			}

			if (!more) break;
		}

		return negative ? num * -1 : num;
	}

	/**
	 * Reads a name index from the current buffer position.
	 * <p>
	 * For Unreal Engines 1 and 2, this is the same as {@link #readIndex()},
	 * but Unreal Engine 3 also contains an additional integer string number.
	 *
	 * @return index of name in names table
	 */
	public NameNumber readNameIndex() {
		if (version == 0) throw new IllegalStateException("Version is not set");

		if (version < 343) return new NameNumber(readIndex());
		else {
			int index = readIndex();
			int number = readInt(); // for UE3, not used
			return new NameNumber(index, number);
		}
	}

	public String readString() {
		return readString(-1);
	}

	/**
	 * Read a string from the current buffer position.
	 * <p>
	 * String read operations differ between package versions, so {@link #version} should
	 * be set prior to string read operations.
	 *
	 * @param length length of the string to read, or -1 to read it automatically
	 * @return a string
	 */
	public String readString(int length) {
		if (version == 0) throw new IllegalStateException("Version is not set");

		String string = "";

		if (version < 64) {
			// read to NUL/0x00
			byte[] val = new byte[255];
			byte len = 0, v;
			while ((v = readByte()) != 0x00) {
				val[len] = v;
				len++;
			}
			if (len > 0) string = new String(Arrays.copyOfRange(val, 0, len), StandardCharsets.ISO_8859_1);
		} else {
			// Note: Oddity in some properties, where length byte reports longer than the property length
			int len = version > 117 ? readIndex() : length > -1 ? Math.min(length, readByte() & 0xFF) : readByte() & 0xFF;

			// UE3 uses negative lengths to indicate unicode strings
			Charset charset = len < 0 ? StandardCharsets.UTF_16LE : StandardCharsets.ISO_8859_1;
			int readLen = len < 0 ? -(len * 2) : len;

			if (readLen != 0) {
				byte[] val = new byte[readLen];
				ensureRemaining(readLen);
				buffer.get(val);
				string = new String(val, charset);
			}
		}

		return string.trim();
	}

	// -- private helpers

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = HEX_ARRAY[v >>> 4];
			hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * For Unreal Engine 3 packages, loads data from a compressed chunk, and
	 * returns a readable channel, within which normal package read
	 * operations may be invoked.
	 *
	 * @param chunk chunk to load
	 * @return a decompressed chunk
	 */
	public ChunkChannel loadChunk(CompressedChunk chunk) {
		return loadChunk(chunk, false);
	}

	private ChunkChannel loadChunk(CompressedChunk chunk, boolean cache) {
		try {
			Supplier<ChunkChannel> chunkLoader = () -> {
				try {
					moveTo(chunk.compressedOffset, true);
					if (readInt() != Package.PKG_SIGNATURE)
						throw new IllegalStateException("Chunk does not seem to be Unreal package data");
					int blockSize = readInt();
					int compressedSize = readInt();
					int uncompressedSize = readInt();
					int numBlocks = (uncompressedSize + blockSize - 1) / blockSize;
					int[] blockSizes = new int[numBlocks * 2];
					for (int i = 0; i < blockSizes.length; i += 2) {
						blockSizes[i] = readInt(); // compressed size
						blockSizes[i + 1] = readInt(); // uncompressed size
					}

					return new ChunkChannel(this, chunk, uncompressedSize, blockSizes);
				} finally {
					stats.chunkLoadCount++;
				}
			};

			return !cache ? chunkLoader.get() : chunkCache.computeIfAbsent(chunk, c -> chunkLoader.get());
		} finally {
			stats.chunkFetchCount++;
		}
	}

	public static class ReaderStats {

		public int moveToCount;
		public int moveRelativeCount;
		public int ensureRemainingCount;
		public int fillBufferCount;
		public int chunkCount;
		public int chunkLoadCount;
		public int chunkFetchCount;

		@Override
		public String toString() {
			return String.format(
				"ReaderStats [moveToCount=%s, moveRelativeCount=%s, ensureRemainingCount=%s, fillBufferCount=%s, chunkCount=%s, chunkLoadCount=%s, chunkFetchCount=%s]",
				moveToCount, moveRelativeCount, ensureRemainingCount, fillBufferCount, chunkCount, chunkLoadCount, chunkFetchCount);
		}
	}
}
