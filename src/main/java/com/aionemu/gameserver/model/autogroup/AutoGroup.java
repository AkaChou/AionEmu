package com.aionemu.gameserver.model.autogroup;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 自动队伍，用于 autogroup 相关逻辑。
 * Auto Group for autogroup logic.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AutoGroup")
public class AutoGroup {
	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(required = true)
	protected int instanceId;

	@XmlAttribute(name = "name_id")
	protected int nameId;

	@XmlAttribute(name = "title_id")
	protected int titleId;

	@XmlAttribute(name = "min_lvl")
	protected int minLvl;

	@XmlAttribute(name = "max_lvl")
	protected int maxLvl;

	@XmlAttribute(name = "register_fast")
	protected boolean registerFast;

	@XmlAttribute(name = "register_group")
	protected boolean registerGroup;

	@XmlAttribute(name = "special_purpose")
	protected boolean specialPurpose;

	@XmlAttribute(name = "register_new")
	protected boolean registerNew;

	@XmlAttribute(name = "npc_ids")
	protected List<Integer> npcIds;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回副本 ID / Returns the instance id */
	public int getInstanceId() {
		return instanceId;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 返回标题 ID / Returns the title id */
	public int getTitleId() {
		return titleId;
	}

	/** 返回 min lvl / Returns the min lvl */
	public int getMinLvl() {
		return minLvl;
	}

	/** 返回 max lvl / Returns the max lvl */
	public int getMaxLvl() {
		return maxLvl;
	}

	/** Whether 登记 fast / Whether register fast */
	public boolean hasRegisterFast() {
		return registerFast;
	}

	/** Whether 登记小队 / Whether register group */
	public boolean hasRegisterGroup() {
		return registerGroup;
	}

	/**
	 * @return 是否特殊用途 / Whether special purpose
	 */
	public boolean hasSpecialPurpose() {
		return specialPurpose;
	}

	/** Whether 登记 new / Whether register new */
	public boolean hasRegisterNew() {
		return registerNew;
	}

	/** 返回 npc ids / Returns the npc ids */
	public List<Integer> getNpcIds() {
		if (npcIds == null) {
			npcIds = Collections.emptyList();
		}
		return this.npcIds;
	}
}
