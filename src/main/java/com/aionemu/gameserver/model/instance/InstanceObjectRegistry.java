package com.aionemu.gameserver.model.instance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.world.WorldMapInstance;

public final class InstanceObjectRegistry {
	private static final String ENTITY_PREFIX = "entity:";

	private InstanceObjectRegistry() {
	}

	public static String entityKey(int entityId) {
		if (entityId <= 0) {
			throw new IllegalArgumentException("Stable entity id must be positive");
		}
		return ENTITY_PREFIX + entityId;
	}

	public static String keyOf(VisibleObject object) {
		if ((object instanceof Npc || object instanceof StaticDoor) && object.getSpawn().getEntityId() > 0) {
			return entityKey(object.getSpawn().getEntityId());
		}
		return null;
	}

	public static void bind(WorldMapInstance instance, String businessKey, VisibleObject object) {
		if (businessKey == null || businessKey.isBlank() || businessKey.startsWith(ENTITY_PREFIX)) {
			throw new IllegalArgumentException("Dynamic instance object requires a business key");
		}
		instance.getOrCreateTransientState(State.class, State::new).objects.put(businessKey, object);
	}

	public static VisibleObject resolve(WorldMapInstance instance, String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (!key.startsWith(ENTITY_PREFIX)) {
			State state = instance.getTransientState(State.class);
			return state == null ? null : state.objects.get(key);
		}
		int entityId;
		try {
			entityId = Integer.parseInt(key.substring(ENTITY_PREFIX.length()));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid stable entity key " + key, e);
		}
		for (Iterator<VisibleObject> objects = instance.objectIterator(); objects.hasNext();) {
			VisibleObject object = objects.next();
			if ((object instanceof Npc || object instanceof StaticDoor) && object.getSpawn().getEntityId() == entityId) {
				return object;
			}
		}
		return null;
	}

	public static void unbind(WorldMapInstance instance, String businessKey) {
		State state = instance.getTransientState(State.class);
		if (state != null) {
			state.objects.remove(businessKey);
		}
	}

	public static void clear(WorldMapInstance instance) {
		instance.removeTransientState(State.class);
	}

	private static final class State {
		private final Map<String, VisibleObject> objects = new HashMap<>();
	}
}
