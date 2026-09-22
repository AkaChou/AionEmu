package com.aionemu.gameserver.services.player.CreativityPanel;

import lombok.Setter;
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
    /**
     * -- SETTER --
     *  setInstanceProvider 方法。
     *  setInstanceProvider method.
     * 提供者 / provider
     */
    @Setter
    private static volatile ObjectProvider<CreativitySkillService> instanceProvider;

	/**
	 * 附魔技能。
	 * Enchants a skill.
	 * 玩家 / player
	 * @param id ID / id
	 * @param point 点数 / point
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
	 * 玩家 / player
	 * @param id ID / id
	 * @param point 点数 / point
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
	 * 玩家 / player
	 * @param id ID / id
	 * @param point 点数 / point
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static CreativitySkillService getInstance() {
		ObjectProvider<CreativitySkillService> provider = instanceProvider;
		CreativitySkillService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("CreativitySkillService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
