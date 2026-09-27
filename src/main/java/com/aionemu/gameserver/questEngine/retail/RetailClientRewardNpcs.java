package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端交付 NPC 集登记（{@code quest_client_reward_npcs.tsv}，只读视图）。
 * <p>
 * 真端家族表的奖励引用可以是 {@code <地图>_<势力名>} 复合名（不是 NPC 名，真端 npcs.xml 亦无）。
 * 交付 NPC 集的唯一权威是客户端任务书 dic 链：{@code QUEST_Q<id>.html} 引用
 * {@code STR_DIC_E_<token>}（本任务 id 或共享名，如 {@code LDF5a_Silverlin_BL}）→ 串表正文点名
 * {@code STR_DIC_N_<显示名>} → npc {@code name_desc} 唯一匹配（生成器：
 * {@code m5b3x_client_reward_npcs.py}）。一个任务可点名多个支部 NPC（2–3 个，与现行 XML 逐一对齐）。
 * <p>
 * Read-only view of the client reward-NPC registry: the quest-letter dic chain is the sole
 * authority for hand-in NPC sets behind composite faction references.
 */
public final class RetailClientRewardNpcs {

	private static final RetailClientRewardNpcs EMPTY = new RetailClientRewardNpcs(Map.of());

	private final Map<Integer, List<Integer>> entries;

	private RetailClientRewardNpcs(Map<Integer, List<Integer>> entries) {
		this.entries = entries;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientRewardNpcs empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientRewardNpcs load(InputStream input) throws IOException {
		Map<Integer, List<Integer>> entries = new HashMap<>();
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
				List<Integer> npcIds = new ArrayList<>();
				for (String id : parts[1].split(",")) {
					npcIds.add(Integer.parseInt(id.trim()));
				}
				entries.put(Integer.parseInt(parts[0]), List.copyOf(npcIds));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client reward npc registry", e);
		}
		return new RetailClientRewardNpcs(Map.copyOf(entries));
	}

	/** 该任务的交付 NPC 集（复合名行非空）。 / The hand-in NPC set for one quest. */
	public List<Integer> rewardNpcs(int questId) {
		return entries.getOrDefault(questId, List.of());
	}
}
