package com.aionemu.gameserver.commands.admin;


import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 查询在线玩家详细信息（位置、物品、队伍、技能、军团、AP/GP 等）的管理员命令。
 * Admin command to inspect online player details (location, items, group, skills, legion, AP/GP, etc.).
 */
public class PlayerInfo extends AdminCommand {
	/**
	 * 以别名 {@code playerinfo} 构造命令。
	 * Construct the command with alias {@code playerinfo}.
	 */
	public PlayerInfo() {
		super("playerinfo");
	}

	/**
	 * 输出基础信息，并按第二参数切换 item/group/skill/loc/legion/ap/gp/chars/knownlist/visual 等详情。
	 * Print basic info and switch on the second arg for item/group/skill/loc/legion/ap/gp/chars/knownlist/visual details.
	 *
	 * @param params 角色名与详情开关 / Character name and detail switch
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> <loc | item | group | skill | legion | ap | gp | chars | knownlist[info|add|remove] | visual[see|notsee]> ");
			return;
		}
		Player target = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (target == null) {
			PacketSendUtility.sendMessage(admin, "Selected player is not online!");
			return;
		}
		PacketSendUtility.sendMessage(admin, "\n[Info about " + target.getName() + "]\n-common: lv" + target.getLevel() + "("
			+ target.getCommonData().getExpShown() + " xp), " + target.getRace() + ", "
			+ target.getPlayerClass() + "\n-ip: " + target.getClientConnection().getIP() + "\n" + "-account name: "
			+ target.getClientConnection().getAccount().getName() + "\n");
		if (params.length < 2)
			return;
		switch (params[1]) {
			case "item" -> {
				StringBuilder strbld = new StringBuilder("-items in inventory:\n");
				List<Item> items = target.getInventory().getItemsWithKinah();
				Iterator<Item> it = items.iterator();
				if (items.isEmpty()) {
					strbld.append("none\n");
				} else {
					while (it.hasNext()) {
						Item act = it.next();
						strbld.append("    ")
							.append(act.getItemCount())
							.append("(s) of ")
							.append(ChatUtil.item(act.getItemTemplate().getTemplateId()))
							.append("\n");
					}
				}
				items.clear();
				items = target.getEquipment().getEquippedItems();
				it = items.iterator();
				strbld.append("-equipped items:\n");
				if (items.isEmpty()) {
					strbld.append("none\n");
				} else {
					while (it.hasNext()) {
						Item act = it.next();
						strbld.append("    ")
							.append(act.getItemCount())
							.append("(s) of ")
							.append(ChatUtil.item(act.getItemTemplate().getTemplateId()))
							.append("\n");
					}
				}
				items.clear();
				items = target.getWarehouse().getItemsWithKinah();
				it = items.iterator();
				strbld.append("-items in warehouse:\n");
				if (items.isEmpty()) {
					strbld.append("none\n");
				} else {
					while (it.hasNext()) {
						Item act = it.next();
						strbld.append("    ")
							.append(act.getItemCount())
							.append("(s) of ")
							.append(ChatUtil.item(act.getItemTemplate().getTemplateId()))
							.append("\n");
					}
				}
				showAllLines(admin, strbld.toString());
			}
			case "group" -> {
				final StringBuilder strbld = new StringBuilder("-group info:\n  Leader: ");
				PlayerGroup group = target.getPlayerGroup2();
				if (group == null) {
					PacketSendUtility.sendMessage(admin, "-group info: no group");
				} else {
					strbld.append(group.getLeader().getName()).append("\n  Members:\n");
					group.applyOnMembers(player -> {
						if (player != null) {
							strbld.append(" ").append(player.getName()).append("\n");
						}
						return true;
					});
					PacketSendUtility.sendMessage(admin, strbld.toString());
				}
			}
			case "skill" -> {
				StringBuilder strbld = new StringBuilder("-list of skills:\n");
				PlayerSkillEntry[] sle = target.getSkillList().getAllSkills();
				for (PlayerSkillEntry playerSkillEntry : sle)
					strbld.append("    level ")
						.append(playerSkillEntry.getSkillLevel())
						.append(" of ")
						.append(playerSkillEntry.getSkillName())
						.append("\n");
				showAllLines(admin, strbld.toString());
			}
			case "loc" -> {
				String chatLink = ChatUtil.position(target.getName(), target.getPosition());
				PacketSendUtility.sendMessage(admin, "- " + chatLink + "'s location:\n  mapid: " + target.getWorldId() + "\n  X: " + target.getX() + " Y: "
					+ target.getY() + "Z: " + target.getZ() + "heading: " + target.getHeading());
			}
			case "legion" -> {
				StringBuilder strbld = new StringBuilder();
				Legion legion = target.getLegion();
				if (legion == null) {
					PacketSendUtility.sendMessage(admin, "-legion info: no legion");
				} else {
					ArrayList<LegionMemberEx> legionmemblist = LegionService.getInstance().loadLegionMemberExList(legion, null);
					Iterator<LegionMemberEx> it = legionmemblist.iterator();
					strbld.append("-legion info:\n  name: ")
						.append(legion.getLegionName())
						.append(", level: ")
						.append(legion.getLegionLevel())
						.append("\n  members(online):\n");
					while (it.hasNext()) {
						LegionMemberEx act = it.next();
						strbld.append("    ")
							.append(act.getName())
							.append("(")
							.append(((act.isOnline()) ? "online" : "offline"))
							.append(")")
							.append(act.getRank().toString())
							.append("\n");
					}
				}
				showAllLines(admin, strbld.toString());
			}
			case "ap" -> {
				PacketSendUtility.sendMessage(admin, "AP info about " + target.getName());
				PacketSendUtility.sendMessage(admin, "Total AP = " + target.getAbyssRank().getAp());
				PacketSendUtility.sendMessage(admin, "Total Kills = " + target.getAbyssRank().getAllKill());
				PacketSendUtility.sendMessage(admin, "Today Kills = " + target.getAbyssRank().getDailyKill());
				PacketSendUtility.sendMessage(admin, "Today AP = " + target.getAbyssRank().getDailyAP());
			}
			case "gp" -> {
				PacketSendUtility.sendMessage(admin, "GP info about " + target.getName());
				PacketSendUtility.sendMessage(admin, "Total GP = " + target.getAbyssRank().getGp());
				PacketSendUtility.sendMessage(admin, "Today GP = " + target.getAbyssRank().getDailyGP());
			}
			case "chars" -> {
				PacketSendUtility.sendMessage(admin, "Others characters of " + target.getName() + " (" + target.getClientConnection().getAccount().size() + ") :");
				for (PlayerAccountData d : target.getClientConnection().getAccount()) {
					if (d != null && d.getPlayerCommonData() != null) {
						PacketSendUtility.sendMessage(admin, d.getPlayerCommonData().getName());
					}
				}
			}
			case "knownlist" -> {
				if (params.length < 3) {
					PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> knownlist <info | add <id> | remove <id>>");
					return;
				}
				switch (params[2]) {
					case "info" -> {
						PacketSendUtility.sendMessage(admin, "KnownList of " + target.getName());
						for (VisibleObject obj : target.getKnownList().getKnownObjectsSnapshot()) {
							PacketSendUtility.sendMessage(admin, obj.getName() + " objectId:" + obj.getObjectId());
						}
					}
					case "add" -> {
						if (params.length < 4) {
							PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> knownlist add <objectId>");
							return;
						}
						int objId = Integer.parseInt(params[3]);
						VisibleObject obj = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objId);
						if (obj != null && !target.getKnownList().getKnownObjects().containsKey(objId)) {
							target.getKnownList().getKnownObjects().put(objId, obj);
						}
					}
					case "remove" -> {
						if (params.length < 4) {
							PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> knownlist remove <objectId>");
							return;
						}
						int objId = Integer.parseInt(params[3]);
						VisibleObject obj = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objId);
						if (obj != null) {
							target.getKnownList().getKnownObjects().remove(objId);
						}
					}
				}
			}
			case "visual" -> {
				if (params.length < 3) {
					PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> visual <info | see <id> | notsee <id>>");
					return;
				}
				switch (params[2]) {
					case "info" -> {
						PacketSendUtility.sendMessage(admin, "VisualList of " + target.getName());
						for (VisibleObject obj : target.getKnownList().getVisibleObjectsSnapshot()) {
							PacketSendUtility.sendMessage(admin, obj.getName() + " objectId:" + obj.getObjectId());
						}
					}
					case "see" -> {
						if (params.length < 4) {
							PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> visual see <playerId>");
							return;
						}
						int objId = Integer.parseInt(params[3]);
						Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
						if (player != null) {
							target.getController().see(player);
						}
					}
					case "notsee" -> {
						if (params.length < 4) {
							PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> visual notsee <playerId>");
							return;
						}
						int objId = Integer.parseInt(params[3]);
						Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
						if (player != null) {
							target.getController().notSee(player, true);
						}
					}
				}
			}
			default -> {
				PacketSendUtility.sendMessage(admin, "bad switch!");
				PacketSendUtility.sendMessage(admin, "syntax //playerinfo <playername> <loc | item | group | skill | legion | ap | gp | chars | knownlist[info|add|remove] | visual[see|notsee]> ");
			}
		}
	}

	private void showAllLines(Player admin, String str) {
		int index = 0;
		String[] strarray = str.split("\n");
		while (index < strarray.length - 20) {
			StringBuilder strbld = new StringBuilder();
			for (int i = 0; i < 20; i++, index++) {
				strbld.append(strarray[index]);
				if (i < 20 - 1) {
					strbld.append("\n");
				}
			}
			PacketSendUtility.sendMessage(admin, strbld.toString());
		}
		int odd = strarray.length - index;
		StringBuilder strbld = new StringBuilder();
		for (int i = 0; i < odd; i++, index++)
			strbld.append(strarray[index])
				.append("\n");
		PacketSendUtility.sendMessage(admin, strbld.toString());
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //playerinfo <playername> <loc | item | group | skill | legion | ap | gp | chars | knownlist | visual>");
	}
}
