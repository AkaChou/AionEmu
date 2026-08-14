package com.aionemu.gameserver.services.player.CreativityPanel.stats;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 创造力属性：体力，按等级变更体力加成。
 * Creativity stat: Health; applies health bonus by rank.
 */
public class Health implements StatOwner {

	private static volatile ObjectProvider<Health> instanceProvider;

	private List<IStatFunction> health = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			health.clear();
			player.getGameStats().endEffect(this);
			health.add(new StatAddFunction(StatEnum.HVIT, point, true));
			player.getGameStats().addEffect(this, health);
		} else if (point == 0) {
			health.clear();
			health.add(new StatAddFunction(StatEnum.HVIT, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static Health getInstance() {
		ObjectProvider<Health> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param provider 提供器 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<Health> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Health INSTANCE = new Health();
	}
}
