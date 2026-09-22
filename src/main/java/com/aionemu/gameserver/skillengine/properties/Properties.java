package com.aionemu.gameserver.skillengine.properties;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import lombok.Getter;

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
	 * -- GETTER --
	 *  获取首要目标属性。
	 *  Returns the first-target attribute.
	 *
	 * @return 首要目标属性 / first-target attribute

	 */
	@Getter
	@XmlAttribute(name = "first_target", required = true)
	protected FirstTargetAttribute firstTarget;

	/**
	 * 首要目标最大距离。
	 * Maximum first-target range.
	 * -- GETTER --
	 *  获取首要目标距离。
	 *  Returns the first-target range.
	 *
	 * @return 首要目标距离 / first-target range

	 */
	@Getter
	@XmlAttribute(name = "first_target_range", required = true)
	protected int firstTargetRange;

	/**
	 * 是否叠加武器攻击距离。
	 * Whether to add weapon attack range.
	 * -- GETTER --
	 *  是否叠加武器攻击距离。
	 *  Whether weapon range is added.
	 *
	 * @return 是否叠加武器距离 / true if weapon range is added

	 */
	@Getter
	@XmlAttribute(name = "awr")
	protected boolean addWeaponRange;

	/**
	 * 目标敌友关系筛选。
	 * Target friend/enemy relation filter.
	 * -- GETTER --
	 *  获取目标关系属性。
	 *  Returns the target relation attribute.
	 *
	 * @return 目标关系 / target relation

	 */
	@Getter
	@XmlAttribute(name = "target_relation", required = true)
	protected TargetRelationAttribute targetRelation;

	/**
	 * 目标范围类型。
	 * Target range type.
	 * -- GETTER --
	 *  获取目标范围类型。
	 *  Returns the target range type.
	 *
	 * @return 目标范围类型 / target range type

	 */
	@Getter
	@XmlAttribute(name = "target_type", required = true)
	protected TargetRangeAttribute targetType;

	/**
	 * 区域/队伍目标距离。
	 * Area/party target distance.
	 * -- GETTER --
	 *  获取目标距离。
	 *  Returns the target distance.
	 *
	 * @return 目标距离 / target distance

	 */
	@Getter
	@XmlAttribute(name = "target_distance")
	protected int targetDistance;

	/**
	 * 最大目标数量。
	 * Maximum number of targets.
	 * -- GETTER --
	 *  获取最大目标数。
	 *  Returns the maximum target count.
	 *
	 * @return 最大目标数 / max target count

	 */
	@Getter
	@XmlAttribute(name = "target_maxcount")
	protected int targetMaxCount;

	@Getter
	@XmlAttribute(name = "other_target_only")
	protected boolean otherTargetOnly;

	/**
	 * 目标异常状态筛选列表。
	 * Target abnormal-state filter list.
	 * -- GETTER --
	 *  获取目标异常状态列表。
	 *  Returns the target abnormal-status list.
	 *
	 * @return 状态名列表 / status name list

	 */
	@Getter
	@XmlAttribute(name = "target_status")
	private List<String> targetStatus;

	/**
	 * 施法结束时的修订距离。
	 * Revision distance applied at cast end.
	 * -- GETTER --
	 *  获取修订距离。
	 *  Returns the revision distance.
	 *
	 * @return 修订距离 / revision distance

	 */
	@Getter
	@XmlAttribute(name = "revision_distance")
	protected int revisionDistance;

	/**
	 * 有效宽度（圆柱范围）。
	 * Effective width for cylindrical range.
	 * -- GETTER --
	 *  获取有效宽度。
	 *  Returns the effective width.
	 *
	 * @return 有效宽度 / effective width

	 */
	@Getter
	@XmlAttribute(name = "effective_width")
	private int effectiveWidth;

	@Getter
	@XmlAttribute(name = "effective_range")
	protected int effectiveRange;

	@Getter
	@XmlAttribute(name = "effective_altitude")
	protected int effectiveAltitude;

	/**
	 * 有效角度（扇形范围）。
	 * Effective angle for cone range.
	 * -- GETTER --
	 *  获取有效角度。
	 *  Returns the effective angle.
	 *
	 * @return 有效角度 / effective angle

	 */
	@Getter
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
	 * -- GETTER --
	 *  获取目标物种属性。
	 *  Returns the target species attribute.
	 *
	 * @return 目标物种 / target species

	 */
	@Getter
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
		if (targetStatus != null && !targetStatus.isEmpty()) {
			if (!TargetStatusProperty.set(skill, this)) {
				return false;
			}
		}
		return targetSpecies == TargetSpeciesAttribute.ALL || TargetSpeciesProperty.set(skill, this);
	}

	/**
	 * 过滤由外部区域预选的目标，跳过技能自身的距离和几何选区。
	 * Filters targets preselected by an external area, skipping the skill’s own range and geometry selection.
	 */
	public boolean validatePreselectedTargets(Skill skill) {
		skill.setFirstTargetAttribute(firstTarget);
		skill.setTargetRangeAttribute(targetType);
		List<Creature> accepted = new ArrayList<>();
		for (Creature target : List.copyOf(skill.getEffectedList())) {
			if (otherTargetOnly && target == skill.getEffector()) {
				continue;
			}
			skill.setFirstTarget(target);
			skill.getEffectedList().clear();
			skill.getEffectedList().add(target);
			if (targetRelation != null) {
				TargetRelationProperty.set(skill, this);
				if (!skill.getEffectedList().contains(target)) {
					continue;
				}
			}
			if (targetStatus != null && !targetStatus.isEmpty() && !TargetStatusProperty.set(skill, this)) {
				continue;
			}
			if (targetSpecies != TargetSpeciesAttribute.ALL) {
				TargetSpeciesProperty.set(skill, this);
				if (!skill.getEffectedList().contains(target)) {
					continue;
				}
			}
			accepted.add(target);
		}
		skill.getEffectedList().clear();
		skill.getEffectedList().addAll(accepted);
		skill.setFirstTarget(accepted.isEmpty() ? skill.getEffector() : accepted.getFirst());
		return !accepted.isEmpty();
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
		if (targetStatus != null && !targetStatus.isEmpty()) {
			if (!TargetStatusProperty.set(skill, this)) {
				return false;
			}
		}
		return targetSpecies == TargetSpeciesAttribute.ALL || TargetSpeciesProperty.set(skill, this);
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
	 * 施法阶段：开始或结束（影响距离校验）。
	 * Cast phase: start or end (affects range checks).
	 */
	@Getter
    public enum CastState {
		CAST_START(true), CAST_END(false);

        /**
         * -- GETTER --
         *  是否为施法开始阶段。
         *  Returns whether this is the cast-start phase.
         *
         * @return 是否施法开始 / true if cast start
         */
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

    }
}
