package com.aionemu.gameserver.services.agentservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 代理战启动定时任务。
 * Start runnable for Agent Fight events.
 *
 * <p>提前广播倒计时消息，延时启动战斗并向全服玩家通知代理出现。
 * Broadcasts countdown messages, starts the fight on delay, and notifies all players of the agent spawn.</p>
 *
 * @author Rinzler (Encom)
 */
public class AgentStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public AgentStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行倒计时与启动流程。
	 * Runs the countdown and start sequence.
	 */
	@Override
	public void run() {
		// 代理人之战将在 10 分钟后开始。 / The Agent battle will start in 10 minutes.
		GameLocationBootstrapServices.agentService().agentBattleMsg1(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 代理人之战将在 5 分钟后开始。 / The Agent battle will start in 5 minutes.
				GameLocationBootstrapServices.agentService().agentBattleMsg2(id);
			}
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, AgentLocation> locations = GameLocationBootstrapServices.agentService().getAgentLocations();
				for (final AgentLocation loc : locations.values()) {
					if (loc.getId() == id) {
						// 总督苏纳亚卡 5.8 / Governor Sunayaka 5.8
						GameLocationBootstrapServices.agentService().governorSunayakaMsg(id);
						// 狂战士苏纳亚卡 5.8 / Berserker Sunayaka 5.8
						GameLocationBootstrapServices.agentService().berserkerSunayakaMsg(id);
						// 代理人之战 4.7 / Agent Fight 4.7
						GameLocationBootstrapServices.agentService().startAgentFight(loc.getId());
					}
				}
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 一名代理人已生成。 / An Agent has spawned.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_GodElite);
					}
				});
			}
		}, 600000);
	}
}
