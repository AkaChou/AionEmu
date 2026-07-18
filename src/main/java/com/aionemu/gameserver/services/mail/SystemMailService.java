package com.aionemu.gameserver.services.mail;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.mail_reward.MailRewardTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerMailboxState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

/**
 * 系统邮件服务，向在线/离线玩家投递系统信件与模板奖励。
 * System mail service that delivers system letters and template rewards to online/offline players.
 */
@Slf4j(topic = "SYSMAIL_LOG")
public class SystemMailService {
	@FunctionalInterface
	public interface TransactionAction {
		void execute(Connection connection) throws SQLException;
	}

	/** Spring provider used to override the default singleton / Spring provider used to override the default singleton */
	private static volatile ObjectProvider<SystemMailService> instanceProvider;

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final SystemMailService getInstance() {
		ObjectProvider<SystemMailService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<SystemMailService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 构造服务并输出初始化日志。
	 * Constructs the service and writes the init log.
	 */
	public SystemMailService() {
		log.info(I18n.get("log.156ce5bf14eb"));
	}

	/**
	 * 发送系统邮件，可附带物品、基纳与欧比斯点数。
	 * Sends a system mail with optional attached item, kinah, and abyss points.
	 *
	 * @param sender 发件人名称 / sender name
	 * @param recipientName 收件人名称 / recipient name
	 * @param title 邮件标题 / mail title
	 * mail body
	 * @param attachedItemObjId 附件物品模板 ID / attached item template id
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedKinahCount 附件基纳数量 / attached kinah count
	 * @param attachedAPCount 附件欧比斯点数 / attached abyss points
	 * letter type
	 *
	 * @return 是否发送成功 / whether the mail was sent successfully
	 */
	public boolean sendMail(String sender, String recipientName, String title, String message, int attachedItemObjId,
			long attachedItemCount, long attachedKinahCount, long attachedAPCount, LetterType letterType) {
		return sendMail(sender, recipientName, title, message, attachedItemObjId, attachedItemCount,
				attachedKinahCount, attachedAPCount, letterType, null);
	}

	public boolean sendMail(String sender, String recipientName, String title, String message, int attachedItemObjId,
			long attachedItemCount, long attachedKinahCount, long attachedAPCount, LetterType letterType,
			TransactionAction transactionAction) {
		if (attachedItemObjId != 0) {
			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(attachedItemObjId);
			if (itemTemplate == null) {
				// log.info(I18n.get("log.6645e35c12ff", sender, // recipientName, itemTemplate
				//, attachedItemCount, attachedKinahCount, // " ITEM TEMPLATE IS MISSING "));
				return false;
			}
		}
		if (attachedItemCount == 0 && attachedItemObjId != 0) {
			return false;
		}
		if (recipientName.length() > 16) {
			// log.info(I18n.get("log.44191d66a7b1", sender, // recipientName, attachedItemObjId
			//, attachedItemCount, attachedKinahCount, // " RECIPIENT NAME LENGTH > 16 "));
			return false;
		}
		if (!sender.startsWith("$$") && sender.length() > 50) {
			// log.info(I18n.get("log.44191d66a7b1", sender, // recipientName, attachedItemObjId
			//, attachedItemCount, attachedKinahCount, // " SENDER NAME LENGTH > 16 "));
			return false;
		}
		if (title.length() > 20) {
			title = title.substring(0, 20);
		}
		if (message.length() > 1000) {
			message = message.substring(0, 1000);
		}
		PlayerCommonData recipientCommonData = DAOManager.getDAO(PlayerDAO.class)
				.loadPlayerCommonDataByName(recipientName);
		if (recipientCommonData == null) {
			// log.info(I18n.get("log.2ca8b1e5c461", recipientName));
			return false;
		}
		Player recipient = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipientCommonData.getPlayerObjId());
		Mailbox recipientMailbox = getLoadedMailbox(recipient);
		if (isMailboxFull(recipientMailbox, recipientCommonData, 200)) {
			return false;
		}
		Item attachedItem = null;
		long finalAttachedKinahCount = 0;
		long finalAttachedApCount = 0;
		int itemId = attachedItemObjId;
		long count = attachedItemCount;
		if (itemId != 0) {
			Item senderItem = ItemFactory.newItem(itemId, count);
			if (senderItem != null) {
				senderItem.setEquipped(false);
				senderItem.setEquipmentSlot(0);
				senderItem.setItemLocation(StorageType.MAILBOX.getId());
				attachedItem = senderItem;
			}
		}
		if (attachedKinahCount > 0) {
			finalAttachedKinahCount = attachedKinahCount;
		}
		if (attachedAPCount > 0) {
			finalAttachedApCount = attachedAPCount;
		}
		String finalSender = sender;
		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());
		Letter newLetter = new Letter(GameWorldBootstrapServices.idFactory().nextId(), recipientCommonData.getPlayerObjId(),
				attachedItem, finalAttachedKinahCount, finalAttachedApCount, title, message, finalSender, time, true,
				letterType);
		boolean stored = transactionAction == null
				? storeMail(recipientCommonData, recipient, recipientMailbox, attachedItem, newLetter, time)
				: storeMail(recipientCommonData, recipient, recipientMailbox, attachedItem, newLetter, time,
						transactionAction);
		if (!stored) {
			GameWorldBootstrapServices.idFactory().releaseId(newLetter.getObjectId());
			if (attachedItem != null) {
				ItemService.releaseItemId(attachedItem);
			}
			return false;
		}
		recipientMailbox = getLoadedMailbox(recipient);
		if (recipientMailbox != null) {
			recipientMailbox.putLetterToMailbox(newLetter);
			PacketSendUtility.sendPacket(recipient, new SM_MAIL_SERVICE(recipientMailbox));
			recipientMailbox.isMailListUpdateRequired = true;
			if (recipientMailbox.mailBoxState != 0) {
				boolean isPostman = (recipientMailbox.mailBoxState
						& PlayerMailboxState.EXPRESS) == PlayerMailboxState.EXPRESS;
				PacketSendUtility.sendPacket(recipient,
						new SM_MAIL_SERVICE(recipient, recipientMailbox.getLetters(), isPostman));
			}
			if (letterType == LetterType.EXPRESS) {
				// 快递邮件已到达。 / Express mail has arrived.
				PacketSendUtility.sendPacket(recipient, SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY);
			}
		}
		return true;
	}

	/**
	 * 发送已构造附件物品的系统邮件。
	 * Sends a system mail using a prebuilt attached item instance.
	 *
	 * @param sender 发件人名称 / sender name
	 * mail title
	 * mail body
	 * @param recipientName 收件人名称 / recipient name
	 * @param item 附件物品实例 / attached item instance
	 * @param attachedKinahCount 附件基纳数量 / attached kinah count
	 * @param attachedApCount 附件欧比斯点数 / attached abyss points
	 * @param type 信件类型 / letter type
	 * @return 是否发送成功 / whether the mail was sent successfully
	 */
	public boolean sendSystemMail(String sender, String sysTitle, String sysMessage, String recipientName, Item item,
			long attachedKinahCount, long attachedApCount, LetterType type) {
		return sendSystemMail(sender, sysTitle, sysMessage, recipientName, item, attachedKinahCount, attachedApCount,
				type, null);
	}

	public boolean sendSystemMail(String sender, String sysTitle, String sysMessage, String recipientName, Item item,
			long attachedKinahCount, long attachedApCount, LetterType type, TransactionAction transactionAction) {
		String title = sysTitle;
		String message = sysMessage;
		Item attachedItem = item;
		int attachedItemObjId = 0;
		long attachedItemCount = 0;
		if (attachedItem != null) {
			attachedItemObjId = attachedItem.getItemId();
			attachedItemCount = attachedItem.getItemCount();
		}
		PlayerCommonData recipientCommonData = DAOManager.getDAO(PlayerDAO.class)
				.loadPlayerCommonDataByName(recipientName);
		if (recipientCommonData == null) {
			return false;
		}
		Player recipient = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipientCommonData.getPlayerObjId());
		Mailbox recipientMailbox = getLoadedMailbox(recipient);
		if (isMailboxFull(recipientMailbox, recipientCommonData, 200)) {
			return false;
		}
		Player onlineRecipient = null;
		if (recipientCommonData.getMailboxLetters() >= 100) {
			return false;
		}
		if (recipientCommonData.isOnline()) {
			onlineRecipient = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipientCommonData.getPlayerObjId());
		}
		boolean originalEquipped = attachedItem != null && attachedItem.isEquipped();
		long originalEquipmentSlot = attachedItem == null ? 0 : attachedItem.getEquipmentSlot();
		int originalItemLocation = attachedItem == null ? 0 : attachedItem.getItemLocation();
		PersistentState originalItemState = attachedItem == null ? PersistentState.NOACTION : attachedItem.getPersistentState();
		if (attachedItem != null) {
			attachedItem.setEquipped(false);
			attachedItem.setEquipmentSlot(0);
			attachedItem.setItemLocation(StorageType.MAILBOX.getId());
		}
		Timestamp time = new Timestamp(System.currentTimeMillis());
		Letter newLetter = new Letter(GameWorldBootstrapServices.idFactory().nextId(), recipientCommonData.getPlayerObjId(),
				attachedItem, attachedKinahCount, attachedApCount, title, message, sender, time, true, type);
		boolean stored = transactionAction == null
				? storeMail(recipientCommonData, onlineRecipient, recipientMailbox, attachedItem, newLetter, time)
				: storeMail(recipientCommonData, onlineRecipient, recipientMailbox, attachedItem, newLetter, time,
						transactionAction);
		if (!stored) {
			GameWorldBootstrapServices.idFactory().releaseId(newLetter.getObjectId());
			if (attachedItem != null) {
				attachedItem.setEquipped(originalEquipped);
				attachedItem.setEquipmentSlot(originalEquipmentSlot);
				attachedItem.setItemLocation(originalItemLocation);
				attachedItem.setPersistentState(originalItemState);
			}
			return false;
		}
		recipientMailbox = getLoadedMailbox(onlineRecipient);
		if (recipientMailbox != null) {
			recipientMailbox.putLetterToMailbox(newLetter);
			PacketSendUtility.sendPacket(onlineRecipient,
					new SM_MAIL_SERVICE(onlineRecipient, recipientMailbox.getLetters()));
			PacketSendUtility.sendPacket(onlineRecipient, new SM_MAIL_SERVICE(recipientMailbox));
			// 快递邮件已到达。 / Express mail has arrived.
			if (type == LetterType.EXPRESS || type == LetterType.BLACKCLOUD) {
				PacketSendUtility.sendPacket(onlineRecipient, SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY);
			}
		}
		return true;
	}

	/**
	 * 获取玩家已加载的邮箱实例。
	 * Returns the player's already loaded mailbox instance.
	 *
	 * 玩家 / player
	 * mailbox or null
	 */
	private Mailbox getLoadedMailbox(Player recipient) {
		return recipient == null ? null : recipient.getMailbox();
	}

	/**
	 * 判断邮箱是否已达上限。
	 * Checks whether the mailbox has reached the given letter limit.
	 *
	 * @param recipientMailbox 已加载邮箱 / loaded mailbox
	 * @param recipientCommonData 玩家公共数据 / player common data
	 * @param limit 信件上限 / letter limit
	 * whether the mailbox is full
	 */
	private boolean isMailboxFull(Mailbox recipientMailbox, PlayerCommonData recipientCommonData, int limit) {
		return recipientMailbox != null ? recipientMailbox.size() >= limit : recipientCommonData.getMailboxLetters() >= limit;
	}

	/**
	 * 更新离线玩家邮箱信件计数并写库。
	 * Updates the offline mailbox letter counter and persists it.
	 *
	 * @param recipientCommonData 收件人公共数据 / recipient common data
	 * @param recipient 在线玩家实例 / online player instance
	 * @param recipientMailbox 已加载邮箱 / loaded mailbox
	 */
	protected boolean storeMail(PlayerCommonData recipientCommonData, Player recipient, Mailbox recipientMailbox,
			Item attachedItem, Letter letter, Timestamp time) {
		return storeMail(recipientCommonData, recipient, recipientMailbox, attachedItem, letter, time, null);
	}

	private boolean storeMail(PlayerCommonData recipientCommonData, Player recipient, Mailbox recipientMailbox,
			Item attachedItem, Letter letter, Timestamp time, TransactionAction transactionAction) {
		InventoryDAO inventoryDAO = DAOManager.getDAO(InventoryDAO.class);
		MailDAO mailDAO = DAOManager.getDAO(MailDAO.class);
		PlayerCommonData counterData = recipientMailbox == null && recipient != null && recipient.getCommonData() != null
				? recipient.getCommonData() : recipientCommonData;
		int mailboxLetters = counterData.getMailboxLetters() + 1;
		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {
				if (attachedItem != null) {
					inventoryDAO.storeInTransaction(con, List.of(attachedItem), recipientCommonData.getPlayerObjId(), null, null);
				}
				mailDAO.storeLetterInTransaction(con, time, letter);
				if (recipientMailbox == null) {
					mailDAO.updateMailCounterInTransaction(con, counterData.getName(), mailboxLetters);
				}
				if (transactionAction != null) {
					transactionAction.execute(con);
				}
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.bf6c47a4810b", recipientCommonData.getPlayerObjId(), e));
			return false;
		}
		if (attachedItem != null) {
			inventoryDAO.markStored(List.of(attachedItem));
		}
		letter.setPersistState(PersistentState.UPDATED);
		if (recipientMailbox == null) {
			counterData.setMailboxLetters(mailboxLetters);
			if (counterData != recipientCommonData) {
				recipientCommonData.setMailboxLetters(mailboxLetters);
			}
		}
		return true;
	}

	/**
	 * 按奖励模板向玩家发送系统邮件。
	 * Sends a system mail reward defined by the given template id.
	 *
	 * reward template id
	 * @param playerData 玩家公共数据 / player common data
	 */
	public static void sendTemplateRewardMail(final int templateId, final PlayerCommonData playerData) {
		final MailRewardTemplate reward = DataManager.MAIL_REWARD.getMailReward(templateId);
		GameFeatureServices.systemMailService().sendMail(reward.getSender(), playerData.getName(), reward.getTitle(),
				reward.getBody() + "\\n\\n" + reward.getTail(), reward.getItemId(), reward.getItemCount(),
				reward.getKinahCount(), reward.getApCount(), LetterType.NORMAL);
	}

	/**
	 * 默认单例持有者。
	 * Default singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final SystemMailService instance = new SystemMailService();
	}
}
