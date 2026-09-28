package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailClientAcceptEntryPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接取入口页断言夹具。
 * <p>
 * 5.8 客户端按任务页 HTML 解析"未接态第一屏"，服务端只能下发该任务页实际声明的页：真端接取窗页(4)
 * 仅当客户端声明 {@code ask_quest_accept} 时可用，其余任务下发信页 select_none(4762) / select1(1011)，
 * 否则客户端 {@code load fail}。这些对齐测试原先硬编码真端接取窗页(4)，夹具改成按客户端页契约取页并
 * 校验该页确有客户端声明（规则与证据见 {@link RetailClientAcceptEntryPage}；例外集合由
 * {@code RetailClientAcceptEntryPageTest} 冻结）。
 * <p>
 * Fixture for the accept entry page. The 5.8 client resolves the unaccepted first screen inside the
 * quest's task HTML, so the server may only emit a page that HTML declares: the native ask window (page
 * 4) requires {@code ask_quest_accept}, every other quest gets its letter page select_none(4762) /
 * select1(1011), otherwise the client reports {@code load fail}. The alignment tests used to hardcode
 * page 4; the fixture derives the page from the client contract and checks the quest declares it.
 */
public final class ClientAcceptEntryPageAssertions {

	private ClientAcceptEntryPageAssertions() {
	}

	/** 客户端声明的接取入口页；无声明即断言失败。 / The client-declared entry page; asserts when undeclared. */
	public static int expectedEntryPage(int questId) {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		int page = RetailClientAcceptEntryPage.entryPage(questId, contract);
		assertTrue(contract.hasButtonPage(questId, page),
			() -> "quest " + questId + " 的客户端任务页没有声明接取入口页 " + page
				+ " / the client task HTML declares no accept entry page " + page);
		return page;
	}

	/** 断言下发的接取入口页就是客户端声明页。 / Asserts the emitted page is the client-declared one. */
	public static void assertEntryPage(int questId, int actualPage) {
		assertEquals(expectedEntryPage(questId), actualPage,
			() -> "quest " + questId + " 的接取入口页必须落在客户端任务页声明的页上"
				+ " / the accept entry page must be a page the client task page declares");
	}
}
