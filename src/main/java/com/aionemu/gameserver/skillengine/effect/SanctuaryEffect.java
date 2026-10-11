package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 圣域效果：占位模板，生命周期方法暂无具体逻辑。
 * Sanctuary effect: placeholder template with empty lifecycle hooks.
 */
public class SanctuaryEffect extends EffectTemplate {

	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}
}
