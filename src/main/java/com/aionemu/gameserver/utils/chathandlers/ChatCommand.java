package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 聊天命令基类，封装别名、权限等级与执行入口。
 * Base class for chat/admin commands: alias, access level and execution entry.
 * <p>
 * 一个命令可以声明多个别名（主别名在前，例如 {@code dropinfo} 与中文别名 {@code 掉落}）；
 * 每个别名在 administration/commands.properties 中的访问等级是独立绑定的。
 * A command may declare several aliases (primary first, e.g. {@code dropinfo} plus the Chinese
 * alias {@code 掉落}); each alias binds its own access level from administration/commands.properties.
 * @author KID
 */
@Slf4j
public abstract class ChatCommand {

	/**
	 * 命令别名列表（主别名在前）。
	 * Command aliases, primary alias first.
	 */
	private final List<String> aliases;

	/**
	 * 别名 → 访问等级映射（注册时由配置绑定）。
	 * Alias-to-access-level map (bound from config at registration).
	 */
	private final Map<String, Byte> aliasLevels = new HashMap<>();

	/**
	 * 无参数时使用的空参数数组。
	 * Empty params array used when no arguments are provided.
	 */
	static final String[] EMPTY_PARAMS = new String[] {};

	/**
	 * 以主别名（可附带额外别名）构造命令。
	 * Constructs a command with the primary alias and optional additional aliases.
	 * @param alias 主别名 / Primary alias
	 * @param alternateAliases 额外别名 / Additional aliases
	 */
	protected ChatCommand(String alias, String... alternateAliases) {
		List<String> registered = new ArrayList<>(1 + alternateAliases.length);
		registered.add(alias);
		for (String alternate : alternateAliases) {
			if (alternate != null && !alternate.isEmpty() && !registered.contains(alternate)) {
				registered.add(alternate);
			}
		}
		this.aliases = List.copyOf(registered);
	}

	/**
	 * 安全执行命令，异常时记录日志并回调 onFail。
	 * Run the command safely; on exception log and call onFail.
	 * Invoking player
	 * Command arguments
	 * True on success
	 */
	public boolean run(Player player, String... params) {
		try {
			execute(player, params);
			return true;
		} catch (Exception e) {
			log.error(I18n.get("log.da39a3ee5e6b"), e);
			onFail(player, e.getMessage());
			return false;
		}
	}

	/**
	 * 获取主别名。
	 * Get the primary alias.
	 * Alias
	 */
	public final String getAlias() {
		return aliases.get(0);
	}

	/**
	 * 获取全部别名（主别名在前）。
	 * Get every alias, primary alias first.
	 * Aliases
	 */
	public final List<String> getAliases() {
		return aliases;
	}

	/**
	 * 解析玩家实际输入的别名（最长匹配，未匹配时退回主别名）。
	 * Resolves the alias the player actually typed (longest match, primary alias when unmatched).
	 * @param text 去掉前缀后的命令文本 / Command text without the prefix
	 * @return 匹配到的别名 / Matched alias
	 */
	public final String resolveAlias(String text) {
		String matched = null;
		for (String alias : aliases) {
			if (text.equals(alias) || text.startsWith(alias + ' ')) {
				if (matched == null || alias.length() > matched.length()) {
					matched = alias;
				}
			}
		}
		return matched == null ? getAlias() : matched;
	}

	/**
	 * 去掉匹配别名后的参数文本。
	 * Returns the argument text after the matched alias.
	 * @param text 去掉前缀后的命令文本 / Command text without the prefix
	 * @return 参数文本（可能为空） / Argument text (may be empty)
	 */
	protected final String argumentsOf(String text) {
		String alias = resolveAlias(text);
		return text.length() > alias.length() ? text.substring(alias.length() + 1).trim() : "";
	}

	/**
	 * 绑定某个别名的访问等级。
	 * Binds the access level of one alias.
	 * @param alias 别名 / Alias
	 * @param level 访问等级 / Access level
	 */
	public void setAccessLevel(String alias, Byte level) {
		aliasLevels.put(alias, level);
	}

	/**
	 * 获取主别名的访问等级。
	 * Get the access level of the primary alias.
	 * Access level
	 */
	public final Byte getLevel() {
		return aliasLevels.get(getAlias());
	}

	/**
	 * 获取指定别名的访问等级。
	 * Get the access level of the given alias.
	 * @param alias 别名 / Alias
	 * @return 访问等级或 null / Access level or null
	 */
	public final Byte getLevel(String alias) {
		return aliasLevels.get(alias);
	}

	/**
	 * 检查玩家是否满足权限。
	 * Check whether the player meets the access requirement.
	 * 玩家 / Player
	 * @param alias 实际使用的别名 / Alias that was used
	 * @return 有权限则为 true / True if allowed
	 */
	abstract boolean checkLevel(Player player, String alias);

	/**
	 * 解析文本并处理命令（含权限校验）。
	 * Parse text and process the command (including permission check).
	 * 玩家 / Player
	 * @param text 去掉前缀后的命令文本 / Command text without prefix
	 * @return 是否已处理 / Whether handled
	 */
	abstract boolean process(Player player, String text);

	/**
	 * 执行命令业务逻辑。
	 * Execute the command business logic.
	 * @param player 玩家 / Player
	 * @param params Arguments
	 */
	public abstract void execute(Player player, String... params);

	/**
	 * 执行失败时的默认反馈。
	 * Default failure feedback.
	 * @param player 玩家 / Player
	 * @param message Error message
	 */
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, message);
	}
}
