package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objenesis.ObjenesisStd;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.actions.EnchantItemAction;
import com.aionemu.gameserver.model.templates.item.actions.EnchantStigmaAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.item.actions.TemperingAction;
import com.aionemu.gameserver.world.knownlist.KnownList;
import com.aionemu.testutil.ConfigSnapshot;

/**
 * 验证 GM 成功率提示覆盖各升级入口且不改变判定规则，不启动服务器或执行异步任务。
 * Verifies GM chance notices across upgrade entry points without changing rolls or starting services/tasks.
 */
class EnchantChanceNotificationTest {

	private final ConfigSnapshot enchantConfig = ConfigSnapshot.of(EnchantsConfig.class,
		"ENCHANT_ITEM", "ENCHANT_ESTIMA", "ENCHANT_STIGMA", "ENCHANT_BRACELET", "ENCHANT_PLUME",
		"ENCHANT_ACCESSORY", "SOCKET_MANASTONE");
	private final ConfigSnapshot adminConfig = ConfigSnapshot.of(AdminConfig.class, "GM_LEVEL");

	@BeforeEach
	void configureMessagesAndGmThreshold() {
		ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
		messages.setBasename("messages");
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
		I18n.setMessageSource(messages);
		I18n.applyCountryCode(1);
		AdminConfig.GM_LEVEL = 3;
	}

	@AfterEach
	void restoreGlobals() {
		enchantConfig.restore();
		adminConfig.restore();
		I18n.setMessageSource(null);
		I18n.applyCountryCode(1);
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void suppressesNoticesBelowConfiguredGmThresholdEvenWithPositiveAccessLevel() throws Exception {
		TestPlayer player = player(false);

		EnchantService.sendUpgradeChance(player, null, null, 75);
		EnchantService.sendUpgradeChance(null, null, null, 75);

		assertTrue(player.getAccessLevel() > 0);
		assertTrue(player.messages.isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"150, 100.00", "-5, 0.00", "12.345, 12.35"})
	void localizesAndBoundsOnlyDisplayedPercentages(float rate, String displayed) throws Exception {
		TestPlayer player = player(true);
		I18n.applyCountryCode(5);

		EnchantService.sendUpgradeChance(player, item(187300002, ItemCategory.ESTIMA),
			"item.upgrade.estima", rate);

		assertEquals(List.of("[GM] 天烙之石强化 [item:187300002]：本次成功率：" + displayed + "%\u3002"),
			player.messages);
	}

	@Test
	void distinguishesNormalEquipmentChanceFromExistingGmGuarantee() throws Exception {
		EnchantsConfig.ENCHANT_ITEM = 40;
		TestPlayer player = player(true);

		assertTrue(EnchantService.enchantItem(player, item(166000001, ItemCategory.ENCHANTMENT),
			item(100000001, ItemCategory.SWORD), null));

		assertEquals(List.of("[GM] Equipment enchanting [item:100000001]: Success chance for this attempt: "
			+ "100.00% (normal chance: 37.00%; GM guaranteed success)."), player.messages);
	}

	@Test
	void preservesNonGmEquipmentFailureWithoutLeakingDiagnosticMessages() throws Exception {
		EnchantsConfig.ENCHANT_ITEM = -100;
		TestPlayer player = player(false);

		assertFalse(EnchantService.enchantItem(player, item(166000001, ItemCategory.ENCHANTMENT),
			item(100000001, ItemCategory.SWORD), null));

		assertTrue(player.messages.isEmpty());
	}

	@Test
	void includesSupplementBonusInNormalEquipmentChance() throws Exception {
		EnchantsConfig.ENCHANT_ITEM = 40;
		TestPlayer player = player(true);
		Item stone = item(166000001, ItemCategory.ENCHANTMENT);
		stone.getItemTemplate().setItemQuality(ItemQuality.COMMON);
		stone.getItemTemplate().setActions(new ItemActions());
		Item supplement = item(166100001, ItemCategory.NONE);
		EnchantItemAction supplementAction = new EnchantItemAction();
		setField(supplementAction, EnchantItemAction.class, "chance", 5f);
		ItemActions actions = new ItemActions();
		actions.getItemActions().add(supplementAction);
		supplement.getItemTemplate().setActions(actions);

		assertTrue(EnchantService.enchantItem(player, stone, item(100000001, ItemCategory.SWORD), supplement));

		assertEquals(List.of("[GM] Equipment enchanting [item:100000001]: Success chance for this attempt: "
			+ "100.00% (normal chance: 47.00%; GM guaranteed success)."), player.messages);
	}

	@Test
	void includesAmplificationBonusInNormalEquipmentChance() throws Exception {
		EnchantsConfig.ENCHANT_ITEM = -100;
		TestPlayer player = player(true);
		Item target = item(100000001, ItemCategory.SWORD);
		target.setEnchantLevel(10);
		target.setAmplification(true);

		assertTrue(EnchantService.enchantItem(player, item(166000001, ItemCategory.ENCHANTMENT_AMPLIFICATION),
			target, null));

		assertEquals(List.of("[GM] Equipment enchanting [item:100000001]: Success chance for this attempt: "
			+ "100.00% (normal chance: 61.00%; GM guaranteed success)."), player.messages);
	}

	@ParameterizedTest
	@CsvSource({"true, 0", "true, 7", "true, 9", "false, 7"})
	void reportsConfiguredEstimaChanceAtEveryLevelOnlyToGms(boolean gm, int level) throws Exception {
		EnchantsConfig.ENCHANT_ESTIMA = 80;
		TestPlayer player = player(gm);
		Item target = item(187300002, ItemCategory.ESTIMA);
		target.setEnchantLevel(level);

		new EnchantItemAction().act(player, item(166040001, ItemCategory.ENCHANTMENT), target);

		assertEquals(gm ? List.of("[GM] Estima enchanting [item:187300002]: "
			+ "Success chance for this attempt: 80.00%.") : List.of(), player.messages);
		assertEquals(1, player.controller.scheduledCount);
		assertEquals(level, target.getEnchantLevel());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void reportsStigmaChanceOnceOnlyToGms(boolean gm) throws Exception {
		EnchantsConfig.ENCHANT_STIGMA = 42.5f;
		TestPlayer player = player(gm);

		new EnchantStigmaAction().act(player, item(166060001, ItemCategory.ENCHANTMENT_STIGMA),
			item(140000001, ItemCategory.STIGMA));

		assertEquals(gm ? List.of("[GM] Stigma enchanting [item:140000001]: "
			+ "Success chance for this attempt: 42.50%.") : List.of(), player.messages);
		assertEquals(1, player.controller.scheduledCount);
	}

	@Test
	void doesNotReportChanceWhenEstimaOrStigmaEnchantIsRefused() throws Exception {
		TestPlayer player = player(true);
		Item estima = item(187300002, ItemCategory.ESTIMA);
		estima.setEnchantLevel(10);
		Item stigma = item(140000001, ItemCategory.STIGMA);
		stigma.setEnchantLevel(10);

		EnchantService.estimaEnchant(player, item(166040001, ItemCategory.ENCHANTMENT), estima);
		new EnchantStigmaAction().act(player, item(166060001, ItemCategory.ENCHANTMENT_STIGMA), stigma);

		assertTrue(player.messages.isEmpty());
		assertEquals(0, player.controller.scheduledCount);
	}

	@ParameterizedTest
	@CsvSource({"BRACELET, Bracelet, 18.00", "PLUME, Plume, 27.00", "EARRINGS, Accessory, 36.00"})
	void reportsTheSelectedTemperingCategoryRate(ItemCategory category, String label, String displayed)
		throws Exception {
		EnchantsConfig.ENCHANT_BRACELET = 18;
		EnchantsConfig.ENCHANT_PLUME = 27;
		EnchantsConfig.ENCHANT_ACCESSORY = 36;
		TestPlayer player = player(true);

		new TemperingAction().isTemperingSuccess(player, item(120000001, category));

		assertEquals(List.of("[GM] " + label + " tempering [item:120000001]: "
			+ "Success chance for this attempt: " + displayed + "%."), player.messages);
	}

	@ParameterizedTest
	@EnumSource(value = ItemCategory.class, names = {"BRACELET", "PLUME", "EARRINGS"})
	void doesNotIntroduceAGmGuaranteeForTempering(ItemCategory category) throws Exception {
		EnchantsConfig.ENCHANT_BRACELET = 0;
		EnchantsConfig.ENCHANT_PLUME = 0;
		EnchantsConfig.ENCHANT_ACCESSORY = 0;
		TestPlayer player = player(true);

		assertFalse(new TemperingAction().isTemperingSuccess(player, item(120000001, category)));

		assertEquals(1, player.messages.size());
		assertTrue(player.messages.getFirst().endsWith("Success chance for this attempt: 0.00%."));
	}

	@Test
	void suppressesTemperingNoticesForNonGmsAndInvalidCategories() throws Exception {
		EnchantsConfig.ENCHANT_PLUME = 100;
		TestPlayer ordinary = player(false);
		TestPlayer gm = player(true);

		assertTrue(new TemperingAction().isTemperingSuccess(ordinary, item(187000001, ItemCategory.PLUME)));
		assertFalse(new TemperingAction().isTemperingSuccess(gm, item(100000001, ItemCategory.SWORD)));

		assertTrue(ordinary.messages.isEmpty());
		assertTrue(gm.messages.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void includesQualityAndLevelBonusesInManastoneChance(boolean gm) throws Exception {
		EnchantsConfig.SOCKET_MANASTONE = 30;
		TestPlayer player = player(gm);
		Item stone = item(167000001, ItemCategory.NONE);
		stone.getItemTemplate().setItemQuality(ItemQuality.COMMON);

		EnchantService.socketManastone(player, stone, item(100000001, ItemCategory.SWORD), null, 1);

		assertEquals(gm ? List.of("[GM] Manastone socketing [item:100000001]: "
			+ "Success chance for this attempt: 60.71%.") : List.of(), player.messages);
	}

	@Test
	void doesNotReportChanceWhenManastoneLevelIsTooHigh() throws Exception {
		TestPlayer player = player(true);
		Item stone = item(167000001, ItemCategory.NONE);
		stone.getItemTemplate().setLevel(80);

		assertFalse(EnchantService.socketManastone(player, stone, item(100000001, ItemCategory.SWORD), null, 1));

		assertTrue(player.messages.isEmpty());
	}

	private static Item item(int id, ItemCategory category) {
		ItemTemplate template = new ItemTemplate();
		template.setItemId(id);
		template.setCategory(category);
		template.setItemQuality(ItemQuality.UNIQUE);
		template.setLevel(50);
		template.setMaxEnchant(10);
		template.setManastoneSlots(6);
		template.setEquipmentType(category == ItemCategory.SWORD ? EquipType.WEAPON : EquipType.NONE);
		return new Item(id, template);
	}

	private static TestPlayer player(boolean gm) throws ReflectiveOperationException {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.accessLevel = (byte) (gm ? 3 : 1);
		player.messages = new ArrayList<>();
		player.inventory = new TestStorage();
		player.controller = new CapturingController();
		player.observers = new ObserveController();
		player.setKnownlist(new KnownList(player));
		setField(player, AionObject.class, "objectId", 1);
		return player;
	}

	private static void setField(Object target, Class<?> declaringClass, String name, Object value)
		throws ReflectiveOperationException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * 捕获玩家自己的聊天提示，使用真实 GM 权限判定且不初始化世界或网络连接。
	 * Captures the player's own chat notices using the real GM threshold without world/network initialization.
	 */
	private static final class TestPlayer extends Player {
		private byte accessLevel;
		private List<String> messages;
		private TestStorage inventory;
		private CapturingController controller;
		private ObserveController observers;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public byte getAccessLevel() {
			return accessLevel;
		}

		@Override
		public Storage getInventory() {
			return inventory;
		}

		@Override
		public PlayerController getController() {
			return controller;
		}

		@Override
		public ObserveController getObserveController() {
			return observers;
		}

		/**
		 * 仅记录本人聊天内容，不执行网络发送。
		 * Records only this player's chat text without network sends.
		 */
		@Override
		public void sendMessage(String message) {
			messages.add(message);
		}
	}

	/**
	 * 为概率计算提供足够材料和费用，不访问持久化或发送物品数据包。
	 * Supplies sufficient materials and kinah for chance calculations without persistence or item packets.
	 */
	private static final class TestStorage extends PlayerStorage {
		private long kinah = 100_000;

		private TestStorage() {
			super(StorageType.CUBE);
		}

		@Override
		public long getKinah() {
			return kinah;
		}

		@Override
		public void decreaseKinah(long amount) {
			kinah -= amount;
		}

		@Override
		public long getItemCountByItemId(int itemId) {
			return 1000;
		}

		@Override
		public boolean decreaseByObjectId(int itemObjId, long count) {
			return true;
		}
	}

	/**
	 * 记录升级施法任务而不执行它，保证提示测试不运行后台调度器。
	 * Records upgrade cast tasks without executing them so notice tests never run a background scheduler.
	 */
	private static final class CapturingController extends PlayerController {
		private int scheduledCount;

		/**
		 * 返回未执行的任务句柄，不提交到线程池。
		 * Returns an unexecuted task handle without submitting it to a thread pool.
		 */
		@Override
		public Future<?> scheduleTask(TaskId taskId, Runnable task, long delay) {
			assertEquals(TaskId.ITEM_USE, taskId);
			scheduledCount++;
			return new FutureTask<>(task, null);
		}
	}
}
