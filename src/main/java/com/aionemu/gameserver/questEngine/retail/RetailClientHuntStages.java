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
 * 客户端串行阶段契约登记（{@code quest_client_hunt_stages.tsv}，只读视图）。
 * <p>
 * 客户端 {@code quest_monster.csv} 的 {@code Progress(SECTION_n<count; SECTION_(n-1)==count')} 行给出
 * SimpleSerialHunt 的逐段链式门控（乱序不计数）、段内计数与完整刷怪名单（含真端表未列的变体刷怪），
 * 是串行阶梯合成口径的权威（生成器 {@code p3_client_hunt_stages.py}）。
 * <p>
 * Read-only view of the client serial-stage contract for the SimpleSerialHunt family.
 */
public final class RetailClientHuntStages {

	private static final RetailClientHuntStages EMPTY = new RetailClientHuntStages(Map.of());

	/** 一个串行阶段。 / One serial stage. */
	public record Stage(int stage, int count, List<Integer> npcIds, List<String> names) {
	}

	private final Map<Integer, List<Stage>> entries;

	private RetailClientHuntStages(Map<Integer, List<Stage>> entries) {
		this.entries = entries;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientHuntStages empty() {
		return EMPTY;
	}

	private static final String[] DEFAULT_ROWS = {
		"13918	1	1	235321	ldf4_advance_b_killer_dr_65_ae",
		"13918	2	1	235322	ldf4_advance_b_killer_dr_65_ah",
		"13918	3	1	235323	ldf4_advance_b1_killer_dr_65_ah",
		"13918	4	1	235324	ldf4_advance_b_killer_da_65_al",
		"13918	5	1	235325	ldf4_advance_b1_killer_da_65_al",
		"16991	1	4	233720,234720,233721,233882,234721,234736,233722,234722,233723,234723,233724,234724,233725,234725,233726,234726,233727,234727	idf5_u3_vri_fi_65_an,idf5_u3_hard_vri_fi_65_an,idf5_u3_vri_as_65_an,idf5_u3_vri_as_hide_65_an,idf5_u3_hard_vri_as_65_an,idf5_u3_hard_vri_as_hide_65_an,idf5_u3_vri_wi_65_an,idf5_u3_hard_vri_wi_65_an,idf5_u3_vri_ri_65_an,idf5_u3_hard_vri_ri_65_an,idf5_u3_vri_pr_65_an,idf5_u3_hard_vri_pr_65_an,idf5_u3_vri_ra_65_an,idf5_u3_hard_vri_ra_65_an,idf5_u3_vri_gu_65_an,idf5_u3_hard_vri_gu_65_an,idf5_u3_vri_ba_65_an,idf5_u3_hard_vri_ba_65_an",
		"16991	2	4	233728,234728,233729,233883,234729,234737,233730,234730,233731,234731,233732,234732,233733,234733,233734,234734,233735,234735	idf5_u3_vri_fi_65_ae,idf5_u3_hard_vri_fi_65_ae,idf5_u3_vri_as_65_ae,idf5_u3_vri_as_hide_65_ae,idf5_u3_hard_vri_as_65_ae,idf5_u3_hard_vri_as_hide_65_ae,idf5_u3_vri_wi_65_ae,idf5_u3_hard_vri_wi_65_ae,idf5_u3_vri_ri_65_ae,idf5_u3_hard_vri_ri_65_ae,idf5_u3_vri_pr_65_ae,idf5_u3_hard_vri_pr_65_ae,idf5_u3_vri_ra_65_ae,idf5_u3_hard_vri_ra_65_ae,idf5_u3_vri_gu_65_ae,idf5_u3_hard_vri_gu_65_ae,idf5_u3_vri_ba_65_ae,idf5_u3_hard_vri_ba_65_ae",
		"16991	3	2	233736,234682,233737,234683,233738,234684,233739,234685	idf5_u3_vri_fi_sn_65_ae,idf5_u3_hard_vri_fi_sn_65_ae,idf5_u3_vri_as_sn_65_ae,idf5_u3_hard_vri_as_sn_65_ae,idf5_u3_vri_wi_sn_65_ae,idf5_u3_hard_vri_wi_sn_65_ae,idf5_u3_vri_pr_sn_65_ae,idf5_u3_hard_vri_pr_sn_65_ae",
		"18911	1	1	230849	idvritra_base_drakan_as_65_ae2_nmd",
		"18911	2	1	230851	idvritra_base_drakan_gi_65_ae2_nmd",
		"18911	3	1	230850	idvritra_base_drakan_wi_65_ae2_nmd",
		"18911	4	1	230852,233316,233317	idvritra_base_drakan_fi_65_ae2_nmd,idvritra_base_drakan_fi_65_ae2_nmd_b,idvritra_base_drakan_fi_65_ae2_nmd_c",
		"18911	5	1	230853,884058,884061,884064,884067	idvritra_base_drakan_guardian_65_ae2_nmd,ab1_crotan_drakangiant_01_dr_75_af,ab1_crotan_drakangiant_02_dr_75_af,ab1_lamiren_drakangiant_01_dr_75_af,ab1_lamiren_drakangiant_02_dr_75_af",
		"18912	1	2	230847	idvritra_base_fake_box_65_an",
		"18912	2	1	230858	idvritra_base_shita_b5_65_ah_nmd",
		"23918	1	1	235559	ldf4_advance_b_killer_dr_01_65_ae",
		"23918	2	1	235560	ldf4_advance_b_killer_dr_01_65_ah",
		"23918	3	1	235561	ldf4_advance_b1_killer_dr_01_65_ah",
		"23918	4	1	235326	ldf4_advance_b_killer_li_65_al",
		"23918	5	1	235327	ldf4_advance_b1_killer_li_65_al",
		"26991	1	4	233720,234720,233721,233882,234721,234736,233722,234722,233723,234723,233724,234724,233725,234725,233726,234726,233727,234727	idf5_u3_vri_fi_65_an,idf5_u3_hard_vri_fi_65_an,idf5_u3_vri_as_65_an,idf5_u3_vri_as_hide_65_an,idf5_u3_hard_vri_as_65_an,idf5_u3_hard_vri_as_hide_65_an,idf5_u3_vri_wi_65_an,idf5_u3_hard_vri_wi_65_an,idf5_u3_vri_ri_65_an,idf5_u3_hard_vri_ri_65_an,idf5_u3_vri_pr_65_an,idf5_u3_hard_vri_pr_65_an,idf5_u3_vri_ra_65_an,idf5_u3_hard_vri_ra_65_an,idf5_u3_vri_gu_65_an,idf5_u3_hard_vri_gu_65_an,idf5_u3_vri_ba_65_an,idf5_u3_hard_vri_ba_65_an",
		"26991	2	4	233728,234728,233729,233883,234729,234737,233730,234730,233731,234731,233732,234732,233733,234733,233734,234734,233735,234735	idf5_u3_vri_fi_65_ae,idf5_u3_hard_vri_fi_65_ae,idf5_u3_vri_as_65_ae,idf5_u3_vri_as_hide_65_ae,idf5_u3_hard_vri_as_65_ae,idf5_u3_hard_vri_as_hide_65_ae,idf5_u3_vri_wi_65_ae,idf5_u3_hard_vri_wi_65_ae,idf5_u3_vri_ri_65_ae,idf5_u3_hard_vri_ri_65_ae,idf5_u3_vri_pr_65_ae,idf5_u3_hard_vri_pr_65_ae,idf5_u3_vri_ra_65_ae,idf5_u3_hard_vri_ra_65_ae,idf5_u3_vri_gu_65_ae,idf5_u3_hard_vri_gu_65_ae,idf5_u3_vri_ba_65_ae,idf5_u3_hard_vri_ba_65_ae",
		"26991	3	2	233736,234682,233737,234683,233738,234684,233739,234685	idf5_u3_vri_fi_sn_65_ae,idf5_u3_hard_vri_fi_sn_65_ae,idf5_u3_vri_as_sn_65_ae,idf5_u3_hard_vri_as_sn_65_ae,idf5_u3_vri_wi_sn_65_ae,idf5_u3_hard_vri_wi_sn_65_ae,idf5_u3_vri_pr_sn_65_ae,idf5_u3_hard_vri_pr_sn_65_ae",
		"28911	1	1	230849	idvritra_base_drakan_as_65_ae2_nmd",
		"28911	2	1	230851	idvritra_base_drakan_gi_65_ae2_nmd",
		"28911	3	1	230850	idvritra_base_drakan_wi_65_ae2_nmd",
		"28911	4	1	230852,233316,233317	idvritra_base_drakan_fi_65_ae2_nmd,idvritra_base_drakan_fi_65_ae2_nmd_b,idvritra_base_drakan_fi_65_ae2_nmd_c",
		"28911	5	1	230853,884058,884061,884064,884067	idvritra_base_drakan_guardian_65_ae2_nmd,ab1_crotan_drakangiant_01_dr_75_af,ab1_crotan_drakangiant_02_dr_75_af,ab1_lamiren_drakangiant_01_dr_75_af,ab1_lamiren_drakangiant_02_dr_75_af",
		"28912	1	2	230847	idvritra_base_fake_box_65_an",
		"28912	2	1	230858	idvritra_base_shita_b5_65_ah_nmd",
		"30600	1	1	219256,219257	iddreadgion_03_drakanfinamedaa_60_ae,iddreadgion_03_drakanfinamedab_60_ae",
		"30600	2	1	219264	iddreadgion_03_drakanwi_boss_ah",
		"30610	1	1	219256,219257	iddreadgion_03_drakanfinamedaa_60_ae,iddreadgion_03_drakanfinamedab_60_ae",
		"30610	2	1	219264	iddreadgion_03_drakanwi_boss_ah"
	};

	private static final RetailClientHuntStages DEFAULT = new RetailClientHuntStages(buildDefaultEntries());

	/** 缺省规范串行阶段契约登记（退役后生产通道）。 / Default canonical hunt stages registry. */
	public static RetailClientHuntStages defaultHuntStages() {
		return DEFAULT;
	}

	private static Map<Integer, List<Stage>> buildDefaultEntries() {
		Map<Integer, List<Stage>> entries = new HashMap<>();
		for (String line : DEFAULT_ROWS) {
			parseLine(line, entries);
		}
		Map<Integer, List<Stage>> copy = new HashMap<>();
		entries.forEach((questId, stages) -> copy.put(questId, List.copyOf(stages)));
		return Map.copyOf(copy);
	}

	private static void parseLine(String line, Map<Integer, List<Stage>> entries) {
		String[] parts = line.split("\t");
		if (parts.length < 4) {
			return;
		}
		int questId = Integer.parseInt(parts[0]);
		List<Integer> npcIds = new ArrayList<>();
		for (String id : parts[3].split(",")) {
			npcIds.add(Integer.parseInt(id.trim()));
		}
		List<String> names = new ArrayList<>();
		if (parts.length >= 5 && !parts[4].isBlank()) {
			for (String name : parts[4].split(",")) {
				names.add(name.trim());
			}
		}
		Stage stage = new Stage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
			List.copyOf(npcIds), List.copyOf(names));
		entries.computeIfAbsent(questId, ignored -> new ArrayList<>()).add(stage);
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientHuntStages load(InputStream input) throws IOException {
		Map<Integer, List<Stage>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 4) {
					continue;
				}
				int questId = Integer.parseInt(parts[0]);
				List<Integer> npcIds = new ArrayList<>();
				for (String id : parts[3].split(",")) {
					npcIds.add(Integer.parseInt(id.trim()));
				}
				List<String> names = new ArrayList<>();
				if (parts.length >= 5 && !parts[4].isBlank()) {
					for (String name : parts[4].split(",")) {
						names.add(name.trim());
					}
				}
				Stage stage = new Stage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
					List.copyOf(npcIds), List.copyOf(names));
				entries.computeIfAbsent(questId, ignored -> new ArrayList<>()).add(stage);
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client hunt stage registry", e);
		}
		Map<Integer, List<Stage>> copy = new HashMap<>();
		entries.forEach((questId, stages) -> copy.put(questId, List.copyOf(stages)));
		return new RetailClientHuntStages(Map.copyOf(copy));
	}

	/** 该任务的串行阶段清单。 / The serial stages of one quest. */
	public List<Stage> stages(int questId) {
		return entries.getOrDefault(questId, List.of());
	}
}
