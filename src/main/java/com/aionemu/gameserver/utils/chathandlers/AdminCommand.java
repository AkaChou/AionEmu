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
	 * 以给定别名（可附带额外别名）构造管理员命令。
	 * Construct an admin command with the given alias and optional additional aliases.
	 *
	 * @param alias 主别名 / Primary alias
	 * @param alternateAliases 额外别名 / Additional aliases
	 */
	public AdminCommand(String alias, String... alternateAliases) {
		super(alias, alternateAliases);
	}

	/**
	 * 校验玩家访问等级是否足够。
	 * Check whether the player's access level is sufficient.
	 *
	 * @param player 玩家 / Player
	 * @param alias 实际使用的别名 / Alias that was used
	 * @return 有权限则为 true / True if allowed
	 */
	@Override
	public boolean checkLevel(Player player, String alias) {
		Byte level = getLevel(alias);
		return level != null && player.getAccessLevel() >= level;
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

		String alias = resolveAlias(text);

		if (!checkLevel(player, alias)) {
			if (LoggingConfig.LOG_GMAUDIT) {
				log.info(I18n.get("log.47c48a6b530f", player.getName(), alias));
			}
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player,
						"[WARN] You need to have access level " + this.getLevel(alias) + " or more to use " + alias);
				return true;
			}
			return false;
		}

		boolean success = false;
		String arguments = argumentsOf(text);
		if (arguments.isEmpty()) {
			success = this.run(player, EMPTY_PARAMS);
		} else {
			// 连续/首尾空白不得产生空参数，否则命令参数会整体错位。
			// Consecutive or surrounding whitespace must not create empty arguments that shift the parameter list.
			success = this.run(player, arguments.split("\\s+"));
		}

		if (LoggingConfig.LOG_GMAUDIT) {
			if (player.getTarget() != null && player.getTarget() instanceof Creature target) {
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
