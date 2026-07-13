package com.aionemu.gameserver.model.skill;

import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSkillTemplateEntryTest {

	@Test
	void retailUseCountResetsForNextCombat() {
		NpcSkillTemplate template = new NpcSkillTemplate(16516, 1, 100);
		template.setCount(1);
		NpcSkillTemplateEntry entry = new NpcSkillTemplateEntry(template);

		entry.setLastTimeUsed();
		assertFalse(entry.hasUsesLeft());
		entry.resetUseCount();
		assertTrue(entry.hasUsesLeft());
	}
}
