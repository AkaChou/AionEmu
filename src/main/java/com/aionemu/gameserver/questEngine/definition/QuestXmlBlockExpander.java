package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 将严格 XML 编写块降级为规范转换 IR。
 * Lowers strict XML authoring blocks to the canonical transition IR.
 */
final class QuestXmlBlockExpander {
	private QuestXmlBlockExpander() {
	}

	static List<QuestTransition> expand(int questId, QuestMetadata metadata, ProgressLayout progress,
										List<QuestNode> nodes, Element transitionsElement) {
		if (transitionsElement == null) {
			return List.of();
		}
		Context context = new Context(questId, metadata, progress, nodes, explicitDialogRoutes(transitionsElement));
		List<QuestTransition> transitions = new ArrayList<>();
		for (Element element : children(transitionsElement)) {
			switch (element.getTagName()) {
				case "transition":
					transitions.addAll(QuestDefinitionXmlCompiler.parseTransition(element));
					break;
				case "dialog":
					transitions.addAll(expandDialog(context, element));
					break;
				case "npc-start":
					transitions.addAll(expandNpcStart(context, element));
					break;
				case "counter":
					transitions.addAll(expandCounter(context, element));
					break;
				case "counter-grid":
					transitions.addAll(expandCounterGrid(context, element));
					break;
				case "kill-chain":
					transitions.addAll(expandKillChain(context, element));
					break;
				case "kill-routes":
					transitions.addAll(expandKillRoutes(context, element));
					break;
				case "npc-item-report":
					transitions.addAll(expandNpcItemReport(context, element));
					break;
				case "npc-report":
					transitions.addAll(expandNpcReport(context, element));
					break;
				case "npc-complete":
					transitions.addAll(expandNpcComplete(context, element));
					break;
				case "equipment-exchange":
					transitions.addAll(expandEquipmentExchange(context, element));
					break;
				case "npc-dialog":
					transitions.addAll(expandNpcDialog(context, element));
					break;
				case "movie-page-turn":
					transitions.addAll(expandMoviePageTurn(context, element));
					break;
				default:
					fail("UNKNOWN_XML_BLOCK", context, element.getTagName(), "element",
						"unsupported transitions child");
					break;
			}
		}
		String reportedRewardMode = attribute(transitionsElement, "reported-reward-mode");
		if (!reportedRewardMode.isBlank()) {
			transitions.addAll(expandReportedRewards(context, reportedRewardMode, transitions));
		}
		return List.copyOf(transitions);
	}

	/**
	 * 从已校验的普通 NPC 完成路由派生无目标实时报告路由，保留条件、事务动作和优先级，
	 * 仅将最终选择窗口替换为关闭窗口。无完整普通合同或槽位映射时拒绝编译。
	 * Derives targetless reported-reward routes from validated ordinary NPC completion routes,
	 * preserving conditions, transactional actions, and priority while replacing only the final
	 * selection response with a dialog close. Compilation fails when the ordinary contract or slot map
	 * is incomplete.
	 */
	private static List<QuestTransition> expandReportedRewards(Context context, String rawMode,
															   List<QuestTransition> transitions) {
		ReportedRewardMode mode;
		try {
			mode = ReportedRewardMode.valueOf(rawMode);
		} catch (IllegalArgumentException e) {
			return fail("REPORTED_REWARD_MODE_INVALID", context, "transitions", "reported-reward-mode",
				"must be FIXED, CHOICE, or CLASS");
		}
		return switch (mode) {
			case FIXED -> expandFixedReportedReward(context, transitions);
			case CHOICE -> expandChoiceReportedRewards(context, transitions);
			case CLASS -> expandClassReportedRewards(context, transitions);
			default -> throw new IllegalArgumentException();
		};
	}

	private static List<QuestTransition> expandFixedReportedReward(Context context,
																   List<QuestTransition> transitions) {
		if (context.metadata().useClassReward() == 1 || !context.metadata().classRewards().isEmpty()
			|| selectableRewards(context).size() != 0) {
			return fail("REPORTED_REWARD_METADATA_MISMATCH", context, "transitions", "reported-reward-mode",
				"FIXED requires fixed rewards without single-class or selectable rewards");
		}
		List<QuestTransition> contracts = completionContracts(context, transitions,
			QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		if (contracts.isEmpty()) {
			return fail("REPORTED_REWARD_CONTRACT_COUNT", context, "transitions", "reported-reward-mode",
				"FIXED requires at least one ordinary reward contract for action 8");
		}
		int reportedAction = QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id();
		requireNoTargetlessRewardRoutes(context, transitions, Set.of(reportedAction));
		List<QuestTransition> reportedContracts = contracts.stream()
			.map(contract -> targetlessCompletion(contract, reportedAction)).distinct().toList();
		if (reportedContracts.size() != 1) {
			return fail("REPORTED_REWARD_CONTRACT_MISMATCH", context, "transitions", "reported-reward-mode",
				"FIXED ordinary action 8 contracts must be equivalent after removing the NPC target");
		}
		return reportedContracts;
	}

	private static List<QuestTransition> expandChoiceReportedRewards(Context context,
																	 List<QuestTransition> transitions) {
		if (context.metadata().useClassReward() != 0 || !context.metadata().classRewards().isEmpty()) {
			return fail("REPORTED_REWARD_METADATA_MISMATCH", context, "transitions", "reported-reward-mode",
				"CHOICE does not accept class rewards");
		}
		List<QuestReward> selectableRewards = selectableRewards(context);
		if (selectableRewards.size() < 2 || selectableRewards.size() > 15) {
			return fail("REPORTED_REWARD_METADATA_MISMATCH", context, "transitions", "reported-reward-mode",
				"CHOICE requires between 2 and 15 selectable rewards, found " + selectableRewards.size());
		}
		List<QuestTransition> contracts = new ArrayList<>(selectableRewards.size());
		Set<Integer> generatedActions = new LinkedHashSet<>();
		for (int slot = 0; slot < selectableRewards.size(); slot++) {
			int ordinaryAction = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + slot;
			List<QuestTransition> slotContracts = completionContracts(context, transitions, ordinaryAction);
			if (slotContracts.size() != 1) {
				return fail("REPORTED_REWARD_CONTRACT_COUNT", context, "transitions", "reported-reward-mode",
					"CHOICE requires exactly one ordinary contract for action " + ordinaryAction
						+ ", found " + slotContracts.size());
			}
			QuestReward reward = selectableRewards.get(slot);
			QuestAction expectedReward = new QuestAction.GrantReward(QuestRewardKind.ITEM.name(), reward.id(),
				reward.amount(), QuestRewardAmountMode.EXACT);
			if (!slotContracts.getFirst().actions().contains(expectedReward)) {
				return fail("REPORTED_REWARD_CHOICE_ACTION_MISMATCH", context, "transitions",
					"reported-reward-mode", "ordinary action " + ordinaryAction
						+ " does not grant selectable reward " + reward.id());
			}
			contracts.add(slotContracts.getFirst());
			generatedActions.add(ordinaryAction);
			generatedActions.add(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + slot);
		}
		for (int ordinaryAction = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + selectableRewards.size();
			 ordinaryAction <= QuestDialogAction.SELECTED_QUEST_REWARD15.id(); ordinaryAction++) {
			if (!completionContracts(context, transitions, ordinaryAction).isEmpty()) {
				return fail("REPORTED_REWARD_SLOT_GAP", context, "transitions", "reported-reward-mode",
					"CHOICE has an ordinary reward contract beyond its " + selectableRewards.size()
						+ " selectable slots at action " + ordinaryAction);
			}
		}
		requireNoTargetlessRewardRoutes(context, transitions, generatedActions);
		List<QuestTransition> result = new ArrayList<>(contracts.size() * 2);
		for (int slot = 0; slot < contracts.size(); slot++) {
			QuestTransition contract = contracts.get(slot);
			result.add(targetlessCompletion(contract, QuestDialogAction.SELECTED_QUEST_REWARD1.id() + slot));
			result.add(targetlessCompletion(contract,
				QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + slot));
		}
		return result;
	}

	private static List<QuestTransition> expandClassReportedRewards(Context context,
																	List<QuestTransition> transitions) {
		if (context.metadata().useClassReward() != 1 || context.metadata().classRewards().size() != 11
			|| !selectableRewards(context).isEmpty()) {
			return fail("REPORTED_REWARD_METADATA_MISMATCH", context, "transitions", "reported-reward-mode",
				"CLASS requires all 11 class rewards and no ordinary selectable rewards");
		}
		List<QuestTransition> contracts = completionContracts(context, transitions,
			QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		if (contracts.size() != 11) {
			return fail("REPORTED_REWARD_CONTRACT_COUNT", context, "transitions", "reported-reward-mode",
				"CLASS requires 11 ordinary action 8 contracts, found " + contracts.size());
		}
		Set<PlayerClass> expectedClasses = Set.of(PlayerClass.GLADIATOR, PlayerClass.TEMPLAR,
			PlayerClass.RANGER, PlayerClass.ASSASSIN, PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER,
			PlayerClass.CLERIC, PlayerClass.CHANTER, PlayerClass.GUNSLINGER, PlayerClass.SONGWEAVER,
			PlayerClass.AETHERTECH);
		Set<PlayerClass> actualClasses = new LinkedHashSet<>();
		for (QuestTransition contract : contracts) {
			List<PlayerClass> classes = contract.conditions().stream()
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(QuestCondition.AdvancedClassIs::playerClass).toList();
			if (classes.size() != 1 || !actualClasses.add(classes.getFirst())) {
				return fail("REPORTED_REWARD_CLASS_CONDITIONS", context, "transitions", "reported-reward-mode",
					"each CLASS contract must select one unique advanced class");
			}
			PlayerClass playerClass = classes.getFirst();
			List<QuestReward> rewards = context.metadata().classRewards().get(classRewardKey(playerClass));
			if (rewards == null || rewards.size() != 1
				|| !contract.actions().contains(rewardAction(context, "reported-reward-mode", 0,
				rewards.getFirst()))) {
				return fail("REPORTED_REWARD_CLASS_ACTION_MISMATCH", context, "transitions",
					"reported-reward-mode", "ordinary action 8 does not grant the declared reward for "
						+ playerClass);
			}
		}
		if (!actualClasses.equals(expectedClasses)) {
			return fail("REPORTED_REWARD_CLASS_CONDITIONS", context, "transitions", "reported-reward-mode",
				"CLASS contracts do not cover all 11 advanced classes");
		}
		Set<Integer> generatedActions = Set.of(QuestDialogAction.SELECTED_QUEST_REWARD1.id(),
			QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id());
		requireNoTargetlessRewardRoutes(context, transitions, generatedActions);
		List<QuestTransition> result = new ArrayList<>(contracts.size() * 2);
		for (QuestTransition contract : contracts) {
			result.add(targetlessCompletion(contract, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
			result.add(targetlessCompletion(contract, QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()));
		}
		return result;
	}

	private static String classRewardKey(PlayerClass playerClass) {
		return switch (playerClass) {
			case GLADIATOR -> "FIGHTER";
			case TEMPLAR -> "KNIGHT";
			case RANGER -> "RANGER";
			case ASSASSIN -> "ASSASSIN";
			case SORCERER -> "WIZARD";
			case SPIRIT_MASTER -> "ELEMENTALIST";
			case CLERIC -> "PRIEST";
			case CHANTER -> "CHANTER";
			case GUNSLINGER -> "GUNSLINGER";
			case SONGWEAVER -> "SONGWEAVER";
			case AETHERTECH -> "AETHERTECH";
			default -> throw new IllegalArgumentException("unsupported advanced class " + playerClass);
		};
	}

	private static List<QuestReward> selectableRewards(Context context) {
		return context.metadata().rewards().stream()
			.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
			.toList();
	}

	private static List<QuestTransition> completionContracts(Context context,
															 List<QuestTransition> transitions, int dialogId) {
		List<QuestTransition> result = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == dialogId)
			.toList();
		for (QuestTransition transition : result) {
			validateCompletionContract(context, transition, dialogId);
		}
		return result;
	}

	private static void validateCompletionContract(Context context, QuestTransition transition, int dialogId) {
		QuestNode source = requireNode(context, "transitions", "reported-reward-mode", transition.sourceNode());
		QuestNode target = requireNode(context, "transitions", "reported-reward-mode", transition.targetNode());
		if (source.projection().status() != QuestStatus.REWARD) {
			fail("REPORTED_REWARD_SOURCE_STATUS", context, "transitions", "reported-reward-mode",
				"ordinary action " + dialogId + " source must project REWARD");
		}
		if (target.projection().status() != QuestStatus.COMPLETE) {
			fail("REPORTED_REWARD_TARGET_STATUS", context, "transitions", "reported-reward-mode",
				"ordinary action " + dialogId + " target must project COMPLETE");
		}
		if (transition.actions().stream().filter(QuestAction.CompleteQuest.class::isInstance).count() != 1) {
			fail("REPORTED_REWARD_COMPLETE_ACTION", context, "transitions", "reported-reward-mode",
				"ordinary action " + dialogId + " must contain exactly one complete-quest action");
		}
		List<AfterCommitAction> afterCommit = transition.afterCommit();
		if (afterCommit.size() < 3 || !(afterCommit.get(0) instanceof AfterCommitAction.RefreshPlayerStats)
			|| !(afterCommit.get(1) instanceof AfterCommitAction.SyncQuestState(QuestStateSyncMode mode))
			|| mode != QuestStateSyncMode.COMPLETION
			|| !(afterCommit.getLast() instanceof AfterCommitAction.ShowQuestSelectionDialog(int id))
			|| id != QuestDialogPage.SELECT_QUEST.id()
			|| afterCommit.subList(2, afterCommit.size() - 1).stream()
			.anyMatch(QuestXmlBlockExpander::isDialogResponse)) {
			fail("REPORTED_REWARD_AFTER_COMMIT_ORDER", context, "transitions", "reported-reward-mode",
				"ordinary action " + dialogId
					+ " must end refresh-player-stats, COMPLETION sync, then SELECT_QUEST response");
		}
	}

	private static boolean isDialogResponse(AfterCommitAction action) {
		return action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow
			|| action instanceof AfterCommitAction.CloseDialog;
	}

	private static void requireNoTargetlessRewardRoutes(Context context, List<QuestTransition> transitions,
														Set<Integer> dialogIds) {
		for (QuestTransition transition : transitions) {
			if (transition.event() instanceof QuestEvent.QuestDialog(int dialogId) && dialogIds.contains(dialogId)) {
				fail("REPORTED_REWARD_ROUTE_CONFLICT", context, "transitions", "reported-reward-mode",
					"targetless action " + dialogId + " is already declared");
			}
		}
	}

	private static QuestTransition targetlessCompletion(QuestTransition contract, int dialogId) {
		List<AfterCommitAction> afterCommit = new ArrayList<>(contract.afterCommit());
		afterCommit.set(afterCommit.size() - 1, new AfterCommitAction.CloseDialog());
		return new QuestTransition(new QuestEvent.QuestDialog(dialogId), contract.conditions(), contract.actions(),
			contract.targetNode(), afterCommit, contract.priority(), contract.sourceNode());
	}

	private static Set<DialogRouteKey> explicitDialogRoutes(Element transitionsElement) {
		Set<DialogRouteKey> result = new LinkedHashSet<>();
		for (Element transition : children(transitionsElement, "transition")) {
			String source = attribute(transition, "source");
			Element eventContainer = child(transition, "event");
			if (source.isBlank() || eventContainer == null) {
				continue;
			}
			for (QuestEvent event : QuestDefinitionXmlCompiler.parseEvents(onlyChild(eventContainer))) {
				if (event instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
					result.add(new DialogRouteKey(source, talk.npcId(), talk.dialogId()));
				}
			}
		}
		// 编写块同样占用 (source, npc, action) 路由；派生块必须能看到它们，否则会生成冲突的重复路由。
		// Authoring blocks occupy (source, npc, action) routes too; derived blocks must see them or they
		// would emit conflicting duplicate routes.
		for (Element block : children(transitionsElement, "movie-page-turn")) {
			String source = attribute(block, "source");
			if (source.isBlank() || !block.hasAttribute("npc-id") || !block.hasAttribute("action")) {
				continue;
			}
			QuestDialogAction action = knownAction(attribute(block, "action"));
			if (action == null) {
				// 未知动作交给块展开阶段带任务上下文报错，这里只登记可解析的路由。
				// Unknown actions are reported by the block expansion with the quest context; only
				// resolvable routes are registered here.
				continue;
			}
			int npcId;
			try {
				npcId = Integer.parseInt(attribute(block, "npc-id"));
			} catch (NumberFormatException e) {
				// 非法 npc-id 同样交给块展开阶段报错，预扫描只登记可解析路由。
				// Malformed npc-id values are reported by the block expansion; the pre-scan registers
				// only resolvable routes.
				continue;
			}
			result.add(new DialogRouteKey(source, npcId, action.id()));
			String nextAction = attribute(block, "next-action").trim();
			if (!nextAction.isEmpty()) {
				QuestDialogAction next = knownAction(nextAction);
				if (next != null) {
					result.add(new DialogRouteKey(source, npcId, next.id()));
				}
			}
		}
		return Set.copyOf(result);
	}

	/**
	 * 解析已知的对话动作；未知动作返回 null。 / Resolves a known dialog action; unknown values yield null.
	 */
	private static QuestDialogAction knownAction(String value) {
		try {
			return QuestDialogAction.valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static List<QuestTransition> expandDialog(Context context, Element block) {
		QuestDialogType type = QuestDefinitionXmlCompiler.dialogType(block);
		return switch (type) {
			case NPC_START, NPC_REPORT -> {
				QuestDefinitionXmlCompiler.validateTransitionDialogShape(block, type);
				yield type == QuestDialogType.NPC_START ? expandNpcStart(context, block) : expandNpcReport(context, block);
			}
			default -> fail("DIALOG_TYPE_NOT_ALLOWED_IN_TRANSITIONS", context, "dialog", "type", type.name());
		};
	}

	private static List<QuestTransition> expandNpcDialog(Context context, Element block) {
		String source = attribute(block, "source");
		requireNode(context, "npc-dialog", "source", source);
		List<Integer> npcIds = positiveIntegerTokens(context, block, "npc-dialog", "npc-ids");
		if (npcIds.size() < 2) {
			return fail("NPC_DIALOG_TOO_FEW_NPCS", context, "npc-dialog", "npc-ids",
				"must contain at least two NPC ids");
		}
		Set<Integer> uniqueNpcIds = new LinkedHashSet<>();
		for (int npcId : npcIds) {
			if (!uniqueNpcIds.add(npcId)) {
				return fail("NPC_DIALOG_DUPLICATE_NPC_ID", context, "npc-dialog", "npc-ids",
					"duplicate NPC id " + npcId);
			}
		}
		List<QuestDialogAction> dialogActions = QuestDefinitionXmlCompiler.dialogActions(block);
		List<Element> responses = children(block, null);
		if (responses.size() != 1) {
			return fail("NPC_DIALOG_RESPONSE_COUNT", context, "npc-dialog", "element",
				"exactly one response child is required, found " + responses.size());
		}
		Element response = responses.getFirst();
		AfterCommitAction afterCommit;
		try {
			afterCommit = QuestDefinitionXmlCompiler.parseAfterCommitAction(response);
		} catch (RuntimeException e) {
			return fail("NPC_DIALOG_RESPONSE_INVALID", context, "npc-dialog", response.getTagName(),
				response.getTagName() + ": " + e.getMessage());
		}
		List<QuestTransition> result = new ArrayList<>(npcIds.size() * dialogActions.size());
		for (int npcId : npcIds) {
			for (QuestDialogAction action : dialogActions) {
				result.add(new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(),
					source, List.of(afterCommit), null, source));
			}
		}
		return result;
	}

	/**
	 * 将 movie-page-turn 编写块降级为「影片 + 后续页」合同：播放影片的动作必须同时下发客户端合同里
	 * 命名的目标页，否则客户端会停在原页反复重发同一个 CM_DIALOG_SELECT。
	 * Lowers the movie-page-turn authoring block to the "movie plus continuation page" contract: the
	 * action that plays the movie must also show the client-contract page named by that action, so the
	 * client cannot stay on the previous page and resend the same CM_DIALOG_SELECT.
	 */
	private static List<QuestTransition> expandMoviePageTurn(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		requireNode(context, "movie-page-turn", "source", source);
		requireNode(context, "movie-page-turn", "target", target);
		int npcId = positiveInteger(context, block, "movie-page-turn", "npc-id");
		int movieId = positiveInteger(context, block, "movie-page-turn", "movie-id");
		QuestDialogAction action = requiredAction(context, block, "action");
		QuestDialogPage page = block.hasAttribute("page")
			? QuestDefinitionXmlCompiler.dialogPageSymbol(block, "page")
			: sameNamedPage(context, block, "action", action);
		QuestMovieType movieType = block.hasAttribute("movie-type")
			? movieType(context, block) : QuestMovieType.CUTSCENE;

		List<QuestTransition> result = new ArrayList<>(2);
		result.add(talk(npcId, action.id(), blockConditions(context, block), blockActions(context, block),
			source, target, null, List.of(new AfterCommitAction.PlayMovie(movieId, movieType),
				new AfterCommitAction.ShowQuestDialog(page.id()))));
		String nextAction = attribute(block, "next-action").trim();
		if (!nextAction.isEmpty()) {
			QuestDialogAction next = requiredAction(context, block, "next-action");
			if (next == action) {
				return fail("MOVIE_PAGE_TURN_DUPLICATE_ACTION", context, "movie-page-turn", "next-action",
					next.name() + " is already the movie page-turn action");
			}
			result.add(talk(npcId, next.id(), List.of(), List.of(), source, target, null,
				List.of(new AfterCommitAction.ShowQuestDialog(
					sameNamedPage(context, block, "next-action", next).id()))));
		}
		return List.copyOf(result);
	}

	private static QuestDialogAction requiredAction(Context context, Element block, String attribute) {
		String value = attribute(block, attribute);
		try {
			return QuestDialogAction.valueOf(value);
		} catch (IllegalArgumentException e) {
			return fail("MOVIE_PAGE_TURN_UNKNOWN_ACTION", context, "movie-page-turn", attribute, value);
		}
	}

	/**
	 * 解析影片之后必须下发的客户端页。默认取与动作同名的页面；动作没有同名页面时必须显式声明 page，
	 * 否则拒绝编译——这是「只播影片、客户端停在原页」缺陷的编写期护栏。
	 * Resolves the client page that must follow the movie. The page defaults to the action name and the
	 * block refuses to compile when that page is missing, which is the authoring-time guardrail against
	 * the "movie only, client stays on the old page" defect.
	 */
	private static QuestDialogPage sameNamedPage(Context context, Element block, String attribute,
												 QuestDialogAction action) {
		try {
			return QuestDialogPage.valueOf(action.name());
		} catch (IllegalArgumentException e) {
			return fail("MOVIE_PAGE_TURN_PAGE_MISSING", context, "movie-page-turn", attribute,
				action.name() + " has no same-named client page; declare page explicitly");
		}
	}

	private static QuestMovieType movieType(Context context, Element block) {
		String value = attribute(block, "movie-type");
		try {
			return QuestMovieType.valueOf(value);
		} catch (IllegalArgumentException e) {
			return fail("MOVIE_PAGE_TURN_UNKNOWN_MOVIE_TYPE", context, "movie-page-turn", "movie-type", value);
		}
	}

	private static List<QuestCondition> blockConditions(Context context, Element block) {
		Element conditionsElement = child(block, "conditions");
		if (conditionsElement == null) {
			return List.of();
		}
		List<QuestCondition> conditions = new ArrayList<>();
		for (Element condition : children(conditionsElement)) {
			try {
				conditions.add(QuestDefinitionXmlCompiler.parseCondition(condition));
			} catch (RuntimeException e) {
				return fail("MOVIE_PAGE_TURN_CONDITION_INVALID", context, "movie-page-turn", "conditions",
					e.getMessage());
			}
		}
		return List.copyOf(conditions);
	}

	private static List<QuestAction> blockActions(Context context, Element block) {
		Element actionsElement = child(block, "actions");
		if (actionsElement == null) {
			return List.of();
		}
		List<QuestAction> actions = new ArrayList<>();
		for (Element action : children(actionsElement)) {
			try {
				actions.add(QuestDefinitionXmlCompiler.parseAction(action));
			} catch (RuntimeException e) {
				return fail("MOVIE_PAGE_TURN_ACTION_INVALID", context, "movie-page-turn", "actions",
					action.getTagName() + ": " + e.getMessage());
			}
		}
		return List.copyOf(actions);
	}

	private static List<QuestTransition> expandNpcStart(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-start", "source", source);
		QuestNode targetNode = requireNode(context, "npc-start", "target", target);
		if (sourceNode.projection().status() != QuestStatus.NONE) {
			fail("NPC_START_SOURCE_STATUS", context, "npc-start", "source",
				"node " + source + " must project NONE");
		}
		if (targetNode.projection().status() != QuestStatus.START) {
			fail("NPC_START_TARGET_STATUS", context, "npc-start", "target",
				"node " + target + " must project START");
		}
		int npcId = positiveInteger(context, block, "npc-start", "npc-id");
		// NONE 状态首次开启对话时下发的页。默认 SELECT1；部分 quest（如 luna 80875/80876）
		// 客户端只有 SELECT_NONE 的 html，必须显式指定才能命中客户端资源。
		// The page sent when the dialog opens for the first time from NONE state. Default is SELECT1; some quests
		// (e.g. luna 80875/80876) have HTML only for SELECT_NONE and must declare it explicitly.
		int startDialogId = block.hasAttribute("start-page")
			? QuestDefinitionXmlCompiler.dialogPageSymbol(block, "start-page").id()
			: QuestDialogPage.SELECT1.id();
		List<String> selectionSources = block.hasAttribute("selection-sources")
			? tokens(context, block, "npc-start", "selection-sources", true) : List.of();
		for (String selectionSource : selectionSources) {
			requireNode(context, "npc-start", "selection-sources", selectionSource);
		}

		List<QuestAction> acceptActions = new ArrayList<>();
		Element actionsElement = child(block, "accept-actions");
		if (actionsElement != null) {
			for (Element action : children(actionsElement)) {
				try {
					acceptActions.add(QuestDefinitionXmlCompiler.parseAction(action));
				} catch (RuntimeException e) {
					fail("NPC_START_ACCEPT_ACTION_INVALID", context, "npc-start", "accept-actions",
						action.getTagName() + ": " + e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		result.add(talk(npcId, QuestDialogAction.QUEST_SELECT.id(), List.of(), List.of(), source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(startDialogId))));
		// SELECT1's next action is quest-specific; only an explicit XML route may introduce SELECT1_1.
		// SELECT1 的下一动作取决于具体任务；只有 XML 显式路由才能引入 SELECT1_1。
		if (startDialogId == QuestDialogPage.SELECT_NONE.id()) {
			result.add(talk(npcId, QuestDialogAction.SELECT_NONE_1.id(), List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE_1.id()))));
		}
		result.add(talk(npcId, QuestDialogAction.ASK_QUEST_ACCEPT.id(), List.of(), List.of(), source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		List<QuestCondition> acceptConditions = List.of(new QuestCondition.StartEligible());
		result.add(talk(npcId, QuestDialogAction.QUEST_ACCEPT_1.id(), acceptConditions, acceptActions, source, target, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id()))));
		result.add(talk(npcId, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id(), acceptConditions, acceptActions, source, target, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog())));
		result.add(talk(npcId, QuestDialogAction.QUEST_REFUSE_1.id(), List.of(), List.of(), source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_REFUSE_2,
			QuestDialogAction.QUEST_REFUSE_SIMPLE)) {
			result.add(talk(npcId, action.id(), List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.CloseDialog())));
		}
		Set<String> finishSources = new LinkedHashSet<>(List.of(source, target));
		finishSources.addAll(selectionSources);
		for (String selectionSource : finishSources) {
			result.add(talk(npcId, QuestDialogAction.FINISH_DIALOG.id(), List.of(), List.of(), selectionSource, selectionSource, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()))));
		}
		return result.stream().filter(transition -> {
			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
			return !context.explicitDialogRoutes().contains(
				new DialogRouteKey(transition.sourceNode(), talk.npcId(), talk.dialogId()));
		}).toList();
	}

	private static List<QuestTransition> expandKillRoutes(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		requireNode(context, "kill-routes", "source", source);
		QuestNode targetNode = requireNode(context, "kill-routes", "target", target);
		List<Integer> npcIds = positiveIntegerTokens(context, block, "kill-routes", "npc-ids");
		if (npcIds.size() < 2) {
			return fail("KILL_ROUTES_TOO_SHORT", context, "kill-routes", "npc-ids",
				"must contain at least two NPC ids");
		}
		Set<Integer> uniqueNpcIds = new LinkedHashSet<>();
		for (int npcId : npcIds) {
			if (!uniqueNpcIds.add(npcId)) {
				return fail("KILL_ROUTES_DUPLICATE_NPC_ID", context, "kill-routes", "npc-ids",
					"duplicate NPC id " + npcId);
			}
		}
		List<QuestTransition> result = new ArrayList<>(npcIds.size());
		for (int npcId : npcIds) {
			result.add(new QuestTransition(new QuestEvent.KillNpc(npcId), List.of(), List.of(), target,
				List.of(syncQuestState(targetNode)), null, source));
		}
		return result;
	}

	private static List<QuestTransition> expandNpcReport(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-report", "source", source);
		QuestNode targetNode = requireNode(context, "npc-report", "target", target);
		if (sourceNode.projection().status() != QuestStatus.START
			&& sourceNode.projection().status() != QuestStatus.REWARD) {
			fail("NPC_REPORT_SOURCE_STATUS", context, "npc-report", "source",
				"node " + source + " must project START or REWARD");
		}
		if (targetNode.projection().status() != QuestStatus.REWARD) {
			fail("NPC_REPORT_TARGET_STATUS", context, "npc-report", "target",
				"node " + target + " must project REWARD");
		}
		int npcId = positiveInteger(context, block, "npc-report", "npc-id");
		int page = QuestDefinitionXmlCompiler.dialogPageSymbol(block, "page").id();
		if (!Set.of(QuestDialogPage.SELECT2.id(), QuestDialogPage.SELECT5.id(),
			QuestDialogPage.DEFAULT_SUCCESS.id()).contains(page)) {
			fail("NPC_REPORT_INVALID_PAGE", context, "npc-report", "page",
				"must be SELECT2, SELECT5, or DEFAULT_SUCCESS");
		}
		List<AfterCommitAction> rewardAfterCommit = new ArrayList<>();
		if (!source.equals(target)) {
			rewardAfterCommit.add(syncQuestState(targetNode));
		}
		rewardAfterCommit.add(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		List<QuestTransition> result = List.of(
			talk(npcId, QuestDialogAction.QUEST_SELECT.id(), List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.ShowQuestDialog(page))),
			talk(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id(), List.of(), List.of(), source, target, null,
				List.copyOf(rewardAfterCommit)));
		return result.stream().filter(transition -> {
			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
			return !context.explicitDialogRoutes().contains(
				new DialogRouteKey(transition.sourceNode(), talk.npcId(), talk.dialogId()));
		}).toList();
	}

	private static List<QuestTransition> expandNpcItemReport(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-item-report", "source", source);
		QuestNode targetNode = requireNode(context, "npc-item-report", "target", target);
		if (sourceNode.projection().status() != QuestStatus.START) {
			fail("NPC_ITEM_REPORT_SOURCE_STATUS", context, "npc-item-report", "source",
				"node " + source + " must project START");
		}
		if (targetNode.projection().status() != QuestStatus.REWARD) {
			fail("NPC_ITEM_REPORT_TARGET_STATUS", context, "npc-item-report", "target",
				"node " + target + " must project REWARD");
		}
		int npcId = positiveInteger(context, block, "npc-item-report", "npc-id");
		int itemId = positiveInteger(context, block, "npc-item-report", "item-id");
		int required = positiveInteger(context, block, "npc-item-report", "required");
		int removeCount = removeCount(context, block, required);
		List<QuestCondition> hasItem = List.of(new QuestCondition.HasItem(itemId, required));
		List<QuestAction> removeItem = List.of(new QuestAction.RemoveItem(itemId, removeCount));
		List<AfterCommitAction> successAfterCommit = List.of(
			syncQuestState(targetNode),
			new AfterCommitAction.ShowQuestDialog(5));
		AfterCommitAction failureAfterCommit = itemReportFailureAfterCommit(block);
		return List.of(
			talk(npcId, 39, hasItem, removeItem, source, target, 0, successAfterCommit),
			talk(npcId, 39, List.of(), List.of(), source, source, 1,
				List.of(failureAfterCommit)),
			talk(npcId, 20002, hasItem, removeItem, source, target, 0, successAfterCommit),
			talk(npcId, 20002, List.of(), List.of(), source, source, 1,
				List.of(new AfterCommitAction.CloseDialog())));
	}

	private static AfterCommitAction itemReportFailureAfterCommit(Element block) {
		String configured = block.hasAttribute("failure-page")
			? block.getAttribute("failure-page").trim() : "";
		if (configured.isEmpty()) {
			return new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id());
		}
		if ("CLOSE".equalsIgnoreCase(configured)) {
			return new AfterCommitAction.CloseDialog();
		}
		return new AfterCommitAction.ShowQuestDialog(
			QuestDefinitionXmlCompiler.dialogPageSymbol(block, "failure-page").id());
	}

	private static int removeCount(Context context, Element block, int required) {
		if (!block.hasAttribute("remove-count") || block.getAttribute("remove-count").isBlank()) {
			return required;
		}
		String value = block.getAttribute("remove-count");
		if ("ALL".equalsIgnoreCase(value)) {
			return QuestAction.RemoveItem.ALL;
		}
		int parsed = positiveInteger(context, block, "npc-item-report", "remove-count");
		if (parsed != required) {
			fail("NPC_ITEM_REPORT_REMOVE_COUNT_MISMATCH", context, "npc-item-report", "remove-count",
				"must equal required or be ALL");
		}
		return parsed;
	}

	private static List<QuestTransition> expandCounterGrid(Context context, Element block) {
		List<CounterGridDimension> dimensions = new ArrayList<>();
		Set<String> fields = new LinkedHashSet<>();
		Set<Integer> npcIds = new LinkedHashSet<>();
		for (Element dimensionElement : children(block, "dimension")) {
			String fieldName = attribute(dimensionElement, "field");
			if (!fields.add(fieldName)) {
				return fail("COUNTER_GRID_DUPLICATE_FIELD", context, "counter-grid", "field",
					"duplicate field " + fieldName);
			}
			BitField field = context.progress().field(fieldName);
			if (field == null) {
				return fail("COUNTER_GRID_UNKNOWN_FIELD", context, "counter-grid", "field",
					"unknown progress field " + fieldName);
			}
			int required = integer(context, dimensionElement, "counter-grid", "required");
			if (required < 1) {
				return fail("COUNTER_GRID_INVALID_REQUIRED", context, "counter-grid", "required",
					"must be at least 1");
			}
			if (required > field.maxValue() || field.minValue() > 0) {
				return fail("COUNTER_GRID_FIELD_TOO_NARROW", context, "counter-grid", "required",
					"field " + fieldName + " cannot represent 0.." + required);
			}
			List<Integer> dimensionNpcIds = positiveIntegerTokens(context, dimensionElement,
				"counter-grid", "npc-ids");
			Set<Integer> dimensionUniqueNpcIds = new LinkedHashSet<>();
			for (int npcId : dimensionNpcIds) {
				if (!dimensionUniqueNpcIds.add(npcId)) {
					return fail("COUNTER_GRID_DUPLICATE_NPC_ID", context, "counter-grid", "npc-ids",
						"duplicate NPC id " + npcId + " within dimension " + fieldName);
				}
				if (!npcIds.add(npcId)) {
					return fail("COUNTER_GRID_OVERLAPPING_NPC_ID", context, "counter-grid", "npc-ids",
						"NPC id " + npcId + " is used by more than one dimension");
				}
			}
			SourceOrder sourceOrder = sourceOrder(context, dimensionElement);
			dimensions.add(new CounterGridDimension(fieldName, required, dimensionNpcIds, sourceOrder));
		}

		Set<String> expectedFields = Set.copyOf(fields);
		List<QuestNode> startNodes = context.nodes().values().stream()
			.filter(node -> node.projection().status() == QuestStatus.START).toList();
		Map<CounterGridKey, List<QuestNode>> nodesByKey = new LinkedHashMap<>();
		for (QuestNode node : startNodes) {
			if (!node.projection().variables().keySet().equals(expectedFields)) {
				return fail("COUNTER_GRID_NODE_FIELDS_MISMATCH", context, "counter-grid", "nodes",
					"START node " + node.label() + " must project exactly " + expectedFields);
			}
			List<Integer> values = new ArrayList<>(dimensions.size());
			for (CounterGridDimension dimension : dimensions) {
				int value = node.projection().variables().get(dimension.field());
				if (value < 0 || value > dimension.required()) {
					return fail("COUNTER_GRID_NODE_VALUE_OUT_OF_RANGE", context, "counter-grid", "nodes",
						"START node " + node.label() + " projects " + dimension.field() + "=" + value
							+ " outside 0.." + dimension.required());
				}
				values.add(value);
			}
			CounterGridKey key = new CounterGridKey(values);
			nodesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
		}

		long expectedProduct = 1;
		for (CounterGridDimension dimension : dimensions) {
			expectedProduct *= dimension.required() + 1L;
		}
		if (startNodes.isEmpty() || expectedProduct != nodesByKey.size()
			|| expectedProduct != startNodes.size()
			|| nodesByKey.values().stream().anyMatch(nodes -> nodes.size() != 1)) {
			return fail("COUNTER_GRID_INCOMPLETE_PRODUCT", context, "counter-grid", "nodes",
				"START nodes must form the complete Cartesian product");
		}

		List<QuestTransition> result = new ArrayList<>();
		List<AfterCommitAction> packetOnly = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		for (CounterGridDimension dimension : dimensions) {
			for (QuestNode source : orderedSources(startNodes, dimension)) {
				CounterGridKey sourceKey = keyOf(source, dimensions);
				int currentValue = sourceKey.values().get(fieldsIndex(dimensions, dimension.field()));
				if (currentValue >= dimension.required()) {
					continue;
				}
				CounterGridKey targetKey = sourceKey.increment(fieldsIndex(dimensions, dimension.field()));
				List<QuestNode> targets = nodesByKey.getOrDefault(targetKey, List.of());
				if (targets.size() != 1) {
					return fail("COUNTER_GRID_TARGET_MISSING", context, "counter-grid", "nodes",
						"source node " + source.label() + " has no unique target for " + dimension.field());
				}
				for (int npcId : dimension.npcIds()) {
					result.add(new QuestTransition(new QuestEvent.KillNpc(npcId), List.of(), List.of(),
						targets.getFirst().label(), packetOnly, null, source.label()));
				}
			}
		}
		return result;
	}

	private static SourceOrder sourceOrder(Context context, Element dimension) {
		String value = dimension.hasAttribute("source-order") && !dimension.getAttribute("source-order").isBlank()
			? dimension.getAttribute("source-order") : SourceOrder.NODE.name();
		try {
			return SourceOrder.valueOf(value);
		} catch (IllegalArgumentException e) {
			return fail("INVALID_XML", context, "counter-grid", "source-order", "unsupported value " + value);
		}
	}

	private static List<QuestNode> orderedSources(List<QuestNode> startNodes, CounterGridDimension dimension) {
		if (dimension.sourceOrder() == SourceOrder.NODE) {
			return startNodes;
		}
		List<QuestNode> result = new ArrayList<>(startNodes.size());
		for (int value = 0; value < dimension.required(); value++) {
			for (QuestNode node : startNodes) {
				if (node.projection().variables().get(dimension.field()) == value) {
					result.add(node);
				}
			}
		}
		return result;
	}

	private static int fieldsIndex(List<CounterGridDimension> dimensions, String field) {
		for (int index = 0; index < dimensions.size(); index++) {
			if (dimensions.get(index).field().equals(field)) {
				return index;
			}
		}
		throw new IllegalStateException("unknown counter-grid field " + field);
	}

	private static CounterGridKey keyOf(QuestNode node, List<CounterGridDimension> dimensions) {
		List<Integer> values = new ArrayList<>(dimensions.size());
		for (CounterGridDimension dimension : dimensions) {
			values.add(node.projection().variables().get(dimension.field()));
		}
		return new CounterGridKey(values);
	}

	private static List<QuestTransition> expandCounter(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		String fieldName = attribute(block, "field");
		QuestNode sourceNode = requireNode(context, "counter", "source", source);
		QuestNode targetNode = requireNode(context, "counter", "target", target);
		BitField field = context.progress().field(fieldName);
		if (field == null) {
			fail("COUNTER_UNKNOWN_FIELD", context, "counter", "field", "unknown progress field " + fieldName);
		}
		int required = integer(context, block, "counter", "required");
		if (required < 1) {
			fail("COUNTER_INVALID_REQUIRED", context, "counter", "required", "must be at least 1");
		}
		if (required > field.maxValue()) {
			fail("COUNTER_FIELD_TOO_NARROW", context, "counter", "required",
				"value " + required + " exceeds " + fieldName + " max " + field.maxValue());
		}
		if (required - 1 < field.minValue()) {
			fail("COUNTER_FIELD_RANGE_INVALID", context, "counter", "required",
				"value before completion is below " + fieldName + " min " + field.minValue());
		}
		if (sourceNode.projection().variables().containsKey(fieldName)) {
			fail("COUNTER_SOURCE_PROJECTION_CONFLICT", context, "counter", "source",
				"node " + source + " fixes counter field " + fieldName);
		}
		Integer targetValue = targetNode.projection().variables().get(fieldName);
		if (targetValue != null && targetValue != required) {
			fail("COUNTER_TARGET_PROJECTION_CONFLICT", context, "counter", "target",
				"node " + target + " projects " + fieldName + "=" + targetValue
					+ " instead of " + required);
		}

		Element eventContainer = child(block, "event");
		List<QuestEvent> events;
		try {
			events = QuestDefinitionXmlCompiler.parseEvents(onlyChild(eventContainer));
		} catch (RuntimeException e) {
			return fail("COUNTER_EVENT_INVALID", context, "counter", "event", e.getMessage());
		}
		List<QuestCondition> sharedConditions = new ArrayList<>();
		Element conditionsElement = child(block, "conditions");
		if (conditionsElement != null) {
			for (Element condition : children(conditionsElement)) {
				try {
					sharedConditions.add(QuestDefinitionXmlCompiler.parseCondition(condition));
				} catch (RuntimeException e) {
					return fail("COUNTER_CONDITION_INVALID", context, "counter", "conditions", e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		for (QuestEvent event : events) {
			List<QuestCondition> continuing = new ArrayList<>(sharedConditions);
			continuing.add(new QuestCondition.VariableBelow(fieldName, required - 1));
			result.add(new QuestTransition(event, continuing,
				List.of(new QuestAction.IncrementVariable(fieldName, 1)), source,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), 1, source));

			List<QuestCondition> completing = new ArrayList<>(sharedConditions);
			completing.add(new QuestCondition.QuestVariableIs(fieldName, required - 1));
			result.add(new QuestTransition(event, completing,
				List.of(new QuestAction.IncrementVariable(fieldName, 1)), target,
				List.of(syncQuestState(targetNode)), 0, source));
		}
		return result;
	}

	private static List<QuestTransition> expandKillChain(Context context, Element block) {
		List<String> nodes = tokens(context, block, "kill-chain", "nodes", true);
		if (nodes.size() < 3) {
			return fail("KILL_CHAIN_TOO_SHORT", context, "kill-chain", "nodes",
				"must contain at least three nodes");
		}
		Set<String> uniqueNodes = new LinkedHashSet<>();
		for (String node : nodes) {
			requireNode(context, "kill-chain", "nodes", node);
			if (!uniqueNodes.add(node)) {
				return fail("KILL_CHAIN_DUPLICATE_NODE", context, "kill-chain", "nodes",
					"duplicate node " + node);
			}
		}

		List<QuestEvent> events;
		try {
			events = QuestDefinitionXmlCompiler.parseEvents(onlyChild(child(block, "event")));
		} catch (RuntimeException e) {
			return fail("KILL_CHAIN_EVENT_INVALID", context, "kill-chain", "event", e.getMessage());
		}
		if (events.stream().anyMatch(event -> !(event instanceof QuestEvent.KillNpc)
			&& !(event instanceof QuestEvent.KillNpcSet))) {
			return fail("KILL_CHAIN_EVENT_TYPE", context, "kill-chain", "event",
				"must be kill-npc");
		}

		List<QuestCondition> conditions = new ArrayList<>();
		Element conditionsElement = child(block, "conditions");
		if (conditionsElement != null) {
			for (Element condition : children(conditionsElement)) {
				try {
					conditions.add(QuestDefinitionXmlCompiler.parseCondition(condition));
				} catch (RuntimeException e) {
					return fail("KILL_CHAIN_CONDITION_INVALID", context, "kill-chain", "conditions", e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		for (int index = 0; index < nodes.size() - 1; index++) {
			QuestNode targetNode = context.nodes().get(nodes.get(index + 1));
			List<AfterCommitAction> afterCommit = List.of(syncQuestState(targetNode));
			for (QuestEvent event : events) {
				result.add(new QuestTransition(event, conditions, List.of(), nodes.get(index + 1),
					afterCommit, null, nodes.get(index)));
			}
		}
		return result;
	}

	private static AfterCommitAction.SyncQuestState syncQuestState(QuestNode targetNode) {
		QuestStatus status = targetNode.projection().status();
		QuestStateSyncMode mode = status == QuestStatus.REWARD || status == QuestStatus.COMPLETE
			? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH
			: QuestStateSyncMode.PACKET_ONLY;
		return new AfterCommitAction.SyncQuestState(mode);
	}

	private static List<QuestTransition> expandEquipmentExchange(Context context, Element block) {
		String source = attribute(block, "source");
		String started = attribute(block, "started");
		String reward = attribute(block, "reward");
		String complete = attribute(block, "complete");
		QuestNode sourceNode = requireNode(context, "equipment-exchange", "source", source);
		QuestNode startedNode = requireNode(context, "equipment-exchange", "started", started);
		QuestNode rewardNode = requireNode(context, "equipment-exchange", "reward", reward);
		QuestNode completeNode = requireNode(context, "equipment-exchange", "complete", complete);
		if (sourceNode.projection().status() != QuestStatus.NONE
			|| startedNode.projection().status() != QuestStatus.START
			|| rewardNode.projection().status() != QuestStatus.REWARD
			|| completeNode.projection().status() != QuestStatus.COMPLETE) {
			return fail("EQUIPMENT_EXCHANGE_NODE_STATUS", context, "equipment-exchange", "nodes",
				"source, started, reward, and complete must project NONE, START, REWARD, and COMPLETE");
		}

		int npcId = positiveInteger(context, block, "equipment-exchange", "npc-id");
		int materialItemId = positiveInteger(context, block, "equipment-exchange", "material-item-id");
		int materialCount = positiveInteger(context, block, "equipment-exchange", "material-count");
		String selectionField = attribute(block, "selection-field");
		String rewardField = attribute(block, "reward-field");
		if (selectionField.equals(rewardField)) {
			return fail("EQUIPMENT_EXCHANGE_FIELD_CONFLICT", context, "equipment-exchange", "reward-field",
				"selection-field and reward-field must be different");
		}

		List<ExchangeCategory> categories = new ArrayList<>();
		List<ExchangeEquipment> equipment = new ArrayList<>();
		List<ExchangeRewardGroup> rewardGroups = new ArrayList<>();
		Set<Integer> routeActions = new LinkedHashSet<>(Set.of(
			QuestDialogAction.EXCHANGE_COIN.id(), QuestDialogAction.SELECT1.id()));
		Set<Integer> equipmentItems = new LinkedHashSet<>();
		Set<Integer> groupIndices = new LinkedHashSet<>();
		for (Element child : children(block)) {
			switch (child.getTagName()) {
				case "category": {
					int action = singleDialogAction(context, child, "equipment-exchange", "category");
					addExchangeAction(context, routeActions, action, "category.action");
					categories.add(new ExchangeCategory(action,
						QuestDefinitionXmlCompiler.dialogPageSymbol(child, "page").id()));
					break;
				}
				case "equipment": {
					int action = singleDialogAction(context, child, "equipment-exchange", "equipment");
					addExchangeAction(context, routeActions, action, "equipment.action");
					boolean singular = child.hasAttribute("item-id");
					boolean plural = child.hasAttribute("item-ids");
					if (singular == plural) {
						return fail(singular ? "EQUIPMENT_EXCHANGE_ITEM_ATTRIBUTE_CONFLICT"
								: "EQUIPMENT_EXCHANGE_ITEM_REQUIRED", context, "equipment-exchange", "equipment",
							"declare item-id or item-ids");
					}
					List<Integer> itemIds = singular
						? List.of(positiveInteger(context, child, "equipment-exchange", "item-id"))
						: positiveIntegerTokens(context, child, "equipment-exchange", "item-ids");
					for (int itemId : itemIds) {
						if (!equipmentItems.add(itemId)) {
							return fail("EQUIPMENT_EXCHANGE_DUPLICATE_ITEM", context, "equipment-exchange", "item-id",
								"item " + itemId + " is declared more than once");
						}
					}
					equipment.add(new ExchangeEquipment(action, itemIds, equipment.size() + 1));
					break;
				}
				case "reward-group": {
					int action = singleDialogAction(context, child, "equipment-exchange", "reward-group");
					addExchangeAction(context, routeActions, action, "reward-group.action");
					int index = integer(context, child, "equipment-exchange", "index");
					if (index < 0 || !groupIndices.add(index)) {
						return fail("EQUIPMENT_EXCHANGE_INVALID_REWARD_GROUP", context, "equipment-exchange", "index",
							"reward group indices must be unique and non-negative");
					}
					rewardGroups.add(new ExchangeRewardGroup(action, index,
						QuestDefinitionXmlCompiler.dialogPageSymbol(child, "page").id()));
					break;
				}
				default:
					fail("EQUIPMENT_EXCHANGE_UNKNOWN_CHILD", context, "equipment-exchange",
						child.getTagName(), "unsupported child");
					break;
			}
		}
		if (categories.isEmpty() || equipment.isEmpty() || rewardGroups.size() < 2) {
			return fail("EQUIPMENT_EXCHANGE_INCOMPLETE", context, "equipment-exchange", "children",
				"requires category, equipment, and at least two reward-group children");
		}
		BitField selection = context.progress().field(selectionField);
		BitField selectedReward = context.progress().field(rewardField);
		if (selection == null || selection.minValue() > 0 || selection.maxValue() < equipment.size()) {
			return fail("EQUIPMENT_EXCHANGE_SELECTION_FIELD", context, "equipment-exchange", "selection-field",
				"must represent 0.." + equipment.size());
		}
		if (selectedReward == null || selectedReward.minValue() > 0
			|| selectedReward.maxValue() < rewardGroups.size()) {
			return fail("EQUIPMENT_EXCHANGE_REWARD_FIELD", context, "equipment-exchange", "reward-field",
				"must represent 0.." + rewardGroups.size());
		}
		if (context.metadata().rewardGroups().size() != rewardGroups.size()) {
			return fail("EQUIPMENT_EXCHANGE_REWARD_GROUP_COUNT", context, "equipment-exchange", "reward-group",
				"declares " + rewardGroups.size() + " routes for "
					+ context.metadata().rewardGroups().size() + " metadata groups");
		}
		for (int index = 0; index < rewardGroups.size(); index++) {
			if (!groupIndices.contains(index)) {
				return fail("EQUIPMENT_EXCHANGE_INVALID_REWARD_GROUP", context, "equipment-exchange", "index",
					"reward group indices must be contiguous from zero");
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		List<QuestAction> resetSelection = List.of(new QuestAction.SetVariable(selectionField, 0),
			new QuestAction.SetVariable(rewardField, 0));
		result.add(talk(npcId, QuestDialogAction.EXCHANGE_COIN.id(), List.of(new QuestCondition.StartEligible()),
			resetSelection, source, started, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		for (int action : List.of(QuestDialogAction.EXCHANGE_COIN.id(), QuestDialogAction.SELECT1.id())) {
			result.add(talk(npcId, action, List.of(), resetSelection, started, started, null,
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		}
		for (ExchangeCategory category : categories) {
			result.add(talk(npcId, category.action(), List.of(), List.of(), started, started, null,
				List.of(new AfterCommitAction.ShowQuestDialog(category.page()))));
		}
		for (ExchangeEquipment item : equipment) {
			for (int option = 0; option < item.itemIds().size(); option++) {
				result.add(talk(npcId, item.action(),
					List.of(new QuestCondition.HasItem(item.itemIds().get(option), 1)),
					List.of(new QuestAction.SetVariable(selectionField, item.selection())), started, started,
					optionPriority(item, option),
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
						new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id()))));
			}
			result.add(talk(npcId, item.action(), missingItems(item),
				List.of(new QuestAction.SetVariable(selectionField, 0)), started, started, null,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id()))));
		}

		for (ExchangeRewardGroup group : rewardGroups) {
			List<QuestReward> rewards = rewardGroup(context, group.index());
			List<Integer> selectableIndices = new ArrayList<>();
			List<QuestAction> fixedRewards = new ArrayList<>();
			for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
				QuestReward declared = rewards.get(rewardIndex);
				if (rewardKind(context, "reward-group", rewardIndex, declared) == QuestRewardKind.SELECTABLE_ITEM) {
					selectableIndices.add(rewardIndex);
				} else {
					fixedRewards.add(rewardAction(context, "reward-group", rewardIndex, declared));
				}
			}
			if (selectableIndices.isEmpty() || selectableIndices.size() > 15) {
				return fail("EQUIPMENT_EXCHANGE_SELECTABLE_REWARD_COUNT", context, "equipment-exchange",
					"reward-group", "each group must contain 1..15 selectable rewards");
			}
			int groupValue = group.index() + 1;
			for (ExchangeEquipment item : equipment) {
				for (int option = 0; option < item.itemIds().size(); option++) {
					int itemId = item.itemIds().get(option);
					List<QuestCondition> selectedItem = List.of(
						new QuestCondition.QuestVariableIs(selectionField, item.selection()),
						new QuestCondition.HasItem(itemId, 1));
					List<QuestCondition> success = new ArrayList<>(selectedItem);
					success.add(new QuestCondition.HasItem(materialItemId, materialCount));
					result.add(talk(npcId, group.action(), success,
						List.of(new QuestAction.SetVariable(rewardField, groupValue)), started, reward,
						optionPriority(item, option),
						List.of(syncQuestState(rewardNode), new AfterCommitAction.ShowQuestDialog(group.page()))));
					List<QuestCondition> noMaterial = new ArrayList<>(selectedItem);
					noMaterial.add(new QuestCondition.HasItem(materialItemId, materialCount, false));
					result.add(talk(npcId, group.action(), noMaterial, List.of(), started, started,
						optionPriority(item, option),
						List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_FAILED_1.id()))));
				}
				List<QuestCondition> missingItem = new ArrayList<>();
				missingItem.add(new QuestCondition.QuestVariableIs(selectionField, item.selection()));
				missingItem.addAll(missingItems(item));
				result.add(talk(npcId, group.action(), missingItem, List.of(), started, started, null,
					List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_FAILED_1.id()))));

				for (int option = 0; option < item.itemIds().size(); option++) {
					int itemId = item.itemIds().get(option);
					for (int choice = 0; choice < selectableIndices.size(); choice++) {
						int rewardIndex = selectableIndices.get(choice);
						List<QuestAction> actions = new ArrayList<>();
						actions.add(new QuestAction.RemoveItem(itemId, 1));
						actions.add(new QuestAction.RemoveItem(materialItemId, materialCount));
						actions.addAll(fixedRewards);
						actions.add(rewardAction(context, "reward-group", rewardIndex, rewards.get(rewardIndex)));
						actions.add(new QuestAction.CompleteQuest(group.index()));
						result.add(talk(npcId, QuestDialogAction.SELECTED_QUEST_REWARD1.id() + choice,
							List.of(new QuestCondition.QuestVariableIs(selectionField, item.selection()),
								new QuestCondition.QuestVariableIs(rewardField, groupValue),
								new QuestCondition.HasItem(itemId, 1)), actions,
							reward, complete, optionPriority(item, option), completionAfterCommit()));
					}
				}
			}
			for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
				result.add(talk(npcId, preview.id(),
					List.of(new QuestCondition.QuestVariableIs(rewardField, groupValue)), List.of(),
					reward, reward, null, List.of(new AfterCommitAction.ShowQuestDialog(group.page()))));
			}
		}
		return result;
	}

	private static List<AfterCommitAction> completionAfterCommit() {
		return List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
	}

	private static List<QuestCondition> missingItems(ExchangeEquipment equipment) {
		return equipment.itemIds().stream()
			.map(itemId -> (QuestCondition) new QuestCondition.HasItem(itemId, 1, false)).toList();
	}

	private static Integer optionPriority(ExchangeEquipment equipment, int option) {
		return equipment.itemIds().size() == 1 ? null : option;
	}

	private static int singleDialogAction(Context context, Element element, String block, String attribute) {
		List<QuestDialogAction> actions = QuestDefinitionXmlCompiler.dialogActions(element);
		if (actions.size() != 1) {
			return fail("EQUIPMENT_EXCHANGE_SINGLE_ACTION_REQUIRED", context, block, attribute,
				"requires exactly one action");
		}
		return actions.getFirst().id();
	}

	private static void addExchangeAction(Context context, Set<Integer> seen, int action, String attribute) {
		if (!seen.add(action)) {
			fail("EQUIPMENT_EXCHANGE_DUPLICATE_ACTION", context, "equipment-exchange", attribute,
				"action " + action + " is declared more than once");
		}
	}

	private static List<QuestTransition> expandNpcComplete(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-complete", "source", source);
		QuestNode targetNode = requireNode(context, "npc-complete", "target", target);
		if (sourceNode.projection().status() != QuestStatus.REWARD) {
			fail("NPC_COMPLETE_SOURCE_STATUS", context, "npc-complete", "source",
				"node " + source + " must project REWARD");
		}
		if (targetNode.projection().status() != QuestStatus.COMPLETE) {
			fail("NPC_COMPLETE_TARGET_STATUS", context, "npc-complete", "target",
				"node " + target + " must project COMPLETE");
		}
		int npcId = positiveInteger(context, block, "npc-complete", "npc-id");
		int completeRewardIndex = integer(context, block, "npc-complete", "complete-reward-index");
		if (completeRewardIndex < 0) {
			fail("NPC_COMPLETE_INVALID_COMPLETE_REWARD_INDEX", context, "npc-complete",
				"complete-reward-index", "must be non-negative");
		}
		List<QuestReward> selectedRewardGroup = rewardGroup(context, completeRewardIndex);

		List<Integer> fixedRewardIndices = block.hasAttribute("fixed-reward-indices")
			? integerTokens(context, block, "npc-complete", "fixed-reward-indices", false) : List.of();
		if (new LinkedHashSet<>(fixedRewardIndices).size() != fixedRewardIndices.size()) {
			fail("NPC_COMPLETE_DUPLICATE_REWARD_INDEX", context, "npc-complete", "fixed-reward-indices",
				"contains duplicate indices");
		}
		List<QuestAction> fixedRewards = new ArrayList<>();
		for (int rewardIndex : fixedRewardIndices) {
			QuestReward reward = reward(context, selectedRewardGroup, "fixed-reward-indices", rewardIndex);
			if (rewardKind(context, "fixed-reward-indices", rewardIndex, reward) == QuestRewardKind.SELECTABLE_ITEM) {
				fail("NPC_COMPLETE_FIXED_REWARD_TYPE", context, "npc-complete", "fixed-reward-indices",
					"reward index " + rewardIndex + " is SELECTABLE_ITEM");
			}
			fixedRewards.add(rewardAction(context, "fixed-reward-indices", rewardIndex, reward));
		}

		DialogIds dialogs = new DialogIds(context, "npc-complete");
		Element preview = child(block, "preview");
		List<Integer> previewDialogIds = preview == null ? List.of() : dialogs.addActions(preview, "preview");
		List<AfterCommitAction> extraAfterCommit = new ArrayList<>();
		Element afterCommitElement = child(block, "after-commit");
		if (afterCommitElement != null) {
			for (Element action : children(afterCommitElement)) {
				try {
					extraAfterCommit.add(QuestDefinitionXmlCompiler.parseAfterCommitAction(action));
				} catch (RuntimeException e) {
					return fail("NPC_COMPLETE_AFTER_COMMIT_INVALID", context, "npc-complete", "after-commit",
						action.getTagName() + ": " + e.getMessage());
				}
			}
		}
		List<CompletionRoute> routes = new ArrayList<>();
		if (block.hasAttribute("actions")) {
			for (int dialogId : dialogs.addActions(block, "actions")) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		for (Element choice : children(block, "choice")) {
			int rewardIndex = integer(context, choice, "npc-complete", "reward-index");
			QuestReward reward = reward(context, selectedRewardGroup, "choice.reward-index", rewardIndex);
			if (rewardKind(context, "choice.reward-index", rewardIndex, reward) != QuestRewardKind.SELECTABLE_ITEM) {
				fail("NPC_COMPLETE_CHOICE_REWARD_TYPE", context, "npc-complete", "choice.reward-index",
					"reward index " + rewardIndex + " is not SELECTABLE_ITEM");
			}
			List<Integer> choiceDialogIds = dialogs.addActions(choice, "choice");
			for (int dialogId : choiceDialogIds) {
				routes.add(new CompletionRoute(dialogId,
					rewardAction(context, "choice.reward-index", rewardIndex, reward)));
			}
		}
		Element fallback = child(block, "fallback");
		if (fallback != null) {
			List<Integer> fallbackIds = dialogs.addActions(fallback, "fallback");
			for (int dialogId : fallbackIds) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		if (routes.isEmpty()) {
			fail("NPC_COMPLETE_NO_COMPLETION_ROUTE", context, "npc-complete", "actions",
				"declare actions, choice, or fallback");
		}
		Finish finish;
		try {
			finish = Finish.valueOf(attribute(block, "finish"));
		} catch (IllegalArgumentException e) {
			return fail("NPC_COMPLETE_INVALID_FINISH", context, "npc-complete", "finish",
				"must be SELECTION_DIALOG, CLOSE_DIALOG, or NONE");
		}

		// 预览路由必须下发本档自己的奖励窗口；写死第 1 档会让多档任务重开窗口时渲染错误文案与奖励。
		// Preview routes must open this block's own reward window; a hard-coded tier-1 page makes
		// multi-tier quests render the wrong text and rewards whenever the window is re-opened.
		Optional<QuestDialogPage> previewWindow = QuestDialogPage.rewardWindowForTier(completeRewardIndex);
		if (!previewDialogIds.isEmpty() && previewWindow.isEmpty()) {
			return fail("NPC_COMPLETE_REWARD_WINDOW_UNSUPPORTED", context, "npc-complete",
				"complete-reward-index", "reward tier " + completeRewardIndex
					+ " has no client reward window page");
		}
		int previewPage = previewWindow.map(QuestDialogPage::id)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id());

		List<QuestTransition> result = new ArrayList<>();
		for (int dialogId : previewDialogIds) {
			result.add(talk(npcId, dialogId, List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.ShowQuestDialog(previewPage))));
		}
		for (CompletionRoute route : routes) {
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (route.choiceReward() != null) {
				actions.add(route.choiceReward());
			}
			actions.add(new QuestAction.CompleteQuest(completeRewardIndex));
			List<AfterCommitAction> afterCommit = new ArrayList<>();
			afterCommit.add(new AfterCommitAction.RefreshPlayerStats());
			afterCommit.add(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
			afterCommit.addAll(extraAfterCommit);
			switch (finish) {
				case SELECTION_DIALOG:
					afterCommit.add(new AfterCommitAction.ShowQuestSelectionDialog(10));
					break;
				case CLOSE_DIALOG:
					afterCommit.add(new AfterCommitAction.CloseDialog());
					break;
				case NONE:
					break;
			}
			result.add(talk(npcId, route.dialogId(), List.of(), actions, source, target, null, afterCommit));
		}
		return result;
	}

	private static List<QuestReward> rewardGroup(Context context, int completeRewardIndex) {
		List<QuestRewardGroup> groups = context.metadata().rewardGroups();
		if (groups.isEmpty()) {
			// 无奖励任务仍会把完成奖励索引持久化为 QuestState 的一部分；
			// 任何固定或可选奖励引用会在下方针对这个空组校验。
			// Rewardless quests still persist the completion reward index as part of QuestState.
			// Any fixed or selectable reward reference is validated against this empty group below.
			return List.of();
		}
		// 少数有证据支撑的自定义所有者会声明一个物理奖励组的同时持久化非零完成奖励。
		// 保留该状态契约；只有多组时才把索引当作选择器。
		// A few evidence-backed custom owners persist a non-zero completion reward while declaring one
		// physical reward group. Preserve that state contract; only multiple groups use the index as a selector.
		if (groups.size() == 1) {
			return groups.get(0).rewards();
		}
		if (completeRewardIndex >= groups.size()) {
			return fail("NPC_COMPLETE_REWARD_GROUP_OUT_OF_RANGE", context, "npc-complete",
				"complete-reward-index", "reward group " + completeRewardIndex + " is not present in metadata");
		}
		return groups.get(completeRewardIndex).rewards();
	}

	private static QuestReward reward(Context context, List<QuestReward> group, String attribute, int rewardIndex) {
		if (rewardIndex < 0 || rewardIndex >= group.size()) {
			return fail("NPC_COMPLETE_REWARD_INDEX_OUT_OF_RANGE", context, "npc-complete", attribute,
				"reward index " + rewardIndex + " is not present in the selected reward group");
		}
		return group.get(rewardIndex);
	}

	private static QuestRewardKind rewardKind(Context context, String attribute, int rewardIndex,
											  QuestReward reward) {
		try {
			return QuestRewardKind.fromWire(reward.kind());
		} catch (IllegalArgumentException e) {
			return fail("NPC_COMPLETE_REWARD_TYPE_INVALID", context, "npc-complete", attribute,
				"reward index " + rewardIndex + " has unsupported kind " + reward.kind());
		}
	}

	private static QuestAction rewardAction(Context context, String attribute, int rewardIndex,
											QuestReward reward) {
		QuestRewardKind kind = rewardKind(context, attribute, rewardIndex, reward);
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode amountMode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), amountMode);
	}

	private static QuestTransition talk(int npcId, int dialogId, List<QuestCondition> conditions,
										List<QuestAction> actions, String source, String target, Integer priority,
										List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, dialogId), conditions, actions, target,
			afterCommit, priority, source);
	}

	private static QuestNode requireNode(Context context, String block, String attribute, String label) {
		QuestNode node = context.nodes().get(label);
		if (node == null) {
			return fail("XML_BLOCK_BAD_NODE_REFERENCE", context, block, attribute, "unknown node " + label);
		}
		return node;
	}

	private static int positiveInteger(Context context, Element element, String block, String attribute) {
		int value = integer(context, element, block, attribute);
		if (value <= 0) {
			return fail("XML_BLOCK_INVALID_POSITIVE_INTEGER", context, block, attribute, "must be positive");
		}
		return value;
	}

	private static int integer(Context context, Element element, String block, String attribute) {
		try {
			return Integer.parseInt(attribute(element, attribute));
		} catch (NumberFormatException e) {
			return fail("XML_BLOCK_INVALID_INTEGER", context, block, attribute, "must be an integer");
		}
	}

	private static List<Integer> integerTokens(Context context, Element element, String block,
											   String attribute, boolean required) {
		List<String> values = tokens(context, element, block, attribute, required);
		List<Integer> result = new ArrayList<>(values.size());
		for (String value : values) {
			try {
				result.add(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				return fail("XML_BLOCK_INVALID_INTEGER_SET", context, block, attribute,
					"contains non-integer " + value);
			}
		}
		return List.copyOf(result);
	}

	private static List<Integer> positiveIntegerTokens(Context context, Element element, String block,
													   String attribute) {
		List<String> values = tokens(context, element, block, attribute, true);
		List<Integer> result = new ArrayList<>(values.size());
		for (String value : values) {
			int parsed;
			try {
				parsed = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return fail("XML_BLOCK_INVALID_INTEGER_SET", context, block, attribute,
					"contains non-integer " + value);
			}
			if (parsed <= 0) {
				return fail("XML_BLOCK_INVALID_POSITIVE_INTEGER", context, block, attribute,
					"contains non-positive value " + value);
			}
			result.add(parsed);
		}
		return List.copyOf(result);
	}

	private static List<String> tokens(Context context, Element element, String block,
									   String attribute, boolean required) {
		String raw = attribute(element, attribute).trim();
		if (raw.isEmpty()) {
			if (required) {
				return fail("XML_BLOCK_EMPTY_ATTRIBUTE", context, block, attribute, "must not be empty");
			}
			return List.of();
		}
		return List.of(raw.split("\\s+"));
	}

	private static String attribute(Element element, String name) {
		return element.getAttribute(name);
	}

	private static Element child(Element parent, String name) {
		if (parent == null) {
			return null;
		}
		for (Element element : children(parent)) {
			if (name.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}

	private static List<Element> children(Element parent) {
		return children(parent, null);
	}

	private static List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && (name == null || name.equals(element.getTagName()))) {
				result.add(element);
			}
		}
		return result;
	}

	private static Element onlyChild(Element parent) {
		if (parent == null) {
			throw new IllegalArgumentException("missing event");
		}
		List<Element> children = children(parent);
		if (children.size() != 1) {
			throw new IllegalArgumentException("event must contain exactly one child");
		}
		return children.getFirst();
	}

	private static <T> T fail(String code, Context context, String block, String attribute, String detail) {
		throw new QuestCompilationException(code, "quest " + context.questId() + " " + block
			+ " attribute '" + attribute + "': " + detail);
	}

	private record Context(int questId, QuestMetadata metadata, ProgressLayout progress,
						   Map<String, QuestNode> nodes, Set<DialogRouteKey> explicitDialogRoutes) {
		private Context(int questId, QuestMetadata metadata, ProgressLayout progress, List<QuestNode> nodes,
						Set<DialogRouteKey> explicitDialogRoutes) {
			this(questId, metadata, progress, index(nodes), explicitDialogRoutes);
		}

		private static Map<String, QuestNode> index(List<QuestNode> nodes) {
			Map<String, QuestNode> result = new LinkedHashMap<>();
			for (QuestNode node : nodes) {
				result.putIfAbsent(node.label(), node);
			}
			return Collections.unmodifiableMap(result);
		}
	}

	private record DialogRouteKey(String source, int npcId, int dialogId) {
	}

	private record CounterGridDimension(String field, int required, List<Integer> npcIds,
										SourceOrder sourceOrder) {
		private CounterGridDimension {
			npcIds = List.copyOf(npcIds);
		}
	}

	private record CounterGridKey(List<Integer> values) {
		private CounterGridKey {
			values = List.copyOf(values);
		}

		private CounterGridKey increment(int index) {
			List<Integer> next = new ArrayList<>(values);
			next.set(index, next.get(index) + 1);
			return new CounterGridKey(next);
		}
	}

	private enum SourceOrder {
		NODE,
		VALUE_THEN_NODE
	}

	private record CompletionRoute(int dialogId, QuestAction choiceReward) {
	}

	private record ExchangeCategory(int action, int page) {
	}

	private record ExchangeEquipment(int action, List<Integer> itemIds, int selection) {
		private ExchangeEquipment {
			itemIds = List.copyOf(itemIds);
		}
	}

	private record ExchangeRewardGroup(int action, int index, int page) {
	}

	private enum Finish {
		SELECTION_DIALOG,
		CLOSE_DIALOG,
		NONE
	}

	/**
	 * 实时报告任务的奖励完成合同形态。
	 * Completion-contract shape for reported quest rewards.
	 */
	private enum ReportedRewardMode {
		FIXED,
		CHOICE,
		CLASS
	}

	private static final class DialogIds {
		private final Context context;
		private final String block;
		private final Set<Integer> seen = new LinkedHashSet<>();

		private DialogIds(Context context, String block) {
			this.context = context;
			this.block = block;
		}

		private List<Integer> addActions(Element element, String attribute) {
			List<Integer> result = new ArrayList<>();
			for (QuestDialogAction action : QuestDefinitionXmlCompiler.dialogActions(element)) {
				addSingle(action.id(), attribute);
				result.add(action.id());
			}
			return List.copyOf(result);
		}

		private void addSingle(int dialogId, String attribute) {
			if (!seen.add(dialogId)) {
				fail("NPC_COMPLETE_DUPLICATE_DIALOG_ID", context, block, attribute,
					"dialog id " + dialogId + " is declared more than once");
			}
		}
	}
}
