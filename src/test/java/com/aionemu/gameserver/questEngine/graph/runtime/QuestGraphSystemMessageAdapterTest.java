package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SystemMessageKind;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphSystemMessageAdapter.SystemMessageCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphSystemMessageAdapter.SystemMessagePacketShape;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;

class QuestGraphSystemMessageAdapterTest {

	@Test
	void buildsOnlyTheProvenPacketShapes() {
		assertPacket(SystemMessageKind.INSTANCE_DUNGEON_NEED_SOLO,
			new SystemMessagePacketShape(1403080, 0, 26, false, false));
		assertPacket(SystemMessageKind.WAREHOUSE_FULL_INVENTORY,
			new SystemMessagePacketShape(1390149, 0, 26, false, false));
		assertPacket(SystemMessageKind.COMMON_SAY_08,
			new SystemMessagePacketShape(1111307, 7, 2, false, true));
		assertEquals(1403080, SystemMessageKind.INSTANCE_DUNGEON_NEED_SOLO.code());
		assertEquals(1390149, SystemMessageKind.WAREHOUSE_FULL_INVENTORY.code());
		assertEquals(1111307, SystemMessageKind.COMMON_SAY_08.code());
	}

	@Test
	void rejectsWrongOwnerAndInvalidCommandsBeforeDelivery() {
		AtomicInteger deliveries = new AtomicInteger();
		QuestGraphSystemMessageAdapter adapter = new QuestGraphSystemMessageAdapter(7, command -> {
			deliveries.incrementAndGet();
			return ActionResult.APPLIED;
		});

		assertEquals(ActionResult.FAILED, adapter.execute(1, 8, SystemMessageKind.WAREHOUSE_FULL_INVENTORY, "wrong-owner"));
		assertEquals(ActionResult.FAILED, adapter.execute(0, 7, SystemMessageKind.WAREHOUSE_FULL_INVENTORY, "bad-quest"));
		assertEquals(ActionResult.FAILED, adapter.execute(1, 7, null, "bad-kind"));
		assertEquals(ActionResult.FAILED, adapter.execute(1, 7, SystemMessageKind.WAREHOUSE_FULL_INVENTORY, " "));
		assertEquals(0, deliveries.get());
		assertEquals(0, adapter.size());
	}

	@Test
	void acceptedDeliveryClaimsTheStableIdempotencyKey() {
		AtomicInteger deliveries = new AtomicInteger();
		AtomicReference<SystemMessageCommand> delivered = new AtomicReference<>();
		QuestGraphSystemMessageAdapter adapter = new QuestGraphSystemMessageAdapter(7, command -> {
			deliveries.incrementAndGet();
			delivered.set(command);
			return ActionResult.APPLIED;
		});

		assertEquals(ActionResult.APPLIED,
			adapter.execute(18602, 7, SystemMessageKind.COMMON_SAY_08, "system-message:1"));
		assertEquals(ActionResult.ALREADY_APPLIED,
			adapter.execute(18602, 7, SystemMessageKind.COMMON_SAY_08, "system-message:1"));
		assertEquals(1, deliveries.get());
		assertEquals(18602, delivered.get().questId());
		assertEquals(7, delivered.get().playerId());
		assertEquals(SystemMessageKind.COMMON_SAY_08, delivered.get().kind());
		assertEquals(1, adapter.size());
	}

	@Test
	void failedDirectDeliveryRequiresExplicitRetryAcceptance() {
		AtomicInteger direct = new AtomicInteger();
		AtomicInteger retries = new AtomicInteger();
		AtomicReference<SystemMessageCommand> retried = new AtomicReference<>();
		QuestGraphSystemMessageAdapter accepted = new QuestGraphSystemMessageAdapter(7, command -> {
			direct.incrementAndGet();
			throw new IllegalStateException("offline");
		}, command -> {
			retries.incrementAndGet();
			retried.set(command);
			return ActionResult.ALREADY_APPLIED;
		});

		assertEquals(ActionResult.ALREADY_APPLIED,
			accepted.execute(20032, 7, SystemMessageKind.INSTANCE_DUNGEON_NEED_SOLO, "retry-accepted"));
		assertEquals(ActionResult.ALREADY_APPLIED,
			accepted.execute(20032, 7, SystemMessageKind.INSTANCE_DUNGEON_NEED_SOLO, "retry-accepted"));
		assertEquals("retry-accepted", retried.get().idempotencyKey());
		assertEquals(1, direct.get());
		assertEquals(1, retries.get());
		assertEquals(1, accepted.size());

		AtomicInteger rejectedDirect = new AtomicInteger();
		AtomicInteger rejectedRetry = new AtomicInteger();
		QuestGraphSystemMessageAdapter rejected = new QuestGraphSystemMessageAdapter(7, command -> {
			rejectedDirect.incrementAndGet();
			return ActionResult.REJECTED;
		}, command -> {
			rejectedRetry.incrementAndGet();
			return ActionResult.FAILED;
		});
		assertEquals(ActionResult.FAILED,
			rejected.execute(10032, 7, SystemMessageKind.WAREHOUSE_FULL_INVENTORY, "retry-rejected"));
		assertEquals(ActionResult.FAILED,
			rejected.execute(10032, 7, SystemMessageKind.WAREHOUSE_FULL_INVENTORY, "retry-rejected"));
		assertEquals(2, rejectedDirect.get());
		assertEquals(2, rejectedRetry.get());
		assertEquals(0, rejected.size());
	}

	@Test
	void disconnectedProductionPlayerCannotSilentlySucceed() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		QuestGraphSystemMessageAdapter adapter = new QuestGraphSystemMessageAdapter(player, command -> ActionResult.FAILED);

		assertEquals(ActionResult.FAILED,
			adapter.execute(15300, 7, SystemMessageKind.INSTANCE_DUNGEON_NEED_SOLO, "disconnected"));
		assertEquals(0, adapter.size());
	}

	private static void assertPacket(SystemMessageKind kind, SystemMessagePacketShape expected) {
		SystemMessageCommand command = new SystemMessageCommand(1, 7, kind, "packet:" + kind);
		assertEquals(expected, QuestGraphSystemMessageAdapter.packetShape(command));
		assertEquals(SM_SYSTEM_MESSAGE.class, QuestGraphSystemMessageAdapter.packet(command).getClass());
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
