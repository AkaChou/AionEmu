package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.HallOfTenacityReward;
import com.aionemu.gameserver.model.instance.playerreward.HallOfTenacityPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 孤独竞技场副本事件处理器。
 * Instance event handler for Arena Of Tenacity.
 *
 * @author Encom
 */


@InstanceID(302310000)
public class ArenaOfTenacityInstance extends GeneralInstanceHandler {
	/** 副本时间戳 / instance timestamp */
	private long instanceTime;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
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
        doors = instance.getDoors();
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
        PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, player, 0, instanceReward));
	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(4, player, getTime(), instanceReward));
	Iterator<Player> iter = instance.getPlayersInside().iterator();
	while (iter.hasNext()) {
		PacketSendUtility.sendPacket(iter.next(), new SM_INSTANCE_SCORE(11, iter.next(), iter.next(), 0, instanceReward));
	}
        //sendEnterPacket(player);
    }

    /**
     * 处理玩家复活事件。
     * Handle a player revive event.
     *
     * @param player 玩家 / player
     * @return 结果 / result
     */
    @Override
    public boolean onReviveEvent(Player player) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PlayerReviveService.revive(player, 100, 100, false, 0);
        player.getGameStats().updateStatsAndSpeedVisually();
        instanceReward.portToArena(player);
        return true;
    }

    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * @param player 玩家 / player
     * @param lastAttacker 最后攻击者 / last attacker
     * @return 结果 / result
     */
    @Override
    public boolean onDie(Player player, Creature lastAttacker) {
	HallOfTenacityPlayerReward ownerReward = instanceReward.getPlayerReward(player.getObjectId());
	sendPacket();
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player);
        return true;
    }
    /**
     * 向副本内玩家发送数据包。
     * Send a packet to players in the instance.
     */

    protected void sendPacket() {
		instanceReward.sendPacket();
	}
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		HallOfTenacityPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			instanceReward.removePlayerReward(playerReward);
		}
	}

    /**
     * 启动副本计时/任务。
     * Start instance timer/tasks.
     */

    protected void startInstanceTask() {
	instanceTime = System.currentTimeMillis();
	instanceReward.setInstanceStartTime();
	hotTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!instanceReward.isRewarded()) {
	openDoors();
	instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
				    Iterator<Player> iter = instance.getPlayersInside().iterator();
			    	while (iter.hasNext()) {
			    		PacketSendUtility.sendPacket(iter.next(), new SM_INSTANCE_SCORE(5, iter.next(), iter.next(), getTime(), instanceReward));
			    	}
                    //startInstancePacket();
				}
            }
        }, 60000));
	hotTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!instanceReward.isRewarded()) {
	stopInstance();
				}
            }
        }, 300000));
    }
    /**
     * 停止副本并结算。
     * Stop the instance and settle.
     */

    protected void stopInstance() {
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
    /**
     * 处理 openDoors。
     * Handle openDoors.
     */

    protected void openDoors() {
        openDoor(157);
		openDoor(7);
    }
    /**
     * 打开指定门。
     * Open the given door.
     * 
     * @param doorId 门 ID / doorId
     */

    protected void openDoor(int doorId) {
        StaticDoor door = doors.get(doorId);
        if (door != null) {
            door.setOpen(true);
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
