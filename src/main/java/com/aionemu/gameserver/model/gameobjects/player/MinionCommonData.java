package com.aionemu.gameserver.model.gameobjects.player;

import java.sql.Timestamp;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import lombok.Getter;
import lombok.Setter;

/**
 * 守护灵公共数据。
 * Minion Common Data game object.
 */

@Getter
@Setter
public class MinionCommonData extends VisibleObjectTemplate implements IExpirable {

	/** 返回 minion id / Returns the minion id */
	private int minionId;
	/** 设置 birthday / Sets the birthday */
	private Timestamp birthday;
	private int minionObjId = 0;
	/** 返回 master object id / Returns the master object id */
	private final int masterObjectId;
	/** 返回 minion grade / Returns the minion grade */
	private final String minionGrade;
	/** 设置名称。 / Sets the name. */
	private String name;
	/** 获取守护灵等级。 / Returns the minion level. */
	private int minionLevel;
	private int miniongrowthpoint = 0;
	/**
	 * @return 是否已锁定。 / Whether lock
	 */
	private boolean lock = false;
	/** 设置 is buffing / Sets the is buffing */
	private boolean IsBuffing = false;
	/**
	 * @return 是否正在拾取 / Whether looting
	 */
	private boolean isLooting = false;
	/** 返回 doping bag / Returns the doping bag */
	MinionDopingBag dopingBag = null;
	/**
	 * @return the despawnTime
	 */
	private Timestamp despawnTime;
	/**
	 * @return the minionSkillPoints
	 */
	private int minionSkillPoints;
	/**
	 * @return the minionFunctionTime
	 */
	private Timestamp minionFunctionTime;

	public MinionCommonData(int minionId, int masterObjectId, String name, String minionGrade, int minionLevel, int miniongrowthpoint) {
		this(GameWorldBootstrapServices.idFactory().nextId(), minionId, masterObjectId, name, minionGrade, minionLevel,
				miniongrowthpoint);
	}

	public MinionCommonData(int minionObjId, int minionId, int masterObjectId, String name, String minionGrade,
			int minionLevel, int miniongrowthpoint) {
		this.minionObjId = minionObjId;
		this.minionId = minionId;
		this.masterObjectId = masterObjectId;
		this.name = name;
		this.minionGrade = minionGrade;
		this.minionLevel = minionLevel;
		this.miniongrowthpoint = miniongrowthpoint;
		if (minionId > 980013) {
			this.dopingBag = new MinionDopingBag();
		}
	}

	/** 设置 object id / Sets the object id */
	public void setObjectId(int minionObjId) {
		this.minionObjId = minionObjId;
	}

	/** 返回对象 ID / Returns the object id */
	public int getObjectId() {
		return minionObjId;
	}

	/** 设置 minion id / Sets the minion id */
	public int setMinionId(int minionId) {
		return this.minionId = minionId;
	}

	/** 设置守护灵等级。 / Sets the minion level. */
	public int setMinionLevel(int minionLevel) {
		return this.minionLevel = minionLevel;
	}

	/** 返回 birthday / Returns the birthday */
	public int getBirthday() {
		if (birthday == null) {
			return 0;
		}
		return (int) (birthday.getTime() / 1000);
	}

	/** 返回 birthday timestamp / Returns the birthday timestamp */
	public Timestamp getBirthdayTimestamp() {
		return birthday;
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return 0;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return false;
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int n) {
	}

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return minionId;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return 0;
	}

	/** 返回 minion growth point / Returns the minion growth point */
	public int getMinionGrowthPoint() {
		return miniongrowthpoint;
	}

	/** 设置 minion growth point / Sets the minion growth point */
	public void setMinionGrowthPoint(int miniongrowthpoint) {
		this.miniongrowthpoint = miniongrowthpoint;
	}

	/** 是否增益中 / Is Buffing. */
	public boolean IsBuffing() {
		return IsBuffing;
	}

	/** 设置 is looting / Sets the is looting */
	public void setIsLooting(boolean isLooting) {
		this.isLooting = isLooting;
	}
}
