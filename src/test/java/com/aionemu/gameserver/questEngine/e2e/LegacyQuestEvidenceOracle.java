package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取 origin/history 正式模板合同，为页面、NPC 和状态时序提供独立证据；绝不读取顺序审计 CSV。
 * Reads origin/history retail-template contracts as independent evidence for pages, NPCs, and state timing; it
 * never reads the sequence-audit CSV.
 */
public final class LegacyQuestEvidenceOracle {
	private static final String FILE = "legacy-quest-dialog-contracts.csv";
	private final Map<Integer, Contract> contracts;

	private LegacyQuestEvidenceOracle(Map<Integer, Contract> contracts) {
		this.contracts = Map.copyOf(contracts);
	}

	/** 旧正式模板中与页面时序相关的最小合同。 / Minimal page-timing contract from a legacy retail template. */
	public record Contract(int questId, List<Integer> startNpcIds, List<Integer> endNpcIds,
			int startPageId, int reportPageId, int reportActionId, int rewardPageId,
			String reportSourceStatus, String reportTargetStatus) {
		public Contract {
			if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
			startNpcIds = List.copyOf(startNpcIds);
			endNpcIds = List.copyOf(endNpcIds);
			reportSourceStatus = normalize(reportSourceStatus);
			reportTargetStatus = normalize(reportTargetStatus);
		}

		private static String normalize(String value) {
			return value == null ? "" : value;
		}
	}

	/** 返回没有旧模板证据的空 oracle。 / Returns an empty oracle with no legacy evidence. */
	public static LegacyQuestEvidenceOracle empty() {
		return new LegacyQuestEvidenceOracle(Map.of());
	}

	/** 从客户端映射目录加载旧正式模板合同。 / Loads legacy retail-template contracts from the client-mapping directory. */
	public static LegacyQuestEvidenceOracle load(Path mappingDirectory) throws IOException {
		Path path = mappingDirectory.resolve(FILE);
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			if (header == null) throw new IOException("empty legacy contract CSV: " + path);
			List<String> columns = parse(stripBom(header));
			Map<Integer, Contract> result = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) continue;
				List<String> values = parse(line);
				if (values.size() != columns.size()) {
					throw new IOException("legacy contract column mismatch: " + line);
				}
				Map<String, String> row = new LinkedHashMap<>();
				for (int index = 0; index < columns.size(); index++) row.put(columns.get(index), values.get(index));
				int questId = integer(row, "quest_id");
				result.put(questId, new Contract(questId, ids(row.get("start_npc_ids")), ids(row.get("end_npc_ids")),
					integerOrZero(row, "start_page_id"), integerOrZero(row, "report_page_id"),
					integerOrZero(row, "report_action_id"), integerOrZero(row, "reward_page_id"),
					row.get("report_source_status"), row.get("report_target_status")));
			}
			return new LegacyQuestEvidenceOracle(result);
		}
	}

	/** 返回一个任务的旧合同；缺失时为空。 / Returns one quest's legacy contract, if present. */
	public Contract contract(int questId) {
		return contracts.get(questId);
	}

	/**
	 * 校验当前 transition 的 NPC、状态和响应页是否与旧正式模板合同一致。
	 * Validates the transition NPC, states, and response page against the legacy retail-template contract.
	 */
	public String mismatch(CompiledQuestDefinition definition, QuestTransition transition, int shownPage) {
		Contract contract = contracts.get(definition.id());
		if (contract == null) return "";
		QuestNode source = node(definition, transition.sourceNode());
		QuestNode target = node(definition, transition.targetNode());
		if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
			if (source != null && target != null && source.projection().status().name().equals("NONE")
					&& target.projection().status().name().equals("START")
					&& !contract.startNpcIds().contains(talk.npcId())) {
				return "legacy start NPC contract does not contain " + talk.npcId();
			}
			int expectedPage = expectedPage(contract, talk.npcId(), talk.dialogId(), source, target);
			if (expectedPage > 0 && shownPage > 0 && expectedPage != shownPage) {
				return "legacy page " + expectedPage + " differs from observed page " + shownPage;
			}
		}
		return "";
	}

	private static int expectedPage(Contract contract, int npcId, Integer dialogId, QuestNode source, QuestNode target) {
		if (dialogId == null || source == null || target == null) return 0;
		if (dialogId == 31 && source.projection().status().name().equals("NONE")
				&& contract.startNpcIds().contains(npcId)) return contract.startPageId();
		if (dialogId == 31 && source.projection().status().name().equals("START")
				&& contract.endNpcIds().contains(npcId)) return contract.reportPageId();
		if (dialogId == contract.reportActionId()) return contract.rewardPageId();
		return 0;
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream().filter(node -> java.util.Objects.equals(node.label(), label))
			.findFirst().orElse(null);
	}

	private static int integer(Map<String, String> row, String name) {
		String value = row.get(name);
		if (value == null || value.isBlank()) throw new IllegalArgumentException("missing legacy integer " + name);
		return Integer.parseInt(value);
	}

	private static int integerOrZero(Map<String, String> row, String name) {
		String value = row.get(name);
		return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
	}

	private static List<Integer> ids(String value) {
		if (value == null || value.isBlank()) return List.of();
		List<Integer> result = new ArrayList<>();
		for (String token : value.trim().split("\\s+")) result.add(Integer.parseInt(token));
		return result;
	}

	private static String stripBom(String value) {
		return value.startsWith("\uFEFF") ? value.substring(1) : value;
	}

	private static List<String> parse(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				} else quoted = !quoted;
			} else if (character == ',' && !quoted) {
				values.add(current.toString());
				current.setLength(0);
			} else current.append(character);
		}
		if (quoted) throw new IllegalArgumentException("unterminated legacy CSV quote");
		values.add(current.toString());
		return values;
	}
}
