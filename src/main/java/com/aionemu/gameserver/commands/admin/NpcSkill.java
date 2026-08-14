package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.List;

/**
 * 列出当前目标 NPC 技能模板的管理员命令。
 * Admin command to list skill templates of the targeted NPC.
 *
 * @author Wakizashi
 */
public class NpcSkill extends AdminCommand {

	/**
	 * 以别名 {@code npcskill} 构造命令。
	 * Construct the command with alias {@code npcskill}.
	 */
	public NpcSkill() {
		super("npcskill");
	}

	/**
	 * 读取目标 NPC 的技能列表并分批发送给管理员。
	 * Read the targeted NPC's skill list and send it to the admin in batches.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		Npc target = null;
		VisibleObject creature = admin.getTarget();
		if (admin.getTarget() instanceof Npc) {
			target = (Npc) creature;
		}

		if (target == null) {
			PacketSendUtility.sendMessage(admin, "You should select a valid target first!");
			return;
		}

		StringBuilder strbld = new StringBuilder("-list of skills:\n");

		List<NpcSkillTemplate> list = DataManager.NPC_SKILL_DATA.getNpcSkillList(target.getNpcId()).getNpcSkills();

		for (NpcSkillTemplate skill : list)
			strbld.append("    level " + skill.getSkillLevel() + " of " + skill.getSkillid() + ".\n");
		showAllLines(admin, strbld.toString());
	}

	private void showAllLines(Player admin, String str) {
		int index = 0;
		String[] strarray = str.split("\n");

		while (index < strarray.length - 20) {
			StringBuilder strbld = new StringBuilder();
			for (int i = 0; i < 20; i++, index++) {
				strbld.append(strarray[index]);
				if (i < 20 - 1)
					strbld.append("\n");
			}
			PacketSendUtility.sendMessage(admin, strbld.toString());
		}
		int odd = strarray.length - index;
		StringBuilder strbld = new StringBuilder();
		for (int i = 0; i < odd; i++, index++)
			strbld.append(strarray[index] + "\n");
		PacketSendUtility.sendMessage(admin, strbld.toString());
	}

}
