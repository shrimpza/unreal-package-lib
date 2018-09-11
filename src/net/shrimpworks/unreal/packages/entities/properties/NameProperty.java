package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class NameProperty extends Property {

	public final Name value;

	public NameProperty(Package pkg, Name name, Name value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("NameProperty [name=%s, value=%s]", name, value);
	}
}
