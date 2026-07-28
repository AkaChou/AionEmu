package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SkillDuplicatePolicy;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.SkillUsedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.SkillUseSource;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphSkillSignalBridge.DeduplicationGate;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphSkillSignalBridge.SkillUseSnapshot;

/** 验证 skill bridge 的服务端 authority、去重、恢复输入与 cleanup 合同。 / Verifies server authority, deduplication, recovery input, and cleanup contracts for the skill bridge. */
class QuestGraphSkillSignalBridgeTest {

	/** 验证只有完整服务端执行快照能产生可编码恢复的 skill-use 事件。 / Verifies only complete server-execution snapshots produce codec-recoverable skill-use events. */
	@Test
	void createsSkillEventOnlyFromServerAcceptedExecution() {
		SkillUsedEvent event = QuestGraphSkillSignalBridge.skillUsed("skill:41:controller", 1000,
			new SkillUseSnapshot(7, 41, 9832, 1, 99, 300040000, 12, SkillUseSource.CONTROLLER_ACCEPTED, true));

		assertEquals(9832, event.targetId());
		assertEquals(event, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(event)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphSkillSignalBridge.skillUsed("rejected", 1000,
			new SkillUseSnapshot(7, 42, 9832, 1, 99, 300040000, 12, SkillUseSource.SKILL_ACTIONS_APPLIED, false)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphSkillSignalBridge.skillUsed("missing", 1000, null));
	}

	/** 验证 RAW 保留双入口，而旧策略仅在 START owner 上执行严格 500ms 窗口。 / Verifies RAW preserves both entries while legacy policy applies a strict 500ms window only to START owners. */
	@Test
	void appliesOwnerScopedLegacyWindowWithoutSuppressingSameEventRoutes() {
		DeduplicationGate gate = new DeduplicationGate();
		SkillUsedEvent first = event("first", 7, 1000);
		SkillUsedEvent duplicate = event("duplicate", 7, 1499);

		assertTrue(gate.allow(100, QuestStatus.START, SkillDuplicatePolicy.RAW_SOURCE, first));
		assertTrue(gate.allow(100, QuestStatus.START, SkillDuplicatePolicy.RAW_SOURCE, duplicate));
		assertTrue(gate.allow(200, QuestStatus.NONE, SkillDuplicatePolicy.LEGACY_500_MILLIS, first));
		assertTrue(gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, first));
		assertTrue(gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, first));
		assertFalse(gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, duplicate));
		assertTrue(gate.allow(201, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, duplicate));
		assertTrue(gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, event("boundary", 7, 1500)));
		assertTrue(gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, event("other-player", 8, 1501)));
	}

	/** 验证登出和 owner reload 会清理全部对应临时 scope。 / Verifies logout and owner reload clear every corresponding temporary scope. */
	@Test
	void clearsTemporaryWindowsByPlayerAndOwner() {
		DeduplicationGate gate = new DeduplicationGate();
		gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, event("one", 7, 1000));
		gate.allow(201, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, event("two", 7, 1000));
		gate.allow(200, QuestStatus.START, SkillDuplicatePolicy.LEGACY_500_MILLIS, event("three", 8, 1000));
		assertEquals(3, gate.size());

		gate.clearPlayer(7);
		assertEquals(1, gate.size());
		gate.clearQuest(200);
		assertEquals(0, gate.size());
	}

	/** 构造共享技能的服务端事件。 / Builds a server event for the shared skill. */
	private static SkillUsedEvent event(String eventId, int playerId, long occurredAt) {
		return new SkillUsedEvent(eventId, playerId, occurredAt, occurredAt, 9832, 1, 0, 300040000, 12,
			SkillUseSource.CONTROLLER_ACCEPTED, true);
	}
}
