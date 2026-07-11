package com.aionemu.gameserver;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * GM 在线/离线模式切换管理命令（{@code //gm}）。
 * Admin command that toggles GM available/offline mode ({@code //gm}).
 *
 * @author Eloann
 */
public class GMMode extends AdminCommand {

    /**
     * 注册命令名为 {@code gm}。
     * Registers the command name {@code gm}.
     */
    public GMMode() {
        super("gm");
    }

    /**
     * 执行 GM 模式开关：{@code on|off} 与是否公告（{@code y/n}）。
     * Executes GM mode toggle: {@code on|off} and announce flag ({@code y/n}).
     *
     * @param admin 执行命令的 GM / admin player
     * @param params 参数：模式与公告标志 / mode and announce flag
     */
    @Override
    public void execute(Player admin, String... params) {
        if (admin.getAccessLevel() < 1) {
            PacketSendUtility.sendMessage(admin, "You cannot use this command.");
            return;
        }

        if (params.length != 2) {
            onFail(admin, null);
            return;
        }

        if (params[0].toLowerCase().equals("on")) {
            if (params[1].equals("y")){
                GameRuntimeServices.gmService().onPlayerAvailable(admin); //send available message
                admin.setWispable();
            } else if (params[1].toLowerCase().equals("n")) {
                PacketSendUtility.sendMessage(admin, "You are Back Online");
                admin.setWispable();
            } else {
                admin.setWispable();
                PacketSendUtility.sendMessage(admin, "You are Back Online with GM Tag");
            }

            if (!admin.isGmMode()) {
                admin.setGmMode(true);

                //GameRuntimeServices.gmService().onPlayerLogin(admin); //put gm into gmlist

                admin.clearKnownlist();
                PacketSendUtility.sendPacket(admin, new SM_PLAYER_INFO(admin, false));
                PacketSendUtility.sendPacket(admin, new SM_MOTION(admin.getObjectId(), admin.getMotions().getActiveMotions()));
                admin.updateKnownlist();
                PacketSendUtility.sendMessage(admin, "you are now Available and Wispable by players");
            }
        }
        if (params[0].equals("off")) {
            if (params[1].toLowerCase().equals("y")){
                GameRuntimeServices.gmService().onPlayerUnavailable(admin); //send unavailable message
                GameRuntimeServices.gmService().onPlayerLogedOut(admin); //remove gm into gmlist
            } else if (params[1].toLowerCase().equals("n")) {
                PacketSendUtility.sendMessage(admin, "You are in Offline Status");
                PacketSendUtility.sendMessage(admin, "you are now Unavailable but can be Whisperable by players");
            } else {
                PacketSendUtility.sendMessage(admin, "You are Offline without GM Tag, But people can Whisper you.");
            }
            if (admin.isGmMode()) {
                admin.setGmMode(false);

                admin.clearKnownlist();
                PacketSendUtility.sendPacket(admin, new SM_PLAYER_INFO(admin, false));
                PacketSendUtility.sendPacket(admin, new SM_MOTION(admin.getObjectId(), admin.getMotions().getActiveMotions()));
                admin.updateKnownlist();
                PacketSendUtility.sendMessage(admin, "You are unavailable to players now.");
            }
        }
        if (params[0].equalsIgnoreCase("detector")) {
            //if (params[1].equalsIgnoreCase("on")){
                // 管理员。 / admin.
            // }
        }
    }

    /**
     * 发送命令语法帮助。
     * Sends command syntax help.
     *
     * @param admin 执行 GM / admin player
     * @param message 可选消息 / optional message
     */
    @Override
    public void onFail(Player admin, String message) {
        String syntax = "syntax //gm <on|off> <y/n>\n y = You want to announce the players, that you are On\nAlso your Whisperable state changes to 'Whisperable'\n n = You don't want to announce the players, + You 'Whisperable' State goes Off";
        PacketSendUtility.sendMessage(admin, syntax);
    }
}
