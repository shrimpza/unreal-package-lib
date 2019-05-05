package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * Simple Vector type with floating point signed X, Y, Z values.
 * <p>
 * Data format:
 * <pre>
 *   - [float] x
 *   - [float] y
 *   - [float] z
 * </pre>
 */
public class Vector {

	public final float x;
	public final float y;
	public final float z;

	public Vector(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector(PackageReader reader) {
		this(reader.readFloat(), reader.readFloat(), reader.readFloat());
	}

	@Override
	public String toString() {
		return String.format("[x=%.4f, y=%.4f, z=%.4f]", x, y, z);
	}
}
