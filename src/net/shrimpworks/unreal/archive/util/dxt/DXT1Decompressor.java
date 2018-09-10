/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package net.shrimpworks.unreal.archive.util.dxt;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DXT1Decompressor {

	public static final int DXT1_BLOCK_SIZE = 4;

	public static BufferedImage decompressDXT1(byte[] data, int width, int height) {

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

	private static class Color24 {

		/**
		 * The red color component.
		 */
		public int r;
		/**
		 * The green color component.
		 */
		public int g;
		/**
		 * The blue color component.
		 */
		public int b;

		/**
		 * Creates a 24 bit 888 RGB color with all values set to 0.
		 */
		public Color24() {
			this.r = this.g = this.b = 0;
		}

		public Color24(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public int getPixel888() {
			return (this.r << 16 | this.g << 8 | this.b);
		}

		public static Color24 fromPixel565(int pixel) {
			Color24 color = new Color24();

			color.r = (int)(((long)pixel) & 0xf800) >>> 8;
			color.g = (int)(((long)pixel) & 0x07e0) >>> 3;
			color.b = (int)(((long)pixel) & 0x001f) << 3;

			return color;
		}

		public static Color24 multiplyAlpha(Color24 color, int alpha) {
			Color24 result = new Color24();

			double alphaF = alpha / 256.0;

			result.r = (int)(color.r * alphaF);
			result.g = (int)(color.g * alphaF);
			result.b = (int)(color.b * alphaF);

			return result;
		}

		public static Color24[] expandLookupTable(short minColor, short maxColor) {
			Color24 colorMin = Color24.fromPixel565(minColor);
			Color24 colorMax = Color24.fromPixel565(maxColor);

			Color24 color3 = new Color24();
			Color24 color4 = new Color24();

			color3.r = (2 * colorMin.r + colorMax.r + 1) / 3;
			color3.g = (2 * colorMin.g + colorMax.g + 1) / 3;
			color3.b = (2 * colorMin.b + colorMax.b + 1) / 3;

			color4.r = (colorMin.r + 2 * colorMax.r + 1) / 3;
			color4.g = (colorMin.g + 2 * colorMax.g + 1) / 3;
			color4.b = (colorMin.b + 2 * colorMax.b + 1) / 3;

			return new Color24[] { colorMin, colorMax, color3, color4 };
		}

	}
}
