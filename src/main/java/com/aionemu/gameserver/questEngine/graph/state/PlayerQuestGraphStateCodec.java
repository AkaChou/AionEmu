package com.aionemu.gameserver.questEngine.graph.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;

/**
 * 使用有界、版本化二进制格式编码玩家任务图状态的动态负载。
 * Encodes the dynamic payload of player quest graph state with a bounded, versioned binary format.
 */
public final class PlayerQuestGraphStateCodec {

	private static final int MAGIC = 0x51475331;
	private static final int MAX_VARIABLES = 1024;
	private static final int MAX_DEADLINES = 256;
	private static final int MAX_CLEANUP_LEASES = 1024;
	private static final int MAX_EVENT_PAYLOAD = 1024 * 1024;
	private static final int MAX_TOTAL_PAYLOAD = 2 * 1024 * 1024;
	private static final byte INT_VALUE = 1;
	private static final byte BOOLEAN_VALUE = 2;

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
				output.writeInt(MAGIC);
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
			if (input.readInt() != MAGIC) {
				throw new IllegalArgumentException("Unsupported quest graph state payload version");
			}
			Map<String, VariableValue> variables = readVariables(input);
			Map<String, Long> deadlines = readDeadlines(input);
			PreparedTransition journal = readJournal(input);
			Map<String, CleanupLease> cleanupLeases = readCleanupLeases(input);
			String quarantineReason = readOptionalText(input);
			if (input.read() != -1) {
				throw new IllegalArgumentException("Quest graph state payload has trailing data");
			}
			return new PlayerQuestGraphState(questId, definitionVersion, revision, nodeId, instanceRunId, lifecycle, variables,
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
	private static PreparedTransition readJournal(DataInputStream input) throws IOException {
		if (!input.readBoolean()) {
			return null;
		}
		long baseRevision = input.readLong();
		String eventId = input.readUTF();
		String transitionId = input.readUTF();
		int nextActionIndex = input.readInt();
		int payloadSize = readCount(input, "prepared event bytes", MAX_EVENT_PAYLOAD);
		byte[] eventPayload = input.readNBytes(payloadSize);
		if (eventPayload.length != payloadSize) {
			throw new EOFException("Prepared event payload is truncated");
		}
		return new PreparedTransition(baseRevision, eventId, transitionId, nextActionIndex, eventPayload);
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

