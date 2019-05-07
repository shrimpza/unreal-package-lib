package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * I'm not sure what this is. Some sort of vertex index and "side" value.
 * <p>
 * Data format:
 * <pre>
 *   - [compact index] vertex (0-based index, number?)
 *   - [compact index] side (side of what?)
 * </pre>
 */
public class Vert {

	public final int vertex;
	public final int side;

	public Vert(int vertex, int side) {
		this.vertex = vertex;
		this.side = side;
	}

	public Vert(PackageReader reader) {
		this(reader.readIndex(), reader.readIndex());
	}

	@Override
	public String toString() {
		return String.format("[vertex=%s, side=%s]", vertex, side);
	}
}
