package com.aionemu.gameserver.ai.instance.contaminedUnderpath;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Contamined Underpath 副本 NPC AI：Defense Turret Platform（@AIName "defense_turret_platform"），继承 NpcAI2。
 * Contamined Underpath instance NPC AI: Defense Turret Platform (@AIName "defense_turret_platform"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("defense_turret_platform")
public class Defense_Turret_PlatformAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 10) {
		    // 这是主神创造的魔法变身背包。 / This is a magical transformation cube created by the Empyrean Lord.
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		} else {
            // 你可使用【明亮奥德】设置任意炮塔。 / You can use [Bright Aether] to set up any turret you like.
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        }
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		// 速射多管火炮安装（1 明亮奥德）。 / Rapid Fire Multiple Fire Cannon Installation (1 Bright Aether).
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(182007405, 1)) { // 明亮奥德 / Bright Aether.
			spawn(833808, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); // 单发火焰加农 / Single Fire Cannon.
		}
		// 远程加农安装（2 明亮奥德） / Ranged Cannon Installation (2 Bright Aether)
		else if (dialogId == 10001 && player.getInventory().decreaseByItemId(182007405, 2)) { // 明亮奥德 / Bright Aether.
			spawn(833809, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); // 区域防空炮 / Area Antiaircraft Gun.
		}
		// 强力魔法加农安装（2 明亮奥德）。 / Powerful Magic Cannon Installation (2 Bright Aether).
		else if (dialogId == 10002 && player.getInventory().decreaseByItemId(182007405, 2)) { // 明亮奥德 / Bright Aether.
			spawn(833810, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); // 广域捕获装置 / Wide Area Capture Device.
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
}
