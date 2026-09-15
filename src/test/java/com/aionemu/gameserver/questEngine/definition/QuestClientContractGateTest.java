package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定的全量客户端任务契约门禁：当前 IR 不得产生新的任务页缺失或可见按钮无路由。
 * Locked full-catalog client quest contract gate: the current IR must not introduce a missing task page or a
 * visible button without a route.
 *
 * <p>已有缺陷保存在 TSV 基线中，出现新问题则直接失败；该门禁不把尚无权威页面的历史缺口当作已修复，也不
 * 允许使用任务级通配豁免。默认允许尚未来得及清理的旧指纹，启用
 * {@code -Dquest.client.contract.failOnStaleBaseline=true} 后要求基线同步删除已修复项。</p>
 * Existing defects are retained in a TSV baseline, and new defects fail immediately. The gate neither treats
 * unresolved historical evidence gaps as repaired nor permits quest-wide wildcard exemptions. Stale entries are
 * allowed by default and become a failure when {@code -Dquest.client.contract.failOnStaleBaseline=true} is set.
 */
class QuestClientContractGateTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static final String BASELINE_RESOURCE = "/quest/quest-client-contract-baseline.tsv";
	private static final int MAX_REPORTED_FINGERPRINTS = 30;
	private static final boolean FAIL_ON_STALE_BASELINE = Boolean.getBoolean(
		"quest.client.contract.failOnStaleBaseline");

	@Test
	void productionQuestDialogsDoNotIntroduceFatalClientContractRegressions() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = QuestDialogOrderAudit.readClientPages(
			CLIENT_MAPPING.resolve("quest-dialog-pages.csv"),
			CLIENT_MAPPING.resolve("quest-dialog-action-details.csv"));
		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);
		List<QuestPrematureRewardRouteAudit.Violation> prematureRewardRoutes =
			QuestPrematureRewardRouteAudit.audit(catalog, clientQuests);
		assertTrue(prematureRewardRoutes.isEmpty(), () -> prematureRewardFailureMessage(prematureRewardRoutes));

		Set<String> current = rows.stream()
			.map(QuestClientContractGateTest::fatalFingerprint)
			.flatMap(Optional::stream)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<String> baseline = loadBaseline();

		List<String> introduced = difference(current, baseline);
		assertTrue(introduced.isEmpty(), () -> failureMessage(
			"新的客户端任务契约问题 / New client quest contract failures", introduced));

		List<String> repairedButStillBaseline = difference(baseline, current);
		if (FAIL_ON_STALE_BASELINE) {
			assertTrue(repairedButStillBaseline.isEmpty(), () -> failureMessage(
				"基线中的问题已经不再复现，必须删除对应指纹 / Baseline entries no longer reproduce and must be removed",
				repairedButStillBaseline));
		}
	}

	private static Optional<String> fatalFingerprint(QuestDialogOrderAudit.AuditRow row) {
		String reason = row.unresolvedReason();
		String failureType;
		if (reason.startsWith("compiled IR emits a task page absent from the active client page index")) {
			failureType = "PAGE_NOT_IN_TASK_HTML";
		} else if (reason.startsWith("visible client action has no route")) {
			failureType = "BUTTON_WITHOUT_ROUTE";
		} else {
			return Optional.empty();
		}
		return Optional.of(String.join("\t", failureType, Integer.toString(row.questId()),
			row.serverSourceState(), row.npcId(), row.triggerAction(), row.shownPage(),
			row.clientVisibleAction(), row.actualPath()));
	}

	private static Set<String> loadBaseline() throws Exception {
		InputStream input = QuestClientContractGateTest.class.getResourceAsStream(BASELINE_RESOURCE);
		assertNotNull(input, "missing baseline resource " + BASELINE_RESOURCE);
		Set<String> fingerprints = new LinkedHashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String fingerprint = line;
				String[] fields = fingerprint.split("\t", -1);
				assertEquals(8, fields.length,
					() -> "invalid baseline fingerprint field count: " + fingerprint);
				assertTrue(fingerprints.add(fingerprint), () -> "duplicate baseline fingerprint: " + fingerprint);
			}
		}
		return fingerprints;
	}

	private static List<String> difference(Set<String> left, Set<String> right) {
		List<String> result = new ArrayList<>(left.stream().filter(value -> !right.contains(value)).toList());
		result.sort(String::compareTo);
		return List.copyOf(result);
	}

	private static String failureMessage(String heading, List<String> fingerprints) {
		String sample = fingerprints.stream().limit(MAX_REPORTED_FINGERPRINTS)
			.map(fingerprint -> "  " + fingerprint.replace('\t', '|'))
			.collect(Collectors.joining("\n"));
		return heading + " count=" + fingerprints.size()
			+ (sample.isEmpty() ? "" : "\n" + sample);
	}

	private static String prematureRewardFailureMessage(
		List<QuestPrematureRewardRouteAudit.Violation> violations) {
		String sample = violations.stream().limit(MAX_REPORTED_FINGERPRINTS)
			.map(violation -> "  " + violation.questId() + "|" + violation.sourceNode() + "|"
				+ violation.npcId() + "|" + violation.dialogId() + "|" + violation.reason() + "|"
				+ violation.evidence())
			.collect(Collectors.joining("\n"));
		return "SETPRO 直接领奖路由缺少高置信保护 / high-confidence SETPRO premature reward routes count="
			+ violations.size() + (sample.isEmpty() ? "" : "\n" + sample);
	}
}
