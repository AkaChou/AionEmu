package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.house.PlayerHouseBid;
import com.aionemu.gameserver.persistence.dao.HouseBidDao;
import com.aionemu.gameserver.persistence.entity.HouseBidPo;
import com.aionemu.gameserver.persistence.mapper.HouseBidMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋竞拍数据访问的 MyBatis-Flex 实现。
 * MyBatis-Flex implementation of the house bids data access layer.
 */
@Slf4j
@Service
@Lazy
public class HouseBidDaoImpl extends ServiceImpl<HouseBidMapper, HouseBidPo> implements HouseBidDao {

	@Override
	public Set<PlayerHouseBid> loadBids() {
		Set<PlayerHouseBid> bids = new HashSet<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.orderBy(HouseBidPo::getHouseId, true)
				.orderBy(HouseBidPo::getBid, false))
				.forEach(po -> bids.add(
					new PlayerHouseBid(po.getPlayerId(), po.getHouseId(), po.getBid(), po.getBidTime())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ff14dcd21c64"), e);
		}
		return bids;
	}

	@Override
	public boolean addBid(int playerId, int houseId, long bidOffer, Timestamp time) {
		try {
			HouseBidPo po = new HouseBidPo();
			po.setPlayerId(playerId);
			po.setHouseId(houseId);
			po.setBid(bidOffer);
			po.setBidTime(time);
			return mapper.insert(po) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f1c5d89c6500", playerId, houseId), e);
			return false;
		}
	}

	@Override
	public void changeBid(int playerId, int houseId, long newBidOffer, Timestamp time) {
		try {
			if (mapper.changeBid(playerId, houseId, newBidOffer, time) == 0) {
				addBid(playerId, houseId, newBidOffer, time);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.68c1f0c325d4", playerId, houseId), e);
		}
	}

	@Override
	public void deleteHouseBids(int houseId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(HouseBidPo::getHouseId).eq(houseId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.913c026c08a6", houseId), e);
		}
	}
}
