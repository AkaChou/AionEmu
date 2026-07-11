package com.aionemu.gameserver.skillengine.properties;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 技能目标属性：JAXB 绑定的首要目标/范围/关系等筛选配置，并驱动施法校验。
 * Skill target properties: JAXB-bound first-target/range/relation filters and cast validation.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Properties")
public class Properties {

	/**
	 * 首要目标选取方式。
	 * First-target selection mode.
	 */
	@XmlAttribute(name = "first_target", required = true)
	protected FirstTargetAttribute firstTarget;

	/**
	 * 首要目标最大距离。
	 * Maximum first-target range.
	 */
	@XmlAttribute(name = "first_target_range", required = true)
	protected int firstTargetRange;

	/**
	 * 是否叠加武器攻击距离。
	 * Whether to add weapon attack range.
	 */
	@XmlAttribute(name = "awr")
	protected boolean addWeaponRange;

	/**
	 * 目标敌友关系筛选。
	 * Target friend/enemy relation filter.
	 */
	@XmlAttribute(name = "target_relation", required = true)
	protected TargetRelationAttribute targetRelation;

	/**
	 * 目标范围类型。
	 * Target range type.
	 */
	@XmlAttribute(name = "target_type", required = true)
	protected TargetRangeAttribute targetType;

	/**
	 * 区域/队伍目标距离。
	 * Area/party target distance.
	 */
	@XmlAttribute(name = "target_distance")
	protected int targetDistance;

	/**
	 * 最大目标数量。
	 * Maximum number of targets.
	 */
	@XmlAttribute(name = "target_maxcount")
	protected int targetMaxCount;

	/**
	 * 目标异常状态筛选列表。
	 * Target abnormal-state filter list.
	 */
	@XmlAttribute(name = "target_status")
	private List<String> targetStatus;

	/**
	 * 施法结束时的修订距离。
	 * Revision distance applied at cast end.
	 */
	@XmlAttribute(name = "revision_distance")
	protected int revisionDistance;

	/**
	 * 有效宽度（圆柱范围）。
	 * Effective width for cylindrical range.
	 */
	@XmlAttribute(name = "effective_width")
	private int effectiveWidth;

	/**
	 * 有效角度（扇形范围）。
	 * Effective angle for cone range.
	 */
	@XmlAttribute(name = "effective_angle")
	private int effectiveAngle;

	/**
	 * 方向（1 表示背后）。
	 * Direction flag (1 = back).
	 */
	@XmlAttribute(name = "direction")
	protected int direction;

	/**
	 * 目标物种筛选。
	 * Target species filter.
	 */
	@XmlAttribute(name = "target_species")
	protected TargetSpeciesAttribute targetSpecies;

	/**
	 * 默认构造：目标物种为 ALL。
	 * Default constructor: target species is ALL.
	 */
	public Properties() {
		targetSpecies = TargetSpeciesAttribute.ALL;
	}

	/**
	 * 施法开始时校验并填充目标列表。
	 * Validates and fills the target list at cast start.
	 *
	 * @param skill 技能上下文 / skill context
	 * @return 校验是否通过 / true if validation passes
	 */
	public boolean validate(Skill skill) {
		if (firstTarget != null) {
			if (!FirstTargetProperty.set(skill, this)) {
				return false;
			}
		}
		if (firstTargetRange != 0 || addWeaponRange) {
			if (!FirstTargetRangeProperty.set(skill, this, CastState.CAST_START)) {
				return false;
			}
		}
		if (targetType != null) {
			if (!TargetRangeProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetRelation != null) {
			if (!TargetRelationProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetType != null) {
			if (!MaxCountProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetStatus != null) {
			if (!TargetStatusProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetSpecies != TargetSpeciesAttribute.ALL && !TargetSpeciesProperty.set(skill, this)) {
			return false;
		}
		return true;
	}

	/**
	 * 施法结束时重新校验并填充目标列表。
	 * Re-validates and refills the target list at cast end.
	 *
	 * @param skill 技能上下文 / skill context
	 * @return 校验是否通过 / true if validation passes
	 */
	public boolean endCastValidate(Skill skill) {
		Creature firstTarget = skill.getFirstTarget();
		skill.getEffectedList().clear();
		skill.getEffectedList().add(firstTarget);

		if (firstTargetRange != 0) {
			if (!FirstTargetRangeProperty.set(skill, this, CastState.CAST_END)) {
				return false;
			}
		}
		if (targetType != null) {
			if (!TargetRangeProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetRelation != null) {
			if (!TargetRelationProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetType != null) {
			if (!MaxCountProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetStatus != null) {
			if (!TargetStatusProperty.set(skill, this)) {
				return false;
			}
		}
		if (targetSpecies != TargetSpeciesAttribute.ALL && !TargetSpeciesProperty.set(skill, this)) {
			return false;
		}
		return true;
	}

	/**
	 * 获取首要目标属性。
	 * Returns the first-target attribute.
	 *
	 * @return 首要目标属性 / first-target attribute
	 */
	public FirstTargetAttribute getFirstTarget() {
		return firstTarget;
	}

	/**
	 * 获取首要目标距离。
	 * Returns the first-target range.
	 *
	 * @return 首要目标距离 / first-target range
	 */
	public int getFirstTargetRange() {
		return firstTargetRange;
	}

	/**
	 * 是否叠加武器攻击距离。
	 * Whether weapon range is added.
	 *
	 * @return 是否叠加武器距离 / true if weapon range is added
	 */
	public boolean isAddWeaponRange() {
		return addWeaponRange;
	}

	/**
	 * 获取目标关系属性。
	 * Returns the target relation attribute.
	 *
	 * target relation
	 */
	public TargetRelationAttribute getTargetRelation() {
		return targetRelation;
	}

	/**
	 * 获取目标范围类型。
	 * Returns the target range type.
	 *
	 * @return 目标范围类型 / target range type
	 */
	public TargetRangeAttribute getTargetType() {
		return targetType;
	}

	/**
	 * 获取目标距离。
	 * Returns the target distance.
	 *
	 * target distance
	 */
	public int getTargetDistance() {
		return targetDistance;
	}

	/**
	 * 获取最大目标数。
	 * Returns the maximum target count.
	 *
	 * @return 最大目标数 / max target count
	 */
	public int getTargetMaxCount() {
		return targetMaxCount;
	}

	/**
	 * 获取目标异常状态列表。
	 * Returns the target abnormal-status list.
	 *
	 * @return 状态名列表 / status name list
	 */
	public List<String> getTargetStatus() {
		return targetStatus;
	}

	/**
	 * 获取修订距离。
	 * Returns the revision distance.
	 *
	 * revision distance
	 */
	public int getRevisionDistance() {
		return revisionDistance;
	}

	/**
	 * 获取有效宽度。
	 * Returns the effective width.
	 *
	 * effective width
	 */
	public int getEffectiveWidth() {
		return effectiveWidth;
	}

	/**
	 * 获取有效角度。
	 * Returns the effective angle.
	 *
	 * effective angle
	 */
	public int getEffectiveAngle() {
		return effectiveAngle;
	}

	/**
	 * 是否为背后方向。
	 * Whether the direction is back-facing.
	 *
	 * @return 若 direction is 1 则为 true / true if direction is 1
	 */
	public boolean isBackDirection() {
		return direction == 1;
	}

	/**
	 * 获取目标物种属性。
	 * Returns the target species attribute.
	 *
	 * target species
	 */
	public TargetSpeciesAttribute getTargetSpecies() {
		return targetSpecies;
	}

	/**
	 * 施法阶段：开始或结束（影响距离校验）。
	 * Cast phase: start or end (affects range checks).
	 */
	public enum CastState {
		CAST_START(true), CAST_END(false);

		private final boolean isCastStart;

		/**
		 * 构造施法阶段。
		 * Creates a cast state.
		 *
		 * @param isCastStart 是否为施法开始 / whether this is cast start
		 */
		CastState(boolean isCastStart) {
			this.isCastStart = isCastStart;
		}

		/**
		 * 是否为施法开始阶段。
		 * Returns whether this is the cast-start phase.
		 *
		 * @return 是否施法开始 / true if cast start
		 */
		public boolean isCastStart() {
			return isCastStart;
		}
	}
}
