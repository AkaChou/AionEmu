package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** 真端感知区域移动事件分发。 */
public final class RetailSensoryAreaEngine {

	private static final Map<WorldMapInstance, Set<RetailPatternAI2>> BY_INSTANCE = new ConcurrentHashMap<>();
	private static final Map<Player, Set<RetailPatternAI2>> ACTIVE_BY_PLAYER =
		Collections.synchronizedMap(new WeakHashMap<>());

	private RetailSensoryAreaEngine() {
	}

	static void register(WorldMapInstance instance, RetailPatternAI2 ai) {
		BY_INSTANCE.computeIfAbsent(instance, ignored -> ConcurrentHashMap.newKeySet()).add(ai);
		instance.getPlayersInside().forEach(RetailSensoryAreaEngine::onPlayerMoved);
	}

	static void unregister(WorldMapInstance instance, RetailPatternAI2 ai) {
		Set<RetailPatternAI2> registered = BY_INSTANCE.get(instance);
		if (registered != null && registered.remove(ai) && registered.isEmpty()) {
			BY_INSTANCE.remove(instance, registered);
		}
		synchronized (ACTIVE_BY_PLAYER) {
			ACTIVE_BY_PLAYER.values().removeIf(active -> {
				active.remove(ai);
				return active.isEmpty();
			});
		}
	}

	public static void onPlayerMoved(Player player) {
		RetailAreaEngine.onPlayerMoved(player);
		update(player, BY_INSTANCE.get(player.getPosition().getWorldMapInstance()));
	}

	public static void onPlayerDespawned(Player player) {
		RetailAreaEngine.onPlayerDespawned(player);
		Set<RetailPatternAI2> active = ACTIVE_BY_PLAYER.remove(player);
		if (active != null) {
			active.forEach(ai -> ai.leaveSensoryArea(player));
		}
	}

	static void update(Player player, Set<RetailPatternAI2> registered) {
		Set<RetailPatternAI2> targets = new HashSet<>();
		if (registered != null) {
			targets.addAll(registered);
		}
		Set<RetailPatternAI2> previous = ACTIVE_BY_PLAYER.remove(player);
		if (previous != null) {
			targets.addAll(previous);
		}
		for (RetailPatternAI2 ai : targets) {
			if (ai.updateSensoryArea(player)) {
				ACTIVE_BY_PLAYER.computeIfAbsent(player, ignored -> ConcurrentHashMap.newKeySet()).add(ai);
			}
		}
	}
}
