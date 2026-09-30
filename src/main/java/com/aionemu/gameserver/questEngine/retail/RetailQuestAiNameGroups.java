package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真端对话名组表内存规范视图（原 {@code retail-quest-ai-name-groups.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 守备队同组共用 ScriptDLL 对话名，组名解析为全组成员 name_desc 并打标。
 * <p>
 * Memory canonical view of the retail dialog-name group table.
 */
public final class RetailQuestAiNameGroups {

	private RetailQuestAiNameGroups() {
	}

	private static InputStream openStream() {
		InputStream in = RetailQuestAiNameGroups.class.getResourceAsStream("/quest/retail-quest-ai-name-groups.tsv");
		if (in == null) {
			in = RetailQuestAiNameGroups.class.getResourceAsStream("/aion/data/static_data/quest/retail/retail-quest-ai-name-groups.tsv");
		}
		if (in == null) {
			return new ByteArrayInputStream(new byte[0]);
		}
		return in;
	}

	/** 获取内存规范流列表。 / Returns the canonical input streams. */
	public static List<InputStream> streams() {
		return List.of(openStream());
	}

	/** 获取全量声明的对话名组映射。 / Returns all declared dialog-name groups. */
	public static Map<String, List<String>> defaultGroups() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(openStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length == 2) {
					groups.put(parts[0].trim(), List.of(parts[1].trim().split(",")));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to parse retail quest ai name groups", e);
		}
		return Map.copyOf(groups);
	}
}
