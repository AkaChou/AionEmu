package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 疾跑飞行值消耗减免效果：作为 Buff 壳，降低疾跑时的 FP 消耗。
 * Sprint FP reduce effect: buff shell that reduces FP cost while sprinting.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SprintFpReduceEffect")
public class SprintFpReduceEffect extends BuffEffect {

	@Override
	public void calculate(Effect effect) {
		effect.setSprintFpReduce(true);
		effect.addSucessEffect(this);
	}
}
