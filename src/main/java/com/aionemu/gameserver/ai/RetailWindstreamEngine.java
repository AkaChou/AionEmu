package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.windstreams.Location2D;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WINDSTREAM_ANNOUNCE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 真端 AI 控制的实例级风道开关。
 * Retail AI-controlled instance-level windstream switches.
 */
public final class RetailWindstreamEngine {

	private static final Map<WorldMapInstance, Map<Integer, Integer>> STATES = new ConcurrentHashMap<>();

	private RetailWindstreamEngine() {
	}

	public static boolean supports(int mapId, int groupId) {
		return location(mapId, groupId) != null;
	}

	public static boolean setEnabled(WorldMapInstance instance, int groupId, boolean enabled) {
		Location2D location = location(instance.getMapId(), groupId);
		if (location == null) {
			return false;
		}
		int state = enabled ? 1 : 0;
		STATES.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>()).put(groupId, state);
		instance.doOnAllPlayers(player -> send(player, location, state));
		return true;
	}

	public static void sendStates(Player player) {
		WindstreamTemplate template = template(player.getWorldId());
		if (template == null) {
			return;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		for (Location2D location : template.getLocations().getLocation()) {
			send(player, location, state(instance, location));
		}
	}

	public static void clear(WorldMapInstance instance) {
		STATES.remove(instance);
	}

	static int state(WorldMapInstance instance, Location2D location) {
		return STATES.getOrDefault(instance, Map.of()).getOrDefault(location.getId(), location.getState());
	}

	private static WindstreamTemplate template(int mapId) {
		return DataManager.WINDSTREAM_DATA == null ? null : DataManager.WINDSTREAM_DATA.getStreamTemplate(mapId);
	}

	private static Location2D location(int mapId, int groupId) {
		WindstreamTemplate template = template(mapId);
		if (template != null) {
			for (Location2D location : template.getLocations().getLocation()) {
				if (location.getId() == groupId) {
					return location;
				}
			}
		}
		return null;
	}

	private static void send(Player player, Location2D location, int state) {
		PacketSendUtility.sendPacket(player, new SM_WINDSTREAM_ANNOUNCE(location.getFlyPathType().getId(),
			player.getWorldId(), location.getId(), state));
	}
}
