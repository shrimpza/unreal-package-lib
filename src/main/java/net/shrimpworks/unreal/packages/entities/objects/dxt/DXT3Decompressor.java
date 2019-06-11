/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package net.shrimpworks.unreal.packages.entities.objects.dxt;

import java.awt.image.BufferedImage;

public class DXT3Decompressor {

	private static final int BLOCK_SIZE = 4;

	public static BufferedImage decompress(byte[] data, int width, int height) {

		// 8 bit per color ARGB packed in to an integer as a8r8g8b8
		final DXTParams params = new DXTParams(data, width, height, BufferedImage.TYPE_INT_ARGB_PRE, BLOCK_SIZE);

		for (int row = 0; row < params.numTilesHigh; row++) {
			for (int col = 0; col < params.numTilesWide; col++) {
				long alphaData = params.buffer.getLong();
				short minColor = params.buffer.getShort();
				short maxColor = params.buffer.getShort();
				int colorIndexMask = params.buffer.getInt();

				Color24[] lookupTable = Color24.expandLookupTable(minColor, maxColor);

				for (int k = BLOCK_SIZE * BLOCK_SIZE - 1; k >= 0; k--) {
					int alpha = (int)(alphaData >>> (k * 4)) & 0xF; // Alphas are just 4 bits per pixel
					alpha <<= 4;

					int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

					Color24 color = lookupTable[colorIndex];
					int pixel8888 = (alpha << 24) | color.getPixel888();

					int h = k / BLOCK_SIZE, w = k % BLOCK_SIZE;
					int pixelIndex = h * width + (col * BLOCK_SIZE + w);

					params.pixels[pixelIndex] = pixel8888;
				}
			}

			params.image.setRGB(0, row * BLOCK_SIZE, width, BLOCK_SIZE, params.pixels, 0, width);
		}
		return params.image;
	}
}