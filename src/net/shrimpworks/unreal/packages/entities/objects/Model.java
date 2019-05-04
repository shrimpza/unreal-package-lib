package net.shrimpworks.unreal.packages.entities.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class Model extends Object {

	public final Bound boundingBox;
	public final Sphere boundingSphere;

	public final List<Vector> vectors;
	public final List<Vector> points;
	public final List<Node> nodes;
	public final List<Surf> surfs;
	public final List<Vert> verts;

	public final int numSharedSides;
	public final int numZones;

	public final ObjectReference polys;

	public final List<LightMap> lightMaps;
	public final List<Byte> lightBits;
	public final List<Bound> bounds;
	public final List<Integer> leafHulls;
	public final List<ObjectReference> lights;

	public final int rootOutside;
	public final int linked;

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

	public static class Plane extends Vector {

		public final float w;

		public Plane(float x, float y, float z, float w) {
			super(x, y, z);
			this.w = w;
		}

		public String toString() {
			return String.format("[x=%.4f, y=%.4f, z=%.4f, w=%.4f]", x, y, z, w);
		}
	}

	public static class Node {

		public final Plane plane;
		public final long nodeMask;
		public final byte nodeFlags;

		public final int iVertPool;
		public final int iSurf;
		public final int iFront;
		public final int iBack;
		public final int iPlane;
		public final int iCollisionBound;
		public final int iRenderBound;
		public final byte[] iZone;
		public final byte numVertices;
		public final int[] iLeaf;

		public Node(
				Plane plane, long nodeMask, byte nodeFlags, int iVertPool, int iSurf, int iFront, int iBack, int iPlane,
				int iCollisionBound, int iRenderBound, byte[] iZone, byte numVertices, int[] iLeaf) {
			this.plane = plane;
			this.nodeMask = nodeMask;
			this.nodeFlags = nodeFlags;
			this.iVertPool = iVertPool;
			this.iSurf = iSurf;
			this.iFront = iFront;
			this.iBack = iBack;
			this.iPlane = iPlane;
			this.iCollisionBound = iCollisionBound;
			this.iRenderBound = iRenderBound;
			this.iZone = iZone;
			this.numVertices = numVertices;
			this.iLeaf = iLeaf;
		}

		@Override
		public String toString() {
			return String.format("[plane=%s, nodeMask=%s, nodeFlags=%s, iVertPool=%s, iSurf=%s, iFront=%s, iBack=%s, iPlane=%s, " +
								 "iCollisionBound=%s, iRenderBound=%s, iZone=%s, numVertices=%s, iLeaf=%s]",
								 plane, nodeMask, nodeFlags, iVertPool, iSurf, iFront, iBack, iPlane,
								 iCollisionBound, iRenderBound, Arrays.toString(iZone), numVertices, Arrays.toString(iLeaf));
		}
	}

	public static class Surf {

		public final int texture;
		public final int polyFlags;
		public final int pBase;
		public final int vNormal;
		public final int vTextureU;
		public final int vTextureV;
		public final int iLightMap;
		public final int iBrushPoly;
		public final short panU;
		public final short panV;
		public final int actor;

		public Surf(
				int texture, int polyFlags, int pBase, int vNormal, int vTextureU, int vTextureV, int iLightMap, int iBrushPoly,
				short panU, short panV, int actor) {
			this.texture = texture;
			this.polyFlags = polyFlags;
			this.pBase = pBase;
			this.vNormal = vNormal;
			this.vTextureU = vTextureU;
			this.vTextureV = vTextureV;
			this.iLightMap = iLightMap;
			this.iBrushPoly = iBrushPoly;
			this.panU = panU;
			this.panV = panV;
			this.actor = actor;
		}

		@Override
		public String toString() {
			return String.format("[texture=%s, polyFlags=%s, pBase=%s, vNormal=%s, vTextureU=%s, vTextureV=%s, iLightMap=%s, " +
								 "iBrushPoly=%s, panU=%s, panV=%s, actor=%s]",
								 texture, polyFlags, pBase, vNormal, vTextureU, vTextureV, iLightMap,
								 iBrushPoly, panU, panV, actor);
		}
	}

	public static class Vert {

		public final int pVertex;
		public final int iSide;

		public Vert(int pVertex, int iSide) {
			this.pVertex = pVertex;
			this.iSide = iSide;
		}

		@Override
		public String toString() {
			return String.format("[pVertex=%s, iSide=%s]", pVertex, iSide);
		}
	}

	public static class LightMap {

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

		@Override
		public String toString() {
			return String.format("[dataOffset=%s, pan=%s, uClamp=%s, vClamp=%s, uScale=%s, vScale=%s, lightActors=%s]",
								 dataOffset, pan, uClamp, vClamp, uScale, vScale, lightActors);
		}
	}

	public static class Bound {

		public final Vector min;
		public final Vector max;
		public final boolean valid;

		public Bound(Vector min, Vector max, boolean valid) {
			this.min = min;
			this.max = max;
			this.valid = valid;
		}

		@Override
		public String toString() {
			return String.format("[min=%s, max=%s, valid=%s]", min, max, valid);
		}
	}

	public Model(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);

		reader.moveTo(dataStart);
		reader.ensureRemaining(64);

		this.boundingBox = new Bound(
				new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()),
				new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()),
				reader.readByte() > 0
		);

		this.boundingSphere = new Sphere(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat());

		int vectorCount = reader.readIndex();
		this.vectors = new ArrayList<>(vectorCount);
		for (int i = 0; i < vectorCount; i++) {
			reader.ensureRemaining(12);
			vectors.add(new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()));
		}

		int pointCount = reader.readIndex();
		this.points = new ArrayList<>(pointCount);
		for (int i = 0; i < pointCount; i++) {
			reader.ensureRemaining(12);
			points.add(new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()));
		}

		int nodeCount = reader.readIndex();
		this.nodes = new ArrayList<>(nodeCount);
		for (int i = 0; i < nodeCount; i++) {
			reader.ensureRemaining(50);
			nodes.add(new Node(
					new Plane(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat()),
					reader.readLong(), reader.readByte(),
					reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(),
					reader.readIndex(),
					new byte[] { reader.readByte(), reader.readByte() }, reader.readByte(), new int[] { reader.readInt(), reader.readInt() }
			));
		}

		int surfCount = reader.readIndex();
		this.surfs = new ArrayList<>(surfCount);
		for (int i = 0; i < surfCount; i++) {
			reader.ensureRemaining(24);
			surfs.add(new Surf(reader.readIndex(), reader.readInt(), reader.readIndex(), reader.readIndex(),
							   reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(),
							   reader.readShort(), reader.readShort(), reader.readIndex()));
		}

		int vertCount = reader.readIndex();
		this.verts = new ArrayList<>(vertCount);
		for (int i = 0; i < vertCount; i++) {
			reader.ensureRemaining(4);
			verts.add(new Vert(reader.readIndex(), reader.readIndex()));
		}

		this.numSharedSides = reader.readInt();
		this.numZones = reader.readInt(); // FIXME is there supposed to be data before polys?

		this.polys = new ObjectReference(pkg, reader.readIndex());

		int lightmapCount = reader.readIndex();
		this.lightMaps = new ArrayList<>(lightmapCount);
		for (int i = 0; i < lightmapCount; i++) {
			reader.ensureRemaining(32);
			lightMaps.add(new LightMap(reader.readInt(),
									   new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()),
									   reader.readIndex(), reader.readIndex(), reader.readFloat(), reader.readFloat(),
									   reader.readInt()));
		}

		int lightbitCount = reader.readIndex();
		reader.ensureRemaining(lightbitCount);
		this.lightBits = new ArrayList<>(lightbitCount);
		for (int i = 0; i < lightbitCount; i++) {
			lightBits.add(reader.readByte());
		}

		int boundCount = reader.readIndex();
		this.bounds = new ArrayList<>(boundCount);
		for (int i = 0; i < boundCount; i++) {
			reader.ensureRemaining(25);
			bounds.add(new Bound(
					new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()),
					new Vector(reader.readFloat(), reader.readFloat(), reader.readFloat()),
					reader.readByte() > 0)
			);
		}

		int leafhullsCount = reader.readIndex();
		this.leafHulls = new ArrayList<>(leafhullsCount);
		for (int i = 0; i < leafhullsCount; i++) {
			reader.ensureRemaining(4);
			leafHulls.add(reader.readInt());
		}

		int leavesCount = reader.readIndex(); // FIXME find example with leaves - will fail from here if > 0

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
				"surfs=%s, export=%s, properties=%s]",
				boundingBox, boundingSphere, vectors, points, nodes, surfs, export, properties);
	}
}
