package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestEventPort}: the pre-event player facts are frozen into a
 * snapshot that later phases read, so the whole event commit works on a single
 * consistent view of the player state.
 */
class PlayerQuestEventPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void snapshotCapturesFrozenPlayerFacts() throws Exception {
		Player player = playerWithQuestState(QuestStatus.LOCKED, 0x42);
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event());

		assertEquals(PLAYER_ID, snapshot.playerId());
		assertEquals(QUEST_ID, snapshot.questId());
		assertEquals(QuestStatus.LOCKED, snapshot.status());
		assertEquals(0x42, snapshot.packedVariables());
		// 背包、CommonData 和 AbyssRank 都存在时才认为货币事实已完整捕获
		assertTrue(snapshot.inventoryCaptured());
		assertTrue(snapshot.currenciesCaptured());
	}

	@Test
	void snapshotCapturesNoneStateWhenQuestNotStarted() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event());

		assertEquals(QuestStatus.NONE, snapshot.status());
		assertEquals(0, snapshot.packedVariables());
	}

	@Test
	void talkSnapshotCarriesAuthoritativeInteractionObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.TalkToNpc(203700, 1011, 900007));

		assertEquals(900007, snapshot.interactionObjectId());
	}

	@Test
	void useItemSnapshotKeepsDialogTargetlessWhileEventCarriesItemObjectId() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestEvent.UseItem event = new QuestEvent.UseItem(182200501, 900008);
		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event);

		assertEquals(0, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
		assertEquals(900008, event.itemObjectId());
	}

	@Test
	void attackSnapshotCarriesAuthoritativeNpcForLifecycleButKeepsDialogTargetless() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);
		QuestNpcAttackFacts facts = new QuestNpcAttackFacts(
			PLAYER_ID, 900009, 210319, 1000, 1000, 210030000, 1);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.AttackNpc(210319, facts));

		assertEquals(900009, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
	}

	@Test
	void targetlessDialogSnapshotMarksTheProtocolBoundary() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.QuestDialog(1002));

		assertEquals(0, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
	}

	@Test
	void levelUpSnapshotKeepsAutomaticQuestDialogTargetless() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.LevelUp());

		assertEquals(0, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
	}

	@Test
	void enterZoneSnapshotKeepsAutomaticQuestDialogTargetless() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.EnterZone("VERTERON_CITADEL_210030000"));

		assertEquals(0, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
	}

	@Test
	void zoneMissionEndSnapshotKeepsAutomaticQuestDialogTargetless() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.ZoneMissionEnd());

		assertEquals(0, snapshot.interactionObjectId());
		assertTrue(snapshot.targetlessDialog());
	}

	@Test
	void nonNpcEventsWithDialogActionsRemainTargetless() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player);

		for (QuestEvent event : List.of(new QuestEvent.AtDistance(203700), new QuestEvent.EquipItem(140000001))) {
			QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event);

			assertEquals(0, snapshot.interactionObjectId(), event.type());
			assertTrue(snapshot.targetlessDialog(), event.type());
		}
	}

	@Test
	void productionEventBoundaryOnlyFreezesStartEligibilityWhenRequestedByTransition() throws Exception {
		Player player = emptyPlayer();
		AtomicInteger calls = new AtomicInteger();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player,
			(playerId, questId, event) -> {
				calls.incrementAndGet();
				return QuestStartEligibility.allowed();
			});

		port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.TalkToNpc(203700, 1002, 900007));
		assertEquals(0, calls.get());

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID,
			new QuestEvent.TalkToNpc(203700, 1002, 900007), true);
		assertEquals(1, calls.get());
		assertTrue(snapshot.startEligibility().eligible());
	}

	@Test
	void snapshotCapturesOnlyExplicitlyRequestedExternalEventActivities() throws Exception {
		Player player = emptyPlayer();
		List<Integer> requestedQuestIds = new ArrayList<>();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player, null, questId -> {
			requestedQuestIds.add(questId);
			return questId == 80029;
		});

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event(), false,
			Set.of(80029));

		assertEquals(Boolean.TRUE, snapshot.eventActivity(80029));
		assertNull(snapshot.eventActivity(80032));
		assertEquals(List.of(QUEST_ID, 80029), requestedQuestIds);
	}

	@Test
	void unavailableEventServiceLeavesExternalActivityUnknownAndFailsClosed() throws Exception {
		Player player = emptyPlayer();
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> player, null, questId -> {
			throw new IllegalStateException("event service unavailable");
		});

		QuestSnapshot snapshot = port.snapshot(connection(), PLAYER_ID, QUEST_ID, event(), false,
			Set.of(80029));

		assertNull(snapshot.eventActivity(80029));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.EventActive(80029))));
	}

	@Test
	void snapshotFailsWhenPlayerIsUnavailable() {
		PlayerQuestEventPort port = new PlayerQuestEventPort(playerId -> null);

		SQLException thrown = assertThrows(SQLException.class,
			() -> port.snapshot(connection(), PLAYER_ID, QUEST_ID, event()));
		assertTrue(thrown.getMessage().contains("player is unavailable"));
	}

	private static QuestEvent event() {
		return new QuestEvent.TalkToNpc(203700);
	}

	private static Player playerWithQuestState(QuestStatus status, int packedVars) throws Exception {
		Player player = emptyPlayer();
		QuestState state = new QuestState(QUEST_ID, status, packedVars, 0, null, null, null);
		player.getQuestStateList().addQuest(QUEST_ID, state);
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		setField(Player.class, player, "playerCommonData", new PlayerCommonData(PLAYER_ID));
		setField(Player.class, player, "abyssRank",
			new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0));
		setField(Player.class, player, "equipment", new Equipment(player));
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

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> false;
				case "toString" -> "test-connection";
				default -> defaultValue(method.getReturnType());
			});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class || type == short.class || type == byte.class) {
			return 0;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == float.class) {
			return 0F;
		}
		if (type == double.class) {
			return 0D;
		}
		if (type == char.class) {
			return '\0';
		}
		return null;
	}
}
