package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMap3DInstance;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

class SMTeamMemberInfoTest {

	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void writesMapAndInstanceIdsForAllianceMembers() throws ReflectiveOperationException {
		SM_ALLIANCE_MEMBER_INFO packet = OBJENESIS.newInstance(SM_ALLIANCE_MEMBER_INFO.class);
		setField(SM_ALLIANCE_MEMBER_INFO.class, packet, "player", playerAt(210020000, 37));
		setField(SM_ALLIANCE_MEMBER_INFO.class, packet, "event", PlayerAllianceEvent.MOVEMENT);

		assertLocation(write(packet));
	}

	@Test
	void writesMapAndInstanceIdsForGroupMembers() throws ReflectiveOperationException {
		SM_GROUP_MEMBER_INFO packet = OBJENESIS.newInstance(SM_GROUP_MEMBER_INFO.class);
		setField(SM_GROUP_MEMBER_INFO.class, packet, "player", playerAt(210020000, 37));
		setField(SM_GROUP_MEMBER_INFO.class, packet, "event", GroupEvent.MOVEMENT);

		assertLocation(write(packet));
	}

	@Test
	void writesFullEffectSlotMaskAndFreshEffectAgesForGroupUpdates() throws ReflectiveOperationException {
		Player player = playerAt(210020000, 37);
		player.setEffectController(new PlayerEffectController(player));
		SM_GROUP_MEMBER_INFO packet = OBJENESIS.newInstance(SM_GROUP_MEMBER_INFO.class);
		setField(SM_GROUP_MEMBER_INFO.class, packet, "player", player);
		setField(SM_GROUP_MEMBER_INFO.class, packet, "event", GroupEvent.UPDATE);

		ByteBuffer buffer = write(packet);
		buffer.position(64);
		while (buffer.getChar() != 0) {
		}
		buffer.getInt();
		buffer.getInt();

		assertEquals(0x7F, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.getShort());
		for (int i = 0; i < 8; i++) {
			assertEquals(0, buffer.getInt());
		}
	}

	private static ByteBuffer write(SM_ALLIANCE_MEMBER_INFO packet) {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer write(SM_GROUP_MEMBER_INFO packet) {
		ByteBuffer buffer = ByteBuffer.allocate(128);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();
		return buffer;
	}

	private static void assertLocation(ByteBuffer buffer) {
		buffer.position(36);

		assertEquals(210020000, buffer.getInt());
		assertEquals(37, buffer.getInt());
	}

	private static Player playerAt(int mapId, int instanceId) throws ReflectiveOperationException {
		WorldMapInstance instance = OBJENESIS.newInstance(WorldMap3DInstance.class);
		setField(WorldMapInstance.class, instance, "instanceId", instanceId);
		MapRegion region = OBJENESIS.newInstance(MapRegion.class);
		setField(MapRegion.class, region, "parent", instance);
		WorldPosition position = new WorldPosition(mapId);
		setField(WorldPosition.class, position, "mapRegion", region);

		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setName("Member");
		commonData.setPlayerClass(PlayerClass.WARRIOR);
		commonData.setGender(Gender.MALE);
		commonData.setPosition(position);

		Player player = OBJENESIS.newInstance(Player.class);
		setField(AionObject.class, player, "objectId", 1);
		setField(Player.class, player, "playerCommonData", commonData);
		setField(VisibleObject.class, player, "position", position);
		return player;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
