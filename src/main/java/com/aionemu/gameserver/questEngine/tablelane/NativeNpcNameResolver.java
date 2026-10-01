package com.aionemu.gameserver.questEngine.tablelane;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 真端 NPC 名解析器（计划 §6.2：任务表里的 {@code *_npc_name} 引用的是
 * {@code npcTemplates} 的 {@code name_desc} 全名，短名 {@code name} 亦有效；P0a
 * owner-identity 修正判例）。索引 = name ∪ name_desc，规范化 trim + 小写，精确唯一解析；
 * 缺失（{@code NATIVE_NAME_UNRESOLVED}）与多义（{@code NATIVE_NAME_AMBIGUOUS}）一律
 * fail-closed——本类不做别名表、模糊匹配或前缀补全（反漂移红线 D.3）。
 * <p>
 * 数据源 = 仓库 NPC 模板分片 {@code aion/data/static_data/npcs/npc_template_*.xml}（与生产
 * {@code XmlDataLoader} 同一分片约定）。已知数据事实：分片间存在重复定义的模板 id（836025），
 * 同名索引按 id 去重，不产生假多义。
 * <p>
 * Retail NPC name resolver (plan §6.2: quest-table {@code *_npc_name} values reference the
 * {@code name_desc} full dev names in {@code npcTemplates}; the short {@code name} is valid too;
 * P0a owner-identity correction precedent). The index is name ∪ name_desc, normalized via
 * trim + lowercase, exact-unique resolution only; missing ({@code NATIVE_NAME_UNRESOLVED}) and
 * ambiguous ({@code NATIVE_NAME_AMBIGUOUS}) fail closed — no alias tables, fuzzy matching, or
 * prefix completion (anti-drift red line D.3).
 * <p>
 * Source = the in-repo NPC template shards {@code aion/data/static_data/npcs/npc_template_*.xml}
 * (same shard convention as the production {@code XmlDataLoader}). Known data fact: template id
 * 836025 is defined twice in the shard data; the name index dedupes ids so no phantom ambiguity
 * arises.
 */
public final class NativeNpcNameResolver {

	/** 解析结论。 / Resolution verdict. */
	public enum Resolution {
		/** 唯一命中。 / Exactly one template matched. */
		UNIQUE,
		/** 多个模板命中。 / More than one template matched. */
		AMBIGUOUS,
		/** 无命中。 / No template matched. */
		MISSING
	}

	/** 解析结果：结论 + 命中的模板 id（升序）。 / A resolution: verdict + matched template ids (ascending). */
	public record Match(Resolution resolution, List<Integer> npcIds) {
	}

	private static final String RESOURCE_DIR = "aion/data/static_data/npcs";
	private static final String ALIASES_RESOURCE =
			"aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv";
	/** 与生产 XmlDataLoader 相同的分片命名约定。 / Same shard naming convention as XmlDataLoader. */
	private static final Pattern SHARD_PATTERN = Pattern.compile("npc_template_(\\d+)_(\\d+)\\.xml");
	private static final Pattern NPC_TAG = Pattern.compile("<npc_template\\b([^>]*)>");
	private static final Pattern ATTR = Pattern.compile("\\b(npc_id|name|name_desc)=\"([^\"]*)\"");

	private static volatile NativeNpcNameResolver instance;

	/** 权威真端名 (name_desc) 索引。 / Authoritative retail name_desc index. */
	private final Map<String, List<Integer>> idsByNameDesc;
	/** 短名/客户端名 (name) 索引。 / Short name index. */
	private final Map<String, List<Integer>> idsByName;
	/** 联合全集索引（用于总量度量兼容）。 / Combined index for metrics. */
	private final Map<String, List<Integer>> idsByAll;
	/** 目标名/别名 (quest_ai_name 等) 索引。 / Target/alias index. */
	private final Map<String, List<Integer>> monsterAliases;
	/** 解析过的模板定义数。 / Number of template definitions parsed. */
	private final int templateCount;

	private NativeNpcNameResolver(Map<String, List<Integer>> idsByNameDesc,
			Map<String, List<Integer>> idsByName, Map<String, List<Integer>> idsByAll,
			Map<String, List<Integer>> monsterAliases, int templateCount) {
		this.idsByNameDesc = idsByNameDesc;
		this.idsByName = idsByName;
		this.idsByAll = idsByAll;
		this.monsterAliases = monsterAliases;
		this.templateCount = templateCount;
	}

	/** 已装载的解析器（未装载则先装载）。 / The loaded resolver; loads it first when absent. */
	public static NativeNpcNameResolver instance() {
		NativeNpcNameResolver local = instance;
		if (local == null) {
			synchronized (NativeNpcNameResolver.class) {
				local = instance;
				if (local == null) {
					local = load(NativeNpcNameResolver.class.getClassLoader());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 启动期强制装载（失败即异常，由调用方决定是否终止启动）。 / Eagerly loads at startup; throws on failure. */
	public static void ensureLoaded() {
		instance();
	}

	/** 从 classpath 分片目录装载。 / Loads from the classpath shard directory. */
	static NativeNpcNameResolver load(ClassLoader loader) {
		URL dirUrl = loader.getResource(RESOURCE_DIR);
		if (dirUrl == null || !"file".equals(dirUrl.getProtocol())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: npc template shard directory '"
					+ RESOURCE_DIR + "' is not available as classpath files");
		}
		File dir;
		try {
			dir = new File(URI.create(dirUrl.toExternalForm().replace(" ", "%20")));
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: bad shard directory URL " + dirUrl, e);
		}
		File[] shards = dir.listFiles(file -> SHARD_PATTERN.matcher(file.getName()).matches());
		if (shards == null || shards.length == 0) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: no npc_template_*.xml shards under "
					+ RESOURCE_DIR);
		}
		List<File> ordered = new ArrayList<>(List.of(shards));
		ordered.sort(Comparator.comparing(File::getName));
		Map<String, List<Integer>> idsByNameDesc = new LinkedHashMap<>();
		Map<String, List<Integer>> idsByName = new LinkedHashMap<>();
		Map<String, List<Integer>> idsByAll = new LinkedHashMap<>();
		int templateCount = 0;
		for (File shard : ordered) {
			String raw;
			try {
				raw = Files.readString(shard.toPath());
			} catch (IOException e) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot read " + shard.getName(), e);
			}
			Matcher tag = NPC_TAG.matcher(raw);
			while (tag.find()) {
				templateCount++;
				String npcId = null;
				String name = null;
				String nameDesc = null;
				Matcher attr = ATTR.matcher(tag.group(1));
				while (attr.find()) {
					switch (attr.group(1)) {
						case "npc_id" -> npcId = attr.group(2);
						case "name" -> name = attr.group(2);
						case "name_desc" -> nameDesc = attr.group(2);
						default -> {
						}
					}
				}
				if (npcId == null) {
					throw new IllegalStateException(
							"NATIVE_TABLE_PARSE_FAILED: <npc_template> without npc_id in " + shard.getName());
				}
				int id = Integer.parseInt(npcId);
				// 重复定义的模板 id（现网 836025，同分片两处）：两个定义的名字都入索引；
				// index() 内按名去重 id，重复定义不会制造假多义。
				// Duplicate template ids (836025 today, twice in one shard): both definitions' names
				// are indexed; index() dedupes ids per name so no phantom ambiguity arises.
				index(idsByName, name, id);
				index(idsByNameDesc, nameDesc, id);
				index(idsByAll, name, id);
				index(idsByAll, nameDesc, id);
			}
		}
		if (idsByAll.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: no npc template names indexed");
		}
		Map<String, List<Integer>> aliases = new LinkedHashMap<>();
		URL aliasesUrl = loader.getResource(ALIASES_RESOURCE);
		if (aliasesUrl != null) {
			try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(aliasesUrl.openStream(), java.nio.charset.StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.strip();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					String[] parts = line.split("\t");
					if (parts.length >= 2) {
						String key = parts[0].strip().toLowerCase(Locale.ROOT);
						List<Integer> ids = new ArrayList<>();
						for (String idStr : parts[1].split(",")) {
							idStr = idStr.strip();
							if (!idStr.isEmpty()) {
								ids.add(Integer.parseInt(idStr));
							}
						}
						if (!ids.isEmpty()) {
							aliases.put(key, Collections.unmodifiableList(ids));
						}
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: failed to read npc aliases", e);
			}
		}
		return new NativeNpcNameResolver(Collections.unmodifiableMap(idsByNameDesc),
				Collections.unmodifiableMap(idsByName), Collections.unmodifiableMap(idsByAll),
				Collections.unmodifiableMap(aliases), templateCount);
	}

	private static void index(Map<String, List<Integer>> idsByName, String rawName, int npcId) {
		if (rawName == null) {
			return;
		}
		String normalized = rawName.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			// 真端存在 name=" " 的占位模板，不得成为索引键。 / Retail ships name=" " placeholders; never keys.
			return;
		}
		List<Integer> ids = idsByName.get(normalized);
		if (ids == null) {
			idsByName.put(normalized, List.of(npcId));
		} else if (!ids.contains(npcId)) {
			List<Integer> grown = new ArrayList<>(ids);
			grown.add(npcId);
			idsByName.put(normalized, List.copyOf(grown));
		}
	}

	/** 精确解析（null/空白按无命中处理；优先以权威 name_desc 解析消歧）。 / Exact resolution (prioritizes authoritative name_desc). */
	public Match resolve(String rawName) {
		if (rawName == null) {
			return new Match(Resolution.MISSING, List.of());
		}
		String normalized = rawName.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			return new Match(Resolution.MISSING, List.of());
		}
		// 1. 真端权威：优先按 name_desc 全名（开发英文名，对齐真端 <name> 语义）解析
		List<Integer> byDesc = idsByNameDesc.get(normalized);
		if (byDesc != null && !byDesc.isEmpty()) {
			return new Match(byDesc.size() == 1 ? Resolution.UNIQUE : Resolution.AMBIGUOUS, byDesc);
		}
		// 2. 回退：按 name 短名（客户端名）解析
		List<Integer> byName = idsByName.get(normalized);
		if (byName != null && !byName.isEmpty()) {
			return new Match(byName.size() == 1 ? Resolution.UNIQUE : Resolution.AMBIGUOUS, byName);
		}
		List<Integer> byAlias = monsterAliases.get(normalized);
		if (byAlias != null && !byAlias.isEmpty()) {
			return new Match(byAlias.size() == 1 ? Resolution.UNIQUE : Resolution.AMBIGUOUS, byAlias);
		}
		return new Match(Resolution.MISSING, List.of());
	}

	/**
	 * 解析怪物候选 ID 集合（支持单个唯一 NPC 及真端同名多模板野怪）。
	 * Resolves candidate NPC ids for monster matching (supports unique NPC and multi-template mobs).
	 */
	public List<Integer> resolveMonsterIds(String rawName) {
		if (rawName == null) {
			return List.of();
		}
		String normalized = rawName.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			return List.of();
		}
		List<Integer> byDesc = idsByNameDesc.get(normalized);
		if (byDesc != null && !byDesc.isEmpty()) {
			return byDesc;
		}
		List<Integer> byName = idsByName.get(normalized);
		if (byName != null && !byName.isEmpty()) {
			return byName;
		}
		List<Integer> byAlias = monsterAliases.get(normalized);
		return byAlias != null ? byAlias : List.of();
	}

	/**
	 * 唯一解析，缺失/多义 fail-closed。
	 * Resolves to the single id; missing and ambiguous names fail closed.
	 */
	public int uniqueId(String rawName) {
		Match match = resolve(rawName);
		if (match.resolution() == Resolution.MISSING) {
			throw new IllegalStateException("NATIVE_NAME_UNRESOLVED: npc name '" + rawName + "'");
		}
		if (match.resolution() == Resolution.AMBIGUOUS) {
			throw new IllegalStateException(
					"NATIVE_NAME_AMBIGUOUS: npc name '" + rawName + "' -> " + match.npcIds());
		}
		return match.npcIds().get(0);
	}

	/** 已索引的规范化名数量。 / Number of indexed normalized names. */
	public int size() {
		return idsByAll.size();
	}

	/** 解析过的模板定义数。 / Number of template definitions parsed. */
	public int templateCount() {
		return templateCount;
	}
}
