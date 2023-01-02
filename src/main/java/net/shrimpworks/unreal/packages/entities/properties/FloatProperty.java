package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class FloatProperty extends Property {

	public final float value;

	public FloatProperty(Package pkg, Name name, float value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("FloatProperty [name=%s, value=%s]", name, value);
	}
}
