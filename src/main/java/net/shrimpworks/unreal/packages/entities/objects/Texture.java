package net.shrimpworks.unreal.packages.entities.objects;

import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * A texture object from Unreal Engine 1 and 2.
 */
public class Texture extends TextureBase<Texture.MipMap> {

	public Texture(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public MipMap[] mipMaps() {
		reader.moveTo(dataStart);

		int mipCount = pkg.version >= 178 ? reader.readInt() : reader.readByte();

		MipMap[] mips = new MipMap[mipCount];

		for (int i = 0; i < mipCount; i++) {

			int widthOffset = 0;
			if (pkg.version >= 63) {
				widthOffset = reader.readInt();
			}

			int size = reader.readIndex();

			// infer widthOffset for older versions; probably not even needed :|
			if (pkg.version < 63) {
				widthOffset = reader.currentPosition() + size;
			}

			// skip over the image content - mipmap class can return it on demand
			reader.moveRelative(size);

			int width = reader.readInt();
			int height = reader.readInt();

			byte bitWidth = reader.readByte();
			byte bitHeight = reader.readByte();

			mips[i] = new MipMap(widthOffset, size, width, height, bitWidth, bitHeight);
		}

		return mips;
	}

	@Override
	protected byte[] readImage(MipMap mip) {
		if (mip == null) throw new IllegalArgumentException("MipMap must be non-null must be provided");
		if (mip.size <= 0) throw new IllegalArgumentException("MipMap size must be greater than zero");
		if (mip.widthOffset <= 0) throw new IllegalArgumentException("MipMap offset must be greater than zero");
		if (mip.width <= 0 || mip.height <= 0) throw new IllegalArgumentException("MipMap must have a width and height");

		reader.moveTo(mip.widthOffset - mip.size);
		byte[] data = new byte[mip.size];
		int pos = 0;

		while (pos < mip.size) {
			pos += reader.readBytes(data, pos, mip.size - pos);
			reader.fillBuffer();
		}

		return data;
	}

	public class MipMap extends MipMapBase {

		public final int widthOffset;
		public final int size;
		private final byte bitsWidth;
		private final byte bitsHeight;

		private MipMap(int widthOffset, int size, int width, int height, byte bitsWidth, byte bitsHeight) {
			super(width, height);
			this.widthOffset = widthOffset;
			this.size = size;
			this.bitsWidth = bitsWidth;
			this.bitsHeight = bitsHeight;
		}

		@Override
		public String toString() {
			return String.format("MipMap [size=%s, width=%s, height=%s, bitsWidth=%s, bitsHeight=%s]",
								 size, width, height, bitsWidth, bitsHeight);
		}

	}
}
