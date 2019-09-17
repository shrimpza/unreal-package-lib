package net.shrimpworks.unreal.packages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedEntry;
import net.shrimpworks.unreal.packages.entities.ExportedField;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.FieldTypes;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.ImportedPackage;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.ObjectFlag;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.ObjectFactory;
import net.shrimpworks.unreal.packages.entities.objects.ObjectHeader;
import net.shrimpworks.unreal.packages.entities.properties.ArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.BooleanProperty;
import net.shrimpworks.unreal.packages.entities.properties.ByteProperty;
import net.shrimpworks.unreal.packages.entities.properties.FixedArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.FloatProperty;
import net.shrimpworks.unreal.packages.entities.properties.IntegerProperty;
import net.shrimpworks.unreal.packages.entities.properties.NameProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.PropertyType;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;
import net.shrimpworks.unreal.packages.entities.properties.StructProperty;

/**
 * An Unreal package.
 * <p>
 * Some common file extensions are <code>.u, .unr, .ut2, .utx</code>, and are
 * used by Unreal Engine games for packaging content such as maps, textures,
 * sounds, and the gameplay code itself.
 * <p>
 * Although the files all have different file extensions for organisation
 * purposes only, they all have the same structure and are capable of holding
 * the same content.
 * <p>
 * This implementation supports at least Unreal Engine 1 and 2, and has been
 * tested using content and assets from Unreal, Unreal Tournament, and Unreal
 * Tournament 2004. Your mileage may vary with other games using these engines.
 * <p>
 * Reading a packages' exported objects and their properties is currently
 * supported in this implementation. There is currently no support for
 * extraction of data such as UnrealScript classes.
 * <p>
 * See the {@link ObjectFactory} implementation for details on implementing
 * additional object content readers.
 */
public class Package implements Closeable {

	private static final int PKG_SIGNATURE = 0x9E2A83C1;
	private static final int MAX_PROPERTIES = 256;

	private static final String SHA1 = "SHA-1";

	private static final int[] PROPERTY_SIZE_MAP = { 1, 2, 4, 12, 16 };

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

		public static Set<PackageFlag> fromFlags(int flags) {
			Set<PackageFlag> objectFlags = EnumSet.noneOf(PackageFlag.class);
			objectFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
			return objectFlags;
		}
	}

	private final PackageReader reader;

	/**
	 * Package file version.
	 *
	 * <ul>
	 * <li><= 68 Unreal</li>
	 * <li>>= 69 Unreal Tournament</li>
	 * <li>>= 117(?) UE2 (UT2003/4)</li>
	 * </ul>
	 */
	public final int version;
	public final int license;

	/**
	 * Flags set for this package, also see {@link #flags()}.
	 */
	public final int flags;

	/**
	 * Names defined in this package.
	 */
	public final Name[] names;

	/**
	 * Exported entities, objects and fields, contained within this package.
	 */
	public final Export[] exports;

	/**
	 * Import references which this packages' exports depend on.
	 */
	public final Import[] imports;

	/**
	 * Convenience collection of all objects exported by this package.
	 */
	public final ExportedObject[] objects;

	/**
	 * Convenience collection of all non-objects exported by this package.
	 */
	public final ExportedField[] fields;

	// cache of already-parsed/read objects, simply keyed by file position
	private final WeakHashMap<Integer, Object> loadedObjects;

	public Package(Path packageFile) throws IOException {
		this(new PackageReader(packageFile));
	}

	public Package(PackageReader reader) {
		this.reader = reader;

		reader.moveTo(0); // overly explicit start from the start

		if (reader.readInt() != PKG_SIGNATURE) throw new IllegalArgumentException("Package does not seem to be an Unreal package");

		this.version = reader.readShort();
		this.license = reader.readShort();

		this.flags = reader.readInt();

		int nameCount = reader.readInt();
		int namePos = reader.readInt();

		int exportCount = reader.readInt();
		int exportPos = reader.readInt();

		int importCount = reader.readInt();
		int importPos = reader.readInt();

		if (version < 68) {
			// unused, we don't care about the heritage values or the heritage table
			reader.readInt(); // consume heritageCount
			reader.readInt(); // consume heritagePos
		} else {
			// unused, we don't care about the guid, or generation counters or the generation information
			byte[] guid = new byte[16];
			reader.readBytes(guid, 0, 16);
			int generationCount = reader.readInt();
			for (int i = 0; i < generationCount; i++) {
				reader.readInt(); // consume genExpCount
				reader.readInt(); // consume genNameCount
			}
		}

		// read the names table
		this.names = names(nameCount, namePos);

		// read the exports table; this simply reads the exports and makes no attempt to classify the exported content
		this.exports = exports(exportCount, exportPos);

		// read the imports table (arguably might be useful to read before exports)
		this.imports = imports(importCount, importPos);

		// convenience - try to collect objects and fields into separate collections for easier management
		this.objects = new ExportedObject[exports.length];
		this.fields = new ExportedField[exports.length];
		for (int i = 0; i < exports.length; i++) {
			ExportedEntry e = (ExportedEntry)exports[i];
			if (FieldTypes.isField(e.classIndex)) {
				fields[i] = e.asField();
			} else {
				objects[i] = e.asObject();
			}
		}

		this.loadedObjects = new WeakHashMap<>();
	}

	@Override
	public void close() throws IOException {
		this.reader.close();
	}

	public String sha1Hash() {
		return reader.hash(SHA1);
	}

	/**
	 * Get flags set on the package.
	 *
	 * @return flag set
	 */
	public Set<PackageFlag> flags() {
		return PackageFlag.fromFlags(flags);
	}

	public Map<Name, ImportedPackage> imports() {
		Map<Name, ImportedPackage> rootPackages = new HashMap<>();
		// get root level packages
		for (Import i : this.imports) {
			if (!i.className.name.equals("Package")) {
				Deque<Import> stack = new ArrayDeque<>();
				stack.addFirst(i);

				Named pkg = i.packageIndex.get();
				while (pkg != Named.NULL) {
					if (pkg instanceof Import) {
						stack.addFirst((Import)pkg);
						pkg = ((Import)pkg).packageIndex.get();
					}
				}

				Import root = stack.removeFirst();
				rootPackages.computeIfAbsent(root.name(), ImportedPackage::new).add(stack);
			}
		}
		return Collections.unmodifiableMap(rootPackages);
	}

	public Set<Export> rootExports() {
		return Arrays.stream(exports)
					 .filter(e -> e.groupName().equals(Name.NONE))
					 .collect(Collectors.toSet());
	}

	/**
	 * Convenience to get all exported elements by a known class name.
	 *
	 * @param className class to search for
	 * @return matching exports
	 */
	public Collection<Export> exportsByClassName(String className) {
		Set<Export> exports = new HashSet<>();
		for (Export ex : this.exports) {
			Named type = ex.classIndex.get();
			if (type instanceof Import && ((Import)type).name.name.equals(className)) {
				exports.add(ex);
			}
		}
		return exports;
	}

	/**
	 * Convenience to get all exported objects by a known class name.
	 *
	 * @param className class to search for
	 * @return matching objects
	 */
	public Collection<ExportedObject> objectsByClassName(String className) {
		Set<ExportedObject> exports = new HashSet<>();
		for (ExportedObject ex : this.objects) {
			if (ex == null) continue;
			Named type = ex.classIndex.get();
			if (type instanceof Import && ((Import)type).name.name.equals(className)) {
				exports.add(ex);
			}
		}
		return exports;
	}

	/**
	 * Convenience to get an object by an object reference.
	 *
	 * @return found object
	 * @throws IllegalArgumentException the object could not be found or does not exist
	 */
	public ExportedObject objectByRef(ObjectReference ref) {
		Named resolved = ref.get();
		if (!(resolved instanceof Export)) throw new IllegalArgumentException("No exported object found for reference " + ref);

		ExportedObject exportedObject = objects[((Export)resolved).index];

		if (exportedObject == null) throw new IllegalArgumentException("Found export is not an object " + ref);

		return exportedObject;
	}

	/**
	 * Convenience to get an object by its name.
	 *
	 * @return found object
	 * @throws IllegalArgumentException the object could not be found or does not exist
	 */
	public ExportedObject objectByName(Name name) {
		for (ExportedObject object : objects) {
			if (object == null) continue;

			if (object.name.name.equalsIgnoreCase(name.name)) return object;
		}

		return null;
	}

	/**
	 * Convenience to get an object by its export representation.
	 *
	 * @return found object
	 * @throws IllegalArgumentException the object could not be found or does not exist
	 */
	public ExportedObject objectByExport(Export export) {
		ExportedObject exportedObject = objects[export.index];
		if (exportedObject == null) throw new IllegalArgumentException("Found export is not an object " + export);
		return exportedObject;
	}

	// --- primary data table readers

	/**
	 * Read all names from the file.
	 *
	 * @param count number of names in the file
	 * @param pos   position of first name within the file
	 * @return array of names
	 */
	private Name[] names(int count, int pos) {
		Name[] names = new Name[count];

		reader.moveTo(pos);

		for (int i = 0; i < count; i++) {
			reader.ensureRemaining(256); // more-or-less
			names[i] = new Name(reader.readString(version), reader.readInt());
		}

		return names;
	}

	/**
	 * Read all exports from the file.
	 *
	 * @param count number of exports in the file
	 * @param pos   position of the first export within the file
	 * @return array of exports
	 */
	private Export[] exports(int count, int pos) {
		assert names != null && names.length > 0;

		Export[] exports = new Export[count];

		reader.moveTo(pos);

		for (int i = 0; i < count; i++) {
			reader.ensureRemaining(28); // more-or-less, probably less
			exports[i] = readExport(i);
		}

		return exports;
	}

	/**
	 * Read all exports from the file.
	 *
	 * @param count number of exports in the file
	 * @param pos   position of the first export within the file
	 * @return array of exports
	 */
	private Import[] imports(int count, int pos) {
		assert names != null && names.length > 0;

		Import[] imports = new Import[count];

		reader.moveTo(pos);

		for (int i = 0; i < count; i++) {
			reader.ensureRemaining(28); // more-or-less, probably less
			imports[i] = readImport();
		}

		return imports;
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Export readExport(int index) {
		ObjectReference expClass = new ObjectReference(this, reader.readIndex());
		ObjectReference expSuper = new ObjectReference(this, reader.readIndex());
		ObjectReference expGroup = new ObjectReference(this, reader.readInt());

		Name name = names[reader.readIndex()];
		int flags = reader.readInt();
		int size = reader.readIndex();
		int pos = name.equals(Name.NONE)
				? 0
				: reader.readIndex(); // magical undocumented case; "None" does not have a pos, though it has a (0) size

		return new ExportedEntry(
				this, index,
				expClass, expSuper, expGroup,
				name, flags, size, pos
		);
	}

	/**
	 * Read a single export from the current buffer position.
	 *
	 * @return a new export
	 */
	private Import readImport() {
		return new Import(
				names[reader.readIndex()], // package file
				names[reader.readIndex()], // class
				new ObjectReference(this, reader.readInt()),   // package name
				names[reader.readIndex()]  // name
		);
	}

	/**
	 * Create an Object for the provided export.
	 * <p>
	 * This implementation will read and populate the resulting object's
	 * properties collection, and then return an object of an appropriate
	 * type if possible.
	 * <p>
	 * The specific object implementation should expose methods to obtain
	 * instances of the object data itself in appropriate formats.
	 *
	 * @param export the export to get an object for
	 * @return a new object instance
	 */
	public Object object(ExportedObject export) {
		Object existing = loadedObjects.get(export.pos);
		if (existing != null) return existing;

		if (export.size <= 0) throw new IllegalStateException(String.format("Export %s has no associated object data!", export.name));

		if (export.classIndex.index == 0) return null;

		reader.moveTo(export.pos);

		ObjectHeader header;
		if (export.flags().contains(ObjectFlag.HasStack)) {
			int node = reader.readIndex();
			header = new ObjectHeader(
					node, reader.readIndex(), reader.readLong(), reader.readInt(),
					node != 0 ? reader.readIndex() : 0
			);
		} else {
			header = null;
		}

		List<Property> properties = new ArrayList<>();
		for (int i = 0; i < MAX_PROPERTIES; i++) {
			Property p = readProperty();

			if (p.name.equals(Name.NONE)) break;
			else properties.add(p);
		}

		// keep track of how long the properties were, so we can potentially continue reading object data from this point
		long propsLength = reader.currentReadPosition();

		Object newObject = ObjectFactory.newInstance(this, reader, export, header, properties, (int)propsLength);

		loadedObjects.put(export.pos, newObject);

		return newObject;
	}

	/**
	 * Utility method to read an individual object property.
	 *
	 * @return property of the appropriate type
	 */
	private Property readProperty() {
		int nameIndex = reader.readIndex();
		Name name = names[nameIndex];

		// the end - don't read or process anything beyond here
		if (name.equals(Name.NONE)) return new NameProperty(this, name, name);

		byte propInfo = reader.readByte();

		byte type = (byte)(propInfo & 0b00001111); // bits 0 to 3 are the type
		int size = (propInfo & 0b01110000) >> 4; // bits 4 to 6 is the size
		boolean arrayFlag = (propInfo & 0x80) != 0; // bit 7 is either array size (??), or boolean value

		PropertyType propType = PropertyType.get(type);

		if (propType == null) {
			throw new IllegalStateException(String.format("Unknown property type index %d for property %s", type, name.name));
		}

		// When a struct, type of struct follows before size and body
		StructProperty.StructType structType = null;
		if (propType == PropertyType.StructProperty) {
			int structIdx = reader.readIndex();
			structType = structIdx >= 0 ? StructProperty.StructType.get(names[structIdx]) : null;
			if (structType == null) {
				throw new IllegalStateException(String.format("Unknown struct type index %d for property %s", structIdx, name.name));
			}
		}

		switch (size) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				size = PROPERTY_SIZE_MAP[size];
				break;
			case 5:
				size = reader.readByte() & 0xFF;
				break;
			case 6:
				size = reader.readShort();
				break;
			case 7:
				size = reader.readInt();
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown property field size %d", size));
		}

		// if array and not boolean, next byte is index of property within the array (??)
		int arrayIndex = 0;
		if (arrayFlag && propType != PropertyType.BooleanProperty) {
			arrayIndex = reader.readByte();
		}

		return createProperty(name, propType, structType, arrayIndex, size, arrayFlag);
	}

	/**
	 * Utilities all the way down, this creates a typed property instance
	 * based on the provided property type.
	 *
	 * @param name       property name
	 * @param type       type of property
	 * @param structType if a struct property, the struct type
	 * @param arrayIndex in an array property, the index of this property within an array
	 * @param size       the byte length of the property
	 * @param arrayFlag  the final bit of the property header, sometimes used to infer things
	 * @return a new property
	 */
	private Property createProperty(
			Name name, PropertyType type, StructProperty.StructType structType, int arrayIndex, int size, boolean arrayFlag) {

		int startPos = reader.position();

		try {
			switch (type) {
				case BooleanProperty:
					return new BooleanProperty(this, name, arrayFlag);
				case ByteProperty:
					return new ByteProperty(this, name, reader.readByte());
				case IntegerProperty:
					return new IntegerProperty(this, name, reader.readInt());
				case FloatProperty:
					return new FloatProperty(this, name, reader.readFloat());
				case StrProperty:
				case StringProperty:
					return new StringProperty(this, name, reader.readString(version));
				case NameProperty:
					return new NameProperty(this, name, name.equals(Name.NONE) ? Name.NONE : names[reader.readIndex()]);
				case ObjectProperty:
					return new ObjectProperty(this, name, new ObjectReference(this, reader.readIndex()));
				case StructProperty:
					switch (structType) {
						case PointRegion:
							return new StructProperty.PointRegionProperty(this, name, new ObjectReference(this, reader.readIndex()),
																		  reader.readInt(), reader.readByte());
						case Scale:
							return new StructProperty.ScaleProperty(this, name, reader.readFloat(), reader.readFloat(), reader.readFloat(),
																	reader.readFloat(), reader.readByte());
						case Rotator:
							return new StructProperty.RotatorProperty(this, name, reader.readInt(), reader.readInt(), reader.readInt());
						case Color:
							return new StructProperty.ColorProperty(this, name, reader.readByte(), reader.readByte(), reader.readByte(),
																	reader.readByte());
						case Sphere:
							return new StructProperty.SphereProperty(this, name, reader.readFloat(), reader.readFloat(), reader.readFloat(),
																	 reader.readFloat());
						case Vector:
						default:
							// unknown struct, but perhaps we can assume it to be a vector at least
							if (size == 12) {
								return new StructProperty.VectorProperty(this, name, reader.readFloat(), reader.readFloat(),
																		 reader.readFloat());
							}
							return new StructProperty.UnknownStructProperty(this, name);
					}
				case RotatorProperty:
					return new StructProperty.RotatorProperty(this, name, reader.readInt(), reader.readInt(), reader.readInt());
				case VectorProperty:
					return new StructProperty.VectorProperty(this, name, reader.readFloat(), reader.readFloat(), reader.readFloat());
				case ArrayProperty:
					return new ArrayProperty(this, name, new ObjectReference(this, reader.readIndex()));
				case FixedArrayProperty:
					return new FixedArrayProperty(this, name, new ObjectReference(this, reader.readIndex()), reader.readIndex());
				default:
					throw new IllegalArgumentException("Cannot read unsupported property type " + type.name());
			}
		} finally {
			// if we didn't read all the property's bytes somehow, fast-forward to the end of the property...
			// FIXME PointRegionProperty in version >= 126 specifically seems larger than specs indicate; 7 extra bytes
			// this also lets move past array payloads without consuming them yet in this version
			if (reader.position() - startPos < size) {
				reader.moveRelative(size - (reader.position() - startPos));
			}
		}
	}

}
