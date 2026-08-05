package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class QuestAdditionalCapabilityDefinitionTest {
	@Test
	void compilesTeamAdvancedClassAndRawDialogCapabilities() {
		String xml = """
				<quest-definition id="20034" version="1">
				  <metadata name="capabilities" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
				  <nodes><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="started" target="started">
				    <event><talk-to-npc npc-id="799513"/></event>
				    <conditions>
				      <player-in-group/>
				      <advanced-class-is class="GLADIATOR"/>
				    </conditions>
				    <after-commit><show-dialog-window dialog-id="10"/></after-commit>
				  </transition></transitions>
				</quest-definition>
				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);

		assertEquals(new QuestCondition.PlayerInGroup(), transition.conditions().get(0));
		assertEquals(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR), transition.conditions().get(1));
		assertInstanceOf(AfterCommitAction.ShowDialogWindow.class, transition.afterCommit().get(0));
		assertEquals(10, ((AfterCommitAction.ShowDialogWindow) transition.afterCommit().get(0)).dialogId());
	}

	@Test
	void treatsOppositeFactBranchesAsMutuallyExclusiveTransitions() {
		String xml = """
				<quest-definition id="20035" version="1">
				  <metadata name="branches" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
				  <progress><bit-field name="step" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="started"><project status="START"><vars><var name="step" value="0"/></vars></project></node>
				    <node label="grouped"><project status="START"><vars><var name="step" value="1"/></vars></project></node>
				    <node label="solo"><project status="START"><vars><var name="step" value="2"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition source="started" target="grouped"><event><talk-to-npc npc-id="799513"/></event><conditions><player-in-group/></conditions></transition>
				    <transition source="started" target="solo"><event><talk-to-npc npc-id="799513"/></event><conditions><player-in-group expected="false"/></conditions></transition>
				  </transitions>
				</quest-definition>
				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(2, compiled.definition().transitions().size());
	}

	@Test
	void compilesResidentNpcCoordinateFollowAction() {
		String xml = """
				<quest-definition id="20036" version="1">
				  <metadata name="escort" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
				  <nodes><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="started" target="started">
				    <event><talk-to-npc npc-id="799036" dialog-id="10000"/></event>
				    <after-commit><start-follow-current-target x="292.63895" y="489.47452" z="574.2429"/></after-commit>
				  </transition></transitions>
				</quest-definition>
				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		AfterCommitAction.StartFollowCurrentTargetToPoint action =
				assertInstanceOf(AfterCommitAction.StartFollowCurrentTargetToPoint.class,
					compiled.definition().transitions().get(0).afterCommit().get(0));
		assertEquals(292.63895f, action.x());
		assertEquals(489.47452f, action.y());
		assertEquals(574.2429f, action.z());
	}
}
