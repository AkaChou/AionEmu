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

	private final List<IStatFunction> will = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / point
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static Will getInstance() {
		ObjectProvider<Will> provider = instanceProvider;
		Will provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("Will 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param provider 提供器 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<Will> provider) {
		instanceProvider = provider;
	}
}
