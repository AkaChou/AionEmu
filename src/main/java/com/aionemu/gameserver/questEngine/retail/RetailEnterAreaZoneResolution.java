package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DD enterarea 别名 → 服务端登记区名解析表内存规范视图（原 {@code quest_enterarea_zone_resolution.tsv} 退役后转为内存静态规范）。
 * <p>
 * Memory canonical view of the DD enterarea alias to registered zone name resolution table.
 */
public final class RetailEnterAreaZoneResolution {

	private static final String TSV_CONTENT =
		"# DD enterarea 别名 → 服务端登记区名（遗留 enter-zone 证据 + 真端世界 questscript_area）。\n" +
		"# DD enterarea alias to the registered server zone name (legacy enter-zone evidence + retail\n" +
		"# world questscript_area).\n" +
		"#quest_id	dd_alias	zone_name	source\n" +
		"10011	IDAb1_Ere_SensoryArea_Q10011a	IDAB1_ERE_Q10011_A_302340000	legacy-enterzone\n" +
		"10011	IDAb1_Ere_SensoryArea_Q10012a	IDAB1_ERE_Q10011_B_302340000	legacy-enterzone\n" +
		"10035	LF4_SensoryArea_Q10035A	ANGRIEF_GATE_210050000	legacy-enterzone\n" +
		"10113	IDAbRe_Core_03_SensoryArea_Q10113	IDABRE_CORE_03_Q10113_A_301720000	legacy-enterzone\n" +
		"10503	LF5_SensoryArea_Q10503	LF5_SENSORYAREA_Q10503_210070000	legacy-enterzone\n" +
		"10506	LF5_SensoryArea_Q10506	LF5_SENSORYAREA_Q10506_210070000	legacy-enterzone\n" +
		"10507	LF5_SensoryArea_Q10507	LF5_SENSORYAREA_Q10507_210070000	legacy-enterzone\n" +
		"10527	LF6_SensoryArea_Q10527a	FALLOW_RUINS_210100000	legacy-enterzone\n" +
		"10527	LF6_SensoryArea_Q10527b	TARHA_KRALL_VILLAGE_210100000	legacy-enterzone\n" +
		"10527	LF6_SensoryArea_Q10527c	VALLEY_OF_THE_WAYWARD_210100000	legacy-enterzone\n" +
		"13956	IDAb1_Ere_SensoryArea_Q10012a	IDAB1_ERE_Q13956_D_302340000	legacy-enterzone\n" +
		"13956	IDAb1_Ere_SensoryArea_Q13956a	IDAB1_ERE_Q13956_A_302340000	legacy-enterzone\n" +
		"13956	IDAb1_Ere_SensoryArea_Q13956b	IDAB1_ERE_Q13956_B_302340000	legacy-enterzone\n" +
		"13956	IDAb1_Ere_SensoryArea_Q13956c	IDAB1_ERE_Q13956_C_302340000	legacy-enterzone\n" +
		"13960	IDEternity_War_SensoryArea_Q13960	IDETERNITY_WAR_Q13960_302350000	legacy-enterzone\n" +
		"13965	IDEternity_War_SensoryArea_Q13965a	IDETERNITY_WAR_Q13965_302350000	legacy-enterzone\n" +
		"13967	IDEternity_War_SensoryArea_Q13967a	IDETERNITY_WAR_Q13967_A_302350000	legacy-enterzone-name-match\n" +
		"14263	IDLF1_SensoryArea_MasterBoss01	IDLF1_QuestArea_MasterBoss01	retail-world-questscript-area\n" +
		"15400	AB1_SensoryArea_Q15400a	KROTAN_REFUGE_400010000	legacy-enterzone\n" +
		"15551	LF6_SensoryArea_Q15551_AtoB	LF6_SENSORY_AREA_Q15551_A_TO_B_210100000	legacy-enterzone\n" +
		"15551	LF6_SensoryArea_Q15551_BtoA	LF6_SENSORY_AREA_Q15551_B_TO_A_210100000	legacy-enterzone\n" +
		"15552	LF6_SensoryArea_Q15552_AtoD	LF6_SENSORY_AREA_Q15552_A_TO_D_210100000	legacy-enterzone\n" +
		"15552	LF6_SensoryArea_Q15552_DtoA	LF6_SENSORY_AREA_Q15552_D_TO_A_210100000	legacy-enterzone\n" +
		"15553	LF6_SensoryArea_Q15553_AtoF	LF6_SENSORY_AREA_Q15553_A_TO_F_210100000	legacy-enterzone\n" +
		"15553	LF6_SensoryArea_Q15553_FtoA	LF6_SENSORY_AREA_Q15553_F_TO_A_210100000	legacy-enterzone\n" +
		"15554	LF6_SensoryArea_Q15554_AtoH	LF6_SENSORY_AREA_Q15554_A_TO_H_210100000	legacy-enterzone\n" +
		"15554	LF6_SensoryArea_Q15554_HtoA	LF6_SENSORY_AREA_Q15554_H_TO_A_210100000	legacy-enterzone\n" +
		"15601	LF6_SensoryArea_Q15601a_Dynamic_Env	BARTHOR_LANDING_210100000	legacy-enterzone\n" +
		"15601	LF6_SensoryArea_Q15601b	LOST_COVE_210100000	legacy-enterzone\n" +
		"15602	LF6_SensoryArea_Q15602a_Dynamic_Env	CELLATUN_CIRCLE_210100000	legacy-enterzone\n" +
		"15604	LF6_SensoryArea_Q15604a_Dynamic_Env	ERASMID_HOLLOW_210100000	legacy-enterzone\n" +
		"15605	LF6_SensoryArea_Q15605a_Named	COURT_OF_AURONUS_210100000	legacy-enterzone\n" +
		"15608	LF6_SensoryArea_Q15608a_Dynamic_Env	LF6_SENSORY_AREA_Q15608_A_DYNAMIC_ENV_210100000	legacy-enterzone\n" +
		"15613	DF6_SensoryArea_Q15613a	DF6_SENSORY_AREA_Q15613_A_220110000	legacy-enterzone\n" +
		"15613	DF6_SensoryArea_Q25604a_Dynamic_Env	DF6_SENSORY_AREA_Q25604_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"16800	LF_Tower_SensoryArea_Q16800	LF_TOWER_SENSORY_AREA_Q16800_210110000	legacy-enterzone\n" +
		"16800	IDEternity_01_SensoryArea_Q16800	IDETERNITY_01_Q16800_301540000	legacy-enterzone\n" +
		"16821	IDEternity_02_SensoryArea_Q16821a	IDETERNITY_02_Q16821_A_301550000	legacy-enterzone\n" +
		"16821	IDEternity_02_SensoryArea_Q16821b	IDETERNITY_02_Q16821_B_301550000	legacy-enterzone\n" +
		"16822	IDEternity_02_SensoryArea_Q16822a	IDETERNITY_02_Q16822_A_301550000	legacy-enterzone\n" +
		"16822	IDEternity_02_SensoryArea_Q16822b	IDETERNITY_02_Q16822_B_301550000	legacy-enterzone\n" +
		"16822	IDEternity_02_SensoryArea_Q16822c	IDETERNITY_02_Q16822_C_301550000	legacy-enterzone\n" +
		"16822	IDEternity_02_SensoryArea_Q16822d	IDETERNITY_02_Q16822_D_301550000	legacy-enterzone\n" +
		"16823	IDEternity_02_SensoryArea_Q16823a	IDETERNITY_02_Q16823_A_301550000	legacy-enterzone\n" +
		"16823	IDEternity_02_SensoryArea_Q16823b	IDETERNITY_02_Q16823_B_301550000	legacy-enterzone\n" +
		"16827	IDEternity_02_SensoryArea_Q16827a	IDETERNITY_02_Q16827_A_301550000	legacy-enterzone\n" +
		"16830	IDEternity_02_SensoryArea_Q16830b	IDETERNITY_02_Q16830_B_301550000	legacy-enterzone\n" +
		"16831	IDEternity_02_SensoryArea_Q16831a	IDETERNITY_02_Q16831_A_301550000	legacy-enterzone\n" +
		"16831	IDEternity_02_SensoryArea_Q16831b	IDETERNITY_02_Q16831_B_301550000	legacy-enterzone\n" +
		"16831	IDEternity_02_SensoryArea_Q16831c	IDETERNITY_02_Q16831_C_301550000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836a	IDETERNITY_03_Q16836_A_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836b	IDETERNITY_03_Q16836_B_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836c	IDETERNITY_03_Q16836_C_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836d	IDETERNITY_03_Q16836_D_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836e	IDETERNITY_03_Q16836_E_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836f	IDETERNITY_03_Q16836_F_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836g	IDETERNITY_03_Q16836_G_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836h	IDETERNITY_03_Q16836_H_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836i	IDETERNITY_03_Q16836_I_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836j	IDETERNITY_03_Q16836_J_301560000	legacy-enterzone\n" +
		"16836	IDEternity_03_SensoryArea_Q16836k	IDETERNITY_03_Q16836_K_301560000	legacy-enterzone\n" +
		"16839	IDEternity_03_SensoryArea_Q16836g	IDETERNITY_03_Q16836_G_301560000	legacy-enterzone\n" +
		"16987	DF5_SensoryArea_Q16987	DF5_SENSORYAREA_Q16987_220080000	legacy-enterzone\n" +
		"17505	IDLDF5_Under_02_War_SensoryArea_Q17505a	IDLDF5_UNDER_02_WAR_ITEMUSEAREA_17505A	legacy-enterzone\n" +
		"18252	IDInfinity_SensoryArea_Q18252a	IDINFINITY_SENSORYAREA_Q18252A_302400000	legacy-enterzone\n" +
		"18253	IDInfinity_SensoryArea_Q18253a	IDINFINITY_SENSORYAREA_Q18253A_302400000	legacy-enterzone\n" +
		"20011	IDAb1_Ere_SensoryArea_Q10011a	IDAB1_ERE_Q10011_A_302340000	legacy-enterzone\n" +
		"20011	IDAb1_Ere_SensoryArea_Q10012a	IDAB1_ERE_Q10011_B_302340000	legacy-enterzone\n" +
		"20113	IDAbRe_Core_03_SensoryArea_Q10113	IDABRE_CORE_03_Q10113_A_301720000	legacy-enterzone\n" +
		"20506	DF5_SensoryArea_Q20506a	MINDBOGGLE_WASTE_220080000	legacy-enterzone\n" +
		"20527	DF6_SensoryArea_Q20527a	AZURELIGHT_FOREST_220110000	legacy-enterzone\n" +
		"20527	DF6_SensoryArea_Q20527b	ZENZEN_TRIBAL_GROUNDS_220110000	legacy-enterzone\n" +
		"20527	DF6_SensoryArea_Q20527c	FEATHERFERN_JUNGLE_220110000	legacy-enterzone\n" +
		"23956	IDAb1_Ere_SensoryArea_Q10012a	IDAB1_ERE_Q13956_D_302340000	legacy-enterzone\n" +
		"23956	IDAb1_Ere_SensoryArea_Q13956a	IDAB1_ERE_Q13956_A_302340000	legacy-enterzone\n" +
		"23956	IDAb1_Ere_SensoryArea_Q13956b	IDAB1_ERE_Q13956_B_302340000	legacy-enterzone\n" +
		"23956	IDAb1_Ere_SensoryArea_Q13956c	IDAB1_ERE_Q13956_C_302340000	legacy-enterzone\n" +
		"23960	IDEternity_War_SensoryArea_Q23960	IDETERNITY_WAR_Q23960_302350000	legacy-enterzone\n" +
		"23965	IDEternity_War_SensoryArea_Q13965a	IDETERNITY_WAR_Q13965_302350000	legacy-enterzone\n" +
		"23967	IDEternity_War_SensoryArea_Q13967a	IDETERNITY_WAR_Q13967_A_302350000	legacy-enterzone-name-match\n" +
		"24263	IDLF1_SensoryArea_MasterBoss01	IDLF1_QuestArea_MasterBoss01	retail-world-questscript-area\n" +
		"25084	DF5_SensoryArea_Q25084	DF5_SENSORYAREA_Q25084_220080000	legacy-enterzone\n" +
		"25400	AB1_SensoryArea_Q25400a	MIREN_ISLAND_400010000	legacy-enterzone\n" +
		"25551	DF6_SensoryArea_Q25551_AtoB	DF6_SENSORY_AREA_Q25551_A_TO_B_220110000	legacy-enterzone\n" +
		"25551	DF6_SensoryArea_Q25551_BtoA	DF6_SENSORY_AREA_Q25551_B_TO_A_220110000	legacy-enterzone\n" +
		"25552	DF6_SensoryArea_Q25552_AtoD	DF6_SENSORY_AREA_Q25552_A_TO_D_220110000	legacy-enterzone\n" +
		"25552	DF6_SensoryArea_Q25552_DtoA	DF6_SENSORY_AREA_Q25552_D_TO_A_220110000	legacy-enterzone\n" +
		"25553	DF6_SensoryArea_Q25553_AtoF	DF6_SENSORY_AREA_Q25553_A_TO_F_220110000	legacy-enterzone\n" +
		"25553	DF6_SensoryArea_Q25553_FtoA	DF6_SENSORY_AREA_Q25553_F_TO_A_220110000	legacy-enterzone\n" +
		"25554	DF6_SensoryArea_Q25554_AtoH	DF6_SENSORY_AREA_Q25554_A_TO_H_220110000	legacy-enterzone\n" +
		"25554	DF6_SensoryArea_Q25554_HtoA	DF6_SENSORY_AREA_Q25554_H_TO_A_220110000	legacy-enterzone\n" +
		"25601	DF6_SensoryArea_Q25601a_Dynamic_Env	DF6_SENSORY_AREA_Q25601_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25601	DF6_SensoryArea_Q25601c_Named	DF6_SENSORY_AREA_Q25601_B_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25602	DF6_SensoryArea_Q25602a_Dynamic_Env	DF6_SENSORY_AREA_Q25602_A_DYNAMIC_ENV_220110000	legacy-enterzone-name-match\n" +
		"25604	DF6_SensoryArea_Q25604a_Dynamic_Env	DF6_SENSORY_AREA_Q25604_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25605	DF6_SensoryArea_Q25605a_Dynamic_Env	DF6_SENSORY_AREA_Q25605_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25605	DF6_SensoryArea_Q25605c_Named	DF6_SENSORY_AREA_Q25605_B_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25606	DF6_SensoryArea_Q25606a	DF6_SENSORY_AREA_Q25606_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25606	DF6_SensoryArea_Q25606b_Dynamic_Env	DF6_SENSORY_AREA_Q25606_B_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25606	DF6_SensoryArea_Q25606c_Named	DF6_SENSORY_AREA_Q25606_C_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25608	DF6_SensoryArea_Q25608a_Dynamic_Env	DF6_SENSORY_AREA_Q25608_A_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"25608	DF6_SensoryArea_Q25608b_Named	DF6_SENSORY_AREA_Q25608_B_DYNAMIC_ENV_220110000	legacy-enterzone\n" +
		"26800	DF_Tower_SensoryArea_Q26800	DF_TOWER_SENSORY_AREA_Q26800_220120000	legacy-enterzone\n" +
		"26800	IDEternity_01_SensoryArea_Q16800	IDETERNITY_01_Q16800_301540000	legacy-enterzone\n" +
		"26821	IDEternity_02_SensoryArea_Q16821a	IDETERNITY_02_Q16821_A_301550000	legacy-enterzone\n" +
		"26821	IDEternity_02_SensoryArea_Q16821b	IDETERNITY_02_Q16821_B_301550000	legacy-enterzone\n" +
		"26822	IDEternity_02_SensoryArea_Q16822a	IDETERNITY_02_Q16822_A_301550000	legacy-enterzone\n" +
		"26822	IDEternity_02_SensoryArea_Q16822b	IDETERNITY_02_Q16822_B_301550000	legacy-enterzone\n" +
		"26822	IDEternity_02_SensoryArea_Q16822c	IDETERNITY_02_Q16822_C_301550000	legacy-enterzone\n" +
		"26822	IDEternity_02_SensoryArea_Q16822d	IDETERNITY_02_Q16822_D_301550000	legacy-enterzone\n" +
		"26823	IDEternity_02_SensoryArea_Q16823a	IDETERNITY_02_Q16823_A_301550000	legacy-enterzone\n" +
		"26823	IDEternity_02_SensoryArea_Q16823b	IDETERNITY_02_Q16823_B_301550000	legacy-enterzone\n" +
		"26827	IDEternity_02_SensoryArea_Q16827a	IDETERNITY_02_Q16827_A_301550000	legacy-enterzone\n" +
		"26830	IDEternity_02_SensoryArea_Q16830b	IDETERNITY_02_Q16830_B_301550000	legacy-enterzone-name-match\n" +
		"26831	IDEternity_02_SensoryArea_Q16831a	IDETERNITY_02_Q16831_A_301550000	legacy-enterzone\n" +
		"26831	IDEternity_02_SensoryArea_Q16831b	IDETERNITY_02_Q16831_B_301550000	legacy-enterzone\n" +
		"26831	IDEternity_02_SensoryArea_Q16831c	IDETERNITY_02_Q16831_C_301550000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836a	IDETERNITY_03_Q16836_A_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836b	IDETERNITY_03_Q16836_B_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836c	IDETERNITY_03_Q16836_C_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836d	IDETERNITY_03_Q16836_D_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836e	IDETERNITY_03_Q16836_E_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836f	IDETERNITY_03_Q16836_F_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836g	IDETERNITY_03_Q16836_G_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836h	IDETERNITY_03_Q16836_H_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836i	IDETERNITY_03_Q16836_I_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836j	IDETERNITY_03_Q16836_J_301560000	legacy-enterzone\n" +
		"26836	IDEternity_03_SensoryArea_Q16836k	IDETERNITY_03_Q16836_K_301560000	legacy-enterzone\n" +
		"26839	IDEternity_03_SensoryArea_Q16836g	IDETERNITY_03_Q16836_G_301560000	legacy-enterzone\n" +
		"26987	LF5_SensoryArea_Q26987	LF5_SENSORYAREA_Q26987_210070000	legacy-enterzone\n" +
		"27505	IDLDF5_Under_02_War_SensoryArea_Q17505a	IDLDF5_UNDER_02_WAR_ITEMUSEAREA_17505A	legacy-enterzone\n" +
		"28252	IDInfinity_SensoryArea_Q18252a	IDINFINITY_SENSORYAREA_Q18252A_302400000	legacy-enterzone\n" +
		"28253	IDInfinity_SensoryArea_Q18253a	IDINFINITY_SENSORYAREA_Q18253A_302400000	legacy-enterzone\n" +
		"20501	DF5_SensoryArea_Q20501a	DF5_SENSORYAREA_Q20501A_220080000	retail-world-sensory-area\n" +
		"20503	DF5_SensoryArea_Q20503a	DF5_SENSORYAREA_Q20503A_220080000	retail-world-sensory-area\n" +
		"20507	DF5_SensoryArea_Q20507a	DF5_SENSORYAREA_Q20507A_220080000	retail-world-sensory-area\n" +
		"17525	IDAbRe_Core_03_SensoryArea_Q17525	IDABRE_CORE_03_SENSORYAREA_Q17525_301720000	retail-world-sensory-area\n" +
		"27525	IDAbRe_Core_03_SensoryArea_Q17525	IDABRE_CORE_03_SENSORYAREA_Q17525_301720000	retail-world-sensory-area\n" +
		"16820	IDEternity_02_SensoryArea_Q16820a	IDETERNITY_02_SENSORYAREA_Q16820A_301550000	retail-world-sensory-area\n" +
		"26820	IDEternity_02_SensoryArea_Q16820a	IDETERNITY_02_SENSORYAREA_Q16820A_301550000	retail-world-sensory-area\n" +
		"18996	IDF6_LF1_SensoryArea_Q18996	IDF6_LF1_SENSORYAREA_Q18996_301660000	retail-world-sensory-area\n" +
		"28996	IDF6_LF1_SensoryArea_Q18996	IDF6_LF1_SENSORYAREA_Q18996_301660000	retail-world-sensory-area\n" +
		"17500	IDLDF5_Fortress_War_SensoryArea_Q17500a	IDLDF5_FORTRESS_WAR_SENSORYAREA_Q17500A_301680000	retail-world-sensory-area\n" +
		"27500	IDLDF5_Fortress_War_SensoryArea_Q17500a	IDLDF5_FORTRESS_WAR_SENSORYAREA_Q17500A_301680000	retail-world-sensory-area\n" +
		"15673	DF6_SensoryArea_Q15673	DF6_SENSORYAREA_Q15673_220110000	retail-world-sensory-area\n" +
		"25673	LF6_SensoryArea_Q25673	LF6_SENSORYAREA_Q25673_210100000	retail-world-sensory-area\n";

	private static final RetailEnterAreaZoneResolution EMPTY = new RetailEnterAreaZoneResolution(Map.of());
	private static volatile RetailEnterAreaZoneResolution defaultInstance;

	private final Map<Integer, Map<String, String>> zones;

	private RetailEnterAreaZoneResolution(Map<Integer, Map<String, String>> zones) {
		this.zones = zones;
	}

	/** 内存静态规范实例（83 个任务 146 个区域映射全覆盖）。 / Memory static canonical instance. */
	public static RetailEnterAreaZoneResolution defaultZoneResolution() {
		RetailEnterAreaZoneResolution instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailEnterAreaZoneResolution.class) {
				instance = defaultInstance;
				if (instance == null) {
					try {
						instance = load(new ByteArrayInputStream(TSV_CONTENT.getBytes(StandardCharsets.UTF_8)));
						defaultInstance = instance;
					} catch (IOException e) {
						throw new IllegalStateException("Failed to decode canonical enterarea zone resolution", e);
					}
				}
			}
		}
		return instance;
	}

	public static RetailEnterAreaZoneResolution load() {
		return defaultZoneResolution();
	}

	/** 空解析表（测试合成器用）。 / Empty table for tests. */
	public static RetailEnterAreaZoneResolution empty() {
		return EMPTY;
	}

	/** 全量区域别名映射（任务 ID → (别名 → 登记名)）。 / All zone alias mappings. */
	public Map<Integer, Map<String, String>> zones() {
		return zones;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the resolution TSV. */
	public static RetailEnterAreaZoneResolution load(InputStream input) throws IOException {
		if (input == null) {
			return defaultZoneResolution();
		}
		Map<Integer, Map<String, String>> parsed = new HashMap<>();
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
				parsed.computeIfAbsent(Integer.parseInt(parts[0].trim()), key -> new HashMap<>())
					.put(parts[1].trim(), parts[2].trim());
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed enterarea zone-resolution registry", e);
		}
		Map<Integer, Map<String, String>> frozen = new HashMap<>();
		parsed.forEach((questId, aliases) -> frozen.put(questId, Map.copyOf(aliases)));
		return new RetailEnterAreaZoneResolution(Map.copyOf(frozen));
	}

	/** 别名的登记区名；未登记返回 null（编译器以 RETAIL_ENTERAREA_ZONE_UNRESOLVED 拒绝）。
	 * The registered zone name for the alias; null when unresolvable (the compiler rejects). */
	public String zoneName(int questId, String alias) {
		Map<String, String> aliases = zones.get(questId);
		return aliases == null ? null : aliases.get(alias);
	}
}
