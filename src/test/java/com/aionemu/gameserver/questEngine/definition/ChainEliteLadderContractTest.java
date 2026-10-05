package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.RawQuestVarsCodec;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定精锐兵族 13918（天族）/ 23918（魔族）的链式 0/1 击杀阶梯。
 * <p>
 * 2026-10-05 重锚：两侧已随 P7 步 f 退役 XML、由 SimpleSerialHunt 原生车道直驱（无 IR），原 IR 判据
 * （SECTION 五槽位域、节点投影、修复边）随车道退场。现断言面 = 真端表行（count_first..fifth 五阶段
 * 各 1 杀、按序推进）× 原生处理器（owns/acquireNpc/rewardNpc/onKill/onDialog）× CameraRegistry
 * （宽度 SIX 的槽位编码）× 真端 quest.xml 固定奖励列。两处历史漂移仍在原生面锁定：QE-052 的
 * 「领奖 owner 不得由接取 NPC 兼任」、c44c50bd0 的「第三条固定 ITEM 索引丢失」。
 * <p>
 * Re-anchored 2026-10-05: both sides are retired XML driven natively by SimpleSerialHunt (no IR); the
 * assertions read the retail row (five 1-kill stages), the native handler faces, the CameraRegistry
 * slot coding (width SIX) and the retail quest.xml fixed reward columns.
 */
class ChainEliteLadderContractTest {

	/** 任务 / 镜像 / 接取 NPC / 行 5 NPC / 五只精锐兵。 */
	private record Contract(int questId, int mirrorId, int offerNpc, int reportNpc, List<Integer> kills) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(13918, 23918, 802328, 802350,
			List.of(235321, 235322, 235323, 235324, 235325)),
		new Contract(23918, 13918, 802347, 802353,
			List.of(235559, 235560, 235561, 235326, 235327)));

	private static final int GOLD = 451980;
	private static final int EXP = 7927072;
	private static final int ITEM = 169405255;
	private static final int ITEM_COUNT = 6;
	private static final String ITEM_NAME = "ac_material_id_S_PvE_M_61a";

	@Test
	void everyClientRowOwnsOneChainedCounterSlot() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		for (Contract contract : CONTRACTS) {
			assertTrue(RetiredQuestIds.contains(contract.questId()),
				() -> "quest " + contract.questId() + " must be retail-table owned");
			assertTrue(handler.owns(contract.questId()),
				() -> "quest " + contract.questId() + " must be owned by the native serial hunt handler");
			assertTrue(handler.routes(contract.questId()),
				() -> "quest " + contract.questId() + " must be routed by the native serial hunt handler");

			NativeQuestTableLoader.SimpleSerialHuntRow row = row(contract.questId());
			assertEquals(5, row.stages().size(),
				() -> "quest " + contract.questId() + " must declare the five chained stages");
			for (int index = 0; index < contract.kills().size(); index++) {
				NativeQuestTableLoader.SerialStage stage = row.stages().get(index);
				int slot = index + 1;
				int elite = contract.kills().get(index);
				assertEquals(slot, stage.stage(),
					() -> "quest " + contract.questId() + " ladder must stay in client row order");
				assertEquals(1, stage.count(),
					() -> "quest " + contract.questId() + " slot " + slot + " is a 0/1 counter");
				List<Integer> resolved = stage.monsters().stream()
					.flatMap(name -> resolver.resolveMonsterIds(name).stream())
					.toList();
				assertTrue(resolved.contains(elite),
					() -> "quest " + contract.questId() + " slot " + slot + " must resolve to elite " + elite);
			}

			/* 镜像同形：两侧五槽阶梯必须一致。 / Mirror shape: the five-stage ladder on both sides. */
			assertEquals(5, row(contract.mirrorId()).stages().size(),
				() -> "mirror quest " + contract.mirrorId() + " must keep the five-stage ladder");
		}
	}

	@Test
	void eachEliteAdvancesExactlyItsOwnSlot() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		CameraRegistry cameras = CameraRegistry.instance();
		for (Contract contract : CONTRACTS) {
			Player player = NativeTalkFixture.player();
			QuestState state = NativeTalkFixture.start(player, contract.questId());
			CameraRegistry.CameraRow camera = cameras.require(contract.questId());
			assertEquals(RawQuestVarsCodec.Width.SIX, camera.width(),
				() -> "quest " + contract.questId() + " must code the five slots in six-bit cells");
			int expected = 0;
			for (int index = 0; index < contract.kills().size(); index++) {
				int elite = contract.kills().get(index);
				int slot = index + 1;
				assertTrue(handler.onKill(player, elite),
					() -> "quest " + contract.questId() + " elite " + elite + " must count on its own slot");
				expected |= 1 << camera.width().shift(slot);
				assertEquals(expected, state.getQuestVars().getQuestVars(),
					() -> "quest " + contract.questId() + " elite " + elite + " must set exactly slot " + slot);
				assertEquals(index == contract.kills().size() - 1 ? QuestStatus.REWARD : QuestStatus.START,
					state.getStatus(),
					() -> "quest " + contract.questId() + " elite " + elite + " must only close on the fifth slot");
			}
			assertEquals(camera.fullValue(), state.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " must saturate all five slots");
		}
	}

	@Test
	void outOfOrderOrBackwardKillsHaveNoPlan() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		for (Contract contract : CONTRACTS) {
			Player player = NativeTalkFixture.player();
			QuestState state = NativeTalkFixture.start(player, contract.questId());
			/* 阶梯未到时击杀后续槽位的精英：零副作用（客户端只渲染当前欠的那只）。 */
			for (int index = 1; index < contract.kills().size(); index++) {
				int elite = contract.kills().get(index);
				assertFalse(handler.onKill(player, elite),
					() -> "quest " + contract.questId() + " elite " + elite + " must not count out of order");
				assertEquals(0, state.getQuestVars().getQuestVars(),
					() -> "quest " + contract.questId() + " out-of-order kill must not write state");
			}
			/* 顺序打完前四槽；重复击杀已满槽零副作用。 */
			for (int index = 0; index < 4; index++) {
				assertTrue(handler.onKill(player, contract.kills().get(index)));
			}
			int saturatedFirstFour = state.getQuestVars().getQuestVars();
			assertFalse(handler.onKill(player, contract.kills().get(0)),
				() -> "quest " + contract.questId() + " filled slot replay must not count");
			assertEquals(saturatedFirstFour, state.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " filled slot replay must not write state");
			/* 收口第 5 槽翻 REWARD；领奖态击杀不再计数。 */
			assertTrue(handler.onKill(player, contract.kills().get(4)));
			assertEquals(QuestStatus.REWARD, state.getStatus(),
				() -> "quest " + contract.questId() + " fifth kill must close the ladder");
			int rewardVars = state.getQuestVars().getQuestVars();
			assertFalse(handler.onKill(player, contract.kills().get(0)),
				() -> "quest " + contract.questId() + " kill in REWARD must not count");
			assertEquals(rewardVars, state.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " kill in REWARD must not write state");
		}
	}

	@Test
	void offerAndCompletionStayOnTheRowFiveNpc() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		for (Contract contract : CONTRACTS) {
			NativeQuestTableLoader.SimpleSerialHuntRow row = row(contract.questId());
			assertEquals(contract.offerNpc(), resolver.uniqueId(row.acquiredNpcName()),
				() -> "quest " + contract.questId() + " offer must stay on the row's acquired npc");
			assertEquals(contract.reportNpc(), resolver.uniqueId(row.rewardNpcName()),
				() -> "quest " + contract.questId() + " report must stay on the row-5 npc");
			assertEquals(contract.offerNpc(), handler.acquireNpc(contract.questId()));
			assertEquals(contract.reportNpc(), handler.rewardNpc(contract.questId()));
			assertNotEquals(contract.offerNpc(), contract.reportNpc(),
				() -> "quest " + contract.questId() + " offer NPC must not own the report or completion (QE-052)");

			/* 完成只在行 5 NPC：REWARD 态的领奖窗只由交付 NPC 应答，接取 NPC 零应答。 */
			/* Completion stays on the row-5 NPC: the claim window answers there and nowhere else. */
			Player player = NativeTalkFixture.player();
			NativeTalkFixture.add(player, contract.questId(), QuestStatus.REWARD, 0);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, contract.reportNpc(),
				contract.questId(), 31)),
				() -> "quest " + contract.questId() + " claim window must answer on the row-5 npc");
			assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, contract.offerNpc(),
				contract.questId(), 31)),
				() -> "quest " + contract.questId() + " offer npc must not answer the claim window");
		}
	}

	@Test
	void completionGrantsTheFullFixedRewardSet() throws Exception {
		RetailItemNameIndex itemIndex = RetailItemNameIndex.loadItemTemplates();
		for (Contract contract : CONTRACTS) {
			NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().require(contract.questId());
			assertEquals(String.valueOf(EXP), row.text("reward_exp1"),
				() -> "quest " + contract.questId() + " fixed EXP column");
			assertEquals(String.valueOf(GOLD), row.text("reward_gold1"),
				() -> "quest " + contract.questId() + " fixed GOLD column");
			/* fixed-reward-indices 必须覆盖第三条 ITEM（c44c50bd0 把 13918/23918 的 0 1 2 改成 0 1 过）。 */
			/* The third fixed ITEM entry must survive (c44c50bd0 wrongly dropped it). */
			assertEquals(ITEM_NAME + " " + ITEM_COUNT, row.text("reward_item1_1"),
				() -> "quest " + contract.questId() + " must keep the third fixed ITEM entry");
			assertEquals(ITEM, itemIndex.resolve(ITEM_NAME),
				() -> "quest " + contract.questId() + " ITEM symbol must resolve to the production id");
		}
	}

	@Test
	void legacyStepModelSavesAreNotRewrittenByTheRetailShape() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		for (Contract contract : CONTRACTS) {
			/* 退役行无 IR：EnterWorld 修复边在结构上不可能存在（IR 退役清扫口径）。 */
			/* The retired rows have no IR: an enter-world repair edge cannot exist structurally. */
			assertTrue(RetiredQuestIds.contains(contract.questId()));
			IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> ProductionQuestDefinitions.definition(contract.questId()),
				() -> "quest " + contract.questId() + " must have no IR definition");
			assertTrue(failure.getMessage() != null
					&& failure.getMessage().contains("missing production quest definition"),
				() -> "quest " + contract.questId() + " must fail closed as a retired row");

			/* 旧 step 模型的存档（var0=killsDone）不被阶梯"修复"：槽 1 视为已满、原值原样保留。 */
			/* The legacy step-model save is not healed: slot 1 reads full and the raw value is kept. */
			Player player = NativeTalkFixture.player();
			QuestState stale = NativeTalkFixture.start(player, contract.questId());
			stale.getQuestVars().setVar(5);
			assertFalse(handler.onKill(player, contract.kills().get(0)),
				() -> "quest " + contract.questId() + " legacy save must not be rewritten");
			assertEquals(5, stale.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " legacy save must stay untouched");
		}
	}

	@Test
	void saturatedRewardStateStillOpensTheClientRewardWindow() {
		SimpleSerialHuntHandler handler = SimpleSerialHuntHandler.instance();
		for (Contract contract : CONTRACTS) {
			Player player = NativeTalkFixture.player();
			QuestState state = NativeTalkFixture.add(player, contract.questId(), QuestStatus.REWARD, 0);
			state.getQuestVars().setVar(CameraRegistry.instance().require(contract.questId()).fullValue());

			/* 正规领奖态（五槽全 1）：领奖窗必须仍被行 5 NPC 打开。 */
			/* The legitimate saturated reward state must still open the reward window. */
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, contract.reportNpc(),
				contract.questId(), 31)),
				() -> "quest " + contract.questId() + " must open the reward window from REWARD");
			int rewardVars = state.getQuestVars().getQuestVars();
			assertFalse(handler.onKill(player, contract.kills().get(0)),
				() -> "quest " + contract.questId() + " must not count in REWARD");
			assertEquals(rewardVars, state.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " must not rewrite the reward projection");

			/* 旧 step 领奖投影（var0=5）同样不被"修复"：原生面没有 heal 通道。 */
			/* The stale reward projection is not healed either: the native lane has no heal channel. */
			QuestState stale = NativeTalkFixture.add(player, contract.questId(), QuestStatus.REWARD, 0);
			stale.getQuestVars().setVar(5);
			assertFalse(handler.onKill(player, contract.kills().get(0)),
				() -> "quest " + contract.questId() + " must not heal the legacy reward projection");
			assertEquals(5, stale.getQuestVars().getQuestVars(),
				() -> "quest " + contract.questId() + " legacy reward projection must stay untouched");
		}
	}

	private static NativeQuestTableLoader.SimpleSerialHuntRow row(int questId) {
		return NativeQuestTableLoader.instance().requireSerial(questId);
	}
}
