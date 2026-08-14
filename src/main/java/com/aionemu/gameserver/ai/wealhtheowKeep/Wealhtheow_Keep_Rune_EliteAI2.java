package com.aionemu.gameserver.ai.wealhtheowKeep;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 维尔索要塞相关 NPC AI：Wealhtheow Keep Rune Elite（@AIName "rune_elite"），继承 AggressiveNpcAI2。
 * Wealhtheow Keep related NPC AI: Wealhtheow Keep Rune Elite (@AIName "rune_elite"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("rune_elite")
public class Wealhtheow_Keep_Rune_EliteAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		announceRuneElite();
	}
	
	@Override
	protected void handleDied() {
		// 指定 NPC 死亡后 10 秒在固定位置生成 3 个宝箱。 / Spawns 3 treasure chests at fixed spots 10s after these NPCs die.
		switch (getNpcId()) {
			// 韦尔休堡垒。 / Wealhtheow's Keep.
			case 251825:
			case 251830:
				treasureChest();
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			        @Override
			        public void run() {
						spawn(701481, 780.46515f, 288.62924f, 143.18782f, (byte) 45);
                        spawn(701481, 787.1314f, 288.72644f, 143.20233f, (byte) 30);
                        spawn(701481, 793.9525f, 289.05054f, 143.18248f, (byte) 15);
			        }
		        }, 10000);
			break;
		}
		super.handleDied();
	}
	
	private void treasureChest() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 出现了一个宝箱。 / A treasure chest has appeared.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdC_BoxSpawn);
			}
		});
	}
	
	private void announceRuneElite() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_RuneElite);
			}
		});
	}
}
