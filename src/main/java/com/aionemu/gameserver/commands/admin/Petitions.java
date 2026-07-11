package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.PetitionType;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.Collection;

/**
 * 玩家工单（Petition）查询、删除与邮件回复的管理员命令。
 * Admin command to list, inspect, delete or mail-reply to player petitions.
 *
 * @author zdead
 */
public class Petitions extends AdminCommand {

	/**
	 * 以别名 {@code petition} 构造命令。
	 * Construct the command with alias {@code petition}.
	 */
	public Petitions() {
		super("petition");
	}

	/**
	 * 无参数列出待处理工单；{@code <id>} 查看详情；{@code <id> delete} 删除；{@code <id> reply ...} 邮件回复。
	 * With no args list open petitions; {@code <id>} shows details; {@code <id> delete} removes; {@code <id> reply ...} mails a reply.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 工单 ID 与操作 / Petition id and action
	 */
	@Override
	public void execute(Player admin, String... params) {
		// 发送门票一般信息 / Send ticket general info
		if (params == null || params.length == 0) {
			Collection<Petition> petitions = GameRuntimeServices.petitionService().getRegisteredPetitions();
			Petition[] petitionsArray = petitions.toArray(new Petition[0]);
			PacketSendUtility.sendMessage(admin, petitionsArray.length + " unprocessed petitions.");
			if (petitionsArray.length < 5) {
				PacketSendUtility.sendMessage(admin, "== " + petitionsArray.length + " first petitions to reply ==");
				for (int i = 0; i < petitionsArray.length; i++) {
					PacketSendUtility.sendMessage(admin, petitionsArray[i].getPetitionId() + " | "
						+ petitionsArray[i].getTitle());
				}
			}
			else {
				PacketSendUtility.sendMessage(admin, "== 5 first petitions to reply ==");
				for (int i = 0; i < 5; i++) {
					PacketSendUtility.sendMessage(admin, petitionsArray[i].getPetitionId() + " | "
						+ petitionsArray[i].getTitle());
				}
			}
			return;
		}

		int petitionId = 0;

		try {
			petitionId = Integer.parseInt(params[0]);
		}
		catch (NumberFormatException nfe) {
			PacketSendUtility.sendMessage(admin, "Invalid petition id.");
			return;
		}

		Petition petition = DAOManager.getDAO(PetitionDAO.class).getPetitionById(petitionId);

		if (petition == null) {
			PacketSendUtility.sendMessage(admin, "There is no petition with id #" + petitionId);
			return;
		}

		String petitionPlayer = "";
		boolean isOnline;

		if (com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(petition.getPlayerObjId()) != null) {
			petitionPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(petition.getPlayerObjId()).getName();
			isOnline = true;
		}
		else {
			petitionPlayer = DAOManager.getDAO(PlayerDAO.class).getPlayerNameByObjId(petition.getPlayerObjId());
			isOnline = false;
		}

		// 读取申诉 / Read petition
		if (params.length == 1) {
			StringBuilder message = new StringBuilder();
			message.append("== Petition #" + petitionId + " ==\n");
			message.append("Player: " + petitionPlayer + " (");
			if (isOnline)
				message.append("Online");
			else
				message.append("Offline");
			message.append(")\n");
			message.append("Type: " + getHumanizedValue(petition.getPetitionType()) + "\n");
			message.append("Title: " + petition.getTitle() + "\n");
			message.append("Text: " + petition.getContentText() + "\n");
			message.append("= Additional Data =\n");
			message.append(getFormattedAdditionalData(petition.getPetitionType(), petition.getAdditionalData()));
			PacketSendUtility.sendMessage(admin, message.toString());
		}
		// 删除 / Delete
		else if (params.length == 2 && params[1].equals("delete")) {
			GameRuntimeServices.petitionService().deletePetition(petition.getPlayerObjId());
			PacketSendUtility.sendMessage(admin, "Petition #" + petitionId + " deleted.");
		}
		// 回复 / Reply
		else if (params.length >= 3 && params[1].equals("reply")) {
			String replyMessage = "";
			for (int i = 2; i < params.length - 1; i++)
				replyMessage += params[i] + " ";
			replyMessage += params[params.length - 1];
			if (replyMessage.equals("")) {
				PacketSendUtility.sendMessage(admin, "You must specify a reply to that petition");
				return;
			}

			GameCoreGameplayServices.mailService().sendMail(admin, petitionPlayer, "GM-Re:" + petition.getTitle(), replyMessage, 0, 0, 0, 0,
				LetterType.NORMAL);
			GameRuntimeServices.petitionService().setPetitionReplied(petitionId);

			PacketSendUtility.sendMessage(admin, "Your reply has been sent to " + petitionPlayer
				+ ". Petition is now closed.");
		}
	}

	private String getHumanizedValue(PetitionType type) {
		String result = "";
		switch (type) {
			case CHARACTER_STUCK:
				result = "Character Stuck";
				break;
			case CHARACTER_RESTORATION:
				result = "Character Restoration";
				break;
			case BUG:
				result = "Bug";
				break;
			case QUEST:
				result = "Quest";
				break;
			case UNACCEPTABLE_BEHAVIOR:
				result = "Unacceptable Behavior";
				break;
			case SUGGESTION:
				result = "Suggestion";
			case INQUIRY:
				result = "Inquiry about the game";
			default:
				result = "Unknown";
		}
		return result;
	}

	private String getFormattedAdditionalData(PetitionType type, String additionalData) {
		String result = "";
		switch (type) {
			case CHARACTER_STUCK:
				result = "Character Location: " + additionalData;
				break;
			case CHARACTER_RESTORATION:
				result = "Category: " + additionalData;
				break;
			case BUG:
				String[] bugData = additionalData.split("/");
				result = "Time Occured: " + bugData[0] + "\n";
				result += "Zone and Coords: " + bugData[1];
				if (bugData.length > 2)
					result += "\nHow to Replicate: " + bugData[2];
				break;
			case QUEST:
				result = "Quest Title: " + additionalData;
				break;
			case UNACCEPTABLE_BEHAVIOR:
				String[] bData = additionalData.split("/");
				result = "Time Occured: " + bData[0] + "\n";
				result += "Character Name: " + bData[1] + "\n";
				result += "Category: " + bData[2];
				break;
			case SUGGESTION:
				//
				result = "Category: " + additionalData;
				break;
			case INQUIRY:
				//
				result = "Petition Category: " + additionalData;
				break;
			default:
				result = additionalData;
		}
		return result;
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //petition");
		PacketSendUtility.sendMessage(player, "Syntax: //petition <id>");
		PacketSendUtility.sendMessage(player, "Syntax: //petition <id> <reply | delete>");
	}
}
