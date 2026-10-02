package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 真端任务字符串 id 索引（DD 附加动作 case 7 Message 的 `STR_*` 键解析，真端
 * `XML_ParseStringIndex` 的 Java 侧对位）。
 * <p>
 * 数据源 {@code retail-quest-string-ids.tsv}：逐键取自真端字符串表
 * （{@code Map/XML/strings.xml} 的 {@code <id>/<name>} 对，逐字入仓），与生产在用的
 * {@code quest_name_string_ids.tsv} 同源同型。查不到的键 = 真端装载失败
 * （"undefined string name"），调用方必须 fail-closed，不得猜 id。
 * <p>
 * Retail quest string-id index: resolves the {@code STR_*} keys of the DD Message extra
 * action (the Java counterpart of the retail {@code XML_ParseStringIndex}). Missing keys are
 * a retail load failure and must fail closed.
 */
public final class RetailStringIds {

	/** 真端字符串 id 表资源路径。 / The retail string-id table resource. */
	public static final String RESOURCE = "/aion/data/static_data/quest/retail/retail-quest-string-ids.tsv";

	private static volatile RetailStringIds instance;

	private final Map<String, Integer> idsByKey;
	/** 字符串 id → 真端正文（say 气泡通道用）。 / String id to retail body (say-bubble channel). */
	private final Map<Integer, String> bodiesById;

	private RetailStringIds(Map<String, Integer> idsByKey, Map<Integer, String> bodiesById) {
		this.idsByKey = Map.copyOf(idsByKey);
		this.bodiesById = Map.copyOf(bodiesById);
	}

	public static RetailStringIds instance() {
		RetailStringIds local = instance;
		if (local == null) {
			synchronized (RetailStringIds.class) {
				local = instance;
				if (local == null) {
					try (InputStream input = RetailStringIds.class.getResourceAsStream(RESOURCE)) {
						if (input == null) {
							throw new IllegalStateException("RETAIL_STRING_ID_TABLE_MISSING: " + RESOURCE);
						}
						instance = local = load(input);
					} catch (IOException e) {
						throw new IllegalStateException("RETAIL_STRING_ID_TABLE_UNREADABLE: " + RESOURCE, e);
					}
				}
			}
		}
		return local;
	}

	static RetailStringIds load(InputStream input) throws IOException {
		Map<String, Integer> ids = new HashMap<>();
		Map<Integer, String> bodies = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] columns = line.split("\t");
				if (columns.length < 2 || !columns[1].chars().allMatch(Character::isDigit)) {
					throw new IOException("RETAIL_STRING_ID_TABLE_MALFORMED: " + line);
				}
				ids.put(columns[0], Integer.valueOf(columns[1]));
				// 第 3 列 = 真端 <body> 原文（实体已解码；say 气泡通道正文，缺列 = 旧格式兼容空正文）。
				// Column 3 = the retail <body> text (entities decoded; say-bubble body; absent column
				// = legacy-format compatibility with an empty body).
				if (columns.length >= 3 && !columns[2].isBlank()) {
					bodies.put(Integer.valueOf(columns[1]), columns[2]);
				}
			}
		}
		return new RetailStringIds(ids, bodies);
	}

	/** 键 → 字符串 id（查不到返回 null = 真端装载失败语义）。 / Key to string id (null = retail load failure). */
	public Integer resolve(String key) {
		if (key == null) {
			return null;
		}
		return idsByKey.get(key.trim());
	}

	/** 字符串 id → 真端正文（缺正文返回 null = 旧格式行，调用方走登记偏差兜底）。 / String id to retail body (null = legacy row; caller falls back). */
	public String bodyOf(int stringId) {
		return bodiesById.get(stringId);
	}

	/** 已登记正文覆盖率断言入口（门禁用）。 / Bodies view for gate assertions. */
	public Map<Integer, String> bodies() {
		return bodiesById;
	}

	/** 全部已登记键（门禁冻结键集用）。 / All registered keys (for gate key-set freezing). */
	public java.util.Set<String> keys() {
		return idsByKey.keySet();
	}
}
