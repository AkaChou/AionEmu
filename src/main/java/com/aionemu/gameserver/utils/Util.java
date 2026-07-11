package com.aionemu.gameserver.utils;

import java.nio.ByteBuffer;

import com.aionemu.commons.utils.PrintUtils;
import com.aionemu.gameserver.configs.main.NameConfig;

/**
 * 通用工具方法集合（十六进制转储、名称规范化等）。
 * General utility methods (hex dumps, name normalization, etc.).
 */
public class Util {

	/**
	 * 打印带分隔线的章节标题。
	 * Prints a section title with separator lines.
	 *
	 * @param s 章节标题 / Section title
	 */
	public static void printSection(String s) {
		PrintUtils.printSection(s);
	}

	/**
	 * 将 ByteBuffer 中的数据转为十六进制转储（含偏移与 ASCII 侧栏）。
	 * Converts data from the given ByteBuffer into a hex dump with offsets and ASCII sidebar.
	 *
	 * @param data 字节缓冲 / Byte buffer
	 * @return 十六进制转储字符串 / Hex dump string
	 */
	public static String toHex(ByteBuffer data) {
		StringBuilder result = new StringBuilder();
		int counter = 0;
		int b;
		while (data.hasRemaining()) {
			if (counter % 16 == 0) {
				result.append(String.format("%04X: ", counter));
			}
			b = data.get() & 0xff;
			result.append(String.format("%02X ", b));

			counter++;
			if (counter % 16 == 0) {
				result.append("  ");
				toText(data, result, 16);
				result.append("\n");
			}
		}
		int rest = counter % 16;
		if (rest > 0) {
			for (int i = 0; i < 17 - rest; i++) {
				result.append("   ");
			}
			toText(data, result, rest);
		}
		return result.toString();
	}

	/**
	 * 将 ByteBuffer 中的数据转为纯十六进制流（无偏移与 ASCII）。
	 * Converts data from the given ByteBuffer into a plain hex stream (no offsets or ASCII).
	 *
	 * @param data 字节缓冲 / Byte buffer
	 * @return 十六进制流字符串 / Hex stream string
	 */
	public static String toHexStream(ByteBuffer data) {
		StringBuilder result = new StringBuilder();
		int counter = 0;
		int b;
		while (data.hasRemaining()) {
			b = data.get() & 0xff;
			result.append(String.format("%02X ", b));

			counter++;
			if (counter % 16 == 0) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	/**
	 * 将最近读取的字节转为 ASCII 侧栏文本：可打印字符原样输出，其余输出点号。
	 * Appends the last read bytes as ASCII sidebar text: printable chars as-is, others as dots.
	 *
	 * @param data 字节缓冲 / Byte buffer
	 * @param result 结果构建器 / Result builder
	 * @param cnt 字节数量 / Byte count
	 */
	private static void toText(ByteBuffer data, StringBuilder result, int cnt) {
		int charPos = data.position() - cnt;
		for (int a = 0; a < cnt; a++) {
			int c = data.get(charPos++);
			if (c > 0x1f && c < 0x80) {
				result.append((char) c);
			} else {
				result.append('.');
			}
		}
	}

	/**
	 * 将角色名规范化为首字母大写、其余小写；若允许自定义名称则原样返回。
	 * Normalizes a character name to title case; returns as-is when custom names are allowed.
	 *
	 * @param name 原始名称 / Original name
	 * @return 规范化后的名称 / Normalized name
	 */
	public static String convertName(String name) {
		if (!name.isEmpty()) {
			if (NameConfig.ALLOW_CUSTOM_NAMES) {
				return name;
			} else {
				return name.substring(0, 1).toUpperCase() + name.toLowerCase().substring(1);
			}
		} else {
			return "";
		}
	}
}
