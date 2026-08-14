package com.aionemu.gameserver.ai.instance.ophidanWarpath;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Ophidan Warpath 副本 NPC AI：Idle Power Generator（@AIName "idle_power_generator"），继承 NpcAI2。
 * Ophidan Warpath instance NPC AI: Idle Power Generator (@AIName "idle_power_generator"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("idle_power_generator")
public class Idle_Power_GeneratorAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		switch (getNpcId()) {
			case 806391: // 北方发电机。 / North Power Generator.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
						announceWarNeu01();
						spawn(833935, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 3);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
			case 806392: // 南方发电机。 / South Power Generator.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
						announceWarNeu01();
						spawn(833936, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 42);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
		}
		super.handleSpawned();
	}
	
	private void announceWarNeu01() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5_Under_02_war_neu_01);
			}
		});
	}
}
