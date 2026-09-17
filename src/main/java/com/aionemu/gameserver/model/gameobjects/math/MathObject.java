package com.aionemu.gameserver.model.gameobjects.math;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.MathController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.CreatureAwareKnownList;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;
import lombok.Getter;
import lombok.Setter;

/**
 * 数学对象。
 * Math Object game object.
 */

@Getter
@Setter
public class MathObject extends VisibleObject {
	/** 返回最小范围。 / Returns the min range. */
	private final double minRange;
	/** 返回最大范围。 / Returns the max range. */
	private final double maxRange;
	/** 设置 skill id / Sets the skill id */
	private int skillId;
	/** 设置 npc id / Sets the npc id */
	private int npcId;
	/** 返回主人。 / Returns the master. */
	private Npc master;
	/** 获取类型。 / Returns the type. */
	private final MathObjectType type;
	/** 返回反应类型。 / Returns the reaction. */
	private MathObjectReaction reaction = MathObjectReaction.PC;
	/** 返回时长。 / Returns the duration. */
	private int duration;

	public MathObject(SpawnTemplate spawnTemplate, MathObjectType type, MathObjectReaction reaction, double minRange,
			double maxRange) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new MathController(), spawnTemplate, null,
				new WorldPosition(spawnTemplate.getWorldId()));
		this.type = type;
		this.reaction = reaction;
		this.minRange = minRange;
		this.maxRange = maxRange;
		this.getController().setOwner(this);
		switch (this.reaction) {
		case PC: {
			this.setKnownlist(new PlayerAwareKnownList(this));
			break;
		}
		case NPC: {
			this.setKnownlist(new NpcKnownList(this));
			break;
		}
		case ALL: {
			this.setKnownlist(new CreatureAwareKnownList(this));
		}
		}
	}

	/** 返回控制器。 / Returns the controller. */
	public MathController getController() {
		return (MathController) super.getController();
	}

	/** 返回可见距离。 / Returns the visibility distance. */
	@Override
	public float getVisibilityDistance() {
		return (float) (this.getMaxRange() + 5.0);
	}

	/** 返回最大 Z 轴可见距离。 / Returns the max z visible distance. */
	@Override
	public float getMaxZVisibleDistance() {
		return (float) (this.getMaxRange() + 5.0);
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return "Geometric Object";
	}
}
