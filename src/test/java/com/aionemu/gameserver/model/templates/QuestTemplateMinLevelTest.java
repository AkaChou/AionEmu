package com.aionemu.gameserver.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;

class QuestTemplateMinLevelTest {

	@Test
	void getMinlevelPermittedDefaultsToZeroWhenFieldIsNull() {
		QuestTemplate template = new QuestTemplate();
		assertEquals(0, template.getMinlevelPermitted());
	}

	@Test
	void fromMetadataPreservesZeroMinLevelWithoutExposingNull() {
		String xml = """
			<quest-definition id="80834" version="1">
			  <metadata name="[Event] Crown Collector" display-name-id="1000" min-level="0" max-level="0" category="EVENT">
			    <races><race id="PC_ALL"/></races>
			  </metadata>
			  <nodes><node label="start" status="START"/></nodes>
			  <transitions><transition source="start" target="start"><event><level-up/></event></transition></transitions>
			</quest-definition>
			""";
		QuestMetadata metadata = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).definition().metadata();

		QuestTemplate template = QuestTemplate.fromMetadata(80834, metadata);
		assertEquals(0, template.getMinlevelPermitted());
	}

	@Test
	void fromMetadataPreservesExplicitMinLevel() {
		String xml = """
			<quest-definition id="1001" version="1">
			  <metadata name="Test Quest" display-name-id="1001" min-level="10" max-level="20" category="QUEST">
			    <races><race id="PC_ALL"/></races>
			  </metadata>
			  <nodes><node label="start" status="START"/></nodes>
			  <transitions><transition source="start" target="start"><event><level-up/></event></transition></transitions>
			</quest-definition>
			""";
		QuestMetadata metadata = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).definition().metadata();

		QuestTemplate template = QuestTemplate.fromMetadata(1001, metadata);
		assertEquals(10, template.getMinlevelPermitted());
	}
}
