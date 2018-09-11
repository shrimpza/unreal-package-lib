package net.shrimpworks.unreal.packages.entities;

/**
 * Represents an element imported by a package, normally required by a
 * package's exports.
 */
public class Import implements Named {

	public final Name file;
	public final Name className;
	public final ObjectReference packageName;
	public final Name name;

	public Import(Name file, Name className, ObjectReference packageName, Name name) {
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
