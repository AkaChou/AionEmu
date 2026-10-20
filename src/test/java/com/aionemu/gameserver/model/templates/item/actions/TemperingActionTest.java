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
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.testutil.ConfigSnapshot;
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
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.world.knownlist.KnownList;

class TemperingActionTest {

	private final ConfigSnapshot enchantConfigSnapshot = ConfigSnapshot.of(EnchantsConfig.class,
		"ENCHANT_PLUME", "ENCHANT_BRACELET", "ENCHANT_ACCESSORY", "AUTHORIZE_SPEED",
		"MAX_AUTHORIZE_ACCESSORY_LEVEL", "MAX_AUTHORIZE_PLUME_LEVEL", "MAX_AUTHORIZE_BRACELET_LEVEL");

	private final TemperingAction action = new TemperingAction();

	@AfterEach
	void restoreConfig() {
		enchantConfigSnapshot.restore();
	}

	@Test
	void usesEachSlotRateForItsOwnCategory() throws Exception {
		EnchantsConfig.ENCHANT_ACCESSORY = 100;
		EnchantsConfig.ENCHANT_PLUME = 100;
		EnchantsConfig.ENCHANT_BRACELET = 0;

		assertTrue(action.isTemperingSuccess(null, item(ItemCategory.EARRINGS)));
		assertTrue(action.isTemperingSuccess(null, item(ItemCategory.PLUME)));
		assertFalse(action.isTemperingSuccess(null, item(ItemCategory.BRACELET)));
	}

	@Test
	void allowsConfiguredAuthorizeCapAboveTemplateLimit() throws Exception {
		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 50;
		TestPlayer player = player();
		Item material = item(166030001, ItemCategory.TEMPERING);
		Item target = item(120000001, ItemCategory.EARRINGS);
		setField(target.getItemTemplate(), "maxAuthorize", 10);
		target.setAuthorize(10);

		assertTrue(action.canAct(player, material, target));

		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 10;
		assertFalse(action.canAct(player, material, target));
	}

	@Test
	void refusesNonAccessoriesInsteadOfUsingTheAccessoryRate() throws Exception {
		EnchantsConfig.ENCHANT_ACCESSORY = 100;

		assertFalse(action.isTemperingSuccess(null, item(ItemCategory.SWORD)));
	}

	@ParameterizedTest
	@CsvSource({
		"true, 2, 3, 1",
		"false, 3, 0, 2"
	})
	void refreshesAuthorizeBeforeCompletionAndResultMessage(boolean success, int oldLevel, int newLevel, int end)
		throws Exception {
		EnchantsConfig.ENCHANT_ACCESSORY = success ? 100 : -1;
		EnchantsConfig.AUTHORIZE_SPEED = 500;
		EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL = 50;
		TestPlayer player = player();
		Item material = item(166030001, ItemCategory.TEMPERING);
		Item target = item(120000001, ItemCategory.EARRINGS);
		target.setAuthorize(oldLevel);
		RecordingTransport transport = recordPackets(player, material, target);

		action.act(player, material, target);
		assertTrue(transport.events.isEmpty());
		player.controller.complete();

		assertEquals(newLevel, target.getAuthorize());
		assertEquals(List.of("update:STATS_CHANGE:" + newLevel, "end:" + end + ":" + newLevel, "message"),
			transport.events);
	}

	@Test
	void refreshesAuthorizeToZeroBeforeCompletionAndFailureMessage() throws Exception {
		EnchantsConfig.ENCHANT_BRACELET = -1;
		EnchantsConfig.AUTHORIZE_SPEED = 500;
		EnchantsConfig.MAX_AUTHORIZE_BRACELET_LEVEL = 30;
		TestPlayer player = player();
		Item material = item(166030001, ItemCategory.TEMPERING);
		Item target = item(120000002, ItemCategory.BRACELET);
		target.setAuthorize(4);
		RecordingTransport transport = recordPackets(player, material, target);

		action.act(player, material, target);
		player.controller.complete();

		assertEquals(0, target.getAuthorize());
		assertEquals(List.of("update:STATS_CHANGE:0", "end:2:0", "message"), transport.events);
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

	private static TestPlayer player() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.inventory = new TestStorage();
		player.controller = new CapturingController();
		player.observers = new ObserveController();
		player.setKnownlist(new KnownList(player));
		setInheritedField(player, AionObject.class, "objectId", 1);
		return player;
	}

	private static Item item(ItemCategory category) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "category", category);
		setField(template, "maxAuthorize", 255);
		return new Item(1, template);
	}

	private static Item item(int itemId, ItemCategory category) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "itemId", itemId);
		setField(template, "category", category);
		setField(template, "maxAuthorize", 255);
		return new Item(itemId, template);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void setInheritedField(Object target, Class<?> declaring, String name, Object value) {
		try {
			Field field = declaring.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot set " + declaring.getSimpleName() + "." + name, e);
		}
	}

	private static final class TestPlayer extends Player {
		private TestStorage inventory;
		private CapturingController controller;
		private ObserveController observers;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public Storage getInventory() {
			return inventory;
		}

		@Override
		public boolean isGM() {
			return false;
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

	private static final class TestStorage extends PlayerStorage {
		private TestStorage() {
			super(StorageType.CUBE);
		}

		@Override
		public boolean decreaseByItemId(int itemId, long count) {
			return true;
		}
	}

	private static final class RecordingTransport implements ConnectionTransport {
		private final List<AionServerPacket> packets;
		private final Item material;
		private final Item target;
		private final List<String> events = new ArrayList<>();

		private RecordingTransport(List<AionServerPacket> packets, Item material, Item target) {
			this.packets = packets;
			this.material = material;
			this.target = target;
		}

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			AionServerPacket packet = packets.getLast();
			try {
				if (packet instanceof SM_INVENTORY_UPDATE_ITEM) {
					assertEquals(target, packetField(packet, "item"));
					events.add("update:" + packetField(packet, "updateType") + ":" + target.getAuthorize());
				} else if (packet instanceof SM_ITEM_USAGE_ANIMATION) {
					assertEquals(material.getObjectId(), packetField(packet, "itemObjId"));
					assertEquals(material.getItemId(), packetField(packet, "itemId"));
					if ((int) packetField(packet, "end") != 0) {
						events.add("end:" + packetField(packet, "end") + ":" + target.getAuthorize());
					}
				} else if (packet instanceof SM_SYSTEM_MESSAGE) {
					events.add("message");
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Cannot inspect tempering result packet", e);
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

	private static final class CapturingController extends PlayerController {
		private FutureTask<Void> pending;
		private int scheduledCount;

		@Override
		public Future<?> scheduleTask(TaskId taskId, Runnable task, long delay) {
			assertEquals(TaskId.ITEM_USE, taskId);
			assertEquals(500, delay);
			pending = new FutureTask<>(task, null);
			scheduledCount++;
			return pending;
		}

		private void complete() throws Exception {
			assertEquals(1, scheduledCount);
			pending.run();
			pending.get();
		}
	}
}
