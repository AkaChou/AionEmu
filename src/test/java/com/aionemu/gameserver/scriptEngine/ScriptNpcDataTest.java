package com.aionemu.gameserver.scriptEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ScriptNpcDataTest {

	@Test
	void loadsRetailTalocHealingConsumers() {
		ScriptNpcData data = ScriptNpcData.load(Path.of(
				"src/main/resources/aion/definitions/compact/script-npcs.xml").toFile());
		ScriptRegistry registry = new ScriptRegistry();
		data.register(registry);

		assertEquals(2, registry.scriptNpcCount());
		assertUseSkill(data.getUseSkills().get(0), 700940, 19229);
		assertUseSkill(data.getUseSkills().get(1), 700941, 19230);
	}

	private static void assertUseSkill(ScriptNpcData.UseSkillScriptNpc script, int npcId, int skillId) {
		assertEquals(npcId, script.getNpcId());
		assertEquals(skillId, script.getSkillId());
		assertEquals(1, script.getSkillLevel());
		assertTrue(script.isDespawnOnSuccess());
	}
}
