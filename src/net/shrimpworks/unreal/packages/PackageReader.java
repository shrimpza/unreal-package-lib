package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Provides the means of directly accessing and parsing the content of package
 * files.
 * <p>
 * Manages the buffers and navigation and read operations within package files
 * required for parsing a package's contents.
 */
public class PackageReader {

	private final SeekableByteChannel channel;
	private final ByteBuffer buffer;

	public PackageReader(SeekableByteChannel channel) {
		this.channel = channel;
		this.buffer = ByteBuffer.allocateDirect(1024 * 8) // allocate an 8k buffer for read operations
								.order(ByteOrder.LITTLE_ENDIAN);
	}

	public PackageReader(Path packageFile) throws IOException {
		this(FileChannel.open(packageFile, StandardOpenOption.READ));
	}

	// --- buffer positioning and management

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
		}
	}

	/**
	 * Move to a position in the file, relative to the current position,
	 * and fill the buffer with data from that point on.
	 *
	 * @param amount amount to move forward by
	 */
	public void moveRelative(int amount) {
		// FIXME move channel position rather than just moving the buffer repeatedly
		int bypass = amount;
		while (bypass > 0) {
			if (!buffer.hasRemaining()) {
				fillBuffer();
			}
			int chunk = Math.min(buffer.remaining(), bypass);
			buffer.position(buffer.position() + chunk);
			bypass -= chunk;
		}
		fillBuffer();
	}

	/**
	 * Ensure at least the specified number of bytes are available for
	 * subsequent read operations.
	 *
	 * @param minRemaining bytes
	 */
	public void ensureRemaining(int minRemaining) {
		if (buffer.capacity() < minRemaining) {
			throw new IllegalArgumentException("Impossible to fill buffer with " + minRemaining + " bytes");
		}

		if (buffer.remaining() < minRemaining) fillBuffer();
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
		}
	}

	/**
	 * Get the current read position within the file.
	 *
	 * @return position
	 */
	public int currentPosition() {
		try {
			return (int)(channel.position() - buffer.remaining());
		} catch (IOException e) {
			throw new IllegalStateException("Could not determine current file position");
		}
	}

	/**
	 * Get the current read position (number of bytes read), relative to the
	 * last {@link #moveTo(long)}, {@link #fillBuffer()}, or
	 * {@link #ensureRemaining(int)} operation.
	 *
	 * @return bytes read
	 */
	public int currentReadPosition() {
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
	 *
	 * @param packageVersion package version
	 * @return a string
	 */
	public String readString(int packageVersion) {
		String name = "";

		if (packageVersion < 64) {
			// read to NUL/0x00
			byte[] val = new byte[255];
			byte len = 0, v;
			while ((v = readByte()) != 0x00) {
				val[len] = v;
				len++;
			}
			if (len > 0) name = new String(Arrays.copyOfRange(val, 0, len), StandardCharsets.US_ASCII);
		} else {
			byte len = readByte();
			if (len > 0) {
				byte[] val = new byte[len];
				buffer.get(val);
				assert readByte() == 0x00; // drain trailing NUL
				name = new String(val, StandardCharsets.US_ASCII);
			}
		}

		return name.trim();
	}
}
