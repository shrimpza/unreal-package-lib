package net.shrimpworks.unreal.archive;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface Entities {

	static Name NONE = new Name("None", 0);

	enum ObjectFlag {
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

	interface Named {

		public static Named NULL = () -> NONE;

		public Name name();
	}

	enum FieldTypes {
		Const,
		Enum,
		Struct,
		Function,
		State,
		TextBuffer,
		Property,
		ByteProperty,
		ObjectProperty,
		FixedArrayProperty,
		ArrayProperty,
		MapProperty,
		ClassProperty,
		StructProperty,
		IntProperty,
		BoolProperty,
		FloatProperty,
		NameProperty,
		StrProperty,
		StringProperty;

		private static Set<String> STRINGS = Arrays.stream(values()).map(java.lang.Enum::name).collect(Collectors.toSet());

		static boolean isField(ObjectReference classRef) {
			Named named = classRef.get();
			if (named == null) return true;
			if (classRef.index == 0) return true;
			return STRINGS.contains(named.name().name);
		}
	}

	/**
	 * A reference to an entry in either the package's imports or exports
	 * table.
	 * <p>
	 * It works as follows:
	 *
	 * <ul>
	 * <li>If Index==0: The object is NULL (known as NULL in C++, None in UnrealScript).</li>
	 * <li>If Index<0: Refers to the (-Index-1)th object in this file's import table.</li>
	 * <li>If Index>0: Refers to the (Index-1)th object in this file's export table.</li>
	 * </ul>
	 */
	public class ObjectReference {

		private final Package pkg;
		public final int index;

		ObjectReference(Package pkg, int index) {
			this.pkg = pkg;
			this.index = index;
		}

		public Named get() {
			if (index < 0) {
				return pkg.imports[(-index) - 1];
			} else if (index > 0) {
				return pkg.exports[index - 1];
			} else {
				return Named.NULL;
			}
		}

		@Override
		public String toString() {
			return String.format("ObjectReference [index=%s]", index);
		}
	}

	/**
	 * Represents a name from a package's names table.
	 */
	public class Name {

		public final String name;
		public final int flags;

		Name(String name, int flags) {
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Name)) return false;
			Name name1 = (Name)o;
			return java.util.Objects.equals(name, name1.name);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(name);
		}
	}

	/**
	 * Base class for entities representing entries within a package's Exports
	 * table.
	 * <p>
	 * This class holds the properties common to all implementations.
	 */
	public abstract class Export implements Named {

		final Package pkg;

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

		public EnumSet<ObjectFlag> flags() {
			return ObjectFlag.fromFlags(flags);
		}

		@Override
		public String toString() {
			return String.format("Export [objClass=%s, objSuper=%s, objGroup=%s, name=%s, flags=%s, size=%s, pos=%s]",
								 objClass, objSuper, objGroup, name, flags(), size, pos);
		}
	}

	/**
	 * Internal utility implementation of an Export, with factories for
	 * obtaining the referenced exported entity as either an Object with
	 * properties and content, or as a Field (class, script, enum, etc).
	 */
	class ExportedEntry extends Export {

		ExportedEntry(
				Package pkg, ObjectReference objClass, ObjectReference objSuper, ObjectReference objGroup, Name name, int flags, int size,
				int pos) {
			super(pkg, objClass, objSuper, objGroup, name, flags, size, pos);
		}

		ExportedObject asObject() {
			return new ExportedObject(this);
		}

		ExportedField asField() {
			return new ExportedField(this);
		}
	}

	/**
	 * Represents an object exported by a package.
	 * <p>
	 * Typically content, such as a texture, mesh or sound, or otherwise
	 * things like instances of entities placed within a level (lights,
	 * playerstarts, weapons, etc).
	 */
	public class ExportedObject extends ExportedEntry {

		private ExportedObject(ExportedEntry export) {
			super(export.pkg, export.objClass, export.objSuper, export.objGroup, export.name, export.flags, export.size, export.pos);
		}

		public Objects.Object object() {
			return pkg.object(this);
		}

	}

	/**
	 * Represents something not an object exported by a package.
	 * <p>
	 * These are typically not instances of things as with Objects, but rather
	 * the definitions of things, such as classes, scripts, etc.
	 */
	public class ExportedField extends ExportedEntry {

		private ExportedField(ExportedEntry export) {
			super(export.pkg, export.objClass, export.objSuper, export.objGroup, export.name, export.flags, export.size, export.pos);
		}
	}

	/**
	 * Represents an element imported by a package, normally required by a
	 * package's exports.
	 */
	public class Import implements Named {

		public final Name file;
		public final Name className;
		public final ObjectReference packageName;
		public final Name name;

		Import(Name file, Name className, ObjectReference packageName, Name name) {
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
