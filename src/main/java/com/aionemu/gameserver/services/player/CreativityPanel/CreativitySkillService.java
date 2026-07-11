package com.aionemu.gameserver.services.player.CreativityPanel;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.panel_cp.PanelCp;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CREATIVITY_POINTS_APPLY;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 创造力面板技能服务，处理技能附魔与学习。
 * Creativity panel skill service handling skill enchant and learning.
 */
public class CreativitySkillService {
	private static volatile ObjectProvider<CreativitySkillService> instanceProvider;

	/**
	 * 附魔技能。
	 * Enchants a skill.
	 *
	 * 玩家 / player
	 * @param id ID / id
	 * point
	 */
	public void enchantSkill(Player player, int id, int point) {
		PanelCp pcp = DataManager.PANEL_CP_DATA.getPanelCpId(id);
		if (point == 0) {
			player.getSkillList().addSkill(player, pcp.getSkillId(), 1);
			player.getCP().removePoint(player, id);
		} else {
			if (pcp.getSkillId() <= 0) {
				player.getSkillList().addSkill(player, pcp.getLearnSkill(), point + 1);
			} else {
				player.getSkillList().addSkill(player, pcp.getSkillId(), point + 1);
			}

			player.getCP().addPoint(player, id, point);
		}
		PacketSendUtility.sendPacket(player, new SM_CREATIVITY_POINTS_APPLY(0, 1, id, point));
	}

	/**
	 * 学习技能。
	 * Learns a skill.
	 *
	 * 玩家 / player
	 * @param id ID / id
	 * point
	 */
	public void learnSkill(Player player, int id, int point) {
		PanelCp pcp = DataManager.PANEL_CP_DATA.getPanelCpId(id);
		if (point >= 1) {
			player.getSkillList().addSkill(player, pcp.getLearnSkill(), point + 1);
			player.getCP().addPoint(player, id, point);
		} else if (point == 0) {
			SkillLearnService.removeSkill(player, pcp.getLearnSkill());
			player.getCP().removePoint(player, id);
		}
		PacketSendUtility.sendPacket(player, new SM_CREATIVITY_POINTS_APPLY(1, 1, id, point));
	}

	/**
	 * 登录同步大天使技能。
	 * Syncs daeva skills on login.
	 *
	 * 玩家 / player
	 * @param id ID / id
	 * point
	 */
	public void loginDaevaSkill(Player player, int id, int point) {
		PanelCp pcp = DataManager.PANEL_CP_DATA.getPanelCpId(id);
		if (point >= 1) {
			player.getSkillList().addSkill(player, pcp.getSkillId(), point + 1);
			player.getCP().addPoint(player, id, point);
		} else if (point == 0) {
			player.getSkillList().addSkill(player, pcp.getSkillId(), 1);
			player.getCP().removePoint(player, id);
		}
		PacketSendUtility.sendPacket(player, new SM_CREATIVITY_POINTS_APPLY(id, point));
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static CreativitySkillService getInstance() {
		ObjectProvider<CreativitySkillService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<CreativitySkillService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final CreativitySkillService INSTANCE = new CreativitySkillService();
	}
}
