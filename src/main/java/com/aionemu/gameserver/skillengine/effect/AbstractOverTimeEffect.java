package com.aionemu.gameserver.skillengine.effect;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 持续类效果基类：按 checktime 周期触发 onPeriodicAction（DoT/HoT 等）。
 * Base for over-time effects: runs onPeriodicAction on checktime intervals (DoT/HoT, etc.).
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractOverTimeEffect")
@Slf4j
public abstract class AbstractOverTimeEffect extends EffectTemplate {

	@XmlAttribute(required = true)
	protected int checktime;
	@XmlAttribute
	protected boolean percent;

	/**
	 * 返回效果数值。
	 * Returns the effect value.
	 *
	 * @return 效果数值 / value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 启动持续效果（无异常状态）。
	 * Starts the over-time effect without an abnormal state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		this.startEffect(effect, null);
	}

	/**
	 * 启动持续效果：可选设置异常状态，并按 checktime 调度周期任务。
	 * Starts the over-time effect: optional abnormal state, schedules periodic task by checktime.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @param abnormal 异常状态，可为 null / abnormal state, may be null
	 */
	public void startEffect(final Effect effect, AbnormalState abnormal) {
		final Creature effected = effect.getEffected();

		if (abnormal != null) {
			effect.setAbnormal(abnormal.getId());
			effected.getEffectController().setAbnormal(abnormal.getId());
		}

		if (checktime == 0) {
			return;
		}
		try {
			Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					onPeriodicAction(effect);
				}
			}, checktime, checktime);
			effect.setPeriodicTask(task, position);
		} catch (Exception e) {
			log.warn(I18n.get("log.3cb547ba6faf", effect.getSkillId(), e));
		}
	}

	/**
	 * 结束持续效果并清除可选异常状态。
	 * Ends the over-time effect and clears the optional abnormal state.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @param abnormal 异常状态，可为 null / abnormal state, may be null
	 */
	public void endEffect(Effect effect, AbnormalState abnormal) {
		if (abnormal != null) {
			effect.getEffected().getEffectController().unsetAbnormal(abnormal.getId());
		}
	}
}
