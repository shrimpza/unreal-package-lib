package net.shrimpworks.unreal.archive;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Collection;

public interface Objects {

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
		public final Package.Export export;
		public final ObjectHeader header;
		public final Collection<Properties.Property> properties;
		final int dataStart;

		public Object(
				Package pkg, Package.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {
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

		@Override
		public String toString() {
			return String.format("UnrealObject [export=%s, header=%s, properties=%s]", export, header, properties);
		}
	}

	class TextureObject extends Object {

		public TextureObject(
				Package pkg, Package.Export export, ObjectHeader header, Collection<Properties.Property> properties, int dataStart) {
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

			private final TextureObject texture;
			private final int widthOffset;
			public final int size;
			public final int width;
			public final int height;
			private final byte bitsWidth;
			private final byte bitsHeight;

			private MipMap(TextureObject texture, int widthOffset, int size, int width, int height, byte bitsWidth, byte bitsHeight) {
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
				// FIXME read texture properties, look for "Format", support P8 or DXT1
				// FIXME read Pallet from properties
				// see https://github.com/acmi/l2tool/blob/master/src/main/java/acmi/l2/clientmod/l2tool/img/P8.java
				byte[] data = texture.readImage(this);
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
				System.arraycopy(data, 0, ((DataBufferByte)image.getRaster().getDataBuffer()).getData(), 0, data.length);

				return image;

			}
		}
	}
}
