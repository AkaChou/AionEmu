package com.aionemu.gameserver.questEngine.handlers.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class ReportToTest {

	@Test
	void firstBatchSameNpcQuestsUseTheTurnInBranchWhileStarted() {
		for (int questId : List.of(80586, 80591)) {
			Player player = new ObjenesisStd().newInstance(Player.class);
			QuestStateList states = new QuestStateList();
			QuestState started = new StartedQuestState(questId);
			states.addQuest(questId, started);
			player.setQuestStateList(states);
			RecordingReportTo reportTo = new RecordingReportTo(questId);

			assertTrue(reportTo.onDialogEvent(new FixedQuestEnv(player, questId)));
			assertEquals(2375, reportTo.sentDialogId);
		}
	}

	private static final class StartedQuestState extends QuestState {

		private StartedQuestState(int questId) {
			super(questId, QuestStatus.START, 0, 0, null, 0, null);
		}

		@Override
		public boolean canRepeat() {
			return false;
		}
	}

	private static final class RecordingReportTo extends ReportTo {

		private int sentDialogId;

		private RecordingReportTo(int questId) {
			super(questId, List.of(832267), List.of(832267), 0, 0, 0);
		}

		@Override
		public boolean sendQuestDialog(QuestEnv env, int dialogId) {
			sentDialogId = dialogId;
			return true;
		}
	}

	private static final class FixedQuestEnv extends QuestEnv {

		private FixedQuestEnv(Player player, int questId) {
			super(null, player, questId, QuestDialog.START_DIALOG.id());
		}

		@Override
		public int getTargetId() {
			return 832267;
		}

		@Override
		public QuestDialog getDialog() {
			return QuestDialog.START_DIALOG;
		}
	}
}
