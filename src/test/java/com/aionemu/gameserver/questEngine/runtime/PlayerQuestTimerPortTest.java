package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestTimerPort}: after commit starts/cancels quest timers on the
 * player. 玩家离线时 best-effort 跳过;超时由权威事件回调。
 */
class PlayerQuestTimerPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void timerCommandsIssueThroughPort() {
		List<PlayerQuestTimerPort.Command> issued = new ArrayList<>();
		List<Integer> seconds = new ArrayList<>();
		Player player = player();
		PlayerQuestTimerPort port = new PlayerQuestTimerPort(playerId -> player,
			(p, questId, secs, command, policy, identity) -> {
				issued.add(command);
				seconds.add(secs);
				return true;
			});

		assertTrue(port.startQuestTimer(snapshot(), plan(), 300));
		assertTrue(port.startInvisibleTimer(snapshot(), plan(), 60));
		assertTrue(port.cancelQuestTimer(snapshot(), plan()));
		assertEquals(List.of(PlayerQuestTimerPort.Command.START_QUEST,
			PlayerQuestTimerPort.Command.START_INVISIBLE,
			PlayerQuestTimerPort.Command.CANCEL_QUEST), issued);
		assertEquals(List.of(300, 60, 0), seconds);
	}

	@Test
	void timerIsBestEffortWhenPlayerLoggedOut() {
		PlayerQuestTimerPort port = new PlayerQuestTimerPort(playerId -> null,
			(p, questId, secs, command, policy, identity) -> true);

		assertFalse(port.startQuestTimer(snapshot(), plan(), 300));
		assertFalse(port.cancelQuestTimer(snapshot(), plan()));
	}

	@Test
	void timerPortFailsClosedOnInvalidSeconds() {
		PlayerQuestTimerPort port = new PlayerQuestTimerPort(playerId -> player(),
			(p, questId, secs, command, policy, identity) -> true);

		assertThrows(IllegalArgumentException.class, () -> port.startQuestTimer(snapshot(), plan(), 0));
		assertThrows(IllegalArgumentException.class, () -> port.startInvisibleTimer(snapshot(), plan(), -5));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0, List.of(), List.of());
	}

	private static Player player() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setPosition(new WorldPosition(110010000));
		return player;
	}
}
