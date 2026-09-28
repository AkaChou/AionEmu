package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
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

	private static final Pages STANDARD_PAGES = new Pages(4762, 1011, 10000, 10001, 10002, false);
	private static final Pages LOCAL_CLOSE_PAGES = new Pages(4762, 1011, 10000, 10001, 10002, true);

	private static final Set<Integer> LOCAL_CLOSE_QUESTS = Set.of(
		3124, 4121, 4124, 9691, 9692, 13056, 13968, 13978, 14253, 14273, 15215, 15216, 15217, 15353,
		15354, 15689, 15690, 15691, 17512, 18397, 18742, 18975, 18976, 23056, 23968, 23978, 24253,
		25215, 25216, 25217, 25353, 25354, 25689, 25690, 25691, 26838, 27512, 28397, 28742, 28975,
		28976, 41504, 41516, 80723, 80795, 80849, 80850, 80851, 80852
	);

	private static final Set<Integer> STANDARD_QUESTS = Set.of(
		9716, 9717, 15011, 15012, 15021, 15022, 15043, 15044, 15052, 15053, 15066, 15071, 15072,
		15102, 15103, 15230, 15231, 15232, 15307, 15323, 15335, 15363, 15364, 15365, 15366, 15367,
		15368, 15403, 15404, 15405, 15467, 15468, 15478, 15479, 15481, 15502, 15505, 15508, 15511,
		15517, 15523, 15526, 15532, 15535, 15538, 15540, 15541, 15665, 15666, 16977, 16994, 16995,
		16996, 18745, 18953, 19672, 19677, 19684, 19685, 19686, 19687, 19688, 19689, 19694, 25001,
		25020, 25033, 25091, 25307, 25323, 25335, 25363, 25364, 25365, 25366, 25367, 25368, 25403,
		25404, 25405, 25467, 25468, 25478, 25479, 25481, 25502, 25505, 25508, 25511, 25517, 25523,
		25540, 25541, 25665, 25666, 26977, 26994, 26995, 26996, 28745, 28953, 29672, 29677, 29684,
		29685, 29686, 29687, 29688, 29689, 29694, 38508, 38509, 48508, 48509, 50019, 50020, 50052,
		50053, 50054, 50055, 50056, 50057, 50058, 50059, 50060, 50061, 50062, 50063, 50064, 50065,
		50066, 50067, 50071, 50088, 50089, 50090, 50094, 50095, 50096, 50100, 50101, 50107, 50108,
		50109, 51019, 51020, 51059, 51060, 51061, 51062, 51063, 51064, 51071, 51100, 51101, 80300,
		80301, 80306, 80307, 80724, 80725, 80726, 80727, 80728, 80729, 80730, 80735, 80736, 80742,
		80743, 80771, 80773, 80774, 80775, 80776, 80777, 80778, 80796, 80797, 80798, 80834, 80835,
		80836, 80837, 80838, 80839, 80840, 80841, 80854, 80855, 80856, 80857, 80858, 80859, 80875,
		80877, 80878, 80881, 80883, 80887, 80888, 80889, 80899, 80900, 80901, 80902, 80903, 80904,
		80905, 80906, 80907, 80908, 80909, 80910, 80911, 80912, 80913, 80914, 80915, 80916, 80917,
		80918, 80919, 80923, 80924, 80925, 80926, 80942, 80943, 80944, 80947, 80948, 80949, 80950,
		80951, 80953, 80957, 80959, 80962, 80979, 80980, 80981, 80982, 80983, 80984, 80985, 80986,
		80987, 80988, 80993, 80996
	);

	/**
	 * 交付型超出模板例外任务集（固化 48 例，稳定拒绝码 RETAIL_HANDIN_VOCABULARY_UNSUPPORTED）。
	 * 2026-09-28 退役 417 KB 的 quest_client_handin_exceptions.tsv 后转为内存静态规范集合。
	 */
	private static final Set<Integer> DEFAULT_EXCLUDED = Set.of(
		1870, 2870, 15002, 15010, 15070, 15514, 16838, 18977, 18978, 19010, 19016, 19022, 19028, 19034,
		25012, 25013, 25062, 25073, 25080, 25081, 25085, 25092, 25094, 25526, 25532, 25535, 25538, 28977,
		28978, 29010, 29016, 29022, 29028, 29034, 80745, 80748, 80785, 80786, 80870, 80871, 80872, 80874,
		80886, 80945, 80946, 80975, 80976, 80977
	);

	private static final RetailClientHandinPages DEFAULT = new RetailClientHandinPages(null, DEFAULT_EXCLUDED);
	private static final RetailClientHandinPages EMPTY = new RetailClientHandinPages(Map.of(), Set.of());

	private final Map<Integer, Pages> entries;
	/** 交付型「像但不标准」的任务（页面集合超出模板）→ 合成器按稳定码拒绝。 / Hand-in-like exceptions. */
	private final Set<Integer> excluded;

	private RetailClientHandinPages(Map<Integer, Pages> entries, Set<Integer> excluded) {
		this.entries = entries != null ? Map.copyOf(entries) : null;
		this.excluded = excluded;
	}

	/** 缺省规范交付型对话页登记（退役后生产通道）。 / Default canonical hand-in pages registry. */
	public static RetailClientHandinPages defaultHandinPages() {
		return DEFAULT;
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
		return new RetailClientHandinPages(Map.copyOf(entries), DEFAULT_EXCLUDED);
	}

	/** 附加例外登记：页面集合超出交付型模板的任务（合成器拒绝、保留 XML）。 / Attaches the exception set. */
	public RetailClientHandinPages withExceptions(InputStream exceptions) throws IOException {
		Set<Integer> ids = new HashSet<>(DEFAULT_EXCLUDED);
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
		if (entries != null) {
			return Optional.ofNullable(entries.get(questId));
		}
		if (LOCAL_CLOSE_QUESTS.contains(questId)) {
			return Optional.of(LOCAL_CLOSE_PAGES);
		}
		return STANDARD_QUESTS.contains(questId) ? Optional.of(STANDARD_PAGES) : Optional.empty();
	}

	public int size() {
		return entries != null ? entries.size() : (LOCAL_CLOSE_QUESTS.size() + STANDARD_QUESTS.size());
	}
}
