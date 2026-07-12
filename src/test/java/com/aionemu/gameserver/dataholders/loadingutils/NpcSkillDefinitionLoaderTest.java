package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.dataholders.NpcSkillData;

class NpcSkillDefinitionLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsAllRetailAssignmentsAndResolvedSkills() {
		NpcSkillData data = NpcSkillDefinitionLoader.load(
			Path.of("src/main/resources/aion/definitions/compact/npc-skills.xml").toFile());

		assertEquals(59058, data.size());
		var skills = data.getNpcSkillList(211157).getNpcSkills();
		assertEquals(2, skills.size());
		assertEquals(16526, skills.get(0).getSkillid());
		assertEquals(30, skills.get(0).getSkillLevel());
		assertEquals(100, skills.get(0).getProbability());
		assertEquals(16716, skills.get(1).getSkillid());
		assertNotNull(data.getNpcSkillList(281301));
	}

	@Test
	void keepsAssignmentWhenRetailSkillHasNoResolvedId() throws Exception {
		Path file = tempDir.resolve("npc-skills.xml");
		Files.writeString(file, """
			<npc_skill_data><groups>
				<group id="G"><skill name="unknown" probability="100"/></group>
			</groups><assignments><assign group="G" npc_ids="100 200"/></assignments></npc_skill_data>
			""");

		NpcSkillData data = NpcSkillDefinitionLoader.load(file.toFile());

		assertEquals(2, data.size());
		assertEquals(0, data.getNpcSkillList(100).getNpcSkills().size());
	}
}
