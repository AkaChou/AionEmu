package com.aionemu.gameserver.services.player.CreativityPanel.stats;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 创造力属性：精准，按等级变更精准加成。
 * Creativity stat: Precision; applies precision bonus by rank.
 */
public class Precision implements StatOwner {

    /**
     * -- SETTER --
     *  setInstanceProvider 方法。
     *  setInstanceProvider method.
     * 提供器 / provider
     */
    @Setter
    private static volatile ObjectProvider<Precision> instanceProvider;

	private final List<IStatFunction> accuracy = new ArrayList<>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 * @param player 玩家 / player
	 * @param point 点数 / point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			accuracy.clear();
			player.getGameStats().endEffect(this);
			accuracy.add(new StatAddFunction(StatEnum.HAGI, point, true));
			player.getGameStats().addEffect(this, accuracy);
		} else if (point == 0) {
			accuracy.clear();
			accuracy.add(new StatAddFunction(StatEnum.HAGI, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static Precision getInstance() {
		ObjectProvider<Precision> provider = instanceProvider;
		Precision provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("Precision 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
