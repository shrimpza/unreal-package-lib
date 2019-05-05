package net.shrimpworks.unreal.packages.entities.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Polygon;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * A collection of polygon information contained in objects of class Polys.
 *
 * Data format:
 * <pre>
 *   - [int] polygon count
 *   - [int] polygon count (again...?)
 *   - [[Polygon, ...]] polygons, repeated for polygon count
 * </pre>
 */
public class Polys extends Object {

	public final List<Polygon> polys;

	public Polys(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);

		reader.moveTo(dataStart);
		reader.ensureRemaining(32);

		int polyCount = reader.readInt();

		assert polyCount == reader.readInt(); // it appears as though there are two polycount values at the head of the file

		this.polys = new ArrayList<>(polyCount);
		for (int i = 0; i < polyCount; i++) {
			polys.add(new Polygon(pkg, reader));
		}
	}

	@Override
	public String toString() {
		return String.format("Polys [polys=%s, export=%s, properties=%s]", polys, export, properties);
	}
}
