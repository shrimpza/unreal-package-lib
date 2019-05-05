package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * Simple Plane type with floating point signed X, Y, Z, W.
 * <p>
 * Data format:
 * <pre>
 *   - [float] x
 *   - [float] y
 *   - [float] z
 *   - [float] w
 * </pre>
 */
public class Plane extends Vector {

	public final float w;

	public Plane(float x, float y, float z, float w) {
		super(x, y, z);
		this.w = w;
	}

	public Plane(PackageReader reader) {
		this(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat());
	}

	public String toString() {
		return String.format("[x=%.4f, y=%.4f, z=%.4f, w=%.4f]", x, y, z, w);
	}
}
