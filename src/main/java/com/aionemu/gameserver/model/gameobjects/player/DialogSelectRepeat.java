package com.aionemu.gameserver.model.gameobjects.player;

/**
 * 客户端对话选择的重发跟踪快照，用于识别服务端没有下发后续页时客户端反复重发同一个
 * {@code CM_DIALOG_SELECT} 的死循环。
 * Repeat-tracking snapshot of a client dialog selection, used to detect the resend loop that appears
 * when the server never produces a continuation for a selected dialog action.
 *
 * <p>只保存最近一次选择的签名与计数，随玩家对象一起回收，不参与持久化；并发读写按 best-effort
 * 处理，计数偏差只会推迟或漏掉一次断路器判定，不影响任务状态。</p>
 * <p>It stores only the latest selection signature and count, is released together with the player
 * object, and is never persisted. Concurrent reads and writes are best-effort: a stale count can only
 * delay or skip one breaker decision and never affects quest state.</p>
 *
 * @param targetObjectId 交互目标对象 ID / interaction target object id
 * @param lastPage 客户端发包前所在页面 / page shown by the client before sending the packet
 * @param dialogId 对话动作 ID / dialog action id
 * @param questId 客户端携带或记忆的任务 ID / quest id carried or remembered for this selection
 * @param count 连续相同选择次数 / consecutive identical selection count
 * @param lastMillis 最近一次选择的毫秒时间戳 / timestamp of the latest selection in milliseconds
 */
public record DialogSelectRepeat(int targetObjectId, int lastPage, int dialogId, int questId, int count,
		long lastMillis) {
	public DialogSelectRepeat {
		if (count < 1) {
			throw new IllegalArgumentException("count must be positive");
		}
	}

	/** 判断另一次选择是否具有相同签名。/ Checks whether another selection carries the same signature. */
	public boolean matches(int targetObjectId, int lastPage, int dialogId, int questId) {
		return this.targetObjectId == targetObjectId && this.lastPage == lastPage
			&& this.dialogId == dialogId && this.questId == questId;
	}

	/** 返回计数加一、时间戳更新的新快照。/ Returns a new snapshot with an incremented count and refreshed timestamp. */
	public DialogSelectRepeat incremented(long nowMillis) {
		return new DialogSelectRepeat(targetObjectId, lastPage, dialogId, questId, count + 1, nowMillis);
	}
}
