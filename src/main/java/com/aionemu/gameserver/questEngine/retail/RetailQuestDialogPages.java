package com.aionemu.gameserver.questEngine.retail;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;

/**
 * 从客户端任务页契约推导原版任务阶段的首个本地页面。
 * Derives the first local page of a retail quest stage from the client quest-page contract.
 * <p>
 * 只使用客户端契约中的页面名称和编号，不维护逐任务页梯，也不把页面按钮转成服务端事件。
 * Only client contract page names and ids are used; no per-quest ladder is maintained and page buttons
 * are never converted into server-side events.</p>
 */
public final class RetailQuestDialogPages {

	/** collect 段整组检查按钮（客户端 {@code HACTION_CHECK_USER_HAS_QUEST_ITEM}）。 /
	 * The collect stage's whole-group check button (the client's {@code HACTION_CHECK_USER_HAS_QUEST_ITEM}). */
	public static final int COLLECT_CHECK_ACTION_ID = QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id();

	/** collect 检查结果页：整组在背包（{@code check_user_item_ok}）。 /
	 * The collect check's result page: the whole group is in the bag ({@code check_user_item_ok}). */
	public static final int COLLECT_OK_PAGE_ID = 10000;

	/** collect 检查结果页：整组缺失（{@code check_user_item_fail}）。 /
	 * The collect check's result page: the group is missing ({@code check_user_item_fail}). */
	public static final int COLLECT_FAIL_PAGE_ID = 10001;

	private RetailQuestDialogPages() {
	}

	/**
	 * 一个对话阶段的客户端页面：页族号 K 与首屏页 id。
	 * A dialog stage's client pages: the page family number K and the head page id.
	 *
	 * @param familyNumber 页族号 K（页名 {@code selectK}） / page family number K (page name {@code selectK})
	 * @param headPageId   首屏页 id / head page id
	 */
	public record StagePage(int familyNumber, int headPageId) {

		/**
		 * 页尾推进按钮 {@code SETPRO{K}}（客户端 HACTION 编号 = 10000 + K - 1）。
		 * The tail page's advance button {@code SETPRO{K}} (client HACTION id = 10000 + K - 1).
		 * @return 推进动作 id / advance action id
		 */
		public int advanceActionId() {
			return QuestDialogAction.SETPRO1.id() + familyNumber - 1;
		}
	}

	/**
	 * 返回某一可见对话阶段的客户端页（首屏页 + 页族号）。阶段序号取**客户端声明的 select 页族序**：
	 * 任务页 HTML 只声明它用到的页族（例如 10010 声明 select1/2/3/5/6），第 i 个可见步（talk/collectitem）
	 * 对应的页族就是第 {@code familyOffset + i} 个声明族——这是客户端自身的页选择约定，不是服务端补丁表。
	 * Returns the client page of one visible dialog stage (head page + family number). The stage ordinal is
	 * the **order of the client-declared select families**: the quest page HTML declares only the families it
	 * uses (10010 declares select1/2/3/5/6), and the i-th visible step (talk/collectitem) maps to the
	 * {@code familyOffset + i}-th declared family — the client's own page-selection convention rather than a
	 * server-side patch table.
	 * @param questId 任务 ID / quest id
	 * @param familyOffset 接取段占用的页族数（SimpleTalk 的 select1 是接取入口 ⇒ 1；DataDriven/混合链 ⇒ 0）
	 *                     the number of leading page families reserved for the acquire segment (SimpleTalk's
	 *                     select1 is the accept entry ⇒ 1; DataDriven/mixed chains ⇒ 0)
	 * @param stageIndex 零基可见阶段下标 / zero-based visible stage index
	 * @return 阶段页；客户端未声明足够页族时为空（调用方如实拒绝，不发明页） / the stage page, empty when the
	 *         client declares too few families (the caller rejects honestly instead of inventing a page)
	 */
	public static Optional<StagePage> stage(int questId, int familyOffset, int stageIndex) {
		if (questId <= 0 || familyOffset < 0 || stageIndex < 0) {
			return Optional.empty();
		}
		List<StagePage> families = declaredFamilies(questId);
		int wanted = familyOffset + stageIndex;
		return wanted < families.size() ? Optional.of(families.get(wanted)) : Optional.empty();
	}

	/**
	 * 客户端为该任务声明的 select 页族数（建立阶段覆盖判据用）。
	 * The number of select page families the client declares for the quest (used for stage-coverage gates).
	 * @param questId 任务 ID / quest id
	 * @return 页族数 / number of declared families
	 */
	public static int familyCount(int questId) {
		return questId <= 0 ? 0 : declaredFamilies(questId).size();
	}

	private static List<StagePage> declaredFamilies(int questId) {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		return contract.pagesForQuest(questId).entrySet().stream()
			.map(entry -> new StagePage(stageNumber(entry.getValue()), entry.getKey()))
			.filter(page -> page.familyNumber() > 0)
			.sorted(Comparator.comparingInt(StagePage::familyNumber))
			.toList();
	}

	/**
	 * 返回一个对话阶段的推进按钮全集。中间段 = 页族尾的「继续」按钮 {@code SETPRO{K}}；
	 * 末段 = 客户端末段页尾可能声明的三种收尾按钮：{@code SETPRO{K}}（继续）、
	 * {@code SET_SUCCEED}（结束对话）、{@code SELECT_QUEST_REWARD}（领取奖励）。
	 * 依据 = 客户端 HyperLinks/HTML 的页尾按钮（全量混合链登记核对：末段 100% 落在这三个 id 内，
	 * 中间段 100% 等于 {@code SETPRO{K}}）；末段不读客户端页梯也能收下这三种按钮，因此引擎不再维护
	 * 逐任务页梯，也绝不发明登记表外的按钮。
	 * Returns the advance buttons of one dialog stage. A mid stage carries the family tail's continue
	 * button {@code SETPRO{K}}; the final stage carries the three terminal buttons the client's last
	 * tail page may declare: {@code SETPRO{K}} (continue), {@code SET_SUCCEED} (finish the dialog) and
	 * {@code SELECT_QUEST_REWARD} (claim the reward). The evidence is the client HyperLinks/HTML tail
	 * buttons: across the full mixed-chain registry the final stage always lands on one of these three
	 * ids and every mid stage equals {@code SETPRO{K}}. Accepting the three terminal ids needs no
	 * per-quest ladder and never invents a button the client does not declare.
	 * @param stagePage 阶段页族 / the stage page family
	 * @param finalStage 是否为本可见对话链的末段 / whether this is the last visible dialog stage
	 * @return 推进按钮 id（去重后的副本） / the advance button ids (deduplicated copy)
	 */
	public static List<Integer> advanceActions(StagePage stagePage, boolean finalStage) {
		if (!finalStage) {
			return List.of(stagePage.advanceActionId());
		}
		java.util.LinkedHashSet<Integer> actions = new java.util.LinkedHashSet<>(3);
		actions.add(stagePage.advanceActionId());
		actions.add(QuestDialogAction.SET_SUCCEED.id());
		actions.add(QuestDialogAction.SELECT_QUEST_REWARD.id());
		return List.copyOf(actions);
	}

	/**
	 * 返回对话动作所属的零基可见阶段；非 select 页面返回 -1，接取段页族按 0 处理。
	 * Returns the zero-based visible stage owning a dialog action, or -1 for non-select pages; page
	 * families inside the acquire segment clamp to 0.
	 * @param questId 任务 ID / quest id
	 * @param actionId 客户端动作/页面 ID / client action or page id
	 * @param familyOffset 接取段占用的页族数 / number of leading page families owned by the acquire segment
	 * @return 零基阶段下标，无法定位时为 -1 / zero-based stage index, or -1 when it cannot be located
	 */
	public static int stageIndexForAction(int questId, int actionId, int familyOffset) {
		if (questId <= 0 || actionId <= 0) {
			return -1;
		}
		String pageName = QuestDialogContract.loadDefault().pagesForQuest(questId).get(actionId);
		int stageNumber = pageName == null ? 0 : stageNumber(pageName);
		return stageNumber > 0 ? Math.max(0, stageNumber - 1 - Math.max(0, familyOffset)) : -1;
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
