package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DD（DataDriven）行的"演出道具可得"门（P8 重建步 f 退役的 {@code QuestItemPlayGrantGateTest}）：
 * 可路由行的每条 ItemPlay 步（kind 3）载荷 itemId 必须**可诉诸三源之一**，否则链在演出步死亡
 * （玩家拿不到道具却要"使用"它）。三源口径：
 * <ol>
 * <li>**物品模板**：载荷符号经真端物品模板索引解析（解析成功即模板存在；路由集行解析失败本就
 *     被 NAME_UNRESOLVED 冻结，这里做一致性复查）；</li>
 * <li>**掉落/授予（任务数据面）**：同一行 GIVE_ITEMS 列（列 1，`符号 数量` 对，任意步含接取步 0）
 *     ∪ 真端 quest.xml 掉落列（{@code RetailQuestMetadataCompiler} 同链）；</li>
 * <li>**工作物品采集面**：itemId == 本任务自己的 {@code quest_work_itemN} 声明——真端数据把该物品
 *     声明为本任务凭证，活运行时由掉落系统供给（{@code QuestService} 对工作物品做已持有去重闸门），
 *     不由任务数据直接发放；</li>
 * </ol>
 * 之外的真实来源逐条登记在 {@link #REGISTERED_EXTERNAL}（附真端证据），并断言登记仍被使用
 * （数据漂移后登记作废即红）。
 * <p>
 * Gate for the DataDriven rows (rebuilt for the gate retired in step f): every ItemPlay step of a
 * routed row must resolve to a real item template and be covered by the in-row GIVE_ITEMS grants,
 * the quest.xml drops, the quest's own quest_work_item declaration (drop-system-served credential),
 * or a registered external source with retail evidence. Registrations must stay in use.
 */
class DataDrivenItemPlayGrantGateTest {

	private static final String DD_TABLE = DataDrivenNativeRuntime.TABLE_RESOURCE;

	/**
	 * 任务数据之外的真实来源（quest_id → 真端证据）。当前 = 18738/28738 的炸弹
	 * {@code idraksha_solo_bomb_01a}(164000342)：真端 npcs_npcs.xml 三颗宝箱 NPC 的 items_info
	 * 100% common 掉落（IDRaksha_Solo_TreasureBox_A/B/C = 702694/702817/702818，×20/5/10），
	 * 不走任务掉落列。
	 * External sources with retail evidence: the 18738/28738 bombs drop from the three treasure-box
	 * NPCs (items_info, 100% common), outside the quest drop columns.
	 */
	private static final Map<Integer, String> REGISTERED_EXTERNAL = Map.of(
		18738, "idraksha_solo_bomb_01a 由真端宝箱 NPC 702694/702817/702818 items_info 100% common 掉落",
		28738, "idraksha_solo_bomb_01a 由真端宝箱 NPC 702694/702817/702818 items_info 100% common 掉落");

	/** 与运行时同形：载荷尾部 `, N` 计数。 / Mirrors the runtime's trailing-count payload form. */
	private static final Pattern TRAILING_INT = Pattern.compile("^(.*?)(?:\\s*,\\s*|\\s+)(\\d+)$");

	private static DataDrivenQuestTable table;
	private static RetailItemNameIndex itemIndex;
	private static Set<Integer> routed;

	@BeforeAll
	static void loadFixtures() throws Exception {
		table = DataDrivenQuestTable.load(resource(DD_TABLE));
		itemIndex = RetailItemNameIndex.loadItemTemplates();
		routed = DataDrivenNativeRuntime.instance().routedQuestIds();
	}

	@Test
	void everyRoutedItemPlayStepConsumesAnObtainableItem() throws Exception {
		int playSteps = 0;
		Set<Integer> quests = new TreeSet<>();
		Set<Integer> usedRegistrations = new TreeSet<>();
		List<String> problems = new ArrayList<>();
		Map<Integer, Set<Integer>> grantsCache = new HashMap<>();
		for (int questId : routed) {
			Optional<DataDrivenQuestTable.Row> row = table.find(questId);
			if (row.isEmpty()) {
				continue;
			}
			Set<Integer> grants = grantsCache.computeIfAbsent(questId, DataDrivenItemPlayGrantGateTest::inRowGrants);
			Set<Integer> obtainable = new TreeSet<>(grants);
			Set<Integer> workItems = new TreeSet<>();
			try {
				Optional<RetailQuestMetadataCompiler.Outcome> metadata = RetailQuestDriver.ensureLoaded()
					.retailMetadataOf(questId);
				metadata.ifPresent(outcome -> {
					for (QuestDrop drop : outcome.metadata().drops()) {
						obtainable.add(drop.itemId());
					}
				});
				metadata.ifPresent(outcome -> {
					for (QuestItemRequirement item : outcome.metadata().questWorkItems()) {
						workItems.add(item.itemId());
					}
				});
			} catch (java.io.IOException e) {
				throw new AssertionError("retail quest driver unavailable", e);
			}
			for (DataDrivenQuestTable.Step step : row.get().steps()) {
				if (step.kind() != DataDrivenQuestTable.Kind.ITEM_PLAY) {
					continue;
				}
				playSteps++;
				quests.add(questId);
				String symbol = payloadSymbol(step);
				Integer itemId = itemIndex.resolve(symbol);
				// 源 1 物品模板：解析成功即模板存在；路由行解析失败本应被 NAME_UNRESOLVED 冻结。
				// Source 1 (item template): a resolved symbol implies the template exists; routed rows
				// freeze on unresolved names anyway, this re-checks the same invariant.
				assertNotNull(itemId, () -> questId + ": ItemPlay 载荷 " + symbol + " 未解析（路由行必须可解析）");
				if (obtainable.contains(itemId) || workItems.contains(itemId)) {
					continue;
				}
				String registered = REGISTERED_EXTERNAL.get(questId);
				if (registered != null) {
					usedRegistrations.add(questId);
					continue;
				}
				problems.add(questId + ": ItemPlay 道具 " + symbol + "(" + itemId + ") 无授予（行内 GIVE_ITEMS "
					+ grants + "）、无任务掉落（" + obtainable.size() + " 项）、非本任务工作物品（" + workItems
					+ "）、亦无外部登记");
			}
		}
		assertTrue(playSteps > 0, "ItemPlay 步扫描必须真的看到条目（口径失效）");
		assertTrue(quests.size() > 0, "ItemPlay 任务扫描必须真的看到行（口径失效）");
		assertEquals(REGISTERED_EXTERNAL.keySet(), usedRegistrations,
			"外部登记必须仍被数据使用（数据漂移后登记作废）");
		assertTrue(problems.isEmpty(), () -> "ItemPlay 道具不可得：" + problems.stream().limit(20).toList());
	}

	/** 行内授予：任意步（含接取步 0）的 GIVE_ITEMS 列 `符号 数量` 对，与运行时执行器同形解析。 */
	private static Set<Integer> inRowGrants(int questId) {
		Set<Integer> grants = new LinkedHashSet<>();
		DataDrivenQuestTable.Row row = table.find(questId).orElse(null);
		if (row == null) {
			return grants;
		}
		for (DataDrivenQuestTable.Step step : row.steps()) {
			for (DataDrivenQuestTable.ExtraAction action : step.extraActions()) {
				if (action != DataDrivenQuestTable.ExtraAction.GIVE_ITEMS) {
					continue;
				}
				String text = step.column(action.column());
				if (text == null || text.isBlank()) {
					continue;
				}
				String[] tokens = text.trim().split("[,\\s]+");
				assertEquals(0, tokens.length % 2, () -> questId + ": GIVE_ITEMS 列必须为 `符号 数量` 对：" + text);
				for (int index = 0; index < tokens.length; index += 2) {
					String symbolToken = tokens[index];
					String countToken = tokens[index + 1];
					Integer itemId = itemIndex.resolve(symbolToken);
					assertNotNull(itemId, () -> questId + ": GIVE_ITEMS 符号未解析：" + symbolToken);
					assertTrue(Integer.parseInt(countToken) > 0,
						() -> questId + ": GIVE_ITEMS 数量必须为正：" + text);
					grants.add(itemId);
				}
			}
		}
		return grants;
	}

	/** 载荷符号（剥掉尾部计数）。 / The payload symbol with the trailing count stripped. */
	private static String payloadSymbol(DataDrivenQuestTable.Step step) {
		String text = step.payload().trim();
		Matcher matcher = TRAILING_INT.matcher(text);
		return matcher.matches() ? matcher.group(1).trim() : text;
	}

	private static InputStream resource(String name) {
		InputStream input = DataDrivenItemPlayGrantGateTest.class.getResourceAsStream(name);
		if (input == null) {
			throw new IllegalStateException("missing resource " + name);
		}
		return input;
	}
}
