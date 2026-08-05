package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestSpawnPortRaceTest {
	@Test
	void aRegistrationRaceDoesNotLeaveAnUntrackedNpc() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		QuestSnapshot snapshot = snapshot();
		Npc winner = npc();
		Npc loser = npc();
		Player player = new ObjenesisStd().newInstance(Player.class);

		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> {
				assertTrue(registry.register(snapshot, "guardian", winner));
				return loser;
			});

		assertTrue(port.spawnNpc(snapshot, plan(), "guardian", 310040000,
			new QuestSpawnLocation.Fixed(310040000,
				com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget.currentOrDefault(),
				1f, 2f, 3f, (byte) 0)));
		assertSame(winner, registry.get(snapshot, "guardian"));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(), Map.of(),
			true, true, 0, 0, 110010000, 5, 10f, 20f, 30f, (byte) 0);
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(1001, QuestStatus.COMPLETE, 0, List.of(), List.of());
	}

	private static Npc npc() {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		npc.setPosition(new WorldPosition(310040000));
		return npc;
	}
}
