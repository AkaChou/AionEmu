package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Real {@link QuestDialogPort}: after commit the player's quest dialog window is
 * closed. When the player has already logged out the close is best-effort skipped.
 */
class PlayerQuestDialogPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void closeDialogSendsWindowCloseToLivePlayer() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		assertDoesNotThrow(() -> port.closeDialog(snapshot(), plan()));
	}

	@Test
	void closeDialogIsBestEffortWhenPlayerLoggedOut() {
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> null);

		assertFalse(port.closeDialog(snapshot(), plan()));
	}

	@Test
	void showDialogSendsWindowWithAuthoritativeObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);
		QuestSnapshot snapshot = snapshot().withInteractionObjectId(204160);

		assertDoesNotThrow(() -> port.showDialog(snapshot, plan(), 1011));
	}

	@Test
	void showDialogAllowsTargetlessQuestDialogWithObjectZero() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		assertDoesNotThrow(() -> port.showDialog(snapshot().withTargetlessDialog(), plan(), 4));
	}

	@Test
	void showSelectionDialogAllowsTargetlessContextWithObjectZero() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		assertDoesNotThrow(() -> port.showSelectionDialog(snapshot().withTargetlessDialog(), plan(), 10));
	}

	@Test
	void showDialogFailsClosedWithoutAuthoritativeObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		// 玩家在线但缺少权威交互 objectId (interactionObjectId == 0) 时必须 fail closed,
		// 禁止用 NPC templateId 或玩家 target 猜测。
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> port.showDialog(snapshot(), plan(), 1011));
		assertEquals(true, error.getMessage().contains("authoritative interaction objectId"));
	}

	@Test
	void showDialogIsBestEffortWhenPlayerLoggedOut() throws Exception {
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> null);

		assertFalse(port.showDialog(snapshot().withInteractionObjectId(204160), plan(), 1011));
	}

	@Test
	void showSelectionDialogUsesTheAuthoritativeObjectAndNoQuestIdProtocol() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		assertDoesNotThrow(() -> port.showSelectionDialog(
			snapshot().withInteractionObjectId(204160), plan(), 10));
	}

	@Test
	void showSelectionDialogFailsClosedWithoutAuthoritativeObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestDialogPort port = new PlayerQuestDialogPort(playerId -> player);

		assertThrows(IllegalStateException.class, () -> port.showSelectionDialog(snapshot(), plan(), 10));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0,
			List.of(), List.of(new AfterCommitAction.CloseDialog()));
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
