package com.aionemu.gameserver.questEngine.e2e.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 只读取四份 Aion 5.8 客户端导出表的页面与动作 oracle；它从不读取顺序审计产物。
 * Page/action oracle that reads only the four Aion 5.8 client export tables and never consumes sequence-audit
 * output.
 */
public final class ClientResourceOracle {
	public static final String QUEST_PAGES = "quest-dialog-pages.csv";
	public static final String QUEST_ACTIONS = "quest-dialog-action-details.csv";
	public static final String HYPERLINKS = "client-hyperlinks.csv";
	public static final String HTML_PAGES = "client-html-pages.csv";

	private final Map<Integer, Map<Integer, ClientPage>> questPages;
	private final Map<Integer, String> hyperlinks;
	private final Map<Integer, String> htmlPages;

	private ClientResourceOracle(Map<Integer, Map<Integer, ClientPage>> questPages,
			Map<Integer, String> hyperlinks, Map<Integer, String> htmlPages) {
		this.questPages = freezeNested(questPages);
		this.hyperlinks = Map.copyOf(hyperlinks);
		this.htmlPages = Map.copyOf(htmlPages);
	}

	/** 客户端任务页中的一个可见动作。 / One visible action on a client quest page. */
	public record ClientAction(int actionId, String actionConstant, String buttonTextZh) {
		public ClientAction {
			if (actionId == 0) {
				throw new IllegalArgumentException("actionId must not be zero");
			}
			actionConstant = Objects.requireNonNullElse(actionConstant, "");
			buttonTextZh = Objects.requireNonNullElse(buttonTextZh, "");
		}
	}

	/** 一个任务在 Aion 5.8 客户端中的具体页面及其可见动作。 / One concrete Aion 5.8 client quest page and its visible actions. */
	public record ClientPage(int questId, int pageId, String pageName, List<ClientAction> actions) {
		public ClientPage {
			if (questId <= 0 || pageId <= 0) {
				throw new IllegalArgumentException("questId and pageId must be positive");
			}
			pageName = Objects.requireNonNullElse(pageName, "");
			actions = List.copyOf(actions);
		}
	}

	/** 从固定目录装载四份客户端表。 / Loads the four client tables from one fixed directory. */
	public static ClientResourceOracle load(Path directory) throws IOException {
		Objects.requireNonNull(directory, "directory");
		Map<Integer, String> hyperlinkMap = readTwoColumn(directory.resolve(HYPERLINKS), "action_id",
			"action_constant");
		Map<Integer, String> htmlPageMap = readTwoColumn(directory.resolve(HTML_PAGES), "page_id",
			"page_constant");
		Map<Integer, Map<Integer, MutablePage>> mutablePages = new LinkedHashMap<>();
		for (Map<String, String> row : readRows(directory.resolve(QUEST_PAGES))) {
			if (!"active".equals(row.get("source_variant"))) {
				continue;
			}
			int questId = integer(row, "quest_id");
			int pageId = integer(row, "page_id");
			mutablePages.computeIfAbsent(questId, ignored -> new LinkedHashMap<>())
				.putIfAbsent(pageId, new MutablePage(questId, pageId, row.get("html_page_name")));
		}
		for (Map<String, String> row : readRows(directory.resolve(QUEST_ACTIONS))) {
			if (!"active".equals(row.get("source_variant"))) {
				continue;
			}
			int questId = integer(row, "quest_id");
			int pageId = integer(row, "page_id");
			MutablePage page = mutablePages.computeIfAbsent(questId, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(pageId, ignored -> new MutablePage(questId, pageId, row.get("html_page_name")));
			String actionValue = row.get("action_id");
			if (actionValue == null || actionValue.isBlank()) {
				continue;
			}
			int actionId = Integer.parseInt(actionValue);
			page.actions.add(new ClientAction(actionId, row.get("action_constant"), row.get("button_text_zh")));
		}
		Map<Integer, Map<Integer, ClientPage>> pages = new LinkedHashMap<>();
		mutablePages.forEach((questId, byPage) -> {
			Map<Integer, ClientPage> frozen = new LinkedHashMap<>();
			byPage.forEach((pageId, page) -> frozen.put(pageId, page.freeze()));
			pages.put(questId, frozen);
		});
		return new ClientResourceOracle(pages, hyperlinkMap, htmlPageMap);
	}

	/** 判断页面是否存在于通用 HtmlPages 或任务专用页面索引。 / Checks whether a page exists in HtmlPages or a quest-specific page index. */
	public boolean pageExists(int questId, int pageId) {
		return htmlPages.containsKey(pageId) || questPages.getOrDefault(questId, Map.of()).containsKey(pageId);
	}

	/**
	 * 任务作用域严格判定：页面必须在该任务自己的 quest html 段落索引中。客户端加载
	 * Quest_Q&lt;id&gt;.html 时按段落定位，全局 HtmlPages 注册表只证明页面 ID 是合法常量，
	 * 不证明该任务的文件包含该段落；任务无任何页面证据（导出表缺失）时回退全局表，避免无证据误报。
	 * Quest-scoped strict check: the page must exist in the quest's own quest html section index. The
	 * client loads Quest_Q&lt;id&gt;.html by section, so the global HtmlPages registry only proves the page
	 * id is a legal constant, not that this quest's file contains it; quests without any page evidence
	 * fall back to the global table to avoid evidence-free false positives.
	 */
	public boolean questPageExists(int questId, int pageId) {
		Map<Integer, ClientPage> taskPages = questPages.getOrDefault(questId, Map.of());
		if (taskPages.isEmpty()) {
			return htmlPages.containsKey(pageId);
		}
		return taskPages.containsKey(pageId);
	}

	/**
	 * 判断客户端是否会在该任务的任意任务页面上发送该动作；不在任何页面上的动作是死路由，
	 * 服务端为其发送缺失页面不会产生运行时影响。全局动作（任务列表、接受/拒绝流等）不在此列。
	 * Whether the client can send this action on any of the quest's own pages; actions absent from
	 * every page are dead routes whose missing-page responses have no runtime effect. Global actions
	 * (quest list, accept/refuse flow) are not covered here.
	 */
	public boolean actionVisibleOn(int questId, int actionId) {
		for (ClientPage page : questPages.getOrDefault(questId, Map.of()).values()) {
			for (ClientAction action : page.actions()) {
				if (action.actionId() == actionId) {
					return true;
				}
			}
		}
		return false;
	}

	/** 返回页面上由客户端资源直接证明可见的动作。 / Returns actions directly proven visible by client resources. */
	public List<ClientAction> visibleActions(int questId, int pageId) {
		ClientPage page = questPages.getOrDefault(questId, Map.of()).get(pageId);
		return page == null ? List.of() : page.actions();
	}

	/** 判断动作是否在 HyperLinks 中定义。 / Checks whether an action is defined by HyperLinks. */
	public boolean actionExists(int actionId) {
		return hyperlinks.containsKey(actionId);
	}

	/** 返回任务具备客户端页面证据的 ID 集。 / Returns quest ids with client-page evidence. */
	public Set<Integer> questIds() {
		return questPages.keySet();
	}

	private static Map<Integer, Map<Integer, ClientPage>> freezeNested(
			Map<Integer, Map<Integer, ClientPage>> source) {
		Map<Integer, Map<Integer, ClientPage>> result = new LinkedHashMap<>();
		source.forEach((questId, pages) -> result.put(questId,
			Collections.unmodifiableMap(new LinkedHashMap<>(pages))));
		return Collections.unmodifiableMap(result);
	}

	private static Map<Integer, String> readTwoColumn(Path path, String idField, String valueField)
			throws IOException {
		Map<Integer, String> result = new LinkedHashMap<>();
		for (Map<String, String> row : readRows(path)) {
			result.put(integer(row, idField), row.getOrDefault(valueField, ""));
		}
		return result;
	}

	private static int integer(Map<String, String> row, String field) {
		String value = row.get(field);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing CSV integer field " + field);
		}
		return Integer.parseInt(value);
	}

	private static List<Map<String, String>> readRows(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new IOException("empty CSV: " + path);
			}
			List<String> headers = parseCsv(stripBom(headerLine));
			List<Map<String, String>> result = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> values = parseCsv(line);
				if (values.size() != headers.size()) {
					throw new IOException("CSV column mismatch in " + path + ": " + line);
				}
				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < headers.size(); i++) {
					row.put(headers.get(i), values.get(i));
				}
				result.add(row);
			}
			return result;
		}
	}

	private static String stripBom(String value) {
		return value.startsWith("\uFEFF") ? value.substring(1) : value;
	}

	private static List<String> parseCsv(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '"') {
				if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					quoted = !quoted;
				}
			} else if (ch == ',' && !quoted) {
				values.add(current.toString());
				current.setLength(0);
			} else {
				current.append(ch);
			}
		}
		if (quoted) {
			throw new IllegalArgumentException("unterminated quoted CSV field");
		}
		values.add(current.toString());
		return values;
	}

	private static final class MutablePage {
		private final int questId;
		private final int pageId;
		private final String name;
		private final List<ClientAction> actions = new ArrayList<>();

		private MutablePage(int questId, int pageId, String name) {
			this.questId = questId;
			this.pageId = pageId;
			this.name = name;
		}

		private ClientPage freeze() {
			return new ClientPage(questId, pageId, name, actions);
		}
	}
}
