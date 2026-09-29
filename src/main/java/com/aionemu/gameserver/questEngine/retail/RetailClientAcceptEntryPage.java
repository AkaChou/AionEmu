package com.aionemu.gameserver.questEngine.retail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 接取入口页的客户端契约（未接态首屏页必须落在客户端实际声明的页上）。
 * <p>
 * 真端表只有"接取 NPC/物品"列，没有对话页列，因此合成器给出的是真端原生相位 A——未接态
 * {@code QUEST_SELECT}（物品接取形为 {@code USE_ITEM}）直发接取窗页 {@link #ASK_WINDOW_PAGE}。
 * 但 5.8 客户端按任务页 HTML 解析该页：只有 {@code ask_quest_accept} 页的任务能这样开窗，
 * 其余任务页里往往是 {@code select_none}(4762) 或 {@code select1}(1011)——发一个客户端任务页里
 * 不存在的页，客户端会直接报 {@code load fail}（例：80787 家族任务页只有 select_none）。
 * 本类按客户端页契约（{@link QuestDialogContract}）把接取入口页改发客户端实际声明的那一页；
 * select1 首屏没有接受按钮时，再按同一契约补齐 1012/1013 页面翻页边。除这些接取入口边外，
 * 路由目标/条件/动作与完成流都不动。
 * <p>
 * Client contract for the accept entry page (the unaccepted first page must exist in the quest's
 * client task HTML). The retail tables carry no dialog-page column, so the synthesizer emits the
 * native phase A shape — unaccepted {@code QUEST_SELECT} (or {@code USE_ITEM} for item-acquired
 * rows) pops the ask window page {@link #ASK_WINDOW_PAGE}. The 5.8 client resolves that page inside
 * the quest's task HTML: only quests declaring {@code ask_quest_accept} can open it; the rest
 * declare {@code select_none}(4762) or {@code select1}(1011) and fail with {@code load fail}
 * (e.g. the 80787 family declares select_none only). This class re-points the accept entry at the
 * page the client actually declares; select1 entry points also receive client-declared 1012/1013
 * page-turn edges. Aside from these accept-entry edges, targets, conditions, actions, and completion
 * flows remain untouched.
 */
public final class RetailClientAcceptEntryPage {

	/** 真端接取窗页（客户端 {@code ask_quest_accept}）。 / The native ask window page ({@code ask_quest_accept}). */
	public static final int ASK_WINDOW_PAGE = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
	/** 极简信页 {@code select_none}(4762)：接取/拒绝按钮是 20000/20001。 /
	 * The minimal letter page {@code select_none}(4762) whose accept/refuse buttons are 20000/20001. */
	public static final int SELECT_NONE_PAGE = QuestDialogPage.SELECT_NONE.id();
	/** 对话入口页 {@code select1}(1011)。 / The dialog entry page {@code select1}(1011). */
	public static final int SELECT1_PAGE = QuestDialogPage.SELECT1.id();
	/** {@code select1} 的续页 {@code select1_1}(1012)。 / The {@code select1} continuation page. */
	private static final int SELECT1_1_PAGE = QuestDialogPage.SELECT1_1.id();
	/** {@code select1_1} 的续页 {@code select1_1_1}(1013)。 / The {@code select1_1} continuation page. */
	private static final int SELECT1_1_1_PAGE = QuestDialogPage.SELECT1_1_1.id();

	private RetailClientAcceptEntryPage() {
	}

	/**
	 * 该任务未接态第一屏应下发的页：客户端声明了哪一页就发哪一页。
	 * The unaccepted first page for this quest: whichever page the client declares.
	 */
	public static int entryPage(int questId, QuestDialogContract contract) {
		if (contract.hasButtonPage(questId, ASK_WINDOW_PAGE)) {
			return ASK_WINDOW_PAGE;
		}
		if (contract.hasButtonPage(questId, SELECT_NONE_PAGE)) {
			return SELECT_NONE_PAGE;
		}
		if (contract.hasButtonPage(questId, SELECT1_PAGE)) {
			return SELECT1_PAGE;
		}
		// 客户端页契约无登记时保持合成器既有页（不改动无法判定的行）。
		// Without a client page registry row the synthesizer's page is kept (undecidable rows stay put).
		return ASK_WINDOW_PAGE;
	}

	/**
	 * 修复接取入口页；无改动时原样返回。
	 * Repairs the accept entry page; returns the input unchanged when nothing needs changing.
	 */
	public static CompiledQuestDefinition repair(CompiledQuestDefinition compiled, QuestDialogContract contract) {
		QuestDefinition definition = compiled.definition();
		int entryPage = entryPage(definition.id(), contract);
		if (entryPage == ASK_WINDOW_PAGE) {
			return compiled;
		}
		Map<String, QuestStatus> statuses = new LinkedHashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		List<QuestTransition> transitions = new ArrayList<>(definition.transitions().size());
		List<QuestTransition> continuations = new ArrayList<>(2);
		boolean changed = false;
		for (QuestTransition transition : definition.transitions()) {
			QuestTransition repaired = withEntryPage(statuses, transition, entryPage);
			changed |= repaired != transition;
			transitions.add(repaired);
			if (isAcceptEntryEdge(statuses, repaired) && isAcceptTrigger(repaired.event())
					&& showsPage(repaired, entryPage)) {
				addClientContinuations(definition.id(), repaired, contract, transitions, continuations);
			}
		}
		if (!changed && continuations.isEmpty()) {
			return compiled;
		}
		transitions.addAll(continuations);
		return QuestDefinitionCompiler.compile(new QuestDefinition(definition.id(), definition.version(),
			definition.metadata(), definition.progressLayout(), definition.nodes(), transitions));
	}

	/**
	 * 按客户端页契约补齐 select1 接取梯：1011 首屏本身没有接受按钮，必须先登记 1012（以及客户端
	 * 继续声明的 1013）页面翻页边；接受/拒绝动作仍由既有 canonical 接取流处理。重复路由不登记，
	 * SimpleTalk 等已有客户端续页梯的家族保持原定义。
	 * Adds the client-declared select1 accept ladder: page 1011 has no accept button, so page-turn
	 * routes for 1012 (and 1013 when the client declares it) are required; the existing canonical
	 * flow still owns accept/refuse. Existing ladders (for example SimpleTalk) are left unchanged.
	 */
	private static void addClientContinuations(int questId, QuestTransition entry, QuestDialogContract contract,
			List<QuestTransition> existing, List<QuestTransition> additions) {
		if (!(entry.event() instanceof QuestEvent.TalkToNpc talk)) {
			return;
		}
		int npcId = talk.npcId();
		for (int pageId : List.of(SELECT1_1_PAGE, SELECT1_1_1_PAGE)) {
			QuestDialogAction action = QuestDialogAction.findId(pageId);
			if (action == null || !contract.hasButtonPage(questId, pageId)) {
				continue;
			}
			QuestTransition continuation = new QuestTransition(
				new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), entry.targetNode(),
				List.of(new AfterCommitAction.ShowQuestDialog(pageId)), null, entry.sourceNode());
			if (hasCompatibleRoute(existing, continuation) || hasCompatibleRoute(additions, continuation)) {
				continue;
			}
			additions.add(continuation);
		}
	}

	/** 判断已有路由是否已覆盖同 NPC、同动作、同来源的客户端续页。 / Whether an equivalent route already exists. */
	private static boolean hasCompatibleRoute(List<QuestTransition> transitions, QuestTransition candidate) {
		if (!(candidate.event() instanceof QuestEvent.TalkToNpc expected)) {
			return false;
		}
		return transitions.stream().anyMatch(transition -> {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc actual)
					|| actual.npcId() != expected.npcId()
					|| !Objects.equals(actual.dialogId(), expected.dialogId())) {
				return false;
			}
			// 隐式来源会覆盖全部节点；显式来源不同才可能互斥并允许并存。
			// An implicit source covers every node; only distinct explicit sources may coexist.
			return transition.sourceNode() == null
				|| Objects.equals(transition.sourceNode(), candidate.sourceNode());
		});
	}

	private static boolean showsPage(QuestTransition transition, int pageId) {
		return transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(pageId));
	}

	/** 未接态接取边换页；其余 transition 原样返回（同一实例，便于判等）。 /
	 * Re-points an unaccepted accept edge; other transitions come back as the same instance. */
	static QuestTransition withEntryPage(Map<String, QuestStatus> statuses, QuestTransition transition,
			int entryPage) {
		if (!isAcceptEntryEdge(statuses, transition) || !isAcceptTrigger(transition.event())
				|| !transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(ASK_WINDOW_PAGE))) {
			return transition;
		}
		List<AfterCommitAction> afterCommit = transition.afterCommit().stream()
			.map(action -> action instanceof AfterCommitAction.ShowQuestDialog show
				&& show.dialogId() == ASK_WINDOW_PAGE
					? (AfterCommitAction) new AfterCommitAction.ShowQuestDialog(entryPage)
					: action)
			.toList();
		return new QuestTransition(transition.event(), transition.conditions(), transition.actions(),
			transition.targetNode(), afterCommit, transition.priority(), transition.sourceNode());
	}

	/**
	 * 是否接取入口边：未接态（NONE 投影；无源节点时看 StatusIs(NONE) 条件），或重复任务的再开局
	 * 别名（COMPLETE 投影 + StartEligible——编译期「重复别名」把同一条未接态对话边复制到 complete
	 * 节点而来）。再开局走信页阶梯同样有迁移前 XML 见证，例：15478 的 complete 边发 SELECT_NONE_1。
	 * <p>
	 * Whether this is an accept entry edge: the unaccepted projection (StatusIs(NONE) when the source is
	 * implicit), or a repeat quest's reopen alias (COMPLETE projection carrying StartEligible, copied from
	 * the same unaccepted edge by the compile-time repeat alias). Pre-migration XMLs reopen on the letter
	 * ladder as well, e.g. 15478's complete edge emits SELECT_NONE_1.
	 */
	private static boolean isAcceptEntryEdge(Map<String, QuestStatus> statuses, QuestTransition transition) {
		QuestStatus source;
		if (transition.sourceNode() != null) {
			source = statuses.get(transition.sourceNode());
		} else {
			source = transition.conditions().stream()
				.filter(QuestCondition.StatusIs.class::isInstance)
				.map(QuestCondition.StatusIs.class::cast)
				.map(QuestCondition.StatusIs::status)
				.findFirst()
				.orElse(null);
		}
		if (source == QuestStatus.NONE) {
			return true;
		}
		return source == QuestStatus.COMPLETE && transition.conditions().stream()
			.anyMatch(QuestCondition.StartEligible.class::isInstance);
	}

	/** 接取触发：NPC 任务列表选择（31）或使用任务起始道具。 /
	 * The accept trigger: the npc quest-list selection (31) or the quest-start item use. */
	private static boolean isAcceptTrigger(QuestEvent event) {
		return event instanceof QuestEvent.UseItem
			|| event instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id();
	}
}
