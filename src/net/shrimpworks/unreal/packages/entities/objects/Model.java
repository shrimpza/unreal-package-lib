package net.shrimpworks.unreal.packages.entities.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class Model extends Object {

	public final Vector boundingBoxMin;
	public final Vector boundingBoxMax;
	public final boolean boundingBoxValid;
	public final Sphere boundingSphere;

	public final List<Vector> vectors;

	public static class Vector {

		public final float x;
		public final float y;
		public final float z;

		private Vector(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public String toString() {
			return String.format("[x=%.4f, y=%.4f, z=%.4f]", x, y, z);
		}
	}

	public static class Sphere {

		public final float x;
		public final float y;
		public final float z;
		public final float radius;

		private Sphere(float x, float y, float z, float radius) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
		}

		@Override
		public String toString() {
			return String.format("[x=%.4f, y=%.4f, z=%.4f, radius=%.4f]", x, y, z, radius);
		}
	}

	public Model(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);

		reader.moveTo(dataStart);
		this.boundingBoxMin = new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());
		this.boundingBoxMax = new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());
		this.boundingBoxValid = reader.readByte() > 0;
		this.boundingSphere = new Sphere(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat());

		int vectorCount = reader.readInt();
		this.vectors = new ArrayList<>(vectorCount);
		for (int i = 0; i < vectorCount; i++) {
			vectors.add(new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()));
		}
	}

	@Override
	public String toString() {
		return String.format(
				"Model [boundingBoxMin=%s, boundingBoxMax=%s, boundingBoxValid=%s, boundingSphere=%s, vectors=%s, export=%s, properties=%s]",
				boundingBoxMin, boundingBoxMax, boundingBoxValid, boundingSphere, vectors, export, properties);
	}
}
