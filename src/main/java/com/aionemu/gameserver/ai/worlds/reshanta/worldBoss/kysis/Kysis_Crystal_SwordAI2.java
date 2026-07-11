package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.kysis;

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
 * Reshanta 区域 NPC AI：Kysis Crystal Sword（@AIName "kysis_crystal_sword"），继承 NpcAI2。
 * Reshanta zone NPC AI: Kysis Crystal Sword (@AIName "kysis_crystal_sword"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kysis_crystal_sword")
public class Kysis_Crystal_SwordAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000246) != null) { //Spirit Of Kysis's Pendant.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			// 需要基西斯之灵吊坠才能打破此封印。 / Spirit of Kysis's Pendant is required to break this seal.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Named_Spawn_Fail03);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000246, 1)) { //Spirit Of Kysis's Pendant.
			switch (getNpcId()) {
				case 702844: //Kysis's Crystal Sword [Elyos]
				    announceSpiritOfKysis30Min();
					announceSpiritOfKysisAppears();
				    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(883663, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Kysis.
						}
					}, 1800000); //30 Minutes.
			    break;
				case 702845: //Kysis's Crystal Sword [Asmodians]
					announceSpiritOfKysis30Min();
					announceSpiritOfKysisAppears();
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(884029, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Kysis.
						}
					}, 1800000); //30 Minutes.
				break;
			}
		}
		// 基西斯之灵吊坠打破了封印。 / Spirit Of Kysis' Pendant has broken the seal.
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Dkisas_Named_Spawn_Item);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
		return true;
	}
	
	private void announceSpiritOfKysisAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 基西斯守护之灵将在 5 分钟后出现。 / The Kysis Protector Spirit will appear after 5 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_05, 1500000);
				// 基西斯守护之灵将在 3 分钟后出现。 / The Kysis Protector Spirit will appear after 3 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_03, 1620000);
				// 基西斯守护之灵将在 1 分钟后出现。 / The Kysis Protector Spirit will appear after 1 minute
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_01, 1740000);
			}
		});
	}
	
	private void announceSpiritOfKysis30Min() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 基西斯之灵将在 30 分钟后从基西斯水晶剑中被召唤。 / Spirit of Kysis will be summoned from Kysis's Crystal Sword in 30 minutes.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Dkisas_Named_Spawn_System);
			}
		});
	}
}
