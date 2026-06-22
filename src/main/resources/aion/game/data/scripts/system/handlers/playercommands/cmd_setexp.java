package playercommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

public class cmd_setexp extends PlayerCommand {

    public cmd_setexp() {
        super("setexp");
    }

    @Override
    public void execute(Player player, String... params) {
        if (params.length != 1) {
            onFail(player, null);
            return;
        }
        try {
            double multiplier = Double.parseDouble(params[0]);
            if (multiplier < 0 || multiplier > 10) { // 限制在0%到1000%之间
                PacketSendUtility.sendMessage(player, "Multiplier must be between 0.0 and 10.0.");
                return;
            }
            player.getCommonData().setExpMultiplier(multiplier);
            PacketSendUtility.sendMessage(player, "Experience multiplier set to " + (multiplier * 100) + "%.");
        } catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(player, "Invalid multiplier value.");
        }
    }

    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, "Usage: .setexp <multiplier>");
    }
}