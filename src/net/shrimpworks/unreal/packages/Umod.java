package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An Unreal modification package.
 * <p>
 * UMod files are used to bundle third party Unreal Engine 1 and 2 content,
 * normally for more complex modifications with many files, rather than
 * individual pieces of content like maps. The Unreal game in question will
 * unpack these packages into their installation directories.
 * <p>
 * They may hold any content, not just Unreal {@link Package}s.
 * <p>
 * This implementation supports reading the file list, and then reading the
 * individual files as {@link UmodFile}s, which may be either saved to disk
 * or used in conjunction with the {@link Package} class to inspect and
 * extract package contents without first unpacking the Umod.
 */
public class Umod {

	private static final int UMOD_SIGNATURE = 0x9FE3C5A3;

	private final PackageReader reader;

	public final int version;
	public final int size;
	public final UmodFile[] files;

	// TODO probably expose the manifest as a collection of Properties

	public Umod(Path umodFile) throws IOException {
		this(new PackageReader(umodFile));
	}

	public Umod(PackageReader reader) {
		this.reader = reader;

		reader.moveTo(reader.size() - 20);

		if (reader.readInt() != UMOD_SIGNATURE) throw new IllegalArgumentException("Package does not seem to be a UMOD package");

		final long filesOffset = reader.readInt();

		this.size = reader.readInt(); // this is actually just the filesize; perhaps useful for validation
		this.version = reader.readInt();

		final long checksum = reader.readInt(); // cool story bro

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

	/**
	 * Represents a single file entry in a Umod package.
	 */
	public class UmodFile {

		public final String name;
		public final int size;

		private final int offset;
		private final int flags;

		private UmodFile(String name, int size, int offset, int flags) {
			this.name = name;
			this.size = size;
			this.offset = offset;
			this.flags = flags;
		}

		/**
		 * Get a byte channel exposing the contents of this file.
		 * <p>
		 * This channel may be used in conjunction with a {@link PackageReader}
		 * and {@link Package} to inspect the contents of Unreal packages
		 * without the need to extract them first, or may simply be written to
		 * a file on disk.
		 *
		 * @return a byte channel
		 */
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
	private static class UmodFileChannel implements SeekableByteChannel {

		private final PackageReader reader;

		private final int offset;
		private final int size;

		private UmodFileChannel(PackageReader reader, int offset, int size) {
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
