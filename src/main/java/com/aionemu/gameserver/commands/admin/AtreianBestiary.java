package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员亚特雷亚图鉴命令：为管理员增加指定 NPC 的图鉴击杀计数。
 * Admin Atreian Bestiary command: adds a bestiary kill count for a given NPC.
 *
 * @author Ranastic
 */
public class AtreianBestiary extends AdminCommand {

	/**
	 * 注册 {@code //bestiary} 命令。
	 * Registers the {@code //bestiary} command.
	 */
	public AtreianBestiary() {
		super("bestiary");
	}

	/**
	 * 执行图鉴击杀：校验模板后增加击杀计数。
	 * Executes bestiary kill: validates the template then increments kill count.
	 *
	 * admin
	 * npc id
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 2) {
			PacketSendUtility.sendMessage(admin, "syntax //bestiary <npc_id>");
			return;
		}
		int templateId = Integer.parseInt(params[0]);
		AtreianBestiaryTemplate template = DataManager.ATREIAN_BESTIARY.getAtreianBestiaryTemplateByNpcId(templateId);
		if (template != null) {
			GameFeatureServices.atreianBestiaryService().onKill(admin, templateId);
			PacketSendUtility.sendMessage(admin, "Added kill count to Atreian Bestiary for npc "+ templateId);
		}
		else {
			PacketSendUtility.sendMessage(admin, "Npc "+ templateId + " isn't exist in monster_books.xml");
		}
	}

	/**
	 * 参数错误时输出 {@code //bestiary} 用法。
	 * Prints {@code //bestiary} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //bestiary <npc_id>");
	}
}