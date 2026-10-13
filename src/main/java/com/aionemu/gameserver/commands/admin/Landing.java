package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.services.AbyssLandingService;

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
                AbyssLandingService.getInstance().onUpdate();
            } if ("level".startsWith(params[i])) {
                int level = Integer.parseInt(params[i + 2]);
                if (params[i + 1].equalsIgnoreCase("elyos")) {
                    if (level > AbyssLandingService.getInstance().redemptionLanding().getLevel()){
                        AbyssLandingService.getInstance().levelUpRedemptionLanding(level);
                    } else if (level < AbyssLandingService.getInstance().redemptionLanding().getLevel()){
                        AbyssLandingService.getInstance().onRedemptionLandingLevelDown(level);
                    }
                } if (params[i + 1].equalsIgnoreCase("asmodians")) {
                    if (level > AbyssLandingService.getInstance().harbingerLanding().getLevel()){
                        AbyssLandingService.getInstance().levelUpHarbingerLanding(level);
                    } else if (level < AbyssLandingService.getInstance().harbingerLanding().getLevel()) {
                        AbyssLandingService.getInstance().onHarbingerLandingLevelDown(level);
                    }
                }
			}
        }
    }

    private void showHelp(Player admin) {
        PacketSendUtility.sendMessage(admin, """
                [Help: Landing Command]
                 Use Ex: //landing level elyos 8.
                 Notice: This command uses smart matching. You may abbreviate most commands.
                """);
    }

    private void showHelpLevel(Player admin) {
        PacketSendUtility.sendMessage(admin, "Syntax: //landing level [Elyos/Asmodians] [Lvl 1-8]\n");
    }

    /**
     * 参数错误时显示帮助。
     * Show help on invalid arguments.
     * @param player 接收提示的玩家 / Player receiving the hint
     */
    @Override
    public void onFail(Player player, String message) {
        showHelp(player);
    }
}
