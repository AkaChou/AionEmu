package com.aionemu.gameserver.model.gameobjects;

import java.util.HashSet;
import java.util.Set;

import com.aionemu.gameserver.controllers.MinionController;
import com.aionemu.gameserver.controllers.movement.MinionMoveController;
import com.aionemu.gameserver.controllers.movement.MoveController;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 守护灵游戏对象。
 * Minion game object.
 *
 * @author Falke_34
 */
public class Minion extends VisibleObject {

	private final Player master;
	private MoveController moveController;
	private final MinionTemplate minionTemplate;
	private final Set<Integer> grantedSkills = new HashSet<>();

	public Minion(MinionTemplate minionTemplate, MinionController controller, MinionCommonData commonData,
			Player master) {
		super(commonData.getObjectId(), controller, null, commonData, new WorldPosition(master.getWorldId()));
		controller.setOwner(this);
		this.master = master;
		this.minionTemplate = minionTemplate;
		this.moveController = new MinionMoveController();
	}

	/** 返回大师 / Returns the master*/
	public Player getMaster() {
		return master;
	}

	/** 返回 minion id / Returns the minion id */
	public int getMinionId() {
		return objectTemplate.getTemplateId();
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return objectTemplate.getName();
	}

	/** 获取公共数据。 / Returns the common data. */
	public final MinionCommonData getCommonData() {
		return (MinionCommonData) objectTemplate;
	}

	/** 返回 move controller / Returns the move controller */
	public final MoveController getMoveController() {
		return moveController;
	}

	/** 获取守护灵模板。 / Returns the minion template. */
	public final MinionTemplate getMinionTemplate() {
		return minionTemplate;
	}

	public void addGrantedSkill(int skillId) {
		grantedSkills.add(skillId);
	}

	public Set<Integer> getGrantedSkills() {
		return grantedSkills;
	}
}
