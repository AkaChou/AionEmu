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
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;

/**
 * 客户端「链式信件」登记（只读内存视图）。
 * <p>
 * 签名 = select_none（单页接取窗）+ select1..selectK 阶梯页（第 i 步对话页 = select{i}，
 * 按钮 = 中间步 SETPRO{i} / 末步 SET_SUCCEED）。DataDriven Talk 链行按本表接入口页与逐阶段页合成；
 * 登记缺失或阶段页不足 = 客户端词汇不在标准链形状内 → 按稳定码拒绝、保留 XML。
 * 2026-09-28 退役 {@code quest_client_talk_chain_pages.tsv} 后转为内存静态规范视图。
 * <p>
 * In-memory view of the client chain-letter registry for DataDriven talk-chain rows.
 */
public final class RetailClientTalkChainPages {

	/**
	 * 一个阶段的页面梯与推进动作。 / One stage's page ladder and advance action.
	 *
	 * @param ladder       子页梯（select{i} → select{i}_1 → ...；导航动作 id = 下一页 id）
	 * @param advanceAction 梯尾页的推进按钮（SETPRO{i} / SET_SUCCEED / SELECT_QUEST_REWARD）
	 */
	record Stage(int advanceActionId, List<Integer> ladder) {
	}

	private static final List<Integer> STEP_PAGE_HEADS = List.of(
		1011, 1352, 1693, 2034, 2375, 2716, 3057, 3398, 3739, 4080, 6500, 6841, 7182, 7523);

	/**
	 * 一个任务的链式信件页。 / The chain letter pages of one quest.
	 *
	 * @param stageLadders 每阶段的梯与推进动作（外层下标 = 阶段-1）
	 */
	record Pages(int entryPage, List<Stage> stageLadders) {
	}

	private static final RetailClientTalkChainPages DEFAULT = new RetailClientTalkChainPages(buildDefaultEntries(), true);
	private static final RetailClientTalkChainPages EMPTY = new RetailClientTalkChainPages(Map.of(), false);

	private final Map<Integer, Pages> entries;
	private final boolean allowContractFallback;

	private RetailClientTalkChainPages(Map<Integer, Pages> entries, boolean allowContractFallback) {
		this.entries = Map.copyOf(entries);
		this.allowContractFallback = allowContractFallback;
	}

	/** 缺省规范链式信件页梯登记（退役后生产通道）。 / Default canonical talk chain pages registry. */
	public static RetailClientTalkChainPages defaultTalkChainPages() {
		return DEFAULT;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientTalkChainPages empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientTalkChainPages load(InputStream input) throws IOException {
		Map<Integer, Pages> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 3) {
					continue;
				}
				List<Stage> stageLadders = new ArrayList<>(parts.length - 2);
				for (int index = 2; index < parts.length; index++) {
					String[] fields = parts[index].split(":");
					List<Integer> ladder = new ArrayList<>();
					for (String pageId : fields[0].split(">")) {
						ladder.add(Integer.parseInt(pageId.trim()));
					}
					stageLadders.add(new Stage(Integer.parseInt(fields[1].trim()), List.copyOf(ladder)));
				}
				entries.put(Integer.parseInt(parts[0]), new Pages(Integer.parseInt(parts[1]),
					List.copyOf(stageLadders)));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client talk chain page registry", e);
		}
		return new RetailClientTalkChainPages(entries, false);
	}

	/** 该任务的链式信件页梯（未登记为空）。 / The chain letter page ladders, empty when unregistered. */
	public Optional<Pages> find(int questId) {
		return find(questId, false);
	}

	public Optional<Pages> find(int questId, boolean allowDerive) {
		Pages existing = entries.get(questId);
		if (existing != null) {
			return Optional.of(existing);
		}
		if (!allowContractFallback || !allowDerive) {
			return Optional.empty();
		}
		return deriveFromContract(questId);
	}

	private static Optional<Pages> deriveFromContract(int questId) {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		Map<Integer, String> pages = contract.pagesForQuest(questId);
		if (pages.isEmpty()) {
			return Optional.empty();
		}
		List<Stage> stages = new ArrayList<>();
		for (int step = 0; step < STEP_PAGE_HEADS.size(); step++) {
			int head = STEP_PAGE_HEADS.get(step);
			String headName = pages.get(head);
			if (headName == null) {
				break;
			}
			List<Integer> ladder = new ArrayList<>();
			ladder.add(head);
			String prefix = headName + "_";
			List<Integer> subpages = new ArrayList<>();
			for (Map.Entry<Integer, String> entry : pages.entrySet()) {
				if (entry.getValue().startsWith(prefix)) {
					subpages.add(entry.getKey());
				}
			}
			subpages.sort(Integer::compareTo);
			ladder.addAll(subpages);
			int advanceActionId = 10000 + step;
			stages.add(new Stage(advanceActionId, List.copyOf(ladder)));
		}
		if (stages.isEmpty()) {
			return Optional.empty();
		}
		Stage last = stages.get(stages.size() - 1);
		stages.set(stages.size() - 1, new Stage(10255, last.ladder()));
		return Optional.of(new Pages(4762, List.copyOf(stages)));
	}

	public int size() {
		return entries.size();
	}

	private static Map<Integer, Pages> buildDefaultEntries() {
		// 存量 Talk+Hunt 混合链过渡页梯（待战役 2 全面切入契约派生后退役）
		Map<Integer, Pages> m = new HashMap<>();
		Pages p0 = new Pages(4762, List.of(new Stage(10000, List.of(1011))));
		m.put(13945, p0);
		m.put(15101, p0);
		m.put(16820, p0);
		m.put(16835, p0);
		m.put(18990, p0);
		m.put(18992, p0);
		m.put(18994, p0);
		m.put(23945, p0);
		m.put(26820, p0);
		m.put(26835, p0);
		m.put(28990, p0);
		m.put(28992, p0);
		m.put(28994, p0);
		Pages p4 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352))));
		m.put(18996, p4);
		m.put(28996, p4);
		Pages p5 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012, 1013)), new Stage(10001, List.of(1352, 1353, 1354))));
		m.put(13950, p5);
		m.put(23950, p5);
		return Map.copyOf(m);
	}
}
