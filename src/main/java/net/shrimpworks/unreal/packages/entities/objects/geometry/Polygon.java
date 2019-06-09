package net.shrimpworks.unreal.packages.entities.objects.geometry;

import java.util.ArrayList;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

/**
 * A polygon entry, exists within an array of polygons in objects of class Polys.
 * <p>
 * Data format:
 * <pre>
 *   - [compact int] vertex count
 *   - [Vector] base
 *   - [Vector] normal
 *   - [Vector] texture u
 *   - [Vector] texture v
 *   - [[Vector], ...] vertices, repeated for vertex count
 *   - [int] polygon flags
 *   - [compact int] (object reference) actor
 *   - [compact int] (object reference) texture
 *   - [compact int] (name reference) item name
 *   - [compact int] link
 *   - [compact int] brush polygon (0-based polygon index?)
 *   - [short] pan u
 *   - [short] pan v
 * </pre>
 */
public class Polygon {

	public final Vector base;
	public final Vector normal;
	public final Vector textureU;
	public final Vector textureV;
	public final List<Vector> vertices;

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
		this.base = new Vector(reader);
		this.normal = new Vector(reader);
		this.textureU = new Vector(reader);
		this.textureV = new Vector(reader);

		this.vertices = new ArrayList<>(vertCount);
		for (int i = 0; i < vertCount; i++) {
			reader.ensureRemaining(12);
			vertices.add(new Vector(reader));
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
		return String.format("[base=%s, normal=%s, textureU=%s, textureV=%s, vertices=%s, polyFlags=%s, actor=%s, " +
							 "texture=%s, itemName=%s, link=%s, brushPoly=%s, panU=%s, panV=%s]",
							 base, normal, textureU, textureV, vertices, polyFlags, actor,
							 texture, itemName, link, brushPoly, panU, panV);
	}
}
