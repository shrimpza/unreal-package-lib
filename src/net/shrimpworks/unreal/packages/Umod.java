package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Umod {

	private static final int UMOD_SIGNATURE = 0x9FE3C5A3;

	private final PackageReader reader;

	public final int version;
	public final UmodFile[] files;

	public Umod(Path umodFile) throws IOException {
		this(new PackageReader(umodFile));
	}

	public Umod(PackageReader reader) {
		this.reader = reader;

		reader.moveTo(reader.size() - 20);

		if (reader.readInt() != UMOD_SIGNATURE) throw new IllegalArgumentException("Package does not seem to be a UMOD package");

		final long filesOffset = reader.readInt();
		final long size = reader.readInt();
		this.version = reader.readInt();
		final long checksum = reader.readInt();

		System.out.println("o: " + filesOffset + " s: " + size + " v: " + version + " c: " + checksum);

		// read the files directory/table
		List<UmodFile> files = new ArrayList<>();
		reader.moveTo(filesOffset + 1); // hmm :)

		// keep reading until we get back to the header
		while (reader.currentPosition() < reader.size() - 20) {
			reader.ensureRemaining(270); // enough to read a full file path and the other bytes
			files.add(readFile());
		}

		this.files = files.toArray(new UmodFile[0]);
	}

	private UmodFile readFile() {
		int nameSize = reader.readIndex();
		byte[] val = new byte[nameSize];
		reader.readBytes(val, 0, nameSize);
		String name = new String(val, StandardCharsets.US_ASCII).trim();
		int offset = reader.readInt();
		int size = reader.readInt();
		int flags = reader.readInt();

		return new UmodFile(name, size, offset, flags);
	}

	public class UmodFile {

		public final String name;
		public final int size;

		private final int offset;
		private final int flags;

		public UmodFile(String name, int size, int offset, int flags) {
			this.name = name;
			this.size = size;
			this.offset = offset;
			this.flags = flags;
		}

		public SeekableByteChannel read() {
			// provide a channel which presents the contents of this file as a standalone-seeming channel
			return new UmodFileChannel(reader, offset, size);
		}

		@Override
		public String toString() {
			return String.format("UmodFile [name=%s]", name);
		}
	}

	/**
	 * A simple channel implementation which remains within the bounds of a
	 * file within a Umod archive.
	 */
	public static class UmodFileChannel implements SeekableByteChannel {

		private final PackageReader reader;

		private final int offset;
		private final int size;

		public UmodFileChannel(PackageReader reader, int offset, int size) {
			this.reader = reader;
			this.offset = offset;
			this.size = size;

			reader.moveTo(offset);
		}

		@Override
		public int read(ByteBuffer dst) {
			if (position() == size) return -1;

			int cnt = dst.position();
			int remain = (int)(size - position());
			byte[] buff = new byte[Math.min(dst.remaining(), remain)]; // possibly expensive if using a large buffer

			reader.ensureRemaining(buff.length);
			int read = reader.readBytes(buff, 0, buff.length);
			dst.put(buff, 0, read);

			return dst.position() - cnt;
		}

		@Override
		public long position() {
			return reader.currentPosition() - offset;
		}

		@Override
		public SeekableByteChannel position(long newPosition) {
			if (newPosition > size) throw new IllegalArgumentException("Cannot seek beyond size " + size);
			reader.moveTo(offset + newPosition);
			return this;
		}

		@Override
		public long size() {
			return size;
		}

		@Override
		public SeekableByteChannel truncate(long size) {
			throw new UnsupportedOperationException("Truncate not supported");
		}

		@Override
		public int write(ByteBuffer src) {
			throw new UnsupportedOperationException("Write not supported");
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() {
			// no-op
		}
	}
}
