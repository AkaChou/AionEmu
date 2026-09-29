package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 客户端 talk+collect 混合链登记（原 {@code quest_client_talk_collect_chain_pages.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 每阶段列 = {@code talk:p1>p2>...:advance} 或 {@code collect:p:advance:ok页:fail页}：talk 段 =
 * select{i} 页梯 + 梯尾推进按钮；collect 段 = 段首页 + 39 检查按钮 + check_user_item_ok/fail
 * 结果页。阶段类别序列与真端表 stepCategories 逐位一致。
 * <p>
 * Read-only view of the client talk+collect chain registry; the stage kinds mirror the retail
 * table's step categories position for position.
 */
public final class RetailClientTalkCollectChainPages {

	/** 阶段类别。 / Stage kind. */
	enum Kind {
		TALK, COLLECT
	}

	/**
	 * 一个阶段的页梯与推进/检查动作。
	 * One stage's page ladder with its advance (talk) or item-check (collect) action.
	 *
	 * @param kind            段类别（talk / collect）
	 * @param ladder          页梯（talk 段含导航链；collect 段含信息页扇出）
	 * @param advanceActionId talk 段推进按钮 / collect 段检查按钮（39）
	 * @param resultPages     collect 段结果页 {ok, fail}；talk 段为 null
	 * @param movieId         梯尾页 CutScene 声明的过场 id（无过场为 null）
	 */
	record Stage(Kind kind, List<Integer> ladder, int advanceActionId, int[] resultPages, Integer movieId) {

		boolean isCollect() {
			return kind == Kind.COLLECT;
		}
	}

	/**
	 * 一个任务的混合链页。 / The mixed chain pages of one quest.
	 *
	 * @param entryPage    接取入口页（select_none）
	 * @param stageLadders 每阶段的页梯与动作（外层下标 = 阶段-1）
	 */
	record Pages(int entryPage, List<Stage> stageLadders) {
	}

	private static final RetailClientTalkCollectChainPages EMPTY =
		new RetailClientTalkCollectChainPages(Map.of());
	private static volatile RetailClientTalkCollectChainPages defaultInstance;

	private final Map<Integer, Pages> entries;

	private RetailClientTalkCollectChainPages(Map<Integer, Pages> entries) {
		this.entries = Map.copyOf(entries);
	}

	/** 缺省规范混合链页梯登记（退役后生产通道）。 / Default canonical talk+collect chain pages registry. */
	public static RetailClientTalkCollectChainPages defaultTalkCollectChainPages() {
		RetailClientTalkCollectChainPages instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientTalkCollectChainPages.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientTalkCollectChainPages empty() {
		return EMPTY;
	}

	private static RetailClientTalkCollectChainPages decodeDefaultInstance() {
		InputStream in = RetailClientTalkCollectChainPages.class.getResourceAsStream("/quest/quest_client_talk_collect_chain_pages.tsv");
		if (in == null) {
			in = RetailClientTalkCollectChainPages.class.getResourceAsStream("/aion/data/static_data/quest_retail/quest_client_talk_collect_chain_pages.tsv");
		}
		if (in == null) {
			return EMPTY;
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return EMPTY;
		}
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientTalkCollectChainPages load(InputStream input) throws IOException {
		Map<Integer, Pages> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				parseLine(line, entries);
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client talk+collect chain registry", e);
		}
		return new RetailClientTalkCollectChainPages(entries);
	}

	private static void parseLine(String line, Map<Integer, Pages> entries) {
		String[] parts = line.split("	");
		if (parts.length < 3) {
			return;
		}
		List<Stage> stageLadders = new ArrayList<>(parts.length - 2);
		for (int index = 2; index < parts.length; index++) {
			String[] fields = parts[index].split(":");
			Kind kind = Kind.valueOf(fields[0].trim().toUpperCase(java.util.Locale.ROOT));
			List<Integer> ladder = new ArrayList<>();
			for (String pageId : fields[1].split(">")) {
				ladder.add(Integer.parseInt(pageId.trim()));
			}
			int advance = Integer.parseInt(fields[2].trim());
			int[] resultPages = null;
			Integer movieId = null;
			if (kind == Kind.COLLECT) {
				resultPages = new int[] {Integer.parseInt(fields[3].trim()),
					Integer.parseInt(fields[4].trim())};
			} else if (fields.length >= 4 && !fields[3].isBlank()) {
				movieId = Integer.parseInt(fields[3].trim());
			}
			stageLadders.add(new Stage(kind, List.copyOf(ladder), advance, resultPages, movieId));
		}
		entries.put(Integer.parseInt(parts[0]), new Pages(Integer.parseInt(parts[1]),
			List.copyOf(stageLadders)));
	}

	/** 该任务的混合链页（未登记为空）。 / The mixed chain pages, empty when unregistered. */
	public Optional<Pages> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}
}
