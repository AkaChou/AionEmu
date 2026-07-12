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

	/** 返回 next state / Returns the next state */
	@Override
	public int getNextState() {
		return STATE_VULNERABLE;
	}

	/** 返回 last activation / Returns the last activation */
	public long getLastActivation() {
		return this.lastArtifactActivation;
	}

	/** 设置 last activation / Sets the last activation */
	public void setLastActivation(long paramLong) {
		this.lastArtifactActivation = paramLong;
	}

	/** 返回 cool down / Returns the cool down */
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
	 * 返回 DescriptionIddescribes 名称的此 artifact.<br>。 / Returns DescriptionId that describes name of this artifact.<br>
	 *
	 * @return DescriptionId with name
	 */
	public DescriptionId getNameAsDescriptionId() {
		// 获取每个神器定义的技能 ID、物品、数量与目标。 / Get Skill id, item, count and target defined for each artifact.
		ArtifactActivation activation = getTemplate().getActivation();
		int skillId = activation.getSkillId();
		SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		return new DescriptionId(skillTemplate.getNameId());
	}

	/**
	 * @return Whether stand alone
	 */
	public boolean isStandAlone() {
		return !GameFeatureServices.siegeService().getFortresses().containsKey(getLocationId());
	}

	/** 返回 owning fortress / Returns the owning fortress */
	public FortressLocation getOwningFortress() {
		return GameFeatureServices.siegeService().getFortress(getLocationId());
	}

	/**
	 * @return the status
	 */
	public ArtifactStatus getStatus() {
		return status != null ? status : ArtifactStatus.IDLE;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(ArtifactStatus status) {
		this.status = status;
	}
}
