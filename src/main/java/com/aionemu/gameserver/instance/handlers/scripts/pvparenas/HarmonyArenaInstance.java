package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.HarmonyArenaReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 合作竞技场副本事件处理器。
 * Instance event handler for Harmony Arena.
 *
 * @author Encom
 */

public class HarmonyArenaInstance extends GeneralInstanceHandler
{
	/** 击杀奖励分 / kill bonus points */
	protected int killBonus = 1000;
	/** 死亡扣分 / death fine */
	protected int deathFine = -150;
	/** 副本奖励对象 / instance reward object */
	protected HarmonyArenaReward instanceReward;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed;
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
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
		if (!instanceReward.containPlayer(object)) {
			instanceReward.regPlayerReward(object);
			instanceReward.getPlayerReward(object).applyBoostMoraleEffect(player);
			instanceReward.setRndPosition(object);
		} else {
			instanceReward.portToPosition(player);
		}
		sendEnterPacket(player);
	}
	
	private void sendEnterPacket(final Player player) {
		final Integer object = player.getObjectId();
		final HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(object);
		if (group == null) {
			return;
		}
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * opponent
			 */
			@Override
			public void visit(Player opponent) {
				if (!group.containPlayer(opponent.getObjectId())) {
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), object));
					PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), opponent.getObjectId()));
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), object));
				} else {
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), opponent.getObjectId()));
					if (object != opponent.getObjectId()) {
						PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), object));
					}
				}
			}
		});
		Integer NullObject = null;
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), getInstanceReward(), null));
		instanceReward.sendPacket(4, object);
	}
	
	private void updatePoints(Creature victim) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int bonus = 0;
		int rank = 0;
		if (victim instanceof Player) {
			final HarmonyGroupReward victimGroup = instanceReward.getHarmonyGroupReward(victim.getObjectId());
			victimGroup.addPoints(deathFine);
			bonus = killBonus;
			rank = instanceReward.getRank(victimGroup.getPoints());
			instanceReward.sendPacket(10, victim.getObjectId());
		} else {
			bonus = getNpcBonus(((Npc) victim).getNpcId());
		} if (bonus == 0) {
			return;
		}
		for (AggroInfo damager : victim.getAggroList().getList()) {
			if (!(damager.getAttacker() instanceof Creature)) {
				continue;
			}
			Creature master = ((Creature) damager.getAttacker()).getMaster();
			if (master == null) {
				continue;
			} if (master instanceof Player) {
				Player attaker = (Player) master;
				int rewardPoints = (victim instanceof Player && instanceReward.getRound() == 3 && rank == 0 ? bonus * 3 : bonus) * damager.getDamage() / victim.getAggroList().getTotalDamage();
				instanceReward.getHarmonyGroupReward(attaker.getObjectId()).addPoints(rewardPoints);
				sendSystemMsg(attaker, victim, rewardPoints);
				instanceReward.sendPacket(10, attaker.getObjectId());
			}
		} if (instanceReward.hasCapPoints()) {
			instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			reward();
			instanceReward.sendPacket(5, null);
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
		if (npc.getAggroList().getMostPlayerDamage() == null) {
			return;
		}
		updatePoints(npc);
	}
	/**
	 * 处理 sendSystemMsg。
	 * Handle sendSystemMsg.
	 *
	 * 玩家 / player
	 * creature
	 * rewardPoints
	 */
	
	protected void sendSystemMsg(Player player, Creature creature, int rewardPoints) {
		int nameId = creature.getObjectTemplate().getNameId();
		DescriptionId name = new DescriptionId(nameId * 2 + 1);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, nameId == 0 ? creature.getName() : name, rewardPoints));
	}
	
	private int getNpcBonus(int npcId) {
		switch (npcId) {
			case 207102:
			case 207116:
			case 243678: //Roaming Volcanic Petrahulk.
				return 400;
			case 207099:
				return 200;
			case 243679: //Heated Negotiator Grangvolkan.
				return 100;
			case 219328: //Plaza Wall.	
			case 243680: //Lurking Fangwing.
				return 50;
			default:
				return 0;
		}
	}
	
	private int getTime() {
		return instanceReward.getTime();
	}
	
	/**
	 * 玩家登录到该副本时处理。
	 * Handle a player logging into this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogin(Player player) {
		sendEnterPacket(player);
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
		instanceReward = new HarmonyArenaReward(mapId, instanceId, instance);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		instanceReward.setInstanceStartTime();
		spawnRings();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed && !instanceReward.isRewarded() && canStart()) {
					openDoors();
					// 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
					sendMsgByRace(1401181, Race.PC_ALL, 0);
					instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
					instanceReward.sendPacket(10, null);
					instanceReward.sendPacket(2, null);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						/**
						 * 处理 run。
						 * Handle run.
						 */
						@Override
						public void run() {
							if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
								instanceReward.setRound(2);
								instanceReward.setRndZone();
								instanceReward.sendPacket(10, null);
								instanceReward.sendPacket(2, null);
								changeZone();
								// 本轮击败更高军阶队伍可获得额外点数。 / If you defeat a higher rank group in this round, you can earn additional points.
								sendMsgByRace(1401491, Race.PC_ALL, 2000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									/**
									 * 处理 run。
									 * Handle run.
									 */
									@Override
									public void run() {
										if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
											instanceReward.setRound(3);
											instanceReward.setRndZone();
											instanceReward.sendPacket(10, null);
											instanceReward.sendPacket(2, null);
											changeZone();
											// 本轮击败更高军阶队伍可获得额外点数。 / If you defeat a higher rank group in this round, you can earn additional points.
											sendMsgByRace(1401491, Race.PC_ALL, 2000);
											GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
												/**
												 * 处理 run。
												 * Handle run.
												 */
												@Override
												public void run() {
													if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
														instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
														reward();
														instanceReward.sendPacket(5, null);
													}
												}
											}, 180000);
										}
									}
								}, 180000);
							}
						}
					}, 180000);
				}
			}
		}, 120000);
	}
	/**
	 * 处理 spawnRings。
	 * Handle spawnRings.
	 */
	
	protected void spawnRings() {
	}
	
	private boolean canStart() {
		if (instance.getPlayersInside().size() < 2) {
			onInstanceDestroy();
			// 独自一人时无法使用。 / Unavailable to use when you're alone.
			sendMsgByRace(1403045, Race.PC_ALL, 0);
			instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			reward();
			instanceReward.sendPacket(5, null);
			return false;
		}
		return true;
	}
	
	private void changeZone() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				for (Player player : instance.getPlayersInside()) {
					instanceReward.portToPosition(player);
					instanceReward.sendPacket(4, player.getObjectId());
				}
			}
		}, 1000);
	}
	
	private void sendPacket(final AionServerPacket packet) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, packet);
			}
		});
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			instanceReward.clearPosition(playerReward.getPosition(), Boolean.FALSE);
			instanceReward.removePlayerReward(playerReward);
		}
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
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
		}, time);
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
	
	private void openDoors() {
		for (StaticDoor door : instance.getDoors().values()) {
			if (door != null) {
				door.setOpen(true);
			}
		}
	}
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	
	protected void reward() {
		if (instanceReward.canRewarded()) {
			for (Player player : instance.getPlayersInside()) {
				HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(player.getObjectId());
				float playerRate = player.getRates().getGloryRewardRate();
				// <欧比斯点数> / <Abyss Points>
				AbyssPointsService.addAp(player, group.getBasicAP() + group.getRankingAP() + (int) (group.getScoreAP() * playerRate));
				// <荣耀点数> / <Glory Points>
				AbyssPointsService.addGp(player, group.getBasicGP() + group.getRankingGP() + (int) (group.getScoreGP() * playerRate));
				int courage = group.getBasicCourage() + group.getRankingCourage() + (int) (group.getScoreCourage()* playerRate);
				if (courage != 0) {
					ItemService.addItem(player, 186000137, courage);
				}
				int gloryTicket = group.getGloryTicket();
				if (gloryTicket != 0) {
					ItemService.addItem(player, 186000185, gloryTicket);
				}
				int infinity = group.getBasicInfinity() + group.getRankingInfinity() + (int) (group.getScoreInfinity()* playerRate);
				if (infinity != 0) {
					ItemService.addItem(player, 186000442, infinity);
				}
			}
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
						if (PlayerActions.isAlreadyDead(player)) {
							PlayerReviveService.duelRevive(player);
						}
						onExitInstance(player);
					}
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
				}
			}
		}, 10000);
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
		PvPArenaPlayerReward ownerReward = instanceReward.getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player);
		instanceReward.sendPacket(4, player.getObjectId());
		if (lastAttacker != null && lastAttacker != player) {
			if (lastAttacker instanceof Player) {
				Player winner = (Player) lastAttacker;
				Integer winnerObj = winner.getObjectId();
				instanceReward.getHarmonyGroupReward(winnerObj).addPvPKillToPlayer();
				int worldId = winner.getWorldId();
				GameEngineServices.questEngine().onKillInWorld(new QuestEnv(player, winner, 0, 0), worldId);
			}
		}
		updatePoints(player);
		return true;
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
		final Integer object = player.getObjectId();
		final HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(object);
		if (!instanceReward.isStartProgress() || group == null) {
			return;
		}
		int rewardetPoints = getNpcBonus(npc.getNpcId());
		int skill = instanceReward.getNpcBonusSkill(npc.getNpcId());
		if (skill != 0) {
			useSkill(npc, player, skill >> 8, skill & 0xFF);
		}
		group.addPoints(rewardetPoints);
		sendSystemMsg(player, npc, rewardetPoints);
		instanceReward.sendPacket(10, object);
	}
	/**
	 * 处理 useSkill。
	 * Handle useSkill.
	 *
	 * npc
	 * 玩家 / player
	 * skill id
	 * level
	 */
	
	protected void useSkill(Npc npc, Player player, int skillId, int level) {
		GameEngineServices.skillEngine().getSkill(npc, skillId, level, player).useNoAnimationSkill();
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
		if (!isInstanceDestroyed) {
			instanceReward.portToPosition(player);
		}
		return true;
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
}