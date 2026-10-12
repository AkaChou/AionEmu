package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.persistence.dao.PlayerSkillSkinDao;
import com.aionemu.gameserver.persistence.entity.PlayerSkillSkinPo;
import com.aionemu.gameserver.persistence.mapper.PlayerSkillSkinMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能皮肤列表数据访问实现。
 * Data access implementation for player skill skin list persistence.
 */
@Slf4j
@Service
@Lazy
public class PlayerSkillSkinDaoImpl extends ServiceImpl<PlayerSkillSkinMapper, PlayerSkillSkinPo>
	implements PlayerSkillSkinDao {

	@Override
	public SkillSkinList loadSkillSkinList(final int playerId) {
		final SkillSkinList tl = new SkillSkinList();

		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerSkillSkinPo::getPlayerId).eq(playerId))
				.forEach(po -> tl.addEntry(intValue(po.getSkinId()), intValue(po.getRemaining()),
					intValue(po.getActive())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7cb3e1e1c2aa", playerId), e);
		}

		return tl;
	}

	@Override
	public void storeSkillSkins(Player player, SkillSkin entry) {
		try {
			mapper.upsert(player.getObjectId(), entry.getId(), entry.getExpireTime(), entry.getIsActive());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b49a1a09f26e", player.getObjectId()), e);
		}
	}

	@Override
	public boolean setActive(final int playerObjId, final int skinId) {
		try {
			return UpdateChain.of(PlayerSkillSkinPo.class)
				.set(PlayerSkillSkinPo::getActive, 1)
				.where(PlayerSkillSkinPo::getPlayerId).eq(playerObjId)
				.and(PlayerSkillSkinPo::getSkinId).eq(skinId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1c834a3bbe53", playerObjId, skinId), e);
			return false;
		}
	}

	@Override
	public void setDeactive(final int playerObjId, final int skinId) {
		try {
			UpdateChain.of(PlayerSkillSkinPo.class)
				.set(PlayerSkillSkinPo::getActive, 0)
				.where(PlayerSkillSkinPo::getPlayerId).eq(playerObjId)
				.and(PlayerSkillSkinPo::getSkinId).eq(skinId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.467b5e662059", playerObjId, skinId), e);
		}
	}

	@Override
	public void removeSkillSkin(int playerId, int skinId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerSkillSkinPo::getPlayerId).eq(playerId)
				.and(PlayerSkillSkinPo::getSkinId).eq(skinId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.84df7864194f", playerId), e);
		}
	}

	/**
	 * 可空整数列的空值归一化，保持原 rs.getInt 读取语义（NULL 视为 0）。
	 * Normalizes nullable integer columns on read, keeping the legacy rs.getInt semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的整数 / normalized int
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
