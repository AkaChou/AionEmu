package com.aionemu.gameserver.questEngine.retail;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;

/**
 * 从客户端任务页契约推导真端任务阶段的首个本地页面。
 * Derives the first local page of a retail quest stage from the client quest-page contract.
 * <p>
 * 只使用客户端契约中的页面名称和编号，不维护逐任务页梯，也不把页面按钮转成服务端事件。
 * Only client contract page names and ids are used; no per-quest ladder is maintained and page buttons
 * are never converted into server-side events.</p>
 */
public final class RetailQuestDialogPages {

	/** 客户端 select1 页的规范页 id（selectN 依次以 341 为步长）。 / Canonical client page id of select1 (selectN step by 341). */
	private static final int STANDARD_SELECT_HEAD = 1011;
	/** 相邻 selectN 页 id 的步长。 / Page id stride between adjacent selectN pages. */
	private static final int SELECT_PAGE_STRIDE = 341;

	private RetailQuestDialogPages() {
	}

	/**
	 * 返回某一零基对话阶段的规范首屏。首屏号 = {@code firstStageNumber + stageIndex}，即家族接取段占用的
	 * selectN 页之后的第一个对话页（SimpleTalk 的 select1 是接取入口页 ⇒ 对话阶段从 select2 起；
	 * DataDriven 链行的接取用 select_none 询问窗 ⇒ 对话阶段从 select1 起）。
	 * Returns the canonical first page for a zero-based talk stage. The stage number is
	 * {@code firstStageNumber + stageIndex} — the first dialog page after the family's acquire page
	 * (SimpleTalk carries the accept entry in select1, so its talk stages start at select2; DataDriven
	 * chain rows accept through the select_none ask window, so their stages start at select1).
	 * @param questId 任务 ID / quest id
	 * @param firstStageNumber 首个对话阶段的 select 页码 / select page number of the first talk stage
	 * @param stageIndex 零基阶段下标 / zero-based stage index
	 * @return 首屏页 ID；客户端契约未声明时为空 / first page id, empty when the client contract has no such page
	 */
	public static OptionalInt stageHead(int questId, int firstStageNumber, int stageIndex) {
		if (questId <= 0 || firstStageNumber <= 0 || stageIndex < 0) {
			return OptionalInt.empty();
		}
		int desired = firstStageNumber + stageIndex;
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		List<Map.Entry<Integer, String>> heads = contract.pagesForQuest(questId).entrySet().stream()
			.filter(entry -> stageNumber(entry.getValue()) > 0)
			.sorted(Comparator.comparingInt(entry -> stageNumber(entry.getValue())))
			.toList();
		for (Map.Entry<Integer, String> head : heads) {
			if (stageNumber(head.getValue()) == desired) {
				return OptionalInt.of(head.getKey());
			}
		}
		// 无同名页时退回该阶段的标准页 id（selectN 的客户端页 id 以 341 为步长）；既无同名页也无标准页
		// 说明该任务的页面命名属于其它约定，此时不发明页、由调用方如实拒绝。
		// Without a same-named page, fall back to the stage's standard page id (client selectN page ids
		// step by 341). Neither a same-named nor a standard page means the quest uses another naming
		// convention: never invent a page — the caller rejects the row instead.
		int standardPage = STANDARD_SELECT_HEAD + (desired - 1) * SELECT_PAGE_STRIDE;
		return contract.hasButtonPage(questId, standardPage) ? OptionalInt.of(standardPage) : OptionalInt.empty();
	}

	/**
	 * 返回对话动作所属的零基 talk 阶段；非 select 页面返回 -1。
	 * Returns the zero-based talk stage owning a dialog action, or -1 for non-select pages.
	 * @param questId 任务 ID / quest id
	 * @param actionId 客户端动作/页面 ID / client action or page id
	 * @return 零基阶段下标，无法定位时为 -1 / zero-based stage index, or -1 when it cannot be located
	 */
	public static int stageIndexForAction(int questId, int actionId) {
		if (questId <= 0 || actionId <= 0) {
			return -1;
		}
		String pageName = QuestDialogContract.loadDefault().pagesForQuest(questId).get(actionId);
		int stageNumber = pageName == null ? 0 : stageNumber(pageName);
		return stageNumber > 0 ? Math.max(0, stageNumber - 2) : -1;
	}

	private static int stageNumber(String pageName) {
		if (!pageName.startsWith("select")) {
			return 0;
		}
		String digits = pageName.substring("select".length());
		if (digits.isEmpty()) {
			return 0;
		}
		for (int i = 0; i < digits.length(); i++) {
			if (!Character.isDigit(digits.charAt(i))) {
				return 0;
			}
		}
		try {
			return Integer.parseInt(digits);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
}
