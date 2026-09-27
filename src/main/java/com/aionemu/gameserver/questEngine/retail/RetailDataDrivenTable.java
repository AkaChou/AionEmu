package com.aionemu.gameserver.questEngine.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 真端 DataDriven 模板表（Map/XML/data_driven_quest.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 行 = 接取方式（{@code category_acquire_} + 参数）+ 报告 NPC + 进度步骤链。步骤链在
 * {@code progress_info/data} 内：{@code category_progress_} + {@code value0_progress_}
 * （"名单 计数"，名单逗号分隔，分号分隔多段）。本类只解析 Hunt 步骤（P5-1 范围），
 * 其余步骤类型（Talk/CollectItem/PVP/EnterArea/ItemPlay/EnterWorld）按批补齐。
 * <p>
 * Read-only view of the retail DataDriven template table; P5-1 parses Hunt progress stages.
 */
public final class RetailDataDrivenTable {

	/** 一个 Hunt 进度段。 / One hunt stage. */
	public record HuntStage(List<String> monsters, int count) {
	}

	/**
	 * 一个 ItemPlay 步骤载荷：道具符号 + 可选数量（DD value0 形如 {@code QUEST_X} / {@code QUEST_X, 3}）。
	 * One ItemPlay step payload: the item symbol plus an optional count (DD value0 shaped
	 * {@code QUEST_X} / {@code QUEST_X, 3}).
	 *
	 * @param itemId 编译期经物品名索引解析的物品 id（0 = 未解析，由链编译器就地回填）
	 *               the item id resolved at compile time through the item-name index
	 *               (0 = unresolved, backfilled in place by the chain compiler)
	 */
	public record ItemPlayTarget(String symbol, int count, int itemId, String outputSymbol,
			int outputCount, int outputItemId) {

		public ItemPlayTarget(String symbol, int count) {
			this(symbol, count, 0, null, 0, 0);
		}

		public ItemPlayTarget(String symbol, int count, String outputSymbol, int outputCount) {
			this(symbol, count, 0, outputSymbol, outputCount, 0);
		}
	}

	/**
	 * TalkFOBJ 载荷：FOBJ npc 模板名 + 是否在步内授予任务 work item（value2 凭证声明，
	 * 15601 形：FOBJ USE_OBJECT 边 give-item ×1）。
	 * TalkFOBJ payload: the fobj npc template name plus whether the step grants this quest's work
	 * item (a value2 credential declaration; the 15601 shape grants ×1 on the fobj USE_OBJECT edge).
	 */
	public record TalkFobjTarget(String npcName, boolean grantsWorkItem) {
	}

	/**
	 * 一个任务行。 / One row.
	 *
	 * @param stepCategories 进度步骤类别序列（小写规范形，table 顺序）——形状判定统一从这里派生，
	 *                       新步骤类型只加类别与载荷，不再叠加布尔标志
	 * @param huntBlocks     每个 hunt 块的分号段数（与 stepCategories 块序对齐；talk/hunt 交错行据此恢复步骤序）
	 * @param huntStages     Hunt 步骤的计数段（category=hunt 的载荷）
	 * @param talkSteps      Talk 步骤的 NPC 名序列（category=talk 的载荷，table 顺序）
	 * @param collectSteps   CollectItem 步骤的交付 NPC 名序列（category=collectitem 的载荷，table 顺序）
	 * @param itemPlaySteps  ItemPlay 步骤的道具载荷序列（category=itemplay 的载荷，table 顺序）
	 * @param talkFobjSteps  TalkFOBJ 步骤的载荷序列（category=talkfobj 的载荷，table 顺序）
	 */
	public record Entry(int questId, String acquireCategory, String acquireParam, String rewardNpc,
			List<String> stepCategories, Map<Integer, Integer> stepCutscenes,
			List<Integer> huntBlocks, List<HuntStage> huntStages,
			List<String> talkSteps, List<String> collectSteps, List<String> enterAreaSteps,
			List<Integer> enterWorldSteps,
			List<ItemPlayTarget> itemPlaySteps, List<TalkFobjTarget> talkFobjSteps,
			List<Integer> pvpCounts, int pvpMinRank, int pvpRankCeiling, int pvpLevelGap) {

		/** 纯 PVP 步骤行（P5-4）。 / Rows whose steps are all PVP counters. */
		public boolean allPvp() {
			return !stepCategories.isEmpty()
				&& stepCategories.stream().allMatch("pvp"::equals);
		}

		/** 无进度行（纯接取→交付，P5-3 wave A）。 / Rows without any progress block. */
		public boolean noProgress() {
			return stepCategories.isEmpty();
		}

		/** 全部步骤均为 Hunt。 / Whether every progress step is a hunt step. */
		public boolean allHunt() {
			return isHomogeneous("hunt");
		}

		/** 全部步骤均为 CollectItem。 / Whether every progress step is a collectitem step. */
		public boolean allCollect() {
			return isHomogeneous("collectitem");
		}

		/** 全部步骤均为 Talk（对话链行；不含无进度行）。 / Whether every progress step is a talk step. */
		public boolean allTalk() {
			return isHomogeneous("talk");
		}

		private boolean isHomogeneous(String category) {
			return !stepCategories.isEmpty()
				&& stepCategories.stream().allMatch(category::equals);
		}
	}

	private final Map<Integer, Entry> entries;

	private RetailDataDrivenTable(Map<Integer, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	/** 按任务 id 取行。 / Looks up a row by quest id. */
	public Optional<Entry> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	/** 全部任务 ID。 / All quest ids. */
	public Set<Integer> questIds() {
		return entries.keySet();
	}

	/**
	 * 解析真端模板表；文件带内部 DTD（实体是纯文本替换），因此允许内部子集、禁止外部访问。
	 * Parses the retail table; it carries an internal DTD subset with text-only entities.
	 */
	public static RetailDataDrivenTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, Entry> entries = new HashMap<>();
			NodeList nodes = document.getElementsByTagName("quest_data_driven");
			for (int index = 0; index < nodes.getLength(); index++) {
				Node node = nodes.item(index);
				if (node instanceof Element element) {
					Entry entry = parseEntry(element);
					if (entry != null) {
						entries.put(entry.questId(), entry);
					}
				}
			}
			return new RetailDataDrivenTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail DataDriven table", e);
		}
	}

	private static Entry parseEntry(Element element) {
		int questId = Integer.parseInt(text(element, "id"));
		String acquire = text(element, "category_acquire_");
		String acquireParam = text(element, "value0_acquire_");
		String reward = text(element, "reward_npc_name");
		List<String> stepCategories = new ArrayList<>();
		Map<Integer, Integer> stepCutscenes = new HashMap<>();
		List<Integer> huntBlocks = new ArrayList<>();
		List<HuntStage> stages = new ArrayList<>();
		List<String> talkSteps = new ArrayList<>();
		List<String> collectSteps = new ArrayList<>();
		List<String> enterAreaSteps = new ArrayList<>();
		List<Integer> enterWorldSteps = new ArrayList<>();
		List<ItemPlayTarget> itemPlaySteps = new ArrayList<>();
		List<TalkFobjTarget> talkFobjSteps = new ArrayList<>();
		List<Integer> pvpCounts = new ArrayList<>();
		int pvpMinRank = 0;
		int pvpRankCeiling = 0;
		int pvpLevelGap = 0;
		NodeList infos = element.getElementsByTagName("data");
		for (int index = 0; index < infos.getLength(); index++) {
			Node node = infos.item(index);
			if (!(node instanceof Element data)) {
				continue;
			}
			String category = text(data, "category_progress_");
			if (category == null) {
				continue;
			}
		String cat = category.trim().toLowerCase(java.util.Locale.ROOT);
		stepCategories.add(cat);
		String effect = text(data, "value4_progress_");
		if (effect != null && effect.matches("(?i)Cutscene [0-9]+")) {
			stepCutscenes.put(stepCategories.size() - 1,
				Integer.parseInt(effect.substring(effect.indexOf(' ') + 1)));
		}
			if (cat.equals("pvp")) {
				// PVP 步骤（P5-4）：value0 = 所需击杀数（数值，无目标名）；分号分段 = 多计数段。
				// value1/value2 = 军衔阈值/上限（KillRanked 最低阈值语义，如紧急军令 1877–1887）→
				// 军衔网格按 KillRanked(value1) 发边；上限非 15 的形状本服未见，保持待裁定。
				// PVP step (P5-4): value0 is the required kill count; semicolon stages = multi-slot.
				// value1/value2 form a rank threshold/ceiling (KillRanked minimum-threshold semantics,
				// e.g. the urgent orders) — ranked grids emit KillRanked(value1); unseen ceilings defer.
				String low = text(data, "value1_progress_");
				String high = text(data, "value2_progress_");
				if (low != null && !low.isBlank() && low.chars().allMatch(Character::isDigit)) {
					pvpMinRank = Integer.parseInt(low.trim());
				}
				if (high != null && !high.isBlank() && high.chars().allMatch(Character::isDigit)) {
					pvpRankCeiling = Integer.parseInt(high.trim());
				}
				// value3 = PvP Target Level Gap（ScriptDLL DataDrivenQuest 进度槽位；缺省 10 由编译器补齐）。
				// value3 is the PvP target level gap (ScriptDLL progress slot); the compiler applies the
				// loader default of 10 when absent.
				String gap = text(data, "value3_progress_");
				if (gap != null && !gap.isBlank() && gap.chars().allMatch(Character::isDigit)) {
					pvpLevelGap = Integer.parseInt(gap.trim());
				}
				String value = text(data, "value0_progress_");
				if (value != null) {
					for (String stage : value.split(";")) {
						stage = stage.trim();
						if (!stage.isEmpty() && stage.chars().allMatch(Character::isDigit)) {
							pvpCounts.add(Integer.parseInt(stage));
						}
					}
				}
				continue;
			}
			if (cat.equals("talk")) {
				// Talk 步骤：value0 = 目标 NPC 名（链内顺序即 table 顺序；P5-3 wave B 链形状消费）。
				// Talk step: value0 names the npc to visit, in table order (chain shape, wave B).
				String talkTarget = text(data, "value0_progress_");
				if (talkTarget != null && !talkTarget.isBlank()) {
					talkSteps.add(talkTarget.trim());
				}
			}
			if (cat.equals("collectitem")) {
				// CollectItem 步骤：value0 = 交付检查所在 NPC 名（混合链切片 2 消费；采集物由
				// 真端元数据交付物承载，此处名字是检查动作的对话对象）。
				// CollectItem step: value0 names the npc carrying the hand-in check (mixed-chain
				// slice 2); the collect goods live in the retail metadata item requirements.
				String collectTarget = text(data, "value0_progress_");
				if (collectTarget != null && !collectTarget.isBlank()) {
					collectSteps.add(collectTarget.trim());
				}
			}
			if (cat.equals("enterarea")) {
				// EnterArea 步骤：value0 = 区域别名（遗留 <enter-zone zone="..."/> 的事件参数；
				// 进入区域推进行，无计数）。
				// EnterArea step: value0 is the area alias (the legacy enter-zone event argument);
				// entering the area advances the row and counts nothing.
				String areaAlias = text(data, "value0_progress_");
				if (areaAlias != null && !areaAlias.isBlank()) {
					enterAreaSteps.add(areaAlias.trim());
				}
				continue;
			}
			if (cat.equals("enterworld")) {
				// EnterWorld 步骤：value0 = 世界 id（遗留 `<enter-world/>` + `<world-is world-id="..."/>`
				// 的事件参数；进入该世界推进行，无计数无对话段）。非数字值（未见样本）记 -1，编译期拒绝。
				// EnterWorld step: value0 is the world id (the legacy enter-world event plus its
				// world-is condition); entering that world advances the row with no counter and no
				// dialog stage. A non-numeric value (no sample seen) is recorded as -1 and rejected
				// by the compiler.
				String worldId = text(data, "value0_progress_");
				if (worldId != null && !worldId.isBlank()) {
					enterWorldSteps.add(parseWorldId(worldId.trim()));
				}
				continue;
			}
			if (cat.equals("itemplay")) {
				// ItemPlay 步骤：value0 = 道具符号（可带 ", 数量"，缺省 1）；使用道具演出推进行。
				// ItemPlay step: value0 is the item symbol (optional ", count", default 1); using
				// the item plays and advances the row.
				String value = text(data, "value0_progress_");
				if (value != null && !value.isBlank()) {
					String symbol = value.trim();
					int count = 1;
					int comma = symbol.indexOf(',');
					if (comma > 0) {
						String tail = symbol.substring(comma + 1).trim();
						if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit)) {
							count = Integer.parseInt(tail);
						}
						symbol = symbol.substring(0, comma).trim();
					}
					String output = text(data, "value1_progress_");
					if (output == null) {
						itemPlaySteps.add(new ItemPlayTarget(symbol, count));
					} else {
						int split = output.lastIndexOf(' ');
						int outputCount = split > 0 && output.substring(split + 1).chars()
							.allMatch(Character::isDigit)
							? Integer.parseInt(output.substring(split + 1)) : 0;
						itemPlaySteps.add(new ItemPlayTarget(symbol, count,
							output.substring(0, Math.max(split, 0)).trim(), outputCount));
					}
				}
				continue;
			}
			if (cat.equals("talkfobj")) {
				// TalkFOBJ 步骤（15601 形）：value0 = FOBJ npc 模板名（可带 ", count"，该数非消耗
				// 计数，忽略）；value2 = 步内凭证声明 → 本任务 work item（遗留形 FOBJ USE_OBJECT 边
				// give-item ×1）。
				// TalkFOBJ step (the 15601 shape): value0 names the fobj npc template (an optional
				// ", count" rides along but is not a consumption count); value2 declares the in-step
				// credential → this quest's work item (the legacy shape grants ×1 on the fobj
				// USE_OBJECT edge).
				String target = text(data, "value0_progress_");
				if (target != null && !target.isBlank()) {
					String name = target.trim();
					int comma = name.indexOf(',');
					if (comma > 0) {
						name = name.substring(0, comma).trim();
					}
					String credential = text(data, "value2_progress_");
					talkFobjSteps.add(new TalkFobjTarget(name, credential != null && !credential.isBlank()));
				}
				continue;
			}
			if (!cat.equals("hunt")) {
				if (!cat.equals("collectitem")) {
					continue;
				}
			}
			String value = text(data, "value0_progress_");
			if (value == null) {
				continue;
			}
			int blockStages = 0;
			for (String stage : value.split(";")) {
				stage = stage.trim();
				if (stage.isEmpty()) {
					continue;
				}
				int split = stage.lastIndexOf(' ');
				if (split <= 0) {
					continue;
				}
				String countPart = stage.substring(split + 1).trim();
				if (!countPart.chars().allMatch(Character::isDigit)) {
					continue;
				}
				List<String> monsters = new ArrayList<>();
				for (String name : stage.substring(0, split).split(",")) {
					if (!name.isBlank()) {
						monsters.add(name.trim());
					}
				}
				if (!monsters.isEmpty()) {
					stages.add(new HuntStage(List.copyOf(monsters), Integer.parseInt(countPart)));
					blockStages++;
				}
			}
			if (cat.equals("hunt")) {
				huntBlocks.add(blockStages);
			}
		}
		if (questId <= 0) {
			return null;
		}
		// 没有进度块的行（纯对话接取/发放型）不属于任何已覆盖形状：noProgress() 派生判定保证
		// 它不会被误派到 hunt 分支并报出"怪物未解析"这种误导性拒绝码（P5-2 探针实测 78 行）。
		// Rows without progress blocks belong to no covered shape; the derived noProgress() keeps
		// them out of the hunt branch with its misleading rejection code.
		return new Entry(questId, acquire, acquireParam, reward, List.copyOf(stepCategories),
			Map.copyOf(stepCutscenes),
			List.copyOf(huntBlocks), List.copyOf(stages), List.copyOf(talkSteps), List.copyOf(collectSteps),
			List.copyOf(enterAreaSteps), List.copyOf(enterWorldSteps), List.copyOf(itemPlaySteps),
			List.copyOf(talkFobjSteps), List.copyOf(pvpCounts), pvpMinRank, pvpRankCeiling, pvpLevelGap);
	}

	/** 世界 id 解析：非数字值记 -1（编译期以稳定码拒绝，不使整表装载失败）。 /
	 * Parses a world id; a non-numeric value is recorded as -1 so the compiler rejects that row with
	 * a stable code instead of failing the whole table load. */
	private static int parseWorldId(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}
}
