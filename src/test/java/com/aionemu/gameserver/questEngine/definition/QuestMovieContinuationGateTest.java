package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1/2 硬门禁：生产契约里的同状态 movie-only page-turn 必须与显式 ledger 完全一致。
 * Phase 1/2 hard gate: same-state movie-only page turns in the production contract must match the explicit ledger.
 * <p>生产编译器已在 `QuestDefinitionCompiler` 中 fail-closed；本测试额外验证 ledger 没有遗留条目，
 * 即修复后的 ledger 也必须同步缩小。Production compilation already fails closed in
 * `QuestDefinitionCompiler`; this test additionally verifies there are no stale ledger entries.</p>
 * <p>同时校验签入的契约 TSV 与 tracked 客户端 CSV 的源哈希一致，避免契约腐烂后编译期检查静默失效。
 * It also verifies the checked-in contract TSV still matches the tracked client mapping CSVs, so a
 * stale contract cannot silently disable the compile-time check.</p>
 * <p>最后锁定生产加载路径：契约优先来自外部 definitions 目录，只有该目录没有副本时才回退 classpath。
 * Finally it locks the production loading path: the contract comes from the external definitions
 * directory first and only falls back to the classpath when that directory has no copy.</p>
 */
class QuestMovieContinuationGateTest {
	@Test
	void sameStateMovieOnlyPageTurnsMatchTheProductionLedger() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		QuestDialogContract contract = QuestDialogContract.loadDefault();

		Set<LedgerKey> actual = new TreeSet<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			for (QuestMovieContinuation.Violation violation : QuestMovieContinuation.violations(
					compiled.definition(), contract)) {
				actual.add(new LedgerKey(violation.questId(), violation.sourceNode(), violation.dialogId()));
			}
		}
		Set<LedgerKey> ledger = contract.movieContinuationExceptions().stream()
			.map(exception -> new LedgerKey(exception.questId(), exception.sourceNode(), exception.dialogId()))
			.collect(Collectors.toCollection(TreeSet::new));

		assertEquals(ledger, actual,
			() -> "same-state movie-only page-turn violations changed; update the explicit ledger: " + actual);
	}

	@Test
	void compilerRejectsMovieOnlyPageTurnWithoutLedger() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/14045.xml";
		String xml;
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		// 把 movie-page-turn 块替换回「只播影片」的旧写法，验证编译期仍然拒绝该形态。
		// Replaces the movie-page-turn block with the legacy movie-only shape to prove compilation still
		// rejects it.
		String closing = "</movie-page-turn>";
		int start = xml.indexOf("<movie-page-turn");
		int end = xml.indexOf(closing);
		assertTrue(start > 0 && end > start, "14045 must author its movie page turn with the block");
		String mutated = xml.substring(0, start) + """
			<transition source="started" target="started">
			      <event>
			        <dialog type="TALK_TO_NPC" npc-id="278506" action="SELECT1_1_1"/>
			      </event>
			      <conditions>
			        <variable-is field="var0" value="0"/>
			      </conditions>
			      <after-commit>
			        <play-movie movie-id="272"/>
			      </after-commit>
			    </transition>""" + xml.substring(end + closing.length());

		QuestCompilationException failure = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(mutated.getBytes(StandardCharsets.UTF_8))));
		assertEquals("MOVIE_WITHOUT_CONTINUATION", failure.code());
	}

	@Test
	void checkedInContractMatchesTheTrackedClientMappingCsv() throws Exception {
		Path pages = Path.of("docs/quest/client-dialog-mapping/quest-dialog-pages.csv");
		Path actions = Path.of("docs/quest/client-dialog-mapping/quest-dialog-action-details.csv");
		Map<String, String> headers = contractHeaders();

		assertEquals(sha256(pages), headers.get("source_pages_sha256"),
			() -> "quest-dialog-pages.csv changed; regenerate the contract with "
				+ "python3 .agents/summary/quest/generate_quest_dialog_contract.py");
		assertEquals(sha256(actions), headers.get("source_actions_sha256"),
			() -> "quest-dialog-action-details.csv changed; regenerate the contract with "
				+ "python3 .agents/summary/quest/generate_quest_dialog_contract.py");
	}

	@Test
	void externalDefinitionsDirectoryTakesPrecedenceOverTheClasspathCopy() throws Exception {
		Path directory = Files.createTempDirectory("quest-dialog-contract-external");
		Path questDialog = Files.createDirectories(directory.resolve("quest_dialog"));
		Files.writeString(questDialog.resolve("client_dialog_contract.tsv"),
			"# generated\n# quest_id\tpage_id\tpage_name\n424242\t7\tTEST_PAGE\n",
			StandardCharsets.UTF_8);
		Files.writeString(questDialog.resolve("movie_continuation_exceptions.tsv"),
			"# quest_id\tsource_node\tdialog_id\treason\n", StandardCharsets.UTF_8);

		withDefinitionsDirectory(directory, () -> {
			QuestDialogContract external = QuestDialogContract.load();
			assertTrue(external.hasButtonPage(424242, 7), "external definitions contract was not used");
			assertFalse(external.hasButtonPage(14045, 1013), "classpath contract leaked into the external load");
		});
	}

	@Test
	void classpathContractIsUsedWhenTheDefinitionsDirectoryHasNoCopy() throws Exception {
		Path directory = Files.createTempDirectory("quest-dialog-contract-empty");

		withDefinitionsDirectory(directory, () -> {
			QuestDialogContract fallback = QuestDialogContract.load();
			assertTrue(fallback.hasButtonPage(14045, 1013), "classpath fallback did not resolve the contract");
		});
	}

	@Test
	void invalidatingTheDefaultContractReloadsFromTheDefinitionsDirectory() throws Exception {
		Path directory = Files.createTempDirectory("quest-dialog-contract-reload");
		Path questDialog = Files.createDirectories(directory.resolve("quest_dialog"));
		Files.writeString(questDialog.resolve("client_dialog_contract.tsv"),
			"# generated\n# quest_id\tpage_id\tpage_name\n424243\t8\tRELOAD_PAGE\n",
			StandardCharsets.UTF_8);
		Files.writeString(questDialog.resolve("movie_continuation_exceptions.tsv"),
			"# quest_id\tsource_node\tdialog_id\treason\n", StandardCharsets.UTF_8);

		assertFalse(QuestDialogContract.loadDefault().hasButtonPage(424243, 8),
			"temporary reload page must not exist before the reload");
		withDefinitionsDirectory(directory, () -> {
			QuestDialogContract.invalidateDefault();
			assertTrue(QuestDialogContract.loadDefault().hasButtonPage(424243, 8),
				"hot reload did not re-read the external contract");
		});
		QuestDialogContract.invalidateDefault();
		assertFalse(QuestDialogContract.loadDefault().hasButtonPage(424243, 8),
			"reload test leaked the temporary contract into the cached default");
	}

	private Map<String, String> contractHeaders() throws Exception {
		Map<String, String> headers = new HashMap<>();
		String resource = "/" + QuestDialogContract.CONTRACT_RESOURCE;
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			assertNotNull(input, "generated contract resource is missing: " + resource);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null && line.startsWith("#")) {
					int separator = line.indexOf('=');
					if (separator > 0) {
						headers.put(line.substring(1, separator).strip(), line.substring(separator + 1).strip());
					}
				}
			}
		}
		return headers;
	}

	private static String sha256(Path path) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (InputStream input = Files.newInputStream(path)) {
			byte[] buffer = new byte[8192];
			for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
				digest.update(buffer, 0, read);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void withDefinitionsDirectory(Path directory, ThrowingRunnable assertion) throws Exception {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", directory.toString());
		try {
			assertion.run();
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	private record LedgerKey(int questId, String sourceNode, int dialogId) implements Comparable<LedgerKey> {
		@Override
		public int compareTo(LedgerKey other) {
			int byQuest = Integer.compare(questId, other.questId);
			if (byQuest != 0) {
				return byQuest;
			}
			int bySource = sourceNode.compareTo(other.sourceNode);
			if (bySource != 0) {
				return bySource;
			}
			return Integer.compare(dialogId, other.dialogId);
		}
	}
}
