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

	/**
	 * 一个任务的链式信件页。 / The chain letter pages of one quest.
	 *
	 * @param stageLadders 每阶段的梯与推进动作（外层下标 = 阶段-1）
	 */
	record Pages(int entryPage, List<Stage> stageLadders) {
	}

	private static final RetailClientTalkChainPages DEFAULT = new RetailClientTalkChainPages(buildDefaultEntries());
	private static final RetailClientTalkChainPages EMPTY = new RetailClientTalkChainPages(Map.of());

	private final Map<Integer, Pages> entries;

	private RetailClientTalkChainPages(Map<Integer, Pages> entries) {
		this.entries = Map.copyOf(entries);
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
		return new RetailClientTalkChainPages(entries);
	}

	/** 该任务的链式信件页梯（未登记为空）。 / The chain letter page ladders, empty when unregistered. */
	public Optional<Pages> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	private static Map<Integer, Pages> buildDefaultEntries() {
		Map<Integer, Pages> m = new HashMap<>();
		Pages p0 = new Pages(4762, List.of(new Stage(10000, List.of(1011))));
		m.put(9666, p0);
		m.put(11400, p0);
		m.put(12004, p0);
		m.put(13048, p0);
		m.put(13945, p0);
		m.put(14263, p0);
		m.put(15101, p0);
		m.put(16820, p0);
		m.put(16835, p0);
		m.put(16997, p0);
		m.put(16998, p0);
		m.put(16999, p0);
		m.put(17520, p0);
		m.put(18980, p0);
		m.put(18981, p0);
		m.put(18982, p0);
		m.put(18983, p0);
		m.put(18984, p0);
		m.put(18985, p0);
		m.put(18990, p0);
		m.put(18992, p0);
		m.put(18994, p0);
		m.put(21400, p0);
		m.put(22004, p0);
		m.put(23048, p0);
		m.put(23945, p0);
		m.put(24263, p0);
		m.put(26820, p0);
		m.put(26835, p0);
		m.put(26997, p0);
		m.put(26998, p0);
		m.put(26999, p0);
		m.put(27520, p0);
		m.put(28980, p0);
		m.put(28981, p0);
		m.put(28982, p0);
		m.put(28983, p0);
		m.put(28984, p0);
		m.put(28985, p0);
		m.put(28990, p0);
		m.put(28992, p0);
		m.put(28994, p0);
		m.put(30405, p0);
		m.put(30455, p0);
		m.put(30461, p0);
		m.put(41162, p0);
		m.put(41170, p0);
		m.put(41204, p0);
		m.put(41527, p0);
		m.put(41537, p0);
		m.put(41551, p0);
		Pages p1 = new Pages(4762, List.of(new Stage(10255, List.of(1011))));
		m.put(9699, p1);
		m.put(13817, p1);
		m.put(14262, p1);
		m.put(15401, p1);
		m.put(18315, p1);
		m.put(18970, p1);
		m.put(19602, p1);
		m.put(19608, p1);
		m.put(19671, p1);
		m.put(19678, p1);
		m.put(19679, p1);
		m.put(19683, p1);
		m.put(23817, p1);
		m.put(24262, p1);
		m.put(25401, p1);
		m.put(28315, p1);
		m.put(28970, p1);
		m.put(29602, p1);
		m.put(29608, p1);
		m.put(29671, p1);
		m.put(29678, p1);
		m.put(29679, p1);
		m.put(29683, p1);
		m.put(30503, p1);
		m.put(30553, p1);
		m.put(41254, p1);
		m.put(80990, p1);
		Pages p2 = new Pages(4762, List.of(new Stage(10255, List.of(1011, 1012))));
		m.put(9714, p2);
		m.put(9715, p2);
		m.put(15456, p2);
		m.put(15459, p2);
		m.put(15462, p2);
		m.put(25456, p2);
		m.put(25459, p2);
		m.put(25462, p2);
		Pages p3 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(1009, List.of(1352))));
		m.put(15591, p3);
		m.put(15592, p3);
		m.put(15593, p3);
		m.put(25591, p3);
		m.put(25592, p3);
		m.put(25593, p3);
		Pages p4 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352))));
		m.put(11512, p4);
		m.put(18996, p4);
		m.put(25680, p4);
		m.put(28996, p4);
		Pages p5 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012, 1013)), new Stage(10001, List.of(1352, 1353, 1354))));
		m.put(13950, p5);
		m.put(13954, p5);
		m.put(23950, p5);
		m.put(23954, p5);
		Pages p6 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012))));
		m.put(15321, p6);
		m.put(17505, p6);
		m.put(25321, p6);
		m.put(27505, p6);
		Pages p7 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012, 1097))));
		m.put(1868, p7);
		m.put(2868, p7);
		Pages p8 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352, 1353)), new Stage(10255, List.of(1693, 1694, 1695))));
		m.put(1871, p8);
		m.put(2871, p8);
		Pages p9 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352)), new Stage(10002, List.of(1693)), new Stage(10003, List.of(2034)), new Stage(10255, List.of(2375))));
		m.put(11323, p9);
		m.put(21323, p9);
		Pages p10 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012, 1013))));
		m.put(13403, p10);
		m.put(23403, p10);
		Pages p11 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352, 1353)), new Stage(10001, List.of(1693, 1694)), new Stage(10001, List.of(2034, 2035)), new Stage(10001, List.of(2375, 2376))));
		m.put(14220, p11);
		m.put(24220, p11);
		Pages p12 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10001, List.of(1352)), new Stage(10002, List.of(1693)), new Stage(10255, List.of(2034))));
		m.put(15590, p12);
		m.put(25590, p12);
		Pages p13 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10255, List.of(1352))));
		m.put(19666, p13);
		m.put(29666, p13);
		Pages p14 = new Pages(4762, List.of(new Stage(10000, List.of(1011)), new Stage(10255, List.of(1352, 1353))));
		m.put(19670, p14);
		m.put(29670, p14);
		Pages p15 = new Pages(4762, List.of(new Stage(10000, List.of(1011, 1012)), new Stage(10001, List.of(1352, 1353))));
		m.put(30721, p15);
		m.put(30771, p15);
		Pages p16 = new Pages(4762, List.of(new Stage(1009, List.of(1011))));
		m.put(3044, p16);
		Pages p17 = new Pages(4762, List.of(new Stage(10001, List.of(1011, 1012))));
		m.put(3056, p17);
		return Map.copyOf(m);
	}
}
