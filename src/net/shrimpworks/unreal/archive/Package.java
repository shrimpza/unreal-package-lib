package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import static net.shrimpworks.unreal.archive.Properties.*;

// reference: http://www.unrealtexture.com/Unreal/Downloads/3DEditing/UnrealEd/Tutorials/unrealwiki-offline/package-file-format.html
// http://www.unrealtexture.com/Unreal/Downloads/3DEditing/UnrealEd/Tutorials/unrealwiki-offline/package-file-format-data-de.html
// possible reference for newer engines: http://wiki.tesnexus.com/index.php/UPK_File_Format_-_XCOM:EU_2012
public class Package {

	private static final int PKG_SIGNATURE = 0x9E2A83C1;

	private static final int MAX_PROPERTIES = 256;

	private final FileChannel channel;
	private final ByteBuffer buffer;

	/**
	 * Package file version.
	 *
	 * <ul>
	 * <li><= 68 Unreal</li>
	 * <li>>= 69 Unreal Tournament</li>
	 * <li>>= 100 UE2 (UT2003/4)</li>
	 * </ul>
	 *
	 * @return package version
	 */
	public final int version;
	public final int flags;

	public final Name[] names;
	public final Export[] exports;
	public final Import[] imports;

	// cache of already-parsed/read objects, simply keyed by file position
	private final WeakHashMap<Integer, Objects.Object> loadedObjects;

	private final Name none;

	public interface Named {

		public static Named NULL = () -> new Name("Null", 0);

		public Name name();
	}

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

		public static EnumSet<PackageFlag> fromFlags(int flags) {
			EnumSet<PackageFlag> objectFlags = EnumSet.noneOf(PackageFlag.class);
			objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
			return objectFlags;
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

		public static EnumSet<ObjectFlag> fromFlags(int flags) {
			EnumSet<ObjectFlag> objectFlags = EnumSet.noneOf(ObjectFlag.class);
			objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
			return objectFlags;
		}
	}

	public Package(Path pkg) throws IOException {
		this.channel = FileChannel.open(pkg, StandardOpenOption.READ);
		this.buffer = ByteBuffer.allocateDirect(1024 * 8)
								.order(ByteOrder.LITTLE_ENDIAN);

		moveTo(0); // overly explicit start from the start

		if (readInt() != PKG_SIGNATURE) throw new IllegalArgumentException("File " + pkg + " does not seem to be an Unreal package");

		this.version = readShort();
		short license = readShort();

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

		this.loadedObjects = new WeakHashMap<>();

		this.none = Arrays.stream(names).filter(n -> n.name.equals("None")).findFirst().orElse(null);
	}

	public EnumSet<PackageFlag> flags() {
		return PackageFlag.fromFlags(flags);
	}

	public Collection<Export> exportsByClassName(String className) {
		Set<Export> exports = new HashSet<>();
		for (Export ex : this.exports) {
			Named type = ex.objClass.get();
			if (type instanceof Import && ((Import)type).name.name.equals(className)) {
				exports.add(ex);
			}
		}
		return exports;
	}

	private Objects.Object object(Export export) {
		Objects.Object existing = loadedObjects.get(export.pos);
		if (existing != null) return existing;

		if (export.size <= 0) throw new IllegalStateException("Export has no associated object data!");

		moveTo(export.pos);

		Objects.ObjectHeader header;
		if (export.flags().contains(ObjectFlag.HasStack)) {
			int node = readIndex();
			header = new Objects.ObjectHeader(
					node, readIndex(), readLong(), readInt(),
					node != 0 ? readIndex() : 0
			);
		} else {
			header = null;
		}

		List<Property> properties = new ArrayList<>();
		for (int i = 0; i < MAX_PROPERTIES; i++) {
			Property p = readProperty();

			if (p.name.equals(none)) break;
			else properties.add(p);
		}

		// keep track of how long the properties were, so we can potentially continue reading object data from this point
		long propsLength = 0;
		try {
			propsLength = channel.position() - buffer.remaining();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Named type = export.objClass.get();
		Objects.Object newObject;

		// FIXME just testing; define these as enum types probable, with factories
		if (type instanceof Import && ((Import)type).name.name.equals("Texture")) {
			newObject = new Objects.Texture(this, export, header, properties, (int)propsLength);
		} else if (type instanceof Import && ((Import)type).name.name.equals("Palette")) {
			newObject = new Objects.Palette(this, export, header, properties, (int)propsLength);
		} else {
			newObject = new Objects.Object(this, export, header, properties, (int)propsLength);
		}

		loadedObjects.put(export.pos, newObject);

		return newObject;
	}

	private Property readProperty() {
		int nameIndex = readIndex();
		Name name = names[nameIndex];

		// the end - don't read or process anything beyond here
		if (name.equals(none)) return new NameProperty(this, name, name);

		byte propInfo = readByte();

		byte type = (byte)(propInfo & 0b00001111); // bits 0 to 3 are the type
		int size = (propInfo & 0b01110000) >> 4; // bits 4 to 6 is the size
		int lastBit = (propInfo & 0x80); // bit 7 is either array size, or boolean value

		PropertyType propType = PropertyType.get(type);

		if (propType == null) {
			throw new IllegalStateException(String.format("Unknown property type index %d for property %s", type, name.name));
		}

		// if array and not boolean, next byte is index of property within the array
		int arrayIndex = 0;
		if (lastBit != 0 && propType != PropertyType.BooleanProperty) {
			arrayIndex = readByte();
		}

		// When a struct, type of struct follows before size and body
		StructType structType = null;
		if (propType == PropertyType.StructProperty) {
			int structIdx = readIndex();
			structType = StructType.get(names[structIdx]);
			if (structType == null) {
				throw new IllegalStateException(String.format("Unknown struct type index %d for property %s", structIdx, name.name));
			}
		}

		switch (size) {
			case 0:
				size = 1; break;
			case 1:
				size = 2; break;
			case 2:
				size = 4; break;
			case 3:
				size = 12; break;
			case 4:
				size = 16; break;
			case 5:
				size = readByte(); break;
			case 6:
				size = readShort(); break;
			case 7:
				size = readInt(); break;
		}

		return createProperty(name, propType, structType, arrayIndex, size, lastBit);
	}

	private Property createProperty(Name name, PropertyType type, StructType structType, int arrayIndex, int size, int flagBit) {

//		System.out.println("name = [" + name + "], type = [" + type + "], structType = [" + structType + "], arrayIndex = [" + arrayIndex +
//						   "], size = [" + size + "], flagBit = [" + flagBit + "]");

		int startPos = buffer.position();

		try {
			switch (type) {
				case BooleanProperty:
					return new BooleanProperty(this, name, flagBit > 0);
				case ByteProperty:
					return new ByteProperty(this, name, readByte());
				case IntegerProperty:
					return new IntegerProperty(this, name, readInt());
				case FloatProperty:
					return new FloatProperty(this, name, readFloat());
				case StrProperty:
				case StringProperty:
					return new StringProperty(this, name, readString());
				case NameProperty:
					return new NameProperty(this, name, name.equals(none) ? none : names[readIndex()]);
				case ObjectProperty:
					return new ObjectProperty(this, name, new ObjectReference(readIndex()));
				case StructProperty:
					switch (structType) {
						case PointRegion:
							return new PointRegionProperty(this, name, new ObjectReference(readIndex()), readInt(), readByte());
						case Vector:
							return new VectorProperty(this, name, readFloat(), readFloat(), readFloat());
						case Scale:
							return new ScaleProperty(this, name, readFloat(), readFloat(), readFloat(), readFloat(), readByte());
						case Rotator:
							return new RotatorProperty(this, name, readInt(), readInt(), readInt());
						case Color:
							return new ColorProperty(this, name, readByte(), readByte(), readByte(), readByte());
						default:
							// unknown struct, but perhaps we can assume it to be a vector at least
							if (size == 12) return new VectorProperty(this, name, readFloat(), readFloat(), readFloat());
							return new UnknownStructProperty(this, name);
					}
				case RotatorProperty:
					return new RotatorProperty(this, name, readInt(), readInt(), readInt());
				case VectorProperty:
					return new VectorProperty(this, name, readFloat(), readFloat(), readFloat());
				case ArrayProperty:
					return new ArrayProperty(this, name, new ObjectReference(readIndex()));
				case FixedArrayProperty:
					return new FixedArrayProperty(this, name, new ObjectReference(readIndex()), readIndex());
				default:
					throw new IllegalArgumentException("FIXME " + type);
			}
		} finally {
			if (buffer.position() - startPos < size) {
				// FIXME PointRegionProperty in version >= 126 specifically seems larger than specs indicate; 7 extra bytes
				// this also lets move past array payloads without consuming them yet
				moveRelative(size - (buffer.position() - startPos));
			}
		}
	}

	// --- buffer positioning and management

	/**
	 * Move to a position in the file, clear the buffer, and read from there.
	 *
	 * @param pos position in file
	 */
	void moveTo(long pos) {
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
	void moveRelative(int amount) {
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
	 * Read more data from the current position in the file, retaining unread
	 * bytes in the buffer.
	 */
	void fillBuffer() {
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
	int currentPosition() {
		try {
			return (int)(channel.position() - buffer.remaining());
		} catch (IOException e) {
			throw new IllegalStateException("Coult not determine current file position");
		}
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
			names[i] = new Name(readString(), readInt());
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
		assert names != null && names.length > 0;

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
		assert names != null && names.length > 0;

		Import[] imports = new Import[count];

		moveTo(pos);

		for (int i = 0; i < count; i++) {
			if (buffer.remaining() < 28) fillBuffer(); // more-or-less, probably less
			imports[i] = readImport();
		}

		return imports;
	}

	/**
	 * Read a string from the current buffer position.
	 *
	 * @return a string
	 */
	private String readString() {
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

		return name.trim();
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Export readExport() {
		return new Export(
				this,
				new ObjectReference(readIndex()), // class
				new ObjectReference(readIndex()), // super
				new ObjectReference(readInt()),   // group
				names[readIndex()], // name
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
				names[readIndex()], // package file
				names[readIndex()], // class
				new ObjectReference(readInt()),   // package name
				names[readIndex()]  // name
		);
	}

	// --- convenience

	byte readByte() {
		return buffer.get();
	}

	short readShort() {
		return buffer.getShort();
	}

	int readInt() {
		return buffer.getInt();
	}

	long readLong() {
		return buffer.getLong();
	}

	float readFloat() {
		return buffer.getFloat();
	}

	int readBytes(byte[] dest, int offset, int length) {
		int start = buffer.remaining();
		buffer.get(dest, offset, Math.min(buffer.remaining(), length));
		return start - buffer.remaining();
	}

	int readIndex() {
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

	public static class Name {

		public final String name;
		public final int flags;

		private Name(String name, int flags) {
			this.name = name;
			this.flags = flags;
		}

		public EnumSet<ObjectFlag> flags() {
			return ObjectFlag.fromFlags(flags);
		}

		@Override
		public String toString() {
			return String.format("Name [name=%s, flags=%s]", name, flags());
		}
	}

	public class ObjectReference {

		private final int index;

		private ObjectReference(int index) {
			this.index = index;
		}

		public Named get() {
			if (index < 0) {
				return imports[(-index) - 1];
			} else if (index > 0) {
				return exports[index - 1];
			} else {
				return Named.NULL;
			}
		}

		@Override
		public String toString() {
			return String.format("ObjectReference [index=%s]", index);
		}
	}

	public static class Export implements Named {

		private final Package pkg;

		public final ObjectReference objClass;
		public final ObjectReference objSuper;
		public final ObjectReference objGroup;
		public final Name name;
		public final int flags;
		public final int size;
		public final int pos;

		private Export(
				Package pkg, ObjectReference objClass, ObjectReference objSuper, ObjectReference objGroup,
				Name name, int flags, int size, int pos) {
			this.pkg = pkg;
			this.objClass = objClass;
			this.objSuper = objSuper;
			this.objGroup = objGroup;
			this.name = name;
			this.flags = flags;
			this.size = size;
			this.pos = pos;
		}

		@Override
		public Name name() {
			return name;
		}

		public Objects.Object object() {
			return pkg.object(this);
		}

		public EnumSet<ObjectFlag> flags() {
			return ObjectFlag.fromFlags(flags);
		}

		@Override
		public String toString() {
			return String.format("Export [objClass=%s, objSuper=%s, objGroup=%s, name=%s, flags=%s, size=%s, pos=%s]",
								 objClass, objSuper, objGroup, name, flags(), size, pos);
		}
	}

	public static class Import implements Named {

		public final Name file;
		public final Name className;
		public final ObjectReference packageName;
		public final Name name;

		private Import(Name file, Name className, ObjectReference packageName, Name name) {
			this.file = file;
			this.className = className;
			this.packageName = packageName;
			this.name = name;
		}

		@Override
		public Name name() {
			return name;
		}

		@Override
		public String toString() {
			return String.format("Import [file=%s, className=%s, packageName=%s, name=%s]", file, className, packageName, name);
		}
	}

}
