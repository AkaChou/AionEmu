package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员添加表情命令：为目标玩家解锁表情动作。
 * Admin add-emotion command: unlocks an emotion action for the target player.
 *
 * @author ginho1, Damon
 */
public class AddEmotion extends AdminCommand {

	/**
	 * 注册 {@code //addemotion} 命令。
	 * Registers the {@code //addemotion} command.
	 */
	public AddEmotion() {
		super("addemotion");
	}

	/**
	 * 执行添加表情：解析表情 ID/过期时间，或展示 HTML 列表。
	 * Executes add-emotion: parses emotion id/expiry, or shows the HTML list.
	 *
	 * admin
	 * @param params 参数：表情 ID、过期分钟或 html / emotion id, expire minutes, or html
	 */
	@Override
	public void execute(Player admin, String... params) {
		
		long expireMinutes = 0;
		int emotionId = 0;
		VisibleObject target = null;
		Player finalTarget = null;
			
		if((params.length < 1) || (params.length > 2)) {
			PacketSendUtility.sendMessage(admin, "syntax: //addemotion <emotion id [expire time] || html>\nhtml to show html with names.");
			return;
		}
		
		try {
			emotionId = Integer.parseInt(params[0]);
			if(params.length == 2)
				expireMinutes = Long.parseLong(params[1]);
		}
		catch (NumberFormatException ex) {
			if(params[0].equalsIgnoreCase("html"))
				HTMLService.showHTML(admin, GameStaticDataServices.htmlCache().getHTML("emote.xhtml"));
				return;
		}

		if(emotionId < 1 || (emotionId > 35 && emotionId < 64) || emotionId > 129) {
			PacketSendUtility.sendMessage(admin, "Invalid <emotion id>, must be in intervals : [1-35]U[64-129]");
			return;
		}
		
		target = admin.getTarget();
		
		if (target == null) {
			finalTarget = admin;
		} 
		else if (target instanceof Player) {
			finalTarget = (Player) target;
		}
		
		if(finalTarget.getEmotions().contains(emotionId)) {
			PacketSendUtility.sendMessage(admin, "Target has aldready this emotion !");
			return;
		}
			
		if(params.length == 2) {
			finalTarget.getEmotions().add(emotionId, (int)((System.currentTimeMillis()/1000)+expireMinutes*60), true);
		}
		else {
			finalTarget.getEmotions().add(emotionId, 0, true);
		}
	}
}