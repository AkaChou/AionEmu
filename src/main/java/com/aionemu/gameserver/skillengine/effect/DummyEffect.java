package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 占位效果：不执行任何操作，用于数据占位。
 * Dummy effect: performs no action, used as a placeholder.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DummyEffect")
public class DummyEffect extends EffectTemplate {

	@Override
	public void applyEffect(Effect effect) {
	}
}
