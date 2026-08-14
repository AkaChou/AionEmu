package com.aionemu.gameserver.services.player;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate;
import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate.AtreianBestiaryAchievementTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_BESTIARY;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_BESTIARY_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 艾特利亚图鉴服务，记录击杀与升级进度。
 * Atreian bestiary service tracking kill and level-up progress.
 *
 * @author Ranastic
 */

@Slf4j

public class AtreianBestiaryService {
	private static volatile ObjectProvider<AtreianBestiaryService> instanceProvider;

	/**
	 * 玩家登录时同步状态。
	 * Syncs state when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_ATREIAN_BESTIARY_LIST(player));
	}

	/**
	 * 击杀时处理：更新图鉴击杀计数并检查成就奖励。
	 * Handles a kill event: updates the bestiary kill count and checks achievement rewards.
	 *
	 * @param player 玩家 / player
	 * @param npcId 被击杀 NPC ID / killed NPC id
	 */
	public void onKill(Player player, int npcId) {
		AtreianBestiaryTemplate template = DataManager.ATREIAN_BESTIARY.getAtreianBestiaryTemplateByNpcId(npcId);
		if (template == null || template.getNpcIds() == null) {
			return;
		}
		for (int tmpNpcId : template.getNpcIds()) {
			if (npcId == tmpNpcId) {
				int killCount = DAOManager.getDAO(PlayerABDAO.class).getKillCountById(player.getObjectId(),
						template.getId());
				byte currentLvl = (byte) DAOManager.getDAO(PlayerABDAO.class).getLevelById(player.getObjectId(),
						template.getId());
				killCount++;
				int claimReward = 0;
				for (AtreianBestiaryAchievementTemplate at : template.getAtreianBestiaryAchievementTemplate()) {
					if (at == null) {
						return;
					}
					if (killCount == at.getKillCondition()) {
						claimReward = (currentLvl + 1);
					}
				}
				PacketSendUtility.sendPacket(player,
						new SM_ATREIAN_BESTIARY(template.getId(), killCount, currentLvl, claimReward));
				player.getAtreianBestiary().add(player, template.getId(), killCount, currentLvl, claimReward);
			}
		}
	}

	/**
	 * 升级时处理。
	 * Handles level-up.
	 *
	 * @param player 玩家 / player
	 * @param id ID / id
	 */
	public void onLvlUp(Player player, int id) {
		AtreianBestiaryTemplate template = DataManager.ATREIAN_BESTIARY.getAtreianBestiaryTemplate(id);
		if (template == null) {
			return;
		}
		int killCount = DAOManager.getDAO(PlayerABDAO.class).getKillCountById(player.getObjectId(), id);
		byte currentLvl = (byte) DAOManager.getDAO(PlayerABDAO.class).getLevelById(player.getObjectId(), id);
		int isClaimReward = DAOManager.getDAO(PlayerABDAO.class).getClaimRewardById(player.getObjectId(), id);
		currentLvl = (byte) (currentLvl + 1);
		long exp = 0;
		for (AtreianBestiaryAchievementTemplate at : template.getAtreianBestiaryAchievementTemplate()) {
			if (killCount == at.getKillCondition()) {
				exp = at.getRewardExp();
			}
		}
		player.getCommonData().addExp(exp, RewardType.MONSTER_BOOK);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GET_EXP2(exp));
		PacketSendUtility.sendPacket(player, new SM_ATREIAN_BESTIARY(id, killCount, currentLvl, isClaimReward));
		player.getAtreianBestiary().add(player, id, killCount, currentLvl, isClaimReward);
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static AtreianBestiaryService getInstance() {
		ObjectProvider<AtreianBestiaryService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * 设置实例提供者（Spring 注入）。
	 * Sets the instance provider (Spring injection).
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AtreianBestiaryService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {
		private static final AtreianBestiaryService INSTANCE = new AtreianBestiaryService();
	}
}
