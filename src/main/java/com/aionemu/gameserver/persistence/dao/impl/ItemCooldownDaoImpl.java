package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.persistence.dao.ItemCooldownDao;
import com.aionemu.gameserver.persistence.entity.ItemCooldownPo;
import com.aionemu.gameserver.persistence.mapper.ItemCooldownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 物品冷却数据访问实现。
 * Data access implementation for item cooldowns.
 * @author ATracer
 */
@Slf4j
@Service
@Lazy
public class ItemCooldownDaoImpl extends ServiceImpl<ItemCooldownMapper, ItemCooldownPo> implements ItemCooldownDao {

	@Override
	public void loadItemCooldowns(final Player player) {
		try {
			long now = System.currentTimeMillis();
			for (ItemCooldownPo po : mapper.selectListByQuery(
					QueryWrapper.create().where(ItemCooldownPo::getPlayerId).eq(player.getObjectId()))) {
				long reuseTime = po.getReuseTime();
				if (reuseTime > now) {
					player.addItemCoolDown(po.getDelayId(), reuseTime, po.getUseDelay());
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a442a442976d", player.getObjectId()), e);
		}
		player.getEffectController().broadCastEffects();
	}

	@Override
	public void storeItemCooldowns(Player player) {
		deleteItemCooldowns(player);

		Map<Integer, ItemCooldown> itemCoolDowns = player.getItemCoolDowns();
		if (itemCoolDowns == null || itemCoolDowns.isEmpty()) {
			return;
		}

		List<Map.Entry<Integer, ItemCooldown>> validCooldowns = new ArrayList<>();
		long threshold = System.currentTimeMillis() + 30000;
		for (Map.Entry<Integer, ItemCooldown> entry : itemCoolDowns.entrySet()) {
			ItemCooldown cooldown = entry.getValue();
			if (cooldown != null && cooldown.getReuseTime() > threshold) {
				validCooldowns.add(entry);
			}
		}

		if (validCooldowns.isEmpty()) {
			return;
		}

		try {
			for (Map.Entry<Integer, ItemCooldown> entry : validCooldowns) {
				ItemCooldown cooldown = entry.getValue();
				mapper.upsert(player.getObjectId(), entry.getKey(), cooldown.useDelay(), cooldown.getReuseTime());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.77f5a5e723bf", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部物品冷却。
	 * Deletes all item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	private void deleteItemCooldowns(final Player player) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(ItemCooldownPo::getPlayerId).eq(player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8c3c2893f6d3", player.getObjectId()), e);
		}
	}
}
