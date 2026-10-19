package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 验证物品使用技能的真端目标种族限制（activate_target_race → target_race）。
 * Verifies the retail target-race restriction carried by item skill actions.
 */
class SkillUseActionTest {

	@Test
	void rejectsEnemyOfAnotherRace() {
		Player player = playerOn(npc(Race.UNDEAD, NpcType.ATTACKABLE));

		assertFalse(SkillUseAction.isTargetRaceAllowed("Event_halloween", player));
	}

	@Test
	void acceptsEnemyOfTheNamedRace() {
		Player player = playerOn(npc(Race.EVENT_HALLOWEEN, NpcType.ATTACKABLE));

		assertTrue(SkillUseAction.isTargetRaceAllowed("Event_halloween", player));
	}

	@Test
	void matchesTheRaceNameIgnoringCase() {
		Player player = playerOn(npc(Race.EVENT_HALLOWEEN, NpcType.ATTACKABLE));

		assertTrue(SkillUseAction.isTargetRaceAllowed("event_HALLOWEEN", player));
	}

	@Test
	void rejectsNonAttackableTargetsOfThatRace() {
		Player player = playerOn(npc(Race.EVENT_HALLOWEEN, NpcType.NON_ATTACKABLE));

		assertFalse(SkillUseAction.isTargetRaceAllowed("Event_halloween", player));
	}

	@Test
	void unrestrictedItemKeepsAnyTarget() {
		Player player = playerOn(npc(Race.UNDEAD, NpcType.ATTACKABLE));

		assertTrue(SkillUseAction.isTargetRaceAllowed(null, player));
		assertTrue(SkillUseAction.isTargetRaceAllowed(" ", player));
	}

	@Test
	void alternativesMatchAnyListedRace() {
		assertTrue(SkillUseAction.isTargetRaceAllowed("{Livingwater}|{krall}",
			playerOn(npc(Race.KRALL, NpcType.ATTACKABLE))));
		assertTrue(SkillUseAction.isTargetRaceAllowed("{Livingwater}|{krall}",
			playerOn(npc(Race.LIVINGWATER, NpcType.ATTACKABLE))));
		assertFalse(SkillUseAction.isTargetRaceAllowed("{Livingwater}|{krall}",
			playerOn(npc(Race.UNDEAD, NpcType.ATTACKABLE))));
	}

	@Test
	void targetRaceIsBoundFromItemXml() throws ReflectiveOperationException {
		Field field = SkillUseAction.class.getDeclaredField("targetRace");
		jakarta.xml.bind.annotation.XmlAttribute binding = field.getAnnotation(jakarta.xml.bind.annotation.XmlAttribute.class);

		assertEquals("target_race", binding.name());
	}

	private static Npc npc(Race race, NpcType type) {
		NpcTemplate template = new NpcTemplate();
		template.setRace(race);
		template.setNpcType(type);
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		npc.setObjectTemplate(template);
		npc.setNpcType(type);
		return npc;
	}

	private static Player playerOn(Npc target) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setTarget(target);
		return player;
	}
}
