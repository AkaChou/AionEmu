package com.aionemu.gameserver.services.mail;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.Disposition;
import com.aionemu.gameserver.model.templates.mail.MailMessage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerMailboxState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

/**
 * 玩家邮件服务：发送、阅读、领取附件、删除邮件，以及登录时加载邮箱。
 * Player mail service: send, read, claim attachments, delete mail, and load mailbox on login.
 *
 * @author kosyachok
 * @author ATracer
 */
@Slf4j(topic = "MAIL_LOG")
public class MailService {

	/** Spring ObjectProvider used to override the default singleton / Spring ObjectProvider used to override the default singleton */
	private static volatile ObjectProvider<MailService> instanceProvider;

	/** 新登录玩家队列（预留）。 / Queue of newly logged-in players (reserved). */
	protected Queue<Player> newPlayers;

	/**
	 * 获取 MailService 单例，优先走 Spring ObjectProvider。
	 * Returns the MailService singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final MailService getInstance() {
		ObjectProvider<MailService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 构造邮件服务并初始化新玩家队列。
	 * Constructs the mail service and initializes the new-player queue.
	 */
	public MailService() {
		newPlayers = new ConcurrentLinkedQueue<Player>();
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<MailService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 玩家间发送邮件（含物品/基纳/欧比斯点附件与手续费结算）。
	 * Sends player-to-player mail including item/kinah/AP attachments and commission settlement.
	 *
	 * sender player
	 * @param recipientName 收件人角色名 / recipient character name
	 * @param title 邮件标题 / mail title
	 * mail body
	 * @param attachedItemObjId 附件物品对象 ID，0 表示无物品 / attached item object id, 0 if none
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedKinahCount 附件基纳数量 / attached kinah amount
	 * @param attachedApCount 附件欧比斯点数量 / attached abyss points
	 * letter type
	 */
	public void sendMail(Player sender, String recipientName, String title, String message, int attachedItemObjId,
			int attachedItemCount, int attachedKinahCount, int attachedApCount, LetterType letterType) {

		if (letterType == LetterType.BLACKCLOUD || recipientName.length() > 16) {
			return;
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
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.NO_SUCH_CHARACTER_NAME));
			return;
		}

		if ((recipientCommonData.getRace() != sender.getRace()) && sender.getAccessLevel() < AdminConfig.GM_LEVEL) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.MAIL_IS_ONE_RACE_ONLY));
			return;
		}

		Player recipient = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipientCommonData.getPlayerObjId());
		Mailbox recipientMailbox = recipient == null ? null : recipient.getMailbox();
		if ((recipientMailbox != null && !recipientMailbox.haveFreeSlots())
				|| (recipientMailbox == null && recipientCommonData.getMailboxLetters() > 99)) {
			PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.RECIPIENT_MAILBOX_FULL));
			return;
		}

		Storage senderInventory = sender.getInventory();
		if (!validateMailSendPrice(sender, attachedKinahCount, attachedItemObjId, attachedItemCount)) {
			return;
		}
		InventorySnapshot inventorySnapshot = InventorySnapshot.capture(senderInventory);
		Item attachedItem = null;
		boolean createdAttachedItem = false;
		int finalAttachedKinahCount = 0;
		int finaAttachedApCount = 0;

		int kinahMailCommission = 0;
		int itemMailCommission = 0;

		if (attachedItemObjId != 0 && attachedItemCount > 0) {
			Item senderItem = senderInventory.getItemByObjId(attachedItemObjId);

			if (senderItem == null) {
				return;
			}

			if (!GameRuntimeServices.adminService().canOperate(sender, null, senderItem, "mail")) {
				return;
			}
			float qualityPriceRate;
			switch (senderItem.getItemTemplate().getItemQuality()) {
			case JUNK:
			case COMMON:
				qualityPriceRate = 0.02f;
				break;

			case RARE:
				qualityPriceRate = 0.03f;
				break;

			case LEGEND:
			case UNIQUE:
				qualityPriceRate = 0.04f;
				break;

			case MYTHIC:
			case EPIC:
				qualityPriceRate = 0.05f;
				break;

			default:
				qualityPriceRate = 0.02f;
				break;
			}

			if (senderItem.getItemCount() < attachedItemCount) {
				return;// Client hack
			}

			// 检查邮件中不可交易与现金物品（特殊邮差通行证） / Check Mailing untradables with Cash items (Special courier passes)
			if (!senderItem.isTradeable(sender)) {
				Disposition dispo = senderItem.getItemTemplate().getDisposition();
				if (dispo == null || dispo.getId() == 0 || dispo.getCount() == 0) { // can not be traded, hack
					return;
				}
				if (senderItem.getWrappableCount() <= senderItem.getItemTemplate().getWrappableCount()
						&& !senderItem.isPacked()) {
					return;
				}
				if (senderInventory.getItemCountByItemId(dispo.getId()) >= dispo.getCount()) {
					senderInventory.decreaseByItemId(dispo.getId(), dispo.getCount());
				} else {
					return;
				}
			}

			// 数量减至耗尽时复用物品对象。 / reuse item in case of full decrease of count
			if (senderItem.getItemCount() == attachedItemCount) {
				senderInventory.remove(senderItem);
				PacketSendUtility.sendPacket(sender, new SM_DELETE_ITEM(attachedItemObjId));
				attachedItem = senderItem;
			} else if (senderItem.getItemCount() > attachedItemCount) {
				attachedItem = ItemFactory.newItem(senderItem.getItemTemplate().getTemplateId(), attachedItemCount);
				createdAttachedItem = attachedItem != null;
				senderInventory.decreaseItemCount(senderItem, attachedItemCount);
			}

			if (attachedItem == null) {
				inventorySnapshot.restore();
				if (createdAttachedItem && attachedItem != null) {
					ItemService.releaseItemId(attachedItem);
				}
				return;
			}
			attachedItem.setEquipped(false);
			attachedItem.setEquipmentSlot(0);
			attachedItem.setItemLocation(StorageType.MAILBOX.getId());
			itemMailCommission = Math.round(
					(attachedItem.getItemTemplate().getPrice() * attachedItem.getItemCount()) * qualityPriceRate);
		}

		/**
		 * 计算附件基纳与手续费。
		 * Calculate attached kinah and commission.
		 */
		if (attachedKinahCount > 0) {
			if (senderInventory.getKinah() - attachedKinahCount >= 0) {
				finalAttachedKinahCount = attachedKinahCount;
				kinahMailCommission = Math.round(attachedKinahCount * 0.01f);
			}
		}
		if (attachedApCount > 0) {
			finaAttachedApCount = attachedApCount;
		}

		int finalMailKinah = 10 + kinahMailCommission + itemMailCommission + finalAttachedKinahCount;

		if (senderInventory.getKinah() > finalMailKinah) {
			senderInventory.decreaseKinah(finalMailKinah);
		} else {
			AuditLogger.info(sender, "Mail kinah exploit.");
			inventorySnapshot.restore();
			if (createdAttachedItem && attachedItem != null) {
				ItemService.releaseItemId(attachedItem);
			}
			return;
		}

		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());

		Letter newLetter = new Letter(GameWorldBootstrapServices.idFactory().nextId(), recipientCommonData.getPlayerObjId(),
				attachedItem, finalAttachedKinahCount, finaAttachedApCount, title, message, sender.getName(), time,
				true, letterType);

		if (!storeSentMail(sender, recipientCommonData, recipientMailbox, attachedItem, newLetter, time)) {
			inventorySnapshot.restore();
			GameWorldBootstrapServices.idFactory().releaseId(newLetter.getObjectId());
			if (createdAttachedItem && attachedItem != null) {
				ItemService.releaseItemId(attachedItem);
			}
			return;
		}
		PacketSendUtility.sendPacket(sender, new SM_MAIL_SERVICE(MailMessage.MAIL_SEND_SECCESS));
		/**
		 * 在线收件人：写入邮箱并推送邮件相关数据包。
		 * Online recipient: put letter into mailbox and push mail update packets.
		 */
		if (recipientMailbox != null) {
			recipientMailbox.putLetterToMailbox(newLetter);

			// 收件人的数据包 / packets for recipient
			PacketSendUtility.sendPacket(recipient, new SM_MAIL_SERVICE(recipientMailbox));
			recipientMailbox.isMailListUpdateRequired = true;

			// 若收件人已打开邮件列表则应更新 / if recipient have opened mail list we should update it
			if (recipientMailbox.mailBoxState != 0) {
				boolean isPostman = (recipientMailbox.mailBoxState
						& PlayerMailboxState.EXPRESS) == PlayerMailboxState.EXPRESS;
				PacketSendUtility.sendPacket(recipient,
						new SM_MAIL_SERVICE(recipient, recipientMailbox.getLetters(), isPostman));
			}

			if (letterType == LetterType.EXPRESS) {
				PacketSendUtility.sendPacket(recipient, SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY);
			}
		}

	}

	private boolean storeSentMail(Player sender, PlayerCommonData recipientCommonData, Mailbox recipientMailbox,
			Item attachedItem, Letter letter, Timestamp time) {
		InventoryDAO inventoryDAO = DAOManager.getDAO(InventoryDAO.class);
		MailDAO mailDAO = DAOManager.getDAO(MailDAO.class);
		List<Item> senderItems = sender.getDirtyItemsToUpdate();
		Integer accountId = sender.getPlayerAccount() == null ? null : sender.getPlayerAccount().getId();
		Integer legionId = sender.getLegion() == null ? null : sender.getLegion().getLegionId();
		int mailboxLetters = recipientCommonData.getMailboxLetters() + 1;

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {
				inventoryDAO.storeInTransaction(con, senderItems, sender.getObjectId(), accountId, legionId);
				if (attachedItem != null) {
					inventoryDAO.storeInTransaction(con, List.of(attachedItem), recipientCommonData.getPlayerObjId(), null, null);
				}
				mailDAO.storeLetterInTransaction(con, time, letter);
				if (recipientMailbox == null) {
					mailDAO.updateMailCounterInTransaction(con, recipientCommonData.getName(), mailboxLetters);
				}
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.d40fe011fd92", sender.getObjectId(), recipientCommonData.getPlayerObjId(), e));
			return false;
		}

		inventoryDAO.markStored(senderItems);
		sender.markDirtyItemContainersStored();
		if (attachedItem != null) {
			inventoryDAO.markStored(List.of(attachedItem));
		}
		letter.setPersistState(com.aionemu.gameserver.model.gameobjects.PersistentState.UPDATED);
		if (recipientMailbox == null) {
			recipientCommonData.setMailboxLetters(mailboxLetters);
		}
		return true;
	}

	/**
	 * 阅读指定 ID 的信件并标记为已读。
	 * Reads the letter with the given id and marks it as read.
	 *
	 * 玩家 / player
	 * letter id
	 */
	public void readMail(Player player, int letterId) {
		Letter letter = player.getMailbox().getLetterFromMailbox(letterId);
		if (letter == null) {
			log.warn(I18n.get("log.ec29809b7806", player.getObjectId(), letterId));
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player, letter, letter.getTimeStamp().getTime()));
		letter.setReadLetter();
	}

	/**
	 * 领取邮件附件（物品 / 基纳 / 欧比斯点）。
	 * kinah / abyss points).
	 *
	 * 玩家 / player
	 * letter id
	 * @param attachmentType 附件类型：0 物品，1 基纳，2 欧比斯点 / attachment type: 0 item, 1 kinah, 2 AP
	 */
	public void getAttachments(Player player, int letterId, int attachmentType) {
		Letter letter = player.getMailbox().getLetterFromMailbox(letterId);

		if (letter == null) {
			return;
		}
		Item attachedItem = letter.getAttachedItem();
		long attachedKinah = letter.getAttachedKinah();
		long attachedAp = letter.getAttachedAp();
		PersistentState letterState = letter.getLetterPersistentState();
		switch (attachmentType) {
		case 0: {
			if (attachedItem == null)
				return;
			if (player.getInventory().isFull()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
				return;
			}
			boolean packed = attachedItem.isPacked();
			PersistentState itemState = attachedItem.getPersistentState();
			if (attachedItem.isPacked()) {
				attachedItem.setPacked(false);
			}
			if (player.getInventory().add(attachedItem) == null) {
				attachedItem.setPacked(packed);
				attachedItem.setPersistentState(itemState);
				return;
			}
			letter.removeAttachedItem();
			if (!storeClaimedAttachment(player, letter, List.of(attachedItem), null)) {
				player.getInventory().remove(attachedItem);
				attachedItem.setItemLocation(StorageType.MAILBOX.getId());
				attachedItem.setPacked(packed);
				attachedItem.setPersistentState(itemState);
				letter.restoreAttachments(attachedItem, attachedKinah, attachedAp, letterState);
				PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(attachedItem.getObjectId()));
				return;
			}
			PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(letterId, attachmentType));
			break;
		}
		case 1: {
			if (attachedKinah <= 0) {
				return;
			}
			Item kinahItem = player.getInventory().getKinahItem();
			PersistentState kinahState = kinahItem == null ? PersistentState.NOACTION : kinahItem.getPersistentState();
			player.getInventory().increaseKinah(attachedKinah);
			kinahItem = player.getInventory().getKinahItem();
			letter.removeAttachedKinah();
			if (!storeClaimedAttachment(player, letter, List.of(kinahItem), null)) {
				player.getInventory().decreaseKinah(attachedKinah);
				kinahItem.setPersistentState(kinahState);
				letter.restoreAttachments(attachedItem, attachedKinah, attachedAp, letterState);
				return;
			}
			PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(letterId, attachmentType));
			break;
		}
		case 2: {
			if (attachedAp <= 0 || attachedAp > Integer.MAX_VALUE) {
				return;
			}
			AbyssRank rank = player.getAbyssRank();
			AbyssRank storedRank = new AbyssRank(rank.getDailyAP(), rank.getDailyGP(), rank.getWeeklyAP(), rank.getWeeklyGP(),
					rank.getAp(), rank.getGp(), rank.getRank().getId(), rank.getTopRanking(), rank.getDailyKill(),
					rank.getWeeklyKill(), rank.getAllKill(), rank.getMaxRank(), rank.getLastKill(), rank.getLastAP(),
					rank.getLastGP(), rank.getLastUpdate());
			storedRank.setPersistentState(rank.getPersistentState() == PersistentState.NEW
					? PersistentState.NEW : PersistentState.UPDATE_REQUIRED);
			storedRank.addAp((int) attachedAp, player);
			letter.removeAttachedAP();
			if (!storeClaimedAttachment(player, letter, List.of(), storedRank)) {
				letter.restoreAttachments(attachedItem, attachedKinah, attachedAp, letterState);
				return;
			}
			AbyssPointsService.addAp(player, (int) attachedAp);
			player.getAbyssRank().setPersistentState(PersistentState.UPDATED);
			PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(letterId, attachmentType));
			break;
		}
		}
	}

	private boolean storeClaimedAttachment(Player player, Letter letter, List<Item> items, AbyssRank abyssRank) {
		InventoryDAO inventoryDAO = DAOManager.getDAO(InventoryDAO.class);
		MailDAO mailDAO = DAOManager.getDAO(MailDAO.class);
		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {
				inventoryDAO.storeInTransaction(con, items, player.getObjectId(), null, null);
				if (abyssRank != null) {
					DAOManager.getDAO(AbyssRankDAO.class).storeInTransaction(con, player.getObjectId(), abyssRank);
				}
				mailDAO.storeLetterInTransaction(con, letter.getTimeStamp(), letter);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.9b61ee4df55e", player.getObjectId(), letter.getObjectId(), e));
			return false;
		}
		inventoryDAO.markStored(items);
		letter.setPersistState(PersistentState.UPDATED);
		return true;
	}

	/**
	 * 删除玩家邮箱中的指定信件。
	 * Deletes the specified letters from the player's mailbox.
	 *
	 * @param player 玩家 / player
	 * @param mailObjId 要删除的信件 ID 数组 / letter id array to delete
	 */
	public void deleteMail(Player player, int[] mailObjId) {
		Mailbox mailbox = player.getMailbox();

		for (int letterId : mailObjId) {
			mailbox.removeLetter(letterId);
			DAOManager.getDAO(MailDAO.class).deleteLetter(letterId);
		}
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(mailObjId));
	}

	/**
	 * 校验发件人是否负担得起邮费（基础费 + 物品/基纳手续费）。
	 * Validates that the sender can afford the mail fee (base + item/kinah commission).
	 *
	 * sender player
	 *
	 * @param attachedKinahCount 附件基纳数量 / attached kinah amount
	 * @param attachedItemObjId 附件物品对象 ID / attached item object id
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedItemCount
	 * @return 是否足够支付邮费 / whether the sender can pay the mail fee
	 */
	private boolean validateMailSendPrice(Player sender, int attachedKinahCount, int attachedItemObjId,
			int attachedItemCount) {
		int itemMailCommission = 0;
		int kinahMailCommission = Math.round(attachedKinahCount * 0.01f);
		if (attachedItemObjId != 0) {
			Item senderItem = sender.getInventory().getItemByObjId(attachedItemObjId);
			if (senderItem == null || senderItem.getItemTemplate() == null) {
				return false;
			}
			float qualityPriceRate;
			switch (senderItem.getItemTemplate().getItemQuality()) {
			case JUNK:
			case COMMON:
				qualityPriceRate = 0.02f;
				break;

			case RARE:
				qualityPriceRate = 0.03f;
				break;

			case LEGEND:
			case UNIQUE:
				qualityPriceRate = 0.04f;
				break;

			case MYTHIC:
			case EPIC:
				qualityPriceRate = 0.05f;
				break;

			default:
				qualityPriceRate = 0.02f;
				break;
			}
			itemMailCommission = Math
					.round((senderItem.getItemTemplate().getPrice() * attachedItemCount) * qualityPriceRate);
		}

		int finalMailPrice = 10 + itemMailCommission + kinahMailCommission;

		if (sender.getInventory().getKinah() >= finalMailPrice) {
			return true;
		}
		return false;
	}

	private record ItemSnapshot(Item item, long count, int location, PersistentState state) {}

	private record InventorySnapshot(Storage inventory, PersistentState state, List<ItemSnapshot> items,
			List<Item> deletedItems) {

		private static InventorySnapshot capture(Storage inventory) {
			List<ItemSnapshot> items = new ArrayList<>();
			for (Item item : inventory.getItemsWithKinah()) {
				items.add(new ItemSnapshot(item, item.getItemCount(), item.getItemLocation(), item.getPersistentState()));
			}
			return new InventorySnapshot(inventory, inventory.getPersistentState(), items,
					new ArrayList<>(inventory.getDeletedItems()));
		}

		private void restore() {
			for (ItemSnapshot snapshot : items) {
				Item item = snapshot.item();
				if (item.getItemTemplate().isKinah()) {
					long difference = snapshot.count() - inventory.getKinah();
					if (difference > 0) {
						inventory.increaseKinah(difference);
					} else if (difference < 0) {
						inventory.decreaseKinah(-difference);
					}
				} else {
					Item currentItem = inventory.getItemByObjId(item.getObjectId());
					if (currentItem == null) {
						item.setItemCount(snapshot.count());
						item.setItemLocation(snapshot.location());
						inventory.add(item);
					} else {
						long difference = snapshot.count() - currentItem.getItemCount();
						if (difference > 0) {
							inventory.increaseItemCount(currentItem, difference);
						} else if (difference < 0) {
							inventory.decreaseItemCount(currentItem, -difference);
						}
					}
				}
				item.setItemLocation(snapshot.location());
				item.setPersistentState(snapshot.state());
			}
			inventory.getDeletedItems().removeIf(item -> !deletedItems.contains(item));
			inventory.setPersistentState(state);
		}
	}

	/**
	 * 玩家登录后延迟加载邮箱。
	 * Schedules delayed mailbox loading after player login.
	 *
	 * @param player 玩家 / player
	 */
	public void onPlayerLogin(Player player) {
		GameThreadPoolServices.threadPoolManager().schedule(new MailLoadTask(player), 5000);
	}

	/**
	 * 向客户端刷新当前邮箱状态与信件列表。
	 * Refreshes the client with the current mailbox state and letter list.
	 *
	 * @param player 玩家 / player
	 */
	public void refreshMail(Player player) {
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player.getMailbox()));
		PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player, player.getMailbox().getLetters(), false));
	}

	/**
	 * 延迟加载玩家全部邮件的任务。
	 * Task that loads all player mail items after a delay.
	 *
	 * @author ATracer
	 */
	private class MailLoadTask implements Runnable {

		/** 目标玩家。 / Target player. */
		private Player player;

		/**
		 * 创建邮件加载任务。
		 * Creates a mail load task for the given player.
		 *
		 * 玩家 / player
		 */
		private MailLoadTask(Player player) {
			this.player = player;
		}

		/**
		 * 从数据库加载邮箱并通知客户端，同时触发房屋竞拍登录处理。
		 * Loads mailbox from DB, notifies the client, and triggers housing bid login handling.
		 */
		@Override
		public void run() {
			player.setMailbox(DAOManager.getDAO(MailDAO.class).loadPlayerMailbox(player));
			PacketSendUtility.sendPacket(player, new SM_MAIL_SERVICE(player.getMailbox()));
			GameHousingServices.housingBidService().onPlayerLogin(player);
		}
	}

	/**
	 * 默认单例持有者。
	 * Default singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final MailService instance = new MailService();
	}
}
