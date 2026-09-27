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
 * 客户端「链式信件」登记（{@code quest_client_talk_chain_pages.tsv}，只读视图）。
 * <p>
 * 签名 = select_none（单页接取窗）+ select1..selectK 阶梯页（第 i 步对话页 = select{i}，
 * 按钮 = 中间步 SETPRO{i} / 末步 SET_SUCCEED）。DataDriven Talk 链行按本表接入口页与逐阶段页合成；
 * 登记缺失或阶段页不足 = 客户端词汇不在标准链形状内 → 按稳定码拒绝、保留 XML。
 * <p>
 * Read-only view of the client chain-letter registry for DataDriven talk-chain rows.
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

	/**
	 * 一个任务的链式信件页。 / The chain letter pages of one quest.
	 *
	 * @param stageLadders 每阶段的梯与推进动作（外层下标 = 阶段-1）
	 */
	record Pages(int entryPage, List<Stage> stageLadders) {
	}

	private static final RetailClientTalkChainPages EMPTY = new RetailClientTalkChainPages(Map.of());

	private final Map<Integer, Pages> entries;

	private RetailClientTalkChainPages(Map<Integer, Pages> entries) {
		this.entries = Map.copyOf(entries);
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
		return new RetailClientTalkChainPages(entries);
	}

	/** 该任务的链式信件页梯（未登记为空）。 / The chain letter page ladders, empty when unregistered. */
	public Optional<Pages> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}
}
