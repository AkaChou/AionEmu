package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
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
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.PvPArenaReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.ArenaReward;
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
 * PvP 竞技场副本事件处理器。
 * Instance event handler for PvP Arena.
 *
 * @author Encom
 */

public class PvPArenaInstance extends GeneralInstanceHandler
{
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 副本奖励对象 / instance reward object */
	protected PvPArenaReward instanceReward;
	
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
		PvPArenaPlayerReward ownerReward = getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player,
				instanceReward.getRebirthBuffDuration(instanceReward.getRank(ownerReward.getPoints())));
		sendPacket();
		if (lastAttacker != null && lastAttacker != player) {
			if (lastAttacker instanceof Player) {
				Player winner = (Player) lastAttacker;
				PvPArenaPlayerReward reward = getPlayerReward(winner.getObjectId());
				reward.addPvPKillToPlayer();
				int worldId = winner.getWorldId();
				GameEngineServices.questEngine().onKillInWorld(new QuestEnv(player, winner, 0, 0), worldId);
			}
		}
		updatePoints(player);
		return true;
	}
	
	private void updatePoints(Creature victim) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int bonus = 0;
		int rank = 0;
		if (victim instanceof Player) {
			PvPArenaPlayerReward victimFine = getPlayerReward(victim.getObjectId());
			rank = instanceReward.getRank(victimFine.getPoints());
			victimFine.addPoints(-instanceReward.getDeathScore(rank));
			bonus = instanceReward.getKillScore() * instanceReward.getScoreModifier(rank) / 100;
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
				int rewardPoints = bonus * damager.getDamage() / victim.getAggroList().getTotalDamage();
				getPlayerReward(attaker.getObjectId()).addPoints(rewardPoints);
				sendSystemMsg(attaker, victim, rewardPoints);
			}
		} if (instanceReward.hasCapPoints()) {
			reward();
		}
		sendPacket();
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
		final int npcId = npc.getNpcId();
		if (npcId == 701173 || //Blessed Relics.
			npcId == 701187) { //Blessed Relics.
			spawnBlessedRelics(30000);
		} if (npcId == 701174 || //Cursed Relics.
			npcId == 701188) { //Cursed Relics.
			spawnCursedRelics(30000);
		}
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		Integer object = player.getObjectId();
		if (!containPlayer(object)) {
			instanceReward.regPlayerReward(object);
			PvPArenaPlayerReward playerReward = getPlayerReward(object);
			playerReward.setRewardRate(rewardRate(player));
			playerReward.applyBoostMoraleEffect(player, instanceReward.getRebirthBuffDuration(0));
			instanceReward.setRndPosition(object);
		} else {
			getPlayerReward(object).setRewardRate(rewardRate(player));
			instanceReward.portToPosition(player);
		}
		applyStageBuff(player);
		sendPacket();
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
	
	private void spawnBlessedRelics(int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
					spawn(Rnd.get(1, 2) == 1 ? 701173 : 701187, 1841.951f, 1733.968f, 300.242f, (byte) 0);
				}
			}
		}, time);
	}
	
	private void spawnCursedRelics(int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
					spawn(Rnd.get(1, 2) == 1 ? 701174 : 701188, 674.517f, 1778.428f, 204.693f, (byte) 0);
				}
			}
		}, time);
	}
	
	private int getNpcBonus(int npcId) {
		switch (npcId) {
			case 243666: //Black Claw Scratcher.
			case 243675: //Red Sand Brax.
			case 243676: //Red Sand Tog.
			case 243667: //Mutated Drakan Fighter.
			    return 100;
			case 243681: //Casus Manor Chief Maid.
			    return 400;
			case 243671: //Casus Manor Butler.
			    return 650;
			case 243672: //Casus Manor Noble.
			    return 750;
			case 243665: //Mumu Rake Gatherer.
			    return 1250;
			case 243673: //Pale Carmina.
			case 243674: //Corrupt Casus.
				return 1500;
			// 祝福遗物/诅咒遗物 / Blessed Relics/Cursed Relics
			case 701173:
			case 701174:
			case 701187:
			case 701188:
			case 701201:
			case 701202:
			case 701834:
			case 701835:
			    return 1750;
			default:
				return 0;
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
		return instanceReward;
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		getPlayerReward(player.getObjectId()).beginAbsence();
	}
	
	/**
	 * 玩家登录到该副本时处理。
	 * Handle a player logging into this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogin(Player player) {
		PvPArenaPlayerReward playerReward = getPlayerReward(player.getObjectId());
		playerReward.endAbsence();
		playerReward.setRewardRate(rewardRate(player));
		applyStageBuff(player);
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
		instanceReward = new PvPArenaReward(mapId, instanceId, instance);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		spawnRings();
		if (!instanceReward.isSoloArena()) {
			spawnCursedRelics(0);
			spawnBlessedRelics(0);
		}
		instanceReward.setInstanceStartTime();
		GameThreadPoolServices.threadPoolManager().schedule(this::startBattle,
				instanceReward.getWaitTimeSeconds() * 1000L);
	}

	private void startBattle() {
		if (isInstanceDestroyed || instanceReward.isRewarded() || !canStart()) {
			return;
		}
		openDoors();
		sendMsgByRace(1401181, Race.PC_ALL, 0);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendPacket();
		GameThreadPoolServices.threadPoolManager().schedule(this::finishRound,
				instanceReward.getStageTimeSeconds() * 1000L);
	}

	private void finishRound() {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		if (instanceReward.getRound() == 3) {
			finishBattle();
			return;
		}
		removeStageBuffs();
		instanceReward.setRound(instanceReward.getRound() + 1);
		instanceReward.setRndZone();
		applyStageBuffs();
		changeZone();
		if (instanceReward.getRound() == instanceReward.getScoreModifierStartStage()) {
			sendMsgByRace(1401491, Race.PC_ALL, 2000);
		}
		sendPacket();
		GameThreadPoolServices.threadPoolManager().schedule(this::finishRound,
				instanceReward.getStageTimeSeconds() * 1000L);
	}
	
	private boolean canStart() {
		if (instanceReward.getInstanceRewards().isEmpty()) {
			// 独自一人时无法使用。 / Unavailable to use when you're alone.
			sendMsgByRace(1403045, Race.PC_ALL, 0);
			finishBattle();
			return false;
		}
		return true;
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
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * visible object
	 * result
	 */
	
	protected PvPArenaPlayerReward getPlayerReward(Integer object) {
		instanceReward.regPlayerReward(object);
		return (PvPArenaPlayerReward) instanceReward.getPlayerReward(object);
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
			playerReward.endStageBuff(player);
			playerReward.beginAbsence();
			instanceReward.clearPosition(playerReward.getPosition(), Boolean.FALSE);
		}
	}
	/**
	 * 向副本内玩家发送数据包。
	 * Send a packet to players in the instance.
	 */
	
	protected void sendPacket() {
		instanceReward.sendPacket();
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
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
				}
				sendPacket();
			}
		}, 1000);
	}
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	
	protected synchronized void reward() {
		finishBattle();
	}

	private synchronized void finishBattle() {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		removeStageBuffs();
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = System.currentTimeMillis();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			playerReward.finalizePlaytimeBonus(instanceReward.getTotalPlayMillis(), endedAt);
		}
		int playerCount = instanceReward.getInstanceRewards().size();
		int totalScore = instanceReward.getTotalPoints();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			int rank = instanceReward.getRank(playerReward.getScorePoints());
			ArenaReward arenaReward = InstanceSettlementService.arenaReward(instanceReward.getArenaRow(), rank,
					playerCount, playerReward.getScorePoints(), totalScore, playerReward.getRewardRate());
			playerReward.applyArenaReward(arenaReward);
			Player player = instance.getPlayer(playerReward.getOwner());
			if (player == null) {
				InstanceSettlementService.queue(instance, playerReward.getOwner(), "arena", arenaReward.plan());
				continue;
			}
			if (PlayerActions.isAlreadyDead(player)) {
				PlayerReviveService.duelRevive(player);
			}
			InstanceSettlementService.settle(instance.getDynamicInstance().getInstanceUid(), player, "arena",
					arenaReward.plan());
		}
		sendPacket();
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

	private double rewardRate(Player player) {
		if (instanceReward.isSoloArena()) {
			return player.getRates().getDisciplineRewardRate();
		}
		if (instanceReward.isGlory()) {
			return player.getRates().getGloryRewardRate();
		}
		return player.getRates().getChaosRewardRate();
	}

	private void applyStageBuffs() {
		for (Player player : instance.getPlayersInside()) {
			applyStageBuff(player);
		}
	}

	private void applyStageBuff(Player player) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int buffId = instanceReward.getStageEndBuffId(instanceReward.getRound());
		int targetRank = instanceReward.getStageEndBuffTargetRank(instanceReward.getRound());
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (buffId > 0 && playerReward != null
				&& instanceReward.getRank(playerReward.getPoints()) + 1 >= targetRank) {
			playerReward.applyStageBuff(player, buffId, instanceReward.getStageTimeSeconds() * 1000);
		}
	}

	private void removeStageBuffs() {
		for (Player player : instance.getPlayersInside()) {
			PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
			if (playerReward != null) {
				playerReward.endStageBuff(player);
			}
		}
	}
	/**
	 * 处理 spawnRings。
	 * Handle spawnRings.
	 */
	
	protected void spawnRings() {
	}
	/**
	 * 返回 npc。
	 * Return the npc.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * result
	 */
	
	protected Npc getNpc(float x, float y, float z) {
		if (!isInstanceDestroyed) {
			for (Npc npc : instance.getNpcs()) {
				SpawnTemplate st = npc.getSpawn();
				if (st.getX() == x && st.getY() == y && st.getZ() == z) {
					return npc;
				}
			}
		}
		return null;
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
		switch(npc.getNpcId()) {
			case 701169: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1798.8951f, 1727.2413f, 302.81836f, (byte) 62);
				spawn(702405, 1808.9938f, 1703.7997f, 302.73233f, (byte) 74);
			break;
			case 701170: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1848.1892f, 1689.1056f, 302.74982f, (byte) 92);
				spawn(702405, 1871.4725f, 1699.5228f, 303.0393f, (byte) 104);
			break;
			case 701171: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1886.8333f, 1738.3987f, 302.5374f, (byte) 3);
				spawn(702405, 1876.5596f, 1761.9902f, 302.6582f, (byte) 14);
			break;
			case 701172: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1837.242f, 1776.3717f, 302.7615f, (byte) 32);
				spawn(702405, 1814.1249f, 1766.2068f, 302.61606f, (byte) 43);
			break;
			case 207102: //Recovery Relics.
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 10000);
				player.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, 10000);
			break;
		   /**
	 * Treasure Box [Arena Of Chaos/Chaos Training Grounds]
	 */
			case 218784:
			case 218785:
			case 218786:
			case 218787:
			case 218788:
			case 218789:
		   /**
	 * Treasure Box [Arena Of Discipline/Discipline Training Grounds]
	 */
			case 218791:
			case 218792:
			case 218793:
			case 218794:
			case 218795:
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				} switch (Rnd.get(1, 11)) {
					case 1:
					    ItemService.addItem(player, 186000030, 1); //Gold Medal.
					break;
					case 2:
					    ItemService.addItem(player, 186000031, 1); //Silver Medal.
					break;
					case 3:
					    ItemService.addItem(player, 186000096, 1); //Platinum Medal.
					break;
					case 4:
					    ItemService.addItem(player, 186000130, 5); //Crucible Insignia.
					break;
					case 5:
					    ItemService.addItem(player, 186000137, 5); //Courage Insignia.
					break;
					case 6:
					    ItemService.addItem(player, 186000147, 1); //Mithril Medal.
					break;
					case 7:
					    ItemService.addItem(player, 186000165, 5); //Opportunity Token.
					break;
					case 8:
					    ItemService.addItem(player, 186000242, 1); //Ceramium Medal.
					break;
					case 9:
					    ItemService.addItem(player, 186000442, 5); //Valor Insignia.
					break;
					case 10:
					    ItemService.addItem(player, 182213259, 5); //Glorious Insignia.
					break;
					case 11:
					    ItemService.addItem(player, 186000454, 5); //Spinel Medal.
					break;
				}
				despawnNpc(npc);
			break;
		} if (!instanceReward.isStartProgress()) {
			return;
		}
		int rewardetPoints = getNpcBonus(npc.getNpcId());
		int skill = instanceReward.getNpcBonusSkill(npc.getNpcId());
		if (skill != 0) {
			useSkill(npc, player, skill >> 8, skill & 0xFF);
		}
		getPlayerReward(player.getObjectId()).addPoints(rewardetPoints);
		sendSystemMsg(player, npc, rewardetPoints);
		sendPacket();
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
}
