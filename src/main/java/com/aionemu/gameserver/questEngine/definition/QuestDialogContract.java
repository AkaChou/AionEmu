package com.aionemu.gameserver.questEngine.definition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.configs.Config;

/**
 * 生产用客户端页面契约与 movie-only 例外 ledger。
 * Production client dialog page contract and movie-only exception ledger.
 */
public final class QuestDialogContract {
	public static final String CONTRACT_RESOURCE =
		"aion/definitions/quest_dialog/client_dialog_contract.tsv";
	public static final String EXCEPTIONS_RESOURCE =
		"aion/definitions/quest_dialog/movie_continuation_exceptions.tsv";
	private static final String CONTRACT_RELATIVE_PATH = "quest_dialog/client_dialog_contract.tsv";
	private static final String EXCEPTIONS_RELATIVE_PATH = "quest_dialog/movie_continuation_exceptions.tsv";

	private static final QuestDialogContract EMPTY = new QuestDialogContract(Map.of(), Set.of());
	private static volatile QuestDialogContract defaultContract;

	private final Map<Integer, Map<Integer, String>> buttonPages;
	private final Set<ExceptionKey> movieContinuationExceptions;

	private QuestDialogContract(Map<Integer, Map<Integer, String>> buttonPages,
			Set<ExceptionKey> movieContinuationExceptions) {
		this.buttonPages = buttonPages;
		this.movieContinuationExceptions = movieContinuationExceptions;
	}

	public static QuestDialogContract empty() {
		return EMPTY;
	}

	public static QuestDialogContract loadDefault() {
		QuestDialogContract result = defaultContract;
		if (result != null) {
			return result;
		}
		synchronized (QuestDialogContract.class) {
			result = defaultContract;
			if (result == null) {
				result = load();
				defaultContract = result;
			}
		}
		return result;
	}

	/**
	 * 优先读取外部 definitions 目录（生产布局，随任务数据一起热重载），缺失时回退 classpath（开发与测试）。
	 * Prefers the external definitions directory (production layout, reloaded with the quest data) and
	 * falls back to the classpath (development and tests).
	 */
	static QuestDialogContract load() {
		Path contractPath = Config.definitionFile(CONTRACT_RELATIVE_PATH).toPath();
		Path exceptionsPath = Config.definitionFile(EXCEPTIONS_RELATIVE_PATH).toPath();
		boolean contractPresent = Files.isRegularFile(contractPath);
		boolean exceptionsPresent = Files.isRegularFile(exceptionsPath);
		if (contractPresent != exceptionsPresent) {
			throw missing((contractPresent ? exceptionsPath : contractPath).toString());
		}
		if (!contractPresent) {
			return load(QuestDialogContract.class.getClassLoader());
		}
		try (InputStream contract = Files.newInputStream(contractPath);
				InputStream exceptions = Files.newInputStream(exceptionsPath)) {
			return parse(contract, exceptions);
		} catch (IOException e) {
			throw new QuestCompilationException("QUEST_DIALOG_CONTRACT_READ_FAILED",
				contractPath + ": " + e.getMessage());
		}
	}

	/**
	 * 丢弃缓存的默认契约，让任务热重载重新读取外部 definitions 目录。
	 * Drops the cached default contract so a quest hot reload re-reads the external definitions directory.
	 */
	public static void invalidateDefault() {
		synchronized (QuestDialogContract.class) {
			defaultContract = null;
		}
	}

	public static QuestDialogContract load(ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		try (InputStream contract = loader.getResourceAsStream(CONTRACT_RESOURCE);
				InputStream exceptions = loader.getResourceAsStream(EXCEPTIONS_RESOURCE)) {
			if (contract == null) {
				throw missing(CONTRACT_RESOURCE);
			}
			if (exceptions == null) {
				throw missing(EXCEPTIONS_RESOURCE);
			}
			return parse(contract, exceptions);
		} catch (IOException e) {
			throw new QuestCompilationException("QUEST_DIALOG_CONTRACT_READ_FAILED", e.getMessage());
		}
	}

	public boolean isEmpty() {
		return buttonPages.isEmpty();
	}

	public boolean hasButtonPage(int questId, int pageId) {
		return buttonPages.getOrDefault(questId, Map.of()).containsKey(pageId);
	}

	public String pageName(int questId, int pageId) {
		return buttonPages.getOrDefault(questId, Map.of()).get(pageId);
	}

	public boolean isMovieContinuationException(int questId, String sourceNode, int dialogId) {
		return movieContinuationExceptions.contains(new ExceptionKey(questId, sourceNode, dialogId));
	}

	public Set<ExceptionKey> movieContinuationExceptions() {
		return movieContinuationExceptions;
	}

	public record ExceptionKey(int questId, String sourceNode, int dialogId) {
		public ExceptionKey {
			if (questId <= 0) {
				throw new IllegalArgumentException("questId must be positive");
			}
			sourceNode = Objects.requireNonNull(sourceNode, "sourceNode");
			if (sourceNode.isBlank()) {
				throw new IllegalArgumentException("sourceNode must not be blank");
			}
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
		}
	}

	private static QuestDialogContract parse(InputStream contract, InputStream exceptions) throws IOException {
		return new QuestDialogContract(parseButtonPages(contract), parseExceptions(exceptions));
	}

	private static Map<Integer, Map<Integer, String>> parseButtonPages(InputStream input) throws IOException {
		Map<Integer, Map<Integer, String>> parsed = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length < 3) {
					throw invalid("expected quest_id, page_id, page_name: " + line);
				}
				int questId = parseInt(parts[0], "quest_id", line);
				int pageId = parseInt(parts[1], "page_id", line);
				parsed.computeIfAbsent(questId, ignored -> new LinkedHashMap<>())
					.put(pageId, parts[2].strip());
			}
		}
		Map<Integer, Map<Integer, String>> immutable = new HashMap<>();
		for (Map.Entry<Integer, Map<Integer, String>> entry : parsed.entrySet()) {
			immutable.put(entry.getKey(), Map.copyOf(entry.getValue()));
		}
		return Map.copyOf(immutable);
	}

	private static Set<ExceptionKey> parseExceptions(InputStream input) throws IOException {
		Set<ExceptionKey> result = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length < 4) {
					throw invalid("expected quest_id, source_node, dialog_id, reason: " + line);
				}
				result.add(new ExceptionKey(parseInt(parts[0], "quest_id", line),
					parts[1].strip(), parseInt(parts[2], "dialog_id", line)));
			}
		}
		return Set.copyOf(result);
	}

	private static int parseInt(String value, String field, String line) {
		try {
			return Integer.parseInt(value.strip());
		} catch (NumberFormatException e) {
			throw invalid(field + " is not an integer: " + line);
		}
	}

	private static QuestCompilationException invalid(String message) {
		return new QuestCompilationException("QUEST_DIALOG_CONTRACT_INVALID", message);
	}

	private static QuestCompilationException missing(String resource) {
		return new QuestCompilationException("QUEST_DIALOG_CONTRACT_MISSING", resource);
	}
}
