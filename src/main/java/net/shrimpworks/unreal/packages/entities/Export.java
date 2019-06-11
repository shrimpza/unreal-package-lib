package net.shrimpworks.unreal.packages.entities;

import java.util.Set;

import net.shrimpworks.unreal.packages.Package;

/**
 * Base class for entities representing entries within a package's Exports
 * table.
 * <p>
 * This class holds the properties common to all implementations.
 */
public abstract class Export implements Named {

	final Package pkg;
	public final int index;

	public final ObjectReference objClass;
	public final ObjectReference objSuper;
	public final ObjectReference objGroup;
	public final Name name;
	public final int flags;
	public final int size;
	public final int pos;

	Export(
			Package pkg, int index, ObjectReference objClass, ObjectReference objSuper, ObjectReference objGroup, Name name, int flags,
			int size, int pos) {
		this.pkg = pkg;
		this.index = index;
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

	public Set<ObjectFlag> flags() {
		return ObjectFlag.fromFlags(flags);
	}

	@Override
	public String toString() {
		return String.format("Export [index=%s, objClass=%s, objSuper=%s, objGroup=%s, name=%s, flags=%s, size=%s, pos=%s]",
							 index, objClass, objSuper, objGroup, name, flags(), size, pos);
	}
}
