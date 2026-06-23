package admincommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

public class Cooldown extends AdminCommand {

    public Cooldown() {
        super("cooldown");
    }

    @Override
    public void execute(Player admin, String... params) {
        Player target = admin;
        
        if (params.length > 0) {
            String targetName = params[0];
            target = World.getInstance().findPlayer(targetName);
            
            if (target == null) {
                PacketSendUtility.sendMessage(admin, "Player " + targetName + " not found online.");
                return;
            }
        }
        
        resetAllCooldowns(target);
        
        PacketSendUtility.sendMessage(admin, "All cooldowns have been reset for " + target.getName() + ".");
        if (!admin.equals(target)) {
            PacketSendUtility.sendMessage(target, "Your cooldowns have been reset by an administrator.");
        }
    }

    /**
     * Reset all cooldowns (skills and items)
     */
    private void resetAllCooldowns(Player player) {
        List<Integer> delayIds = new ArrayList<Integer>();
        
        if (player.getSkillCoolDowns() != null && !player.getSkillCoolDowns().isEmpty()) {
            long currentTime = System.currentTimeMillis();
            for (Entry<Integer, Long> en : player.getSkillCoolDowns().entrySet()) {
                delayIds.add(en.getKey());
            }
            for (Integer delayId : delayIds) {
                player.setSkillCoolDown(delayId, currentTime);
            }
            delayIds.clear();
            PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
        }

        if (player.getItemCoolDowns() != null && !player.getItemCoolDowns().isEmpty()) {
            for (Entry<Integer, ItemCooldown> en : player.getItemCoolDowns().entrySet()) {
                delayIds.add(en.getKey());
            }
            for (Integer delayId : delayIds) {
                player.addItemCoolDown(delayId, 0, 0);
            }
            delayIds.clear();
            PacketSendUtility.sendPacket(player, new SM_ITEM_COOLDOWN(player.getItemCoolDowns()));
        }
    }

    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, "Syntax: //cooldown <playerName>");
        PacketSendUtility.sendMessage(player, "Example: //cooldown - reset your own cooldowns");
        PacketSendUtility.sendMessage(player, "Example: //cooldown PlayerName - reset cooldowns for PlayerName");
    }
}