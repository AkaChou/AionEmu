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

	private RetailSensoryAreaEngine() {
	}

	static void register(WorldMapInstance instance, RetailPatternAI2 ai) {
		state(instance).registered.add(ai);
		instance.getPlayersInside().forEach(RetailSensoryAreaEngine::onPlayerMoved);
	}

	static void unregister(WorldMapInstance instance, RetailPatternAI2 ai) {
		State state = instance.getTransientState(State.class);
		if (state == null) {
			return;
		}
		state.registered.remove(ai);
		synchronized (state.activeByPlayer) {
			state.activeByPlayer.values().removeIf(active -> {
				active.remove(ai);
				return active.isEmpty();
			});
		}
	}

	public static void onPlayerMoved(Player player) {
		RetailAreaEngine.onPlayerMoved(player);
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		State state = state(instance);
		update(player, state);
	}

	public static void onPlayerDespawned(Player player) {
		RetailAreaEngine.onPlayerDespawned(player);
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		State state = instance.getTransientState(State.class);
		Set<RetailPatternAI2> active = state == null ? null : state.activeByPlayer.remove(player);
		if (active != null) {
			active.forEach(ai -> ai.leaveSensoryArea(player));
		}
	}

	private static void update(Player player, State state) {
		Set<RetailPatternAI2> targets = new HashSet<>();
		targets.addAll(state.registered);
		Set<RetailPatternAI2> previous = state.activeByPlayer.remove(player);
		if (previous != null) {
			targets.addAll(previous);
		}
		for (RetailPatternAI2 ai : targets) {
			if (ai.updateSensoryArea(player)) {
				state.activeByPlayer.computeIfAbsent(player, ignored -> ConcurrentHashMap.newKeySet()).add(ai);
			}
		}
	}

	public static void clear(WorldMapInstance instance) {
		State state = instance.removeTransientState(State.class);
		if (state != null) {
			state.activeByPlayer.clear();
			state.registered.clear();
		}
	}

	private static State state(WorldMapInstance instance) {
		return instance.getOrCreateTransientState(State.class, State::new);
	}

	private static final class State {
		private final Set<RetailPatternAI2> registered = ConcurrentHashMap.newKeySet();
		private final Map<Player, Set<RetailPatternAI2>> activeByPlayer =
			Collections.synchronizedMap(new WeakHashMap<>());
	}
}
