package com.aionemu.gameserver.services.conquestservice;

import com.aionemu.gameserver.model.conquest.ConquestLocation;
import com.aionemu.gameserver.model.conquest.ConquestStateType;

/**
 * 征服/供奉默认实现：切入 CONQUEST 并初始化 BOSS / 回到 PEACE。
 * back to PEACE. / back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Offering extends ConquestOffering<ConquestLocation> {

	/**
	 * 绑定征服地点。
	 * Binds the conquest location.
	 *
	 * location
	 */
	public Offering(ConquestLocation conquest) {
		super(conquest);
	}

	/**
	 * 激活供奉、刷新 CONQUEST 刷怪并初始化 BOSS。
	 * Activates offering, spawns CONQUEST entities and inits the boss.
	 */
	@Override
	public void startConquest() {
		getConquestLocation().setActiveConquest(this);
		despawn();
		spawn(ConquestStateType.CONQUEST);
		initConquestBoss();
	}

	/**
	 * 结束供奉：移除监听并恢复 PEACE。
	 * Ends offering: removes listeners and restores PEACE.
	 */
	@Override
	public void stopConquest() {
		getConquestLocation().setActiveConquest(null);
		rmvConquestBossListener();
		despawn();
		spawn(ConquestStateType.PEACE);
	}
}
