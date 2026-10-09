package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural and typed-runtime coverage for quest 1929. */
class Quest1929RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml");

	@Test
	void preservesMetadataAndAllElevenClassRewards() throws Exception {
		QuestMetadata metadata = load().definition().metadata();
		assertEquals("A Sliver Of Darkness", metadata.name());
		assertEquals(1102929, metadata.displayNameId());
		assertEquals(20, metadata.minLevel());
		assertEquals("MISSION", metadata.category());
		assertTrue(metadata.permittedRaces().contains("ELYOS"));
		assertEquals(25000, metadata.rewards().get(0).amount());
		assertEquals(457760, metadata.rewards().get(1).amount());
		assertEquals(162000048, metadata.rewards().get(2).id());
		assertEquals(11, metadata.classRewards().size());
		// 原版 reward_extend_stigma1=1：槽位资格由任务数据声明，Java 侧不再硬编码任务 ID。
		// Retail reward_extend_stigma1=1: the slot entitlement is declared by quest data, so the Java
		// side no longer hardcodes quest ids.
		assertTrue(metadata.extendStigmaSlots());

		Map<String, Integer> expected = Map.ofEntries(
			Map.entry("FIGHTER", 140001110), Map.entry("KNIGHT", 140001133),
			Map.entry("RANGER", 140001159), Map.entry("ASSASSIN", 140001146),
			Map.entry("WIZARD", 140001180), Map.entry("ELEMENTALIST", 140001204),
			Map.entry("PRIEST", 140001237), Map.entry("CHANTER", 140001218),
			Map.entry("GUNSLINGER", 140001257), Map.entry("SONGWEAVER", 140001288),
			Map.entry("AETHERTECH", 140001272));
		for (Map.Entry<String, Integer> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), metadata.classRewards().get(entry.getKey()).get(0).id());
		}
	}

	@Test
	void retainsShortcutMovieAndEquipmentBranches() throws Exception {
		CompiledQuestDefinition compiled = load();
		var transitions = compiled.definition().transitions();

		assertEquals(3, transitions.stream().filter(t -> t.event() instanceof QuestEvent.LevelUp).count());
		QuestTransition shortcut = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.LevelUp && t.targetNode().equals("complete")
				&& t.sourceNode() == null)
			.findFirst().orElseThrow();
		assertTrue(shortcut.conditions().contains(
			new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST)));

		QuestTransition movie = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.MovieEnd(int movieId) && movieId == 155)
			.findFirst().orElseThrow();
		assertEquals("spawned98", movie.targetNode());
		assertTrue(movie.actions().contains(new QuestAction.SetVariable("step", 98)));
		assertTrue(movie.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawn
			&& spawn.templateId() == 205111));

		assertEquals(4, transitions.stream().filter(t -> t.event() instanceof QuestEvent.EquipItem
			&& "spawned98".equals(t.sourceNode())).count());
		long equippedBranches = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 205111 && Integer.valueOf(-1).equals(talk.dialogId()))
			.filter(t -> t.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.EquippedItem equipped
				&& equipped.expected()))
			.count();
		long unequippedBranches = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 205111 && Integer.valueOf(-1).equals(talk.dialogId()))
			.filter(t -> t.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.EquippedItem equipped
				&& !equipped.expected()))
			.count();
		assertEquals(11, equippedBranches);
		assertEquals(11, unequippedBranches);
	}

	@Test
	void grantsTheStigmaOnceAndKeepsTheInstallStep() throws Exception {
		CompiledQuestDefinition compiled = load();
		Map<String, Integer> stoneByClass = Map.ofEntries(
			Map.entry("GLADIATOR", 140000003), Map.entry("TEMPLAR", 140000003),
			Map.entry("ASSASSIN", 140000003), Map.entry("RANGER", 140000003),
			Map.entry("SORCERER", 140000002), Map.entry("SPIRIT_MASTER", 140000002),
			Map.entry("CLERIC", 140000002), Map.entry("CHANTER", 140000003),
			Map.entry("GUNSLINGER", 140000004), Map.entry("SONGWEAVER", 140000004),
			Map.entry("AETHERTECH", 140000004));
		// 步数必须停在 98：客户端的烙印凹槽展开态跟随教学步数——98 时开启，一旦推进到 95 即关闭且会话内
		// 不可恢复（第 5–8 轮实机：与是否关窗无关、重发槽位数无效，只有重登才由登录包序重建）；
		// 原版/退役 XML 在发放时同样不推进步数（98 → 装备后才到 96）。
		// The step must stay at 98: the client's stigma-slot expansion follows the tutorial step - open at 98,
		// and once it advances to 95 the slots close and cannot be recovered in-session (live rounds 5-8:
		// independent of the window close, slot re-announces do not help, only a re-login rebuilds them).
		// Retail/retired XML likewise never advances the step on the grant (98 -> 96 only after the equip).
		assertTrue(compiled.definition().nodes().stream().noneMatch(node -> node.label().equals("granted95")));
		assertEquals(Map.of("step", 98), compiled.definition().nodes().stream()
			.filter(node -> node.label().equals("spawned98")).findFirst().orElseThrow()
			.projection().variables());

		List<QuestTransition> branches = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()) && "spawned98".equals(t.targetNode()))
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(205111, QuestDialogAction.SELECT5_2.id())))
			.toList();
		assertEquals(22, branches.size());
		int handOvers = 0;
		for (QuestTransition transition : branches) {
			String playerClass = transition.conditions().stream()
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(condition -> condition.playerClass().name()).findFirst().orElseThrow();
			int stone = stoneByClass.get(playerClass);
			assertTrue(transition.actions().stream().noneMatch(QuestAction.SetVariable.class::isInstance));
			boolean handsOverTheStone = transition.actions().stream().anyMatch(QuestAction.GiveItem.class::isInstance);
			if (handsOverTheStone) {
				handOvers++;
				assertTrue(transition.conditions().contains(new QuestCondition.HasItem(stone, 1, false)));
				assertEquals(List.of(new QuestAction.GiveItem(stone, 1)), transition.actions());
				// 交付分支：同步（携带槽位推送）后直接打开烙印窗口，不关窗
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowDialogWindow(1)), transition.afterCommit());
			} else {
				assertTrue(transition.conditions().contains(new QuestCondition.HasItem(stone, 1, true)));
				assertEquals(List.of(), transition.actions());
				assertEquals(List.of(new AfterCommitAction.ShowDialogWindow(1)), transition.afterCommit());
			}
		}
		assertEquals(11, handOvers);
	}

	@Test
	void routesHeldStoneTalksStraightToTheGuidePageAndTheWindow() throws Exception {
		CompiledQuestDefinition compiled = load();
		// 入口持有量门控（priority 0 先于 priority 1 的剧情页）：已持有结晶 ⇒ 直接出示装备引导页
		QuestEvent questSelect = new QuestEvent.TalkToNpc(205111, QuestDialogAction.QUEST_SELECT.id());
		List<QuestTransition> gated = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()) && t.event().equals(questSelect))
			.filter(t -> t.conditions().stream().anyMatch(QuestCondition.HasItem.class::isInstance))
			.toList();
		assertEquals(11, gated.size());
		for (QuestTransition transition : gated) {
			assertEquals(0, transition.priority().intValue());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogAction.SELECT5_2.id())),
				transition.afterCommit());
		}
		QuestTransition storyFallback = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()) && t.event().equals(questSelect))
			.filter(t -> t.conditions().isEmpty())
			.findFirst().orElseThrow();
		assertEquals(1, storyFallback.priority().intValue());

		// 引导页按钮（select5_3）⇒ 打开烙印窗口（页 1，由对话口绑定到进行中的任务对话对象）
		QuestTransition windowRoute = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()))
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(205111, QuestDialogAction.SELECT5_3.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowDialogWindow(1)), windowRoute.afterCommit());

		// 装备分支仍按原版形状：推进 96 + 同步 + 关窗（此时结晶已装上，槽位语义不再受影响）
		List<QuestTransition> equipRoutes = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()) && t.event() instanceof QuestEvent.EquipItem)
			.toList();
		assertEquals(4, equipRoutes.size());
		for (QuestTransition transition : equipRoutes) {
			assertEquals("equipped96", transition.targetNode());
			assertEquals(List.of(new QuestAction.SetVariable("step", 96)), transition.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), transition.afterCommit());
		}
	}

	@Test
	void plansTheStigmaGrantOnlyWhileTheStoneIsAbsent() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestEvent tutorialClick = new QuestEvent.TalkToNpc(205111, QuestDialogAction.SELECT5_2.id());
		List<QuestTransition> candidates = compiled.definition().transitions().stream()
			.filter(t -> "spawned98".equals(t.sourceNode()) && t.event().equals(tutorialClick))
			.filter(t -> t.conditions().contains(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)))
			.toList();
		assertEquals(2, candidates.size());
		QuestTransition granting = candidates.stream()
			.filter(t -> t.actions().stream().anyMatch(QuestAction.GiveItem.class::isInstance))
			.findFirst().orElseThrow();
		QuestTransition alreadyHolding = candidates.stream()
			.filter(t -> t.actions().stream().noneMatch(QuestAction.GiveItem.class::isInstance))
			.findFirst().orElseThrow();

		int packed98 = compiled.definition().progressLayout().pack(Map.of("step", 98));
		QuestSnapshot withoutStone = new QuestSnapshot(7, 1929, QuestStatus.START, packed98, Map.of(), Map.of())
			.withPlayerClass(PlayerClass.GLADIATOR);
		var grantPlan = QuestMutationPlanner.plan(compiled, withoutStone, tutorialClick, granting).orElseThrow();
		assertEquals(List.of(new QuestAction.GiveItem(140000003, 1)), grantPlan.requiredActions());
		assertEquals(packed98, grantPlan.nextPackedVariables());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowDialogWindow(1)), grantPlan.afterCommit());
		assertTrue(QuestMutationPlanner.plan(compiled, withoutStone, tutorialClick, alreadyHolding).isEmpty());

		QuestSnapshot holdingStone = new QuestSnapshot(7, 1929, QuestStatus.START, packed98,
			Map.of(140000003, 1), Map.of()).withPlayerClass(PlayerClass.GLADIATOR);
		var heldPlan = QuestMutationPlanner.plan(compiled, holdingStone, tutorialClick, alreadyHolding).orElseThrow();
		assertEquals(List.of(), heldPlan.requiredActions());
		assertEquals(packed98, heldPlan.nextPackedVariables());
		assertEquals(List.of(new AfterCommitAction.ShowDialogWindow(1)), heldPlan.afterCommit());
		assertTrue(QuestMutationPlanner.plan(compiled, holdingStone, tutorialClick, granting).isEmpty());
	}

	@Test
	void removesTheClassSpecificStigmaOnDieAndEnterWorldFailure() throws Exception {
		CompiledQuestDefinition compiled = load();
		Map<PlayerClass, Integer> expected = Map.ofEntries(
			Map.entry(PlayerClass.GLADIATOR, 140000003), Map.entry(PlayerClass.TEMPLAR, 140000003),
			Map.entry(PlayerClass.ASSASSIN, 140000003), Map.entry(PlayerClass.RANGER, 140000003),
			Map.entry(PlayerClass.SORCERER, 140000002), Map.entry(PlayerClass.SPIRIT_MASTER, 140000002),
			Map.entry(PlayerClass.CLERIC, 140000002), Map.entry(PlayerClass.CHANTER, 140000003),
			Map.entry(PlayerClass.GUNSLINGER, 140000004), Map.entry(PlayerClass.SONGWEAVER, 140000004),
			Map.entry(PlayerClass.AETHERTECH, 140000004));

		var die = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.Die)
			.toList();
		assertEquals(11, die.size());
		for (QuestTransition transition : die) {
			PlayerClass playerClass = transition.conditions().stream()
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(QuestCondition.AdvancedClassIs::playerClass).findFirst().orElseThrow();
			QuestAction.UnequipItem action = transition.actions().stream()
				.filter(QuestAction.UnequipItem.class::isInstance)
				.map(QuestAction.UnequipItem.class::cast).findFirst().orElseThrow();
			assertEquals(expected.get(playerClass), action.itemId());
			assertEquals("started2", transition.targetNode());
		}

		assertEquals(11, compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld && t.targetNode().equals("started2"))
			.filter(t -> t.actions().stream().anyMatch(QuestAction.UnequipItem.class::isInstance))
			.count());
		assertEquals(11, compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld && t.targetNode().equals("postFight8"))
			.filter(t -> t.actions().stream().anyMatch(QuestAction.UnequipItem.class::isInstance))
			.count());
	}

	@Test
	void plansTheSelectableGladiatorReward() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestTransition reward = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203711 && Integer.valueOf(8).equals(talk.dialogId())
				&& t.targetNode().equals("complete"))
			.filter(t -> t.conditions().contains(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)))
			.findFirst().orElseThrow();
		int packed = compiled.definition().progressLayout().pack(Map.of("step", 9));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1929, QuestStatus.REWARD, packed, Map.of())
			.withPlayerClass(PlayerClass.GLADIATOR);
		var plan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(203711, 8), reward).orElseThrow();
		assertTrue(plan.requiredActions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward grant
			&& grant.id() == 140001110));
		assertTrue(plan.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
	}


	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
