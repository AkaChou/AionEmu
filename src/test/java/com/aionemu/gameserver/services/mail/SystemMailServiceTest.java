package com.aionemu.gameserver.services.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class SystemMailServiceTest {

	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void sendMailDefersOnlineDeliveryWhenRecipientMailboxIsNotLoadedYet() throws Exception {
		try (ServiceContext.Scope ignored = ServiceContext.use("system-mail-service-test-" + System.nanoTime())) {
			PlayerCommonData daoRecipientData = recipientData(1, "Recipient", 4);
			PlayerCommonData onlineRecipientData = recipientData(1, "Recipient", 4);
			Player onlineRecipient = playerWithCommonData(onlineRecipientData);
			FakeMailDAO mailDao = new FakeMailDAO();
			installDaos(new FakePlayerDAO(daoRecipientData), mailDao, new FakeInventoryDAO());

			FakeWorld world = OBJENESIS.newInstance(FakeWorld.class);
			world.player = onlineRecipient;
			FakeIDFactory idFactory = OBJENESIS.newInstance(FakeIDFactory.class);
			GameWorldBootstrapServices bootstrapServices = new GameWorldBootstrapServices(
					provider(IDFactory.class, idFactory), null, null, null, provider(World.class, world));
			try {
				SystemMailService service = new SystemMailService();

				assertDoesNotThrow(() -> service.sendMail("$$TEST", "Recipient", "Title", "Body", 0, 0, 0, 0,
						LetterType.NORMAL));

				assertNull(onlineRecipient.getMailbox());
				assertEquals(1, mailDao.storedLetters);
				assertEquals(5, onlineRecipientData.getMailboxLetters());
				assertEquals(5, mailDao.updatedMailboxLetters);
				assertEquals("Recipient", mailDao.updatedRecipientName);
			} finally {
				bootstrapServices.destroy();
				DAOManager.shutdown();
			}
		}
	}

	@Test
	void sendSystemMailUsesLatestOnlineRecipientForExpressNotification() throws Exception {
		try (ServiceContext.Scope ignored = ServiceContext.use("system-mail-service-test-" + System.nanoTime())) {
			PlayerCommonData daoRecipientData = recipientData(1, "Recipient", 4);
			PlayerCommonData onlineRecipientData = recipientData(1, "Recipient", 4);
			Player onlineRecipient = playerWithCommonData(onlineRecipientData);
			onlineRecipient.setMailbox(new Mailbox(onlineRecipient));
			FakeMailDAO mailDao = new FakeMailDAO();
			installDaos(new FakePlayerDAO(daoRecipientData), mailDao, new FakeInventoryDAO());

			FakeWorld world = OBJENESIS.newInstance(FakeWorld.class);
			world.findResults = new Player[] { null, onlineRecipient };
			FakeIDFactory idFactory = OBJENESIS.newInstance(FakeIDFactory.class);
			GameWorldBootstrapServices bootstrapServices = new GameWorldBootstrapServices(
					provider(IDFactory.class, idFactory), null, null, null, provider(World.class, world));
			try {
				SystemMailService service = new SystemMailService();

				assertDoesNotThrow(() -> service.sendSystemMail("$$TEST", "Title", "Body", "Recipient", item(182400001),
						0, 0, LetterType.EXPRESS));

				assertEquals(1, mailDao.storedLetters);
				assertEquals(1, onlineRecipient.getMailbox().size());
			} finally {
				bootstrapServices.destroy();
				DAOManager.shutdown();
			}
		}
	}

	private static PlayerCommonData recipientData(int objectId, String name, int mailboxLetters) {
		PlayerCommonData data = new PlayerCommonData(objectId);
		data.setName(name);
		data.setOnline(true);
		data.setMailboxLetters(mailboxLetters);
		return data;
	}

	private static Player playerWithCommonData(PlayerCommonData data) throws ReflectiveOperationException {
		Player player = OBJENESIS.newInstance(Player.class);
		Field field = Player.class.getDeclaredField("playerCommonData");
		field.setAccessible(true);
		field.set(player, data);
		return player;
	}

	private static Item item(int itemId) throws ReflectiveOperationException {
		ItemTemplate template = OBJENESIS.newInstance(ItemTemplate.class);
		template.setItemId(itemId);
		Item item = OBJENESIS.newInstance(Item.class);
		setField(item, "itemTemplate", template);
		setField(item, "itemCount", 1L);
		setField(item, "persistentState", PersistentState.NEW);
		return item;
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private static void installDaos(DAO... daos) throws ReflectiveOperationException {
		Field statesField = DAOManager.class.getDeclaredField("states");
		statesField.setAccessible(true);
		Map<String, Object> states = (Map<String, Object>) statesField.get(null);

		Class<?> daoStateClass = Class.forName("com.aionemu.commons.database.dao.DAOManager$DaoState");
		Constructor<?> constructor = daoStateClass.getDeclaredConstructor();
		constructor.setAccessible(true);
		Object state = constructor.newInstance();

		Field daoMapField = daoStateClass.getDeclaredField("daoMap");
		daoMapField.setAccessible(true);
		Map<String, DAO> daoMap = (Map<String, DAO>) daoMapField.get(state);
		for (DAO dao : daos) {
			daoMap.put(dao.getClassName(), dao);
		}
		states.put(ServiceContext.current(), state);
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class FakeWorld extends World {
		private Player player;
		private Player[] findResults;
		private int findCalls;

		@Override
		public Player findPlayer(int objectId) {
			if (findResults != null && findCalls < findResults.length) {
				return findResults[findCalls++];
			}
			return player;
		}
	}

	private static final class FakeIDFactory extends IDFactory {
		@Override
		public int nextId() {
			return 9001;
		}
	}

	private static final class FakePlayerDAO extends PlayerDAO {
		private final PlayerCommonData recipientData;

		private FakePlayerDAO(PlayerCommonData recipientData) {
			this.recipientData = recipientData;
		}

		@Override
		public PlayerCommonData loadPlayerCommonDataByName(String name) {
			return recipientData;
		}

		@Override
		public boolean supports(String database, int majorVersion, int minorVersion) {
			return true;
		}

		@Override
		public int[] getUsedIDs() {
			return new int[0];
		}

		@Override
		public boolean isNameUsed(String name) {
			throw unsupported();
		}

		@Override
		public Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds) {
			throw unsupported();
		}

		@Override
		public void storePlayer(Player player) {
			throw unsupported();
		}

		@Override
		public boolean saveNewPlayer(PlayerCommonData pcd, int accountId, String accountName) {
			throw unsupported();
		}

		@Override
		public PlayerCommonData loadPlayerCommonData(int playerObjId) {
			throw unsupported();
		}

		@Override
		public void deletePlayer(int playerId) {
			throw unsupported();
		}

		@Override
		public void updateDeletionTime(int objectId, Timestamp deletionDate) {
			throw unsupported();
		}

		@Override
		public void storeCreationTime(int objectId, Timestamp creationDate) {
			throw unsupported();
		}

		@Override
		public void setCreationDeletionTime(PlayerAccountData acData) {
			throw unsupported();
		}

		@Override
		public List<Integer> getPlayerOidsOnAccount(int accountId) {
			throw unsupported();
		}

		@Override
		public void storeLastOnlineTime(int objectId, Timestamp lastOnline) {
			throw unsupported();
		}

		@Override
		public void onlinePlayer(Player player, boolean online) {
			throw unsupported();
		}

		@Override
		public void setPlayersOffline(boolean online) {
			throw unsupported();
		}

		@Override
		public int getAccountIdByName(String name) {
			throw unsupported();
		}

		@Override
		public String getPlayerNameByObjId(int playerObjId) {
			throw unsupported();
		}

		@Override
		public int getPlayerIdByName(String playerName) {
			throw unsupported();
		}

		@Override
		public void storePlayerName(PlayerCommonData recipientCommonData) {
			throw unsupported();
		}

		@Override
		public int getCharacterCountOnAccount(int accountId) {
			throw unsupported();
		}

		@Override
		public int getCharacterCountForRace(Race race) {
			throw unsupported();
		}

		@Override
		public int getOnlinePlayerCount() {
			throw unsupported();
		}

		@Override
		public List<Integer> getPlayersToDelete(int paramInt1, int paramInt2) {
			throw unsupported();
		}

		@Override
		public void setPlayerLastTransferTime(int playerId, long time) {
			throw unsupported();
		}

		@Override
		public Timestamp getCharacterCreationDateId(int obj) {
			throw unsupported();
		}

		@Override
		public void updateLegionJoinRequestState(int playerId, LegionJoinRequestState state) {
			throw unsupported();
		}

		@Override
		public void clearJoinRequest(int playerId) {
			throw unsupported();
		}

		@Override
		public void getJoinRequestState(Player player) {
			throw unsupported();
		}

		@Override
		public int getPlayerLunaConsumeByObjId(int playerObjId) {
			throw unsupported();
		}
	}

	private static final class FakeMailDAO extends MailDAO {
		private int storedLetters;
		private int updatedMailboxLetters;
		private String updatedRecipientName;

		@Override
		public boolean storeLetter(Timestamp time, Letter letter) {
			storedLetters++;
			return true;
		}

		@Override
		public Mailbox loadPlayerMailbox(Player player) {
			throw unsupported();
		}

		@Override
		public void storeMailbox(Player player) {
			throw unsupported();
		}

		@Override
		public boolean deleteLetter(int letterId) {
			throw unsupported();
		}

		@Override
		public void updateOfflineMailCounter(PlayerCommonData recipientCommonData) {
			updatedMailboxLetters = recipientCommonData.getMailboxLetters();
			updatedRecipientName = recipientCommonData.getName();
		}

		@Override
		public boolean haveUnread(int playerId) {
			throw unsupported();
		}

		@Override
		public boolean supports(String database, int majorVersion, int minorVersion) {
			return true;
		}

		@Override
		public int[] getUsedIDs() {
			return new int[0];
		}
	}

	private static final class FakeInventoryDAO extends InventoryDAO {
		@Override
		public Storage loadStorage(int playerId, StorageType storageType) {
			throw unsupported();
		}

		@Override
		public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
			throw unsupported();
		}

		@Override
		public Equipment loadEquipment(Player player) {
			throw unsupported();
		}

		@Override
		public List<Item> loadEquipment(int playerId) {
			throw unsupported();
		}

		@Override
		public boolean store(Player player) {
			throw unsupported();
		}

		@Override
		public boolean store(Item item, Player player) {
			return true;
		}

		@Override
		public boolean store(List<Item> items, int playerId) {
			return true;
		}

		@Override
		public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
			return true;
		}

		@Override
		public boolean deletePlayerItems(int playerId) {
			throw unsupported();
		}

		@Override
		public void deleteAccountWH(int accountId) {
			throw unsupported();
		}

		@Override
		public boolean supports(String database, int majorVersion, int minorVersion) {
			return true;
		}

		@Override
		public int[] getUsedIDs() {
			return new int[0];
		}
	}

	private static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException("Unexpected DAO call in SystemMailServiceTest");
	}
}
