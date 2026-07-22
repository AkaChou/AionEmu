package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 聊天命令基类，封装别名、权限等级与执行入口。
 * Base class for chat/admin commands: alias, access level and execution entry.
 *
 * @author KID
 */
@Slf4j
public abstract class ChatCommand {

	/**
	 * 命令别名（不含前缀）。
	 * Command alias (without prefix).
	 */
	private String alias;

	/**
	 * 所需访问等级。
	 * Required access level.
	 */
	private Byte level;

	/**
	 * 无参数时使用的空参数数组。
	 * Empty params array used when no arguments are provided.
	 */
	static final String[] EMPTY_PARAMS = new String[] {};

	/**
	 * 以给定别名构造命令。
	 * Construct a command with the given alias.
	 *
	 * @param alias 命令别名 / Command alias
	 */
	public ChatCommand(String alias) {
		this.alias = alias;
	}

	/**
	 * 安全执行命令，异常时记录日志并回调 onFail。
	 * Run the command safely; on exception log and call onFail.
	 *
	 * Invoking player
	 * Command arguments
	 * True on success
	 */
	public boolean run(Player player, String... params) {
		try {
			execute(player, params);
			return true;
		} catch (Exception e) {
			log.error(I18n.get("log.da39a3ee5e6b", e), e);
			onFail(player, e.getMessage());
			return false;
		}
	}

	/**
	 * 获取命令别名。
	 * Get the command alias.
	 *
	 * Alias
	 */
	public final String getAlias() {
		return alias;
	}

	/**
	 * 设置所需访问等级。
	 * Set the required access level.
	 *
	 * @param level 访问等级 / Access level
	 */
	public void setAccessLevel(Byte level) {
		this.level = level;
	}

	/**
	 * 获取所需访问等级。
	 * Get the required access level.
	 *
	 * Access level
	 */
	public final Byte getLevel() {
		return level;
	}

	/**
	 * 检查玩家是否满足权限。
	 * Check whether the player meets the access requirement.
	 *
	 * 玩家 / Player
	 * @return 有权限则为 true / True if allowed
	 */
	abstract boolean checkLevel(Player player);

	/**
	 * 解析文本并处理命令（含权限校验）。
	 * Parse text and process the command (including permission check).
	 *
	 * 玩家 / Player
	 * @param text 去掉前缀后的命令文本 / Command text without prefix
	 * @return 是否已处理 / Whether handled
	 */
	abstract boolean process(Player player, String text);

	/**
	 * 执行命令业务逻辑。
	 * Execute the command business logic.
	 *
	 * 玩家 / Player
	 * Arguments
	 */
	public abstract void execute(Player player, String... params);

	/**
	 * 执行失败时的默认反馈。
	 * Default failure feedback.
	 *
	 * 玩家 / Player
	 * Error message
	 */
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, message);
	}
}
