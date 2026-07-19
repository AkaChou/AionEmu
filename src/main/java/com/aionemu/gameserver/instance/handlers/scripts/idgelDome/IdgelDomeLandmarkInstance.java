package com.aionemu.gameserver.instance.handlers.scripts.idgelDome;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

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
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.LandMarkReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.LandMarkPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 伊吉尔穹顶地标副本事件处理器。
 * Instance event handler for Idgel Dome Landmark.
 *
 * @author Encom
 */

@InstanceID(301680000)
public class IdgelDomeLandmarkInstance extends GeneralInstanceHandler
{
    /** 副本时间戳 / instance timestamp */
        private long instanceTime;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
    /** 地标奖励 / landmark reward */
        protected LandMarkReward landMarkReward;
    /** 败方倍率 / losing-group multiplier */
        private float loosingGroupMultiplier = 1;
    /** 副本是否已销毁 / whether the instance is destroyed */
    private boolean isInstanceDestroyed = false;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
    /** 副本是否已开始 / whether the instance started */
        protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
    /** 地标任务 / landmark task */
        private final List<Future<?>> landMarkTask = new ArrayList<Future<?>>();
	private boolean elyosTargetCompleted;
	private boolean asmodianTargetCompleted;
    /**
     * 返回玩家奖励记录。
     * Return the player's reward record.
     *
     * 玩家 / player
     * result
     */
    
    protected LandMarkPlayerReward getPlayerReward(Player player) {
        landMarkReward.regPlayerReward(player);
        return (LandMarkPlayerReward) landMarkReward.getPlayerReward(player.getObjectId());
    }
	
    private boolean containPlayer(Integer object) {
        return landMarkReward.containPlayer(object);
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
		int index = dropItems.size() + 1;
        switch (npcId) {
            case 834168: //Bomb Support Box.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000413, 1)); //Support Bomb.
			break;
			case 834169: //Bomb Restraint Support Box.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000414, 1)); //Support Restraining Bomb.
			break;
        }
    }
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000413, storage.getItemCountByItemId(164000413)); //Support Bomb.
		storage.decreaseByItemId(164000414, storage.getItemCountByItemId(164000414)); //Support Restraining Bomb.
	}
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
    protected void startInstanceTask() {
    	instanceTime = System.currentTimeMillis();
        landMarkReward.setInstanceStartTime();
		landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!landMarkReward.isRewarded()) {
				    openFirstDoors();
					spawn(833898, 264.65891f, 259.27396f, 88.502739f, (byte) 0, 60); //Sealed Reian Relic.
					spawn(806303, 249.47313f, 172.33987f, 79.688995f, (byte) 0, 198); //Central Square Teleport.
					spawn(806304, 279.98080f, 346.39691f, 79.695137f, (byte) 0, 197); //Central Square Teleport.
				    // 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
				    sendMsgByRace(1401181, Race.PC_ALL, 5000);
					// 你需要激活奥德供应装置。 / You need to activate the Aether Supply Device.
					sendMsgByRace(1403564, Race.PC_ALL, 10000);
                    landMarkReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
                    startInstancePacket();
                    landMarkReward.sendPacket(4, null);
				}
            }
        }, 90000));
		landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				sendPacket(false);
                landMarkReward.sendPacket(4, null);
                // 血战室出现了轰炸支援箱。 / A bomb support chest has appeared at the Blood War Room.
				sendMsgByRace(1403625, Race.ELYOS, 0);
				// 血战室出现了轰炸支援箱。 / A bomb support chest has appeared at the Blood War Room.
				sendMsgByRace(1403626, Race.ASMODIANS, 0);
				sp(834168, 252.9754f, 246.21234f, 92.94253f, (byte) 15, 0); //Bomb Support Box.
				sp(834169, 276.4865f, 271.9778f, 92.94253f, (byte) 75, 0); //Bomb Restraint Support Box.
            }
        }, 300000));
		landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
            	if (!landMarkReward.isRewarded()) {
					Race winnerRace = landMarkReward.getWinnerRaceByScore();
					stopInstance(winnerRace);
				}
            }
        }, 1200000));
    }
	/**
	 * 停止副本并结算。
	 * Stop the instance and settle.
	 *
	 * @param race 阵营 / race
	 */
	
    protected void stopInstance(Race race) {
        stopInstanceTask();
        landMarkReward.setWinnerRace(race);
        landMarkReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        reward();
        landMarkReward.sendPacket(5, null);
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
            landMarkReward.regPlayerReward(player);
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
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), landMarkReward, instance.getPlayersInside(), true));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(3, getTime(), landMarkReward, player.getObjectId(), 0, 0));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), landMarkReward, instance.getPlayersInside(), true));
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
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), landMarkReward, instance.getPlayersInside(), true));
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
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), landMarkReward, instance.getPlayersInside(), true));
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
        landMarkReward = new LandMarkReward(mapId, instanceId, instance);
        landMarkReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        doors = instance.getDoors();
        startInstanceTask();
    }
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	
	protected void reward() {
        int elyosPoints = getPointsByRace(Race.ELYOS).intValue();
        int asmodianPoints = getPointsByRace(Race.ASMODIANS).intValue();
        int minimumTeamSize = (int) Math.min(
                landMarkReward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ELYOS).count(),
                landMarkReward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ASMODIANS).count());
        long endedAt = System.currentTimeMillis();
        for (Player player : instance.getPlayersInside()) {
            if (PlayerActions.isAlreadyDead(player)) {
				PlayerReviveService.duelRevive(player);
			}
			LandMarkPlayerReward playerReward = landMarkReward.getPlayerReward(player.getObjectId());
            int teamScore = player.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
            int opposingScore = player.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
            int calculateMask = player.getRace() == Race.ELYOS && elyosTargetCompleted ? 1
                    : player.getRace() == Race.ASMODIANS && asmodianTargetCompleted ? 2 : 0;
            BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
            double bonusRate = InstanceSettlementService.battlegroundBonusRate(
                    playerReward.calculateParticipation(instanceTime, endedAt), teamScore, opposingScore);
            RewardPlan base = InstanceSettlementService.battlegroundPlan(instance, result, 0, teamScore,
                    calculateMask, minimumTeamSize);
            RewardPlan total = InstanceSettlementService.battlegroundPlan(instance, result, bonusRate, teamScore,
                    calculateMask, minimumTeamSize);
            InstanceSettlementService.applyBattlegroundDisplay(playerReward, base, total);
            InstanceSettlementService.settleBattleground(instance, player, result, bonusRate, teamScore,
                    calculateMask, minimumTeamSize);
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
        } else if (result < 1200000) { //20-Mins
            return (int) (1200000 - (result - 90000));
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
        landMarkReward.portToPosition(player);
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
		LandMarkPlayerReward ownerReward = landMarkReward.getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player);
        int points = 60;
        if (lastAttacker instanceof Player) {
            if (lastAttacker.getRace() != player.getRace()) {
                InstancePlayerReward playerReward = landMarkReward.getPlayerReward(player.getObjectId());
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
        return landMarkReward.getPvpKillsByRace(race);
    }
	
    private MutableInt getPointsByRace(Race race) {
        return landMarkReward.getPointsByRace(race);
    }
	
    private void addPointsByRace(Race race, int points) {
        landMarkReward.addPointsByRace(race, points);
    }
	
    private void addPvpKillsByRace(Race race, int points) {
        landMarkReward.addPvpKillsByRace(race, points);
    }
	
    private void addPointToPlayer(Player player, int points) {
        landMarkReward.getPlayerReward(player.getObjectId()).addPoints(points);
    }
	
    private void addPvPKillToPlayer(Player player) {
        landMarkReward.getPlayerReward(player.getObjectId()).addPvPKillToPlayer();
    }
	/**
	 * 处理 updateScore。
	 * Handle updateScore.
	 *
	 * 玩家 / player
	 * target
	 * points
	 * pvpKill
	 */
	
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
        landMarkReward.sendPacket(11, player.getObjectId());
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
        } switch (npc.getNpcId()) {
		    case 243965: //Rotten Clodworm.
			case 243966: //Rotten Mudthorn.
                point = 50;
				despawnNpc(npc);
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
		int point = 0;
		switch (npc.getNpcId()) {
			case 833898: //Sealed Reian Relic.
				point = 1000;
				despawnNpc(npc);
			break;
			/**
	 * Unsealing Device [Elyos]
	 */
			case 806343: //解封装置。 / Unsealing Device.
				point = 200;
				despawnNpc(npc);
				// 天族激活了装置第 1 阶段。 / The Elyos activated stage 1 of the device.
				sendMsgByRace(1403428, Race.PC_ALL, 0);
			break;
			case 806344: //解封装置。 / Unsealing Device.
			    point = 1000;
				despawnNpc(npc);
				// 天族激活了装置第 2 阶段。 / The Elyos activated stage 2 of the device.
				sendMsgByRace(1403429, Race.PC_ALL, 0);
			break;
			case 806345: //解封装置。 / Unsealing Device.
			    point = 500;
				despawnNpc(npc);
				// 天族激活了装置第 3 阶段。 / The Elyos activated stage 3 of the device.
				sendMsgByRace(1403430, Race.PC_ALL, 0);
			break;
			case 806346: //解封装置。 / Unsealing Device.
			    point = 50000;
				elyosTargetCompleted = true;
				despawnNpc(npc);
				// 天族正在激活装置的最后阶段。 / The Elyos are activating the last stage of the device.
				sendMsgByRace(1403431, Race.PC_ALL, 0);
				// 天族成功占领了此区域。 / The Elyos successfully occupied this area.
				sendMsgByRace(1403434, Race.PC_ALL, 10000);
			break;
			/**
	 * Unsealing Device [Asmodians]
	 */
			case 806375: //解封装置。 / Unsealing Device.
			    point = 200;
				despawnNpc(npc);
				// 魔族激活了装置第 1 阶段。 / The Asmodians activated stage 1 of the device.
				sendMsgByRace(1403435, Race.PC_ALL, 0);
			break;
			case 806376: //解封装置。 / Unsealing Device.
			    point = 1000;
				despawnNpc(npc);
				// 魔族激活了装置第 2 阶段。 / The Asmodians activated stage 2 of the device.
				sendMsgByRace(1403436, Race.PC_ALL, 0);
			break;
			case 806377: //解封装置。 / Unsealing Device.
			    point = 500;
				despawnNpc(npc);
				// 魔族激活了装置第 3 阶段。 / The Asmodians activated stage 3 of the device.
				sendMsgByRace(1403437, Race.PC_ALL, 0);
			break;
			case 806378: //解封装置。 / Unsealing Device.
			    point = 50000;
				asmodianTargetCompleted = true;
				despawnNpc(npc);
				// 魔族正在激活装置的最后阶段。 / The Asmodians are activating the last stage of the device.
				sendMsgByRace(1403438, Race.PC_ALL, 0);
				// 魔族成功占领了此区域。 / The Asmodians successfully occupied this area.
				sendMsgByRace(1403441, Race.PC_ALL, 10000);
			break;
			case 802192: //Flame Vent [Elyos].
			 // 魔族火焰喷口已激活。\n 魔族被困住了！ / The Asmodian Flame Vent has been activated.\nThe Asmodians are trapped!
				sendMsgByRace(1402368, Race.PC_ALL, 0);
				sp(702404, 234.43842f, 194.1041f, 79.23065f, (byte) 105, 0);
				sp(702405, 234.13383f, 194.39594f, 79.23065f, (byte) 105, 0);
                sp(702405, 234.62419f, 193.95747f, 79.23065f, (byte) 45, 0);
                sp(702405, 234.42247f, 194.1363f, 79.23065f, (byte) 16, 0);
                sp(702405, 234.53394f, 194.27177f, 79.23065f, (byte) 75, 0);
			break;
			case 802193: //Flame Vent [Asmodians]
			 // 天族火焰喷口已激活。\n 天族被困住了！ / The Elyos Flame Vent has been activated.\nThe Elyos are trapped!
				sendMsgByRace(1402369, Race.PC_ALL, 0);
				sp(702404, 294.57443f, 324.22205f, 79.23065f, (byte) 45, 0);
				sp(702405, 294.53418f, 324.0909f, 79.23065f, (byte) 105, 0);
                sp(702405, 294.66284f, 324.29172f, 79.23065f, (byte) 75, 0);
                sp(702405, 294.4634f, 323.84235f, 79.23065f, (byte) 15, 0);
                sp(702405, 294.70172f, 324.23065f, 79.23065f, (byte) 45, 0);
			break;
        }
		updateScore(player, npc, point, false);
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
        isInstanceDestroyed = true;
        landMarkReward.clear();
        stopInstanceTask();
        doors.clear();
    }
	/**
	 * 处理 openFirstDoors。
	 * Handle openFirstDoors.
	 */
	
    protected void openFirstDoors() {
        openDoor(180);
		openDoor(181);
    }
	/**
	 * 打开指定门。
	 * Open the given door.
	 *
	 * doorId
	 */
	
    protected void openDoor(int doorId) {
        StaticDoor door = doors.get(doorId);
        if (door != null) {
            door.setOpen(true);
        }
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * entity id
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * walkerId
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        landMarkTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        for (Future<?> task : landMarkTask) {
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
        return landMarkReward;
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
		LandMarkPlayerReward playerReward = landMarkReward.getPlayerReward(player.getObjectId());
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
		LandMarkPlayerReward reward = landMarkReward.getPlayerReward(player.getObjectId());
		if (reward != null) {
			reward.updateLogOutTime();
		}
		removeItems(player);
	}
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
	
    /**
     * 玩家登录到该副本时处理。
     * Handle a player logging into this instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onPlayerLogin(Player player) {
        LandMarkPlayerReward reward = landMarkReward.getPlayerReward(player.getObjectId());
        if (reward != null) {
            reward.updateBonusTime();
        }
        landMarkReward.sendPacket(10, player.getObjectId());
    }
}
