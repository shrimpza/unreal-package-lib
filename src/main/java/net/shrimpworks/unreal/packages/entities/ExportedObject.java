package net.shrimpworks.unreal.packages.entities;

import net.shrimpworks.unreal.packages.entities.objects.Object;

/**
 * Represents an object exported by a package.
 * <p>
 * Typically content, such as a texture, mesh or sound, or otherwise
 * things like instances of entities placed within a level (lights,
 * playerstarts, weapons, etc).
 */
public class ExportedObject extends ExportedEntry {

	ExportedObject(ExportedEntry export) {
		super(export.pkg, export.index, export.classIndex, export.classSuperIndex, export.groupIndex, export.name, export.flags, export.size,
			  export.pos);
	}

	public Object object() {
		return pkg.object(this);
	}

}
