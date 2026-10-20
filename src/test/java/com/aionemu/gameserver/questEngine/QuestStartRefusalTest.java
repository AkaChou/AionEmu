package com.aionemu.gameserver.questEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;

/**
 * 接取拒绝提示面：玩家可补救的拒绝轴必须按原版消息码与占位符形态下发，服务端状态轴保持静默。
 * <p>
 * The refusal hint face: axes the player can remedy must carry their retail code and placeholder
 * shape, while server-state axes stay silent exactly like retail.
 */
class QuestStartRefusalTest {

	/** 合成原版行：minlevel 50 / maxlevel 200 / ELYOS / 可无限重复。 */
	private static final int SYNTHETIC = 990101;

	/** 真实原版表的行（用于验证目录元数据路径）。 / A row of the real retail table (metadata path). */
	private static final int EXISTING_QUEST = 1132;

	@Test
	void levelMessagesCarryADeclaredBoundAndNeverZero() {
		// typed owner 的等级轴取目录元数据；元数据缺失时回落原版列，再不行回落玩家等级，绝不发 0。
		// The typed bound comes from catalog metadata; without it the retail column then the player's
		// own level is shown, never 0.
		SM_SYSTEM_MESSAGE metadataBound = QuestStartRefusal.messageFor("MIN_LEVEL_NOT_MET",
			player(Race.ELYOS, 1), EXISTING_QUEST);
		assertEquals(1300571, code(metadataBound));
		assertEquals(1, params(metadataBound).length);
		assertEquals(false, "0".equals(params(metadataBound)[0]), "等级提示不得显示 0");

		// 合成行不在 typed 目录里 ⇒ 回落原版列。
		// The synthetic row has no typed metadata, so the retail column is used.
		assertMessage(QuestStartRefusal.messageFor("MIN_LEVEL_NOT_MET", player(Race.ELYOS, 20), SYNTHETIC),
			1300571, "50");
	}

	@Test
	void parameterlessAxesUseTheirRetailCodes() {
		assertMessage(QuestStartRefusal.messageFor("RACE_NOT_PERMITTED", player(Race.ELYOS, 60), SYNTHETIC),
			1300575);
		assertMessage(QuestStartRefusal.messageFor("CLASS_NOT_PERMITTED", player(Race.ELYOS, 60), SYNTHETIC),
			1300580);
		assertMessage(QuestStartRefusal.messageFor("GENDER_NOT_PERMITTED", player(Race.ELYOS, 60), SYNTHETIC),
			1300579);
	}

	@Test
	void rankMessageCarriesThePlayersRankNameId() {
		SM_SYSTEM_MESSAGE rank = QuestStartRefusal.messageFor("RANK_NOT_MET", rankedPlayer(), SYNTHETIC);
		assertEquals(1300573, code(rank));
		// 占位符必须是 DescriptionId（军衔名），不是数字文本：与 legacy 车道同一 ResourceId 形态。
		// The placeholder must be a DescriptionId (the rank name), not numeric text.
		Object param = params(rank)[0];
		assertEquals(DescriptionId.class, param.getClass());
	}

	@Test
	void serverStateAxesStaySilentLikeRetail() {
		Player player = player(Race.ELYOS, 60);
		for (String reason : List.of("LIMITED_QUEST_ACQUISITION_UNSUPPORTED",
				"QUEST_ALREADY_ACTIVE", "NPC_FACTION_QUEST_COOLDOWN", "NPC_FACTION_MISMATCH",
				"QUEST_METADATA_MISSING", "SENTINEL_LEVEL_REQUIRES_NPC_FACTION")) {
			assertNull(QuestStartRefusal.messageFor(reason, player, SYNTHETIC),
				"轴 " + reason + " 在原版不按单个接取动作报错");
		}
	}

	@Test
	void fullQuestJournalReportsTheRetailMaxNormalLine() {
		// 任务栏满有专属原版句（1300622），legacy 车道同样下发；typed 车道不能静默。
		// A full journal has its own retail line (1300622), which the legacy lane already sends;
		// the typed lane must not stay silent.
		SM_SYSTEM_MESSAGE message =
			QuestStartRefusal.messageFor("QUEST_LIST_FULL", player(Race.ELYOS, 60), SYNTHETIC);
		assertEquals(1300622, code(message));
	}

	@Test
	void nativeLevelBlockReportsTheRowsMinimum() {
		// native 等级轴先判下限（原版判定顺序）：20 级 < minlevel 50 ⇒ 等级不足 + 下限 50。
		// The native level axis is lower-bound first: level 20 < minlevel 50 ⇒ "too low", bound 50.
		assertMessage(QuestStartRefusal.messageFor(nativeVerdict(Race.ELYOS, 20), SYNTHETIC),
			1300571, "50");
	}

	@Test
	void unmetPrerequisiteReportsTheFirstUnmetName() throws Exception {
		// 真实前置行：1132 要求先完成 1131。无存档时前置未完成，必须报出一个具体任务名。
		// The real gated row: 1132 needs 1131. Without a save the prerequisite is unmet, so a name
		// must be reported.
		var metadata = com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(1132)
			.map(com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler.Outcome::metadata)
			.orElse(null);
		if (metadata == null || metadata.startConditionGroups().isEmpty()) {
			return;
		}
		assertEquals(false, QuestStartRefusal.unmetPrerequisiteName(metadata, player(Race.ELYOS, 60))
			.isEmpty(), "未完成前置必须报出一个任务名");
	}

	@Test
	void messageIsSentOnceAndOnlyWhenAnAxisHits() {
		Player player = player(Race.ELYOS, 20);
		NativeTalkFixture.clearPackets(player);

		QuestStartRefusal.notifyIfRefused(player, SYNTHETIC, nativeVerdict(Race.ELYOS, 20));
		assertEquals(List.of(1300571), systemMessageCodes(player), "等级不足必须提示一次");

		NativeTalkFixture.clearPackets(player);
		QuestStartRefusal.notifyTypedRefused(player, SYNTHETIC, QuestStartEligibility.allowed());
		assertEquals(List.of(), systemMessageCodes(player), "资格通过（页状态机拒绝）时不得提示");

		NativeTalkFixture.clearPackets(player);
		QuestStartRefusal.notifyTypedRefused(player, SYNTHETIC,
			QuestStartEligibility.rejected("MIN_LEVEL_NOT_MET"));
		assertEquals(List.of(1300571), systemMessageCodes(player), "typed 等级轴同样提示");
	}

	@Test
	void nativeAcceptRefusalReportsTheAxisAndCreatesNoState() {
		Player player = player(Race.ELYOS, 20);
		NativeTalkFixture.clearPackets(player);

		NativeQuestStartPort.StartResult verdict =
			NativeQuestStartPort.instance().startTraced(player, SYNTHETIC, 20000);

		assertEquals(false, verdict.started(), "等级不足必须拒接");
		assertNull(player.getQuestStateList().getQuestState(SYNTHETIC), "被拒时不得建档");
		assertEquals(List.of(1300571), systemMessageCodes(player), "表车道拒绝面必须给出原版等级提示");
	}

	private static NativeQuestStartPort.AcquireVerdict nativeVerdict(Race race, int level) {
		return NativeQuestStartPort.instance().evaluateAcquire(player(race, level), SYNTHETIC);
	}

	private static Player player(Race race, int level) {
		Player player = NativeTalkFixture.player();
		PlayerCommonData common = player.getCommonData();
		setField(PlayerCommonData.class, common, "race", race);
		setField(PlayerCommonData.class, common, "level", level);
		setField(Player.class, player, "questStateList", new QuestStateList());
		return player;
	}

	/** 带军衔的玩家：拒绝提示需要读取军衔名 DescriptionId。 / A player carrying an abyss rank. */
	private static Player rankedPlayer() {
		Player player = player(Race.ELYOS, 60);
		setField(Player.class, player, "abyssRank",
			new com.aionemu.gameserver.model.gameobjects.player.AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0));
		return player;
	}

	private static void assertMessage(SM_SYSTEM_MESSAGE message, int expectedCode, String... expectedParams) {
		assertEquals(expectedCode, code(message), "消息码");
		Object[] params = params(message);
		assertEquals(expectedParams.length, params.length, "占位符个数");
		for (int i = 0; i < expectedParams.length; i++) {
			assertEquals(expectedParams[i], params[i], "占位符 " + i);
		}
	}

	private static int code(SM_SYSTEM_MESSAGE message) {
		return (Integer) readField(SM_SYSTEM_MESSAGE.class, message, "code");
	}

	private static Object[] params(SM_SYSTEM_MESSAGE message) {
		return (Object[]) readField(SM_SYSTEM_MESSAGE.class, message, "params");
	}

	private static List<Integer> systemMessageCodes(Player player) {
		List<Integer> codes = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<AionServerPacket> queue = (List<AionServerPacket>) readField(AionConnection.class,
			player.getClientConnection(), "sendMsgQueue");
		for (AionServerPacket packet : queue) {
			if (packet instanceof SM_SYSTEM_MESSAGE message) {
				codes.add(code(message));
			}
		}
		return codes;
	}

	/**
	 * 生产单例替换为只含合成行的表（其余 native 用例不依赖本行的 id）。
	 * The production instance pinned to a synthetic table.
	 */
	static {
		NativeQuestXmlTable synthetic = NativeQuestStartPortTestSupport.table(Map.of(SYNTHETIC, new NativeQuestXmlTable.QuestRow(
			SYNTHETIC, Map.of(
				"minlevel_permitted", List.of("50"),
				"maxlevel_permitted", List.of("200"),
				"race_permitted", List.of("pc_light"),
				"max_repeat_count", List.of("255")))), Map.of("refusal_hint_probe", SYNTHETIC));
		NativeQuestStartPortTestSupport.override(
			new NativeQuestStartPort(synthetic, NativeQuestStartPort.RestrictionBitmap.EMPTY));
	}

	private static Object readField(Class<?> declaringClass, Object target, String name) {
		try {
			var field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) {
		try {
			var field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
