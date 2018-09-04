package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

// reference: http://www.unrealtexture.com/Unreal/Downloads/3DEditing/UnrealEd/Tutorials/unrealwiki-offline/package-file-format.html
public class PackageReader {

	private final FileChannel channel;
	private final ByteBuffer buffer;

	private final int version;

	public PackageReader(Path pkg) throws IOException {
		this.channel = FileChannel.open(pkg, StandardOpenOption.READ);
		this.buffer = ByteBuffer.allocateDirect(1024 * 8)
								.order(ByteOrder.LITTLE_ENDIAN);

		readFrom(0); // overly explicit start from the start

		System.out.printf("sig: %d%n", buffer.getInt());
		this.version = buffer.getInt();
		System.out.printf("ver: %d%n", version);
		System.out.printf("flags: %d%n", buffer.getInt());

		int nameCount = buffer.getInt();
		System.out.printf("names: %d%n", nameCount);
		int namePos = buffer.getInt();
		System.out.printf("name pos: %d%n", namePos);

		int exportCount = buffer.getInt();
		System.out.printf("exports: %d%n", exportCount);
		int exportPos = buffer.getInt();
		System.out.printf("export pos: %d%n", exportPos);

		System.out.printf("imports: %d%n", buffer.getInt());
		System.out.printf("import pos: %d%n", buffer.getInt());

		// consume GUID from supported versions
		if (version >= 68) {
			byte[] guid = new byte[16];
			buffer.get(guid);
			System.out.printf("guid: %s%n", Arrays.toString(guid));
		}

		System.out.println(Arrays.toString(names(nameCount, namePos)));
		System.out.println(Arrays.toString(exports(exportCount, exportPos)));
	}

	/**
	 * Move to a position in the file, clear the buffer, and read from there.
	 *
	 * @param pos position in file
	 * @return true if any data was read from here
	 * @throws IOException io failure
	 */
	private boolean readFrom(int pos) throws IOException {
		channel.position(pos);

		buffer.clear();
		int read = channel.read(buffer);
		buffer.flip();

		return read > 0;
	}

	/**
	 * Read more data from the current position in the file, retaining unread
	 * bytes in the buffer.
	 *
	 * @return true if more data was read
	 * @throws IOException io failure
	 */
	private boolean readMore() throws IOException {
		buffer.compact();
		int read = channel.read(buffer);
		buffer.flip();

		return read > 0;
	}

	/**
	 * Read all names from the file.
	 *
	 * @param count number of names in the file
	 * @param pos   position of first name within the file
	 * @return array of names
	 * @throws IOException io failure
	 */
	private Name[] names(int count, int pos) throws IOException {
		Name[] names = new Name[count];

		readFrom(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 256) readMore(); // more-or-less
			names[i] = name();
		}

		return names;
	}

	/**
	 * Read a single name from the current buffer position.
	 *
	 * @return a new name
	 */
	private Name name() {
		String name = "";

		if (version < 64) {
			// read to NUL/0x00
			byte[] val = new byte[255];
			byte len = 0, v;
			while ((v = buffer.get()) != 0x00) {
				val[len] = v;
				len++;
			}
			if (len > 0) name = new String(Arrays.copyOfRange(val, 0, len), StandardCharsets.US_ASCII);
		} else {
			byte len = buffer.get();
			if (len > 0) {
				byte[] val = new byte[len];
				buffer.get(val);
				assert buffer.get() == 0x00; // drain trailing NUL
				name = new String(val, StandardCharsets.US_ASCII);
			}
		}

		return new Name(name, buffer.getInt());
	}

	/**
	 * Read all exports from the file.
	 *
	 * @param count number of exports in the file
	 * @param pos   position of the first export within the file
	 * @return array of exports
	 * @throws IOException io failure
	 */
	private Export[] exports(int count, int pos) throws IOException {
		Export[] exports = new Export[count];

		readFrom(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 28) readMore(); // more-or-less, probably less
			exports[i] = export();
		}

		return exports;
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Export export() {
		// see http://www.unrealtexture.com/Unreal/Downloads/3DEditing/UnrealEd/Tutorials/unrealwiki-offline/package-file-format-data-de.html
		return null;
	}

	private static class Name {

		private final String name;
		private final int flags;

		public Name(String name, int flags) {
			this.name = name;
			this.flags = flags;
		}

		@Override
		public String toString() {
			return String.format("Name [name=%s, flags=%s]", name, flags);
		}
	}

	private static class Export {

		private final long eClass;
		private final long eSuper;
		private final int eGroup;
		private final long name;
		private final long flags;
		private final long size;
		private final long pos;

		public Export(long eClass, long eSuper, int eGroup, long name, long flags, long size, long pos) {
			this.eClass = eClass;
			this.eSuper = eSuper;
			this.eGroup = eGroup;
			this.name = name;
			this.flags = flags;
			this.size = size;
			this.pos = pos;
		}

		@Override
		public String toString() {
			return String.format("Export [eClass=%s, eSuper=%s, eGroup=%s, name=%s, flags=%s, size=%s, pos=%s]",
								 eClass, eSuper, eGroup, name, flags, size, pos);
		}
	}

}
