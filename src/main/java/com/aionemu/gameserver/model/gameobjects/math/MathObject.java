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

/**
 * 数学对象。
 * Math Object game object.
 */

public class MathObject extends VisibleObject {
	private double minRange;
	private double maxRange;
	private int skillId;
	private int npcId;
	private Npc master;
	private MathObjectType type;
	private MathObjectReaction reaction = MathObjectReaction.PC;
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

	/** 设置 skill id / Sets the skill id */
	public void setSkillId(int skillId) {
		this.skillId = skillId;
	}

	/** 设置 npc id / Sets the npc id */
	public void setNpcId(int npcId) {
		this.npcId = npcId;
	}

	/** 返回 controller / Returns the controller */
	public MathController getController() {
		return (MathController) super.getController();
	}

	/** 返回最小范围 / Returns the min range*/
	public double getMinRange() {
		return this.minRange;
	}

	/** 返回最大范围 / Returns the max range*/
	public double getMaxRange() {
		return this.maxRange;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return this.skillId;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return this.npcId;
	}

	/** 返回大师 / Returns the master*/
	public Npc getMaster() {
		return this.master;
	}

	/** 设置 master / Sets the master */
	public void setMaster(Npc master) {
		this.master = master;
	}

	/** 获取类型。 / Returns the type. */
	public MathObjectType getType() {
		return this.type;
	}

	/** 返回 reaction / Returns the reaction */
	public MathObjectReaction getReaction() {
		return this.reaction;
	}

	/** 返回时长 / Returns the duration*/
	public int getDuration() {
		return this.duration;
	}

	/** 设置 duration / Sets the duration */
	public void setDuration(int duration) {
		this.duration = duration;
	}

	/** 返回 visibility distance / Returns the visibility distance */
	@Override
	public float getVisibilityDistance() {
		return (float) (this.getMaxRange() + 5.0);
	}

	/** 返回 max z visible distance / Returns the max z visible distance */
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
