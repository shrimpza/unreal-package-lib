package net.shrimpworks.unreal.packages.entities.objects.geometry;

import java.util.Arrays;

import net.shrimpworks.unreal.packages.PackageReader;

/**
 * A Node. I'm not sure what this is actually for.
 * <p>
 * Data format:
 * <pre>
 *   - [Plane] plane
 *   - [long] node mask
 *   - [byte] node flags
 *   - [compact int] vert(ex?) pool
 *   - [compact int] surf(ace?)
 *   - [compact int] front
 *   - [compact int] back
 *   - [compact int] plane (?)
 *   - [compact int] collision bound
 *   - [compact int] render bound
 *   - [[byte, byte]] zone
 *   - [byte] num vertices
 *   - [[int, int]] leaf (?)
 * </pre>
 */
public class Node {

	public final Plane plane;
	public final long nodeMask;
	public final byte nodeFlags;

	public final int vertPool;
	public final int surf;
	public final int front;
	public final int back;
	public final int iPlane;
	public final int collisionBound;
	public final int renderBound;
	public final byte[] zone;
	public final byte numVertices;
	public final int[] leaf;

	public Node(
			Plane plane, long nodeMask, byte nodeFlags, int vertPool, int surf, int front, int back, int iPlane,
			int collisionBound, int renderBound, byte[] zone, byte numVertices, int[] leaf) {
		this.plane = plane;
		this.nodeMask = nodeMask;
		this.nodeFlags = nodeFlags;
		this.vertPool = vertPool;
		this.surf = surf;
		this.front = front;
		this.back = back;
		this.iPlane = iPlane;
		this.collisionBound = collisionBound;
		this.renderBound = renderBound;
		this.zone = zone;
		this.numVertices = numVertices;
		this.leaf = leaf;
	}

	public Node(PackageReader reader) {
		this(new Plane(reader),
			 reader.readLong(), reader.readByte(),
			 reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(),
			 reader.readIndex(),
			 new byte[] { reader.readByte(), reader.readByte() }, reader.readByte(), new int[] { reader.readInt(), reader.readInt() }
		);
	}

	@Override
	public String toString() {
		return String.format("[plane=%s, nodeMask=%s, nodeFlags=%s, vertPool=%s, surf=%s, front=%s, back=%s, iPlane=%s, " +
							 "collisionBound=%s, renderBound=%s, zone=%s, numVertices=%s, leaf=%s]",
							 plane, nodeMask, nodeFlags, vertPool, surf, front, back, iPlane,
							 collisionBound, renderBound, Arrays.toString(zone), numVertices, Arrays.toString(leaf));
	}
}
