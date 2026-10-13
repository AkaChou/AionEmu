package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.persistence.dao.FriendDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.entity.FriendPo;
import com.aionemu.gameserver.persistence.mapper.FriendMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 好友列表数据访问实现。
 * Data access implementation for the friend list.
 */
@Slf4j
@Service
@Lazy
public class FriendDaoImpl extends ServiceImpl<FriendMapper, FriendPo> implements FriendDao {

	private final PlayerDao playerDao;

	public FriendDaoImpl(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}

	@Override
	public FriendList load(Player player) {
		List<Friend> friends = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(FriendPo::getPlayer).eq(player.getObjectId()))
				.forEach(po -> {
					PlayerCommonData pcd = playerDao.loadPlayerCommonData(po.getFriend());
					if (pcd != null) {
						friends.add(new Friend(pcd));
					}
				});
		} catch (Exception e) {
			log.error(I18n.get("log.76f2514feafb", player.getObjectId(), " from DB"), e);
		}
		return new FriendList(player, friends);
	}

	@Override
	public void addFriends(Player player, Player friend) {
		try {
			mapper.insert(row(player.getObjectId(), friend.getObjectId()));
			mapper.insert(row(friend.getObjectId(), player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3eacec776c80", player.getObjectId(), friend.getObjectId()), e);
		}
	}

	private static FriendPo row(int player, int friend) {
		FriendPo po = new FriendPo();
		po.setPlayer(player);
		po.setFriend(friend);
		return po;
	}

	@Override
	public boolean delFriends(int playerOid, int friendOid) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(FriendPo::getPlayer).eq(playerOid).and(FriendPo::getFriend).eq(friendOid));
			mapper.deleteByQuery(QueryWrapper.create()
				.where(FriendPo::getPlayer).eq(friendOid).and(FriendPo::getFriend).eq(playerOid));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.11aae36500f7", playerOid, friendOid), e);
			return false;
		}
	}

	@Override
	public void setFriendNote(int playerId, int friendId, String note) {
		// friends 表没有 note 列，原 SQL 恒抛 Unknown column 并返回 false（无实际效果）；此处改为无操作。
		// The friends table has no note column; the old SQL always threw Unknown column and returned false
		// (no effect), so this is now an explicit no-op.
	}
}
