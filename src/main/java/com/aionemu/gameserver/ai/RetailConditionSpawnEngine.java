package com.aionemu.gameserver.ai;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawn;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnChoice;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnNpc;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
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
import java.util.concurrent.Future;

/** 真端实例级条件变量与条件刷怪。 */
public final class RetailConditionSpawnEngine {

	private static final String STATE_PREFIX = "retail.condition.";

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
		State state = state(instance);
		synchronized (state) {
			evaluate(instance, state);
		}
	}

	public static boolean setVariable(WorldMapInstance instance, String variable, int set, int modify) {
		if (!supports(instance.getMapId(), variable)) {
			return false;
		}
		State state = state(instance);
		synchronized (state) {
			String name = variable.toLowerCase(Locale.ROOT);
			int value = nextValue(state.variables.getOrDefault(name, 0), set, modify);
			state.variables.put(name, value);
			state.runtime.put(variableKey(name), value);
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
		State state = state(instance);
		synchronized (state) {
			boolean changed = updateFlag(state.flags, flag, set);
			if (changed) {
				String key = flagKey(flag);
				if (set) {
					state.runtime.put(key, true);
				} else {
					state.runtime.remove(key);
				}
			}
			return changed;
		}
	}

	public static boolean testFlag(WorldMapInstance instance, String flag, boolean expected) {
		State state = state(instance);
		synchronized (state) {
			boolean value = consumeFlag(state.flags, flag);
			if (value) {
				state.runtime.remove(flagKey(flag));
			}
			return value == expected;
		}
	}

	public static void clear(WorldMapInstance instance) {
		State state = instance.removeTransientState(State.class);
		if (state == null) {
			instance.getRuntimeState().removePrefix(STATE_PREFIX);
			return;
		}
		synchronized (state) {
			List<ActiveSpawn> active = List.copyOf(state.active.values());
			state.active.clear();
			active.forEach(RetailConditionSpawnEngine::delete);
		}
		instance.getRuntimeState().removePrefix(STATE_PREFIX);
	}

	public static void onDie(Npc npc) {
		onDie(npc.getPosition().getWorldMapInstance(), npc);
	}

	/** Returns and consumes the condition-spawn death mode captured before respawn state is cleared. */
	public static Boolean consumeConditionSpawnDeath(Npc npc) {
		WorldMapInstance instance = npc.getPosition().getWorldMapInstance();
		State state = instance == null ? null : instance.getTransientState(State.class);
		if (state == null) {
			return null;
		}
		synchronized (state) {
			return state.conditionSpawnDeaths.remove(npc.getObjectId());
		}
	}

	static void onDie(WorldMapInstance instance, Npc npc) {
		State state = instance.getTransientState(State.class);
		if (state == null) {
			return;
		}
		synchronized (state) {
			for (ActiveSpawn active : state.active.values()) {
				Spawned spawned = active.spawns.remove(npc.getSpawn());
				if (spawned != null) {
					state.conditionSpawnDeaths.put(npc.getObjectId(),
						spawned.npc().respawnTime() > 0 || spawned.npc().respawnTimeExtra() > 0);
					if (spawned.npc().respawnTime() == 0 && spawned.npc().respawnTimeExtra() == 0) {
						state.runtime.put(spawned.key() + "dead", true);
					} else {
						String deadlineKey = spawned.key() + "respawn_deadline";
						if (state.runtime.getLong(deadlineKey, 0) != 0) {
							return;
						}
						long deadline = System.currentTimeMillis() + (long) respawnDelaySeconds(spawned.npc()) * 1000;
						state.runtime.put(deadlineKey, deadline);
						npc.getSpawn().setRespawnTime(0);
						scheduleRespawn(instance, state, active, spawned, deadline);
					}
					return;
				}
			}
		}
	}

	static int nextValue(int current, int set, int modify) {
		return modify == 0 ? set : current + modify;
	}

	static int respawnDelaySeconds(ConditionSpawnNpc npc) {
		return npc.respawnTime() + Rnd.get(0, npc.respawnTimeExtra());
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

	private static State state(WorldMapInstance instance) {
		int spawnPage = instance.getDynamicInstance() == null ? 0 : instance.getDynamicInstance().getSpawnPage();
		return instance.getOrCreateTransientState(State.class, () -> new State(instance.getRuntimeState(),
			DataManager.RETAIL_AI_DATA == null ? List.of()
				: conditionsForPage(DataManager.RETAIL_AI_DATA.getConditionSpawns(instance.getMapId()), spawnPage)));
	}

	static List<ConditionSpawn> conditionsForPage(List<ConditionSpawn> conditions, int spawnPage) {
		return conditions.stream()
			.filter(condition -> spawnPage >= condition.pageStart() && spawnPage <= condition.pageEnd())
			.toList();
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
				state.runtime.removePrefix(conditionKey(condition.id()));
			}
		}
	}

	private static void activate(WorldMapInstance instance, State state, ConditionSpawn condition) {
		ActiveSpawn active = new ActiveSpawn();
		state.active.put(condition.id(), active);
		List<Integer> groupIndexes = selectedGroups(state, condition);
		for (int groupIndex : groupIndexes) {
			ConditionSpawnGroup group = condition.groups().get(groupIndex);
			for (int slotIndex = 0; slotIndex < group.slots().size(); slotIndex++) {
				List<ConditionSpawnChoice> slot = group.slots().get(slotIndex);
				int choiceIndex = selectedChoice(state, condition.id(), groupIndex, slotIndex, slot);
				ConditionSpawnChoice choice = slot.get(choiceIndex);
				for (int memberIndex = 0; memberIndex < choice.members().size(); memberIndex++) {
					ConditionSpawnNpc npc = choice.members().get(memberIndex);
					String spawnKey = conditionKey(condition.id()) + "object." + groupIndex + '.' + slotIndex + '.' + memberIndex + '.';
					String deadlineKey = spawnKey + "spawn_deadline";
					long deadline = state.runtime.getLong(deadlineKey, 0);
					if (deadline == 0) {
						deadline = System.currentTimeMillis()
							+ (long) (npc.initialDelay() + Rnd.get(0, npc.initialDelayExtra())) * 1000;
						state.runtime.put(deadlineKey, deadline);
					}
					long delay = Math.max(0, deadline - System.currentTimeMillis());
					if (delay == 0) {
						spawn(instance, state, condition.id(), active, choice.partyId(), npc, spawnKey, deadline);
					} else {
						long spawnDeadline = deadline;
						active.tasks.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
							synchronized (state) {
								if (instance.getTransientState(State.class) == state && state.active.get(condition.id()) == active) {
									spawn(instance, state, condition.id(), active, choice.partyId(), npc, spawnKey, spawnDeadline);
								}
							}
						}, delay));
					}
				}
			}
		}
	}

	private static List<Integer> selectedGroups(State state, ConditionSpawn condition) {
		if (condition.groupMode().equals("all")) {
			List<Integer> indexes = new ArrayList<>(condition.groups().size());
			for (int i = 0; i < condition.groups().size(); i++) {
				indexes.add(i);
			}
			return indexes;
		}
		String key = conditionKey(condition.id()) + "group";
		int index = state.runtime.getInt(key, -1);
		if (index < 0) {
			ConditionSpawnGroup selected = select(condition.groups(), 1000, ConditionSpawnGroup::probability);
			index = condition.groups().indexOf(selected);
			state.runtime.put(key, index);
		}
		return List.of(index);
	}

	private static int selectedChoice(State state, int conditionId, int groupIndex, int slotIndex,
			List<ConditionSpawnChoice> choices) {
		String key = conditionKey(conditionId) + "choice." + groupIndex + '.' + slotIndex;
		int index = state.runtime.getInt(key, -1);
		if (index < 0) {
			ConditionSpawnChoice selected = select(choices, 10000, ConditionSpawnChoice::probability);
			index = choices.indexOf(selected);
			state.runtime.put(key, index);
		}
		return index;
	}

	private static String variableKey(String variable) {
		return STATE_PREFIX + "variable." + variable.toLowerCase(Locale.ROOT);
	}

	private static String flagKey(String flag) {
		return STATE_PREFIX + "flag." + flag.toLowerCase(Locale.ROOT);
	}

	private static String conditionKey(int conditionId) {
		return STATE_PREFIX + "spawn." + conditionId + '.';
	}

	private static void spawn(WorldMapInstance instance, State state, int conditionId, ActiveSpawn active,
			String partyId, ConditionSpawnNpc npc, String spawnKey, long spawnDeadline) {
		if (state.active.get(conditionId) != active || state.runtime.getBoolean(spawnKey + "dead", false)) {
			return;
		}
		long respawnDeadline = state.runtime.getLong(spawnKey + "respawn_deadline", 0);
		if (respawnDeadline > System.currentTimeMillis()) {
			scheduleRespawn(instance, state, active,
				new Spawned(conditionId, partyId, npc, spawnKey, spawnDeadline), respawnDeadline);
			return;
		}
		state.runtime.remove(spawnKey + "respawn_deadline");
		long lifeDeadline = 0;
		if (npc.life() > 0) {
			lifeDeadline = state.runtime.getLong(spawnKey + "life_deadline", 0);
			if (lifeDeadline == 0) {
				lifeDeadline = spawnDeadline + (long) npc.life() * 1000;
				state.runtime.put(spawnKey + "life_deadline", lifeDeadline);
			}
			if (lifeDeadline <= System.currentTimeMillis()) {
				return;
			}
		}
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(instance.getMapId(), npc.id(), npc.x(), npc.y(),
			npc.z(), MathUtil.convertDegreeToHeading(npc.heading()));
		template.setWalkerId(npc.walkerId());
		template.setNpcPartyId(partyId);
		VisibleObject object = SpawnEngine.spawnObject(template, instance.getInstanceId());
		active.spawns.put(template, new Spawned(conditionId, partyId, npc, spawnKey, spawnDeadline));
		if (npc.life() > 0) {
			long remainingLife = lifeDeadline - System.currentTimeMillis();
			active.tasks.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
				synchronized (state) {
					if (instance.getTransientState(State.class) == state && object.isSpawned()) {
						object.getController().onDelete();
					}
				}
			}, remainingLife));
		}
	}

	private static void scheduleRespawn(WorldMapInstance instance, State state, ActiveSpawn active,
			Spawned spawned, long deadline) {
		active.tasks.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
			synchronized (state) {
				if (instance.getTransientState(State.class) == state
					&& state.active.get(spawned.conditionId()) == active) {
					state.runtime.remove(spawned.key() + "respawn_deadline");
					spawn(instance, state, spawned.conditionId(), active, spawned.partyId(), spawned.npc(),
						spawned.key(), spawned.spawnDeadline());
				}
			}
		}, Math.max(0, deadline - System.currentTimeMillis())));
	}

	private static void delete(ActiveSpawn active) {
		active.tasks.forEach(task -> task.cancel(false));
		for (SpawnTemplate template : active.spawns.keySet()) {
			VisibleObject object = template.getVisibleObject();
			if (object instanceof Npc npc) {
				npc.getController().cancelTask(TaskId.RESPAWN);
			}
			if (object != null && object.isSpawned()) {
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
		private final InstanceRuntimeState runtime;
		private final List<ConditionSpawn> conditions;
		private final Map<String, Integer> variables = new HashMap<>();
		private final Set<String> flags = new HashSet<>();
		private final Map<Integer, ActiveSpawn> active = new HashMap<>();
		private final Map<Integer, Boolean> conditionSpawnDeaths = new HashMap<>();

		private State(InstanceRuntimeState runtime, List<ConditionSpawn> conditions) {
			this.runtime = runtime;
			this.conditions = conditions;
			runtime.snapshot(STATE_PREFIX + "variable.").forEach((key, value) ->
				variables.put(key.substring((STATE_PREFIX + "variable.").length()), Integer.parseInt(value)));
			runtime.snapshot(STATE_PREFIX + "flag.").keySet().forEach(key ->
				flags.add(key.substring((STATE_PREFIX + "flag.").length())));
		}
	}

	private static final class ActiveSpawn {
		private final List<Future<?>> tasks = new ArrayList<>();
		private final Map<SpawnTemplate, Spawned> spawns = new HashMap<>();
	}

	private record Spawned(int conditionId, String partyId, ConditionSpawnNpc npc, String key, long spawnDeadline) {
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
