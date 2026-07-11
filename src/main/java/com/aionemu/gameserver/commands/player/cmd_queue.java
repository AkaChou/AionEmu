package com.aionemu.gameserver.commands.player;

import java.util.Collection;
import java.util.Iterator;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.world.World;

/**
 * 玩家命令：报名参加 GM 正在举办的活动队列。
 * Player command: registers the player for an ongoing GM-hosted event queue.
 *
 * @author Kill3r
 */
public class cmd_queue extends PlayerCommand {

	/**
	 * 注册命令别名 {@code queue}。
	 * Registers the command alias {@code queue}.
	 */
    public cmd_queue(){
        super("queue");
    }

	/**
	 * 查找已开启报名的 GM 活动并尝试占位注册。
	 * Finds a GM with event registration open and attempts to register a slot.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
    public void execute(Player player,String...params){
        boolean anyEventfound = false;
        if(player.isRegedEvent()){
            PacketSendUtility.sendMessage(player, "You've already registered to the event!");
            return;
        }
        if(player.isInPrison()){
            PacketSendUtility.sendMessage(player, "You cant register inside Prison!");
            return;
        }
        Iterator<Player> ita = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

        while(ita.hasNext()){
            Player player1 = ita.next();

            if(player1.getAccessLevel() >= 2 && player1.isEventStarted()){
                int playerCounter = player1.getCountPlayers();
                if (player1.getCountPlayers() != checkRegedPlayers()){
                    PacketSendUtility.sendMessage(player, "Found an Event! Registering to " + player1.getName() + "'s Event!");
                    anyEventfound = true;
                    if (player1.getCountPlayers() != 500){
                        checkPosition(player1, player, playerCounter);
                    }
                }else{
                    PacketSendUtility.sendMessage(player, "Sorry all slots are taken now! Better luck next time <3");
                }
            }
        }

        // 修复 GM 开启无限报名时输入 .queue 的问题。 / Find a way to fix the problem when u type .queue when the gm has enabled the registration for unlimited players..
        // getCountplayers 在 .queue 工作时设值，为 0 时结束，默认即为 0。 / since the getCountplayers are set to a value when .queue is working.. and it ends when its 0.. and by default its by 0..

        if(anyEventfound == true){
            PacketSendUtility.sendMessage(player, "You've registered to the upcoming event!");
            player.setRegedEvent(true);
        }else{
            PacketSendUtility.sendMessage(player, "Currently there are no event running!");
        }
    }

    private void checkPosition(Player admin, Player oneWhoExecute, int countPosition){
        int count = admin.getCountPlayers();
        count = count - checkRegedPlayers();

        PacketSendUtility.sendMessage(oneWhoExecute, "There are " + count + " slots remaining!");
    }

    private int checkRegedPlayers(){
        int count = 0;
        Collection<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();
        for(Player p : players){
            if(p.isRegedEvent()){
                count = count + 1;
            }
        }
        return count;
    }
}
