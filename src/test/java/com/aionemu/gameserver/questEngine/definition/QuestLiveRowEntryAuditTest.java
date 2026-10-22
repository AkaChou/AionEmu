package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectTestData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归审计：进行中步骤的入口动作必须是客户端真会发的那个。
 * <p>
 * 客户端进入「进行中」任务只有两条路：任务列表行点击（{@code QUEST_SELECT(31)}，coin_quest 行是
 * {@code EXCHANGE_COIN(59)}）与交互物自身的对话选择（物件 AI 发 {@code USE_OBJECT(-1)}）。开门
 * （点 NPC）只重放可交付（REWARD）任务，进行中任务不再被开门认领（见 {@code QuestDialogRouter} 的
 * 「可接取/进行中的任务一律不参与开门」）。因此把进行中步骤挂在 {@code USE_OBJECT(-1)} 上、而该 NPC
 * 又没有任务行入口时，这一步在实机上永远点不动——2026-10-22 任务 1006 贝尔帕坦（205000）即此形。
 * <p>
 * 本审计扫描生产目录，把这类「死步骤」逐条列出来；已审计的例外（对话平面、传送门、物件交互、
 * 未接线的任务页）逐条列在 {@link #AUDITED_EXCEPTIONS}，新增同类形状会在这里变红。
 * <p>
 * Regression audit: a live step must be entered by an action the client really sends. The client enters
 * a live quest through a task-list row click ({@code QUEST_SELECT(31)}, or {@code EXCHANGE_COIN(59)}
 * for coin_quest rows) or through an interaction object's own dialog select ({@code USE_OBJECT(-1)});
 * the npc open only replays deliverable (REWARD) quests. A live step bound to {@code USE_OBJECT(-1)} at
 * an npc without a task-row entry is therefore unclickable (live quest 1006, Belpartan 205000).
 */
class QuestLiveRowEntryAuditTest {

	/** 客户端任务行动作：任务列表行（31）与 coin_quest 行（59）。 / The client task-row actions. */
	private static final Set<Integer> ROW_ACTIONS = Set.of(
		QuestDialogAction.QUEST_SELECT.id(), QuestDialogAction.EXCHANGE_COIN.id());

	/**
	 * 交互物 AI：物件自己会发出 {@code USE_OBJECT(-1)} 对话选择，这些 NPC 上的 -1 路由可达。
	 * Interaction-object AIs: the object sends its own {@code USE_OBJECT(-1)} dialog select, so the
	 * -1 routes on those npcs are reachable.
	 */
	private static final Set<String> INTERACTION_OBJECT_AIS = Set.of(
		"quest_use_item", "quest_start_use_item", "scroll_q41", "scroll_q49", "scroll_q2498",
		"npc_ai_box_q1559", "npc_ai_fobj_q11036a", "npc_ai_fobj_q11123a", "npc_ai_fobj_q11143a");

	/**
	 * 已审计的例外（{@code questId/npcId/sourceNode}）：
	 * <ul>
	 * <li>1929/205111/equipped96：污名对话平面，-1 是「结束对话」的反应，不是步骤入口。</li>
	 * <li>2620/204787/s1：交付由 {@code SELECT_QUEST_REWARD(1009)} 承担，-1 只补默认页。</li>
	 * <li>2919/204206/bookInserted、3200/700522/s2、18602/730308/s1、36506/206134/started、
	 * 36510/700761/started、46511/700775/started、50019/701466/s1、50020/701466/s1、51019/701467/s1：
	 * 书本/箱子/药瓶/残骸/卷轴/可交互道具面，客户端走物件交互而非任务行。</li>
	 * <li>24026/700183/started、24051/700359/s5、24154/700359/v2：传送门 AI 自带开门页轴。</li>
	 * <li>80300/831402/s1、80301/831402/s1、80306/831403/s1、80307/831403/s1：巧克力塔任务页从未接线。</li>
	 * </ul>
	 * The audited exceptions ({@code questId/npcId/sourceNode}).
	 */
	private static final Set<String> AUDITED_EXCEPTIONS = Set.of(
		"1929/205111/equipped96",
		"2620/204787/s1",
		"2919/204206/bookInserted",
		"3200/700522/s2",
		"18602/730308/s1",
		"36506/206134/started",
		"36510/700761/started",
		"46511/700775/started",
		"50019/701466/s1",
		"50020/701466/s1",
		"51019/701467/s1",
		"24026/700183/started",
		"24051/700359/s5",
		"24154/700359/v2",
		"80300/831402/s1",
		"80301/831402/s1",
		"80306/831403/s1",
		"80307/831403/s1");

	@Test
	void everyLiveStepAtATalkNpcIsEnteredByATaskRowAction() throws Exception {
		IntFunction<String> aiNames = QuestInteractionObjectTestData.npcAiNames(getClass().getClassLoader());
		List<String> dead = new ArrayList<>();
		int examined = 0;
		for (QuestCatalogEntry entry : ProductionQuestDefinitions.catalog().entries()) {
			if (entry.executable().isEmpty()) {
				continue;
			}
			QuestDefinition definition = entry.executable().get().definition();
			Map<String, QuestStatus> statusByLabel = new HashMap<>();
			for (QuestNode node : definition.nodes()) {
				statusByLabel.put(node.label(), node.projection().status());
			}
			Set<String> rowRoutes = new HashSet<>();
			for (QuestTransition transition : definition.transitions()) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
						&& ROW_ACTIONS.contains(talk.dialogId())) {
					rowRoutes.add(routeKey(talk.npcId(), transition.sourceNode()));
				}
			}
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| !Integer.valueOf(QuestDialogAction.USE_OBJECT.id()).equals(talk.dialogId())) {
					continue;
				}
				String source = transition.sourceNode();
				if (source == null || statusByLabel.get(source) != QuestStatus.START) {
					continue;
				}
				examined++;
				if (rowRoutes.contains(routeKey(talk.npcId(), source))) {
					continue;
				}
				String ai = aiNames.apply(talk.npcId());
				if (ai != null && INTERACTION_OBJECT_AIS.contains(ai)) {
					continue;
				}
				String key = entry.id() + "/" + talk.npcId() + "/" + source;
				if (!AUDITED_EXCEPTIONS.contains(key)) {
					dead.add(key);
				}
			}
		}
		assertTrue(examined > 0, "审计没有考察到任何进行中的 -1 路由，规则本身可能已经失效");
		assertTrue(dead.isEmpty(),
			() -> "进行中步骤只挂在客户端不会发的 USE_OBJECT(-1) 上（该 NPC 没有任务行入口）：" + dead);
	}

	private static String routeKey(int npcId, String sourceNode) {
		return npcId + "/" + sourceNode;
	}
}
