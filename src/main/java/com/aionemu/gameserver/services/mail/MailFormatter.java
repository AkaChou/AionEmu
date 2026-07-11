package com.aionemu.gameserver.services.mail;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.templates.mail.MailPart;
import com.aionemu.gameserver.model.templates.mail.MailTemplate;

/**
 * 系统邮件模板格式化器，负责黑云、房屋维护/拍卖及欧比斯/露娜奖励邮件。
 * System mail template formatter for black-cloud, house maintenance/auction, and abyss/luna reward mails.
 */
public final class MailFormatter {

	/**
	 * 发送黑云商城购买邮件。
	 * Sends a black-cloud cash purchase mail.
	 *
	 * @param recipientName 收件人名称 / recipient name
	 * item template id
	 * item count
	 */
	public static void sendBlackCloudMail(String recipientName, final int itemObjectId, final int itemCount) {
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$CASH_ITEM_MAIL", "",
				Race.PC_ALL);
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("itemid".equals(name)) {
					return Integer.toString(itemObjectId);
				} else if ("count".equals(name)) {
					return Integer.toString(itemCount);
				} else if ("unk1".equals(name)) {
					return "0";
				} else if ("purchasedate".equals(name)) {
					return Long.toString(System.currentTimeMillis() / 1000);
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(formatter);
		String body = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail("$$CASH_ITEM_MAIL", recipientName, title, body, itemObjectId,
				itemCount, 0, 0, LetterType.BLACKCLOUD);
	}

	/**
	 * 发送房屋维护逾期警告邮件。
	 * Sends a house maintenance overdue warning mail.
	 *
	 * owned house
	 * warning count (1-3)
	 * @param impoundTime 扣押时间戳（毫秒） / impound timestamp in ms
	 */
	public static void sendHouseMaintenanceMail(final House ownedHouse, int warnCount, final long impoundTime) {
		String templateName = "";
		switch (warnCount) {
		case 1:
			templateName = "$$HS_OVERDUE_FIRST";
			break;
		case 2:
			templateName = "$$HS_OVERDUE_SECOND";
			break;
		case 3:
			templateName = "$$HS_OVERDUE_FINAL";
			break;
		default:
			return;
		}
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate(templateName, "",
				ownedHouse.getPlayerRace());
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("address".equals(name)) {
					return Integer.toString(ownedHouse.getAddress().getId());
				} else if ("datetime".equals(name)) {
					return Long.toString(impoundTime / 1000);
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(null);
		String message = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail(templateName, ownedHouse.getButler().getMasterName(), title, message,
				0, 0, 0, 0, LetterType.NORMAL);
	}

	/**
	 * 发送房屋拍卖结果邮件。
	 * Sends a house auction result mail.
	 *
	 * owned house
	 * @param playerData 玩家公共数据 / player common data
	 * auction result
	 * @param time 结果时间戳（毫秒） / result timestamp in ms
	 * @param returnKinah 退还基纳数量 / returned kinah amount
	 */
	public static void sendHouseAuctionMail(final House ownedHouse, final PlayerCommonData playerData,
			final AuctionResult result, final long time, long returnKinah) {
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$HS_AUCTION_MAIL", "",
				playerData.getRace());
		if (ownedHouse == null || playerData == null || result == null) {
			return;
		}
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("address".equals(name)) {
					return Integer.toString(ownedHouse.getAddress().getId());
				} else if ("datetime".equals(name)) {
					return Long.toString(time / 1000);
				} else if ("resultid".equals(name)) {
					return Integer.toString(result.getId());
				} else if ("raceid".equals(name)) {
					return Integer.toString(playerData.getRace().getRaceId());
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail("$$HS_AUCTION_MAIL", playerData.getName(), title, message, 0, 0,
				returnKinah, 0, LetterType.NORMAL);
	}

	/**
	 * 发送欧比斯攻城奖励邮件（附基纳）。
	 * Sends an abyss siege reward mail with attached kinah.
	 *
	 * siege location
	 * @param playerData 玩家公共数据 / player common data
	 * @param level 欧比斯攻城等级 / abyss siege level
	 * siege result
	 * @param time 结果时间戳（毫秒） / result timestamp in ms
	 * attached item id
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedKinahCount 附件基纳数量 / attached kinah count
	 */
	public static void sendAbyssRewardMail(final SiegeLocation siegeLocation, final PlayerCommonData playerData,
			final AbyssSiegeLevel level, final SiegeResult result, final long time, int attachedItemObjId,
			long attachedItemCount, long attachedKinahCount) {
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$ABYSS_REWARD_MAIL", "",
				playerData.getRace());
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("siegelocid".equals(name)) {
					return Integer.toString(siegeLocation.getTemplate().getId());
				} else if ("datetime".equals(name)) {
					return Long.toString(time / 1000);
				} else if ("rankid".equals(name)) {
					return Integer.toString(level.getId());
				} else if ("raceid".equals(name)) {
					return Integer.toString(playerData.getRace().getRaceId());
				} else if ("resultid".equals(name)) {
					return Integer.toString(result.getId());
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail("$$ABYSS_REWARD_MAIL", playerData.getName(), title, message,
				attachedItemObjId, attachedItemCount, attachedKinahCount, 0, LetterType.NORMAL);
	}

	/**
	 * 发送欧比斯攻城奖励邮件（附欧比斯点数）。
	 * Sends an abyss siege reward mail with attached abyss points.
	 *
	 * siege location
	 * @param playerData 玩家公共数据 / player common data
	 * @param level 欧比斯攻城等级 / abyss siege level
	 * siege result
	 * @param time 结果时间戳（毫秒） / result timestamp in ms
	 * attached item id
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedApCount 附件欧比斯点数 / attached abyss points
	 */
	public static void sendAbyssPointRewardMail(final SiegeLocation siegeLocation, final PlayerCommonData playerData,
			final AbyssSiegeLevel level, final SiegeResult result, final long time, int attachedItemObjId,
			long attachedItemCount, long attachedApCount) {
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$ABYSS_REWARD_MAIL", "",
				playerData.getRace());
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("siegelocid".equals(name)) {
					return Integer.toString(siegeLocation.getTemplate().getId());
				} else if ("datetime".equals(name)) {
					return Long.toString(time / 1000);
				} else if ("rankid".equals(name)) {
					return Integer.toString(level.getId());
				} else if ("raceid".equals(name)) {
					return Integer.toString(playerData.getRace().getRaceId());
				} else if ("resultid".equals(name)) {
					return Integer.toString(result.getId());
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail("$$ABYSS_REWARD_MAIL", playerData.getName(), title, message,
				attachedItemObjId, attachedItemCount, 0, attachedApCount, LetterType.NORMAL);
	}

	/**
	 * 发送露娜奖励邮件。
	 * Sends a luna reward mail.
	 *
	 * siege location
	 * @param playerData 玩家公共数据 / player common data
	 * @param level 欧比斯攻城等级 / abyss siege level
	 * siege result
	 * @param time 结果时间戳（毫秒） / result timestamp in ms
	 * attached item id
	 * @param attachedItemCount 附件物品数量 / attached item count
	 * @param attachedKinahCount 附件基纳数量 / attached kinah count
	 */
	public static void sendLunaRewardMail(final SiegeLocation siegeLocation, final PlayerCommonData playerData,
			final AbyssSiegeLevel level, final SiegeResult result, final long time, int attachedItemObjId,
			long attachedItemCount, long attachedKinahCount) {
		final MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$LUNA_REWARD_MAIL", "",
				playerData.getRace());
		MailPart formatter = new MailPart() {
			@Override
			public String getParamValue(String name) {
				if ("siegelocid".equals(name)) {
					return Integer.toString(siegeLocation.getTemplate().getId());
				} else if ("datetime".equals(name)) {
					return Long.toString(time / 1000);
				} else if ("rankid".equals(name)) {
					return Integer.toString(level.getId());
				} else if ("raceid".equals(name)) {
					return Integer.toString(playerData.getRace().getRaceId());
				} else if ("resultid".equals(name)) {
					return Integer.toString(result.getId());
				}
				return "";
			}
		};
		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);
		GameFeatureServices.systemMailService().sendMail("$$LUNA_REWARD_MAIL", playerData.getName(), title, message,
				attachedItemObjId, attachedItemCount, attachedKinahCount, 0, LetterType.NORMAL);
	}
}
