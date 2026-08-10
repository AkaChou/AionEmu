package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestMoviePortTest {
	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@Test
	void movieUsesTheCompiledIdAndAuthoritativePlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<Integer> movies = new ArrayList<>();
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> player, (resolved, movieId) -> {
			assertEquals(player, resolved);
			movies.add(movieId);
			return true;
		});

		assertTrue(port.playMovie(snapshot(), plan(), 250));
		assertEquals(List.of(250), movies);
	}

	@Test
	void movieFailsClosedOnInvalidIdAndReportsLoggedOutPlayer() {
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> null, (player, movieId) -> true);

		assertFalse(port.playMovie(snapshot(), plan(), 250));
		assertThrows(IllegalArgumentException.class, () -> port.playMovie(snapshot(), plan(), 0));
	}

	@Test
	void moviePassesTheClientResourceType() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<QuestMovieType> types = new ArrayList<>();
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> player, (resolved, movieId, type) -> {
			assertEquals(player, resolved);
			assertEquals(30, movieId);
			types.add(type);
			return true;
		});

		assertTrue(port.playMovie(snapshot(), plan(), 30, QuestMovieType.CUTSCENE_MOVIE));
		assertEquals(List.of(QuestMovieType.CUTSCENE_MOVIE), types);
	}

	@Test
	void productionMovieSendsTheRequestedClientResourceType() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		AionConnection connection = packetConnection();
		setField(Player.class, player, "clientConnection", connection);
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> player);

		assertTrue(port.playMovie(snapshot(), plan(), 30, QuestMovieType.CUTSCENE_MOVIE));

		SM_PLAY_MOVIE packet = assertInstanceOf(SM_PLAY_MOVIE.class, packetQueue(connection).getFirst());
		assertEquals(QuestMovieType.CUTSCENE_MOVIE.wireValue(), intField(packet, "type"));
		assertEquals(30, intField(packet, "movieId"));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(7, 24154, QuestStatus.START, 2, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(24154, QuestStatus.START, 2, List.of(), List.of());
	}

	private static AionConnection packetConnection() throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(AConnection.class, connection, "transport", new RecordingTransport());
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(AionConnection connection) throws Exception {
		Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
		field.setAccessible(true);
		return (List<AionServerPacket>) field.get(connection);
	}

	private static int intField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class RecordingTransport implements ConnectionTransport {
		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}
}
