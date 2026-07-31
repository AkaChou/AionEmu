package com.aionemu.gameserver.questEngine.graph.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord.Status;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode;

class QuestGraphActionOutboxCodecTest {

	@Test
	void deterministicallyRoundTripsFullyResolvedTeleportCommand() {
		TeleportOutboxCommand command = command("teleport:7:2634:-1:accept:2", -1);

		byte[] first = QuestGraphActionOutboxCodec.encode(command);
		byte[] second = QuestGraphActionOutboxCodec.encode(command);

		assertArrayEquals(first, second);
		assertEquals(command, QuestGraphActionOutboxCodec.decode(first));
		for (InstanceRecoveryMode mode : InstanceRecoveryMode.values()) {
			TeleportOutboxCommand variant = command("mode:" + mode, -1, mode);
			assertEquals(variant, QuestGraphActionOutboxCodec.decode(QuestGraphActionOutboxCodec.encode(variant)));
		}
	}

	@Test
	void rejectsDigestDamageUnknownVersionTruncationAndTrailingData() throws Exception {
		byte[] payload = QuestGraphActionOutboxCodec.encode(command("teleport:7:2634:4:accept:2", 4));
		byte[] damaged = payload.clone();
		damaged[12] ^= 1;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphActionOutboxCodec.decode(damaged));

		byte[] unknownVersion = payload.clone();
		unknownVersion[3] = 0x33;
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(unknownVersion, unknownVersion.length - 32));
		System.arraycopy(digest, 0, unknownVersion, unknownVersion.length - 32, digest.length);
		assertThrows(IllegalArgumentException.class, () -> QuestGraphActionOutboxCodec.decode(unknownVersion));

		byte[] unknownRecoveryMode = payload.clone();
		unknownRecoveryMode[42] = 99;
		digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(unknownRecoveryMode, unknownRecoveryMode.length - 32));
		System.arraycopy(digest, 0, unknownRecoveryMode, unknownRecoveryMode.length - 32, digest.length);
		assertThrows(IllegalArgumentException.class, () -> QuestGraphActionOutboxCodec.decode(unknownRecoveryMode));

		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphActionOutboxCodec.decode(Arrays.copyOf(payload, payload.length - 1)));
		byte[] trailing = Arrays.copyOf(payload, payload.length + 1);
		assertThrows(IllegalArgumentException.class, () -> QuestGraphActionOutboxCodec.decode(trailing));
	}

	@Test
	void validatesCommandAndIndependentGraphAcknowledgementState() {
		TeleportOutboxCommand command = command("teleport:7:2634:4:accept:2", 4);
		QuestGraphActionOutboxRecord graphAckedBeforeDelivery = new QuestGraphActionOutboxRecord(command, 1, Status.ACCEPTED,
			0, null, 100, null, true);
		QuestGraphActionOutboxRecord claimed = new QuestGraphActionOutboxRecord(command, 1, Status.CLAIMED, 3,
			200L, 100, null, true);
		QuestGraphActionOutboxRecord terminal = new QuestGraphActionOutboxRecord(command, 1, Status.GRAPH_ACKED, 3,
			null, 100, 150L, true);

		assertTrue(graphAckedBeforeDelivery.graphAcked());
		assertTrue(claimed.pendingDelivery());
		assertTrue(terminal.deletable());
		assertFalse(terminal.pendingDelivery());
		assertThrows(IllegalArgumentException.class,
			() -> new QuestGraphActionOutboxRecord(command, 1, Status.COMPLETED, 3, null, 100, 150L, true));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestGraphActionOutboxRecord(command, 1, Status.CLAIMED, 3, null, 100, null, false));
		assertThrows(IllegalArgumentException.class,
			() -> new TeleportOutboxCommand(7, 2634, 4, "accept", 2, 210010000, 0,
				TeleportOutboxCommand.InstanceRecoveryMode.EXACT, Float.NaN, 2, 3,
				(byte) 4, "key"));
		assertThrows(IllegalArgumentException.class,
			() -> new TeleportOutboxCommand(7, 2634, 4, "bad\uD800", 2, 210010000, 1,
				TeleportOutboxCommand.InstanceRecoveryMode.EXACT, 1, 2, 3,
				(byte) 4, "key"));
		assertThrows(IllegalArgumentException.class,
			() -> new TeleportOutboxCommand(7, 2634, 4, "accept", 2, 210010000, 1,
				TeleportOutboxCommand.InstanceRecoveryMode.EXACT, 1, 2, 3,
				(byte) 4, "bad\uDC00"));
	}

	private static TeleportOutboxCommand command(String operationKey, long baseRevision) {
		return command(operationKey, baseRevision, InstanceRecoveryMode.EXACT);
	}

	private static TeleportOutboxCommand command(String operationKey, long baseRevision, InstanceRecoveryMode mode) {
		return new TeleportOutboxCommand(7, 2634, baseRevision, "accept", 2, 210010000, 30001,
			mode, 123.5f, 456.25f, 789.75f, (byte) 64, operationKey);
	}
}
