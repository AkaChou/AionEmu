package com.aionemu.gameserver.questEngine.graph.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.AttackEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillInWorldEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerDeathEvent;

/**
 * 使用有界、版本化二进制格式持久化类型化任务图事件。
 * Persists typed quest graph events with a bounded, versioned binary format.
 */
public final class QuestGraphEventCodec {

	private static final int MAGIC = 0x51474531;
	private static final int MAX_PAYLOAD = 128 * 1024;
	private static final byte DIALOG = 1;
	private static final byte KILL = 2;
	private static final byte ATTACK = 3;
	private static final byte PLAYER_DEATH = 4;
	private static final byte KILL_IN_WORLD = 5;

	/**
	 * 禁止实例化纯静态 codec。
	 * Prevents instantiation of this static codec.
	 */
	private QuestGraphEventCodec() {
	}

	/**
	 * 确定性编码类型化事件。
	 * Deterministically encodes a typed event.
	 */
	public static byte[] encode(QuestGraphEvent event) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(MAGIC);
				switch (event) {
					case DialogEvent dialog -> {
						output.writeByte(DIALOG);
						writeCommon(output, dialog);
						output.writeInt(dialog.npcId());
						output.writeUTF(dialog.dialog());
					}
					case KillEvent kill -> {
						output.writeByte(KILL);
						writeCommon(output, kill);
						output.writeInt(kill.npcId());
					}
					case AttackEvent attack -> {
						output.writeByte(ATTACK);
						writeCommon(output, attack);
						output.writeInt(attack.npcId());
						output.writeLong(attack.currentHp());
						output.writeLong(attack.maximumHp());
					}
					case PlayerDeathEvent death -> {
						output.writeByte(PLAYER_DEATH);
						writeCommon(output, death);
					}
					case KillInWorldEvent killInWorld -> {
						output.writeByte(KILL_IN_WORLD);
						writeCommon(output, killInWorld);
						output.writeInt(killInWorld.worldId());
						output.writeInt(killInWorld.victimPlayerId());
						output.writeInt(killInWorld.victimLevel());
					}
				}
			}
			byte[] payload = bytes.toByteArray();
			if (payload.length > MAX_PAYLOAD) {
				throw new IllegalArgumentException("Quest graph event payload exceeds " + MAX_PAYLOAD + " bytes");
			}
			return payload;
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to encode quest graph event", e);
		}
	}

	/**
	 * 解码事件并拒绝未知版本、未知类型、截断或尾随数据。
	 * Decodes an event and rejects unknown versions, unknown types, truncation, or trailing data.
	 */
	public static QuestGraphEvent decode(byte[] payload) {
		if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD) {
			throw new IllegalArgumentException("Quest graph event payload is missing or oversized");
		}
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
			if (input.readInt() != MAGIC) {
				throw new IllegalArgumentException("Unsupported quest graph event payload version");
			}
			byte type = input.readByte();
			String eventId = input.readUTF();
			int playerId = input.readInt();
			long occurredAt = input.readLong();
			QuestGraphEvent event = switch (type) {
				case DIALOG -> new DialogEvent(eventId, playerId, occurredAt, input.readInt(), input.readUTF());
					case KILL -> new KillEvent(eventId, playerId, occurredAt, input.readInt());
					case ATTACK -> new AttackEvent(eventId, playerId, occurredAt, input.readInt(), input.readLong(), input.readLong());
					case PLAYER_DEATH -> new PlayerDeathEvent(eventId, playerId, occurredAt);
					case KILL_IN_WORLD -> new KillInWorldEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(), input.readInt());
				default -> throw new IllegalArgumentException("Unknown quest graph event tag " + type);
			};
			if (input.read() != -1) {
				throw new IllegalArgumentException("Quest graph event payload has trailing data");
			}
			return event;
		} catch (EOFException e) {
			throw new IllegalArgumentException("Quest graph event payload is truncated", e);
		} catch (IOException | RuntimeException e) {
			if (e instanceof IllegalArgumentException invalid) {
				throw invalid;
			}
			throw new IllegalArgumentException("Failed to decode quest graph event", e);
		}
	}

	/**
	 * 写入全部事件共享的不可变字段。
	 * Writes immutable fields shared by all events.
	 */
	private static void writeCommon(DataOutputStream output, QuestGraphEvent event) throws IOException {
		output.writeUTF(event.eventId());
		output.writeInt(event.playerId());
		output.writeLong(event.occurredAt());
	}
}
