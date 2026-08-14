package com.aionemu.gameserver.model.templates.pet;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.stats.PetStatsTemplate;

/**
 * 宠物模板（静态数据/XML）。
 * Pet template (static data / XML).
 *
 * @author IlBuono
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pet")
public class PetTemplate {

	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlAttribute(name = "nameid", required = true)
	private int nameId;

	@XmlAttribute(name = "condition_reward")
	private int conditionReward;

	@XmlElement(name = "petfunction")
	private List<PetFunction> petFunctions;

	@XmlElement(name = "petstats")
	private PetStatsTemplate petStats;

	@XmlTransient
	Boolean hasPlayerFuncs = null;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 返回宠物函数列表 / Returns the pet functions */
	public List<PetFunction> getPetFunctions() {
		if (hasPlayerFuncs == null) {
			hasPlayerFuncs = false;
			if (petFunctions == null) {
				List<PetFunction> result = new ArrayList<PetFunction>();
				result.add(PetFunction.CreateEmpty());
				petFunctions = result;
			} else {
				for (PetFunction func : petFunctions) {
					if (func.getPetFunctionType().isPlayerFunction()) {
						hasPlayerFuncs = true;
						break;
					}
				}
				if (!hasPlayerFuncs)
					petFunctions.add(PetFunction.CreateEmpty());
			}
		}
		return petFunctions;
	}

	/** 获取仓库函数。 / Returns the warehouse function. */
	public PetFunction getWarehouseFunction() {
		if (petFunctions == null)
			return null;
		for (PetFunction pf : petFunctions) {
			if (pf.getPetFunctionType() == PetFunctionType.WAREHOUSE)
				return pf;
		}
		return null;
	}

	/**
	 * 判断是否包含指定函数；写入 SM_PET 包时使用，仅检查所需项。
	 * Checks whether the pet contains the given function; used for SM_PET packet, so checks only needed ones.
	 *
	 * @param type 宠物函数类型 / pet function type
	 * @return 是否包含 / whether contained
	 */
	public boolean ContainsFunction(PetFunctionType type) {
		if (type.getId() < 0) {
			return false;

		}
		for (PetFunction t : getPetFunctions()) {
			if (t.getPetFunctionType() == type) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 找到则返回函数，否则为空。
	 * Returns function if found, otherwise null.
	 *
	 * @param type 宠物函数类型 / pet function type
	 * @return 匹配的宠物函数 / matching pet function
	 */
	public PetFunction getPetFunction(PetFunctionType type) {
		for (PetFunction t : getPetFunctions()) {
			if (t.getPetFunctionType() == type) {
				return t;
			}
		}
		return null;
	}

	/** 获取宠物属性。 / Returns the pet stats. */
	public PetStatsTemplate getPetStats() {
		return petStats;
	}

	/** 返回条件奖励 / Returns the condition reward */
	public final int getConditionReward() {
		return conditionReward;
	}
}
