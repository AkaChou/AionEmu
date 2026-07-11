package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.controllers.observer.MathObjectObserver;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.math.MathObject;
import com.aionemu.gameserver.model.gameobjects.math.MathObjectReaction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数学区域对象控制器，按反应类型为进入范围的生物注册观察者。
 * Math-object controller that registers observers for creatures entering range by reaction type.
 */
public class MathController extends VisibleObjectController<MathObject> {

	/** 已注册的生物观察者映射。 / Map of registered creature observers. */
	Map<Creature, MathObjectObserver> observers = new LinkedHashMap<Creature, MathObjectObserver>();

	/**
	 * 符合反应类型的生物进入范围时注册观察者。
	 * Registers an observer when a creature matching the reaction type enters range.
	 *
	 * @param object 进入视野的可见对象 / the visible object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		super.see(object);
		if (this.getOwner().getReaction() == MathObjectReaction.PC ? !(object instanceof Player)
				: (this.getOwner().getReaction() == MathObjectReaction.NPC ? !(object instanceof Npc)
						: this.getOwner().getReaction() == MathObjectReaction.ALL
								&& !(object instanceof Creature))) {
			return;
		}
		Creature creature = (Creature) object;
		MathObjectObserver observer = new MathObjectObserver(this.getOwner(), creature,
				this.getOwner().getType());
		creature.getObserveController().addObserver(observer);
		this.observers.put(creature, observer);
		observer.moved();
	}

	/**
	 * 生物离开范围时移除观察者并清理调度。
	 * Removes the observer and clears schedules when a creature leaves range.
	 *
	 * @param object 离开视野的可见对象 / the visible object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		super.notSee(object, isOutOfRange);
		if (this.getOwner().getReaction() == MathObjectReaction.PC ? !(object instanceof Player)
				: (this.getOwner().getReaction() == MathObjectReaction.NPC ? !(object instanceof Npc)
						: this.getOwner().getReaction() == MathObjectReaction.ALL
								&& !(object instanceof Creature))) {
			return;
		}
		if (isOutOfRange && object instanceof Creature) {
			Creature creature = (Creature) object;
			MathObjectObserver observer = this.observers.remove((Object) creature);
			observer.clearShedules();
			creature.getObserveController().removeObserver(observer);
		}
	}

	/*
	 * public Npc spawn(int worldId, int npcId, float x, float y, float z, byte
	 * heading, int staticId, int instanceId, int randomWalk, String ai) {
	 * SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn((int)worldId,
	 * (int)npcId, (float)x, (float)y, (float)z, (byte)heading, (int)randomWalk);
	 * template.setStaticId(staticId); Npc master =
	 * (Npc)SpawnEngine.spawnObject(template, instanceId); if (randomWalk > 0) { if
	 * (master.getObjectTemplate().getStatsTemplate().getWalkSpeed() == 0.0f) { //
	 * empty if block } WalkManager.startWalking((NpcAI2)master.getAi2()); } if (ai
	 * != null) { GameEngineServices.ai2Engine().setupAI(ai, (Creature)master);
	 * ((NpcAI2)master.getAi2()).setStateIfNot(AIState.IDLE); }
	 * ((MathObject)this.getOwner()).setMaster(master); return
	 * ((MathObject)this.getOwner()).getMaster(); }
	 */

	/**
	 * 延迟后删除本数学对象。
	 * Deletes this math object after a delay.
	 *
	 * @param delay 延迟毫秒数 / delay in milliseconds
	 */
	public void onDelete(int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				MathController.this.delete();
			}
		}, delay);
	}

	/**
	 * 删除数学对象，同时清理主人与所有观察者。
	 * Deletes the math object, also clearing its master and all observers.
	 */
	@Override
	public void delete() {
		if (this.getOwner().getMaster() != null) {
			this.getOwner().getMaster().getController().delete();
		}
		this.getOwner().getKnownList().doOnAllObjects(new Visitor<VisibleObject>() {

			@Override
			public void visit(VisibleObject object) {
				if (!(object instanceof Creature)) {
					return;
				}
				Creature creature = (Creature) object;
				MathObjectObserver observer = MathController.this.observers
						.remove((Object) creature);
				if (observer == null) {
					return;
				}
				observer.clearShedules();
				creature.getObserveController().removeObserver(observer);
			}
		});
		super.delete();
	}
}
