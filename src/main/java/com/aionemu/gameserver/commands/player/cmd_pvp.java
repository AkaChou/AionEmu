package com.aionemu.gameserver.commands.player;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapType;

import java.util.Calendar;

/**
 * 玩家命令：按日程传送到 PvP 地图并发送欢迎信息。
 * Player command: teleports to scheduled PvP maps and shows welcome info.
 *
 * @author Ghostfur
 * @author Nimwey
 */
public class cmd_pvp extends PlayerCommand {

    public cmd_pvp() {
        super("pvp");
    }

    /**
     * 根据星期/活动状态选择并进入对应 PvP 地图。
     * Chooses and enters the matching PvP map by weekday/event state.
     *
     * @param player 执行命令的玩家 / invoking player
     * @param param 命令参数 / command parameters
     */
    public void execute(Player player, String...param){
        if (player.isAttackMode()){
            PacketSendUtility.sendMessage(player, "You cannot Go to Insane PvP while in Attack Mode!");
            return;
        }
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
            monPvP(player);
            givePvPWelcomeMsg(player, "monPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY){
            wedPvP(player);
            givePvPWelcomeMsg(player, "wedPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY){
            monPvP(player);
            givePvPWelcomeMsg(player, "monPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY){
            wedPvP(player);
            givePvPWelcomeMsg(player, "wedPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY){
            monPvP(player);
            givePvPWelcomeMsg(player, "monPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY){
            wedPvP(player);
            givePvPWelcomeMsg(player, "wedPvP");
        }else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY){
            monPvP(player);
            givePvPWelcomeMsg(player, "monPvP");
        }
    }
		
	  private void monPvP(Player player){
        checkotherEvents(player);
        if (player.getRace() == Race.ASMODIANS  && player.getWorldId() != 220040000 && !player.isInPrison()) { //Beluslan
            goTo(player, WorldMapType.BELUSLAN.getId(), 2154.291f, 1031.396f, 484.41196f);
        } else if (player.getRace() == Race.ELYOS && player.getWorldId() != 220040000 && !player.isInPrison()) {
            goTo(player, WorldMapType.BELUSLAN.getId(), 1883.6552f, 1298.7427f, 428.79642f);
        }
    } 				

	
    private void wedPvP(Player player){
        checkotherEvents(player);
        if (player.getRace() == Race.ASMODIANS  && player.getWorldId() != 210040000 && !player.isInPrison()) { //Heiron
            goTo(player, WorldMapType.HEIRON.getId(), 1933.721f, 2482.6672f, 311.38864f);
        } else if (player.getRace() == Race.ELYOS && player.getWorldId() != 210040000 && !player.isInPrison()) {
            goTo(player, WorldMapType.HEIRON.getId(), 1262.2222f, 2283.0647f, 239.9606f);
        }
    }					

    private void checkotherEvents(Player player){
        if (player.isAttackMode()) {
            PacketSendUtility.sendMessage(player, "You can not use this command during the fight!");
            return;
        }
	}
    private static void goTo(final Player player, int worldId, float x, float y, float z) {
        WorldMap destinationMap = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
        if (destinationMap.isInstanceType()) {
            TeleportService2.teleportToInstance(player, worldId, x, y, z);
        } else {
            TeleportService2.teleportTo(player, worldId, x, y, z);
        }
    }

    private void givePvPWelcomeMsg(Player player, String PvPMap){
        String msg = "";
        if(PvPMap.equalsIgnoreCase("monPvP")){
            if(player.getWorldId() == 220040000){
                return;
            }
        }else if(PvPMap.equalsIgnoreCase("tuePvP")){
            if(player.getWorldId() == 210040000){
                return;
            }
        }else if(PvPMap.equalsIgnoreCase("wedPvP")){
            if(player.getWorldId() == 220040000){
                return;
            }
        }else if(PvPMap.equalsIgnoreCase("monPvP")){
            if(player.getWorldId() == 210040000){
                return;
            }
        }else if(PvPMap.equalsIgnoreCase("tuePvP")) {
            if (player.getWorldId() == 220040000) {
                return;
            }
	    }else if(PvPMap.equalsIgnoreCase("wedPvP")) {
            if (player.getWorldId() == 210040000) {
                return;
            }
        }
		 
        if(player.getRace() == Race.ASMODIANS){
            msg = "all the ELYOS :]";
        }else if(player.getRace() == Race.ELYOS){
            msg = "all the ASMODIANS :]";
        }
		PacketSendUtility.sendSys3Message(player, "\uE059", "[PvP Zone] Welcome to the PvP Zone!!");
        PacketSendUtility.sendYellowMessage(player, "\n[PvP Rules]" +  "\n # No Camping at Spawn Area" +  "\n # No Hacking" + "\n # No Bug Abusing" + "\n # And as always remember to kill "+ msg);
    }
}
