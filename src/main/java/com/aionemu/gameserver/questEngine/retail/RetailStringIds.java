package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * 原版任务字符串 id 索引（DD 附加动作 case 7 Message 的 `STR_*` 键解析，原版
 * `XML_ParseStringIndex` 的 Java 侧对位）。
 * <p>
 * 数据源 {@code retail-quest-string-ids.xml}：逐键取自原版字符串表
 * （{@code Map/XML/strings.xml} 的 {@code <id>/<name>} 对，逐字入仓），与生产在用的
 * {@code quest_name_string_ids.xml} 同源同型。查不到的键 = 原版装载失败
 * （"undefined string name"），调用方必须 fail-closed，不得猜 id。
 * <p>
 * Retail quest string-id index: resolves the {@code STR_*} keys of the DD Message extra
 * action (the Java counterpart of the retail {@code XML_ParseStringIndex}). Missing keys are
 * a retail load failure and must fail closed.
 */
public final class RetailStringIds {

	/** 原版字符串 id 表资源路径。 / The retail string-id table resource. */
	public static final String RESOURCE = "/aion/data/static_data/quest/retail/retail-quest-string-ids.xml";

	private static volatile RetailStringIds instance;

	private final Map<String, Integer> idsByKey;
	/** 字符串 id → 原版正文（say 气泡通道用）。 / String id to retail body (say-bubble channel). */
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

	/**
	 * 解析字符串 id 表（{@code retail-quest-string-ids.xml}：key / string_id / body 列；旧 TSV 的
	 * 「第二列必须全数字、否则 MALFORMED」语义保留；{@code body} 列缺席 = 旧格式兼容空正文）。
	 * Parses the string-id table (columns key / string_id / body; the old "second column must be
	 * all digits or MALFORMED" rule is preserved; an absent {@code body} column keeps the
	 * legacy-format empty-body semantics).
	 */
	static RetailStringIds load(InputStream input) throws IOException {
		Map<String, Integer> ids = new HashMap<>();
		Map<Integer, String> bodies = new HashMap<>();
		for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(input), "quest_string_id")) {
			String key = RetailLedgerXml.text(row, "key");
			String idText = RetailLedgerXml.text(row, "string_id");
			if (key == null || idText == null || idText.isEmpty()
					|| !idText.chars().allMatch(Character::isDigit)) {
				throw new IOException("RETAIL_STRING_ID_TABLE_MALFORMED: " + key);
			}
			ids.put(key, Integer.valueOf(idText));
			// body 列 = 原版 <body> 原文（实体已解码；say 气泡通道正文，缺席/空白 = 旧格式兼容空正文）。
			// The body column = the retail <body> text (entities decoded; say-bubble body; absent or
			// blank = legacy-format compatibility with an empty body).
			String body = RetailLedgerXml.text(row, "body");
			if (body != null && !body.isBlank()) {
				bodies.put(Integer.valueOf(idText), body);
			}
		}
		return new RetailStringIds(ids, bodies);
	}

	/** 键 → 字符串 id（查不到返回 null = 原版装载失败语义）。 / Key to string id (null = retail load failure). */
	public Integer resolve(String key) {
		if (key == null) {
			return null;
		}
		return idsByKey.get(key.trim());
	}

	/** 字符串 id → 原版正文（缺正文返回 null = 旧格式行，调用方走登记偏差兜底）。 / String id to retail body (null = legacy row; caller falls back). */
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
