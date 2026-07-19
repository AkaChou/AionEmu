package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.DynamicArea;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WINDSTREAM_ANNOUNCE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.services.instance.InstanceDeadlineScheduler;

/** 真端 AI 控制的 MovingCollision 与 WindBox 实例状态。 */
public final class RetailDynamicAreaEngine {

	private static final String STATE_PREFIX = "retail.dynamic_area.";

	private RetailDynamicAreaEngine() {
	}

	public static boolean supports(int worldId, String type, int id) {
		return area(worldId, type, id) != null;
	}

	public static boolean setEnabled(WorldMapInstance instance, String type, int id, boolean enabled) {
		DynamicArea area = area(instance.getMapId(), type, id);
		if (area == null) {
			return false;
		}
		String key = key(type, id);
		transientState(instance);
		instance.getRuntimeState().put(enabledKey(key), enabled);
		InstanceDeadlineScheduler.cancel(instance, expiryTaskKey(key));
		instance.doOnAllPlayers(player -> send(player, area, enabled));
		if (enabled && area.lifeTime() > 0) {
			long deadline = System.currentTimeMillis() + area.lifeTime() * 1000L;
			instance.getRuntimeState().put(deadlineKey(key), deadline);
			scheduleExpiry(instance, area, deadline);
		} else {
			instance.getRuntimeState().remove(deadlineKey(key));
		}
		return true;
	}

	public static void sendStates(Player player) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		transientState(instance);
		int hour = GameTimeManager.getGameTime().getHour();
		for (DynamicArea area : DataManager.RETAIL_AI_DATA.getDynamicAreas(player.getWorldId())) {
			send(player, area, state(instance, area, hour));
		}
	}

	public static void clear(WorldMapInstance instance) {
		if (DataManager.RETAIL_AI_DATA != null) {
			for (DynamicArea area : DataManager.RETAIL_AI_DATA.getDynamicAreas(instance.getMapId())) {
				InstanceDeadlineScheduler.cancel(instance, expiryTaskKey(key(area.type(), area.id())));
			}
		}
		instance.removeTransientState(State.class);
		instance.getRuntimeState().removePrefix(STATE_PREFIX);
	}

	static boolean state(WorldMapInstance instance, DynamicArea area, int hour) {
		return instance.getRuntimeState().getBoolean(enabledKey(key(area.type(), area.id())), initialState(area, hour));
	}

	static boolean initialState(DynamicArea area, int hour) {
		if (area.startTime() == area.endTime()) {
			return area.alwaysEnabled();
		}
		return area.startTime() < area.endTime()
			? hour >= area.startTime() && hour < area.endTime()
			: hour >= area.startTime() || hour < area.endTime();
	}

	static int packetType(String type) {
		return type.equals("MOVING_COLLISION_WINDBOX") ? 0 : 2;
	}

	private static DynamicArea area(int worldId, String type, int id) {
		return DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getDynamicArea(worldId, type, id);
	}

	private static String key(String type, int id) {
		return type + ':' + id;
	}

	private static String enabledKey(String areaKey) {
		return STATE_PREFIX + areaKey + ".enabled";
	}

	private static String deadlineKey(String areaKey) {
		return STATE_PREFIX + areaKey + ".deadline";
	}

	private static String expiryTaskKey(String areaKey) {
		return STATE_PREFIX + areaKey + ".expiry";
	}

	private static State transientState(WorldMapInstance instance) {
		State state = instance.getOrCreateTransientState(State.class, State::new);
		synchronized (state) {
			if (state.initialized) {
				return state;
			}
			state.initialized = true;
			if (DataManager.RETAIL_AI_DATA == null) {
				return state;
			}
			long now = System.currentTimeMillis();
			for (DynamicArea area : DataManager.RETAIL_AI_DATA.getDynamicAreas(instance.getMapId())) {
				String areaKey = key(area.type(), area.id());
				if (!instance.getRuntimeState().getBoolean(enabledKey(areaKey), false)) {
					continue;
				}
				long deadline = instance.getRuntimeState().getLong(deadlineKey(areaKey), 0);
				if (deadline > now) {
					scheduleExpiry(instance, area, deadline);
				} else if (deadline > 0) {
					setEnabled(instance, area.type(), area.id(), false);
				}
			}
		}
		return state;
	}

	private static void scheduleExpiry(WorldMapInstance instance, DynamicArea area, long deadline) {
		String areaKey = key(area.type(), area.id());
		InstanceDeadlineScheduler.schedule(instance, expiryTaskKey(areaKey), deadline,
			() -> setEnabled(instance, area.type(), area.id(), false));
	}

	private static void send(Player player, DynamicArea area, boolean enabled) {
		PacketSendUtility.sendPacket(player, new SM_WINDSTREAM_ANNOUNCE(packetType(area.type()), area.worldId(), area.id(),
			enabled ? 1 : 0));
	}

	private static final class State {
		private boolean initialized;
	}
}
