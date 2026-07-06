package com.aionemu.gameserver.ai2.follow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;

class FollowSummonTaskAITest {

	@Test
	void siegeWeaponUsesNpcSkillRangeForAttackTrigger() throws Exception {
		float range = FollowSummonTaskAI.siegeWeaponTargetRange(npcSkills(npcSkill(18403)), skillId -> 15);

		assertEquals(17f, range, 0.001f);
	}

	@Test
	void siegeWeaponFallsBackToNormalFollowRangeWhenSkillRangeIsMissing() throws Exception {
		assertEquals(2f, FollowSummonTaskAI.siegeWeaponTargetRange(npcSkills(npcSkill(18403)), skillId -> 0), 0.001f);
		assertEquals(2f, FollowSummonTaskAI.siegeWeaponTargetRange(null, skillId -> 15), 0.001f);
	}

	@Test
	void siegeWeaponKeepsNormalFollowRangeForMasterTarget() throws Exception {
		float range = FollowSummonTaskAI.targetRangeFor(false, npcSkills(npcSkill(18403)), skillId -> 15);

		assertEquals(2f, range, 0.001f);
	}

	private static NpcSkillTemplates npcSkills(NpcSkillTemplate... skills) throws Exception {
		NpcSkillTemplates templates = new NpcSkillTemplates();
		setField(templates, "npcSkills", List.of(skills));
		return templates;
	}

	private static NpcSkillTemplate npcSkill(int skillId) throws Exception {
		NpcSkillTemplate template = new NpcSkillTemplate();
		setField(template, "skillid", skillId);
		return template;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
