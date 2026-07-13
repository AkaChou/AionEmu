package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 印记效果：在目标上挂载可被爆发消耗的印记。
 * Signet effect: places a signet on the target that can later be consumed by a burst.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignetEffect")
public class SignetEffect extends EffectTemplate {
	@XmlAttribute(name = "signet_type", required = true)
	private int signetType;

	@XmlAttribute(name = "signet_level", required = true)
	private int signetLevel;

	/**
	 * 将效果加入目标的效果控制器。
	 * Adds this effect to the target effect controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 直接标记本效果成功。
	 * Always marks this effect successful.
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	public int getSignetType() {
		return signetType;
	}

	public int getSignetLevel() {
		return signetLevel;
	}
}
