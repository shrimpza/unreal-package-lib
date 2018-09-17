package net.shrimpworks.unreal.packages.entities.objects;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT5Decompressor;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT1Decompressor;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT3Decompressor;
import net.shrimpworks.unreal.packages.entities.properties.ByteProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class Texture extends Object {

	public enum Format {
		PALETTE_8_BIT,
		RGBA7,
		RGB16,
		DXT1,
		RBG8,
		RGBA8,
		UNKNOWN, // dunno what this might be
		DXT3,
		DXT5,
		L8,
		G16
	}

	public Texture(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public MipMap[] mipMaps() {
		reader.moveTo(dataStart);

		int mipCount = reader.readByte();

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

			mips[i] = new MipMap(this, widthOffset, size, width, height, bitWidth, bitHeight);
		}

		return mips;
	}

	public Format format() {
		Property prop = property("Format");
		if (prop instanceof ByteProperty) {
			return Format.values()[((ByteProperty)prop).value];
		}

		return Format.PALETTE_8_BIT;
	}

	public Palette palette() {
		Property prop = property("Palette");
		if (prop instanceof ObjectProperty) {
			Named exp = ((ObjectProperty)prop).value.get();
			if (exp instanceof ExportedObject) {
				Object pallete = ((ExportedObject)exp).object();
				if (pallete instanceof Palette) return (Palette)pallete;
			}
		}

		return null;
	}

	private byte[] readImage(MipMap mip) {
		reader.moveTo(mip.widthOffset - mip.size);
		byte[] data = new byte[mip.size];
		int pos = 0;

		while (pos < mip.size) {
			pos += reader.readBytes(data, pos, mip.size - pos);
			reader.fillBuffer();
		}

		return data;
	}

	public class MipMap {

		private final Texture texture;
		private final int widthOffset;
		public final int size;
		public final int width;
		public final int height;
		private final byte bitsWidth;
		private final byte bitsHeight;

		private MipMap(Texture texture, int widthOffset, int size, int width, int height, byte bitsWidth, byte bitsHeight) {
			this.texture = texture;
			this.widthOffset = widthOffset;
			this.size = size;
			this.width = width;
			this.height = height;
			this.bitsWidth = bitsWidth;
			this.bitsHeight = bitsHeight;
		}

		@Override
		public String toString() {
			return String.format("MipMap [size=%s, width=%s, height=%s, bitsWidth=%s, bitsHeight=%s]",
								 size, width, height, bitsWidth, bitsHeight);
		}

		public BufferedImage get() {
			byte[] data = texture.readImage(this);

			// read texture properties, look for "Format", support P8 or DXT1
			switch (texture.format()) {
				case PALETTE_8_BIT:
					// see https://github.com/acmi/l2tool/blob/master/src/main/java/acmi/l2/clientmod/l2tool/img/P8.java

					// read Palette from properties
					Palette palette = texture.palette();
					if (palette == null) throw new IllegalStateException("Could not find palette for texture");

					BufferedImage p8Img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, palette.colorModel());
					System.arraycopy(data, 0, ((DataBufferByte)p8Img.getRaster().getDataBuffer()).getData(), 0, data.length);
					return p8Img;
				case G16:
					short[] shortData = new short[data.length / 2];
					ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData);
					BufferedImage g16 = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
					System.arraycopy(shortData, 0, ((DataBufferUShort)g16.getRaster().getDataBuffer()).getData(), 0, shortData.length);
					return g16;
				case RBG8:
				case RGBA8:
					// i don't actually know enough about image processing at the moment to make this better...
					// this actually works for DXT3 as well, but the DXT3Decompressor does not work for it, so keep my hack for now
					BufferedImage rgbImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
					for (int i = 0; i < data.length; i += 4) {
						byte[] dest = ((DataBufferByte)rgbImg.getRaster().getDataBuffer()).getData();
						dest[i + 0] = data[i + 3];
						dest[i + 1] = data[i + 0];
						dest[i + 2] = data[i + 1];
						dest[i + 3] = data[i + 2];
					}
					return rgbImg;
				case DXT1:
					return DXT1Decompressor.decompress(data, width, height);
				case DXT3:
					return DXT3Decompressor.decompress(data, width, height);
				case DXT5:
					return DXT5Decompressor.decompress(data, width, height);
				default:
					throw new UnsupportedOperationException("Reading texture " + texture.export.name.name
															+ " in format " + texture.format() + " not supported");
			}
		}
	}
}
