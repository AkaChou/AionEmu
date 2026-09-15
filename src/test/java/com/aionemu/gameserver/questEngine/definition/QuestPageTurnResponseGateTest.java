package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证「翻页动作完全无回包」编译期门禁：客户端按钮点击得不到任何包时必须编译失败。
 * Verifies the compile-time gate for page-turn transitions that answer nothing at all: a client button
 * press without any outbound packet must fail compilation.
 */
class QuestPageTurnResponseGateTest {
	@Test
	void pageTurnWithAnEmptyAfterCommitIsRejected() throws Exception {
		String xml = read("/aion/data/static_data/quest_definition/quests/24053.xml");
		String mutated = xml.replace("<play-movie movie-id=\"252\"/>", "");
		assertFalse(mutated.contains("<play-movie movie-id=\"252\"/>"),
			"24053 must author its movie page turns with play-movie elements");

		QuestCompilationException failure = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(mutated.getBytes(StandardCharsets.UTF_8))));

		assertEquals("PAGE_TURN_WITHOUT_ANY_RESPONSE", failure.code());
	}

	@Test
	void movieOnlyPageTurnKeepsItsLedgerException() throws Exception {
		String xml = read("/aion/data/static_data/quest_definition/quests/24053.xml");

		// 24053 step1-4 是显式 ledger 例外：影片静默形态仍按影片规则豁免，新门禁不得因此误报。
		// 24053 step1-4 stay in the explicit ledger: the silent movie form is still exempted by the movie
		// rule, and the new gate must not report it as a response-less page turn.
		QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static String read(String resource) throws Exception {
		try (InputStream input = QuestPageTurnResponseGateTest.class.getResourceAsStream(resource)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
