package com.aionemu.gameserver.questEngine.tablelane;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.retail.RetailClientHandinNpcSets;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;

/**
 * P5 两族的**行集/可行性冻结门**（计划 §7 批门第 1 条）：
 * <ul>
 *   <li>SimpleUseItem 160 行 / SimpleItemPlay 43 行（装载器逐行 = 原版表逐行）；</li>
 *   <li>退役集（{@code retail-xml-retention.xml} owner=RETAIL_TABLE）= 104 / 6；</li>
 *   <li>NATIVE_READY = 退役 ∧ 非 XML-only ∧ 接取/交付 NPC 可解 ∧ 道具符号（两通道）可解 ∧
 *       中继 NPC 与第 K 步发/扣物品可解 ∧ 原版元数据可编译 ⇒ **102 / 6**；</li>
 *   <li>fail-closed 残余冻结 = SimpleUseItem {30720, 30723}（复合交付名无客户端登记，同 P4 39611/49611 类）。</li>
 * </ul>
 * 断言口径与本类内的独立复算一致（不复用 handler 判定），任何行漂移即红灯。
 * <p>
 * Frozen row-set / feasibility gate for the two P5 families: table rows, retired set, NATIVE_READY set
 * (accept/hand-in faces, item symbols through the two-channel rule, relay faces, metadata) and the
 * fail-closed residue. The verdict is recomputed here, not read from the handler.
 */
class UseItemFamilyRowInventoryGateTest {

	@Test
	void rowInventoryAndReadySetsAreFrozen() throws Exception {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		NativeQuestOwnerResolver owners = NativeQuestOwnerResolver.instance();
		NativeNpcNameResolver npcs = NativeNpcNameResolver.instance();
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		RetailClientHandinNpcSets handin = RetailClientHandinNpcSets.defaultSets();
		RetailQuestDriver driver = RetailQuestDriver.ensureLoaded();

		List<String> lines = new ArrayList<>();
		Map<String, Integer> reasons = new TreeMap<>();
		Map<String, int[]> perFamily = new LinkedHashMap<>();

		for (NativeQuestTableLoader.SimpleUseItemRow row : loader.useItemRows()) {
			int q = row.questId();
			List<String> why = new ArrayList<>();
			boolean retired = RetiredQuestIds.contains(q);
			boolean xmlOnly = owners.xmlOnlyIds().contains(q);
			boolean itemOk = itemResolves(row.useItemName(), items);
			boolean rewardOk = rewardResolves(row.rewardNpcName(), npcs, handin, q);
			boolean talkOk = row.talkNpcNames().stream().allMatch(n -> unique(n, npcs));
			boolean stepItemsOk = row.stepGiveItems().stream().filter(java.util.Objects::nonNull)
				.allMatch(s -> itemResolves(s, items))
				&& row.stepRemoveItems().stream().filter(java.util.Objects::nonNull)
				.allMatch(s -> itemResolves(s, items));
			boolean metaOk = metadataOk(driver, q);
			if (!retired) why.add("NOT_RETIRED");
			if (xmlOnly) why.add("XML_ONLY");
			if (!itemOk) why.add("USE_ITEM_UNRESOLVED");
			if (!rewardOk) why.add("REWARD_NPC_UNRESOLVED");
			if (!talkOk) why.add("TALK_NPC_UNRESOLVED");
			if (!stepItemsOk) why.add("STEP_ITEM_UNRESOLVED");
			if (!metaOk) why.add("METADATA_UNRESOLVED");
			boolean ready = why.isEmpty();
			lines.add(String.join("\t", "SimpleUseItem", String.valueOf(q), row.useItemName(),
				row.rewardNpcName(), String.valueOf(row.talkNpcNames().size()),
				String.valueOf(row.conQuest()), String.valueOf(row.itemCheck()),
				ready ? "NATIVE_READY" : String.join("+", why)));
			if (retired) {
				for (String reason : why) {
					if (!"NOT_RETIRED".equals(reason)) {
						reasons.merge("SimpleUseItem/" + reason, 1, Integer::sum);
					}
				}
			}
			int[] stat = perFamily.computeIfAbsent("SimpleUseItem", k -> new int[3]);
			stat[0]++;
			if (retired) stat[1]++;
			if (ready) stat[2]++;
		}

		for (NativeQuestTableLoader.SimpleItemPlayRow row : loader.itemPlayRows()) {
			int q = row.questId();
			List<String> why = new ArrayList<>();
			boolean retired = RetiredQuestIds.contains(q);
			boolean xmlOnly = owners.xmlOnlyIds().contains(q);
			boolean itemOk = row.useItemName() == null || itemResolves(row.useItemName(), items);
			boolean acquireOk = unique(row.acquiredNpcName(), npcs);
			boolean rewardOk = rewardResolves(row.rewardNpcName(), npcs, handin, q);
			boolean talkOk = row.talkNpcNames().stream().allMatch(n -> unique(n, npcs));
			boolean grantOk = (row.acceptGiveItem() == null || itemResolves(row.acceptGiveItem(), items))
				&& row.stepGiveItems().stream().filter(java.util.Objects::nonNull)
					.allMatch(s -> itemResolves(s, items))
				&& row.stepRemoveItems().stream().filter(java.util.Objects::nonNull)
					.allMatch(s -> itemResolves(s, items));
			boolean metaOk = metadataOk(driver, q);
			if (!retired) why.add("NOT_RETIRED");
			if (xmlOnly) why.add("XML_ONLY");
			if (!itemOk) why.add("USE_ITEM_UNRESOLVED");
			if (!acquireOk) why.add("ACQUIRE_NPC_UNRESOLVED");
			if (!rewardOk) why.add("REWARD_NPC_UNRESOLVED");
			if (!talkOk) why.add("TALK_NPC_UNRESOLVED");
			if (!grantOk) why.add("GIVE_ITEM_UNRESOLVED");
			if (!metaOk) why.add("METADATA_UNRESOLVED");
			boolean ready = why.isEmpty();
			lines.add(String.join("\t", "SimpleItemPlay", String.valueOf(q),
				String.valueOf(row.useItemName()), row.rewardNpcName(),
				String.valueOf(row.talkNpcNames().size()), String.valueOf(row.conQuest()),
				String.valueOf(row.itemCheck()), ready ? "NATIVE_READY" : String.join("+", why)));
			if (retired) {
				for (String reason : why) {
					if (!"NOT_RETIRED".equals(reason)) {
						reasons.merge("SimpleItemPlay/" + reason, 1, Integer::sum);
					}
				}
			}
			int[] stat = perFamily.computeIfAbsent("SimpleItemPlay", k -> new int[3]);
			stat[0]++;
			if (retired) stat[1]++;
			if (ready) stat[2]++;
		}

		String out = System.getProperty("p5.out");
		if (out != null) {
			Files.write(Path.of(out), ("# P5 可行性探针（临时；NATIVE_READY 判据见报告）\n"
				+ "family\tquest_id\tuse_item\treward_npc\ttalk_count\tcon_quest\titem_check\tverdict\n"
				+ String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
		}
		org.junit.jupiter.api.Assertions.assertEquals(160,
			(int) lines.stream().filter(line -> line.startsWith("SimpleUseItem\t")).count(),
			"SimpleUseItem 表行数必须冻结（原版表逐行装载）");
		org.junit.jupiter.api.Assertions.assertEquals(43,
			(int) lines.stream().filter(line -> line.startsWith("SimpleItemPlay\t")).count(),
			"SimpleItemPlay 表行数必须冻结");
		org.junit.jupiter.api.Assertions.assertEquals(104, perFamily.get("SimpleUseItem")[1],
			"SimpleUseItem 退役行数必须冻结（retention owner=RETAIL_TABLE）");
		org.junit.jupiter.api.Assertions.assertEquals(8, perFamily.get("SimpleItemPlay")[1],
			"SimpleItemPlay 退役行数必须冻结（P5D 步 3 激活 18213/28213 后）");
		org.junit.jupiter.api.Assertions.assertEquals(102, perFamily.get("SimpleUseItem")[2],
			"SimpleUseItem NATIVE_READY 行数必须冻结");
		org.junit.jupiter.api.Assertions.assertEquals(8, perFamily.get("SimpleItemPlay")[2],
			"SimpleItemPlay NATIVE_READY 行数必须冻结（中继面全解 ⇒ 8）");
		java.util.Set<String> failClosed = new java.util.TreeSet<>();
		for (String line : lines) {
			String[] parts = line.split("\t");
			if (parts[0].equals("SimpleUseItem") && !parts[7].startsWith("NOT_RETIRED")
					&& !parts[7].equals("NATIVE_READY")) {
				failClosed.add(parts[1]);
			}
		}
		org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("30720", "30723"), failClosed,
			"复核后仍不可路由的退役行必须冻结（复合交付名无客户端登记）");

		perFamily.forEach((family, stat) -> System.out.println("P5_PROBE " + family + " rows=" + stat[0]
			+ " retired=" + stat[1] + " ready=" + stat[2]));
		System.out.println("P5_PROBE reasons " + reasons);
	}

	private static boolean metadataOk(RetailQuestDriver driver, int questId) {
		try {
			return driver.retailMetadataOf(questId).map(RetailQuestMetadataCompiler.Outcome::clean)
				.orElse(false);
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static boolean unique(String name, NativeNpcNameResolver npcs) {
		if (name == null || name.isBlank()) {
			return true;
		}
		return npcs.resolve(name).resolution() == NativeNpcNameResolver.Resolution.UNIQUE;
	}

	private static boolean rewardResolves(String name, NativeNpcNameResolver npcs,
			RetailClientHandinNpcSets handin, int questId) {
		if (unique(name, npcs)) {
			return true;
		}
		return !handin.npcIds(questId).isEmpty();
	}

	private static boolean itemResolves(String cell, RetailItemNameIndex items) {
		if (cell == null || cell.isBlank()) {
			return false;
		}
		// 单元 = 「符号 [数量]」；符号解析走两通道（先原名，未命中再去 ITEM_ 前缀）。
		String symbol = cell.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
		if (items.resolve(symbol) != null) {
			return true;
		}
		return symbol.startsWith("item_") && items.resolve(symbol.substring("item_".length())) != null;
	}
}
