package com.aionemu.gameserver.services.toypet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.MinionData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.minion.MinionBuff;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;

class MinionServiceTest {
	@Test
	void keepsPersistedMinionObjectId() {
		MinionCommonData minion = new MinionCommonData(123_456, 980010, 42, "Minion", "D", 1, 10);

		assertEquals(123_456, minion.getObjectId());
	}

	@Test
	void spawnsWithTheObjectIdButResolvesTheTemplateId() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		String spawner = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java"));

		assertTrue(service.contains("VisibleObjectSpawner.spawnMinion(player, minionObjId)"));
		assertTrue(service.contains("addSkillWithoutSave(player, skillId, 1)"));
		assertTrue(spawner.contains("getMinion(minionObjectId)"));
		assertTrue(spawner.contains("int minionId = minionCommonData.getMinionId()"));
	}

	@Test
	void persistsInitialGrowthPoints() throws Exception {
		String dao = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/persistence/dao/impl/PlayerMinionDaoImpl.java"));
		String mapper = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/persistence/mapper/PlayerMinionMapper.java"));

		assertTrue(mapper.contains("INSERT INTO player_minions (player_id, object_id, minion_id, name, grade, level, growthpoints"));
		assertTrue(dao.contains("getMinionGrowthPoint()"));
	}

	@Test
	void keepsMinionBuffsSeparateBetweenPlayers() {
		MinionData original = DataManager.MINION_DATA;
		try {
			DataManager.MINION_DATA = new MinionData() {
				@Override
				public MinionTemplate getMinionTemplate(int minionId) {
					return minionWithMaxHp(minionId == 1 ? 50 : 100);
				}
			};
			Player first = new ObjenesisStd().newInstance(TestPlayer.class);
			Player second = new ObjenesisStd().newInstance(TestPlayer.class);
			first.setGameStats(new QuietPlayerGameStats(first));
			second.setGameStats(new QuietPlayerGameStats(second));

			MinionBuff buff = new MinionBuff();
			buff.apply(first, 1);
			buff.apply(second, 2);

			assertEquals(50, first.getGameStats().getStat(StatEnum.MAXHP, 0).getCurrent());
			assertEquals(100, second.getGameStats().getStat(StatEnum.MAXHP, 0).getCurrent());
		} finally {
			DataManager.MINION_DATA = original;
		}
	}

	private static MinionTemplate minionWithMaxHp(int maxHp) {
		return new MinionTemplate() {
			@Override
			public List<StatFunction> getModifiers() {
				return List.of(new StatFunction(StatEnum.MAXHP, maxHp, true));
			}
		};
	}

	private static final class QuietPlayerGameStats extends PlayerGameStats {
		private QuietPlayerGameStats(Player owner) {
			super(owner);
		}

		@Override
		protected void onStatsChange() {
		}
	}

	private static final class TestPlayer extends Player {
		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public byte isPlayer() {
			return 1;
		}
	}

	@Test
	void rejectsMinionsAtAndAboveTheLimit() {
		assertFalse(MinionService.isMinionLimitReached(199));
		assertTrue(MinionService.isMinionLimitReached(200));
		assertTrue(MinionService.isMinionLimitReached(256));
	}

	@Test
	void chargesOnlyForMissingSkillPoints() {
		assertEquals(1_000_000, MinionService.chargePrice(0));
		assertEquals(960, MinionService.chargePrice(49_952));
		assertEquals(0, MinionService.chargePrice(50_000));
	}

	@Test
	void acceptsOnlyTheSixFamiliarDopingSlots() {
		assertFalse(MinionService.isDopingSlot(-1));
		assertTrue(MinionService.isDopingSlot(0));
		assertTrue(MinionService.isDopingSlot(5));
		assertFalse(MinionService.isDopingSlot(6));
	}

	@Test
	void recognizesTheQuestContractAndUsesItsRetailPool() {
		Item contract = new Item(1, new QuestContractTemplate());

		assertTrue(MinionService.isMinionContract(contract));
		assertEquals(980020, MinionService.questContractMinionId(true));
		assertEquals(980030, MinionService.questContractMinionId(false));
	}

	@Test
	void recognizesBothLevelTenTutorialTicketsAsMinionContracts() {
		assertTrue(MinionService.isMinionContract(new Item(1, new QuestContractTemplate(190080020))));
		assertTrue(MinionService.isMinionContract(new Item(2, new QuestContractTemplate(190080021))));
	}

	@Test
	void acceptsOnlyImplementedMinionContracts() {
		assertTrue(MinionService.isSupportedMinionContract(190080005));
		assertTrue(MinionService.isSupportedMinionContract(190080012));
		assertTrue(MinionService.isSupportedMinionContract(190080013));
		assertTrue(MinionService.isSupportedMinionContract(190080020));
		assertTrue(MinionService.isSupportedMinionContract(190080021));
		assertTrue(MinionService.isSupportedMinionContract(190089999));
		assertFalse(MinionService.isSupportedMinionContract(190080004));
		assertFalse(MinionService.isSupportedMinionContract(190080014));
	}

	@Test
	void advancesTypedItemPlayOnlyAfterTheMinionWasCreated() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		int addMinion = service.indexOf("addNewMinion(player, minionId");
		int failedCreation = service.indexOf("if (addNewMinion == null)", addMinion);
		int questEvent = service.indexOf("onItemPlayCompletedEvent(player, item.getItemId())", failedCreation);

		assertTrue(addMinion >= 0);
		assertTrue(failedCreation > addMinion);
		assertTrue(questEvent > failedCreation);
	}

	@Test
	void contractChainMatchesTheRetailPacketSequence() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		int addMinion = service.indexOf("public void addMinion(final Player player, final int itemObjId)");
		int methodEnd = service.indexOf("private static int rollMinionExcludingGrades", addMinion);
		int decrease = service.indexOf("decreaseByObjectId(itemObjId, 1)", addMinion);
		// 实测定案：0x16A 列表新增 + 0x2EA type=0 记录 + 携带真实 itemId 的 end=1，三者齐备形象才显示
		// ——end 帧的 itemId 是客户端完成尾部查模板串的键，置 0 则 UI 事件不入队、形象不渲染。
		int listAdd = service.indexOf("new SM_MINIONS(1, addNewMinion, 0)", decrease);
		int familiarRecord = service.indexOf("new SM_FAMILIAR(0, addNewMinion)", listAdd);
		int successFrame = service.indexOf("item.getItemId(), 0, 1)", familiarRecord);
		int lambdaEnd = service.indexOf("}, 1500);", successFrame);

		assertTrue(addMinion >= 0);
		assertTrue(methodEnd > addMinion);
		assertTrue(decrease > addMinion);
		assertTrue(listAdd > decrease);
		assertTrue(familiarRecord > listAdd);
		assertTrue(successFrame > familiarRecord);
		assertTrue(lambdaEnd > successFrame);
		assertFalse(service.substring(successFrame, lambdaEnd).contains("SM_ITEM_USAGE_ANIMATION"));

		// 全文件不存在任何 end=3 停止帧。
		assertFalse(service.contains(", 0, 3)"));
		assertFalse(service.contains("broadcastContractUseEnd"));

		// 失败收尾对齐真端：0x2EA type=4 补偿通知在前、end=2（真实 itemId）在后。
		int failure = service.indexOf("private static void broadcastContractFailure(");
		assertTrue(failure > 0);
		assertTrue(service.indexOf("new SM_FAMILIAR(4)", failure) > failure);
		assertTrue(service.indexOf("itemObjId, itemId, 0, 2)", failure) > failure);
	}

	@Test
	void familiarRecordMatchesTheRetailPacketWriterLayout() throws Exception {
		// 真端 FUN_140a50d10 + 构造器 FUN_140a37530 交叉定案的 payload 序：type, 0, IdFactory id,
		// DBId, masterId, minionId, name, birthday, 0(+0x74 无写点), 0(+0x78), 6×doping, lock。
		// 等级与成长值不在包内（客户端按 minionId 查本地模板）。
		String packet = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_FAMILIAR.java"));
		List<String> order = List.of(
				"writeH(type)",
				"writeQ(0)",
				"writeD(minionCommonData.getObjectId())",
				"writeQ(minionCommonData.getObjectId()",
				"writeD(minionCommonData.getMasterObjectId())",
				"writeD(minionCommonData.getMinionId())",
				"writeS(minionCommonData.getName())",
				"writeQ(minionCommonData.getBirthday()",
				"writeD(0)",
				"writeC(0)",
				"writeD(minionCommonData.getDopingBag() == null ? 0 : minionCommonData.getDopingBag().getItem(slot))",
				"writeC(minionCommonData.isLock() ? 1 : 0)");
		int prev = -1;
		for (String fragment : order) {
			int at = packet.indexOf(fragment, prev + 1);
			assertTrue(at > prev, fragment);
			prev = at;
		}
	}

	private static final class QuestContractTemplate extends ItemTemplate {
		private final int templateId;

		private QuestContractTemplate() {
			this(190080012);
		}

		private QuestContractTemplate(int templateId) {
			this.templateId = templateId;
		}

		@Override
		public int getTemplateId() {
			return templateId;
		}

		@Override
		public boolean getMinionTicket() {
			return true;
		}

		@Override
		public boolean isMinionCashContract() {
			return true;
		}
	}

	@Test
	void ignoresEmptyMaterialSlotsAndRejectsDuplicates() {
		assertEquals(Set.of(101, 202), MinionService.uniqueNonZeroIds(List.of(101, 0, 202, 0)));
		assertTrue(MinionService.uniqueNonZeroIds(List.of(101, 0, 101)).isEmpty());
	}

	@Test
	void extendsMinionFunctionFromTheLaterOfNowAndCurrentExpiry() {
		long now = 1_700_000_000_000L;
		long thirtyDays = 2_592_000_000L;

		assertEquals(now + thirtyDays, MinionService.nextMinionFunctionExpiry(null, now));
		assertEquals(now + thirtyDays, MinionService.nextMinionFunctionExpiry(new Timestamp(now - 1), now));
		assertEquals(now + 60_000 + thirtyDays,
				MinionService.nextMinionFunctionExpiry(new Timestamp(now + 60_000), now));
	}

	@Test
	void sendsOnlyActiveFunctionExpiryAsUnixSeconds() {
		long now = 1_700_000_000_000L;

		assertEquals(1_700_000_060, MinionService.activeMinionFunctionExpiry(new Timestamp(now + 60_000), now));
		assertEquals(0, MinionService.activeMinionFunctionExpiry(new Timestamp(now), now));
		assertEquals(0, MinionService.activeMinionFunctionExpiry(null, now));
	}

	@Test
	void refundsRemainingFunctionTimeAfterTenPercentFee() {
		long now = 1_700_000_000_000L;
		long thirtyDays = 2_592_000_000L;

		assertEquals(22_500_000, MinionService.minionFunctionRefund(new Timestamp(now + thirtyDays), now));
		assertEquals(11_250_000, MinionService.minionFunctionRefund(new Timestamp(now + thirtyDays / 2), now));
		assertEquals(45_000_000, MinionService.minionFunctionRefund(new Timestamp(now + thirtyDays * 2), now));
		assertEquals(0, MinionService.minionFunctionRefund(new Timestamp(now), now));
		assertEquals(0, MinionService.minionFunctionRefund(null, now));
	}

	@Test
	void syncsInactiveFunctionAsStoppedAndHandlesStopRequests() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		String login = service.substring(service.indexOf("public void onPlayerLogin"), service.indexOf("public void addMinion"));
		String deactivation = service.substring(service.indexOf("public void deactivateMinionFunction"),
				service.indexOf("static long nextMinionFunctionExpiry"));

		assertTrue(login.contains("functionExpiry == 0 ? new SM_MINIONS(10) : new SM_MINIONS(9, functionExpiry)"));
		assertTrue(deactivation.contains("setMinionFunctionTime(null)"));
		assertTrue(deactivation.contains("new SM_MINIONS(10)"));
	}

	@Test
	void reportsActivationPaymentFailureAndPersistsSuccessfulExpiry() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		String activation = service.substring(service.indexOf("public void activateMinionFunction"),
				service.indexOf("static long nextMinionFunctionExpiry"));

		assertTrue(activation.contains("STR_FAMILIAR_MSG_FFUNCTION_USE_FAIL_BY_GOLD"));
		assertTrue(activation.contains("playerDAO.storePlayer(player)"));
	}

	@Test
	void acknowledgesEveryDopingUseAsABuffOperation() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		String buffPlayer = service.substring(service.indexOf("public void buffPlayer"), service.indexOf("public void relocateDoping"));

		assertEquals(3, buffPlayer.split("new SM_MINIONS\\(8, 3", -1).length - 1);
		assertFalse(buffPlayer.contains("new SM_MINIONS(8, 0"));
	}

	@Test
	void consumesConfiguredEnergyBeforeApplyingSkillActions() throws Exception {
		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		assertTrue(service.contains("int energyCost = minionSkill.getEnergyCost()"));
		assertTrue(service.contains("player.setMinionSkillPoints(currentSkillPoints - energyCost)"));
		assertTrue(service.contains("setMinionSkillPointsAutoCharge(autoCharge)"));

		String skill = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/skillengine/model/Skill.java"));
		int usageCheck = skill.indexOf("if (!preUsageCheck())");
		int energyUse = skill.indexOf("consumeMinionSkillPoints", usageCheck);
		int actions = skill.indexOf("Actions skillActions", energyUse);
		assertTrue(usageCheck < energyUse && energyUse < actions);
	}
}
