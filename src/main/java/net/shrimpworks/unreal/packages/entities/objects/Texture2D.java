package net.shrimpworks.unreal.packages.entities.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.compression.ChunkChannel;
import net.shrimpworks.unreal.packages.compression.CompressedChunk;
import net.shrimpworks.unreal.packages.compression.CompressionFormat;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * A texture object from Unreal Engine 3.
 * <p>
 * Able to read texture data from LZO compressed chunks.
 */
public class Texture2D extends TextureBase<Texture2D.MipMap> {

	public enum BulkDataFlags {
		StoredInSeparateFile(0x00000001),
		StoredAsSeparateData(0x00000040),
		EmptyData(0x00000020),
		CompressedZlib(0x00000002),
		CompressedLzo(0x00000010),
		CompressedLzx(0x00000080);

		private final int flag;

		BulkDataFlags(int flag) {
			this.flag = flag;
		}

		static Set<BulkDataFlags> fromFlags(long flags) {
			Set<BulkDataFlags> dataFlags = EnumSet.noneOf(BulkDataFlags.class);
			dataFlags.addAll(Arrays.stream(values()).filter(f -> (flags & f.flag) == f.flag).collect(Collectors.toSet()));
			return dataFlags;
		}
	}

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
			int fullDataSize = reader.readInt(); // the actual uncompressed data size, if compressed
			int savedBulkDataSizeOnDisk = reader.readInt();
			int savedBulkDataOffsetInFile = reader.readInt();

			reader.moveTo(savedBulkDataOffsetInFile + savedBulkDataSizeOnDisk);

			int width = reader.readInt();
			int height = reader.readInt();

			mips[i] = new MipMap(BulkDataFlags.fromFlags(bulkDataFlags), savedBulkDataOffsetInFile, savedBulkDataSizeOnDisk,
								 fullDataSize, width, height);
		}

		return mips;
	}

	@Override
	protected byte[] readImage(MipMap mip) {
		boolean lzo = mip.bulkDataFlags.contains(BulkDataFlags.CompressedLzo);

		if (lzo) {
			CompressedChunk chunk = new CompressedChunk(CompressionFormat.LZO, 0, mip.dataSize, mip.offset, mip.size);
			ChunkChannel chunkChannel = reader.loadChunk(chunk);
			byte[] data = new byte[mip.dataSize];
			ByteBuffer buf = ByteBuffer.allocate(mip.dataSize);
			try {
				chunkChannel.read(buf);
				buf.flip();
				buf.get(data);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read compressed image data", e);
			}

			return data;
		} else {
			byte[] data = new byte[mip.size];
			reader.moveTo(mip.offset);

			int pos = 0;

			while (pos < mip.size) {
				pos += reader.readBytes(data, pos, mip.size - pos);
				reader.fillBuffer();
			}

			return data;
		}
	}

	public class MipMap extends MipMapBase {

		public final Set<BulkDataFlags> bulkDataFlags;
		public final int offset;
		public final int size;
		public final int dataSize;

		private MipMap(Set<BulkDataFlags> bulkDataFlags, int offset, int size, int dataSize, int width, int height) {
			super(width, height);
			this.bulkDataFlags = bulkDataFlags;
			this.offset = offset;
			this.size = size;
			this.dataSize = dataSize;
		}

		@Override
		public String toString() {
			return String.format("Texture2D.MipMap [size=%s, dataSize=%s, width=%s, height=%s]", size, dataSize, width, height);
		}
	}
}
