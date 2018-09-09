package net.shrimpworks.unreal.archive;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public interface Objects {

	enum ObjectType {
		Texture(Objects.Texture.class),
		Palette(Objects.Palette.class);

		private final Class<? extends Object> clazz;

		ObjectType(Class<? extends Object> clazz) {
			this.clazz = clazz;
		}

		/**
		 * Magically create a new typed instance of an object for the provided
		 * export, if a specific type exists and is defined in the
		 * {@link ObjectType} enum.
		 *
		 * @param pkg        package
		 * @param export     exported entity
		 * @param header     object header
		 * @param properties properties read for object
		 * @param dataStart  position of object payload in package
		 * @return a new object
		 */
		public static Object newInstance(
				Package pkg, Entities.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {

			Entities.Named type = export.objClass.get();

			try {
				ObjectType objectType = ObjectType.valueOf(type.name().name);
				Constructor<? extends Object> constructor = objectType.clazz.getConstructor(Package.class, Entities.Export.class,
																							ObjectHeader.class, Collection.class,
																							int.class);
				return constructor.newInstance(pkg, export, header, properties, dataStart);
			} catch (IllegalArgumentException | NoSuchMethodException
					| IllegalAccessException | InstantiationException | InvocationTargetException e) {
				return new Object(pkg, export, header, properties, dataStart);
			}
		}
	}

	class ObjectHeader {

		private final int node;
		private final int stateNode;
		private final long probeMask;
		private final int latentAction;
		private final int offset;

		public ObjectHeader(int node, int stateNode, long probeMask, int latentAction, int offset) {
			this.node = node;
			this.stateNode = stateNode;
			this.probeMask = probeMask;
			this.latentAction = latentAction;
			this.offset = offset;
		}

		@Override
		public String toString() {
			return String.format("UnrealObjectHeader [node=%s, stateNode=%s, probeMask=%s, latentAction=%s, offset=%s]",
								 node, stateNode, probeMask, latentAction, offset);
		}
	}

	class Object {

		final Package pkg;
		public final Entities.Export export;
		public final ObjectHeader header;
		public final Collection<Properties.Property> properties;
		final int dataStart;

		public Object(
				Package pkg, Entities.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {
			this.pkg = pkg;
			this.export = export;
			this.header = header;
			this.properties = properties;
			this.dataStart = dataStart;
		}

		public Properties.Property property(String propertyName) {
			for (Properties.Property p : properties) {
				if (p.name.name.equals(propertyName)) {
					return p;
				}
			}
			return null;
		}

		public String className() {
			return export.objClass.get().name().name;
		}

		@Override
		public String toString() {
			return String.format("UnrealObject [export=%s, header=%s, properties=%s]", export, header, properties);
		}
	}

	class Texture extends Object {

		public enum Format {
			PALETTE_8_BIT,
			RGBA7,
			RGB16,
			DXT1,
			RBG8,
			RGBA8
		}

		public Texture(
				Package pkg, Entities.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {
			super(pkg, export, header, properties, dataStart);
		}

		public MipMap[] mipMaps() {
			pkg.moveTo(dataStart);

			int mipCount = pkg.readByte();

			MipMap[] mips = new MipMap[mipCount];

			for (int i = 0; i < mipCount; i++) {

				int widthOffset = 0;
				if (pkg.version >= 63) {
					widthOffset = pkg.readInt();
				}

				int size = pkg.readIndex();

				// infer widthOffset for older versions; probably not even needed :|
				if (pkg.version < 63) {
					widthOffset = pkg.currentPosition() + size;
				}

				// skip over the image content - mipmap class can return it on demand
				pkg.moveRelative(size);

				int width = pkg.readInt();
				int height = pkg.readInt();

				byte bitWidth = pkg.readByte();
				byte bitHeight = pkg.readByte();

				mips[i] = new MipMap(this, widthOffset, size, width, height, bitWidth, bitHeight);
			}

			return mips;
		}

		public Format format() {
			Properties.Property prop = property("Format");
			if (prop instanceof Properties.ByteProperty) {
				return Format.values()[((Properties.ByteProperty)prop).value];
			}

			return Format.PALETTE_8_BIT;
		}

		public Palette palette() {
			Properties.Property prop = property("Palette");
			if (prop instanceof Properties.ObjectProperty) {
				Entities.Named exp = ((Properties.ObjectProperty)prop).value.get();
				if (exp instanceof Entities.ExportedObject) {
					Object pallete = ((Entities.ExportedObject)exp).object();
					if (pallete instanceof Palette) return (Palette)pallete;
				}
			}

			return null;
		}

		private byte[] readImage(MipMap mip) {
			pkg.moveTo(mip.widthOffset - mip.size);
			byte[] data = new byte[mip.size];
			int pos = 0;

			while (pos < mip.size) {
				pos += pkg.readBytes(data, pos, mip.size - pos);
				pkg.fillBuffer();
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
				// see https://github.com/acmi/l2tool/blob/master/src/main/java/acmi/l2/clientmod/l2tool/img/P8.java
				byte[] data = texture.readImage(this);

				// read texture properties, look for "Format", support P8 or DXT1
				switch (texture.format()) {
					case PALETTE_8_BIT:
						// read Palette from properties
						Palette palette = texture.palette();
						if (palette == null) throw new IllegalStateException("Could not find palette for texture");

						BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, palette.colorModel());
						System.arraycopy(data, 0, ((DataBufferByte)image.getRaster().getDataBuffer()).getData(), 0, data.length);
						return image;
					case DXT1:
					default:
						throw new UnsupportedOperationException("Not implemented");

				}
			}
		}
	}

	class Palette extends Object {

		public Palette(
				Package pkg, Entities.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {
			super(pkg, export, header, properties, dataStart);
		}

		public IndexColorModel colorModel() {
			pkg.moveTo(dataStart);

			int size = pkg.readIndex();

			byte[] r = new byte[size];
			byte[] g = new byte[size];
			byte[] b = new byte[size];
			byte[] a = new byte[size];

			for (int i = 0; i < size; i++) {
				r[i] = pkg.readByte();
				g[i] = pkg.readByte();
				b[i] = pkg.readByte();
				a[i] = pkg.readByte();
			}

			return new IndexColorModel(8, size, r, g, b, a);
		}
	}
}
