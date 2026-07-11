package com.aionemu.gameserver.services;

import com.aionemu.gameserver.configs.main.NameConfig;

/**
 * 名称限制服务，校验角色名合法性与屏蔽词。
 * Name restriction service validating character names and filtering forbidden words.
 */
public class NameRestrictionService {

	private static final String ENCODED_BAD_WORD = "----";
	/**
	 * 校验名称是否符合配置的字符模式。
	 * Checks whether the name matches the configured character pattern.
	 *
	 * @param name 待校验名称 / name to validate
	 * whether valid
	 */
	public static boolean isValidName(String name) {
		return NameConfig.CHAR_NAME_PATTERN.matcher(name).matches();
	}

	/**
	 * 判断名称是否命中客户端屏蔽词或禁用序列。
	 * Checks whether the name hits client forbidden words or forbidden sequences.
	 *
	 * @param name 待检查名称 / name to check
	 * @return 是否为禁用词 / whether forbidden
	 */
	public static boolean isForbiddenWord(String name) {
		return isForbiddenByClient(name) || isForbiddenBySequence(name);
	}

	private static boolean isForbiddenByClient(String name) {
		if (!NameConfig.NAME_FORBIDDEN_ENABLE || NameConfig.NAME_FORBIDDEN_CLIENT.equals("")) {
			return false;
		}
		for (String s : NameConfig.NAME_FORBIDDEN_CLIENT.split(",")) {
			if (name.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isForbiddenBySequence(String name) {
		if (NameConfig.NAME_SEQUENCE_FORBIDDEN.equals("")) {
			return false;
		}
		for (String s : NameConfig.NAME_SEQUENCE_FORBIDDEN.toLowerCase().split(",")) {
			if (name.toLowerCase().contains(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 过滤聊天消息中的屏蔽词（替换为掩码）。
	 * Filters forbidden words in chat messages (replaces with a mask).
	 *
	 * original message
	 *
	 * @param message @return 过滤后消息 / filtered message
	 */
	public static String filterMessage(String message) {
		for (String word : message.split(" ")) {
			if (isForbiddenWord(word)) {
				message.replace(word, ENCODED_BAD_WORD);
			}
		}
		return message;
	}
}
