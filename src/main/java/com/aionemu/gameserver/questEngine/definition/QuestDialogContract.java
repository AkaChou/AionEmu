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
import java.util.List;
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

	/**
	 * 真端接取入口页偏好序：极简信页 → 对话入口页 → 接取窗页（兜底）。
	 * <p>
	 * 真端入口页来自**信页/阶段页表**而不是接取窗：`fun_731` 的入口选择器对未接态
	 * （状态 0/10）返回 `select_none`(4762)、阶段页 `select1..select14`(1011/1352/1693/…/7864)
	 * 或 `default_success`(10002)，**从不返回接取窗页 4**；页 4 只能由页动作 `1007`
	 * （`ASK_QUEST_ACCEPT` → `mgr+0x1a0`）打开。故本车道入口先发信页（4762 → 1011），
	 * 仅在客户端两者都未声明时回落到页 4（客户端任务页必须声明该页，否则 load fail）。
	 * <p>
	 * The retail accept entry page comes from the letter/stage page table, never from the ask window:
	 * the {@code fun_731} entry selector returns {@code select_none}(4762), a stage page
	 * ({@code select1}..{@code select14}) or {@code default_success}(10002) for the unaccepted states,
	 * and page 4 is reachable only through the {@code 1007} page action ({@code mgr+0x1a0}).
	 */
	private static final List<Integer> RETAIL_ENTRY_PAGE_PREFERENCE = List.of(
		QuestDialogPage.SELECT_NONE.id(),
		QuestDialogPage.SELECT1.id(),
		QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id());

	private static final QuestDialogContract EMPTY = new QuestDialogContract(Map.of(), Set.of());
	private static volatile QuestDialogContract defaultContract;

	private final Map<Integer, Map<Integer, String>> buttonPages;
	private final Set<ExceptionKey> movieContinuationExceptions;

	private QuestDialogContract(Map<Integer, Map<Integer, String>> buttonPages,
			Set<ExceptionKey> movieContinuationExceptions) {
		this.buttonPages = buttonPages;
		this.movieContinuationExceptions = movieContinuationExceptions;
	}

	public Map<Integer, String> pagesForQuest(int questId) {
		return buttonPages.getOrDefault(questId, Map.of());
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

	/**
	 * 真端表车道的接取入口页（信页优先）：{@code select_none}(4762) → {@code select1}(1011) → 页 4 兜底。
	 * <p>
	 * 页 4 不是首选：真端页 4 只能由页动作 {@code 1007} 打开，作为入口直接下发会使信页（以及挂在
	 * {@code 1007} 上的过场）永不可达。旧「页 4 优先」typed 形（{@code acceptEntryPage}）随 P7 步 f
	 * 的 DD 编译车道退场删除（§10.3-#22）。
	 * <p>
	 * The native-lane accept entry page (letter page first): retail reaches page 4 only through the
	 * {@code 1007} page action, so emitting it first would make the letter page unreachable. The old
	 * ask-window-first typed form retired with the DD compile lane in P7 step f (§10.3-#22).
	 */
	public int retailEntryPage(int questId) {
		for (int pageId : RETAIL_ENTRY_PAGE_PREFERENCE) {
			if (hasButtonPage(questId, pageId)) {
				return pageId;
			}
		}
		return QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
	}

	/**
	 * 页动作 {@code 1007}({@code ASK_QUEST_ACCEPT}) 的目标页：真端 {@code mgr+0x1a0} 打开接取窗页 4；
	 * 客户端任务页未声明该页时返回 {@code -1}（fail-closed，不发明页，否则客户端 load fail）。
	 * <p>
	 * The target page of the {@code 1007} page action: retail {@code mgr+0x1a0} opens ask window page 4.
	 * A quest whose client task page does not declare page 4 gets {@code -1} (fail closed).
	 */
	public int askWindowPage(int questId) {
		int pageId = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
		return hasButtonPage(questId, pageId) ? pageId : -1;
	}

	/**
	 * 报告确认页（裁定 a，2026-10-03）：旧编译器 {@code NPC_REPORT} 分型三页
	 * {@code SELECT2(1352)/SELECT5(2375)/DEFAULT_SUCCESS(10002)}，以客户端任务页声明为准
	 * （契约与退役 XML 交叉印证一致：80787 族=10002、1101=2375、1102=1352）。两步语义：
	 * 任务行选择（31）只发该页不推进，报告确认——直翻型 {@code 1009} 或检查型
	 * {@code 39}（{@link #checkFailPage}）——才推进 REWARD + 奖励窗；
	 * {@code 10002}(select_success) 为客户端自动确认页。无声明返回 {@code -1}（调用方按
	 * DEFAULT_SUCCESS 行为一步直达奖励窗，保持可用）。
	 * <p>
	 * The report-confirm page (retail NPC_REPORT three-page typing, adjudicated by the client
	 * contract): the row selection shows it without advancing; {@code 1009} advances to REWARD.
	 * {@code -1} = undeclared, callers fall back to the one-step reward window.
	 */
	public int reportConfirmPage(int questId) {
		for (int pageId : new int[] { QuestDialogPage.SELECT2.id(), QuestDialogPage.SELECT5.id(),
				QuestDialogPage.DEFAULT_SUCCESS.id() }) {
			if (hasButtonPage(questId, pageId)) {
				return pageId;
			}
		}
		return -1;
	}

	/**
	 * 39 检查按钮（{@code HACTION_CHECK_USER_HAS_QUEST_ITEM}）的失败结果页（{@code select6=2716}）：
	 * 报告页的「拿出…」按钮触发服务端整组检查，未通过时真端下发客户端声明的失败应答页
	 * （任务作者写作 select6，如 1103「你难道不会数数吗」/ 1105「您别跟我开玩笑」）。
	 * 两族 39 用户（SimpleTalk 675 + SimpleCollectItem 85，2026-10-04 全量客户端页普查）全部声明该页；
	 * 未声明返回 {@code -1}（fail-closed，不发明页）。
	 * <p>
	 * The failure result page of the 39 check button ({@code select6=2716}): the report page's check
	 * button triggers the server-side whole-group check; when the group is missing the retail server
	 * shows the client-declared fail page. Every in-family 39 row declares it; undeclared rows return
	 * {@code -1} (fail closed, never invent a page).
	 */
	public int checkFailPage(int questId) {
		int pageId = QuestDialogPage.SELECT6.id();
		return hasButtonPage(questId, pageId) ? pageId : -1;
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
