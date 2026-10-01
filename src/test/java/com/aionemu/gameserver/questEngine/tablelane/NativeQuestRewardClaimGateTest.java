package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.Rewards;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailQuestTitleIds;
import com.aionemu.gameserver.questEngine.retail.RetailQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable.QuestRow;
import com.aionemu.gameserver.services.QuestService;

/**
 * native 车道**领奖段**门禁（计划 §10.3-#11 / §6.2 {@code NativeReportRewardFlow}；QE-113 的安全网）。
 * <p>
 * 覆盖三件事：
 * <ol>
 *   <li>奖励面逐列对拍：真端 {@code quest.xml} 的 {@code reward_exp/gold/abyss_point/title/
 *       reward_item1_N/selectable_reward_item1_N/{class}_selectable_reward/*_ext} 列 → native 完成口
 *       解析出的 typed 奖励面；</li>
 *   <li>领奖段门态：三个族（Talk/Hunt/SerialHunt）的 {@code REWARD} 行经奖励窗动作 → {@code COMPLETE}
 *       页，且结算体拿到的模板来自真端行（不再依赖 typed XML/IR 模板）；</li>
 *   <li>fail-closed：未声明按钮、未解析奖励符号名、缺元数据、缺 {@code REWARD} 态、结算拒绝
 *       —— 一律不发奖不完成。</li>
 * </ol>
 * 结算体的发放细节（背包/经验/金币/称号/AP/GP/DP）由 {@link QuestService} 的共用结算体承担，本门
 * 只锁定「模板来自真端行 + 门态判定 + 页契约」三面。
 * <p>
 * Claim-segment gate for the native lane: the reward face is compared column-by-column against the
 * retail row, the three switched families complete through the reward window with a retail-derived
 * template, and every unproven/undeclared face fails closed.
 */
class NativeQuestRewardClaimGateTest {

	/** 单槽 6 可选奖励的 talk 行（奖励窗按钮 = 档内选项）。 / Single-slot talk row with six options. */
	private static final int TALK_SELECTABLE_QUEST = 1207;
	/** 纯标量奖励的 hunt 行。 / Scalar-only hunt row. */
	private static final int HUNT_SCALAR_QUEST = 1102;
	/** 固定道具奖励的 serial hunt 行。 / Serial-hunt row with a fixed item reward. */
	private static final int SERIAL_REWARD_QUEST = 16991;
	/** 称号 + 固定道具的 talk 行。 / Talk row with a title and a fixed item. */
	private static final int TALK_TITLE_QUEST = 1124;
	/** 10 次重复 + 扩展奖励（{@code *_ext}）的 talk 行。 / Repeat row carrying the extended rewards. */
	private static final int TALK_EXT_QUEST = 4316;
	/** 职业奖励（{@code use_class_reward=1}）的 talk 行。 / Talk row using per-class rewards. */
	private static final int TALK_CLASS_QUEST = 1690;

	private static RetailItemNameIndex items;

	@BeforeAll
	static void loadRetailFixtures() throws IOException {
		items = RetailItemNameIndex.loadItemTemplates();
		if (RetailQuestDriver.current().isEmpty()) {
			// 真端驱动是 native 完成口的元数据来源（与生产目录同一条装载路径）。
			// The retail driver is the metadata source of the native completion port.
			RetailQuestDriver.overlay(ImmutableQuestCatalog.fromEntries(List.of()));
		}
	}

	/** 记录结算调用的假结算体。 / A recording settlement sink. */
	private static final class RecordingSink implements NativeReportRewardFlow.CompletionSink {
		private final List<Call> calls = new ArrayList<>();
		private boolean result = true;

		@Override
		public boolean complete(QuestEnv env, int rewardTier, QuestTemplate template) {
			calls.add(new Call(env, rewardTier, template));
			return result;
		}

		private record Call(QuestEnv env, int rewardTier, QuestTemplate template) {
		}
	}

	// ------------------------------------------------------------------ 奖励面逐列对拍

	@Test
	void nativeRewardFaceFollowsTheRetailColumns() {
		// 1102：纯标量（经验/金币）。 / 1102: scalar-only.
		QuestRow hunt = row(HUNT_SCALAR_QUEST);
		Rewards huntTier = claimRewards(HUNT_SCALAR_QUEST, 0);
		assertEquals(hunt.integer("reward_exp1"), huntTier.getExp(), "reward_exp1");
		assertEquals(hunt.integer("reward_gold1"), huntTier.getGold(), "reward_gold1");

		// 16991：固定道具（50 枚）。 / 16991: a fixed item stack.
		QuestRow serial = row(SERIAL_REWARD_QUEST);
		Rewards serialTier = claimRewards(SERIAL_REWARD_QUEST, 0);
		assertEquals(serial.integer("reward_exp1"), serialTier.getExp(), "reward_exp1");
		assertEquals(serial.integer("reward_gold1"), serialTier.getGold(), "reward_gold1");
		assertItems(serial, "reward_item1_", serialTier.getRewardItem());

		// 1207：6 个可选奖励，无固定道具。 / 1207: six selectable rewards, no fixed item.
		QuestRow selectable = row(TALK_SELECTABLE_QUEST);
		Rewards selectableTier = claimRewards(TALK_SELECTABLE_QUEST, 0);
		assertTrue(selectableTier.getRewardItem().isEmpty(), "真端行未声明固定道具");
		assertItems(selectable, "selectable_reward_item1_", selectableTier.getSelectableRewardItem());

		// 1124：称号 + 固定道具。 / 1124: title plus a fixed item.
		QuestRow titled = row(TALK_TITLE_QUEST);
		Rewards titleTier = claimRewards(TALK_TITLE_QUEST, 0);
		assertEquals(RetailQuestTitleIds.idOf(titled.text("reward_title1")), titleTier.getTitle(),
			"reward_title1");
		assertItems(titled, "reward_item1_", titleTier.getRewardItem());

		// 4316：扩展奖励（_ext 族）。 / 4316: the extended reward group.
		QuestRow extended = row(TALK_EXT_QUEST);
		QuestTemplate extTemplate = claimTemplate(TALK_EXT_QUEST, 0);
		assertEquals(1, extTemplate.getExtendedRewards().size(), "*_ext 族单组");
		Rewards extTier = extTemplate.getExtendedRewards().getFirst();
		assertEquals(extended.integer("reward_exp_ext"), extTier.getExp(), "reward_exp_ext");
		assertEquals(extended.integer("reward_gold_ext"), extTier.getGold(), "reward_gold_ext");
		assertItems(extended, "reward_item_ext_", extTier.getRewardItem());
		assertEquals(10, extTemplate.getRewardRepeatCount(), "reward_repeat_count");

		// 1690：职业奖励（{@code {class}_selectable_reward}）。 / 1690: per-class rewards.
		QuestTemplate classTemplate = claimTemplate(TALK_CLASS_QUEST, 0, PlayerClass.GLADIATOR);
		assertTrue(classTemplate.isUseSingleClassReward(), "use_class_reward=1");
		assertClassRewards(TALK_CLASS_QUEST, "fighter_selectable_reward",
			classTemplate.getFighterSelectableReward());
		assertClassRewards(TALK_CLASS_QUEST, "elementalist_selectable_reward",
			classTemplate.getElementalistSelectableReward());
	}

	// ------------------------------------------------------------------ 奖励窗口按钮

	@Test
	void rewardWindowButtonStaysInsideTheDeclaredOptions() {
		// 1207 声明 6 个可选奖励 ⇒ 按钮下标 0..5 合法，6 起 fail-closed。
		RecordingSink accepted = new RecordingSink();
		NativeReportRewardFlow flow = NativeReportRewardFlow.withSink(accepted);
		assertTrue(claim(flow, TALK_SELECTABLE_QUEST, 5, QuestStatus.REWARD).completed());
		assertEquals(1, accepted.calls.size());
		assertEquals(0, accepted.calls.getFirst().rewardTier(), "单槽行档位固定首档");
		assertEquals(5, accepted.calls.getFirst().env().getDialogId() - 8, "选项下标仍由对话动作决定");

		RecordingSink rejected = new RecordingSink();
		assertFalse(claim(NativeReportRewardFlow.withSink(rejected), TALK_SELECTABLE_QUEST, 6,
			QuestStatus.REWARD).completed());
		assertTrue(rejected.calls.isEmpty(), "未声明按钮不得进入结算体");
	}

	@Test
	void rewardWindowWithoutOptionsOnlyAcceptsTheClaimButton() {
		RecordingSink accepted = new RecordingSink();
		assertTrue(claim(NativeReportRewardFlow.withSink(accepted), HUNT_SCALAR_QUEST, 0,
			QuestStatus.REWARD).completed());
		assertEquals(1, accepted.calls.size());

		RecordingSink rejected = new RecordingSink();
		assertFalse(claim(NativeReportRewardFlow.withSink(rejected), HUNT_SCALAR_QUEST, 1,
			QuestStatus.REWARD).completed());
		assertTrue(rejected.calls.isEmpty(), "无声明选项时只有领取按钮合法");
	}

	@Test
	void classRewardButtonsFollowThePlayersClassList() {
		// 1690 的 fighter 职业奖励 4 项 ⇒ 战士系(GLIADIATOR) 下标 0..3 合法。
		RecordingSink accepted = new RecordingSink();
		assertTrue(claim(NativeReportRewardFlow.withSink(accepted), TALK_CLASS_QUEST, 3,
			QuestStatus.REWARD, PlayerClass.GLADIATOR).completed());

		RecordingSink rejected = new RecordingSink();
		assertFalse(claim(NativeReportRewardFlow.withSink(rejected), TALK_CLASS_QUEST, 4,
			QuestStatus.REWARD, PlayerClass.GLADIATOR).completed());
		assertTrue(rejected.calls.isEmpty(), "超出职业奖励表长的按钮 fail-closed");
	}

	// ------------------------------------------------------------------ fail-closed

	@Test
	void unresolvedRetailRewardSymbolsFailClosed() {
		RecordingSink sink = new RecordingSink();
		RetailQuestMetadataCompiler.Outcome compiled = RetailQuestDriver.current().orElseThrow()
			.retailMetadataOf(TALK_SELECTABLE_QUEST).orElseThrow();
		RetailQuestMetadataCompiler.Outcome broken = new RetailQuestMetadataCompiler.Outcome(
			compiled.metadata(), List.of("reward:q_unknown_symbol"));
		NativeReportRewardFlow flow = new NativeReportRewardFlow(questId -> java.util.Optional.of(broken), sink);

		NativeReportRewardFlow.Outcome outcome = claim(flow, TALK_SELECTABLE_QUEST, 0, QuestStatus.REWARD);
		assertFalse(outcome.completed());
		assertTrue(outcome.code().startsWith("NATIVE_REWARD_UNRESOLVED:"), outcome.code());
		assertTrue(sink.calls.isEmpty(), "奖励符号名未解析 ⇒ 不发放");
	}

	@Test
	void claimRequiresRewardStateAndRetailMetadata() {
		RecordingSink sink = new RecordingSink();
		NativeReportRewardFlow flow = NativeReportRewardFlow.withSink(sink);

		NativeReportRewardFlow.Outcome started = claim(flow, TALK_SELECTABLE_QUEST, 0, QuestStatus.START);
		assertFalse(started.completed());
		assertEquals("NATIVE_REWARD_NOT_IN_REWARD", started.code());

		NativeReportRewardFlow.Outcome missing = claim(
			new NativeReportRewardFlow(questId -> java.util.Optional.empty(), sink),
			TALK_SELECTABLE_QUEST, 0, QuestStatus.REWARD);
		assertFalse(missing.completed());
		assertEquals("NATIVE_REWARD_METADATA_UNAVAILABLE", missing.code());
		assertTrue(sink.calls.isEmpty());
	}

	@Test
	void rejectedSettlementDoesNotComplete() {
		RecordingSink sink = new RecordingSink();
		sink.result = false;
		NativeReportRewardFlow.Outcome outcome = claim(NativeReportRewardFlow.withSink(sink),
			TALK_SELECTABLE_QUEST, 0, QuestStatus.REWARD);
		assertFalse(outcome.completed());
		assertEquals("NATIVE_REWARD_REJECTED", outcome.code());
	}

	@Test
	void legacyFinishEntryPointFailsClosedWithoutTypedTemplate() throws Exception {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, TALK_SELECTABLE_QUEST, QuestStatus.REWARD, 0);
		QuestEnv env = NativeTalkFixture.dialog(player, 0, TALK_SELECTABLE_QUEST, 9);

		Field field = QuestService.class.getDeclaredField("questsData");
		field.setAccessible(true);
		Object previous = field.get(null);
		try {
			// 已切换的 native 行不在生产目录里（无 typed 模板）⇒ 旧完成口必须返回 false 而不是抛异常。
			field.set(null, QuestsData.fromCatalog(ImmutableQuestCatalog.fromEntries(List.of())));
			assertFalse(QuestService.finishQuest(env, 0), "缺 typed 模板必须 fail-closed");
		} finally {
			field.set(null, previous);
		}
	}

	// ------------------------------------------------------------------ 三族领奖段门态

	@Test
	void talkClaimCompletesWithTheRetailDerivedTemplate() {
		RecordingSink sink = new RecordingSink();
		SimpleTalkHandler handler = NativeTalkFixture.handler(NativeInventoryPort.live(),
			NativeReportRewardFlow.withSink(sink));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, TALK_SELECTABLE_QUEST, QuestStatus.REWARD, 0);
		int npcId = handler.rewardNpc(TALK_SELECTABLE_QUEST);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, npcId, TALK_SELECTABLE_QUEST, 9)),
			"奖励窗按钮必须被领奖段服务");
		assertEquals(List.of(SimpleTalkHandler.PAGE_COMPLETE), NativeTalkFixture.dialogPages(player));
		assertEquals(1, sink.calls.size());
		assertEquals(TALK_SELECTABLE_QUEST, sink.calls.getFirst().env().getQuestId());
		assertRewardFaceFromRetailRow(sink.calls.getFirst().template(), TALK_SELECTABLE_QUEST);
	}

	@Test
	void huntClaimCompletesWithTheRetailDerivedTemplate() {
		RecordingSink sink = new RecordingSink();
		SimpleHuntHandler handler = new SimpleHuntHandler(NativeQuestTableLoader.instance(),
			CameraRegistry.instance(), NativeNpcNameResolver.instance(), HtmlPagesRegistry.instance(),
			NativeQuestOwnerResolver.instance().xmlOnlyIds(), NativeReportRewardFlow.withSink(sink));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, HUNT_SCALAR_QUEST, QuestStatus.REWARD, 0);
		int npcId = handler.rewardNpc(HUNT_SCALAR_QUEST);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, npcId, HUNT_SCALAR_QUEST, 8)),
			"奖励窗按钮必须被领奖段服务");
		assertEquals(List.of(1008), NativeTalkFixture.dialogPages(player));
		assertEquals(1, sink.calls.size());
		assertRewardFaceFromRetailRow(sink.calls.getFirst().template(), HUNT_SCALAR_QUEST);
	}

	@Test
	void serialHuntClaimCompletesWithTheRetailDerivedTemplate() {
		RecordingSink sink = new RecordingSink();
		SimpleSerialHuntHandler handler = new SimpleSerialHuntHandler(NativeQuestTableLoader.instance(),
			CameraRegistry.instance(), NativeNpcNameResolver.instance(),
			NativeQuestOwnerResolver.instance().xmlOnlyIds(), NativeReportRewardFlow.withSink(sink));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, SERIAL_REWARD_QUEST, QuestStatus.REWARD, 0);
		int npcId = handler.rewardNpc(SERIAL_REWARD_QUEST);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, npcId, SERIAL_REWARD_QUEST, 8)),
			"奖励窗按钮必须被领奖段服务");
		assertEquals(List.of(1008), NativeTalkFixture.dialogPages(player));
		assertEquals(1, sink.calls.size());
		assertRewardFaceFromRetailRow(sink.calls.getFirst().template(), SERIAL_REWARD_QUEST);
	}

	/**
	 * 已切换行里只有两条真端行声明多档（18706/28706，等级轴 999 = 不可达），可达行全部单档。
	 * 多档行的档位语义属计划 P6，本口对其 fail-closed（见第二条断言），此冻结断言先破。
	 * Only two switched rows declare multiple reward slots (18706/28706, level-axis 999 = unreachable);
	 * every reachable row is single-slot and multi-slot rows fail closed.
	 */
	@Test
	void multiSlotRowsAreUnreachableAndFailClosed() {
		Set<Integer> multiSlot = new TreeSet<>();
		Set<Integer> reachable = new TreeSet<>();
		for (int questId : switchedQuestIds()) {
			QuestRow row = row(questId);
			int slots = row.fields().keySet().stream()
				.mapToInt(NativeQuestRewardClaimGateTest::rewardSlot).max().orElse(0);
			if (slots <= 1) {
				continue;
			}
			multiSlot.add(questId);
			Integer minLevel = row.integer("minlevel_permitted");
			if (minLevel == null || minLevel < 999) {
				reachable.add(questId);
			}
		}
		assertEquals(Set.of(18706, 28706), multiSlot, "真端多档行的完整清单");
		assertTrue(reachable.isEmpty(), () -> "可达的多档行必须先坐实 P6 档位语义：" + reachable);

		RecordingSink sink = new RecordingSink();
		NativeReportRewardFlow.Outcome outcome = claim(NativeReportRewardFlow.withSink(sink), 18706, 0,
			QuestStatus.REWARD);
		assertFalse(outcome.completed());
		assertEquals("NATIVE_REWARD_TIER_UNRESOLVED", outcome.code());
		assertTrue(sink.calls.isEmpty(), "档位未坐实 ⇒ 不发放不完成");
	}

	/** 字段名 → 奖励槽位（非槽位族返回 0）。 / Column name to reward slot (0 when not a slot family). */
	private static int rewardSlot(String key) {
		var matcher = java.util.regex.Pattern.compile(
			"^(?:reward_(?:exp|gold|abyss_point|glory_point|dp|cp|exp_boost|abyss_op_point|title)(\\d+)"
				+ "|reward_item(\\d+)_\\d+|selectable_reward_item(\\d+)_\\d+)$").matcher(key);
		if (!matcher.matches()) {
			return 0;
		}
		for (int group = 1; group <= matcher.groupCount(); group++) {
			if (matcher.group(group) != null) {
				return Integer.parseInt(matcher.group(group));
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------ 工具

	private static Set<Integer> switchedQuestIds() {
		Set<Integer> ids = new TreeSet<>();
		SimpleTalkHandler talk = SimpleTalkHandler.instance();
		ids.addAll(talk.routedQuestIds());
		SimpleHuntHandler hunt = SimpleHuntHandler.instance();
		ids.addAll(hunt.routedQuestIds());
		SimpleSerialHuntHandler serial = SimpleSerialHuntHandler.instance();
		ids.addAll(serial.routedQuestIds());
		return ids;
	}

	private static QuestRow row(int questId) {
		return NativeQuestXmlTable.instance().require(questId);
	}

	private static Rewards claimRewards(int questId, int buttonIndex) {
		return claimTemplate(questId, buttonIndex).getRewards().getFirst();
	}

	private static QuestTemplate claimTemplate(int questId, int buttonIndex) {
		return claimTemplate(questId, buttonIndex, PlayerClass.WARRIOR);
	}

	private static QuestTemplate claimTemplate(int questId, int buttonIndex, PlayerClass playerClass) {
		RecordingSink sink = new RecordingSink();
		Player player = NativeTalkFixture.player(Race.ELYOS, playerClass, 50);
		NativeTalkFixture.add(player, questId, QuestStatus.REWARD, 0);
		QuestEnv env = NativeTalkFixture.dialog(player, 0, questId, 8 + buttonIndex);
		assertTrue(NativeReportRewardFlow.withSink(sink).claim(env, buttonIndex).completed(),
			() -> "quest " + questId + " 的奖励面必须可解析");
		return sink.calls.getFirst().template();
	}

	private static NativeReportRewardFlow.Outcome claim(NativeReportRewardFlow flow, int questId,
			int buttonIndex, QuestStatus status) {
		return claim(flow, questId, buttonIndex, status, PlayerClass.WARRIOR);
	}

	private static NativeReportRewardFlow.Outcome claim(NativeReportRewardFlow flow, int questId,
			int buttonIndex, QuestStatus status, PlayerClass playerClass) {
		Player player = NativeTalkFixture.player(Race.ELYOS, playerClass, 50);
		NativeTalkFixture.add(player, questId, status, 0);
		return flow.claim(NativeTalkFixture.dialog(player, 0, questId, 8 + buttonIndex), buttonIndex);
	}

	private static void assertRewardFaceFromRetailRow(QuestTemplate template, int questId) {
		assertNotNull(template, "native 完成口必须给出真端行模板");
		QuestRow row = row(questId);
		Rewards tier = template.getRewards().getFirst();
		// 0 与缺列同义（转换约定：0 值标量不进奖励面）。 / Zero and missing are the same (0 emits nothing).
		if (row.integer("reward_exp1") != null && row.integer("reward_exp1") != 0) {
			assertEquals(row.integer("reward_exp1"), tier.getExp(), "reward_exp1");
		}
		if (row.integer("reward_gold1") != null && row.integer("reward_gold1") != 0) {
			assertEquals(row.integer("reward_gold1"), tier.getGold(), "reward_gold1");
		}
		assertItems(row, "reward_item1_", tier.getRewardItem());
		assertItems(row, "selectable_reward_item1_", tier.getSelectableRewardItem());
	}

	/** 真端 {@code baseN} 列（{@code name [count]}）↔ 模板道具表逐项对拍。 / Column-to-template item对拍. */
	private static void assertItems(QuestRow row, String base, List<QuestItems> granted) {
		List<String> declared = row.numbered(base);
		assertEquals(declared.size(), granted.size(), base + " 数量");
		for (int index = 0; index < declared.size(); index++) {
			String[] tokens = declared.get(index).trim().split("\\s+");
			Integer itemId = items.resolve(tokens[0]);
			assertNotNull(itemId, () -> "真端道具名不可解析: " + tokens[0]);
			int count = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 1;
			assertEquals(itemId, granted.get(index).getItemId(), base + " 第 " + (index + 1) + " 项 id");
			assertEquals(count, granted.get(index).getCount(), base + " 第 " + (index + 1) + " 项数量");
		}
	}

	/** 真端嵌套的职业奖励列（{@code <data><x_selectable_item>…}）↔ 模板职业表对拍。 */
	private static void assertClassRewards(int questId, String column, List<QuestItems> granted) {
		List<String> declared = retailClassItems(questId, column);
		assertEquals(declared.size(), granted.size(), column + " 数量");
		for (int index = 0; index < declared.size(); index++) {
			String[] tokens = declared.get(index).trim().split("\\s+");
			Integer itemId = items.resolve(tokens[0]);
			assertNotNull(itemId, () -> "真端道具名不可解析: " + tokens[0]);
			assertEquals(itemId, granted.get(index).getItemId(), column + " 第 " + (index + 1) + " 项");
		}
	}

	/** 用真端行解析器读嵌套职业奖励列。 / Reads the nested class-reward column through the retail row parser. */
	private static List<String> retailClassItems(int questId, String column) {
		try (InputStream input = NativeQuestRewardClaimGateTest.class.getResourceAsStream(
				"/aion/data/static_data/quest/retail/quest.xml")) {
			assertNotNull(input, "quest.xml 资源");
			RetailQuestXmlTable table = RetailQuestXmlTable.load(input);
			RetailQuestXmlTable.Entry entry = table.find(questId).orElseThrow();
			// 容器列按 <data> 子块逐条返回 {@code name [count]}。 / One {@code name [count]} per <data> child.
			return entry.block(column);
		} catch (IOException e) {
			throw new AssertionError("retail quest.xml unreadable", e);
		}
	}
}
