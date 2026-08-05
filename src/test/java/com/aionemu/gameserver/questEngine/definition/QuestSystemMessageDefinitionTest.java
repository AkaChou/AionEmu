package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestSystemMessageDefinitionTest {
	@Test
	void compilesQuestSpecificSystemMessagePacketShape() {
		String xml = """
				<quest-definition id="18602" version="1">
				  <metadata name="message" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
				  <nodes><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="started" target="started">
				    <event><talk-to-npc npc-id="700939" dialog-id="10000"/></event>
				    <after-commit><system-message message-id="1111307" target="PLAYER" text-color-id="2"/></after-commit>
				  </transition></transitions>
				</quest-definition>
				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		AfterCommitAction action = compiled.definition().transitions().get(0).afterCommit().get(0);
		assertEquals(new AfterCommitAction.SendSystemMessagePacket(new QuestSystemMessagePacket(
			1111307, QuestSystemMessageTarget.PLAYER, false, 2, java.util.List.of())), action);
		assertEquals(QuestStatus.START, compiled.definition().nodes().get(0).projection().status());
	}
}
