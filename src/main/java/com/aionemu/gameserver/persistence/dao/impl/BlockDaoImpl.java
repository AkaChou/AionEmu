package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.persistence.dao.BlockDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.entity.BlockPo;
import com.aionemu.gameserver.persistence.mapper.BlockMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家黑名单数据访问实现。
 * Data access implementation for the player block list.
 */
@Slf4j
@Service
@Lazy
public class BlockDaoImpl extends ServiceImpl<BlockMapper, BlockPo> implements BlockDao {

	private final PlayerDao playerDao;

	public BlockDaoImpl(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}

	@Override
	public BlockList load(Player player) {
		Map<Integer, BlockedPlayer> list = new HashMap<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(BlockPo::getPlayer).eq(player.getObjectId()))
				.forEach(po -> {
					int blockedOid = po.getBlockedPlayer();
					PlayerCommonData pcd = playerDao.loadPlayerCommonData(blockedOid);
					if (pcd == null) {
						log.error(I18n.get("log.53686495edb4", player.getName(), blockedOid));
					} else {
						list.put(blockedOid, new BlockedPlayer(pcd, po.getReason()));
					}
				});
		} catch (Exception e) {
			log.error(I18n.get("log.54a6187d47e3", player.getObjectId()), e);
		}
		return new BlockList(list);
	}

	@Override
	public boolean addBlockedUser(int playerObjId, int objIdToBlock, String reason) {
		try {
			return mapper.insert(row(playerObjId, objIdToBlock, reason)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5b7ace3c98ed", playerObjId, objIdToBlock), e);
			return false;
		}
	}

	@Override
	public boolean delBlockedUser(int playerObjId, int objIdToDelete) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create()
				.where(BlockPo::getPlayer).eq(playerObjId)
				.and(BlockPo::getBlockedPlayer).eq(objIdToDelete)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4721ed6a731b", playerObjId, objIdToDelete), e);
			return false;
		}
	}

	@Override
	public boolean setReason(int playerObjId, int blockedPlayerObjId, String reason) {
		try {
			return mapper.updateReason(playerObjId, blockedPlayerObjId, reason) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.efb77a6431a9", playerObjId, blockedPlayerObjId), e);
			return false;
		}
	}

	private static BlockPo row(int player, int blockedPlayer, String reason) {
		BlockPo po = new BlockPo();
		po.setPlayer(player);
		po.setBlockedPlayer(blockedPlayer);
		po.setReason(reason);
		return po;
	}
}
