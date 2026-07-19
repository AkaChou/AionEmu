package com.aionemu.gameserver.model.autogroup;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.PlayerClass;

public final class MatchDefinition {
	private static final ZoneId RETAIL_ZONE = ZoneId.of("Asia/Shanghai");
	private final Row row;

	private MatchDefinition(Row row) {
		this.row = row;
	}

	public int getInstanceMaskId() {
		return row.requiredInt("id");
	}

	public int getTime() {
		return row.intValue("limit_ins_draft", 0) * 1000;
	}

	public long getAgeRequisiteMillis() {
		return row.longValue("age_requisite", 0) * 1000L;
	}

	public long getAgeToleranceMillis() {
		return row.longValue("age_tolerance", 0) * 1000L;
	}

	public int getShuffleLimitSize() {
		return row.intValue("shuffle_limitsize", 0);
	}

	public int getShuffleMinimum() {
		return row.intValue("shuffle_limitmin", 0);
	}

	public long getShuffleIntervalMillis() {
		return row.longValue("shuffle_limitsec", 0) * 1000L;
	}

	public int getDraftLimitPerTurn() {
		return Math.max(1, row.intValue("draft_limit_per_turn", Integer.MAX_VALUE));
	}

	public int getMaximumInstances() {
		return row.intValue("max_ins_num", Integer.MAX_VALUE);
	}

	public int getPlayerSize() {
		return getPlayersPerSide() * getMatchSides();
	}

	public int getMinimumPlayers() {
		return getMinimumPlayersPerSide() * getMatchSides();
	}

	public int getPlayersPerSide() {
		return row.intValue("max_user", 0);
	}

	public int getMinimumPlayersPerSide() {
		return row.intValue("min_user", getPlayersPerSide());
	}

	public byte getDifficultId() {
		Row definition = DataManager.RETAIL_INSTANCE_DATA.definition(row.requiredInt("creation_id"));
		return (byte) definition.intValue("spawn_page", 0);
	}

	public int getCreationId() {
		return row.requiredInt("creation_id");
	}

	public int getInstanceMapId() {
		return row.requiredInt("world_id");
	}

	public int getNameId() {
		return row.intValue("name_id", 0);
	}

	public int getTitleId() {
		return row.intValue("title_id", 0);
	}

	public int getMinLevel() {
		return row.intValue("min_pc_lvl", 0);
	}

	public int getMaxLevel() {
		return row.intValue("max_pc_lvl", Integer.MAX_VALUE);
	}

	public boolean hasRegisterGroup() {
		return row.intValue("register_group", 0) > 0;
	}

	public boolean hasRegisterFast() {
		return row.intValue("register_fast", 0) > 0;
	}

	public boolean hasSpecialPurpose() {
		return row.intValue("special_purpose", 0) > 0 || row.values().containsKey("custom_npc_ids");
	}

	public boolean hasRegisterNew() {
		return row.intValue("register_new", 1) != 0;
	}

	public boolean hasHudRegister() {
		return flag("hud_register");
	}

	public boolean containNpcId(int npcId) {
		return getNpcIds().contains(npcId);
	}

	public List<Integer> getNpcIds() {
		String value = row.values().getOrDefault("npc_ids", "");
		return value.isEmpty() ? List.of() : Arrays.stream(value.split(",")).map(Integer::valueOf).toList();
	}

	public boolean isTrainingHarmonyArena() {
		return "TRAINING_HARMONY".equals(category());
	}

	public boolean isHarmonyArena() {
		return "HARMONY".equals(category());
	}

	public boolean isTournament() {
		return "TOURNAMENT".equals(handler());
	}

	public boolean isTeamMatch() {
		return "TEAM_MATCH".equals(category());
	}

	public int getTournamentId() {
		return row.intValue("tournament_id", 0);
	}

	public boolean hasLevelPermit(int level) {
		return level >= getMinLevel() && level <= getMaxLevel();
	}

	public boolean isOpen(ZonedDateTime time) {
		time = time.withZoneSameInstant(RETAIL_ZONE);
		String day = time.getDayOfWeek().name().substring(0, 3).toLowerCase();
		int hour = time.getHour();
		String slot = day + (hour < 12 ? "_am" : "_pm") + hour % 12;
		return time.getMinute() < row.intValue(slot, 0);
	}

	public boolean isOpen() {
		return isOpen(ZonedDateTime.now(RETAIL_ZONE));
	}

	public int getMatchSides() {
		return Math.max(1, row.intValue("num_matchside", 1));
	}

	public boolean isRaceFree() {
		return flag("racefree");
	}

	public boolean canAdd(PlayerClass playerClass, Collection<AGPlayer> players, int additions) {
		String key = classKey(playerClass);
		long count = players.stream().filter(player -> classKey(player.getPlayerClass()).equals(key)).count();
		int maximum = getMaximumPlayers(playerClass);
		return count + additions <= maximum;
	}

	public int getRequiredPlayers(PlayerClass playerClass) {
		return row.intValue("req_" + classKey(playerClass), 0);
	}

	public int getMaximumPlayers(PlayerClass playerClass) {
		return row.intValue("max_" + classKey(playerClass), getPlayersPerSide());
	}

	public boolean isCompositionReady(Collection<AGPlayer> players) {
		return isCompositionReady(players, 0);
	}

	public boolean isCompositionReady(Collection<AGPlayer> players, long oldestWaitMillis) {
		for (byte side = 0; side < getMatchSides(); side++) {
			byte matchSide = side;
			List<AGPlayer> sidePlayers = players.stream()
					.filter(player -> player.getMatchSide() == matchSide).toList();
			if (sidePlayers.size() < getMinimumPlayersPerSide()) {
				return false;
			}
			if (getAgeRequisiteMillis() > 0 && oldestWaitMillis >= getAgeRequisiteMillis()) {
				continue;
			}
			for (String key : List.of("fighter", "knight", "ranger", "assassin", "wizard", "element", "priest",
					"chanter", "gunner", "rider", "bard")) {
				long count = sidePlayers.stream().filter(player -> classKey(player.getPlayerClass()).equals(key)).count();
				if (count < row.intValue("req_" + key, 0)) {
					return false;
				}
			}
		}
		return true;
	}

	public AutoInstance getAutoInstance() {
		String className = row.value("adapter");
		if (className.isEmpty()) {
			throw new IllegalStateException("Matchmaker " + getInstanceMaskId() + " has no runtime adapter");
		}
		try {
			Class<?> type = Class.forName(className);
			if (!AutoInstance.class.isAssignableFrom(type)) {
				throw new IllegalStateException("Invalid matchmaker adapter " + className);
			}
			return (AutoInstance) type.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot create matchmaker adapter " + className, e);
		}
	}

	public static MatchDefinition getByMaskId(int instanceMaskId) {
		Row row = DataManager.RETAIL_INSTANCE_DATA.match(instanceMaskId);
		if (row != null) {
			return new MatchDefinition(row);
		}
		return DataManager.RETAIL_INSTANCE_DATA.teamMatches().stream()
				.filter(team -> team.requiredInt("world_id") == instanceMaskId)
				.map(MatchDefinition::teamDefinition).findFirst().orElse(null);
	}

	public static MatchDefinition forNpc(int level, int npcId) {
		return DataManager.RETAIL_INSTANCE_DATA.matchesForNpc(npcId).stream()
				.map(MatchDefinition::new).filter(type -> type.hasLevelPermit(level)).findFirst().orElse(null);
	}

	public static MatchDefinition forWorld(int level, int worldId) {
		return DataManager.RETAIL_INSTANCE_DATA.matchesForWorld(worldId).stream()
				.map(MatchDefinition::new).filter(type -> type.hasLevelPermit(level)).findFirst().orElse(null);
	}

	public static MatchDefinition forNpc(int npcId) {
		return DataManager.RETAIL_INSTANCE_DATA.matchesForNpc(npcId).stream()
				.map(MatchDefinition::new).findFirst().orElse(null);
	}

	public static List<MatchDefinition> all() {
		List<MatchDefinition> definitions = new ArrayList<>();
		DataManager.RETAIL_INSTANCE_DATA.matches().stream().map(MatchDefinition::new).forEach(definitions::add);
		DataManager.RETAIL_INSTANCE_DATA.teamMatches().stream().map(MatchDefinition::teamDefinition)
				.forEach(definitions::add);
		return List.copyOf(definitions);
	}

	public static Row teamMatch(int matchmakerId) {
		return DataManager.RETAIL_INSTANCE_DATA.teamMatch(matchmakerId);
	}

	private String handler() {
		return row.values().getOrDefault("handler", "GENERAL");
	}

	private String category() {
		return row.values().getOrDefault("category", "GENERAL");
	}

	private boolean flag(String name) {
		String value = row.values().get(name);
		return "1".equals(value) || Boolean.parseBoolean(value);
	}

	private static String classKey(PlayerClass playerClass) {
		return switch (playerClass) {
			case GLADIATOR, WARRIOR -> "fighter";
			case TEMPLAR -> "knight";
			case RANGER -> "ranger";
			case ASSASSIN, SCOUT -> "assassin";
			case SORCERER, MAGE -> "wizard";
			case SPIRIT_MASTER -> "element";
			case CLERIC, PRIEST -> "priest";
			case CHANTER -> "chanter";
			case GUNSLINGER, TECHNIST -> "gunner";
			case AETHERTECH -> "rider";
			case SONGWEAVER, MUSE -> "bard";
			case ALL -> "fighter";
		};
	}

	private static MatchDefinition teamDefinition(Row team) {
		Map<String, String> values = new LinkedHashMap<>(team.values());
		int sides = Math.max(1, team.intValue("req_team_num", 1));
		values.put("id", team.value("world_id"));
		values.put("num_matchside", Integer.toString(sides));
		values.put("min_user", team.value("req_user_num"));
		values.put("max_user", Integer.toString(team.requiredInt("max_user_num") / sides));
		values.put("register_group", "1");
		values.put("register_fast", "1");
		values.put("register_new", "0");
		values.put("racefree", "FALSE");
		values.put("draft_limit_per_turn", team.value("max_ins_num"));
		values.put("category", "TEAM_MATCH");
		values.put("handler", "GENERAL");
		return new MatchDefinition(new Row(values));
	}
}
