package net.shrimpworks.unreal.packages.entities;

import java.util.Map;

import net.shrimpworks.unreal.packages.Package;

/**
 * Internal utility implementation of an Export, with factories for
 * obtaining the referenced exported entity as either an Object with
 * properties and content, or as a Field (class, script, enum, etc).
 */
public class ExportedEntry extends Export {

	public ExportedEntry(
			Package pkg, int index, ObjectReference classIndex, ObjectReference superClassIndex, ObjectReference groupIndex, Name name,
			long flags, int size, int pos, Map<Name, ObjectReference> components) {
		super(pkg, index, classIndex, superClassIndex, groupIndex, name, flags, size, pos, components);
	}

	public ExportedObject asObject() {
		return new ExportedObject(this);
	}

	public ExportedField asField() {
		return new ExportedField(this);
	}
}
