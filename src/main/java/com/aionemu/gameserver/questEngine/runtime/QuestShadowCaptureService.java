package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * 运维装配组件（阶段 3）：把候选目录、shadow runner、capture 与报告落盘闭合成服务器
 * 运行期可操作的 shadow 采集链路。
 *
 * <p>{@link #install} 原子安装 capture——旧 Handler 继续真实执行，采集零副作用；
 * {@link #drainAndPersist} 把当前批次差分报告原子落盘到迁移工具消费的
 * {@code unified-shadow-batch.json}；{@link #stop} 落盘剩余批次并恢复 no-op 桥。
 * 未安装时所有方法 fail-closed，绝不改变旧 QuestEngine 路由。</p>
 */
public final class QuestShadowCaptureService {
	private final QuestCatalog catalog;
	private final Set<Integer> expectedOwners;
	private final Path reportPath;
	private final QuestShadowCapture capture = new QuestShadowCapture();
	private QuestEngine engine;
	private QuestShadowRunner runner;

	public QuestShadowCaptureService(QuestCatalog catalog, Set<Integer> expectedOwners, Path reportPath) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		Set<Integer> owners = Set.copyOf(Objects.requireNonNull(expectedOwners, "expectedOwners"));
		if (owners.isEmpty()) {
			throw new IllegalArgumentException("expectedOwners must not be empty");
		}
		this.expectedOwners = owners;
		this.reportPath = Objects.requireNonNull(reportPath, "reportPath");
	}

	/** 原子安装 capture 到目标引擎；重复安装 fail-closed。 */
	public synchronized void install(QuestEngine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine must not be null");
		}
		if (installed()) {
			throw new IllegalStateException("shadow capture service is already installed");
		}
		this.runner = new QuestShadowRunner(catalog);
		this.engine = engine;
		engine.setShadowCapture(capture);
	}

	/** 落盘当前批次差分报告，返回报告供日志/断言；未安装 fail-closed。 */
	public synchronized QuestShadowBatchReport drainAndPersist() throws IOException {
		requireInstalled();
		QuestShadowBatchReport report = capture.drain(runner, expectedOwners);
		QuestShadowReportWriter.writeAtomic(reportPath, report);
		return report;
	}

	/** 落盘剩余批次并恢复 no-op 桥；未安装 fail-closed。 */
	public synchronized QuestShadowBatchReport stop() throws IOException {
		requireInstalled();
		QuestShadowBatchReport report = capture.drain(runner, expectedOwners);
		QuestShadowReportWriter.writeAtomic(reportPath, report);
		engine.setShadowCapture(null);
		engine = null;
		runner = null;
		return report;
	}

	public synchronized boolean installed() {
		return engine != null;
	}

	private void requireInstalled() {
		if (!installed()) {
			throw new IllegalStateException("shadow capture service is not installed");
		}
	}
}
