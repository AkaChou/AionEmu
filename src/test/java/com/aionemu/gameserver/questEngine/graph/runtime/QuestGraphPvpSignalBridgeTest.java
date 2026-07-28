package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DredgionSettledEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RankedPlayerKillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphPvpSignalBridge.ParticipantSnapshot;

class QuestGraphPvpSignalBridgeTest {

	private static final ParticipantSnapshot KILLER = participant(7, Race.ELYOS, 0, true);
	private static final ParticipantSnapshot RECIPIENT = participant(8, Race.ELYOS, 3, true);
	private static final ParticipantSnapshot VICTIM = participant(9, Race.ASMODIANS, 8, true);

	/** 验证军衔击杀事件冻结 recipient、killer、victim、军衔、距离和实例。 / Verifies frozen recipient, killer, victim, rank, distance, and instance data. */
	@Test
	void rankedKillCarriesExplicitCreditAuthority() {
		RankedPlayerKillEvent event = QuestGraphPvpSignalBridge.rankedKill("ranked-kill", 1000, RECIPIENT, KILLER, VICTIM,
			12, 100);

		assertEquals(8, event.playerId());
		assertEquals(7, event.killerPlayerId());
		assertEquals(9, event.victimPlayerId());
		assertEquals(12, event.victimRankId());
		assertEquals(5.0f, event.creditDistance());
		assertEquals(1, event.instanceId());
	}

	/** 验证死亡、同阵营、越界距离和跨实例 recipient 均被拒绝。 / Verifies rejection of dead, same-faction, out-of-range, and cross-instance recipients. */
	@Test
	void rankedKillRejectsIneligibleCredit() {
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("dead", 1000,
			participant(8, Race.ELYOS, 3, false), KILLER, VICTIM, 12, 100));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("faction", 1000,
			RECIPIENT, KILLER, participant(9, Race.ELYOS, 8, true), 12, 100));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("distance", 1000,
			RECIPIENT, KILLER, VICTIM, 12, 5));
		ParticipantSnapshot crossInstance = new ParticipantSnapshot(8, Race.ELYOS, 400010000, 2, 3, 0, 0, true);
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("instance", 1000,
			crossInstance, KILLER, VICTIM, 12, 100));
	}

	/** 验证 Dredgion 结算由实例成员快照定向，且保留死亡成员旧语义。 / Verifies member-targeted Dredgion settlement including legacy dead-member eligibility. */
	@Test
	void dredgionSettlementUsesInstanceMemberSnapshot() {
		ParticipantSnapshot deadMember = participant(10, Race.ASMODIANS, 0, false);
		DredgionSettledEvent event = QuestGraphPvpSignalBridge.dredgionSettled("settled", 1001, deadMember);

		assertEquals(10, event.playerId());
		assertEquals(400010000, event.worldId());
		assertEquals(1, event.instanceId());
	}

	/** 验证参与者 snapshot 拒绝非玩家阵营和无效实例。 / Verifies participant-snapshot rejection of non-player factions and invalid instances. */
	@Test
	void snapshotsRejectInvalidAuthorityFields() {
		assertThrows(IllegalArgumentException.class,
			() -> new ParticipantSnapshot(7, Race.NPC, 400010000, 1, 0, 0, 0, true));
		assertThrows(IllegalArgumentException.class,
			() -> new ParticipantSnapshot(7, Race.ELYOS, 400010000, 0, 0, 0, 0, true));
	}

	/** 创建同一 world/instance 中的测试参与者。 / Creates a test participant in one world and instance. */
	private static ParticipantSnapshot participant(int playerId, Race race, float x, boolean alive) {
		return new ParticipantSnapshot(playerId, race, 400010000, 1, x, 0, 0, alive);
	}
}
