package com.aionemu.gameserver.services.events.bg;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class BattlegroundCollectionsTest {

	@Test
	void participantListsUseSnapshotSafeCollectionsForAsyncEventCallbacks() {
		TestBattleground battleground = new TestBattleground();

		assertAll(
			() -> assertTrue(battleground.players() instanceof CopyOnWriteArrayList),
			() -> assertTrue(battleground.groups() instanceof CopyOnWriteArrayList),
			() -> assertTrue(battleground.alliances() instanceof CopyOnWriteArrayList),
			() -> assertTrue(battleground.spectators() instanceof CopyOnWriteArrayList)
		);
	}

	private static class TestBattleground extends Battleground {

		@Override
		public void createMatch(List<Integer> players) {
		}

		@Override
		public void startMatch() {
		}

		@Override
		public void onDie(Player player, Creature lastAttacker) {
		}

		@Override
		public void onLeave(Player player, boolean isLogout, boolean isAfk) {
		}

		private List<Player> players() {
			return getPlayers();
		}

		private List<?> groups() {
			return getGroups();
		}

		private List<?> alliances() {
			return getAlliances();
		}

		private List<Player> spectators() {
			return getSpectators();
		}
	}
}
