package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.Named;

/**
 * Base type for object properties.
 * <p>
 * Individual properties are instantiated by the {@link Package} class during
 * object instantiation.
 */
public abstract class Property implements Named {

	final Package pkg;
	public final Name name;

	Property(Package pkg, Name name) {
		this.pkg = pkg;
		this.name = name;
	}

	@Override
	public Name name() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("Property [name=%s]", name);
	}
}
