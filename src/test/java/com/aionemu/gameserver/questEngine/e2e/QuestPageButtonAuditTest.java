package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定页面-按钮可达性审计的分类合同：服务端显示的客户端页面上每个可见按钮都必须有已注册
 * 路由；缺失时按 (quest, page, button) 产出 BUTTON_WITHOUT_ROUTE 行，路由完整的任务不产出。
 * Locks the page-button reachability audit contract: every visible button on a client page the
 * server displays must have a registered route; missing ones produce BUTTON_WITHOUT_ROUTE rows per
 * (quest, page, button), while fully routed quests produce none.
 */
class QuestPageButtonAuditTest {

	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");

	@Test
	void quest1107FlagsFinishDialogButtonWithoutRoute() throws Exception {
		CompiledQuestDefinition definition = definition(1107);
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		List<QuestE2eAuditRow> rows = QuestE2eBatchAudit.auditPageButtons(definition, oracle);
		assertTrue(rows.stream().anyMatch(row -> row.status() == QuestE2eStatus.BUTTON_WITHOUT_ROUTE
				&& row.dialogId() == 1008),
			"FINISH_DIALOG (1008) button shown on displayed pages must be flagged: " + rows);
	}

	@Test
	void quest3711FullyRoutedPagesProduceNoButtonRows() throws Exception {
		CompiledQuestDefinition definition = definition(3711);
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		List<QuestE2eAuditRow> rows = QuestE2eBatchAudit.auditPageButtons(definition, oracle);
		assertEquals(List.of(), rows, "fully routed quest must not produce button rows");
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestPageButtonAuditTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest resource " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
