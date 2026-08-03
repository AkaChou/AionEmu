package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Real player-effect boundary checks for authoritative interaction targets. */
class PlayerQuestEffectPortTest {
	@Test
	void playerEmotionFailsClosedWithoutAnAuthoritativeInteractionObject() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerQuestEffectPort port = new PlayerQuestEffectPort(playerId -> player);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1004, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1004, QuestStatus.START, 0, List.of(), List.of());

		assertThrows(IllegalStateException.class,
			() -> port.playerEmotion(snapshot, plan, QuestPlayerEmotion.STAND));
	}
}
