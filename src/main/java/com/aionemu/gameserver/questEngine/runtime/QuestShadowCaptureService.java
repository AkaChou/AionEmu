package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCandidateManifest;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 运维装配组件（阶段 3）：把候选目录、shadow runner、capture 与报告落盘闭合成服务器
 * 运行期可操作的 shadow 采集链路。
 *
 * <p>{@link #install} 原子安装 capture——旧 Handler 继续真实执行，采集零副作用；
 * {@link #drainAndPersist} 把当前批次差分报告原子落盘到配置的累计报告路径；
 * {@link #stop} 落盘剩余批次并恢复 no-op 桥。
 * 未安装时所有方法 fail-closed，绝不改变旧 QuestEngine 路由。</p>
 */
public final class QuestShadowCaptureService {
	private static final String CANDIDATE_MANIFEST =
		"aion/data/static_data/quest_definition/quest_definition_candidate_manifest.xml";
	private final QuestCatalog catalog;
	private final Set<Integer> expectedOwners;
	private final Path reportPath;
	private final QuestShadowCapture capture;
	private QuestEngine engine;
	private QuestShadowRunner runner;
	private QuestShadowBatchReport accumulated;

	public QuestShadowCaptureService(QuestCatalog catalog, Set<Integer> expectedOwners, Path reportPath) {
		this(catalog, expectedOwners, reportPath, new QuestShadowCapture());
	}

	private QuestShadowCaptureService(QuestCatalog catalog, Set<Integer> expectedOwners, Path reportPath,
			QuestShadowCapture capture) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		Set<Integer> owners = Set.copyOf(Objects.requireNonNull(expectedOwners, "expectedOwners"));
		if (owners.isEmpty()) {
			throw new IllegalArgumentException("expectedOwners must not be empty");
		}
		this.expectedOwners = owners;
		this.reportPath = Objects.requireNonNull(reportPath, "reportPath");
		this.capture = Objects.requireNonNull(capture, "capture");
	}

	/** Builds the production capture graph from the packaged single-owner candidate manifest. */
	public static QuestShadowCaptureService production(Path reportPath) {
		ClassLoader loader = QuestShadowCaptureService.class.getClassLoader();
		QuestCatalog catalog;
		try (InputStream manifest = loader.getResourceAsStream(CANDIDATE_MANIFEST)) {
			if (manifest == null) {
				throw new IllegalStateException("missing quest candidate manifest: " + CANDIDATE_MANIFEST);
			}
			catalog = QuestDefinitionCandidateManifest.compile(manifest, loader);
		} catch (IOException failure) {
			throw new IllegalStateException("failed to read quest candidate manifest", failure);
		}
		Set<Integer> owners = catalog.all().stream().map(CompiledQuestDefinition::id)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		Set<Integer> eligibilityOwners = new LinkedHashSet<>();
		for (CompiledQuestDefinition definition : catalog.all()) {
			boolean required = definition.definition().transitions().stream()
				.flatMap(transition -> transition.conditions().stream())
				.anyMatch(QuestCondition.StartEligible.class::isInstance);
			if (required) {
				eligibilityOwners.add(definition.id());
			}
		}
		QuestPlayerPort players = playerId -> GameWorldBootstrapServices.world().findPlayer(playerId);
		QuestShadowCapture capture = new QuestShadowCapture(
			new PlayerQuestStartEligibilityPort(players), eligibilityOwners);
		return new QuestShadowCaptureService(catalog, owners, reportPath, capture);
	}

	/** 原子安装 capture 到目标引擎；重复安装 fail-closed。 */
	public synchronized void install(QuestEngine engine) throws IOException {
		if (engine == null) {
			throw new IllegalArgumentException("engine must not be null");
		}
		if (installed()) {
			throw new IllegalStateException("shadow capture service is already installed");
		}
		QuestShadowRunner newRunner = new QuestShadowRunner(catalog);
		QuestShadowBatchReport empty = QuestShadowBatchRunner.compare(newRunner, java.util.List.of(), expectedOwners);
		QuestShadowBatchReport restored = empty;
		if (Files.exists(reportPath)) {
			if (!Files.isRegularFile(reportPath)) {
				throw new IllegalArgumentException("shadow report path is not a regular file: " + reportPath);
			}
			restored = QuestShadowReportWriter.read(reportPath);
		}
		// merge performs the strict expected-owner and expected-coverage compatibility check.
		this.accumulated = empty.merge(restored);
		this.runner = newRunner;
		engine.setShadowCapture(capture);
		this.engine = engine;
	}

	/** 落盘当前批次差分报告，返回报告供日志/断言；未安装 fail-closed。 */
	public synchronized QuestShadowBatchReport drainAndPersist() throws IOException {
		requireInstalled();
		QuestShadowBatchReport current = capture.drain(runner, expectedOwners);
		accumulated = accumulated.merge(current);
		QuestShadowReportWriter.writeAtomic(reportPath, accumulated);
		return accumulated;
	}

	/** 落盘剩余批次并恢复 no-op 桥；未安装 fail-closed。 */
	public synchronized QuestShadowBatchReport stop() throws IOException {
		requireInstalled();
		try {
			return drainAndPersist();
		} finally {
			detach();
		}
	}

	/**
	 * Rolls back an installation that could not finish its surrounding runtime
	 * assembly. Pending samples are discarded and never become migration evidence.
	 */
	public synchronized void abort() {
		requireInstalled();
		detach();
	}

	public synchronized boolean installed() {
		return engine != null;
	}

	public Set<Integer> expectedOwners() {
		return expectedOwners;
	}

	public Path reportPath() {
		return reportPath;
	}

	private void requireInstalled() {
		if (!installed()) {
			throw new IllegalStateException("shadow capture service is not installed");
		}
	}

	private void detach() {
		QuestEngine installedEngine = engine;
		engine = null;
		runner = null;
		accumulated = null;
		capture.discard();
		installedEngine.setShadowCapture(null);
	}
}
