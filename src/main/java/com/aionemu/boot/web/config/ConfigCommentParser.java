package com.aionemu.boot.web.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置键备注解析器：从 {@code .properties} 文本里提取每个键上方注释块的双语说明与默认值。
 * Configuration key comment parser: extracts the bilingual description and the default value from the comment
 * block above each key of a {@code .properties} text.
 * <p>
 * 注释块的形态固定为「{@code # 中文 / English}」，默认值行形态为「{@code # 默认值: X / Default: X}」。
 * 一个键上方可能有多行注释：说明取最靠近键的那一行，其余行作为补充说明保留。
 * <p>
 * The comment blocks have the shape "{@code # Chinese / English}" and the default value line has the shape
 * "{@code # 默认值: X / Default: X}". A key may carry several comment lines above it: the description is the
 * line closest to the key, and the remaining lines are kept as supplementary description.
 *
 * @author AionEmu
 */
public final class ConfigCommentParser {

	/**
	 * 默认值行的中文前缀。
	 * The Chinese prefix of the default value line.
	 */
	private static final String DEFAULT_VALUE_PREFIX = "默认值:";

	/**
	 * 默认值行的英文前缀。
	 * The English prefix of the default value line.
	 */
	private static final String DEFAULT_VALUE_PREFIX_EN = "Default:";

	/**
	 * 默认值行里中英文两半的分隔标记。
	 * The separator between the two halves of a default value line.
	 */
	private static final String DEFAULT_VALUE_SEPARATOR = " / Default:";

	/**
	 * 工具类不允许实例化。
	 * The utility class must not be instantiated.
	 */
	private ConfigCommentParser() {
	}

	/**
	 * 解析文本中每个键的备注。
	 * Parses the comment of every key in the text.
	 * @param text 文件文本 / file text
	 * @return 键到备注的映射；没有备注的键不在其中 / the key-to-comment map, without the keys that carry no comment
	 */
	public static Map<String, ConfigKeyComment> parse(String text) {
		Map<String, ConfigKeyComment> comments = new LinkedHashMap<>();
		List<String> pending = new ArrayList<>();
		for (String raw : text.split("\n", -1)) {
			String trimmed = raw.strip();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
				pending.add(trimmed.substring(1).strip());
				continue;
			}
			int separator = raw.indexOf('=');
			if (separator >= 0) {
				String key = raw.substring(0, separator).strip();
				ConfigKeyComment comment = commentOf(pending);
				if (comment.isPresent()) {
					comments.putIfAbsent(key, comment);
				}
			}
			pending.clear();
		}
		return comments;
	}

	/**
	 * 由键上方的注释行构造备注。
	 * Builds the comment from the comment lines above a key.
	 * @param lines 注释行 / comment lines
	 * @return 备注 / the comment
	 */
	private static ConfigKeyComment commentOf(List<String> lines) {
		String defaultValue = null;
		List<String> descriptions = new ArrayList<>();
		for (String line : lines) {
			if (line.isEmpty()) {
				continue;
			}
			String defaultValueText = defaultValueOf(line);
			if (defaultValueText != null) {
				if (defaultValue == null) {
					defaultValue = defaultValueText;
				}
				continue;
			}
			descriptions.add(line);
		}
		if (descriptions.isEmpty()) {
			return new ConfigKeyComment(null, null, defaultValue);
		}
		String description = descriptions.get(descriptions.size() - 1);
		String descriptionEn = null;
		int slash = description.indexOf('/');
		if (slash >= 0) {
			// 英文半段必须从原始注释行切出：中文半段一旦被截断，偏移量就不再对得上。
			// The English half has to be cut out of the original comment line: once the Chinese half is
			// truncated the offsets no longer line up.
			descriptionEn = description.substring(slash + 1).strip();
			description = description.substring(0, slash).strip();
		}
		return new ConfigKeyComment(emptyToNull(description), emptyToNull(descriptionEn), defaultValue);
	}

	/**
	 * 从注释行里取出默认值；该行不是默认值行时返回 {@code null}。
	 * Extracts the default value from a comment line, or returns {@code null} when the line is not a default
	 * value line.
	 * @param line 注释行 / comment line
	 * @return 默认值；不是默认值行则为 {@code null} / the default value, {@code null} when the line is not one
	 */
	private static String defaultValueOf(String line) {
		if (line.startsWith(DEFAULT_VALUE_PREFIX)) {
			String value = line.substring(DEFAULT_VALUE_PREFIX.length()).strip();
			// 以英文标记切分，值本身含 " / " 时也不会被截断。
			// The English marker does the split so that a value containing " / " is not truncated.
			int separator = value.indexOf(DEFAULT_VALUE_SEPARATOR);
			return (separator >= 0 ? value.substring(0, separator) : value).strip();
		}
		if (line.startsWith(DEFAULT_VALUE_PREFIX_EN)) {
			return line.substring(DEFAULT_VALUE_PREFIX_EN.length()).strip();
		}
		return null;
	}

	/**
	 * 把空串归一为 {@code null}。
	 * Normalizes a blank string to {@code null}.
	 * @param value 原始值 / the raw value
	 * @return 归一后的值 / the normalized value
	 */
	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}
}
