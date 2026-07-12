package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.GameServerError;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.*;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;

/**
 * 运行时热重载指令；可重载任务、技能、传送门、指令、掉落、商城、活动与配置。
 * Runtime hot-reload admin command for quests, skills, portals, chat commands, drops, shop, events and config.
 *
 * @author MrPoke
 */
@Slf4j
public class Reload extends AdminCommand {


	public Reload() {
		super("reload");
	}

	/**
	 * 按子命令热重载对应静态数据或运行时组件。
	 * Hot-reloads the static data or runtime component selected by the sub-command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * @param params 单一子命令：quest / skill/portal/commands/drop/gameshop/events/config
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length != 1) {
			PacketSendUtility.sendMessage(admin, "syntax //reload <quest | skill | portal | commands | drop | gameshop | events | config>");
			return;
		}
		if (params[0].equals("quest")) {
			try {
				XmlDataLoader loader = XmlDataLoader.getInstance();
				reloadQuests(loader.loadQuestData().getQuestsData(), loader.loadQuestScripts().getQuest());
				PacketSendUtility.sendMessage(admin, "Quest reload Success!");
			}
			catch (Exception | GameServerError e) {
				PacketSendUtility.sendMessage(admin, "Quest reload failed!");
				log.error(I18n.get("log.bc69156970fe", e));
			}
		}

		else if (params[0].equals("skill")) {
			try {
				DataManager.SKILL_DATA = XmlDataLoader.getInstance().loadSkillData();
				PacketSendUtility.sendMessage(admin, "Skill reload Success!");
			}
			catch (RuntimeException e) {
				PacketSendUtility.sendMessage(admin, "Skill reload failed!");
				log.error(I18n.get("log.9229e36d9667", e));
			}
		}
		else if (params[0].equals("portal")) {
			try {
				JAXBContext jc = JAXBContext.newInstance(StaticData.class);
				Unmarshaller un = jc.createUnmarshaller();
				PortalLocData portalLocData = (PortalLocData) un.unmarshal(Config.dataFile("./data/static_data/portals/portal_loc.xml"));
				Portal2Data portal2Data = (Portal2Data) un.unmarshal(Config.dataFile("./data/static_data/portals/portal_template2.xml"));
				DataManager.PORTAL_LOC_DATA = portalLocData;
				DataManager.PORTAL2_DATA = portal2Data;
				PacketSendUtility.sendMessage(admin, "Portal reload Success!");
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(admin, "Portal reload failed!");
				log.error(I18n.get("log.e210b296177e", e));
			}
		}
		else if (params[0].equals("commands")) {
			try {
				GameEngineServices.chatProcessor().reload();
				PacketSendUtility.sendMessage(admin, "Admin commands successfully reloaded!");
			} catch (GameServerError e) {
				PacketSendUtility.sendMessage(admin, "Admin command reload failed; existing commands were kept.");
				log.error(I18n.get("log.555f9d822d8e"), e);
			}
		}
		else if (params[0].equals("config")) {
			Config.reload();
			PacketSendUtility.sendMessage(admin, "Configs successfully reloaded!");
		}
		else if (params[0].equals("drop")) {
			DataManager.NPC_DROP_DATA = XmlDataLoader.getInstance().loadNpcDropData();
			PacketSendUtility.sendMessage(admin, "NpcDrops successfully reloaded!");
		}
		else if (params[0].equals("gameshop")) {
			GameRuntimeServices.inGameShopEn().reload();
			PacketSendUtility.sendMessage(admin, "Gameshop successfully reloaded!");
		}
		else if (params[0].equals("events")) {
			File eventXml = Config.dataFile("./data/static_data/events_config/events_config.xml");
			EventData data = null;
			try {
				JAXBContext jc = JAXBContext.newInstance(EventData.class);
				Unmarshaller un = jc.createUnmarshaller();
				data = (EventData) un.unmarshal(eventXml);
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(admin, "Event reload failed! Keeping the last version ...");
				log.error(I18n.get("log.e8459365ba32", e));
				return;
			}
			if (data != null) {
				GameEventServices.eventService().stop();
				String text = data.getActiveText();
				if (text == null || text.trim().length() == 0)
					text = "NONE";
				DataManager.EVENT_DATA.setAllEvents(data.getAllEvents(), data.getActiveText());
				PacketSendUtility.sendMessage(admin, "Active events: " + text);
				GameEventServices.eventService().start();
			}
		}
		else
			PacketSendUtility.sendMessage(admin, "syntax //reload <quest | skill | portal | commands | drop | gameshop | events | config>");

	}

	/**
	 * 原子替换任务数据与脚本；失败时回滚到旧数据并重新加载。
	 * Atomically replaces quest data/scripts; rolls back and reloads previous data on failure.
	 *
	 * @param quests 新任务模板列表 / new quest templates
	 * @param scripts 新 XML 任务脚本列表 / new XML quest scripts
	 */
	private void reloadQuests(List<QuestTemplate> quests, List<XMLQuest> scripts) {
		List<QuestTemplate> oldQuests = DataManager.QUEST_DATA.getQuestsData();
		List<XMLQuest> oldScripts = DataManager.XML_QUESTS.getQuest();
		QuestEngine questEngine = GameEngineServices.questEngine();
		questEngine.shutdown();
		try {
			DataManager.QUEST_DATA.setQuestsData(quests);
			DataManager.XML_QUESTS.setData(scripts);
			questEngine.load(null);
		} catch (GameServerError e) {
			questEngine.shutdown();
			DataManager.QUEST_DATA.setQuestsData(oldQuests);
			DataManager.XML_QUESTS.setData(oldScripts);
			try {
				questEngine.load(null);
			} catch (Throwable rollbackFailure) {
				e.addSuppressed(rollbackFailure);
			}
			throw e;
		}
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player,
			"syntax //reload <quest | skill | portal | commands | drop | gameshop | events | config>");
	}
}
