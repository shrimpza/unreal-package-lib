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
// http://www.unrealtexture.com/Unreal/Downloads/3DEditing/UnrealEd/Tutorials/unrealwiki-offline/package-file-format-data-de.html
// possible reference for newer engines: http://wiki.tesnexus.com/index.php/UPK_File_Format_-_XCOM:EU_2012
public class Package {

	private static final int PKG_SIGNATURE = 0x9E2A83C1;

	private final FileChannel channel;
	private final ByteBuffer buffer;

	private final int version;
	private final int flags;

	private final Name[] names;
	private final Export[] exports;
	private final Import[] imports;

	public enum PackageFlag {
		AllowDownload(0x0001),    //	Allow downloading package
		ClientOptional(0x0002),   //	Purely optional for clients
		ServerSideOnly(0x0004),   //	Only needed on the server side
		BrokenLinks(0x0008),      //	Loaded from linker with broken import links
		Unsecure(0x0010),         //	Not trusted
		Need(0x8000);             //	Client needs to download this package

		private final int flag;

		PackageFlag(int flag) {
			this.flag = flag;
		}

		public boolean appliesTo(Package pkg) {
			return (pkg.flags & flag) == flag;
		}
	}

	public enum ObjectFlag {
		Transactional(0x00000001),
		Unreachable(0x00000002),
		Public(0x00000004),
		TagImp(0x00000008),
		TagExp(0x00000010),
		SourceModified(0x00000020),
		TagGarbage(0x00000040),
		NeedLoad(0x00000200),
		HighlightedName(0x00000400),
		EliminateObject(0x00000400),
		InSingularFunc(0x00000800),
		RemappedName(0x00000800),
		Suppress(0x00001000),
		StateChanged(0x00001000),
		InEndState(0x00002000),
		Transient(0x00004000),
		PreLoading(0x00008000),
		LoadForClient(0x00010000),
		LoadForServer(0x00020000),
		LoadForEdit(0x00040000),
		Standalone(0x00080000),
		NotForClient(0x00100000),
		NotForServer(0x00200000),
		NotForEdit(0x00400000),
		Destroyed(0x00800000),
		NeedPostLoad(0x01000000),
		HasStack(0x02000000),
		Native(0x04000000),
		Marked(0x08000000),
		ErrorShutdown(0x10000000),
		DebugPostLoad(0x20000000),
		DebugSerialize(0x40000000),
		DebugDestroy(0x80000000);

		private final int flag;

		ObjectFlag(int flag) {
			this.flag = flag;
		}

		public boolean appliesTo(Export obj) {
			return (obj.flags & flag) == flag;
		}
	}

	public Package(Path pkg) throws IOException {
		this.channel = FileChannel.open(pkg, StandardOpenOption.READ);
		this.buffer = ByteBuffer.allocateDirect(1024 * 8)
								.order(ByteOrder.LITTLE_ENDIAN);

		moveTo(0); // overly explicit start from the start

		if (readInt() != PKG_SIGNATURE) throw new IllegalArgumentException("File " + pkg + " does not seem to be an Unreal package");

		this.version = readInt() & 0xFF;

		this.flags = readInt();

		int nameCount = readInt();
		int namePos = readInt();

		int exportCount = readInt();
		int exportPos = readInt();

		int importCount = readInt();
		int importPos = readInt();

		if (version < 68) {
			// unused, we don't care about the heritage values or the heritage table
			int heritageCount = readInt();
			int heritagePos = readInt();
		} else {
			// unused, we don't care about the guid, or generation counters or the generation information
			byte[] guid = new byte[16];
			buffer.get(guid);
			int generationCount = readInt();
		}

		this.names = names(nameCount, namePos);
		this.exports = exports(exportCount, exportPos);
		this.imports = imports(importCount, importPos);
	}

	/**
	 * Package file version.
	 *
	 * <ul>
	 * <li><= 68 Unreal</li>
	 * <li>>= 69 Unreal Tournament</li>
	 * <li>>= ~126 UT2004</li>
	 * </ul>
	 *
	 * @return package version
	 */
	public int version() {
		return version;
	}

	public Name[] names() {
		return names;
	}

	public Export[] exports() {
		return exports;
	}

	public Import[] imports() {
		return imports;
	}

	// --- buffer positioning and management

	/**
	 * Move to a position in the file, clear the buffer, and read from there.
	 *
	 * @param pos position in file
	 * @throws IOException io failure
	 */
	private void moveTo(int pos) throws IOException {
		channel.position(pos);

		buffer.clear();
		channel.read(buffer);
		buffer.flip();
	}

	/**
	 * Read more data from the current position in the file, retaining unread
	 * bytes in the buffer.
	 *
	 * @throws IOException io failure
	 */
	private void fillBuffer() throws IOException {
		buffer.compact();
		channel.read(buffer);
		buffer.flip();
	}

	// --- primary data table readers

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

		moveTo(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 256) fillBuffer(); // more-or-less
			names[i] = readName();
		}

		return names;
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

		moveTo(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 28) fillBuffer(); // more-or-less, probably less
			exports[i] = readExport();
		}

		return exports;
	}

	/**
	 * Read all exports from the file.
	 *
	 * @param count number of exports in the file
	 * @param pos   position of the first export within the file
	 * @return array of exports
	 * @throws IOException io failure
	 */
	private Import[] imports(int count, int pos) throws IOException {
		Import[] imports = new Import[count];

		moveTo(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 28) fillBuffer(); // more-or-less, probably less
			imports[i] = readImport();
		}

		return imports;
	}

	/**
	 * Read a single name from the current buffer position.
	 *
	 * @return a new name
	 */
	private Name readName() {
		String name = "";

		if (version < 64) {
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

		return new Name(name, buffer.getInt());
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Export readExport() {
		return new Export(
				readIndex(), // class
				readIndex(), // super
				readInt(),     // group
				readIndex(), // name
				readInt(),   // flags
				readIndex(), // size
				readIndex()  // pos
		);
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Import readImport() {
		return new Import(
				readIndex(), // package file
				readIndex(), // class
				readInt(),   // package name
				readIndex()  // name
		);
	}

	// --- convenience

	private byte readByte() {
		return buffer.get();
	}

	private int readInt() {
		return buffer.getInt();
	}

	private int readIndex() {
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

	// --- exposed classes

	public class Name {

		private final String name;
		private final int flags;

		private Name(String name, int flags) {
			this.name = name;
			this.flags = flags;
		}

		public String name() {
			return name;
		}

		public int flags() {
			return flags;
		}

		@Override
		public String toString() {
			return String.format("Name [name=%s, flags=%s]", name, flags);
		}
	}

	public class Export {

		private final int eClass;
		private final int eSuper;
		private final int eGroup;
		private final int name;
		private final int flags;
		private final int size;
		private final int pos;

		private Export(int eClass, int eSuper, int eGroup, int name, int flags, int size, int pos) {
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

	public class Import {

		private final int file;
		private final int className;
		private final int packageName;
		private final int name;

		private Import(int file, int className, int packageName, int name) {
			this.file = file;
			this.className = className;
			this.packageName = packageName;
			this.name = name;
		}

		@Override
		public String toString() {
			return String.format("Import [file=%s, className=%s, packageName=%s, name=%s]",
								 file, className, packageName, name);
		}
	}

}
