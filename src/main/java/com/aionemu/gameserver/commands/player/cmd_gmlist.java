package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家命令：列出当前在线的 GM/团队成员。
 * Player command: lists currently online GMs/team members.
 *
 * @author Eloann
 */
public class cmd_gmlist extends PlayerCommand {

	/**
	 * 注册命令别名 {@code gmlist}。
	 * Registers the command alias {@code gmlist}.
	 */
    public cmd_gmlist() {
        super("gmlist");
    }

	/**
	 * 收集非离线的管理权限玩家并输出名单。
	 * Collects non-offline staff players and prints their names/tags.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
    @Override
    public void execute(Player player, String... params) {
        final List<Player> admins = new ArrayList<Player>();
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player object) {
                if (object.getAccessLevel() > 0 && object.getFriendList().getStatus() != FriendList.Status.OFFLINE) {
                    admins.add(object);
                }
            }
        });

        if (admins.size() > 0) {
            PacketSendUtility.sendMessage(player, "====================");
            if (admins.size() == 1) {
                PacketSendUtility.sendMessage(player, "There's one team member online:");
            } else {
                PacketSendUtility.sendMessage(player, "There are team member online:");
            }

            for (Player admin : admins) {

                if (AdminConfig.ADMIN_TAG_ENABLE) {
                    String adminTag = "%s";
                    StringBuilder sb = new StringBuilder(adminTag);
                    if (player.getAccessLevel() == 1) {
                        adminTag = sb.insert(0, AdminConfig.ADMIN_TAG_1.substring(0, AdminConfig.ADMIN_TAG_1.length() - 3)).toString();
                    } else if (player.getAccessLevel() == 2) {
                        adminTag = sb.insert(0, AdminConfig.ADMIN_TAG_2.substring(0, AdminConfig.ADMIN_TAG_2.length() - 3)).toString();
                    } else if (player.getAccessLevel() == 3) {
                        adminTag = sb.insert(0, AdminConfig.ADMIN_TAG_3.substring(0, AdminConfig.ADMIN_TAG_3.length() - 3)).toString();
                    } else if (player.getAccessLevel() == 4) {
                        adminTag = sb.insert(0, AdminConfig.ADMIN_TAG_4.substring(0, AdminConfig.ADMIN_TAG_4.length() - 3)).toString();
                    } else if (player.getAccessLevel() == 5) {
                        adminTag = sb.insert(0, AdminConfig.ADMIN_TAG_5.substring(0, AdminConfig.ADMIN_TAG_5.length() - 3)).toString();
					}
                    PacketSendUtility.sendMessage(player, String.format(adminTag, admin.getName()));
                }
            }
            PacketSendUtility.sendMessage(player, "====================");

        } else {
            PacketSendUtility.sendMessage(player, "There's no team member online!");
        }
    }
}
