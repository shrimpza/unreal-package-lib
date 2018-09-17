package net.shrimpworks.unreal.packages.entities.objects.dxt;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DXT5Decompressor {

	private static final int BLOCK_SIZE = 4;

	public static BufferedImage decompress(byte[] data, int width, int height) {

		if (width < BLOCK_SIZE || height < BLOCK_SIZE) {
			throw new IllegalArgumentException("Invalid image size");
		}

		ByteBuffer buffer = ByteBuffer.wrap(data);
		if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		}

		int numTilesWide = width / BLOCK_SIZE;
		int numTilesHigh = height / BLOCK_SIZE;

		// 8 bit per color ARGB packed in to an integer as a8r8g8b8
		int[] pixels = new int[BLOCK_SIZE * width];

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

		for (int row = 0; row < numTilesHigh; row++) {
			for (int col = 0; col < numTilesWide; col++) {
				short alpha0 = (short)(buffer.get() & 0xFF);
				short alpha1 = (short)(buffer.get() & 0xFF);

				byte[] alphaCodes = new byte[6];
				buffer.get(alphaCodes, 0, 6);

				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();

				int colorIndexMask = buffer.getInt();

				Color24[] lookupTable = Color24.expandLookupTable(minColor, maxColor);

				// KW no idea what i'm going with the alpha here, just cobbled some methods together and got it working :|
				byte[] alphaTable = new byte[] {
						(byte)(0x7 & alphaCodes[0]),
						(byte)(0x7 & (alphaCodes[0] >>> 3)),
						(byte)(0x7 & (((0x1 & alphaCodes[1]) << 2) + (alphaCodes[0] >>> 6))),
						(byte)(0x7 & (alphaCodes[1] >>> 1)),
						(byte)(0x7 & (alphaCodes[1] >>> 4)),
						(byte)(0x7 & (((0x3 & alphaCodes[2]) << 1) + (alphaCodes[1] >>> 7))),
						(byte)(0x7 & (alphaCodes[2] >>> 2)),
						(byte)(0x7 & (alphaCodes[2] >>> 5)),
						(byte)(0x7 & alphaCodes[3]),
						(byte)(0x7 & (alphaCodes[3] >>> 3)),
						(byte)(0x7 & (((0x1 & alphaCodes[4]) << 2) + (alphaCodes[3] >>> 6))),
						(byte)(0x7 & (alphaCodes[4] >>> 1)),
						(byte)(0x7 & (alphaCodes[4] >>> 4)),
						(byte)(0x7 & (((0x3 & alphaCodes[5]) << 1) + (alphaCodes[4] >>> 7))),
						(byte)(0x7 & (alphaCodes[5] >>> 2)),
						(byte)(0x7 & (alphaCodes[5] >>> 5))
				};

				for (int k = BLOCK_SIZE * BLOCK_SIZE - 1; k >= 0; k--) {
					int alpha = a2Value(alphaTable[k], alpha0, alpha1); //alpha

					int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

					Color24 color = lookupTable[colorIndex];
					int pixel8888 = (alpha << 24) | color.getPixel888();

					int h = k / BLOCK_SIZE, w = k % BLOCK_SIZE;
					int pixelIndex = h * width + (col * BLOCK_SIZE + w);

					pixels[pixelIndex] = pixel8888;
				}
			}

			result.setRGB(0, row * BLOCK_SIZE, width, BLOCK_SIZE, pixels, 0, width);
		}

		return result;
	}

	private static byte a2Value(byte code, short a0, short a1) {
		if (a0 > a1) {
			switch (code) {
				case 0:
					return (byte)a0;
				case 1:
					return (byte)a1;
				case 2:
					return (byte)((6 * a0 + 1 * a1) / 7);
				case 3:
					return (byte)((5 * a0 + 2 * a1) / 7);
				case 4:
					return (byte)((4 * a0 + 3 * a1) / 7);
				case 5:
					return (byte)((3 * a0 + 4 * a1) / 7);
				case 6:
					return (byte)((2 * a0 + 5 * a1) / 7);
				case 7:
					return (byte)((1 * a0 + 6 * a1) / 7);
			}
		} else {
			switch (code) {
				case 0:
					return (byte)a0;
				case 1:
					return (byte)a1;
				case 2:
					return (byte)((4 * a0 + 1 * a1) / 5);
				case 3:
					return (byte)((3 * a0 + 2 * a1) / 5);
				case 4:
					return (byte)((2 * a0 + 3 * a1) / 5);
				case 5:
					return (byte)((1 * a0 + 4 * a1) / 5);
				case 6:
					return 0;
				case 7:
					return (byte)0xFF;
			}
		}

		return (byte)a0;
	}
}
