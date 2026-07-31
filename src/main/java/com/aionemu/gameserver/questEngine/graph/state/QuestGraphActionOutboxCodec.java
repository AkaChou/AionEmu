package com.aionemu.gameserver.questEngine.graph.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode;

/** Strict, versioned codec for durable teleport outbox commands. */
public final class QuestGraphActionOutboxCodec {

	private static final int MAGIC_QGT2 = 0x51475432;
	private static final int SHA_256_BYTES = 32;
	private static final int MAX_PAYLOAD_BYTES = 16 * 1024;
	private static final int MAX_TRANSITION_ID_BYTES = TeleportOutboxCommand.MAX_TRANSITION_ID_LENGTH * 4;
	private static final int MAX_OPERATION_KEY_BYTES = TeleportOutboxCommand.MAX_OPERATION_KEY_LENGTH * 4;
	private static final byte INSTANCE_EXACT = 1;
	private static final byte INSTANCE_PLAYER_CURRENT = 2;
	private static final byte INSTANCE_PLAYER_REGISTERED_OR_CREATE = 3;
	private static final byte INSTANCE_DEFAULT = 4;

	private QuestGraphActionOutboxCodec() {
	}

	public static byte[] encode(TeleportOutboxCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Teleport outbox command is missing");
		}
		try {
			ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bodyBytes)) {
				output.writeInt(MAGIC_QGT2);
				output.writeInt(command.playerId());
				output.writeInt(command.questId());
				output.writeLong(command.baseRevision());
				writeText(output, command.transitionId(), MAX_TRANSITION_ID_BYTES);
				output.writeInt(command.actionIndex());
				output.writeInt(command.worldId());
				output.writeInt(command.instanceId());
				output.writeByte(instanceRecoveryTag(command.instanceRecoveryMode()));
				output.writeFloat(command.x());
				output.writeFloat(command.y());
				output.writeFloat(command.z());
				output.writeByte(command.heading());
				writeText(output, command.operationKey(), MAX_OPERATION_KEY_BYTES);
			}
			byte[] body = bodyBytes.toByteArray();
			ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream(body.length + SHA_256_BYTES);
			payloadBytes.write(body);
			payloadBytes.write(sha256(body));
			byte[] payload = payloadBytes.toByteArray();
			if (payload.length > MAX_PAYLOAD_BYTES) {
				throw new IllegalArgumentException("Teleport outbox command payload is oversized");
			}
			return payload;
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to encode teleport outbox command", e);
		}
	}

	public static TeleportOutboxCommand decode(byte[] payload) {
		if (payload == null || payload.length <= SHA_256_BYTES || payload.length > MAX_PAYLOAD_BYTES) {
			throw new IllegalArgumentException("Teleport outbox command payload is missing or oversized");
		}
		int bodyLength = payload.length - SHA_256_BYTES;
		byte[] body = Arrays.copyOf(payload, bodyLength);
		byte[] expectedDigest = Arrays.copyOfRange(payload, bodyLength, payload.length);
		if (!MessageDigest.isEqual(expectedDigest, sha256(body))) {
			throw new IllegalArgumentException("Teleport outbox command payload digest does not match");
		}
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
			if (input.readInt() != MAGIC_QGT2) {
				throw new IllegalArgumentException("Unsupported teleport outbox command payload version");
			}
			int playerId = input.readInt();
			int questId = input.readInt();
			long baseRevision = input.readLong();
			String transitionId = readText(input, MAX_TRANSITION_ID_BYTES);
			int actionIndex = input.readInt();
			int worldId = input.readInt();
			int instanceId = input.readInt();
			TeleportOutboxCommand command = new TeleportOutboxCommand(playerId, questId, baseRevision,
				transitionId, actionIndex, worldId, instanceId, instanceRecoveryMode(input.readByte()), input.readFloat(),
				input.readFloat(), input.readFloat(), input.readByte(), readText(input, MAX_OPERATION_KEY_BYTES));
			if (input.read() != -1 || !Arrays.equals(payload, encode(command))) {
				throw new IllegalArgumentException("Teleport outbox command payload is non-canonical or has trailing data");
			}
			return command;
		} catch (EOFException e) {
			throw new IllegalArgumentException("Teleport outbox command payload is truncated", e);
		} catch (IOException | RuntimeException e) {
			if (e instanceof IllegalArgumentException invalid) {
				throw invalid;
			}
			throw new IllegalArgumentException("Failed to decode teleport outbox command", e);
		}
	}

	private static void writeText(DataOutputStream output, String value, int maximumBytes) throws IOException {
		byte[] bytes = encodeUtf8(value);
		if (bytes.length > maximumBytes) {
			throw new IllegalArgumentException("Teleport outbox command text is oversized");
		}
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	private static String readText(DataInputStream input, int maximumBytes) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > maximumBytes) {
			throw new IllegalArgumentException("Teleport outbox command text length is invalid");
		}
		byte[] bytes = input.readNBytes(length);
		if (bytes.length != length) {
			throw new EOFException("Teleport outbox command text is truncated");
		}
		try {
			return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("Teleport outbox command text is not valid UTF-8", e);
		}
	}

	private static byte[] encodeUtf8(String value) {
		try {
			ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("Teleport outbox command text is not valid UTF-8", e);
		}
	}

	private static byte[] sha256(byte[] value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	private static byte instanceRecoveryTag(InstanceRecoveryMode mode) {
		return switch (mode) {
			case EXACT -> INSTANCE_EXACT;
			case PLAYER_CURRENT -> INSTANCE_PLAYER_CURRENT;
			case PLAYER_REGISTERED_OR_CREATE -> INSTANCE_PLAYER_REGISTERED_OR_CREATE;
			case DEFAULT_INSTANCE -> INSTANCE_DEFAULT;
		};
	}

	private static InstanceRecoveryMode instanceRecoveryMode(byte tag) {
		return switch (tag) {
			case INSTANCE_EXACT -> InstanceRecoveryMode.EXACT;
			case INSTANCE_PLAYER_CURRENT -> InstanceRecoveryMode.PLAYER_CURRENT;
			case INSTANCE_PLAYER_REGISTERED_OR_CREATE -> InstanceRecoveryMode.PLAYER_REGISTERED_OR_CREATE;
			case INSTANCE_DEFAULT -> InstanceRecoveryMode.DEFAULT_INSTANCE;
			default -> throw new IllegalArgumentException("Unknown teleport outbox instance recovery mode tag");
		};
	}
}
