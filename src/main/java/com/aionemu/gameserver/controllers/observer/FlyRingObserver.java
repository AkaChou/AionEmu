package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 飞行环观察者：穿过飞行环时加速并触发任务/副本事件。
 * Fly-ring observer: speeds up and fires quest/instance events when passing a fly ring.
 */
public class FlyRingObserver extends ActionObserver {
	/** 被观察玩家 / Observed player */
	private Player player;
	/** 飞行环 / Fly ring */
	private FlyRing ring;
	/** 上一位置 / Previous position */
	private Point3D oldPosition;
	/** 奥德之翼技能模板 / Wings Of Aether skill template */
	SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(260); // 奥德之翼 4.8 / Wings Of Aether 4.8

	/**
	 * 空构造（字段为 null）。
	 * Empty constructor (fields null).
	 */
	public FlyRingObserver() {
		super(ObserverType.MOVE);
		this.player = null;
		this.ring = null;
		this.oldPosition = null;
	}

	/**
	 * @param ring 飞行环 / fly ring
	 * @param player 被观察玩家 / observed player
	 */
	public FlyRingObserver(FlyRing ring, Player player) {
		super(ObserverType.MOVE);
		this.player = player;
		this.ring = ring;
		this.oldPosition = new Point3D(player.getX(), player.getY(), player.getZ());
	}

	@Override
	public void moved() {
		Point3D newPosition = new Point3D(player.getX(), player.getY(), player.getZ());
		boolean passedThrough = false;
		if (ring.getPlane().intersect(oldPosition, newPosition)) {
			Point3D intersectionPoint = ring.getPlane().intersection(oldPosition, newPosition);
			if (intersectionPoint != null) {
				double distance = Math.abs(ring.getPlane().getCenter().distance(intersectionPoint));
				if (distance < ring.getTemplate().getRadius()) {
					passedThrough = true;
				}
			} else {
				if (MathUtil.isIn3dRange(ring, player, ring.getTemplate().getRadius())) {
					passedThrough = true;
				}
			}
		}
		if (passedThrough) {
			if (ring.getTemplate().getMap() == 210020000 || // 埃尔特内 / Eltnen.
					ring.getTemplate().getMap() == 220020000 || // 莫尔海姆 / Morheim.
					ring.getTemplate().getMap() == 400010000 || isQuestActive() || isInstanceActive()) {
				Effect speedUp = new Effect(player, player, skillTemplate, skillTemplate.getLvl(), 0);
				speedUp.initialize();
				speedUp.addAllEffectToSucess();
				speedUp.applyEffect();
			}
			GameEngineServices.questEngine().onPassFlyingRing(new QuestEnv(null, player, 0, 0), ring.getName());
		}
		oldPosition = newPosition;
	}

	/**
	 * 副本处理器是否激活本飞行环。
	 * Whether the instance handler activates this fly ring.
	 *
	 * @return 是否激活 / whether active
	 */
	private boolean isInstanceActive() {
		return ring.getPosition().getWorldMapInstance().getInstanceHandler().onPassFlyingRing(player, ring.getName());
	}

	/**
	 * 相关飞行任务是否处于可触发阶段。
	 * Whether the related flight quest is in a triggerable stage.
	 *
	 * @return 任务是否激活 / whether quest is active
	 */
	private boolean isQuestActive() {
		int questId = player.getRace() == Race.ASMODIANS ? 2042 : 1044;
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		return qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) >= 2 && qs.getQuestVarById(0) <= 8;
	}
}
