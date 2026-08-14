package com.aionemu.gameserver.services.beritraservice;

import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.beritra.BeritraStateType;

/**
 * 贝尔特拉入侵默认实现：切入 INVASION / 回到 PEACE。
 * Default Beritra invasion implementation: switches to INVASION / back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Invade extends BeritraInvasion<BeritraLocation> {

	/**
	 * 绑定入侵地点。
	 * Binds the invasion location.
	 *
	 * @param beritra 入侵地点 / invasion location
	 */
	public Invade(BeritraLocation beritra) {
		super(beritra);
	}

	/**
	 * 激活入侵并刷新 INVASION 刷怪。
	 * Activates invasion and spawns INVASION entities.
	 */
	@Override
	public void startBeritraInvasion() {
		getBeritraLocation().setActiveBeritra(this);
		despawn();
		spawn(BeritraStateType.INVASION);
	}

	/**
	 * 结束入侵并恢复 PEACE 刷怪。
	 * Ends invasion and restores PEACE spawns.
	 */
	@Override
	public void stopBeritraInvasion() {
		getBeritraLocation().setActiveBeritra(null);
		despawn();
		spawn(BeritraStateType.PEACE);
	}
}
