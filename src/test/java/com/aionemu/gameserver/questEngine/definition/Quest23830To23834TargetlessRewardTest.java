package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 23830-23834（魔族升级型支援品五连，真端 LevelUpLogIn + ItemPlay 行）的接取等级、
 * 发放文档与 11 职业奖励梯。
 * Locks quests 23830-23834's (retail LevelUpLogIn + ItemPlay rows) acquire levels, granted documents
 * and the eleven-class reward ladder.
 * <p>
 * 已退役（保留清单 owner=RETAIL_TABLE，family=DataDriven）：旧 typed planner 断言（每职业路由优先级、
 * QuestDialog 双协议、after-commit 分形）随 P7 步 f 退场，其引擎层语义由 DD 运行时与领奖结算体承担；
 * 按计划 §8.9（P3 重锚口径）改锚 DD 运行时公共面：等级接取面（acquireLevelInterests）、发放文档的
 * 使用进度面（questsReferencingItem）、真端元数据的 11 职业奖励梯与领取面。
 * <p>
 * Re-anchored (plan §8.9) to the DD runtime faces: the level-acquire interest, the granted document's
 * item-play interest, the retail metadata's eleven-class reward ladder and the reward-window face.
 */
class Quest23830To23834TargetlessRewardTest {
	private static final int REWARD_NPC_ID = 204061;
	/**
	 * 真端 class tag（{@code *_selectable_reward} 族）→ 生产职业名的登记键；元数据 classRewards 键 =
	 * 真端 tag 大写形。框内对应关系（与 p3 class 轴审计一致）：fighter=GLADIATOR、knight=TEMPLAR、
	 * wizard=SORCERER、elementalist=SPIRIT_MASTER、priest=CLERIC（cleric/priest 互换已复核）。
	 * <p>
	 * Retail class tags (the {@code *_selectable_reward} family); the metadata classRewards keys are the
	 * uppercased retail tags: fighter=GLADIATOR, knight=TEMPLAR, wizard=SORCERER,
	 * elementalist=SPIRIT_MASTER, priest=CLERIC (the cleric/priest swap is audited).
	 */
	private static final List<String> CLASS_TAGS = List.of(
		"FIGHTER", "KNIGHT", "RANGER", "ASSASSIN", "WIZARD", "ELEMENTALIST", "PRIEST", "CHANTER",
		"GUNSLINGER", "SONGWEAVER", "AETHERTECH");

	private static final List<Spec> SPECS = List.of(
		new Spec(23830, 30, 182216123, 46544, List.of(
			140001109, 140001130, 140001168, 140001145, 140001189, 140001202, 140001236, 140001221,
			140001253, 140001289, 140001271)),
		new Spec(23831, 40, 182216124, 337255, List.of(
			140001111, 140001128, 140001171, 140001142, 140001186, 140001203, 140001240, 140001220,
			140001254, 140001290, 140001275)),
		new Spec(23832, 45, 182216125, 555019, List.of(
			140001105, 140001123, 140001154, 140001136, 140001177, 140001196, 140001231, 140001216,
			140001249, 140001284, 140001264)),
		new Spec(23833, 50, 182216126, 731094, List.of(
			140001103, 140001124, 140001156, 140001137, 140001176, 140001198, 140001228, 140001214,
			140001247, 140001282, 140001265)),
		new Spec(23834, 55, 182216127, 1005193, List.of(
			140001118, 140001135, 140001173, 140001151, 140001192, 140001210, 140001245, 140001227,
			140001262, 140001296, 140001279)));

	@Test
	void retailRowsCarryTheLevelUpAcquireTheDocumentAndTheClassLadder() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(NativeNpcNameResolver.instance().resolveMembers("Aud").contains(REWARD_NPC_ID),
			"交付 NPC 名必须解析到 204061");
		DataDrivenQuestTable table = DataDrivenQuestTable.load(
			Quest23830To23834TargetlessRewardTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));

		for (Spec spec : SPECS) {
			assertTrue(runtime.owns(spec.questId()), spec.questId() + " 必须由 DD 运行时拥有");
			assertTrue(runtime.routes(spec.questId()), spec.questId() + " 必须由 DD 运行时路由");

			DataDrivenQuestTable.Row row = table.find(spec.questId()).orElseThrow();
			assertEquals("leveluplogin", row.acquireKind(), spec.questId() + " 接取类别 = 真端 LevelUpLogIn");
			assertEquals(String.valueOf(spec.level()), row.acquireParam(), spec.questId() + " 接取等级");
			assertEquals("Aud", row.rewardNpc(), spec.questId() + " 交付 NPC 名 = 真端 reward_npc_name");

			assertTrue(runtime.acquireLevelInterests().getOrDefault(spec.level(), List.of())
				.contains(spec.questId()), spec.questId() + " 必须注册在等级 " + spec.level() + " 的接取面");
			assertTrue(runtime.questsReferencingItem(spec.documentItem()).contains(spec.questId()),
				spec.questId() + " 的发放文档必须注册使用进度面");

			QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
				.retailMetadataOf(spec.questId()).orElseThrow().metadata();
			assertEquals(spec.level(), metadata.minLevel(), spec.questId() + " 真端 minlevel_permitted");
			assertEquals(java.util.Set.of("ASMODIANS"), metadata.permittedRaces(), "真端 pc_dark");
			assertTrue(metadata.rewards().contains(new QuestReward("EXP", 0, spec.exp())),
				() -> spec.questId() + " 奖励 " + metadata.rewards());

			// 11 职业奖励梯：真端 tag 键 → 首个奖励物品 id（真端 *_selectable_reward 族）。
			Map<String, Integer> expectedLadder = new LinkedHashMap<>();
			for (int index = 0; index < CLASS_TAGS.size(); index++) {
				expectedLadder.put(CLASS_TAGS.get(index), spec.classRewardIds().get(index));
			}
			Map<String, Integer> actualLadder = new LinkedHashMap<>();
			metadata.classRewards().forEach((className, rewards) ->
				actualLadder.put(className, rewards.get(0).id()));
			assertEquals(expectedLadder, actualLadder, spec.questId() + " 职业奖励梯");
		}
	}

	@Test
	void documentPlayStepAndTheDeliveryFaceBoundaryFollowTheRetailRow() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		Spec spec = SPECS.get(0);

		// 行数据：单步 ItemPlay（发放文档 = 进度载荷）；ItemPlay 可达性门由
		// DataDrivenItemPlayGrantGateTest 承担。
		DataDrivenQuestTable table = DataDrivenQuestTable.load(
			Quest23830To23834TargetlessRewardTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));
		DataDrivenQuestTable.Row row = table.find(spec.questId()).orElseThrow();
		assertEquals(1, row.steps().size(), spec.questId() + " 单步进度行");
		assertEquals(DataDrivenQuestTable.Kind.ITEM_PLAY, row.steps().get(0).kind(),
			spec.questId() + " 进度类别 = 真端 ItemPlay");

		// 交付面（真端**所有行**恒建对象 #2 `reward_npc_name`，槽 +0x238 = `FUN_180c473e0`，
		// P7-STEP2E1 §1/§7）：交付面不限于 Talk 接取行——LevelUpLogIn 行同样注册。2026-10-07 实机
		// 13830：客户端任务书写明「在任务窗点击[领取奖励]% 或 和[奥尔佩]%对话」；旧断言
		//（「LevelUpLogIn 行不得注册 Talk 交付面」）与真端原码证据冲突，已按真端修正。
		// The delivery face registers for every row (the always-built retail object #2), not only
		// Talk-acquired rows — corrected against the retail source evidence.
		assertTrue(runtime.reportTalkInterests().getOrDefault(REWARD_NPC_ID, List.of()).contains(spec.questId()),
			spec.questId() + " 必须注册在交付 NPC（Aud " + REWARD_NPC_ID + "）的交付面");
	}

	/** 一条真端行的锚点事实。 / The anchor facts of one retail row. */
	private record Spec(int questId, int level, int documentItem, int exp, List<Integer> classRewardIds) {
	}
}
