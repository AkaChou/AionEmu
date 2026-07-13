package com.aionemu.gameserver.model.skill;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NpcSkillListTest {

	@Test
	void preservesRetailSkillIndexesWithoutExposingUnresolvedSkills() {
		NpcSkillData previous = DataManager.NPC_SKILL_DATA;
		try {
			NpcSkillTemplate first = new NpcSkillTemplate(100, 1, 100, 0, 0, false, 0);
			NpcSkillTemplate third = new NpcSkillTemplate(300, 1, 100, 0, 0, false, 2);
			DataManager.NPC_SKILL_DATA = new NpcSkillData(List.of(new NpcSkillTemplates(1, List.of(first, third))));

			NpcSkillList skills = new NpcSkillList(1);

			assertEquals(100, skills.getSkillByIndex(0).getSkillId());
			assertNull(skills.getSkillByIndex(1));
			assertEquals(300, skills.getSkillByIndex(2).getSkillId());
		} finally {
			DataManager.NPC_SKILL_DATA = previous;
		}
	}
}
