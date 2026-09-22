package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务作用域页面判定与可达性豁免合同：全局注册表只证明页面 ID 合法，任务 html 段落
 * 缺失且触发动作可达时审计必须产出 PAGE_NOT_IN_TASK_HTML；死动作路由后的缺失页面无运行时
 * 影响，不得计入。
 * Locks the quest-scoped page evidence and reachability exemption contract: the global registry only
 * proves a page id is legal; a missing quest html section behind a reachable action must produce
 * PAGE_NOT_IN_TASK_HTML, while missing pages behind dead-action routes must not count.
 */
class ClientTaskScopeAuditTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");

	@Test
	void oracleSeparatesGlobalRegistryFromQuestScopedPageEvidence() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		// 任务 14015 修复前的 load fail 页：全局注册表认识 1352，但该任务的 quest html 没有该段落。
		assertTrue(oracle.pageExists(14015, 1352), "global registry still backs the legacy loose check");
		assertFalse(oracle.questPageExists(14015, 1352), "quest_q14015.html has no 1352 section");
		assertTrue(oracle.questPageExists(14015, 1011));
		assertTrue(oracle.questPageExists(14015, 1012));
		// 无客户端页面证据的任务回退全局注册表，避免无证据误报。
		assertTrue(oracle.questPageExists(5135, 1011), "quests without page evidence fall back to the global table");
		// 动作可达性：14015 的 1011 页按钮发 1012，页面动作明细之外的是死动作。
		assertTrue(oracle.actionVisibleOn(14015, 1012));
		assertTrue(oracle.actionVisibleOn(14015, 39));
		assertFalse(oracle.actionVisibleOn(14015, 3399));
	}

	@Test
	void missingQuestPageBehindReachableActionIsFlagged() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		String xml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<quest-definition id="1198" version="1">
			  <metadata name="MissingPage" display-name-id="1" min-level="1" max-level="10" category="QUEST">
			    <races><race id="ELYOS"/></races>
			  </metadata>
			  <nodes><node label="unaccepted" status="NONE"/></nodes>
			  <transitions>
			    <transition source="unaccepted" target="unaccepted">
			      <event><dialog type="TALK_TO_NPC" npc-id="203098" action="QUEST_SELECT"/></event>
			      <after-commit><dialog type="SHOW_QUEST_PAGE" page="SELECT2"/></after-commit>
			    </transition>
			  </transitions>
			</quest-definition>
			""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		QuestTransition route = definition.definition().transitions().getFirst();
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, route, oracle);
		assertEquals(QuestE2eStatus.PAGE_NOT_IN_TASK_HTML, row.status(), row.reason());
	}

	@Test
	void repairedQuest14015PassesTheQuestScopedCheck() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		CompiledQuestDefinition definition = definition(14015);
		QuestTransition route = questSelectRoute(definition);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, route, oracle);
		assertEquals(QuestE2eStatus.PASS, row.status(), row.reason());
	}

	@Test
	void standardNpcStartDoesNotEmitAnUnsupportedSelect11Route() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		CompiledQuestDefinition definition = definition(1101);
		// 1011 的客户端按钮实际发 1007；没有 XML 显式声明时，不应生成 1012 桥接。
		assertFalse(oracle.actionVisibleOn(1101, 1012), "action 1012 is not visible on quest 1101");
		assertFalse(definition.definition().transitions().stream()
			.anyMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.SELECT1_1.id()));
	}

	private static QuestTransition questSelectRoute(CompiledQuestDefinition definition) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = ClientTaskScopeAuditTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest resource " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
