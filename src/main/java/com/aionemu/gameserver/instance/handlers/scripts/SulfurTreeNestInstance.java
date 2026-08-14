package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.Future;

/**
 * 硫磺树巢副本事件处理器。
 * Instance event handler for Sulfur Tree Nest.
 *
 * @author Encom
 */

@InstanceID(300060000)
public class SulfurTreeNestInstance extends GeneralInstanceHandler
{
		/** sulfurtreenest 任务 / sulfur tree nest task */
		private Future<?> sulfurTreeNestTask;
		/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        spawnSulfurTreeNestRings();
    }
	
	/**
	 * 玩家通过飞行环时处理。
	 * Handle a player passing a flying ring.
	 *
	 * @param player 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * @return 结果 / result
	 */
	@Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
        if (flyingRing.equals("SULFUR_TREE_NEST")) {
		    if (!isStartTimer) {
			    isStartTimer = true;
			    System.currentTimeMillis();
			    instance.doOnAllPlayers(new Visitor<Player>() {
			        /**
			         * 处理 visit。
			         * Handle visit.
			         *
			         * @param player 玩家 / player
			         */
			        @Override
			        public void visit(Player player) {
						if (player.isOnline()) {
							startSulfurTreeNestTimer();
							PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
							// 龙族防护魔法结界已激活。 / The Balaur protective magic ward has been activated.
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
						}
					}
				});
			}
		}
		return false;
	}
	
	private void startSulfurTreeNestTimer() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
				    sulfurTreeNestTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						/**
						 * 处理 run。
						 * Handle run.
						 */
						@Override
						public void run() {
							instance.doOnAllPlayers(new Visitor<Player>() {
								/**
								 * 处理 visit。
								 * Handle visit.
								 *
								 * @param player 玩家 / player
								 */
								@Override
								public void visit(Player player) {
									onExitInstance(player);
								}
							});
							onInstanceDestroy();
						}
					}, 910000); //15 Minutes + 10s.
				}
			}
		});
    }
	
	private void spawnSulfurTreeNestRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("SULFUR_TREE_NEST", mapId,
        new Point3D(462.9394, 380.34888, 168.97256),
        new Point3D(462.9394, 380.34888, 174.97256),
        new Point3D(468.9229, 380.7933, 168.97256), 6), instanceId);
        f1.spawn();
    }
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
	}
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}