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
 * 创造力属性：意志，按等级变更意志加成。
 * Creativity stat: Will; applies will bonus by rank.
 */
public class Will implements StatOwner {

	private static volatile ObjectProvider<Will> instanceProvider;

	private List<IStatFunction> will = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * 玩家 / player
	 * point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			will.clear();
			player.getGameStats().endEffect(this);
			will.add(new StatAddFunction(StatEnum.HWIL, point, true));
			player.getGameStats().addEffect(this, will);
		} else if (point == 0) {
			will.clear();
			will.add(new StatAddFunction(StatEnum.HWIL, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static Will getInstance() {
		ObjectProvider<Will> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<Will> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Will INSTANCE = new Will();
	}
}
