package com.aionemu.gameserver.questEngine.graph.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortNpcDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortZoneDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineDisposition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;

/**
 * 使用有界、版本化二进制格式编码玩家任务图状态的动态负载。
 * Encodes the dynamic payload of player quest graph state with a bounded, versioned binary format.
 */
public final class PlayerQuestGraphStateCodec {

	private static final int MAGIC_QGS4 = 0x51475334;
	private static final int MAGIC_QGS5 = 0x51475335;
	private static final int MAGIC_QGS6 = 0x51475336;
	private static final int MAGIC_QGS7 = 0x51475337;
	private static final int MAGIC_QGR1 = 0x51475231;
	private static final int MAX_VARIABLES = 1024;
	private static final int MAX_DEADLINES = 256;
	private static final int MAX_CLEANUP_LEASES = 1024;
	private static final int MAX_ITEM_MUTATION_PLANS = 1024;
	private static final int MAX_EVENT_PAYLOAD = 1024 * 1024;
	private static final int MAX_TOTAL_PAYLOAD = 2 * 1024 * 1024;
	private static final byte INT_VALUE = 1;
	private static final byte BOOLEAN_VALUE = 2;
	private static final byte STATUS_NONE = 0;
	private static final byte STATUS_START = 1;
	private static final byte STATUS_REWARD = 2;
	private static final byte STATUS_COMPLETE = 3;
	private static final byte STATUS_LOCKED = 4;
	private static final byte REPEAT_NOT_APPLICABLE = 0;
	private static final byte REPEAT_DEADLINE = 1;
	private static final byte REPEAT_PRIVILEGED_BYPASS = 2;
	private static final byte ITEM_GIVE_TOP_UP_TO = 1;
	private static final byte ITEM_REMOVE_EXACT = 2;
	private static final byte ITEM_REMOVE_OPTIONAL_EXACT = 3;
	private static final byte ITEM_GIVE_ADD_EXACT = 4;
	private static final byte ITEM_REMOVE_ALL = 5;
	private static final byte RESOURCE_UNRESOLVED = 0;
	private static final byte RESOURCE_INSTANCE_SPAWN = 1;
	private static final byte RESOURCE_ESCORT = 2;
	private static final byte ESCORT_SOURCE_EVENT_NPC = 1;
	private static final byte ESCORT_SOURCE_PLAYER_POSITION_SPAWN = 2;
	private static final byte ESCORT_SOURCE_REPLACE_EVENT_NPC = 3;
	private static final byte ESCORT_DESTINATION_ZONE = 1;
	private static final byte ESCORT_DESTINATION_NPC = 2;
	private static final byte ESCORT_DESTINATION_COORDINATES = 3;

	/**
	 * 禁止实例化纯静态 codec。
	 * Prevents instantiation of this static codec.
	 */
	private PlayerQuestGraphStateCodec() {
	}

	/**
	 * 将动态状态按确定顺序编码为数据库负载。
	 * Encodes dynamic state in deterministic order for database storage.
	 */
	public static byte[] encode(PlayerQuestGraphState state) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(MAGIC_QGS7);
				output.writeByte(switch (state.getQuestStatus()) {
					case NONE -> STATUS_NONE;
					case START -> STATUS_START;
					case REWARD -> STATUS_REWARD;
					case COMPLETE -> STATUS_COMPLETE;
					case LOCKED -> STATUS_LOCKED;
				});
				writeHistory(output, state.getHistory());
				writeVariables(output, state.getVariables());
				writeDeadlines(output, state.getDeadlines());
				writeJournal(output, state.getJournal());
				writeCleanupLeases(output, state.getCleanupLeases());
				writeOptionalText(output, state.getQuarantineReason());
			}
			byte[] payload = bytes.toByteArray();
			if (payload.length > MAX_TOTAL_PAYLOAD) {
				throw new IllegalArgumentException("Quest graph state payload exceeds " + MAX_TOTAL_PAYLOAD + " bytes");
			}
			return payload;
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to encode player quest graph state", e);
		}
	}

	/**
	 * 解码数据库负载并拒绝未知、损坏或非规范数据。
	 * Decodes a database payload and rejects unknown, corrupt, or non-canonical data.
	 */
	public static PlayerQuestGraphState decode(int questId, int definitionVersion, long revision, String nodeId, Long instanceRunId,
			Lifecycle lifecycle, byte[] payload) {
		if (payload == null || payload.length == 0 || payload.length > MAX_TOTAL_PAYLOAD) {
			throw new IllegalArgumentException("Quest graph state payload is missing or oversized");
		}
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
			int magic = input.readInt();
			if (magic != MAGIC_QGS4 && magic != MAGIC_QGS5 && magic != MAGIC_QGS6 && magic != MAGIC_QGS7) {
				throw new IllegalArgumentException("Unsupported quest graph state payload version");
			}
			QuestStatus questStatus = switch (input.readByte()) {
				case STATUS_NONE -> QuestStatus.NONE;
				case STATUS_START -> QuestStatus.START;
				case STATUS_REWARD -> QuestStatus.REWARD;
				case STATUS_COMPLETE -> QuestStatus.COMPLETE;
				case STATUS_LOCKED -> QuestStatus.LOCKED;
				default -> throw new IllegalArgumentException("Unknown quest graph status tag");
			};
			QuestHistory history = readHistory(input);
			Map<String, VariableValue> variables = readVariables(input);
			Map<String, Long> deadlines = readDeadlines(input);
			PreparedTransition journal = readJournal(input, magic != MAGIC_QGS4, magic == MAGIC_QGS7);
			Map<String, CleanupLease> cleanupLeases = readCleanupLeases(input, magic == MAGIC_QGS6 || magic == MAGIC_QGS7);
			String quarantineReason = readOptionalText(input);
			if (input.read() != -1) {
				throw new IllegalArgumentException("Quest graph state payload has trailing data");
			}
			return new PlayerQuestGraphState(questId, definitionVersion, revision, nodeId, questStatus, history, instanceRunId, lifecycle, variables,
				deadlines, journal, cleanupLeases, quarantineReason);
		} catch (EOFException e) {
			throw new IllegalArgumentException("Quest graph state payload is truncated", e);
		} catch (IOException | RuntimeException e) {
			if (e instanceof IllegalArgumentException invalid) {
				throw invalid;
			}
			throw new IllegalArgumentException("Failed to decode player quest graph state", e);
		}
	}

	/** Encodes one typed cleanup lease for the durable resource-operation registry. */
	public static byte[] encodeCleanupLease(CleanupLease lease) {
		if (lease == null || lease.identity() == null || !lease.identity().materialized()) {
			throw new IllegalArgumentException("Resource operation lease must have a materialized typed identity");
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(MAGIC_QGR1);
				writeCleanupLeases(output, Map.of(lease.resourceKey(), lease));
			}
			byte[] payload = bytes.toByteArray();
			if (payload.length > MAX_TOTAL_PAYLOAD) {
				throw new IllegalArgumentException("Resource operation payload exceeds " + MAX_TOTAL_PAYLOAD + " bytes");
			}
			return payload;
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to encode resource operation lease", e);
		}
	}

	/** Decodes one typed cleanup lease from the durable resource-operation registry. */
	public static CleanupLease decodeCleanupLease(byte[] payload) {
		if (payload == null || payload.length == 0 || payload.length > MAX_TOTAL_PAYLOAD) {
			throw new IllegalArgumentException("Resource operation payload is missing or oversized");
		}
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
			if (input.readInt() != MAGIC_QGR1) {
				throw new IllegalArgumentException("Unsupported resource operation payload version");
			}
			Map<String, CleanupLease> leases = readCleanupLeases(input, true);
			if (leases.size() != 1 || input.read() != -1) {
				throw new IllegalArgumentException("Resource operation payload is not canonical");
			}
			CleanupLease lease = leases.values().iterator().next();
			if (lease.identity() == null || !lease.identity().materialized()) {
				throw new IllegalArgumentException("Resource operation identity is unresolved");
			}
			return lease;
		} catch (EOFException e) {
			throw new IllegalArgumentException("Resource operation payload is truncated", e);
		} catch (IOException | RuntimeException e) {
			if (e instanceof IllegalArgumentException invalid) {
				throw invalid;
			}
			throw new IllegalArgumentException("Failed to decode resource operation lease", e);
		}
	}

	/**
	 * 写入跨重复周期保留的 canonical 任务历史。
	 * Writes canonical quest history retained across repeat cycles.
	 */
	private static void writeHistory(DataOutputStream output, QuestHistory history) throws IOException {
		output.writeInt(history.completionCount());
		output.writeInt(history.lastRewardIndex());
		writeOptionalLong(output, history.completedAt());
		writeOptionalLong(output, history.nextRepeatAt());
		writeRepeatDisposition(output, history.repeatDeadlineDisposition());
	}

	/**
	 * 读取并校验 canonical 任务历史。
	 * Reads and validates canonical quest history.
	 */
	private static QuestHistory readHistory(DataInputStream input) throws IOException {
		return new QuestHistory(input.readInt(), input.readInt(), readOptionalLong(input), readOptionalLong(input), readRepeatDisposition(input));
	}

	/**
	 * 写入强类型变量集合。
	 * Writes the typed variable collection.
	 */
	private static void writeVariables(DataOutputStream output, Map<String, VariableValue> variables) throws IOException {
		checkCount("variables", variables.size(), MAX_VARIABLES);
		output.writeInt(variables.size());
		for (Map.Entry<String, VariableValue> entry : variables.entrySet()) {
			output.writeUTF(entry.getKey());
			switch (entry.getValue()) {
				case IntValue value -> {
					output.writeByte(INT_VALUE);
					output.writeInt(value.value());
				}
				case BooleanValue value -> {
					output.writeByte(BOOLEAN_VALUE);
					output.writeBoolean(value.value());
				}
			}
		}
	}

	/**
	 * 读取强类型变量集合并拒绝重复名称或未知 tag。
	 * Reads typed variables and rejects duplicate names or unknown tags.
	 */
	private static Map<String, VariableValue> readVariables(DataInputStream input) throws IOException {
		int count = readCount(input, "variables", MAX_VARIABLES);
		Map<String, VariableValue> variables = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			String name = input.readUTF();
			VariableValue value = switch (input.readByte()) {
				case INT_VALUE -> new IntValue(input.readInt());
				case BOOLEAN_VALUE -> new BooleanValue(input.readBoolean());
				default -> throw new IllegalArgumentException("Unknown quest graph variable tag");
			};
			putUnique(variables, name, value, "variable");
		}
		return variables;
	}

	/**
	 * 写入全部绝对 deadline。
	 * Writes all absolute deadlines.
	 */
	private static void writeDeadlines(DataOutputStream output, Map<String, Long> deadlines) throws IOException {
		checkCount("deadlines", deadlines.size(), MAX_DEADLINES);
		output.writeInt(deadlines.size());
		for (Map.Entry<String, Long> entry : deadlines.entrySet()) {
			output.writeUTF(entry.getKey());
			output.writeLong(entry.getValue());
		}
	}

	/**
	 * 读取绝对 deadline 并拒绝重复名称。
	 * Reads absolute deadlines and rejects duplicate names.
	 */
	private static Map<String, Long> readDeadlines(DataInputStream input) throws IOException {
		int count = readCount(input, "deadlines", MAX_DEADLINES);
		Map<String, Long> deadlines = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			putUnique(deadlines, input.readUTF(), input.readLong(), "deadline");
		}
		return deadlines;
	}

	/**
	 * 写入可选 PREPARED journal。
	 * Writes the optional PREPARED journal.
	 */
	private static void writeJournal(DataOutputStream output, PreparedTransition journal) throws IOException {
		output.writeBoolean(journal != null);
		if (journal == null) {
			return;
		}
		output.writeLong(journal.getBaseRevision());
		output.writeUTF(journal.getEventId());
		output.writeUTF(journal.getTransitionId());
		output.writeInt(journal.getNextActionIndex());
		output.writeBoolean(journal.isTargetCommitted());
		writeRepeatResolution(output, journal.getRepeatDeadlineResolution());
		writeItemMutationPlans(output, journal.getItemMutationPlans());
		byte[] eventPayload = journal.getEventPayload();
		if (eventPayload.length > MAX_EVENT_PAYLOAD) {
			throw new IllegalArgumentException("Prepared event payload exceeds " + MAX_EVENT_PAYLOAD + " bytes");
		}
		output.writeInt(eventPayload.length);
		output.write(eventPayload);
	}

	/**
	 * 读取可选 PREPARED journal。
	 * Reads the optional PREPARED journal.
	 */
	private static PreparedTransition readJournal(DataInputStream input, boolean hasItemMutationPlans, boolean hasTargetCommitMarker) throws IOException {
		if (!input.readBoolean()) {
			return null;
		}
		long baseRevision = input.readLong();
		String eventId = input.readUTF();
		String transitionId = input.readUTF();
		int nextActionIndex = input.readInt();
		boolean targetCommitted = hasTargetCommitMarker && input.readBoolean();
		RepeatDeadlineResolution repeatDeadlineResolution = readRepeatResolution(input);
		Map<Integer, ItemMutationPlan> itemMutationPlans = hasItemMutationPlans ? readItemMutationPlans(input) : Map.of();
		int payloadSize = readCount(input, "prepared event bytes", MAX_EVENT_PAYLOAD);
		byte[] eventPayload = input.readNBytes(payloadSize);
		if (eventPayload.length != payloadSize) {
			throw new EOFException("Prepared event payload is truncated");
		}
		return new PreparedTransition(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution,
			itemMutationPlans, eventPayload);
	}

	/** 写入按动作序号排序的冻结物品计划。 / Writes frozen item plans ordered by action index. */
	private static void writeItemMutationPlans(DataOutputStream output, Map<Integer, ItemMutationPlan> plans) throws IOException {
		checkCount("item mutation plans", plans.size(), MAX_ITEM_MUTATION_PLANS);
		output.writeInt(plans.size());
		for (ItemMutationPlan plan : plans.values()) {
			output.writeInt(plan.actionIndex());
			output.writeByte(switch (plan.kind()) {
				case GIVE_TOP_UP_TO -> ITEM_GIVE_TOP_UP_TO;
				case GIVE_ADD_EXACT -> ITEM_GIVE_ADD_EXACT;
				case REMOVE_EXACT -> ITEM_REMOVE_EXACT;
				case REMOVE_OPTIONAL_EXACT -> ITEM_REMOVE_OPTIONAL_EXACT;
				case REMOVE_ALL -> ITEM_REMOVE_ALL;
			});
			output.writeInt(plan.itemId());
			output.writeLong(plan.requestedCount());
			output.writeLong(plan.beforeCount());
			output.writeLong(plan.afterCount());
		}
	}

	/** 读取冻结物品计划并拒绝未知语义或重复动作索引。 / Reads frozen item plans and rejects unknown semantics or duplicate action indices. */
	private static Map<Integer, ItemMutationPlan> readItemMutationPlans(DataInputStream input) throws IOException {
		int count = readCount(input, "item mutation plans", MAX_ITEM_MUTATION_PLANS);
		Map<Integer, ItemMutationPlan> plans = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			int actionIndex = input.readInt();
			ItemMutationKind kind = switch (input.readByte()) {
				case ITEM_GIVE_TOP_UP_TO -> ItemMutationKind.GIVE_TOP_UP_TO;
				case ITEM_GIVE_ADD_EXACT -> ItemMutationKind.GIVE_ADD_EXACT;
				case ITEM_REMOVE_EXACT -> ItemMutationKind.REMOVE_EXACT;
				case ITEM_REMOVE_OPTIONAL_EXACT -> ItemMutationKind.REMOVE_OPTIONAL_EXACT;
				case ITEM_REMOVE_ALL -> ItemMutationKind.REMOVE_ALL;
				default -> throw new IllegalArgumentException("Unknown item mutation kind tag");
			};
			ItemMutationPlan plan = new ItemMutationPlan(actionIndex, kind, input.readInt(), input.readLong(), input.readLong(), input.readLong());
			if (plans.putIfAbsent(actionIndex, plan) != null) {
				throw new IllegalArgumentException("Duplicate item mutation action index " + actionIndex);
			}
		}
		return plans;
	}

	/** 写入 repeat deadline disposition 的稳定 tag。 / Writes the stable tag for a repeat-deadline disposition. */
	private static void writeRepeatDisposition(DataOutputStream output, RepeatDeadlineDisposition disposition) throws IOException {
		output.writeByte(switch (disposition) {
			case NOT_APPLICABLE -> REPEAT_NOT_APPLICABLE;
			case DEADLINE -> REPEAT_DEADLINE;
			case PRIVILEGED_BYPASS -> REPEAT_PRIVILEGED_BYPASS;
		});
	}

	/** 读取 repeat deadline disposition 并拒绝未知 tag。 / Reads a repeat-deadline disposition and rejects unknown tags. */
	private static RepeatDeadlineDisposition readRepeatDisposition(DataInputStream input) throws IOException {
		return switch (input.readByte()) {
			case REPEAT_NOT_APPLICABLE -> RepeatDeadlineDisposition.NOT_APPLICABLE;
			case REPEAT_DEADLINE -> RepeatDeadlineDisposition.DEADLINE;
			case REPEAT_PRIVILEGED_BYPASS -> RepeatDeadlineDisposition.PRIVILEGED_BYPASS;
			default -> throw new IllegalArgumentException("Unknown repeat deadline disposition tag");
		};
	}

	/** 写入冻结的 repeat deadline 解析结果。 / Writes the frozen repeat-deadline resolution. */
	private static void writeRepeatResolution(DataOutputStream output, RepeatDeadlineResolution resolution) throws IOException {
		writeRepeatDisposition(output, resolution.disposition());
		if (resolution.disposition() == RepeatDeadlineDisposition.DEADLINE) {
			output.writeLong(resolution.deadlineAt());
		}
	}

	/** 读取并校验冻结的 repeat deadline 解析结果。 / Reads and validates the frozen repeat-deadline resolution. */
	private static RepeatDeadlineResolution readRepeatResolution(DataInputStream input) throws IOException {
		return switch (readRepeatDisposition(input)) {
			case NOT_APPLICABLE -> RepeatDeadlineResolution.NOT_APPLICABLE;
			case DEADLINE -> RepeatDeadlineResolution.deadline(input.readLong());
			case PRIVILEGED_BYPASS -> RepeatDeadlineResolution.PRIVILEGED_BYPASS;
		};
	}

	/**
	 * 写入 cleanup ledger。
	 * Writes the cleanup ledger.
	 */
	private static void writeCleanupLeases(DataOutputStream output, Map<String, CleanupLease> cleanupLeases) throws IOException {
		checkCount("cleanup leases", cleanupLeases.size(), MAX_CLEANUP_LEASES);
		output.writeInt(cleanupLeases.size());
		for (Map.Entry<String, CleanupLease> entry : cleanupLeases.entrySet()) {
			output.writeUTF(entry.getKey());
			CleanupLease lease = entry.getValue();
			output.writeUTF(lease.capability());
			output.writeUTF(lease.resourceKey());
			switch (lease.identity()) {
				case null -> output.writeByte(RESOURCE_UNRESOLVED);
				case InstanceSpawnResourceIdentity spawn -> {
					output.writeByte(RESOURCE_INSTANCE_SPAWN);
					writeInstanceSpawnIdentity(output, spawn);
				}
				case EscortResourceIdentity escort -> {
					output.writeByte(RESOURCE_ESCORT);
					writeEscortIdentity(output, escort);
				}
			}
		}
	}

	/**
	 * 读取 cleanup ledger 并拒绝重复 lease。
	 * Reads the cleanup ledger and rejects duplicate leases.
	 */
	private static Map<String, CleanupLease> readCleanupLeases(DataInputStream input, boolean hasTypedIdentity) throws IOException {
		int count = readCount(input, "cleanup leases", MAX_CLEANUP_LEASES);
		Map<String, CleanupLease> cleanupLeases = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			String leaseId = input.readUTF();
			String capability = input.readUTF();
			String resourceKey = input.readUTF();
			CleanupLease lease = !hasTypedIdentity ? new CleanupLease(capability, resourceKey)
				: switch (input.readByte()) {
					case RESOURCE_UNRESOLVED -> new CleanupLease(capability, resourceKey);
					case RESOURCE_INSTANCE_SPAWN -> new CleanupLease(capability, resourceKey, readInstanceSpawnIdentity(input));
					case RESOURCE_ESCORT -> new CleanupLease(capability, resourceKey, readEscortIdentity(input));
					default -> throw new IllegalArgumentException("Unknown cleanup resource identity tag");
				};
			putUnique(cleanupLeases, leaseId, lease, "cleanup lease");
		}
		return cleanupLeases;
	}

	private static void writeInstanceSpawnIdentity(DataOutputStream output, InstanceSpawnResourceIdentity identity) throws IOException {
		writeResourceOwner(output, identity.playerId(), identity.questId(), identity.objectId(), identity.npcId(),
			identity.worldId(), identity.instanceId(), identity.idempotencyKey());
		output.writeByte(switch (identity.placement()) {
			case STATIC_SPAWN -> 1;
			case DIALOG_TARGET -> 2;
			case PLAYER -> 3;
			case FIXED -> 4;
		});
		output.writeInt(identity.sourceNpcId());
		output.writeInt(identity.sourceObjectId());
		output.writeFloat(identity.x());
		output.writeFloat(identity.y());
		output.writeFloat(identity.z());
		output.writeByte(identity.heading());
	}

	private static InstanceSpawnResourceIdentity readInstanceSpawnIdentity(DataInputStream input) throws IOException {
		ResourceOwner owner = readResourceOwner(input);
		SpawnPlacementKind placement = switch (input.readByte()) {
			case 1 -> SpawnPlacementKind.STATIC_SPAWN;
			case 2 -> SpawnPlacementKind.DIALOG_TARGET;
			case 3 -> SpawnPlacementKind.PLAYER;
			case 4 -> SpawnPlacementKind.FIXED;
			default -> throw new IllegalArgumentException("Unknown spawn placement tag");
		};
		return new InstanceSpawnResourceIdentity(owner.playerId(), owner.questId(), owner.objectId(), owner.npcId(), placement,
			input.readInt(), input.readInt(), owner.worldId(), owner.instanceId(), input.readFloat(), input.readFloat(),
			input.readFloat(), input.readByte(), owner.idempotencyKey());
	}

	private static void writeEscortIdentity(DataOutputStream output, EscortResourceIdentity identity) throws IOException {
		writeResourceOwner(output, identity.playerId(), identity.questId(), identity.objectId(), identity.npcId(),
			identity.worldId(), identity.instanceId(), identity.idempotencyKey());
		output.writeFloat(identity.x());
		output.writeFloat(identity.y());
		output.writeFloat(identity.z());
		output.writeInt(identity.eventNpcId());
		output.writeInt(identity.eventNpcObjectId());
		output.writeBoolean(identity.spawnedFollower());
		writeOptionalText(output, identity.previousWalkerId());
		writeEscortAction(output, identity.action());
	}

	private static EscortResourceIdentity readEscortIdentity(DataInputStream input) throws IOException {
		ResourceOwner owner = readResourceOwner(input);
		return new EscortResourceIdentity(owner.playerId(), owner.questId(), owner.objectId(), owner.npcId(), owner.worldId(),
			owner.instanceId(), input.readFloat(), input.readFloat(), input.readFloat(), input.readInt(), input.readInt(), input.readBoolean(), readOptionalText(input),
			readEscortAction(input), owner.idempotencyKey());
	}

	private static void writeResourceOwner(DataOutputStream output, int playerId, int questId, int objectId, int npcId,
			int worldId, int instanceId, String idempotencyKey) throws IOException {
		output.writeInt(playerId);
		output.writeInt(questId);
		output.writeInt(objectId);
		output.writeInt(npcId);
		output.writeInt(worldId);
		output.writeInt(instanceId);
		output.writeUTF(idempotencyKey);
	}

	private static ResourceOwner readResourceOwner(DataInputStream input) throws IOException {
		return new ResourceOwner(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(),
			input.readInt(), input.readUTF());
	}

	private static void writeEscortAction(DataOutputStream output, StartEscortAction action) throws IOException {
		output.writeByte(switch (action.source()) {
			case EVENT_NPC -> ESCORT_SOURCE_EVENT_NPC;
			case PLAYER_POSITION_SPAWN -> ESCORT_SOURCE_PLAYER_POSITION_SPAWN;
			case REPLACE_EVENT_NPC_AT_PLAYER_POSITION -> ESCORT_SOURCE_REPLACE_EVENT_NPC;
		});
		output.writeInt(action.npcId());
		output.writeByte(action.heading());
		writeOptionalText(output, action.walkerId());
		output.writeBoolean(action.startWalking());
		output.writeBoolean(action.followMe());
		output.writeBoolean(action.startEmote2());
		output.writeBoolean(action.sendNpcInfo());
		switch (action.destination()) {
			case EscortZoneDestination zone -> {
				output.writeByte(ESCORT_DESTINATION_ZONE);
				output.writeUTF(zone.zoneName());
			}
			case EscortNpcDestination npc -> {
				output.writeByte(ESCORT_DESTINATION_NPC);
				output.writeInt(npc.npcId());
			}
			case EscortCoordinatesDestination coordinates -> {
				output.writeByte(ESCORT_DESTINATION_COORDINATES);
				output.writeFloat(coordinates.x());
				output.writeFloat(coordinates.y());
				output.writeFloat(coordinates.z());
			}
		}
	}

	private static StartEscortAction readEscortAction(DataInputStream input) throws IOException {
		EscortSource source = switch (input.readByte()) {
			case ESCORT_SOURCE_EVENT_NPC -> EscortSource.EVENT_NPC;
			case ESCORT_SOURCE_PLAYER_POSITION_SPAWN -> EscortSource.PLAYER_POSITION_SPAWN;
			case ESCORT_SOURCE_REPLACE_EVENT_NPC -> EscortSource.REPLACE_EVENT_NPC_AT_PLAYER_POSITION;
			default -> throw new IllegalArgumentException("Unknown escort source tag");
		};
		int npcId = input.readInt();
		byte heading = input.readByte();
		String walkerId = readOptionalText(input);
		boolean startWalking = input.readBoolean();
		boolean followMe = input.readBoolean();
		boolean startEmote2 = input.readBoolean();
		boolean sendNpcInfo = input.readBoolean();
		var destination = switch (input.readByte()) {
			case ESCORT_DESTINATION_ZONE -> new EscortZoneDestination(input.readUTF());
			case ESCORT_DESTINATION_NPC -> new EscortNpcDestination(input.readInt());
			case ESCORT_DESTINATION_COORDINATES -> new EscortCoordinatesDestination(input.readFloat(), input.readFloat(), input.readFloat());
			default -> throw new IllegalArgumentException("Unknown escort destination tag");
		};
		return new StartEscortAction(source, npcId, heading, walkerId, startWalking, followMe, startEmote2, sendNpcInfo, destination);
	}

	private record ResourceOwner(int playerId, int questId, int objectId, int npcId, int worldId, int instanceId,
			String idempotencyKey) {
	}

	/**
	 * 写入可选文本。
	 * Writes optional text.
	 */
	private static void writeOptionalText(DataOutputStream output, String value) throws IOException {
		output.writeBoolean(value != null);
		if (value != null) {
			output.writeUTF(value);
		}
	}

	/**
	 * 写入可选 long 值。
	 * Writes an optional long value.
	 */
	private static void writeOptionalLong(DataOutputStream output, Long value) throws IOException {
		output.writeBoolean(value != null);
		if (value != null) {
			output.writeLong(value);
		}
	}

	/**
	 * 读取可选 long 值。
	 * Reads an optional long value.
	 */
	private static Long readOptionalLong(DataInputStream input) throws IOException {
		return input.readBoolean() ? input.readLong() : null;
	}

	/**
	 * 读取可选文本。
	 * Reads optional text.
	 */
	private static String readOptionalText(DataInputStream input) throws IOException {
		return input.readBoolean() ? input.readUTF() : null;
	}

	/**
	 * 读取并校验集合或字节计数。
	 * Reads and validates a collection or byte count.
	 */
	private static int readCount(DataInputStream input, String label, int maximum) throws IOException {
		int count = input.readInt();
		checkCount(label, count, maximum);
		return count;
	}

	/**
	 * 拒绝负数或超过格式上限的计数。
	 * Rejects negative counts or counts above the format limit.
	 */
	private static void checkCount(String label, int count, int maximum) {
		if (count < 0 || count > maximum) {
			throw new IllegalArgumentException("Invalid quest graph " + label + " count " + count);
		}
	}

	/**
	 * 添加唯一键并拒绝损坏负载中的重复项。
	 * Adds a unique key and rejects duplicates in corrupt payloads.
	 */
	private static <T> void putUnique(Map<String, T> values, String key, T value, String label) {
		if (values.putIfAbsent(key, value) != null) {
			throw new IllegalArgumentException("Duplicate quest graph " + label + " " + key);
		}
	}
}
