package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

public class FixedArrayProperty extends ArrayProperty {

	public final int count;

	public FixedArrayProperty(Package pkg, Name name, ObjectReference arrayType, int count) {
		super(pkg, name, arrayType);
		this.count = count;
	}

	@Override
	public String toString() {
		return String.format("FixedArrayProperty [name=%s, arrayType=%s, count=%s]", name, arrayType, count);
	}
}
