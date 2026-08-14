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
 * 创造力属性：力量，按等级变更力量加成。
 * Creativity stat: Power; applies power bonus by rank.
 */
public class Power implements StatOwner {

	private static volatile ObjectProvider<Power> instanceProvider;

	private List<IStatFunction> power = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			power.clear();
			player.getGameStats().endEffect(this);
			power.add(new StatAddFunction(StatEnum.HSTR, point, true));
			player.getGameStats().addEffect(this, power);
		} else if (point == 0) {
			power.clear();
			power.add(new StatAddFunction(StatEnum.HSTR, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static Power getInstance() {
		ObjectProvider<Power> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<Power> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Power INSTANCE = new Power();
	}
}
