package net.shrimpworks.unreal.packages.entities;

/**
 * Represents an element imported by a package, normally required by a
 * package's exports.
 */
public class Import implements Named {

	/**
	 * Package of the import type, eg; the "Engine" in "Engine.Texture".
	 */
	public final Name classPackage;

	/**
	 * The class type of the import; eg: Package, Class, Texture, etc.
	 */
	public final Name className;

	/**
	 * Package or group the import references.
	 */
	public final ObjectReference packageName;

	/**
	 *
	 */
	public final Name name;

	public Import(Name classPackage, Name className, ObjectReference packageName, Name name) {
		this.classPackage = classPackage;
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
		return String.format("Import [classPackage=%s, className=%s, packageName=%s, name=%s]", classPackage, className, packageName, name);
	}
}
