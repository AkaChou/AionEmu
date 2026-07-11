package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.Collection;

/**
 * 充能 NPC 召唤指令；在管理员位置开关充能器（NPC 730397）。
 * Admin command that toggles a recharger NPC (id 730397) at the admin position.
 *
 * @author Kill3r
 */
public class Recharger extends AdminCommand {

    public Recharger(){
        super("recharger");
    }

    private static boolean isOpened = false;

    /**
     * 按 {@code on}/{@code off} 生成或删除充能器 NPC。
     * Spawns or removes the recharger NPC based on {@code on}/{@code off}.
     *
     * @param player 执行指令的管理员 / admin executing the command
     * {@code on} or {@code off}。 / {@code on} or {@code off}
     */
    public void execute(Player player, String...params){
        int RechargerID = 730397;
        if(params[0].equals("off")){
            if(isOpened){
                Collection<Npc> recharger = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs();
                for(Npc n : recharger){
                    if(n.getNpcId() == RechargerID){
                        n.getController().delete();
                    }
                }
                PacketSendUtility.sendMessage(player, "Recharger Closing!");
                isOpened = false;
            }
        }else if(params[0].equals("on")){
            float x = player.getX();
            float y = player.getY();
            float z = player.getZ();
            byte heading = player.getHeading();
            int worldId = player.getWorldId();
            if(!isOpened){
                SpawnTemplate spawn = SpawnEngine.addNewSpawn(worldId, RechargerID, x, y, z, heading, 0);
                VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, player.getInstanceId());
                PacketSendUtility.sendMessage(player, visibleObject.getName() + " has been Summoned!");
                isOpened = true;
            }else{
                PacketSendUtility.sendMessage(player, "Already Open");
            }
        }
    }
}