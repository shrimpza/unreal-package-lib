package net.shrimpworks.unreal.packages.entities;

import java.util.EnumSet;

/**
 * Represents a name from a package's names table.
 */
public class Name {

	public static final Name NONE = new Name("None", 0);

	public final String name;
	public final int flags;

	public Name(String name, int flags) {
		this.name = name;
		this.flags = flags;
	}

	public Name(String name) {
		this(name, 0);
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
		Name other = (Name)o;
		return java.util.Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(name);
	}
}
