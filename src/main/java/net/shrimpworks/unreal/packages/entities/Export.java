package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.packages.Package;

/**
 * Base class for entities representing entries within a package's Exports
 * table.
 * <p>
 * This class holds the properties common to all implementations.
 */
public abstract class Export implements Named {

	protected final Package pkg;

	public final int index;

	/**
	 * Reference to the class of this export.
	 */
	public final ObjectReference classIndex;

	/**
	 * Reference to the super class of this export.
	 */
	public final ObjectReference classSuperIndex;

	/**
	 * Reference to the group this export is within.
	 */
	public final ObjectReference groupIndex;

	/**
	 * Name of the export.
	 */
	public final Name name;

	public final int flags;
	public final int size;
	public final int pos;

	Export(Package pkg, int index, ObjectReference classIndex, ObjectReference classSuperIndex, ObjectReference groupIndex, Name name,
		   int flags, int size, int pos) {
		this.pkg = pkg;
		this.index = index;
		this.classIndex = classIndex;
		this.classSuperIndex = classSuperIndex;
		this.groupIndex = groupIndex;
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

	public Name groupName() {
		return groupIndex.get().name();
	}

	public Set<Export> children() {
		return Arrays.stream(pkg.exports).filter(e -> e.groupIndex.index == index + 1).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return String.format("Export [index=%s, classIndex=%s, classSuperIndex=%s, groupIndex=%s, name=%s, flags=%s, size=%s, pos=%s]",
							 index, classIndex, classSuperIndex, groupIndex, name, flags(), size, pos);
	}
}
