package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

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

	public final int count;

	public UnknownArrayProperty(Package pkg, Name name, int count) {
		super(pkg, name);
		this.count = count;
	}

	@Override
	public String toString() {
		return String.format("ArrayProperty [name=%s, count=%d]", name, count);
	}
}
