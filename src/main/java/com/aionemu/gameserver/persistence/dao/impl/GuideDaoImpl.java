package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;
import com.aionemu.gameserver.persistence.dao.GuideDao;
import com.aionemu.gameserver.persistence.entity.GuidePo;
import com.aionemu.gameserver.persistence.mapper.GuideMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 新手引导数据访问实现。
 * Data access implementation for player guides.
 * @author xTz
 */
@Slf4j
@Service
@Lazy
public class GuideDaoImpl extends ServiceImpl<GuideMapper, GuidePo> implements GuideDao {

	@Override
	public void deleteGuide(int guide_id) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(GuidePo::getGuideId).eq(guide_id));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.257617627810", guide_id), e);
		}
	}

	@Override
	public List<Guide> loadGuides(int playerId) {
		List<Guide> guides = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(GuidePo::getPlayerId).eq(playerId))
				.forEach(po -> guides.add(new Guide(po.getGuideId(), po.getPlayerId(), po.getTitle())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.040d5371000d", playerId, " from DB"), e);
		}
		return guides;
	}

	@Override
	public Guide loadGuide(int player_id, int guide_id) {
		try {
			// 引导表以 guide_id 为自增主键，玩家与引导的组合唯一由业务保证。
			// guide_id is the auto-increment key; the player/guide pair is unique by business rule only.
			GuidePo po = mapper.selectOneByQuery(QueryWrapper.create()
				.where(GuidePo::getGuideId).eq(guide_id)
				.and(GuidePo::getPlayerId).eq(player_id));
			return po == null ? null : new Guide(guide_id, player_id, po.getTitle());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5857d08149b7", player_id, " from DB"), e);
			return null;
		}
	}

	@Override
	public void saveGuide(int guide_id, Player player, String title) {
		try {
			GuidePo po = new GuidePo();
			po.setGuideId(guide_id);
			po.setPlayerId(player.getObjectId());
			po.setTitle(title);
			// 显式写入业务侧自增主键，故用 insertWithPk（insert 会丢弃 Auto 主键）。
			// The business-side auto id is written explicitly, hence insertWithPk (insert drops Auto keys).
			mapper.insertWithPk(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9ad0924840e7", player), e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		try {
			List<GuidePo> rows = mapper.selectListByQuery(QueryWrapper.create().select(GuidePo::getGuideId));
			int[] result = new int[rows.size()];
			for (int i = 0; i < rows.size(); i++) {
				result[i] = rows.get(i).getGuideId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4c2e4d8e0a12"), e);
			return new int[0];
		}
	}
}
