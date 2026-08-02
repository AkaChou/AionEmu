package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.configs.main.PvPConfig;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.controllers.attack.KillList;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.utils.stats.StatFunctions;

/**
 * PvP 服务，处理玩家击杀计数、奖励分发与阵营战相关逻辑。
 * PvP service handling player kill counts, reward distribution, and faction combat logic.
 *
 * @author (Encom)
 */
@Slf4j(topic = "KILL_LOG")
public class PvpService {

	private static volatile ObjectProvider<PvpService> instanceProvider;

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final PvpService getInstance() {
		ObjectProvider<PvpService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/** 玩家击杀记录映射。 / Player kill-list map. */
	private Map<Integer, KillList> pvpKillLists;

	/**
	 * 初始化击杀记录表。
	 * Initializes the kill-list map.
	 */
	public PvpService() {
		pvpKillLists = new HashMap<>();
	}

	/**
	 * 注入 Spring ObjectProvider。
	 * Injects the Spring ObjectProvider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<PvpService> provider) {
		instanceProvider = provider;
	}

	/**
	 * @param winnerId
	 * @param victimId
	 * @return
	 */
	private int getKillsFor(int winnerId, int victimId) {
		KillList winnerKillList = pvpKillLists.get(winnerId);

		if (winnerKillList == null) {
			return 0;
		}
		return winnerKillList.getKillsFor(victimId);
	}

	/**
	 * @param winnerId
	 * @param victimId
	 */

	private void addKillFor(int winnerId, int victimId) {
		KillList winnerKillList = pvpKillLists.get(winnerId);
		if (winnerKillList == null) {
			winnerKillList = new KillList();
			pvpKillLists.put(winnerId, winnerKillList);
		}
		winnerKillList.addKillFor(victimId);
	}

	/**
	 * 处理 PvP 击杀奖励：击杀计数、AP/物品/任务通知，支持小队与联盟分摊。
	 * Handles PvP kill rewards: kill counts, AP/item/quest notify, with group and alliance sharing.
	 *
	 * @param victim 被击杀玩家 / killed player
	 */
	public void doReward(Player victim) {
		// 胜者为获得击杀数的玩家 / winner is the player that receives the kill count
		final Player winner = victim.getAggroList().getMostPlayerDamage();

		int totalDamage = victim.getAggroList().getTotalDamage();

		if (totalDamage == 0 || winner == null || winner.getRace() == victim.getRace()) {
			return;
		}

		// 将玩家击杀加入记录。 / Add Player Kill to record.
		if (this.getKillsFor(winner.getObjectId(), victim.getObjectId()) < PvPConfig.MAX_DAILY_PVP_KILLS) {
			winner.getAbyssRank().setAllKill();
			@SuppressWarnings("unused")
			int kills = winner.getAbyssRank().getAllKill();

			// PvP 通行奖励 / PvP Toll Reward
			if (PvPConfig.ENABLE_TOLL_REWARD) {
				if (Rnd.get(0, 100) > PvPConfig.TOLL_CHANCE) {
					GameRuntimeServices.inGameShopEn().addToll(winner, PvPConfig.TOLL_QUANTITY);
					PacketSendUtility.sendMessage(winner,
							"You've received " + PvPConfig.TOLL_QUANTITY + " tolls from PvP!");
				}
			}
		}

		// 宣布玩家已死亡。 / Announce that player has died.
		PacketSendUtility.broadcastPacketAndReceive(victim,
				SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_FRIENDLY_DEATH_TO_B(victim.getName(), winner.getName()));

		// PvP 击杀奖励。 / Pvp Kill Reward.
		int reduceap = PunishmentConfig.PUNISHMENT_REDUCEAP;
		if (reduceap < 0) {
			reduceap *= -1;
		}
		if (reduceap > 100) {
			reduceap = 100;
		}

		// 击杀日志 / Kill-log
		if (LoggingConfig.LOG_KILL) {
			log.info(I18n.get("log.ffb3dd3cc55c", winner.getName(), victim.getName()));
		}

		// 击杀日志 / Kill-log
		if ((LoggingConfig.LOG_PL) || (reduceap > 0)) {
			String ip1 = winner.getClientConnection().getIP();
			String mac1 = winner.getClientConnection().getMacAddress();
			String ip2 = victim.getClientConnection().getIP();
			String mac2 = victim.getClientConnection().getMacAddress();
			if ((mac1 != null) && (mac2 != null)) {
				if ((ip1.equalsIgnoreCase(ip2)) && (mac1.equalsIgnoreCase(mac2))) {
					AuditLogger.info(winner, "Power Leveling : " + winner.getName() + " with " + victim.getName()
							+ ", They have the sames ip=" + ip1 + " and mac=" + mac1 + ".");
					if (reduceap > 0) {
						int win_ap = winner.getAbyssRank().getAp() * reduceap / 100;
						int vic_ap = victim.getAbyssRank().getAp() * reduceap / 100;
						AbyssPointsService.addAp(winner, -win_ap);
						AbyssPointsService.addAp(victim, -vic_ap);
						PacketSendUtility.sendMessage(winner, "[PL-AP] You lost " + reduceap + "% of your total ap");
						PacketSendUtility.sendMessage(victim, "[PL-AP] You lost " + reduceap + "% of your total ap");
					}
					return;
				}
				if (ip1.equalsIgnoreCase(ip2)) {
					AuditLogger.info(winner, "Possible Power Leveling : " + winner.getName() + " with "
							+ victim.getName() + ", They have the sames ip=" + ip1 + ".");
					AuditLogger.info(winner, "Check if " + winner.getName() + " and " + victim.getName()
							+ " are Brothers-Sisters-Lovers-dogs-cats...");
				}

			}
		}
		if (winner.getLevel() - victim.getLevel() <= PvPConfig.MAX_AUTHORIZED_LEVEL_DIFF) {
			if (getKillsFor(winner.getObjectId().intValue(),
					victim.getObjectId().intValue()) < PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION) {
				if (PvPConfig.ENABLE_MEDAL_REWARDING) {
					if (Rnd.get() * 100 < PvPRewardService.getMedalRewardChance(winner, victim)) {
						int medalId = PvPRewardService.getRewardId(winner, victim, false);
						long medalCount = PvPRewardService.getRewardQuantity(winner, victim);
						ItemService.addItem(winner, medalId, medalCount);
						if (winner.getInventory().getItemCountByItemId(medalId) > 0) {
							if (medalCount == 1)
								PacketSendUtility.sendPacket(winner,
										new SM_SYSTEM_MESSAGE(1390000, new DescriptionId(medalId)));
							else {
								PacketSendUtility.sendPacket(winner,
										new SM_SYSTEM_MESSAGE(1390005, medalCount, new DescriptionId(medalId)));
							}
						}
					}
				}
				if ((PvPConfig.ENABLE_TOLL_REWARD)
						&& (Rnd.get() * 100.0F < PvPRewardService.getTollRewardChance(winner, victim))) {
					int qt = PvPRewardService.getTollQuantity(winner, victim);
					GameRuntimeServices.inGameShopEn().addToll(winner, qt);
					if (qt == 1)
						PacketSendUtility.sendBrightYellowMessage(winner, "You obtained " + qt + " point toll.");
					else {
						PacketSendUtility.sendBrightYellowMessage(winner, "You obtained " + qt + " point toll.");
					}
				}
				if (PvPConfig.GENOCIDE_SPECIAL_REWARDING != 0) {
					switch (PvPConfig.GENOCIDE_SPECIAL_REWARDING) {
					case 1:
						if ((winner.getSpreeLevel() <= 2) || (Rnd.get() * 100 >= PvPConfig.SPECIAL_REWARD_CHANCE))
							break;
						int abyssId = PvPRewardService.getRewardId(winner, victim, true);
						ItemService.addItem(winner, abyssId, 1L);
						log.info(I18n.get("log.3dd877c09dfb", winner.getName(), abyssId, victim.getName()));
						break;
					default:
						if ((winner.getSpreeLevel() <= 2) || (Rnd.get() * 100 >= PvPConfig.SPECIAL_REWARD_CHANCE))
							break;
						ItemService.addItem(winner, PvPConfig.GENOCIDE_SPECIAL_REWARDING, 1L);
						log.info(I18n.get("log.3dd877c09dfb", winner.getName(), PvPConfig.GENOCIDE_SPECIAL_REWARDING, victim.getName()));
						break;
					}
				}
			} else {
				PacketSendUtility.sendMessage(winner, "You will not gain anything by killing the player "
						+ victim.getName() + " for the rest of the day because you kill too many times.");
			}
		}
		int playerDamage = 0;
		boolean success = false;

		// 向造成伤害的小队与玩家分配 AP。 / Distribute AP to groups and players that had damage.
		for (AggroInfo aggro : victim.getAggroList().getFinalDamageList(true)) {
			success = false;
			if (aggro.getAttacker() instanceof Player) {
				success = rewardPlayer(victim, totalDamage, aggro);
			} else if (aggro.getAttacker() instanceof PlayerGroup) {
				success = rewardPlayerGroup(victim, totalDamage, aggro);
			} else if (aggro.getAttacker() instanceof PlayerAlliance) {
				success = rewardPlayerAlliance(victim, totalDamage, aggro);
			}

			// 最后添加伤害，以免计入同种族伤害（决斗、竞技场）。 / Add damage last, so we don't include damage from same race. (Duels, Arena)
			if (success) {
				playerDamage += aggro.getDamage();
			}
		}
		GameFeatureServices.protectorConquerorService().updateRanks(winner, victim);

		// 通知任务引擎胜者及其小队 / notify Quest engine for winner + his group
		notifyKillQuests(winner, victim);

		// 对战败玩家应用损失 GP / Apply lost GP to defeated player
		final int gpLost = StatFunctions.calculatePvPGpLost(victim, winner);
		final int gpActuallyLost = gpLost * playerDamage / totalDamage;

		// 对战败玩家应用损失 AP / Apply lost AP to defeated player
		final int apLost = StatFunctions.calculatePvPApLost(victim, winner);
		final int apActuallyLost = apLost * playerDamage / totalDamage;

		if (apActuallyLost > 0) {
			AbyssPointsService.addAp(victim, -apActuallyLost);
			// victim.setRawKillCount(0);

			// 取消 PvP 连杀 / Cancel Spree in PvP
			if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
				Creature killer = null;
				boolean isPvPDeath = false;
				PvPSpreeService.cancelSpree(victim, killer, isPvPDeath);
			}
		}

		if (PvPConfig.ENABLE_GP_LOSE) {
			if (PvPConfig.ENABLE_GP_FIXED_LOSE) {
				AbyssPointsService.addGp(victim, -PvPConfig.GP_LOSE);
			}
		}
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if group is not same race
	 */
	private boolean rewardPlayerGroup(Player victim, int totalDamage, AggroInfo aggro) {
		// 奖励小队 / Reward Group
		PlayerGroup group = ((PlayerGroup) aggro.getAttacker());

		// 不奖励同阵营玩家。 / Don't Reward Player of Same Faction.
		if (group.getRace() == victim.getRace()) {
			return false;
		}

		// 查找范围内的小队成员 / Find group members in range
		List<Player> players = new ArrayList<Player>();

		// 在本地小队中找最高军阶与等级 / Find highest rank and level in local group
		int maxRank = AbyssRankEnum.GRADE9_SOLDIER.getId();
		int maxLevel = 0;

		for (Player member : group.getMembers()) {
			if (MathUtil.isIn3dRange(member, victim, GroupConfig.GROUP_MAX_DISTANCE)) {
				// 不要向已死亡玩家分配 AP！ / Don't distribute AP to a dead player!
				if (!member.getLifeStats().isAlreadyDead()) {
					players.add(member);
					if (member.getLevel() > maxLevel) {
						maxLevel = member.getLevel();
					}
					if (member.getAbyssRank().getRank().getId() > maxRank) {
						maxRank = member.getAbyssRank().getRank().getId();
					}
				}
			}
		}

		// 他们都已死亡或超出范围。 / They are all dead or out of range.
		if (players.isEmpty()) {
			return false;
		}
		int baseApReward = StatFunctions.calculatePvpApGained(victim, maxRank, maxLevel);
		int baseXpReward = StatFunctions.calculatePvpXpGained(victim, maxRank, maxLevel);
		int baseDpReward = StatFunctions.calculatePvpDpGained(victim, maxRank, maxLevel);
		float groupPercentage = (float) aggro.getDamage() / totalDamage;
		int apRewardPerMember = Math.round(baseApReward * groupPercentage / players.size());
		int xpRewardPerMember = Math.round(baseXpReward * groupPercentage / players.size());
		int dpRewardPerMember = Math.round(baseDpReward * groupPercentage / players.size());

		for (Player member : players) {
			int memberApGain = 1;
			int memberXpGain = 1;
			int memberDpGain = 1;
			if (this.getKillsFor(member.getObjectId(), victim.getObjectId()) < PvPConfig.MAX_DAILY_PVP_KILLS) {
				if (apRewardPerMember > 0) {
					memberApGain = Math.round(RewardType.AP_PLAYER.calcReward(member, apRewardPerMember));
				}
				if (xpRewardPerMember > 0) {
					memberXpGain = Math.round(xpRewardPerMember * member.getRates().getXpPlayerGainRate());
				}
				if (dpRewardPerMember > 0) {
					memberDpGain = Math.round(
							StatFunctions.adjustPvpDpGained(dpRewardPerMember, victim.getLevel(), member.getLevel())
									* member.getRates().getDpPlayerRate());
				}
				if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
					Player luckyPlayer = players.get(Rnd.get(players.size()));
					PvPSpreeService.increaseRawKillCount(luckyPlayer);
				}
			}
			AbyssPointsService.addAp(member, victim, memberApGain);
			if (PvPConfig.ENABLE_GP_REWARD) {
				AbyssPointsService.addGp(member, 150);
			}
			member.getCommonData().addExp(memberXpGain, RewardType.PVP_KILL, victim.getName());
			member.getCommonData().addDp(memberDpGain);
			this.addKillFor(member.getObjectId(), victim.getObjectId());
		}
		return true;
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if group is not same race
	 */
	private boolean rewardPlayerAlliance(Player victim, int totalDamage, AggroInfo aggro) {
		// 奖励联盟 / Reward Alliance
		PlayerAlliance alliance = ((PlayerAlliance) aggro.getAttacker());

		// 不奖励同阵营玩家。 / Don't Reward Player of Same Faction.
		if (alliance.getLeaderObject().getRace() == victim.getRace()) {
			return false;
		}

		// 查找范围内的小队成员 / Find group members in range
		List<Player> players = new ArrayList<Player>();

		// 在本地小队中找最高军阶与等级 / Find highest rank and level in local group
		int maxRank = AbyssRankEnum.GRADE9_SOLDIER.getId();
		int maxLevel = 0;

		for (Player member : alliance.getMembers()) {
			if (!member.isOnline()) {
				continue;
			}
			if (MathUtil.isIn3dRange(member, victim, GroupConfig.GROUP_MAX_DISTANCE)) {
				// 不要向已死亡玩家分配 AP！ / Don't distribute AP to a dead player!
				if (!member.getLifeStats().isAlreadyDead()) {
					players.add(member);
					if (member.getLevel() > maxLevel) {
						maxLevel = member.getLevel();
					}
					if (member.getAbyssRank().getRank().getId() > maxRank) {
						maxRank = member.getAbyssRank().getRank().getId();
					}
				}
			}
		}

		// 他们都已死亡或超出范围。 / They are all dead or out of range.
		if (players.isEmpty()) {
			return false;
		}
		int baseApReward = StatFunctions.calculatePvpApGained(victim, maxRank, maxLevel);
		int baseXpReward = StatFunctions.calculatePvpXpGained(victim, maxRank, maxLevel);
		int baseDpReward = StatFunctions.calculatePvpDpGained(victim, maxRank, maxLevel);
		float groupPercentage = (float) aggro.getDamage() / totalDamage;
		int apRewardPerMember = Math.round(baseApReward * groupPercentage / players.size());
		int xpRewardPerMember = Math.round(baseXpReward * groupPercentage / players.size());
		int dpRewardPerMember = Math.round(baseDpReward * groupPercentage / players.size());

		for (Player member : players) {
			int memberApGain = 1;
			int memberXpGain = 1;
			int memberDpGain = 1;
			if (this.getKillsFor(member.getObjectId(), victim.getObjectId()) < PvPConfig.MAX_DAILY_PVP_KILLS) {
				if (apRewardPerMember > 0) {
					memberApGain = Math.round(RewardType.AP_PLAYER.calcReward(member, apRewardPerMember));
				}
				if (xpRewardPerMember > 0) {
					memberXpGain = Math.round(xpRewardPerMember * member.getRates().getXpPlayerGainRate());
				}
				if (dpRewardPerMember > 0) {
					memberDpGain = Math.round(
							StatFunctions.adjustPvpDpGained(dpRewardPerMember, victim.getLevel(), member.getLevel())
									* member.getRates().getDpPlayerRate());
				}
			}
			AbyssPointsService.addAp(member, victim, memberApGain);

			if (PvPConfig.ENABLE_GP_REWARD) {
				AbyssPointsService.addGp(member, 150);
			}
			member.getCommonData().addExp(memberXpGain, RewardType.PVP_KILL, victim.getName());
			member.getCommonData().addDp(memberDpGain);

			this.addKillFor(member.getObjectId(), victim.getObjectId());
		}
		return true;
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if player is not same race
	 */
	private boolean rewardPlayer(Player victim, int totalDamage, AggroInfo aggro) {
		// 奖励玩家 / Reward Player
		Player winner = ((Player) aggro.getAttacker());

		// 不奖励超出范围/死亡/同阵营玩家 / Don't Reward Player out of range/dead/same faction
		if (winner.getRace() == victim.getRace()
				|| !MathUtil.isIn3dRange(winner, victim, GroupConfig.GROUP_MAX_DISTANCE)
				|| winner.getLifeStats().isAlreadyDead()) {
			return false;
		}
		int baseApReward = 1;
		int baseXpReward = 1;
		int baseDpReward = 1;

		if (this.getKillsFor(winner.getObjectId(), victim.getObjectId()) < PvPConfig.MAX_DAILY_PVP_KILLS) {
			baseApReward = StatFunctions.calculatePvpApGained(victim, winner.getAbyssRank().getRank().getId(),
					winner.getLevel());
			baseXpReward = StatFunctions.calculatePvpXpGained(victim, winner.getAbyssRank().getRank().getId(),
					winner.getLevel());
			baseDpReward = StatFunctions.calculatePvpDpGained(victim, winner.getAbyssRank().getRank().getId(),
					winner.getLevel());
			if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
				PvPSpreeService.increaseRawKillCount(winner);
			}
		}
		int apPlayerReward = Math.round(baseApReward * aggro.getDamage() / totalDamage);
		apPlayerReward = (int) RewardType.AP_PLAYER.calcReward(winner, apPlayerReward);
		int xpPlayerReward = Math
				.round(baseXpReward * winner.getRates().getXpPlayerGainRate() * aggro.getDamage() / totalDamage);
		int dpPlayerReward = Math
				.round(baseDpReward * winner.getRates().getDpPlayerRate() * aggro.getDamage() / totalDamage);

		AbyssPointsService.addAp(winner, victim, apPlayerReward);
		if (PvPConfig.ENABLE_GP_REWARD) {
			AbyssPointsService.addGp(winner, 150);
		}
		winner.getCommonData().addExp(xpPlayerReward, RewardType.PVP_KILL, victim.getName());
		winner.getCommonData().addDp(dpPlayerReward);
		this.addKillFor(winner.getObjectId(), victim.getObjectId());
		return true;
	}

	private void notifyKillQuests(Player winner, Player victim) {
		if (winner.getRace() == victim.getRace()) {
			return;
		}

		List<Player> rewarded = new ArrayList<Player>();
		QuestPvpCreditSource creditSource;
		int worldId = victim.getWorldId();

		if (winner.isInGroup2()) {
			creditSource = QuestPvpCreditSource.GROUP;
			rewarded.addAll(winner.getPlayerGroup2().getOnlineMembers());
		} else if (winner.isInAlliance2()) {
			creditSource = QuestPvpCreditSource.ALLIANCE;
			rewarded.addAll(winner.getPlayerAllianceGroup2().getOnlineMembers());
		} else {
			creditSource = QuestPvpCreditSource.SOLO;
			rewarded.add(winner);
		}

		for (Player p : rewarded) {
			if (!MathUtil.isIn3dRange(p, victim, GroupConfig.GROUP_MAX_DISTANCE) || p.getLifeStats().isAlreadyDead()) {
				continue;
			}
			// 通知击杀任务 / notify Kill-Quests
			GameEngineServices.questEngine().onKillInWorld(new QuestEnv(victim, p, 0, 0), worldId,
				winner, creditSource);
			GameEngineServices.questEngine().onKillRanked(new QuestEnv(victim, p, 0, 0),
				victim.getAbyssRank().getRank(), winner, creditSource);
		}
		rewarded.clear();
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final PvpService instance = new PvpService();
	}
}
