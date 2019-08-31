package net.shrimpworks.unreal.packages.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportedPackage implements Named {

	public static class ImportedObject implements Named {

		public final Name type;
		public final Name name;

		public ImportedObject(Name type, Name name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public Name name() {
			return name;
		}
	}

	private final Map<Import, ImportedPackage> packages;
	private final Set<ImportedObject> objects;
	private final Name name;

	public ImportedPackage(Name name) {
		this.name = name;
		this.packages = new HashMap<>();
		this.objects = new HashSet<>();
	}

	public ImportedPackage add(Deque<Import> imports) {
		Import next = imports.removeFirst();
		if (next.className.name.equals("Package")) {
			packages.computeIfAbsent(next, added -> new ImportedPackage(added.name)).add(imports);
		} else {
			this.objects.add(new ImportedObject(next.className, next.name));
		}
		return this;
	}

	@Override
	public Name name() {
		return name;
	}

	public Collection<ImportedPackage> packages() {
		return Collections.unmodifiableCollection(packages.values());
	}

	public Collection<ImportedObject> objects() {
		return Collections.unmodifiableCollection(objects);
	}

	@Override
	public String toString() {
		return String.format("ImportedPackage [packages=%s, objects=%s, name=%s]", packages.values(), objects, name.name);
	}
}
