package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestJourneyRunner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真端表驱动任务的黑盒生命周期契约门：只锁"接取 → 进度 → 交付 → 结算"四相位合同，
 * 不锁任何具体对话页链（页链形状允许随真端规范形改造滚动）。
 * <p>
 * 合同（对全部代表任务逐条验证）：
 * <ol>
 * <li>接取：与接取 NPC 交互后沿客户端可见动作走到 ask_quest_accept 窗（页 4），回传
 * {@code QUEST_ACCEPT_1(1002)} 后必须建档进入 {@code START}；</li>
 * <li>进度：按定义自身的击杀/使用/演出/步骤边驱动，进度字段或状态必须真实前进，
 * 且进度期间不得进入 {@code COMPLETE}；</li>
 * <li>交付：进度达成后沿真实交互走到分档奖励窗（页面必须是
 * {@code QuestDialogPage.rewardWindowForTier} 查表值），状态为 {@code REWARD}；</li>
 * <li>结算：原生奖励确认动作（{@code SELECTED_QUEST_REWARD1(8)}）必须一次性
 * {@code CompleteQuest} 并发放奖励，进入 {@code COMPLETE}。</li>
 * </ol>
 * Black-box lifecycle contract gate for retail-table-driven quests: locks the four-phase
 * acquire/progress/deliver/settle contract without locking any concrete dialog page chain.
 */
class RetailQuestContractTest {

	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	/** 分档奖励窗页面全集（rewardWindowForTier 0..5 档）。 / Every tiered reward-window page. */
	private static final Set<Integer> REWARD_WINDOW_PAGES = Set.of(5, 6, 7, 8, 45, 46);
	/** 奖励确认动作区间（8..23 与 108/110..124 全局窗通道）。 / Reward-confirm action ids. */
	private static final Set<Integer> REWARD_CONFIRM_ACTIONS = rewardConfirmActions();
	/** 接取/拒绝/翻页导航动作：永远不算进度边。 / Accept/refuse/navigation actions, never progress edges. */
	private static final Set<Integer> NAVIGATION_ACTIONS = Set.of(
		QuestDialogAction.QUEST_ACCEPT_1.id(), QuestDialogAction.QUEST_REFUSE_1.id(),
		QuestDialogAction.QUEST_REFUSE_2.id(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id(),
		QuestDialogAction.QUEST_REFUSE_SIMPLE.id(), QuestDialogAction.ASK_QUEST_ACCEPT.id(),
		QuestDialogAction.FINISH_DIALOG.id(), QuestDialogAction.SELECT_QUEST_REWARD.id(),
		QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), QuestDialogAction.QUEST_SELECT.id());
	private static ClientResourceOracle oracle;

	@BeforeAll
	static void loadClientEvidence() throws Exception {
		oracle = ClientResourceOracle.load(CLIENT_MAPPING);
	}

	/** 家族代表：每个真端驱动家族至少一条已验证形状。 / One proven shape per retail family. */
	private record ContractCase(String family, int questId) {
		@Override
		public String toString() {
			return family + "#" + questId;
		}
	}

	private static final List<ContractCase> CASES = List.of(
		new ContractCase("SimpleHunt", 1112),
		new ContractCase("SimpleHunt", 1102),
		new ContractCase("SimpleHunt", 30715),
		new ContractCase("SimpleHunt", 1350),
		new ContractCase("SimpleSerialHunt", 13918),
		new ContractCase("SimpleSerialHunt", 23918),
		new ContractCase("SimpleCollectItem", 1103),
		// SimpleCollectItem 14120：带中间 NPC 简报步（P0-2 一步直达采集行）+ HasItem 门控交付。
		// SimpleCollectItem 14120: briefed collect quest (one-step briefing) with the HasItem-gated delivery.
		new ContractCase("SimpleCollectItem", 14120),
		// SimpleUseItem：道具接取（UseItem→页 4→无主 1002）——1107 是 1009 直交形（canonical 后
		// QUEST_SELECT 无条件直翻），80554 是 CHECK 交付物门控形（QUEST_SELECT+HasItem）。
		// SimpleUseItem: item-use accept (UseItem -> page 4 -> targetless 1002); 1107 is the direct
		// form (QUEST_SELECT flips unconditionally), 80554 the CHECK form gated on the hand-in.
		new ContractCase("SimpleUseItem", 1107),
		new ContractCase("SimpleUseItem", 80554),
		new ContractCase("SimpleTalk", 1101),
		new ContractCase("SimpleTalk", 1115),
		new ContractCase("SimpleTalk", 1118),
		// SimpleTalk 1938: 2-step talk chain
		new ContractCase("SimpleTalk", 1938),
		new ContractCase("SimpleTalk", 2641),
		new ContractCase("SimpleTalk", 2101),
		new ContractCase("SimpleItemPlay", 13704),
		// DataDriven hunt grid 形：带击杀推进
		new ContractCase("DataDriven", 13770),
		// DataDriven talk chain 形：多 NPC 链式对话推进
		new ContractCase("DataDriven", 11323),
		// DataDriven talk collect canonical 形：带采集物门控的对话交付
		new ContractCase("DataDriven", 13968),
		// DataDriven no-progress 形（D-a 片）：接取 → 交付 NPC 对话直翻 REWARD + 分档窗，无进度相位
		// （契约门对无进度形允许零进度步）。 / DataDriven no-progress shape (the D-a slice): acquire,
		// then the reward npc's dialogue flips REWARD with the tiered window; no progress phase.
		new ContractCase("DataDriven", 1919),
		new ContractCase("DataDriven", 15042),
		new ContractCase("DataDriven", 15546),
		new ContractCase("DataDriven", 18996));

	@Test
	void retailQuestsHonorTheNativeLifecycleContract() throws Exception {
		List<String> failures = new ArrayList<>();
		for (ContractCase c : CASES) {
			try {
				runLifecycle(c);
			} catch (AssertionError | Exception failure) {
				failures.add(c + ": " + failure.getMessage());
			}
		}
		assertTrue(failures.isEmpty(), () -> "lifecycle contract violations / 生命周期合同违约: "
			+ String.join(" || ", failures));
	}
	// ---------------------------------------------------------------- 合同执行

	private void runLifecycle(ContractCase c) throws Exception {
		CompiledQuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(c.questId());
		Optional<QuestTransition> itemAccept = itemAcceptTransition(definition);
		if (itemAccept.isPresent()) {
			runItemUseLifecycle(c, definition, itemAccept.orElseThrow());
			return;
		}
		QuestTransition accept = acceptTransition(definition);
		int acquireNpc = ((QuestEvent.TalkToNpc) accept.event()).npcId();
		QuestTransition ingress = ingressTransition(definition, accept.sourceNode(), acquireNpc);
		// 采集族的交付门是背包事实：按元数据交付物种子背包（无交付物的家族 → 空种子无害）。
		// Collect-family delivery gates on inventory facts: seed the bag from the metadata hand-ins
		// (families without hand-ins seed nothing).
		Map<Integer, Integer> initialInventory = new LinkedHashMap<>();
		for (QuestItemRequirement item : definition.definition().metadata().itemRequirements()) {
			initialInventory.merge(item.itemId(), item.count(), Integer::sum);
		}
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, ingress, oracle,
				PlayerClass.GLADIATOR, initialInventory)) {
			// 相位 A：接取 —— 沿可见动作走到接取提交（询问窗 1002 形或直接接取 20000 形）。
			// Phase A: acquire — walk visible actions to an accept commit (ask-window 1002 or simple 20000).
			QuestJourneyRunner.Step first = journey.interact(acquireNpc, QuestDialogAction.QUEST_SELECT.id());
			assertHandled(c, journey, first);
			QuestJourneyRunner.Step accepted = walkToAcceptCommit(c, journey, definition, acquireNpc, 6);
			assertEquals(QuestStatus.START, accepted.status(), c + ": 接取提交后必须建档进入 START");
			assertEquals(0, accepted.packedVariables(), c + ": 接取时进度必须为空");

			// 相位 B：进度 —— 按定义自身的进度边驱动到可交付。
			// Phase B: progress — drive the definition's own progress edges to deliverability.
			driveProgress(c, journey, definition);

			// 相位 C：交付 —— 沿真实交互走到分档奖励窗。
			// Phase C: deliver — reach the tiered reward window through real interactions.
			walkToRewardWindow(c, journey, definition, 8);
			assertEquals(QuestStatus.REWARD, journey.status(), c + ": 交付窗前必须处于 REWARD");
			int expectedWindow = QuestDialogPage.rewardWindowForTier(
				definition.definition().metadata().rewardGroups().size() - 1)
				.orElseThrow(() -> new AssertionError(c + ": 奖励档位超出客户端六档窗")).id();
			assertEquals(expectedWindow, journey.page(), c + ": 奖励窗必须按档位查表下发");

			// 相位 D：结算 —— 原生确认动作一次性完成并发奖。
			// Phase D: settle — one native confirm completes once and grants rewards.
			QuestJourneyRunner.Step settled = journey.clickNativeAction(
				QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			assertHandled(c, journey, settled);
			assertEquals(QuestStatus.COMPLETE, settled.status(), c + ": 结算后必须 COMPLETE");
			assertEquals(1, settled.committedActions().stream()
				.filter(action -> action instanceof QuestAction.CompleteQuest).count(),
				c + ": CompleteQuest 必须恰好一次");
			assertTrue(settled.committedActions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward),
				c + ": 结算必须发放奖励");
		}
	}

	/**
	 * 用物品接取族（SimpleUseItem）的黑盒生命周期：交付门是背包事实——种子用物品本体与全部
	 * 交付边的 HasItem 门控物；相位 A = 右键道具弹接取窗（页 4）→ 无主 1002 建档；
	 * 相位 B/C/D（进度/交付/结算）与 NPC 接取族同构。
	 * The item-use acquire family's black-box lifecycle: delivery gates on inventory facts — seed
	 * the use item itself plus every delivery edge's HasItem gates; phase A = the item pops the ask
	 * window (page 4) and the targetless 1002 commits; phases B/C/D are isomorphic to the
	 * NPC-acquire families.
	 */
	private void runItemUseLifecycle(ContractCase c, CompiledQuestDefinition definition,
			QuestTransition itemAccept) throws Exception {
		int useItemId = ((QuestEvent.UseItem) itemAccept.event()).itemId();
		Map<Integer, Integer> initialInventory = new LinkedHashMap<>();
		initialInventory.merge(useItemId, 1, Integer::sum);
		for (QuestTransition edge : definition.definition().transitions()) {
			for (QuestCondition condition : edge.conditions()) {
				if (condition instanceof QuestCondition.HasItem has) {
					initialInventory.merge(has.itemId(), has.count(), Integer::sum);
				}
			}
		}
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, itemAccept, oracle,
				PlayerClass.GLADIATOR, initialInventory)) {
			QuestJourneyRunner.Step popped = journey.useItem(useItemId);
			assertHandled(c, journey, popped);
			assertEquals(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(), journey.page(),
				c + ": 用物品必须弹出接取确认窗");
			QuestJourneyRunner.Step accepted = journey.clickTargetlessAction(
				QuestDialogAction.QUEST_ACCEPT_1.id());
			assertHandled(c, journey, accepted);
			assertEquals(QuestStatus.START, accepted.status(), c + ": 接取提交后必须建档进入 START");
			assertEquals(0, accepted.packedVariables(), c + ": 接取时进度必须为空");

			driveProgress(c, journey, definition);
			walkToRewardWindow(c, journey, definition, 8);
			assertEquals(QuestStatus.REWARD, journey.status(), c + ": 交付窗前必须处于 REWARD");
			int expectedWindow = QuestDialogPage.rewardWindowForTier(
				definition.definition().metadata().rewardGroups().size() - 1)
				.orElseThrow(() -> new AssertionError(c + ": 奖励档位超出客户端六档窗")).id();
			assertEquals(expectedWindow, journey.page(), c + ": 奖励窗必须按档位查表下发");
			QuestJourneyRunner.Step settled = journey.clickNativeAction(
				QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			assertHandled(c, journey, settled);
			assertEquals(QuestStatus.COMPLETE, settled.status(), c + ": 结算后必须 COMPLETE");
			assertEquals(1, settled.committedActions().stream()
				.filter(action -> action instanceof QuestAction.CompleteQuest).count(),
				c + ": CompleteQuest 必须恰好一次");
			assertTrue(settled.committedActions().stream()
					.anyMatch(action -> action instanceof QuestAction.GrantReward),
				c + ": 结算必须发放奖励");
		}
	}

	// ---------------------------------------------------------------- 相位 B：进度驱动

	/** 沿定义自身的进度边驱动；击杀即翻形（直达 REWARD 节点）与交付时翻转两种形状都合法。 */
	private void driveProgress(ContractCase c, QuestJourneyRunner journey, CompiledQuestDefinition definition) {
		int previousPacked = journey.packedVariables();
		int stalled = 0;
		int steps = 0;
		// 无进度形（noProgress talk 行）：接取即可交付，零进度步合法；声明了进度边的定义仍要求至少
		// 驱动一步（防"进度边全不可驱动"的静默缺口）。 / The no-progress shape has zero progress steps
		// by design; a definition that declares progress edges must still drive at least one.
		boolean declaresProgress = !progressEdges(definition, currentNode(definition, journey)).isEmpty();
		while (steps < 300) {
			String node = currentNode(definition, journey);
			if (statusOf(definition, node) == QuestStatus.REWARD) {
				break;
			}
			List<QuestTransition> edges = progressEdges(definition, node);
			if (edges.isEmpty()) {
				break;
			}
			// 按序尝试全部进度边（配对狩猎等条件门控边在前不满足时由后续计数边推进）；
			// 未命中请求无副作用，可安全重试。
			// Try every progress edge in order: condition-gated advance edges yield to counting edges;
			// unmatched requests are side-effect free and safe to retry.
			QuestJourneyRunner.Step fired = null;
			for (QuestTransition edge : edges) {
				QuestJourneyRunner.Step step = emitProgressEdge(journey, definition, edge);
				if (step.outcome().handled() && !step.outcome().failed()) {
					fired = step;
					break;
				}
			}
			if (fired == null) {
				break;
			}
			assertHandled(c, journey, fired);
			assertTrue(fired.status() != QuestStatus.COMPLETE, c + ": 进度期间不得完成");
			steps++;
			int packed = journey.packedVariables();
			// 协议归因在多候选时 matchedTransition 为空，此时按节点未变处理。
			// Protocol attribution leaves matchedTransition null on multi-candidate hits; treat as same node.
			String firedTarget = fired.matchedTransition() == null ? node : fired.matchedTransition().targetNode();
			boolean advanced = !firedTarget.equals(node) || packed != previousPacked;
			stalled = advanced ? 0 : stalled + 1;
			previousPacked = packed;
			assertTrue(stalled < 4, c + ": 进度边在 " + node + " 连续空转" + stepDump(journey));
		}
		assertTrue(steps > 0 || !declaresProgress, c + ": 定义声明了进度边但一条都驱动不了" + stepDump(journey));
	}

	private QuestJourneyRunner.Step emitProgressEdge(QuestJourneyRunner journey, CompiledQuestDefinition definition,
			QuestTransition edge) {
		return switch (edge.event()) {
			case QuestEvent.KillNpc kill -> journey.emitWorldEvent(new QuestEvent.KillNpc(kill.npcId()));
			case QuestEvent.TalkToNpc talk -> fireTalkEdge(journey, definition, talk);
			case QuestEvent.QuestDialog dialog -> journey.clickTargetlessAction(dialog.dialogId());
			case QuestEvent.UseItem use -> journey.useItem(use.itemId());
			case QuestEvent.ItemPlay play -> journey.playItem(play.itemId(), play.animationMillis());
			default -> journey.emitWorldEvent(edge);
		};
	}

	/**
	 * 按真实客户端顺序触发对话边：QUEST_SELECT 用 CM_DIALOG_SELECT 开对话；-1 是 CM_SHOW_DIALOG
	 * 的开对话语义（useObject）；其余动作先保证有目标 NPC 的对话页上下文（NPC 变了或当前停在
	 * 通用任务列表页 10 时先开对话），再点页上可见按钮，页上不可见才退回冷发。
	 * Fires a talk edge in the real client order: QUEST_SELECT opens via CM_DIALOG_SELECT; -1 is the
	 * CM_SHOW_DIALOG open-dialog semantic (useObject); other actions first establish the target NPC's
	 * dialog-page context (fresh NPC or lingering quest-list page 10), then click the visible button,
	 * falling back to a cold dialog-select only when the page lacks the button.
	 */
	private QuestJourneyRunner.Step fireTalkEdge(QuestJourneyRunner journey, CompiledQuestDefinition definition,
			QuestEvent.TalkToNpc talk) {
		int action = talk.dialogId();
		int npc = talk.npcId();
		if (action == QuestDialogAction.QUEST_SELECT.id()) {
			return journey.interact(npc, action);
		}
		if (action == -1) {
			return journey.useObject(npc);
		}
		int lastNpc = journey.steps().getLast().npcId();
		if (npc != lastNpc || journey.page() == QuestDialogPage.SELECT_QUEST.id()) {
			QuestJourneyRunner.Step open = journey.interact(npc, QuestDialogAction.QUEST_SELECT.id());
			if (!open.outcome().handled()) {
				open = journey.useObject(npc);
			}
			if (!open.outcome().handled()) {
				throw new AssertionError("无法与 NPC " + npc + " 打开对话（31/useObject 均未命中）");
			}
		}
		boolean visible = oracle.visibleActions(definition.id(), journey.page()).stream()
			.anyMatch(candidate -> candidate.actionId() == action);
		return visible ? journey.clickVisibleAction(action) : journey.interact(npc, action);
	}

	/**
	 * 进度边 = 击杀/使用/演出/拾取/进区域等世界事件，或"推进到新节点"的对话步骤
	 * （对话链步骤、简报清位、无主步骤动作）。
	 */
	private static List<QuestTransition> progressEdges(CompiledQuestDefinition definition, String node) {
		return definition.definition().transitions().stream()
			.filter(candidate -> node.equals(candidate.sourceNode()))
			.filter(candidate -> switch (candidate.event()) {
				case QuestEvent.TalkToNpc talk -> isStepTalk(candidate, talk);
				case QuestEvent.QuestDialog dialog -> isStepQuestDialog(candidate, dialog);
				default -> isAdvancing(candidate);
			})
			.toList();
	}

	/** 事件类进度边：推进到新节点即算（EnterZone/LevelUp 等场景推进）。 */
	private static boolean isAdvancing(QuestTransition candidate) {
		return !candidate.targetNode().equals(candidate.sourceNode());
	}

	/** 无主步骤边（DD 链双协议注册的 QuestDialog 形）：仅"推进到新节点"的算进度。 */
	private static boolean isStepQuestDialog(QuestTransition candidate, QuestEvent.QuestDialog dialog) {
		boolean advancing = !candidate.targetNode().equals(candidate.sourceNode());
		return advancing && !NAVIGATION_ACTIONS.contains(dialog.dialogId())
			&& !REWARD_CONFIRM_ACTIONS.contains(dialog.dialogId());
	}

	private static boolean isStepTalk(QuestTransition candidate, QuestEvent.TalkToNpc talk) {
		if (talk.dialogId() == null || REWARD_CONFIRM_ACTIONS.contains(talk.dialogId())) {
			return false;
		}
		boolean advancing = !candidate.targetNode().equals(candidate.sourceNode());
		if (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()) {
			// QUEST_SELECT 只有换节点时才是步骤边（对话链到下一个 talk NPC）；自环是页导航。
			// A QUEST_SELECT is a step edge only when it changes nodes (chain hop); self-loops are page nav.
			return advancing;
		}
		return !NAVIGATION_ACTIONS.contains(talk.dialogId()) && advancing;
	}

	// ---------------------------------------------------------------- 对话页行走器

	/**
	 * 沿客户端可见动作走到"接取提交"：询问窗形（翻到页 4 后回传 1002）或直接接取形
	 * （入口页可见 20000）。返回提交步；不锁任何中间页链。
	 * Walks visible actions to an accept commit: the ask-window form (page 4 then 1002) or the
	 * simple-accept form (visible 20000 on the letter page). Returns the commit step.
	 */
	private QuestJourneyRunner.Step walkToAcceptCommit(ContractCase c, QuestJourneyRunner journey,
			CompiledQuestDefinition definition, int npcId, int maxHops) {
		for (int hop = 0; hop <= maxHops; hop++) {
			String node = currentNode(definition, journey);
			// 提交边按"当前页可见"挑选：直接接取形页面上 20000 可见而 1002 不可见，反之亦然。
			// Pick the commit edge visible on the current page: 20000 vs 1002 visibility differs by form.
			Optional<QuestTransition> commit = acceptCommitEdges(definition, node, npcId).stream()
				.filter(edge -> {
					int action = ((QuestEvent.TalkToNpc) edge.event()).dialogId();
					return oracle.visibleActions(definition.id(), journey.page()).stream()
						.anyMatch(candidate -> candidate.actionId() == action);
				})
				.findFirst();
			if (commit.isPresent()) {
				int action = ((QuestEvent.TalkToNpc) commit.get().event()).dialogId();
				QuestJourneyRunner.Step step = journey.clickVisibleAction(action);
				assertHandled(c, journey, step);
				return step;
			}
			// 接取窗（页 4）的接受/拒绝是原生控件，客户端页表可以没有登记——停在询问窗而没有
			// 可见提交边时，按真实客户端语义直接向接取 NPC 发送提交动作。
			// The ask window's accept/refuse are native controls the client page table may not
			// register; while parked on the ask window, send the commit action to the NPC directly.
			if (journey.page() == QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()) {
				Optional<QuestTransition> nativeCommit = acceptCommitEdges(definition, node, npcId).stream()
					.findFirst();
				if (nativeCommit.isPresent()) {
					int action = ((QuestEvent.TalkToNpc) nativeCommit.get().event()).dialogId();
					QuestJourneyRunner.Step step = journey.interact(npcId, action);
					assertHandled(c, journey, step);
					return step;
				}
			}
			Optional<Integer> choice = nextActionToward(journey, definition, npcId,
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id());
			assertTrue(choice.isPresent(), c + ": 页 " + journey.page() + "（节点 " + node
				+ "）没有通往接取提交的可见动作" + stepDump(journey));
			assertHandled(c, journey, journey.clickVisibleAction(choice.get()));
		}
		throw new AssertionError(c + ": 接取提交未在 " + maxHops + " 跳内到达");
	}

	/** 接取提交边集：接取 NPC 上可见的 1002（询问窗确认）与 20000（直接接取）。 */
	private static List<QuestTransition> acceptCommitEdges(CompiledQuestDefinition definition, String node,
			int npcId) {
		return definition.definition().transitions().stream()
			.filter(candidate -> node.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id()
					|| talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()))
			.filter(candidate -> statusOf(definition, candidate.targetNode()) == QuestStatus.START)
			.toList();
	}

	/** 从当前页的可见动作里挑一个：优先直达目标页的，其次翻到新页的。 */
	private Optional<Integer> nextActionToward(QuestJourneyRunner journey, CompiledQuestDefinition definition,
			int npcId, int targetPage) {
		String node = currentNode(definition, journey);
		List<Integer> visible = oracle.visibleActions(definition.id(), journey.page()).stream()
			.map(ClientResourceOracle.ClientAction::actionId).toList();
		Optional<Integer> hop = Optional.empty();
		for (int action : visible) {
			Optional<Integer> shown = shownPage(definition, node, npcId, action);
			if (shown.isEmpty() || shown.get() == 0 || shown.get() == journey.page()) {
				continue;
			}
			if (shown.get() == targetPage) {
				return Optional.of(action);
			}
			hop = hop.isEmpty() ? Optional.of(action) : hop;
		}
		return hop;
	}

	/** 沿真实交互走到分档奖励窗：直达边（含满格门控的 1009）优先，页链自省跟进。 */
	private void walkToRewardWindow(ContractCase c, QuestJourneyRunner journey, CompiledQuestDefinition definition,
			int maxHops) {
		for (int hop = 0; hop < maxHops; hop++) {
			if (REWARD_WINDOW_PAGES.contains(journey.page())) {
				return;
			}
			String node = currentNode(definition, journey);
			Optional<QuestTransition> direct = dialogEdgesFrom(definition, node).stream()
				.filter(edge -> shownPageOf(edge).filter(REWARD_WINDOW_PAGES::contains).isPresent())
				.filter(edge -> fireable(journey, definition, edge))
				.findFirst();
			if (direct.isPresent()) {
				assertHandled(c, journey, fireDialogEdge(journey, definition, direct.get()));
				continue;
			}
			Optional<QuestTransition> pageHop = dialogEdgesFrom(definition, node).stream()
				.filter(edge -> shownPageOf(edge)
					.filter(shown -> shown != 0 && shown != journey.page() && !REWARD_WINDOW_PAGES.contains(shown))
					.isPresent())
				.filter(edge -> fireable(journey, definition, edge))
				.findFirst();
			if (pageHop.isPresent()) {
				assertHandled(c, journey, fireDialogEdge(journey, definition, pageHop.get()));
				continue;
			}
			fail(c, "节点 " + node + "（页 " + journey.page() + "）到奖励窗无可见路径" + stepDump(journey));
		}
		fail(c, "奖励窗未在 " + maxHops + " 跳内到达");
	}

	/**
	 * 边当前是否值得尝试：行走器的开对话/可见性策略在 fireTalkEdge 内自守，这里只排除
	 * 无 dialogId 的悬空对话边。
	 * Whether the edge is worth attempting: fireTalkEdge self-guards the open/visibility policy; only
	 * dialog edges without an action id are excluded here.
	 */
	private boolean fireable(QuestJourneyRunner journey, CompiledQuestDefinition definition, QuestTransition edge) {
		return switch (edge.event()) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId() != null;
			case QuestEvent.QuestDialog dialog -> true;
			default -> false;
		};
	}

	private QuestJourneyRunner.Step fireDialogEdge(QuestJourneyRunner journey, CompiledQuestDefinition definition,
			QuestTransition edge) {
		if (edge.event() instanceof QuestEvent.TalkToNpc talk) {
			return fireTalkEdge(journey, definition, talk);
		}
		return journey.clickTargetlessAction(((QuestEvent.QuestDialog) edge.event()).dialogId());
	}

	// ---------------------------------------------------------------- IR 检索

	private static QuestTransition acceptTransition(CompiledQuestDefinition definition) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(QuestDialogAction.QUEST_ACCEPT_1.id()).equals(talk.dialogId()))
			.filter(candidate -> statusOf(definition, candidate.sourceNode()) == QuestStatus.NONE)
			.min(java.util.Comparator.comparingInt(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId()))
			.orElseThrow(() -> new AssertionError("定义没有 NONE 态 1002 接取边（世界/系统发放形不在本门范围）"));
	}

	/** 用物品接取边（NONE 态 UseItem 弹接取窗）；无则 empty（NPC 对话接取形）。 /
	 * The item-use accept edge (a NONE-state UseItem pops the ask window); empty for NPC-acquire forms. */
	private static Optional<QuestTransition> itemAcceptTransition(CompiledQuestDefinition definition) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.UseItem)
			.filter(candidate -> statusOf(definition, candidate.sourceNode()) == QuestStatus.NONE)
			.findFirst();
	}

	private static QuestTransition ingressTransition(CompiledQuestDefinition definition, String noneNode,
			int acquireNpc) {
		return definition.definition().transitions().stream()
			.filter(candidate -> noneNode.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == acquireNpc
				&& Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("接取 NPC 缺少 QUEST_SELECT 入口边"));
	}

	private static List<QuestTransition> dialogEdgesFrom(CompiledQuestDefinition definition, String node) {
		return definition.definition().transitions().stream()
			.filter(candidate -> node.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc
				|| candidate.event() instanceof QuestEvent.QuestDialog)
			.toList();
	}

	private static Optional<Integer> shownPageOf(QuestTransition edge) {
		return edge.afterCommit().stream()
			.filter(action -> action instanceof AfterCommitAction.ShowQuestDialog)
			.map(action -> ((AfterCommitAction.ShowQuestDialog) action).dialogId())
			.findFirst();
	}

	private Optional<Integer> shownPage(CompiledQuestDefinition definition, String node, int npcId, int action) {
		return dialogEdgesFrom(definition, node).stream()
			.filter(edge -> edge.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action).equals(talk.dialogId()))
			.map(RetailQuestContractTest::shownPageOf)
			.flatMap(java.util.Optional::stream)
			.findFirst();
	}

	private static String currentNode(CompiledQuestDefinition definition, QuestJourneyRunner journey) {
		List<QuestJourneyRunner.Step> steps = journey.steps();
		return steps.isEmpty()
			? definition.definition().transitions().getFirst().sourceNode()
			: steps.getLast().matchedTransition() == null
				? previousNode(steps)
				: steps.getLast().matchedTransition().targetNode();
	}

	/** 多候选归因时回退：取最近一个带归因的节点。 / Fall back to the last attributed node. */
	private static String previousNode(List<QuestJourneyRunner.Step> steps) {
		for (int index = steps.size() - 1; index >= 0; index--) {
			QuestTransition matched = steps.get(index).matchedTransition();
			if (matched != null) {
				return matched.targetNode();
			}
		}
		throw new AssertionError("旅程没有任何归因步骤");
	}

	private static QuestStatus statusOf(CompiledQuestDefinition definition, String node) {
		return definition.definition().nodes().stream()
			.filter(questNode -> questNode.label().equals(node))
			.map(questNode -> questNode.projection().status())
			.findFirst()
			.orElseThrow(() -> new AssertionError("未知节点 " + node));
	}

	private static void assertHandled(ContractCase c, QuestJourneyRunner journey, QuestJourneyRunner.Step step) {
		String context = c + " @ " + step.label() + "（已执行 " + journey.steps().size() + " 步）";
		assertTrue(step.outcome().handled(), () -> context + " 未命中: " + step + stepDump(journey));
		assertTrue(!step.outcome().failed(), () -> context + " 失败: " + step.outcome().failure());
	}

	/** 最近 8 步的轨迹摘要（label→节点/状态/页），用于门禁失败定位。 / Last-8 step summary for gate triage. */
	private static String stepDump(QuestJourneyRunner journey) {
		List<QuestJourneyRunner.Step> steps = journey.steps();
		int from = Math.max(0, steps.size() - 8);
		StringBuilder dump = new StringBuilder(" | 近程轨迹:");
		for (QuestJourneyRunner.Step step : steps.subList(from, steps.size())) {
			String node = step.matchedTransition() == null ? "?" : step.matchedTransition().targetNode();
			dump.append(' ').append(step.label()).append("→").append(node)
				.append('/').append(step.status()).append('/').append("p").append(step.page());
		}
		return dump.toString();
	}

	/** JUnit 断言失败必须携带任务上下文。 / Assertion failures must carry the case context. */
	private static void fail(ContractCase c, String message) {
		throw new AssertionError(c + ": " + message);
	}

	private static Set<Integer> rewardConfirmActions() {
		java.util.Set<Integer> ids = new java.util.TreeSet<>();
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			ids.add(id);
		}
		ids.add(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id());
		for (int id = QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()
					+ QuestDialogAction.AUTO_REWARD_SLOT_COUNT; id++) {
			ids.add(id);
		}
		return Set.copyOf(ids);
	}
}
