package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.player.PlayerService;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 离线角色修复工具；支持送回主城、按物品 ID 删除、全服清物与背包擦除等高危数据库操作。
 * Offline character repair toolkit for city return, item purge, global item delete and inventory wipe (high-risk DB ops).
 * @author Kill3r
 */
@Slf4j(topic = "GM_MONITOR_LOG")
public class Repairkit extends AdminCommand {

    public Repairkit(){
        super("repairkit");
    }


    /**
     * 执行该管理指令。
     * Executes this admin command.
     * @param admin 执行指令的管理员 / admin executing the command
     */
    public void execute(Player admin, String...params){
        if(params.length == 0){
            PacketSendUtility.sendMessage(admin, """
                    Syntax
                     //repairkit [operation] [playername] [playerRace | itemID]
                    ##### NOTE ####
                    # You will need playerRace to use the 'RETURN' Operation.
                    # You will need itemID to use the 'REMOVE' Operation.
                    # NEVER USE @LINK OF ITEMS.. ONLY ITEM ID!!!!.
                    # PLEASE STRICTLY FOLLOW THESE RULES , BECAUSE IF WRITTEN WRONG, THIS CAN CAUSE ALOT OF DATABASE LOSE!!!!!.
                    # Player Name Also MUST be used as Exactly case sensitive, (example: Extreme is correct , and extreme is wrong... First letter should be capital)\
                    #### NOTE END ####
                     \
                    # [operation] : return | remove | terminate | wipe
                    # [playername] : name of the player to initialize the operation
                    # [playerRace | itemID] : race should be given in correct spelling and case ( ELYOS | ASMODIANS ) case Sensitive!! OR use itemID if you want to remove an Item from the Player!!(NO LINKS ALLOWED, ONLY ITEM ID!!)
                    #######################""");
            return;
        }

        if(params[0].equalsIgnoreCase("return")){
            String playerToreturn = params[1];
            String raceOfPlayer = params[2];

            returnPlayer(admin, playerToreturn, raceOfPlayer.toUpperCase());
        }else if (params[0].equalsIgnoreCase("remove")){
            String playerToRemove = params[1];
            int itemId = Integer.parseInt(params[2]);

            removeItemByID(admin, playerToRemove, itemId);
        } else if (params[0].equalsIgnoreCase("terminate")){
            if(admin.getAccessLevel() <= 3){
                PacketSendUtility.sendMessage(admin, "You are not Insane Enough to use this Command! ;)");
                return;
            }
            int itemId = Integer.parseInt(params[1]);

            removeItemFromDatabase(admin, itemId);
        } else if (params[0].equalsIgnoreCase("wipe")){
            if(admin.getAccessLevel() <= 3){
                PacketSendUtility.sendMessage(admin, "You are not Insane Enough to use this Command! ;)");
                return;
            }
            String playerToWipe = params[1];

            wipeInventoryExceptEquiped(admin, playerToWipe);
        }

    }

    private void removeItemFromDatabase(final Player admin, final int itemId){
        try {
            DatabaseAccess.jdbcClient()
                .sql("DELETE FROM inventory WHERE item_id = ?")
                .param(itemId)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.f66c8c8d1e6d"));
        }
        PacketSendUtility.sendMessage(admin, "[item:"+itemId+"] ("+itemId+") has been successfully removed from all players!");
    }

    private void wipeInventoryExceptEquiped(final Player admin, final String playerToWipe){
        Player checkPOnline = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerToWipe);
        if(checkPOnline != null){
            PacketSendUtility.sendMessage(admin, "You cannot proceed if the player is Online!");
            return;
        }
        if (playerToWipe == admin.getName()){
            PacketSendUtility.sendMessage(admin, "Noob! you can't remove item from you're self!");
            return;
        }

        int playerID = 0;

        try {
            playerID = PlayerService.daos().playerDAO().getPlayerIdByName(playerToWipe);
        } catch (Exception e) {
            log.info(I18n.get("log.30d38a7399c3", admin.getName()));
        }

        if (playerID == 0){
            PacketSendUtility.sendMessage(admin, "Couldn't find that name in the database!");
            return;
        }

        try {
            DatabaseAccess.jdbcClient()
                .sql("DELETE FROM inventory WHERE item_owner = ? AND is_equiped = 0")
                .param(playerID)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.9757aec268c2", admin.getName()));
        }
        PacketSendUtility.sendMessage(admin, "All items from the player has been removed except the Equiped ones!");
        log.error(I18n.get("log.b48fef936307", admin.getName(), playerToWipe));
    }

    private void removeItemByID(final Player admin, final String playerToRemoveFrom, final int itemID){
        Player checkPlayerOnline = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerToRemoveFrom);
        if(checkPlayerOnline != null){
            PacketSendUtility.sendMessage(admin, "You can only remove items from Offline Player using this Command, If you want to remove item from Online Players, use '//remove'!!");
            return;
        }
        if (playerToRemoveFrom == admin.getName()){
            PacketSendUtility.sendMessage(admin, "Noob! you can't remove item from you're self!");
            return;
        }

        int playerID = 0;

        try {
            playerID = PlayerService.daos().playerDAO().getPlayerIdByName(playerToRemoveFrom);
        } catch (Exception e) {
            log.info(I18n.get("log.51323fb6b7d1", admin.getName()));
        }

        if (playerID == 0){
            PacketSendUtility.sendMessage(admin, "Couldn't find that name in the database!");
            return;
        }

        try {
            DatabaseAccess.jdbcClient()
                .sql("DELETE FROM inventory WHERE item_id = ? AND item_owner = ?")
                .param(itemID).param(playerID)
                .update();
        } catch (Exception e){
            log.error(I18n.get("log.ab777d089876", admin.getName()));
        }
        PacketSendUtility.sendMessage(admin, "[item:"+itemID+"] ("+itemID+") successfully removed from '" + playerToRemoveFrom + "'");
        log.error(I18n.get("log.3a3cf03b9db4", admin.getName(), itemID, playerToRemoveFrom));
    }

    private void returnPlayer(final Player admin, final String playerToReturn, final String race){
        Player checkPlayerOnline = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(playerToReturn));
        if(checkPlayerOnline != null){
            PacketSendUtility.sendMessage(admin, "You cannot return a player while he's Online!");
            return;
        }
        if (playerToReturn == admin.getName()){
            PacketSendUtility.sendMessage(admin, "Noob! you can't return you're self!");
            return;
        }

        float x,y,z;
        int heading,world_id;

        if(race.equalsIgnoreCase("ELYOS") || race.equalsIgnoreCase("ASMODIANS")){
            PacketSendUtility.sendMessage(admin, "GIVEN RACE : CORRECT!");
        }else{
            PacketSendUtility.sendMessage(admin, "The Given RACE is not correct!.. Please use 'ELYOS' or 'ASMODIANS'");
            return;
        }

        if (race.equalsIgnoreCase("ELYOS")){
            x = 1443.6982f;
            y = 1574.4116f;
            z = 572.87537f;
            world_id = 110010000;
            heading = 0;
        }else if (race.equalsIgnoreCase("ASMODIANS")){
            x = 1316.4557f;
            y = 1425.6559f;
            z = 209.09084f;
            world_id = 120010000;
            heading = 65;
        } else {
            x = admin.getX();
            y = admin.getY();
            z = admin.getZ();
            world_id = admin.getWorldId();
            heading = admin.getHeading();
        }

        int playerID = 0;

        try {
            playerID = PlayerService.daos().playerDAO().getPlayerIdByName(playerToReturn);
        } catch (Exception e) {
            log.info(I18n.get("log.d256e26b97d9", admin.getName()));
        }

        if (playerID == 0){
            PacketSendUtility.sendMessage(admin, "Couldn't find that name in the database!");
            return;
        }

        try{
            DatabaseAccess.jdbcClient()
                .sql("UPDATE players SET x = ?, y = ?, z = ?, heading = ?, world_id = ? WHERE id = ?")
                .param(x).param(y).param(z).param(heading).param(world_id).param(playerID)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.1471af37e6dd", admin.getName()));
        }
        PacketSendUtility.sendMessage(admin, playerToReturn+" has been successfully moved to main city of '"+race+"'.");
        log.error(I18n.get("log.e5aa7080240f", admin.getName(), playerToReturn, race));
    }

}
