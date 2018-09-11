package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class BooleanProperty extends Property {

	public final boolean value;

	public BooleanProperty(Package pkg, Name name, boolean value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("BooleanProperty [name=%s, value=%s]", name, value);
	}
}
