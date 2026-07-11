package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 混沌竞技场副本事件处理器。
 * Instance event handler for Arena Of Chaos.
 *
 * @author Encom
 */

@InstanceID(300350000)
public class ArenaOfChaosInstance extends PvPArenaInstance
{
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		killBonus = 1000;
		deathFine = -125;
		super.onInstanceCreate(instance);
	}
	
	/**
	 * 玩家采集完成时处理。
	 * Handle player gathering completion.
	 *
	 * 玩家 / player
	 * gatherable
	 */
	@Override
	public void onGather(Player player, Gatherable gatherable) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		getPlayerReward(player.getObjectId()).addPoints(1250);
		sendPacket();
		int nameId = gatherable.getObjectTemplate().getNameId();
		DescriptionId name = new DescriptionId(nameId * 2 + 1);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, name, 1250));
	}
	
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	@Override
	protected void reward() {
		int totalPoints = instanceReward.getTotalPoints();
		int size = instanceReward.getInstanceRewards().size();
		float totalScoreAP = (1.0f * size) * 100;
		float totalScoreGP = (1.0f * size) * 100;
		float totalScoreCrucible = (0.01f * size) * 100;
		float totalScoreCourage = (0.01f * size) * 100;
		float totalScoreInfinity = (0.01f * size) * 100;
		float rankingRate = 0;
		if (size > 1) {
			rankingRate = (0.077f * (8 - size));
		}
		float totalRankingAP = 750 - 750 * rankingRate;
		float totalRankingGP = 250 - 250 * rankingRate;
		float totalRankingCrucible = 500 - 500 * rankingRate;
		float totalRankingCourage = 100 - 100 * rankingRate;
		float totalRankingInfinity = 100 - 100 * rankingRate;
		for (InstancePlayerReward playerReward : instanceReward.getInstanceRewards()) {
			PvPArenaPlayerReward reward = (PvPArenaPlayerReward) playerReward;
			if (!reward.isRewarded()) {
				float playerRate = 1;
				Player player = instance.getPlayer(playerReward.getOwner());
				if (player != null) {
					playerRate = player.getRates().getChaosRewardRate();
				}
				int score = reward.getScorePoints();
				float scoreRate = ((float) score / (float) totalPoints);
				int rank = instanceReward.getRank(score);
				float percent = reward.getParticipation();
				float generalRate = 0.167f + rank * 0.095f;
				int basicAP = 100;
				int basicGP = 100;
				float rankingAP = totalRankingAP;
				float rankingGP = totalRankingGP;
				if (rank > 0) {
					rankingAP = rankingAP - rankingAP * generalRate;
					rankingGP = rankingGP - rankingGP * generalRate;
				}
				int scoreAP = (int) (totalScoreAP * scoreRate);
				int scoreGP = (int) (totalScoreGP * scoreRate);
				// <欧比斯点数> / <Abyss Points>
				basicAP *= percent;
				rankingAP *= percent;
				rankingAP *= playerRate;
				reward.setBasicAP(basicAP);
				reward.setRankingAP((int) rankingAP);
				reward.setScoreAP(scoreAP);
				// <荣耀点数> / <Glory Points>
				basicGP *= percent;
				rankingGP *= percent;
				rankingGP *= playerRate;
				reward.setBasicGP((int)(basicGP * 0.1));
				reward.setRankingGP((int) (rankingGP * 0.1));
				reward.setScoreGP((int)(scoreGP * 0.1));
				int basicCrI = 0;
				basicCrI *= percent;
				float rankingCrI = totalRankingCrucible;
				if (rank > 0) {
					rankingCrI = rankingCrI - rankingCrI * generalRate;
				}
				rankingCrI *= percent;
				rankingCrI *= playerRate;
				int scoreCrI = (int) (totalScoreCrucible * scoreRate);
				reward.setBasicCrucible(basicCrI);
				reward.setRankingCrucible((int) rankingCrI);
				reward.setScoreCrucible(scoreCrI);
				int basicCoI = 0;
				basicCoI *= percent;
				float rankingCoI = totalRankingCourage;
				if (rank > 0) {
					rankingCoI = rankingCoI - rankingCoI * generalRate;
				}
				rankingCoI *= percent;
				rankingCoI *= playerRate;
				int scoreCoI = (int) (totalScoreCourage * scoreRate);
				reward.setBasicCourage(basicCoI);
				reward.setRankingCourage((int) rankingCoI);
				reward.setScoreCourage(scoreCoI);
				// 5.1「无限试炼徽章」可从新「高阶守护者」竞技场获得。 / 5.1 "Crucible Insignia of Infinity" can be obtained from new "ArchDaeva" Arena
				int basicCiI = 0;
				basicCiI *= percent;
				float rankingCiI = totalRankingInfinity;
				if (rank > 0) {
					rankingCiI = rankingCiI - rankingCiI * generalRate;
				}
				rankingCiI *= percent;
				rankingCiI *= playerRate;
				int scoreCiI = (int) (totalScoreInfinity * scoreRate);
				reward.setBasicInfinity(basicCiI);
				reward.setRankingInfinity((int) rankingCiI);
				reward.setScoreInfinity(scoreCiI);
				if (instanceReward.canRewardOpportunityToken(reward)) {
					reward.setOpportunity(4);
				} if (rank < 2) {
					reward.setGloryTicket(1);
				}
			}
		}
		super.reward();
	}
}