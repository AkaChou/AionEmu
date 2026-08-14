package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.math.MathObject;
import com.aionemu.gameserver.model.gameobjects.math.MathObjectType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 数学区域对象观察者：进入范围后周期施放技能。
 * Math-object observer: periodically applies skills while inside range.
 */
public class MathObjectObserver extends ActionObserver {
	/** 被观察生物 / Observed creature */
	private final Creature creature;
	/** 数学区域对象 / Math object */
	private final MathObject mathObject;
	/** 对象行为类型 / Math object action type */
	private final MathObjectType type;
	/** 周期任务 / Periodic schedule */
	private ScheduledFuture<?> shedules;
	/** 技能模板 / Skill template */
	private SkillTemplate template;

	/**
	 * 空构造（字段为 null/默认）。
	 * Empty constructor (fields null/default).
	 */
	public MathObjectObserver() {
		super(ObserverType.MOVE);
		this.creature = null;
		this.mathObject = null;
		this.type = MathObjectType.SKILL_USE;
	}

	/**
	 * @param mathObject 数学区域对象 / math object
	 * @param creature 被观察生物 / observed creature
	 * @param type 行为类型 / action type
	 */
	public MathObjectObserver(MathObject mathObject, Creature creature, MathObjectType type) {
		super(ObserverType.MOVE);
		this.creature = creature;
		this.mathObject = mathObject;
		this.type = type;
		this.template = DataManager.SKILL_DATA.getSkillTemplate(mathObject.getSkillId());
	}

	@Override
	public void moved() {
		if (this.creature == null || this.mathObject == null) {
			return;
		}
		if (this.creature instanceof Player && !((Player) this.creature).isOnline()) {
			this.clearShedules();
			return;
		}
		if (this.creature.getLifeStats().isAlreadyDead()) {
			this.clearShedules();
			return;
		}
		double distance = MathUtil.getDistance(this.mathObject.getMaster(), this.creature);
		switch (this.type) {
		case SKILL_USE: {
			if (this.creature.getVisualState() == 20) {
				return;
			}
			if (distance >= this.mathObject.getMinRange() && distance <= this.mathObject.getMaxRange()) {
				if (this.shedules != null) {
					return;
				}
				this.onActionEvent();
				this.shedulesEvent();
				break;
			}
			this.clearShedules();
		}
		default:
			break;
		}
	}

	/**
	 * 按技能/对象持续时间启动周期触发任务。
	 * Start a fixed-rate task based on skill/object duration.
	 */
	private void shedulesEvent() {
		int delay = this.template != null && this.template.getDuration() >= 1000 ? this.template.getDuration()
				: (this.mathObject.getDuration() >= 1000 ? this.mathObject.getDuration() : 1000);
		this.shedules = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (!MathObjectObserver.this.mathObject.isSpawned()) {
					MathObjectObserver.this.creature.getObserveController().removeObserver(MathObjectObserver.this);
					MathObjectObserver.this.clearShedules();
					return;
				}
				MathObjectObserver.this.onActionEvent();
			}
		}, delay, delay);
	}

	/**
	 * 按类型执行一次动作（如直接施加技能）。
	 * Execute one action by type (e.g. apply skill directly).
	 */
	private void onActionEvent() {
		switch (this.type) {
		case SKILL_USE: {
			if (this.creature.getEffectController().hasAbnormalEffect(this.mathObject.getSkillId()))
				break;
			if (this.template == null) {
				return;
			}
			GameEngineServices.skillEngine().applyEffectDirectly(this.mathObject.getSkillId(), this.mathObject.getMaster(),
					this.creature, this.template.getDuration());
			break;
		}
		default:
			break;
		}
	}

	/**
	 * 取消并清空周期任务。
	 * Cancel and clear the periodic schedule.
	 */
	public void clearShedules() {
		if (this.shedules != null) {
			this.shedules.cancel(true);
			this.shedules = null;
		}
	}
}
