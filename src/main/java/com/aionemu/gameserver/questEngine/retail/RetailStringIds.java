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

	private RetailStringIds(Map<String, Integer> idsByKey) {
		this.idsByKey = Map.copyOf(idsByKey);
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
			}
		}
		return new RetailStringIds(ids);
	}

	/** 键 → 字符串 id（查不到返回 null = 真端装载失败语义）。 / Key to string id (null = retail load failure). */
	public Integer resolve(String key) {
		if (key == null) {
			return null;
		}
		return idsByKey.get(key.trim());
	}
}
