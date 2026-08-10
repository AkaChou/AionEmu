package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestRawDialogAfterCommitTest {
	@Test
	void routesRawDialogWindowWithoutQuestIdThroughTypedDialogPort() {
		List<Integer> dialogs = new ArrayList<>();
		QuestDialogPort port = new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}

			@Override
			public boolean showDialogWindow(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				dialogs.add(dialogId);
				return true;
			}
		};
		TypedQuestAfterCommitPort afterCommit = new TypedQuestAfterCommitPort(port);
		QuestSnapshot snapshot = new QuestSnapshot(7, 20034, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(20034, QuestStatus.START, 0, List.of(), List.of());

		afterCommit.execute(new AfterCommitAction.ShowDialogWindow(10009), snapshot, plan);

		assertEquals(List.of(10009), dialogs);
	}

	@Test
	void rejectsNullPageAsRawDialogWindow() {
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.ShowDialogWindow(0));
	}
}
