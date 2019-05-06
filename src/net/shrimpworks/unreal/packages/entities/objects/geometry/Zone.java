package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

/**
 * Zone information.
 * <p>
 * Data format:
 * <pre>
 *   - [compact int] (object reference) zoneinfo actor
 *   - [long] connectivity (?)
 *   - [long] visibility (?)
 * </pre>
 *
 * In addition, there may be an additional field depending on package version:
 *
 * - For versions < 63: [float] last render time
 * - For versions >= 117: [int] unknown value
 */
public class Zone {

	public final ObjectReference zoneActor;
	public final long connectivity;
	public final long visibility;

	public Zone(ObjectReference zoneActor, long connectivity, long visibility) {
		this.zoneActor = zoneActor;
		this.connectivity = connectivity;
		this.visibility = visibility;
	}

	public Zone(Package pkg, PackageReader reader) {
		this(new ObjectReference(pkg, reader.readIndex()), reader.readLong(), reader.readLong());
	}

	@Override
	public String toString() {
		return String.format("[zoneActor=%s, connectivity=%s, visibility=%s]", zoneActor, connectivity, visibility);
	}
}
