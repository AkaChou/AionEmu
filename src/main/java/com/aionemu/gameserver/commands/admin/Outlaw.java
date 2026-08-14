package com.aionemu.gameserver.commands.admin;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import java.util.Iterator;

/**
 * 切换目标或当前地图玩家 PK/中立（Outlaw）模式的管理员命令。
 * Admin command to toggle PK or neutral (Outlaw) mode for a target or all players on the map.
 *
 */
@Slf4j(topic = "GM_MONITOR_LOG")
public class Outlaw extends AdminCommand {
    /**
     * 以别名 {@code outlaw} 构造命令。
     * Construct the command with alias {@code outlaw}.
     */
    public Outlaw(){
        super("outlaw");
    }


    /**
     * 按 attackable/neutral/clear 子命令切换目标或全图玩家的 Outlaw 状态。
     * Toggle Outlaw state for the target or all map players via attackable/neutral/clear.
     *
     * @param param 子命令与范围 / Subcommand and scope
     */
    public void execute(final Player admin, String...param){
        // 固定对面 / Fixed opposite
        if(param.length == 0){
            onFail(admin, "== SYNAX ==\n" +
                    "//Outlaw attackable 0 - changes current target to attackable (do same to turn off/on)\n" +
                    "//Outlaw neutral 0 - changes current target to neutral (do same to turn off/on)\n" +
                    "//Outlaw attackable all - changes everyone in the current map to attackable\n" +
                    "//Outlaw neutral all - changes everyone in the current map to neutral\n" +
                    "//Outlaw attackable cancel - turns the attackable state off from everyone in map (NO SKULL)\n" +
                    "//Outlaw neutral cancel - turns the neutral mode off from everyone in map(NO SHIELD)\n" +
                    "//Outlaw clear - removes both attackable and neutral mode from everyone in map.");
            return;
        }
        VisibleObject visibleObject = admin.getTarget();

        if(visibleObject == null || !(visibleObject instanceof Player)){
            PacketSendUtility.sendMessage(admin, "You need to target a player!");
            return;
        }
        final Player target = (Player) visibleObject;

        if(param[0].equalsIgnoreCase("attackable")){


            if(param[1].equalsIgnoreCase("all")){
                Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

                while(ita.hasNext()){
                    Player player = ita.next();
                    if(player.getWorldId() == admin.getWorldId()){
                        player.setInPkMode(true);
                        refresh(player);
                        PacketSendUtility.sendMessage(player , "[Outlaw] : You've been changed to \"[color:Atta;1 0 0][color:ckab;1 0 0][color:le;1 0 0]\" Mode!");
                    }
                }
                PacketSendUtility.sendMessage(admin, "[Outlaw] : All players in map has been changed to \"[color:Atta;1 0 0][color:ckab;1 0 0][color:le;1 0 0]\" !");
                log.info(I18n.get("log.ad57117b7271", admin.getName(), admin.getWorldId()));
            }else if(param[1].equalsIgnoreCase("cancel")){
                Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

                while(ita.hasNext()){
                    Player player = ita.next();
                    if(player.getWorldId() == admin.getWorldId()){
                        player.setInPkMode(false);
                        refresh(player);
                        PacketSendUtility.sendMessage(player, "[Outlaw] : You're now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    }
                }
                PacketSendUtility.sendMessage(admin, "[Outlaw] : All players in map has been changed to \"[color:Norm;1 1 1][color:al;1 1 1]\" !");
                log.info(I18n.get("log.7ade2b39245b", admin.getName(), admin.getWorldId()));
            }else{
                if(!target.isInPkMode()){
                    target.setInPvEMode(false);
                    target.setInPkMode(true);
                    refresh(target);
                    PacketSendUtility.sendMessage(admin, "[Outlaw] : Player " + target.getName() + " is now in \"[color:Atta;1 0 0][color:ckab;1 0 0][color:le;1 0 0]\" Mode!");
                    PacketSendUtility.sendMessage(target, "[Outlaw] : You've been changed to \"[color:Atta;1 0 0][color:ckab;1 0 0][color:le;1 0 0]\" Mode!");
                    log.info(I18n.get("log.db7adb299558", admin.getName(), target.getName(), admin.getWorldId()));
                }else{
                    target.setInPkMode(false);
                    refresh(target);
                    PacketSendUtility.sendMessage(admin, "[Outlaw] : Player " + target.getName() + " is now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    PacketSendUtility.sendMessage(target, "[Outlaw] : You're now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    log.info(I18n.get("log.021705f9145d", admin.getName(), target.getName(), admin.getWorldId()));
                }
            }


        }else if(param[0].equalsIgnoreCase("neutral")){

            if(param[1].equalsIgnoreCase("all")){
                Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

                while(ita.hasNext()){
                    Player player = ita.next();
                    if(player.getWorldId() == admin.getWorldId()){
                        player.setInPvEMode(true);
                        refresh(player);
                        PacketSendUtility.sendMessage(player, "[Outlaw] : You're now in \"[color:Neut;0 1 0][color:ral;0 1 0]\" Mode!");
                    }
                }
                PacketSendUtility.sendMessage(admin, "[Outlaw] : All players in map has been changed to \"[color:Neut;0 1 0][color:ral;0 1 0]\" Mode!");
                log.info(I18n.get("log.c37acd0a8bc2", admin.getName(), admin.getWorldId()));
            }else if(param[1].equalsIgnoreCase("cancel")){
                Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

                while(ita.hasNext()){
                    Player player = ita.next();
                    if(player.getWorldId() == admin.getWorldId()){
                        player.setInPvEMode(false);
                        refresh(player);
                        PacketSendUtility.sendMessage(player, "[Outlaw] : You're now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    }
                }
                PacketSendUtility.sendMessage(admin, "[Outlaw] : All players in map has been changed to \"[color:Norm;1 1 1][color:al;1 1 1]\" !");
                log.info(I18n.get("log.97caea294957", admin.getName(), admin.getWorldId()));
            }else{
                if(!target.isInPvEMode()){
                    target.setInPkMode(false);
                    target.setInPvEMode(true);
                    refresh(target);
                    PacketSendUtility.sendMessage(admin, "[Outlaw] : Player " + target.getName() + " is now in \"[color:Neut;0 1 0][color:ral;0 1 0]\" Mode!");
                    PacketSendUtility.sendMessage(target, "[Outlaw] : You've been changed to \"[color:Neut;0 1 0][color:ral;0 1 0]\" Mode!");
                    log.info(I18n.get("log.58a4597b2244", admin.getName(), target.getName(), admin.getWorldId()));
                }else{
                    target.setInPvEMode(false);
                    refresh(target);
                    PacketSendUtility.sendMessage(admin, "[Outlaw] : Player " + target.getName() + " is now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    PacketSendUtility.sendMessage(target, "[Outlaw] : You're now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                    log.info(I18n.get("log.36cc09cfea5a", admin.getName(), target.getName(), admin.getWorldId()));
                }
            }

        }else if(param[0].equalsIgnoreCase("clear")){
            Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

            while(ita.hasNext()){
                Player player = ita.next();
                if(player.getWorldId() == admin.getWorldId()){
                    player.setInPvEMode(false);
                    player.setInPkMode(false);
                    refresh(player);
                    PacketSendUtility.sendMessage(player, "[Outlaw] : You're now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
                }
            }
            PacketSendUtility.sendMessage(admin, "[Outlaw] : Player " + target.getName() + " is now in \"[color:Norm;1 1 1][color:al;1 1 1]\" Mode!");
            log.info(I18n.get("log.4362b87a09d5", admin.getName(), admin.getWorldId()));
        }
    }

    /**
     * 显示 Outlaw 命令语法帮助。
     * Show Outlaw command syntax help.
     *
     * @param admin 执行 GM / Admin player
     * @param msg 失败消息 / Failure message
     */
    public void onFail(Player admin, String msg){
        PacketSendUtility.sendMessage(admin, "== SYNAX ==\n" +
            "//Outlaw attackable 0 - changes current target to attackable (do same to turn off/on)\n" +
            "//Outlaw neutral 0 - changes current target to neutral (do same to turn off/on)\n" +
            "//Outlaw attackable all - changes everyone in the current map to attackable\n" +
            "//Outlaw neutral all - changes everyone in the current map to neutral\n" +
            "//Outlaw attackable cancel - turns the attackable state off from everyone in map (NO SKULL)\n" +
            "//Outlaw neutral cancel - turns the neutral mode off from everyone in map(NO SHIELD)\n" +
            "//Outlaw clear - removes both attackable and neutral mode from everyone in map.");

    }

    /**
     * 通过原地传送刷新玩家客户端状态。
     * Refresh the player's client state via an in-place teleport.
     *
     */
    public void refresh(Player player){
        TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getX(),player.getY(),player.getZ());
    }
}
