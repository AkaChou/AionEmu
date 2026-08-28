package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定主线使命与卡里佳武器兑换任务中的任务道具扣除契约。
 * Locks the item consumption contract across campaign missions and Kaliga weapon exchange quests.
 */
class MissionItemConsumptionBatchRegressionTest {

	@Test
	void kaligaWeaponExchangeQuestsConsumeKaligaKey() throws Exception {
		int[] kaligaQuests = {
			18618, 18619, 18620, 18621, 18622, 18623, 18624, 18625, 18626, 18627,
			18643, 18644, 18645, 18648,
			28618, 28619, 28620, 28621, 28622, 28623, 28624, 28625, 28626, 28627,
			28643, 28644, 28645, 28648
		};

		for (int questId : kaligaQuests) {
			QuestDefinition definition = load(questId).definition();
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(t -> "started".equals(t.sourceNode()) && "reward".equals(t.targetNode()))
				.toList();
			assertFalse(turnIns.isEmpty(), "quest " + questId + " must have started -> reward transition");
			for (QuestTransition turnIn : turnIns) {
				if (turnIn.priority() != null && turnIn.priority() == 0) {
					assertTrue(turnIn.actions().contains(new QuestAction.RemoveItem(185000102, 1)),
						"quest " + questId + " must remove Kaliga key 185000102 on turn in");
				}
			}
		}
	}

	@Test
	void campaignMissionsConsumeRequiredCollectionItems() throws Exception {
		// 10010 & 20010: 永恒之塔主线 4 项收集物扣除
		assertTransitionRemovesItems(10010, "s1", "s2", Set.of(182216171, 182216172, 182216173, 182216174));
		assertTransitionRemovesItems(20010, "s1", "s2", Set.of(182216180, 182216181, 182216182, 182216183));

		// 14025: 进军计划书扣除
		assertTransitionRemovesItems(14025, "s1", "s2", Set.of(182215323));

		// 14046: 记忆材料扣除
		assertTransitionRemovesItems(14046, "s2", "s3", Set.of(167000323, 152000309, 182215353));

		// 14051: 调查物扣除
		assertTransitionRemovesItems(14051, "s1", "s2", Set.of(182215337, 182215338));

		// 15400 & 25400: 军团援助物资扣除
		assertTransitionRemovesItems(15400, "s3", "s4", Set.of(182215897, 182215898, 182215899));
		assertTransitionRemovesItems(25400, "s3", "s4", Set.of(182215900, 182215901, 182215902));

		// 20110: 紧急情报扣除
		assertTransitionRemovesItems(20110, "s1", "s2", Set.of(182216233, 182216234, 182216235, 182216236));

		// 20112: 支援任务道具扣除
		assertTransitionRemovesItems(20112, "s4", "reward", Set.of(182216244));

		// 20525: 调查材料扣除
		assertTransitionRemovesItems(20525, "s4", "s5", Set.of(182216081, 182216082, 182216083));

		// 20529: 结界石材料扣除
		assertTransitionRemovesItems(20529, "s9", "reward", Set.of(182216090, 182216091, 182216092));

		// 24030: 命运决战证物扣除
		assertTransitionRemovesItems(24030, "s3", "s4", Set.of(182215391));

		// 2001 ~ 2006: 伊夏尔根新手使命收集物扣除
		assertTransitionRemovesItems(2001, "v1", "v2", Set.of(182203002));
		assertTransitionRemovesItems(2002, "s11", "s12", Set.of(182203003));
		assertTransitionRemovesItems(2003, "v1", "reward", Set.of(182203004));
		assertTransitionRemovesItems(2004, "v1", "v2", Set.of(182203005));
		assertTransitionRemovesItems(2005, "v1", "reward", Set.of(182203006));
		assertTransitionRemovesItems(2006, "v1", "reward", Set.of(182203008));

		// 1922 & 2947: 奥德提取装置领奖扣除
		assertTransitionRemovesItems(1922, "reward", "complete", Set.of(182206030));
		assertTransitionRemovesItems(2947, "s9", "complete", Set.of(182207037));

		// 1362 & 1367: 旁路交付与领奖交付分支道具扣除
		assertTransitionRemovesItems(1362, "started", "reward", Set.of(182201328, 182201329));
		assertTransitionRemovesItems(1367, "started", "reward", Set.of(182201331, 182201332, 182201333));
	}

	private static void assertTransitionRemovesItems(int questId, String source, String target,
			Set<Integer> expectedRemovedItemIds) throws Exception {
		QuestDefinition definition = load(questId).definition();
		List<QuestTransition> transitions = definition.transitions().stream()
			.filter(t -> source.equals(t.sourceNode()) && target.equals(t.targetNode()))
			.toList();
		assertFalse(transitions.isEmpty(), "quest " + questId + " missing transition " + source + " -> " + target);

		Set<Integer> removedInTransitions = transitions.stream()
			.flatMap(t -> t.actions().stream())
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(a -> ((QuestAction.RemoveItem) a).itemId())
			.collect(Collectors.toSet());

		assertTrue(removedInTransitions.containsAll(expectedRemovedItemIds),
			"quest " + questId + " transition " + source + " -> " + target
				+ " must remove items " + expectedRemovedItemIds + ", but removed " + removedInTransitions);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = MissionItemConsumptionBatchRegressionTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		}
	}
}
