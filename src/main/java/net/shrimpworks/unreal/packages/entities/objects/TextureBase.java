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
import net.shrimpworks.unreal.packages.entities.objects.TextureBase.MipMapBase;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT1Decompressor;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT3Decompressor;
import net.shrimpworks.unreal.packages.entities.objects.dxt.DXT5Decompressor;
import net.shrimpworks.unreal.packages.entities.properties.ByteProperty;
import net.shrimpworks.unreal.packages.entities.properties.EnumProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * Base type with some shared functionality between all supported engine versions.
 */
public abstract class TextureBase<T extends MipMapBase> extends Object {

	public enum Format {
		PALETTE_8_BIT("PF_Unsupported"),
		RGBA7("PF_Unsupported"),
		RGB16("PF_Unsupported"),
		DXT1("PF_DXT1"),
		RBG8("PF_G8"),
		RGBA8("PF_R8G8B8A8"),
		UNKNOWN("PF_Unknown"), // dunno what this might be
		DXT3("PF_DXT3"),
		DXT5("PF_DXT5"),
		L8("PF_L8"),
		G16("PF_G16");

		private final String ue3Name;

		Format(String ue3Name) {
			this.ue3Name = ue3Name;
		}

		public static Format forFormatName(String name) {
			for (Format value : values()) {
				if (value.ue3Name.equalsIgnoreCase(name)) return value;
			}
			return UNKNOWN;
		}
	}

	public TextureBase(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties,
					   int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public Format format() {
		Property prop = property("Format");
		if (prop instanceof ByteProperty) {
			return Format.values()[((ByteProperty)prop).value];
		} else if (prop instanceof EnumProperty) {
			return Format.forFormatName(((EnumProperty)prop).value.name);
		}

		return Format.PALETTE_8_BIT;
	}

	/**
	 * Note: may not be available in UE3, have not yet seen 8-bit indexed textures.
	 */
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

	protected abstract byte[] readImage(T mip);

	protected abstract class MipMapBase {

		public final int width;
		public final int height;

		protected MipMapBase(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public BufferedImage get() {
			@SuppressWarnings("unchecked")
			byte[] data = TextureBase.this.readImage((T)this);

			// read texture properties, look for "Format", support P8 or DXT1
			switch (TextureBase.this.format()) {
				case PALETTE_8_BIT:
					// see https://github.com/acmi/l2tool/blob/master/src/main/java/acmi/l2/clientmod/l2tool/img/P8.java

					// read Palette from properties
					Palette palette = TextureBase.this.palette();
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
						dest[i + 0] = (byte)255; //data[i + 3]; // FIXME something's missing, fallback to no alpha :(
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
					throw new UnsupportedOperationException("Reading texture " + TextureBase.this.export.name.name
															+ " in format " + TextureBase.this.format() + " not supported");
			}
		}
	}
}
