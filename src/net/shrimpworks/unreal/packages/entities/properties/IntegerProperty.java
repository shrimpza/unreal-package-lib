package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class IntegerProperty extends Property {

	public final int value;

	public IntegerProperty(Package pkg, Name name, int value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("IntegerProperty [name=%s, value=%s]", name, value);
	}
}
