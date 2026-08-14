package com.aionemu.gameserver.questEngine.runtime;

/** 对话协议动作的类型化 best-effort 边界。 / Typed, best-effort boundary for the dialog protocol actions. */
public interface QuestDialogPort {
	boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan);

	/**
	 * 打开指定对话页。objectId 必须来自当前执行上下文的权威交互对象（绝不猜测目标）。
	 * Opens the given dialog page. The objectId must come from the authoritative
	 * interaction object of the current execution context (never a guessed target).
	 */
	boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId);

	/** 打开不带任务 ID 的对话页，用于任务选择协议。 / Opens a dialog page without the quest id, as used by the quest-selection protocol. */
	boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId);

	/**
	 * 打开任意不附加任务 ID 的 SM_DIALOG_WINDOW 页。不需要原始数据包的实现
	 * 可将常规任务对话路径作为保守兼容默认。
	 * Opens an arbitrary SM_DIALOG_WINDOW page without attaching a quest id.
	 * Implementations that do not need the raw packet may use the regular quest
	 * dialog path as a conservative compatibility default.
	 */
	default boolean showDialogWindow(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		return showDialog(snapshot, plan, dialogId);
	}
}
