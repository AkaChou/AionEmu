package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 10 的单步零售族「领奖行」合同，并按 owner 分轴（P3 重锚，计划 §8.9）。
 * <p>
 * 原断言全部落在 IR 形状上（reward 节点的 journal 投影行 = 1、无 source 的 enter-world 自愈边、
 * 领奖路由写行值）。SimpleTalk 切换批把这批里的 15 行移入 native 车道后 typed 定义退出生产视图，
 * 因此本测试改为双轴：
 * <ul>
 * <li><b>native 行</b>：断言真端表行（接取/交付 NPC、中继数、接取发放、步内发扣、交付门），并把
 * 「旧存档领奖行自愈」锚到 native 的进世界自愈（链形 vars=0 → 中继数；单步 vars=1 → 0）；</li>
 * <li><b>XML 保留行</b>：仍由 IR 车道拥有，逐条保留原来的 journal 行与自愈边断言。</li>
 * </ul>
 * Locks batch 10's "reward row" contract, split by owner (P3 re-anchor, plan §8.9). The 15 rows that
 * moved to the native lane are asserted on their retail rows plus the native enter-world heal; the rows
 * still owned by XML keep the original journal-row and recovery-edge assertions.
 */
class RetailSingleStepRewardRowContractTest {

	/** native 行的真端表事实（发放/回收为空即真端该列未声明）。 / Retail row facts of the native contracts. */
	private record NativeRow(int questId, int acquireNpc, int rewardNpc, int relayCount,
			Integer acceptGive, Integer stepGive, Integer stepRemove, List<SimpleTalkHandler.ItemStack> gate) {
	}

	private static final List<NativeRow> NATIVE_ROWS = List.of(
		new NativeRow(1526, 204555, 204555, 0, null, null, null,
			List.of(new SimpleTalkHandler.ItemStack(182201713, 10))),
		new NativeRow(1527, 204555, 205229, 1, 182201781, null, null, List.of()),
		new NativeRow(1528, 204553, 204583, 1, 182201778, null, null, List.of()),
		new NativeRow(1725, 278520, 278590, 1, 182202153, null, null, List.of()),
		new NativeRow(2135, 203532, 203532, 1, 182203131, null, 182203131, List.of()),
		new NativeRow(2247, 203645, 203645, 1, null, 182203231, null, List.of()),
		new NativeRow(2266, 203558, 203654, 1, 182203244, null, null, List.of()),
		new NativeRow(3087, 798201, 798144, 1, null, 182208063, null, List.of()),
		new NativeRow(4020, 205120, 205120, 1, 182209080, 182209081, 182209080, List.of()),
		new NativeRow(21455, 799404, 799244, 1, 182209514, 182209515, 182209514, List.of()),
		new NativeRow(1963, 203726, 203726, 1, 182206032, null, 182206032, List.of()),
		new NativeRow(1964, 203726, 203726, 1, 182206033, null, 182206033, List.of()),
		new NativeRow(18035, 730732, 804709, 1, 182213483, null, null, List.of()),
		new NativeRow(21458, 799249, 799249, 1, 182209518, null, 182209518, List.of()),
		new NativeRow(11455, 799070, 798946, 1, 182209503, 182209504, 182209503, List.of()));

	private record Contract(int questId, String group) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/** 仍由 XML 车道拥有的原合同行。P7 步 f 起 A/B 组（26838/80735/80736/16838/16977）已随 DD 1467 行
	 * 切到原生车道，其单步奖励行语义由 DataDrivenNativeRuntimeGateTest 承担；C 组 29002 仍是 XML 保留行。
	 * Contract rows still owned by XML. Since P7 step f the A/B groups moved to the native lane with the
	 * 1467 DD rows (their single-step reward-row semantics live in DataDrivenNativeRuntimeGateTest);
	 * group C (29002) remains an XML-retained row. */
	private static final List<Contract> XML_CONTRACTS = List.of(new Contract(29002, "C"));

	@Test
	void nativeRowsCarryTheirRetailItemChannels() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertTrue(handler.unresolvedItemSymbols().isEmpty(),
			() -> "原生物品符号面必须全解: " + handler.unresolvedItemSymbols());
		for (NativeRow row : NATIVE_ROWS) {
			int questId = row.questId();
			assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
			assertEquals(row.acquireNpc(), handler.acquireNpc(questId), "acquire npc: " + questId);
			assertEquals(row.rewardNpc(), handler.rewardNpc(questId), "reward npc: " + questId);
			assertEquals(row.relayCount(), handler.relayCount(questId), "relay count: " + questId);
			assertEquals(stack(row.acceptGive()), handler.acceptGiveItem(questId), "accept grant: " + questId);
			assertEquals(stack(row.stepGive()), handler.stepGiveItem(questId, 1), "step grant: " + questId);
			assertEquals(stack(row.stepRemove()), handler.stepRemoveItem(questId, 1), "step removal: " + questId);
			assertEquals(row.gate(), handler.workItems(questId), "hand-in gate: " + questId);
			assertFalse(handler.unresolvedGate(questId), "门不得 fail-closed: " + questId);
		}
	}

	/**
	 * 旧存档领奖行自愈：REWARD 态的异常行值在进世界时被修回真端投影行，且第二次进入零写入。
	 * Enter-world heal of stale reward rows: the native lane repairs the row value and stays idempotent.
	 */
	@Test
	void nativeRewardRowsSelfHealOnEnterWorld() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		for (NativeRow row : NATIVE_ROWS) {
			int questId = row.questId();
			int relays = handler.relayCount(questId);
			int staleVars = relays > 0 ? STALE_ROW : REWARD_ROW;
			Player player = createTestPlayer();
			player.getQuestStateList().addQuest(questId,
				new QuestState(questId, QuestStatus.REWARD, staleVars, 0, null, 0, null));

			assertTrue(handler.onEnterWorld(player), "quest " + questId + " 旧存档领奖行必须自愈");
			QuestState healed = player.getQuestStateList().getQuestState(questId);
			assertEquals(relays, healed.getQuestVars().getQuestVars(),
				"quest " + questId + " 必须自愈到真端投影行");
			assertFalse(handler.onEnterWorld(player), "quest " + questId + " 自愈必须幂等");
		}
	}

	@Test
	void xmlRetainedRowsKeepTheirJournalRowAndRecoveryContract() throws Exception {
		for (Contract contract : XML_CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();
			assertEquals(REWARD_ROW, rewardRow(definition),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(Map.of("var0", STALE_ROW), node(definition, "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state keeps journal row 0");

			QuestTransition recovery = recoveryRoute(definition);
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", STALE_ROW)), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", STALE_ROW)), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void xmlRetainedRewardRoutesNeverWriteAStaleJournalRow() throws Exception {
		for (Contract contract : XML_CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(REWARD_ROW, value, () -> "quest " + contract.questId()
							+ " route writes a non reward journal row");
					}
				}
			}
		}
	}

	private static SimpleTalkHandler.ItemStack stack(Integer itemId) {
		return itemId == null ? null : new SimpleTalkHandler.ItemStack(itemId, 1);
	}

	/** 极简测试玩家（无 DB、无网络栈），与本族 native 门禁同法。 / Minimal test player, same style as the native gates. */
	private static Player createTestPlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		setField(pcd, PlayerCommonData.class, "playerClass", PlayerClass.WARRIOR);
		setField(pcd, PlayerCommonData.class, "level", 20);
		setField(player, Player.class, "playerCommonData", pcd);
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static void setField(Object target, Class<?> declaring, String name, Object value) {
		try {
			Field field = declaring.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("test fixture failed: " + declaring.getSimpleName() + "." + name, e);
		}
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = node(definition, "reward");
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
