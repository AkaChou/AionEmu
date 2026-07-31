package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphQuestStatusSyncAdapter.QuestStatusSyncCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestStatusSyncSnapshot;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

class QuestGraphQuestStatusSyncAdapterTest {

	private static final DialogEvent EVENT = new DialogEvent("sync", 7, 1_700_000_000_000L, 203709, "STEP_TO_1");

	@Test
	void projectsOnlyTheFrozenOccurrenceWithoutProcessLocalDeduplication() {
		AtomicInteger deliveries = new AtomicInteger();
		AtomicReference<QuestStatusSyncCommand> delivered = new AtomicReference<>();
		QuestGraphQuestStatusSyncAdapter adapter = new QuestGraphQuestStatusSyncAdapter(7, command -> {
			deliveries.incrementAndGet();
			delivered.set(command);
			return ActionResult.APPLIED;
		});
		QuestStatusSyncSnapshot snapshot = new QuestStatusSyncSnapshot(4, 2, QuestStatus.REWARD, 4161);
		ActionInvocation invocation = invocation(7, new SyncQuestStatusAction(2), snapshot, "sync:4");

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(2, deliveries.get());
		assertEquals(new QuestStatusSyncCommand(1, 7, QuestStatus.REWARD, 4161, "sync:4"), delivered.get());
		assertEquals(SM_QUEST_ACTION.class, QuestGraphQuestStatusSyncAdapter.packet(delivered.get()).getClass());
	}

	@Test
	void rejectsMissingMismatchedOrWrongOwnerSnapshotsBeforeDelivery() {
		AtomicInteger deliveries = new AtomicInteger();
		QuestGraphQuestStatusSyncAdapter adapter = new QuestGraphQuestStatusSyncAdapter(7, command -> {
			deliveries.incrementAndGet();
			return ActionResult.APPLIED;
		});
		ActionInvocation missing = new ActionInvocation(new SyncQuestStatusAction(2), 1, 4, QuestStatus.COMPLETE, EVENT,
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "missing");
		QuestStatusSyncSnapshot wrongCheckpoint = new QuestStatusSyncSnapshot(4, 1, QuestStatus.START, 0);

		assertEquals(ActionResult.FAILED, adapter.execute(missing));
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(7, new SyncQuestStatusAction(2), wrongCheckpoint, "checkpoint")));
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(8, new SyncQuestStatusAction(2), new QuestStatusSyncSnapshot(4, 2, QuestStatus.START, 0), "owner")));
		assertEquals(ActionResult.FAILED,
			adapter.execute(new ActionInvocation(new PlayMovieAction(913), 1, 4, QuestStatus.START, EVENT,
				RepeatDeadlineResolution.NOT_APPLICABLE, null, "movie")));
		assertEquals(0, deliveries.get());
	}

	@Test
	void disconnectedProductionPlayerPreservesLegacyNoOpPacketSemantics() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		QuestGraphQuestStatusSyncAdapter adapter = new QuestGraphQuestStatusSyncAdapter(player);

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, new SyncQuestStatusAction(-1),
			new QuestStatusSyncSnapshot(4, 3, QuestStatus.START, 0), "disconnected")));
	}

	private static ActionInvocation invocation(int playerId, SyncQuestStatusAction action, QuestStatusSyncSnapshot snapshot, String key) {
		DialogEvent event = new DialogEvent("sync", playerId, 1_700_000_000_000L, 203709, "STEP_TO_1");
		return new ActionInvocation(action, 1, 4, QuestStatus.COMPLETE, event, null, RepeatDeadlineResolution.NOT_APPLICABLE,
			null, null, snapshot, java.util.Map.of(), key);
	}

	private static final class TestPlayer extends Player {
		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public Integer getObjectId() {
			return 7;
		}
	}
}
