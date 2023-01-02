package net.shrimpworks.unreal.packages.entities;

import java.util.Set;

/**
 * Represents a name from a package's names table.
 */
public class Name implements Comparable<Name> {

	public static final Name NONE = new Name("None", 0, 0);

	public final String name;
	public final int number;
	public final long flags;

	public Name(String name, int number, long flags) {
		this.name = name;
		this.number = number;
		this.flags = flags;
	}

	public Name(String name, int number) {
		this(name, number, 0);
	}

	public Name(String name) {
		this(name, 0, 0);
	}

	public Set<ObjectFlag> flags() {
		return ObjectFlag.fromFlags(flags);
	}

	@Override
	public String toString() {
		return String.format("Name [name=%s, number=%d, flags=%s]", name, number, flags());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Name other)) return false;
		return java.util.Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(name);
	}

	@Override
	public int compareTo(Name other) {
		return name.compareTo(other.name);
	}
}
