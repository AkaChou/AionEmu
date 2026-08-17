package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证高阶守护者晋升在调用方事务中持久化，并仅在提交成功后发布在线升级。
 * Verifies that ArchDaeva promotion persists in the caller transaction and publishes the live upgrade only after
 * commit.
 */
class PlayerQuestProgressionPortTest {
	private static final int PLAYER_ID = 7;
	private static final long LEVEL_66_EXP = 2066885620L;

	@Test
	void persistsPromotionOnTheCallerConnectionAndPublishesOnlyAfterCommit() throws Exception {
		Player player = playerAtLevel(65);
		Connection connection = connection();
		AtomicReference<Connection> storedConnection = new AtomicReference<>();
		AtomicInteger storedPlayerId = new AtomicInteger();
		AtomicLong storedMinimumExp = new AtomicLong();
		AtomicBoolean promoted = new AtomicBoolean();
		PlayerQuestProgressionPort port = new PlayerQuestProgressionPort(playerId -> player,
			(storedOn, playerId, minimumExp) -> {
				storedConnection.set(storedOn);
				storedPlayerId.set(playerId);
				storedMinimumExp.set(minimumExp);
			}, () -> LEVEL_66_EXP, ignored -> promoted.set(true));
		List<QuestAction.PromoteArchDaeva> actions = List.of(new QuestAction.PromoteArchDaeva());

		port.preflight(connection, snapshot(), actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot(), actions);

		assertSame(connection, storedConnection.get());
		assertEquals(PLAYER_ID, storedPlayerId.get());
		assertEquals(LEVEL_66_EXP, storedMinimumExp.get());
		assertFalse(promoted.get());
		participant.afterCommit();
		assertTrue(promoted.get());
	}

	@Test
	void rollbackDoesNotPublishTheLivePromotion() throws Exception {
		Player player = playerAtLevel(65);
		AtomicBoolean promoted = new AtomicBoolean();
		PlayerQuestProgressionPort port = new PlayerQuestProgressionPort(playerId -> player,
			(connection, playerId, minimumExp) -> { }, () -> LEVEL_66_EXP, ignored -> promoted.set(true));

		QuestTransactionParticipant participant = port.apply(connection(), snapshot(),
			List.of(new QuestAction.PromoteArchDaeva()));
		participant.afterRollback();

		assertFalse(promoted.get());
	}

	@Test
	void rejectsPromotionBelowLevel65() throws Exception {
		Player player = playerAtLevel(64);
		PlayerQuestProgressionPort port = new PlayerQuestProgressionPort(playerId -> player,
			(connection, playerId, minimumExp) -> { }, () -> LEVEL_66_EXP, ignored -> { });

		assertThrows(SQLException.class, () -> port.preflight(connection(), snapshot(),
			List.of(new QuestAction.PromoteArchDaeva())));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, 10520, QuestStatus.REWARD, 6, Map.of(), Map.of());
	}

	private static Player playerAtLevel(int level) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(PLAYER_ID);
		setField(PlayerCommonData.class, commonData, "level", level);
		setField(Player.class, player, "playerCommonData", commonData);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
			(proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
	}
}
