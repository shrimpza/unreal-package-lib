package net.shrimpworks.unreal.packages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;

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

	private final SeekableByteChannel channel;
	private final ByteBuffer buffer;

	public PackageReader(SeekableByteChannel channel) {
		this.channel = channel;
		this.buffer = ByteBuffer.allocateDirect(READ_BUFFER).order(ByteOrder.LITTLE_ENDIAN);
	}

	public PackageReader(Path packageFile) throws IOException {
		this(FileChannel.open(packageFile, StandardOpenOption.READ));
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	public String hash(String alg) {
		try {
			MessageDigest md = MessageDigest.getInstance(alg);

			channel.position(0);
			buffer.clear();
			while (channel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			return bytesToHex(md.digest()).toLowerCase();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate hash for package.", e);
		}
	}

	// --- buffer positioning and management

	public long size() {
		try {
			return channel.size();
		} catch (IOException e) {
			throw new IllegalStateException("Could not determine size of package.");
		}
	}

	public int position() {
		return buffer.position();
	}

	/**
	 * Move to a position in the file, clear the buffer, and read from there.
	 *
	 * @param pos position in file
	 */
	public void moveTo(long pos) {
		try {
			channel.position(pos);

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
			channel.position(channel.position() + amount - buffer.remaining());

			buffer.clear();
			channel.read(buffer);
			buffer.flip();
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
	 * Read more data from the current position in the file, retaining unread
	 * bytes in the buffer.
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

	/**
	 * Get the current read position within the file.
	 *
	 * @return position in package
	 */
	public int currentPosition() {
		try {
			return (int)(channel.position() - buffer.remaining());
		} catch (IOException e) {
			throw new IllegalStateException("Could not determine current file position");
		}
	}

	// --- read operations

	public byte readByte() {
		return buffer.get();
	}

	public short readShort() {
		return buffer.getShort();
	}

	public int readInt() {
		return buffer.getInt();
	}

	public long readLong() {
		return buffer.getLong();
	}

	public float readFloat() {
		return buffer.getFloat();
	}

	public int readBytes(byte[] dest, int offset, int length) {
		int start = buffer.remaining();
		buffer.get(dest, offset, Math.min(buffer.remaining(), length));
		return start - buffer.remaining();
	}

	/**
	 * Reads a "Compact Index" integer value.
	 * <p>
	 * Refer to package documentation and reference for description of the
	 * format.
	 *
	 * @return a compact index integer
	 */
	public int readIndex() {
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
	 * Read a string from the current buffer position.
	 * <p>
	 * The <code>packageVersion</code> parameter is required since string read
	 * operations differ by version.
	 * <p>
	 * The length of the string will be determined automatically.
	 *
	 * @param packageVersion package version
	 * @return a string
	 */
	public String readString(int packageVersion) {
		return readString(packageVersion, -1);
	}

	/**
	 * Read a string from the current buffer position.
	 * <p>
	 * The <code>packageVersion</code> parameter is required since string read
	 * operations differ by version.
	 * <p>
	 * For some properties, length is provided as part of the property and not
	 * at the start of the string in the case of names or other string values.
	 *
	 * @param packageVersion package version
	 * @param length         length of the string to read, or -1 to read it automatically
	 * @return a string
	 */
	public String readString(int packageVersion, int length) {
		String string = "";

		if (packageVersion < 64) {
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
			int len = packageVersion > 117 ? readIndex() : length > -1 ? Math.min(length, readByte() & 0xFF) : readByte() & 0xFF;

			if (len > 0) {
				byte[] val = new byte[len];
				ensureRemaining(len);
				buffer.get(val);
				string = new String(val, StandardCharsets.ISO_8859_1);
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

	public static class ReaderStats {

		public int moveToCount;
		public int moveRelativeCount;
		public int ensureRemainingCount;
		public int fillBufferCount;

		@Override
		public String toString() {
			return String.format("ReaderStats [moveToCount=%s, moveRelativeCount=%s, ensureRemainingCount=%s, fillBufferCount=%s]",
								 moveToCount, moveRelativeCount, ensureRemainingCount, fillBufferCount);
		}
	}
}
