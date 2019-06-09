package net.shrimpworks.unreal.packages.entities.objects.geometry;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

/**
 * A Surf[ace?] representation.
 * <p>
 * Data format:
 * <pre>
 *   - [compact int] (object reference) texture
 *   - [int] poly flags
 *   - [compact int] pBase
 *   - [compact int] normal
 *   - [compact int] texture u
 *   - [compact int] texture v
 *   - [compact int] (object reference) light map
 *   - [compact int] (object reference) brush poly
 *   - [short] pan u
 *   - [short] pan v
 *   - [compact int] (object reference) actor
 * </pre>
 */
public class Surface {

	public final ObjectReference texture;
	public final int polyFlags;
	public final int pBase;
	public final int normal;
	public final int textureU;
	public final int textureV;
	public final ObjectReference lightMap;
	public final ObjectReference brushPoly;
	public final short panU;
	public final short panV;
	public final ObjectReference actor;

	public Surface(
			ObjectReference texture, int polyFlags, int pBase, int normal, int textureU, int textureV, ObjectReference lightMap,
			ObjectReference brushPoly, short panU, short panV, ObjectReference actor) {
		this.texture = texture;
		this.polyFlags = polyFlags;
		this.pBase = pBase;
		this.normal = normal;
		this.textureU = textureU;
		this.textureV = textureV;
		this.lightMap = lightMap;
		this.brushPoly = brushPoly;
		this.panU = panU;
		this.panV = panV;
		this.actor = actor;
	}

	public Surface(Package pkg, PackageReader reader) {
		this(new ObjectReference(pkg, reader.readIndex()),
			 reader.readInt(), reader.readIndex(), reader.readIndex(), reader.readIndex(), reader.readIndex(),
			 new ObjectReference(pkg, reader.readIndex()), new ObjectReference(pkg, reader.readIndex()),
			 reader.readShort(), reader.readShort(),
			 new ObjectReference(pkg, reader.readIndex())
		);
	}

	@Override
	public String toString() {
		return String.format("[texture=%s, polyFlags=%s, pBase=%s, normal=%s, textureU=%s, textureV=%s, lightMap=%s, brushPoly=%s, " +
							 "panU=%s, panV=%s, actor=%s]",
							 texture, polyFlags, pBase, normal, textureU, textureV, lightMap, brushPoly,
							 panU, panV, actor);
	}
}
