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
 * 客户端交付 NPC 集登记（只读内存视图）。
 * <p>
 * 真端家族表的奖励引用可以是 {@code <地图>_<势力名>} 复合名（不是 NPC 名，真端 npcs.xml 亦无）。
 * 交付 NPC 集的唯一权威是客户端任务书 dic 链：{@code QUEST_Q<id>.html} 引用
 * {@code STR_DIC_E_<token>}（本任务 id 或共享名，如 {@code LDF5a_Silverlin_BL}）→ 串表正文点名
 * {@code STR_DIC_N_<显示名>} → npc {@code name_desc} 唯一匹配（生成器：
 * {@code m5b3x_client_reward_npcs.py}）。一个任务可点名多个支部 NPC（2–3 个，与现行 XML 逐一对齐）。
 * 2026-09-28 退役 {@code quest_client_reward_npcs.tsv} 后转为内存静态规范视图。
 * <p>
 * In-memory view of the client reward-NPC registry (retired TSV, in-memory view).
 */
public final class RetailClientRewardNpcs {

	private static final RetailClientRewardNpcs DEFAULT = new RetailClientRewardNpcs(buildDefaultEntries());
	private static final RetailClientRewardNpcs EMPTY = new RetailClientRewardNpcs(Map.of());

	private final Map<Integer, List<Integer>> entries;

	private RetailClientRewardNpcs(Map<Integer, List<Integer>> entries) {
		this.entries = entries;
	}

	/** 缺省规范交付 NPC 集登记（退役后生产通道）。 / Default canonical reward NPCs registry. */
	public static RetailClientRewardNpcs defaultRewardNpcs() {
		return DEFAULT;
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

	private static Map<Integer, List<Integer>> buildDefaultEntries() {
		Map<Integer, List<Integer>> m = new HashMap<>();
		List<Integer> npcs_2_799800 = List.of(799800, 799801);
		m.put(35021, npcs_2_799800);
		m.put(35022, npcs_2_799800);
		m.put(35023, npcs_2_799800);
		m.put(35024, npcs_2_799800);
		m.put(35025, npcs_2_799800);
		m.put(35026, npcs_2_799800);
		m.put(35027, npcs_2_799800);
		m.put(35028, npcs_2_799800);
		m.put(35029, npcs_2_799800);
		m.put(35030, npcs_2_799800);
		m.put(35045, npcs_2_799800);
		m.put(35046, npcs_2_799800);
		m.put(35047, npcs_2_799800);
		m.put(35048, npcs_2_799800);
		m.put(35049, npcs_2_799800);
		m.put(35050, npcs_2_799800);
		m.put(35051, npcs_2_799800);
		List<Integer> npcs_1_799805 = List.of(799805);
		m.put(35007, npcs_1_799805);
		m.put(35008, npcs_1_799805);
		List<Integer> npcs_1_799806 = List.of(799806);
		m.put(35014, npcs_1_799806);
		m.put(35015, npcs_1_799806);
		List<Integer> npcs_2_799837 = List.of(799837, 799838);
		m.put(36015, npcs_2_799837);
		m.put(36016, npcs_2_799837);
		m.put(36502, npcs_2_799837);
		m.put(36503, npcs_2_799837);
		m.put(36508, npcs_2_799837);
		m.put(36509, npcs_2_799837);
		m.put(36514, npcs_2_799837);
		m.put(36515, npcs_2_799837);
		m.put(36518, npcs_2_799837);
		m.put(36519, npcs_2_799837);
		m.put(36520, npcs_2_799837);
		m.put(36521, npcs_2_799837);
		m.put(36522, npcs_2_799837);
		m.put(36523, npcs_2_799837);
		m.put(36524, npcs_2_799837);
		m.put(36525, npcs_2_799837);
		m.put(36526, npcs_2_799837);
		m.put(36527, npcs_2_799837);
		m.put(36528, npcs_2_799837);
		List<Integer> npcs_2_799842 = List.of(799842, 799843);
		m.put(45021, npcs_2_799842);
		m.put(45022, npcs_2_799842);
		m.put(45023, npcs_2_799842);
		m.put(45024, npcs_2_799842);
		m.put(45025, npcs_2_799842);
		m.put(45026, npcs_2_799842);
		m.put(45027, npcs_2_799842);
		m.put(45028, npcs_2_799842);
		m.put(45029, npcs_2_799842);
		m.put(45030, npcs_2_799842);
		m.put(45049, npcs_2_799842);
		m.put(45050, npcs_2_799842);
		m.put(45051, npcs_2_799842);
		List<Integer> npcs_1_799848 = List.of(799848);
		m.put(45007, npcs_1_799848);
		m.put(45008, npcs_1_799848);
		List<Integer> npcs_1_799849 = List.of(799849);
		m.put(45014, npcs_1_799849);
		m.put(45015, npcs_1_799849);
		List<Integer> npcs_2_799882 = List.of(799882, 799883);
		m.put(46015, npcs_2_799882);
		m.put(46016, npcs_2_799882);
		m.put(46502, npcs_2_799882);
		m.put(46503, npcs_2_799882);
		m.put(46508, npcs_2_799882);
		m.put(46509, npcs_2_799882);
		m.put(46514, npcs_2_799882);
		m.put(46515, npcs_2_799882);
		m.put(46518, npcs_2_799882);
		m.put(46519, npcs_2_799882);
		m.put(46520, npcs_2_799882);
		m.put(46521, npcs_2_799882);
		m.put(46522, npcs_2_799882);
		m.put(46523, npcs_2_799882);
		m.put(46524, npcs_2_799882);
		m.put(46525, npcs_2_799882);
		m.put(46526, npcs_2_799882);
		m.put(46527, npcs_2_799882);
		m.put(46528, npcs_2_799882);
		List<Integer> npcs_2_800922 = List.of(800922, 800923);
		m.put(39601, npcs_2_800922);
		m.put(39603, npcs_2_800922);
		m.put(39604, npcs_2_800922);
		m.put(39605, npcs_2_800922);
		m.put(39607, npcs_2_800922);
		m.put(39608, npcs_2_800922);
		List<Integer> npcs_2_800925 = List.of(800925, 800926);
		m.put(49601, npcs_2_800925);
		m.put(49603, npcs_2_800925);
		m.put(49604, npcs_2_800925);
		m.put(49605, npcs_2_800925);
		m.put(49607, npcs_2_800925);
		m.put(49608, npcs_2_800925);
		List<Integer> npcs_3_800927 = List.of(800927, 800928, 800929);
		m.put(39609, npcs_3_800927);
		m.put(39610, npcs_3_800927);
		m.put(39612, npcs_3_800927);
		m.put(39613, npcs_3_800927);
		m.put(39615, npcs_3_800927);
		m.put(49609, npcs_3_800927);
		m.put(49610, npcs_3_800927);
		m.put(49612, npcs_3_800927);
		m.put(49613, npcs_3_800927);
		m.put(49615, npcs_3_800927);
		List<Integer> npcs_2_800931 = List.of(800931, 800932);
		m.put(39701, npcs_2_800931);
		m.put(39702, npcs_2_800931);
		m.put(39706, npcs_2_800931);
		m.put(39707, npcs_2_800931);
		m.put(39708, npcs_2_800931);
		List<Integer> npcs_2_800934 = List.of(800934, 800935);
		m.put(49701, npcs_2_800934);
		m.put(49702, npcs_2_800934);
		m.put(49706, npcs_2_800934);
		m.put(49707, npcs_2_800934);
		m.put(49708, npcs_2_800934);
		List<Integer> npcs_3_800936 = List.of(800936, 800937, 800938);
		m.put(39709, npcs_3_800936);
		m.put(39710, npcs_3_800936);
		m.put(39711, npcs_3_800936);
		m.put(39712, npcs_3_800936);
		m.put(39715, npcs_3_800936);
		m.put(49709, npcs_3_800936);
		m.put(49710, npcs_3_800936);
		m.put(49711, npcs_3_800936);
		m.put(49712, npcs_3_800936);
		m.put(49715, npcs_3_800936);
		return Map.copyOf(m);
	}
}
