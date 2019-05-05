package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * A bounding sphere with floating point signed X, Y, Z position
 * and a radius.
 * <p>
 * Data format:
 * <pre>
 *   - [float] x
 *   - [float] y
 *   - [float] z
 *   - [float] radius
 * </pre>
 */
public class Sphere {

	public final float x;
	public final float y;
	public final float z;
	public final float radius;

	public Sphere(float x, float y, float z, float radius) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
	}

	public Sphere(PackageReader reader) {
		this(reader.readFloat(),
			 reader.readFloat(),
			 reader.readFloat(),
			 reader.readFloat()
		);
	}

	@Override
	public String toString() {
		return String.format("[x=%.4f, y=%.4f, z=%.4f, radius=%.4f]", x, y, z, radius);
	}
}
