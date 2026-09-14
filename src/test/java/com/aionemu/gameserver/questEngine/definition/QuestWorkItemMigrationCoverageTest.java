package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 1192「贝尔特伦要塞的支援请求」的工作物品契约，并审计全部生产任务
 * 不得丢失 legacy {@code quest_work_items} 的迁移。
 * Locks quest 1192's work-item contract and audits that every production
 * definition preserves its legacy {@code quest_work_items} migration.
 *
 * <p>权威来源是 {@code quest_data.xml} 的 {@code <quest_work_items>}。旧引擎
 * {@code QuestService.setFinishingState} 在完成时无条件清理这些物品；typed
 * 引擎只在 metadata 声明了 {@code <work-items>} 时执行同样的清理
 * （{@code QuestMutationPlanner.appendCompletionWorkItemCleanup}）。迁移时漏掉该
 * 声明、又没有任何显式 {@code remove-item} 覆盖该物品时，任务完成后道具必然残留在背包，
 * 1192 的 182200556 即为此形态。
 * The authority is {@code quest_data.xml}'s {@code <quest_work_items>}. The legacy
 * engine unconditionally cleared these items on completion; the typed engine only
 * mirrors that when the metadata declares {@code <work-items>}. A definition that
 * dropped the declaration and carries no explicit {@code remove-item} for the item
 * necessarily leaves it in the inventory, which is quest 1192's 182200556.</p>
 */
class QuestWorkItemMigrationCoverageTest {
	private static final Path QUEST_DATA = Path.of(
		"src/main/resources/aion/data/static_data/quest_data/quest_data.xml");
	private static final int QUEST_1192 = 1192;
	private static final int WORK_ITEM_1192 = 182200556;

	/**
	 * 证据冲突, 需人工裁定, 不纳入自动断言:旧 handler 完成时移除 186000085(收集物),
	 * quest_data.xml 声明 work item 为 182207122,而当前 XML 声明 6 个 152206* 分支图纸
	 * (sN->s7 时 give-item 发放)。三者互不相同,无法据此自动收敛。
	 * Conflicting evidence pending human adjudication; excluded from the automated assertion.
	 */
	private static final Set<Integer> EVIDENCE_CONFLICT_QUESTS = Set.of(4942);

	@Test
	void verteronReinforcementsDeclaresItsWorkItemAndTurnsItInAtLavirintos() {
		QuestDefinition definition = definition(QUEST_1192);
		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM_1192, 1)),
			definition.metadata().questWorkItems());

		// 接取发放工作物品:与 quest_data.xml 的 work item 声明一致。
		QuestTransition accept = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203098
				&& talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id())
			.findFirst().orElseThrow();
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(WORK_ITEM_1192, 1)));

		// retai zz_retail_simple_quests.xml:8533 要求 203701 处交出物品;
		// 用 ALL 表达「有则交出、旧存档缺失也不阻断」。
		QuestTransition handover = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203701
				&& talk.dialogId() == QuestDialogAction.SETPRO1.id())
			.findFirst().orElseThrow();
		assertTrue(handover.actions().contains(
			new QuestAction.RemoveItem(WORK_ITEM_1192, QuestAction.RemoveItem.ALL)));
	}

	@Test
	void everyExecutableWorkItemQuestIsCleanedUpOnCompletion() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Map<Integer, List<Integer>> legacyWorkItems = legacyWorkItems();

		List<String> omitted = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : legacyWorkItems.entrySet()) {
			int questId = entry.getKey();
			var executable = catalog.findExecutable(questId);
			if (executable.isEmpty() || EVIDENCE_CONFLICT_QUESTS.contains(questId)) {
				continue;
			}
			// legacy 的顺序差异不构成契约差异, 按集合比较。
			List<Integer> declared = executable.orElseThrow().definition().metadata()
				.questWorkItems().stream().map(QuestItemRequirement::itemId).toList();
			List<Integer> missing = entry.getValue().stream()
				.filter(itemId -> !declared.contains(itemId)).toList();
			if (!missing.isEmpty()) {
				omitted.add(questId + " legacy=" + entry.getValue()
					+ " declared=" + declared + " missing=" + missing);
			}
		}

		assertEquals(List.of(), omitted,
			"the following quests declare <work-items> that omit legacy quest_work_items;"
				+ " completion then leaves those items in the inventory");
	}

	@Test
	void everyItemEnteringRewardIsHandedInOrDeclaredAsAWorkItem() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> defects = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : legacyWorkItems().entrySet()) {
			int questId = entry.getKey();
			var executable = catalog.findExecutable(questId);
			if (executable.isEmpty()) {
				continue;
			}
			QuestDefinition definition = executable.orElseThrow().definition();
			if (!definition.metadata().questWorkItems().isEmpty()) {
				// 引擎在完成/放弃时清理声明的 work items, 不依赖单条路由。
				continue;
			}
			// 未声明时, 每一条真正进入 REWARD 的路由都必须自己交出物品:
			// 1192 的 182200556 正是漏在这一条上。
			Map<String, QuestStatus> statuses = definition.nodes().stream().collect(Collectors
				.toMap(QuestNode::label, node -> node.projection().status(), (left, right) -> left));
			for (QuestTransition transition : definition.transitions()) {
				if (statuses.get(transition.targetNode()) != QuestStatus.REWARD
						|| statuses.get(transition.sourceNode()) == QuestStatus.REWARD) {
					continue;
				}
				List<Integer> notHandedIn = entry.getValue().stream()
					.filter(itemId -> transition.actions().stream().noneMatch(action ->
						action instanceof QuestAction.RemoveItem remove && remove.itemId() == itemId))
					.toList();
				if (!notHandedIn.isEmpty()) {
					defects.add(questId + " " + transition.sourceNode() + "->REWARD keeps " + notHandedIn);
				}
			}
		}
		assertEquals(List.of(), defects,
			"these quests neither declare <work-items> nor hand the item in on every REWARD entry;"
				+ " the completing player keeps the item");
	}

	private static Map<Integer, List<Integer>> legacyWorkItems() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Element root = factory.newDocumentBuilder().parse(QUEST_DATA.toFile()).getDocumentElement();
		NodeList quests = root.getElementsByTagName("quest");
		Map<Integer, List<Integer>> result = new LinkedHashMap<>();
		for (int i = 0; i < quests.getLength(); i++) {
			Element quest = (Element) quests.item(i);
			NodeList blocks = quest.getElementsByTagName("quest_work_items");
			if (blocks.getLength() == 0) {
				continue;
			}
			NodeList items = ((Element) blocks.item(0)).getElementsByTagName("quest_work_item");
			List<Integer> itemIds = new ArrayList<>();
			for (int j = 0; j < items.getLength(); j++) {
				itemIds.add(Integer.parseInt(((Element) items.item(j)).getAttribute("item_id")));
			}
			if (!itemIds.isEmpty()) {
				result.put(Integer.parseInt(quest.getAttribute("id")), itemIds);
			}
		}
		return result;
	}

	private static QuestDefinition definition(int questId) {
		return QuestDefinitionDirectoryLoader.compile(QuestWorkItemMigrationCoverageTest.class
			.getClassLoader()).findExecutable(questId).orElseThrow().definition();
	}
}
