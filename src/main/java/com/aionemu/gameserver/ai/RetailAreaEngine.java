package com.aionemu.gameserver.ai;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.dataholders.RetailAiData.LimitArea;
import com.aionemu.gameserver.dataholders.RetailAiData.QuestArea;
import com.aionemu.gameserver.dataholders.RetailAiData.ResurrectArea;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DYNAMIC_LIMIT_AREA_INFO;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** 真端实例级动态区域。 */
public final class RetailAreaEngine {

	private static final Map<WorldMapInstance, Map<String, Boolean>> RESURRECT_STATES = new ConcurrentHashMap<>();
	private static final Map<WorldMapInstance, Map<String, Boolean>> QUEST_STATES = new ConcurrentHashMap<>();
	private static final Map<WorldMapInstance, Map<String, Boolean>> LIMIT_NOPARK_STATES = new ConcurrentHashMap<>();
	private static final Map<WorldMapInstance, Map<String, Boolean>> LIMIT_NORECALL_STATES = new ConcurrentHashMap<>();
	private static final Map<Player, QuestPresence> QUEST_PRESENCE =
		Collections.synchronizedMap(new WeakHashMap<>());

	private RetailAreaEngine() {
	}

	public static boolean supports(int worldId, String areaType, String prefix) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return false;
		}
		return switch (areaType) {
			case "AI_CONTROL_AREA_RESURRECT" -> DataManager.RETAIL_AI_DATA.hasResurrectArea(worldId, prefix);
			case "AI_CONTROL_AREA_QUESTSCRIPT" -> DataManager.RETAIL_AI_DATA.hasQuestArea(worldId, prefix)
				&& DataManager.RETAIL_AI_DATA.findQuestAreas(worldId, prefix).stream().allMatch(RetailAreaEngine::hasQuestTemplates);
			case "AI_CONTROL_AREA_LIMIT_NOPARK", "AI_CONTROL_AREA_LIMIT_NORECALL" ->
				!DataManager.RETAIL_AI_DATA.findLimitAreas(worldId, prefix).isEmpty()
					&& DataManager.RETAIL_AI_DATA.findLimitAreas(worldId, prefix).stream().allMatch(LimitArea::dynamic);
			case "AI_CONTROL_AREA_GROUPCTRL" -> RetailGroupControlEngine.supports(worldId, prefix);
			default -> false;
		};
	}

	public static boolean setEnabled(WorldMapInstance instance, String areaType, String prefix, boolean enabled) {
		if (!supports(instance.getMapId(), areaType, prefix)) {
			return false;
		}
		if (areaType.equals("AI_CONTROL_AREA_GROUPCTRL")) {
			return RetailGroupControlEngine.setAreaEnabled(instance, prefix, enabled);
		} else if (areaType.equals("AI_CONTROL_AREA_RESURRECT")) {
			Map<String, Boolean> states = RESURRECT_STATES.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>());
			for (ResurrectArea area : DataManager.RETAIL_AI_DATA.getResurrectAreas(instance.getMapId())) {
				if (matchesPrefix(area.name(), prefix)) {
					states.put(key(area.name()), enabled);
				}
			}
		} else if (areaType.equals("AI_CONTROL_AREA_QUESTSCRIPT")) {
			Map<String, Boolean> states = QUEST_STATES.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>());
			DataManager.RETAIL_AI_DATA.findQuestAreas(instance.getMapId(), prefix)
				.forEach(area -> states.put(key(area.name()), enabled));
			instance.getPlayersInside().forEach(RetailAreaEngine::onPlayerMoved);
		} else {
			boolean noPark = areaType.equals("AI_CONTROL_AREA_LIMIT_NOPARK");
			Map<WorldMapInstance, Map<String, Boolean>> stateStore = noPark
				? LIMIT_NOPARK_STATES : LIMIT_NORECALL_STATES;
			Map<String, Boolean> states = stateStore.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>());
			for (LimitArea area : DataManager.RETAIL_AI_DATA.findLimitAreas(instance.getMapId(), prefix)) {
				states.put(key(area.name()), enabled);
				if (!noPark) {
					var packet = new SM_DYNAMIC_LIMIT_AREA_INFO(area.name(), enabled);
					instance.getPlayersInside().forEach(player -> PacketSendUtility.sendPacket(player, packet));
				}
			}
		}
		return true;
	}

	public static void onPlayerMoved(Player player) {
		RetailGroupControlEngine.initialize(player.getPosition().getWorldMapInstance());
		if (DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		List<QuestArea> entered;
		synchronized (QUEST_PRESENCE) {
			QuestPresence presence = QUEST_PRESENCE.get(player);
			if (presence == null || presence.instance() != instance) {
				presence = new QuestPresence(instance, new HashSet<>());
			}
			entered = enteredQuestAreas(DataManager.RETAIL_AI_DATA.getQuestAreas(player.getWorldId()).stream()
				.filter(RetailAreaEngine::hasQuestTemplates).toList(), QUEST_STATES.getOrDefault(instance, Map.of()),
				presence.areas(), player.getX(), player.getY(), player.getZ());
			if (presence.areas().isEmpty()) {
				QUEST_PRESENCE.remove(player);
			} else {
				QUEST_PRESENCE.put(player, presence);
			}
		}
		for (QuestArea area : entered) {
			for (int questId : area.questIds()) {
				QuestService.startQuest(new QuestEnv(null, player, questId, 0));
			}
		}
	}

	public static void onPlayerDespawned(Player player) {
		QUEST_PRESENCE.remove(player);
	}

	public static LocationAliasPoint findResurrectPoint(Player player) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return null;
		}
		Map<String, Boolean> states = RESURRECT_STATES.getOrDefault(
			player.getPosition().getWorldMapInstance(), Map.of());
		return findResurrectPoint(DataManager.RETAIL_AI_DATA.getResurrectAreas(player.getWorldId()), states,
			player.getRace().getRaceId(), player.getTribe().name(), player.getX(), player.getY(), player.getZ());
	}

	public static boolean isNoPark(Player player, long secondsOffline) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return false;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		return isNoPark(DataManager.RETAIL_AI_DATA.getLimitAreas(player.getWorldId()),
			LIMIT_NOPARK_STATES.getOrDefault(instance, Map.of()), player.getRace().getRaceId(), secondsOffline,
			player.getX(), player.getY(), player.getZ());
	}

	public static boolean isNoRecall(Player player) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return false;
		}
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		return isNoRecall(DataManager.RETAIL_AI_DATA.getLimitAreas(player.getWorldId()),
			LIMIT_NORECALL_STATES.getOrDefault(instance, Map.of()), player.getX(), player.getY(), player.getZ());
	}

	public static Map<String, Boolean> getNoRecallStates(WorldMapInstance instance) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return Map.of();
		}
		Map<String, Boolean> overrides = LIMIT_NORECALL_STATES.getOrDefault(instance, Map.of());
		Map<String, Boolean> states = new LinkedHashMap<>();
		for (LimitArea area : DataManager.RETAIL_AI_DATA.getLimitAreas(instance.getMapId())) {
			if (area.dynamic()) {
				states.put(area.name(), overrides.getOrDefault(key(area.name()), area.noRecall()));
			}
		}
		return Collections.unmodifiableMap(states);
	}

	public static void clear(WorldMapInstance instance) {
		RESURRECT_STATES.remove(instance);
		QUEST_STATES.remove(instance);
		LIMIT_NOPARK_STATES.remove(instance);
		LIMIT_NORECALL_STATES.remove(instance);
		RetailGroupControlEngine.clear(instance);
		synchronized (QUEST_PRESENCE) {
			QUEST_PRESENCE.values().removeIf(presence -> presence.instance() == instance);
		}
	}

	static List<QuestArea> enteredQuestAreas(Iterable<QuestArea> areas, Map<String, Boolean> states,
			Set<QuestArea> active, float x, float y, float z) {
		Set<QuestArea> current = new HashSet<>();
		List<QuestArea> entered = new ArrayList<>();
		for (QuestArea area : areas) {
			String key = key(area.name());
			if (states.getOrDefault(key, true) && area.area().isInside3D(x, y, z)) {
				current.add(area);
				if (!active.contains(area)) {
					entered.add(area);
				}
			}
		}
		active.clear();
		active.addAll(current);
		return entered;
	}

	static LocationAliasPoint findResurrectPoint(Iterable<ResurrectArea> areas, Map<String, Boolean> states,
			int race, String tribe, float x, float y, float z) {
		for (ResurrectArea area : areas) {
			if (states.getOrDefault(area.name().toLowerCase(Locale.ROOT), true)
				&& matchesActor(area, race, tribe) && area.area().isInside3D(x, y, z)) {
				return area.destinations().get(Rnd.get(area.destinations().size()));
			}
		}
		return null;
	}

	static boolean isNoPark(Iterable<LimitArea> areas, Map<String, Boolean> states, int race,
			long secondsOffline, float x, float y, float z) {
		for (LimitArea area : areas) {
			boolean enabled = states.getOrDefault(key(area.name()), true);
			boolean blocksRace = area.noPark().equalsIgnoreCase("All")
				|| area.noPark().equalsIgnoreCase(race == 0 ? "Light" : "Dark");
			if (enabled && blocksRace && secondsOffline > area.noParkReenterInterval() && area.area().isInside3D(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	static boolean isNoRecall(Iterable<LimitArea> areas, Map<String, Boolean> states, float x, float y, float z) {
		for (LimitArea area : areas) {
			if (states.getOrDefault(key(area.name()), area.noRecall()) && area.area().isInside3D(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesActor(ResurrectArea area, int race, String tribe) {
		return (area.race() == 2 || area.race() == race)
			&& (area.tribe().equalsIgnoreCase("none") || area.tribe().equalsIgnoreCase(tribe));
	}

	private static boolean hasQuestTemplates(QuestArea area) {
		var catalog = GameEngineServices.questEngine().questCatalog();
		return area.questIds().stream().allMatch(id -> catalog.findMetadata(id).isPresent());
	}

	private static boolean matchesPrefix(String name, String prefix) {
		return name.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	private record QuestPresence(WorldMapInstance instance, Set<QuestArea> areas) {
	}
}
