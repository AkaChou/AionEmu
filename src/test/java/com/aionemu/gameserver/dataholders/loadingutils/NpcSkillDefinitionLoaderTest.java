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
			Path.of("src/main/resources/aion/definitions/compact/skills/npc-skills.xml").toFile());

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
		assertEquals(1, data.getNpcSkillList(100).getNpcSkills().size());
		assertEquals(0, data.getNpcSkillList(100).getNpcSkills().get(0).getSkillid());
	}

	@Test
	void keepsSourceIndexAfterUnresolvedRetailSkill() throws Exception {
		Path file = tempDir.resolve("npc-skills-index.xml");
		Files.writeString(file, """
			<npc_skill_data><groups><group id="G">
			<skill name="unknown" probability="100"/><skill id="16516" probability="100"/>
			</group></groups><assignments><assign group="G" npc_ids="100"/></assignments></npc_skill_data>
			""");

		var skills = NpcSkillDefinitionLoader.load(file.toFile()).getNpcSkillList(100).getNpcSkills();

		assertEquals(2, skills.size());
		assertEquals(0, skills.get(0).getSourceIndex());
		assertEquals(1, skills.get(1).getSourceIndex());
	}

	@Test
	void keepsRetailSchedulingFields() throws Exception {
		Path file = tempDir.resolve("npc-skills-fields.xml");
		Files.writeString(file, """
			<npc_skill_data><groups><group id="G">
			<skill id="16516" level="40" probability="70" raw_rate="700" delay_time="15000" count="2" ultra_skill="1"/>
			</group></groups><assignments><assign group="G" npc_ids="100"/></assignments></npc_skill_data>
			""");

		var skill = NpcSkillDefinitionLoader.load(file.toFile()).getNpcSkillList(100).getNpcSkills().get(0);

		assertEquals(700, skill.getRawRate());
		assertEquals(15000, skill.getCooldown());
		assertEquals(2, skill.getCount());
		assertEquals(true, skill.isUltraSkill());
	}
}
