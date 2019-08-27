package net.shrimpworks.unreal.packages.entities;

import java.util.ArrayList;
import java.util.List;

public class ImportedPackage implements Named {

	public enum ImportType {
		Package,
		Class,
		Texture,
		ScriptedTexture,
		LodMesh;

		public static ImportType fromName(Name className) {
			return ImportType.valueOf(className.name);
		}
	}

	public static class ImportObject implements Named {

		public final ImportType type;
		public final Name name;

		public ImportObject(ImportType type, Name name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public Name name() {
			return name;
		}
	}

	public final List<ImportedPackage> packages;
	public final List<ImportObject> objects;

	public final Name name;

	public ImportedPackage(Name name) {
		this.name = name;
		this.packages = new ArrayList<>();
		this.objects = new ArrayList<>();
	}

	@Override
	public Name name() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("ImportedPackage [packages=%s, objects=%s, name=%s]", packages, objects, name.name);
	}
}
