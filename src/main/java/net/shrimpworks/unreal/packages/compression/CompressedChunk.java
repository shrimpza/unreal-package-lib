package net.shrimpworks.unreal.packages.compression;

public class CompressedChunk {

	public final CompressionFormat compressionFormat;
	public final int uncompressedOffset;
	public final int uncompressedSize;
	public final int compressedOffset;
	public final int compressedSize;

	public CompressedChunk(CompressionFormat compressionFormat, int uncompressedOffset,
						   int uncompressedSize, int compressedOffset, int compressedSize) {
		this.compressionFormat = compressionFormat;
		this.uncompressedOffset = uncompressedOffset;
		this.uncompressedSize = uncompressedSize;
		this.compressedOffset = compressedOffset;
		this.compressedSize = compressedSize;
	}

	@Override
	public String toString() {
		return String.format("CompressedChunk [uncompressedOffset=%s, uncompressedSize=%s, compressedOffset=%s, compressedSize=%s]",
							 uncompressedOffset, uncompressedSize, compressedOffset, compressedSize);
	}
}
