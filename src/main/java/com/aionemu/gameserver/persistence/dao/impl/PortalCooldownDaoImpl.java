package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownItem;
import com.aionemu.gameserver.persistence.dao.PortalCooldownDao;
import com.aionemu.gameserver.persistence.entity.PortalCooldownPo;
import com.aionemu.gameserver.persistence.mapper.PortalCooldownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家传送门冷却数据访问实现。
 * Data access implementation for player portal cooldowns.
 */
@Slf4j
@Service
@Lazy
public class PortalCooldownDaoImpl extends ServiceImpl<PortalCooldownMapper, PortalCooldownPo>
	implements PortalCooldownDao {

	@Override
	public void loadPortalCooldowns(Player player) {
		Map<Integer, PortalCooldownItem> portalCoolDowns = new HashMap<>();

		try {
			long now = System.currentTimeMillis();
			List<PortalCooldownPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(PortalCooldownPo::getPlayerId).eq(player.getObjectId()));
			for (PortalCooldownPo po : rows) {
				long reuseTime = po.getReuseTime();
				if (reuseTime > now) {
					portalCoolDowns.put(po.getWorldId(),
						new PortalCooldownItem(po.getWorldId(), po.getEntryCount(), reuseTime));
				}
			}

			player.getPortalCooldownList().setPortalCoolDowns(portalCoolDowns);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bd26489b30c6", player.getObjectId()), e);
		}
	}

	@Override
	public void storePortalCooldowns(Player player) {
		deletePortalCooldowns(player);

		Map<Integer, PortalCooldownItem> portalCoolDowns = player.getPortalCooldownList().getPortalCoolDowns();
		if (portalCoolDowns == null || portalCoolDowns.isEmpty()) {
			return;
		}

		try {
			long now = System.currentTimeMillis();
			for (Map.Entry<Integer, PortalCooldownItem> entry : portalCoolDowns.entrySet()) {
				PortalCooldownItem item = entry.getValue();
				long reuseTime = item.getCooldown();

				if (reuseTime < now) {
					continue;
				}

				mapper.upsert(player.getObjectId(), entry.getKey(), reuseTime, item.getEntryCount());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3f6c79bd16dd", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部传送门冷却。
	 * Deletes all portal cooldowns for a player.
	 * @param player 玩家 / player
	 */
	private void deletePortalCooldowns(Player player) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(PortalCooldownPo::getPlayerId).eq(player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4b9e8239bcf5", player.getObjectId()), e);
		}
	}
}
