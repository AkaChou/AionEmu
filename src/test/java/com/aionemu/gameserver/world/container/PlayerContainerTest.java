package com.aionemu.gameserver.world.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class PlayerContainerTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void iteratorReturnsSnapshotSafeForRemovalDuringIteration() {
		PlayerContainer players = playerContainerWithThreePlayers();
		Iterator<Player> iterator = players.iterator();

		assertDoesNotThrow(() -> {
			while (iterator.hasNext()) {
				players.remove(iterator.next());
			}
		});
		assertTrue(players.getAllPlayers().isEmpty());
	}

	@Test
	void doOnAllPlayersUsesSnapshotSafeForRemovalDuringVisit() {
		PlayerContainer players = playerContainerWithThreePlayers();
		List<Integer> visited = new ArrayList<Integer>();

		players.doOnAllPlayers(player -> {
			visited.add(player.getObjectId());
			players.remove(player);
		});

		assertEquals(List.of(1, 2, 3), visited);
		assertTrue(players.getAllPlayers().isEmpty());
	}

	@Test
	void getAllPlayersReturnsSnapshotSafeForRemovalDuringIteration() {
		PlayerContainer players = playerContainerWithThreePlayers();
		Collection<Player> snapshot = players.getAllPlayers();

		assertDoesNotThrow(() -> {
			for (Player player : snapshot) {
				players.remove(player);
			}
		});
		assertTrue(players.getAllPlayers().isEmpty());
	}

	private PlayerContainer playerContainerWithThreePlayers() {
		PlayerContainer players = new PlayerContainer();
		players.add(player(1));
		players.add(player(2));
		players.add(player(3));
		return players;
	}

	private Player player(int objectId) {
		try {
			Player player = objenesis.newInstance(Player.class);
			Field objectIdField = AionObject.class.getDeclaredField("objectId");
			objectIdField.setAccessible(true);
			objectIdField.set(player, objectId);
			PlayerCommonData commonData = new PlayerCommonData(objectId);
			commonData.setName("player-" + objectId);
			Field commonDataField = Player.class.getDeclaredField("playerCommonData");
			commonDataField.setAccessible(true);
			commonDataField.set(player, commonData);
			return player;
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError(ex);
		}
	}
}
