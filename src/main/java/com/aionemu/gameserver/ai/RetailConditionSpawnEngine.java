package com.aionemu.gameserver.ai;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawn;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnChoice;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnNpc;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 真端实例级条件变量与条件刷怪。
 * Retail instance-level condition variables and condition-based spawning.
 */
public final class RetailConditionSpawnEngine {

	private static final Map<WorldMapInstance, State> STATES = new ConcurrentHashMap<>();

	private RetailConditionSpawnEngine() {
	}

	public static boolean supports(int worldId, String variable) {
		return DataManager.RETAIL_AI_DATA != null
			&& DataManager.RETAIL_AI_DATA.supportsConditionVariable(worldId, variable);
	}

	public static boolean supports(String worldName, String variable) {
		Integer worldId = DataManager.RETAIL_AI_DATA == null ? null
			: DataManager.RETAIL_AI_DATA.findConditionWorldId(worldName);
		return worldId != null && supports(worldId, variable);
	}

	public static void initialize(WorldMapInstance instance) {
		if (DataManager.RETAIL_AI_DATA == null || DataManager.RETAIL_AI_DATA.getConditionSpawns(instance.getMapId()).isEmpty()) {
			return;
		}
		State state = STATES.computeIfAbsent(instance,
			ignored -> new State(DataManager.RETAIL_AI_DATA.getConditionSpawns(instance.getMapId())));
		synchronized (state) {
			evaluate(instance, state);
		}
	}

	public static boolean setVariable(WorldMapInstance instance, String variable, int set, int modify) {
		if (!supports(instance.getMapId(), variable)) {
			return false;
		}
		State state = STATES.computeIfAbsent(instance,
			ignored -> new State(DataManager.RETAIL_AI_DATA.getConditionSpawns(instance.getMapId())));
		synchronized (state) {
			String name = variable.toLowerCase(Locale.ROOT);
			state.variables.put(name, nextValue(state.variables.getOrDefault(name, 0), set, modify));
			evaluate(instance, state);
		}
		return true;
	}

	public static boolean setVariableToWorld(String worldName, String variable, int set, int modify) {
		Integer worldId = DataManager.RETAIL_AI_DATA == null ? null
			: DataManager.RETAIL_AI_DATA.findConditionWorldId(worldName);
		if (worldId == null || !supports(worldId, variable)) {
			return false;
		}
		boolean updated = false;
		for (WorldMapInstance instance : GameWorldBootstrapServices.world().getWorldMap(worldId).getInstances()) {
			updated |= setVariable(instance, variable, set, modify);
		}
		return updated;
	}

	public static boolean setFlag(WorldMapInstance instance, String flag, boolean set) {
		State state = STATES.computeIfAbsent(instance, RetailConditionSpawnEngine::newState);
		synchronized (state) {
			return updateFlag(state.flags, flag, set);
		}
	}

	public static boolean testFlag(WorldMapInstance instance, String flag, boolean expected) {
		State state = STATES.computeIfAbsent(instance, RetailConditionSpawnEngine::newState);
		synchronized (state) {
			return consumeFlag(state.flags, flag) == expected;
		}
	}

	public static void clear(WorldMapInstance instance) {
		State state = STATES.remove(instance);
		if (state == null) {
			return;
		}
		synchronized (state) {
			List<ActiveSpawn> active = List.copyOf(state.active.values());
			state.active.clear();
			active.forEach(RetailConditionSpawnEngine::delete);
		}
	}

	static int nextValue(int current, int set, int modify) {
		return modify == 0 ? set : current + modify;
	}

	static boolean evaluate(String expression, Map<String, Integer> variables) {
		return new Expression(expression, variables).parse();
	}

	static boolean updateFlag(Set<String> flags, String flag, boolean set) {
		String name = flag.toLowerCase(Locale.ROOT);
		return set ? flags.add(name) : flags.remove(name);
	}

	static boolean consumeFlag(Set<String> flags, String flag) {
		return flags.remove(flag.toLowerCase(Locale.ROOT));
	}

	private static State newState(WorldMapInstance instance) {
		return new State(DataManager.RETAIL_AI_DATA == null ? List.of()
			: DataManager.RETAIL_AI_DATA.getConditionSpawns(instance.getMapId()));
	}

	private static void evaluate(WorldMapInstance instance, State state) {
		for (ConditionSpawn condition : state.conditions) {
			boolean matches = evaluate(condition.expression(), state.variables);
			ActiveSpawn active = state.active.get(condition.id());
			if (matches && active == null) {
				activate(instance, state, condition);
			} else if (!matches && active != null && condition.despawnAtOther()) {
				state.active.remove(condition.id());
				delete(active);
			}
		}
	}

	private static void activate(WorldMapInstance instance, State state, ConditionSpawn condition) {
		ActiveSpawn active = new ActiveSpawn();
		state.active.put(condition.id(), active);
		List<ConditionSpawnGroup> groups = condition.groupMode().equals("all")
			? condition.groups() : List.of(select(condition.groups(), 1000, ConditionSpawnGroup::probability));
		for (ConditionSpawnGroup group : groups) {
			for (List<ConditionSpawnChoice> slot : group.slots()) {
				ConditionSpawnChoice choice = select(slot, 10000, ConditionSpawnChoice::probability);
				for (ConditionSpawnNpc npc : choice.members()) {
					long delay = (long) (npc.initialDelay() + Rnd.get(0, npc.initialDelayExtra())) * 1000;
					if (delay == 0) {
						spawn(instance, state, condition.id(), active, choice.partyId(), npc);
					} else {
						active.tasks.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
							synchronized (state) {
								if (STATES.get(instance) == state && state.active.get(condition.id()) == active) {
									spawn(instance, state, condition.id(), active, choice.partyId(), npc);
								}
							}
						}, delay));
					}
				}
			}
		}
	}

	private static void spawn(WorldMapInstance instance, State state, int conditionId, ActiveSpawn active,
			String partyId, ConditionSpawnNpc npc) {
		if (state.active.get(conditionId) != active) {
			return;
		}
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(instance.getMapId(), npc.id(), npc.x(), npc.y(),
			npc.z(), MathUtil.convertDegreeToHeading(npc.heading()));
		template.setWalkerId(npc.walkerId());
		template.setNpcPartyId(partyId);
		VisibleObject object = SpawnEngine.spawnObject(template, instance.getInstanceId());
		active.objects.add(object);
	}

	private static void delete(ActiveSpawn active) {
		active.tasks.forEach(task -> task.cancel(false));
		for (VisibleObject object : active.objects) {
			if (object.isSpawned()) {
				object.getController().onDelete();
			}
		}
	}

	private static <T> T select(List<T> values, int total, java.util.function.ToIntFunction<T> probability) {
		int roll = Rnd.get(1, total);
		for (T value : values) {
			roll -= probability.applyAsInt(value);
			if (roll <= 0) {
				return value;
			}
		}
		throw new IllegalStateException("Retail condition spawn probabilities do not total " + total);
	}

	private static final class State {
		private final List<ConditionSpawn> conditions;
		private final Map<String, Integer> variables = new HashMap<>();
		private final Set<String> flags = new HashSet<>();
		private final Map<Integer, ActiveSpawn> active = new HashMap<>();

		private State(List<ConditionSpawn> conditions) {
			this.conditions = conditions;
		}
	}

	private static final class ActiveSpawn {
		private final List<Future<?>> tasks = new ArrayList<>();
		private final List<VisibleObject> objects = new ArrayList<>();
	}

	private static final class Expression {
		private final String source;
		private final Map<String, Integer> variables;
		private int position;

		private Expression(String source, Map<String, Integer> variables) {
			this.source = source;
			this.variables = variables;
		}

		private boolean parse() {
			int value = or();
			skipWhitespace();
			if (position != source.length()) {
				throw new IllegalArgumentException("Unexpected retail condition token at " + position + ": " + source);
			}
			return value != 0;
		}

		private int or() {
			int value = and();
			while (consume("||")) {
				int right = and();
				value = value != 0 || right != 0 ? 1 : 0;
			}
			return value;
		}

		private int and() {
			int value = comparison();
			while (consume("&&")) {
				int right = comparison();
				value = value != 0 && right != 0 ? 1 : 0;
			}
			return value;
		}

		private int comparison() {
			int left = primary();
			for (String operator : List.of("==", "!=", ">=", "<=", ">", "<")) {
				if (consume(operator)) {
					int right = primary();
					return switch (operator) {
						case "==" -> left == right ? 1 : 0;
						case "!=" -> left != right ? 1 : 0;
						case ">=" -> left >= right ? 1 : 0;
						case "<=" -> left <= right ? 1 : 0;
						case ">" -> left > right ? 1 : 0;
						default -> left < right ? 1 : 0;
					};
				}
			}
			return left;
		}

		private int primary() {
			skipWhitespace();
			if (consume("(")) {
				int value = or();
				if (!consume(")")) {
					throw new IllegalArgumentException("Unclosed retail condition: " + source);
				}
				return value;
			}
			int start = position;
			if (position < source.length() && source.charAt(position) == '-') {
				position++;
			}
			while (position < source.length() && Character.isDigit(source.charAt(position))) {
				position++;
			}
			if (position > start && !(position == start + 1 && source.charAt(start) == '-')) {
				return Integer.parseInt(source.substring(start, position));
			}
			position = start;
			while (position < source.length()) {
				char character = source.charAt(position);
				if (!Character.isLetterOrDigit(character) && character != '_') {
					break;
				}
				position++;
			}
			if (position == start) {
				throw new IllegalArgumentException("Missing retail condition value at " + position + ": " + source);
			}
			return variables.getOrDefault(source.substring(start, position).toLowerCase(Locale.ROOT), 0);
		}

		private boolean consume(String token) {
			skipWhitespace();
			if (!source.startsWith(token, position)) {
				return false;
			}
			position += token.length();
			return true;
		}

		private void skipWhitespace() {
			while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
				position++;
			}
		}
	}
}
