package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

public class ArrayProperty extends Property {

	public final ObjectReference arrayType;

	public ArrayProperty(Package pkg, Name name, ObjectReference arrayType) {
		super(pkg, name);
		this.arrayType = arrayType;
	}

	@Override
	public String toString() {
		return String.format("ArrayProperty [name=%s, arrayType=%s]", name, arrayType);
	}
}
