package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.EngulfedOphidanBridgeReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 淹没的奥菲丹桥副本事件处理器。
 * Instance event handler for Engulfed Ophidan Bridge.
 *
 * @author Encom
 */

@InstanceID(301210000)
public class EngulfedOphidanBridgeInstance extends GeneralInstanceHandler
{
	/** 副本时间戳 / instance timestamp */
	private long instanceTime;
	/** 能量发生器 / power generator */
		private int powerGenerator;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
    /** engulfed ophidan bridge reward / engulfed ophidan bridge reward */
        protected EngulfedOphidanBridgeReward engulfedOphidanBridgeReward;
    /** 败方倍率 / losing-group multiplier */
        private float loosingGroupMultiplier = 1;
    /** 副本是否已销毁 / whether the instance is destroyed */
    private boolean isInstanceDestroyed = false;
    /** 副本是否已开始 / whether the instance started */
        protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
    /** ophidan 任务 / ophidan task */
        private final List<Future<?>> ophidanTask = new ArrayList<Future<?>>();
	
    protected EngulfedOphidanBridgePlayerReward getPlayerReward(Player player) {
        engulfedOphidanBridgeReward.regPlayerReward(player);
        return (EngulfedOphidanBridgePlayerReward) engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
    }
	
    private boolean containPlayer(Integer object) {
        return engulfedOphidanBridgeReward.containPlayer(object);
    }
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
        //http://aion.power.plaync.com/wiki/%EC%9A%94%EB%A5%B4%EB%AC%B8%EA%B0%84%EB%93%9C+%EC%A7%84%EA%B2%A9%EB%A1%9C+-+%EC%A7%84%ED%96%89+%EC%A0%95%EB%B3%B4
		switch (npcId) {
			case 701974: //Supply Box.
			case 701975: //Emergency Supply Box.
			case 701976: //Hidden Supply Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000279, 1)); //进阶路线传送卷轴。 / Advance Route Teleport Scroll.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000148, 1)); //Special Baily Juice.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000278, 1)); //Bombing Device Activation Key.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000150, 1)); //Emergency Stasis Potion.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000149, 1)); //Ambush Scroll.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000147, 1)); //Emergency Support Recovery Potion.
			break;
        }
    }
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000279, storage.getItemCountByItemId(164000279)); //进阶路线传送卷轴。 / Advance Route Teleport Scroll.
		storage.decreaseByItemId(162000148, storage.getItemCountByItemId(162000148)); //Special Baily Juice.
		storage.decreaseByItemId(164000278, storage.getItemCountByItemId(164000278)); //Bombing Device Activation Key.
		storage.decreaseByItemId(162000150, storage.getItemCountByItemId(162000150)); //Emergency Stasis Potion.
		storage.decreaseByItemId(162000149, storage.getItemCountByItemId(162000149)); //Ambush Scroll.
		storage.decreaseByItemId(162000147, storage.getItemCountByItemId(162000147)); //Emergency Support Recovery Potion.
	}
	
    protected void startInstanceTask() {
    	instanceTime = System.currentTimeMillis();
        engulfedOphidanBridgeReward.setInstanceStartTime();
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!engulfedOphidanBridgeReward.isRewarded()) {
				    openFirstDoors();
				    // 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
				    sendMsgByRace(1401181, Race.PC_ALL, 5000);
                    engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
                    startInstancePacket();
                    engulfedOphidanBridgeReward.sendPacket(4, null);
				}
            }
        }, 90000));
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 较弱阵营的增援已抵达哨所。 / Reinforcements for the weaker camp have arrived at the sentry post.
				sendMsgByRace(1401949, Race.ELYOS, 0);
				// 较弱阵营的增援已抵达哨所。 / Reinforcements for the weaker camp have arrived at the sentry post.
				sendMsgByRace(1401950, Race.ASMODIANS, 0);
				sp(802023, 755.64215f, 545.90179f, 577.8269f, (byte) 0, 155);
				sp(802023, 337.73990f, 491.16772f, 597.2395f, (byte) 0, 156);
            }
        }, 220000));
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 起点出现了一名英雄。 / A hero has been spotted at the starting point.
				sendMsgByRace(1401967, Race.PC_ALL, 0);
				// 一名英雄及其增援已在起点出现。 / A hero and their reinforcements have been spotted at the starting point.
				sendMsgByRace(1401968, Race.PC_ALL, 10000);
				sp(701988, 313.6124f, 489.13992f, 597.13184f, (byte) 2, 0); //Rearguard Telekesis.
				sp(801957, 313.6124f, 489.13992f, 597.13184f, (byte) 2, 0); //Elyos Reinforcements Flag.
				sp(701989, 759.2739f, 569.3167f, 577.37885f, (byte) 87, 0); //Rearguard Freidr.
				sp(801958, 759.2739f, 569.3167f, 577.37885f, (byte) 87, 0); //Asmodians Reinforcements Flag.
            }
        }, 400000));
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 补给已送达部分哨所。 / Supplies have been delivered to some of the sentry posts.
				sendMsgByRace(1401965, Race.PC_ALL, 0);
				// 补给已投放在机密区域。 / Supplies have been dropped in a confidential area.
				sendMsgByRace(1402086, Race.PC_ALL, 10000);
				sp(701974, 322.18567f, 490.11285f, 596.1117f, (byte) 1, 0); //Supply Box.
				sp(701974, 758.0247f, 560.9797f, 576.9838f, (byte) 87, 0); //Supply Box.
				sp(701975, 574.02966f, 477.84848f, 620.6126f, (byte) 93, 10000); //Emergency Supply Box.
                sp(701975, 619.36755f, 515.6929f, 592.13336f, (byte) 55, 10000); //Emergency Supply Box.
                sp(701976, 582.56866f, 396.15695f, 603.4048f, (byte) 2, 10000); //Hidden Supply Box.
            }
        }, 600000));
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				switch (Rnd.get(1, 4)) {
                    case 1:
                        // 龙族袭击者已出现在部分哨所。 / The Balaur raiders have appeared at some of the sentry posts.
						sendMsgByRace(1401966, Race.PC_ALL, 0);
						// 龙族已抵达北接近哨所。 / The Balaur have arrived at the Northern Approach Post.
						sendMsgByRace(1402071, Race.PC_ALL, 5000);
						// 龙族正在攻击北接近哨所。 / The Balaur are attacking the Northern Approach Post.
						sendMsgByRace(1402073, Race.PC_ALL, 10000);
						sp(233491, 532.19055f, 445.263f, 620.25f, (byte) 105, 0); //Captain Avran.
						sp(801956, 532.19055f, 445.263f, 620.25f, (byte) 105, 0); //Assault Team Commander Flag.
                    break;
					case 2:
                        // 龙族袭击者已出现在部分哨所。 / The Balaur raiders have appeared at some of the sentry posts.
						sendMsgByRace(1401966, Race.PC_ALL, 0);
						// 龙族已抵达南接近哨所。 / The Balaur have arrived at the Southern Approach Post.
						sendMsgByRace(1402066, Race.PC_ALL, 5000);
						// 南接近哨所正遭受龙族攻击。 / The Southern Approach Post is under attack by the Balaur.
						sendMsgByRace(1402068, Race.PC_ALL, 10000);
						sp(233491, 620.5344f, 562.1826f, 590.91034f, (byte) 81, 0); //Captain Avran.
						sp(801956, 620.5344f, 562.1826f, 590.91034f, (byte) 81, 0); //Assault Team Commander Flag.
                    break;
					case 3:
                        // 龙族袭击者已出现在部分哨所。 / The Balaur raiders have appeared at some of the sentry posts.
						sendMsgByRace(1401966, Race.PC_ALL, 0);
						// 龙族已抵达防御哨所。 / The Balaur have arrived at the Defense Post.
						sendMsgByRace(1402056, Race.PC_ALL, 5000);
						// 防御哨所正遭受龙族攻击。 / The Defense Post is under attack by the Balaur.
						sendMsgByRace(1402058, Race.PC_ALL, 10000);
						sp(233491, 688.96906f, 484.00226f, 599.91016f, (byte) 94, 0); //Captain Avran.
						sp(801956, 688.96906f, 484.00226f, 599.91016f, (byte) 94, 0); //Assault Team Commander Flag.
                    break;
					case 4:
                        // 龙族袭击者已出现在部分哨所。 / The Balaur raiders have appeared at some of the sentry posts.
						sendMsgByRace(1401966, Race.PC_ALL, 0);
						// 龙族已抵达守卫哨所。 / The Balaur have arrived at the Guard Post.
						sendMsgByRace(1402061, Race.PC_ALL, 5000);
						// 守卫哨所正遭受龙族攻击。 / The Guard Post is under attack by the Balaur.
						sendMsgByRace(1402063, Race.PC_ALL, 10000);
						sp(233491, 499.92856f, 520.9595f, 597.6485f, (byte) 20, 0); //Captain Avran.
						sp(801956, 499.92856f, 520.9595f, 597.6485f, (byte) 20, 0); //Assault Team Commander Flag.
                    break;
                }
            }
        }, 900000));
		ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!engulfedOphidanBridgeReward.isRewarded()) {
					Race winnerRace = engulfedOphidanBridgeReward.getWinnerRaceByScore();
					stopInstance(winnerRace);
				}
            }
        }, 1800000));
    }
	
    protected void stopInstance(Race race) {
        stopInstanceTask();
        engulfedOphidanBridgeReward.setWinnerRace(race);
        engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        reward();
        engulfedOphidanBridgeReward.sendPacket(5, null);
    }
	
    /**
     * 玩家进入副本时处理。
     * Handle a player entering the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onEnterInstance(final Player player) {
        if (!containPlayer(player.getObjectId())) {
            engulfedOphidanBridgeReward.regPlayerReward(player);
        }
        sendEnterPacket(player);
    }
	
    private void sendEnterPacket(final Player player) {
    	instance.doOnAllPlayers(new Visitor<Player>() {
            /**
             * 处理 visit。
             * Handle visit.
             *
             * opponent
             */
            @Override
            public void visit(Player opponent) {
                if (player.getRace() != opponent.getRace()) {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), player.getObjectId()));
                    PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), opponent.getObjectId()));
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(),  player.getObjectId()));
                } else {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), opponent.getObjectId()));
                    if (player.getObjectId() != opponent.getObjectId()) {
                        PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), player.getObjectId(), 20, 0));
                    }
                }
            }
        });
    	sendPacket(true);
    	sendPacket(false);
        PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(4, getTime(), getInstanceReward(), player.getObjectId(), 20, 0));
    }
	
    private void startInstancePacket() {
    	instance.doOnAllPlayers(new Visitor<Player>() {
            /**
             * 处理 visit。
             * Handle visit.
             *
             * @param player 玩家 / player
             */
            @Override
            public void visit(Player player) {
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(3, getTime(), engulfedOphidanBridgeReward, player.getObjectId(), 0, 0));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), player.getObjectId()));
            }
        });
    }
	
    private void sendPacket(boolean isObjects) {
    	if (isObjects) {
    		instance.doOnAllPlayers(new Visitor<Player>() {
                /**
                 * 处理 visit。
                 * Handle visit.
                 *
                 * @param player 玩家 / player
                 */
                @Override
                public void visit(Player player) {
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
                }
            });
    	} else {
    		instance.doOnAllPlayers(new Visitor<Player>() {
                /**
                 * 处理 visit。
                 * Handle visit.
                 *
                 * @param player 玩家 / player
                 */
                @Override
                public void visit(Player player) {
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
                }
            });
    	}
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
        engulfedOphidanBridgeReward = new EngulfedOphidanBridgeReward(mapId, instanceId, instance);
        engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        doors = instance.getDoors();
        startInstanceTask();
    }
	
	protected void reward() {
        int elyosPoints = getPointsByRace(Race.ELYOS).intValue();
        int asmodianPoints = getPointsByRace(Race.ASMODIANS).intValue();
        int minimumTeamSize = (int) Math.min(
                engulfedOphidanBridgeReward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ELYOS).count(),
                engulfedOphidanBridgeReward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ASMODIANS).count());
        long endedAt = System.currentTimeMillis();
        for (Player player : instance.getPlayersInside()) {
            if (PlayerActions.isAlreadyDead(player)) {
				PlayerReviveService.duelRevive(player);
			}
			EngulfedOphidanBridgePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
            int teamScore = player.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
            int opposingScore = player.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
            BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
            double bonusRate = InstanceSettlementService.battlegroundBonusRate(
                    playerReward.calculateParticipation(instanceTime, endedAt), teamScore, opposingScore);
            RewardPlan base = InstanceSettlementService.battlegroundPlan(instance, result, 0, teamScore, 0,
                    minimumTeamSize);
            RewardPlan total = InstanceSettlementService.battlegroundPlan(instance, result, bonusRate, teamScore, 0,
                    minimumTeamSize);
            InstanceSettlementService.applyBattlegroundDisplay(playerReward, base, total);
            InstanceSettlementService.settleBattleground(instance, player, result, bonusRate, teamScore, 0,
                    minimumTeamSize);
        }
        for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player : instance.getPlayersInside()) {
						onExitInstance(player);
					}
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
				}
			}
		}, 60000);
    }
	
    private int getTime() {
        long result = System.currentTimeMillis() - instanceTime;
        if (result < 90000) {
            return (int) (90000 - result);
        } else if (result < 1800000) { //30-Mins
            return (int) (1800000 - (result - 90000));
        }
        return 0;
    }
	
    /**
     * 处理玩家复活事件。
     * Handle a player revive event.
     *
     * 玩家 / player
     * result
     */
    @Override
    public boolean onReviveEvent(Player player) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PlayerReviveService.revive(player, 100, 100, false, 0);
        player.getGameStats().updateStatsAndSpeedVisually();
        engulfedOphidanBridgeReward.portToPosition(player);
        return true;
    }
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * 玩家 / player
     * @param lastAttacker 最后攻击者 / last attacker
     * result
     */
    @Override
    public boolean onDie(Player player, Creature lastAttacker) {
		EngulfedOphidanBridgePlayerReward ownerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player);
        int points = 60;
        if (lastAttacker instanceof Player) {
            if (lastAttacker.getRace() != player.getRace()) {
                InstancePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
				if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
                    points *= loosingGroupMultiplier;
                } else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
                    points = 0;
                }
                updateScore((Player) lastAttacker, player, points, true);
            }
        }
        updateScore(player, player, -points, false);
        return true;
    }
	
	private MutableInt getPvpKillsByRace(Race race) {
        return engulfedOphidanBridgeReward.getPvpKillsByRace(race);
    }
	
    private MutableInt getPointsByRace(Race race) {
        return engulfedOphidanBridgeReward.getPointsByRace(race);
    }
	
    private void addPointsByRace(Race race, int points) {
        engulfedOphidanBridgeReward.addPointsByRace(race, points);
    }
	
    private void addPvpKillsByRace(Race race, int points) {
        engulfedOphidanBridgeReward.addPvpKillsByRace(race, points);
    }
	
    private void addPointToPlayer(Player player, int points) {
        engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId()).addPoints(points);
    }
	
    private void addPvPKillToPlayer(Player player) {
        engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId()).addPvPKillToPlayer();
    }
	
    protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
        if (points == 0) {
            return;
        }
        addPointsByRace(player.getRace(), points);
        List<Player> playersToGainScore = new ArrayList<Player>();
        if (target != null && player.isInGroup2()) {
            for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
                if (member.getLifeStats().isAlreadyDead()) {
                    continue;
                } if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
                    playersToGainScore.add(member);
                }
            }
        } else {
            playersToGainScore.add(player);
        }
        for (Player playerToGainScore : playersToGainScore) {
            addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
            if (target instanceof Npc) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
            } else if (target instanceof Player) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
            }
        }
        int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
        if (pointDifference < 0) {
            pointDifference *= -1;
        } if (pointDifference >= 3000) {
            loosingGroupMultiplier = 10;
        } else if (pointDifference >= 1000) {
            loosingGroupMultiplier = 1.5f;
        } else {
            loosingGroupMultiplier = 1;
        } if (pvpKill && points > 0) {
            addPvpKillsByRace(player.getRace(), 1);
            addPvPKillToPlayer(player);
        }
        engulfedOphidanBridgeReward.sendPacket(11, player.getObjectId());
        if (engulfedOphidanBridgeReward.hasCapPoints()) {
            stopInstance(engulfedOphidanBridgeReward.getWinnerRaceByScore());
        }
    }
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ENGULFED_OPHIDAN_BRIDGE_CHOKEPOINT_DEFENSE_POST_301210000")) {
            powerGenerator = 1;
	    } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ENGULFED_OPHIDAN_BRIDGE_NORTHERN_APPROACH_POST_301210000")) {
			powerGenerator = 2;
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ENGULFED_OPHIDAN_BRIDGE_SOUTHERN_APPROACH_POST_301210000")) {
			powerGenerator = 3;
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ENGULFED_OPHIDAN_BRIDGE_BRIDGEWATCH_POST_301210000")) {
			powerGenerator = 4;
		}
    }
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
	public void onDie(Npc npc) {
        int point = 0;
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 233473: //Beritra's Sentinel.
			case 233856: //Beritra Barricade.
				point = 100;
				despawnNpc(npc);
			break;
			case 233474: //Defense Post Magus.
			case 233475: //Defense Post Combatant.
			case 233476: //Defense Post Scout.	
			case 233478: //Northern Approach Post Magus.
			case 233479: //Northern Approach Post Combatant.
			case 233481: //Southern Approach Post Combatant.
			case 233482: //Southern Approach Post Scout.
			case 233484: //Guard Post Magus.
			case 233485: //Guard Post Combatant.
			case 233486: //Guard Post Scout.
			    point = 200;
				despawnNpc(npc);
			break;
			case 233477: //Defense Post Rearguard.
			case 233480: //Northern Approach Post Magician.
			case 233483: //Southern Approach Post Assaulter.
			case 233487: //Guard Post Rearguard.
			    point = 300;
				despawnNpc(npc);
			break;
			case 233846: //圣骑士后卫。 / Templar Rearguard.
			case 233847: //牧师后卫。 / Cleric Rearguard.
			case 233848: //巫师后卫。 / Sorcerer Rearguard.
			case 233849: //圣骑士后卫。 / Templar Rearguard.
			case 233850: //牧师后卫。 / Cleric Rearguard.
			case 233851: //巫师后卫。 / Sorcerer Rearguard.
			    point = 1500;
				despawnNpc(npc);
			break;
			case 233491: //Captain Avran.
			    point = 5000;
				despawnNpc(npc);
				deleteNpc(801956);
			break;
			case 701988: //Rearguard Telekesis.
			    despawnNpc(npc);
				// 天族主神已死亡。 / The Elyos Empyrean Lord has died.
				sendMsgByRace(1401959, Race.PC_ALL, 0);
			break;
			case 701989: //Rearguard Freidr.
			    despawnNpc(npc);
				// 魔族主神已死亡。 / The Asmodian Empyrean Lord has died.
				sendMsgByRace(1401960, Race.PC_ALL, 0);
			break;
			case 701943: //Elyos Power Generator.
				despawnNpc(npc);
				point = 5000;
				if (powerGenerator == 1) {
					if (race.equals(Race.ASMODIANS)) {
						deleteNpc(802033);
						deleteNpc(701969);
						// 魔族占领了守卫哨所。 / The Asmodians have captured the Guard Post.
						sendMsgByRace(1401991, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
					    sp(802034, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 0); //Chokepoint Defense Post Flag.
						sp(701944, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 173); //Asmodians Power Generator.
						sp(701969, 762.6721f, 544.30493f, 577.7007f, (byte) 91, 0); //Chokepoint Defense Post Mortar.
						sp(233849, 672.62286f, 467.2902f, 599.53894f, (byte) 107, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233849, 663.40594f, 483.60574f, 599.7871f, (byte) 37, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 660.30194f, 466.5498f, 599.8218f, (byte) 77, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 674.94977f, 478.36877f, 599.5594f, (byte) 8, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 2) {
					if (race.equals(Race.ASMODIANS)) {
						deleteNpc(802036);
						deleteNpc(701970);
						// 魔族占领了北接近哨所。 / The Asmodians have captured the Northern Approach Post.
						sendMsgByRace(1401992, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802037, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 0); //Northern Approach Post Flag.
						sp(701944, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 174); //Asmodians Power Generator.
						sp(701970, 760.40955f, 544.2923f, 577.7035f, (byte) 90, 0); //Northern Approach Post Mortar.
						sp(233849, 519.06854f, 434.295f, 620.125f, (byte) 45, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 533.65063f, 428.35898f, 620.25f, (byte) 5, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 525.0882f, 436.3445f, 620.25f, (byte) 27, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 3) {
					if (race.equals(Race.ASMODIANS)) {
						deleteNpc(802039);
						deleteNpc(701971);
						// 魔族占领了南接近哨所。 / The Asmodians have captured the Southern Approach Post.
						sendMsgByRace(1401993, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802040, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 0); //Southern Approach Post Flag.
						sp(701944, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 166); //Asmodians Power Generator.
						sp(701971, 750.75836f, 545.71686f, 577.7213f, (byte) 84, 0); //Southern Approach Post Mortar.
						sp(233849, 610.57794f, 559.381f, 590.625f, (byte) 5, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 593.646f, 556.11426f, 590.5221f, (byte) 58, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 607.1519f, 548.563f, 590.5f, (byte) 103, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 4) {
					if (race.equals(Race.ASMODIANS)) {
						deleteNpc(802042);
						deleteNpc(701972);
						// 魔族占领了防御哨所。 / The Asmodians have captured the Defense Post.
						sendMsgByRace(1401994, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802043, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 0); //Bridge Watchpost Flag.
						sp(701944, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 170); //Asmodians Power Generator.
						sp(701972, 748.57916f, 546.3481f, 577.72815f, (byte) 84, 0); //Bridge Watchpost Mortar.
						sp(233849, 483.3327f, 538.1493f, 597.5f, (byte) 58, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233849, 498.373f, 543.4837f, 597.5f, (byte) 9, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 500.20483f, 532.2458f, 597.5f, (byte) 116, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 484.61765f, 531.6245f, 597.375f, (byte) 70, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				}
			break;
			case 701944: //Asmodians Power Generator.
				point = 5000;
				despawnNpc(npc);
				if (powerGenerator == 1) {
					if (race.equals(Race.ELYOS)) {
						deleteNpc(802034);
						deleteNpc(701969);
						// 天族占领了守卫哨所。 / The Elyos have captured the Guard Post.
						sendMsgByRace(1401961, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
					    sp(802033, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 0); //Chokepoint Defense Post Flag.
						sp(701943, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 172); //Elyos Power Generator.
						sp(701969, 337.6665f, 498.31458f, 597.0435f, (byte) 3, 0); //Chokepoint Defense Post Mortar.
						sp(233846, 672.62286f, 467.2902f, 599.53894f, (byte) 107, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233846, 663.40594f, 483.60574f, 599.7871f, (byte) 37, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 660.30194f, 466.5498f, 599.8218f, (byte) 77, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 674.94977f, 478.36877f, 599.5594f, (byte) 8, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 2) {
					if (race.equals(Race.ELYOS)) {
						deleteNpc(802037);
						deleteNpc(701970);
						// 天族占领了北接近哨所。 / The Elyos have captured the Northern Approach Post.
						sendMsgByRace(1401962, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802036, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 0); //Northern Approach Post Flag.
						sp(701943, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 175); //Elyos Power Generator.
						sp(701970, 338.08813f, 496.11847f, 597.04626f, (byte) 3, 0); //Northern Approach Post Mortar.
						sp(233846, 519.06854f, 434.295f, 620.125f, (byte) 45, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 533.65063f, 428.35898f, 620.25f, (byte) 5, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 525.0882f, 436.3445f, 620.25f, (byte) 27, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 3) {
					if (race.equals(Race.ELYOS)) {
						deleteNpc(802040);
						deleteNpc(701971);
						// 天族占领了南接近哨所。 / The Elyos have captured the Southern Approach Post.
						sendMsgByRace(1401963, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802039, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 0); //Southern Approach Post Flag.
						sp(701943, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 169); //Elyos Power Generator.
						sp(701971, 338.6412f, 486.42004f, 597.0637f, (byte) 118, 0); //Southern Approach Post Mortar.
						sp(233846, 610.57794f, 559.381f, 590.625f, (byte) 5, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 593.646f, 556.11426f, 590.5221f, (byte) 58, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 607.1519f, 548.563f, 590.5f, (byte) 103, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 4) {
					if (race.equals(Race.ELYOS)) {
						deleteNpc(802043);
						deleteNpc(701972);
						// 天族占领了防御哨所。 / The Elyos have captured the Defense Post.
						sendMsgByRace(1401964, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802042, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 0); //Bridge Watchpost Flag.
						sp(701943, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 171); //Elyos Power Generator.
						sp(701972, 338.46423f, 484.23608f, 597.07074f, (byte) 118, 0); //Bridge Watchpost Mortar.
						sp(233846, 483.3327f, 538.1493f, 597.5f, (byte) 58, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233846, 498.373f, 543.4837f, 597.5f, (byte) 9, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 500.20483f, 532.2458f, 597.5f, (byte) 116, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 484.61765f, 531.6245f, 597.375f, (byte) 70, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				}
			break;
			case 701945: //Balaur Power Generator.
				point = 5000;
				despawnNpc(npc);
				if (powerGenerator == 1) {
					if (race.equals(Race.ELYOS)) {
					    deleteNpc(802035);
						// *龙族* / *Balaur*//
						deleteNpc(233484); //Guard Post Magus.
						deleteNpc(233485); //Guard Post Combatant.
						deleteNpc(233486); //Guard Post Scout.
						deleteNpc(233487); //Guard Post Rearguard.
						// 天族占领了守卫哨所。 / The Elyos have captured the Guard Post.
						sendMsgByRace(1401961, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802033, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 0); //Chokepoint Defense Post Flag.
						sp(701943, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 172); //Elyos Power Generator.
						sp(701969, 337.6665f, 498.31458f, 597.0435f, (byte) 3, 0); //Chokepoint Defense Post Mortar.
						sp(233846, 672.62286f, 467.2902f, 599.53894f, (byte) 107, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233846, 663.40594f, 483.60574f, 599.7871f, (byte) 37, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 660.30194f, 466.5498f, 599.8218f, (byte) 77, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 674.94977f, 478.36877f, 599.5594f, (byte) 8, 0); //巫师后卫。 / Sorcerer Rearguard.
					} else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(802035);
						// *龙族* / *Balaur*//
						deleteNpc(233484); //Guard Post Magus.
						deleteNpc(233485); //Guard Post Combatant.
						deleteNpc(233486); //Guard Post Scout.
						deleteNpc(233487); //Guard Post Rearguard.
						// 魔族占领了守卫哨所。 / The Asmodians have captured the Guard Post.
						sendMsgByRace(1401991, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
					    sp(802034, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 0); //Chokepoint Defense Post Flag.
						sp(701944, 667.11389f, 474.22995f, 600.48346f, (byte) 0, 173); //Asmodians Power Generator.
						sp(701969, 762.6721f, 544.30493f, 577.7007f, (byte) 91, 0); //Chokepoint Defense Post Mortar.
						sp(233849, 672.62286f, 467.2902f, 599.53894f, (byte) 107, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233849, 663.40594f, 483.60574f, 599.7871f, (byte) 37, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 660.30194f, 466.5498f, 599.8218f, (byte) 77, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 674.94977f, 478.36877f, 599.5594f, (byte) 8, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 2) {
					if (race.equals(Race.ELYOS)) {
					    deleteNpc(802038);
						// *龙族* / *Balaur*//
						deleteNpc(233478); //Northern Approach Post Magus.
						deleteNpc(233479); //Northern Approach Post Combatant.
						deleteNpc(233480); //Northern Approach Post Magician.
						// 天族占领了北接近哨所。 / The Elyos have captured the Northern Approach Post.
						sendMsgByRace(1401962, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802036, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 0); //Northern Approach Post Flag.
						sp(701943, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 175); //Elyos Power Generator.
						sp(701970, 338.08813f, 496.11847f, 597.04626f, (byte) 3, 0); //Northern Approach Post Mortar.
						sp(233846, 519.06854f, 434.295f, 620.125f, (byte) 45, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 533.65063f, 428.35898f, 620.25f, (byte) 5, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 525.0882f, 436.3445f, 620.25f, (byte) 27, 0); //巫师后卫。 / Sorcerer Rearguard.
					} else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(802038);
						// *龙族* / *Balaur*//
						deleteNpc(233478); //Northern Approach Post Magus.
						deleteNpc(233479); //Northern Approach Post Combatant.
						deleteNpc(233480); //Northern Approach Post Magician.
						// 魔族占领了北接近哨所。 / The Asmodians have captured the Northern Approach Post.
						sendMsgByRace(1401992, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802037, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 0); //Northern Approach Post Flag.
						sp(701944, 524.84589f, 427.63959f, 621.21320f, (byte) 0, 174); //Asmodians Power Generator.
						sp(701970, 760.40955f, 544.2923f, 577.7035f, (byte) 90, 0); //Northern Approach Post Mortar.
						sp(233849, 519.06854f, 434.295f, 620.125f, (byte) 45, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 533.65063f, 428.35898f, 620.25f, (byte) 5, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 525.0882f, 436.3445f, 620.25f, (byte) 27, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 3) {
					if (race.equals(Race.ELYOS)) {
					    deleteNpc(802041);
						// *龙族* / *Balaur*//
						deleteNpc(233481); //Southern Approach Post Combatant.
						deleteNpc(233482); //Southern Approach Post Scout.
						deleteNpc(233483); //Southern Approach Post Assaulter.
						// 天族占领了南接近哨所。 / The Elyos have captured the Southern Approach Post.
						sendMsgByRace(1401963, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802039, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 0); //Southern Approach Post Flag.
						sp(701943, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 169); //Elyos Power Generator.
						sp(701971, 338.6412f, 486.42004f, 597.0637f, (byte) 118, 0); //Southern Approach Post Mortar.
						sp(233846, 610.57794f, 559.381f, 590.625f, (byte) 5, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 593.646f, 556.11426f, 590.5221f, (byte) 58, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 607.1519f, 548.563f, 590.5f, (byte) 103, 0); //巫师后卫。 / Sorcerer Rearguard.
					} else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(802041);
						// *龙族* / *Balaur*//
						deleteNpc(233481); //Southern Approach Post Combatant.
						deleteNpc(233482); //Southern Approach Post Scout.
						deleteNpc(233483); //Southern Approach Post Assaulter.
						// 魔族占领了南接近哨所。 / The Asmodians have captured the Southern Approach Post.
						sendMsgByRace(1401993, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802040, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 0); //Southern Approach Post Flag.
						sp(701944, 602.73395f, 556.29407f, 591.52533f, (byte) 0, 166); //Asmodians Power Generator.
						sp(701971, 750.75836f, 545.71686f, 577.7213f, (byte) 84, 0); //Southern Approach Post Mortar.
						sp(233849, 610.57794f, 559.381f, 590.625f, (byte) 5, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 593.646f, 556.11426f, 590.5221f, (byte) 58, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 607.1519f, 548.563f, 590.5f, (byte) 103, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				} else if (powerGenerator == 4) {
					if (race.equals(Race.ELYOS)) {
					    deleteNpc(802044);
						// *龙族* / *Balaur*//
						deleteNpc(233474); //Defense Post Magus.
						deleteNpc(233475); //Defense Post Combatant.
						deleteNpc(233476); //Defense Post Scout.
						deleteNpc(233477); //Defense Post Rearguard.
						// 天族占领了防御哨所。 / The Elyos have captured the Defense Post.
						sendMsgByRace(1401964, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ELYOS, 5000);
						sp(802042, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 0); //Bridge Watchpost Flag.
						sp(701943, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 171); //Elyos Power Generator.
						sp(701972, 338.46423f, 484.23608f, 597.07074f, (byte) 118, 0); //Bridge Watchpost Mortar.
						sp(233846, 483.3327f, 538.1493f, 597.5f, (byte) 58, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233846, 498.373f, 543.4837f, 597.5f, (byte) 9, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233847, 500.20483f, 532.2458f, 597.5f, (byte) 116, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233848, 484.61765f, 531.6245f, 597.375f, (byte) 70, 0); //巫师后卫。 / Sorcerer Rearguard.
					} else if (race.equals(Race.ASMODIANS)) {
					    deleteNpc(802044);
						// *龙族* / *Balaur*//
						deleteNpc(233474); //Defense Post Magus.
						deleteNpc(233475); //Defense Post Combatant.
						deleteNpc(233476); //Defense Post Scout.
						deleteNpc(233477); //Defense Post Rearguard.
						// 魔族占领了防御哨所。 / The Asmodians have captured the Defense Post.
						sendMsgByRace(1401994, Race.PC_ALL, 0);
						//你从能量发生器获得了额外点数。 / You have obtained extra points from the power generator.
						sendMsgByRace(1401957, Race.ASMODIANS, 5000);
						sp(802043, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 0); //Bridge Watchpost Flag.
						sp(701944, 492.81982f, 536.56732f, 598.24933f, (byte) 0, 170); //Asmodians Power Generator.
						sp(701972, 748.57916f, 546.3481f, 577.72815f, (byte) 84, 0); //Bridge Watchpost Mortar.
						sp(233849, 483.3327f, 538.1493f, 597.5f, (byte) 58, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233849, 498.373f, 543.4837f, 597.5f, (byte) 9, 0); //圣骑士后卫。 / Templar Rearguard.
						sp(233850, 500.20483f, 532.2458f, 597.5f, (byte) 116, 0); //牧师后卫。 / Cleric Rearguard.
						sp(233851, 484.61765f, 531.6245f, 597.375f, (byte) 70, 0); //巫师后卫。 / Sorcerer Rearguard.
				    }
				}
			break;
        }
        updateScore(mostPlayerDamage, npc, point, false);
    }
	
    /**
     * 玩家对 NPC 使用物品完成时处理。
     * Handle item-use finish on an NPC.
     *
     * 玩家 / player
     * npc
     */
    @Override
    public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
		    case 701947: //Elyos Field Gun.
			case 701949: //Elyos Field Gun.
                if (player.getInventory().decreaseByItemId(164000277, 1)) { //Power Breaker.
				    // 你已使用一个破力装置。 / You've used one Power Breaker.
					sendMsgByRace(1402010,  Race.PC_ALL, 1000);
					GameEngineServices.skillEngine().getSkill(player, 21065, 1, player).useNoAnimationSkill();
			    } else {
					// 你需要破力装置。 / You need a Power Breaker.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402006));
				}
            break;
			case 701948: //Asmodians Field Gun.
			case 701950: //Asmodians Field Gun.
                if (player.getInventory().decreaseByItemId(164000277, 1)) { //Power Breaker.
				    // 你已使用一个破力装置。 / You've used one Power Breaker.
					sendMsgByRace(1402010,  Race.PC_ALL, 1000);
					GameEngineServices.skillEngine().getSkill(player, 21066, 1, player).useNoAnimationSkill();
			    } else {
					// 你需要破力装置。 / You need a Power Breaker.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402006));
				}
            break;
			case 701969: //Chokepoint Defense Post Mortar.
                if (player.getInventory().decreaseByItemId(164000278, 1)) { //Bombing Device Activation Key.
					// 攻城基地已启动轰炸。\n 轰炸即将开始。 / Bombardment has been activated on the siege base.\nBombing will begin soon.
					sendMsgByRace(1402110,  Race.PC_ALL, 0);
					// 你已使用一把奥菲丹轰炸装置激活钥匙。 / You've used one Ophidan Bombing Device Activation Key.
					sendMsgByRace(1402009,  Race.PC_ALL, 1000);
                    sp(855240, 659.4056f, 464.89233f, 599.9122f, (byte) 21, 2000);
					sp(855240, 666.66907f, 462.55884f, 599.7151f, (byte) 31, 2500);
					sp(855240, 675.36145f, 465.22815f, 599.625f, (byte) 47, 3000);
					sp(855240, 679.05774f, 473.21796f, 599.6911f, (byte) 60, 3500);
					sp(855240, 677.78613f, 480.65442f, 599.625f, (byte) 72, 4000);
					sp(855240, 669.91797f, 486.01047f, 599.75f, (byte) 88, 4500);
					sp(855240, 662.33215f, 486.08054f, 599.98425f, (byte) 96, 5000);
			    } else {
					// 你需要奥菲丹轰炸装置激活钥匙。 / You need an Ophidan Bombing Device Activation Key.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402005));
				}
            break;
			case 701970: //Northern Approach Post Mortar.
                if (player.getInventory().decreaseByItemId(164000278, 1)) { //Bombing Device Activation Key.
					// 攻城基地已启动轰炸。\n 轰炸即将开始。 / Bombardment has been activated on the siege base.\nBombing will begin soon.
					sendMsgByRace(1402110,  Race.PC_ALL, 0);
					// 你已使用一把奥菲丹轰炸装置激活钥匙。 / You've used one Ophidan Bombing Device Activation Key.
					sendMsgByRace(1402009,  Race.PC_ALL, 1000);
					sp(855240, 529.0096f, 417.22366f, 620.125f, (byte) 43, 2000);
					sp(855240, 533.75183f, 421.21304f, 620.2008f, (byte) 48, 2500);
					sp(855240, 535.80133f, 429.1748f, 620.25f, (byte) 66, 3000);
					sp(855240, 531.301f, 436.3631f, 620.25f, (byte) 76, 3500);
					sp(855240, 525.2899f, 438.66245f, 620.25f, (byte) 88, 4000);
					sp(855240, 516.44604f, 436.7846f, 620.125f, (byte) 102, 4500);
					sp(855240, 512.9798f, 429.75674f, 620.25f, (byte) 116, 5000);
			    } else {
					// 你需要奥菲丹轰炸装置激活钥匙。 / You need an Ophidan Bombing Device Activation Key.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402005));
				}
            break;
			case 701971: //Southern Approach Post Mortar.
                if (player.getInventory().decreaseByItemId(164000278, 1)) { //Bombing Device Activation Key.
					// 攻城基地已启动轰炸。\n 轰炸即将开始。 / Bombardment has been activated on the siege base.\nBombing will begin soon.
					sendMsgByRace(1402110,  Race.PC_ALL, 0);
					// 你已使用一把奥菲丹轰炸装置激活钥匙。 / You've used one Ophidan Bombing Device Activation Key.
					sendMsgByRace(1402009,  Race.PC_ALL, 1000);
					sp(855240, 613.2318f, 552.8324f, 590.625f, (byte) 55, 2000);
					sp(855240, 612.37695f, 559.9156f, 590.625f, (byte) 67, 2500);
					sp(855240, 606.91644f, 565.8719f, 590.5f, (byte) 84, 3000);
					sp(855240, 599.67896f, 566.28455f, 590.8712f, (byte) 96, 3500);
					sp(855240, 594.0308f, 563.2582f, 590.5786f, (byte) 103, 4000);
					sp(855240, 591.802f, 555.8142f, 590.625f, (byte) 2, 4500);
					sp(855240, 594.1872f, 549.05316f, 590.625f, (byte) 14, 5000);
					sp(855240, 600.87866f, 545.5543f, 590.52783f, (byte) 27, 5500);
					sp(855240, 609.32367f, 547.27893f, 590.54504f, (byte) 44, 6000);
			    } else {
					// 你需要奥菲丹轰炸装置激活钥匙。 / You need an Ophidan Bombing Device Activation Key.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402005));
				}
            break;
			case 701972: //Bridge Watchpost Mortar.
                if (player.getInventory().decreaseByItemId(164000278, 1)) { //Bombing Device Activation Key.
					// 攻城基地已启动轰炸。\n 轰炸即将开始。 / Bombardment has been activated on the siege base.\nBombing will begin soon.
					sendMsgByRace(1402110,  Race.PC_ALL, 0);
					// 你已使用一把奥菲丹轰炸装置激活钥匙。 / You've used one Ophidan Bombing Device Activation Key.
					sendMsgByRace(1402009,  Race.PC_ALL, 1000);
					sp(855240, 495.5121f, 527.32605f, 597.5f, (byte) 37, 2000);
					sp(855240, 502.44363f, 531.56396f, 597.5f, (byte) 52, 2500);
					sp(855240, 502.87552f, 538.83093f, 597.5f, (byte) 65, 3000);
					sp(855240, 499.54196f, 544.8168f, 597.5f, (byte) 80, 3500);
					sp(855240, 491.6645f, 546.2845f, 597.5f, (byte) 93, 4000);
					sp(855240, 485.03534f, 543.95984f, 597.5f, (byte) 107, 4500);
					sp(855240, 481.37946f, 538.3027f, 597.4801f, (byte) 118, 5000);
					sp(855240, 483.40137f, 530.04224f, 597.375f, (byte) 13, 5500);
					sp(855240, 489.6f, 525.62933f, 597.475f, (byte) 25, 6000);
			    } else {
					// 你需要奥菲丹轰炸装置激活钥匙。 / You need an Ophidan Bombing Device Activation Key.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402005));
				}
            break;
        }
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
    /**
     * 副本销毁时清理资源。
     * Clean up resources when the instance is destroyed.
     */
    @Override
    public void onInstanceDestroy() {
        engulfedOphidanBridgeReward.clear();
        isInstanceDestroyed = true;
        stopInstanceTask();
        doors.clear();
    }
	
    protected void openFirstDoors() {
        openDoor(176);
		openDoor(177);
    }
	
    protected void openDoor(int doorId) {
        StaticDoor door = doors.get(doorId);
        if (door != null) {
            door.setOpen(true);
        }
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
    }
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        ophidanTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
    private void stopInstanceTask() {
        for (Future<?> task : ophidanTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	
    /**
     * 返回本副本奖励对象。
     * Return this instance's reward object.
     *
     * result
     */
    @Override
    public InstanceReward<?> getInstanceReward() {
        return engulfedOphidanBridgeReward;
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
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
		EngulfedOphidanBridgePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
		playerReward.endBoostMoraleEffect(player);
		removeItems(player);
    }
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		EngulfedOphidanBridgePlayerReward reward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
		if (reward != null) {
			reward.updateLogOutTime();
		}
		removeItems(player);
	}
	
    /**
     * 玩家登录到该副本时处理。
     * Handle a player logging into this instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onPlayerLogin(Player player) {
        EngulfedOphidanBridgePlayerReward reward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
        if (reward != null) {
            reward.updateBonusTime();
        }
        engulfedOphidanBridgeReward.sendPacket(10, player.getObjectId());
    }
}
