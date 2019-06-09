package net.shrimpworks.unreal.packages.entities;

/**
 * Represents something not an object exported by a package.
 * <p>
 * These are typically not instances of things as with Objects, but rather
 * the definitions of things, such as classes, scripts, etc.
 */
public class ExportedField extends ExportedEntry {

	ExportedField(ExportedEntry export) {
		super(export.pkg, export.index, export.objClass, export.objSuper, export.objGroup, export.name, export.flags, export.size,
			  export.pos);
	}
}
