package net.shrimpworks.unreal.packages.entities;

import net.shrimpworks.unreal.packages.Package;

/**
 * A reference to an entry in either the package's imports or exports
 * table.
 * <p>
 * It works as follows:
 *
 * <ul>
 * <li>If Index==0: The object is NULL (known as NULL in C++, None in UnrealScript).</li>
 * <li>If Index<0: Refers to the (-Index-1)th object in this file's import table.</li>
 * <li>If Index>0: Refers to the (Index-1)th object in this file's export table.</li>
 * </ul>
 */
public class ObjectReference {

	private final Package pkg;
	public final int index;

	public static final ObjectReference NULL = new ObjectReference(null, 0);

	public ObjectReference(Package pkg, int index) {
		this.pkg = pkg;
		this.index = index;
	}

	public Named get(boolean simpleExports) {
		if (index < 0) {
			return pkg.imports[(-index) - 1];
		} else if (index > 0) {
			// find most specific match
			if (!simpleExports && pkg.objects[index - 1] != null) return pkg.objects[index - 1];
			else if (!simpleExports && pkg.fields[index - 1] != null) return pkg.fields[index - 1];
			else return pkg.exports[index - 1];
		} else {
			return Named.NULL;
		}
	}

	public Named get() {
		return get(false);
	}

	@Override
	public String toString() {
		return String.format("ObjectReference [index=%s]", index);
	}
}
