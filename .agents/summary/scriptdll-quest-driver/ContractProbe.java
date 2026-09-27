package com.aionemu.gameserver.questEngine.definition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

/**
 * 契约门探针：用与 QuestClientContractGateTest 同一套装载（XML 目录 + 真端 overlay）复算
 * fatal 指纹，并按给定任务 id 过滤打印（门禁只打新增项、且截断 30 条，单任务诊断需要本探针）。
 * Contract-gate probe: recomputes the fatal fingerprints through the same load path the gate uses
 * (XML directory plus the retail overlay) and prints them filtered by quest id (the gate reports
 * only introduced entries and truncates at 30, so per-quest diagnosis needs this probe).
 *
 * 用法 / usage: java -cp <classes>:<cp> com.aionemu.gameserver.questEngine.definition.ContractProbe <questId...>
 */
public final class ContractProbe {

	private ContractProbe() {
	}

	public static void main(String[] args) throws Exception {
		QuestCatalog catalog = RetailQuestDriver.overlay(
			QuestDefinitionDirectoryLoader.compile(ContractProbe.class.getClassLoader()));
		Path mapping = Path.of("docs/quest/client-dialog-mapping");
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = QuestDialogOrderAudit.readClientPages(
			mapping.resolve("quest-dialog-pages.csv"), mapping.resolve("quest-dialog-action-details.csv"));
		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);
		java.util.Set<String> wanted = new java.util.HashSet<>(List.of(args));
		int total = 0;
		for (QuestDialogOrderAudit.AuditRow row : rows) {
			String reason = row.unresolvedReason();
			String failureType;
			if (reason.startsWith("compiled IR emits a task page absent from the active client page index")) {
				failureType = "PAGE_NOT_IN_TASK_HTML";
			} else if (reason.startsWith("visible client action has no route")) {
				failureType = "BUTTON_WITHOUT_ROUTE";
			} else {
				continue;
			}
			total++;
			if (!wanted.isEmpty() && !wanted.contains(Integer.toString(row.questId()))) {
				continue;
			}
			System.out.println(String.join("\t", failureType, Integer.toString(row.questId()),
				row.serverSourceState(), row.npcId(), row.triggerAction(), row.shownPage(),
				row.clientVisibleAction(), row.actualPath()));
		}
		System.out.println("fatal_total=" + total);
		if (!Boolean.getBoolean("probe.all")) {
			return;
		}
		// -Dprobe.all=true：打印给定任务的全部审计行（含匹配行），用于定位“同样形状为何只有一行被
		// 判缺陷”的差异（候选路由列即证据）。
		// -Dprobe.all=true dumps every audit row of the given quests (matched rows included) to locate
		// why one of two identically-shaped rows is flagged (the candidate columns are the evidence).
		for (QuestDialogOrderAudit.AuditRow row : rows) {
			if (!wanted.contains(Integer.toString(row.questId()))) {
				continue;
			}
			String reason = row.unresolvedReason();
			System.out.println(String.join("\t", Integer.toString(row.questId()), row.serverSourceState(),
				row.npcId(), row.triggerAction(), row.shownPage(), row.clientVisibleAction(),
				row.clientExpected(), Integer.toString(row.candidateCount()),
				row.candidate() == null ? "-" : row.candidate().sourceNode(),
				row.candidate() == null ? "-" : row.candidate().response(),
				row.auditStatus(), reason.substring(0, Math.min(90, reason.length()))));
		}
	}
}
