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
 * 客户端对话出口登记表（原 {@code quest_client_dialog_exits.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 历史背景：客户端 5.8 的部分任务页里，{@code select1}、{@code select2}、{@code select5}、{@code select6}
 * 带微观翻页动作（如 {@code SELECT2_CONTINUE} 动作 1353、{@code SELECT5_CHECK} 等）。
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

	private static final RetailClientDialogExits EMPTY = new RetailClientDialogExits(Map.of());
	private static volatile RetailClientDialogExits defaultInstance;

	private final Map<Integer, Set<String>> exits;

	private RetailClientDialogExits(Map<Integer, Set<String>> exits) {
		this.exits = exits != null ? Map.copyOf(exits) : Map.of();
	}

	/** 缺省规范对话出口登记（退役后生产通道）。 / Default canonical dialog exits registry. */
	public static RetailClientDialogExits defaultExits() {
		RetailClientDialogExits instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientDialogExits.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空登记表（用于不涉及对话续页的场景）。 / An empty registry. */
	public static RetailClientDialogExits empty() {
		return EMPTY;
	}

	private static RetailClientDialogExits decodeDefaultInstance() {
		InputStream in = RetailClientDialogExits.class.getResourceAsStream("/quest/quest_client_dialog_exits.tsv");
		if (in == null) {
			in = RetailClientDialogExits.class.getResourceAsStream("/aion/data/static_data/quest/retail/quest_client_dialog_exits.tsv");
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

	/** 该任务是否需要某个对话出口。 / Whether the quest needs the given dialog exit. */
	public boolean requires(int questId, String exit) {
		return exits.getOrDefault(questId, Set.of()).contains(exit);
	}

	public int size() {
		return exits.size();
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
