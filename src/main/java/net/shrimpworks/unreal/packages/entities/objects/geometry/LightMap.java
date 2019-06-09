package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;

/**
 * A light map value.
 * <p>
 * Data format:
 * <pre>
 *   - [int] data offset (offset within the package?)
 *   - [Vector] pan
 *   - [compact int] u clamp
 *   - [compact int] v clamp
 *   - [float] u scale
 *   - [float] v scale
 *   - [int] light actors
 * </pre>
 */
public class LightMap {

	public final int dataOffset;
	public final Vector pan;
	public final int uClamp;
	public final int vClamp;
	public final float uScale;
	public final float vScale;
	public final int lightActors;

	public LightMap(int dataOffset, Vector pan, int uClamp, int vClamp, float uScale, float vScale, int lightActors) {
		this.dataOffset = dataOffset;
		this.pan = pan;
		this.uClamp = uClamp;
		this.vClamp = vClamp;
		this.uScale = uScale;
		this.vScale = vScale;
		this.lightActors = lightActors;
	}

	public LightMap(Package pkg, PackageReader reader) {
		this(reader.readInt(),
			 new Vector(reader),
			 pkg.version < 117 ? reader.readIndex() : 0, pkg.version < 117 ? reader.readIndex() : 0,
			 reader.readFloat(), reader.readFloat(),
			 reader.readInt());
	}

	@Override
	public String toString() {
		return String.format("[dataOffset=%s, pan=%s, uClamp=%s, vClamp=%s, uScale=%s, vScale=%s, lightActors=%s]",
							 dataOffset, pan, uClamp, vClamp, uScale, vScale, lightActors);
	}
}
