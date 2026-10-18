package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1007/2009「典礼」的任务奖励必须按“职业 × 槽位”落到具体物品。
 * <p>
 * 这两个任务的奖励窗口按职业展示 1–3 个可选项（原版 {@code {class}_selectable_reward} 列），
 * 玩家点选后服务端必须发放对应物品。历史缺陷：迁移时只写成一条
 * {@code actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD"} 的通用完成路由，
 * 只发基础奖励（{@code wrap_equip_u_grow_up_1st_50a} = 188057339），选择奖励全部丢失。
 * <p>
 * Quest 1007/2009's reward window lists 1–3 selectable items per class (retail
 * {@code {class}_selectable_reward}); the server must grant the picked item. The historical defect
 * collapsed every slot into one generic completion route that granted only the base reward.
 */
class Quest1007ClassSelectableRewardTest {
	private static final int BASE_ITEM = 188057339;
	private static final Path ELYOS_XML = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests/1007.xml");
	private static final Path ASMODIAN_XML = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests/2009.xml");

	/** class-rewards 键 → 具体进阶职业。 / class-rewards key to the concrete advanced class. */
	private static final Map<String, PlayerClass> ADVANCED = Map.ofEntries(
		Map.entry("FIGHTER", PlayerClass.GLADIATOR), Map.entry("KNIGHT", PlayerClass.TEMPLAR),
		Map.entry("RANGER", PlayerClass.RANGER), Map.entry("ASSASSIN", PlayerClass.ASSASSIN),
		Map.entry("WIZARD", PlayerClass.SORCERER), Map.entry("ELEMENTALIST", PlayerClass.SPIRIT_MASTER),
		Map.entry("PRIEST", PlayerClass.CLERIC), Map.entry("CHANTER", PlayerClass.CHANTER),
		Map.entry("GUNSLINGER", PlayerClass.GUNSLINGER), Map.entry("SONGWEAVER", PlayerClass.SONGWEAVER),
		Map.entry("AETHERTECH", PlayerClass.AETHERTECH));

	/** 1007（天族）原版职业可选奖励。 / Quest 1007 (Elyos) retail per-class selectable rewards. */
	private static final Map<String, List<Integer>> ELYOS = Map.ofEntries(
		Map.entry("FIGHTER", List.of(100002135, 100901634, 101301513)),
		Map.entry("KNIGHT", List.of(100002135, 100901634)),
		Map.entry("RANGER", List.of(100201775, 100002135, 101701616)),
		Map.entry("ASSASSIN", List.of(100201775, 100002135, 101701616)),
		Map.entry("WIZARD", List.of(100601665, 100501553)),
		Map.entry("ELEMENTALIST", List.of(100601665, 100501553)),
		Map.entry("PRIEST", List.of(100101596, 101501613)),
		Map.entry("CHANTER", List.of(100101596, 101501613)),
		Map.entry("GUNSLINGER", List.of(101801429)),
		Map.entry("SONGWEAVER", List.of(102001457)),
		Map.entry("AETHERTECH", List.of(102101272)));

	/** 2009（魔族）原版职业可选奖励。 / Quest 2009 (Asmodian) retail per-class selectable rewards. */
	private static final Map<String, List<Integer>> ASMODIANS = Map.ofEntries(
		Map.entry("FIGHTER", List.of(100002134, 100901633, 101301512)),
		Map.entry("KNIGHT", List.of(100002134, 100901633)),
		Map.entry("RANGER", List.of(100201774, 100002134, 101701615)),
		Map.entry("ASSASSIN", List.of(100201774, 100002134, 101701615)),
		Map.entry("WIZARD", List.of(100601664, 100501552)),
		Map.entry("ELEMENTALIST", List.of(100601664, 100501552)),
		Map.entry("PRIEST", List.of(100101595, 101501612)),
		Map.entry("CHANTER", List.of(100101595, 101501612)),
		Map.entry("GUNSLINGER", List.of(101801428)),
		Map.entry("SONGWEAVER", List.of(102001456)),
		Map.entry("AETHERTECH", List.of(102101271)));

	@Test
	void elyosDeliversEveryClassChoiceAsAConcreteItem() throws Exception {
		assertClassChoices(ELYOS_XML, ELYOS);
	}

	@Test
	void asmodianDeliversEveryClassChoiceAsAConcreteItem() throws Exception {
		assertClassChoices(ASMODIAN_XML, ASMODIANS);
	}

	private static void assertClassChoices(Path xml, Map<String, List<Integer>> expected) throws Exception {
		QuestDefinition definition = load(xml).definition();
		Map<String, List<QuestReward>> declared = definition.metadata().classRewards();
		assertEquals(expected.keySet(), declared.keySet(),
			"class-rewards must cover exactly the eleven advanced classes");
		Map<String, QuestStatus> statuses = definition.nodes().stream()
			.collect(Collectors.toMap(QuestNode::label, node -> node.projection().status()));

		for (Map.Entry<String, List<Integer>> entry : expected.entrySet()) {
			List<Integer> items = entry.getValue();
			assertEquals(items, declared.get(entry.getKey()).stream().map(QuestReward::id).toList(),
				"class-rewards must match the retail item list for " + entry.getKey());
			PlayerClass advanced = ADVANCED.get(entry.getKey());
			for (int slot = 0; slot < items.size(); slot++) {
				int action = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + slot;
				int item = items.get(slot);
				int slotNumber = slot + 1;
				QuestTransition route = completionRoute(definition, statuses, action, advanced);
				assertNotNull(route, () -> "missing reward-window route: " + entry.getKey()
					+ " slot " + slotNumber + " (action " + action + ")");
				List<Integer> granted = route.actions().stream()
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.filter(reward -> reward.rewardKind() == QuestRewardKind.ITEM)
					.map(QuestAction.GrantReward::id)
					.toList();
				assertTrue(granted.contains(BASE_ITEM), () -> "missing base reward on " + entry.getKey());
				assertTrue(granted.contains(item), () -> "missing selectable reward "
					+ item + " on " + entry.getKey() + " slot " + slotNumber);
				assertEquals(1, route.actions().stream()
					.filter(QuestAction.CompleteQuest.class::isInstance).count(),
					"each reward-window route must complete the quest exactly once");
			}
		}
	}

	private static QuestTransition completionRoute(QuestDefinition definition, Map<String, QuestStatus> statuses,
			int action, PlayerClass advanced) {
		QuestCondition.AdvancedClassIs expected = new QuestCondition.AdvancedClassIs(advanced);
		return definition.transitions().stream()
			.filter(transition -> statuses.get(transition.sourceNode()) == QuestStatus.REWARD
				&& statuses.get(transition.targetNode()) == QuestStatus.COMPLETE)
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == action)
			.filter(transition -> transition.conditions().contains(expected))
			.findFirst().orElse(null);
	}

	private static CompiledQuestDefinition load(Path xml) throws Exception {
		try (InputStream input = Files.newInputStream(xml)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
