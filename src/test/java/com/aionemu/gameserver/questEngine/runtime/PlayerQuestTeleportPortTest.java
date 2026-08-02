package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestTeleportPort}: after commit the player is teleported to the
 * compiled world coordinates. A logged-out player is best-effort skipped.
 */
class PlayerQuestTeleportPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void teleportsLivePlayerToCompiledCoordinates() throws Exception {
		Player player = emptyPlayer();
		String[] recorded = new String[1];
		PlayerQuestTeleportPort port = new PlayerQuestTeleportPort(playerId -> player,
			(p, worldId, x, y, z, heading) -> {
				recorded[0] = worldId + ":" + x + ":" + y + ":" + z + ":" + heading;
				return true;
			});

		boolean initiated = port.teleportPlayer(snapshot(), plan(), 110010000, 1474f, 1352f, 564f, (byte) 21);

		assertTrue(initiated);
		assertEquals("110010000:1474.0:1352.0:564.0:21", recorded[0]);
	}

	@Test
	void teleportIsBestEffortWhenPlayerLoggedOut() {
		PlayerQuestTeleportPort port = new PlayerQuestTeleportPort(playerId -> null);

		assertFalse(port.teleportPlayer(snapshot(), plan(), 110010000, 1474f, 1352f, 564f, (byte) 21));
	}

	@Test
	void teleportFailsClosedOnInvalidWorldId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestTeleportPort port = new PlayerQuestTeleportPort(playerId -> player,
			(p, worldId, x, y, z, heading) -> true);

		assertThrows(IllegalArgumentException.class,
			() -> port.teleportPlayer(snapshot(), plan(), 0, 1474f, 1352f, 564f, (byte) 21));
	}

	@Test
	void fixedInstanceTargetIsPassedWithoutUsingLivePlayerPosition() throws Exception {
		Player player = emptyPlayer();
		int[] capturedInstance = {0};
		PlayerQuestTeleportPort port = new PlayerQuestTeleportPort(playerId -> player,
			(PlayerQuestTeleportPort.TeleportCall) (p, worldId, instanceId, x, y, z, heading) -> {
				capturedInstance[0] = instanceId;
				return true;
			});

		assertTrue(port.teleportPlayer(snapshot(), plan(), QuestInstanceTarget.fixed(37),
			220040000, 1f, 2f, 3f, (byte) 4));
		assertEquals(37, capturedInstance[0]);
	}

	@Test
	void teleportAfterCommitRoutesThroughTypedPort() {
		// 经 TypedQuestAfterCommitPort 路由: commit 后 action 才能触达 teleport port。
		boolean[] called = {false};
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(
			new QuestDialogPort() {
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
			},
			(snapshot, plan, worldId, x, y, z, heading) -> {
				called[0] = true;
				return true;
			});
		port.execute(new AfterCommitAction.TeleportPlayer(110010000, 1f, 2f, 3f, (byte) 0), snapshot(), plan());
		assertTrue(called[0]);
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of(),
			true, true, 0, 0, 110010000, 3, 0f, 0f, 0f, (byte) 0);
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0, List.of(), List.of());
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "equipment", new ObjenesisStd().newInstance(Equipment.class));
		setField(Player.class, player, "regularWarehouse", new PlayerStorage(StorageType.REGULAR_WAREHOUSE));
		setField(Player.class, player, "accountWarehouse", new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE));
		setField(Player.class, player, "petBag",
			new Storage[StorageType.PET_BAG_MAX - StorageType.PET_BAG_MIN + 1]);
		setField(Player.class, player, "cabinets",
			new Storage[StorageType.HOUSE_WH_MAX - StorageType.HOUSE_WH_MIN + 1]);
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
