package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.xml.JAXBUtil;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

import java.io.File;
import java.util.List;
import java.util.Collection;
import com.aionemu.gameserver.world.World;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * GM command to add a combined skill to a player.
 *
 * @author ginho1 (original logic)
 * new adaptation for CmdGMHandler made by Dezalmado
 */
public final class CmdCombineSkill extends AbstractGMHandler {

	private static final File SKILLS_XML_FILE = Config.dataFile("./data/scripts/system/handlers/consolecommands/skills.xml");

	public CmdCombineSkill(Player admin, String params) {
		super(admin, params);
		run();
	}

	public void run() {
		if (admin.getAccessLevel() < AdminConfig.GM_LEVEL) {
			PacketSendUtility.sendMessage(admin, "You do not have sufficient access level to use this command.");
			return;
		}

		String[] commandArgs = params.split(" ");
		if (commandArgs.length < 1 || commandArgs[0].isEmpty()) {
			PacketSendUtility.sendMessage(admin, "Syntax: //combineskill <skill_id_or_name> [skill_level] [player_name]");
			return;
		}

		String skillNameOrId = commandArgs[0];
		int skillLvl = 1;
		Player player = target;

		if (commandArgs.length >= 2) {
			try {
				skillLvl = Integer.parseInt(commandArgs[1]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Error: Invalid skill level. Use a number.");
				return;
			}
		}

		if (commandArgs.length >= 3) {
			String playerName = commandArgs[2];
			player = null;
			Collection<Player> allPlayers = World.getInstance().getAllPlayers();
			for (Player p : allPlayers) {
				if (p.getName().equalsIgnoreCase(playerName)) {
					player = p;
					break;
				}
			}
		}

		if (player == null) {
			PacketSendUtility.sendMessage(admin, "Error: Target player not found. Specify a player name or select a target.");
			return;
		}

		SkillsXmlData data;
		try {
			data = JAXBUtil.unmarshal(SKILLS_XML_FILE, SkillsXmlData.class);
		} catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "Error loading skill data from XML: " + e.getMessage());
			return;
		}

		XmlSkillTemplate skillTemplate = data.getSkillTemplate(skillNameOrId);
		if (skillTemplate == null) {
			PacketSendUtility.sendMessage(admin, "Error: Skill '" + skillNameOrId + "' not found in skills.xml file.");
			return;
		}

		int skillId = skillTemplate.getTemplateId();

		if (skillId > 0) {

			SkillTemplate actualSkillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);
			if (actualSkillTemplate != null) {
				player.getSkillList().addSkill(player, skillId, skillLvl);
				PacketSendUtility.sendMessage(admin, "Skill '" + skillTemplate.getName() + "' (ID: " + skillId + ", Level: " + skillLvl + ") successfully added to " + player.getName() + "!");
				if (!player.equals(admin)) {
					PacketSendUtility.sendMessage(player, "Admin " + admin.getName() + " added skill " + skillTemplate.getName() + " to you.");
				}
			} else {
				PacketSendUtility.sendMessage(admin, "Error: Skill ID " + skillId + " exists in XML but not in game's DataManager. Cannot add skill.");
			}
		}
	}

	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(namespace = "", name = "XmlSkillTemplate")
	private static class XmlSkillTemplate {
		@XmlAttribute(name = "id", required = true)
		private int skillId;

		@XmlAttribute(name = "name")
		private String name;

		public String getName() {
			return name;
		}

		public int getTemplateId() {
			return skillId;
		}
	}

	@XmlRootElement(name = "skills")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class SkillsXmlData {
		@XmlElement(name = "skill")
		private List<XmlSkillTemplate> its;

		public XmlSkillTemplate getSkillTemplate(String skillNameOrId) {
			try {
				int id = Integer.parseInt(skillNameOrId);
				for (XmlSkillTemplate skill : its) {
					if (skill.getTemplateId() == id) {
						return skill;
					}
				}
			} catch (NumberFormatException e) {

			}


			for (XmlSkillTemplate skill : its) {
				if (skill.getName().equalsIgnoreCase(skillNameOrId)) {
					return skill;
				}
			}
			return null;
		}
	}
}
