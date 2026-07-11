package com.aionemu.gameserver.utils.captcha;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * CAPTCHA 生成工具：随机词、绘制图片并转为 DXT1 DDS 缓冲。
 * CAPTCHA utility: random words, image drawing and DXT1 DDS conversion.
 *
 * @author Cura
 */
@Slf4j
public class CAPTCHAUtil {

	/**
	 * 默认验证码字符长度。
	 * Default CAPTCHA word length.
	 */
	private final static int DEFAULT_WORD_LENGTH = 6;
	/**
	 * 可用字符集。
	 * Available character set.
	 */
	private final static String WORD = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";

	/**
	 * 图片宽度。
	 * Image width.
	 */
	private final static int IMAGE_WIDTH = 160;
	/**
	 * 图片高度。
	 * Image height.
	 */
	private final static int IMAGE_HEIGHT = 80;
	/**
	 * 文字字号。
	 * Text font size.
	 */
	private final static int TEXT_SIZE = 25;
	/**
	 * 字体族名称。
	 * Font family name.
	 */
	private final static String FONT_FAMILY_NAME = "Verdana";

	/**
	 * 根据文本生成 CAPTCHA 的 DXT1 字节缓冲。
	 * Creates a DXT1 byte buffer CAPTCHA for the given word.
	 *
	 * @param word 验证码文本 / CAPTCHA word
	 * DXT1 buffer
	 */
	public static ByteBuffer createCAPTCHA(String word) {
		ByteBuffer byteBuffer = null;
		BufferedImage bImg = createImage(word);

		byteBuffer = DDSConverter.convertToDxt1NoTransparency(bImg);

		return byteBuffer;
	}

	/**
	 * 绘制 CAPTCHA 图片。
	 * Draws the CAPTCHA image.
	 *
	 * @param word 验证码文本 / CAPTCHA word
	 * @return 位图；失败时为 null / image, or null on failure
	 */
	private static BufferedImage createImage(String word) {
		BufferedImage bImg = null;

		try {
			// 创建图像 / image create
			bImg = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D g2 = bImg.createGraphics();

			// 设置背景颜色 / set backgroup color
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

			// 设置字体族、颜色、大小、抗锯齿 / set font family, color, size, antialiasing
			Font font = new Font(FONT_FAMILY_NAME, Font.BOLD, TEXT_SIZE);
			g2.setFont(font);
			g2.setColor(Color.WHITE);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// 单词绘制 / word drawing
			char[] chars = word.toCharArray();
			int x = 10;
			int y = IMAGE_HEIGHT / 2 + TEXT_SIZE / 2;

			for (int i = 0; i < chars.length; i++) {
				char ch = chars[i];
				g2.drawString(String.valueOf(ch), x + font.getSize() * i, y + (int) Math.pow(-1, i) * (TEXT_SIZE / 6));
			}

			// 资源释放 / resource dispose
			g2.dispose();
		} catch (Exception e) {
			log.error(I18n.get("log.1e24158b9157", e));
			bImg = null;
		}
		return bImg;
	}

	/**
	 * 生成默认长度的随机验证码词。
	 * Returns a random CAPTCHA word of default length.
	 *
	 * random word
	 */
	public static String getRandomWord() {
		return randomWord(DEFAULT_WORD_LENGTH);
	}

	/**
	 * 按指定长度生成随机验证码词。
	 * Builds a random CAPTCHA word of the given length.
	 *
	 * word length
	 * random word
	 */
	private static String randomWord(int wordLength) {
		StringBuffer word = new StringBuffer();

		for (int i = 0; i < wordLength; i++) {
			int index = Math.abs((int) (Math.random() * WORD.length()));
			char ch = WORD.charAt(index);
			word.append(ch);
		}
		return word.toString();
	}
}
