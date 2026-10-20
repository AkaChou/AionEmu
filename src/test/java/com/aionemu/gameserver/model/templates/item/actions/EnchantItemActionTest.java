package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.world.knownlist.KnownList;
import com.aionemu.testutil.ConfigSnapshot;

/**
 * 手动完成物品使用任务，验证天烙与烙印强化边界及回包顺序，不启动线程池或游戏服务。
 * Manually completes item-use tasks to verify Estima/stigma enchant boundaries and packet order without services.
 */
class EnchantItemActionTest {

	private final ConfigSnapshot configSnapshot = ConfigSnapshot.of(EnchantsConfig.class,
		"MAX_EQUIPMENT_ENCHANT_LEVEL", "ENCHANT_ITEM_KINAH", "ENCHANT_ESTIMA",
		"ESTIMA_ENCHANT_SPEED", "ENCHANT_SPEED", "AUTHORIZE_SPEED", "ENCHANT_STIGMA",
		"MAX_STIGMA_ENCHANT_LEVEL", "STIGMA_ENCHANT_SPEED");
	private final EnchantItemAction action = new EnchantItemAction();

	@AfterEach
	void restoreConfig() {
		configSnapshot.restore();
	}

	@ParameterizedTest
	@ValueSource(ints = {187300002, 187300004, 187300005})
	void refusesEstimaAtTemplateCapWithoutEquipmentCapOrPriceChecks(int itemId) throws Exception {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 10;
		EnchantsConfig.ENCHANT_ITEM_KINAH = 1_000_000;
		TestPlayer player = player(34323);
		Item material = material(166040001);
		Item target = estima(itemId, 9);

		assertEquals(10, target.getItemTemplate().getMaxEnchantLevel());
		assertTrue(action.canAct(player, material, target));
		target.setEnchantLevel(10);
		assertFalse(action.canAct(player, material, target));
	}

	@Test
	void usesTemplateCapInsteadOfEquipmentCap() throws Exception {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 50;
		TestPlayer player = player(100_000);
		Item material = material(166040001);
		Item target = estima(187300002, 9);

		assertTrue(action.canAct(player, material, target));
		target.setEnchantLevel(10);
		assertFalse(action.canAct(player, material, target));
	}

	@Test
	void rejectsNonEstimaMaterialForEstimaTarget() throws Exception {
		TestPlayer player = player(100_000);
		Item target = estima(187300002, 9);

		assertFalse(action.canAct(player, item(167000001, ItemCategory.MANASTONE, 0), target));
	}

	@ParameterizedTest
	@ValueSource(ints = {166040001, 166040002, 166040003, 166040004, 166040005, 166040006, 166040007})
	void routesEveryWaterVariantDirectlyToEstimaEnchant(int materialId) throws Exception {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 10;
		EnchantsConfig.ENCHANT_ITEM_KINAH = 1_000_000;
		EnchantsConfig.ENCHANT_ESTIMA = 100;
		EnchantsConfig.ESTIMA_ENCHANT_SPEED = 5000;
		TestPlayer player = player(100_000);
		Item material = material(materialId);
		Item target = estima(187300002, 9);

		action.act(player, material, target, null, 1);

		assertEquals(1, player.controller.scheduledCount);
		assertEquals(5000, player.controller.delay);
		assertEquals(100_000 - 34323, player.inventory.getKinah());
		assertEquals(0, player.inventory.consumedStones);
		player.controller.complete();
		assertEquals(10, target.getEnchantLevel());
		assertFalse(target.isAmplified());
		assertEquals(1, player.inventory.consumedStones);
	}

	@Test
	void successfullyEnchantsToTemplateCapAndThenRefuses() throws Exception {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 50;
		EnchantsConfig.ENCHANT_ESTIMA = 100;
		TestPlayer player = player(100_000);
		Item material = material(166040001);
		Item target = estima(187300002, 9);

		action.act(player, material, target);
		player.controller.complete();

		assertEquals(10, target.getEnchantLevel());
		assertEquals(1, player.inventory.consumedStones);
		assertFalse(action.canAct(player, material, target));
	}

	@ParameterizedTest
	@CsvSource({"100, 0, 1", "100, 9, 10", "0, 0, 0", "0, 9, 0"})
	void usesConfiguredEstimaSuccessRateWithoutLevelAdjustment(float rate, int oldLevel, int expectedLevel)
		throws Exception {
		EnchantsConfig.ENCHANT_ESTIMA = rate;
		TestPlayer player = player(100_000);
		Item target = estima(187300002, oldLevel);

		action.act(player, material(166040001), target);
		player.controller.complete();

		assertEquals(expectedLevel, target.getEnchantLevel());
		assertEquals(1, player.inventory.consumedStones);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 750, 2400})
	void usesIndependentEstimaSpeedForAnimationAndCompletion(int castTime) throws Exception {
		EnchantsConfig.ENCHANT_ESTIMA = 100;
		EnchantsConfig.ESTIMA_ENCHANT_SPEED = castTime;
		EnchantsConfig.ENCHANT_SPEED = 777;
		EnchantsConfig.AUTHORIZE_SPEED = 888;
		TestPlayer player = player(100_000);
		Item material = material(166040001);
		Item target = estima(187300002, 2);
		RecordingTransport transport = recordPackets(player, material, target);

		action.act(player, material, target);

		assertEquals(castTime, player.controller.delay);
		assertEquals(List.of(castTime), transport.castTimes);
		assertEquals(2, target.getEnchantLevel());
		assertTrue(transport.events.isEmpty());
		player.controller.complete();
		assertEquals(3, target.getEnchantLevel());
		assertEquals(List.of("update:STATS_CHANGE:3", "end:1:3", "message"), transport.events);
	}

	@Test
	void refusesAtOrAboveCapWithoutChargingOrDowngrading() throws Exception {
		TestPlayer player = player(100_000);
		Item material = material(166040001);

		for (int level : new int[] {10, 20}) {
			Item target = estima(187300002, level);
			action.act(player, material, target);
			EnchantService.estimaEnchant(player, material, target);
			assertEquals(level, target.getEnchantLevel());
		}

		assertEquals(0, player.controller.scheduledCount);
		assertEquals(100_000, player.inventory.getKinah());
		assertEquals(0, player.inventory.consumedStones);
	}

	@Test
	void refusesEquippedEstimaAndInsufficientKinahWithoutCharging() throws Exception {
		TestPlayer player = player(100_000);
		Item material = material(166040001);
		Item target = estima(187300002, 9);
		target.setEquipped(true);

		assertFalse(action.canAct(player, material, target));
		EnchantService.estimaEnchant(player, material, target);
		assertEquals(100_000, player.inventory.getKinah());

		target.setEquipped(false);
		player.inventory.kinah = 34322;
		assertFalse(action.canAct(player, material, target));
		action.act(player, material, target);
		assertEquals(34322, player.inventory.getKinah());
		assertEquals(0, player.controller.scheduledCount);
		assertEquals(0, player.inventory.consumedStones);
	}

	@Test
	void leavesOrdinaryEquipmentSubjectToItsOwnCap() throws Exception {
		EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL = 10;
		EnchantsConfig.ENCHANT_ITEM_KINAH = 0;
		TestPlayer player = player(100_000);
		Item target = item(100000001, ItemCategory.SWORD, 9);
		Item material = material(166040001);

		assertTrue(action.canAct(player, material, target));
		target.setEnchantLevel(10);
		assertFalse(action.canAct(player, material, target));
	}

	@Test
	void refusesStigmaAtConfiguredCapAndCapsItAtOneHundred() throws Exception {
		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 15;
		TestPlayer player = player(100_000);
		Item material = item(166060001, ItemCategory.ENCHANTMENT_STIGMA, 0);
		Item target = item(140000001, ItemCategory.STIGMA, 15);

		assertFalse(new EnchantStigmaAction().canAct(player, material, target));

		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 500;

		assertEquals(100, EnchantService.getMaxStigmaEnchantLevel());
		target.setEnchantLevel(100);
		assertFalse(new EnchantStigmaAction().canAct(player, material, target));
		target.setEnchantLevel(99);
		assertTrue(new EnchantStigmaAction().canAct(player, material, target));
	}

	@Test
	void stigmaUsesConfiguredCastTime() throws Exception {
		EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL = 15;
		EnchantsConfig.STIGMA_ENCHANT_SPEED = 1800;
		EnchantsConfig.ENCHANT_STIGMA = 100;
		TestPlayer player = player(100_000);
		Item material = item(166060001, ItemCategory.ENCHANTMENT_STIGMA, 0);
		Item target = item(140000001, ItemCategory.STIGMA, 14);

		new EnchantStigmaAction().act(player, material, target);

		assertEquals(1800, player.controller.delay);
		player.controller.complete();
		assertEquals(15, target.getEnchantLevel());
	}

	@Test
	void rejectsMissingItemsBeforeDereferencingThem() throws Exception {
		Item material = material(166040001);
		assertFalse(action.canAct(null, material, null));
		assertFalse(action.canAct(null, null, estima(187300002, 10)));
	}

	@ParameterizedTest
	@CsvSource({"true, true, 2", "false, true, 2", "true, true, 3", "false, true, 3",
		"true, false, 3", "false, false, 3"})
	void refreshesEnchantAttributesBeforeOneCompletionEvent(boolean estima, boolean success, int oldLevel)
		throws Exception {
		EnchantsConfig.ENCHANT_ESTIMA = success ? 100 : 0;
		EnchantsConfig.ENCHANT_STIGMA = success ? 100 : -1;
		TestPlayer player = player(100_000);
		Item target = estima ? estima(187300002, oldLevel) : item(140000001, ItemCategory.STIGMA, oldLevel);
		Item material = estima ? material(166040001) : item(166060001, ItemCategory.ENCHANTMENT_STIGMA, 0);
		RecordingTransport transport = recordPackets(player, material, target);
		AbstractItemAction enchantAction = estima ? action : new EnchantStigmaAction();

		enchantAction.act(player, material, target);
		assertTrue(transport.events.isEmpty());
		player.controller.complete();

		int newLevel = success ? oldLevel + 1 : 0;
		assertEquals(newLevel, target.getEnchantLevel());
		assertEquals(List.of("update:STATS_CHANGE:" + newLevel,
			"end:" + (success ? 1 : 2) + ":" + newLevel, "message"), transport.events);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 3})
	void refreshesProtectedStigmaFailureBeforeCompletion(int oldLevel) throws Exception {
		EnchantsConfig.ENCHANT_STIGMA = -1;
		TestPlayer player = player(100_000);
		player.setEffectController(new StigmaProtectionController(player));
		Item target = item(140000001, ItemCategory.STIGMA, oldLevel);
		Item material = item(166060001, ItemCategory.ENCHANTMENT_STIGMA, 0);
		RecordingTransport transport = recordPackets(player, material, target);

		new EnchantStigmaAction().act(player, material, target);
		player.controller.complete();

		int newLevel = Math.max(0, oldLevel - 1);
		assertEquals(newLevel, target.getEnchantLevel());
		assertEquals(List.of("update:STATS_CHANGE:" + newLevel,
			"end:2:" + newLevel, "message"), transport.events);
	}

	private static RecordingTransport recordPackets(TestPlayer player, Item material, Item target) {
		List<AionServerPacket> packets = new ArrayList<>();
		RecordingTransport transport = new RecordingTransport(packets, material, target);
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setInheritedField(connection, AConnection.class, "guard", new Object());
		setInheritedField(connection, AConnection.class, "transport", transport);
		setInheritedField(connection, AionConnection.class, "sendMsgQueue", packets);
		player.setClientConnection(connection);
		return transport;
	}

	private static TestPlayer player(long kinah) {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.inventory = new TestStorage(kinah);
		player.controller = new CapturingController();
		player.observers = new ObserveController();
		player.setKnownlist(new KnownList(player));
		// 强化流程会把玩家 objectId 写进动画包；Objenesis 绕过了构造器，这里补上。
		// The enchant flow writes the player's object id into the usage animation; Objenesis skipped the
		// constructor, so supply it here.
		setInheritedField(player, AionObject.class, "objectId", 1);
		return player;
	}

	/**
	 * 沿继承链设置字段：{@link #setField} 只查声明类，{@code objectId} 声明在基类上。
	 * Sets a field declared on a superclass: {@link #setField} only looks at the target's own class.
	 */
	private static void setInheritedField(Object target, Class<?> declaring, String name, Object value) {
		try {
			Field field = declaring.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot set " + declaring.getSimpleName() + "." + name, e);
		}
	}

	private static Item material(int itemId) throws Exception {
		return item(itemId, itemId == 166040007 ? ItemCategory.ESTIMA : ItemCategory.ENCHANTMENT, 0);
	}

	private static Item estima(int itemId, int level) throws Exception {
		return item(itemId, ItemCategory.ESTIMA, level);
	}

	private static Item item(int itemId, ItemCategory category, int level) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "itemId", itemId);
		setField(template, "category", category);
		setField(template, "itemQuality", ItemQuality.MYTHIC);
		setField(template, "maxEnchant", 10);
		Item item = new Item(itemId, template);
		item.setEnchantLevel(level);
		return item;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * 只提供强化所需的玩家边界，避免初始化角色、数据库和世界服务。
	 * Supplies only enchant-related player boundaries without character, database or world-service setup.
	 */
	private static final class TestPlayer extends Player {
		private TestStorage inventory;
		private CapturingController controller;
		private ObserveController observers;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public boolean isGM() {
			return false;
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
	}

	/**
	 * 在内存中记录费用和材料消耗，不执行持久化或发包。
	 * Records costs and material consumption in memory without persistence or packet sends.
	 */
	private static final class TestStorage extends PlayerStorage {
		private long kinah;
		private long consumedStones;

		private TestStorage(long kinah) {
			super(StorageType.CUBE);
			this.kinah = kinah;
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
		public boolean decreaseByObjectId(int objectId, long count) {
			consumedStones += count;
			return true;
		}
	}

	/**
	 * 仅模拟烙印保护效果，不加载技能模板或调度效果任务。
	 * Simulates only stigma protection without loading skill templates or scheduling effects.
	 */
	private static final class StigmaProtectionController extends PlayerEffectController {
		private StigmaProtectionController(Player player) {
			super(player);
		}

		@Override
		public boolean hasEffectById(int effectId) {
			return effectId == 147141;
		}
	}

	/**
	 * 入队时快照等级和包顺序，避免可变物品引用在事后掩盖先通知、后写入的错误。
	 * Snapshots levels and packet order at enqueue time so later item mutations cannot hide premature completion.
	 */
	private static final class RecordingTransport implements ConnectionTransport {
		private final List<AionServerPacket> packets;
		private final Item material;
		private final Item target;
		private final List<String> events = new ArrayList<>();
		private final List<Integer> castTimes = new ArrayList<>();

		private RecordingTransport(List<AionServerPacket> packets, Item material, Item target) {
			this.packets = packets;
			this.material = material;
			this.target = target;
		}

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		/**
		 * 同步观察真实发送队列，不执行网络写入。
		 * Observes the real send queue synchronously without network writes.
		 */
		@Override
		public void enableWriteInterest() {
			AionServerPacket packet = packets.getLast();
			try {
				if (packet instanceof SM_INVENTORY_UPDATE_ITEM) {
					assertEquals(target, packetField(packet, "item"));
					events.add("update:" + packetField(packet, "updateType") + ":" + target.getEnchantLevel());
				} else if (packet instanceof SM_ITEM_USAGE_ANIMATION) {
					assertEquals(material.getObjectId(), packetField(packet, "itemObjId"));
					assertEquals(material.getItemId(), packetField(packet, "itemId"));
					if ((int) packetField(packet, "end") == 0) {
						castTimes.add((int) packetField(packet, "time"));
					} else {
						events.add("end:" + packetField(packet, "end") + ":" + target.getEnchantLevel());
					}
				} else if (packet instanceof SM_SYSTEM_MESSAGE) {
					events.add("message");
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Cannot inspect enchant result packet", e);
			}
		}

		private static Object packetField(Object packet, String name) throws ReflectiveOperationException {
			Field field = packet.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(packet);
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}

	/**
	 * 捕获物品使用任务供测试手动完成，不访问全局线程池。
	 * Captures item-use tasks for manual completion without accessing the global thread pool.
	 */
	private static final class CapturingController extends PlayerController {
		private FutureTask<Void> pending;
		private int scheduledCount;
		private long delay;

		/**
		 * 记录任务和延迟，由测试显式触发完成。
		 * Records the task and delay for explicit completion by the test.
		 */
		@Override
		public Future<?> scheduleTask(TaskId taskId, Runnable task, long delay) {
			assertEquals(TaskId.ITEM_USE, taskId);
			pending = new FutureTask<>(task, null);
			scheduledCount++;
			this.delay = delay;
			return pending;
		}

		private void complete() throws Exception {
			assertEquals(1, scheduledCount);
			pending.run();
			pending.get();
		}
	}
}
