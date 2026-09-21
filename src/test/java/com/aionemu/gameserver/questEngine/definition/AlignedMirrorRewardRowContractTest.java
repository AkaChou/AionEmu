package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestWorldFacts;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证“领奖行投影 = 客户端任务书最后一行”在镜像一侧已对齐的任务族里逐对成立：
 * 11294/21294、15002/25002、15010/25010、15070/25070、15514/25514、19004/29004、
 * 25062/15062、25073/15073、26820/16820。旧投影落后一行的 9 个任务改为与镜像相同的领奖行，
 * 并补上无 source 的进入世界自愈边，避免已落盘的 REWARD/旧行号存档卡在领奖流程外。
 * Verifies the “reward projection equals the client journal last row” contract for the mirror pairs
 * whose other side already aligned: 11294/21294, 15002/25002, 15010/25010, 15070/25070,
 * 15514/25514, 19004/29004, 25062/15062, 25073/15073 and 26820/16820. The nine quests whose
 * projection lagged one row now match their mirror, and each gained a source-less enter-world
 * recovery edge so that saves persisted as REWARD/old-row are repaired instead of being stuck.
 */
class AlignedMirrorRewardRowContractTest {

	private record Contract(int questId, int mirrorId, String source, QuestEvent event,
			Map<Integer, Integer> inventory, String zone, int staleRow, int rewardRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(11294, 21294, "started",
			new QuestEvent.TalkToNpc(799010, QuestDialogAction.SETPRO1.id()),
			Map.of(182213038, 1), null, 0, 1),
		new Contract(15002, 25002, "started",
			new QuestEvent.TalkToNpc(804698, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215663, 7), null, 0, 1),
		new Contract(15010, 25010, "started",
			new QuestEvent.TalkToNpc(804700, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215664, 5, 182215665, 3), null, 0, 1),
		new Contract(15070, 25070, "started",
			new QuestEvent.TalkToNpc(804891, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215682, 10), null, 0, 1),
		new Contract(15514, 25514, "started",
			new QuestEvent.TalkToNpc(806093, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
			Map.of(182215975, 10), null, 0, 1),
		new Contract(19004, 29004, "s1",
			new QuestEvent.TalkToNpc(203701, QuestDialogAction.SETPRO2.id()),
			Map.of(), null, 1, 2),
		new Contract(25062, 15062, "started",
			new QuestEvent.TalkToNpc(804917, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
			Map.of(182215722, 10), null, 0, 1),
		new Contract(25073, 15073, "started",
			new QuestEvent.TalkToNpc(731556, QuestDialogAction.SET_SUCCEED.id()),
			Map.of(182215725, 1), null, 0, 1),
		new Contract(26820, 16820, "s1",
			new QuestEvent.EnterZone("IDEternity_02_SensoryArea_Q16820a"),
			Map.of(), "IDEternity_02_SensoryArea_Q16820a", 1, 2));

	@Test
	void rewardRowMatchesTheClientJournalLastRowAndTheAlignedMirror() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(contract.rewardRow(), rewardRow(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(contract.rewardRow(), rewardRow(definition(contract.mirrorId()).definition()),
				() -> "mirror " + contract.mirrorId() + " reward journal row");
		}
	}

	@Test
	void handoversAdvanceTheJournalToTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition handover = handoverRoute(compiled.definition(), contract);
			QuestSnapshot snapshot = snapshot(compiled, QuestStatus.START,
				Map.of("var0", contract.staleRow()), contract.inventory());
			if (contract.zone() != null) {
				snapshot = snapshot.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of(contract.zone())));
			}
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot,
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " handover status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " handover journal row");
		}
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", contract.staleRow())), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan recoveryPlan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", contract.staleRow()), Map.of()),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, recoveryPlan.nextStatus());
			assertEquals(contract.rewardRow(), unpack(compiled, recoveryPlan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			int row = rewardRow(definition);
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(row, value, () -> "quest " + contract.questId()
							+ " route " + route.sourceNode() + " writes a non reward journal row");
					}
				}
			}
		}
	}

	private static QuestTransition handoverRoute(QuestDefinition definition, Contract contract) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> contract.source().equals(candidate.sourceNode()))
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(contract.event()))
			.toList();
		assertFalse(matches.isEmpty(),
			() -> "quest " + contract.questId() + " route " + contract.event());
		if (matches.size() > 1) {
			// NPC_REPORT 简写与显式交付路线可能共用同一事件；带 has-item 守卫的是交付路线。
			// NPC_REPORT shorthands may share the event with the explicit turn-in route; the one
			// guarded by has-item is the turn-in route.
			matches = matches.stream().filter(candidate -> candidate.conditions().stream()
				.anyMatch(QuestCondition.HasItem.class::isInstance)).toList();
		}
		assertEquals(1, matches.size(), () -> "quest " + contract.questId() + " route " + contract.event());
		return matches.getFirst();
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = AlignedMirrorRewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
