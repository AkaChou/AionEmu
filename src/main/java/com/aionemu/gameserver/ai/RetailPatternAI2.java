package com.aionemu.gameserver.ai;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.handler.ReturningEventHandler;
import com.aionemu.gameserver.ai2.handler.TargetEventHandler;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.questEngine.model.RetailQuestState;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.services.LimitedQuestService;
import com.aionemu.gameserver.services.instance.InstanceDeadlineScheduler;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/** 执行真端 NPC AI Pattern 的通用 AI。 */
@AIName("retail_pattern")
public class RetailPatternAI2 extends AggressiveNpcAI2 {

	private static final Set<String> SUPPORTED_EVENTS = Set.of(
		"on_wake_up", "on_enter_attack_state", "on_attacked", "on_battle_timer", "on_idle_timer",
		"on_enter_idle_state", "on_leave_attack_state", "on_die", "on_killed_by_user", "on_killed_by_npc",
		"on_see_user", "on_spelled", "on_healed_by_user", "on_despawn", "on_message", "on_stop_to_random_move",
		"on_arrived_at_waypoint", "on_talked_by_user", "on_see_npc", "on_see_user_move", "on_see_npc_move",
		"on_damaged", "on_most_hating_updated", "on_stop_to_flee", "on_enter_abnormal_state",
		"on_leave_abnormal_state", "on_see_friend_attacked", "on_see_friend_attacking", "on_friend_spelling",
		"on_friend_spelled", "on_see_master_spelling", "on_see_master_spelled", "on_master_attacked",
		"on_see_friend_killed_by_user", "on_sense_friend_killed_by_user",
		"on_friend_enter_attack_state", "on_casted", "on_see_spell", "on_see_attacked", "on_enter_return_sp",
		"on_leave_return_sp", "on_hyperlink_clicked", "on_user_enter_sensory_area", "on_user_leave_sensory_area",
		"on_end_feared", "on_arrived_at_point", "on_gauge_begin", "on_gauge_stop", "on_gauge_end",
		"on_enter_wakeup_state", "on_leave_wakeup_state", "on_quit_cutscene", "on_quest_finished",
		"on_party_mbr_attacking", "on_party_mbr_attacked", "on_party_mbr_spelled",
		"on_party_mbr_enter_attack_state");
	private static final Set<String> SUPPORTED_CONDITIONS = Set.of(
		"is_hp_lower_than", "is_hp_in_boundary", "is_battle_timer_indicator", "test_probability",
		"set_flag_var", "unset_flag_var", "is_skill_count_left", "is_message", "is_user", "is_enemy",
		"is_event_skill_id", "is_user_flying", "is_race", "increase_intvar", "is_distance_longer_than",
		"is_distance_shorter_than", "is_npc_state", "set_intvar_if_larger_than", "set_intvar_if_less_than",
		"is_user_class", "set_world_flag_var", "unset_world_flag_var", "is_npc", "is_in_abnormal_state",
			"is_obj_in_abnormal_state", "is_waypoint_index", "is_last_waypoint", "is_tribe", "is_abnormal_state",
			"add_intvar", "decrease_intvar", "is_user_level", "is_user_gender", "is_my_curent_target",
			"is_world_flag_var", "sub_intvar", "is_event_skill_category", "is_hyperlink_id",
			"has_attack_damage_flag", "is_target_quest_state");
	private static final Set<String> ATTACK_DAMAGE_FLAGS = Set.of("DODGE");
	private static final Set<String> SUPPORTED_ACTIONS = Set.of(
		"use_skill", "use_skill_by_attacker_indicator", "switch_target_by_attacker_indicator", "add_battle_timer",
		"set_idle_timer", "spawn", "spawn_on_target", "despawn", "despawn_self", "do_nothing", "broadcast_message",
		"say_to_all", "display_system_message", "send_system_msg_by_user_indicator", "reset_hatepoints",
		"spawn_on_target_by_attacker_indicator",
		"spawn_on_multi_target", "despawn_by_nameid", "control_door", "set_condition_spawn_variable",
		"set_condition_spawn_variable_to_world",
		"give_item_by_user_indicator", "give_item_by_obj_indicator", "give_score", "give_exp",
		"toggle_attackable_status_flag",
		"play_cutscene_by_user_indicator", "add_hate_point", "switch_target", "attack_most_hating",
		"change_direction", "random_move", "goto_waypoint", "goto_next_waypoint", "send_system_msg", "flee_from",
		"switch_target_by_class_indicator", "close_dialog", "teleport_target_alias", "shout_to_all",
			"change_world_scene_status", "say_to_all_str", "teleport_target", "say", "send_message",
				"return_to_spawn_point", "reset_queued_actions", "goto_alias", "activate_skillarea", "open_directportal",
				"open_directportal_by_user", "close_directportal", "enable_area", "on_off_windpath",
				"on_off_moving_collision",
				"system_message_to_all_by_obj_indicator_param",
				"broadcast_message_to_party", "charge_limitedquest");
	private static final Set<String> SUPPORTED_TARGETS = Set.of(
		"OBJI_SELF", "OBJI_CUR_TARGET", "OBJI_EVENT_TARGET", "OBJI_CASTER", "OBJI_MESSAGE_SENDER",
		"OBJI_MESSAGE_PARAM", "OBJI_ATTACKER", "OBJI_FLEE_FROM", "OBJI_FRIEND", "OBJI_KILLER", "OBJI_SEEN",
		"OBJI_TALKER", "OBJI_PARTY_MEMBER");
	private static final Set<String> SUPPORTED_MESSAGE_OBJECTS = Set.of(
		"OBJI_SELF", "OBJI_CUR_TARGET", "OBJI_EVENT_TARGET", "OBJI_FLEE_FROM", "OBJI_ATTACKER", "OBJI_CASTER",
		"OBJI_FRIEND", "OBJI_KILLER", "OBJI_SEEN", "OBJI_TALKER");
	private static final Set<String> SUPPORTED_ATTACKER_TARGETS = Set.of(
		"ATTACKERI_RANDOM_ONE", "ATTACKERI_RANDOM_ONE_EXCEPT_CURRENT_TARGET", "ATTACKERI_SECOND_HATING",
		"ATTACKERI_THIRD_HATING", "ATTACKERI_HAS_LOWEST_HP", "ATTACKERI_HAS_MOST_HP");
	private static final Set<String> TARGET_EVENTS = Set.of(
		"on_enter_attack_state", "on_attacked", "on_battle_timer", "on_killed_by_user", "on_killed_by_npc", "on_see_user", "on_spelled",
		"on_healed_by_user", "on_message", "on_talked_by_user", "on_see_npc", "on_see_user_move", "on_see_npc_move", "on_damaged",
		"on_most_hating_updated", "on_stop_to_flee", "on_enter_abnormal_state", "on_leave_abnormal_state",
		"on_see_friend_attacked", "on_see_friend_attacking", "on_friend_spelling", "on_friend_spelled",
		"on_see_master_spelling", "on_see_master_spelled", "on_master_attacked",
		"on_see_friend_killed_by_user", "on_sense_friend_killed_by_user", "on_friend_enter_attack_state",
		"on_casted", "on_see_spell", "on_hyperlink_clicked", "on_user_enter_sensory_area",
		"on_user_leave_sensory_area", "on_gauge_begin", "on_gauge_stop", "on_gauge_end", "on_quit_cutscene",
		"on_party_mbr_attacking", "on_party_mbr_attacked", "on_party_mbr_spelled",
		"on_party_mbr_enter_attack_state");
	private static final Set<String> GAUGE_EVENTS = Set.of("on_gauge_begin", "on_gauge_stop", "on_gauge_end");
	private static final Set<String> WAKE_UP_EVENTS = Set.of("on_enter_wakeup_state", "on_leave_wakeup_state");
	private static final Set<String> RETAIL_IDLE_EVENTS = Set.of(
		"on_wake_up", "on_enter_idle_state", "on_enter_wakeup_state", "on_leave_wakeup_state");
	private static final Set<String> RETAIL_COMBAT_SKILL_EVENTS = Set.of(
		"on_enter_attack_state", "on_battle_timer");
	private static final String WAKE_UP_TIMER = "WAKE_UP_STATE";
	private static final String STATE_PREFIX = "retail.pattern.ai.";
	private static final Set<String> LOCAL_STATE_CONDITIONS = Set.of(
		"set_flag_var", "unset_flag_var", "increase_intvar", "set_intvar_if_larger_than",
		"set_intvar_if_less_than", "add_intvar", "decrease_intvar", "sub_intvar");
	private static final Set<String> SENSORY_EVENTS = Set.of(
		"on_user_enter_sensory_area", "on_user_leave_sensory_area");
	private static final Set<String> FRIEND_EVENTS = Set.of(
		"on_see_friend_attacked", "on_see_friend_attacking", "on_friend_spelling", "on_friend_spelled");
	private static final Set<String> MASTER_EVENTS = Set.of(
		"on_see_master_spelling", "on_see_master_spelled", "on_master_attacked");
	private static final Set<String> NPC_PARTY_EVENTS = Set.of(
		"on_party_mbr_attacking", "on_party_mbr_attacked", "on_party_mbr_spelled",
		"on_party_mbr_enter_attack_state");
	private static final Set<String> TERMINAL_EVENTS = Set.of(
		"on_leave_attack_state", "on_die", "on_killed_by_user", "on_killed_by_npc", "on_despawn");
	private final Map<String, Future<?>> timers = new HashMap<>();
	private final Set<String> persistentTimers = new HashSet<>();
	private final Set<Future<?>> actionTasks = ConcurrentHashMap.newKeySet();
	private final Map<String, List<VisibleObject>> spawned = new HashMap<>();
	private final Map<Operation, String> spawnActionKeys = new IdentityHashMap<>();
	private final Map<VisibleObject, Boolean> despawnAtAttackState = new ConcurrentHashMap<>();
	private final Set<String> flags = new HashSet<>();
	private final Map<String, Integer> intVars = new HashMap<>();
	private final Set<Integer> usersInSensoryArea = ConcurrentHashMap.newKeySet();
	private final Map<Player, ItemUseObserver> gaugeObservers = new ConcurrentHashMap<>();
	private final Map<Integer, PendingCutsceneTeleport> pendingCutsceneTeleports = new ConcurrentHashMap<>();
	private Pattern pattern;
	private Area sensoryArea;
	private boolean fighting;
	private volatile long wakeUpUntil;
	private boolean waypointMoving;
	private boolean aliasMoving;
	private boolean waypointWasWalking;
	private int waypointIndex = -1;
	private int waypointMoveGeneration;
	private AIState waypointReturnState = AIState.IDLE;
	private AISubState waypointReturnSubState = AISubState.NONE;
	private Future<?> fleeMoveTask;
	private Future<?> fleeStopTask;
	private AIState fleeReturnState = AIState.IDLE;
	private AISubState fleeReturnSubState = AISubState.NONE;
	private Creature deathKiller;
	private String runtimeStatePrefix;
	private boolean restoringPattern;

	private record RetailMessage(int type, int param1, int param2, Creature sender, Creature paramObject) {
	}

	private record PendingCutsceneTeleport(int cutsceneId, String alias) {
	}

	public static boolean supports(Pattern pattern) {
		return supports(pattern, false);
	}

	private static boolean supports(Pattern pattern, boolean allowNpcScore) {
		if (pattern == null) {
			return false;
		}
		for (Map.Entry<String, List<Rule>> event : pattern.events().entrySet()) {
			if (!SUPPORTED_EVENTS.contains(event.getKey())) {
				return false;
			}
			for (Rule rule : event.getValue()) {
				if (!(rule.category() == null || rule.category().isBlank() || Set.of("PLANNED", "DIRECT", "INSTANT").contains(rule.category()))
					|| !rule.conditions().stream().allMatch(condition -> supportsCondition(event.getKey(), condition))
					|| !rule.actions().stream().allMatch(action -> supportsAction(event.getKey(), action, allowNpcScore))
					|| !supportsNpcPartyRule(event.getKey(), rule)
					|| !TARGET_EVENTS.contains(event.getKey()) && rule.actions().stream()
						.anyMatch(RetailPatternAI2::usesEventTarget)
					|| TERMINAL_EVENTS.contains(event.getKey())
						&& hasUnsupportedActionsAfterSkill(event.getKey(), rule.actions())) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean supports(Pattern pattern, Npc npc) {
		if (!supports(pattern, true) || DataManager.RETAIL_AI_DATA == null) {
			return false;
		}
		com.aionemu.gameserver.dataholders.RetailAiData.Npc definition = npc == null ? null
			: DataManager.RETAIL_AI_DATA.getNpc(npc.getNpcId());
		if (!hasCompleteGaugeData(pattern, definition)) {
			return false;
		}
		NpcTemplateType npcType = npc == null ? null : npc.getObjectTemplate().getNpcTemplateType();
		if (!hasCompleteWakeUpData(pattern, npcType)) {
			return false;
		}
		if (!hasCompleteMasterData(pattern, npc)) {
			return false;
		}
		if (!hasCompleteNpcPartyData(pattern, npc)) {
			return false;
		}
		if (!hasWorldSceneConsumer(pattern, npc == null ? 0 : npc.getWorldId())) {
			return false;
		}
		if (!Collections.disjoint(pattern.events().keySet(), SENSORY_EVENTS)
			&& DataManager.RETAIL_AI_DATA.findSensoryArea(npc.getWorldId(), npc.getNpcId(), npc.getSpawn().getX(),
				npc.getSpawn().getY(), npc.getSpawn().getZ()) == null) {
			return false;
		}
		WalkerTemplate npcWalker = walker(npc);
		if (pattern.events().containsKey("on_arrived_at_waypoint") && !hasWaypoints(npcWalker)) {
			return false;
		}
		if (pattern.events().containsKey("on_arrived_at_point")
			&& pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
				.noneMatch(action -> action.type().equals("goto_alias"))) {
			return false;
		}
		for (List<Rule> rules : pattern.events().values()) {
			for (Rule rule : rules) {
				for (Operation operation : concat(rule.conditions(), rule.actions())) {
					if ((operation.type().equals("goto_next_waypoint") || operation.type().equals("is_last_waypoint"))
						&& !hasWaypoints(npcWalker)) {
						return false;
					}
					if ((operation.type().equals("goto_waypoint") || operation.type().equals("is_waypoint_index"))
						&& (!hasWaypoints(npcWalker) || integer(operation,
							operation.type().equals("goto_waypoint") ? "waypoint" : "index") >= npcWalker.getRouteSteps().size())) {
						return false;
					}
					if ((isSkillAction(operation) || operation.type().equals("is_skill_count_left"))
						&& skill(npc.getSkillList(), operation) == null) {
						return false;
					}
					if ((operation.type().equals("spawn") || operation.type().equals("spawn_on_target")
						|| operation.type().equals("spawn_on_target_by_attacker_indicator")
						|| operation.type().equals("spawn_on_multi_target") || operation.type().equals("despawn_by_nameid"))
						&& DataManager.RETAIL_AI_DATA.findNpcId(value(operation,
							operation.type().equals("despawn_by_nameid") ? "target_npc_nameid" : "npc_nameid")) == null) {
						return false;
					}
					if (operation.type().equals("spawn") && !value(operation, "pathname").isBlank()) {
						String walkerId = retailWalkerId(npc.getWorldId(), value(operation, "pathname"));
						WalkerTemplate walker = DataManager.WALKER_DATA == null ? null
							: DataManager.WALKER_DATA.getWalkerTemplate(walkerId);
						if (walker == null || walker.getRouteSteps() == null || walker.getRouteSteps().isEmpty()) {
							return false;
						}
					}
					if (operation.type().equals("activate_skillarea")
						&& !DataManager.RETAIL_AI_DATA.hasSkillArea(integer(operation, "areaid"))) {
						return false;
					}
					if ((operation.type().equals("open_directportal") || operation.type().equals("open_directportal_by_user")
						|| operation.type().equals("close_directportal"))
						&& !DataManager.RETAIL_AI_DATA.hasDirectPortal(integer(operation, "direct_portal_id"))) {
						return false;
					}
					if (operation.type().equals("open_directportal_by_user")) {
						var portal = DataManager.RETAIL_AI_DATA.getDirectPortal(integer(operation, "direct_portal_id"));
						if (portal == null || portal.needItem().isBlank() || portal.groupId() <= 0 || portal.invadeType() != 5
							|| DataManager.ITEM_DATA == null || DataManager.ITEM_DATA.getItemTemplate(portal.needItem()) == null) {
							return false;
						}
					}
						if (operation.type().equals("enable_area") && !RetailAreaEngine.supports(npc.getWorldId(),
							value(operation, "area_type"), value(operation, "area_name"))) {
							return false;
						}
						if (operation.type().equals("on_off_windpath")
							&& !RetailWindstreamEngine.supports(npc.getWorldId(), integer(operation, "groupid"))) {
							return false;
						}
						if (operation.type().equals("on_off_moving_collision")
							&& !RetailDynamicAreaEngine.supports(npc.getWorldId(), value(operation, "type"),
								integer(operation, "sunzoneid"))) {
							return false;
						}
					if ((operation.type().equals("say") || operation.type().equals("say_to_all") || operation.type().equals("shout_to_all")
						|| operation.type().equals("display_system_message")
						|| operation.type().equals("send_system_msg_by_user_indicator")
						|| operation.type().equals("send_system_msg"))
						&& DataManager.RETAIL_AI_DATA.findStringId(value(operation, "string_id")) == null) {
						return false;
					}
					if (operation.type().equals("display_system_message") && !value(operation, "area_name").isBlank()
						&& !DataManager.RETAIL_AI_DATA.hasArea(value(operation, "area_name"))) {
						return false;
					}
					if (operation.type().equals("set_condition_spawn_variable")
						&& !RetailConditionSpawnEngine.supports(npc.getWorldId(), value(operation, "string"))) {
						return false;
					}
					if (operation.type().equals("set_condition_spawn_variable_to_world")
						&& !RetailConditionSpawnEngine.supports(value(operation, "worldid"), value(operation, "string"))) {
						return false;
					}
					if (Set.of("give_item_by_user_indicator", "give_item_by_obj_indicator").contains(operation.type())
						&& (DataManager.ITEM_DATA == null
							|| DataManager.ITEM_DATA.getItemTemplate(value(operation, "item_id")) == null)) {
						return false;
					}
					if (operation.type().equals("give_score")) {
						if (npc == null) {
							return false;
						}
						var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
						if (!supportsNpcScore(npc, score)) {
							return false;
						}
					}
					if ((operation.type().equals("teleport_target_alias") || operation.type().equals("goto_alias"))
						&& DataManager.RETAIL_AI_DATA.findLocationAlias(npc.getWorldId(), value(operation, "alias")) == null) {
						return false;
					}
					if (operation.type().equals("play_cutscene_by_user_indicator")
						&& !value(operation, "teleport_alias").isBlank()
						&& DataManager.RETAIL_AI_DATA.findLocationAlias(npc.getWorldId(), value(operation, "teleport_alias")) == null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean isMoveSupported() {
		Race race = getRace();
		return race != Race.PC_LIGHT_CASTLE_DOOR && race != Race.PC_DARK_CASTLE_DOOR
			&& race != Race.DRAGON_CASTLE_DOOR && super.isMoveSupported();
	}

	static boolean hasWorldSceneConsumer(Pattern pattern, int worldId) {
		return Set.of(300300000, 300320000).contains(worldId)
			|| pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
				.noneMatch(action -> action.type().equals("change_world_scene_status"));
	}

	static boolean hasCompleteNpcPartyData(Pattern pattern, Npc npc) {
		return pattern == null || !usesNpcParty(pattern)
			|| npc != null && npc.getNpcPartyId() != null && !npc.getNpcPartyId().isBlank();
	}

	private static boolean usesNpcParty(Pattern pattern) {
		return !Collections.disjoint(pattern.events().keySet(), NPC_PARTY_EVENTS)
			|| pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
				.anyMatch(action -> action.type().equals("broadcast_message_to_party"));
	}

	private static boolean supportsNpcPartyRule(String event, Rule rule) {
		if (!NPC_PARTY_EVENTS.contains(event)) {
			return true;
		}
		return rule.conditions().stream().allMatch(condition -> supportsNpcPartyCondition(event, condition))
			&& rule.actions().stream().allMatch(action -> supportsNpcPartyAction(event, action));
	}

	private static boolean supportsNpcPartyCondition(String event, Operation condition) {
		return switch (event) {
			case "on_party_mbr_attacking" -> switch (condition.type()) {
				case "is_user", "is_npc" -> value(condition, "obj_indicator").equals("OBJI_EVENT_TARGET");
				case "is_npc_state", "set_flag_var" -> true;
				default -> false;
			};
			case "on_party_mbr_attacked" -> switch (condition.type()) {
				case "is_hp_lower_than" -> value(condition, "who").equals("OBJI_PARTY_MEMBER");
				case "is_user" -> value(condition, "obj_indicator").equals("OBJI_ATTACKER");
				case "is_distance_longer_than", "is_distance_shorter_than" ->
					Set.of("OBJI_ATTACKER", "OBJI_EVENT_TARGET").contains(value(condition, "who"));
				case "is_skill_count_left", "is_npc_state" -> true;
				default -> false;
			};
			case "on_party_mbr_spelled" -> switch (condition.type()) {
				case "is_hp_lower_than" -> value(condition, "who").equals("OBJI_PARTY_MEMBER");
				case "is_user" -> value(condition, "obj_indicator").equals("OBJI_CASTER");
				case "is_distance_longer_than", "is_distance_shorter_than" ->
					Set.of("OBJI_CASTER", "OBJI_EVENT_TARGET").contains(value(condition, "who"));
				case "is_skill_count_left", "is_npc_state" -> true;
				default -> false;
			};
			case "on_party_mbr_enter_attack_state" -> switch (condition.type()) {
				case "is_distance_longer_than" -> value(condition, "who").equals("OBJI_EVENT_TARGET");
				case "is_npc_state" -> true;
				default -> false;
			};
			default -> false;
		};
	}

	private static boolean supportsNpcPartyAction(String event, Operation action) {
		return switch (event) {
			case "on_party_mbr_attacking" -> switch (action.type()) {
				case "switch_target", "add_hate_point" -> value(action, "target").equals("OBJI_EVENT_TARGET");
				case "add_battle_timer", "attack_most_hating", "do_nothing" -> true;
				default -> false;
			};
			case "on_party_mbr_attacked" -> switch (action.type()) {
				case "switch_target" -> value(action, "target").equals("OBJI_ATTACKER");
				case "attack_most_hating", "do_nothing" -> true;
				default -> false;
			};
			case "on_party_mbr_spelled" -> switch (action.type()) {
				case "switch_target" -> value(action, "target").equals("OBJI_CASTER");
				case "attack_most_hating", "do_nothing" -> true;
				default -> false;
			};
			case "on_party_mbr_enter_attack_state" -> action.type().equals("do_nothing");
			default -> false;
		};
	}

	static boolean hasCompleteGaugeData(Pattern pattern,
			com.aionemu.gameserver.dataholders.RetailAiData.Npc npc) {
		return pattern == null || Collections.disjoint(pattern.events().keySet(), GAUGE_EVENTS)
			|| npc != null && npc.talkDelay() > 0;
	}

	static boolean hasCompleteWakeUpData(Pattern pattern, NpcTemplateType npcType) {
		return pattern == null || Collections.disjoint(pattern.events().keySet(), WAKE_UP_EVENTS)
			|| npcType == NpcTemplateType.MONSTER;
	}

	static boolean hasCompleteMasterData(Pattern pattern, Npc npc) {
		return pattern == null || Collections.disjoint(pattern.events().keySet(), MASTER_EVENTS)
			&& pattern.events().values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream())
				.noneMatch(action -> value(action, "target").equals("USERI_MASTER"))
			|| npc != null && npc.getMaster() != npc;
	}

	@Override
	protected void handleSpawned() {
		pattern = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getPattern(getNpcId());
		indexSpawnActions();
		sensoryArea = pattern == null || Collections.disjoint(pattern.events().keySet(), SENSORY_EVENTS) ? null
			: DataManager.RETAIL_AI_DATA.findSensoryArea(getOwner().getWorldId(), getNpcId(), getOwner().getSpawn().getX(),
				getOwner().getSpawn().getY(), getOwner().getSpawn().getZ());
		super.handleSpawned();
		if (sensoryArea != null) {
			RetailSensoryAreaEngine.register(getPosition().getWorldMapInstance(), this);
		}
		if (restorePatternState()) {
			restoreDynamicSpawns();
			restoreTimers();
			restoringPattern = true;
			try {
				runEvent("on_wake_up", null, null);
			} finally {
				restoringPattern = false;
			}
			return;
		}
		if (runtimeStatePrefix != null) {
			runtimeState().put(runtimeStatePrefix + "initialized", true);
		}
		runEvent("on_wake_up", null, null);
		if (Collections.disjoint(pattern.events().keySet(), WAKE_UP_EVENTS)) {
			runEvent("on_enter_idle_state", null, null);
		} else {
			startWakeUpState();
		}
	}

	private void startWakeUpState() {
		long until = System.nanoTime() + 5_000_000_000L;
		wakeUpUntil = until;
		Future<?> previous = timers.put(WAKE_UP_TIMER,
			GameThreadPoolServices.threadPoolManager().schedule(() -> leaveWakeUpState(until), 5000));
		if (previous != null) {
			previous.cancel(false);
		}
		runEvent("on_enter_wakeup_state", null, null);
	}

	private void leaveWakeUpState(long expectedUntil) {
		if (wakeUpUntil != expectedUntil) {
			return;
		}
		wakeUpUntil = 0;
		runEvent("on_leave_wakeup_state", null, null);
		if (!isAlreadyDead() && getState() == AIState.IDLE) {
			runEvent("on_enter_idle_state", null, null);
		}
	}

	@Override
	public void think() {
		if (getState() != AIState.IDLE || shouldUseDefaultIdleThinking(pattern)) {
			super.think();
		}
	}

	static boolean shouldUseDefaultIdleThinking(Pattern pattern) {
		return pattern == null || Collections.disjoint(pattern.events().keySet(), RETAIL_IDLE_EVENTS);
	}

	@Override
	protected boolean usesScriptedSkillRotation() {
		return hasScriptedCombatSkills(pattern);
	}

	static boolean hasScriptedCombatSkills(Pattern pattern) {
		return pattern != null && pattern.events().entrySet().stream()
			.filter(event -> RETAIL_COMBAT_SKILL_EVENTS.contains(event.getKey()))
			.flatMap(event -> event.getValue().stream())
			.flatMap(rule -> rule.actions().stream())
			.anyMatch(RetailPatternAI2::isSkillAction);
	}

	@Override
	protected void handleAttack(Creature creature) {
		handleAttack(creature, AttackStatus.NORMALHIT);
	}

	@Override
	protected void handleAttack(Creature creature, AttackStatus attackStatus) {
		super.handleAttack(creature);
		if (!fighting) {
			fighting = true;
			runEvent("on_enter_attack_state", null, creature);
			notifyNpcParty("on_party_mbr_enter_attack_state", creature, null);
			getKnownList().doOnAllNpcs(observer ->
				observer.getAi2().onFriendEnterAttackState(getOwner(), creature));
		}
		runEvent("on_attacked", null, creature, null, null, 0, attackStatus);
		notifyNpcParty("on_party_mbr_attacked", creature, null);
	}

	@Override
	protected void handleCreatureSee(Creature creature) {
		if (shouldUseDefaultSightHandling(pattern, creature)) {
			super.handleCreatureSee(creature);
		}
		if (!isInRetailSight(creature)) {
			return;
		}
		if (creature instanceof Player) {
			runEvent("on_see_user", null, creature);
		} else if (creature instanceof Npc) {
			runEvent("on_see_npc", null, creature);
		}
	}

	static boolean shouldUseDefaultSightHandling(Pattern pattern, Creature creature) {
		return creature instanceof Player ? !pattern.events().containsKey("on_see_user")
			: !(creature instanceof Npc) || !pattern.events().containsKey("on_see_npc");
	}

	@Override
	protected void handleCreatureMoved(Creature creature) {
		super.handleCreatureMoved(creature);
		if (!isInRetailSight(creature)) {
			return;
		}
		if (creature instanceof Player) {
			runEvent("on_see_user_move", null, creature);
		} else if (creature instanceof Npc) {
			runEvent("on_see_npc_move", null, creature);
		}
	}

	@Override
	protected void handleDialogStart(Player player) {
		com.aionemu.gameserver.dataholders.RetailAiData.Npc npc = DataManager.RETAIL_AI_DATA.getNpc(getNpcId());
		if (!Collections.disjoint(pattern.events().keySet(), GAUGE_EVENTS) && npc.talkDelay() > 0) {
			startGauge(player, npc.talkDelay() * 1000);
			return;
		}
		handleTalkedByUser(player);
	}

	private void startGauge(Player player, int delay) {
		ItemUseObserver previous = gaugeObservers.get(player);
		if (previous != null) {
			stopGauge(player, previous);
		}
		ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				stopGauge(player, this);
			}

			@Override
			public void npcdialogrequested(Npc npc) {
				abort();
			}
		};
		gaugeObservers.put(player, observer);
		player.getObserveController().addObserver(observer);
		PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), delay, 1));
		PacketSendUtility.broadcastPacket(player,
			new SM_EMOTION(player, EmotionType.START_QUESTLOOT, 0, getObjectId()), true);
		player.getController().addTask(TaskId.ACTION_ITEM_NPC,
			GameThreadPoolServices.threadPoolManager().schedule(() -> finishGauge(player, observer, delay), delay));
		runEvent("on_gauge_begin", null, player);
	}

	private void stopGauge(Player player, ItemUseObserver observer) {
		if (!gaugeObservers.remove(player, observer)) {
			return;
		}
		player.getController().cancelTask(TaskId.ACTION_ITEM_NPC);
		player.getObserveController().removeObserver(observer);
		PacketSendUtility.broadcastPacket(player,
			new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
		PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), 0, 2));
		runEvent("on_gauge_stop", null, player);
	}

	private void finishGauge(Player player, ItemUseObserver observer, int delay) {
		if (!gaugeObservers.remove(player, observer)) {
			return;
		}
		player.getController().cancelTask(TaskId.ACTION_ITEM_NPC);
		player.getObserveController().removeObserver(observer);
		PacketSendUtility.broadcastPacket(player,
			new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getObjectId()), true);
		PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), delay, 2));
		handleTalkedByUser(player);
		runEvent("on_gauge_end", null, player);
	}

	private void handleTalkedByUser(Player player) {
		super.handleDialogStart(player);
		runEvent("on_talked_by_user", null, player);
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (!handlesHyperlink(pattern, dialogId)) {
			return false;
		}
		runEvent("on_hyperlink_clicked", null, player, new RetailMessage(0, dialogId, 0, null, null));
		return true;
	}

	static boolean handlesHyperlink(Pattern pattern, int dialogId) {
		return pattern != null && pattern.event("on_hyperlink_clicked").stream()
			.anyMatch(rule -> rule.conditions().stream().anyMatch(condition -> condition.type().equals("is_hyperlink_id")
				&& retailHyperlinkId(value(condition, "hyperlink_id")) == dialogId));
	}

	@Override
	protected void handleSpelled(Creature caster, int skillId, int skillLevel) {
		SkillTemplate skill = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (skill != null) {
			runEvent("on_spelled", null, caster, null, skill);
		}
		notifyNpcParty("on_party_mbr_spelled", caster, skill);
	}

	@Override
	public void onHealedByUser(Player player) {
		runEvent("on_healed_by_user", null, player);
	}

	@Override
	public void onDamaged(Creature attacker, int skillId) {
		runEvent("on_damaged", null, attacker, null,
			skillId == 0 ? null : DataManager.SKILL_DATA.getSkillTemplate(skillId));
	}

	@Override
	public void onSeeAttack(Creature attacker, Creature attacked) {
		if (attacker == getOwner()) {
			notifyNpcParty("on_party_mbr_attacking", attacked, null);
		}
		if (isMaster(attacker)) {
			return;
		}
		if (isMaster(attacked)) {
			runEvent("on_master_attacked", null, attacker);
			return;
		}
		boolean attackedFriend = isFriend(attacked);
		boolean attackerFriend = isFriend(attacker);
		if (attackedFriend && getOwner().isEnemy(attacker)) {
			runFriendEvent("on_see_friend_attacked", attacker, attacked, null);
		}
		if (attackerFriend && getOwner().isEnemy(attacked)) {
			runFriendEvent("on_see_friend_attacking", attacked, attacker, null);
		}
		if (!attackedFriend && !attackerFriend) {
			runEvent("on_see_attacked", null, attacker);
		}
	}

	@Override
	protected boolean handleCreatureNeedsSupport(Creature creature) {
		if (pattern != null && (pattern.events().containsKey("on_see_friend_attacked")
			|| pattern.events().containsKey("on_see_friend_attacking")
			|| pattern.events().containsKey("on_master_attacked"))) {
			return true;
		}
		return super.handleCreatureNeedsSupport(creature);
	}

	@Override
	public void onSeeSkill(Creature caster, Creature target, int skillId, int skillLevel) {
		SkillTemplate skill = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (isMaster(caster)) {
			runEvent("on_see_master_spelling", null, target,
				new RetailMessage(0, 0, 0, null, caster), skill);
			return;
		}
		if (isMaster(target)) {
			runEvent("on_see_master_spelled", null, caster,
				new RetailMessage(0, 0, 0, null, caster), skill);
			return;
		}
		if (isFriend(caster)) {
			runFriendEvent("on_friend_spelling", target, caster, skill);
		}
	}

	@Override
	public void onFriendSpelled(Creature caster, Creature friend, int skillId, int skillLevel) {
		if (isMaster(caster) || isMaster(friend)) {
			return;
		}
		if (isFriend(friend) && getOwner().isEnemy(caster)) {
			runFriendEvent("on_friend_spelled", caster, friend, DataManager.SKILL_DATA.getSkillTemplate(skillId));
		}
	}

	@Override
	public void onFriendKilledByUser(Creature friend, Player killer) {
		if (!isFriend(friend)) {
			return;
		}
		com.aionemu.gameserver.dataholders.RetailAiData.Npc npc = DataManager.RETAIL_AI_DATA.getNpc(getNpcId());
		if (npc == null) {
			return;
		}
		if (inRetailSenseRange(getOwner(), friend, killer, npc.sensoryRangeShort())) {
			runEvent("on_sense_friend_killed_by_user", null, killer);
		}
		if (inRetailSight(getOwner(), friend, npc.sensoryRange(), npc.sensoryAngle())) {
			runEvent("on_see_friend_killed_by_user", null, killer);
		}
	}

	@Override
	public void onFriendEnterAttackState(Creature friend, Creature target) {
		if (isFriend(friend)) {
			runEvent("on_friend_enter_attack_state", null, target);
		}
	}

	@Override
	public void onCasted(Creature caster, int skillId, int skillLevel) {
		runEvent("on_casted", null, caster, null, DataManager.SKILL_DATA.getSkillTemplate(skillId));
	}

	@Override
	public void onSeeSpell(Creature caster, Creature target, int skillId, int skillLevel) {
		if (isMaster(caster) || isMaster(target)) {
			return;
		}
		runEvent("on_see_spell", null, target, new RetailMessage(0, 0, 0, null, caster),
			DataManager.SKILL_DATA.getSkillTemplate(skillId));
	}

	@Override
	public void onQuitCutscene(Player player, int cutsceneId) {
		Integer playerObjectId = player.getObjectId();
		String alias = playerObjectId == null ? null : consumePendingCutsceneTeleport(playerObjectId, cutsceneId);
		if (alias != null) {
			List<LocationAliasPoint> points = DataManager.RETAIL_AI_DATA.findLocationAlias(getOwner().getWorldId(), alias);
			if (points != null && !points.isEmpty()) {
				LocationAliasPoint point = Rnd.get(points);
				TeleportService2.teleportTo(player, getOwner().getWorldId(), getOwner().getInstanceId(),
					point.x(), point.y(), point.z(), MathUtil.convertDegreeToHeading(point.direction()));
			}
		}
		runEvent("on_quit_cutscene", null, player);
	}

	private String consumePendingCutsceneTeleport(int playerObjectId, int cutsceneId) {
		PendingCutsceneTeleport pending = pendingCutsceneTeleports.get(playerObjectId);
		return pending != null && pending.cutsceneId() == cutsceneId
			&& pendingCutsceneTeleports.remove(playerObjectId, pending) ? pending.alias() : null;
	}

	@Override
	public void onQuestFinished(Player player, int questId) {
		runEvent("on_quest_finished", null, player);
	}

	static boolean inRetailSenseRange(Creature observer, Creature friend, Creature killer, float range) {
		return range > 0 && (MathUtil.isIn3dRange(observer, friend, range) || MathUtil.isIn3dRange(observer, killer, range));
	}

	static boolean inRetailSight(Creature observer, Creature friend, float range, float angle) {
		if (range <= 0 || !MathUtil.isIn3dRange(observer, friend, range)) {
			return false;
		}
		float difference = Math.abs(MathUtil.calculateAngleFrom(observer, friend)
			- MathUtil.convertHeadingToDegree(observer.getHeading()));
		difference = Math.min(difference, 360 - difference);
		return angle >= 360 || difference <= angle / 2;
	}

	private boolean isInRetailSight(Creature creature) {
		com.aionemu.gameserver.dataholders.RetailAiData.Npc npc = DataManager.RETAIL_AI_DATA.getNpc(getNpcId());
		return npc != null && inRetailSight(getOwner(), creature, npc.sensoryRange(), npc.sensoryAngle());
	}

	private boolean isFriend(Creature creature) {
		return creature == getOwner() || DataManager.TRIBE_RELATIONS_DATA
			.isFriendlyRelation(getOwner().getTribe(), creature.getTribe());
	}

	private boolean isMaster(Creature creature) {
		Creature master = getOwner().getMaster();
		return master != getOwner() && master == creature;
	}

	private void runFriendEvent(String event, Creature eventTarget, Creature friend, SkillTemplate skill) {
		runEvent(event, null, eventTarget, new RetailMessage(0, 0, 0, null, friend), skill);
	}

	private void notifyNpcParty(String event, Creature eventTarget, SkillTemplate skill) {
		for (Npc member : RetailNpcParty.members(getOwner())) {
			if (member.getAi2() instanceof RetailPatternAI2 ai) {
				ai.handleNpcPartyEvent(event, eventTarget, getOwner(), skill);
			}
		}
	}

	void handleNpcPartyEvent(String event, Creature eventTarget, Npc partyMember, SkillTemplate skill) {
		runEvent(event, null, eventTarget, new RetailMessage(0, 0, 0, partyMember, null), skill);
	}

	@Override
	public void onMostHatingUpdated(Creature creature) {
		runEvent("on_most_hating_updated", null, creature);
	}

	@Override
	public void onRetailMessage(int type, int param1, int param2, Creature sender, Creature parameter) {
		runEvent("on_message", null, parameter, new RetailMessage(type, param1, param2, sender, parameter));
	}

	@Override
	public void onEnterAbnormalState(Creature caster, int abnormalState) {
		runEvent("on_enter_abnormal_state", null, caster, null, null, abnormalState);
	}

	@Override
	public void onLeaveAbnormalState(Creature caster, int abnormalState) {
		runEvent("on_leave_abnormal_state", null, caster, null, null, abnormalState);
		if (matchesRetailAbnormal(abnormalState, "ABNSTATEI_FEAR")) {
			runEvent("on_end_feared", null, caster);
		}
	}

	@Override
	protected void handleMoveArrived() {
		if (!waypointMoving) {
			super.handleMoveArrived();
			return;
		}
		getOwner().getController().onMove();
		getOwner().getMoveController().abortMove();
		int generation = waypointMoveGeneration;
		runEvent(aliasMoving ? "on_arrived_at_point" : "on_arrived_at_waypoint", null, null);
		if (generation == waypointMoveGeneration) {
			waypointMoving = false;
			aliasMoving = false;
			if (getState() != AIState.DIED && getState() != AIState.DESPAWNED) {
				setStateIfNot(waypointReturnState);
				setSubStateIfNot(waypointReturnSubState);
				if (waypointWasWalking) {
					getOwner().setState(CreatureState.WALKING);
				} else {
					getOwner().unsetState(CreatureState.WALKING);
				}
			}
		}
	}

	@Override
	protected void handleNotAtHome() {
		runEvent("on_enter_return_sp", null, null);
		super.handleNotAtHome();
	}

	@Override
	protected void handleBackHome() {
		Set<Future<?>> previousActionTasks = Set.copyOf(actionTasks);
		runEvent("on_leave_attack_state", null, null);
		resetPatternState(previousActionTasks);
		super.handleBackHome();
		runEvent("on_leave_return_sp", null, null);
		runEvent("on_enter_idle_state", null, null);
	}

	@Override
	protected void handleDied() {
		runDeathEvent();
		unregisterSensoryArea();
		resetPatternState();
		super.handleDied();
	}

	@Override
	protected void handleKilled(Creature killer) {
		deathKiller = killer;
		Creature source = killer == null ? null : killer.getMaster();
		if (source instanceof Player) {
			runEvent("on_killed_by_user", null, source);
		} else if (source instanceof Npc) {
			runEvent("on_killed_by_npc", null, source);
		}
	}

	private void runDeathEvent() {
		Creature killer = deathKiller;
		deathKiller = null;
		runEvent("on_die", null, killer);
	}

	@Override
	protected void handleDespawned() {
		runEvent("on_despawn", null, null);
		unregisterSensoryArea();
		resetPatternState();
		super.handleDespawned();
	}

	boolean updateSensoryArea(Player player) {
		boolean inside = sensoryArea != null && getOwner().isSpawned() && !isAlreadyDead()
			&& player.getWorldId() == getOwner().getWorldId() && player.getInstanceId() == getOwner().getInstanceId()
			&& sensoryArea.isInside3D(player.getX(), player.getY(), player.getZ());
		if (inside && usersInSensoryArea.add(player.getObjectId())) {
			runEvent("on_user_enter_sensory_area", null, player);
		} else if (!inside) {
			leaveSensoryArea(player);
		}
		return inside && getOwner().isSpawned() && !isAlreadyDead();
	}

	void leaveSensoryArea(Player player) {
		if (usersInSensoryArea.remove(player.getObjectId())) {
			runEvent("on_user_leave_sensory_area", null, player);
		}
	}

	private void unregisterSensoryArea() {
		if (sensoryArea != null) {
			RetailSensoryAreaEngine.unregister(getPosition().getWorldMapInstance(), this);
			usersInSensoryArea.clear();
		}
	}

	private void runEvent(String event, String timer, Creature eventTarget) {
		runEvent(event, timer, eventTarget, null);
	}

	private void runEvent(String event, String timer, Creature eventTarget, RetailMessage message) {
		runEvent(event, timer, eventTarget, message, null);
	}

	private void runEvent(String event, String timer, Creature eventTarget, RetailMessage message, SkillTemplate eventSkill) {
		runEvent(event, timer, eventTarget, message, eventSkill, 0);
	}

	private void runEvent(String event, String timer, Creature eventTarget, RetailMessage message, SkillTemplate eventSkill,
			int eventAbnormalState) {
		runEvent(event, timer, eventTarget, message, eventSkill, eventAbnormalState, null);
	}

	private void runEvent(String event, String timer, Creature eventTarget, RetailMessage message, SkillTemplate eventSkill,
			int eventAbnormalState, AttackStatus attackStatus) {
		if (pattern == null || isAlreadyDead() && !TERMINAL_EVENTS.contains(event)) {
			return;
		}
		List<Rule> rules = pattern.event(event);
		if (restoringPattern) {
			Rule rule = restoreRule(rules, runtimeState(), runtimeStatePrefix + "restore_rule." + event);
			if (rule != null) {
				executeActions(rule.actions(), 0, eventTarget, message, TERMINAL_EVENTS.contains(event),
					supportsImmediateTerminalCleanup(event, rule.actions()), true);
			}
			return;
		}
		for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
			Rule rule = rules.get(ruleIndex);
			if (matches(rule, timer, eventTarget, message, eventSkill, eventAbnormalState, attackStatus)) {
				if (runtimeStatePrefix != null && event.equals("on_wake_up")) {
					runtimeState().put(runtimeStatePrefix + "restore_rule." + event, ruleIndex);
				}
				executeActions(rule.actions(), 0, eventTarget, message, TERMINAL_EVENTS.contains(event),
					supportsImmediateTerminalCleanup(event, rule.actions()), false);
				return;
			}
		}
	}

	static Rule restoreRule(List<Rule> rules, InstanceRuntimeState state, String key) {
		int ruleIndex = state.getInt(key, -1);
		return ruleIndex >= 0 && ruleIndex < rules.size() ? rules.get(ruleIndex) : null;
	}

	private void executeActions(List<Operation> actions, int start, Creature eventTarget, RetailMessage message,
			boolean allowDead, boolean immediateTerminalCleanup, boolean restoreOnly) {
		if (!allowDead && isAlreadyDead()) {
			return;
		}
		for (int i = start; i < actions.size(); i++) {
			Operation action = actions.get(i);
			if (restoreOnly && !canReplayDuringRestore(action)) {
				continue;
			}
			if (isSkillAction(action)) {
				int duration = useSkill(action, eventTarget, message);
				if (immediateTerminalCleanup) {
					continue;
				}
				if (duration >= 0 && i + 1 < actions.size()) {
					int next = i + 1;
					long delay = duration == 0 ? 0 : duration + (actions.get(next).type().equals("despawn_self") ? 1000L : 100L);
					actionTasks.removeIf(Future::isDone);
					actionTasks.add(GameThreadPoolServices.threadPoolManager()
						.schedule(() -> executeActions(actions, next, eventTarget, message, false, false, restoreOnly), delay));
					return;
				}
			} else if (action.type().equals("despawn_self")) {
				AI2Actions.deleteOwner(this);
			} else {
				execute(action, eventTarget, message);
			}
		}
	}

	static boolean canReplayDuringRestore(Operation action) {
		if (isSkillAction(action)) {
			return action.type().equals("activate_skillarea") || value(action, "target").equals("OBJI_SELF");
		}
		return Set.of("control_door", "toggle_attackable_status_flag", "change_direction", "random_move",
			"goto_waypoint", "goto_next_waypoint", "goto_alias", "do_nothing").contains(action.type());
	}

	private boolean matches(Rule rule, String timer, Creature eventTarget, RetailMessage message, SkillTemplate eventSkill,
			int eventAbnormalState, AttackStatus attackStatus) {
		for (Operation condition : rule.conditions()) {
			boolean matches = switch (condition.type()) {
				case "is_hp_lower_than" -> {
					Creature object = resolveObject(value(condition, "who"), eventTarget, message);
					yield object != null && object.getLifeStats().getHpPercentage() <= integer(condition, "percent");
				}
				case "is_hp_in_boundary" -> {
					Creature object = resolveObject(value(condition, "who"), eventTarget, message);
					yield object != null && object.getLifeStats().getHpPercentage() > integer(condition, "larger_than")
						&& object.getLifeStats().getHpPercentage() <= integer(condition, "less_than");
				}
				case "is_battle_timer_indicator" -> value(condition, "btimer_indicator").equals(timer);
				case "test_probability" -> Rnd.chance(integer(condition, "percent"));
				case "set_flag_var" -> flags.add(value(condition, "flagvar_indicator"));
				case "unset_flag_var" -> flags.remove(value(condition, "flagvar_indicator"));
				case "is_skill_count_left" -> {
					NpcSkillEntry skill = skill(condition);
					yield skill != null && skill.hasUsesLeft();
				}
				case "is_message" -> message != null && message.type() == integer(condition, "message_type");
				case "is_user" -> resolveObject(value(condition, "obj_indicator"), eventTarget, message) instanceof Player;
				case "is_npc" -> resolveObject(value(condition, "obj_indicator"), eventTarget, message) instanceof Npc;
				case "is_enemy" -> {
					Creature object = resolveObject(value(condition, "who"), eventTarget, message);
					yield object != null && (getOwner().isEnemy(object)
						|| object instanceof Npc npc && npc.isHostileFrom(getOwner()));
				}
				case "is_event_skill_id" -> eventSkill != null
					&& value(condition, "skill_id").equals(eventSkill.getNamedesc());
				case "is_event_skill_category" -> eventSkill != null && value(condition, "skill_category")
					.equals(DataManager.RETAIL_AI_DATA.getSkillCategory(eventSkill.getSkillId()));
				case "is_hyperlink_id" -> message != null
					&& message.param1() == retailHyperlinkId(value(condition, "hyperlink_id"));
				case "has_attack_damage_flag" -> matchesAttackDamageFlag(attackStatus, value(condition, "damage_flag"));
				case "is_target_quest_state" -> resolveObject(value(condition, "target"), eventTarget, message) instanceof Player player
					&& player.getQuestStateList() != null && RetailQuestState.valueOf(value(condition, "quest_progress"))
						.matches(player.getQuestStateList().getQuestState(integer(condition, "quest_id")));
				case "is_user_flying" -> resolveUser(value(condition, "user"), eventTarget, message) instanceof Player player
					&& player.isFlying();
				case "is_race" -> {
					Creature object = resolveObject(value(condition, "from"), eventTarget, message);
					yield object != null && matchesRace(object.getRace(), value(condition, "race_type"));
				}
				case "is_distance_longer_than", "is_distance_shorter_than" -> {
					Creature object = resolveObject(value(condition, "who"), eventTarget, message);
					yield object != null && matchesDistance(object, decimal(condition, "distance"),
						condition.type().equals("is_distance_shorter_than"));
				}
				case "increase_intvar" -> increaseIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "lower_bound"), integer(condition, "upper_bound"),
					Boolean.parseBoolean(value(condition, "be_true_only_when_hit_the_bound")));
				case "set_intvar_if_larger_than" -> setIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "intvar_to_set"), integer(condition, "comparand"), true);
				case "set_intvar_if_less_than" -> setIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "intvar_to_set"), integer(condition, "comparand"), false);
				case "is_npc_state" -> value(condition, "who").equals("NPCI_SELF")
					&& matchesNpcState(getState(), getSubState(), value(condition, "state"),
						System.nanoTime() < wakeUpUntil, aliasMoving);
				case "is_user_class" -> resolveUser(value(condition, "user"), eventTarget, message) instanceof Player player
					&& matchesUserClass(player.getPlayerClass(), value(condition, "class"));
				case "set_world_flag_var" -> RetailConditionSpawnEngine.setFlag(
					getPosition().getWorldMapInstance(), value(condition, "flagvar_indicator"), true);
				case "unset_world_flag_var" -> RetailConditionSpawnEngine.setFlag(
					getPosition().getWorldMapInstance(), value(condition, "flagvar_indicator"), false);
				case "is_in_abnormal_state" -> matchesRetailAbnormal(getOwner(), value(condition, "abnormal_state"));
				case "is_obj_in_abnormal_state" -> matchesRetailAbnormal(
					resolveObject(value(condition, "obj"), eventTarget, message), value(condition, "abnormal_state"));
				case "is_waypoint_index" -> waypointIndex == integer(condition, "index");
				case "is_last_waypoint" -> waypointIndex >= 0 && waypointIndex == waypointCount() - 1;
				case "is_tribe" -> matchesTribe(resolveObject(value(condition, "target"), eventTarget, message),
					value(condition, "tribe_name"));
				case "is_abnormal_state" -> matchesRetailAbnormal(eventAbnormalState, value(condition, "abnormal_state"));
				case "add_intvar" -> addIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "var_to_add"), integer(condition, "lower_bound"), integer(condition, "upper_bound"),
					Boolean.parseBoolean(value(condition, "be_true_only_when_hit_the_bound")));
				case "decrease_intvar" -> decreaseIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "lower_bound"), integer(condition, "upper_bound"),
					Boolean.parseBoolean(value(condition, "be_true_only_when_hit_the_bound")));
				case "is_user_level" -> resolveUser(value(condition, "user"), eventTarget, message) instanceof Player player
					&& matchesLevel(player.getLevel(), integer(condition, "level_min"), integer(condition, "level_max"));
				case "is_user_gender" -> resolveUser(value(condition, "user"), eventTarget, message) instanceof Player player
					&& matchesGender(player.getGender(), value(condition, "gender"));
				case "is_my_curent_target" -> {
					Creature object = resolveObject(value(condition, "who"), eventTarget, message);
					yield object != null && getOwner().isTargeting(object.getObjectId());
				}
				case "is_world_flag_var" -> RetailConditionSpawnEngine.testFlag(getPosition().getWorldMapInstance(),
					value(condition, "flagvar_indicator"), Boolean.parseBoolean(value(condition, "flag_expected")));
				case "sub_intvar" -> subIntVar(intVars, value(condition, "intvar_indicator"),
					integer(condition, "var_to_sub"), integer(condition, "lower_bound"),
					integer(condition, "upper_bound"),
					Boolean.parseBoolean(value(condition, "be_true_only_when_hit_the_bound")));
				default -> false;
			};
			if (LOCAL_STATE_CONDITIONS.contains(condition.type())) {
				persistLocalState(condition);
			}
			if (!matches) {
				return false;
			}
		}
		return true;
	}

	private Creature resolveObject(String indicator, Creature eventTarget, RetailMessage message) {
		return switch (indicator) {
			case "OBJI_SELF" -> getOwner();
			case "OBJI_CUR_TARGET" -> getOwner().getTarget() instanceof Creature creature ? creature : null;
			case "OBJI_EVENT_TARGET", "OBJI_ATTACKER", "OBJI_KILLER", "OBJI_SEEN", "OBJI_TALKER",
				"OBJI_FLEE_FROM" -> eventTarget;
			case "OBJI_CASTER" -> message != null && message.paramObject() != null ? message.paramObject() : eventTarget;
			case "OBJI_MESSAGE_SENDER" -> message == null ? null : message.sender();
			case "OBJI_MESSAGE_PARAM" -> message == null ? null : message.paramObject();
			case "OBJI_FRIEND" -> message == null ? null : message.paramObject();
			case "OBJI_PARTY_MEMBER" -> message == null ? null : message.sender();
			default -> null;
		};
	}

	private Creature resolveUser(String indicator, Creature eventTarget, RetailMessage message) {
		return switch (indicator) {
			case "USERI_CASTER" -> resolveObject("OBJI_CASTER", eventTarget, message);
			case "USERI_ATTACKER", "USERI_KILLER", "USERI_SEEN", "USERI_EVENT_TARGET", "USERI_TALKER" -> eventTarget;
			case "USERI_EVENT_MAKER" -> eventTarget != null ? eventTarget : getAggroList().getMostPlayerDamage();
			default -> null;
		};
	}

	private boolean matchesDistance(Creature object, float distance, boolean shorter) {
		if (getOwner().getWorldId() != object.getWorldId() || getOwner().getInstanceId() != object.getInstanceId()) {
			return false;
		}
		float dx = object.getX() - getOwner().getX();
		float dy = object.getY() - getOwner().getY();
		float dz = object.getZ() - getOwner().getZ();
		float range = distance + getOwner().getObjectTemplate().getBoundRadius().getCollision()
			+ object.getObjectTemplate().getBoundRadius().getCollision();
		return matchesRetailDistance(dx * dx + dy * dy + dz * dz, range, shorter);
	}

	static boolean matchesRetailDistance(float squaredDistance, float range, boolean shorter) {
		return shorter ? squaredDistance <= range * range + 0.01f : range * range + 0.01f < squaredDistance;
	}

	static boolean matchesAttackDamageFlag(AttackStatus status, String flag) {
		return status != null && flag.equals(AttackStatus.getBaseStatus(status).name());
	}

	private void execute(Operation action, Creature eventTarget, RetailMessage message) {
		switch (action.type()) {
			case "add_battle_timer" -> schedule(value(action, "btimer_indicator"), integer(action, "delay"),
				"on_battle_timer", false, eventTarget, message);
			case "set_idle_timer" -> schedule("IDLE", integer(action, "delay"), "on_idle_timer", true,
				eventTarget, message);
			case "spawn" -> spawn(action);
			case "spawn_on_target" -> spawnOnTarget(action, eventTarget, message);
			case "spawn_on_target_by_attacker_indicator" -> spawnOnAttacker(action);
			case "spawn_on_multi_target" -> spawnOnMultiTarget(action);
			case "despawn" -> despawn(action);
			case "despawn_by_nameid" -> despawnByNameId(action);
			case "switch_target_by_attacker_indicator" -> switchTarget(action);
			case "send_message" -> sendMessage(action, eventTarget, message);
			case "broadcast_message" -> broadcastMessage(action, eventTarget, message);
			case "broadcast_message_to_party" -> broadcastMessageToParty(action, eventTarget, message);
			case "return_to_spawn_point" -> ReturningEventHandler.onNotAtHome(this);
			case "reset_queued_actions" -> resetQueuedActions();
			case "say_to_all" -> sayToAll(action, false);
			case "shout_to_all" -> sayToAll(action, true);
			case "say_to_all_str" -> sayToAllString(action);
			case "say" -> say(action, eventTarget, message);
			case "change_world_scene_status" -> {
				var handler = getPosition().getWorldMapInstance().getInstanceHandler();
				StageType stageType = retailStageType(integer(action, "scenestatus"));
				if (handler.getStage() != stageType) {
					handler.onChangeStage(stageType);
				}
			}
			case "display_system_message" -> displaySystemMessage(action);
			case "send_system_msg" -> displaySystemMessage(action);
			case "send_system_msg_by_user_indicator" -> sendSystemMessageToUser(action, eventTarget, message);
			case "system_message_to_all_by_obj_indicator_param" ->
				systemMessageToAll(action, eventTarget, message);
			case "reset_hatepoints" -> resetHatepoints(action);
			case "control_door" -> controlDoor(action);
			case "set_condition_spawn_variable" -> RetailConditionSpawnEngine.setVariable(
				getPosition().getWorldMapInstance(), value(action, "string"), integer(action, "set"), integer(action, "modify"));
			case "set_condition_spawn_variable_to_world" -> RetailConditionSpawnEngine.setVariableToWorld(
				value(action, "worldid"), value(action, "string"), integer(action, "set"), integer(action, "modify"));
			case "give_item_by_user_indicator", "give_item_by_obj_indicator" -> giveItem(action, eventTarget, message);
			case "give_score" -> giveScore(action, eventTarget, message);
			case "give_exp" -> giveExperience(action, eventTarget, message);
			case "toggle_attackable_status_flag" -> getOwner().setNpcType(
				Boolean.parseBoolean(value(action, "attakable")) ? NpcType.ATTACKABLE : NpcType.NON_ATTACKABLE);
			case "play_cutscene_by_user_indicator" -> playCutscene(action, eventTarget);
			case "add_hate_point" -> addHate(action, eventTarget, message);
			case "switch_target" -> switchObjectTarget(action, eventTarget, message);
			case "attack_most_hating" -> changeTarget(getAggroList().getMostHated());
			case "change_direction" -> GameWorldBootstrapServices.world().updatePosition(getOwner(), getOwner().getX(),
				getOwner().getY(), getOwner().getZ(), (byte) integer(action, "direction"));
			case "random_move" -> randomMove(action);
			case "goto_waypoint" -> gotoWaypoint(action, false);
			case "goto_next_waypoint" -> gotoWaypoint(action, true);
			case "goto_alias" -> gotoAlias(action);
			case "flee_from" -> fleeFrom(action, eventTarget, message);
			case "switch_target_by_class_indicator" -> switchTargetByClass(action);
			case "close_dialog" -> {
				Creature target = resolveUser(value(action, "target"), eventTarget, message);
				if (target instanceof Player player) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
				}
			}
			case "teleport_target_alias" -> teleportTargetAlias(action, eventTarget, message);
			case "teleport_target" -> teleportTarget(action, eventTarget, message);
			case "open_directportal" -> RetailDirectPortalEngine.open(integer(action, "direct_portal_id"), getOwner());
			case "open_directportal_by_user" -> {
				if (resolveUser(value(action, "requestuser"), eventTarget, message) instanceof Player player) {
					RetailDirectPortalEngine.openByUser(integer(action, "direct_portal_id"), getOwner(), player);
				}
			}
			case "close_directportal" -> RetailDirectPortalEngine.close(integer(action, "direct_portal_id"));
			case "enable_area" -> RetailAreaEngine.setEnabled(getPosition().getWorldMapInstance(),
				value(action, "area_type"), value(action, "area_name"), integer(action, "op_code") == 1);
			case "on_off_windpath" -> RetailWindstreamEngine.setEnabled(getPosition().getWorldMapInstance(),
				integer(action, "groupid"), Boolean.parseBoolean(value(action, "onoff")));
			case "on_off_moving_collision" -> RetailDynamicAreaEngine.setEnabled(getPosition().getWorldMapInstance(),
				value(action, "type"), integer(action, "sunzoneid"), Boolean.parseBoolean(value(action, "onoff")));
			case "charge_limitedquest" -> LimitedQuestService.charge(integer(action, "quest_id"),
				Boolean.parseBoolean(value(action, "charge_max_count")));
		}
	}

	private void giveScore(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveUser(value(action, "target"), eventTarget, message);
		if (!(target instanceof Player player)) {
			return;
		}
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(getNpcId());
		var handler = getPosition().getWorldMapInstance().getInstanceHandler();
		if (supportsNpcScore(getOwner(), score)) {
			handler.onRetailNpcScore(player, getOwner(), score.scoreApplyType(), score.value());
		}
	}

	private void teleportTarget(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveObject(value(action, "target"), eventTarget, message);
		float x = decimal(action, "x");
		float y = decimal(action, "y");
		float z = decimal(action, "z");
		byte heading = MathUtil.convertDegreeToHeading(decimal(action, "dir"));
		boolean showFx = Boolean.parseBoolean(value(action, "showfx"));
		if (target instanceof Player player) {
			TeleportService2.teleportTo(player, getOwner().getWorldId(), getOwner().getInstanceId(), x, y, z, heading,
				showFx ? TeleportAnimation.BEAM_ANIMATION : TeleportAnimation.NO_ANIMATION);
		} else if (target == getOwner()) {
			teleportSelf(x, y, z, heading);
		}
	}

	private void teleportTargetAlias(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveObject(value(action, "target"), eventTarget, message);
		List<LocationAliasPoint> points = DataManager.RETAIL_AI_DATA
			.findLocationAlias(getOwner().getWorldId(), value(action, "alias"));
		if (target == null || points == null || points.isEmpty()) {
			return;
		}
		LocationAliasPoint point = Rnd.get(points);
		boolean showFx = Boolean.parseBoolean(value(action, "showfx"));
		byte heading = MathUtil.convertDegreeToHeading(point.direction());
		if (target instanceof Player player) {
			TeleportService2.teleportTo(player, getOwner().getWorldId(), getOwner().getInstanceId(),
				point.x(), point.y(), point.z(), heading,
				showFx ? TeleportAnimation.BEAM_ANIMATION : TeleportAnimation.NO_ANIMATION);
		} else if (target == getOwner()) {
			teleportSelf(point.x(), point.y(), point.z(), heading);
		}
	}

	private void teleportSelf(float x, float y, float z, byte heading) {
		getMoveController().abortMove();
		getKnownList().clear(true);
		GameWorldBootstrapServices.world().updatePosition(getOwner(), x, y, z, heading, false);
		getOwner().updateKnownlist();
	}

	static int retailHyperlinkId(String hyperlink) {
		if ("HACTION_FINISH_DIALOG".equals(hyperlink)) {
			return 1008;
		}
		if (hyperlink != null && hyperlink.startsWith("HACTION_SETPRO")) {
			try {
				int step = Integer.parseInt(hyperlink.substring("HACTION_SETPRO".length()));
				return step > 0 ? 9999 + step : -1;
			} catch (NumberFormatException ignored) {
			}
		}
		return -1;
	}

	static StageType retailStageType(int sceneStatus) {
		int id = sceneStatus & 0xffff;
		int type = sceneStatus >>> 16;
		for (StageType stageType : StageType.values()) {
			if (stageType.getId() == id && stageType.getType() == type) {
				return stageType;
			}
		}
		return null;
	}

	private synchronized void fleeFrom(Operation action, Creature eventTarget, RetailMessage message) {
		Creature source = resolveObject(value(action, "from"), eventTarget, message);
		if (source == null || fleeStopTask != null && !fleeStopTask.isDone()) {
			return;
		}
		boolean pushState = Boolean.parseBoolean(value(action, "push_state"));
		fleeReturnState = pushState ? getState() : AIState.IDLE;
		fleeReturnSubState = pushState ? getSubState() : AISubState.NONE;
		if (!pushState) {
			waypointMoving = false;
		}
		getOwner().getMoveController().abortMove();
		setStateIfNot(AIState.FEAR);
		setSubStateIfNot(AISubState.NONE);
		fleeMoveTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> moveAwayFrom(source), 0, 1000);
		fleeStopTask = GameThreadPoolServices.threadPoolManager().schedule(
			() -> stopFlee(source, pushState), Math.max(1L, integer(action, "seconds")) * 1000);
		actionTasks.add(fleeMoveTask);
		actionTasks.add(fleeStopTask);
	}

	private void moveAwayFrom(Creature source) {
		if (isAlreadyDead() || getState() != AIState.FEAR || source.getLifeStats().isAlreadyDead()) {
			return;
		}
		byte heading = PositionUtil.getMoveAwayHeading(source, getOwner());
		Point3D target = fleePoint(getOwner().getX(), getOwner().getY(), getOwner().getZ(),
			Math.max(1, getOwner().getGameStats().getMovementSpeedFloat()), heading, Rnd.get(-45, 45));
		byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
		Vector3f destination = GameWorldServices.geoService().getClosestCollision(getOwner(), target.getX(), target.getY(),
			target.getZ(), true, intentions);
		getOwner().getMoveController().resetMove();
		getOwner().getMoveController().moveToPoint(destination.getX(), destination.getY(), destination.getZ());
	}

	private synchronized void stopFlee(Creature source, boolean pushState) {
		if (fleeMoveTask == null) {
			return;
		}
		fleeMoveTask.cancel(false);
		fleeMoveTask = null;
		fleeStopTask = null;
		if (isAlreadyDead()) {
			return;
		}
		getOwner().getMoveController().abortMove();
		setStateIfNot(pushState ? fleeReturnState
			: getAggroList().getMostHated() == null ? AIState.IDLE : AIState.FIGHT);
		setSubStateIfNot(pushState ? fleeReturnSubState : AISubState.NONE);
		runEvent("on_stop_to_flee", null, source);
		if (getState() == AIState.WALKING && waypointMoving) {
			WalkManager.startWalkingToWaypoint(this, walker(getOwner()), waypointIndex);
		} else if (getState() != AIState.FEAR) {
			think();
		}
	}

	static Point3D fleePoint(float x, float y, float z, float distance, byte heading, int offsetDegrees) {
		double radians = Math.toRadians(MathUtil.convertHeadingToDegree(heading) + offsetDegrees);
		return new Point3D((float) (x + Math.cos(radians) * distance),
			(float) (y + Math.sin(radians) * distance), z);
	}

	private void gotoWaypoint(Operation action, boolean next) {
		WalkerTemplate walker = walker(getOwner());
		int count = hasWaypoints(walker) ? walker.getRouteSteps().size() : 0;
		int target = next ? nextWaypointIndex(waypointIndex, count) : integer(action, "waypoint");
		if (target < 0 || target >= count) {
			return;
		}
		if (!waypointMoving) {
			waypointReturnState = getState() == AIState.WALKING ? AIState.IDLE : getState();
			waypointReturnSubState = getState() == AIState.WALKING ? AISubState.NONE : getSubState();
			waypointWasWalking = getState() != AIState.WALKING && getOwner().isInState(CreatureState.WALKING);
		}
		if (WalkManager.startWalkingToWaypoint(this, walker, target)) {
			aliasMoving = false;
			switch (value(action, "move_type")) {
				case "MOVETYPE_WALK" -> getOwner().setState(CreatureState.WALKING);
				case "MOVETYPE_RUN" -> getOwner().unsetState(CreatureState.WALKING);
			}
			waypointIndex = target;
			waypointMoving = true;
			waypointMoveGeneration++;
		}
	}

	private void gotoAlias(Operation action) {
		List<LocationAliasPoint> points = DataManager.RETAIL_AI_DATA
			.findLocationAlias(getOwner().getWorldId(), value(action, "alias"));
		if (points == null || points.isEmpty()) {
			return;
		}
		if (!waypointMoving) {
			waypointReturnState = getState() == AIState.WALKING ? AIState.IDLE : getState();
			waypointReturnSubState = getState() == AIState.WALKING ? AISubState.NONE : getSubState();
			waypointWasWalking = getState() != AIState.WALKING && getOwner().isInState(CreatureState.WALKING);
		}
		LocationAliasPoint point = Rnd.get(points);
		getOwner().getMoveController().abortMove();
		setStateIfNot(AIState.WALKING);
		setSubStateIfNot(AISubState.NONE);
		switch (value(action, "move_type")) {
			case "MOVETYPE_WALK" -> getOwner().setState(CreatureState.WALKING);
			case "MOVETYPE_RUN" -> getOwner().unsetState(CreatureState.WALKING);
		}
		getOwner().getMoveController().moveToPoint(point.x(), point.y(), point.z());
		waypointIndex = -1;
		waypointMoving = true;
		aliasMoving = true;
		waypointMoveGeneration++;
	}

	static int nextWaypointIndex(int current, int count) {
		return count == 0 ? -1 : current < 0 ? 0 : (current + 1) % count;
	}

	private int waypointCount() {
		WalkerTemplate walker = walker(getOwner());
		return hasWaypoints(walker) ? walker.getRouteSteps().size() : 0;
	}

	private static WalkerTemplate walker(Npc npc) {
		return DataManager.WALKER_DATA == null || npc == null || npc.getSpawn() == null ? null
			: DataManager.WALKER_DATA.getWalkerTemplate(npc.getSpawn().getWalkerId());
	}

	private static boolean hasWaypoints(WalkerTemplate walker) {
		return walker != null && walker.getRouteSteps() != null && !walker.getRouteSteps().isEmpty();
	}

	private void randomMove(Operation action) {
		if (!WalkManager.startRandomWalking(this)) {
			return;
		}
		actionTasks.removeIf(Future::isDone);
		actionTasks.add(GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (!isAlreadyDead() && getSubState() == AISubState.WALK_RANDOM) {
				WalkManager.stopWalking(this);
				runEvent("on_stop_to_random_move", null, null);
			}
		}, integer(action, "time_to_move")));
	}

	private void addHate(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveObject(value(action, "target"), eventTarget, message);
		if (target != null) {
			getAggroList().addHate(target, integer(action, "point_to_add"));
		}
	}

	private void switchObjectTarget(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveObject(value(action, "target"), eventTarget, message);
		switchTarget(target, action);
	}

	private void playCutscene(Operation action, Creature eventTarget) {
		Player player = switch (value(action, "target")) {
			case "USERI_KILLER", "USERI_EVENT_MAKER", "USERI_EVENT_TARGET", "USERI_SEEN", "USERI_TALKER" ->
				eventTarget instanceof Player target ? target : null;
			case "USERI_MASTER" -> getOwner().getMaster() instanceof Player master ? master : null;
			default -> null;
		};
		if (player == null) {
			return;
		}
		int cutsceneId = integer(action, "cutscene_id");
		String teleportAlias = value(action, "teleport_alias");
		if (!teleportAlias.isBlank()) {
			pendingCutsceneTeleports.put(player.getObjectId(), new PendingCutsceneTeleport(cutsceneId, teleportAlias));
		}
		SM_PLAY_MOVIE movie = new SM_PLAY_MOVIE(0, 0, cutsceneId, 0, getObjectId());
		switch (value(action, "play_target_type")) {
			case "CUTSCENE_PLAY_TO_PARTY" -> {
				if (player.getCurrentGroup() == null) {
					PacketSendUtility.sendPacket(player, movie);
				} else {
					player.getCurrentGroup().sendPacket(movie);
				}
			}
			case "CUTSCENE_PLAY_TO_ALLIANCE" -> {
				if (player.getPlayerAlliance2() == null) {
					PacketSendUtility.sendPacket(player, movie);
				} else {
					player.getPlayerAlliance2().sendPacket(movie);
				}
			}
			default -> PacketSendUtility.sendPacket(player, movie);
		}
	}

	private void controlDoor(Operation action) {
		var door = getPosition().getWorldMapInstance().getDoors().get(integer(action, "id"));
		if (door != null) {
			boolean open = integer(action, "method") == 1;
			runtimeState().put("door." + integer(action, "id"), open);
			door.setOpen(open);
		}
	}

	private void resetHatepoints(Operation action) {
		getAggroList().resetHatepoints(value(action, "is_except_most_hating").equals("TRUE"),
			value(action, "volatile_hatepoint_only").equals("TRUE"));
	}

	private void broadcastMessage(Operation action, Creature eventTarget, RetailMessage sourceMessage) {
		Creature paramObject = resolveObject(value(action, "param_obj"), eventTarget, sourceMessage);
		if (paramObject == null) {
			return;
		}
		RetailMessage message = new RetailMessage(integer(action, "message_type"), integer(action, "param1"),
			integer(action, "param2"), getOwner(), paramObject);
		int range = integer(action, "range_as_meter");
		List<AI2> recipients = getPosition().getWorldMapInstance().getNpcs().stream()
			.filter(npc -> npc != getOwner() && npc.isSpawned() && MathUtil.isIn3dRange(getOwner(), npc, range))
			.map(Npc::getAi2)
			.filter(ai -> ai != null)
			.toList();
		queueMessage(recipients, message);
	}

	private void broadcastMessageToParty(Operation action, Creature eventTarget, RetailMessage sourceMessage) {
		Creature paramObject = resolveObject(value(action, "param_obj"), eventTarget, sourceMessage);
		if (paramObject == null) {
			return;
		}
		RetailMessage message = new RetailMessage(integer(action, "message_type"), integer(action, "param1"),
			integer(action, "param2"), getOwner(), paramObject);
		List<AI2> recipients = RetailNpcParty.members(getOwner()).stream()
			.filter(Npc::isSpawned)
			.map(Npc::getAi2)
			.filter(ai -> ai != null)
			.toList();
		queueMessage(recipients, message);
	}

	private void queueMessage(List<AI2> recipients, RetailMessage message) {
		GameThreadPoolServices.threadPoolManager().schedule(() -> recipients.forEach(ai ->
			ai.onRetailMessage(message.type(), message.param1(), message.param2(), message.sender(), message.paramObject())), 1);
	}

	private void sendMessage(Operation action, Creature eventTarget, RetailMessage sourceMessage) {
		Creature target = resolveObject(value(action, "target"), eventTarget, sourceMessage);
		Creature paramObject = resolveObject(value(action, "param_obj"), eventTarget, sourceMessage);
		if (target != getOwner() || paramObject == null) {
			return;
		}
		RetailMessage message = new RetailMessage(integer(action, "message_type"), integer(action, "param1"),
			integer(action, "param2"), getOwner(), paramObject);
		GameThreadPoolServices.threadPoolManager().schedule(
			() -> onRetailMessage(message.type(), message.param1(), message.param2(), message.sender(), message.paramObject()), 1);
	}

	private void sayToAll(Operation action, boolean shout) {
		Integer stringId = DataManager.RETAIL_AI_DATA.findStringId(value(action, "string_id"));
		if (stringId == null) {
			return;
		}
		SM_SYSTEM_MESSAGE message = new SM_SYSTEM_MESSAGE(shout, stringId, getObjectId(), 1);
		int range = getOwner().getObjectTemplate().getMinimumShoutRange();
		getKnownList().doOnAllPlayers(player -> {
			if (!player.getLifeStats().isAlreadyDead() && player.isOnline() && MathUtil.isIn3dRange(getOwner(), player, range)) {
				PacketSendUtility.sendPacket(player, message);
			}
		});
	}

	private void say(Operation action, Creature eventTarget, RetailMessage message) {
		Creature target = resolveUser(value(action, "user"), eventTarget, message);
		Integer stringId = DataManager.RETAIL_AI_DATA.findStringId(value(action, "string_id"));
		if (target instanceof Player player && stringId != null) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, stringId, getObjectId(), 1));
		}
	}

	private void giveItem(Operation action, Creature eventTarget, RetailMessage message) {
		Creature receiver = action.type().equals("give_item_by_obj_indicator")
			? resolveObject(value(action, "receiver"), eventTarget, message)
			: resolveUser(value(action, "receiver"), eventTarget, message);
		var item = DataManager.ITEM_DATA.getItemTemplate(value(action, "item_id"));
		if (receiver instanceof Player player && item != null) {
			ItemService.addItem(player, item.getTemplateId(), Rnd.get(integer(action, "min"), integer(action, "max")));
		}
	}

	private void giveExperience(Operation action, Creature eventTarget, RetailMessage message) {
		if (resolveUser(value(action, "target"), eventTarget, message) instanceof Player player) {
			player.getCommonData().addExp(longInteger(action, "exp"), null);
		}
	}

	private void sayToAllString(Operation action) {
		SM_MESSAGE message = new SM_MESSAGE(getObjectId(), getOwner().getName(), value(action, "string"), ChatType.NORMAL);
		int range = getOwner().getObjectTemplate().getMinimumShoutRange();
		getKnownList().doOnAllPlayers(player -> {
			if (!player.getLifeStats().isAlreadyDead() && player.isOnline() && MathUtil.isIn3dRange(getOwner(), player, range)) {
				PacketSendUtility.sendPacket(player, message);
			}
		});
	}

	private void displaySystemMessage(Operation action) {
		Integer stringId = DataManager.RETAIL_AI_DATA.findStringId(value(action, "string_id"));
		String areaName = value(action, "area_name");
		Area area = areaName.isBlank() ? null : DataManager.RETAIL_AI_DATA.findArea(getOwner().getWorldId(), areaName);
		if (stringId == null || !areaName.isBlank() && area == null) {
			return;
		}
		List<String> params = new ArrayList<>(List.of(value(action, "string_param1"), value(action, "string_param2"),
			value(action, "string_param3")));
		while (!params.isEmpty() && params.getLast().isBlank()) {
			params.removeLast();
		}
		SM_SYSTEM_MESSAGE message = new SM_SYSTEM_MESSAGE(stringId, params.toArray());
		getPosition().getWorldMapInstance().doOnAllPlayers(player -> {
			if (player.isOnline() && (area == null || area.isInside3D(player.getX(), player.getY(), player.getZ()))) {
				PacketSendUtility.sendPacket(player, message);
			}
		});
	}

	private void sendSystemMessageToUser(Operation action, Creature eventTarget, RetailMessage message) {
		Integer stringId = DataManager.RETAIL_AI_DATA.findStringId(value(action, "string_id"));
		if (resolveUser(value(action, "user"), eventTarget, message) instanceof Player player && stringId != null) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(stringId));
		}
	}

	private void systemMessageToAll(Operation action, Creature eventTarget, RetailMessage sourceMessage) {
		Integer stringId = DataManager.RETAIL_AI_DATA.findStringId(value(action, "string_id"));
		Creature param = resolveObject(value(action, "param"), eventTarget, sourceMessage);
		if (stringId == null || param == null) {
			return;
		}
		SM_SYSTEM_MESSAGE message = new SM_SYSTEM_MESSAGE(stringId, param);
		getPosition().getWorldMapInstance().doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, message);
			}
		});
	}

	private int useSkill(Operation action, Creature eventTarget, RetailMessage message) {
		NpcSkillEntry skill = skill(action);
		if (skill == null || !skill.hasUsesLeft()) {
			return -1;
		}
		if (action.type().equals("activate_skillarea")) {
			return activateSkillArea(action, skill);
		}
		VisibleObject previousTarget = getOwner().getTarget();
		VisibleObject target = action.type().equals("use_skill_by_attacker_indicator") ? selectAttacker(action)
			: action.type().equals("attack_most_hating") ? getAggroList().getMostHated() : switch (value(action, "target")) {
				case "OBJI_SELF" -> getOwner();
				case "OBJI_EVENT_TARGET", "OBJI_CASTER", "OBJI_ATTACKER", "OBJI_FLEE_FROM", "OBJI_KILLER",
					"OBJI_SEEN", "OBJI_TALKER" -> eventTarget;
				case "OBJI_MESSAGE_SENDER" -> message == null ? null : message.sender();
				case "OBJI_MESSAGE_PARAM", "OBJI_FRIEND" -> message == null ? null : message.paramObject();
				default -> previousTarget;
			};
		if (target == null) {
			return -1;
		}
		if (action.type().equals("attack_most_hating")) {
			TargetEventHandler.clearTargetLostState(this);
			getOwner().setTarget(target);
		} else {
			getOwner().setTarget(target);
		}
		int duration = -1;
		int skillLevel = effectiveSkillLevel(skill.getSkillLevel(), integer(action, "skill_level"));
		if (getOwner().getController().useSkill(skill.getSkillId(), skillLevel)) {
			skill.setLastTimeUsed();
			duration = DataManager.SKILL_DATA.getSkillTemplate(skill.getSkillId()).getDuration();
		}
		if (!action.type().equals("attack_most_hating")) {
			getOwner().setTarget(previousTarget);
		}
		return duration;
	}

	private int activateSkillArea(Operation action, NpcSkillEntry skillEntry) {
		List<Area> areas = DataManager.RETAIL_AI_DATA.findSkillAreas(
			getOwner().getWorldId(), integer(action, "areaid"));
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillEntry.getSkillId());
		if (areas.isEmpty() || template == null || getOwner().isSkillDisabled(template)) {
			return -1;
		}
		List<Creature> targets = new ArrayList<>();
		for (var objects = getPosition().getWorldMapInstance().objectIterator(); objects.hasNext();) {
			VisibleObject object = objects.next();
			if (object instanceof Creature creature && areas.stream()
					.anyMatch(area -> area.isInside3D(creature.getX(), creature.getY(), creature.getZ()))) {
				targets.add(creature);
			}
		}
		int skillLevel = effectiveSkillLevel(skillEntry.getSkillLevel(), integer(action, "skill_level"));
		Skill skill = GameEngineServices.skillEngine().getSkill(
			getOwner(), skillEntry.getSkillId(), skillLevel, getOwner());
		getOwner().getGameStats().renewLastSkillTime();
		if (skill != null && skill.useSkillOnPreselectedTargets(targets, value(action, "broadcast_type").equals("AREA"))) {
			skillEntry.setLastTimeUsed();
			return 0;
		}
		return -1;
	}

	static int effectiveSkillLevel(int configuredLevel, int requestedLevel) {
		return requestedLevel == 0 ? configuredLevel : requestedLevel;
	}

	private Creature selectAttacker(Operation action) {
		boolean restricted = Boolean.parseBoolean(value(action, "restricted_range"));
		List<AggroInfo> attackers = getAggroList().getList().stream()
			.filter(info -> info.getHate() > 0 && info.getAttacker() instanceof Creature creature
				&& !creature.getLifeStats().isAlreadyDead()
				&& (!restricted || MathUtil.isIn3dRange(getOwner(), creature, 80)))
			.toList();
		if (attackers.isEmpty()) {
			return null;
		}
		String indicator = value(action, "target");
		AggroInfo selected = switch (indicator) {
			case "ATTACKERI_RANDOM_ONE" -> Rnd.get(attackers);
			case "ATTACKERI_RANDOM_ONE_EXCEPT_CURRENT_TARGET" -> randomExceptCurrent(attackers);
			case "ATTACKERI_SECOND_HATING" -> hating(attackers, 1);
			case "ATTACKERI_THIRD_HATING" -> hating(attackers, 2);
			case "ATTACKERI_HAS_LOWEST_HP" -> byHp(attackers, true);
			case "ATTACKERI_HAS_MOST_HP" -> byHp(attackers, false);
			default -> null;
		};
		return selected == null ? null : (Creature) selected.getAttacker();
	}

	private AggroInfo randomExceptCurrent(List<AggroInfo> attackers) {
		List<AggroInfo> candidates = attackers.stream().filter(info -> info.getAttacker() != getOwner().getTarget()).toList();
		return candidates.isEmpty() ? null : Rnd.get(candidates);
	}

	private static AggroInfo hating(List<AggroInfo> attackers, int index) {
		return attackers.stream().sorted(Comparator.comparingInt(AggroInfo::getHate).reversed()).skip(index).findFirst()
			.orElse(null);
	}

	private AggroInfo byHp(List<AggroInfo> attackers, boolean lowest) {
		Comparator<AggroInfo> hp = Comparator.comparingInt(info -> ((Creature) info.getAttacker()).getLifeStats().getHpPercentage());
		return attackers.stream().filter(info -> info.getAttacker() instanceof Player && info.getAttacker() != getOwner().getTarget())
			.min(lowest ? hp : hp.reversed()).orElse(null);
	}

	private void switchTarget(Operation action) {
		switchTarget(selectAttacker(action), action);
	}

	private void switchTargetByClass(Operation action) {
		boolean restricted = Boolean.parseBoolean(value(action, "restricted_range"));
		List<Player> candidates = getAggroList().getList().stream()
			.filter(info -> info.getHate() > 0 && info.getAttacker() instanceof Player player
				&& !player.getLifeStats().isAlreadyDead()
				&& matchesUserClass(player.getPlayerClass(), value(action, "target_class"))
				&& (!restricted || MathUtil.isIn3dRange(getOwner(), player, 80)))
			.map(info -> (Player) info.getAttacker()).toList();
		switchTarget(candidates.isEmpty() ? null : Rnd.get(candidates), action);
	}

	private void switchTarget(Creature target, Operation action) {
		if (target == null) {
			return;
		}
		int maximumHate = getAggroList().getList().stream().mapToInt(AggroInfo::getHate).max().orElse(0);
		AggroInfo targetInfo = getAggroList().getAggroInfo(target);
		targetInfo.addHate(switchHateAddition(maximumHate, targetInfo.getHate(), integer(action, "percent_to_add"),
			integer(action, "points_to_add")));
		changeTarget(target);
	}

	private void changeTarget(Creature target) {
		if (target == null) {
			return;
		}
		if (isInState(AIState.FIGHT)) {
			TargetEventHandler.onTargetChange(this, target);
		} else {
			getOwner().setTarget(target);
		}
	}

	static int switchHateAddition(int maximumHate, int targetHate, int percent, int points) {
		long addition = maximumHate + maximumHate * (long) percent / 100 + points - targetHate;
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, addition));
	}

	static boolean matchesRace(Race race, String retailRace) {
		return switch (retailRace.toLowerCase()) {
			case "pc" -> race.isPlayerRace();
			case "pc_light" -> race == Race.ELYOS;
			case "pc_dark" -> race == Race.ASMODIANS;
			default -> race.name().equalsIgnoreCase(retailRace);
		};
	}

	static boolean matchesUserClass(PlayerClass playerClass, String retailClass) {
		int id = Byte.toUnsignedInt(playerClass.getClassId());
		return switch (retailClass) {
			case "CLASSI_WARRIOR" -> id == 0;
			case "CLASSI_FIGHTER" -> id == 1;
			case "CLASSI_KNIGHT" -> id == 2;
			case "CLASSI_SCOUT" -> id == 3;
			case "CLASSI_ASSASSIN" -> id == 4;
			case "CLASSI_RANGER" -> id == 5;
			case "CLASSI_MAGE" -> id == 6;
			case "CLASSI_WIZARD" -> id == 7;
			case "CLASSI_ELEMENTALIST" -> id == 8;
			case "CLASSI_CLERIC" -> id == 9;
			case "CLASSI_PRIEST" -> id == 10;
			case "CLASSI_CHANTER" -> id == 11;
			case "CLASSI_ENGINEER" -> id == 12;
			case "CLASSI_RIDER" -> id == 13;
			case "CLASSI_GUNNER" -> id == 14;
			case "CLASSI_ARTIST" -> id == 15;
			case "CLASSI_BARD" -> id == 16;
			case "CLASSI_WARRIOR_GROUP" -> id < 3;
			case "CLASSI_SCOUT_GROUP" -> id >= 3 && id < 6;
			case "CLASSI_MAGE_GROUP" -> id >= 6 && id < 9;
			case "CLASSI_CLERIC_GROUP" -> id >= 9 && id < 12;
			case "CLASSI_ENGINEER_GROUP" -> id >= 12 && id < 15;
			case "CLASSI_ARTIST_GROUP" -> id >= 15 && id < 17;
			case "CLASSI_MELEE_GROUP" -> id < 6 || id >= 12 && id < 15;
			case "CLASSI_CASTER_GROUP" -> id >= 6 && id < 12 || id >= 15 && id < 17;
			case "CLASSI_TANKER_GROUP" -> id < 3 || id == 13;
			case "CLASSI_DEALER_GROUP" -> id >= 3 && id < 9 || id == 14;
			case "CLASSI_HEALER_GROUP" -> id >= 9 && id < 12 || id == 16;
			case "CLASSI_JUNIOR_GROUP" -> id < 17 && id % 3 == 0;
			case "CLASSI_SENIOR_GROUP" -> id < 17 && id % 3 != 0;
			default -> false;
		};
	}

	private static boolean isRetailPlayerClass(String retailClass) {
		return switch (retailClass) {
			case "CLASSI_WARRIOR", "CLASSI_FIGHTER", "CLASSI_KNIGHT", "CLASSI_SCOUT", "CLASSI_ASSASSIN",
				"CLASSI_RANGER", "CLASSI_MAGE", "CLASSI_WIZARD", "CLASSI_ELEMENTALIST", "CLASSI_PRIEST",
				"CLASSI_CLERIC", "CLASSI_CHANTER", "CLASSI_ENGINEER", "CLASSI_RIDER", "CLASSI_GUNNER",
				"CLASSI_ARTIST", "CLASSI_BARD", "CLASSI_WARRIOR_GROUP", "CLASSI_SCOUT_GROUP", "CLASSI_MAGE_GROUP",
				"CLASSI_CLERIC_GROUP", "CLASSI_ENGINEER_GROUP", "CLASSI_ARTIST_GROUP", "CLASSI_MELEE_GROUP",
				"CLASSI_CASTER_GROUP", "CLASSI_TANKER_GROUP", "CLASSI_DEALER_GROUP", "CLASSI_HEALER_GROUP",
				"CLASSI_JUNIOR_GROUP", "CLASSI_SENIOR_GROUP", "CLASSI_NONE" -> true;
			default -> false;
		};
	}

	private static boolean supportsUser(String event, String user) {
		if (Set.of("on_battle_timer", "on_idle_timer").contains(event)
			&& Set.of("USERI_EVENT_TARGET", "USERI_ATTACKER").contains(user)) {
			return true;
		}
		return switch (event) {
			case "on_attacked" -> user.equals("USERI_ATTACKER");
			case "on_spelled" -> Set.of("USERI_ATTACKER", "USERI_CASTER").contains(user);
			case "on_see_spell", "on_casted" -> user.equals("USERI_CASTER");
			case "on_see_user", "on_see_user_move" -> user.equals("USERI_SEEN");
			case "on_user_enter_sensory_area", "on_user_leave_sensory_area" -> user.equals("USERI_EVENT_MAKER");
			case "on_enter_attack_state" -> Set.of("USERI_EVENT_TARGET", "USERI_ATTACKER").contains(user);
			case "on_healed_by_user" -> user.equals("USERI_EVENT_TARGET");
			case "on_killed_by_user", "on_see_friend_killed_by_user", "on_sense_friend_killed_by_user" -> user.equals("USERI_KILLER");
			case "on_killed_by_npc" -> user.equals("USERI_MASTER");
			case "on_talked_by_user", "on_hyperlink_clicked", "on_gauge_begin", "on_gauge_stop", "on_gauge_end" ->
				user.equals("USERI_TALKER");
			default -> false;
		};
	}

	static boolean increaseIntVar(Map<String, Integer> variables, String name, int lower, int upper,
			boolean trueOnlyAtUpperBound) {
		int current = variables.getOrDefault(name, 0);
		if (current < lower || current >= upper) {
			return false;
		}
		int next = current + 1;
		variables.put(name, next);
		return !trueOnlyAtUpperBound || next == upper;
	}

	static boolean decreaseIntVar(Map<String, Integer> variables, String name, int lower, int upper,
			boolean trueOnlyAtLowerBound) {
		int current = variables.getOrDefault(name, 0);
		if (current <= lower || current > upper) {
			return false;
		}
		int next = current - 1;
		variables.put(name, next);
		return !trueOnlyAtLowerBound || next == lower;
	}

	static boolean addIntVar(Map<String, Integer> variables, String name, int amount, int lower, int upper,
			boolean trueOnlyAtUpperBound) {
		int current = variables.getOrDefault(name, 0);
		if (current < lower || current >= upper) {
			return false;
		}
		int next = (int) Math.min((long) current + amount, upper);
		variables.put(name, next);
		return !trueOnlyAtUpperBound || next == upper;
	}

	static boolean matchesTribe(Creature creature, String tribeName) {
		if (creature == null) {
			return false;
		}
		try {
			return creature.getTribe() == TribeClass.valueOf(tribeName.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	static boolean matchesLevel(int level, int minimum, int maximum) {
		return minimum == 0 ? level <= maximum : maximum == 0 ? minimum <= level : minimum <= level && level <= maximum;
	}

	static boolean matchesGender(Gender gender, String retailGender) {
		return retailGender.equals("GENDERI_" + gender.name());
	}

	static boolean subIntVar(Map<String, Integer> variables, String name, int amount, int lower, int upper,
			boolean trueOnlyAtLowerBound) {
		int current = variables.getOrDefault(name, 0);
		if (current <= lower || current > upper) {
			return false;
		}
		int next = (int) Math.max((long) current - amount, lower);
		variables.put(name, next);
		return !trueOnlyAtLowerBound || next == lower;
	}

	static boolean setIntVar(Map<String, Integer> variables, String name, int value, int comparand, boolean larger) {
		int current = variables.getOrDefault(name, 0);
		if (larger ? current <= comparand : current >= comparand) {
			return false;
		}
		variables.put(name, value);
		return current != value;
	}

	static boolean matchesNpcState(AIState state, AISubState subState, String retailState) {
		return matchesNpcState(state, subState, retailState, false);
	}

	static boolean matchesNpcState(AIState state, AISubState subState, String retailState, boolean wakingUp) {
		return matchesNpcState(state, subState, retailState, wakingUp, false);
	}

	static boolean matchesNpcState(AIState state, AISubState subState, String retailState, boolean wakingUp,
			boolean goingToPoint) {
		return switch (retailState) {
			case "NPC_STATE_ATTACK" -> state == AIState.FIGHT;
			case "NPC_STATE_IDLE" -> state == AIState.IDLE;
			case "NPC_STATE_GOTO_WAYPOINT" -> state == AIState.WALKING && subState == AISubState.WALK_PATH;
			case "NPC_STATE_RANDOM_MOVE" -> state == AIState.WALKING && subState == AISubState.WALK_RANDOM;
			case "NPC_STATE_FLEE" -> state == AIState.FEAR;
			case "NPC_STATE_USE_SKILL" -> subState == AISubState.CAST;
			case "NPC_STATE_WAKE_UP" -> wakingUp;
			case "NPC_STATE_GOTO_POINT" -> goingToPoint;
			default -> false;
		};
	}

	private static boolean matchesRetailAbnormal(Creature creature, String retailState) {
		if (creature == null) {
			return false;
		}
		if (retailState.equals("ABNSTATEI_SANCTUARY")) {
			return creature.getEffectController().getAbnormalEffects().stream().anyMatch(effect -> effect.isSanctuaryEffect());
		}
		if (retailState.equals("ABNSTATEI_INVULNERABLE_WING")) {
			return creature instanceof Player player && player.isInvulnerableWing();
		}
		Integer mask = retailAbnormalMask(retailState);
		return mask != null && mask != 0 && (creature.getEffectController().getAbnormals() & mask) != 0;
	}

	static boolean matchesRetailAbnormal(int abnormalState, String retailState) {
		Integer mask = retailAbnormalMask(retailState);
		return mask != null && mask != 0 && (abnormalState & mask) != 0;
	}

	static Integer retailAbnormalMask(String retailState) {
		return switch (retailState) {
			case "ABNSTATEI_NONE" -> 0;
			case "ABNSTATEI_POISON" -> 0x00000001;
			case "ABNSTATEI_BLEED" -> 0x00000002;
			case "ABNSTATEI_PARALYZE" -> 0x00000004;
			case "ABNSTATEI_SLEEP" -> 0x00000008;
			case "ABNSTATEI_ROOT" -> 0x00000010;
			case "ABNSTATEI_BLIND" -> 0x00000020;
			case "ABNSTATEI_CHARM" -> 0x00000040;
			case "ABNSTATEI_DISEASE" -> 0x00000080;
			case "ABNSTATEI_SILENCE" -> 0x00000100;
			case "ABNSTATEI_FEAR" -> 0x00000200;
			case "ABNSTATEI_CURSE" -> 0x00000400;
			case "ABNSTATEI_CONFUSE" -> 0x00000800;
			case "ABNSTATEI_STUN" -> 0x00001000;
			case "ABNSTATEI_PETRIFICATION" -> 0x00002000;
			case "ABNSTATEI_STUMBLE" -> 0x00004000;
			case "ABNSTATEI_STAGGER" -> 0x00008000;
			case "ABNSTATEI_OPEN_AERIAL" -> 0x00010000;
			case "ABNSTATEI_SNARE" -> 0x00020000;
			case "ABNSTATEI_SLOW" -> 0x00040000;
			case "ABNSTATEI_SPIN" -> 0x00080000;
			case "ABNSTATEI_BIND" -> 0x00100000;
			case "ABNSTATEI_DEFORM" -> 0x00200000;
			case "ABNSTATEI_PULLED" -> 0x00400000;
			case "ABNSTATEI_INVISIBLE" -> 0x20000000;
			case "ABNSTATEI_SANCTUARY" -> 0x80000000;
			case "ABNSTATEI_CANNOT_MOVE_GROUP" -> 0x0749f05c;
			case "ABNSTATEI_CANNOT_ACT_GROUP" -> 0x0649f04c;
			case "ABNSTATEI_NO_PHYSICAL_SKILL_GROUP" -> 0x14100008;
			case "ABNSTATEI_NO_MAGICAL_SKILL_GROUP" -> 0x0c000108;
			case "ABNSTATEI_STUN_LIKE_GROUP" -> 0x0009d040;
			case "ABNSTATEI_PHYSICAL_GROUP" -> 0x101620b7;
			case "ABNSTATEI_MENTAL_GROUP" -> 0x0c800f48;
			case "ABNSTATEI_INVULNERABLE_WING" -> 0x0000001e;
			default -> null;
		};
	}

	private void schedule(String timer, int delay, String event, boolean zeroCancels, Creature eventTarget,
			RetailMessage message) {
		if (runtimeStatePrefix != null) {
			schedulePersistent(timer, delay, event, zeroCancels, eventTarget, message);
			return;
		}
		Future<?> previous = timers.get(timer);
		if (previous != null && !previous.isDone()) {
			previous.cancel(false);
		}
		if (delay > 0 || !zeroCancels) {
			timers.put(timer, GameThreadPoolServices.threadPoolManager()
				.schedule(() -> runEvent(event, timer, eventTarget, message), delay));
		}
	}

	private boolean restorePatternState() {
		String stableKey = getOwner().getSpawn() == null ? null : getOwner().getSpawn().getStableKey();
		if (stableKey == null || stableKey.isBlank()) {
			return false;
		}
		runtimeStatePrefix = STATE_PREFIX + stableKey + '.';
		InstanceRuntimeState state = runtimeState();
		restoreLocalState(state, runtimeStatePrefix, flags, intVars);
		return state.getBoolean(runtimeStatePrefix + "initialized", false);
	}

	private void indexSpawnActions() {
		spawnActionKeys.clear();
		if (pattern == null) {
			return;
		}
		for (String event : pattern.events().keySet().stream().sorted().toList()) {
			List<Rule> rules = pattern.event(event);
			for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
				List<Operation> actions = rules.get(ruleIndex).actions();
				for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
					Operation action = actions.get(actionIndex);
					if (Set.of("spawn", "spawn_on_target", "spawn_on_target_by_attacker_indicator", "spawn_on_multi_target")
						.contains(action.type())) {
						spawnActionKeys.put(action, event + '.' + ruleIndex + '.' + actionIndex);
					}
				}
			}
		}
	}

	private InstanceRuntimeState runtimeState() {
		return getPosition().getWorldMapInstance().getRuntimeState();
	}

	private void persistLocalState(Operation condition) {
		if (runtimeStatePrefix == null) {
			return;
		}
		String type = condition.type();
		if (type.equals("set_flag_var") || type.equals("unset_flag_var")) {
			persistFlag(runtimeState(), runtimeStatePrefix, value(condition, "flagvar_indicator"), flags);
			return;
		}
		String name = value(condition, "intvar_indicator");
		persistIntVar(runtimeState(), runtimeStatePrefix, name, intVars);
	}

	static void restoreLocalState(InstanceRuntimeState state, String prefix, Set<String> flags,
			Map<String, Integer> intVars) {
		state.snapshot(prefix + "flag.").forEach((key, value) ->
			flags.add(key.substring((prefix + "flag.").length())));
		state.snapshot(prefix + "int.").forEach((key, value) ->
			intVars.put(key.substring((prefix + "int.").length()), Integer.parseInt(value)));
	}

	static void persistFlag(InstanceRuntimeState state, String prefix, String name, Set<String> flags) {
		if (flags.contains(name)) {
			state.put(prefix + "flag." + name, true);
		} else {
			state.remove(prefix + "flag." + name);
		}
	}

	static void persistIntVar(InstanceRuntimeState state, String prefix, String name,
			Map<String, Integer> intVars) {
		Integer current = intVars.get(name);
		if (current == null) {
			state.remove(prefix + "int." + name);
		} else {
			state.put(prefix + "int." + name, current);
		}
	}

	private void schedulePersistent(String timer, int delay, String event, boolean zeroCancels, Creature eventTarget,
			RetailMessage message) {
		String deadlineKey = runtimeStatePrefix + "timer." + timer;
		if (delay == 0 && zeroCancels) {
			persistentTimers.remove(timer);
			runtimeState().remove(deadlineKey + ".event");
			InstanceDeadlineScheduler.cancel(getPosition().getWorldMapInstance(), deadlineKey);
			return;
		}
		long deadline = System.currentTimeMillis() + delay;
		persistentTimers.add(timer);
		runtimeState().put(deadlineKey + ".event", event);
		InstanceDeadlineScheduler.schedule(getPosition().getWorldMapInstance(), deadlineKey, deadline,
			() -> runPersistentTimer(timer, event, eventTarget, message));
	}

	private void restoreTimers() {
		String prefix = runtimeStatePrefix + "timer.";
		for (Map.Entry<String, String> entry : runtimeState().snapshot(prefix).entrySet()) {
			if (!entry.getKey().endsWith(".event")) {
				continue;
			}
			String timer = entry.getKey().substring(prefix.length(), entry.getKey().length() - ".event".length());
			String deadlineKey = runtimeStatePrefix + "timer." + timer;
			long deadline = InstanceDeadlineScheduler.deadline(getPosition().getWorldMapInstance(), deadlineKey);
			if (deadline <= 0 || InstanceDeadlineScheduler.isCompleted(getPosition().getWorldMapInstance(), deadlineKey)) {
				runtimeState().remove(entry.getKey());
				continue;
			}
			persistentTimers.add(timer);
			InstanceDeadlineScheduler.schedule(getPosition().getWorldMapInstance(), deadlineKey, deadline,
				() -> runPersistentTimer(timer, entry.getValue(), null, null));
		}
	}

	private void runPersistentTimer(String timer, String event, Creature eventTarget, RetailMessage message) {
		persistentTimers.remove(timer);
		runtimeState().remove(runtimeStatePrefix + "timer." + timer + ".event");
		runEvent(event, timer, eventTarget, message);
	}

	private void spawn(Operation action) {
		Integer npcId = DataManager.RETAIL_AI_DATA.findNpcId(value(action, "npc_nameid"));
		if (npcId == null) {
			return;
		}
		String pathname = value(action, "pathname");
		String walkerId = pathname.isBlank() ? null : retailWalkerId(getOwner().getWorldId(), pathname);
		WalkerTemplate walker = walkerId == null || DataManager.WALKER_DATA == null ? null
			: DataManager.WALKER_DATA.getWalkerTemplate(walkerId);
		Point3D point = resolveSpawnPoint(action, getPosition().getX(), getPosition().getY(), getPosition().getZ(), walker);
		if (point != null) {
			spawnAt(action, npcId, point.getX(), point.getY(), point.getZ(), (byte) integer(action, "dir"), null,
				walkerId);
		}
	}

	static String retailWalkerId(int worldId, String pathname) {
		return "retail:" + worldId + ':' + pathname.toLowerCase(Locale.ROOT);
	}

	static Point3D resolveSpawnPoint(Operation action, float ownerX, float ownerY, float ownerZ,
			WalkerTemplate walker) {
		if (value(action, "spawn_location_type").equals("SPAWN_LOCATION_WAY_POINT_START")) {
			if (walker == null || walker.getRouteSteps() == null || walker.getRouteSteps().isEmpty()) {
				return null;
			}
			RouteStep first = walker.getRouteSteps().get(0);
			return new Point3D(first.getX(), first.getY(), first.getZ());
		}
		float baseX = value(action, "spawn_location_type").equals("SPAWN_LOCATION_ABSOLUTE") ? 0 : ownerX;
		float baseY = value(action, "spawn_location_type").equals("SPAWN_LOCATION_ABSOLUTE") ? 0 : ownerY;
		float baseZ = value(action, "spawn_location_type").equals("SPAWN_LOCATION_ABSOLUTE") ? 0 : ownerZ;
		return new Point3D(baseX + decimal(action, "x"), baseY + decimal(action, "y"),
			baseZ + decimal(action, "z"));
	}

	private void spawnOnTarget(Operation action, Creature eventTarget, RetailMessage message) {
		Integer npcId = DataManager.RETAIL_AI_DATA.findNpcId(value(action, "npc_nameid"));
		Creature target = resolveObject(value(action, "target_obj"), eventTarget, message);
		if (npcId == null || target == null
			|| !MathUtil.isIn3dRange(getOwner(), target, integer(action, "valid_distance"))) {
			return;
		}
		Creature attackTarget = value(action, "attack_target_after_spawn").equals("TRUE") ? target : null;
		spawnAt(action, npcId, target.getX(), target.getY(), target.getZ(), target.getHeading(), attackTarget, null);
	}

	private void spawnOnAttacker(Operation action) {
		Integer npcId = DataManager.RETAIL_AI_DATA.findNpcId(value(action, "npc_nameid"));
		Creature target = selectAttacker(action);
		if (npcId == null || target == null
			|| !MathUtil.isIn3dRange(getOwner(), target, integer(action, "valid_distance"))) {
			return;
		}
		Creature attackTarget = value(action, "attack_target_after_spawn").equals("TRUE") ? target : null;
		spawnAt(action, npcId, target.getX(), target.getY(), target.getZ(), target.getHeading(), attackTarget, null);
	}

	private void spawnOnMultiTarget(Operation action) {
		Integer npcId = DataManager.RETAIL_AI_DATA.findNpcId(value(action, "npc_nameid"));
		if (npcId == null) {
			return;
		}
		List<AggroInfo> attackers = getAggroList().getList().stream()
			.filter(info -> info.getHate() > 0 && info.getAttacker() instanceof Creature creature
				&& creature.isSpawned() && !creature.getLifeStats().isAlreadyDead()
				&& MathUtil.isIn3dRange(getOwner(), creature, decimal(action, "valid_distance")))
			.toList();
		for (AggroInfo info : selectMultiTargets(attackers, Comparator.comparingInt(AggroInfo::getHate),
			value(action, "order_in_attacker_list"), integer(action, "total_set_to_spawn"))) {
			Creature target = (Creature) info.getAttacker();
			Creature attackTarget = value(action, "attack_target_after_spawn").equals("TRUE") ? target : null;
			spawnAt(action, npcId, target.getX(), target.getY(), target.getZ(), target.getHeading(), attackTarget, null);
		}
	}

	static <T> List<T> selectMultiTargets(List<T> candidates, Comparator<? super T> comparator, String order, int count) {
		List<T> selected = new ArrayList<>(candidates);
		switch (order) {
			case "ORDERI_ASCENDING" -> selected.sort(comparator);
			case "ORDERI_DESCENDING" -> selected.sort(comparator.reversed());
			case "ORDERI_RANDOM" -> Collections.shuffle(selected);
			default -> {
				return List.of();
			}
		}
		return selected.stream().limit(Math.min(count, selected.size())).toList();
	}

	private void spawnAt(Operation action, int npcId, float x, float y, float z, byte heading, Creature attackTarget,
			String walkerId) {
		int count = Math.max(1, integer(action, "num_to_spawn"));
		float range = decimal(action, "spawn_range");
		String generation = nextSpawnGeneration(action);
		for (int i = 0; i < count; i++) {
			double angle = Rnd.nextDouble() * Math.PI * 2;
			float radius = (float) (Rnd.nextDouble() * range);
			float spawnX = x + (float) Math.cos(angle) * radius;
			float spawnY = y + (float) Math.sin(angle) * radius;
			SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), npcId, spawnX, spawnY,
				z, heading);
			String lifecycleKey = generation == null ? null : runtimeStatePrefix + "spawn." + generation + '.' + i + '.';
			if (lifecycleKey != null) {
				template.setStableKey(dynamicStableKey(generation + '.' + i));
				template.setRuntimeLifecycleKey(lifecycleKey);
			}
			template.setMaster(getOwner());
			template.setCreatorId(getOwner().getObjectId());
			template.setMasterName(getOwner().getName());
			if (walkerId != null) {
				template.setWalkerId(walkerId);
			}
			template.setFly(value(action, "is_aerial_spawn").equals("TRUE") ? 1 : 0);
			template.setDespawnAtAttackState(!value(action, "despawn_at_attack_state").equals("FALSE"));
			VisibleObject spawned = SpawnEngine.spawnObject(template, getOwner().getInstanceId());
			boolean trackedBySpawnId = !value(action, "spawn_id").equals("SPAWN_ID_NONE");
			int liveTime = integer(action, "live_time");
			long liveDeadline = liveTime > 0 ? System.currentTimeMillis() + liveTime * 1000L : 0;
			if (lifecycleKey != null) {
				persistDynamicSpawn(lifecycleKey, npcId, spawnX, spawnY, z, heading, walkerId,
					value(action, "spawn_id"), template.getFly(),
					template.isDespawnAtAttackState(), liveDeadline);
			}
			if (trackedBySpawnId || liveTime > 0) {
				despawnAtAttackState.put(spawned, template.isDespawnAtAttackState());
			}
			if (trackedBySpawnId) {
				this.spawned.computeIfAbsent(value(action, "spawn_id"), key -> new ArrayList<>()).add(spawned);
			}
			if (attackTarget != null && spawned instanceof Npc npc && value(action, "attack_target_after_spawn").equals("TRUE")) {
				npc.getAggroList().addHate(attackTarget, integer(action, "hatepoints_to_add"));
			}
			if (liveTime > 0) {
				GameThreadPoolServices.threadPoolManager().schedule(() -> despawnForLifecycle(spawned),
					Math.max(1, liveDeadline - System.currentTimeMillis()));
			}
		}
	}

	private String nextSpawnGeneration(Operation action) {
		String actionKey = spawnActionKeys.get(action);
		if (runtimeStatePrefix == null || actionKey == null) {
			return null;
		}
		String sequenceKey = runtimeStatePrefix + "spawn_sequence." + actionKey;
		int sequence = runtimeState().getInt(sequenceKey, 0) + 1;
		runtimeState().put(sequenceKey, sequence);
		return actionKey + '.' + sequence;
	}

	private void persistDynamicSpawn(String key, int npcId, float x, float y, float z, byte heading, String walkerId,
			String spawnId, int fly, boolean despawnDuringAttack, long liveDeadline) {
		runtimeState().mutate(values -> {
			values.put(key + "npc", Integer.toString(npcId));
			values.put(key + "x", Float.toString(x));
			values.put(key + "y", Float.toString(y));
			values.put(key + "z", Float.toString(z));
			values.put(key + "heading", Integer.toString(Byte.toUnsignedInt(heading)));
			values.put(key + "walker", walkerId == null ? "" : walkerId);
			values.put(key + "spawn_id", spawnId);
			values.put(key + "fly", Integer.toString(fly));
			values.put(key + "despawn_at_attack", Boolean.toString(despawnDuringAttack));
			values.put(key + "live_deadline", Long.toString(liveDeadline));
		});
	}

	private void restoreDynamicSpawns() {
		String prefix = runtimeStatePrefix + "spawn.";
		for (String key : runtimeState().snapshot(prefix).keySet().stream()
			.filter(key -> key.endsWith(".npc")).sorted().toList()) {
			String lifecycleKey = key.substring(0, key.length() - "npc".length());
			if (!hasCompleteDynamicSpawn(lifecycleKey)) {
				runtimeState().removePrefix(lifecycleKey);
				continue;
			}
			long liveDeadline = runtimeState().getLong(lifecycleKey + "live_deadline", 0);
			if (liveDeadline > 0 && liveDeadline <= System.currentTimeMillis()) {
				runtimeState().removePrefix(lifecycleKey);
				continue;
			}
			SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(),
				runtimeState().getInt(lifecycleKey + "npc", 0),
				Float.parseFloat(runtimeState().get(lifecycleKey + "x")),
				Float.parseFloat(runtimeState().get(lifecycleKey + "y")),
				Float.parseFloat(runtimeState().get(lifecycleKey + "z")),
				(byte) runtimeState().getInt(lifecycleKey + "heading", 0));
			String generation = lifecycleKey.substring(prefix.length(), lifecycleKey.length() - 1);
			template.setStableKey(dynamicStableKey(generation));
			template.setRuntimeLifecycleKey(lifecycleKey);
			template.setMaster(getOwner());
			template.setCreatorId(getOwner().getObjectId());
			template.setMasterName(getOwner().getName());
			String walker = runtimeState().get(lifecycleKey + "walker", "");
			template.setWalkerId(walker.isBlank() ? null : walker);
			template.setFly(runtimeState().getInt(lifecycleKey + "fly", 0));
			template.setDespawnAtAttackState(runtimeState().getBoolean(lifecycleKey + "despawn_at_attack", true));
			VisibleObject object = SpawnEngine.spawnObject(template, getOwner().getInstanceId());
			String spawnId = runtimeState().get(lifecycleKey + "spawn_id", "SPAWN_ID_NONE");
			boolean tracked = !spawnId.equals("SPAWN_ID_NONE");
			if (tracked || liveDeadline > 0) {
				despawnAtAttackState.put(object, template.isDespawnAtAttackState());
			}
			if (tracked) {
				spawned.computeIfAbsent(spawnId, ignored -> new ArrayList<>()).add(object);
			}
			if (liveDeadline > 0) {
				GameThreadPoolServices.threadPoolManager().schedule(() -> despawnForLifecycle(object),
					Math.max(1, liveDeadline - System.currentTimeMillis()));
			}
		}
	}

	private boolean hasCompleteDynamicSpawn(String lifecycleKey) {
		Map<String, String> saved = runtimeState().snapshot(lifecycleKey);
		return List.of("npc", "x", "y", "z", "heading", "walker", "spawn_id", "fly", "despawn_at_attack",
				"live_deadline").stream().allMatch(field -> saved.containsKey(lifecycleKey + field));
	}

	private String dynamicStableKey(String generation) {
		return getOwner().getSpawn().getStableKey() + ":dynamic:" + generation.replace('.', ':');
	}

	public static void onDynamicSpawnRemoved(Npc npc) {
		if (npc == null || npc.getSpawn() == null || npc.getSpawn().getRuntimeLifecycleKey() == null
				|| npc.getPosition() == null || npc.getPosition().getWorldMapInstance() == null) {
			return;
		}
		npc.getPosition().getWorldMapInstance().getRuntimeState()
			.removePrefix(npc.getSpawn().getRuntimeLifecycleKey());
	}

	private void despawnForLifecycle(VisibleObject object) {
		if (!object.isSpawned()) {
			if (object instanceof Npc npc) {
				onDynamicSpawnRemoved(npc);
			}
			despawnAtAttackState.remove(object);
			return;
		}
		boolean duringAttack = object instanceof Npc npc && npc.getAi2() instanceof AbstractAI ai
			&& ai.isInState(AIState.FIGHT);
		if (shouldDelayLifecycleDespawn(despawnAtAttackState.getOrDefault(object, true), duringAttack)) {
			GameThreadPoolServices.threadPoolManager().schedule(() -> despawnForLifecycle(object), 1000);
			return;
		}
		despawnAtAttackState.remove(object);
		object.getController().onDelete();
	}

	static boolean shouldDelayLifecycleDespawn(boolean despawnAtAttackState, boolean duringAttack) {
		return !despawnAtAttackState && duringAttack;
	}

	private void despawn(Operation action) {
		List<VisibleObject> objects = spawned.remove(value(action, "spawn_id"));
		if (objects != null) {
			objects.forEach(object -> {
				despawnAtAttackState.remove(object);
				if (object.isSpawned()) {
					object.getController().onDelete();
				}
			});
		}
	}

	private void despawnByNameId(Operation action) {
		Integer npcId = DataManager.RETAIL_AI_DATA.findNpcId(value(action, "target_npc_nameid"));
		if (npcId == null) {
			return;
		}
		float radius = decimal(action, "bound_radius");
		int maxCount = integer(action, "max_count");
		int count = 0;
		for (Npc npc : getPosition().getWorldMapInstance().getNpcs(npcId)) {
			float dx = npc.getX() - getOwner().getX();
			float dy = npc.getY() - getOwner().getY();
			if (npc.isSpawned() && dx * dx + dy * dy < radius * radius) {
				despawnAtAttackState.remove(npc);
				npc.getController().onDelete();
				if (++count == maxCount) {
					return;
				}
			}
		}
	}

	private NpcSkillEntry skill(Operation operation) {
		return skill(getSkillList(), operation);
	}

	private static NpcSkillEntry skill(NpcSkillList skills, Operation operation) {
		if (skills == null) {
			return null;
		}
		return isAnySkill(operation) ? skills.getRandomSkill() : skills.getSkillByIndex(skillIndex(operation));
	}

	private static int skillIndex(Operation operation) {
		return Integer.parseInt(value(operation, "skill").substring(13));
	}

	private static boolean supportsCondition(String event, Operation condition) {
		return SUPPORTED_CONDITIONS.contains(condition.type()) && switch (condition.type()) {
			case "is_hyperlink_id" -> event.equals("on_hyperlink_clicked")
				&& retailHyperlinkId(value(condition, "hyperlink_id")) >= 0;
			case "has_attack_damage_flag" -> event.equals("on_attacked")
				&& ATTACK_DAMAGE_FLAGS.contains(value(condition, "damage_flag"));
			case "is_target_quest_state" -> supportsObject(event, value(condition, "target"))
				&& integerInRange(condition, "quest_id", 1, Integer.MAX_VALUE)
				&& Set.of("QSTATEI_NONE", "QSTATEI_ACQUIRED", "QSTATEI_SUCCEED")
					.contains(value(condition, "quest_progress"));
			case "is_hp_lower_than" -> supportsObject(event, value(condition, "who"))
				&& integerInRange(condition, "percent", 0, 100);
			case "is_hp_in_boundary" -> supportsObject(event, value(condition, "who"))
				&& integerInRange(condition, "larger_than", 0, 100)
				&& integerInRange(condition, "less_than", 0, 100);
			case "is_battle_timer_indicator" -> !value(condition, "btimer_indicator").isBlank();
			case "test_probability" -> integerInRange(condition, "percent", 0, 100);
			case "set_flag_var" -> !value(condition, "flagvar_indicator").isBlank();
			case "unset_flag_var" -> !value(condition, "flagvar_indicator").isBlank();
			case "is_skill_count_left" -> isSkillIndex(condition);
			case "is_message" -> integerInRange(condition, "message_type", 1, Integer.MAX_VALUE);
			case "is_user", "is_npc" -> supportsObject(event, value(condition, "obj_indicator"));
			case "is_enemy" -> supportsObject(event, value(condition, "who"));
			case "is_event_skill_id" -> Set.of("on_spelled", "on_damaged", "on_friend_spelling", "on_friend_spelled",
				"on_see_master_spelling", "on_see_master_spelled", "on_casted", "on_see_spell").contains(event)
				&& !value(condition, "skill_id").isBlank();
			case "is_event_skill_category" -> Set.of("on_spelled", "on_damaged", "on_friend_spelling", "on_friend_spelled",
				"on_see_master_spelling", "on_see_master_spelled", "on_casted", "on_see_spell").contains(event)
				&& !value(condition, "skill_category").isBlank();
			case "is_user_flying" -> supportsUser(event, value(condition, "user"));
			case "is_race" -> supportsObject(event, value(condition, "from"))
				&& isRetailRace(value(condition, "race_type"));
			case "is_distance_longer_than", "is_distance_shorter_than" -> supportsObject(event, value(condition, "who"))
				&& decimalInRange(condition, "distance", Float.MIN_VALUE);
			case "increase_intvar" -> !value(condition, "intvar_indicator").isBlank()
				&& isInteger(condition, "lower_bound") && isInteger(condition, "upper_bound")
				&& integer(condition, "lower_bound") < integer(condition, "upper_bound")
				&& Set.of("TRUE", "FALSE").contains(value(condition, "be_true_only_when_hit_the_bound"));
			case "set_intvar_if_larger_than", "set_intvar_if_less_than" -> !value(condition, "intvar_indicator").isBlank()
				&& isInteger(condition, "intvar_to_set") && isInteger(condition, "comparand");
			case "is_npc_state" -> value(condition, "who").equals("NPCI_SELF")
				&& Set.of("NPC_STATE_ATTACK", "NPC_STATE_IDLE", "NPC_STATE_GOTO_WAYPOINT", "NPC_STATE_RANDOM_MOVE",
					"NPC_STATE_FLEE", "NPC_STATE_USE_SKILL", "NPC_STATE_WAKE_UP", "NPC_STATE_GOTO_POINT")
					.contains(value(condition, "state"));
			case "is_user_class" -> supportsUser(event, value(condition, "user"))
				&& isRetailPlayerClass(value(condition, "class"));
			case "set_world_flag_var", "unset_world_flag_var" -> !value(condition, "flagvar_indicator").isBlank();
			case "is_in_abnormal_state" -> retailAbnormalMask(value(condition, "abnormal_state")) != null;
			case "is_obj_in_abnormal_state" -> supportsObject(event, value(condition, "obj"))
				&& retailAbnormalMask(value(condition, "abnormal_state")) != null;
			case "is_waypoint_index" -> event.equals("on_arrived_at_waypoint")
				&& integerInRange(condition, "index", 0, Integer.MAX_VALUE);
			case "is_last_waypoint" -> event.equals("on_arrived_at_waypoint");
			case "is_tribe" -> supportsObject(event, value(condition, "target"))
				&& isRetailTribe(value(condition, "tribe_name"));
			case "is_abnormal_state" -> Set.of("on_enter_abnormal_state", "on_leave_abnormal_state").contains(event)
				&& retailAbnormalMask(value(condition, "abnormal_state")) != null;
			case "add_intvar" -> !value(condition, "intvar_indicator").isBlank()
				&& integerInRange(condition, "var_to_add", 0, Integer.MAX_VALUE)
				&& validIntegerBounds(condition)
				&& Set.of("TRUE", "FALSE").contains(value(condition, "be_true_only_when_hit_the_bound"));
			case "decrease_intvar" -> !value(condition, "intvar_indicator").isBlank()
				&& validIntegerBounds(condition)
				&& Set.of("TRUE", "FALSE").contains(value(condition, "be_true_only_when_hit_the_bound"));
			case "is_user_level" -> supportsUser(event, value(condition, "user"))
				&& integerInRange(condition, "level_min", 0, Integer.MAX_VALUE)
				&& integerInRange(condition, "level_max", 0, Integer.MAX_VALUE)
				&& (integer(condition, "level_min") == 0 || integer(condition, "level_max") == 0
					|| integer(condition, "level_min") <= integer(condition, "level_max"));
			case "is_user_gender" -> supportsUser(event, value(condition, "user"))
				&& Set.of("GENDERI_MALE", "GENDERI_FEMALE").contains(value(condition, "gender"));
			case "is_my_curent_target" -> supportsObject(event, value(condition, "who"));
			case "is_world_flag_var" -> !value(condition, "flagvar_indicator").isBlank()
				&& Set.of("TRUE", "FALSE").contains(value(condition, "flag_expected"));
			case "sub_intvar" -> !value(condition, "intvar_indicator").isBlank()
				&& integerInRange(condition, "var_to_sub", 0, Integer.MAX_VALUE)
				&& validIntegerBounds(condition)
				&& Set.of("TRUE", "FALSE").contains(value(condition, "be_true_only_when_hit_the_bound"));
			default -> false;
		};
	}

	private static boolean supportsAction(String event, Operation action, boolean allowNpcScore) {
		return SUPPORTED_ACTIONS.contains(action.type()) && switch (action.type()) {
			case "give_score" -> allowNpcScore && supportsScoreTarget(event, value(action, "target"));
			case "system_message_to_all_by_obj_indicator_param" -> !value(action, "string_id").isBlank()
				&& supportsObject(event, value(action, "param"));
			case "give_exp" -> supportsRewardTarget(event, value(action, "target"))
				&& longInRange(action, "exp", 1, Long.MAX_VALUE);
			case "close_dialog" -> Set.of("on_hyperlink_clicked", "on_gauge_end").contains(event)
				&& value(action, "target").equals("USERI_TALKER");
			case "teleport_target_alias" -> Set.of("OBJI_SELF", "OBJI_TALKER", "OBJI_SEEN", "OBJI_MESSAGE_SENDER",
				"OBJI_CUR_TARGET", "OBJI_CASTER").contains(value(action, "target"))
				&& (!value(action, "target").equals("OBJI_CASTER") || event.equals("on_spelled"))
				&& supportsObject(event, value(action, "target")) && !value(action, "alias").isBlank()
				&& Set.of("TRUE", "FALSE").contains(value(action, "showfx"));
			case "teleport_target" -> Set.of("OBJI_SELF", "OBJI_TALKER", "OBJI_SEEN", "OBJI_CASTER")
				.contains(value(action, "target")) && supportsObject(event, value(action, "target"))
				&& decimals(action, "x", "y", "z", "dir")
				&& Set.of("TRUE", "FALSE").contains(value(action, "showfx"));
			case "use_skill" -> SUPPORTED_TARGETS.contains(value(action, "target"))
				&& supportsObject(event, value(action, "target"))
				&& (isSkillIndex(action) || isAnySkill(action))
				&& integerInRange(action, "skill_level", 0, Integer.MAX_VALUE);
			case "use_skill_by_attacker_indicator" -> supportsAttackerTarget(action)
				&& (isSkillIndex(action) || isAnySkill(action))
				&& integerInRange(action, "skill_level", 0, Integer.MAX_VALUE);
			case "activate_skillarea" -> integerInRange(action, "areaid", 1, Integer.MAX_VALUE)
				&& isSkillIndex(action) && (value(action, "skill_level").isBlank()
					|| integerInRange(action, "skill_level", 0, Integer.MAX_VALUE))
				&& Set.of("CASTOR", "AREA").contains(value(action, "broadcast_type"));
			case "open_directportal", "close_directportal" ->
				integerInRange(action, "direct_portal_id", 1, Integer.MAX_VALUE);
			case "open_directportal_by_user" -> event.equals("on_hyperlink_clicked")
				&& value(action, "requestuser").equals("USERI_TALKER")
				&& integerInRange(action, "direct_portal_id", 1, Integer.MAX_VALUE);
			case "enable_area" -> Set.of("AI_CONTROL_AREA_RESURRECT", "AI_CONTROL_AREA_QUESTSCRIPT",
				"AI_CONTROL_AREA_LIMIT_NOPARK", "AI_CONTROL_AREA_LIMIT_NORECALL", "AI_CONTROL_AREA_GROUPCTRL")
				.contains(value(action, "area_type"))
				&& !value(action, "area_name").isBlank()
				&& integerInRange(action, "op_code", 0, 1);
			case "on_off_windpath" -> integerInRange(action, "groupid", 1, Integer.MAX_VALUE)
				&& Set.of("TRUE", "FALSE").contains(value(action, "onoff"));
			case "on_off_moving_collision" -> Set.of("MOVING_COLLISION_JUMP", "MOVING_COLLISION_WINDBOX")
				.contains(value(action, "type")) && integerInRange(action, "sunzoneid", 1, Integer.MAX_VALUE)
				&& Set.of("TRUE", "FALSE").contains(value(action, "onoff"));
			case "charge_limitedquest" -> integerInRange(action, "quest_id", 1, Integer.MAX_VALUE)
				&& Set.of("TRUE", "FALSE").contains(value(action, "charge_max_count"));
			case "switch_target_by_attacker_indicator" -> supportsAttackerTarget(action)
				&& integerInRange(action, "percent_to_add", 0, Integer.MAX_VALUE)
				&& integerInRange(action, "points_to_add", 0, Integer.MAX_VALUE);
			case "add_battle_timer" -> !value(action, "btimer_indicator").isBlank()
				&& integerInRange(action, "delay", 0, Integer.MAX_VALUE);
			case "set_idle_timer" -> integerInRange(action, "delay", 0, Integer.MAX_VALUE);
			case "spawn" -> !value(action, "npc_nameid").isBlank() && !value(action, "spawn_id").isBlank()
				&& Set.of("SPAWN_LOCATION_MY_POINT", "SPAWN_LOCATION_ABSOLUTE", "SPAWN_LOCATION_RELATIVE",
					"SPAWN_LOCATION_WAY_POINT_START").contains(value(action, "spawn_location_type"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "despawn_at_attack_state"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "is_aerial_spawn"))
				&& integerInRange(action, "except_specialize", 0, Integer.MAX_VALUE)
				&& (!value(action, "spawn_location_type").equals("SPAWN_LOCATION_WAY_POINT_START")
					|| !value(action, "pathname").isBlank())
				&& integerInRange(action, "num_to_spawn", 1, Integer.MAX_VALUE) && isInteger(action, "dir")
				&& integerInRange(action, "live_time", 0, Integer.MAX_VALUE) && decimals(action, "x", "y", "z")
				&& decimalInRange(action, "spawn_range", 0);
			case "spawn_on_target" -> SUPPORTED_TARGETS.contains(value(action, "target_obj"))
				&& supportsObject(event, value(action, "target_obj"))
				&& !value(action, "npc_nameid").isBlank() && !value(action, "spawn_id").isBlank()
				&& Set.of("TRUE", "FALSE").contains(value(action, "despawn_at_attack_state"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "attack_target_after_spawn"))
				&& integerInRange(action, "num_to_spawn", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "live_time", 0, Integer.MAX_VALUE)
				&& integerInRange(action, "valid_distance", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "hatepoints_to_add", 0, Integer.MAX_VALUE)
				&& decimalInRange(action, "spawn_range", 0);
			case "spawn_on_target_by_attacker_indicator" -> supportsAttackerTarget(action)
				&& !value(action, "npc_nameid").isBlank() && !value(action, "spawn_id").isBlank()
				&& Set.of("TRUE", "FALSE").contains(value(action, "despawn_at_attack_state"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "attack_target_after_spawn"))
				&& integerInRange(action, "num_to_spawn", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "live_time", 0, Integer.MAX_VALUE)
				&& integerInRange(action, "valid_distance", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "hatepoints_to_add", 0, Integer.MAX_VALUE)
				&& decimalInRange(action, "spawn_range", 0);
			case "spawn_on_multi_target" -> !value(action, "npc_nameid").isBlank()
				&& !value(action, "spawn_id").isBlank()
				&& Set.of("TRUE", "FALSE").contains(value(action, "despawn_at_attack_state"))
				&& Set.of("ORDERI_ASCENDING", "ORDERI_DESCENDING", "ORDERI_RANDOM")
					.contains(value(action, "order_in_attacker_list"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "attack_target_after_spawn"))
				&& integerInRange(action, "num_to_spawn", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "live_time", 0, Integer.MAX_VALUE)
				&& integerInRange(action, "total_set_to_spawn", 1, Integer.MAX_VALUE)
				&& decimalInRange(action, "valid_distance", Float.MIN_VALUE)
				&& integerInRange(action, "hatepoints_to_add", 0, Integer.MAX_VALUE)
				&& decimalInRange(action, "spawn_range", 0);
			case "despawn" -> !value(action, "spawn_id").isBlank();
			case "despawn_by_nameid" -> !value(action, "target_npc_nameid").isBlank()
				&& decimalInRange(action, "bound_radius", Float.MIN_VALUE)
				&& integerInRange(action, "max_count", 1, Integer.MAX_VALUE);
			case "send_message" -> value(action, "target").equals("OBJI_SELF")
				&& integerInRange(action, "message_type", 1, Integer.MAX_VALUE)
				&& isInteger(action, "param1") && isInteger(action, "param2")
				&& SUPPORTED_MESSAGE_OBJECTS.contains(value(action, "param_obj"))
				&& supportsObject(event, value(action, "param_obj"));
			case "broadcast_message" -> integerInRange(action, "message_type", 1, Integer.MAX_VALUE)
				&& isInteger(action, "param1") && isInteger(action, "param2")
				&& integerInRange(action, "range_as_meter", 1, Integer.MAX_VALUE)
				&& SUPPORTED_MESSAGE_OBJECTS.contains(value(action, "param_obj"))
				&& supportsObject(event, value(action, "param_obj"));
			case "broadcast_message_to_party" -> integerInRange(action, "message_type", 1, Integer.MAX_VALUE)
				&& isInteger(action, "param1") && isInteger(action, "param2")
				&& supportsNpcPartyMessageObject(event, value(action, "param_obj"));
			case "say" -> supportsUser(event, value(action, "user")) && !value(action, "string_id").isBlank();
			case "say_to_all", "shout_to_all" -> !value(action, "string_id").isBlank();
			case "say_to_all_str" -> !value(action, "string").isBlank();
			case "change_world_scene_status" -> isInteger(action, "scenestatus")
				&& retailStageType(integer(action, "scenestatus")) != null;
			case "display_system_message" -> !value(action, "string_id").isBlank();
			case "send_system_msg" -> !value(action, "string_id").isBlank();
			case "send_system_msg_by_user_indicator" -> supportsUser(event, value(action, "user"))
				&& !value(action, "string_id").isBlank();
			case "reset_hatepoints" -> Set.of("TRUE", "FALSE").contains(value(action, "is_except_most_hating"))
				&& Set.of("TRUE", "FALSE").contains(value(action, "volatile_hatepoint_only"));
			case "control_door" -> integerInRange(action, "id", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "method", 0, 2);
			case "set_condition_spawn_variable" -> !value(action, "string").isBlank()
				&& isInteger(action, "set") && isInteger(action, "modify");
			case "set_condition_spawn_variable_to_world" -> !value(action, "worldid").isBlank()
				&& !value(action, "string").isBlank() && isInteger(action, "set") && isInteger(action, "modify");
			case "give_item_by_user_indicator" -> supportsUser(event, value(action, "receiver"))
				&& !value(action, "item_id").isBlank() && integerInRange(action, "min", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "max", 1, Integer.MAX_VALUE)
				&& integer(action, "min") <= integer(action, "max");
			case "give_item_by_obj_indicator" -> value(action, "receiver").equals("OBJI_TALKER")
				&& supportsObject(event, value(action, "receiver")) && !value(action, "item_id").isBlank()
				&& integerInRange(action, "min", 1, Integer.MAX_VALUE)
				&& integerInRange(action, "max", 1, Integer.MAX_VALUE)
				&& integer(action, "min") <= integer(action, "max");
			case "toggle_attackable_status_flag" -> Set.of("TRUE", "FALSE").contains(value(action, "attakable"));
			case "play_cutscene_by_user_indicator" -> supportsCutsceneTarget(event, value(action, "target"))
				&& integerInRange(action, "cutscene_id", 1, Integer.MAX_VALUE)
				&& integer(action, "quest_id") == 0
				&& Set.of("CUTSCENE_PLAY_TO_USER", "CUTSCENE_PLAY_TO_PARTY", "CUTSCENE_PLAY_TO_ALLIANCE")
					.contains(value(action, "play_target_type"))
				&& (value(action, "teleport_alias").isBlank()
					|| value(action, "play_target_type").equals("CUTSCENE_PLAY_TO_USER"));
			case "add_hate_point" -> supportsObject(event, value(action, "target"))
				&& integerInRange(action, "point_to_add", 0, Integer.MAX_VALUE);
			case "switch_target" -> supportsObject(event, value(action, "target"))
				&& integerInRange(action, "percent_to_add", 0, Integer.MAX_VALUE)
				&& integerInRange(action, "points_to_add", 0, Integer.MAX_VALUE);
			case "attack_most_hating" -> (value(action, "skill").equals("SKILLI_NONE") || isSkillIndex(action))
				&& (!isInteger(action, "skill_level") || integer(action, "skill_level") == 0);
			case "change_direction" -> integerInRange(action, "direction", 0, 120);
			case "random_move" -> integerInRange(action, "time_to_move", 1, Integer.MAX_VALUE);
			case "goto_waypoint" -> integerInRange(action, "waypoint", 0, Integer.MAX_VALUE)
				&& supportsMoveType(action);
			case "goto_next_waypoint" -> supportsMoveType(action);
			case "goto_alias" -> !value(action, "alias").isBlank() && supportsMoveType(action);
			case "flee_from" -> supportsObject(event, value(action, "from"))
				&& integerInRange(action, "seconds", 0, Integer.MAX_VALUE)
				&& Set.of("TRUE", "FALSE").contains(value(action, "push_state"));
			case "switch_target_by_class_indicator" -> isRetailPlayerClass(value(action, "target_class"))
				&& integerInRange(action, "percent_to_add", 0, 100)
				&& integerInRange(action, "points_to_add", 0, Integer.MAX_VALUE)
				&& Set.of("TRUE", "FALSE").contains(value(action, "restricted_range"));
			case "despawn_self", "do_nothing", "return_to_spawn_point", "reset_queued_actions" -> true;
			default -> false;
		};
	}

	private static boolean supportsScoreTarget(String event, String target) {
		return switch (target) {
			case "USERI_TALKER" -> event.equals("on_talked_by_user");
			case "USERI_KILLER" -> event.equals("on_killed_by_user");
			case "USERI_EVENT_MAKER" -> Set.of("on_die", "on_killed_by_user", "on_user_enter_sensory_area",
				"on_user_leave_sensory_area").contains(event);
			default -> false;
		};
	}

	private static boolean supportsRewardTarget(String event, String target) {
		return switch (target) {
			case "USERI_ATTACKER" -> event.equals("on_attacked");
			case "USERI_KILLER" -> event.equals("on_killed_by_user");
			default -> false;
		};
	}

	private static boolean supportsNpcPartyMessageObject(String event, String paramObject) {
		return switch (event) {
			case "on_battle_timer" -> Set.of("OBJI_SELF", "OBJI_CUR_TARGET").contains(paramObject);
			case "on_killed_by_user" -> paramObject.equals("OBJI_KILLER");
			case "on_attacked", "on_spelled" -> paramObject.equals("OBJI_SELF");
			default -> false;
		};
	}

	static boolean supportsNpcScore(int scoreApplyType, int equalizingScore) {
		return scoreApplyType >= 0 && scoreApplyType <= 3 && equalizingScore == 0;
	}

	private static boolean supportsNpcScore(Npc npc,
			com.aionemu.gameserver.dataholders.RetailAiData.NpcScore score) {
		return score != null && supportsNpcScore(score.scoreApplyType(), score.equalizingScore())
			&& npc.getPosition() != null && npc.getPosition().getWorldMapInstance() != null
			&& npc.getPosition().getWorldMapInstance().getInstanceHandler()
				.supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType());
	}

	private static boolean hasUnsupportedActionsAfterSkill(String event, List<Operation> actions) {
		if (event.equals("on_leave_attack_state")) {
			return false;
		}
		for (int i = 0; i + 1 < actions.size(); i++) {
			if (isSkillAction(actions.get(i))) {
				return !supportsImmediateTerminalCleanup(event, actions);
			}
		}
		return false;
	}

	static boolean supportsImmediateTerminalCleanup(String event, List<Operation> actions) {
		return Set.of("on_despawn", "on_killed_by_user").contains(event) && actions.size() == 2
			&& actions.get(0).type().equals("use_skill") && value(actions.get(0), "target").equals("OBJI_SELF")
			&& actions.get(1).type().equals("despawn") && value(actions.get(1), "spawn_id").equals("SPAWN_ID_1");
	}

	private static boolean isSkillAction(Operation operation) {
		return (operation.type().equals("use_skill") || operation.type().equals("use_skill_by_attacker_indicator"))
			&& !isNoSkill(operation) || operation.type().equals("activate_skillarea")
			|| operation.type().equals("attack_most_hating") && !value(operation, "skill").equals("SKILLI_NONE");
	}

	private static boolean isNoSkill(Operation operation) {
		return value(operation, "skill").equals("SKILLI_NONE");
	}

	private static boolean usesEventTarget(Operation operation) {
		return value(operation, "target").equals("OBJI_EVENT_TARGET")
			|| value(operation, "param_obj").equals("OBJI_EVENT_TARGET");
	}

	private static boolean supportsAttackerTarget(Operation operation) {
		return SUPPORTED_ATTACKER_TARGETS.contains(value(operation, "target"))
			&& Set.of("TRUE", "FALSE").contains(value(operation, "restricted_range"));
	}

	private static boolean supportsMoveType(Operation operation) {
		return Set.of("MOVETYPE_NOT_SPECIFIED", "MOVETYPE_WALK", "MOVETYPE_RUN")
			.contains(value(operation, "move_type"));
	}

	private static boolean supportsCutsceneTarget(String event, String target) {
		return switch (target) {
			case "USERI_KILLER" -> event.equals("on_killed_by_user");
			case "USERI_EVENT_MAKER" -> Set.of("on_user_enter_sensory_area", "on_user_leave_sensory_area").contains(event);
			case "USERI_MASTER" -> true;
			case "USERI_SEEN" -> event.equals("on_see_user");
			case "USERI_TALKER" -> event.equals("on_hyperlink_clicked");
			case "USERI_EVENT_TARGET" -> TARGET_EVENTS.contains(event);
			default -> false;
		};
	}

	private static boolean supportsObject(String event, String indicator) {
		return switch (indicator) {
			case "OBJI_SELF", "OBJI_CUR_TARGET" -> true;
			case "OBJI_EVENT_TARGET" -> TARGET_EVENTS.contains(event);
			case "OBJI_CASTER" -> Set.of("on_attacked", "on_spelled", "on_enter_abnormal_state", "on_leave_abnormal_state", "on_friend_spelled",
				"on_see_master_spelling", "on_see_master_spelled", "on_casted", "on_see_spell",
				"on_party_mbr_spelled").contains(event);
			case "OBJI_ATTACKER" -> Set.of("on_attacked", "on_spelled", "on_damaged", "on_see_friend_attacked", "on_friend_spelled",
				"on_master_attacked", "on_see_attacked", "on_party_mbr_attacked").contains(event);
			case "OBJI_PARTY_MEMBER" -> Set.of("on_party_mbr_attacked", "on_party_mbr_spelled").contains(event);
			case "OBJI_FRIEND" -> FRIEND_EVENTS.contains(event);
			case "OBJI_FLEE_FROM" -> event.equals("on_stop_to_flee");
			case "OBJI_KILLER" -> Set.of("on_die", "on_killed_by_user", "on_killed_by_npc", "on_see_friend_killed_by_user",
				"on_sense_friend_killed_by_user").contains(event);
			case "OBJI_SEEN" -> Set.of("on_see_user", "on_see_npc", "on_see_user_move", "on_see_npc_move").contains(event);
			case "OBJI_TALKER" -> Set.of("on_talked_by_user", "on_hyperlink_clicked", "on_gauge_begin",
				"on_gauge_stop", "on_gauge_end").contains(event);
			case "OBJI_MESSAGE_SENDER", "OBJI_MESSAGE_PARAM" -> event.equals("on_message");
			default -> false;
		};
	}

	private static boolean isRetailRace(String race) {
		if (Set.of("pc", "pc_light", "pc_dark").contains(race.toLowerCase())) {
			return true;
		}
		try {
			Race.valueOf(race.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean isRetailTribe(String tribe) {
		try {
			TribeClass.valueOf(tribe.toUpperCase(Locale.ROOT));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean validIntegerBounds(Operation operation) {
		return isInteger(operation, "lower_bound") && isInteger(operation, "upper_bound")
			&& integer(operation, "lower_bound") < integer(operation, "upper_bound");
	}

	private static boolean isSkillIndex(Operation operation) {
		String skill = value(operation, "skill");
		if (!skill.startsWith("SKILLI_INDEX_")) {
			return false;
		}
		try {
			return Integer.parseInt(skill.substring(13)) >= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isAnySkill(Operation operation) {
		return value(operation, "skill").equals("SKILLI_ANY_SKILL");
	}

	private static boolean decimals(Operation operation, String... keys) {
		try {
			for (String key : keys) {
				Float.parseFloat(value(operation, key));
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isInteger(Operation operation, String key) {
		try {
			Integer.parseInt(value(operation, key));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean decimalInRange(Operation operation, String key, float minimum) {
		try {
			return Float.parseFloat(value(operation, key)) >= minimum;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean integerInRange(Operation operation, String key, int minimum, int maximum) {
		try {
			int value = Integer.parseInt(value(operation, key));
			return value >= minimum && value <= maximum;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean longInRange(Operation operation, String key, long minimum, long maximum) {
		try {
			long value = Long.parseLong(value(operation, key));
			return value >= minimum && value <= maximum;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static List<Operation> concat(List<Operation> first, List<Operation> second) {
		List<Operation> operations = new ArrayList<>(first.size() + second.size());
		operations.addAll(first);
		operations.addAll(second);
		return operations;
	}

	private static String value(Operation operation, String key) {
		return operation.value(key) == null ? "" : operation.value(key);
	}

	private static int integer(Operation operation, String key) {
		String value = value(operation, key);
		return value.isBlank() ? 0 : Integer.parseInt(value);
	}

	private static long longInteger(Operation operation, String key) {
		return Long.parseLong(value(operation, key));
	}

	private static float decimal(Operation operation, String key) {
		String value = value(operation, key);
		return value.isBlank() ? 0 : Float.parseFloat(value);
	}

	private void resetPatternState() {
		resetPatternState(Set.copyOf(actionTasks));
	}

	private void resetPatternState(Set<Future<?>> tasksToCancel) {
		gaugeObservers.forEach(this::stopGauge);
		for (Future<?> timer : timers.values()) {
			timer.cancel(false);
		}
		timers.clear();
		if (runtimeStatePrefix != null) {
			for (String timer : Set.copyOf(persistentTimers)) {
				InstanceDeadlineScheduler.cancel(getPosition().getWorldMapInstance(), runtimeStatePrefix + "timer." + timer);
			}
			persistentTimers.clear();
			runtimeState().removePrefix(runtimeStatePrefix);
		}
		cancelQueuedActions(tasksToCancel);
		fleeMoveTask = null;
		fleeStopTask = null;
		spawned.values().stream().flatMap(List::stream).forEach(this::despawnForLifecycle);
		spawned.clear();
		pendingCutsceneTeleports.clear();
		flags.clear();
		intVars.clear();
		fighting = false;
		wakeUpUntil = 0;
		waypointMoving = false;
		aliasMoving = false;
		waypointIndex = -1;
	}

	private void resetQueuedActions() {
		cancelQueuedActions(Set.copyOf(actionTasks));
	}

	private void cancelQueuedActions(Set<Future<?>> tasks) {
		tasks.forEach(task -> task.cancel(false));
		actionTasks.removeAll(tasks);
	}
}
