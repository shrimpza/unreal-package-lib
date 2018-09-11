package net.shrimpworks.unreal.packages;

import java.io.IOException;
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

	public static class UmodFile {

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
			// TODO provide a channel which presents the contents of this file as a standalone-seeming channel
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public String toString() {
			return String.format("UmodFile [name=%s]", name);
		}
	}
}
