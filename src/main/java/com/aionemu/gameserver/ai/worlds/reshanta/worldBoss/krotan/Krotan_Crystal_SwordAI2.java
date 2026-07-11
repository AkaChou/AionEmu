package com.aionemu.gameserver.ai.worlds.reshanta.worldBoss.krotan;

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
 * Reshanta 区域 NPC AI：Krotan Crystal Sword（@AIName "krotan_crystal_sword"），继承 NpcAI2。
 * Reshanta zone NPC AI: Krotan Crystal Sword (@AIName "krotan_crystal_sword"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("krotan_crystal_sword")
public class Krotan_Crystal_SwordAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000245) != null) { //Spirit Of Krotan's Pendant.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			// 需要克罗坦之灵吊坠才能打破此封印。 / Spirit of Krotan's Pendant is required to break this seal.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Named_Spawn_Fail01);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000245, 1)) { //Spirit Of Krotan's Pendant.
			switch (getNpcId()) {
				case 702840: //Krotan's Crystal Sword [Elyos]
				    announceSpiritOfKrotan30Min();
					announceSpiritOfKrotanAppears();
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(883323, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Krotan.
						}
					}, 1800000); //30 Minutes.
				break;
				case 702841: //Krotan's Crystal Sword [Asmodians]
				    announceSpiritOfKrotan30Min();
					announceSpiritOfKrotanAppears();
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							spawn(884027, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Spirit Of Krotan.
						}
					}, 1800000); //30 Minutes.
				break;
			}
		}
		// 克罗坦之灵吊坠打破了封印。 / Spirit Of Krotan's Pendant has broken the seal.
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Crotan_Named_Spawn_Item);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
		return true;
	}
	
	private void announceSpiritOfKrotanAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦守护之灵将在 5 分钟后出现。 / The Krotan Protector Spirit will appear after 5 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_05, 1500000);
				// 克罗坦守护之灵将在 3 分钟后出现。 / The Krotan Protector Spirit will appear after 3 minutes.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_03, 1620000);
				// 克罗坦守护之灵将在 1 分钟后出现。 / The Krotan Protector Spirit will appear after 1 minute.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_01, 1740000);
			}
		});
	}
	
	private void announceSpiritOfKrotan30Min() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦之灵将在 30 分钟后从克罗坦水晶剑中被召唤。 / The Spirit of Krotan will be summoned from Krotan's Crystal Sword in 30 minutes.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Ab1_Crotan_Named_Spawn_System);
			}
		});
	}
}
