package com.aionemu.gameserver.ai.worlds.kaldor;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Kaldor 区域 NPC AI：Anoha Sword（@AIName "anoha_sword"），继承 NpcAI2。
 * Kaldor zone NPC AI: Anoha Sword (@AIName "anoha_sword"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("anoha_sword")
public class Anoha_SwordAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000215) != null) { // 阿诺哈封印石 / Anoha Sealing Stone.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000215, 1)) { // 阿诺哈封印石 / Anoha Sealing Stone.
		    switch (getNpcId()) {
		        case 804576: // 阿诺哈之剑 [天族] / Anoha Sword [Elyos]
			    case 804577: // 阿诺哈之剑 [魔族] / Anoha Sword [Asmodians]
					announceBerserkAnoha30Min();
					spawn(702644, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							announceReleaseAnoha();
							spawn(855263, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 狂暴的阿诺哈 / Berserk Anoha.
						}
					}, 1800000); // 30 分钟 / 30 Minutes.
				break;
			}
		}
		// 使用阿诺哈封印石释放了阿诺哈。 / The Anoha Sealing Stone was used to release Anoha.
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Named_Spawn_Item);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
		return true;
	}
	
	private void announceBerserkAnoha30Min() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 狂暴阿诺哈将在 30 分钟后返回卡尔多。 / Berserk Anoha will return to Kaldor in 30 minutes.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Named_Spawn_System);
			}
		});
	}
	
	private void announceReleaseAnoha() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 释放阿诺哈。 / Release Anoha.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Named_Spawn);
			}
		});
	}
}
