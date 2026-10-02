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
 * 客户端「交付 NPC 集合」投影（{@code quest_client_handin_npc_sets.tsv}）。
 * <p>
 * 与接取轴对称：真端模板的 {@code reward_npc_name} 同样是**逻辑 NPC 名**（例如每日任务在任意
 * 一名住宅管理员处交付），客户端把它展开成可交付的 NPC 集合。当真端名解析出多于一个 id 时，
 * 合成器只认本投影：解析集必须与该任务客户端声明的集合**逐元素相等**才放行，否则维持
 * fail-closed（{@code RETAIL_REWARD_NPC_AMBIGUOUS}）——客户端是唯一仲裁。
 * <p>
 * Client-declared hand-in NPC sets, symmetric to the accept axis. The client is the sole arbiter
 * when a retail reward name resolves to more than one id; anything else stays fail-closed.
 */
public final class RetailClientHandinNpcSets {

	private static final RetailClientHandinNpcSets EMPTY = new RetailClientHandinNpcSets(Map.of());
	private static volatile RetailClientHandinNpcSets defaultInstance;

	private final Map<Integer, Set<Integer>> entries;

	private RetailClientHandinNpcSets(Map<Integer, Set<Integer>> entries) {
		this.entries = entries;
	}

	/** 缺省投影（退役后生产通道）。 / The default projection used by production. */
	public static RetailClientHandinNpcSets defaultSets() {
		RetailClientHandinNpcSets instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientHandinNpcSets.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空投影（测试合成器用）。 / Empty projection for test synthesizers. */
	public static RetailClientHandinNpcSets empty() {
		return EMPTY;
	}

	private static RetailClientHandinNpcSets decodeDefaultInstance() {
		// 2026-10-02 归位：单一事实源 = quest/retail/，原 src/main/resources/quest/ 双链退役。
		// Relocated 2026-10-02: single canonical home under quest/retail/; the old dual chain
		// under src/main/resources/quest/ is retired.
		InputStream in = RetailClientHandinNpcSets.class.getResourceAsStream(
			"/aion/data/static_data/quest/retail/quest_client_handin_npc_sets.tsv");
		if (in == null) {
			return EMPTY;
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return EMPTY;
		}
	}

	/** 解析投影表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the projection TSV. */
	public static RetailClientHandinNpcSets load(InputStream input) throws IOException {
		Map<Integer, Set<Integer>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					continue;
				}
				Set<Integer> npcIds = new LinkedHashSet<>();
				for (String id : parts[1].split(",")) {
					if (!id.isBlank()) {
						npcIds.add(Integer.parseInt(id.trim()));
					}
				}
				if (npcIds.size() < 2) {
					throw new IOException("hand-in npc set must declare at least two ids: " + line);
				}
				entries.put(Integer.parseInt(parts[0].trim()), Set.copyOf(npcIds));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client hand-in npc sets", e);
		}
		return new RetailClientHandinNpcSets(Map.copyOf(entries));
	}

	/** 该任务的客户端交付 NPC 集（未声明为空集）。 / The client hand-in NPC set for one quest. */
	public Set<Integer> npcIds(int questId) {
		return entries.getOrDefault(questId, Set.of());
	}
}
