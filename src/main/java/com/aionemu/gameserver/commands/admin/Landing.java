package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员欧比斯登陆点命令：刷新状态或调整天族/魔族登陆点等级。
 * Admin abyss landing command: refresh state or set Elyos/Asmodian landing level.
 */
public class Landing extends AdminCommand
{
    public Landing() {
        super("landing");
    }

    /**
     * 处理 help/update/level 子命令。
     * Handle help/update/level subcommands.
     *
     * @param admin 执行命令的管理员 / Admin executing the command
     * @param params 子命令与参数 / Subcommand and arguments
     */
    @Override
    public void execute(Player admin, String... params) {
        if (params.length != 0) {
            int i = 0;
            if ("help".startsWith(params[i])) {
                if (params[i + 1] == null) {
                    showHelp(admin);
                } else if ("level".startsWith(params[i + 1])) {
                    showHelpLevel(admin);
                }
                return;
            } if ("update".startsWith(params[i])) {
                GameLocationBootstrapServices.abyssLandingService().onUpdate();
            } if ("level".startsWith(params[i])) {
                int level = Integer.parseInt(params[i + 2]);
                if (params[i + 1].equalsIgnoreCase("elyos")) {
                    if (level > GameLocationBootstrapServices.abyssLandingService().redemptionLanding().getLevel()){
                        GameLocationBootstrapServices.abyssLandingService().levelUpRedemptionLanding(level);
                    } else if (level < GameLocationBootstrapServices.abyssLandingService().redemptionLanding().getLevel()){
                        GameLocationBootstrapServices.abyssLandingService().onRedemptionLandingLevelDown(level);
                    }
                } if (params[i + 1].equalsIgnoreCase("asmodians")) {
                    if (level > GameLocationBootstrapServices.abyssLandingService().harbingerLanding().getLevel()){
                        GameLocationBootstrapServices.abyssLandingService().levelUpHarbingerLanding(level);
                    } else if (level < GameLocationBootstrapServices.abyssLandingService().harbingerLanding().getLevel()) {
                        GameLocationBootstrapServices.abyssLandingService().onHarbingerLandingLevelDown(level);
                    }
                }
                return;
            }
        }
    }

    private void showHelp(Player admin) {
        PacketSendUtility.sendMessage(admin, "[Help: Landing Command]\n"
        + " Use Ex: //landing level elyos 8.\n"
        + " Notice: This command uses smart matching. You may abbreviate most commands.\n" );
    }

    private void showHelpLevel(Player admin) {
        PacketSendUtility.sendMessage(admin, "Syntax: //landing level [Elyos/Asmodians] [Lvl 1-8]\n");
    }

    /**
     * 参数错误时显示帮助。
     * Show help on invalid arguments.
     *
     * @param player 接收提示的玩家 / Player receiving the hint
     */
    @Override
    public void onFail(Player player, String message) {
        showHelp(player);
    }
}
