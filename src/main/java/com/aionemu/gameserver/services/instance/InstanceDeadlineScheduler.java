package com.aionemu.gameserver.services.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.world.WorldMapInstance;

public final class InstanceDeadlineScheduler {
	private static final String PREFIX = "deadline.";

	private InstanceDeadlineScheduler() {
	}

	public static void schedule(WorldMapInstance instance, String key, long deadline, Runnable action) {
		if (key == null || key.isBlank() || deadline <= 0) {
			throw new IllegalArgumentException("Instance deadline requires a key and absolute time");
		}
		State state = instance.getOrCreateTransientState(State.class, State::new);
		synchronized (state) {
			var runtimeState = instance.getRuntimeState();
			long previousDeadline = runtimeState.getLong(atKey(key), 0);
			if (previousDeadline != deadline || runtimeState.get(completedKey(key)) == null) {
				runtimeState.mutate(values -> {
					values.put(atKey(key), Long.toString(deadline));
					values.put(completedKey(key), Boolean.FALSE.toString());
				});
			}
			if (runtimeState.getBoolean(completedKey(key), false)) {
				return;
			}
			Future<?> previous = state.tasks.remove(key);
			if (previous != null) {
				previous.cancel(false);
			}
			state.tasks.put(key, GameThreadPoolServices.threadPoolManager().schedule(
				() -> execute(instance, state, key, deadline, action),
				Math.max(1, deadline - System.currentTimeMillis())));
		}
	}

	public static void cancel(WorldMapInstance instance, String key) {
		State state = instance.getTransientState(State.class);
		if (state != null) {
			synchronized (state) {
				Future<?> task = state.tasks.remove(key);
				if (task != null) {
					task.cancel(false);
				}
			}
		}
		instance.getRuntimeState().mutate(values -> {
			values.remove(atKey(key));
			values.remove(completedKey(key));
		});
	}

	public static void clearTransient(WorldMapInstance instance) {
		State state = instance.removeTransientState(State.class);
		if (state != null) {
			synchronized (state) {
				state.tasks.values().forEach(task -> task.cancel(false));
				state.tasks.clear();
			}
		}
	}

	public static long deadline(WorldMapInstance instance, String key) {
		return instance.getRuntimeState().getLong(atKey(key), 0);
	}

	public static boolean isCompleted(WorldMapInstance instance, String key) {
		return instance.getRuntimeState().getBoolean(completedKey(key), false);
	}

	private static void execute(WorldMapInstance instance, State state, String key, long expectedDeadline,
			Runnable action) {
		synchronized (state) {
			if (instance.getRuntimeState().getLong(atKey(key), 0) != expectedDeadline
					|| instance.getRuntimeState().getBoolean(completedKey(key), false)) {
				return;
			}
			state.tasks.remove(key);
		}
		action.run();
		instance.getRuntimeState().mutate(values -> {
			if (Long.parseLong(values.getOrDefault(atKey(key), "0")) == expectedDeadline) {
				values.put(completedKey(key), Boolean.TRUE.toString());
			}
		});
	}

	private static String atKey(String key) {
		return PREFIX + key + ".at";
	}

	private static String completedKey(String key) {
		return PREFIX + key + ".completed";
	}

	private static final class State {
		private final Map<String, Future<?>> tasks = new HashMap<>();
	}
}
