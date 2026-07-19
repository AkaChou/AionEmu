package com.aionemu.gameserver.model.gameobjects.player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerInstanceLimits {
	private final Map<Integer, PlayerInstanceLimit> limits = new LinkedHashMap<>();

	public synchronized PlayerInstanceLimit get(int limitKey) {
		return limits.get(limitKey);
	}

	public synchronized PlayerInstanceLimit getOrCreate(int limitKey) {
		return limits.computeIfAbsent(limitKey, key -> new PlayerInstanceLimit(key, 0, 0, 0, 0, 0));
	}

	public synchronized void load(Map<Integer, PlayerInstanceLimit> loaded) {
		limits.clear();
		limits.putAll(loaded);
	}

	public synchronized Map<Integer, PlayerInstanceLimit> snapshot() {
		return new LinkedHashMap<>(limits);
	}

	public synchronized void remove(int limitKey) {
		limits.remove(limitKey);
	}

	public synchronized void clear() {
		limits.clear();
	}
}
