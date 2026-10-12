package com.aionemu.gameserver.persistence.dao.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.persistence.dao.PlayerNpcFactionDao;
import com.aionemu.gameserver.persistence.entity.PlayerNpcFactionPo;
import com.aionemu.gameserver.persistence.mapper.PlayerNpcFactionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 NPC 势力声望数据访问实现。
 * Data access implementation for player NPC-faction data.
 * @author MrPoke
 */
@Slf4j
@Service
@Lazy
public class PlayerNpcFactionDaoImpl extends ServiceImpl<PlayerNpcFactionMapper, PlayerNpcFactionPo>
	implements PlayerNpcFactionDao {

	@Override
	public void loadNpcFactions(Player player) {
		NpcFactions factions = new NpcFactions(player);

		try {
			List<PlayerNpcFactionPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(PlayerNpcFactionPo::getPlayerId).eq(player.getObjectId()));
			for (PlayerNpcFactionPo po : rows) {
				ENpcFactionQuestState state = ENpcFactionQuestState.valueOf(po.getState());
				NpcFaction faction = new NpcFaction(po.getFactionId(), po.getTime(), po.getActive(), state,
					po.getQuestId());
				faction.setPersistentState(PersistentState.UPDATED);
				factions.addNpcFaction(faction);
			}

			player.setNpcFactions(factions);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.165b89344e7f", player.getObjectId(), " from DB"), e);
		}
	}

	@Override
	public void storeNpcFactions(Player player) {
		for (NpcFaction npcFaction : player.getNpcFactions().getNpcFactions()) {
			switch (npcFaction.getPersistentState()) {
				case NEW:
					insertNpcFaction(player.getObjectId(), npcFaction);
					break;
				case UPDATE_REQUIRED:
					updateNpcFaction(player.getObjectId(), npcFaction);
					break;
				default:
					continue;
			}
			npcFaction.setPersistentState(PersistentState.UPDATED);
		}
	}

	/**
	 * 插入 NPC 势力记录。
	 * Inserts an NPC faction record.
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @param faction 势力 / faction
	 */
	private void insertNpcFaction(int playerObjectId, NpcFaction faction) {
		try {
			PlayerNpcFactionPo po = new PlayerNpcFactionPo();
			po.setPlayerId(playerObjectId);
			po.setFactionId(faction.getId());
			po.setActive(faction.isActive());
			po.setTime(faction.getTime());
			po.setState(faction.getState().name());
			po.setQuestId(faction.getQuestId());
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e4fe3ce16d8e", playerObjectId, " from DB"), e);
		}
	}

	/**
	 * 更新 NPC 势力记录。
	 * Updates an NPC faction record.
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @param faction 势力 / faction
	 */
	private void updateNpcFaction(int playerObjectId, NpcFaction faction) {
		try {
			UpdateChain.of(PlayerNpcFactionPo.class)
				.set(PlayerNpcFactionPo::getActive, faction.isActive())
				.set(PlayerNpcFactionPo::getTime, faction.getTime())
				.set(PlayerNpcFactionPo::getState, faction.getState().name())
				.set(PlayerNpcFactionPo::getQuestId, faction.getQuestId())
				.where(PlayerNpcFactionPo::getPlayerId).eq(playerObjectId)
				.and(PlayerNpcFactionPo::getFactionId).eq(faction.getId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.494a98a72b0b", playerObjectId, " from DB"), e);
		}
	}
}
