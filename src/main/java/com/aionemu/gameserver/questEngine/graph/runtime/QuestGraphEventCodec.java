package com.aionemu.gameserver.questEngine.graph.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.AttackEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.HouseItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemEquippedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemObtainedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillInWorldEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.LevelUpEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcProximityEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortLostTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RankedPlayerKillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DredgionSettledEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerDeathEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerLogoutEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.QuestTimerEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneLeftEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneMissionEndedEvent;

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
	private static final byte ITEM_USE = 6;
	private static final byte ITEM_OBTAINED = 7;
	private static final byte ITEM_EQUIPPED = 8;
	private static final byte HOUSE_ITEM_USE = 9;
	private static final byte WORLD_ENTERED = 10;
	private static final byte ZONE_ENTERED = 11;
	private static final byte ZONE_LEFT = 12;
	private static final byte ZONE_MISSION_ENDED = 13;
	private static final byte LEVEL_UP = 14;
	private static final byte PLAYER_LOGOUT = 15;
	private static final byte QUEST_TIMER_ENDED = 16;
	private static final byte MOVIE_ENDED = 17;
	private static final byte NPC_PROXIMITY = 18;
	private static final byte ESCORT_REACHED_TARGET = 19;
	private static final byte ESCORT_LOST_TARGET = 20;
	private static final byte RANKED_PLAYER_KILL = 21;
	private static final byte DREDGION_SETTLED = 22;

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
					case ItemUseEvent itemUse -> {
						output.writeByte(ITEM_USE);
						writeCommon(output, itemUse);
						output.writeInt(itemUse.itemId());
						output.writeInt(itemUse.itemObjectId());
					}
					case ItemObtainedEvent itemObtained -> {
						output.writeByte(ITEM_OBTAINED);
						writeCommon(output, itemObtained);
						output.writeInt(itemObtained.itemId());
					}
					case ItemEquippedEvent itemEquipped -> {
						output.writeByte(ITEM_EQUIPPED);
						writeCommon(output, itemEquipped);
						output.writeInt(itemEquipped.itemId());
					}
					case HouseItemUseEvent houseItemUse -> {
						output.writeByte(HOUSE_ITEM_USE);
						writeCommon(output, houseItemUse);
						output.writeInt(houseItemUse.itemId());
					}
					case WorldEnteredEvent worldEntered -> {
						output.writeByte(WORLD_ENTERED);
						writeCommon(output, worldEntered);
						writeLocation(output, worldEntered.worldId(), worldEntered.instanceId(), worldEntered.x(), worldEntered.y(), worldEntered.z());
					}
					case ZoneEnteredEvent zoneEntered -> {
						output.writeByte(ZONE_ENTERED);
						writeCommon(output, zoneEntered);
						output.writeUTF(zoneEntered.zoneName());
						writeLocation(output, zoneEntered.worldId(), zoneEntered.instanceId(), zoneEntered.x(), zoneEntered.y(), zoneEntered.z());
					}
					case ZoneLeftEvent zoneLeft -> {
						output.writeByte(ZONE_LEFT);
						writeCommon(output, zoneLeft);
						output.writeUTF(zoneLeft.zoneName());
						output.writeInt(zoneLeft.worldId());
						output.writeInt(zoneLeft.instanceId());
					}
						case ZoneMissionEndedEvent zoneMissionEnded -> {
							output.writeByte(ZONE_MISSION_ENDED);
							writeCommon(output, zoneMissionEnded);
							output.writeInt(zoneMissionEnded.questId());
						}
						case LevelUpEvent levelUp -> {
							output.writeByte(LEVEL_UP);
							writeCommon(output, levelUp);
							output.writeInt(levelUp.level());
						}
						case PlayerLogoutEvent playerLogout -> {
							output.writeByte(PLAYER_LOGOUT);
							writeCommon(output, playerLogout);
							output.writeInt(playerLogout.worldId());
							output.writeInt(playerLogout.instanceId());
						}
						case QuestTimerEndedEvent timerEnded -> {
							output.writeByte(QUEST_TIMER_ENDED);
							writeCommon(output, timerEnded);
							output.writeInt(timerEnded.questId());
							output.writeUTF(timerEnded.timer());
							output.writeLong(timerEnded.deadlineAt());
						}
						case MovieEndedEvent movieEnded -> {
							output.writeByte(MOVIE_ENDED);
							writeCommon(output, movieEnded);
							output.writeInt(movieEnded.movieId());
							output.writeLong(movieEnded.playbackId());
							output.writeLong(movieEnded.startedAt());
						}
						case NpcProximityEvent proximity -> {
							output.writeByte(NPC_PROXIMITY);
							writeCommon(output, proximity);
							writeNpcSnapshot(output, proximity.npcId(), proximity.npcObjectId(), proximity.worldId(), proximity.instanceId());
							output.writeFloat(proximity.distance());
						}
						case EscortReachedTargetEvent reached -> {
							output.writeByte(ESCORT_REACHED_TARGET);
							writeCommon(output, reached);
							output.writeInt(reached.questId());
							writeNpcSnapshot(output, reached.npcId(), reached.npcObjectId(), reached.worldId(), reached.instanceId());
						}
						case EscortLostTargetEvent lost -> {
							output.writeByte(ESCORT_LOST_TARGET);
							writeCommon(output, lost);
							output.writeInt(lost.questId());
							writeNpcSnapshot(output, lost.npcId(), lost.npcObjectId(), lost.worldId(), lost.instanceId());
						}
						case RankedPlayerKillEvent rankedKill -> {
							output.writeByte(RANKED_PLAYER_KILL);
							writeCommon(output, rankedKill);
							output.writeInt(rankedKill.killerPlayerId());
							output.writeInt(rankedKill.victimPlayerId());
							output.writeInt(rankedKill.victimRankId());
							output.writeInt(rankedKill.worldId());
							output.writeInt(rankedKill.instanceId());
							output.writeFloat(rankedKill.creditDistance());
							output.writeBoolean(rankedKill.recipientAlive());
						}
						case DredgionSettledEvent dredgionSettled -> {
							output.writeByte(DREDGION_SETTLED);
							writeCommon(output, dredgionSettled);
							output.writeInt(dredgionSettled.worldId());
							output.writeInt(dredgionSettled.instanceId());
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
					case ITEM_USE -> new ItemUseEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt());
					case ITEM_OBTAINED -> new ItemObtainedEvent(eventId, playerId, occurredAt, input.readInt());
					case ITEM_EQUIPPED -> new ItemEquippedEvent(eventId, playerId, occurredAt, input.readInt());
					case HOUSE_ITEM_USE -> new HouseItemUseEvent(eventId, playerId, occurredAt, input.readInt());
					case WORLD_ENTERED -> new WorldEnteredEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(), input.readFloat(),
						input.readFloat(), input.readFloat());
					case ZONE_ENTERED -> new ZoneEnteredEvent(eventId, playerId, occurredAt, input.readUTF(), input.readInt(), input.readInt(),
						input.readFloat(), input.readFloat(), input.readFloat());
						case ZONE_LEFT -> new ZoneLeftEvent(eventId, playerId, occurredAt, input.readUTF(), input.readInt(), input.readInt());
						case ZONE_MISSION_ENDED -> new ZoneMissionEndedEvent(eventId, playerId, occurredAt, input.readInt());
						case LEVEL_UP -> new LevelUpEvent(eventId, playerId, occurredAt, input.readInt());
						case PLAYER_LOGOUT -> new PlayerLogoutEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt());
						case QUEST_TIMER_ENDED -> new QuestTimerEndedEvent(eventId, playerId, occurredAt, input.readInt(), input.readUTF(),
							input.readLong());
						case MOVIE_ENDED -> new MovieEndedEvent(eventId, playerId, occurredAt, input.readInt(), input.readLong(), input.readLong());
						case NPC_PROXIMITY -> new NpcProximityEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(), input.readInt(),
							input.readInt(), input.readFloat());
						case ESCORT_REACHED_TARGET -> new EscortReachedTargetEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(),
							input.readInt(), input.readInt(), input.readInt());
						case ESCORT_LOST_TARGET -> new EscortLostTargetEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(),
							input.readInt(), input.readInt(), input.readInt());
						case RANKED_PLAYER_KILL -> new RankedPlayerKillEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt(),
							input.readInt(), input.readInt(), input.readInt(), input.readFloat(), input.readBoolean());
						case DREDGION_SETTLED -> new DredgionSettledEvent(eventId, playerId, occurredAt, input.readInt(), input.readInt());
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

	/** 写入 NPC 模板、对象与实例快照。 / Writes an NPC template, object, and instance snapshot. */
	private static void writeNpcSnapshot(DataOutputStream output, int npcId, int npcObjectId, int worldId, int instanceId) throws IOException {
		output.writeInt(npcId);
		output.writeInt(npcObjectId);
		output.writeInt(worldId);
		output.writeInt(instanceId);
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

	/** 写入服务端世界、实例和位置快照。 / Writes a server world, instance, and position snapshot. */
	private static void writeLocation(DataOutputStream output, int worldId, int instanceId, float x, float y, float z) throws IOException {
		output.writeInt(worldId);
		output.writeInt(instanceId);
		output.writeFloat(x);
		output.writeFloat(y);
		output.writeFloat(z);
	}
}
