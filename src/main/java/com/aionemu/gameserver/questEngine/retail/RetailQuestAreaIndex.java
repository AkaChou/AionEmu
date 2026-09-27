package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生产区域发放表（{@code definitions/compact/ai/ai-areas.xml} 的 {@code <quest_area>}，只读视图）。
 * <p>
 * 真端把 {@code _area_} 类别的接取名交给世界文件：{@code Map/Worlds/<world>/world*.xml} 的
 * {@code <questscript_area>} 用 {@code <quest>} 绑定任务 id，进区域即由区域引擎
 * {@code RetailAreaEngine} 直接 {@code startQuest}。生产侧同义的载体是该文件的
 * {@code <quest_area ... quests="...">}；
 * 因此"某个 {@code _area_} 任务是否已接线"= 该任务 id 是否出现在 {@code quest_area} 的 quests 里。
 * P0c-4 以真端世界文件重算并补齐了这些绑定（生成器：{@code p0c4_emit_quest_area_snippets.py}）。
 * <p>
 * Read-only view of the production quest-area grant table: a quest id is area-grantable exactly when
 * it is bound by some {@code <quest_area ... quests="...">} entry, mirroring the retail world files'
 * {@code <questscript_area><quest>} bindings that {@code RetailAreaEngine} consumes.
 */
public final class RetailQuestAreaIndex {

	private static final RetailQuestAreaIndex EMPTY = new RetailQuestAreaIndex(Map.of(), Map.of());
	private static final Pattern AREA = Pattern.compile("<quest_area\\b([^>]*)>", Pattern.DOTALL);
	private static final Pattern QUESTS = Pattern.compile("quests=\"([^\"]*)\"");
	private static final Pattern WORLD_ID = Pattern.compile("world_id=\"(\\d+)\"");
	private static final Pattern WORLD_NAME = Pattern.compile("world_name=\"([^\"]*)\"");

	private final Map<Integer, List<Integer>> worldsByQuest;
	private final Map<Integer, List<String>> namesByQuest;

	private RetailQuestAreaIndex(Map<Integer, List<Integer>> worldsByQuest, Map<Integer, List<String>> namesByQuest) {
		this.worldsByQuest = worldsByQuest;
		this.namesByQuest = namesByQuest;
	}

	/** 空表（测试或关闭接线时用）。 / Empty table for tests or a disabled wiring. */
	public static RetailQuestAreaIndex empty() {
		return EMPTY;
	}

	/** 解析 {@code ai-areas.xml}（UTF-8）。 / Parses the compact area definitions. */
	public static RetailQuestAreaIndex load(InputStream input) throws IOException {
		String text;
		try {
			text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw e;
		}
		Map<Integer, Set<Integer>> worlds = new HashMap<>();
		Map<Integer, Set<String>> names = new HashMap<>();
		Matcher areas = AREA.matcher(text);
		while (areas.find()) {
			String attributes = areas.group(1);
			Matcher quests = QUESTS.matcher(attributes);
			if (!quests.find() || quests.group(1).isBlank()) {
				continue;
			}
			Matcher worldId = WORLD_ID.matcher(attributes);
			if (!worldId.find()) {
				continue;
			}
			Matcher worldName = WORLD_NAME.matcher(attributes);
			String name = worldName.find() ? worldName.group(1) : "?";
			int world = Integer.parseInt(worldId.group(1));
			for (String raw : quests.group(1).split(",")) {
				String trimmed = raw.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				int questId = Integer.parseInt(trimmed);
				worlds.computeIfAbsent(questId, ignored -> new TreeSet<>()).add(world);
				names.computeIfAbsent(questId, ignored -> new TreeSet<>()).add(name);
			}
		}
		Map<Integer, List<Integer>> frozenWorlds = new HashMap<>();
		worlds.forEach((questId, values) -> frozenWorlds.put(questId, List.copyOf(values)));
		Map<Integer, List<String>> frozenNames = new HashMap<>();
		names.forEach((questId, values) -> frozenNames.put(questId, List.copyOf(values)));
		return new RetailQuestAreaIndex(Map.copyOf(frozenWorlds), Map.copyOf(frozenNames));
	}

	/** 绑定了该任务的区域数量。 / The number of bound areas for one quest. */
	public int areaCount(int questId) {
		return worldsByQuest.getOrDefault(questId, List.of()).size();
	}

	/** 是否已由区域表接管（≠0 个绑定）。 / Whether the area table owns this quest. */
	public boolean isBound(int questId) {
		return worldsByQuest.containsKey(questId);
	}

	/** 绑定该任务的 world id 集（诊断用）。 / Bound world ids, for diagnostics. */
	public List<Integer> worlds(int questId) {
		return worldsByQuest.getOrDefault(questId, List.of());
	}

	/** 绑定该任务的 world_name 集（诊断用）。 / Bound world names, for diagnostics. */
	public List<String> worldNames(int questId) {
		return namesByQuest.getOrDefault(questId, List.of());
	}
}
