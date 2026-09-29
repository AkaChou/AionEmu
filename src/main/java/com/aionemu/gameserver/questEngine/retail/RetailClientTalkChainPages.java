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
 * 客户端「链式信件」登记（原 {@code quest_client_talk_chain_pages.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 签名 = select_none（单页接取窗）+ select1..selectK 阶梯页（第 i 步对话页 = select{i}，
 * 按钮 = 中间步 SETPRO{i} / 末步 SET_SUCCEED）。DataDriven Talk 链行按本表接入口页与逐阶段页合成；
 * 登记缺失或阶段页不足时，支持从客户端对话契约动态派生规范页梯。
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

	private static final RetailClientTalkChainPages EMPTY = new RetailClientTalkChainPages(Map.of(), false);
	private static volatile RetailClientTalkChainPages defaultInstance;

	private final Map<Integer, Pages> entries;
	private final boolean allowContractFallback;

	private RetailClientTalkChainPages(Map<Integer, Pages> entries, boolean allowContractFallback) {
		this.entries = Map.copyOf(entries);
		this.allowContractFallback = allowContractFallback;
	}

	/** 缺省规范链式信件页梯登记（退役后生产通道）。 / Default canonical talk chain pages registry. */
	public static RetailClientTalkChainPages defaultTalkChainPages() {
		RetailClientTalkChainPages instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientTalkChainPages.class) {
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
	public static RetailClientTalkChainPages empty() {
		return EMPTY;
	}

	private static RetailClientTalkChainPages decodeDefaultInstance() {
		InputStream in = RetailClientTalkChainPages.class.getResourceAsStream("/quest/quest_client_talk_chain_pages.tsv");
		if (in == null) {
			in = RetailClientTalkChainPages.class.getResourceAsStream("/aion/data/static_data/quest_retail/quest_client_talk_chain_pages.tsv");
		}
		Map<Integer, Pages> entries = Map.of();
		if (in != null) {
			try (InputStream input = in) {
				entries = load(input).entries;
			} catch (IOException e) {
				entries = Map.of();
			}
		}
		return new RetailClientTalkChainPages(entries, true);
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
				continue;
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
}
