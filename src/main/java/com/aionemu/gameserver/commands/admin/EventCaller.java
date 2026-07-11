package com.aionemu.gameserver.commands.admin;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESURRECT;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import java.util.Collection;
import java.util.Iterator;

/**
 * 活动报名召集管理命令（{@code //eventcaller}）。
 * Event registration caller admin command ({@code //eventcaller}).
 *
 * @author Kill3r
 */
@Slf4j(topic = "GM_MONITOR_LOG")
public class EventCaller extends AdminCommand {

    /**
     * 注册命令名为 {@code eventcaller}。
     * Registers the command name {@code eventcaller}.
     */
    public EventCaller(){
        super("eventcaller");
    }


    /**
     * 启动/停止/取消活动报名，或列出已报名玩家并传送。
     * Starts, stops or cancels event registration, or lists and teleports registered players.
     *
     * admin
     * @param params show|start|stop|cancel 及可选人数上限 / show|start|stop|cancel and optional player limit
     */
    public void execute(Player player, String...params){
        if(params.length == 0){
            onFail(player, "" +
                    "--Syntax--" +
                    "\n//eventcaller show - shows registered players." +
                    "\n//eventcaller start - starts the event to calling to players." +
                    "\n//eventcaller start (number of players want) - if u want a limit write the number there. or just leave it blank for no limit" +
                    "\n//eventcaller stop - stops/ends the registeration queue and ports the registered players to you." +
                    "\n//eventcaller cancel - cancels an on-going registration , without porting the players.");
            return;
        }

        if(params[0].equals("show")){
            int count = 0;
            PacketSendUtility.sendMessage(player, "\nRegistered Players for the following Event!");
            PacketSendUtility.sendMessage(player, "==================================");
              Collection<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();

            for (Player p : players){
                if(p.isRegedEvent()){
                    PacketSendUtility.sendMessage(player, "# " + p.getName() + " - " + p.getRace() + " - " + p.getPlayerClass());
                    count = count + 1;
                }
            }
            PacketSendUtility.sendMessage(player, "=================( " + count + " )===============");
        }else if(params[0].equals("start")){
            if (params.length == 1) { //eventcaller start
                player.setEventStarted(true);
                player.setCountPlayers(500);
                Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
                while(iter.hasNext()){
                    Player p1 = iter.next();
                    PacketSendUtility.sendSys3Message(p1, player.getName(), "[EVENT] Registering for Event has Started! Type .queue to register to the event!");
                }
                log.info(I18n.get("log.a87a1544f909", player.getName(), player.getWorldId()));
            }

            if (params.length == 2) {//eventcaller start {number of player}
                int countP = Integer.parseInt(params[1]);
                player.setEventStarted(true);
                player.setCountPlayers(countP);

                Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
                while(ita.hasNext()){
                    Player p1 = ita.next();
                    PacketSendUtility.sendSys3Message(p1, player.getName(), "[EVENT] Registering for Event has Started! Type .queue to register to the event! ( '"+countP+"' Slots Available)");
                }
                log.info(I18n.get("log.b81705c5ff3a", player.getName(), countP, player.getWorldId()));
            }
        }else if(params[0].equals("stop")){
            AdminCommand test = new MoveToMe();
            int count = 0;
            player.setEventStarted(false);
            Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
            while(iter.hasNext()){
                Player p1 = iter.next();

                if(p1.isRegedEvent()){
                    if(p1.getLifeStats().isAlreadyDead()){
                        p1.setPlayerResActivate(true);
                        PacketSendUtility.sendPacket(p1, new SM_RESURRECT(player));
                        PlayerReviveService.skillRevive(p1);
                    }
                    test.execute(player, p1.getName());
                    PacketSendUtility.sendMessage(player, "Player : "+p1.getName()+" has been ported and added to reward list!");
                    player.setQueuedPlayers(p1);
                    count = count + 1;
                }
                PacketSendUtility.sendSys3Message(p1, player.getName(), "[EVENT] Event is Closed! Better luck next time!!");
                p1.setRegedEvent(false);
            }
            log.info(I18n.get("log.b767c467d456", player.getName(), count, player.getWorldId()));
            PacketSendUtility.sendMessage(player, "All the players have been added to you're queue list for rewarding!");
            PacketSendUtility.sendMessage(player, "=========================\n" +
                    "Total Players : " +count+ "\n" +
                    "=========================");
            // 重置全部配置 / reseting all configs
            count = 0;
            player.setCountPlayers(0);
        }else if (params[0].equals("cancel")){
            player.setEventStarted(false);
            Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
            while(ita.hasNext()){
                Player p1 = ita.next();

                if(p1.isRegedEvent()){
                    PacketSendUtility.sendMessage(player, "Player : " + p1.getName() + " removed from Event Registration!");
                }
                p1.setRegedEvent(false);
                PacketSendUtility.sendSys3Message(p1, player.getName(), "[EVENT] Event has been Canceled!");
            }
            log.info(I18n.get("log.288b87d468f8", player.getName(), player.getWorldId()));
            player.setCountPlayers(0);
        }
    }
}
