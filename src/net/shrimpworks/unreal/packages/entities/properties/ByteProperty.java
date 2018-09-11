package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class ByteProperty extends Property {

	public final byte value;

	public ByteProperty(Package pkg, Name name, byte value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("ByteProperty [name=%s, value=%s]", name, value);
	}
}
