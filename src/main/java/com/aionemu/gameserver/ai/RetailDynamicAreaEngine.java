package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.DynamicArea;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WINDSTREAM_ANNOUNCE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 真实 AI 控制的 MovingCollision 与 WindBox 实例状态。
 * Retail AI-controlled instance states for MovingCollision and WindBox.
 */
public final class RetailDynamicAreaEngine {

	private static final Map<WorldMapInstance, Map<String, Boolean>> STATES = new ConcurrentHashMap<>();
	private static final Map<WorldMapInstance, Map<String, Future<?>>> EXPIRIES = new ConcurrentHashMap<>();

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
		STATES.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>()).put(key, enabled);
		Map<String, Future<?>> expiries = EXPIRIES.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>());
		Future<?> previous = expiries.remove(key);
		if (previous != null) {
			previous.cancel(false);
		}
		instance.doOnAllPlayers(player -> send(player, area, enabled));
		if (enabled && area.lifeTime() > 0) {
			expiries.put(key, GameThreadPoolServices.threadPoolManager().schedule(
				() -> setEnabled(instance, type, id, false), area.lifeTime() * 1000L));
		}
		return true;
	}

	public static void sendStates(Player player) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		int hour = GameTimeManager.getGameTime().getHour();
		for (DynamicArea area : DataManager.RETAIL_AI_DATA.getDynamicAreas(player.getWorldId())) {
			send(player, area, state(instance, area, hour));
		}
	}

	public static void clear(WorldMapInstance instance) {
		STATES.remove(instance);
		EXPIRIES.getOrDefault(instance, Map.of()).values().forEach(task -> task.cancel(false));
		EXPIRIES.remove(instance);
	}

	static boolean state(WorldMapInstance instance, DynamicArea area, int hour) {
		return STATES.getOrDefault(instance, Map.of()).getOrDefault(key(area.type(), area.id()), initialState(area, hour));
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

	private static void send(Player player, DynamicArea area, boolean enabled) {
		PacketSendUtility.sendPacket(player, new SM_WINDSTREAM_ANNOUNCE(packetType(area.type()), area.worldId(), area.id(),
			enabled ? 1 : 0));
	}
}
