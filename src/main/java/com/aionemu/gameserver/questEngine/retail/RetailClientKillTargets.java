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
 * 客户端击杀目标登记表（{@code quest_client_kill_targets.tsv}，只读视图）——变体轴裁定的生产化。
 * <p>
 * 真端家族表的怪物列只给基础模板名；客户端任务书（{@code quest_monster.csv}）的 SECTION 怪物名单
 * 还包含同族 T_ 实刷变体（name_id 不同、显示名族闭包覆盖不到）。本登记表 = 客户端名单经
 * npc_template 名称解析 + 生产刷怪数据可达性自检的目标全集（生成器：
 * {@code generate_kill_target_contract_tsv.py}）。单段 hunt 行把名单并入唯一计数槽；
 * 多段行需要按 SECTION 拆分的逐段登记，暂缓（15546/25546 curated deferral）。
 * <p>
 * Read-only view of the client kill-target registry (the production form of the variant-axis
 * adjudication): the retail tables name only base templates while the client journal's SECTION
 * lists also cover same-family spawned T_ variants with distinct name_ids. Single-stage hunt rows
 * merge the registry into their only counter; multi-stage rows need a per-SECTION registry and
 * stay deferred.
 */
public final class RetailClientKillTargets {

	private static final RetailClientKillTargets EMPTY = new RetailClientKillTargets(Map.of(), Map.of());

	private final Map<Integer, Set<Integer>> entries;
	private final Map<Integer, Map<Integer, Set<Integer>>> stageEntries;

	private RetailClientKillTargets(Map<Integer, Set<Integer>> entries,
			Map<Integer, Map<Integer, Set<Integer>>> stageEntries) {
		this.entries = entries;
		this.stageEntries = stageEntries;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientKillTargets empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过；目标列为空格分隔 id）。 / Parses the registry TSV. */
	public static RetailClientKillTargets load(InputStream input) throws IOException {
		Map<Integer, Set<Integer>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 跳过注释行与列头行（quest_id<TAB>targets）。
				// Skip comment lines and the column-header line.
				if (line.startsWith("#") || line.isBlank() || !Character.isDigit(line.charAt(0))) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					continue;
				}
				Set<Integer> targets = new LinkedHashSet<>();
				for (String id : parts[1].split("[\\s,]+")) {
					if (!id.isBlank()) {
						targets.add(Integer.parseInt(id.trim()));
					}
				}
				entries.put(Integer.parseInt(parts[0]), Set.copyOf(targets));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed client kill-target registry", e);
		}
		return new RetailClientKillTargets(Map.copyOf(entries), Map.of());
	}

	/** 该任务的客户端击杀目标全集；未登记为空集。 / The quest's client kill targets; empty when unregistered. */
	public Set<Integer> targets(int questId) {
		return entries.getOrDefault(questId, Set.of());
	}

	/**
	 * 并入逐段登记表（{@code quest_client_kill_targets_stages.tsv}：{@code quest_id<TAB>stage<TAB>targets}）。
	 * 客户端 {@code quest_monster.csv} 每个 SECTION 行给出该段全部名字变体（base + T_ 实刷体），
	 * 段号与 DD 计数槽 1..N 一一对应——多段顺序链保持段间互斥的前提。
	 * Merges the per-stage registry: each client SECTION row names that stage's full variant list,
	 * with the stage index matching the DD counter slot 1..N — the precondition for keeping stage
	 * isolation on multi-stage sequential chains.
	 */
	public RetailClientKillTargets withStages(InputStream input) throws IOException {
		Map<Integer, Map<Integer, Set<Integer>>> merged = new HashMap<>(stageEntries);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 跳过注释行与列头行（quest_id<TAB>stage<TAB>targets）。
				// Skip comment lines and the column-header line.
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
