package com.aionemu.gameserver.services.veteranreward;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.VeteranRewardsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.player.PlayerMailboxState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * 老兵奖励服务，按定时任务从数据库加载奖励并通过邮件发放。
 * Veteran reward service loading pending rewards from the database on a cron schedule and mailing them.
 */
@Slf4j(topic = "VETERANREWARD_LOG")
public class VeteranRewardsService {
	private static volatile ObjectProvider<VeteranRewardsService> instanceProvider;

	/**
	 * 奖励接收方类型，按种族或指定玩家过滤。
	 * Recipient filter type selecting by race or a specific player.
	 */
	public enum RecipientType {

		ELYOS, ASMO, ALL, PLAYER;

		/**
		 * 判断该种族是否允许接收本类型奖励。
		 * Whether the given race is allowed for this recipient type.
		 *
		 * 阵营 / Race
		 * Whether allowed
		 */
		private boolean isAllowed(Race race) {
			switch (this) {
			case ELYOS:
				return race == Race.ELYOS;
			case ASMO:
				return race == Race.ASMODIANS;
			case ALL:
				return race == Race.ELYOS || race == Race.ASMODIANS;
			default:
				return false;
			}
		}
	}


	private Collection<VeteranRewards> veteran_rewards;

	private static final String VETERAN_REWARDS_LOOP_STATUS_BROADCAST_SCHEDULE = "0 * * ? * *";

	/**
	 * 构造服务并启动定时扫描循环。
	 * Construct the service and start the periodic scan loop.
	 */
	public VeteranRewardsService() {
		Init_VeteranRewardStatusLoop();
	}

	/**
	 * 注册老兵奖励扫描定时任务（每分钟）。
	 * Register the veteran-reward scan cron job (every minute).
	 */
	private void Init_VeteranRewardStatusLoop() {
		log.info(I18n.get("log.9d55ff406e5f"));

		GameCronServices.cronService().schedule(new Runnable() {
			@Override
			public void run() {
				Init_VeteranRewards();
			}
		}, VETERAN_REWARDS_LOOP_STATUS_BROADCAST_SCHEDULE);
		log.info(I18n.get("log.364637c5d487", VETERAN_REWARDS_LOOP_STATUS_BROADCAST_SCHEDULE));
	}

	/**
	 * 从数据库重新加载待发老兵奖励并触发发放。
	 * Reload pending veteran rewards from the database and trigger delivery.
	 */
	private void Init_VeteranRewards() {
		if (veteran_rewards != null) {
			veteran_rewards.clear();
		}

		veteran_rewards = new HashSet<VeteranRewards>(getDAO().getVeteranReward());

		if (veteran_rewards.size() > 0) {
			if (VeteranRewardConfig.VETERANREWARDS_ENABLED_INFO_LOG) {
				log.info(I18n.get("log.a6f073be0773", veteran_rewards.size()));
			}
			StartVeteranReward();
		}
	}

	/**
	 * 遍历缓存奖励列表并逐条校验发放。
	 * Iterate the cached reward list and verify/deliver each entry.
	 */
	private void StartVeteranReward() {
		if (VeteranRewardConfig.VETERANREWARDS_ENABLED_INFO_LOG) {
			log.info(I18n.get("log.0ebd84848543"));
		}

		for (final VeteranRewards veteran_reward : veteran_rewards) {
			VerifyVeteranReward(veteran_reward.getId(), veteran_reward.getPlayer(), veteran_reward.getType(),
					veteran_reward.getItem(), veteran_reward.getCount(), veteran_reward.getKinah(),
					veteran_reward.getSender(), veteran_reward.getTitle(), veteran_reward.getMessage());
		}
	}

	/**
	 * 校验单条数据库奖励并发送邮件，成功后回收记录。
	 * Verify a single DB reward, mail it, then recycle the record on success.
	 *
	 * @param id      奖励记录 ID / Reward record ID
	 * @param Player  接收玩家名 / Recipient player name
	 * Mail type
	 * Item ID
	 * Item count
	 * Kinah amount
	 * Sender
	 * @param title   邮件标题 / Mail title
	 * Mail body
	 */
	private void VerifyVeteranReward(int id, String Player, int typeID, int itemID, int countID, int kinahID,
			String sender, String title, String message) {
		String recipient = null;
		recipient = Util.convertName(Player);

		int item = 0, count = 0, kinah = 0, mailtype = 0;
		String Sender = sender;
		String Title = title;
		String Message = message;

		item = itemID;
		count = countID;
		kinah = kinahID;
		mailtype = typeID;

		if (item <= 0) {
			item = 0;
		}

		if (count <= 0) {
			count = -1;
		}

		SendVeteranRewardMail(Sender, recipient, Title, Message, item, count, kinah, mailtype);

		if (item != 0) {
		} else if (kinah > 0) {
		}

		if (id > 1) {
			RecycleVeteranReward(id);
		}
	}

	/**
	 * 按接收方类型发放老兵奖励（指定玩家或按种族广播）。
	 * Deliver a veteran reward filtered by recipient type (specific player or race broadcast).
	 *
	 * @param id            奖励记录 ID / Reward record ID
	 * @param Player        接收玩家名 / Recipient player name
	 * Mail type
	 * Item ID
	 * Item count
	 * Kinah amount
	 * Sender
	 * @param title         邮件标题 / Mail title
	 * Mail body
	 * @param recipientType 接收方类型 / Recipient type
	 */
	public void VerifyVeteranReward(int id, String Player, int typeID, int itemID, int countID, int kinahID,
			String sender, String title, String message, RecipientType recipientType) {
		String recipient = null;

		recipient = Util.convertName(Player);

		int item = 0, count = 0, kinah = 0, mailtype = 0;
		String Sender = sender;
		String Title = title;
		String Message = message;

		item = itemID;
		count = countID;
		kinah = kinahID;
		mailtype = typeID;

		if (item <= 0) {
			item = 0;
		}

		if (count <= 0) {
			count = -1;
		}

		if (recipientType == RecipientType.PLAYER) {
			SendVeteranRewardMail(Sender, recipient, Title, Message, item, count, kinah, mailtype);
		} else {
			for (Player player : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
				if (recipientType.isAllowed(player.getCommonData().getRace())) {
					SendVeteranRewardMail(Sender, player.getName(), Title, Message, item, count, kinah, mailtype);
				}
			}
		}
	}

	/**
	 * 删除已处理的老兵奖励记录。
	 * Delete a processed veteran reward record.
	 *
	 * Reward ID
	 */
	private void RecycleVeteranReward(final int rewardId) {
		getDAO().delVeteranReward(rewardId);
	}

	/**
	 * 向指定玩家发送老兵奖励邮件（在线推送，离线写库）。
	 * Send a veteran reward mail to the given player (push if online, store if offline).
	 *
	 * Sender
	 * Recipient name
	 * Title
	 * Message body
	 * @param attachedItemObjId  附件物品模板 ID / Attached item template ID
	 * Attached item count
	 * Attached kinah
	 * @param mailtype           邮件类型编码 / Mail type code
	 */
	private void SendVeteranRewardMail(String sender, String recipientName, String title, String message,
			int attachedItemObjId, int attachedItemCount, int attachedKinahCount, int mailtype) {
		if (attachedItemObjId != 0) {
			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(attachedItemObjId);
			if (itemTemplate == null) {
				if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
					// log.error(I18n.get("log.9f83472dcd76", sender, recipientName, // "] RETURN ITEM ID:", itemTemplate, " ITEM COUNT "
					//, attachedItemCount, attachedKinahCount));
				}
				return;
			}
		}

		if (attachedItemCount == 0) {
			return;
		}

		if (recipientName.length() > 16) {
			if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
				// log.error(I18n.get("log.d82411483d33", sender, recipientName, // "] ITEM RETURN", attachedItemObjId, " ITEM COUNT "
				//, attachedItemCount, attachedKinahCount));
			}
			return;
		}

		if (sender.length() > 16) {
			if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
				// log.error(I18n.get("log.50e74bfd8bb0", sender, recipientName, // "] ITEM RETURN", attachedItemObjId, " ITEM COUNT "
				//, attachedItemCount, attachedKinahCount));
			}
			return;
		}

		if (title.length() > 20) {
			title = title.substring(0, 20);
		}

		if (message.length() > 1000) {
			message = message.substring(0, 1000);
		}

		PlayerCommonData recipientCommonData = (DAOManager.getDAO(PlayerDAO.class))
				.loadPlayerCommonDataByName(recipientName);

		if (recipientCommonData == null) {
			if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
				log.error(I18n.get("log.c085309347c4", recipientName));
			}
			return;
		}

		Player onlineRecipient = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipientCommonData.getPlayerObjId());

		if (recipientCommonData.isOnline()) {
			if (!onlineRecipient.getMailbox().haveFreeSlots()) {
				if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
					// log.error(I18n.get("log.9152b5a91e37", sender, // onlineRecipient.getName(), attachedItemObjId
					//, attachedItemCount, attachedKinahCount, // " MAILBOX FULL "));
				}
				return;
			}
		} else {
			if (recipientCommonData.getMailboxLetters() >= 100) {
				if (VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG) {
					// log.error(I18n.get("log.4f69a14c14ed", sender, recipientName, // "] ITEM RETURN ", attachedItemObjId, " ITEM COUNT "
					//, attachedItemCount, attachedKinahCount));
				}
				return;
			}
			onlineRecipient = null;
		}

		Item attachedItem = null;
		int finalAttachedKinahCount = 0;
		int finalAttachedApCount = 0;
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

		LetterType type;
		if (mailtype == 1) {
			type = LetterType.EXPRESS;
		} else if (mailtype == 2) {
			type = LetterType.BLACKCLOUD;
		} else {
			type = LetterType.NORMAL;
		}

		String finalSender = sender;

		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());
		Letter newLetter = new Letter(GameWorldBootstrapServices.idFactory().nextId(), recipientCommonData.getPlayerObjId(),
				attachedItem, finalAttachedKinahCount, finalAttachedApCount, title, message, finalSender, time, true,
				type);

		if (!DAOManager.getDAO(MailDAO.class).storeLetter(time, newLetter)) {
			return;
		}

		if (attachedItem != null) {
			if (!DAOManager.getDAO(InventoryDAO.class).store(attachedItem, recipientCommonData.getPlayerObjId())) {
				return;
			}
		}

		if (onlineRecipient != null) {
			Mailbox recipientMailbox = onlineRecipient.getMailbox();
			recipientMailbox.putLetterToMailbox(newLetter);
			PacketSendUtility.sendPacket(onlineRecipient, new SM_MAIL_SERVICE(onlineRecipient.getMailbox()));
			recipientMailbox.isMailListUpdateRequired = true;

			if (recipientMailbox.mailBoxState != 0) {
				boolean isPostman = (recipientMailbox.mailBoxState
						& PlayerMailboxState.EXPRESS) == PlayerMailboxState.EXPRESS;
				PacketSendUtility.sendPacket(onlineRecipient,
						new SM_MAIL_SERVICE(onlineRecipient, recipientMailbox.getLetters(), isPostman));
			}
			PacketSendUtility.sendPacket(onlineRecipient, SM_SYSTEM_MESSAGE.STR_POSTMAN_NOTIFY);
		}

		if (!recipientCommonData.isOnline()) {
			recipientCommonData.setMailboxLetters(recipientCommonData.getMailboxLetters() + 1);
			DAOManager.getDAO(MailDAO.class).updateOfflineMailCounter(recipientCommonData);
		}

		if (VeteranRewardConfig.VETERANREWARDS_ENABLED_INFO_LOG) {
			// " + "Item: " +
			// " + "Item Count: " + attachedItemCount + " / "
			// " + "Status: successfully."));
		}
	}

	private VeteranRewardsDAO getDAO() {
		return DAOManager.getDAO(VeteranRewardsDAO.class);
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则回退本地单例）。
	 * Get the service singleton (prefer Spring ObjectProvider, otherwise local holder).
	 *
	 * Service instance
	 */
	public static final VeteranRewardsService getInstance() {
		ObjectProvider<VeteranRewardsService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<VeteranRewardsService> instanceProvider) {
		VeteranRewardsService.instanceProvider = instanceProvider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final VeteranRewardsService instance = new VeteranRewardsService();
	}
}
