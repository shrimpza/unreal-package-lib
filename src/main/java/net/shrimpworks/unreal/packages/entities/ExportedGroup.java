package net.shrimpworks.unreal.packages.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExportedGroup implements Named {

	private final Map<Name, ExportedGroup> groups;
	private final Set<Export> objects;

	private final Name name;

	public ExportedGroup(Name name) {
		this.name = name;
		this.groups = new HashMap<>();
		this.objects = new HashSet<>();
	}

	public ExportedGroup add(Deque<Export> exports) {
		Export next = exports.removeFirst();
		if (next.classIndex.get().equals(Named.NULL)) {
			// the class for a group appears to be blank in the case of groups within a package
			groups.computeIfAbsent(next.name, ExportedGroup::new).add(exports);
		} else {
			this.objects.add(next);
		}
		return this;
	}

	@Override
	public Name name() {
		return name;
	}

	public Map<Name, ExportedGroup> groups() {
		return Collections.unmodifiableMap(groups);
	}

	public Collection<Export> objects() {
		return Collections.unmodifiableCollection(objects);
	}

	@Override
	public String toString() {
		return String.format("ImportedPackage [groups=%s, objects=%s, name=%s]", groups.values(), objects, name.name);
	}
}
