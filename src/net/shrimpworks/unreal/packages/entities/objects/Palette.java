package net.shrimpworks.unreal.packages.entities.objects;

import java.awt.image.IndexColorModel;
import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;


public class Palette extends Object {

	public Palette(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public IndexColorModel colorModel() {
		reader.moveTo(dataStart);

		int size = reader.readIndex();

		byte[] r = new byte[size];
		byte[] g = new byte[size];
		byte[] b = new byte[size];
		byte[] a = new byte[size];

		for (int i = 0; i < size; i++) {
			r[i] = reader.readByte();
			g[i] = reader.readByte();
			b[i] = reader.readByte();
			a[i] = reader.readByte();
		}

		return new IndexColorModel(8, size, r, g, b, a);
	}
}
