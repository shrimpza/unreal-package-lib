package net.shrimpworks.unreal.packages.compression;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoConstraint;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.lzo_uintp;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * A {@link SeekableByteChannel} implementation that wraps a byte[].
 */
public class ChunkChannel implements SeekableByteChannel {

	public final CompressedChunk chunk;

	private final PackageReader reader;
	private final int[] blockSizes;

	private final byte[] data;
	private final AtomicBoolean closed = new AtomicBoolean();

	private int position;
	private int size;

	/**
	 * Constructor taking a byte array.
	 *
	 * <p>This constructor is intended to be used with pre-allocated buffer or when
	 * reading from a given byte array.</p>
	 */
	public ChunkChannel(PackageReader reader, CompressedChunk chunk, int[] blockSizes, byte[] data) {
		this.chunk = chunk;
		this.reader = reader;
		this.blockSizes = blockSizes;
		this.data = data;
		this.size = data.length;

		open();
	}

	/**
	 * Constructor taking a size of storage to be allocated.
	 *
	 * <p>Creates a channel and allocates internal storage of a given size.</p>
	 */
	public ChunkChannel(PackageReader reader, CompressedChunk chunk, int size, int[] blockSizes) {
		this(reader, chunk, blockSizes, new byte[size]);
	}

	private void open() {
		if (chunk.compressionFormat != CompressionFormat.LZO) {
			throw new IllegalArgumentException("Unsupported compression format " + chunk.compressionFormat.name());
		}

		LzoDecompressor dec = LzoLibrary.getInstance().newDecompressor(LzoAlgorithm.LZO1X, LzoConstraint.SAFETY);

		try {
			for (int i = 0; i < blockSizes.length; i += 2) {
				byte[] in = new byte[blockSizes[i]];
				byte[] out = new byte[blockSizes[i + 1]];
				reader.readBytes(in, 0, in.length);

				int lzoRes = dec.decompress(in, 0, in.length, out, 0, new lzo_uintp(out.length));
				if (lzoRes != 0) throw new IllegalStateException("LZO read error: " + lzoRes);
				write(ByteBuffer.wrap(out));
			}

			position(0);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to construct decompressed data channel", ex);
		}

	}

	@Override
	public long position() {
		return position;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		ensureOpen();
		if (newPosition < 0L || newPosition > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Position has to be in range 0.. " + Integer.MAX_VALUE);
		}
		position = (int)newPosition;
		return this;
	}

	/**
	 * Returns the current size of entity to which this channel is connected.
	 *
	 * <p>This method violates the contract of {@link SeekableByteChannel#size} as it will not throw any exception when
	 * invoked on a closed channel. Instead it will return the size the channel had when close has been called.</p>
	 */
	@Override
	public long size() {
		return size;
	}

	/**
	 * Truncates the entity, to which this channel is connected, to the given size.
	 *
	 * <p>This method violates the contract of {@link SeekableByteChannel#truncate} as it will not throw any exception when
	 * invoked on a closed channel.</p>
	 */
	@Override
	public SeekableByteChannel truncate(long newSize) {
		if (newSize < 0L || newSize > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Size has to be in range 0.. " + Integer.MAX_VALUE);
		}
		if (size > newSize) {
			size = (int)newSize;
		}
		if (position > newSize) {
			position = (int)newSize;
		}
		return this;
	}

	@Override
	public int read(ByteBuffer buf) throws IOException {
		ensureOpen();
		int wanted = buf.remaining();
		int possible = size - position;
		if (possible <= 0) {
			return -1;
		}
		if (wanted > possible) {
			wanted = possible;
		}
		buf.put(data, position, wanted);
		position += wanted;
		return wanted;
	}

	@Override
	public void close() {
		closed.set(true);
	}

	@Override
	public boolean isOpen() {
		return !closed.get();
	}

	@Override
	public int write(ByteBuffer b) throws IOException {
		ensureOpen();
		int wanted = b.remaining();
		int possibleWithoutResize = size - position;
		if (wanted > possibleWithoutResize) throw new BufferOverflowException();
		b.get(data, position, wanted);
		position += wanted;
		if (size < position) {
			size = position;
		}
		return wanted;
	}

	private void ensureOpen() throws ClosedChannelException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
	}

}
