package com.aionemu.gameserver.services;

/**
 * 彩色聊天文本工具，按固定步长为消息字符注入客户端颜色标签。
 * Color-chat text utility that injects client color tags into message characters at a fixed stride.
 *
 * @author KorLightning (Encom)
 */
public class ColorChat {

	/**
	 * 将消息按每 3 个字符分段并套上指定颜色标签。
	 * Wraps the message with the given color tag in 3-character segments.
	 *
	 * original message
	 *
	 * @param color 颜色代码 / color code
	 * @param color @return 带颜色标签的消息 / color-tagged message
	 */
	public static String colorChat(String message, String color) {
		StringBuilder sb = new StringBuilder();
		int index = 0;
		int start = 0;
		for (char ch : message.toCharArray()) {
			if (index % 3 == 0) {
				if (start % 2 == 0) {
					if (start > 0) {
						sb.append(";" + color + "][color:");
					} else {
						sb.append("[color:");
					}
				} else if (start % 2 == 1) {
					if (index < message.length()) {
						sb.append(";" + color + "][color:");
					}
				}
				start++;
			}
			sb.append(String.valueOf(ch));
			index++;
		}
		if (start % 2 == 1) {
			sb.append(";" + color + "]");
		}
		if (sb.lastIndexOf("[color:") > sb.lastIndexOf(";" + color + "]")) {
			sb.append(";" + color + "]");
		}
		return sb.toString();
	}
}
