package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * DD 击杀路由的变体族合同门（QE-048 回归，2026-10-08 实机 15546/15500/15503/15640/17510 报障）：
 * 原版 DD 表 hunt 名单写「代表名」（base/最低级），世界实刷同族变体——Iluma 只刷
 * {@code LF6_T_*} 变体（base 模板 0 刷新）、变身副本（302100000）按玩家等级刷
 * {@code IDTransform_Sado_*_66..75_An} 全族。精确解析只命中代表名 ⇒ 实刷成员击杀零路由、
 * 进度恒 0。修复后 hunt 兴趣面必须覆盖客户端 {@code quest_monster.csv} 合同行（QE-048/QE-125
 * 裁定：客户端计数为权威）解析出的全部实存模板；对话/接取/物件等非击杀面保持精确解析。
 * <p>
 * Gate for the DD kill-routing variant family (QE-048 regression; live 2026-10-08 reports for
 * 15546/15500/15503/15640/17510): retail DD hunt payloads name a representative while the world
 * spawns the family variants. After the fix the kill interest face must cover every template the
 * client {@code quest_monster.csv} contract rows resolve to; non-kill faces stay exact.
 */
class DataDrivenHuntVariantFamilyGateTest {

	/** 客户端怪物进度合同（QE-048/QE-125：客户端计数权威）。 / The client monster-progress contract. */
	private static final String CONTRACT_RESOURCE = "/aion/definitions/quest_monster/quest_monster.csv";

	/** 待验任务的 DD hunt 代表名（原版表原样，组内逗号分隔）。 / The DD payload names under test. */
	private static final Map<Integer, List<String>> PAYLOAD_NAMES = Map.of(
		15546, List.of("LF6_ElementalLightF_A_66_n", "LF6_Daru_A_66_n", "LF6_Popoku_As_A2_67_n",
			"LF6_WoodTesinon_A2_67_n"),
		15500, List.of("LF6_ElementalLightF_A_66_n", "LF6_NewMerman_A_66_n", "LF6_Trico_A_66_n",
			"LF6_Baku_A_66_n", "LF6_Daru_A_66_n"),
		15503, List.of("LF6_Elementallight_m_spring_A2_67_n", "LF6_Vespa_A2_67_n", "LF6_Dionaea_A2_67_n",
			"LF6_Trico_A2_67_n", "LF6_Popoku_As_A2_67_n", "LF6_WoodTesinon_A2_67_n", "LF6_NewMerman_A2_67_n"),
		15640, List.of("LF6_Mimic_C_67_d", "LF6_empty_C_67_d", "LF6_Tauric_C_67_d", "LF6_stropoku_E_67_d",
			"LF6_Zaif_E_67_d", "LF6_Neuth_E_67_d", "LF6_Kuillus_G_67_d", "LF6_babymosbear_G_67_d",
			"LF6_Lizardman_Pr_G_67_d", "LF6_Bat_Devil_I_67_d", "LF6_Rottentree_I_67_d", "LF6_Lepisma_I_67_d"),
		17510, List.of("IDTransform_Sado_Fi_66_An", "IDTransform_Sado_As_66_An", "IDTransform_Sado_Wi_66_An",
			"IDTransform_Sado_Pr_66_An", "IDTransform_Boss_TypeA_66_Ae", "IDTransform_Boss_TypeB_66_Ae",
			"IDTransform_Boss_TypeC_66_Ae", "IDTransform_Boss_TypeD_66_Ae"));

	private static NativeNpcNameResolver resolver;

	private static DataDrivenNativeRuntime runtime;

	@BeforeAll
	static void loadFixtures() {
		resolver = NativeNpcNameResolver.instance();
		runtime = DataDrivenNativeRuntime.instance();
	}

	/** 族键形态学：base 与 {@code T_} 变体、相邻等级同族；无等级尾段的名字自族不归并。 */
	@Test
	void familyKeysFoldBaseTaskVariantsAndLevels() {
		assertEquals("lf6_daru_a", NativeNpcNameResolver.monsterFamilyKey("LF6_Daru_A_66_n"));
		assertEquals("lf6_daru_a", NativeNpcNameResolver.monsterFamilyKey("LF6_T_Daru_A_66_n"));
		assertEquals("lf6_daru_a", NativeNpcNameResolver.monsterFamilyKey("LF6_T_Daru_A_67_n"));
		assertEquals("idtransform_sado_fi", NativeNpcNameResolver.monsterFamilyKey("IDTransform_Sado_Fi_66_An"));
		assertEquals("idtransform_boss_typea", NativeNpcNameResolver.monsterFamilyKey("IDTransform_Boss_TypeA_66_Ae"));
		// 无等级尾段：不剥段、不归并（NPC 名不进怪物变体族）。
		// No level tail: nothing strips, nothing merges (NPC names never join a monster family).
		assertEquals("lf6_aquaris_e", NativeNpcNameResolver.monsterFamilyKey("LF6_Aquaris_E"));
		// 纯等级尾（无 tag）也剥。 / A bare level tail (no tag) strips too.
		assertEquals("idtransform_boss_room", NativeNpcNameResolver.monsterFamilyKey("IDTransform_Boss_room_66"));
	}

	/** 精确通道语义不变：其他车道（对话/接取/物件）拿到的仍是代表名自身。 */
	@Test
	void exactChannelStaysRepresentativeOnly() {
		assertEquals(List.of(240483), resolver.resolveMonsterIds("LF6_Daru_A_66_n"));
		assertEquals(List.of(244454), resolver.resolveMonsterIds("IDTransform_Sado_Fi_66_An"));
	}

	/** 族展开覆盖实刷变体：base + {@code T_} 变体 + 相邻等级全部并入。 */
	@Test
	void familyResolutionCoversTheSpawnedVariants() {
		Set<Integer> daru = Set.copyOf(resolver.resolveMonsterFamilyIds("LF6_Daru_A_66_n"));
		assertTrue(daru.containsAll(Set.of(240483, 241664, 241665)),
			"daru 族必须含 base 240483 与实刷 T_ 变体 241664/241665: " + daru);
		Set<Integer> sado = Set.copyOf(resolver.resolveMonsterFamilyIds("IDTransform_Sado_Fi_66_An"));
		assertTrue(sado.containsAll(Set.of(244454, 244495, 244536, 244577, 244618, 244659, 244700, 244741,
			244782, 244823)), "sado_fi 族必须含 66..75 全等级: " + sado);
		// 负例：族外模板不得混入（body 不同的 trico、bosstype 不同名）。
		// Negative: out-of-family templates never join (different body/type).
		assertFalse(daru.contains(241660), "trico 不属于 daru 族");
		Set<Integer> bossA = Set.copyOf(resolver.resolveMonsterFamilyIds("IDTransform_Boss_TypeA_66_Ae"));
		assertFalse(bossA.contains(244492), "TypeB 不属于 TypeA 族");
	}

	/**
	 * 合同对拍：每任务 hunt 族解析并集必须覆盖客户端合同行解析出的全部实存模板；
	 * 17510 的族拆解与合同同构（恰好相等，验证不误扩）。
	 * Contract reconciliation: the per-quest family union must cover every template the client
	 * contract rows resolve to; 17510 must be exactly equal (no over-expansion).
	 */
	@Test
	void familyUnionCoversTheClientContractRows() {
		for (Map.Entry<Integer, List<String>> entry : PAYLOAD_NAMES.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> contract = contractIds(questId);
			assertFalse(contract.isEmpty(), "任务 " + questId + " 的客户端合同行必须非空");
			Set<Integer> family = new LinkedHashSet<>();
			for (String name : entry.getValue()) {
				List<Integer> ids = resolver.resolveMonsterFamilyIds(name);
				assertFalse(ids.isEmpty(), () -> "DD 名单名必须可解析: " + questId + " " + name);
				family.addAll(ids);
			}
			assertTrue(family.containsAll(contract), () -> "任务 " + questId + " 的击杀族必须覆盖客户端合同: 缺 "
				+ new LinkedHashSet<>(contract.stream().filter(id -> !family.contains(id)).toList()));
			if (questId == 17510) {
				assertEquals(contract, family, "17510 的族拆解必须与客户端合同同构（不误扩不漏）");
			}
		}
	}

	/** 生产击杀兴趣面必须登记实刷变体（241664=T_Daru 66、244495=Sado_Fi 67、244491=Boss_TypeA 66）。 */
	@Test
	void productionKillFaceRegistersTheSpawnedVariants() {
		assertTrue(runtime.owns(15546), "15546 属 DD 原生车道");
		assertTrue(runtime.owns(17510), "17510 属 DD 原生车道");
		assertKillFace(241664, 15546);
		assertKillFace(241665, 15546);
		assertKillFace(241676, 15546);
		assertKillFace(241679, 15546);
		assertKillFace(244454, 17510);
		assertKillFace(244495, 17510);
		assertKillFace(244491, 17510);
		assertKillFace(244863, 17510);
	}

	/** 实机故障行为复演：杀实刷变体（241664）必须推进 15546 组计数（修复前恒 false）。 */
	@Test
	void killingTheSpawnedVariantAdvancesTheCounter() {
		Player player = playerWithQuest(15546);
		QuestState state = player.getQuestStateList().getQuestState(15546);
		assertTrue(runtime.onKill(player, 241664), "击杀实刷 T_ 变体必须命中路由");
		assertTrue(state.getQuestVars().getQuestVars() != 0, "组计数必须落写 vars");
	}

	private static void assertKillFace(int npcId, int questId) {
		List<DataDrivenNativeRuntime.StepHit> hits = runtime.killInterests().get(npcId);
		assertFalse(hits == null || hits.isEmpty(), () -> "实刷变体 " + npcId + " 必须在击杀兴趣面");
		boolean routed = hits.stream().anyMatch(hit -> hit.questId() == questId);
		assertTrue(routed, () -> "实刷变体 " + npcId + " 必须路由到任务 " + questId + ": " + hits);
	}

	/** 合同行名单（monsters_gathers_npcs_list）逐名精确解析的并集；不可解析名（模板不存在）跳过。 */
	private static Set<Integer> contractIds(int questId) {
		String csv;
		try (InputStream input = DataDrivenHuntVariantFamilyGateTest.class.getResourceAsStream(CONTRACT_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("客户端合同表不在 classpath: " + CONTRACT_RESOURCE);
			}
			csv = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("客户端合同表不可读: " + CONTRACT_RESOURCE, e);
		}
		Set<Integer> ids = new LinkedHashSet<>();
		for (String line : csv.split("\n")) {
			String[] columns = line.split(",", -1);
			if (columns.length < 7 || !String.valueOf(questId).equals(columns[0].strip())) {
				continue;
			}
			// 名单从第 7 列起每列一名（列即逗号分隔），一直延续到行尾。
			// The list starts at column 7, one name per column, running to end of line.
			for (int col = 6; col < columns.length; col++) {
				String trimmed = columns[col].strip();
				if (trimmed.isEmpty()) {
					continue;
				}
				ids.addAll(resolver.resolveMonsterIds(trimmed));
			}
		}
		return ids;
	}

	/** Objenesis 玩家夹具（与 {@link DataDrivenNativeRuntimeGateTest} 同型）。 / Player fixture. */
	private static Player playerWithQuest(int questId) {
		Player player = new org.objenesis.ObjenesisStd().newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(10002);
		commonData.setRace(Race.ELYOS);
		commonData.setGender(Gender.MALE);
		try {
			java.lang.reflect.Field field = Player.class.getDeclaredField("playerCommonData");
			field.setAccessible(true);
			field.set(player, commonData);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		QuestStateList states = new QuestStateList();
		states.addQuest(questId, new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null));
		player.setQuestStateList(states);
		return player;
	}
}
