package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.NoArgsConstructor;

/**
 * 聊天命令处理器：加载、注册，并将玩家输入路由到已注册命令。
 * Chat command processor: loads, registers and routes player input to commands.
 *
 * @author KID
 * @author Rolandas
 */
@Slf4j(topic = "ADMINAUDIT_LOG")
@NoArgsConstructor
public class ChatProcessor implements GameEngine {

	/**
	 * Spring ObjectProvider，优先于静态单例。
	 * Spring ObjectProvider preferred over the static singleton.
     * -- SETTER --
     *  注入 Spring 实例提供者。
     *  Inject the Spring instance provider.
     *  Provider

     */
	@Setter
    private static volatile ObjectProvider<ChatProcessor> instanceProvider;
	/**
	 * 回退单例实例。
	 * Fallback singleton instance.
	 */
	private static final ChatProcessor instance = new ChatProcessor();
	/**
	 * 别名 → 命令映射。
	 * Alias-to-command map.
	 */
	private volatile Map<String, ChatCommand> commands = new HashMap<>();
	/**
	 * 别名 → 访问等级映射（来自配置）。
	 * Alias-to-access-level map (from config).
	 */
	private Map<String, Byte> accessLevel = new HashMap<>();

	/**
	 * 获取处理器实例（优先 Spring 提供者）。
	 * Get the processor instance (Spring provider preferred).
	 *
	 * Processor
	 */
	public static ChatProcessor getInstance() {
		ObjectProvider<ChatProcessor> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

    /**
	 * 加载并初始化聊天命令。
	 * Load and initialize chat commands.
	 *
	 * @param progressLatch 进度锁存器 / Progress latch
	 */
	@Override
	public void load(CountDownLatch progressLatch) {
		try {
			log.info(I18n.get("log.cc321cc5ac7d"));
			init(this);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	/**
	 * 关闭钩子（当前无操作）。
	 * Shutdown hook (no-op).
	 */
	@Override
	public void shutdown() {
	}

	/**
	 * 加载权限配置并编译注册 admin/player 命令脚本。
	 * Load access levels and compile/register admin/player command scripts.
	 *
	 * @param processor 注册目标处理器 / Target processor for registration
	 */
	private void init(ChatProcessor processor) {
		loadLevels();

		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new ChatCommandsLoader(processor));
		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.commands.admin", "com.aionemu.gameserver.commands.player"));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize chat handlers.", e);
		}
	}

	/**
	 * 注册命令的全部别名，并逐个绑定配置中的访问等级。
	 * Registers every alias of a command and binds each alias's access level from config.
	 * <p>
	 * 单个别名缺少配置时只跳过该别名（例如升级时运行期配置尚未补充中文别名），
	 * 其余别名仍照常注册。
	 * A single alias without a config entry is skipped on its own (for example a Chinese alias that a
	 * runtime config has not been updated with yet), while the other aliases stay registered.
	 *
	 * Command
	 */
	public void registerCommand(ChatCommand cmd) {
		for (String alias : cmd.getAliases()) {
			if (commands.containsKey(alias)) {
				log.warn(I18n.get("log.c03ebfafc509", alias));
				continue;
			}
			if (!accessLevel.containsKey(alias)) {
				log.warn(I18n.get("log.8812baf3b55e", alias));
				continue;
			}
			cmd.setAccessLevel(alias, accessLevel.get(alias));
			commands.put(alias, cmd);
		}
	}

	/**
	 * 热重载命令与权限配置。
	 * Hot-reload commands and access-level config.
	 */
	public void reload() {
		try {
			ChatProcessor reloaded = new ChatProcessor();
			reloaded.init(reloaded);
			accessLevel = reloaded.accessLevel;
			commands = reloaded.commands;
		} catch (Throwable e) {
			throw new GameServerError("Can't reload chat handlers.", e);
		}
	}

	/**
	 * 从 administration/commands.properties 加载访问等级。
	 * Load access levels from administration/commands.properties.
	 */
	private void loadLevels() {
		accessLevel.clear();
		try {
			File configFile = Config.configFile("administration/commands.properties");
			java.util.Properties props = new java.util.Properties();
			// 别名键含中文（如 移动/掉落），必须以 UTF-8 读取，不能用 Properties 默认的 ISO-8859-1。
			// Alias keys contain Chinese characters (移动/掉落), so read the file as UTF-8 instead of
			// the ISO-8859-1 default used by Properties.load(InputStream).
			try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
				props.load(reader);
			}

			for (Object key : props.keySet()) {
				String str = (String) key;
				accessLevel.put(str, Byte.valueOf(props.getProperty(str).trim()));
			}
		} catch (IOException e) {
			log.error(I18n.get("log.541680aef6cb"), e);
		}
	}

	/**
	 * 将玩家聊天输入路由到对应命令。
	 * Route player chat input to the matching command.
	 * <p>
	 * {@code //} 前缀走 AdminCommand；{@code .} 前缀走 PlayerCommand
	 *（或在启用时也接受 AdminCommand）。
	 * Prefix {@code //} → AdminCommand; {@code .} → PlayerCommand
	 * (or AdminCommand when enabled).
	 *
	 * @param player 玩家 / Player
	 * @param text 原始聊天文本 / Raw chat text
	 * @return 已处理则为 true / True if handled
	 */
	public boolean handleChatCommand(Player player, String text) {
		if (text.split(" ").length == 0)
			return false;
		if (text.startsWith("//") && getCommand(text.substring(2)) instanceof AdminCommand) {
			return (getCommand(text.substring(2))).process(player, text.substring(2));
		} else if (text.startsWith(".") && (getCommand(text.substring(1)) instanceof PlayerCommand
				|| (CustomConfig.ENABLE_ADMIN_DOT_COMMANDS && getCommand(text.substring(1)) instanceof AdminCommand))) {
			return (getCommand(text.substring(1))).process(player, text.substring(1));
		} else {
			return false;
		}
	}

	/**
	 * 按文本首词查找已注册命令。
	 * Look up a registered command by the first token of the text.
	 *
	 * @param text 去掉前缀后的文本 / Text without prefix
	 * Command or null
	 */
	private ChatCommand getCommand(String text) {
		String alias = text.split(" ")[0];
		ChatCommand cmd = this.commands.get(alias);
		return cmd;
	}

	/**
	 * 命令编译/注册完成后的回调日志。
	 * Callback log after command compile/registration completes.
	 */
	public void onCompileDone() {
		log.info(I18n.get("log.571a186ccd17", commands.size()));
	}
}
