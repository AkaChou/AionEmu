package com.aionemu.gameserver.questEngine.retail;

import java.util.Optional;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;

/**
 * 从全局对话动作词汇表和客户端页面合同中识别原版本地导航意图。
 * Identifies retail-local navigation intents from the global dialog vocabulary and client page contract.
 * <p>
 * 本类不按任务 ID 或家族登记页链。生命周期动作始终优先由原版编译器生成的语义路由处理；只有
 * 明确属于客户端页面的动作才可能返回本地导航意图。
 * This class never registers page ladders by quest id or family. Lifecycle actions always take priority
 * through compiler-generated semantic routes; only actions explicitly present as client pages can be
 * classified as local navigation.</p>
 */
public final class RetailDialogIntentClassifier {

	private RetailDialogIntentClassifier() {
	}

	/**
	 * 判断动作是否为原版本地对话意图。
	 * Classifies an action as a retail-local dialog intent.
	 * @param questId 对话任务 ID / dialog quest id
	 * @param actionId 客户端对话动作 ID / client dialog action id
	 * @return 本地意图；生命周期或未知动作返回空 / local intent, or empty for lifecycle/unknown actions
	 */
	public static Optional<RetailDialogIntent> classify(int questId, int actionId) {
		if (questId <= 0) {
			return Optional.empty();
		}
		if (actionId == QuestDialogAction.FINISH_DIALOG.id()) {
			return Optional.of(RetailDialogIntent.LOCAL_CLOSE);
		}
		if (isLifecycleAction(actionId)) {
			return Optional.empty();
		}
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		return contract.hasButtonPage(questId, actionId)
			? Optional.of(RetailDialogIntent.LOCAL_PAGE_NAVIGATION) : Optional.empty();
	}

	private static boolean isLifecycleAction(int actionId) {
		QuestDialogAction action = QuestDialogAction.findId(actionId);
		if (action == null) {
			return false;
		}
		String name = action.name();
		return name.equals("USE_OBJECT")
			|| name.startsWith("SELECTED_QUEST_")
			|| name.equals("QUEST_SELECT")
			|| name.equals("EXCHANGE_COIN")
			|| name.startsWith("QUEST_ACCEPT")
			|| name.startsWith("QUEST_REFUSE")
			|| name.equals("ASK_QUEST_ACCEPT")
			|| name.equals("SELECT_QUEST_REWARD")
			|| name.equals("CHECK_USER_HAS_QUEST_ITEM")
			|| name.equals("CHECK_USER_HAS_QUEST_ITEM_SIMPLE")
			|| name.startsWith("SETPRO")
			|| name.equals("SET_SUCCEED");
	}
}
