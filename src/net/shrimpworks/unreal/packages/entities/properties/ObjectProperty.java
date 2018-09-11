package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

public class ObjectProperty extends Property {

	public final ObjectReference value;

	public ObjectProperty(Package pkg, Name name, ObjectReference value) {
		super(pkg, name);
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("ObjectProperty [name=%s, value=%s]", name, value);
	}
}
