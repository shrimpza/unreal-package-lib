package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.packages.Package;

/**
 * Represents an element imported by a package, normally required by a
 * package's exports.
 */
public class Import implements Named {

	private final Package pkg;

	private final int index;

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
	public final ObjectReference packageIndex;

	/**
	 *
	 */
	public final Name name;

	public Import(Package pkg, int index, Name classPackage, Name className, ObjectReference packageIndex, Name name) {
		this.pkg = pkg;
		this.index = index;
		this.classPackage = classPackage;
		this.className = className;
		this.packageIndex = packageIndex;
		this.name = name;
	}

	@Override
	public Name name() {
		return name;
	}

	/**
	 * Get imported groups and objects under this one.
	 *
	 * @return child imports
	 */
	public Set<Import> children() {
		return Arrays.stream(pkg.imports).filter(i -> i.packageIndex.get() == this).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return String.format("Import [classPackage=%s, className=%s, packageName=%s, name=%s]",
							 classPackage, className, packageIndex, name);
	}
}
