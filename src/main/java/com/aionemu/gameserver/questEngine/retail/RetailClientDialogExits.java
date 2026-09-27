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
 * 客户端对话出口登记表（{@code quest_client_dialog_exits.tsv}）只读视图。
 * <p>
 * 真端模板表（{@code Quest_SimpleTalk.xml} 等）只声明接取/报告 NPC 与物品、过场轴，**没有对话页链列**；
 * 而客户端 5.8 的部分任务页里，{@code select1} 带 {@code HACTION_SELECT1_1} 续页按钮（剧情续页），
 * 服务端必须给出对应路由，否则按钮无路由（客户端契约门禁 BUTTON_WITHOUT_ROUTE）。
 * 该登记表由 {@code build_quest_client_dialog_exits.py} 从仓库内客户端映射
 * （{@code docs/quest/client-dialog-mapping/quest-dialog-pages.csv}，active/exact）生成，是客户端契约数据，
 * 不是 quest-definition XML 的替代依赖。
 * <p>
 * Read-only view of the client dialog exit registry (client contract data derived from the in-repo
 * client dialog mapping; the retail template tables carry no dialog-page columns).
 */
public final class RetailClientDialogExits {

	/** {@code select_none} 页存在 {@code select_none_1} 续页（接取/拒绝按钮落在续页上）。 /
	 * The select_none page continues to select_none_1, which carries the accept/refuse buttons. */
	public static final String SELECT_NONE_1 = "SELECT_NONE_1";
	/** 接取 NPC 的 {@code select1} 页存在 {@code select1_1} 续页。 / The select1 page continues to select1_1. */
	public static final String SELECT1_1 = "SELECT1_1";
	/** {@code select1_1} 页还有 {@code select1_1_1} 续页。 / The select1_1 page continues to select1_1_1. */
	public static final String SELECT1_1_1 = "SELECT1_1_1";
	/** {@code SELECT2} 页的客户端按钮发送动作 1353。 / The client SELECT2 button sends action 1353. */
	public static final String SELECT2_CONTINUE = "SELECT2_CONTINUE";
	/** 交付检查有 {@code select6} 失败页（缺物品时下发；否则关窗）。 / The turn-in has a select6 failure page. */
	public static final String SELECT6 = "SELECT6";
	/** {@code SELECT5} 的客户端按钮发送动作 39。 / The client SELECT5 button sends action 39. */
	public static final String SELECT5_CHECK = "SELECT5_CHECK";
	/** {@code SELECT5} 的客户端按钮发送动作 20002。 / The client SELECT5 button sends action 20002. */
	public static final String SELECT5_CHECK_SIMPLE = "SELECT5_CHECK_SIMPLE";

	private final Map<Integer, Set<String>> exits;

	private RetailClientDialogExits(Map<Integer, Set<String>> exits) {
		this.exits = Map.copyOf(exits);
	}

	/** 空登记表（用于不涉及对话续页的场景）。 / An empty registry. */
	public static RetailClientDialogExits empty() {
		return new RetailClientDialogExits(Map.of());
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
