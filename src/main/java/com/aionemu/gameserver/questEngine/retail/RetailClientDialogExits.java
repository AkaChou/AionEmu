package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端对话出口登记表（只读内存视图）。
 * <p>
 * 历史背景：客户端 5.8 的部分任务页里，{@code select1}、{@code select2}、{@code select5}、{@code select6}
 * 带微观翻页动作（如 {@code SELECT2_CONTINUE} 动作 1353、{@code SELECT5_CHECK} 等）。
 * 随着“规范形生命周期”（Canonical Lifecycle）的推进，微观页梯路由逐步退役。
 * 该表在 2026-09-28 正式整表物理退役，转为纯内存静态规范视图。
 * <p>
 * Read-only in-memory view of the client dialog exit registry (retired TSV, in-memory canonical view).
 */
public final class RetailClientDialogExits {

	public static final String SELECT_NONE_1 = "SELECT_NONE_1";
	public static final String SELECT1_1 = "SELECT1_1";
	public static final String SELECT1_1_1 = "SELECT1_1_1";
	public static final String SELECT2_CONTINUE = "SELECT2_CONTINUE";
	public static final String SELECT6 = "SELECT6";
	public static final String SELECT5_CHECK = "SELECT5_CHECK";
	public static final String SELECT5_CHECK_SIMPLE = "SELECT5_CHECK_SIMPLE";

	private static final Set<Integer> SELECT2_CONTINUE_QUESTS = Set.of(
		1115, 1118, 1131, 1152, 1156, 1158, 1163, 1183, 1192, 1218, 1220, 1314, 1323, 1324, 1363, 1394,
		1422, 1452, 1463, 1469, 1471, 1479, 1483, 1484, 1527, 1528, 1537, 1540, 1553, 1560, 1574, 1578,
		1605, 1609, 1620, 1628, 1648, 1691, 1721, 1724, 1725, 1851, 1909, 1918, 1928, 1932, 1935, 1937,
		1938, 1963, 1964, 2125, 2135, 2207, 2209, 2222, 2231, 2247, 2266, 2271, 2278, 2279, 2383, 2414,
		2421, 2428, 2433, 2458, 2480, 2482, 2486, 2488, 2501, 2505, 2512, 2514, 2515, 2523, 2538, 2539,
		2553, 2569, 2583, 2611, 2630, 2641, 2646, 2651, 2653, 2663, 2692, 2693, 2721, 2724, 2725, 2767,
		2912, 2913, 2914, 2917, 2921, 2928, 2953, 2954, 2957, 2958, 2963, 2964, 3001, 3006, 3008, 3020,
		3023, 3035, 3037, 3041, 3076, 3081, 3083, 3085, 3091, 3092, 3093, 3100, 3102, 3116, 3201, 3208,
		3209, 3218, 3319, 3340, 3547, 3913, 3961, 3962, 3963, 3964, 3965, 3966, 3967, 3968, 3969, 3970,
		3972, 3973, 4001, 4015, 4020, 4036, 4052, 4101, 4115, 4201, 4208, 4209, 4218, 4501, 4905, 4906,
		4966, 4967, 4968, 4969, 4970, 4971, 4972, 4973, 4974, 4976, 9550, 9553, 9558, 9559, 11000, 11001,
		11005, 11008, 11009, 11010, 11026, 11068, 11069, 11070, 11072, 11077, 11103, 11105, 11106, 11107,
		11109, 11117, 11139, 11228, 11294, 11455, 11458, 11460, 13700, 13701, 13800, 13900, 14121, 14122,
		14201, 16990, 18035, 18210, 18225, 18600, 18806, 18807, 18830, 18940, 19004, 21004, 21033, 21036,
		21065, 21066, 21068, 21070, 21071, 21073, 21081, 21106, 21110, 21111, 21135, 21136, 21138, 21217,
		21244, 21296, 21455, 21458, 21460, 23700, 23701, 23800, 23900, 24120, 24123, 24150, 24152, 24202,
		24242, 26990, 28035, 28210, 28225, 28600, 28806, 28807, 28830, 28940, 29004, 30042, 30054, 30055,
		30061, 30142, 30154, 30161, 30202, 30302, 30711, 30761, 35010, 35011, 35017, 35018, 35024, 35025,
		35026, 39003, 45010, 45011, 45017, 45018, 45024, 45025, 45026, 49000, 49003, 80020, 80021, 80479, 80483
	);

	private static final Set<Integer> SELECT5_CHECK_QUESTS = Set.of(
		1152, 1932, 3092, 3961, 3962, 3963, 3964, 4966, 4967, 4968, 4969, 80320
	);

	private static final Set<Integer> SELECT5_CHECK_SIMPLE_QUESTS = Set.of(
		3340, 3547, 11304, 14121, 14201, 19064, 24121, 24152, 24202, 24242, 29064
	);

	private static final Set<Integer> SELECT6_QUESTS = Set.of(
		1152, 1932, 2428, 3092, 3961, 3962, 3963, 3964, 4966, 4967, 4968, 4969, 80320
	);

	private static final Set<Integer> SELECT_NONE_1_QUESTS = Set.of(
		1888, 2888, 15478, 15479, 15606, 16800, 25050, 25073, 25094, 25478, 25479, 25606, 26800, 80989
	);

	private static final RetailClientDialogExits DEFAULT = new RetailClientDialogExits(null);
	private static final RetailClientDialogExits EMPTY = new RetailClientDialogExits(Map.of());

	private final Map<Integer, Set<String>> exits;

	private RetailClientDialogExits(Map<Integer, Set<String>> exits) {
		this.exits = exits != null ? Map.copyOf(exits) : null;
	}

	/** 缺省规范对话出口登记（退役后生产通道）。 / Default canonical dialog exits registry. */
	public static RetailClientDialogExits defaultExits() {
		return DEFAULT;
	}

	/** 空登记表（用于不涉及对话续页的场景）。 / An empty registry. */
	public static RetailClientDialogExits empty() {
		return new RetailClientDialogExits(Map.of());
	}

	/** 该任务是否需要某个对话出口。 / Whether the quest needs the given dialog exit. */
	public boolean requires(int questId, String exit) {
		if (exits != null) {
			return exits.getOrDefault(questId, Set.of()).contains(exit);
		}
		return switch (exit) {
			case SELECT2_CONTINUE -> SELECT2_CONTINUE_QUESTS.contains(questId);
			case SELECT5_CHECK -> SELECT5_CHECK_QUESTS.contains(questId);
			case SELECT5_CHECK_SIMPLE -> SELECT5_CHECK_SIMPLE_QUESTS.contains(questId);
			case SELECT6 -> SELECT6_QUESTS.contains(questId);
			case SELECT_NONE_1 -> SELECT_NONE_1_QUESTS.contains(questId);
			default -> false;
		};
	}

	public int size() {
		if (exits != null) {
			return exits.size();
		}
		Set<Integer> all = new HashSet<>(SELECT2_CONTINUE_QUESTS);
		all.addAll(SELECT5_CHECK_QUESTS);
		all.addAll(SELECT5_CHECK_SIMPLE_QUESTS);
		all.addAll(SELECT6_QUESTS);
		all.addAll(SELECT_NONE_1_QUESTS);
		return all.size();
	}

	/**
	 * 解析登记表：{@code quest_id \t exits（空格分隔）}，{@code #} 开头为注释。
	 * Parses the registry: {@code quest_id \t space-separated exits}, {@code #} lines are comments.
	 */
	public static RetailClientDialogExits load(InputStream input) throws IOException {
		Map<Integer, Set<String>> exits = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length < 2 || !parts[0].trim().chars().allMatch(Character::isDigit)) {
					continue;
				}
				Set<String> tokens = new HashSet<>();
				for (String token : parts[1].trim().split("\\s+")) {
					if (!token.isBlank()) {
						tokens.add(token);
					}
				}
				exits.put(Integer.parseInt(parts[0].trim()), Set.copyOf(tokens));
			}
		}
		return new RetailClientDialogExits(exits);
	}
}
