package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerQuestProximityEventPortTest {
	@Test
	void capturesFrozenRecipientTargetWorldAndDistanceFacts() throws Exception {
		Player player = player(7, 210130000, 0f, 0f, 0f);
		Npc npc = npc(20, 835650, 3f, 4f, 0f, 210130000);
		PlayerQuestProximityEventPort port = new PlayerQuestProximityEventPort((target, recipient) -> true,
			ignored -> 0);

		QuestEvent.AtDistance event = port.atDistance(new QuestEnv(npc, player, 0, 0), 835650);

		assertEquals(835650, event.npcId());
		assertEquals(7, event.facts().recipientId());
		assertEquals(20, event.facts().targetObjectId());
		assertEquals(210130000, event.facts().recipientWorldId());
		assertEquals(5d, event.facts().distance(), 0.001d);
		assertEquals(20d, event.facts().maximumDistance());
	}

	@Test
	void rejectsWrongRouteWorldAndRange() throws Exception {
		Player player = player(7, 210130000, 0f, 0f, 0f);
		Npc npc = npc(20, 835650, 3f, 4f, 0f, 210130000);
		PlayerQuestProximityEventPort port = new PlayerQuestProximityEventPort((target, recipient) -> false,
			ignored -> 0);

		assertThrows(IllegalArgumentException.class,
			() -> port.atDistance(new QuestEnv(npc, player, 0, 0), 835651));

		npc.setPosition(new WorldPosition(210070000));
		markSpawned(npc.getPosition());
		assertThrows(IllegalArgumentException.class,
			() -> port.atDistance(new QuestEnv(npc, player, 0, 0), 835650));

		npc.setPosition(new WorldPosition(210130000));
		npc.getPosition().setXYZH(30f, 0f, 0f, (byte) 0);
		markSpawned(npc.getPosition());
		assertThrows(IllegalArgumentException.class,
			() -> port.atDistance(new QuestEnv(npc, player, 0, 0), 835650));
	}

	private static Player player(int id, int worldId, float x, float y, float z) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", id);
		WorldPosition position = new WorldPosition(worldId);
		position.setXYZH(x, y, z, (byte) 0);
		markSpawned(position);
		player.setPosition(position);
		return player;
	}

	private static Npc npc(int objectId, int templateId, float x, float y, float z, int worldId) throws Exception {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(AionObject.class, npc, "objectId", objectId);
		NpcTemplate template = new ObjenesisStd().newInstance(NpcTemplate.class);
		setField(NpcTemplate.class, template, "npcId", templateId);
		setField(VisibleObject.class, npc, "objectTemplate", template);
		WorldPosition position = new WorldPosition(worldId);
		position.setXYZH(x, y, z, (byte) 0);
		markSpawned(position);
		npc.setPosition(position);
		return npc;
	}

	private static void markSpawned(WorldPosition position) throws Exception {
		setField(WorldPosition.class, position, "isSpawned", true);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
