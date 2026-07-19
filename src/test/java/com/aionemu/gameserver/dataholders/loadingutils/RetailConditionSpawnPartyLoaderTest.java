package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.gameserver.dataholders.RetailAiData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetailConditionSpawnPartyLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsPartyAsOneProbabilisticChoiceWithSharedToken() throws Exception {
		Path patterns = Files.createDirectory(tempDir.resolve("patterns"));
		Files.writeString(patterns.resolve("patterns.xml"), """
			<root><npc_ai_pattern><name>Test</name><event_handlers/></npc_ai_pattern></root>
			""");
		Path mappings = tempDir.resolve("npc-ai.xml");
		Files.writeString(mappings, "<npc_ai_mappings/>");
		Path strings = tempDir.resolve("ai-strings.xml");
		Files.writeString(strings, "<ai_strings/>");
		Path areas = tempDir.resolve("ai-areas.xml");
		Files.writeString(areas, "<ai_areas/>");
		Path conditions = tempDir.resolve("condition-spawns.xml");
		Files.writeString(conditions, """
			<condition_spawns version="1"><world id="123" name="TestWorld"><variable name="wave"/>
			<condition id="1" expression="wave == 1" despawn_at_other="false" group_mode="all" source="test">
			<group probability="1000"><slot><party probability="10000" token="test:g1:s1:p1">
			<npc id="1" probability="10000" x="1" y="2" z="3" heading="90" initial_delay="0" initial_delay_extra="0" life="20" respawn_time="120" respawn_time_extra="30"/>
			<npc id="2" probability="10000" x="4" y="5" z="6" heading="90" initial_delay="0" initial_delay_extra="0"/>
			</party></slot></group></condition></world></condition_spawns>
			""");

		RetailAiData data = RetailAiDefinitionLoader.load(patterns.toFile(), mappings.toFile(), strings.toFile(),
			areas.toFile(), conditions.toFile());

		var choice = data.getConditionSpawns(123).get(0).groups().get(0).slots().get(0).get(0);
		assertEquals(10000, choice.probability());
		assertEquals("test:g1:s1:p1", choice.partyId());
		assertEquals(java.util.List.of(1, 2), choice.members().stream().map(RetailAiData.ConditionSpawnNpc::id).toList());
		assertEquals(20, choice.members().get(0).life());
		assertEquals(120, choice.members().get(0).respawnTime());
		assertEquals(30, choice.members().get(0).respawnTimeExtra());
	}
}
