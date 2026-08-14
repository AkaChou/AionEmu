package com.aionemu.gameserver.instance.handlers.scripts;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.HallOfTenacityReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.HallOfTenacityPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 坚韧大厅副本事件处理器。
 * Instance event handler for Hall Of Tenacity.
 *
 * @author Encom
 */


@InstanceID(302320000)
@Slf4j
public class HallOfTenacityInstance extends GeneralInstanceHandler {
	/** 副本时间戳 / instance timestamp */
	private long instanceTime;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed = false;
	/** 副本奖励对象 / instance reward object */
	protected HallOfTenacityReward instanceReward;
        /** 副本是否已开始 / whether the instance started */
        protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
        /** hot 任务 / hot task */
        private final List<Future<?>> hotTask = new ArrayList<Future<?>>();
    /**
     * 返回玩家奖励记录。
     * Return the player's reward record.
     * 
     * @param object 可见对象 / visible object
     * @return 结果 / result
     */

    protected HallOfTenacityPlayerReward getPlayerReward(Integer object) {
		instanceReward.regPlayerReward(object);
		return (HallOfTenacityPlayerReward) instanceReward.getPlayerReward(object);
	}

    private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}

    /**
     * 返回本副本奖励对象。
     * Return this instance's reward object.
     *
     * @return 结果 / result
     */
    @Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

    /**
     * 副本创建时初始化逻辑。
     * Initialize logic when the instance is created.
     *
     * @param instance 世界地图实例 / world-map instance
     */
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        instanceReward = new HallOfTenacityReward(mapId, instanceId, instance);
        instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        startInstanceTask();
    }

    /**
     * 玩家进入副本时处理。
     * Handle a player entering the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onEnterInstance(final Player player) {
	Integer object = player.getObjectId();
	if (!containPlayer(object)) {
			instanceReward.regPlayerReward(object);
			getPlayerReward(object).applyBoostMoraleEffect(player);
			instanceReward.setStartPositions();
		}
        //sendEnterPacket(player);
    }

    /**
     * 玩家请求退出副本时处理。
     * Handle a player exit request.
     *
     * @param player 玩家 / player
     */
    @Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}

    /**
     * 玩家离开副本时处理。
     * Handle a player leaving the instance.
     *
     * @param player 玩家 / player
     */
    @Override
	public void onLeaveInstance(Player player) {
		//clearDebuffs(player);
	HallOfTenacityPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			instanceReward.removePlayerReward(playerReward);
		}
	}

    /**
     * 副本销毁时清理资源。
     * Clean up resources when the instance is destroyed.
     */
    @Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		instanceReward.clear();
	}

    private void sendEnterPacket(final Player player) {
	instance.doOnAllPlayers(new Visitor<Player>() {
            /**
             * 处理 visit。
             * Handle visit.
             *
             * @param player 玩家 / player
             */
            @Override
            public void visit(Player player) {
	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(0, getTime(), instanceReward, instance.getPlayersInside(), true));
	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(9, getTime(), instanceReward, instance.getPlayersInside(), true));
            }
        });
    }
    /**
     * 启动副本计时/任务。
     * Start instance timer/tasks.
     */

    protected void startInstanceTask() {
	instanceTime = System.currentTimeMillis();
	hotTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!instanceReward.isRewarded()) {
	instanceReward.setInstanceStartTime();
	instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
	instanceReward.setCoupleSlotForBattle32();
	instanceReward.sendLog("Hall Of Tenacity got "+instance.getPlayersInside().size()+" player(s)");
	//instanceReward.sendPacket(0, null);
	//instanceReward.sendPacket(9, null);
	instance.doOnAllPlayers(new Visitor<Player>() {
                        /**
                         * 处理 visit。
                         * Handle visit.
                         *
                         * @param player 玩家 / player
                         */
                        @Override
                        public void visit(Player player) {
	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(0, getTime(), instanceReward, instance.getPlayersInside(), true));
	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(9, getTime(), instanceReward, instance.getPlayersInside(), true));
                        }
                    });
				}
            }
        }, 60000));//after enter 1 min will show versus board
	hotTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!instanceReward.isRewarded()) {
				    instance.doOnAllPlayers(new Visitor<Player>() {

			            /**
			             * 处理 visit。
			             * Handle visit.
			             *
			             * @param player 玩家 / player
			             */
			            @Override
			            public void visit(Player player) {
			            	sendRequest(player);
			            }
			        });
				}
            }
        }, 150000));//after enter 1 min 30s will show enter battle window
    }
    /**
     * 处理 sendRequest。
     * Handle sendRequest.
     * 
     * @param player 玩家 / player
     */

    public void sendRequest(final Player player) {
        RequestResponseHandler responseHandler = new RequestResponseHandler(player) {
            /**
             * 处理 acceptRequest。
             * Handle acceptRequest.
             *
             * @param requester 请求者 / requester
             * @param responder 响应者 / responder
             */
            @Override
            public void acceptRequest(Creature requester, Player responder) {
	instanceReward.portToArena(player);
            }
            /**
             * 处理 denyRequest。
             * Handle denyRequest.
             *
             * @param requester 请求者 / requester
             * @param responder 响应者 / responder
             */
            @Override
            public void denyRequest(Creature requester, Player responder) {
            }
        };
        boolean requested = player.getResponseRequester().putRequest(907265, responseHandler);
        if (requested) {
            PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(907265, 60, 0));
        }
    }
    /**
     * 停止副本并结算。
     * Stop the instance and settle.
     * 
     * @param race 阵营 / race
     */

    protected void stopInstance(Race race) {
        stopInstanceTask();
        //hallOfTenacityReward.setWinner(race);
        instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        reward();
    }
    /**
     * 处理 reward。
     * Handle reward.
     */

    protected void reward() {

    }

    private void stopInstanceTask() {
        for (Future<?> task : hotTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }

    private int getTime() {
        long result = System.currentTimeMillis() - instanceTime;
        if (result < 60000) {
            return (int) (60000 - result);
        } else if (result < 300000) { //5-Mins
            return (int) (300000 - (result - 60000));
        }
        return 0;
    }
    /**
     * 向副本内玩家发送消息。
     * Send a message to players in the instance.
     * 
     * @param msg 消息 / message
     * @param race 阵营 / race
     * @param time 时间 / time
     */

    protected void sendMsg(final int msg, final Race race, int time) {
	hotTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
                        if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
                            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
                        }
                    }
                });
            }
        }, time));
    }
}
