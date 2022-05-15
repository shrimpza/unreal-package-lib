package net.shrimpworks.unreal.packages.entities.objects;

import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * A texture object from Unreal Engine 3.
 */
public class Texture2D extends TextureBase<Texture2D.MipMap> {

	public Texture2D(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties,
					 int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public MipMap[] mipMaps() {
		reader.moveTo(dataStart);

		// 3 unknown int values
		reader.moveRelative(4 * 3);
		int absOffset = reader.readInt();

		int mipCount = reader.readInt();

		MipMap[] mips = new MipMap[mipCount];

		for (int i = 0; i < mipCount; i++) {
			int bulkDataFlags = reader.readInt();
			int savedElementCount = reader.readInt();
			int savedBulkDataSizeOnDisk = reader.readInt();
			int savedBulkDataOffsetInFile = reader.readInt();

			reader.moveTo(savedBulkDataOffsetInFile + savedBulkDataSizeOnDisk);

			int width = reader.readInt();
			int height = reader.readInt();

			mips[i] = new MipMap(bulkDataFlags, savedBulkDataOffsetInFile, savedBulkDataSizeOnDisk, width, height);
		}

		return mips;
	}

	@Override
	protected byte[] readImage(MipMap mip) {
		reader.moveTo(mip.offset);
		byte[] data = new byte[mip.size];
		int pos = 0;

		while (pos < mip.size) {
			pos += reader.readBytes(data, pos, mip.size - pos);
			reader.fillBuffer();
		}

		return data;
	}

	public class MipMap extends MipMapBase {

		public final int bulkDataFlags;
		public final int offset;
		public final int size;

		private MipMap(int bulkDataFlags, int offset, int size, int width, int height) {
			super(width, height);
			this.bulkDataFlags = bulkDataFlags;
			this.offset = offset;
			this.size = size;
		}

		@Override
		public String toString() {
			return String.format("Texture2D.MipMap [size=%s, width=%s, height=%s]", size, width, height);
		}
	}
}
