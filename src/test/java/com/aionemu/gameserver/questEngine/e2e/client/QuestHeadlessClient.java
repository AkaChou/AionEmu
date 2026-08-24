package com.aionemu.gameserver.questEngine.e2e.client;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.List;

/**
 * 仅沿 Aion 5.8 客户端可见动作驱动任务的无头客户端，不自行推断下一页或服务器状态。
 * Headless client that drives quests only through Aion 5.8 client-visible actions and never infers the next page or
 * server state.
 */
public final class QuestHeadlessClient {
	private final VirtualClientState state;
	private final ClientResourceOracle oracle;
	private final ActionBridge bridge;
	private final QuestTrace trace;

	/** 客户端动作到任务运行时的同步桥。 / Synchronous bridge from client actions to the quest runtime. */
	@FunctionalInterface
	public interface ActionBridge {
		DispatchOutcome dispatch(ClientActionRequest request);
	}

	/** 一次客户端请求的不可变执行结果。 / Immutable outcome of one client request. */
	public record DispatchOutcome(boolean handled, boolean failed, boolean stateChanged,
			RuntimeException failure, List<ServerPacketObservation> packets) {
		public DispatchOutcome {
			packets = List.copyOf(packets);
		}
	}

	public QuestHeadlessClient(VirtualClientState state, ClientResourceOracle oracle,
			ActionBridge bridge, QuestTrace trace) {
		this.state = java.util.Objects.requireNonNull(state, "state");
		this.oracle = java.util.Objects.requireNonNull(oracle, "oracle");
		this.bridge = java.util.Objects.requireNonNull(bridge, "bridge");
		this.trace = java.util.Objects.requireNonNull(trace, "trace");
	}

	/** 点击指定 NPC 的任务 action。 / Clicks a quest action on the specified NPC. */
	public DispatchOutcome clickNpc(int npcId, int objectId, int questId, int dialogId) {
		if (questId != state.questId()) {
			throw new IllegalArgumentException("questId does not belong to this client state");
		}
		state.interactWith(npcId, objectId);
		return dispatch(ClientActionRequest.dialog(questId, npcId, objectId, dialogId));
	}

	/**
	 * 点击不依赖 NPC 或交互对象的客户端原生任务动作。
	 * Clicks a native client quest action that has no NPC or interaction-object target.
	 */
	public DispatchOutcome clickTargetlessAction(int actionId) {
		if (!oracle.actionExists(actionId)) {
			throw new IllegalArgumentException("action does not exist in the Aion 5.8 client action table");
		}
		return dispatch(ClientActionRequest.targetlessDialog(state.questId(), actionId));
	}

	/**
	 * 点击当前客户端页面上实际可见的 action。
	 * Clicks an action actually visible on the current client page.
	 */
	public DispatchOutcome clickPageAction(int actionId) {
		boolean visible = oracle.visibleActions(state.questId(), state.currentPage()).stream()
			.anyMatch(action -> action.actionId() == actionId && oracle.actionExists(action.actionId()));
		if (!visible) {
			throw new IllegalArgumentException("action is not visible on the current Aion 5.8 client page");
		}
		if (state.currentNpcId() <= 0) {
			return dispatch(ClientActionRequest.targetlessDialog(state.questId(), actionId));
		}
		if (state.currentObjectId() <= 0 || state.currentPageTargetObjectId() <= 0
				|| state.currentObjectId() != state.currentPageTargetObjectId()) {
			throw new IllegalStateException("page action target does not match the authoritative interaction object");
		}
		return dispatch(ClientActionRequest.dialog(state.questId(), state.currentNpcId(),
			state.currentObjectId(), actionId));
	}

	/**
	 * 发送客户端页面上唯一的结束对话动作，并在服务端按历史合同无 route、无响应时由客户端本地关闭页面。
	 * Sends the visible finish-dialog action and closes the page locally only when the server deliberately has no
	 * route or response under the historical contract.
	 */
	public DispatchOutcome finishDialogLocally() {
		int actionId = QuestDialogAction.FINISH_DIALOG.id();
		boolean onlyFinishAction = !oracle.visibleActions(state.questId(), state.currentPage()).isEmpty()
			&& oracle.visibleActions(state.questId(), state.currentPage()).stream()
			.allMatch(action -> action.actionId() == actionId);
		if (!onlyFinishAction) {
			throw new IllegalStateException("client-local finish requires a page whose only visible action is FINISH_DIALOG");
		}
		DispatchOutcome outcome = clickPageAction(actionId);
		if (outcome.failed()) return outcome;
		if (outcome.handled() || outcome.stateChanged() || !outcome.packets().isEmpty()) {
			throw new IllegalStateException("client-local finish unexpectedly received a server route or response");
		}
		state.closePage();
		trace.add("CLIENT", "local-finish-dialog:" + actionId);
		return outcome;
	}

	/**
	 * 点击奖励窗口中由原生客户端控件发送的 action；该控件不出现在任务 HTML 的
	 * {@code <Act>} 列表中。
	 * Clicks an action emitted by a native reward-window control; the control is absent from the quest HTML
	 * {@code <Act>} list.
	 */
	public DispatchOutcome clickNativeAction(int actionId) {
		if (state.currentPage() != QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()) {
			throw new IllegalStateException("native reward action requires the reward-selection window");
		}
		if (actionId < QuestDialogAction.SELECTED_QUEST_REWARD1.id()
				|| actionId > QuestDialogAction.SELECTED_QUEST_NOREWARD.id()
				|| !oracle.actionExists(actionId)) {
			throw new IllegalArgumentException("action is not a native Aion 5.8 reward-selection action");
		}
		if (state.currentNpcId() <= 0) {
			return dispatch(ClientActionRequest.targetlessDialog(state.questId(), actionId));
		}
		if (state.currentObjectId() <= 0 || state.currentPageTargetObjectId() <= 0
				|| state.currentObjectId() != state.currentPageTargetObjectId()) {
			throw new IllegalStateException("native reward action target does not match the authoritative interaction object");
		}
		return dispatch(ClientActionRequest.dialog(state.questId(), state.currentNpcId(),
			state.currentObjectId(), actionId));
	}

	/** 完成任务交互物的使用动作。 / Completes a quest interaction-object use action. */
	public DispatchOutcome useObject(int npcId, int objectId) {
		state.interactWith(npcId, objectId);
		return dispatch(ClientActionRequest.useObject(state.questId(), npcId, objectId));
	}

	/**
	 * 使用需要 ACTION_ITEM_USE gate 的任务交互物，并保留同一个权威 objectId。
	 * Uses a quest interaction object that requires the ACTION_ITEM_USE gate while retaining the same authoritative
	 * object ID.
	 */
	public DispatchOutcome useActionItemObject(int npcId, int objectId) {
		state.interactWith(npcId, objectId);
		return dispatch(ClientActionRequest.actionItemUse(state.questId(), npcId, objectId));
	}

	/** 使用背包中的任务物品。 / Uses a quest item from inventory. */
	public DispatchOutcome useItem(int itemId, int itemObjectId) {
		return dispatch(ClientActionRequest.useItem(state.questId(), itemId, itemObjectId));
	}

	/** 使用背包中的任务物品并等待真实客户端读条完成。 / Uses an inventory quest item and waits for real client cast completion. */
	public DispatchOutcome playItem(int itemId, int itemObjectId, int animationMillis) {
		return dispatch(ClientActionRequest.itemPlay(state.questId(), itemId, itemObjectId, animationMillis));
	}

	/** 注入确定性的世界事件。 / Emits a deterministic world event. */
	public DispatchOutcome emitWorldEvent(QuestEvent event) {
		return dispatch(ClientActionRequest.world(state.questId(), event));
	}

	private DispatchOutcome dispatch(ClientActionRequest request) {
		trace.add("CLIENT", request.kind() + ":" + request.event());
		return bridge.dispatch(request);
	}
}
