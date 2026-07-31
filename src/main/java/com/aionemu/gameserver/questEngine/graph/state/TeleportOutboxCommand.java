package com.aionemu.gameserver.questEngine.graph.state;

import java.nio.charset.StandardCharsets;

/** Immutable, fully resolved teleport command accepted by the durable quest graph action outbox. */
public record TeleportOutboxCommand(int playerId, int questId, long baseRevision, String transitionId, int actionIndex,
		int worldId, int instanceId, InstanceRecoveryMode instanceRecoveryMode, float x, float y, float z, byte heading, String operationKey) {

	public enum InstanceRecoveryMode {
		EXACT,
		PLAYER_CURRENT,
		PLAYER_REGISTERED_OR_CREATE,
		DEFAULT_INSTANCE
	}

	public static final int MAX_TRANSITION_ID_LENGTH = 255;
	public static final int MAX_OPERATION_KEY_LENGTH = 1024;

	public TeleportOutboxCommand {
		if (playerId <= 0 || questId <= 0 || baseRevision < -1 || transitionId == null || transitionId.isBlank()
				|| transitionId.length() > MAX_TRANSITION_ID_LENGTH || !isValidUtf8Text(transitionId)
				|| actionIndex < 0 || worldId <= 0 || instanceId <= 0 || instanceRecoveryMode == null
				|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || operationKey == null
				|| operationKey.isBlank() || operationKey.length() > MAX_OPERATION_KEY_LENGTH || !isValidUtf8Text(operationKey)) {
			throw new IllegalArgumentException("Teleport outbox command is invalid");
		}
	}

	private static boolean isValidUtf8Text(String value) {
		return StandardCharsets.UTF_8.newEncoder().canEncode(value);
	}
}
