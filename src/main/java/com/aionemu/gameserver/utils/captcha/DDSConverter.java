package com.aionemu.gameserver.utils.captcha;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BufferedImage → DXT1 DDS 转换器（无透明），供 CAPTCHA 纹理下发客户端。
 * Converts {@link BufferedImage} to DXT1 DDS (no transparency) for CAPTCHA client textures.
 *
 * @author Cura
 */
public class DDSConverter {

	/** DDS 头标志：包含 CAPS / DDS header flag: CAPS present */
	private static final int DDSD_CAPS = 0x0001;
	/** DDS 头标志：包含高度。 / DDS header flag: height present. */
	private static final int DDSD_HEIGHT = 0x0002;
	/** DDS 头标志：包含宽度。 / DDS header flag: width present. */
	private static final int DDSD_WIDTH = 0x0004;
	/** DDS 头标志：包含像素格式。 / DDS header flag: pixel format present. */
	private static final int DDSD_PIXELFORMAT = 0x1000;
	/** DDSheaderflagmipmap 次数 present / DDS header flag: mipmap count present */
	private static final int DDSD_MIPMAPCOUNT = 0x20000;
	/** DDS 头标志：线性尺寸。 / DDS header flag: linear size. */
	private static final int DDSD_LINEARSIZE = 0x80000;
	/** 像素格式标志：FourCC / Pixel format flag: FourCC */
	private static final int DDPF_FOURCC = 0x0004;
	/** CAPS 标志：纹理 / CAPS flag: texture */
	private static final int DDSCAPS_TEXTURE = 0x1000;

	/**
	 * 简单 RGB 颜色（无 alpha），用于 DXT1 端点与距离计算。
	 * Simple RGB color (no alpha) for DXT1 endpoints and distance.
	 */
	protected static class Color {

		/** 红分量。 / Red component. */
		private int r, g, b;

		/**
		 * 构造黑色。
		 * Constructs black.
		 */
		public Color() {
			this.r = this.g = this.b = 0;
		}

		/**
		 * 按 RGB 分量构造。
		 * Constructs from RGB components.
		 *
		 * @param r 红 / red
		 * @param g 绿 / green
		 * @param b 蓝 / blue
		 */
		public Color(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			final Color color = (Color) o;

			if (b != color.b) {
				return false;
			}
			if (g != color.g) {
				return false;
			}
			// noinspection RedundantIfStatement
			if (r != color.r) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int result;
			result = r;
			result = 29 * result + g;
			result = 29 * result + b;
			return result;
		}
	}

	/**
	 * 将图片转为无透明 DXT1 DDS 字节缓冲。
	 * Converts an image to a no-transparency DXT1 DDS byte buffer.
	 *
	 * @param image 源图；null 则返回 null / source image; null yields null
	 * DDS buffer
	 */
	public static ByteBuffer convertToDxt1NoTransparency(BufferedImage image) {
		if (image == null) {
			return null;
		}

		int[] pixels = new int[16];
		int bufferSize = 128 + image.getWidth() * image.getHeight() / 2;
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buildHeaderDxt1(buffer, image.getWidth(), image.getHeight());

		int numTilesWide = image.getWidth() / 4;
		int numTilesHigh = image.getHeight() / 4;
		for (int i = 0; i < numTilesHigh; i++) {
			for (int j = 0; j < numTilesWide; j++) {
				java.awt.image.BufferedImage originalTile = image.getSubimage(j * 4, i * 4, 4, 4);
				originalTile.getRGB(0, 0, 4, 4, pixels, 0, 4);
				Color[] colors = getColors888(pixels);

				for (int k = 0; k < pixels.length; k++) {
					pixels[k] = getPixel565(colors[k]);
					colors[k] = getColor565(pixels[k]);
				}

				int[] extremaIndices = determineExtremeColors(colors);
				if (pixels[extremaIndices[0]] < pixels[extremaIndices[1]]) {
					int t = extremaIndices[0];
					extremaIndices[0] = extremaIndices[1];
					extremaIndices[1] = t;
				}

				buffer.putShort((short) pixels[extremaIndices[0]]);
				buffer.putShort((short) pixels[extremaIndices[1]]);

				long bitmask = computeBitMask(colors, extremaIndices);
				buffer.putInt((int) bitmask);
			}
		}
		return buffer;
	}

	/**
	 * 写入 DXT1 DDS 文件头。
	 * Writes the DXT1 DDS file header.
	 *
	 * target buffer
	 * width
	 * height
	 */
	protected static void buildHeaderDxt1(ByteBuffer buffer, int width, int height) {
		buffer.rewind();
		buffer.put((byte) 'D');
		buffer.put((byte) 'D');
		buffer.put((byte) 'S');
		buffer.put((byte) ' ');
		buffer.putInt(124);
		int flag = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT | DDSD_MIPMAPCOUNT | DDSD_LINEARSIZE;
		buffer.putInt(flag);
		buffer.putInt(height);
		buffer.putInt(width);
		buffer.putInt(width * height / 2);
		buffer.putInt(0); // depth
		buffer.putInt(0); // mipmap count
		buffer.position(buffer.position() + 44); // 11 unused double-words
		buffer.putInt(32); // pixel format size
		buffer.putInt(DDPF_FOURCC);
		buffer.put((byte) 'D');
		buffer.put((byte) 'X');
		buffer.put((byte) 'T');
		buffer.put((byte) '1');
		buffer.putInt(0); // bits per pixel for RGB (non-compressed) formats
		buffer.putInt(0); // rgb bit masks for RGB formats
		buffer.putInt(0); // rgb bit masks for RGB formats
		buffer.putInt(0); // rgb bit masks for RGB formats
		buffer.putInt(0); // alpha mask for RGB formats
		buffer.putInt(DDSCAPS_TEXTURE);
		buffer.putInt(0); // ddsCaps2
		buffer.position(buffer.position() + 12); // 3 unused double-words
	}

	/**
	 * 在 4x4 块中找欧氏距离最远的两个颜色索引。
	 * Finds the two color indices with the largest Euclidean distance in a 4x4 block.
	 *
	 * 16 colors
	 * endpoint indices
	 */
	protected static int[] determineExtremeColors(Color[] colors) {
		int farthest = Integer.MIN_VALUE;
		int[] ex = new int[2];

		for (int i = 0; i < colors.length - 1; i++) {
			for (int j = i + 1; j < colors.length; j++) {
				int d = distance(colors[i], colors[j]);
				if (d > farthest) {
					farthest = d;
					ex[0] = i;
					ex[1] = j;
				}
			}
		}
		return ex;
	}

	/**
	 * 按两端点插值调色板，为每像素选择最近索引并打包位掩码。
	 * Builds interpolated palette from endpoints and packs nearest-index bitmask.
	 *
	 * block colors
	 * endpoint indices
	 * @return 32 位索引掩码 / 32-bit index mask
	 */
	protected static long computeBitMask(Color[] colors, int[] extremaIndices) {
		Color[] colorPoints = new Color[] { null, null, new Color(), new Color() };
		colorPoints[0] = colors[extremaIndices[0]];
		colorPoints[1] = colors[extremaIndices[1]];
		if (colorPoints[0].equals(colorPoints[1])) {
			return 0;
		}
		colorPoints[2].r = (2 * colorPoints[0].r + colorPoints[1].r + 1) / 3;
		colorPoints[2].g = (2 * colorPoints[0].g + colorPoints[1].g + 1) / 3;
		colorPoints[2].b = (2 * colorPoints[0].b + colorPoints[1].b + 1) / 3;
		colorPoints[3].r = (colorPoints[0].r + 2 * colorPoints[1].r + 1) / 3;
		colorPoints[3].g = (colorPoints[0].g + 2 * colorPoints[1].g + 1) / 3;
		colorPoints[3].b = (colorPoints[0].b + 2 * colorPoints[1].b + 1) / 3;

		long bitmask = 0;
		for (int i = 0; i < colors.length; i++) {
			int closest = Integer.MAX_VALUE;
			int mask = 0;
			for (int j = 0; j < colorPoints.length; j++) {
				int d = distance(colors[i], colorPoints[j]);
				if (d < closest) {
					closest = d;
					mask = j;
				}
			}
			bitmask |= mask << i * 2;
		}
		return bitmask;
	}

	/**
	 * RGB888 颜色压成 RGB565 像素值。
	 * Packs an RGB888 color into an RGB565 pixel value.
	 *
	 * color
	 * RGB565 value
	 */
	protected static int getPixel565(Color color) {
		int r = color.r >> 3;
		int g = color.g >> 2;
		int b = color.b >> 3;
		return r << 11 | g << 5 | b;
	}

	/**
	 * 从 RGB565 像素还原颜色分量。
	 * Expands an RGB565 pixel into color components.
	 *
	 * RGB565 value
	 * color
	 */
	protected static Color getColor565(int pixel) {
		Color color = new Color();

		color.r = (int) (((long) pixel) & 0xf800) >> 11;
		color.g = (int) (((long) pixel) & 0x07e0) >> 5;
		color.b = (int) (((long) pixel) & 0x001f);

		return color;
	}

	/**
	 * 从 ARGB 像素数组提取 RGB888 颜色。
	 * Extracts RGB888 colors from ARGB pixel array.
	 *
	 * ARGB pixels
	 * color array
	 */
	protected static Color[] getColors888(int[] pixels) {
		Color[] colors = new Color[pixels.length];

		for (int i = 0; i < pixels.length; i++) {
			colors[i] = new Color();
			colors[i].r = (int) (((long) pixels[i]) & 0xff0000) >> 16;
			colors[i].g = (int) (((long) pixels[i]) & 0x00ff00) >> 8;
			colors[i].b = (int) (((long) pixels[i]) & 0x0000ff);
		}
		return colors;
	}

	/**
	 * 两颜色的平方欧氏距离。
	 * Squared Euclidean distance between two colors.
	 *
	 * @param ca 颜色 A / color A
	 * @param cb 颜色 B / color B
	 * squared distance
	 */
	protected static int distance(Color ca, Color cb) {
		return (cb.r - ca.r) * (cb.r - ca.r) + (cb.g - ca.g) * (cb.g - ca.g) + (cb.b - ca.b) * (cb.b - ca.b);
	}
}
