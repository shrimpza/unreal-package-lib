package net.shrimpworks.unreal.packages.entities.objects.dxt;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class DXTParams {

	final int blockSize;

	final int numTilesWide;
	final int numTilesHigh;

	final int[] pixels;

	final ByteBuffer buffer;
	final BufferedImage image;

	public DXTParams(byte[] data, int width, int height, int imageType, int blockSize) {
		if (width < blockSize || height < blockSize) {
			throw new IllegalArgumentException("Invalid image size");
		}

		this.blockSize = blockSize;
		this.numTilesWide = width / blockSize;
		this.numTilesHigh = height / blockSize;

		this.buffer = ByteBuffer.wrap(data);
		if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		}

		this.pixels = new int[blockSize * width];
		this.image = new BufferedImage(width, height, imageType);
	}

}
