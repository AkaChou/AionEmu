package com.aionemu.gameserver.questEngine.definition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接取入口页断言夹具（对齐测试的取页预言机）。
 * <p>
 * 5.8 客户端按任务页 HTML 解析"未接态第一屏"，服务端只能下发该任务页实际声明的页：真端接取窗页(4)
 * 仅当客户端声明 {@code ask_quest_accept} 时可用，其余任务下发信页 select_none(4762) / select1(1011)，
 * 否则客户端 {@code load fail}。生产主代码的「页 4 优先」typed 形（{@code RetailClientAcceptEntryPage}）
 * 已随 P7 步 f 的 DD 编译车道退场（§10.3-#22）；本夹具以同一偏好序（页 4 → 4762 → 1011）为
 * XML-only 车道对齐金标充当<b>测试侧预言机</b>，不回主代码。
 * <p>
 * Fixture for the accept entry page (the alignment tests' page oracle). The 5.8 client resolves the
 * unaccepted first screen inside the quest's task HTML, so the server may only emit a page the HTML
 * declares. The ask-window-first production face retired with the DD compile lane in P7 step f
 * (§10.3-#22); this fixture keeps the same preference order (4 &rarr; 4762 &rarr; 1011) as a test-side
 * oracle for the XML-only lane's alignment goldens.
 */
public final class ClientAcceptEntryPageAssertions {

	private ClientAcceptEntryPageAssertions() {
	}

	/** 页 4 优先偏好序（与已退场 typed 形逐位相同）。 / The ask-window-first order (identical to the retired form). */
	private static final List<Integer> ENTRY_PAGE_PREFERENCE = List.of(
		QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
		QuestDialogPage.SELECT_NONE.id(),
		QuestDialogPage.SELECT1.id());

	/** 客户端声明的接取入口页；无声明即断言失败。 / The client-declared entry page; asserts when undeclared. */
	public static int expectedEntryPage(int questId) {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		int resolved = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
		for (int pageId : ENTRY_PAGE_PREFERENCE) {
			if (contract.hasButtonPage(questId, pageId)) {
				resolved = pageId;
				break;
			}
		}
		final int page = resolved;
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
