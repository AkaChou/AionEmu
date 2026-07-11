package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;
import com.aionemu.gameserver.model.templates.Guides.GuideTemplate;
import com.aionemu.gameserver.model.templates.Guides.SurveyTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTIONNAIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * HTML 问卷/引导服务，向客户端推送原始 HTML 并处理引导奖励。
 * HTML questionnaire/guide service pushing raw HTML to clients and handling guide rewards.
 *
 * @author lhw, xTz
 */
@Slf4j(topic = "ITEM_HTML_LOG")
public class HTMLService {


	/**
	 * 根据引导模板生成可下发的 HTML 内容。
	 * Builds deliverable HTML content from a guide template.
	 *
	 * guide template
	 * HTML string
	 */
	public static String getHTMLTemplate(GuideTemplate template) {
		String context = GameStaticDataServices.htmlCache().getHTML("guideTemplate.xhtml");

		StringBuilder sb = new StringBuilder();
		sb.append("<reward_items multi_count='").append(template.getRewardCount()).append("'>\n");
		for (SurveyTemplate survey : template.getSurveys()) {
			sb.append("<item_id count='").append(survey.getCount()).append("'>").append(survey.getItemId())
					.append("</item_id>\n");
		}
		sb.append("</reward_items>\n");
		context = context.replace("%reward%", sb);
		context = context.replace("%radio%", template.getSelect().isEmpty() ? " " : template.getSelect());
		context = context.replace("%html%", template.getMessage().isEmpty() ? " " : template.getMessage());
		context = context.replace("%rewardInfo%", template.getRewardInfo().isEmpty() ? " " : template.getRewardInfo());
		return context;
	}

	/**
	 * 向全体在线玩家推送同一份问卷 HTML。
	 * Pushes the same survey HTML to all online players.
	 *
	 * HTML content
	 */
	public static void pushSurvey(final String html) {
		final int messageId = GameWorldBootstrapServices.idFactory().nextId();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
				sendData(player, messageId, html);
			}
		});
	}

	/**
	 * 向指定玩家展示 HTML 页面。
	 * Shows an HTML page to the given player.
	 *
	 * 玩家 / player
	 * HTML content
	 */
	public static void showHTML(Player player, String html) {
		sendData(player, GameWorldBootstrapServices.idFactory().nextId(), html);
	}

	/**
	 * 将 HTML 按包分片发送给玩家。
	 * Sends HTML to the player, splitting into packets when needed.
	 *
	 * 玩家 / player
	 * message id
	 * HTML content
	 */
	public static void sendData(Player player, int messageId, String html) {
		byte packet_count = (byte) Math.ceil(html.length() / (Short.MAX_VALUE - 8) + 1);
		if (packet_count < 256) {
			for (byte i = 0; i < packet_count; i++) {
				try {
					int from = i * (Short.MAX_VALUE - 8), to = (i + 1) * (Short.MAX_VALUE - 8);
					if (from < 0) {
						from = 0;
					}
					if (to > html.length()) {
						to = html.length();
					}
					String sub = html.substring(from, to);
					player.getClientConnection().sendPacket(new SM_QUESTIONNAIRE(messageId, i, packet_count, sub));
				} catch (Exception e) {
					log.error(I18n.get("log.5db6e71d2d57", e));
				}
			}
		}
	}

	/**
	 * 按等级/职业/种族向玩家发送适用的引导 HTML。
	 * Sends applicable guide HTML to the player by level/class/race.
	 *
	 * 玩家 / player
	 */
	public static void sendGuideHtml(Player player) {
		if (player.getLevel() > 1) {
			GuideTemplate[] surveyTemplate = DataManager.GUIDE_HTML_DATA.getTemplatesFor(player.getPlayerClass(),
					player.getRace(), player.getLevel());

			for (GuideTemplate template : surveyTemplate) {
				if (!template.isActivated()) {
					continue;
				}
				int id = GameWorldBootstrapServices.idFactory().nextId();
				sendData(player, id, getHTMLTemplate(template));
				DAOManager.getDAO(GuideDAO.class).saveGuide(id, player, template.getTitle());
			}
		}
	}

	/**
	 * 玩家登录时重发未领取的引导 HTML。
	 * Re-sends unfinished guide HTML when the player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public static void onPlayerLogin(Player player) {
		if (player == null)
			return;

		List<Guide> guides = DAOManager.getDAO(GuideDAO.class).loadGuides(player.getObjectId());

		for (Guide guide : guides) {
			GuideTemplate template = DataManager.GUIDE_HTML_DATA.getTemplateByTitle(guide.getTitle());
			if (template != null) {
				if (template.isActivated()) {
					sendData(player, guide.getGuideId(), getHTMLTemplate(template));
				}
			} else {
				log.warn(I18n.get("log.c8c7b640eacd", guide.getTitle()));
			}
		}
	}

	/**
	 * 根据玩家选择发放引导奖励物品。
	 * Grants guide reward items based on the player's selection.
	 *
	 * 玩家 / player
	 * questionnaire message id
	 * @param items 选中的物品 ID 列表 / selected item ids
	 */
	public static void getReward(Player player, int messageId, List<Integer> items) {
		if (player == null || messageId < 1) {
			return;
		}

		if (GameRuntimeServices.surveyService().isActive(player, messageId)) {
			return;
		}

		Guide guide = DAOManager.getDAO(GuideDAO.class).loadGuide(player.getObjectId(), messageId);

		if (guide != null) {
			GuideTemplate template = DataManager.GUIDE_HTML_DATA.getTemplateByTitle(guide.getTitle());
			if (template == null) {
				return;
			}

			if (items.size() > template.getRewardCount()) {
				return;
			}

			if (items.size() > player.getInventory().getFreeSlots()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
				return;
			}
			List<SurveyTemplate> templates = null;
			if (template.getSurveys().size() != template.getRewardCount()) {
				templates = getSurveyTemplates(template.getSurveys(), items);
			} else {
				templates = template.getSurveys();
			}
			if (templates.isEmpty()) {
				return;
			}
			for (SurveyTemplate item : templates) {
				ItemService.addItem(player, item.getItemId(), item.getCount());
				if (LoggingConfig.LOG_ITEM) {
					log.info(I18n.get("log.0a8239250ae2", item.getItemId(), item.getCount(), player.getName()));
				}
			}
			DAOManager.getDAO(GuideDAO.class).deleteGuide(guide.getGuideId());
			items.clear();
		}
	}

	/**
	 * 按选中物品 ID 过滤调查模板。
	 * Filters survey templates by selected item ids.
	 *
	 * @param surveys 全部调查项 / all surveys
	 * @param items 选中物品 ID / selected item ids
	 * @return 匹配的模板列表 / matched templates
	 */
	private static List<SurveyTemplate> getSurveyTemplates(List<SurveyTemplate> surveys, List<Integer> items) {
		List<SurveyTemplate> templates = new ArrayList<SurveyTemplate>();
		for (SurveyTemplate survey : surveys) {
			if (items.contains(survey.getItemId())) {
				templates.add(survey);
			}
		}
		return templates;
	}

	/**
	 * 按标题向玩家发送指定引导 HTML。
	 * Sends a specific guide HTML to the player by title.
	 *
	 * @param player 玩家 / player
	 * @param title 引导标题 / guide title
	 */
	public static void sendGuideHtml(Player player, String title) {
		GuideTemplate template = DataManager.GUIDE_HTML_DATA.getTemplateByTitle(title);
		if (template != null) {
			int id = GameWorldBootstrapServices.idFactory().nextId();
			DAOManager.getDAO(GuideDAO.class).saveGuide(id, player, title);
			sendData(player, id, getHTMLTemplate(template));
		}
	}
}
