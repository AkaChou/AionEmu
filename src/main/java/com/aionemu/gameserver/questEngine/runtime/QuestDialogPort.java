package com.aionemu.gameserver.questEngine.runtime;

/** Typed, best-effort boundary for the dialog protocol actions. */
public interface QuestDialogPort {
	boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan);

	/**
	 * Opens the given dialog page. The objectId must come from the authoritative
	 * interaction object of the current execution context (never a guessed target).
	 */
	boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId);

	/** Opens a dialog page without the quest id, as used by the quest-selection protocol. */
	boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId);

	/**
	 * Opens an arbitrary SM_DIALOG_WINDOW page without attaching a quest id.
	 * Implementations that do not need the raw packet may use the regular quest
	 * dialog path as a conservative compatibility default.
	 */
	default boolean showDialogWindow(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
		return showDialog(snapshot, plan, dialogId);
	}
}
