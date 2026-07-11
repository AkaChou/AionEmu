package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 管理员聊天命令，基于访问等级鉴权并可选记录 GM 审计日志。
 * Admin chat command with access-level checks and optional GM audit logging.
 *
 * @author synchro2
 */
@Slf4j(topic = "ADMINAUDIT_LOG")
public abstract class AdminCommand extends ChatCommand {

	/**
	 * 以给定别名构造管理员命令。
	 * Construct an admin command with the given alias.
	 *
	 * @param alias 命令别名 / Command alias
	 */
	public AdminCommand(String alias) {
		super(alias);
	}

	/**
	 * 校验玩家访问等级是否足够。
	 * Check whether the player's access level is sufficient.
	 *
	 * @param player 玩家 / Player
	 * @return 有权限则为 true / True if allowed
	 */
	@Override
	public boolean checkLevel(Player player) {
		return player.getAccessLevel() >= getLevel();
	}

	/**
	 * 处理管理员命令：鉴权、拆分参数、执行并写审计日志。
	 * Process an admin command: authorize, split args, execute and audit-log.
	 *
	 * 玩家 / Player
	 * @param text 去掉前缀后的命令文本 / Command text without prefix
	 * @return 是否已处理 / Whether handled
	 */
	@Override
	boolean process(Player player, String text) {

		if (!checkLevel(player)) {
			if (LoggingConfig.LOG_GMAUDIT) {
				log.info(I18n.get("log.47c48a6b530f", player.getName(), getAlias()));
			}
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player,
						"[WARN] You need to have access level " + this.getLevel() + " or more to use " + getAlias());
				return true;
			}
			return false;
		}

		boolean success = false;
		if (text.length() == getAlias().length()) {
			success = this.run(player, EMPTY_PARAMS);
		} else {
			success = this.run(player, text.substring(getAlias().length() + 1).split(" "));
		}

		if (LoggingConfig.LOG_GMAUDIT) {
			if (player.getTarget() != null && player.getTarget() instanceof Creature) {
				Creature target = (Creature) player.getTarget();
				log.info(I18n.get("log.32147246e963", player.getName(), target.getName(), text));
			} else {
				log.info(I18n.get("log.29261bc60f1d", player.getName(), text));
			}
		}

		if (!success) {
			PacketSendUtility.sendMessage(player, "<You have failed to execute " + text + ">");
			return true;
		} else {
			return success;
		}
	}
}
