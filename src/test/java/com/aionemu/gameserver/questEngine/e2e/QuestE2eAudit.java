package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eWorldFixture;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * 无服务器、无数据库的全量任务端到端报告入口；生产 catalog 编译失败会直接终止而不生成伪报告。
 * Serverless and database-free full quest end-to-end report entry point; catalog compilation failure terminates
 * without producing a fabricated report.
 */
public final class QuestE2eAudit {
	private QuestE2eAudit() {
	}

	/** 命令行参数为客户端映射目录、报告目录和摘要文件，均可省略使用仓库默认路径。 / CLI arguments are client mapping directory, report directory, and summary file; all may be omitted for repository defaults. */
	public static void main(String[] args) throws Exception {
		Logger questRuntimeLogger = (Logger) LoggerFactory.getLogger("QUEST_RUNTIME");
		Level previousLevel = questRuntimeLogger.getLevel();
		questRuntimeLogger.setLevel(Level.ERROR);
		try {
			Path mapping = args.length > 0 ? Path.of(args[0]) : Path.of("docs/quest/client-dialog-mapping");
			Path reportDirectory = args.length > 1 ? Path.of(args[1]) : Path.of("target/quest-e2e");
			Path summary = args.length > 2 ? Path.of(args[2]) : Path.of(".agent/summary/quest-e2e/summary.md");
			ClientResourceOracle oracle = ClientResourceOracle.load(mapping);
			LegacyQuestEvidenceOracle legacyEvidence = LegacyQuestEvidenceOracle.load(mapping);
			QuestWorldReachabilityOracle worldReachability = QuestWorldReachabilityOracle.loadProductionData();
			QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(QuestE2eAudit.class.getClassLoader());
			List<QuestE2eAuditRow> rows = QuestE2eBatchAudit.audit(catalog, oracle, legacyEvidence, worldReachability);
			QuestE2eReportWriter.write(rows, reportDirectory, summary);
			long pass = rows.stream().filter(row -> row.status() == QuestE2eStatus.PASS).count();
			System.out.println("quest-e2e rows=" + rows.size() + " pass=" + pass + " report=" + reportDirectory);
		} finally {
			questRuntimeLogger.setLevel(previousLevel);
			QuestE2eWorldFixture.shutdownPacketProcessor();
		}
	}
}
