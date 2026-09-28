package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真端对话名组表内存规范视图（原 {@code retail-quest-ai-name-groups.tsv} 退役后转为内存静态规范）。
 * <p>
 * 守备队同组共用 ScriptDLL 对话名，组名解析为全组成员 name_desc 并打标。
 * <p>
 * Memory canonical view of the retail dialog-name group table.
 */
public final class RetailQuestAiNameGroups {

	private static final String TSV_CONTENT =
		"# 真端对话名组表（quest_ai_name → 成员 name_desc）——由 p0c52_quest_ai_name_groups.py 生成\n" +
		"# 依据：客户端 npc 块 <quest_ai_name> 的成员 id 集（第一手）+ 服务端 npc 模板（成员名与\n" +
		"# 共享 title_id）+ 客户端词典正文 STR_DIC_E_<名>（可选，存在时须与块 id 集一致）+ 遗留\n" +
		"# 生产 XML 的接取流 id 集（超集见证；多出的 id 必须解释为该组任务的交付 NPC）。\n" +
		"# 候选判据：DD 表引用的名字 ∧ 客户端声明 ≥2 成员 ∧ 组名不是任一成员自己的名字。\n" +
		"# quest_ai_name	member_name_descs\n" +
		"Ab1_BLv4_D01	Ab1_Buildup_Guard60_D,Ab1_Buildup_Guard61_D,Ab1_Buildup_Guard62_D\n" +
		"Ab1_BLv4_D02	Ab1_Buildup_Guard63_D,Ab1_Buildup_Guard64_D,Ab1_Buildup_Guard65_D\n" +
		"Ab1_BLv4_L01	Ab1_Buildup_Guard60_L,Ab1_Buildup_Guard61_L,Ab1_Buildup_Guard62_L\n" +
		"Ab1_BLv4_L02	Ab1_Buildup_Guard63_L,Ab1_Buildup_Guard64_L,Ab1_Buildup_Guard65_L\n" +
		"Ab1_BLv6_D01	Ab1_Buildup_Guard50_D,Ab1_Buildup_Guard52_D,Ab1_Buildup_Guard54_D\n" +
		"Ab1_BLv6_D02	Ab1_Buildup_Guard51_D,Ab1_Buildup_Guard53_D,Ab1_Buildup_Guard55_D\n" +
		"Ab1_BLv6_L01	Ab1_Buildup_Guard50_L,Ab1_Buildup_Guard52_L,Ab1_Buildup_Guard54_L\n" +
		"Ab1_BLv6_L02	Ab1_Buildup_Guard51_L,Ab1_Buildup_Guard53_L,Ab1_Buildup_Guard55_L\n" +
		"Ab1_BLv8_D	Ab1_Buildup_Guard56_D,Ab1_Buildup_Guard57_D,Ab1_Buildup_Guard58_D,Ab1_Buildup_Guard59_D\n" +
		"Ab1_BLv8_L	Ab1_Buildup_Guard56_L,Ab1_Buildup_Guard57_L,Ab1_Buildup_Guard58_L,Ab1_Buildup_Guard59_L\n" +
		"DF4_BountyHunter_Da	Shugo_DQ_001,Shugo_DQ_002\n" +
		"IDRaksha_Solo_StageStart	IDRaksha_Solo_StageStart_A,IDRaksha_Solo_StageStart_B,IDRaksha_Solo_StageStart_C\n" +
		"IDRaksha_Solo_StageStart_Dark	IDRaksha_Solo_StageStart_A_Dark,IDRaksha_Solo_StageStart_B_Dark,IDRaksha_Solo_StageStart_C_Dark\n" +
		"LDF4_Advance_Village_Guard_D_East	LDF4_Advance_Village_Guard08_D2,LDF4_Advance_Village_Guard03_D2,LDF4_Advance_Village_Guard02_D2\n" +
		"LDF4_Advance_Village_Guard_D_North	LDF4_Advance_Village_Guard12_D2,LDF4_Advance_Village_Guard10_D2,LDF4_Advance_Village_Guard01_D2\n" +
		"LDF4_Advance_Village_Guard_D_South	LDF4_Advance_Village_Guard13_D2,LDF4_Advance_Village_Guard11_D2,LDF4_Advance_Village_Guard05_D2\n" +
		"LDF4_Advance_Village_Guard_D_West	LDF4_Advance_Village_Guard04_D2,LDF4_Advance_Village_Guard07_D2,LDF4_Advance_Village_Guard06_D2\n" +
		"LDF4_Advance_Village_Guard_L_East	LDF4_Advance_Village_Guard08_L2,LDF4_Advance_Village_Guard03_L2,LDF4_Advance_Village_Guard02_L2\n" +
		"LDF4_Advance_Village_Guard_L_North	LDF4_Advance_Village_Guard13_L2,LDF4_Advance_Village_Guard10_L2,LDF4_Advance_Village_Guard01_L2\n" +
		"LDF4_Advance_Village_Guard_L_South	LDF4_Advance_Village_Guard12_L2,LDF4_Advance_Village_Guard05_L2,LDF4_Advance_Village_Guard11_L2\n" +
		"LDF4_Advance_Village_Guard_L_West	LDF4_Advance_Village_Guard04_L2,LDF4_Advance_Village_Guard07_L2,LDF4_Advance_Village_Guard06_L2\n" +
		"LF4_BountyHunter_Li	Rima,Socinus\n" +
		"NPC_event_goldstar	NPC_event_goldstar_l,NPC_event_goldstar_d\n" +
		"NPC_event_goldstar_master	NPC_event_goldstar_l_master,NPC_event_goldstar_d_master\n" +
		"NPC_event_idevent_s2	NPC_event_idevent_s2_L_seller,NPC_event_idevent_s2_D_seller\n" +
		"NPC_event_idevent_s3	NPC_event_idevent_s3_L_seller,NPC_event_idevent_s3_D_seller\n" +
		"event_npc_idsolo_s4	LC1_event_npc_idsolo_s4,DC1_event_npc_idsolo_s4\n" +
		"event_npc_idsolo_s5	LC1_event_npc_idsolo_s5,DC1_event_npc_idsolo_s5\n" +
		"event_npc_idsweep_splus	LC1_event_npc_idsweep_splus,DC1_event_npc_idsweep_splus\n" +
		"event_npc_miniring	LC1_event_npc_miniring,DC1_event_npc_miniring\n" +
		"npc_event_ppoba	npc_event_ppoba_l_01,npc_event_ppoba_d_01\n";

	private RetailQuestAiNameGroups() {
	}

	/** 获取内存规范流列表。 / Returns the canonical input streams. */
	public static List<InputStream> streams() {
		return List.of(new ByteArrayInputStream(TSV_CONTENT.getBytes(StandardCharsets.UTF_8)));
	}

	/** 获取全量声明的对话名组映射。 / Returns all declared dialog-name groups. */
	public static Map<String, List<String>> defaultGroups() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(TSV_CONTENT.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("	");
				if (parts.length == 2) {
					groups.put(parts[0].trim(), List.of(parts[1].trim().split(",")));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to parse retail quest ai name groups", e);
		}
		return Map.copyOf(groups);
	}
}
