package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.miren;

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
 * Reshanta 区域 NPC AI：Miren Crystal Sword（@AIName "miren_crystal_sword"），继承 NpcAI2。
 * Reshanta zone NPC AI: Miren Crystal Sword (@AIName "miren_crystal_sword"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("miren_crystal_sword")
public class Miren_Crystal_SwordAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000247) != null) { //Spirit Of Miren's Pendant.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			// 需要米伦之灵吊坠才能打破此封印。 / Spirit of Miren's Pendant is required to break this seal.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Named_Spawn_Fail02);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000247, 1)) { //Spirit of Miren's Pendant.
			switch (getNpcId()) {
				case 702842: //Miren's Crystal Sword [Elyos]
					announceSpiritOfMiren30Min();
					announceSpiritOfMirenAppears();
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(883662, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Miren.
						}
					}, 1800000); //30 Minutes.
				break;
				case 702843: //Miren's Crystal Sword [Asmodians]
					announceSpiritOfMiren30Min();
					announceSpiritOfMirenAppears();
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(884028, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Miren.
						}
					}, 1800000); //30 Minutes.
				break;
			}
		}
		// 米伦之灵吊坠打破了封印。 / Spirit Of Miren's Pendant has broken the seal.
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Lamiren_Named_Spawn_Item);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
		return true;
	}
	
	private void announceSpiritOfMirenAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 米伦守护之灵将在 5 分钟后出现。 / The Miren Protector Spirit will appear after 5 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_05, 1500000);
				// 米伦守护之灵将在 3 分钟后出现。 / The Miren Protector Spirit will appear after 3 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_03, 1620000);
				// 米伦守护之灵将在 1 分钟后出现。 / The Miren Protector Spirit will appear after 1 minute
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_01, 1740000);
			}
		});
	}
	
	private void announceSpiritOfMiren30Min() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 米伦之灵将在 30 分钟后从米伦水晶剑中被召唤。 / Spirit of Miren will be summoned from Miren's Crystal Sword in 30 minutes.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Lamiren_Named_Spawn_System);
			}
		});
	}
}
