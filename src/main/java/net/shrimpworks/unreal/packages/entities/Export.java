package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.Map;
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

	public final long flags;
	public final int size;
	public final int pos;

	public final Map<Name, ObjectReference> components;

	// cached collection of children
	private Set<Export> children;

	Export(Package pkg, int index, ObjectReference classIndex, ObjectReference classSuperIndex, ObjectReference groupIndex, Name name,
		   long flags, int size, int pos, Map<Name, ObjectReference> components) {
		this.pkg = pkg;
		this.index = index;
		this.classIndex = classIndex;
		this.classSuperIndex = classSuperIndex;
		this.groupIndex = groupIndex;
		this.name = name;
		this.flags = flags;
		this.size = size;
		this.pos = pos;
		this.components = components;
	}

	@Override
	public Name name() {
		return name;
	}

	public Set<ObjectFlag> flags() {
		return ObjectFlag.fromFlags(flags);
	}

	/**
	 * Get the name of the group this export belongs to.
	 *
	 * @return parent group
	 */
	public Name groupName() {
		return groupIndex.get().name();
	}

	/**
	 * Get exported groups, objects and properties under this export.
	 *
	 * @return child exports
	 */
	public Set<Export> children() {
		if (children == null) children = Arrays.stream(pkg.exports).filter(e -> e.groupIndex.get(true) == this).collect(Collectors.toSet());
		return children;
	}

	@Override
	public String toString() {
		return String.format("%s [index=%s, classIndex=%s, classSuperIndex=%s, groupIndex=%s, name=%s, flags=%s, size=%s, pos=%s, components=%s]",
							 getClass().getSimpleName(), index, classIndex, classSuperIndex, groupIndex, name, flags(), size, pos, components);
	}
}
