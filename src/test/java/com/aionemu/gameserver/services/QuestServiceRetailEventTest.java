package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

class QuestServiceRetailEventTest {

	@Test
	void notifiesOnlyTheNpcThatSettledTheQuest() {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		Npc npc = objenesis.newInstance(Npc.class);
		RecordingAI ai = new RecordingAI();
		npc.setAi2(ai);

		QuestService.notifyQuestFinished(new QuestEnv(null, player, 9645, 0));
		QuestService.notifyQuestFinished(new QuestEnv(npc, player, 9645, 0));

		assertEquals(1, ai.calls);
		assertSame(player, ai.player);
		assertEquals(9645, ai.questId);
	}

	private static final class RecordingAI extends AITemplate {

		private int calls;
		private Player player;
		private int questId;

		@Override
		public void onQuestFinished(Player player, int questId) {
			calls++;
			this.player = player;
			this.questId = questId;
		}
	}
}
