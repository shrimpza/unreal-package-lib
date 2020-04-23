package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

/**
 * This is a property type defined by {@link PropertyType#ArrayProperty},
 * identified as property type 9.
 * <p>
 * However, there's no documentation for this type, and it appears to
 * be something that was never actually implemented or used, since array
 * properties are simply regular properties, with indexes and things
 * defined by the property header.
 */
public class UnknownArrayProperty extends Property {

	public final ObjectReference arrayType;

	public UnknownArrayProperty(Package pkg, Name name, ObjectReference arrayType) {
		super(pkg, name);
		this.arrayType = arrayType;
	}

	@Override
	public String toString() {
		return String.format("ArrayProperty [name=%s, arrayType=%s]", name, arrayType);
	}
}
