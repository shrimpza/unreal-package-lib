package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

/**
 * A leaf (?).
 * <p>
 * Data format:
 * <pre>
 *   - [compact int] (object reference) zone
 *   - [compact int] permeating (?)
 *   - [compact int] volumetric (?)
 *   - [long] visible zones (?)
 * </pre>
 */
public class Leaf {

	public final ObjectReference zone;
	public final int permeating;
	public final int volumetric;
	public final long visibleZones;

	public Leaf(ObjectReference zone, int permeating, int volumetric, long visibleZones) {
		this.zone = zone;
		this.permeating = permeating;
		this.volumetric = volumetric;
		this.visibleZones = visibleZones;
	}

	public Leaf(Package pkg, PackageReader reader) {
		this(new ObjectReference(pkg, reader.readIndex()), reader.readIndex(), reader.readIndex(), reader.readLong());
	}

	@Override
	public String toString() {
		return String.format("[zone=%s, permeating=%s, volumetric=%s, visibleZones=%s]", zone, permeating, volumetric, visibleZones);
	}
}
