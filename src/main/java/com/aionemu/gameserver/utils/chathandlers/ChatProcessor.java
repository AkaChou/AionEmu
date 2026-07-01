/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.utils.chathandlers;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.commons.utils.PropertiesUtils;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * @author KID
 * @Modified Rolandas
 */
@Slf4j(topic = "ADMINAUDIT_LOG")
public class ChatProcessor implements GameEngine {

	private static volatile ObjectProvider<ChatProcessor> instanceProvider;
	private static ChatProcessor instance = new ChatProcessor();
	private Map<String, ChatCommand> commands = new HashMap<String, ChatCommand>();
	private Map<String, Byte> accessLevel = new HashMap<String, Byte>();

	public static ChatProcessor getInstance() {
		ObjectProvider<ChatProcessor> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

	public static void setInstanceProvider(ObjectProvider<ChatProcessor> provider) {
		instanceProvider = provider;
	}

	@Override
	public void load(CountDownLatch progressLatch) {
		try {
			log.info("Chat processor load started");
			init(this);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	@Override
	public void shutdown() {
	}

	public ChatProcessor() {
	}

	private void init(ChatProcessor processor) {
		loadLevels();

		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new ChatCommandsLoader(processor));
		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.commands.admin", "com.aionemu.gameserver.commands.player", "com.aionemu.gameserver.commands.wedding"));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize chat handlers.", e);
		}
	}

	public void registerCommand(ChatCommand cmd) {
		if (commands.containsKey(cmd.getAlias())) {
			log.warn("Command " + cmd.getAlias() + " is already registered. Fail");
			return;
		}
		if (!accessLevel.containsKey(cmd.getAlias())) {
			log.warn("Command " + cmd.getAlias() + " do not have access level. Fail");
			return;
		}
		cmd.setAccessLevel(accessLevel.get(cmd.getAlias()));
		commands.put(cmd.getAlias(), cmd);
	}

	public void reload() {
		Map<String, ChatCommand> backupCommands = new HashMap<String, ChatCommand>(commands);
		commands.clear();

		try {
			ChatProcessor adminCP = new ChatProcessor();
			adminCP.init(adminCP);
			backupCommands.clear();
			instance = adminCP;
		} catch (Throwable e) {
			commands = backupCommands;
			throw new GameServerError("Can't reload chat handlers.", e);
		}
	}

	private void loadLevels() {
		accessLevel.clear();
		try {
			java.util.Properties props = PropertiesUtils.load(Config.configFile("administration/commands.properties").getPath());

			for (Object key : props.keySet()) {
				String str = (String) key;
				accessLevel.put(str, Byte.valueOf(props.getProperty(str).trim()));
			}
		} catch (IOException e) {
			log.error("Can't read commands.properties", e);
		}
	}

	public boolean handleChatCommand(Player player, String text) {
		if (text.split(" ").length == 0)
			return false;
		if ((text.startsWith("//") && getCommand(text.substring(2)) instanceof AdminCommand)
				|| (text.startsWith("..") && getCommand(text.substring(2)) instanceof WeddingCommand)) {
			return (getCommand(text.substring(2))).process(player, text.substring(2));
		} else if (text.startsWith(".") && (getCommand(text.substring(1)) instanceof PlayerCommand
				|| (CustomConfig.ENABLE_ADMIN_DOT_COMMANDS && getCommand(text.substring(1)) instanceof AdminCommand))) {
			return (getCommand(text.substring(1))).process(player, text.substring(1));
		} else {
			return false;
		}
	}

	private ChatCommand getCommand(String text) {
		String alias = text.split(" ")[0];
		ChatCommand cmd = this.commands.get(alias);
		return cmd;
	}

	public void onCompileDone() {
		log.info("Loaded " + commands.size() + " commands.");
	}
}
