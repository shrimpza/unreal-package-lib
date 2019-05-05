package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * Convenience structure representing a bounding box with minimum
 * and maximum dimensions and a validity flag.
 * <p>
 * Data format:
 * <pre>
 *   - [Vector] min
 *   - [Vector] max
 *   - [byte] valid (0 / 1)
 * </pre>
 */
public class Bound {

	public final Vector min;
	public final Vector max;
	public final boolean valid;

	public Bound(Vector min, Vector max, boolean valid) {
		this.min = min;
		this.max = max;
		this.valid = valid;
	}

	public Bound(PackageReader reader) {
		this(new Vector(reader),
			 new Vector(reader),
			 reader.readByte() > 0);
	}

	@Override
	public String toString() {
		return String.format("[min=%s, max=%s, valid=%s]", min, max, valid);
	}
}
