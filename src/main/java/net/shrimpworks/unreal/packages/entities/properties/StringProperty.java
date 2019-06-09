package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class StringProperty extends Property {

	public final String value;

	public StringProperty(Package pkg, Name name, String value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("StringProperty [name=%s, value=%s]", name, value);
	}
}
