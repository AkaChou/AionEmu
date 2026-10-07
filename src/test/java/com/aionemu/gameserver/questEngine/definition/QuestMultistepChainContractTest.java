package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定「客户端任务书逐行对话链」批量修复合同（12 个任务；1183 见下）。
 * Locks the batch repair contract for client journal row-by-row talk chains (12 quests; 1183 below).
 * <p>这一族的原始缺陷是同一形状：客户端 quest_summary 逐行列出 n 个步骤（末行领奖），
 * 但服务端把整条链压成一个 {@code started(var0=0)} 状态，所有步骤 NPC 共用同一个
 * {@code HACTION_SETPRO1} 并直接跳 reward。玩家跟第 1 个 NPC 说完话就能领奖，第 2..n 行永远不可达；
 * wiki 侧的表现是每一步的 GM 命令都等于 {@code //quest set <id> START 0}。
 * The defect family shares one shape: the client journal lists n rows (last one rewards) while the
 * server compressed the chain into a single {@code started(var0=0)} state whose NPCs all shared one
 * {@code HACTION_SETPRO1} and jumped straight to reward, so row 2..n were unreachable and every wiki
 * GM command collapsed to {@code //quest set <id> START 0}.</p>
 * <p>修复后的合同与已提交的 1192 一致：每行一个 START 状态（var0 = 行号 0..n-1），末行另有同 var0 的
 * REWARD 状态；第 i 行由该行客户端 NPC 的 {@code SETPROi} 推进到下一行状态，领奖行由自己的客户端
 * {@code SELECT_QUEST_REWARD} 进入 REWARD，入口页取自该行客户端链；只有领奖行 NPC 能完成任务。
 * The repaired contract matches the committed 1192: one START state per row (var0 = row index 0..n-1) plus a
 * REWARD state on the last var0; row i advances through that row's client NPC with {@code SETPROi}, the reward
 * row enters REWARD through its own client {@code SELECT_QUEST_REWARD}, entry pages come from the client chain,
 * and only the reward row NPC may complete the quest.</p>
 * <p>P3 重锚（计划 §8.9）：1183 属真端 SimpleTalk 表行（{@code 엘람족}），随 P3 切换批移出 IR 车道；
 * 本类对它的断言改为真端表行锚（接取/交付 NPC 同主、两步中继树、步内发扣、页阶梯取自客户端链），
 * 其余 12 个任务未在真端表内，仍按 IR 合同断言。
 * P3 re-anchor (plan §8.9): 1183 is a retail SimpleTalk row and left the IR lane with the P3 switch
 * batch; it is now pinned through the retail row (same-NPC accept/hand-in, two relay trees, per-step
 * grants/removals, client-chain page ladder), while the remaining 12 quests stay on the IR contract.</p>
 */
class QuestMultistepChainContractTest {
	private record Step(int npcId, QuestDialogPage page) {
	}

	private record Chain(int questId, int rows, List<Step> steps, int rewardNpc, QuestDialogPage rewardPage) {
	}

	/**
	 * 逐任务的三方核对结果（客户端 quest_summary 行 → 客户端对话链 → retail 步骤），
	 * 由 .agents/summary/quest-multistep-contract-batch 的 dump_chain_evidence.py 导出。
	 * Per-quest three-way evidence (client quest_summary rows, client dialog chains, retail steps).
	 */
	/**
	 * 已随 P3 SimpleTalk 切换批移出 IR 的真端行（10 个）：接取/交付 owner 与中继 NPC 取自真端表，
	 * 步页取自客户端对话链；证据见 .agents/summary/quest-engine-native/p3/step5-anchor-evidence.tsv。
	 * Retail rows that left the IR lane with the P3 SimpleTalk switch batch (10 quests): owners and relay
	 * NPCs come from the retail row, step pages from the client dialog chain.
	 */
	private static final List<Chain> RETAIL_TALK_CHAINS = List.of(
		new Chain(1183, 3, List.of(
			new Step(730013, QuestDialogPage.SELECT2),
			new Step(730014, QuestDialogPage.SELECT3)), 730012, QuestDialogPage.SELECT5),
		new Chain(1483, 3, List.of(
			new Step(203940, QuestDialogPage.SELECT2),
			new Step(203944, QuestDialogPage.SELECT3)), 798127, QuestDialogPage.SELECT5),
		new Chain(1721, 3, List.of(
			new Step(278503, QuestDialogPage.SELECT2),
			new Step(278502, QuestDialogPage.SELECT3)), 278518, QuestDialogPage.SELECT5),
		new Chain(1724, 3, List.of(
			new Step(278591, QuestDialogPage.SELECT2),
			new Step(278599, QuestDialogPage.SELECT3)), 278594, QuestDialogPage.SELECT5),
		new Chain(2646, 4, List.of(
			new Step(204777, QuestDialogPage.SELECT2),
			new Step(204700, QuestDialogPage.SELECT3),
			new Step(204702, QuestDialogPage.SELECT4)), 204817, QuestDialogPage.SELECT5),
		new Chain(2692, 4, List.of(
			new Step(204108, QuestDialogPage.SELECT2),
			new Step(279027, QuestDialogPage.SELECT3),
			new Step(279029, QuestDialogPage.SELECT4)), 212164, QuestDialogPage.SELECT5),
		new Chain(2767, 3, List.of(
			new Step(279026, QuestDialogPage.SELECT2),
			new Step(279061, QuestDialogPage.SELECT3)), 279004, QuestDialogPage.SELECT5),
		new Chain(3966, 4, List.of(
			new Step(203994, QuestDialogPage.SELECT2),
			new Step(204030, QuestDialogPage.SELECT3),
			new Step(204568, QuestDialogPage.SELECT4)), 798391, QuestDialogPage.SELECT5),
		new Chain(3968, 4, List.of(
			new Step(798176, QuestDialogPage.SELECT2),
			new Step(204528, QuestDialogPage.SELECT3),
			new Step(203927, QuestDialogPage.SELECT4)), 798390, QuestDialogPage.SELECT5),
		new Chain(4501, 3, List.of(
			new Step(204340, QuestDialogPage.SELECT2),
			new Step(204348, QuestDialogPage.SELECT3)), 204728, QuestDialogPage.SELECT5));

	private static final List<Chain> CHAINS = List.of(
		new Chain(1319, 9, List.of(
			new Step(203923, QuestDialogPage.SELECT2),
			new Step(203910, QuestDialogPage.SELECT3),
			new Step(203906, QuestDialogPage.SELECT4),
			new Step(203915, QuestDialogPage.SELECT5),
			new Step(203907, QuestDialogPage.SELECT6),
			new Step(798050, QuestDialogPage.SELECT7),
			new Step(798049, QuestDialogPage.SELECT8),
			new Step(205240, QuestDialogPage.SELECT9)), 203908, QuestDialogPage.SELECT10),
		new Chain(2449, 2, List.of(
			new Step(798115, QuestDialogPage.SELECT1)), 798080, QuestDialogPage.DEFAULT_SUCCESS));

	/**
	 * 1514 属 P5 SimpleUseItem 族（真端 {@code Quest_SimpleUseItem.xml} 行）：P5 切换批把它交给
	 * {@link SimpleUseItemHandler} 原生直驱，typed 目录里不再有它的压缩定义。行阶梯锚改为真端行锚：
	 * {@code talk_npc1/2} 两级中继（客户端声明 SELECT2/SELECT3 页）+ 交付 NPC 翻 REWARD。
	 * <p>
	 * 1514 belongs to the P5 SimpleUseItem family: the P5 switch batch hands it to the native lane, so the
	 * typed directory no longer carries its compressed definition. The row-ladder anchor is now the retail
	 * row itself — a two-step relay chain (client-declared SELECT2/SELECT3 pages) plus the hand-in npc.
	 */
	@Test
	void useItemChain1514IsDrivenByTheNativeLane() {
		SimpleUseItemHandler handler = SimpleUseItemHandler.instance();
		assertTrue(handler.routes(1514), "1514 必须由 P5 SimpleUseItem native 车道直驱");
		assertTrue(ProductionQuestDefinitions.catalog().findExecutable(1514).isEmpty(),
			"已切换行不得再出现在 typed 目录（单一 owner）");
		assertNotNull(handler.useItemId(1514), "真端 use_item_name 必须解析");

		// 真端 talk_npc1/2 两级中继 = 该行阶梯；每级页必须是客户端声明的可渲染页。
		List<Integer> relays = handler.relayNpcs(1514);
		assertEquals(2, relays.size(), "真端 talk_npc1/2 两级中继");
		assertTrue(NativeTalkFixture.clientDeclares(1514, QuestDialogPage.SELECT2.id()));
		assertTrue(NativeTalkFixture.clientDeclares(1514, QuestDialogPage.SELECT3.id()));

		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 35);
		NativeTalkFixture.start(player, 1514);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), 1514, 26)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT2.id());
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relays.get(1), 1514, 26)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT3.id());

		int rewardNpc = handler.rewardNpcs(1514).getFirst();
		// 两步报告（裁定 a）：31 只发报告确认页——select2/3 都被中继步占用，分型取客户端声明的
		// select5=2375；1009 报告确认才翻 REWARD 并开奖励窗。
		// Two-step report (adjudication a): 31 only shows the report-confirm page (SELECT2/3 are
		// consumed by the relay steps, so the client-declared SELECT5=2375 is picked); 1009 confirms,
		// flips REWARD and opens the reward window.
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, rewardNpc, 1514, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT5.id());
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(1514).getStatus(),
			"31 只发确认页，不推进状态");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, rewardNpc, 1514, 1009)),
			"报告确认后交付 NPC 翻 REWARD 并开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1514).getStatus());
	}

	@Test
	void everyJournalRowOwnsItsOwnProgressState() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());

			// 每一行都要有自己的 START 状态（var0=行号）；旧定义只有 var0=0，第 2 行起没有任何状态，
			// 于是 wiki 里每个步骤的 GM 命令都退化成 //quest set <id> START 0。
			List<Integer> startRows = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START)
				.map(node -> node.projection().variables().get("var0"))
				.sorted()
				.toList();
			assertEquals(IntStream.range(0, chain.rows()).boxed().toList(), startRows,
				"quest " + chain.questId() + " 的 START 状态必须逐行覆盖 var0=0.." + (chain.rows() - 1));

			// 领奖行额外拥有同 var0 的 REWARD 状态。
			assertTrue(definition.nodes().stream()
					.anyMatch(node -> node.projection().status() == QuestStatus.REWARD
						&& node.projection().variables().get("var0") == chain.rows() - 1),
				"quest " + chain.questId() + " 的领奖行必须是 var0=" + (chain.rows() - 1));
		}
	}

	@Test
	void everyStepAdvancesItsOwnStateThroughItsOwnSetproAction() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			for (int index = 0; index < chain.steps().size(); index++) {
				Step step = chain.steps().get(index);
				String source = startLabel(definition, index);
				String target = startLabel(definition, index + 1);

				// 入口页：该行客户端 NPC 的 QUEST_SELECT 必须下发该行客户端链的首页。
				assertTrue(talk(definition, source, source, step.npcId(),
						QuestDialogAction.QUEST_SELECT.id()).afterCommit()
						.contains(new AfterCommitAction.ShowQuestDialog(step.page().id())),
					"quest " + chain.questId() + " 步骤 " + (index + 1) + " 的入口页应为 " + step.page());

				// 推进：该行客户端 NPC 的 SETPRO{i} 从第 i 个状态进入第 i+1 个状态。
				QuestEvent advance = new QuestEvent.TalkToNpc(step.npcId(),
					QuestDialogAction.valueOf("SETPRO" + (index + 1)).id());
				assertTrue(definition.transitions().stream().anyMatch(transition ->
						source.equals(transition.sourceNode()) && target.equals(transition.targetNode())
							&& advance.equals(transition.event())),
					"quest " + chain.questId() + " 步骤 " + (index + 1) + " 缺少 " + source + " -> " + target
						+ " 的 SETPRO" + (index + 1) + " 路由");
			}
		}
	}

	@Test
	void setproActionsStayOnePerStepInsteadOfAllSharingSetproOne() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			List<String> actual = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> (QuestEvent.TalkToNpc) transition.event())
				.filter(talk -> talk.dialogId() != null)
				.map(talk -> QuestDialogAction.fromId(talk.dialogId()).name())
				.filter(name -> name.startsWith("SETPRO"))
				.sorted()
				.toList();
			List<String> expected = IntStream.rangeClosed(1, chain.steps().size())
				.mapToObj(index -> "SETPRO" + index)
				.sorted()
				.toList();
			assertEquals(expected, actual,
				"quest " + chain.questId() + " 的推进动作必须一步一个，不能所有步骤共用 SETPRO1");
		}
	}

	@Test
	void onlyTheRewardRowNpcOwnsRewardRowRoutesAndCompletion() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			String rewardRow = startLabel(definition, chain.rows() - 1);
			String reward = rewardLabel(definition, chain.rows() - 1);

			// 领奖行的所有对话路由（领奖页、提交页）只能属于领奖行 NPC。
			List<Integer> npcs = definition.transitions().stream()
				.filter(transition -> rewardRow.equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.distinct()
				.sorted()
				.toList();
			assertEquals(List.of(chain.rewardNpc()), npcs,
				"quest " + chain.questId() + " 的领奖行只能由 " + chain.rewardNpc() + " 承担");

			// 领奖行的客户端提交动作进入 REWARD（不是由上一步的 SETPRO 直接跳进领奖态）。
			assertTrue(definition.transitions().stream().anyMatch(transition ->
					rewardRow.equals(transition.sourceNode()) && reward.equals(transition.targetNode())
						&& new QuestEvent.TalkToNpc(chain.rewardNpc(),
							QuestDialogAction.SELECT_QUEST_REWARD.id()).equals(transition.event())),
				"quest " + chain.questId() + " 缺少领奖行进入 REWARD 的 SELECT_QUEST_REWARD 路由");

			String complete = completeLabel(definition);
			assertEquals(List.of(chain.rewardNpc()), definition.transitions().stream()
					.filter(transition -> reward.equals(transition.sourceNode())
						&& complete.equals(transition.targetNode()))
					.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
					.distinct()
					.sorted()
					.toList(),
				"quest " + chain.questId() + " 只有领奖行 NPC 能完成任务");
		}
	}

	@Test
	void theChainWalksRowByRowAtRuntime() throws Exception {
		for (Chain chain : CHAINS) {
			CompiledQuestDefinition compiled = compiled(chain.questId());
			QuestDefinition definition = compiled.definition();
			Map<String, Integer> variables = new LinkedHashMap<>(definition.progressLayout().unpack(0));
			QuestStatus status = QuestStatus.START;

			for (int index = 0; index < chain.steps().size(); index++) {
				Step step = chain.steps().get(index);
				QuestTransition advance = talk(definition, startLabel(definition, index),
					startLabel(definition, index + 1), step.npcId(),
					QuestDialogAction.valueOf("SETPRO" + (index + 1)).id());

				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, status, variables), advance.event(), advance).orElseThrow();
				status = plan.nextStatus();
				variables = definition.progressLayout().unpack(plan.nextPackedVariables());

				assertEquals(index + 1, variables.get("var0"),
					"quest " + chain.questId() + " 第 " + (index + 1) + " 步后应停在 var0=" + (index + 1));
				assertEquals(QuestStatus.START, status,
					"quest " + chain.questId() + " 第 " + (index + 1) + " 步后仍是进行中（领奖行尚未提交）");
			}

			// 领奖行自己的客户端提交动作才把第 rows-1 行推进到 REWARD。
			QuestTransition turnIn = talk(definition, startLabel(definition, chain.rows() - 1),
				rewardLabel(definition, chain.rows() - 1), chain.rewardNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD.id());
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, status, variables), turnIn.event(), turnIn).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				"quest " + chain.questId() + " 领奖行提交后应进入 REWARD");
			assertEquals(chain.rows() - 1,
				definition.progressLayout().unpack(plan.nextPackedVariables()).get("var0"),
				"quest " + chain.questId() + " 领奖行的 var0 必须停在末行行号");
		}
	}

	/** 真端行 → native 处理器：owner / 中继树 / 步页 / 交付门逐行对拍。 /
	 * Retail rows against the native handler: owners, relay trees, step pages and gates. */
	@Test
	void retailTalkChainsFollowTheRetailRows() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (Chain chain : RETAIL_TALK_CHAINS) {
			int questId = chain.questId();
			assertTrue(handler.routes(questId), questId + " 必须由 native 车道路由");
			assertEquals(RetailGrantKind.NPC, handler.grantKind(questId), questId + " 为 NPC 接取行");
			assertEquals(chain.rewardNpc(), handler.rewardNpc(questId), questId + " 交付 NPC（真端 reward_npc_name）");
			assertEquals(chain.steps().size(), handler.relayCount(questId),
				questId + " 中继步数 = 客户端链步数");
			for (int index = 0; index < chain.steps().size(); index++) {
				Step step = chain.steps().get(index);
				int expectedStep = index + 1;
				assertTrue(handler.relaysForNpc(step.npcId()).stream()
						.anyMatch(relay -> relay.questId() == questId && relay.step() == expectedStep),
					questId + " 第 " + expectedStep + " 步必须挂在客户端该行的 NPC 上: " + step.npcId());
				assertEquals(step.page().id(), SimpleTalkHandler.pageForStep(expectedStep),
					questId + " 第 " + expectedStep + " 步页 = 客户端链页 " + step.page());
				assertTrue(NativeTalkFixture.clientDeclares(questId, step.page().id()),
					questId + " 客户端任务页必须声明该步页 " + step.page());
			}
			// 这 10 行真端均未声明 item_check：交付门不生效（quest.xml 的 work 通道只对 item_check 行生效）。
			assertTrue(handler.workItems(questId).isEmpty(), questId + " 无 item_check 门");
			assertFalse(handler.unresolvedGate(questId), questId + " 非 item_check 行无门");
			// 真端入口页表：信页优先（select_none 4762 → select1 1011），页 4 只由页动作 1007 打开。
			// Retail entry page table: letter page first (4762 → 1011); page 4 is 1007-only.
			int expectedEntry = NativeTalkFixture.clientDeclares(questId, QuestDialogPage.SELECT_NONE.id())
				? QuestDialogPage.SELECT_NONE.id()
				: NativeTalkFixture.clientDeclares(questId, QuestDialogPage.SELECT1.id())
					? QuestDialogPage.SELECT1.id() : QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
			assertEquals(expectedEntry, NativeTalkFixture.clientEntryPage(questId),
				questId + " 接取入口页（真端信页偏好序）");
		}
	}

	/** 真端 give_item / give_itemN / remove_itemN → native 物品通道（含分档与回收）。 /
	 * Retail give_item / give_itemN / remove_itemN against the native item channels. */
	@Test
	void retailTalkChainsCarryTheirItemChannels() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();

		assertEquals(new SimpleTalkHandler.ItemStack(182200550, 1), handler.stepGiveItem(1183, 1), "1183 步 1 发放");
		assertNull(handler.stepRemoveItem(1183, 1), "1183 步 1 无扣除");
		assertEquals(new SimpleTalkHandler.ItemStack(182200565, 1), handler.stepGiveItem(1183, 2), "1183 步 2 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182200550, 1), handler.stepRemoveItem(1183, 2), "1183 步 2 回收");

		assertNull(handler.acceptGiveItem(1483), "1483 接取无发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182201401, 1), handler.stepGiveItem(1483, 1), "1483 步 1 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182201402, 1), handler.stepGiveItem(1483, 2), "1483 步 2 发放");

		assertEquals(new SimpleTalkHandler.ItemStack(182202151, 1), handler.acceptGiveItem(1721), "1721 接取发放");
		assertNull(handler.stepGiveItem(1721, 1), "1721 步 1 无发放");
		assertNull(handler.stepRemoveItem(1721, 2), "1721 步 2 无扣除");

		assertNull(handler.acceptGiveItem(1724), "1724 接取无发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182202152, 1), handler.stepGiveItem(1724, 2), "1724 步 2 发放");

		assertEquals(new SimpleTalkHandler.ItemStack(182204515, 1), handler.stepGiveItem(2646, 1), "2646 步 1 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182204516, 1), handler.stepGiveItem(2646, 2), "2646 步 2 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182204515, 1), handler.stepRemoveItem(2646, 2), "2646 步 2 回收");
		assertEquals(new SimpleTalkHandler.ItemStack(182204516, 1), handler.stepRemoveItem(2646, 3), "2646 步 3 回收");

		assertEquals(new SimpleTalkHandler.ItemStack(182204510, 1), handler.acceptGiveItem(2692), "2692 接取发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182204511, 1), handler.stepGiveItem(2692, 3), "2692 步 3 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182204510, 1), handler.stepRemoveItem(2692, 3), "2692 步 3 回收");

		assertEquals(new SimpleTalkHandler.ItemStack(182205686, 1), handler.acceptGiveItem(2767), "2767 接取发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182205686, 1), handler.stepRemoveItem(2767, 2), "2767 步 2 回收");

		assertNull(handler.acceptGiveItem(3966), "3966 无发放");
		assertNull(handler.stepGiveItem(3966, 1), "3966 步 1 无发放");
		assertNull(handler.stepRemoveItem(3966, 3), "3966 步 3 无扣除");

		assertEquals(new SimpleTalkHandler.ItemStack(182206123, 1), handler.stepGiveItem(3968, 1), "3968 步 1 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182206124, 1), handler.stepGiveItem(3968, 2), "3968 步 2 发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182206125, 1), handler.stepGiveItem(3968, 3), "3968 步 3 发放");

		assertNull(handler.acceptGiveItem(4501), "4501 接取无发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182204533, 1), handler.stepGiveItem(4501, 2), "4501 步 2 发放");

		// 链式接取窗（真端 con_quest）逐行冻结：1483→1484、3966→3967、3968→3969，其余该列缺席。
		assertEquals(1484, handler.conQuest(1483), "1483 链式接取窗");
		assertEquals(3967, handler.conQuest(3966), "3966 链式接取窗");
		assertEquals(3969, handler.conQuest(3968), "3968 链式接取窗");
		assertNull(handler.conQuest(1183), "1183 无链式接取窗");
		assertNull(handler.conQuest(2692), "2692 无链式接取窗");
	}

	/**
	 * 1183 的 native 运行时逐行走链：问询 → 接取 → 两步中继（步内发扣）→ 报告翻 REWARD；
	 * 重复步零副作用。
	 * The native runtime walk for 1183: ask, accept, two relay steps, then the report flips REWARD;
	 * a repeated step has zero side effects.
	 */
	@Test
	void theRetailChainWalksRowByRowAtRuntime() {
		Chain chain = RETAIL_TALK_CHAINS.getFirst();
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);
		int acquireNpc = chain.rewardNpc();

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, chain.questId(), 31)),
			"接取问询");
		assertNull(player.getQuestStateList().getQuestState(chain.questId()), "问询页不得落库");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(chain.questId()));

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, chain.questId(), 1002)),
			"接取确认");
		QuestState state = player.getQuestStateList().getQuestState(chain.questId());
		assertEquals(QuestStatus.START, state.getStatus(), "接取落 START");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

		for (int index = 0; index < chain.steps().size(); index++) {
			Step step = chain.steps().get(index);
			// 任务行打开面（31/26）：下发该步页（客户端声明为准）。
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, step.npcId(), chain.questId(), 31)),
				"第 " + (index + 1) + " 步打开面");
			NativeTalkFixture.assertOnlyDialogPage(player, step.page().id());
			// 推进面（10000+step-1）after-commit 零发页：var0=步号 + 状态包 + 步物品发扣 + 关窗
			// （真端 FUN_180cabb10；2026-10-05 实机 1131 确证）。
			// The advance face sends no page (retail FUN_180cabb10): vars, action packet, step items, close.
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, step.npcId(), chain.questId(),
				10000 + index)), "第 " + (index + 1) + " 步推进被受理");
			assertEquals(index + 1, state.getQuestVars().getQuestVars(),
				"第 " + (index + 1) + " 步后 raw vars = 步号");
			assertEquals(QuestStatus.START, state.getStatus(), "中继中保持 START");
			NativeTalkFixture.assertCloseDialog(player);
		}
		assertEquals(List.of("give:182200550:1", "give:182200565:1", "remove:182200550:1"), inventory.calls(),
			"步内发扣按真端顺序执行");

		// 重复第 1 步：vars 已推进，零副作用、关窗兜底（真端无匹配转换）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, chain.steps().get(0).npcId(),
			chain.questId(), 10000)), "重复步不得被拒（客户端仍等关窗）");
		assertEquals(2, state.getQuestVars().getQuestVars(), "重复步不得推进");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("give:182200550:1", "give:182200565:1", "remove:182200550:1"), inventory.calls(),
			"重复步零发放/扣除");

		// 报告：中继全满（真端 finalStep）→ 领奖态 + 奖励窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, chain.questId(), 1009)), "报告");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "报告翻 REWARD");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);

		// 领奖态再说话：仍回到奖励窗（页与选择按钮同 owner）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, chain.questId(), 31)),
			"领奖态奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	private static String startLabel(QuestDefinition definition, int var0) {
		return labelOf(definition, QuestStatus.START, var0);
	}

	private static String rewardLabel(QuestDefinition definition, int var0) {
		return labelOf(definition, QuestStatus.REWARD, var0);
	}

	private static String completeLabel(QuestDefinition definition) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.COMPLETE)
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), "必须声明唯一的 COMPLETE 状态");
		return labels.getFirst();
	}

	private static String labelOf(QuestDefinition definition, QuestStatus status, int var0) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().get("var0") == var0)
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), "quest " + definition.id() + " 的 " + status + "(var0=" + var0 + ") 状态必须唯一");
		return labels.getFirst();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& new QuestEvent.TalkToNpc(npcId, action).equals(candidate.event()))
			.findFirst().orElseThrow(() -> new AssertionError(
				"missing route " + source + " -> " + target + " npc=" + npcId + " action=" + action));
	}

	private static QuestDefinition definition(int questId) throws Exception {
		return compiled(questId).definition();
	}

	private static CompiledQuestDefinition compiled(int questId) {
		// 行已由真端表驱动（退役），改从生产视图取定义。
		// The rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(variables),
			workItems(definition), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	/**
	 * 领奖行的交付条件（如 3966/3968 的 has-item）要求背包持有任务物品，
	 * 否则条件路由不会命中，测试无法走到 REWARD。
	 * Turn-in conditions (e.g. the has-item gate on 3966/3968) require the work item in the inventory,
	 * otherwise the conditional route never resolves and the walk cannot reach REWARD.
	 */
	private static Map<Integer, Integer> workItems(CompiledQuestDefinition definition) {
		// <items>（itemRequirements）与 <work-items> 都可能声明交付物品，测试快照两者都要带上。
		// Both <items> (itemRequirements) and <work-items> may declare the turn-in item.
		Map<Integer, Integer> inventory = new LinkedHashMap<>();
		for (QuestItemRequirement requirement : definition.definition().metadata().itemRequirements()) {
			inventory.put(requirement.itemId(), requirement.count());
		}
		for (QuestItemRequirement requirement : definition.definition().metadata().questWorkItems()) {
			inventory.put(requirement.itemId(), requirement.count());
		}
		return Map.copyOf(inventory);
	}
}
