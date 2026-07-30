package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphFlightTeleportAdapter.FlightSession;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphFlightTeleportAdapter.FlightTeleportCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

class QuestGraphFlightTeleportAdapterTest {

	@Test
	void appliesFormalPathAndReplaysTheFrozenCommandOnce() {
		FakeFlightSession session = new FakeFlightSession(7, true);
		QuestGraphFlightTeleportAdapter adapter = new QuestGraphFlightTeleportAdapter(7, pathId -> pathId == 31, session);
		FlightTeleportCommand command = command(1929, 7, 31, "flight-31");

		assertEquals(PreflightResult.READY, adapter.preflight(command));
		assertEquals(ActionResult.APPLIED, adapter.execute(command));
		assertEquals(CreatureState.FLIGHT_TELEPORT.getId(), session.state);
		assertEquals(31001, session.protocolId);
		assertEquals(1, session.sends);
		assertEquals(31001, session.lastSentProtocolId);
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(command));
		assertEquals(1, session.sends);
		session.connected = false;
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(command));
		assertEquals(1, adapter.size());
	}

	@Test
	void failsWithoutConnectionBeforeAnyStateMutation() {
		FakeFlightSession session = new FakeFlightSession(7, false);
		QuestGraphFlightTeleportAdapter adapter = new QuestGraphFlightTeleportAdapter(7, pathId -> true, session);
		FlightTeleportCommand command = command(1002, 7, 1, "offline");

		assertEquals(PreflightResult.FAILED, adapter.preflight(command));
		assertEquals(ActionResult.FAILED, adapter.execute(command));
		assertEquals(CreatureState.ACTIVE.getId(), session.state);
		assertEquals(0, session.protocolId);
		assertEquals(0, session.sends);
		assertEquals(0, adapter.size());
	}

	@Test
	void rejectsWrongOwnerMissingPathAndIdempotencyCollision() {
		FakeFlightSession session = new FakeFlightSession(7, true);
		QuestGraphFlightTeleportAdapter adapter = new QuestGraphFlightTeleportAdapter(7, pathId -> pathId == 1, session);

		assertEquals(ActionResult.FAILED, adapter.execute(command(1002, 8, 1, "wrong-owner")));
		assertEquals(ActionResult.REJECTED, adapter.execute(command(1393, 7, 17, "missing-path")));
		FlightTeleportCommand first = command(1002, 7, 1, "stable-key");
		assertEquals(ActionResult.APPLIED, adapter.execute(first));
		assertEquals(ActionResult.REJECTED, adapter.execute(command(1006, 7, 1, "stable-key")));
		assertEquals(1, session.sends);
	}

	@Test
	void rejectsAnotherActivePathButAcceptsTheSameActiveProtocol() {
		FakeFlightSession session = new FakeFlightSession(7, true);
		session.state = CreatureState.FLIGHT_TELEPORT.getId();
		session.protocolId = 3001;
		QuestGraphFlightTeleportAdapter adapter = new QuestGraphFlightTeleportAdapter(7,
			pathId -> pathId == 1 || pathId == 3, session);

		FlightTeleportCommand samePath = command(2008, 7, 3, "same-active-path");
		assertEquals(PreflightResult.READY, adapter.preflight(samePath));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(samePath));
		assertEquals(0, session.sends);
		assertEquals(ActionResult.REJECTED, adapter.execute(command(1002, 7, 1, "different-active-path")));
		assertEquals(3001, session.protocolId);
		assertEquals(CreatureState.FLIGHT_TELEPORT.getId(), session.state);
	}

	@Test
	void rollsBackStateWhenPacketDeliveryThrows() {
		FakeFlightSession session = new FakeFlightSession(7, true);
		session.failSend = true;
		QuestGraphFlightTeleportAdapter adapter = new QuestGraphFlightTeleportAdapter(7, pathId -> pathId == 1, session);

		assertEquals(ActionResult.FAILED, adapter.execute(command(1002, 7, 1, "send-failed")));
		assertEquals(CreatureState.ACTIVE.getId(), session.state);
		assertEquals(0, session.protocolId);
		assertEquals(1, session.sends);
		assertEquals(0, adapter.size());
	}

	@Test
	void validatesTheFlightProtocolRelation() {
		assertThrows(IllegalArgumentException.class, () -> new FlightTeleportCommand(1002, 7, 0, 1, "zero-path"));
		assertThrows(IllegalArgumentException.class, () -> new FlightTeleportCommand(1002, 7, 1, 1002, "wrong-protocol"));
		assertThrows(IllegalArgumentException.class, () -> new FlightTeleportCommand(1002, 7, 1, 1001, " "));
	}

	private static FlightTeleportCommand command(int questId, int playerId, int pathId, String key) {
		return new FlightTeleportCommand(questId, playerId, pathId, pathId * 1000 + 1, key);
	}

	private static final class FakeFlightSession implements FlightSession {
		private final int playerId;
		private boolean connected;
		private int state = CreatureState.ACTIVE.getId();
		private int protocolId;
		private int sends;
		private int lastSentProtocolId;
		private boolean failSend;

		private FakeFlightSession(int playerId, boolean connected) {
			this.playerId = playerId;
			this.connected = connected;
		}

		@Override
		public int playerId() {
			return playerId;
		}

		@Override
		public boolean hasConnection() {
			return connected;
		}

		@Override
		public int state() {
			return state;
		}

		@Override
		public boolean isFlightTeleport() {
			return (state & CreatureState.FLIGHT_TELEPORT.getId()) != 0;
		}

		@Override
		public int protocolId() {
			return protocolId;
		}

		@Override
		public void setFlightTeleport() {
			state |= CreatureState.FLIGHT_TELEPORT.getId();
		}

		@Override
		public void unsetActive() {
			state &= ~CreatureState.ACTIVE.getId();
		}

		@Override
		public void setProtocolId(int protocolId) {
			this.protocolId = protocolId;
		}

		@Override
		public void sendStart(int protocolId) {
			sends++;
			lastSentProtocolId = protocolId;
			if (failSend) {
				throw new IllegalStateException("send failed");
			}
		}

		@Override
		public void restore(int state, int protocolId) {
			this.state = state;
			this.protocolId = protocolId;
		}
	}
}
