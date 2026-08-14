package com.aionemu.gameserver.model.siege;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.templates.siegelocation.ArtifactActivation;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLocationTemplate;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * Artifact 位置，用于要塞相关逻辑。
 * Artifact Location for siege logic.
 *
 * @author Source
 */
public class ArtifactLocation extends SiegeLocation {

	private ArtifactStatus status;

	public ArtifactLocation() {
		this.status = ArtifactStatus.IDLE;
	}

	public ArtifactLocation(SiegeLocationTemplate template) {
		super(template);
		// 神器始终可攻击 / Artifacts Always Vulnerable
		setVulnerable(true);
	}

	/** 返回下一个状态 / Returns the next state */
	@Override
	public int getNextState() {
		return STATE_VULNERABLE;
	}

	/** 返回上次激活时间 / Returns the last activation */
	public long getLastActivation() {
		return this.lastArtifactActivation;
	}

	/** 设置上次激活时间 / Sets the last activation */
	public void setLastActivation(long paramLong) {
		this.lastArtifactActivation = paramLong;
	}

	/** 返回冷却时间 / Returns the cool down */
	public int getCoolDown() {
		long i = this.template.getActivation().getCd();
		long l = System.currentTimeMillis() - this.lastArtifactActivation;
		if (l > i) {
			return 0;
		} else {
			return (int) ((i - l) / 1000);
		}
	}

	/**
	 * 返回描述此神器名称的 DescriptionId。
	 * Returns DescriptionId that describes the name of this artifact.
	 *
	 * @return 含名称的 DescriptionId / DescriptionId with name
	 */
	public DescriptionId getNameAsDescriptionId() {
		// 获取每个神器定义的技能 ID、物品、数量与目标。 / Get Skill id, item, count and target defined for each artifact.
		ArtifactActivation activation = getTemplate().getActivation();
		int skillId = activation.getSkillId();
		SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		return new DescriptionId(skillTemplate.getNameId());
	}

	/**
	 * 判断神器是否独立存在（不属于任何要塞）。
	 * Checks whether the artifact stands alone (not owned by any fortress).
	 *
	 * @return 是否独立 / Whether stand alone
	 */
	public boolean isStandAlone() {
		return !GameFeatureServices.siegeService().getFortresses().containsKey(getLocationId());
	}

	/** 返回所属要塞 / Returns the owning fortress */
	public FortressLocation getOwningFortress() {
		return GameFeatureServices.siegeService().getFortress(getLocationId());
	}

	/**
	 * 获取当前状态，空值时回落为空闲。
	 * Gets the current status, falling back to IDLE when unset.
	 *
	 * @return 当前状态 / the status
	 */
	public ArtifactStatus getStatus() {
		return status != null ? status : ArtifactStatus.IDLE;
	}

	/**
	 * 设置神器状态。
	 * Sets the artifact status.
	 *
	 * @param status 要设置的状态 / the status to set
	 */
	public void setStatus(ArtifactStatus status) {
		this.status = status;
	}
}
