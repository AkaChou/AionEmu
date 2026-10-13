package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.persistence.dao.WebRewardDao;
import com.aionemu.gameserver.persistence.entity.WebRewardPo;
import com.aionemu.gameserver.persistence.mapper.WebRewardMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Web 奖励服务数据访问实现。
 * Data access implementation for the web reward service.
 */
@Slf4j
@Service
@Lazy
public class WebRewardDaoImpl extends ServiceImpl<WebRewardMapper, WebRewardPo> implements WebRewardDao {

	/**
	 * 将奖励标记为未领取。
	 * Marks a reward as not yet received.
	 *
	 * @param unique 唯一奖励 ID / unique reward id
	 */
	@Override
	public void setUpdateDown(int unique) {
		try {
			UpdateChain.of(WebRewardPo.class)
				.set(WebRewardPo::getRewarded, 0)
				.where(WebRewardPo::getUnique).eq(unique)
				.update();
		} catch (Exception e) {
			log.error(I18n.get("log.865c33530603", unique), e);
		}
	}

	/**
	 * 将奖励标记为已领取。
	 * Marks a reward as received.
	 *
	 * @param unique 唯一奖励 ID / unique reward id
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean setUpdate(int unique) {
		try {
			return UpdateChain.of(WebRewardPo.class)
				.set(WebRewardPo::getRewarded, 1)
				.set(WebRewardPo::getReceived, QueryMethods.now())
				.where(WebRewardPo::getUnique).eq(unique)
				.update();
		} catch (Exception e) {
			log.error(I18n.get("log.248cee6e9874", unique), e);
			return false;
		}
	}

	/**
	 * 获取玩家尚未领取的 Web 奖励列表。
	 * Gets the list of unclaimed web rewards for a player.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 可用奖励条目 / available reward entries
	 */
	@Override
	public List<RewardEntryItem> getAvailable(int playerId) {
		List<RewardEntryItem> list = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.where(WebRewardPo::getItemOwner).eq(playerId)
				.and(WebRewardPo::getRewarded).eq(0))
				.forEach(po -> list.add(new RewardEntryItem(po.getUnique(), po.getItemId(), po.getItemCount())));
		} catch (Exception e) {
			log.error(I18n.get("log.5f1e9111257c", playerId), e);
		}
		return list;
	}

	/**
	 * 批量将奖励标记为已领取。
	 * Batch-marks rewards as received.
	 *
	 * @param ids 奖励唯一 ID 列表 / list of unique reward ids
	 */
	@Override
	public void uncheckAvailable(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}

		try {
			UpdateChain.of(WebRewardPo.class)
				.set(WebRewardPo::getRewarded, 1)
				.set(WebRewardPo::getReceived, QueryMethods.now())
				.in(WebRewardPo::getUnique, ids)
				.update();
		} catch (Exception e) {
			log.error(I18n.get("log.563cfc162266"), e);
		}
	}
}
