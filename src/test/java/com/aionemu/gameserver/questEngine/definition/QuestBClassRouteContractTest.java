package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 B 类「页面已下发但按钮无路由」批量修复合同（1430、1643、2513、2962、4542）。
 * Locks the B-class repair contract for quests whose served client pages carried unrouted buttons
 * (1430, 1643, 2513, 2962, 4542).
 *
 * <p>这一族的原始缺陷是服务端把客户端任务书压成单一 {@code started(var0=0)} 状态：客户端对话页
 * （select1_1、select2、select3、行 3 的收集检查、select5/select6 等）确实由 {@code SHOW_QUEST_PAGE}
 * 下发，但页面上的 {@code HACTION_SETPRO<n>} / {@code HACTION_SELECT<n>_<m>} /
 * {@code HACTION_CHECK_USER_HAS_QUEST_ITEM} 在 XML 里没有任何路由——玩家点下去没有反应、任务卡死，
 * wiki 侧则表现为每一步的 GM 命令都退化成 {@code //quest set <id> START 0}。
 * The family's original defect compressed each client journal into one {@code started(var0=0)} state: the pages
 * were served by {@code SHOW_QUEST_PAGE} while their buttons had no route in the XML, so clicks did nothing and
 * the wiki collapsed every step's GM command onto {@code //quest set <id> START 0}.</p>
 *
 * <p>修复后的合同：客户端任务书行 i 与 var0=i 的状态一一对应（领奖行的状态就是 REWARD 投影），行 i 的
 * 客户端 NPC 用该页面的按钮动作推进，只有领奖行 NPC 拥有完成路由，每条推进路径都能逐行走完并停在
 * REWARD/领奖行行号。每条路径都独立从 {@code started(var0=0)} 出发，用于覆盖分支任务。
 * Repaired contract: journal row i maps to the state projected at var0=i (the reward row's state is the REWARD
 * projection itself), row i advances through that row's client NPC button, only the reward-row NPC owns
 * completion, and every path walks row by row into REWARD on the last row's index. Each path starts from
 * {@code started(var0=0)} so branching quests are covered independently.</p>
 */
class QuestBClassRouteContractTest {
	/** 一行客户端任务书：该行 NPC 的推进按钮动作，以及推进后的状态与行号。 */
	private record Step(int npcId, int actionId, QuestStatus statusAfter, int var0After) {
	}

	private record Spec(int questId, int rows, List<List<Step>> paths, int rewardNpc,
			QuestDialogPage rewardEntryPage) {
	}

	private static Step start(int npcId, int actionId) {
		return new Step(npcId, actionId, QuestStatus.START, -1);
	}

	private static Step reward(int npcId, int actionId, int var0After) {
		return new Step(npcId, actionId, QuestStatus.REWARD, var0After);
	}

	private static final List<Spec> SPECS = List.of(
		// 1430：两行；Sonirim(203337) 的 SETPRO1 在旧 handler 里同时置 REWARD 并传送（行 1 = 领奖行）。
		new Spec(1430, 2, List.of(List.of(
			new Step(203337, QuestDialogAction.SETPRO1.id(), QuestStatus.REWARD, 1),
			reward(203337, QuestDialogAction.SELECT_QUEST_REWARD.id(), 1))),
			203337, QuestDialogPage.DEFAULT_SUCCESS),
		// 1643：五行；行 1 是无按钮的演出行（旧 handler 在 var0==1 才开放冤魂对话），行 3 开对话推进高亮，
		// SET_SUCCEED 进领奖；领奖行 NPC 是利希迪盖(204545)。
		new Spec(1643, 5, List.of(List.of(
			new Step(204630, QuestDialogAction.SETPRO1.id(), QuestStatus.START, 1),
			new Step(204614, QuestDialogAction.SETPRO2.id(), QuestStatus.START, 2),
			new Step(204630, QuestDialogAction.QUEST_SELECT.id(), QuestStatus.START, 3),
			reward(204630, QuestDialogAction.SET_SUCCEED.id(), 4))),
			204545, QuestDialogPage.DEFAULT_SUCCESS),
		// 2513：两行；第 1 行（领奖行）由三个分支 NPC 各自一条链推进（旧 handler 各自 setStatus(REWARD)）。
		new Spec(2513, 2, List.of(
			List.of(reward(204826, QuestDialogAction.SETPRO1.id(), 1)),
			List.of(reward(204827, QuestDialogAction.SETPRO2.id(), 1)),
			List.of(reward(790022, QuestDialogAction.SETPRO3.id(), 1))),
			204732, QuestDialogPage.DEFAULT_SUCCESS),
		// 2962：三行；领奖行由两条报告分支进入同一行号的两个 REWARD 档（旧 handler 的 rewIdex 0/1）。
		new Spec(2962, 3, List.of(
			List.of(start(278067, QuestDialogAction.SETPRO1.id()),
				start(278137, QuestDialogAction.SETPRO2.id()),
				reward(204253, QuestDialogAction.SETPRO3.id(), 2)),
			List.of(start(278067, QuestDialogAction.SETPRO1.id()),
				start(278137, QuestDialogAction.SETPRO2.id()),
				reward(204253, QuestDialogAction.SETPRO4.id(), 2))),
			204253, null),
		// 4542：六行；行 3 是收集交付（CHECK_USER_HAS_QUEST_ITEM + 成功/失败页），行 5 交钥匙领奖。
		new Spec(4542, 6, List.of(List.of(
			start(204743, QuestDialogAction.SETPRO1.id()),
			start(204768, QuestDialogAction.SETPRO2.id()),
			start(204808, QuestDialogAction.SETPRO3.id()),
			new Step(204808, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), QuestStatus.START, 4),
			start(204808, QuestDialogAction.SETPRO5.id()),
			reward(204768, QuestDialogAction.SELECT_QUEST_REWARD.id(), 5))),
			204768, null));

	private static final Path CLIENT_ACTIONS = Path.of(
		"docs/quest/client-dialog-mapping/quest-dialog-action-details.csv");

	/** 客户端 HTML 上属于「推进类」的按钮动作符号（与全库审计脚本口径一致，锚定整符号）。 */
	private static final Pattern PROGRESS_ACTION = Pattern.compile(
		"HACTION_(SETPRO\\d+|SELECT\\d+_\\d+(?:_\\d+)*|SELECT_QUEST_REWARD|SET_SUCCEED)");

	@Test
	void everyClientProgressButtonOfTheRepairedFamilyIsRouted() throws Exception {
		assertTrue(Files.isRegularFile(CLIENT_ACTIONS), "missing in-repo client action mapping " + CLIENT_ACTIONS);
		Map<Integer, Set<Integer>> expected = clientProgressActionIds();
		for (Spec spec : SPECS) {
			Set<Integer> declared = new LinkedHashSet<>();
			for (QuestTransition transition : definition(spec.questId()).transitions()) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
					declared.add(talk.dialogId());
				}
			}
			Set<Integer> buttons = expected.getOrDefault(spec.questId(), Set.of());
			assertTrue(!buttons.isEmpty(),
				"quest " + spec.questId() + " 的客户端推进动作清单为空，客户端映射证据缺失");
			for (int dialogId : buttons) {
				assertTrue(declared.contains(dialogId),
					"quest " + spec.questId() + " 缺少客户端按钮 " + dialogId + " 的路由（点击无响应 / 步骤卡死）");
			}
		}
	}

	@Test
	void everyJournalRowHasItsOwnStateAndTheRewardProjectionStaysOnTheRewardRow() throws Exception {
		for (Spec spec : SPECS) {
			QuestDefinition definition = definition(spec.questId());
			Set<Integer> rows = var0Of(definition, QuestStatus.START);
			rows.addAll(var0Of(definition, QuestStatus.REWARD));
			Set<Integer> expectedRows = new LinkedHashSet<>();
			for (int row = 0; row < spec.rows(); row++) {
				expectedRows.add(row);
			}
			assertEquals(expectedRows, rows,
				"quest " + spec.questId() + " 的每个客户端任务书行都必须有 var0==行号的 START/REWARD 状态");
			assertEquals(Set.of(spec.rows() - 1), var0Of(definition, QuestStatus.REWARD),
				"quest " + spec.questId() + " 的领奖态必须投影在领奖行行号上");
		}
	}

	@Test
	void everyPathWalksRowByRowAtRuntime() throws Exception {
		for (Spec spec : SPECS) {
			for (int pathIndex = 0; pathIndex < spec.paths().size(); pathIndex++) {
				int branchIndex = pathIndex;
				List<Step> path = spec.paths().get(pathIndex);
				CompiledQuestDefinition compiled = compiled(spec.questId());
				QuestDefinition definition = compiled.definition();
				Map<String, Integer> variables = new LinkedHashMap<>(definition.progressLayout().unpack(0));
				QuestStatus status = QuestStatus.START;
				for (int index = 0; index < path.size(); index++) {
					Step step = path.get(index);
					int stepIndex = index;
					QuestStatus currentStatus = status;
					Map<String, Integer> currentVariables = Map.copyOf(variables);
					String sourceLabel = sourceLabelOf(definition, currentStatus, currentVariables);
					QuestTransition advance = talkToNpcFrom(definition, sourceLabel, step.npcId(),
						step.actionId());
					QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
						snapshot(compiled, currentStatus, variables), advance.event(), advance)
						.orElseThrow(() -> new AssertionError("quest " + spec.questId() + " 路径 " + branchIndex
							+ " 第 " + stepIndex + " 步（npc=" + step.npcId() + " action=" + step.actionId()
							+ "）无法在 " + currentStatus + currentVariables + " 状态下提交"));
					status = plan.nextStatus();
					variables = definition.progressLayout().unpack(plan.nextPackedVariables());
					assertEquals(step.statusAfter(), status,
						"quest " + spec.questId() + " 路径 " + branchIndex + " 第 " + stepIndex + " 步后的状态不符合客户端链");
					if (step.var0After() >= 0) {
						assertEquals(step.var0After(), variables.get("var0"),
							"quest " + spec.questId() + " 路径 " + branchIndex + " 第 " + stepIndex
								+ " 步后必须停在客户端行号上");
					}
				}
				assertEquals(QuestStatus.REWARD, status,
					"quest " + spec.questId() + " 路径 " + branchIndex + " 走完客户端链后必须停在领奖态");
			}
		}
	}

	@Test
	void onlyTheRewardRowNpcOwnsCompletion() throws Exception {
		for (Spec spec : SPECS) {
			QuestDefinition definition = definition(spec.questId());
			List<Integer> completionOwners = definition.transitions().stream()
				.filter(transition -> isComplete(definition, transition.targetNode()))
				.map(transition -> (QuestEvent.TalkToNpc) transition.event())
				.map(QuestEvent.TalkToNpc::npcId)
				.distinct()
				.sorted()
				.toList();
			assertEquals(List.of(spec.rewardNpc()), completionOwners,
				"quest " + spec.questId() + " 只有领奖行 NPC 能完成任务（多个 completion 让任意步骤都能提前领奖）");
		}
	}

	@Test
	void rewardRowServesItsOwnClientEntryPage() throws Exception {
		for (Spec spec : SPECS) {
			if (spec.rewardEntryPage() == null) {
				continue;
			}
			QuestDefinition definition = definition(spec.questId());
			assertTrue(definition.transitions().stream().anyMatch(transition ->
					transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == spec.rewardNpc()
						&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						&& transition.afterCommit().contains(
							new AfterCommitAction.ShowQuestDialog(spec.rewardEntryPage().id()))),
				"quest " + spec.questId() + " 的领奖行缺少 " + spec.rewardEntryPage().name() + " 入口页");
		}
	}

	/**
	 * 2962 的两条报告分支各自带一档奖励窗口（客户端 select_quest_reward1/2 = 页面 5/6），
	 * 对应旧 handler 的 {@code 5 + rewIdex} 与 {@code sendQuestEndDialog(env, rewIdex)}。
	 * 2962's two report branches carry their own reward window tier (client pages 5/6), matching the legacy
	 * handler's {@code 5 + rewIdex} and {@code sendQuestEndDialog(env, rewIdex)}.
	 */
	@Test
	void twoBranchReportQuestKeepsBothRewardTiers() throws Exception {
		QuestDefinition definition = definition(2962);
		assertEquals(List.of(6), definition.progressLayout().fields().stream()
			.filter(field -> field.name().equals("var1"))
			.map(BitField::offset).toList(),
			"var1 必须落在 offset 6（客户端任务书行索引只读 SECTION_0 低 6 位）");

		QuestTransition tierOne = talkToNpc(definition, 204253, QuestDialogAction.SETPRO3.id());
		QuestTransition tierTwo = talkToNpc(definition, 204253, QuestDialogAction.SETPRO4.id());
		assertEquals("reward1", tierOne.targetNode(), "SETPRO3 必须进入第 1 档领奖态");
		assertEquals("reward2", tierTwo.targetNode(), "SETPRO4 必须进入第 2 档领奖态");

		// 领奖态入口必须下发本档领奖窗口：步骤 3 的分支页 select3_1/select3_2 上的按钮（SETPRO3/SETPRO4
		// 与两个分支互跳）只在 s2 有路由，领奖态下发这两页会让玩家点下去毫无反应。
		// The reward-state entry must serve this tier's own window: the step-3 branch pages' buttons are only
		// routed from s2, so serving them in the reward state dead-ends the player.
		assertRewardWindow(definition, "reward1", QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertRewardWindow(definition, "reward2", QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		Set<Integer> branchPages = Set.of(QuestDialogPage.SELECT3_1.id(), QuestDialogPage.SELECT3_2.id());
		assertTrue(definition.transitions().stream()
				.filter(transition -> transition.sourceNode() != null
					&& transition.sourceNode().startsWith("reward"))
				.noneMatch(transition -> branchPages.contains(servedPage(transition))),
			"领奖态不得下发步骤 3 的分支页 select3_1(1694)/select3_2(1779)："
				+ "这两页的可见按钮只有 s2 有路由，玩家会被卡在对话里");
		assertEquals(Set.of(0, 1), completionRewardIndices(definition),
			"两档领奖态必须各自携带 complete-reward-index 0/1");
	}

	/**
	 * 4542 的行 3 是收集交付行：成功分支校验并扣除艾卡的钥匙（182215329）并推进行号，
	 * 失败分支留在本行并下发客户端失败页。
	 * 4542 row 3 is the collect row: the success branch checks and consumes Aika's key (182215329) and advances
	 * the row, while the fallback stays on the row and serves the client failure page.
	 */
	@Test
	void collectRowChecksAndConsumesTheRetailKey() throws Exception {
		QuestDefinition definition = definition(4542);
		List<QuestTransition> checks = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204808
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())
			.sorted((left, right) -> Integer.compare(priority(left), priority(right)))
			.toList();
		assertEquals(2, checks.size(), "4542 的收集检查必须有成功与失败两条互斥分支");

		QuestTransition success = checks.getFirst();
		assertTrue(success.conditions().contains(new QuestCondition.HasItem(182215329, 1)),
			"成功分支必须要求艾卡的钥匙 182215329");
		assertTrue(success.actions().contains(new QuestAction.RemoveItem(182215329, 1)),
			"成功分支必须扣除艾卡的钥匙 182215329");
		assertEquals(QuestDialogPage.CHECK_USER_ITEM_OK.id(), servedPage(success),
			"成功分支必须下发客户端成功页");
		assertEquals(QuestStatus.START, statusOf(definition, success.targetNode()),
			"收集成功只推进任务书行号，仍在进行中");

		QuestTransition fallback = checks.get(1);
		assertTrue(fallback.conditions().isEmpty(), "失败分支是无条件兜底");
		assertEquals(QuestDialogPage.CHECK_USER_ITEM_FAIL.id(), servedPage(fallback),
			"失败分支必须下发客户端失败页");
		assertEquals(success.sourceNode(), fallback.targetNode(), "失败分支必须留在本行等待重新收集");

		QuestTransition handover = talkToNpc(definition, 204768, QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id(), servedPage(handover),
			"交出钥匙后必须下发领奖窗口 1");
	}

	private static void assertRewardWindow(QuestDefinition definition, String rewardNode, int actionId,
			QuestDialogPage page) {
		assertTrue(definition.transitions().stream().anyMatch(transition ->
				rewardNode.equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == actionId
					&& transition.afterCommit().contains(
						new AfterCommitAction.ShowQuestDialog(page.id()))),
			"领奖态 " + rewardNode + " 缺少 " + page.name() + " 入口页");
	}

	private static Set<Integer> completionRewardIndices(QuestDefinition definition) {
		Set<Integer> indices = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.CompleteQuest complete) {
					indices.add(complete.rewardIndex());
				}
			}
		}
		return indices;
	}

	private static int priority(QuestTransition transition) {
		return transition.priority() == null ? Integer.MAX_VALUE : transition.priority();
	}

	private static int servedPage(QuestTransition transition) {
		return transition.afterCommit().stream()
			.filter(AfterCommitAction.ShowQuestDialog.class::isInstance)
			.map(AfterCommitAction.ShowQuestDialog.class::cast)
			.map(AfterCommitAction.ShowQuestDialog::dialogId)
			.findFirst().orElse(-1);
	}

	private static QuestStatus statusOf(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> node.label().equals(label))
			.map(node -> node.projection().status())
			.findFirst().orElseThrow();
	}

	private static boolean isComplete(QuestDefinition definition, String label) {
		return statusOf(definition, label) == QuestStatus.COMPLETE;
	}

	private static Set<Integer> var0Of(QuestDefinition definition, QuestStatus status) {
		Set<Integer> values = new LinkedHashSet<>();
		for (QuestNode node : definition.nodes()) {
			if (node.projection().status() == status && node.projection().variables().get("var0") != null) {
				values.add(node.projection().variables().get("var0"));
			}
		}
		return values;
	}

	/**
	 * 当前行号对应的状态标签：自动演出/领奖行都可能有多个节点共用 var0，必须唯一才可定位。
	 * State label for the current row index; it must be unique so the walk can bind the row's own route.
	 */
	private static String sourceLabelOf(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		int var0 = variables.get("var0");
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().get("var0") == var0)
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(),
			"quest " + definition.id() + " 的 " + status + "(var0=" + var0 + ") 状态必须唯一才能定位该行");
		return labels.getFirst();
	}

	private static QuestTransition talkToNpcFrom(QuestDefinition definition, String source, int npcId,
			int actionId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null && talk.dialogId() == actionId)
			.findFirst().orElseThrow(() -> new AssertionError("quest " + definition.id() + " 缺少 " + source
				+ " -> npc=" + npcId + " action=" + actionId + " 的路由"));
	}

	private static QuestTransition talkToNpc(QuestDefinition definition, int npcId, int actionId) {
		return definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null && talk.dialogId() == actionId)
			.findFirst().orElseThrow(() -> new AssertionError(
				"quest " + definition.id() + " 缺少 npc=" + npcId + " action=" + actionId + " 的路由"));
	}

	/**
	 * 客户端任务书行上的推进按钮动作 id（仓库内客户端映射 CSV 为权威证据，不依赖外部解包目录）。
	 * Client advance-button ids from the in-repo client mapping CSV, so the gate needs no external unpack.
	 */
	private static Map<Integer, Set<Integer>> clientProgressActionIds() throws Exception {
		Map<Integer, Set<Integer>> expected = new LinkedHashMap<>();
		List<String> lines = Files.readAllLines(CLIENT_ACTIONS);
		for (String line : lines.subList(1, lines.size())) {
			List<String> fields = splitCsv(line);
			if (fields.size() < 14) {
				continue;
			}
			if (!PROGRESS_ACTION.matcher(fields.get(13).trim()).matches()) {
				continue;
			}
			String actionId = fields.get(12).trim();
			if (actionId.isEmpty()) {
				// 客户端符号没有对应动作 id（action_mapping=missing），不属于可路由按钮。
				// The client symbol has no action id (action_mapping=missing); it is not a routable button.
				continue;
			}
			expected.computeIfAbsent(Integer.parseInt(fields.get(0).trim()),
				ignored -> new LinkedHashSet<>()).add(Integer.parseInt(actionId));
		}
		return expected;
	}

	/** 极简 CSV 拆分（href 字段可能带引号并内含逗号）。 / Minimal CSV split for quoted hrefs containing commas. */
	private static List<String> splitCsv(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char character = line.charAt(i);
			if (quoted) {
				if (character == '"') {
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						current.append('"');
						i++;
					} else {
						quoted = false;
					}
				} else {
					current.append(character);
				}
			} else if (character == '"') {
				quoted = true;
			} else if (character == ',') {
				fields.add(current.toString());
				current.setLength(0);
			} else {
				current.append(character);
			}
		}
		fields.add(current.toString());
		return fields;
	}

	private static QuestDefinition definition(int questId) throws Exception {
		return compiled(questId).definition();
	}

	private static CompiledQuestDefinition compiled(int questId) throws Exception {
		try (InputStream input = QuestBClassRouteContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(variables),
			inventory(definition), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	/**
	 * 收集/交付类步骤要求快照携带任务物品，否则 has-item 条件路由不会命中。
	 * Collect and turn-in steps need the quest items in the snapshot, otherwise the has-item route cannot match.
	 */
	private static Map<Integer, Integer> inventory(CompiledQuestDefinition definition) {
		Map<Integer, Integer> items = new LinkedHashMap<>();
		for (QuestItemRequirement requirement : definition.definition().metadata().itemRequirements()) {
			items.put(requirement.itemId(), requirement.count());
		}
		for (QuestItemRequirement requirement : definition.definition().metadata().questWorkItems()) {
			items.put(requirement.itemId(), requirement.count());
		}
		return Map.copyOf(items);
	}
}
