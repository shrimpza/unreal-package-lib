/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package net.shrimpworks.unreal.packages.entities.objects.dxt;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DXT1Decompressor {

	private static final int DXT1_BLOCK_SIZE = 4;

	public static BufferedImage decompress(byte[] data, int width, int height) {

		if (width < DXT1_BLOCK_SIZE || height < DXT1_BLOCK_SIZE) {
			throw new IllegalArgumentException("Invalid image size");
		}

		ByteBuffer buffer = ByteBuffer.wrap(data);
		if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		}

		int numTilesWide = width / DXT1_BLOCK_SIZE;
		int numTilesHigh = height / DXT1_BLOCK_SIZE;

		// 8 bit per color RGB packed in to an integer as r8g8b8
		int[] pixels = new int[DXT1_BLOCK_SIZE * width];

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int row = 0; row < numTilesHigh; row++) {
			for (int col = 0; col < numTilesWide; col++) {
				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();
				int colorIndexMask = buffer.getInt();

				Color24[] lookupTable = Color24.expandLookupTable(minColor, maxColor);

				for (int k = DXT1_BLOCK_SIZE * DXT1_BLOCK_SIZE - 1; k >= 0; k--) {
					int h = k / DXT1_BLOCK_SIZE, w = k % DXT1_BLOCK_SIZE;
					int pixelIndex = h * width + (col * DXT1_BLOCK_SIZE + w);

					int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

					pixels[pixelIndex] = lookupTable[colorIndex].getPixel888();
				}
			}

			result.setRGB(0, row * DXT1_BLOCK_SIZE, width, DXT1_BLOCK_SIZE, pixels, 0, width);
		}

		return result;
	}

}
