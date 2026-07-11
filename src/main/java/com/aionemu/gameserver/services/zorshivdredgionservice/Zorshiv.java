package com.aionemu.gameserver.services.zorshivdredgionservice;

import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;

/**
 * 佐尔希夫挖掘舰默认实现：切入 LANDING / 回到 PEACE。
 * back to PEACE. / back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Zorshiv extends ZorshivDredgion<ZorshivDredgionLocation> {

	/**
	 * 绑定挖掘舰地点。
	 * Binds the dredgion location.
	 *
	 * location
	 */
	public Zorshiv(ZorshivDredgionLocation zorshivDredgion) {
		super(zorshivDredgion);
	}

	/**
	 * 激活活动并刷新 LANDING 刷怪。
	 * Activates the event and spawns LANDING entities.
	 */
	@Override
	public void startZorshivDredgion() {
		getZorshivDredgionLocation().setActiveZorshivDredgion(this);
		despawn();
		spawn(ZorshivDredgionStateType.LANDING);
	}

	/**
	 * 结束活动并恢复 PEACE 刷怪。
	 * Ends the event and restores PEACE spawns.
	 */
	@Override
	public void stopZorshivDredgion() {
		getZorshivDredgionLocation().setActiveZorshivDredgion(null);
		despawn();
		spawn(ZorshivDredgionStateType.PEACE);
	}
}
