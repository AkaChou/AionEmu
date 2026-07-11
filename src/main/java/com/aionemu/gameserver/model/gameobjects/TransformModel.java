package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.TransformType;

/**
 * 变身 Model 游戏对象。
 * Transform Model game object.
 */

public class TransformModel {
	private int modelId;
	private int originalModelId;
	private TransformType originalType;
	private TransformType transformType;
	private int panelId;
	private boolean isActive = false;
	private TribeClass transformTribe;
	private TribeClass overrideTribe;
	private int ItemId;

	public TransformModel(Creature creature) {
		if (creature instanceof Player) {
			this.originalType = TransformType.PC;
		} else {
			this.originalType = TransformType.NONE;
		}
		this.originalModelId = creature.getObjectTemplate().getTemplateId();
		this.transformType = TransformType.NONE;
	}

	/** 返回 model id / Returns the model id */
	public int getModelId() {
		if (isActive && modelId > 0) {
			return modelId;
		}
		return originalModelId;
	}

	/** 设置 model id / Sets the model id */
	public void setModelId(int modelId) {
		if (modelId == 0 || modelId == originalModelId) {
			modelId = originalModelId;
			isActive = false;
		} else {
			this.modelId = modelId;
			isActive = true;
		}
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		if (ItemId > 0) {
			return ItemId;
		}
		return 0;
	}

	/** 设置物品 ID / Sets the item id */
	public void setItemId(int itemId) {
		if (itemId == 0) {
			ItemId = 0;
		} else {
			this.ItemId = itemId;
		}
	}

	/** 获取类型。 / Returns the type. */
	public TransformType getType() {
		if (isActive) {
			return transformType;
		}
		return originalType;
	}

	/** 设置变身类型。 / Sets the transform type. */
	public void setTransformType(TransformType transformType) {
		this.transformType = transformType;
	}

	/** 返回 panel id / Returns the panel id */
	public int getPanelId() {
		if (isActive) {
			return panelId;
		}
		return 0;
	}

	/** 设置 panel id / Sets the panel id */
	public void setPanelId(int id) {
		this.panelId = id;
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return this.isActive;
	}

	/** 设置 active / Sets the active */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
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
