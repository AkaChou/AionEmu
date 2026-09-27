package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 客户端「交付型」对话页登记（{@code quest_client_handin_pages.tsv}，只读视图）。
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

	private static final RetailClientHandinPages EMPTY = new RetailClientHandinPages(Map.of(), Set.of());

	private final Map<Integer, Pages> entries;
	/** 交付型「像但不标准」的任务（页面集合超出模板）→ 合成器按稳定码拒绝。 / Hand-in-like exceptions. */
	private final Set<Integer> excluded;

	private RetailClientHandinPages(Map<Integer, Pages> entries, Set<Integer> excluded) {
		this.entries = entries;
		this.excluded = excluded;
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
		return new RetailClientHandinPages(Map.copyOf(entries), Set.of());
	}

	/** 附加例外登记：页面集合超出交付型模板的任务（合成器拒绝、保留 XML）。 / Attaches the exception set. */
	public RetailClientHandinPages withExceptions(InputStream exceptions) throws IOException {
		Set<Integer> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(exceptions,
				StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				ids.add(Integer.parseInt(line.split("\t", -1)[0]));
			}
		}
		return new RetailClientHandinPages(entries, Set.copyOf(ids));
	}

	/** 该任务的客户端页面集合是否超出交付型模板。 / Whether the client pages exceed the hand-in template. */
	public boolean excluded(int questId) {
		return excluded.contains(questId);
	}

	/** 该任务的交付型页面组（非交付型流程为空）。 / The hand-in pages of one quest, empty for other flows. */
	public Optional<Pages> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}
}
