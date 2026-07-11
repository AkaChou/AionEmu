package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RvR 相关 NPC AI：Rift Generator（@AIName "rift_generator"），继承 NpcAI2。
 * RvR-related NPC AI: Rift Generator (@AIName "rift_generator"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("rift_generator")
public class Rift_GeneratorAI2 extends NpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			announceRiftGeneratorUnderAttack();
		}
	}
	
	@Override
	protected void handleDied() {
        announceRiftGeneratorDie();
		super.handleDied();
	}
	
	private void announceRiftGeneratorUnderAttack() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 裂隙发生器遭受攻击！被摧毁后次元漩涡将关闭。 / The Rift Generator is under attack! Once it is destroyed, the Dimensional Vortex will close.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CHAT_INVADEPORTL_KEEPER_SYSTEM_MSG01);
			}
		});
	}
	private void announceRiftGeneratorDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 裂隙发生器已被摧毁。 / The Rift Generator has been destroyed.
				// 次元漩涡即将关闭，渗透联盟将解散，成员将被送回。 / The Dimensional Vortex will close shortly, the infiltration alliance will be disbanded, and its members will be returned home.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CHAT_INVADEPORTL_KEEPER_SYSTEM_MSG03);
			}
		});
	}
}
