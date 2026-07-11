package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * GM 指令：按技能名称描述为玩家添加技能。
 * GM command handler that adds a skill to a player by skill name description.
 *
 * @author Alcapwnd
 */
public class CmdAddSkill extends AbstractGMHandler {

	/**
	 * 创建处理器并立即执行添加技能逻辑。
	 * Creates the handler and immediately runs the add-skill logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 技能名称描述 / skill name description
	 */
	public CmdAddSkill(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 校验面板权限后，按技能名称描述为目标玩家添加技能。
	 * After access check, adds the skill matching the name description to the target player.
	 */
	private void run() {
		Player t = admin;
		if (admin.getClientConnection().getAccount().getAccessLevel() <= PanelConfig.SKILL_PANEL_LEVEL) {
			PacketSendUtility.sendMessage(admin, "You haven't access this panel commands");
			return;
		}

		if (admin.getTarget() != null && admin.getTarget() instanceof Player) {
			t = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(admin.getTarget().getName()));
		}
		if (params == null) {
			return;
		}
		for (SkillTemplate template : DataManager.SKILL_DATA.getSkillData().values()) {
			if (template.getNamedesc() != null && template.getNamedesc().equalsIgnoreCase(params)) {
				PacketSendUtility.sendMessage(admin, "You added Skill " + template.getName() + "to " + t.getName());
				PacketSendUtility.sendMessage(t, "Admin has add Skill " + template.getName() + "to you.");
				t.getSkillList().addSkill(t, template.getSkillId(), 1);
			}
		}
	}
}
