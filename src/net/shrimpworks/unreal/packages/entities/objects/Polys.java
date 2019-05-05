package net.shrimpworks.unreal.packages.entities.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class Polys extends Object {

	public final List<Polygon> polys;

	public static class Polygon {

		public final Model.Vector base;
		public final Model.Vector normal;
		public final Model.Vector textureU;
		public final Model.Vector textureV;
		public final List<Model.Vector> vertices;

		public final int polyFlags;
		public final ObjectReference actor;
		public final ObjectReference texture;
		public final Name itemName;
		public final int link;
		public final int brushPoly;

		public final short panU;
		public final short panV;

		public Polygon(Package pkg, PackageReader reader) {
			int vertCount = reader.readIndex();

			reader.ensureRemaining(4 * 12);
			this.base = new Model.Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());
			this.normal = new Model.Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());
			this.textureU = new Model.Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());
			this.textureV = new Model.Vector(reader.readFloat(), reader.readFloat(), reader.readFloat());

			this.vertices = new ArrayList<>(vertCount);
			for (int i = 0; i < vertCount; i++) {
				reader.ensureRemaining(12);
				vertices.add(new Model.Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()));
			}

			reader.ensureRemaining(20);
			this.polyFlags = reader.readInt();
			this.actor = new ObjectReference(pkg, reader.readIndex());
			this.texture = new ObjectReference(pkg, reader.readIndex());
			this.itemName = pkg.names[reader.readIndex()];
			this.link = reader.readIndex();
			this.brushPoly = reader.readIndex();

			this.panU = reader.readShort();
			this.panV = reader.readShort();
		}

		@Override
		public String toString() {
			return String.format("Polygon [base=%s, normal=%s, textureU=%s, textureV=%s, vertices=%s, polyFlags=%s, actor=%s, " +
								 "texture=%s, itemName=%s, link=%s, brushPoly=%s, panU=%s, panV=%s]",
								 base, normal, textureU, textureV, vertices, polyFlags, actor,
								 texture, itemName, link, brushPoly, panU, panV);
		}
	}

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
