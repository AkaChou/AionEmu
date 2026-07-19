package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailPatternAI2Test {
	@Test
	void divineTowerRetailPatternsUseCompleteConditionVariables() {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		NpcSkillData previousNpcSkills = DataManager.NPC_SKILL_DATA;
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			XmlDataLoader loader = new XmlDataLoader();
			DataManager.RETAIL_AI_DATA = loader.loadRetailAiData();
			DataManager.NPC_SKILL_DATA = loader.loadNpcSkillData();
			for (int worldId : new int[] { 310160000, 320160000 }) {
				for (int npcId : new int[] { 248025, 248401, 248404, 248405, 248406, 248407, 248440, 248441, 248442, 248443 }) {
					SkillNpc npc = new ObjenesisStd().newInstance(SkillNpc.class);
					npc.npcId = npcId;
					npc.worldId = worldId;
					npc.objectTemplate = new NpcTemplate();
					npc.skillList = new NpcSkillList(npc);
					assertTrue(RetailPatternAI2.supports(DataManager.RETAIL_AI_DATA.getPattern(npcId), npc),
						worldId + ":" + npcId);
				}
			}
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
			DataManager.NPC_SKILL_DATA = previousNpcSkills;
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void executesSpawnedRagnarokPhaseSkillsThroughRetailEventChain() throws ReflectiveOperationException {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		SkillData previousSkills = DataManager.SKILL_DATA;
		NpcSkillData previousNpcSkills = DataManager.NPC_SKILL_DATA;
		RetailAiData previousRetailAi = DataManager.RETAIL_AI_DATA;
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			XmlDataLoader loader = new XmlDataLoader();
			DataManager.SKILL_DATA = loader.loadSkillData();
			DataManager.NPC_SKILL_DATA = loader.loadNpcSkillData();
			DataManager.RETAIL_AI_DATA = loader.loadRetailAiData();

			Pattern pattern = DataManager.RETAIL_AI_DATA.getPattern(216576);
			assertNotNull(pattern);
			assertEquals("DF4_FieldRaid", pattern.name());
			assertTrue(RetailPatternAI2.supports(pattern));

			SkillNpc owner = new ObjenesisStd().newInstance(SkillNpc.class);
			owner.npcId = 216576;
			owner.objectTemplate = new ObjenesisStd().newInstance(NpcTemplate.class);
			owner.controller = new RecordingNpcController();
			owner.controller.setOwner(owner);
			owner.skillList = skillList(DataManager.NPC_SKILL_DATA.getNpcSkillList(216576).getNpcSkills());
			owner.setLifeStats(new ObjenesisStd().newInstance(FixedNpcLifeStats.class));
			assertTrue(RetailPatternAI2.supports(pattern, owner));
			Map<Integer, Integer> phaseHp = Map.of(10, 80, 11, 60, 12, 40, 15, 20);
			Map<Integer, String> phaseFlag = Map.of(10, "FLAGVARI_BETA_2", 11, "FLAGVARI_BETA_1",
				12, "FLAGVARI_ALPHA_5", 15, "FLAGVARI_ALPHA_2");
			Map<Integer, Integer> phaseSkillId = Map.of(10, 19207, 11, 18675, 12, 19207, 15, 19208);
			for (int priority : List.of(10, 11, 12, 15)) {
				Rule phase = pattern.event("on_battle_timer").stream()
					.filter(rule -> rule.priority() == priority)
					.findFirst().orElseThrow();
				Operation phaseSkill = phase.actions().stream()
					.filter(action -> action.type().equals("use_skill") && action.value("target").equals("OBJI_SELF"))
					.toList().getLast();
				Pattern phaseSkillPattern = new Pattern(pattern.name(), Map.of("on_battle_timer",
					List.of(new Rule(phase.priority(), phase.category(), phase.conditions(), List.of(phaseSkill)))));
				RetailPatternAI2 ai = new RetailPatternAI2();
				setField(AbstractAI.class, ai, "owner", owner);
				setField(RetailPatternAI2.class, ai, "pattern", phaseSkillPattern);
				((FixedNpcLifeStats) owner.getLifeStats()).hpPercentage = phaseHp.get(priority);
				owner.controller.skillUses = 0;

				try {
					invokeEvent(ai, "on_battle_timer", "BTIMERI_INDEX_0", null);

					assertTrue(flags(ai).contains(phaseFlag.get(priority)));
					assertEquals(1, owner.controller.skillUses);
					assertEquals(phaseSkillId.get(priority), owner.controller.lastSkillId);
					assertEquals(46, owner.controller.lastSkillLevel);
					assertNotNull(DataManager.SKILL_DATA.getSkillTemplate(owner.controller.lastSkillId));
				} finally {
					resetPatternState(ai);
				}
			}
		} finally {
			DataManager.SKILL_DATA = previousSkills;
			DataManager.NPC_SKILL_DATA = previousNpcSkills;
			DataManager.RETAIL_AI_DATA = previousRetailAi;
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void supportsSystemMessageToAllWithObjectContext() {
		Operation action = new Operation("system_message_to_all_by_obj_indicator_param",
			Map.of("string_id", "STR_IDRUN_STAGE2_NOTICE", "param", "OBJI_CUR_TARGET"));

		assertTrue(RetailPatternAI2.supports(new Pattern("system_message", Map.of("on_see_user",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(action)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_system_message", Map.of("on_see_user",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(new Operation(action.type(),
				Map.of("string_id", action.value("string_id"), "param", "OBJI_MESSAGE_PARAM")))))))));
	}

	@Test
	void supportsAndMatchesRetailDodgeDamageFlag() {
		Operation dodge = new Operation("has_attack_damage_flag", Map.of("damage_flag", "DODGE"));
		Operation block = new Operation("has_attack_damage_flag", Map.of("damage_flag", "BLOCK"));

		assertTrue(RetailPatternAI2.supports(new Pattern("dodge", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(dodge), List.of(new Operation("do_nothing", Map.of()))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("block", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(block), List.of(new Operation("do_nothing", Map.of()))))))));
		assertTrue(RetailPatternAI2.matchesAttackDamageFlag(AttackStatus.DODGE, "DODGE"));
		assertTrue(RetailPatternAI2.matchesAttackDamageFlag(AttackStatus.OFFHAND_DODGE, "DODGE"));
		assertFalse(RetailPatternAI2.matchesAttackDamageFlag(AttackStatus.NORMALHIT, "DODGE"));
	}

	@Test
	void supportsWakeUpStateEventsOnlyForMonsterMappings() {
		Pattern pattern = new Pattern("wake_up", Map.of(
			"on_enter_wakeup_state", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_leave_wakeup_state", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("do_nothing", Map.of()))))));

		assertTrue(RetailPatternAI2.supports(pattern));
		assertTrue(RetailPatternAI2.hasCompleteWakeUpData(pattern, NpcTemplateType.MONSTER));
		assertFalse(RetailPatternAI2.hasCompleteWakeUpData(pattern, NpcTemplateType.GENERAL));
		assertFalse(RetailPatternAI2.hasCompleteWakeUpData(pattern, null));
	}

	@Test
	void usesDefaultIdleThinkingForCombatOnlyPatterns() {
		Rule rule = new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.shouldUseDefaultIdleThinking(
			new Pattern("combat", Map.of("on_enter_attack_state", List.of(rule)))));
		for (String event : Set.of(
			"on_wake_up", "on_enter_idle_state", "on_enter_wakeup_state", "on_leave_wakeup_state")) {
			assertFalse(RetailPatternAI2.shouldUseDefaultIdleThinking(
				new Pattern("idle", Map.of(event, List.of(rule)))));
		}
	}

	@Test
	void detectsScriptedCombatSkillRotations() {
		Rule skillRule = new Rule(1, "PLANNED", List.of(), List.of(new Operation("use_skill", Map.of(
			"target", "OBJI_CUR_TARGET", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		Rule idleRule = new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.hasScriptedCombatSkills(
			new Pattern("combat", Map.of("on_battle_timer", List.of(skillRule)))));
		assertFalse(RetailPatternAI2.hasScriptedCombatSkills(
			new Pattern("idle", Map.of("on_wake_up", List.of(idleRule)))));

		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			Pattern admaDeathknight = new XmlDataLoader().loadRetailAiData().getPattern(214696);

			assertNotNull(admaDeathknight);
			assertEquals("Adma_DeathknightNamed", admaDeathknight.name());
			assertTrue(RetailPatternAI2.hasScriptedCombatSkills(admaDeathknight));
		} finally {
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void supportsMasterEventsOnlyWhenNpcHasMaster() {
		Pattern pattern = new Pattern("master", Map.of(
			"on_master_attacked", List.of(new Rule(1, "INSTANT",
				List.of(new Operation("is_user", Map.of("obj_indicator", "OBJI_ATTACKER"))),
				List.of(new Operation("add_hate_point", Map.of(
					"target", "OBJI_ATTACKER", "point_to_add", "1000"))))),
			"on_see_master_spelling", List.of(new Rule(1, "INSTANT", List.of(),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_see_master_spelled", List.of(new Rule(1, "INSTANT",
				List.of(new Operation("is_user", Map.of("obj_indicator", "OBJI_CASTER"))),
				List.of(new Operation("add_hate_point", Map.of(
					"target", "OBJI_CASTER", "point_to_add", "1000")))))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc npc = objenesis.newInstance(MasterNpc.class);

		assertTrue(RetailPatternAI2.supports(pattern));
		assertFalse(RetailPatternAI2.hasCompleteMasterData(pattern, null));
		assertFalse(RetailPatternAI2.hasCompleteMasterData(pattern, npc));
		npc.master = objenesis.newInstance(MasterNpc.class);
		assertTrue(RetailPatternAI2.hasCompleteMasterData(pattern, npc));
	}

	@Test
	void dispatchesMasterAttackAndSkillEvents() throws ReflectiveOperationException {
		Pattern pattern = new Pattern("master", Map.of(
			"on_master_attacked", List.of(flagRule("FLAGVARI_ALPHA_1")),
			"on_see_master_spelling", List.of(flagRule("FLAGVARI_ALPHA_2")),
			"on_see_master_spelled", List.of(flagRule("FLAGVARI_ALPHA_3")),
			"on_friend_spelled", List.of(flagRule("FLAGVARI_BETA_1")),
			"on_see_spell", List.of(flagRule("FLAGVARI_BETA_2"))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		MasterNpc master = objenesis.newInstance(MasterNpc.class);
		MasterNpc other = objenesis.newInstance(MasterNpc.class);
		owner.master = master;
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);
		SkillData previous = DataManager.SKILL_DATA;
		DataManager.SKILL_DATA = new SkillData();
		try {
			ai.onSeeAttack(other, master);
			ai.onSeeSkill(master, other, 1, 1);
			ai.onFriendSpelled(master, other, 1, 1);
			ai.onSeeSpell(master, other, 1, 1);
			ai.onSeeSkill(other, master, 1, 1);
			ai.onFriendSpelled(other, master, 1, 1);
			ai.onSeeSpell(other, master, 1, 1);
		} finally {
			DataManager.SKILL_DATA = previous;
		}

		assertEquals(Set.of("FLAGVARI_ALPHA_1", "FLAGVARI_ALPHA_2", "FLAGVARI_ALPHA_3"), flags(ai));
	}

	@Test
	void dispatchesQuitCutsceneEvent() throws ReflectiveOperationException {
		Pattern pattern = new Pattern("cutscene", Map.of(
			"on_quit_cutscene", List.of(flagRule("FLAGVARI_ALPHA_1"))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);

		assertTrue(RetailPatternAI2.supports(pattern));
		ai.onQuitCutscene(objenesis.newInstance(Player.class), 914);

		assertEquals(Set.of("FLAGVARI_ALPHA_1"), flags(ai));
	}

	@Test
	void supportsAndDispatchesRetailQuestEvents() throws ReflectiveOperationException {
		Operation questState = new Operation("is_target_quest_state", Map.of(
			"target", "OBJI_SEEN", "quest_id", "18302", "quest_progress", "QSTATEI_SUCCEED"));
		Pattern pattern = new Pattern("quest", Map.of(
			"on_see_user", List.of(new Rule(1, "DIRECT",
				List.of(questState, new Operation("set_flag_var", Map.of("flagvar_indicator", "FLAGVARI_ALPHA_1"))),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_quest_finished", List.of(flagRule("FLAGVARI_ALPHA_2"))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		Player player = objenesis.newInstance(Player.class);
		QuestStateList quests = new QuestStateList();
		quests.addQuest(18302, new QuestState(18302, QuestStatus.COMPLETE, 0, 0, null, null, null));
		player.setQuestStateList(quests);
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);

		assertTrue(RetailPatternAI2.supports(pattern));
		invokeEvent(ai, "on_see_user", player);
		ai.onQuestFinished(player, 18302);

		assertEquals(Set.of("FLAGVARI_ALPHA_1", "FLAGVARI_ALPHA_2"), flags(ai));
		Operation invalidState = new Operation(questState.type(), Map.of(
			"target", "OBJI_SEEN", "quest_id", "18302", "quest_progress", "QSTATEI_UNKNOWN"));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_quest", Map.of("on_see_user",
			List.of(new Rule(1, "DIRECT", List.of(invalidState), List.of(new Operation("do_nothing", Map.of()))))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("limited_quest", Map.of("on_quest_finished",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("charge_limitedquest",
				Map.of("quest_id", "9645", "charge_max_count", "FALSE")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_limited_quest", Map.of("on_quest_finished",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("charge_limitedquest",
				Map.of("quest_id", "9645", "charge_max_count", "UNKNOWN")))))))));
	}

	@Test
	void givesRawExperienceToRetailEventUser() throws ReflectiveOperationException {
		Operation reward = new Operation("give_exp", Map.of("target", "USERI_KILLER", "exp", "5000000000"));
		Pattern pattern = new Pattern("experience", Map.of("on_killed_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(reward)))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		ExperiencePlayer player = objenesis.newInstance(ExperiencePlayer.class);
		player.commonData = new RecordingPlayerCommonData();
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);

		assertTrue(RetailPatternAI2.supports(pattern));
		ai.handleKilled(player);

		assertEquals(5000000000L, player.commonData.experience);
		assertNull(player.commonData.rewardType);
		Operation invalid = new Operation("give_exp", Map.of("target", "USERI_ATTACKER", "exp", "-1"));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_experience", Map.of("on_killed_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(invalid)))))));
	}

	@Test
	void supportsGaugeEventsOnlyWithRetailTalkDelay() {
		Pattern gauge = new Pattern("gauge", Map.of(
			"on_gauge_begin", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_gauge_stop", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_gauge_end", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("close_dialog", Map.of("target", "USERI_TALKER")))))));
		var missingDelay = new com.aionemu.gameserver.dataholders.RetailAiData.Npc(
			200001, "gauge", "gauge", 0, 0, 360, 0, null,
			com.aionemu.gameserver.dataholders.RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50);
		var complete = new com.aionemu.gameserver.dataholders.RetailAiData.Npc(
			200001, "gauge", "gauge", 0, 0, 360, 10, null,
			com.aionemu.gameserver.dataholders.RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50);

		assertTrue(RetailPatternAI2.supports(gauge));
		assertFalse(RetailPatternAI2.hasCompleteGaugeData(gauge, null));
		assertFalse(RetailPatternAI2.hasCompleteGaugeData(gauge, missingDelay));
		assertTrue(RetailPatternAI2.hasCompleteGaugeData(gauge, complete));
	}

	@Test
	void acceptsOnlyDirectRetailNpcScores() {
		assertTrue(RetailPatternAI2.supportsNpcScore(0, 0));
		assertFalse(RetailPatternAI2.supportsNpcScore(1, 0));
		assertFalse(RetailPatternAI2.supportsNpcScore(0, 1));
		assertTrue(RetailPatternAI2.supportsNpcScore(301120000, 232855, 0, 0));
		assertTrue(RetailPatternAI2.supportsNpcScore(301670000, 833961, 0, 0));
		assertFalse(RetailPatternAI2.supportsNpcScore(301120000, 833961, 0, 0));
		assertFalse(RetailPatternAI2.supportsNpcScore(301120000, 232855, 1, 0));
	}

	@Test
	void keepsNpcScoreOutOfStructureOnlyCoverage() {
		Operation score = new Operation("give_score", Map.of("target", "USERI_TALKER"));
		Pattern pattern = new Pattern("score", Map.of("on_talked_by_user", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(score)))));

		assertFalse(RetailPatternAI2.supports(pattern));
	}

	@Test
	void supportsMovingCollisionStructureWhenRetailAreaExists() {
		for (String type : List.of("MOVING_COLLISION_JUMP", "MOVING_COLLISION_WINDBOX")) {
			Operation action = new Operation("on_off_moving_collision",
				Map.of("type", type, "sunzoneid", "1", "onoff", "TRUE"));
			Pattern pattern = new Pattern(type, Map.of("on_wake_up",
				List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))));

			assertTrue(RetailPatternAI2.supports(pattern));
		}
	}

	@Test
	void calculatesRetailSwitchTargetHate() {
		assertEquals(130, RetailPatternAI2.switchHateAddition(1000, 1000, 3, 100));
		assertEquals(1030, RetailPatternAI2.switchHateAddition(1000, 100, 3, 100));
	}

	@Test
	void supportsRetailMessageChainAndShout() {
		Rule rule = new Rule(1, "DIRECT",
			List.of(new Operation("is_message", Map.of("message_type", "1001"))),
			List.of(
				new Operation("say_to_all", Map.of("string_id", "STR_CHAT_TEST")),
				new Operation("broadcast_message", Map.of("message_type", "1002", "param1", "0", "param2", "0",
					"range_as_meter", "50", "param_obj", "OBJI_EVENT_TARGET"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("message_test", Map.of("on_message", List.of(rule)))));

		Rule senderSkill = new Rule(1, "PLANNED",
			List.of(new Operation("is_message", Map.of("message_type", "1001"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_MESSAGE_SENDER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("message_sender_test",
			Map.of("on_message", List.of(senderSkill)))));

		assertTrue(RetailPatternAI2.supports(new Pattern("shout_test", Map.of("on_battle_timer", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(
				new Operation("shout_to_all", Map.of("string_id", "STR_CHAT_TEST")))))))));

		Operation selfMessage = new Operation("send_message", Map.of(
			"target", "OBJI_SELF", "message_type", "1003", "param1", "0", "param2", "0",
			"param_obj", "OBJI_SELF"));
		assertTrue(RetailPatternAI2.supports(new Pattern("self_message_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(selfMessage)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("unsupported_target_message", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(new Operation("send_message", Map.of(
					"target", "OBJI_CUR_TARGET", "message_type", "1003", "param1", "0", "param2", "0",
					"param_obj", "OBJI_SELF")))))))));

		Rule messageEnemy = new Rule(1, "DIRECT",
			List.of(new Operation("is_enemy", Map.of("who", "OBJI_MESSAGE_PARAM"))),
			List.of(new Operation("do_nothing", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("message_enemy",
			Map.of("on_message", List.of(messageEnemy)))));
	}

	@Test
	void supportsRetailWorldSceneStatuses() {
		assertEquals(StageType.PASS_STAGE_1, RetailPatternAI2.retailStageType(101102));
		assertEquals(StageType.START_BONUS_STAGE_2, RetailPatternAI2.retailStageType(102006));
		assertEquals(StageType.START_BONUS_STAGE_6, RetailPatternAI2.retailStageType(306006));
		assertEquals(StageType.PASS_GROUP_STAGE_7, RetailPatternAI2.retailStageType(407105));
		assertEquals(StageType.PASS_GROUP_STAGE_8, RetailPatternAI2.retailStageType(508105));
		assertEquals(StageType.PASS_GROUP_STAGE_9, RetailPatternAI2.retailStageType(609105));
		assertEquals(StageType.START_BONUS_STAGE_9, RetailPatternAI2.retailStageType(609006));
		assertEquals(StageType.PASS_GROUP_STAGE_10, RetailPatternAI2.retailStageType(810105));

		Operation action = new Operation("change_world_scene_status", Map.of("scenestatus", "101102"));
		assertTrue(RetailPatternAI2.supports(new Pattern("scene_test", Map.of("on_die",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("unknown_scene_test", Map.of("on_die",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("change_world_scene_status",
				Map.of("scenestatus", "999999")))))))));

		Pattern worldScene = new Pattern("world_scene", Map.of("on_die",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))));
		assertTrue(RetailPatternAI2.hasWorldSceneConsumer(worldScene, 300300000));
		assertFalse(RetailPatternAI2.hasWorldSceneConsumer(worldScene, 600050000));
	}

	@Test
	void supportsInstantIdleRulesAndFlagReset() {
		Rule rule = new Rule(1, "INSTANT",
			List.of(
				new Operation("set_flag_var", Map.of("flagvar_indicator", "FLAGVARI_ALPHA_1")),
				new Operation("unset_flag_var", Map.of("flagvar_indicator", "FLAGVARI_ALPHA_1"))),
			List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.supports(new Pattern("idle_test", Map.of("on_enter_idle_state", List.of(rule)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("cancel_idle_test", Map.of("on_idle_timer", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("set_idle_timer", Map.of("delay", "0")))))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("immediate_battle_timer_test", Map.of("on_message", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("add_battle_timer",
				Map.of("btimer_indicator", "BTIMERI_INDEX_0", "delay", "0")))))))));
	}

	@Test
	void supportsDamageAndMostHatingEvents() {
		Rule damaged = new Rule(1, "DIRECT", List.of(
			new Operation("is_event_skill_id", Map.of("skill_id", "IDSeal_SealGuard_Bomb")),
			new Operation("is_user", Map.of("obj_indicator", "OBJI_ATTACKER")),
			new Operation("is_hp_in_boundary", Map.of(
				"who", "OBJI_ATTACKER", "larger_than", "35", "less_than", "65"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_ATTACKER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		Rule mostHating = new Rule(1, "INSTANT",
			List.of(new Operation("is_user", Map.of("obj_indicator", "OBJI_CUR_TARGET"))),
			List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.supports(new Pattern("damage_test", Map.of("on_damaged", List.of(damaged)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("hate_test",
			Map.of("on_most_hating_updated", List.of(mostHating)))));
	}

	@Test
	void supportsFriendAttackAndSkillEvents() {
		Rule friendAttacked = new Rule(1, "PLANNED", List.of(
			new Operation("is_user", Map.of("obj_indicator", "OBJI_ATTACKER")),
			new Operation("is_hp_lower_than", Map.of("who", "OBJI_FRIEND", "percent", "50"))),
			List.of(
				new Operation("use_skill", Map.of(
					"target", "OBJI_FRIEND", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
				new Operation("switch_target", Map.of(
					"target", "OBJI_ATTACKER", "percent_to_add", "0", "points_to_add", "100"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_attacked",
			Map.of("on_see_friend_attacked", List.of(friendAttacked)))));
		Rule retaliate = new Rule(1, "DIRECT", List.of(), List.of(new Operation("use_skill", Map.of(
			"target", "OBJI_ATTACKER", "skill", "SKILLI_INDEX_4", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("retaliate_for_friend",
			Map.of("on_see_friend_attacked", List.of(retaliate)))));

		Rule friendAttacking = new Rule(1, "DIRECT",
			List.of(new Operation("is_race", Map.of("from", "OBJI_EVENT_TARGET", "race_type", "pc"))),
			List.of(new Operation("add_hate_point", Map.of("target", "OBJI_EVENT_TARGET", "point_to_add", "100"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_attacking",
			Map.of("on_see_friend_attacking", List.of(friendAttacking)))));

		Rule friendSpelling = new Rule(1, "DIRECT",
			List.of(new Operation("is_event_skill_category", Map.of("skill_category", "SKILLCTG_HEAL"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_FRIEND", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_spelling",
			Map.of("on_friend_spelling", List.of(friendSpelling)))));

		Rule friendSpelled = new Rule(1, "INSTANT", List.of(
			new Operation("is_enemy", Map.of("who", "OBJI_CASTER")),
			new Operation("is_event_skill_category", Map.of("skill_category", "SKILLCTG_PHYSICAL_DEBUFF")),
			new Operation("is_obj_in_abnormal_state", Map.of(
				"obj", "OBJI_FRIEND", "abnormal_state", "ABNSTATEI_PHYSICAL_GROUP"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_CASTER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_spelled",
			Map.of("on_friend_spelled", List.of(friendSpelled)))));
	}

	@Test
	void supportsFriendKilledByUserEvents() {
		Rule seen = new Rule(1, "PLANNED", List.of(
			new Operation("is_user", Map.of("obj_indicator", "OBJI_KILLER")),
			new Operation("is_distance_shorter_than", Map.of("who", "OBJI_KILLER", "distance", "20"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_KILLER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_killed_seen",
			Map.of("on_see_friend_killed_by_user", List.of(seen)))));

		Rule sensed = new Rule(1, "INSTANT", List.of(), List.of(new Operation("flee_from",
			Map.of("from", "OBJI_KILLER", "seconds", "5", "push_state", "TRUE"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("friend_killed_sensed",
			Map.of("on_sense_friend_killed_by_user", List.of(sensed)))));
	}

	@Test
	void supportsFriendEnteringAttackState() {
		Rule rule = new Rule(1, "DIRECT",
			List.of(new Operation("is_npc_state", Map.of("who", "NPCI_SELF", "state", "NPC_STATE_IDLE"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("friend_attack_state",
			Map.of("on_friend_enter_attack_state", List.of(rule)))));
	}

	@Test
	void supportsOnlyRetailNpcPartyEventShapes() {
		Rule attacking = new Rule(1, "INSTANT", List.of(
			new Operation("is_user", Map.of("obj_indicator", "OBJI_EVENT_TARGET"))), List.of(
			new Operation("add_hate_point", Map.of("target", "OBJI_EVENT_TARGET", "point_to_add", "10000"))));
		Rule attacked = new Rule(1, "PLANNED", List.of(
			new Operation("is_hp_lower_than", Map.of("who", "OBJI_PARTY_MEMBER", "percent", "50")),
			new Operation("is_user", Map.of("obj_indicator", "OBJI_ATTACKER"))),
			List.of(new Operation("do_nothing", Map.of())));
		Rule spelled = new Rule(1, "PLANNED", List.of(
			new Operation("is_hp_lower_than", Map.of("who", "OBJI_PARTY_MEMBER", "percent", "50")),
			new Operation("is_user", Map.of("obj_indicator", "OBJI_CASTER"))),
			List.of(new Operation("do_nothing", Map.of())));
		Rule entered = new Rule(1, "PLANNED", List.of(
			new Operation("is_distance_longer_than", Map.of("who", "OBJI_EVENT_TARGET", "distance", "15"))),
			List.of(new Operation("do_nothing", Map.of())));
		Pattern pattern = new Pattern("npc_party", Map.of(
			"on_party_mbr_attacking", List.of(attacking),
			"on_party_mbr_attacked", List.of(attacked),
			"on_party_mbr_spelled", List.of(spelled),
			"on_party_mbr_enter_attack_state", List.of(entered)));

		assertTrue(RetailPatternAI2.supports(pattern));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_party_member", Map.of(
			"on_party_mbr_attacking", List.of(new Rule(1, "INSTANT", List.of(
				new Operation("is_user", Map.of("obj_indicator", "OBJI_PARTY_MEMBER"))),
				List.of(new Operation("do_nothing", Map.of()))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_party_action", Map.of(
			"on_party_mbr_enter_attack_state", List.of(new Rule(1, "INSTANT", List.of(), List.of(
				new Operation("add_hate_point", Map.of("target", "OBJI_EVENT_TARGET", "point_to_add", "1")))))))));
	}

	@Test
	void requiresExplicitNpcPartyForRuntimeSupport() {
		Pattern party = new Pattern("npc_party", Map.of("on_party_mbr_attacking", List.of(
			new Rule(1, "INSTANT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
		Pattern ordinary = new Pattern("ordinary", Map.of("on_wake_up", List.of(
			new Rule(1, "INSTANT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
		PartyNpc npc = new ObjenesisStd().newInstance(PartyNpc.class);

		assertFalse(RetailPatternAI2.hasCompleteNpcPartyData(party, null));
		assertFalse(RetailPatternAI2.hasCompleteNpcPartyData(party, npc));
		npc.partyId = "instance-party-1";
		assertTrue(RetailPatternAI2.hasCompleteNpcPartyData(party, npc));
		assertTrue(RetailPatternAI2.hasCompleteNpcPartyData(ordinary, null));
	}

	@Test
	void supportsRetailNpcPartyMessagesOnlyInRealContexts() {
		Operation selfMessage = new Operation("broadcast_message_to_party", Map.of(
			"message_type", "22373", "param1", "0", "param2", "0", "param_obj", "OBJI_SELF"));
		Operation killerMessage = new Operation("broadcast_message_to_party", Map.of(
			"message_type", "22370", "param1", "0", "param2", "0", "param_obj", "OBJI_KILLER"));
		Operation targetMessage = new Operation("broadcast_message_to_party", Map.of(
			"message_type", "2001", "param1", "0", "param2", "0", "param_obj", "OBJI_CUR_TARGET"));

		assertTrue(RetailPatternAI2.supports(new Pattern("party_message", Map.of(
			"on_attacked", List.of(new Rule(1, "DIRECT", List.of(), List.of(selfMessage))),
			"on_spelled", List.of(new Rule(1, "DIRECT", List.of(), List.of(selfMessage))),
			"on_killed_by_user", List.of(new Rule(1, "DIRECT", List.of(), List.of(killerMessage))),
			"on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(), List.of(targetMessage)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_party_message_event", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(selfMessage)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_party_message_target", Map.of("on_attacked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(killerMessage)))))));
	}

	@Test
	void dispatchesRetailNpcPartyEventContexts() throws ReflectiveOperationException {
		Pattern pattern = new Pattern("npc_party", Map.of(
			"on_party_mbr_attacking", List.of(partyContextRule("OBJI_EVENT_TARGET", null, "FLAGVARI_ALPHA_1")),
			"on_party_mbr_attacked", List.of(partyContextRule("OBJI_ATTACKER", "OBJI_PARTY_MEMBER", "FLAGVARI_ALPHA_2")),
			"on_party_mbr_spelled", List.of(partyContextRule("OBJI_CASTER", "OBJI_PARTY_MEMBER", "FLAGVARI_ALPHA_3")),
			"on_party_mbr_enter_attack_state", List.of(
				partyContextRule("OBJI_EVENT_TARGET", null, "FLAGVARI_BETA_1"))));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		PartyNpc partyMember = objenesis.newInstance(PartyNpc.class);
		Player target = objenesis.newInstance(Player.class);
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);

		ai.handleNpcPartyEvent("on_party_mbr_attacking", target, partyMember, null);
		ai.handleNpcPartyEvent("on_party_mbr_attacked", target, partyMember, null);
		ai.handleNpcPartyEvent("on_party_mbr_spelled", target, partyMember, null);
		ai.handleNpcPartyEvent("on_party_mbr_enter_attack_state", target, partyMember, null);

		assertEquals(Set.of("FLAGVARI_ALPHA_1", "FLAGVARI_ALPHA_2", "FLAGVARI_ALPHA_3", "FLAGVARI_BETA_1"),
			flags(ai));
	}

	@Test
	void supportsCastedAndSeeSpellEvents() {
		Rule casted = new Rule(1, "PLANNED",
			List.of(new Operation("is_user", Map.of("obj_indicator", "OBJI_CASTER"))),
			List.of(new Operation("flee_from", Map.of(
				"from", "OBJI_CASTER", "seconds", "5", "push_state", "TRUE"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("casted", Map.of("on_casted", List.of(casted)))));

		Rule seeSpell = new Rule(1, "DIRECT", List.of(
			new Operation("is_user_class", Map.of("user", "USERI_CASTER", "class", "CLASSI_CLERIC_GROUP")),
			new Operation("is_enemy", Map.of("who", "OBJI_EVENT_TARGET")),
			new Operation("is_event_skill_category", Map.of("skill_category", "SKILLCTG_HEAL"))),
			List.of(
				new Operation("add_hate_point", Map.of("target", "OBJI_CASTER", "point_to_add", "100")),
				new Operation("use_skill", Map.of(
					"target", "OBJI_CASTER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("see_spell", Map.of("on_see_spell", List.of(seeSpell)))));
	}

	@Test
	void supportsRetailSkillLevelOverride() {
		Rule rule = new Rule(1, "DIRECT", List.of(), List.of(
			new Operation("use_skill", Map.of(
				"target", "OBJI_CUR_TARGET", "skill", "SKILLI_INDEX_0", "skill_level", "100")),
			new Operation("use_skill_by_attacker_indicator", Map.of(
				"target", "ATTACKERI_RANDOM_ONE", "skill", "SKILLI_INDEX_0", "skill_level", "25",
				"restricted_range", "FALSE"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("skill_level_override",
			Map.of("on_battle_timer", List.of(rule)))));
		assertEquals(65, RetailPatternAI2.effectiveSkillLevel(65, 0));
		assertEquals(100, RetailPatternAI2.effectiveSkillLevel(65, 100));
	}

	@Test
	void supportsRetailSkillAreaWithBothBroadcastModes() {
		Operation area = new Operation("activate_skillarea", Map.of(
			"areaid", "103", "skill", "SKILLI_INDEX_0", "skill_level", "0", "broadcast_type", "AREA"));
		Operation castor = new Operation("activate_skillarea", Map.of(
			"areaid", "103", "skill", "SKILLI_INDEX_0", "broadcast_type", "CASTOR"));
		assertTrue(RetailPatternAI2.supports(new Pattern("skill_area", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(area)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("skill_area_castor", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(castor)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("skill_area_invalid", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("activate_skillarea", Map.of(
				"areaid", "103", "skill", "SKILLI_INDEX_0", "broadcast_type", "EVERYWHERE")))))))));
	}

	@Test
	void supportsOnlyWellFormedRetailAreaActionsStructurally() {
		Operation enabled = new Operation("enable_area", Map.of("area_type", "AI_CONTROL_AREA_RESURRECT",
			"area_name", "Boss_Resurrectarea", "op_code", "1"));
		Operation disabled = new Operation("enable_area", Map.of("area_type", "AI_CONTROL_AREA_RESURRECT",
			"area_name", "Boss_Resurrectarea", "op_code", "0"));

		assertTrue(RetailPatternAI2.supports(new Pattern("area", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(enabled, disabled)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_area", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("enable_area", Map.of(
				"area_type", "AI_CONTROL_AREA_RESURRECT", "area_name", "Boss_Resurrectarea", "op_code", "2")))))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("quest_area", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("enable_area", Map.of(
				"area_type", "AI_CONTROL_AREA_QUESTSCRIPT", "area_name", "Boss_Area", "op_code", "1")))))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("groupctrl_area", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("enable_area", Map.of(
				"area_type", "AI_CONTROL_AREA_GROUPCTRL", "area_name", "Boss_Area", "op_code", "1")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("unsupported_area", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("enable_area", Map.of(
				"area_type", "AI_CONTROL_AREA_UNKNOWN", "area_name", "Boss_Area", "op_code", "1")))))))));
	}

	@Test
	void supportsOnlyWellFormedRetailWindstreamActions() {
		assertTrue(RetailPatternAI2.supports(new Pattern("windstream", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("on_off_windpath",
				Map.of("groupid", "159", "onoff", "TRUE")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("bad_windstream_state", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("on_off_windpath",
				Map.of("groupid", "159", "onoff", "1")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("bad_windstream_group", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("on_off_windpath",
				Map.of("groupid", "0", "onoff", "FALSE")))))))));
	}

	@Test
	void supportsRetailRawNpcSpeech() {
		Rule rule = new Rule(1, "DIRECT", List.of(),
			List.of(new Operation("say_to_all_str", Map.of("string", "raw npc message"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("raw_speech",
			Map.of("on_battle_timer", List.of(rule)))));
		assertFalse(RetailPatternAI2.supports(new Pattern("empty_raw_speech", Map.of("on_battle_timer", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("say_to_all_str", Map.of("string", "")))))))));
	}

	@Test
	void supportsRetailSpeechToOneUser() {
		Operation seen = new Operation("say",
			Map.of("user", "USERI_SEEN", "string_id", "STR_CHAT_NPC_Robstin_Patterns_01"));
		Operation talker = new Operation("say",
			Map.of("user", "USERI_TALKER", "string_id", "STR_MSG_F6_Event_G1_Po_Time_Start_01"));

		assertTrue(RetailPatternAI2.supports(new Pattern("say_to_seen", Map.of("on_see_user",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(seen)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("say_to_talker", Map.of("on_talked_by_user",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(talker)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("say_to_wrong_user", Map.of("on_see_user",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(talker)))))));
	}

	@Test
	void supportsRetailCoordinateTeleport() {
		Operation npcTeleport = new Operation("teleport_target", Map.of(
			"target", "OBJI_SELF", "x", "294.4", "y", "504.7", "z", "351", "dir", "90",
			"showfx", "FALSE"));
		Operation playerTeleport = new Operation("teleport_target", Map.of(
			"target", "OBJI_TALKER", "x", "227", "y", "264", "z", "315", "dir", "0",
			"showfx", "TRUE"));

		assertTrue(RetailPatternAI2.supports(new Pattern("npc_teleport", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(npcTeleport)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("player_teleport", Map.of("on_talked_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(playerTeleport)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("npc_teleport_ignores_fx", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("teleport_target", Map.of(
				"target", "OBJI_SELF", "x", "1", "y", "2", "z", "3", "dir", "0", "showfx", "TRUE")))))))));
	}

	@Test
	void supportsRetailDirectPortalActions() {
		Operation open = new Operation("open_directportal", Map.of("direct_portal_id", "73"));
		Operation close = new Operation("close_directportal", Map.of("direct_portal_id", "73"));
		Operation userOpen = new Operation("open_directportal_by_user",
			Map.of("requestuser", "USERI_TALKER", "direct_portal_id", "82"));
		assertTrue(RetailPatternAI2.supports(new Pattern("portal", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(open, close)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("user_portal", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(userOpen)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("bad_portal", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("open_directportal",
				Map.of("direct_portal_id", "0")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("bad_user_portal", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(userOpen)))))));
	}

	@Test
	void supportsSeeingUnrelatedAttack() {
		Rule rule = new Rule(1, "DIRECT",
			List.of(new Operation("is_race", Map.of("from", "OBJI_ATTACKER", "race_type", "pc"))),
			List.of(new Operation("broadcast_message", Map.of(
				"message_type", "1001", "param1", "0", "param2", "0", "range_as_meter", "50",
				"param_obj", "OBJI_ATTACKER"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("see_attacked", Map.of("on_see_attacked", List.of(rule)))));
	}

	@Test
	void supportsReturnSpawnPointLifecycle() {
		Rule enter = new Rule(1, "DIRECT", List.of(), List.of(new Operation("despawn",
			Map.of("spawn_id", "SPAWN_ID_1"))));
		Rule leave = new Rule(1, "PLANNED", List.of(), List.of(
			new Operation("despawn", Map.of("spawn_id", "SPAWN_ID_1")),
			new Operation("use_skill", Map.of(
				"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
			new Operation("use_skill", Map.of(
				"target", "OBJI_SELF", "skill", "SKILLI_INDEX_1", "skill_level", "0"))));
		Rule moveAfterLeaveSkill = new Rule(1, "PLANNED", List.of(), List.of(
			new Operation("use_skill", Map.of(
				"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
			new Operation("goto_next_waypoint", Map.of("move_type", "MOVETYPE_RUN"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("return_state", Map.of(
			"on_enter_return_sp", List.of(enter), "on_leave_attack_state", List.of(leave)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("move_after_return_skill",
			Map.of("on_leave_attack_state", List.of(moveAfterLeaveSkill)))));
		assertFalse(RetailPatternAI2.supports(new Pattern("action_after_death_skill",
			Map.of("on_die", List.of(moveAfterLeaveSkill)))));

		Rule forcedReturn = new Rule(1, "DIRECT", List.of(), List.of(
			new Operation("return_to_spawn_point", Map.of()),
			new Operation("reset_queued_actions", Map.of()),
			new Operation("reset_hatepoints", Map.of(
				"is_except_most_hating", "FALSE", "volatile_hatepoint_only", "FALSE"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("forced_return",
			Map.of("on_message", List.of(forcedReturn)))));

		Rule resetAfterSkill = new Rule(1, "PLANNED", List.of(), List.of(
			new Operation("use_skill", Map.of(
				"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
			new Operation("reset_queued_actions", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("reset_after_leave_skill",
			Map.of("on_leave_attack_state", List.of(resetAfterSkill)))));
	}

	@Test
	void supportsEnterAbnormalStatePayload() {
		Rule rule = new Rule(1, "INSTANT",
			List.of(new Operation("is_abnormal_state", Map.of("abnormal_state", "ABNSTATEI_MENTAL_GROUP"))),
			List.of(new Operation("broadcast_message", Map.of("message_type", "1001", "param1", "0", "param2", "0",
				"range_as_meter", "50", "param_obj", "OBJI_SELF"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("abnormal_test",
			Map.of("on_enter_abnormal_state", List.of(rule)))));
		Rule leave = new Rule(1, "DIRECT",
			List.of(new Operation("is_abnormal_state", Map.of("abnormal_state", "ABNSTATEI_ROOT"))),
			List.of(new Operation("switch_target", Map.of(
				"target", "OBJI_CASTER", "percent_to_add", "0", "points_to_add", "100"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("leave_abnormal_test",
			Map.of("on_leave_abnormal_state", List.of(leave)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("end_feared_test", Map.of("on_end_feared",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))))));
		assertTrue(RetailPatternAI2.matchesRetailAbnormal(0x00000008, "ABNSTATEI_MENTAL_GROUP"));
		assertFalse(RetailPatternAI2.matchesRetailAbnormal(0x00000001, "ABNSTATEI_MENTAL_GROUP"));
		assertTrue(RetailPatternAI2.matchesRetailAbnormal(0x00000200, "ABNSTATEI_FEAR"));
		assertFalse(RetailPatternAI2.matchesRetailAbnormal(0x00001000, "ABNSTATEI_FEAR"));
	}

	@Test
	void appliesRetailGenderAndSaturatingSubtraction() {
		assertTrue(RetailPatternAI2.matchesGender(Gender.MALE, "GENDERI_MALE"));
		assertFalse(RetailPatternAI2.matchesGender(Gender.FEMALE, "GENDERI_MALE"));

		Map<String, Integer> values = new HashMap<>(Map.of("phase", 2));
		assertTrue(RetailPatternAI2.subIntVar(values, "phase", 2, 0, 2, true));
		assertEquals(0, values.get("phase"));
		assertFalse(RetailPatternAI2.subIntVar(values, "phase", 2, 0, 2, false));

		Rule rule = new Rule(1, "DIRECT", List.of(
			new Operation("is_user_gender", Map.of("user", "USERI_ATTACKER", "gender", "GENDERI_MALE")),
			new Operation("is_my_curent_target", Map.of("who", "OBJI_ATTACKER")),
			new Operation("is_world_flag_var", Map.of(
				"flagvar_indicator", "FLAGVARI_ALPHA_1", "flag_expected", "TRUE")),
			new Operation("sub_intvar", Map.of("intvar_indicator", "INTVARI_FIRST", "var_to_sub", "2",
				"lower_bound", "0", "upper_bound", "2", "be_true_only_when_hit_the_bound", "TRUE"))),
			List.of(new Operation("do_nothing", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("condition_test", Map.of("on_attacked", List.of(rule)))));
	}

	@Test
	void supportsTimedRetailRandomMovement() {
		Pattern pattern = new Pattern("random_move_test", Map.of(
			"on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(),
				List.of(new Operation("random_move", Map.of("time_to_move", "5000"))))),
			"on_stop_to_random_move", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("add_battle_timer", Map.of("btimer_indicator", "BTIMERI_INDEX_1", "delay", "1")))))));

		assertTrue(RetailPatternAI2.supports(pattern));
	}

	@Test
	void supportsRetailFleeLifecycle() {
		Rule flee = new Rule(1, "DIRECT", List.of(), List.of(new Operation("flee_from",
			Map.of("from", "OBJI_ATTACKER", "seconds", "5", "push_state", "TRUE"))));
		Rule stopped = new Rule(1, "DIRECT", List.of(), List.of(new Operation("use_skill",
			Map.of("target", "OBJI_FLEE_FROM", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));
		Pattern pattern = new Pattern("flee_test", Map.of(
			"on_attacked", List.of(flee), "on_stop_to_flee", List.of(stopped)));

		assertTrue(RetailPatternAI2.supports(pattern));
		Point3D target = RetailPatternAI2.fleePoint(10, 20, 30, 5, (byte) 0, 0);
		float dx = target.getX() - 10;
		float dy = target.getY() - 20;
		assertEquals(25, dx * dx + dy * dy, 0.001);
		assertEquals(30, target.getZ());
	}

	@Test
	void resumesThinkingAfterFleeReturnsToIdle() throws ReflectiveOperationException {
		ObjenesisStd objenesis = new ObjenesisStd();
		SkillNpc owner = objenesis.newInstance(SkillNpc.class);
		owner.setLifeStats(objenesis.newInstance(FixedNpcLifeStats.class));
		setField(Creature.class, owner, "aggroList", new AggroList(owner));
		setField(Creature.class, owner, "moveController", new RecordingNpcMoveController(owner));
		RecordingRetailPatternAI2 ai = new RecordingRetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", new Pattern("flee", Map.of()));
		setField(RetailPatternAI2.class, ai, "fleeMoveTask", new CompletableFuture<>());
		ai.setStateIfNot(AIState.FEAR);

		Method stopFlee = RetailPatternAI2.class.getDeclaredMethod("stopFlee", Creature.class, boolean.class);
		stopFlee.setAccessible(true);
		stopFlee.invoke(ai, owner, false);

		assertEquals(AIState.IDLE, ai.getState());
		assertEquals(1, ai.thinkCalls);
	}

	@Test
	void supportsRetailWaypointMovementAndArrivalConditions() {
		Pattern pattern = new Pattern("waypoint_test", Map.of(
			"on_wake_up", List.of(new Rule(2, "PLANNED", List.of(), List.of(new Operation("goto_waypoint",
				Map.of("waypoint", "0", "move_type", "MOVETYPE_RUN"))))),
			"on_arrived_at_waypoint", List.of(new Rule(1, "DIRECT", List.of(
				new Operation("is_waypoint_index", Map.of("index", "1")),
				new Operation("is_last_waypoint", Map.of())), List.of(new Operation("goto_next_waypoint",
				Map.of("move_type", "MOVETYPE_WALK")))))));

		assertTrue(RetailPatternAI2.supports(pattern));
		assertTrue(RetailPatternAI2.supports(new Pattern("point_test", Map.of("on_arrived_at_point",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("despawn_self", Map.of()))))))));
		assertEquals(0, RetailPatternAI2.nextWaypointIndex(-1, 3));
		assertEquals(2, RetailPatternAI2.nextWaypointIndex(1, 3));
		assertEquals(0, RetailPatternAI2.nextWaypointIndex(2, 3));
		assertEquals(-1, RetailPatternAI2.nextWaypointIndex(0, 0));
		assertFalse(RetailPatternAI2.supports(new Pattern("bad_move_type", Map.of("on_wake_up", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("goto_next_waypoint",
				Map.of("move_type", "MOVETYPE_UNKNOWN")))))))));
	}

	@Test
	void supportsFullHateResetAndKeepingMostHated() {
		for (String exceptMostHating : List.of("TRUE", "FALSE")) {
			for (String volatileOnly : List.of("TRUE", "FALSE")) {
				Rule rule = new Rule(1, "DIRECT", List.of(), List.of(new Operation("reset_hatepoints",
					Map.of("is_except_most_hating", exceptMostHating, "volatile_hatepoint_only", volatileOnly))));

				assertTrue(RetailPatternAI2.supports(new Pattern("hate_test", Map.of("on_attacked", List.of(rule)))));
			}
		}
	}

	@Test
	void supportsRetailObjectHateActions() {
		Rule message = new Rule(1, "DIRECT", List.of(), List.of(
			new Operation("add_hate_point", Map.of("target", "OBJI_MESSAGE_PARAM", "point_to_add", "100")),
			new Operation("attack_most_hating", Map.of("skill", "SKILLI_NONE", "skill_level", "0"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("hate_message_test", Map.of("on_message", List.of(message)))));

		Operation target = new Operation("switch_target", Map.of(
			"target", "OBJI_CUR_TARGET", "percent_to_add", "10000000", "points_to_add", "100"));
		assertTrue(RetailPatternAI2.supports(new Pattern("switch_target_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(target)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("attack_skill_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("attack_most_hating",
				Map.of("skill", "SKILLI_INDEX_0", "skill_level", "0")))))))));

		Operation classTarget = new Operation("switch_target_by_class_indicator", Map.of(
			"target_class", "CLASSI_CASTER_GROUP", "percent_to_add", "5", "points_to_add", "100",
			"restricted_range", "FALSE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("class_target_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(classTarget)))))));
	}

	@Test
	void rejectsRetailNoneSkillActions() {
		Operation noSkill = new Operation("use_skill", Map.of(
			"target", "OBJI_SELF", "skill", "SKILLI_NONE", "skill_level", "0"));
		Operation noAttackerSkill = new Operation("use_skill_by_attacker_indicator", Map.of(
			"target", "ATTACKERI_RANDOM_ONE", "skill", "SKILLI_NONE", "skill_level", "0", "restricted_range", "FALSE"));

		assertFalse(RetailPatternAI2.supports(new Pattern("none_skill", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(noSkill, noAttackerSkill,
				new Operation("do_nothing", Map.of()))))))));
	}

	@Test
	void supportsAdditionalRetailEventTargets() {
		Operation timerSkill = new Operation("use_skill", Map.of(
			"target", "OBJI_EVENT_TARGET", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		assertTrue(RetailPatternAI2.supports(new Pattern("timer_target", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(timerSkill)))))));

		Operation switchTarget = new Operation("switch_target_by_attacker_indicator", Map.of(
			"target", "ATTACKERI_RANDOM_ONE", "percent_to_add", "10000000", "points_to_add", "100",
			"restricted_range", "FALSE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("large_hate_percent", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(switchTarget)))))));

		Operation flying = new Operation("is_user_flying", Map.of("user", "USERI_EVENT_MAKER"));
		assertTrue(RetailPatternAI2.supports(new Pattern("sensory_user", Map.of("on_user_enter_sensory_area",
			List.of(new Rule(1, "INSTANT", List.of(flying), List.of(new Operation("do_nothing", Map.of()))))))));

		Operation casterSkill = new Operation("use_skill", Map.of(
			"target", "OBJI_CASTER", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		assertTrue(RetailPatternAI2.supports(new Pattern("attacked_caster", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(), List.of(casterSkill)))))));
	}

	@Test
	void supportsOnlyExactTerminalSkillCleanupSequences() {
		Operation skill = new Operation("use_skill", Map.of(
			"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		Operation despawn = new Operation("despawn", Map.of("spawn_id", "SPAWN_ID_1"));
		List<Operation> cleanup = List.of(skill, despawn);

		for (String event : List.of("on_despawn", "on_killed_by_user")) {
			assertTrue(RetailPatternAI2.supports(new Pattern("terminal_cleanup", Map.of(event,
				List.of(new Rule(1, "DIRECT", List.of(), cleanup))))));
		}
		assertFalse(RetailPatternAI2.supports(new Pattern("extra_terminal_action", Map.of("on_despawn",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(skill, despawn, new Operation("do_nothing", Map.of()))))))));
		Operation otherTarget = new Operation("use_skill", Map.of(
			"target", "OBJI_CUR_TARGET", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		assertFalse(RetailPatternAI2.supports(new Pattern("other_terminal_target", Map.of("on_despawn",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(otherTarget, despawn)))))));
		Operation otherSpawn = new Operation("despawn", Map.of("spawn_id", "SPAWN_ID_2"));
		assertFalse(RetailPatternAI2.supports(new Pattern("other_terminal_spawn", Map.of("on_despawn",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(skill, otherSpawn)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("other_terminal_event", Map.of("on_die",
			List.of(new Rule(1, "DIRECT", List.of(), cleanup))))));
	}

	@Test
	void executesTerminalSkillCleanupWithoutQueuingFuture() throws ReflectiveOperationException {
		Operation skill = new Operation("use_skill", Map.of(
			"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		Operation despawn = new Operation("despawn", Map.of("spawn_id", "SPAWN_ID_1"));
		Pattern pattern = new Pattern("terminal_cleanup", Map.of("on_despawn",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(skill, despawn)))));
		ObjenesisStd objenesis = new ObjenesisStd();
		SkillNpc owner = objenesis.newInstance(SkillNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		owner.controller = new RecordingNpcController();
		owner.controller.setOwner(owner);
		owner.skillList = objenesis.newInstance(NpcSkillList.class);
		setField(NpcSkillList.class, owner.skillList, "skills", List.of(new TestNpcSkillEntry(900001, 1)));
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern", pattern);
		RecordingVisibleObjectController cleanupController = new RecordingVisibleObjectController();
		RecordingVisibleObject cleanupTarget = new RecordingVisibleObject(cleanupController);
		spawned(ai).put("SPAWN_ID_1", new ArrayList<>(List.of(cleanupTarget)));
		SkillTemplate template = new SkillTemplate();
		setField(SkillTemplate.class, template, "skillId", 900001);
		setField(SkillTemplate.class, template, "duration", 1000);
		SkillData previous = DataManager.SKILL_DATA;
		DataManager.SKILL_DATA = new SkillData();
		DataManager.SKILL_DATA.setSkillTemplates(List.of(template));

		boolean noQueuedFuture;
		try {
			invokeEvent(ai, "on_despawn");
			noQueuedFuture = actionTasks(ai).isEmpty();
			resetPatternState(ai);
		} finally {
			DataManager.SKILL_DATA = previous;
		}

		assertEquals(1, owner.controller.skillUses);
		assertEquals(1, cleanupController.deletes);
		assertTrue(noQueuedFuture);
		assertTrue(spawned(ai).isEmpty());
	}

	@Test
	void supportsSpawningOnSelectedAttacker() {
		Operation spawn = new Operation("spawn_on_target_by_attacker_indicator", Map.ofEntries(
			Map.entry("target", "ATTACKERI_RANDOM_ONE"), Map.entry("spawn_id", "SPAWN_ID_1"),
			Map.entry("npc_nameid", "TEST_HELPER"), Map.entry("num_to_spawn", "1"),
			Map.entry("spawn_range", "0"), Map.entry("live_time", "15"),
			Map.entry("despawn_at_attack_state", "TRUE"), Map.entry("valid_distance", "50"),
			Map.entry("attack_target_after_spawn", "FALSE"), Map.entry("hatepoints_to_add", "0"),
			Map.entry("restricted_range", "FALSE")));

		assertTrue(RetailPatternAI2.supports(new Pattern("spawn_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(), List.of(spawn)))))));

		Operation selfSpawn = new Operation("spawn_on_target", Map.ofEntries(
			Map.entry("target_obj", "OBJI_SELF"), Map.entry("spawn_id", "SPAWN_ID_1"),
			Map.entry("npc_nameid", "TEST_HELPER"), Map.entry("num_to_spawn", "1"),
			Map.entry("spawn_range", "0"), Map.entry("live_time", "15"),
			Map.entry("despawn_at_attack_state", "TRUE"), Map.entry("valid_distance", "50"),
			Map.entry("attack_target_after_spawn", "FALSE"), Map.entry("hatepoints_to_add", "0")));
		assertTrue(RetailPatternAI2.supports(new Pattern("self_spawn_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(), List.of(selfSpawn)))))));

		for (Map.Entry<String, String> target : Map.of(
			"on_enter_attack_state", "OBJI_EVENT_TARGET",
			"on_killed_by_user", "OBJI_KILLER",
			"on_message", "OBJI_MESSAGE_PARAM",
			"on_attacked", "OBJI_ATTACKER",
			"on_see_user", "OBJI_SEEN").entrySet()) {
			Map<String, String> fields = new HashMap<>(selfSpawn.values());
			fields.put("target_obj", target.getValue());
			assertTrue(RetailPatternAI2.supports(new Pattern("context_spawn_test", Map.of(target.getKey(),
				List.of(new Rule(1, "PLANNED", List.of(), List.of(new Operation("spawn_on_target", fields))))))));
		}
	}

	@Test
	void supportsRelativeAndWaypointStartSpawns() {
		assertEquals("retail:300170000:test_path", RetailPatternAI2.retailWalkerId(300170000, "TEST_PATH"));
		Operation relative = spawnAction("SPAWN_LOCATION_RELATIVE", "", "10", "-5", "2");

		assertTrue(RetailPatternAI2.supports(new Pattern("relative_spawn_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(), List.of(relative)))))));
		assertEquals(new Point3D(110, 195, 302), RetailPatternAI2.resolveSpawnPoint(relative, 100, 200, 300, null));

		WalkerTemplate walker = new WalkerTemplate("TEST_PATH");
		walker.setRouteSteps(new ArrayList<>(List.of(new RouteStep(501, 502, 503, 0),
			new RouteStep(511, 512, 513, 0))));
		Operation waypoint = spawnAction("SPAWN_LOCATION_WAY_POINT_START", "TEST_PATH", "0", "0", "0");

		assertTrue(RetailPatternAI2.supports(new Pattern("waypoint_spawn_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(), List.of(waypoint)))))));
		assertEquals(new Point3D(501, 502, 503), RetailPatternAI2.resolveSpawnPoint(waypoint, 100, 200, 300, walker));
		assertNull(RetailPatternAI2.resolveSpawnPoint(waypoint, 100, 200, 300, null));
		assertFalse(RetailPatternAI2.supports(new Pattern("missing_waypoint_path_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(),
				List.of(spawnAction("SPAWN_LOCATION_WAY_POINT_START", "", "0", "0", "0"))))))));
	}

	@Test
	void supportsRetailSpawnLifecycleFields() {
		Operation action = spawnAction("SPAWN_LOCATION_MY_POINT", "", "0", "0", "0");
		Map<String, String> fields = new HashMap<>(action.values());
		fields.put("despawn_at_attack_state", "FALSE");
		fields.put("is_aerial_spawn", "TRUE");
		fields.put("except_specialize", "1");

		assertTrue(RetailPatternAI2.supports(new Pattern("spawn_lifecycle_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(new Operation("spawn", fields))))))));
		assertTrue(RetailPatternAI2.shouldDelayLifecycleDespawn(false, true));
		assertFalse(RetailPatternAI2.shouldDelayLifecycleDespawn(false, false));
		assertFalse(RetailPatternAI2.shouldDelayLifecycleDespawn(true, true));
	}

	private static Operation spawnAction(String locationType, String path, String x, String y, String z) {
		return new Operation("spawn", Map.ofEntries(
			Map.entry("spawn_id", "SPAWN_ID_1"), Map.entry("npc_nameid", "TEST_HELPER"),
			Map.entry("num_to_spawn", "1"), Map.entry("spawn_location_type", locationType),
			Map.entry("x", x), Map.entry("y", y), Map.entry("z", z), Map.entry("dir", "0"),
			Map.entry("spawn_range", "0"), Map.entry("live_time", "15"),
			Map.entry("despawn_at_attack_state", "TRUE"), Map.entry("is_aerial_spawn", "FALSE"),
			Map.entry("except_specialize", "0"), Map.entry("pathname", path)));
	}

	@Test
	void supportsAndOrdersMultiTargetSpawns() {
		Operation spawn = new Operation("spawn_on_multi_target", Map.ofEntries(
			Map.entry("spawn_id", "SPAWN_ID_1"), Map.entry("npc_nameid", "TEST_HELPER"),
			Map.entry("num_to_spawn", "2"), Map.entry("spawn_range", "5"), Map.entry("live_time", "15"),
			Map.entry("despawn_at_attack_state", "TRUE"), Map.entry("order_in_attacker_list", "ORDERI_DESCENDING"),
			Map.entry("total_set_to_spawn", "2"), Map.entry("valid_distance", "50"),
			Map.entry("attack_target_after_spawn", "TRUE"), Map.entry("hatepoints_to_add", "100")));

		assertTrue(RetailPatternAI2.supports(new Pattern("multi_spawn_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "PLANNED", List.of(), List.of(spawn)))))));
		assertEquals(List.of(30, 20), RetailPatternAI2.selectMultiTargets(
			List.of(10, 30, 20), Comparator.naturalOrder(), "ORDERI_DESCENDING", 2));
		assertEquals(List.of(10, 20), RetailPatternAI2.selectMultiTargets(
			List.of(10, 30, 20), Comparator.naturalOrder(), "ORDERI_ASCENDING", 2));
		List<Integer> random = RetailPatternAI2.selectMultiTargets(
			List.of(10, 30, 20), Comparator.naturalOrder(), "ORDERI_RANDOM", 2);
		assertEquals(2, random.size());
		assertTrue(Set.of(10, 20, 30).containsAll(random));
	}

	@Test
	void supportsOpeningAndClosingDoors() {
		for (String method : List.of("0", "1", "2")) {
			Rule rule = new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("control_door", Map.of("id", "6", "method", method))));

			assertTrue(RetailPatternAI2.supports(new Pattern("door_test", Map.of("on_die", List.of(rule)))));
		}
		for (String method : List.of("-1", "3")) {
			Rule rule = new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("control_door", Map.of("id", "6", "method", method))));
			assertFalse(RetailPatternAI2.supports(new Pattern("invalid_door_test", Map.of("on_die", List.of(rule)))));
		}
	}

	@Test
	void supportsRetailCutscenesAndDeferredTeleport() throws ReflectiveOperationException {
		Operation cutscene = new Operation("play_cutscene_by_user_indicator", Map.of(
			"target", "USERI_KILLER", "cutscene_id", "914", "quest_id", "0",
			"play_target_type", "CUTSCENE_PLAY_TO_ALLIANCE", "teleport_alias", ""));
		assertTrue(RetailPatternAI2.supports(new Pattern("cutscene_test", Map.of("on_killed_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(cutscene)))))));
		Operation sensoryAllianceCutscene = new Operation("play_cutscene_by_user_indicator", Map.of(
			"target", "USERI_EVENT_MAKER", "cutscene_id", "194", "quest_id", "0",
			"play_target_type", "CUTSCENE_PLAY_TO_ALLIANCE", "teleport_alias", ""));
		assertTrue(RetailPatternAI2.supports(new Pattern("sensory_alliance_cutscene", Map.of("on_user_enter_sensory_area",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(sensoryAllianceCutscene)))))));

		Operation teleporting = new Operation("play_cutscene_by_user_indicator", Map.of(
			"target", "USERI_TALKER", "cutscene_id", "914", "quest_id", "0",
			"play_target_type", "CUTSCENE_PLAY_TO_USER", "teleport_alias", "AFTER_MOVIE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("cutscene_teleport_test", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(teleporting)))))));
		Operation invalidGroupTeleport = new Operation("play_cutscene_by_user_indicator", Map.of(
			"target", "USERI_TALKER", "cutscene_id", "914", "quest_id", "0",
			"play_target_type", "CUTSCENE_PLAY_TO_PARTY", "teleport_alias", "AFTER_MOVIE"));
		assertFalse(RetailPatternAI2.supports(new Pattern("group_cutscene_teleport_test", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(invalidGroupTeleport)))))));

		RetailPatternAI2 ai = new RetailPatternAI2();
		Map<Integer, Object> pending = pendingCutsceneTeleports(ai);
		Class<?> pendingType = Class.forName("com.aionemu.gameserver.ai.RetailPatternAI2$PendingCutsceneTeleport");
		var constructor = pendingType.getDeclaredConstructor(int.class, String.class);
		constructor.setAccessible(true);
		pending.put(42, constructor.newInstance(914, "AFTER_MOVIE"));
		assertNull(consumePendingCutsceneTeleport(ai, 42, 913));
		assertEquals(1, pending.size());
		assertEquals("AFTER_MOVIE", consumePendingCutsceneTeleport(ai, 42, 914));
		assertTrue(pending.isEmpty());

		Operation messageMaster = new Operation("play_cutscene_by_user_indicator", Map.of(
			"target", "USERI_MASTER", "cutscene_id", "435", "quest_id", "0",
			"play_target_type", "CUTSCENE_PLAY_TO_USER", "teleport_alias", ""));
		assertTrue(RetailPatternAI2.supports(new Pattern("message_master_cutscene", Map.of("on_message",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(messageMaster)))))));
		Pattern masterPattern = new Pattern("message_master_cutscene", Map.of("on_message",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(messageMaster)))));
		MasterNpc npc = new ObjenesisStd().newInstance(MasterNpc.class);
		assertFalse(RetailPatternAI2.hasCompleteMasterData(masterPattern, npc));
		npc.master = new ObjenesisStd().newInstance(Player.class);
		assertTrue(RetailPatternAI2.hasCompleteMasterData(masterPattern, npc));
	}

	@Test
	void requiresDeferredCutsceneAliasInNpcWorld() {
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		try {
			System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
			DataManager.RETAIL_AI_DATA = new XmlDataLoader().loadRetailAiData();
			SkillNpc npc = new ObjenesisStd().newInstance(SkillNpc.class);
			npc.npcId = 806731;
			npc.worldId = 310160000;
			npc.objectTemplate = new ObjenesisStd().newInstance(NpcTemplate.class);

			assertTrue(RetailPatternAI2.supports(DataManager.RETAIL_AI_DATA.getPattern(806731), npc));
			Operation missingAlias = new Operation("play_cutscene_by_user_indicator", Map.of(
				"target", "USERI_TALKER", "cutscene_id", "968", "quest_id", "0",
				"play_target_type", "CUTSCENE_PLAY_TO_USER", "teleport_alias", "MISSING_ALIAS"));
			Pattern incomplete = new Pattern("missing_cutscene_alias", Map.of("on_hyperlink_clicked",
				List.of(new Rule(1, "DIRECT", List.of(), List.of(missingAlias)))));
			assertTrue(RetailPatternAI2.supports(incomplete));
			assertFalse(RetailPatternAI2.supports(incomplete, npc));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
			if (previousDefinitions == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previousDefinitions);
			}
		}
	}

	@Test
	void supportsBoundedDespawnByNpcName() {
		Operation despawn = new Operation("despawn_by_nameid", Map.of(
			"target_npc_nameid", "TEST_HELPER", "bound_radius", "50", "max_count", "10"));

		assertTrue(RetailPatternAI2.supports(new Pattern("despawn_by_name_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(), List.of(despawn)))))));
	}

	@Test
	void supportsSpelledCasterAndEventSkill() {
		Rule rule = new Rule(1, "INSTANT", List.of(
			new Operation("is_user", Map.of("obj_indicator", "OBJI_CASTER")),
			new Operation("is_enemy", Map.of("who", "OBJI_CASTER")),
			new Operation("is_event_skill_id", Map.of("skill_id", "MA_FlameBolt_G1"))),
			List.of(new Operation("use_skill", Map.of(
				"target", "OBJI_CASTER", "skill", "SKILLI_INDEX_0", "skill_level", "0"))));

		assertTrue(RetailPatternAI2.supports(new Pattern("spelled_test", Map.of("on_spelled", List.of(rule)))));
		assertFalse(RetailPatternAI2.supports(new Pattern("attacked_test", Map.of("on_attacked", List.of(rule)))));

		Operation attackerSkill = new Operation("use_skill", Map.of(
			"target", "OBJI_ATTACKER", "skill", "SKILLI_INDEX_0", "skill_level", "0"));
		assertTrue(RetailPatternAI2.supports(new Pattern("attacker_skill_test", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(), List.of(attackerSkill)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_attacker_event_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "INSTANT", List.of(), List.of(attackerSkill)))))));
	}

	@Test
	void supportsAndExecutesAnyRetailNpcSkill() throws ReflectiveOperationException {
		Operation anySkill = new Operation("use_skill", Map.of(
			"target", "OBJI_SELF", "skill", "SKILLI_ANY_SKILL", "skill_level", "0"));
		Operation attackerAnySkill = new Operation("use_skill_by_attacker_indicator", Map.of(
			"target", "ATTACKERI_RANDOM_ONE", "restricted_range", "FALSE",
			"skill", "SKILLI_ANY_SKILL", "skill_level", "0"));
		assertTrue(RetailPatternAI2.supports(new Pattern("any_skill", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(anySkill)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("attacker_any_skill", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(attackerAnySkill)))))));
		Operation areaAnySkill = new Operation("activate_skillarea", Map.of(
			"areaid", "1", "skill", "SKILLI_ANY_SKILL", "skill_level", "0", "broadcast_type", "AREA"));
		assertFalse(RetailPatternAI2.supports(new Pattern("area_any_skill", Map.of("on_battle_timer",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(areaAnySkill)))))));

		ObjenesisStd objenesis = new ObjenesisStd();
		SkillNpc owner = objenesis.newInstance(SkillNpc.class);
		owner.controller = new RecordingNpcController();
		owner.controller.setOwner(owner);
		owner.skillList = objenesis.newInstance(NpcSkillList.class);
		setField(NpcSkillList.class, owner.skillList, "skills",
			List.of(new TestNpcSkillEntry(900001, 1), new TestNpcSkillEntry(900002, 2)));
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		SkillTemplate first = new SkillTemplate();
		setField(SkillTemplate.class, first, "skillId", 900001);
		SkillTemplate second = new SkillTemplate();
		setField(SkillTemplate.class, second, "skillId", 900002);
		SkillData previous = DataManager.SKILL_DATA;
		DataManager.SKILL_DATA = new SkillData();
		DataManager.SKILL_DATA.setSkillTemplates(List.of(first, second));
		try {
			invokeSkill(ai, anySkill);
		} finally {
			DataManager.SKILL_DATA = previous;
		}
		assertTrue(Set.of(900001, 900002).contains(owner.controller.lastSkillId));
		assertEquals(owner.controller.lastSkillId == 900001 ? 1 : 2, owner.controller.lastSkillLevel);
	}

	@Test
	void supportsRetailKillerTypeConditions() {
		Rule userKiller = new Rule(1, "DIRECT",
			List.of(new Operation("is_user", Map.of("obj_indicator", "OBJI_KILLER"))),
			List.of(new Operation("do_nothing", Map.of())));
		Rule npcKiller = new Rule(1, "DIRECT",
			List.of(new Operation("is_npc", Map.of("obj_indicator", "OBJI_KILLER"))),
			List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.supports(new Pattern("user_killer_test",
			Map.of("on_killed_by_user", List.of(userKiller)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("npc_killer_test",
			Map.of("on_killed_by_npc", List.of(npcKiller)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("death_killer_test",
			Map.of("on_die", List.of(userKiller)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("death_npc_killer_test",
			Map.of("on_die", List.of(npcKiller)))));
		assertFalse(RetailPatternAI2.supports(new Pattern("missing_killer_test",
			Map.of("on_wake_up", List.of(npcKiller)))));
	}

	@Test
	void keepsKillerForRetailDeathEvent() throws ReflectiveOperationException {
		Rule rule = new Rule(1, "DIRECT", List.of(
			new Operation("is_npc", Map.of("obj_indicator", "OBJI_KILLER")),
			new Operation("set_flag_var", Map.of("flagvar_indicator", "FLAGVARI_ALPHA_1"))),
			List.of(new Operation("do_nothing", Map.of())));
		ObjenesisStd objenesis = new ObjenesisStd();
		MasterNpc owner = objenesis.newInstance(MasterNpc.class);
		owner.setLifeStats(objenesis.newInstance(NpcLifeStats.class));
		RetailPatternAI2 ai = new RetailPatternAI2();
		setField(AbstractAI.class, ai, "owner", owner);
		setField(RetailPatternAI2.class, ai, "pattern",
			new Pattern("death_killer", Map.of("on_die", List.of(rule))));
		MasterNpc killer = objenesis.newInstance(MasterNpc.class);
		killer.master = objenesis.newInstance(ExperiencePlayer.class);
		ai.handleKilled(killer);
		Method runDeathEvent = RetailPatternAI2.class.getDeclaredMethod("runDeathEvent");
		runDeathEvent.setAccessible(true);
		runDeathEvent.invoke(ai);

		assertEquals(Set.of("FLAGVARI_ALPHA_1"), flags(ai));
		Field deathKiller = RetailPatternAI2.class.getDeclaredField("deathKiller");
		deathKiller.setAccessible(true);
		assertNull(deathKiller.get(ai));
	}

	@Test
	void supportsAreaSystemMessages() {
		Operation message = new Operation("display_system_message", Map.of(
			"string_id", "STR_MSG_TEST", "area_name", "TEST_AREA",
			"string_param1", "", "string_param2", "", "string_param3", ""));

		assertTrue(RetailPatternAI2.supports(new Pattern("message_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(), List.of(message)))))));
		Operation globalMessage = new Operation("send_system_msg", Map.of("string_id", "STR_MSG_TEST"));
		assertTrue(RetailPatternAI2.supports(new Pattern("global_message_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(), List.of(globalMessage)))))));
	}

	@Test
	void supportsFlyingEventUsers() {
		Map<String, String> events = Map.of(
			"on_attacked", "USERI_ATTACKER",
			"on_spelled", "USERI_CASTER",
			"on_enter_attack_state", "USERI_ATTACKER",
			"on_battle_timer", "USERI_ATTACKER");
		for (Map.Entry<String, String> event : events.entrySet()) {
			Rule rule = new Rule(1, "DIRECT",
				List.of(new Operation("is_user_flying", Map.of("user", event.getValue()))),
				List.of(new Operation("do_nothing", Map.of())));
			assertTrue(RetailPatternAI2.supports(new Pattern("flying_test", Map.of(event.getKey(), List.of(rule)))));
		}
	}

	@Test
	void supportsConfirmedRetailUserClassesOnlyWithMatchingEventUsers() {
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.TEMPLAR, "CLASSI_KNIGHT"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.SPIRIT_MASTER, "CLASSI_ELEMENTALIST"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.AETHERTECH, "CLASSI_RIDER"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.PRIEST, "CLASSI_CLERIC"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.CLERIC, "CLASSI_PRIEST"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.AETHERTECH, "CLASSI_MELEE_GROUP"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.SONGWEAVER, "CLASSI_HEALER_GROUP"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.WARRIOR, "CLASSI_JUNIOR_GROUP"));
		assertTrue(RetailPatternAI2.matchesUserClass(PlayerClass.GLADIATOR, "CLASSI_SENIOR_GROUP"));
		assertFalse(RetailPatternAI2.matchesUserClass(PlayerClass.GLADIATOR, "CLASSI_KNIGHT"));
		assertFalse(RetailPatternAI2.matchesUserClass(PlayerClass.MAGE, "CLASSI_MELEE_GROUP"));

		Operation attackerClass = new Operation("is_user_class",
			Map.of("user", "USERI_ATTACKER", "class", "CLASSI_KNIGHT"));
		assertTrue(RetailPatternAI2.supports(new Pattern("attacker_class_test", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(attackerClass), List.of(new Operation("do_nothing", Map.of()))))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("timer_class_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "INSTANT", List.of(attackerClass), List.of(new Operation("do_nothing", Map.of()))))))));

		Operation classGroup = new Operation("is_user_class",
			Map.of("user", "USERI_ATTACKER", "class", "CLASSI_MELEE_GROUP"));
		assertTrue(RetailPatternAI2.supports(new Pattern("class_group_test", Map.of("on_attacked",
			List.of(new Rule(1, "INSTANT", List.of(classGroup), List.of(new Operation("do_nothing", Map.of()))))))));
	}

	@Test
	void supportsOwnerRace() {
		Rule rule = new Rule(1, "DIRECT",
			List.of(new Operation("is_race", Map.of("from", "OBJI_SELF", "race_type", "magicalmonster"))),
			List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.supports(new Pattern("race_test", Map.of("on_wake_up", List.of(rule)))));
		assertTrue(RetailPatternAI2.matchesRace(Race.MAGICALMONSTER, "magicalmonster"));
		assertTrue(RetailPatternAI2.matchesRace(Race.ELYOS, "pc"));
		assertFalse(RetailPatternAI2.matchesRace(Race.ASMODIANS, "pc_light"));

		Rule targetRule = new Rule(1, "DIRECT", List.of(
			new Operation("is_race", Map.of("from", "OBJI_CUR_TARGET", "race_type", "pc")),
			new Operation("is_distance_longer_than", Map.of("who", "OBJI_CUR_TARGET", "distance", "5")),
			new Operation("is_distance_shorter_than", Map.of("who", "OBJI_CUR_TARGET", "distance", "15"))),
			List.of(new Operation("do_nothing", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("target_test",
			Map.of("on_battle_timer", List.of(targetRule)))));
		assertTrue(RetailPatternAI2.matchesRetailDistance(100, 5, false));
		assertTrue(RetailPatternAI2.matchesRetailDistance(100, 15, true));
		assertFalse(RetailPatternAI2.matchesRetailDistance(25, 5, false));
	}

	@Test
	void supportsSeenNpcAndMovementEvents() {
		Operation seen = new Operation("is_npc", Map.of("obj_indicator", "OBJI_SEEN"));
		Rule npcRule = new Rule(1, "DIRECT", List.of(seen), List.of(new Operation("do_nothing", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("see_npc", Map.of("on_see_npc", List.of(npcRule)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("see_npc_move", Map.of("on_see_npc_move", List.of(npcRule)))));
		Operation userLevel = new Operation("is_user_level",
			Map.of("user", "USERI_SEEN", "level_min", "1", "level_max", "80"));
		assertTrue(RetailPatternAI2.supports(new Pattern("see_user_move", Map.of("on_see_user_move", List.of(
			new Rule(1, "DIRECT", List.of(userLevel), List.of(new Operation("do_nothing", Map.of()))))))));
		Rule seenUser = new Rule(1, "DIRECT", List.of(
			new Operation("is_enemy", Map.of("who", "OBJI_SEEN")),
			new Operation("is_user_flying", Map.of("user", "USERI_SEEN"))),
			List.of(
				new Operation("use_skill", Map.of(
					"target", "OBJI_SEEN", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
				new Operation("broadcast_message", Map.of(
					"message_type", "1000400", "param1", "0", "param2", "0", "range_as_meter", "100",
					"param_obj", "OBJI_SEEN"))));
		assertTrue(RetailPatternAI2.supports(new Pattern("flying_seen_user",
			Map.of("on_see_user", List.of(seenUser)))));
	}

	@Test
	void supportsRetailSkillAvailabilityAndSpelledCategory() {
		Rule rule = new Rule(1, "DIRECT", List.of(
			new Operation("is_skill_count_left", Map.of("skill", "SKILLI_INDEX_1")),
			new Operation("is_event_skill_category", Map.of("skill_category", "SKILLCTG_CHAIN_SKILL"))),
			List.of(new Operation("do_nothing", Map.of())));

		assertTrue(RetailPatternAI2.supports(new Pattern("spelled_skill_category",
			Map.of("on_spelled", List.of(rule)))));
		assertTrue(RetailPatternAI2.supports(new Pattern("healed_by_user", Map.of("on_healed_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))))));
	}

	@Test
	void incrementsBoundedIntegerVariables() {
		Map<String, Integer> variables = new HashMap<>();

		assertFalse(RetailPatternAI2.increaseIntVar(variables, "FIRST", 0, 3, true));
		assertFalse(RetailPatternAI2.increaseIntVar(variables, "FIRST", 0, 3, true));
		assertTrue(RetailPatternAI2.increaseIntVar(variables, "FIRST", 0, 3, true));
		assertFalse(RetailPatternAI2.increaseIntVar(variables, "FIRST", 0, 3, true));
		assertTrue(RetailPatternAI2.increaseIntVar(variables, "SECOND", 0, 2, false));
		variables.put("THIRD", 5);
		assertTrue(RetailPatternAI2.setIntVar(variables, "THIRD", 1, 3, true));
		assertFalse(RetailPatternAI2.setIntVar(variables, "THIRD", 9, 3, true));
		variables.put("FOURTH", 2);
		assertFalse(RetailPatternAI2.decreaseIntVar(variables, "FOURTH", 0, 3, true));
		assertTrue(RetailPatternAI2.decreaseIntVar(variables, "FOURTH", 0, 3, true));
		assertFalse(RetailPatternAI2.addIntVar(variables, "FIFTH", 2, 0, 3, true));
		assertTrue(RetailPatternAI2.addIntVar(variables, "FIFTH", 2, 0, 3, true));
		assertEquals(3, variables.get("FIFTH"));
		assertTrue(RetailPatternAI2.matchesNpcState(AIState.IDLE, AISubState.NONE, "NPC_STATE_IDLE"));
		assertTrue(RetailPatternAI2.matchesNpcState(AIState.WALKING, AISubState.WALK_PATH,
			"NPC_STATE_GOTO_WAYPOINT"));
		assertFalse(RetailPatternAI2.matchesNpcState(AIState.WALKING, AISubState.WALK_RANDOM,
			"NPC_STATE_GOTO_WAYPOINT"));
		assertTrue(RetailPatternAI2.matchesNpcState(AIState.IDLE, AISubState.NONE, "NPC_STATE_WAKE_UP", true));
		assertFalse(RetailPatternAI2.matchesNpcState(AIState.IDLE, AISubState.NONE, "NPC_STATE_WAKE_UP", false));
		assertTrue(RetailPatternAI2.matchesNpcState(AIState.WALKING, AISubState.NONE,
			"NPC_STATE_GOTO_POINT", false, true));
		assertFalse(RetailPatternAI2.matchesNpcState(AIState.WALKING, AISubState.NONE,
			"NPC_STATE_GOTO_POINT", false, false));
	}

	@Test
	void supportsKnownRetailTribesAndSelfAbnormalState() {
		Operation tribe = new Operation("is_tribe", Map.of("target", "OBJI_SELF", "tribe_name", "IDSeal_Boss"));
		Operation abnormal = new Operation("is_abnormal_state", Map.of("abnormal_state", "ABNSTATEI_STUN"));
		assertTrue(RetailPatternAI2.supports(new Pattern("tribe_test", Map.of(
			"on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(tribe),
				List.of(new Operation("do_nothing", Map.of())))),
			"on_enter_abnormal_state", List.of(new Rule(1, "DIRECT", List.of(abnormal),
				List.of(new Operation("do_nothing", Map.of()))))))));
		assertEquals(TribeClass.IDSEAL_BOSS, TribeClass.valueOf("IDSeal_Boss".toUpperCase()));
		Operation unknown = new Operation("is_tribe",
			Map.of("target", "OBJI_SELF", "tribe_name", "UNKNOWN"));
		assertFalse(RetailPatternAI2.supports(new Pattern("unknown_tribe_test",
			Map.of("on_battle_timer", List.of(new Rule(1, "DIRECT", List.of(unknown), List.of()))))));
	}

	@Test
	void supportsTalkerConditionsAndRetailLevelRanges() {
		Rule rule = new Rule(1, "DIRECT", List.of(
			new Operation("is_user", Map.of("obj_indicator", "OBJI_TALKER")),
			new Operation("is_user_level", Map.of("user", "USERI_TALKER", "level_min", "10", "level_max", "20"))),
			List.of(new Operation("do_nothing", Map.of())));
		assertTrue(RetailPatternAI2.supports(new Pattern("talker_test", Map.of("on_talked_by_user", List.of(rule)))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_talker_event", Map.of("on_wake_up", List.of(rule)))));
		assertTrue(RetailPatternAI2.matchesLevel(10, 10, 20));
		assertTrue(RetailPatternAI2.matchesLevel(20, 10, 20));
		assertTrue(RetailPatternAI2.matchesLevel(9, 0, 10));
		assertTrue(RetailPatternAI2.matchesLevel(21, 20, 0));
		assertFalse(RetailPatternAI2.matchesLevel(21, 10, 20));
	}

	@Test
	void supportsRetailHyperlinkIdsAndCloseDialog() {
		assertEquals(1008, RetailPatternAI2.retailHyperlinkId("HACTION_FINISH_DIALOG"));
		assertEquals(10000, RetailPatternAI2.retailHyperlinkId("HACTION_SETPRO1"));
		assertEquals(10005, RetailPatternAI2.retailHyperlinkId("HACTION_SETPRO6"));
		assertEquals(-1, RetailPatternAI2.retailHyperlinkId("HACTION_UNKNOWN"));

		Rule rule = new Rule(1, "PLANNED",
			List.of(new Operation("is_hyperlink_id", Map.of("hyperlink_id", "HACTION_SETPRO1"))),
			List.of(
				new Operation("use_skill", Map.of(
					"target", "OBJI_TALKER", "skill", "SKILLI_INDEX_0", "skill_level", "0")),
				new Operation("close_dialog", Map.of("target", "USERI_TALKER"))));
		Pattern pattern = new Pattern("hyperlink_test", Map.of("on_hyperlink_clicked", List.of(rule)));
		assertTrue(RetailPatternAI2.supports(pattern));
		assertTrue(RetailPatternAI2.handlesHyperlink(pattern, 10000));
		assertFalse(RetailPatternAI2.handlesHyperlink(pattern, 10001));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_hyperlink_event",
			Map.of("on_talked_by_user", List.of(rule)))));
	}

	@Test
	void supportsCompleteRetailLocationAliasActions() {
		Operation move = new Operation("goto_alias",
			Map.of("alias", "Location_Boss", "move_type", "MOVETYPE_RUN"));
		assertTrue(RetailPatternAI2.supports(new Pattern("move_alias_test", Map.of("on_wake_up",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(move)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_move_alias_test", Map.of("on_wake_up",
			List.of(new Rule(1, "PLANNED", List.of(), List.of(new Operation("goto_alias",
				Map.of("alias", "Location_Boss", "move_type", "MOVETYPE_FLY")))))))));
		Operation teleport = new Operation("teleport_target_alias",
			Map.of("target", "OBJI_TALKER", "alias", "Location_Boss", "showfx", "TRUE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("alias_test", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(teleport)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_alias_target", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("teleport_target_alias",
				Map.of("target", "OBJI_MESSAGE_PARAM", "alias", "Location_Boss", "showfx", "TRUE")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_alias_effect", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("teleport_target_alias",
				Map.of("target", "OBJI_TALKER", "alias", "Location_Boss", "showfx", "UNKNOWN")))))))));
		Operation selfTeleport = new Operation("teleport_target_alias",
			Map.of("target", "OBJI_SELF", "alias", "Center_1", "showfx", "FALSE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("self_alias_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(selfTeleport)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("self_alias_fx_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("teleport_target_alias",
				Map.of("target", "OBJI_SELF", "alias", "Center_1", "showfx", "TRUE")))))))));
		for (var target : Map.of("OBJI_MESSAGE_SENDER", "on_message", "OBJI_CUR_TARGET", "on_battle_timer").entrySet()) {
			Operation targetTeleport = new Operation("teleport_target_alias",
				Map.of("target", target.getKey(), "alias", "Center_1", "showfx", "TRUE"));
			assertTrue(RetailPatternAI2.supports(new Pattern("target_alias_" + target.getKey(), Map.of(target.getValue(),
				List.of(new Rule(1, "DIRECT", List.of(), List.of(targetTeleport)))))));
		}
		Operation casterTeleport = new Operation("teleport_target_alias",
			Map.of("target", "OBJI_CASTER", "alias", "Center_1", "showfx", "TRUE"));
		assertTrue(RetailPatternAI2.supports(new Pattern("caster_alias", Map.of("on_spelled",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(casterTeleport)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("wrong_caster_alias_event", Map.of("on_attacked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(casterTeleport)))))));
	}

	@Test
	void supportsCompleteWorldConditionVariableActions() {
		Operation action = new Operation("set_condition_spawn_variable_to_world",
			Map.of("worldid", "DF4_M", "string", "ldf4_sp_craid_on", "set", "3", "modify", "0"));

		assertTrue(RetailPatternAI2.supports(new Pattern("world_condition_test", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))))));
	}

	@Test
	void supportsCompleteRetailItemRewardActions() {
		Operation action = new Operation("give_item_by_user_indicator",
			Map.of("receiver", "USERI_TALKER", "item_id", "world_event_fifth_scentbag_01", "min", "1", "max", "3"));
		Operation objectAction = new Operation("give_item_by_obj_indicator",
			Map.of("receiver", "OBJI_TALKER", "item_id", "potion_l_hp_mp_60a", "min", "1", "max", "3"));

		assertTrue(RetailPatternAI2.supports(new Pattern("item_reward_test", Map.of("on_talked_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("hyperlink_item_reward_test", Map.of("on_hyperlink_clicked",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))))));
		assertTrue(RetailPatternAI2.supports(new Pattern("object_item_reward_test", Map.of("on_talked_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(objectAction)))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_item_reward_test", Map.of("on_talked_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("give_item_by_user_indicator",
				Map.of("receiver", "USERI_TALKER", "item_id", "world_event_fifth_scentbag_01", "min", "4", "max", "3")))))))));
		assertFalse(RetailPatternAI2.supports(new Pattern("invalid_object_item_receiver_test", Map.of("on_talked_by_user",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(new Operation("give_item_by_obj_indicator",
				Map.of("receiver", "OBJI_SELF", "item_id", "potion_l_hp_mp_60a", "min", "1", "max", "3")))))))));
	}

	@Test
	void supportsRetailAttackableStatusActions() {
		Operation action = new Operation("toggle_attackable_status_flag", Map.of("attakable", "FALSE"));

		assertTrue(RetailPatternAI2.supports(new Pattern("attackable_status_test", Map.of("on_wake_up",
			List.of(new Rule(1, "DIRECT", List.of(), List.of(action)))))));
	}

	@Test
	void usesRetailAbnormalStateMasks() {
		assertEquals(0x00000008, RetailPatternAI2.retailAbnormalMask("ABNSTATEI_SLEEP"));
		assertEquals(0x0649f04c, RetailPatternAI2.retailAbnormalMask("ABNSTATEI_CANNOT_ACT_GROUP"));
		assertEquals(0x101620b7, RetailPatternAI2.retailAbnormalMask("ABNSTATEI_PHYSICAL_GROUP"));
		assertNull(RetailPatternAI2.retailAbnormalMask("ABNSTATEI_UNKNOWN"));

		Operation selfState = new Operation("is_obj_in_abnormal_state",
			Map.of("obj", "OBJI_SELF", "abnormal_state", "ABNSTATEI_SANCTUARY"));
		assertTrue(RetailPatternAI2.supports(new Pattern("abnormal_test", Map.of("on_battle_timer",
			List.of(new Rule(1, "DIRECT", List.of(selfState), List.of(new Operation("do_nothing", Map.of()))))))));
	}

	private static final class MasterNpc extends Npc {

		private Creature master;

		private MasterNpc() {
			super(0, null, null, null);
		}

		@Override
		public Creature getMaster() {
			return master == null ? this : master;
		}
	}

	private static final class PartyNpc extends Npc {

		private String partyId;

		private PartyNpc() {
			super(0, null, null, null);
		}

		@Override
		public String getNpcPartyId() {
			return partyId;
		}
	}

	private static final class SkillNpc extends Npc {

		private int npcId;
		private int worldId;
		private NpcTemplate objectTemplate;
		private RecordingNpcController controller;
		private NpcSkillList skillList;

		private SkillNpc() {
			super(0, null, null, null);
		}

		@Override
		public RecordingNpcController getController() {
			return controller;
		}

		@Override
		public int getNpcId() {
			return npcId;
		}

		@Override
		public int getWorldId() {
			return worldId;
		}

		@Override
		public NpcTemplate getObjectTemplate() {
			return objectTemplate;
		}

		@Override
		public NpcSkillList getSkillList() {
			return skillList;
		}

		@Override
		public void setTarget(VisibleObject target) {
		}
	}

	private static final class RecordingNpcController extends NpcController {

		private int skillUses;
		private int lastSkillId;
		private int lastSkillLevel;

		@Override
		public boolean useSkill(int skillId, int skillLevel) {
			skillUses++;
			lastSkillId = skillId;
			lastSkillLevel = skillLevel;
			return true;
		}
	}

	private static final class RecordingNpcMoveController extends NpcMoveController {

		private RecordingNpcMoveController(Npc owner) {
			super(owner);
		}

		@Override
		public void abortMove() {
		}
	}

	private static final class RecordingRetailPatternAI2 extends RetailPatternAI2 {

		private int thinkCalls;

		@Override
		public void think() {
			thinkCalls++;
		}
	}

	private static final class FixedNpcLifeStats extends NpcLifeStats {

		private int hpPercentage;

		private FixedNpcLifeStats() {
			super(null);
		}

		@Override
		public int getHpPercentage() {
			return hpPercentage;
		}
	}

	private static final class TestNpcSkillEntry extends NpcSkillEntry {

		private TestNpcSkillEntry(int skillId, int skillLevel) {
			super(skillId, skillLevel);
		}

		@Override public boolean isReady(int hpPercentage, long fightingTimeInMSec) { return true; }
		@Override public boolean chanceReady() { return true; }
		@Override public boolean hpReady(int hpPercentage) { return true; }
		@Override public boolean timeReady(long fightingTimeInMSec) { return true; }
		@Override public boolean hasCooldown() { return false; }
		@Override public boolean UseInSpawned() { return true; }
	}

	private static final class RecordingVisibleObject extends VisibleObject {

		private RecordingVisibleObject(RecordingVisibleObjectController controller) {
			super(1, controller, null, null, null);
		}

		@Override public boolean isSpawned() { return true; }
		@Override public String getName() { return "terminal-cleanup"; }
	}

	private static final class RecordingVisibleObjectController extends VisibleObjectController<RecordingVisibleObject> {

		private int deletes;

		@Override
		public void onDelete() {
			deletes++;
		}
	}

	private static final class ExperiencePlayer extends Player {

		private RecordingPlayerCommonData commonData;

		private ExperiencePlayer() {
			super(null, null, null, null);
		}

		@Override
		public PlayerCommonData getCommonData() {
			return commonData;
		}
	}

	private static final class RecordingPlayerCommonData extends PlayerCommonData {

		private long experience;
		private RewardType rewardType;

		private RecordingPlayerCommonData() {
			super(1);
		}

		@Override
		public void addExp(long value, RewardType rewardType) {
			experience += value;
			this.rewardType = rewardType;
		}
	}

	private static Rule flagRule(String flag) {
		return new Rule(1, "INSTANT",
			List.of(new Operation("set_flag_var", Map.of("flagvar_indicator", flag))),
			List.of(new Operation("do_nothing", Map.of())));
	}

	private static Rule partyContextRule(String eventIndicator, String partyMemberIndicator, String flag) {
		List<Operation> conditions = new ArrayList<>();
		conditions.add(new Operation("is_user", Map.of("obj_indicator", eventIndicator)));
		if (partyMemberIndicator != null) {
			conditions.add(new Operation("is_npc", Map.of("obj_indicator", partyMemberIndicator)));
		}
		conditions.add(new Operation("set_flag_var", Map.of("flagvar_indicator", flag)));
		return new Rule(1, "INSTANT", conditions, List.of(new Operation("do_nothing", Map.of())));
	}

	private static void setField(Class<?> type, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void invokeEvent(RetailPatternAI2 ai, String event) throws ReflectiveOperationException {
		invokeEvent(ai, event, null);
	}

	private static void invokeEvent(RetailPatternAI2 ai, String event, Creature target)
			throws ReflectiveOperationException {
		invokeEvent(ai, event, null, target);
	}

	private static void invokeEvent(RetailPatternAI2 ai, String event, String timer, Creature target)
			throws ReflectiveOperationException {
		Method method = RetailPatternAI2.class.getDeclaredMethod("runEvent", String.class, String.class, Creature.class);
		method.setAccessible(true);
		method.invoke(ai, event, timer, target);
	}

	private static NpcSkillList skillList(List<NpcSkillTemplate> templates) throws ReflectiveOperationException {
		NpcSkillList list = new ObjenesisStd().newInstance(NpcSkillList.class);
		List<NpcSkillEntry> entries = new ArrayList<>();
		for (NpcSkillTemplate template : templates) {
			while (entries.size() <= template.getSourceIndex()) {
				entries.add(null);
			}
			entries.set(template.getSourceIndex(), new TestNpcSkillEntry(template.getSkillid(), template.getSkillLevel()));
		}
		setField(NpcSkillList.class, list, "skills", entries);
		return list;
	}

	private static void invokeSkill(RetailPatternAI2 ai, Operation skill) throws ReflectiveOperationException {
		Method method = RetailPatternAI2.class.getDeclaredMethod("useSkill", Operation.class, Creature.class,
			Class.forName("com.aionemu.gameserver.ai.RetailPatternAI2$RetailMessage"));
		method.setAccessible(true);
		method.invoke(ai, skill, null, null);
	}

	private static void resetPatternState(RetailPatternAI2 ai) throws ReflectiveOperationException {
		Method method = RetailPatternAI2.class.getDeclaredMethod("resetPatternState");
		method.setAccessible(true);
		method.invoke(ai);
	}

	private static String consumePendingCutsceneTeleport(RetailPatternAI2 ai, int playerObjectId, int cutsceneId)
			throws ReflectiveOperationException {
		Method method = RetailPatternAI2.class.getDeclaredMethod("consumePendingCutsceneTeleport", int.class, int.class);
		method.setAccessible(true);
		return (String) method.invoke(ai, playerObjectId, cutsceneId);
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Object> pendingCutsceneTeleports(RetailPatternAI2 ai) throws ReflectiveOperationException {
		Field field = RetailPatternAI2.class.getDeclaredField("pendingCutsceneTeleports");
		field.setAccessible(true);
		return (Map<Integer, Object>) field.get(ai);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<VisibleObject>> spawned(RetailPatternAI2 ai) throws ReflectiveOperationException {
		Field field = RetailPatternAI2.class.getDeclaredField("spawned");
		field.setAccessible(true);
		return (Map<String, List<VisibleObject>>) field.get(ai);
	}

	@SuppressWarnings("unchecked")
	private static Set<Future<?>> actionTasks(RetailPatternAI2 ai) throws ReflectiveOperationException {
		Field field = RetailPatternAI2.class.getDeclaredField("actionTasks");
		field.setAccessible(true);
		return Set.copyOf((Set<Future<?>>) field.get(ai));
	}

	@SuppressWarnings("unchecked")
	private static Set<String> flags(RetailPatternAI2 ai) throws ReflectiveOperationException {
		Field field = RetailPatternAI2.class.getDeclaredField("flags");
		field.setAccessible(true);
		return Set.copyOf((Set<String>) field.get(ai));
	}
}
