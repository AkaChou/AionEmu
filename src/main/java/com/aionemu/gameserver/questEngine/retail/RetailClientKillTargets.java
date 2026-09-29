package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端击杀目标登记表内存规范视图（原 {@code quest_client_kill_targets.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * Memory canonical view of the client kill-target registry.
 */
public final class RetailClientKillTargets {

	private static final RetailClientKillTargets EMPTY = new RetailClientKillTargets(Map.of(), Map.of());
	private static volatile RetailClientKillTargets defaultInstance;

	private final Map<Integer, Set<Integer>> entries;
	private final Map<Integer, Map<Integer, Set<Integer>>> stageEntries;

	private RetailClientKillTargets(Map<Integer, Set<Integer>> entries,
			Map<Integer, Map<Integer, Set<Integer>>> stageEntries) {
		this.entries = entries;
		this.stageEntries = stageEntries;
	}

	/** 内存静态规范实例（45 个任务 targets + stages 全覆盖）。 / Memory static canonical instance. */
	public static RetailClientKillTargets defaultKillTargets() {
		RetailClientKillTargets instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientKillTargets.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	private static RetailClientKillTargets decodeDefaultInstance() {
		InputStream in = RetailClientKillTargets.class.getResourceAsStream("/quest/quest_client_kill_targets.tsv");
		if (in == null) {
			in = RetailClientKillTargets.class.getResourceAsStream("/aion/data/static_data/quest_retail/quest_client_kill_targets.tsv");
		}
		RetailClientKillTargets targets = EMPTY;
		if (in != null) {
			try (InputStream input = in) {
				targets = load(input);
			} catch (IOException e) {
				targets = EMPTY;
			}
		}
		InputStream stageIn = RetailClientKillTargets.class.getResourceAsStream("/quest/quest_client_kill_targets_stages.tsv");
		if (stageIn == null) {
			stageIn = RetailClientKillTargets.class.getResourceAsStream("/aion/data/static_data/quest_retail/quest_client_kill_targets_stages.tsv");
		}
		if (stageIn != null) {
			try (InputStream sIn = stageIn) {
				targets = targets.withStages(sIn);
			} catch (IOException e) {
				// ignore
			}
		}
		return targets;
	}

	public static RetailClientKillTargets load() {
		return defaultKillTargets();
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientKillTargets empty() {
		return EMPTY;
	}

	/** 全量任务的目标集映射（任务 ID → 目标集）。 / All target entries. */
	public Map<Integer, Set<Integer>> entries() {
		return entries;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过；目标列为空格分隔 id）。 / Parses the registry TSV. */
	public static RetailClientKillTargets load(InputStream input) throws IOException {
		if (input == null) {
			return defaultKillTargets();
		}
		Map<Integer, Set<Integer>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2 || !Character.isDigit(parts[0].charAt(0))) {
					continue;
				}
				int questId = Integer.parseInt(parts[0].trim());
				Set<Integer> targets = new LinkedHashSet<>();
				for (String id : parts[1].split("\\s+")) {
					if (!id.isBlank()) {
						targets.add(Integer.parseInt(id.trim()));
					}
				}
				entries.put(questId, Set.copyOf(targets));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed client kill-target registry", e);
		}
		return new RetailClientKillTargets(Map.copyOf(entries), Map.of());
	}

	/** 目标集包含该怪物的任务集（反查索引；用于判断 NPC 是否属变体目标）。 */
	public Set<Integer> questsTargeting(int npcId) {
		Set<Integer> hits = new LinkedHashSet<>();
		entries.forEach((questId, targets) -> {
			if (targets.contains(npcId)) {
				hits.add(questId);
			}
		});
		return Set.copyOf(hits);
	}

	/** 该任务的客户端目标集；未登记为空集。 / The quest's client targets; empty when unregistered. */
	public Set<Integer> targets(int questId) {
		return entries.getOrDefault(questId, Set.of());
	}

	/** 该任务是否登记了客户端目标名单。 / Whether the quest has an entry. */
	public boolean contains(int questId) {
		return entries.containsKey(questId);
	}

	public int size() {
		return entries.size();
	}

	public RetailClientKillTargets withStages(InputStream input) throws IOException {
		Map<Integer, Map<Integer, Set<Integer>>> merged = new HashMap<>(stageEntries);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank() || !Character.isDigit(line.charAt(0))) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 3) {
					continue;
				}
				int questId = Integer.parseInt(parts[0].trim());
				int stage = Integer.parseInt(parts[1].trim());
				Set<Integer> targets = new LinkedHashSet<>();
				for (String id : parts[2].split("[\\s,]+")) {
					if (!id.isBlank()) {
						targets.add(Integer.parseInt(id.trim()));
					}
				}
				merged.computeIfAbsent(questId, key -> new HashMap<>()).put(stage, Set.copyOf(targets));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed client stage kill-target registry", e);
		}
		Map<Integer, Map<Integer, Set<Integer>>> frozen = new HashMap<>();
		merged.forEach((questId, stages) -> frozen.put(questId, Map.copyOf(stages)));
		return new RetailClientKillTargets(entries, Map.copyOf(frozen));
	}

	/** 该任务指定段（计数槽 1..N）的客户端目标集；未登记为空集。 / The stage's client targets; empty when unregistered. */
	public Set<Integer> stageTargets(int questId, int stage) {
		return stageEntries.getOrDefault(questId, Map.of()).getOrDefault(stage, Set.of());
	}

	/** 该任务的全逐段登记（段号 → 目标集）；未登记为空映射。 / The quest's full per-stage registry; empty when unregistered. */
	public Map<Integer, Set<Integer>> stageTargets(int questId) {
		return stageEntries.getOrDefault(questId, Map.of());
	}
}
