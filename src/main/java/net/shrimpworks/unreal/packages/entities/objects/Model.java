package net.shrimpworks.unreal.packages.entities.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Bound;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Leaf;
import net.shrimpworks.unreal.packages.entities.objects.geometry.LightMap;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Node;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Sphere;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Surface;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Vector;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Vert;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Zone;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * Model class object representation.
 * <p>
 * Data format (refer to individual geometry types for their data formats):
 * <pre>
 *   - [Bound] bounding box
 *   - [Sphere] bounding sphere
 *   - [compact int] vector count
 *   - [[Vector, ...]] vectors
 *   - [compact int] point count
 *   - [[Vector, ...]] points
 *   - [compact int] node count
 *   - [[Node, ...]] nodes
 *   - [compact int] surface count
 *   - [[Surface, ...]] surfaces
 *   - [compact int] vert count
 *   - [[Vert, ...]] verts
 *   - [int] number of shared sides
 *   - [int] zone count
 *   - [[Zone, ...]] zones
 *   - [compact int] (object reference] ref to Polys object
 *   - [compact int] lightmap count
 *   - [[LightMap, ...]] lightmaps
 *   - [compact int] lightbit count (what's a lightbit?)
 *   - [[byte, ...]] lightbits
 *   - [compact int] bound count
 *   - [[Bound, ...]] bounds
 *   - [compact int] leaf hulls count (what's a leaf hull?!)
 *   - [[Int, ...]] leaf hulls
 *   - [compact int] leaves count
 *   - [[Leaf, ...]] leaves
 *   - [compact int] lights count
 *   - [[compact int, ...]] (object references) refs to Light objects
 *   - [int] root outside (??)
 *   - [int] linked (??)
 * </pre>
 */
public class Model extends Object {

	public final Bound boundingBox;
	public final Sphere boundingSphere;

	public final List<Vector> vectors;
	public final List<Vector> points;
	public final List<Node> nodes;
	public final List<Surface> surfaces;
	public final List<Vert> verts;

	public final int numSharedSides;

	public final List<Zone> zones;

	public final ObjectReference polys;

	public final List<LightMap> lightMaps;
	public final List<Byte> lightBits;
	public final List<Bound> bounds;
	public final List<Integer> leafHulls;
	public final List<Leaf> leaves;
	public final List<ObjectReference> lights;

	public final int rootOutside;
	public final int linked;

	public Model(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);

		reader.moveTo(dataStart);
		reader.ensureRemaining(64);

		this.boundingBox = new Bound(reader);
		this.boundingSphere = new Sphere(reader);

		int vectorCount = reader.readIndex();
		this.vectors = new ArrayList<>(vectorCount);
		for (int i = 0; i < vectorCount; i++) {
			reader.ensureRemaining(16);
			vectors.add(new Vector(reader));
		}

		int pointCount = reader.readIndex();
		this.points = new ArrayList<>(pointCount);
		for (int i = 0; i < pointCount; i++) {
			reader.ensureRemaining(16);
			points.add(new Vector(reader));
		}

		int nodeCount = reader.readIndex();
		this.nodes = new ArrayList<>(nodeCount);
		for (int i = 0; i < nodeCount; i++) {
			reader.ensureRemaining(64);
			if (pkg.version < 117) nodes.add(new Node(reader));
//			else {
				// XXX unknown data structures for UE2
//				new Plane(reader);
//				reader.readLong();
//				reader.readInt(); reader.readInt();
//				reader.readFloat();reader.readFloat(); reader.readFloat();reader.readFloat();
//				reader.readInt(); reader.readInt();	reader.readInt(); reader.readInt();
//				reader.readShort();
//				reader.readByte();
//				reader.readLong(); reader.readLong();
//				reader.readInt();
//			}
		}

		int surfCount = reader.readIndex();
		this.surfaces = new ArrayList<>(surfCount);
		for (int i = 0; i < surfCount; i++) {
			reader.ensureRemaining(32);
			if (pkg.version < 117) surfaces.add(new Surface(pkg, reader));
//			else {
				// XXX unknown data structures for UE2
//				reader.readIndex();
//				reader.readInt();
//				reader.readIndex();reader.readIndex();reader.readIndex();reader.readIndex();reader.readIndex();
//				reader.readIndex();
//				reader.readFloat();reader.readFloat();reader.readFloat();reader.readFloat();reader.readFloat();
//				Texture INDEX
//				PolyFlags DWORD
//				5x INDEX (pBase, vNormal, vTextureU, vTextureV, iLightMap ?)
//				iBrushPoly INDEX
//				5x FLOAT
//			}
		}

		int vertCount = reader.readIndex();
		this.verts = new ArrayList<>(vertCount);
		for (int i = 0; i < vertCount; i++) {
			reader.ensureRemaining(8);
			verts.add(new Vert(reader));
		}

		this.numSharedSides = reader.readInt();

		int zoneCount = reader.readInt();
		this.zones = new ArrayList<>(zoneCount);
		for (int i = 0; i < zoneCount; i++) {
			reader.ensureRemaining(32);
			zones.add(new Zone(pkg, reader));

			// extraneous data
			if (pkg.version < 63) reader.readFloat(); // last render time (?)
			if (pkg.version >= 117) reader.readInt(); // unknown
		}

		this.polys = new ObjectReference(pkg, reader.readIndex());

		int lightMapCount = reader.readIndex();
		this.lightMaps = new ArrayList<>(lightMapCount);
		for (int i = 0; i < lightMapCount; i++) {
			reader.ensureRemaining(32);
			lightMaps.add(new LightMap(pkg, reader));
		}

		int lightBitCount = reader.readIndex();
		this.lightBits = new ArrayList<>(lightBitCount);
		for (int i = 0; i < lightBitCount; i++) {
			reader.ensureRemaining(1);
			lightBits.add(reader.readByte());
		}

		int boundCount = reader.readIndex();
		this.bounds = new ArrayList<>(boundCount);
		for (int i = 0; i < boundCount; i++) {
			reader.ensureRemaining(25);
			bounds.add(new Bound(reader));
		}

		int leafHullsCount = reader.readIndex();
		this.leafHulls = new ArrayList<>(leafHullsCount);
		for (int i = 0; i < leafHullsCount; i++) {
			reader.ensureRemaining(4);
			leafHulls.add(reader.readInt());
		}

		int leavesCount = reader.readIndex();
		this.leaves = new ArrayList<>(leavesCount);
		for (int i = 0; i < leavesCount; i++) {
			reader.ensureRemaining(32);
			leaves.add(new Leaf(pkg, reader));
		}

		int lightsCount = reader.readIndex();
		this.lights = new ArrayList<>(lightsCount);
		for (int i = 0; i < lightsCount; i++) {
			reader.ensureRemaining(2);
			lights.add(new ObjectReference(pkg, reader.readIndex()));
		}

		reader.ensureRemaining(8);

		this.rootOutside = reader.readInt();
		this.linked = reader.readInt();
	}

	@Override
	public String toString() {
		return String.format(
				"Model [boundingBox=%s, boundingSphere=%s, vectors=%s, points=%s, nodes=%s, " +
				"surfaces=%s, export=%s, properties=%s]",
				boundingBox, boundingSphere, vectors, points, nodes, surfaces, export, properties);
	}
}
