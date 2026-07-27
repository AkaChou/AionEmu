package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldPosition;

class SMPlayerSpawnTest {

	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void writesRetail58MapLocationPayload() throws ReflectiveOperationException {
		Player player = playerAt(110010000, 1450.97f, 1531.56f, 573.072f);
		World oldWorld = setWorld(worldWithMap(110010000));
		try {
			SM_PLAYER_SPAWN packet = new SM_PLAYER_SPAWN(player);
			ByteBuffer buffer = ByteBuffer.allocate(64);
			packet.setBuf(buffer);
			packet.writeImpl(null);
			buffer.flip();

			assertEquals(44, buffer.remaining());
			assertEquals(110010000, buffer.getInt());
			assertEquals(110010000, buffer.getInt());
			buffer.position(13);
			assertEquals(1450.97f, buffer.getFloat());
			assertEquals(1531.56f, buffer.getFloat());
			assertEquals(573.072f, buffer.getFloat());
			buffer.position(38);
			assertEquals(0, buffer.get());
			assertEquals(0, buffer.getInt());
			assertEquals(0, buffer.get());
		} finally {
			setWorld(oldWorld);
		}
	}

	private static Player playerAt(int mapId, float x, float y, float z) throws ReflectiveOperationException {
		WorldPosition position = new WorldPosition(mapId);
		setField(WorldPosition.class, position, "x", x);
		setField(WorldPosition.class, position, "y", y);
		setField(WorldPosition.class, position, "z", z);
		Player player = OBJENESIS.newInstance(Player.class);
		setField(VisibleObject.class, player, "position", position);
		return player;
	}

	private static World worldWithMap(int mapId) throws ReflectiveOperationException {
		WorldMapTemplate template = OBJENESIS.newInstance(WorldMapTemplate.class);
		WorldMap map = OBJENESIS.newInstance(WorldMap.class);
		setField(WorldMap.class, map, "worldMapTemplate", template);
		IntObjectHashMap<WorldMap> maps = new IntObjectHashMap<>();
		maps.put(mapId, map);
		World world = OBJENESIS.newInstance(World.class);
		setField(World.class, world, "worldMaps", maps);
		return world;
	}

	private static World setWorld(World world) throws ReflectiveOperationException {
		Field field = GameWorldBootstrapServices.class.getDeclaredField("resolvedWorld");
		field.setAccessible(true);
		World oldWorld = (World) field.get(null);
		field.set(null, world);
		return oldWorld;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
