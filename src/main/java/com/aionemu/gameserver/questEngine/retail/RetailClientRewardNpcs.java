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
 * 客户端交付 NPC 集登记（原 {@code quest_client_reward_npcs.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 真端家族表的奖励引用可以是 {@code <地图>_<势力名>} 复合名（不是 NPC 名，真端 npcs.xml 亦无）。
 * 交付 NPC 集的唯一权威是客户端任务书 dic 链：{@code QUEST_Q<id>.html} 引用
 * {@code STR_DIC_E_<token>}（本任务 id 或共享名，如 {@code LDF5a_Silverlin_BL}）→ 串表正文点名
 * {@code STR_DIC_N_<显示名>} → npc {@code name_desc} 唯一匹配。
 * 一个任务可点名多个支部 NPC（2–3 个，与现行 XML 逐一对齐）。
 * <p>
 * In-memory view of the client reward-NPC registry (retired TSV, in-memory view).
 */
public final class RetailClientRewardNpcs {

	private static final RetailClientRewardNpcs EMPTY = new RetailClientRewardNpcs(Map.of());
	private static volatile RetailClientRewardNpcs defaultInstance;

	private final Map<Integer, List<Integer>> entries;

	private RetailClientRewardNpcs(Map<Integer, List<Integer>> entries) {
		this.entries = entries;
	}

	/** 缺省规范交付 NPC 集登记（退役后生产通道）。 / Default canonical reward NPCs registry. */
	public static RetailClientRewardNpcs defaultRewardNpcs() {
		RetailClientRewardNpcs instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientRewardNpcs.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientRewardNpcs empty() {
		return EMPTY;
	}

	private static RetailClientRewardNpcs decodeDefaultInstance() {
		InputStream in = RetailClientRewardNpcs.class.getResourceAsStream("/quest/quest_client_reward_npcs.tsv");
		if (in == null) {
			in = RetailClientRewardNpcs.class.getResourceAsStream("/aion/data/static_data/quest/retail/quest_client_reward_npcs.tsv");
		}
		if (in == null) {
			return EMPTY;
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return EMPTY;
		}
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
