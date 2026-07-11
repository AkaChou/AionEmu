package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员邮件奖励命令：按模板 ID 向执行者发送系统奖励邮件。
 * Admin mail-reward command: send a system reward mail by template id to the invoker.
 *
 * @author Wnkrz
 */
public class MailReward extends AdminCommand
{
    public MailReward() {
        super("mailreward");
    }

    /**
     * 按模板 ID 向管理员发送奖励邮件。
     * Send a reward mail to the admin by template id.
     *
     * @param admin 执行命令的管理员 / Admin executing the command
     * Mail template id
     */
    @Override
    public void execute(Player admin, String... params) {
        int param = 0;
        if (params == null || params.length != 1) {
            PacketSendUtility.sendMessage(admin, "syntax //mailreward <Id> ");
            return;
        } try {
            param = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(admin, "Parameter must be an integer, or cancel.");
            return;
        }
        GameFeatureServices.systemMailService().sendTemplateRewardMail(param, admin.getCommonData());
    }

    /**
     * 失败回调（本命令无额外语法提示）。
     * Failure callback (no extra syntax for this command).
     *
     * @param player 接收提示的玩家 / Player receiving the hint
     * Failure message
     */
    @Override
    public void onFail(Player player, String message) {
    }
}
