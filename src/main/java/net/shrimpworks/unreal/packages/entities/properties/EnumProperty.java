package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class EnumProperty extends NameProperty {

	public EnumProperty(Package pkg, Name name, Name value) {
		super(pkg, name, value);
	}

	@Override
	public String toString() {
		return String.format("EnumProperty [name=%s, value=%s]", name, value);
	}
}
