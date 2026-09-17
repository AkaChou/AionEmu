package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.TransformType;
import lombok.Getter;
import lombok.Setter;

/**
 * 变身 Model 游戏对象。
 * Transform Model game object.
 */

@Getter
@Setter
public class TransformModel {
	private int modelId;
	private final int originalModelId;
	private final TransformType originalType;
	/** 设置变身类型。 / Sets the transform type. */
	private TransformType transformType;
	/** 设置面板 ID / Sets the panel id */
	private int panelId;
	/** 是否激活。 / Whether active. */
	private boolean isActive = false;
	private TribeClass transformTribe;
	private TribeClass overrideTribe;
	/** 设置物品 ID / Sets the item id */
	private int ItemId;
	private Effect activeTransformEffect;
	private int transformLevel;
	private boolean useItem;
	private int animationSkillId;
	private int cantFly;
	private int cantUseSkill;
	private int cantUseItem;
	private int cantAttack;
	private int cantJump;
	private int cantRecall;
	private int cantMove;

	public TransformModel(Creature creature) {
		if (creature instanceof Player) {
			this.originalType = TransformType.PC;
		} else {
			this.originalType = TransformType.NONE;
		}
		this.originalModelId = creature.getObjectTemplate().getTemplateId();
		this.transformType = TransformType.NONE;
	}

	/** 返回模型 ID / Returns the model id */
	public int getModelId() {
		if (isActive) {
			return modelId;
		}
		return originalModelId;
	}

	/** 设置模型 ID / Sets the model id */
	public void setModelId(int modelId) {
		this.modelId = modelId;
		isActive = modelId != 0 && modelId != originalModelId;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		if (ItemId > 0) {
			return ItemId;
		}
		return 0;
	}

	/** 获取类型。 / Returns the type. */
	public TransformType getType() {
		if (isActive) {
			return transformType;
		}
		return originalType;
	}

	/** 返回面板 ID / Returns the panel id */
	public int getPanelId() {
		if (isActive) {
			return panelId;
		}
		return 0;
	}

	public synchronized boolean canReplaceActiveTransform(int level) {
		return activeTransformEffect == null || level >= transformLevel;
	}

	public synchronized void setActiveTransform(Effect effect, int level, boolean useItem, int animationSkillId) {
		activeTransformEffect = effect;
		transformLevel = level;
		this.useItem = useItem;
		this.animationSkillId = animationSkillId;
		isActive = true;
	}

	public synchronized boolean isActiveTransform(Effect effect) {
		return activeTransformEffect == effect;
	}

	public synchronized void clearActiveTransform() {
		activeTransformEffect = null;
		transformLevel = 0;
		useItem = false;
		animationSkillId = 0;
		isActive = false;
	}

	public synchronized void addRestrictions(boolean fly, boolean skill, boolean item, boolean attack,
			boolean jump, boolean recall, boolean move) {
		cantFly += fly ? 1 : 0;
		cantUseSkill += skill ? 1 : 0;
		cantUseItem += item ? 1 : 0;
		cantAttack += attack ? 1 : 0;
		cantJump += jump ? 1 : 0;
		cantRecall += recall ? 1 : 0;
		cantMove += move ? 1 : 0;
	}

	public synchronized void removeRestrictions(boolean fly, boolean skill, boolean item, boolean attack,
			boolean jump, boolean recall, boolean move) {
		cantFly = Math.max(0, cantFly - (fly ? 1 : 0));
		cantUseSkill = Math.max(0, cantUseSkill - (skill ? 1 : 0));
		cantUseItem = Math.max(0, cantUseItem - (item ? 1 : 0));
		cantAttack = Math.max(0, cantAttack - (attack ? 1 : 0));
		cantJump = Math.max(0, cantJump - (jump ? 1 : 0));
		cantRecall = Math.max(0, cantRecall - (recall ? 1 : 0));
		cantMove = Math.max(0, cantMove - (move ? 1 : 0));
	}

	public synchronized boolean isFlyDisabled() {
		return cantFly > 0;
	}

	public synchronized boolean isSkillDisabled() {
		return cantUseSkill > 0;
	}

	public synchronized boolean isItemDisabled() {
		return cantUseItem > 0;
	}

	public synchronized boolean isAttackDisabled() {
		return cantAttack > 0;
	}

	public synchronized boolean isJumpDisabled() {
		return cantJump > 0;
	}

	public synchronized boolean isRecallDisabled() {
		return cantRecall > 0;
	}

	public synchronized boolean isMoveDisabled() {
		return cantMove > 0;
	}

	public synchronized boolean isUseItem() {
		return useItem;
	}

	public synchronized int getAnimationSkillId() {
		return animationSkillId;
	}

	/** 获取部落。 / Returns the tribe. */
	public TribeClass getTribe() {
		if (isActive && transformTribe != null) {
			return transformTribe;
		}
		return overrideTribe;
	}

	/** 设置部落。 / Sets the tribe. */
	public void setTribe(TribeClass transformTribe, boolean override) {
		if (override) {
			this.overrideTribe = transformTribe;
		} else {
			this.transformTribe = transformTribe;
		}
	}
}
