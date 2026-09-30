package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 客户端「交付型」对话页登记（原 {@code quest_client_handin_pages.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 有些任务的客户端任务书流程是「接取窗 → 任务书 → 交付检查 → 成功页 → 领奖窗」而不是
 * 家族规范形的 select1/ask_quest_accept/select2 词汇表——表现为五个角色页齐备
 * （{@code select_none} / {@code select1} / {@code check_user_item_ok} / {@code check_user_item_fail} /
 * {@code select_success}）。合成器按登记表给这些任务发对应的按钮页，客户端契约门禁
 * （{@code QuestClientContractGateTest}）要求每个客户端按钮都有路由，页号只能来自客户端。
 * <p>
 * Read-only view of the client hand-in page registry: quests whose client quest-letter flow is
 * the select_none/select1/check-user-item/success vocabulary rather than the family canonical one.
 */
public final class RetailClientHandinPages {

	private static final RetailClientHandinPages EMPTY = new RetailClientHandinPages(Map.of());
	private static volatile RetailClientHandinPages defaultInstance;

	private final Map<Integer, Pages> entries;

	private RetailClientHandinPages(Map<Integer, Pages> entries) {
		this.entries = entries != null ? Map.copyOf(entries) : Map.of();
	}

	/** 缺省规范交付型对话页登记（退役后生产通道）。 / Default canonical hand-in pages registry. */
	public static RetailClientHandinPages defaultHandinPages() {
		RetailClientHandinPages instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientHandinPages.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	private static RetailClientHandinPages decodeDefaultInstance() {
		InputStream in = RetailClientHandinPages.class.getResourceAsStream("/quest/quest_client_handin_pages.tsv");
		if (in == null) {
			in = RetailClientHandinPages.class.getResourceAsStream("/aion/data/static_data/quest/retail/quest_client_handin_pages.tsv");
		}
		RetailClientHandinPages pages = EMPTY;
		if (in != null) {
			try (InputStream input = in) {
				pages = load(input);
			} catch (IOException e) {
				pages = EMPTY;
			}
		}
		return pages;
	}

	/**
	 * 一个任务的五个角色页 id 与 ok 页本地关闭标记。
	 * The five role pages of one quest plus the ok-page local-close flag.
	 *
	 * @param entryPage    {@code select_none}（接取窗）/ the select_none page
	 * @param letterPage   {@code select1}（任务书正文，交付检查按钮所在页）/ the select1 letter page
	 * @param okPage       {@code check_user_item_ok}（交付成功页）/ the check success page
	 * @param failPage     {@code check_user_item_fail}（交付失败页）/ the check failure page
	 * @param successPage  {@code select_success}（成功收尾页）/ the success page
	 * @param okLocalClose ok 页可见按钮只有本地关闭（FINISH_DIALOG）→ 交付成功分支直接开领奖窗，
	 *                     显示 ok 页会形成死端 / the ok page only offers the local close, so the
	 *                     hand-over branch must open the reward window instead of dead-ending on it
	 */
	public record Pages(int entryPage, int letterPage, int okPage, int failPage, int successPage,
			boolean okLocalClose) {
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientHandinPages empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientHandinPages load(InputStream input) throws IOException {
		Map<Integer, Pages> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 6) {
					continue;
				}
				boolean okLocalClose = parts.length > 6 && Boolean.parseBoolean(parts[6].trim());
				entries.put(Integer.parseInt(parts[0]), new Pages(Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
					Integer.parseInt(parts[5]), okLocalClose));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client hand-in page registry", e);
		}
		return new RetailClientHandinPages(Map.copyOf(entries));
	}

	/** 该任务的交付型页面组（非交付型流程为空）。 / The hand-in pages of one quest, empty for other flows. */
	public Optional<Pages> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}
}
