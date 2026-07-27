package com.aionemu.gameserver.scriptEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ScriptNpcDataTest {

	@Test
	void loadsRetailNpcConsumers() {
		ScriptNpcData data = ScriptNpcData.load(Path.of(
				"src/main/resources/aion/definitions/compact/script-npcs.xml").toFile());
		ScriptRegistry registry = new ScriptRegistry();
		data.register(registry);

		assertEquals(8, registry.scriptNpcCount());
		assertUseSkill(data.getUseSkills().get(0), 700940, 19229);
		assertUseSkill(data.getUseSkills().get(1), 700941, 19230);
		assertItemGate(data.getItemGateVariables().get(0), 834006, 185000266,
				"ideternity_02_a_button_01", 1403447, 1403589);
		assertItemGate(data.getItemGateVariables().get(5), 834007, 185000267,
				"ideternity_02_d_button", 1403448, 1403590);
	}

	private static void assertUseSkill(ScriptNpcData.UseSkillScriptNpc script, int npcId, int skillId) {
		assertEquals(npcId, script.getNpcId());
		assertEquals(skillId, script.getSkillId());
		assertEquals(1, script.getSkillLevel());
		assertTrue(script.isDespawnOnSuccess());
	}

	private static void assertItemGate(ScriptNpcData.ItemGateVariableScriptNpc script, int npcId, int itemId,
			String variable, int failureMessageId, int successMessageId) {
		assertEquals(npcId, script.getNpcId());
		assertEquals(301550000, script.getWorldId());
		assertEquals(itemId, script.getItemId());
		assertEquals(1, script.getItemCount());
		assertEquals(variable, script.getVariable());
		assertEquals(2, script.getValue());
		assertEquals(failureMessageId, script.getFailureMessageId());
		assertEquals(successMessageId, script.getSuccessMessageId());
	}
}
