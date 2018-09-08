package net.shrimpworks.unreal.archive;

import java.awt.image.BufferedImage;
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
				int widthOffset = pkg.readInt();
				int size = pkg.readIndex();

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
				// TODO
				return null;
			}
		}
	}
}
