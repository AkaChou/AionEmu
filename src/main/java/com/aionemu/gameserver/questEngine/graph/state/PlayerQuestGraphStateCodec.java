package com.aionemu.gameserver.questEngine.graph.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineDisposition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;

/**
 * 使用有界、版本化二进制格式编码玩家任务图状态的动态负载。
 * Encodes the dynamic payload of player quest graph state with a bounded, versioned binary format.
 */
public final class PlayerQuestGraphStateCodec {

	private static final int MAGIC_QGS4 = 0x51475334;
	private static final int MAGIC_QGS5 = 0x51475335;
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
				output.writeInt(MAGIC_QGS5);
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
			if (magic != MAGIC_QGS4 && magic != MAGIC_QGS5) {
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
			PreparedTransition journal = readJournal(input, magic == MAGIC_QGS5);
			Map<String, CleanupLease> cleanupLeases = readCleanupLeases(input);
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
	private static PreparedTransition readJournal(DataInputStream input, boolean hasItemMutationPlans) throws IOException {
		if (!input.readBoolean()) {
			return null;
		}
		long baseRevision = input.readLong();
		String eventId = input.readUTF();
		String transitionId = input.readUTF();
		int nextActionIndex = input.readInt();
		RepeatDeadlineResolution repeatDeadlineResolution = readRepeatResolution(input);
		Map<Integer, ItemMutationPlan> itemMutationPlans = hasItemMutationPlans ? readItemMutationPlans(input) : Map.of();
		int payloadSize = readCount(input, "prepared event bytes", MAX_EVENT_PAYLOAD);
		byte[] eventPayload = input.readNBytes(payloadSize);
		if (eventPayload.length != payloadSize) {
			throw new EOFException("Prepared event payload is truncated");
		}
		return new PreparedTransition(baseRevision, eventId, transitionId, nextActionIndex, repeatDeadlineResolution, itemMutationPlans, eventPayload);
	}

	/** 写入按动作序号排序的冻结物品计划。 / Writes frozen item plans ordered by action index. */
	private static void writeItemMutationPlans(DataOutputStream output, Map<Integer, ItemMutationPlan> plans) throws IOException {
		checkCount("item mutation plans", plans.size(), MAX_ITEM_MUTATION_PLANS);
		output.writeInt(plans.size());
		for (ItemMutationPlan plan : plans.values()) {
			output.writeInt(plan.actionIndex());
			output.writeByte(switch (plan.kind()) {
				case GIVE_TOP_UP_TO -> ITEM_GIVE_TOP_UP_TO;
				case REMOVE_EXACT -> ITEM_REMOVE_EXACT;
				case REMOVE_OPTIONAL_EXACT -> ITEM_REMOVE_OPTIONAL_EXACT;
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
				case ITEM_REMOVE_EXACT -> ItemMutationKind.REMOVE_EXACT;
				case ITEM_REMOVE_OPTIONAL_EXACT -> ItemMutationKind.REMOVE_OPTIONAL_EXACT;
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
			output.writeUTF(entry.getValue().capability());
			output.writeUTF(entry.getValue().resourceKey());
		}
	}

	/**
	 * 读取 cleanup ledger 并拒绝重复 lease。
	 * Reads the cleanup ledger and rejects duplicate leases.
	 */
	private static Map<String, CleanupLease> readCleanupLeases(DataInputStream input) throws IOException {
		int count = readCount(input, "cleanup leases", MAX_CLEANUP_LEASES);
		Map<String, CleanupLease> cleanupLeases = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			String leaseId = input.readUTF();
			putUnique(cleanupLeases, leaseId, new CleanupLease(input.readUTF(), input.readUTF()), "cleanup lease");
		}
		return cleanupLeases;
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
