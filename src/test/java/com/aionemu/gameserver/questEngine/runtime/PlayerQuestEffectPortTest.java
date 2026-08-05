package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Real player-effect boundary checks for authoritative interaction targets. */
class PlayerQuestEffectPortTest {
	@Test
	void appliesAndRemovesEffectsThroughTheProductionBoundary() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<String> calls = new java.util.ArrayList<>();
		PlayerQuestEffectPort port = new PlayerQuestEffectPort(playerId -> player,
			new PlayerQuestEffectPort.EffectOperations() {
				@Override
				public void apply(Player target, int skillId, int durationMillis) {
					calls.add("apply:" + skillId + ":" + durationMillis + ":" + (target == player));
				}

				@Override
				public void remove(Player target, int effectId) {
					calls.add("remove:" + effectId + ":" + (target == player));
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 14114, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(14114, QuestStatus.START, 1, List.of(), List.of());

		port.applyEffect(snapshot, plan, 8197, 0);
		port.removeEffect(snapshot, plan, 8197);

		assertEquals(List.of("apply:8197:0:true", "remove:8197:true"), calls);
	}

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
