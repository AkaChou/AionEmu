package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimits;
import com.aionemu.gameserver.model.instance.instancereward.KamarBattlefieldReward;
import com.aionemu.gameserver.model.instance.playerreward.KamarBattlefieldPlayerReward;
import com.aionemu.gameserver.network.aion.AionConnection;

class SMInstanceScoreTest {

	@BeforeAll
	static void initializeBuffData() {
		if (DataManager.RETAIL_INSTANCE_DATA == null) {
			DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
					new File("src/main/resources/aion/definitions/compact/instance"),
					new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
		}
	}

	@Test
	void writesLeadingRaceForFactionScoreUpdates() throws ReflectiveOperationException {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		int objectId = 77;
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(player, AionObject.class, "objectId", objectId);
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(connection, AionConnection.class, "activePlayer", new AtomicReference<>(player));

		assertFactionScorePacket(connection, objectId, null, 65535);
		assertFactionScorePacket(connection, objectId, Race.ELYOS, Race.ELYOS.getRaceId());
		assertFactionScorePacket(connection, objectId, Race.ASMODIANS, Race.ASMODIANS.getRaceId());
	}

	@Test
	void matchesRetailMatchmakerStageAndCooldownPayloads() throws ReflectiveOperationException {
		SM_AUTO_GROUP matchmaker = new ObjenesisStd().newInstance(SM_AUTO_GROUP.class);
		setField(matchmaker, SM_AUTO_GROUP.class, "instanceMaskId", 0x01020304);
		setField(matchmaker, SM_AUTO_GROUP.class, "windowId", (byte) 8);
		setField(matchmaker, SM_AUTO_GROUP.class, "mapId", 0x11223344);
		setField(matchmaker, SM_AUTO_GROUP.class, "waitTime", 0x00010203);
		setField(matchmaker, SM_AUTO_GROUP.class, "name", "A");
		ByteBuffer matchmakerPayload = ByteBuffer.allocate(26);
		matchmaker.setBuf(matchmakerPayload);
		matchmaker.writeImpl(null);
		assertPayload("0403020108443322110000000000000000030201000041000000", matchmakerPayload);

		SM_INSTANCE_STAGE_INFO stage = new SM_INSTANCE_STAGE_INFO(0x12, 0x3456, 0x789A);
		ByteBuffer stagePayload = ByteBuffer.allocate(9);
		stage.setBuf(stagePayload);
		stage.writeImpl(null);
		assertPayload("120000000056349a78", stagePayload);

		int objectId = 0x01020304;
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName("A");
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(player, AionObject.class, "objectId", objectId);
		setField(player, Player.class, "playerCommonData", commonData);
		setField(player, Player.class, "instanceLimits", new PlayerInstanceLimits());
		SM_INSTANCE_INFO cooldown = new SM_INSTANCE_INFO(player, 300220000);
		ByteBuffer cooldownPayload = ByteBuffer.allocate(51);
		cooldown.setBuf(cooldownPayload);
		cooldown.writeImpl(null);
		assertPayload("022c0000000001000403020101002c000000000000000000000000000000000000000000000000000000010000000141000000",
				cooldownPayload);
	}

	private static void assertFactionScorePacket(AionConnection connection, int objectId, Race leadingRace,
			int expectedLeadingRaceId) {
		KamarBattlefieldReward reward = new KamarBattlefieldReward(301120000, 1, null);
		reward.addPlayerReward(new KamarBattlefieldPlayerReward(objectId, (byte) 10, Race.ELYOS));
		if (leadingRace != null) {
			reward.addPointsByRace(leadingRace, 1);
		}

		SM_INSTANCE_SCORE packet = new SM_INSTANCE_SCORE(11, 1234, reward, objectId);
		ByteBuffer buffer = ByteBuffer.allocate(30);
		packet.setBuf(buffer);
		packet.writeImpl(connection);
		if (leadingRace == null) {
			assertEquals("00baf211d2040000000020000b0000000000d80e000000000000ffff0000",
					HexFormat.of().formatHex(buffer.array()));
		}
		buffer.flip();

		assertEquals(301120000, buffer.getInt());
		assertEquals(1234, buffer.getInt());
		assertEquals(reward.getInstanceScoreType().getId(), buffer.getInt());
		assertEquals(11, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.getInt());
		assertEquals(reward.getPointsByRace(Race.ELYOS).intValue(), buffer.getInt());
		assertEquals(Race.ELYOS.getRaceId(), buffer.getInt());
		assertEquals(expectedLeadingRaceId, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	private static void assertPayload(String expected, ByteBuffer buffer) {
		assertEquals(buffer.capacity(), buffer.position());
		assertEquals(expected, HexFormat.of().formatHex(buffer.array()));
	}

	private static void setField(Object target, Class<?> owner, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
