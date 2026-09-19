package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;

import java.util.List;
import java.util.stream.Stream;

/**
 * 上交检查成功页的客户端合同工具：Aion 5.8 客户端把 check_user_item_ok(10000) 页的唯一按钮渲染为
 * HACTION_FINISH_DIALOG(1008) 时，点击只本地关闭窗口（不发任务动作）；只要该 NPC 在目标节点还有续接页，
 * 成功分支就必须直接下发该续接页，否则玩家必须重新对话。
 * Client contract helper for the hand-over success page: when the Aion 5.8 client renders the only
 * check_user_item_ok(10000) button as HACTION_FINISH_DIALOG(1008), clicking only closes the window locally
 * without a quest action; whenever the same NPC still has a continuation page in the target node, the success
 * branch must show that page directly, otherwise the player has to re-open the dialogue.
 */
public final class HandoverContinuationContract {
	private HandoverContinuationContract() {
	}

	/** 该任务客户端 check_user_item_ok 页是否只有本地关闭按钮。 / Whether the page only offers the client-local close. */
	public static boolean closesLocally(ClientResourceOracle oracle, int questId) {
		List<ClientResourceOracle.ClientAction> actions = oracle.visibleActions(questId,
			QuestDialogPage.CHECK_USER_ITEM_OK.id());
		return !actions.isEmpty()
			&& actions.stream().allMatch(action -> action.actionId() == QuestDialogAction.FINISH_DIALOG.id());
	}

	/** 目标节点上同一 NPC 的“打开对话”入口页，即玩家重新对话本来就会看到的页。 */
	public static List<Integer> sameNpcEntryPages(QuestDefinition definition, String node, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> node.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc open
				&& open.npcId() == npcId && open.dialogId() != null
				&& open.dialogId() == QuestDialogAction.USE_OBJECT.id())
			.flatMap(candidate -> candidate.afterCommit().stream())
			.flatMap(action -> {
				if (action instanceof AfterCommitAction.ShowQuestDialog show) {
					return Stream.of(show.dialogId());
				}
				if (action instanceof AfterCommitAction.ShowQuestSelectionDialog show) {
					return Stream.of(show.dialogId());
				}
				return Stream.empty();
			})
			.distinct()
			.toList();
	}

	/**
	 * 交付检查成功分支应下发的页：客户端确认页是本地关闭且同一 NPC 有续接页时用续接页，
	 * 其余情况保留客户端的确认页 10000。
	 * Page a successful hand-over check must show: the same-NPC continuation page when the client
	 * confirmation page is a local close and that continuation exists, otherwise the confirmation page 10000.
	 */
	public static int handOverSuccessPage(QuestDefinition definition, int npcId, ClientResourceOracle oracle) {
		int confirmationPage = QuestDialogPage.CHECK_USER_ITEM_OK.id();
		if (!closesLocally(oracle, definition.id())) {
			return confirmationPage;
		}
		for (QuestTransition transition : definition.transitions()) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.npcId() != npcId
				|| talk.dialogId() == null
				|| talk.dialogId() != QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()) {
				continue;
			}
			List<Integer> continuation = sameNpcEntryPages(definition, transition.targetNode(), npcId);
			if (!continuation.isEmpty()) {
				return continuation.get(0);
			}
		}
		return confirmationPage;
	}
}
