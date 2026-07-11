package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 授权强化加成效果：标记运行中效果启用授权（Authorize）提升。
 * Authorize boost effect: marks the runtime effect to enable authorize-rate increase.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AuthorizeBoostEffect")
public class AuthorizeBoostEffect extends BuffEffect {

	/**
	 * 标记本效果启用授权加成并记为成功。
	 * Marks authorize boost and records this effect as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		effect.setAuthorizeBoost(true);
		effect.addSucessEffect(this);
	}
}
