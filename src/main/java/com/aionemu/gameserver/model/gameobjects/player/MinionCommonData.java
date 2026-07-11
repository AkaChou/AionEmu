package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * 守护灵公共数据。
 * Minion Common Data game object.
 */

public class MinionCommonData extends VisibleObjectTemplate implements IExpirable {

	private int minionId;
	private Timestamp birthday;
	private int minionObjId = 0;
	private int masterObjectId;
	private String minionGrade;
	private String name;
	private int minionLevel;
	private int miniongrowthpoint = 0;
	private boolean lock = false;
	private boolean IsBuffing = false;
	private boolean isLooting = false;
	MinionDopingBag dopingBag = null;
	private Timestamp despawnTime;
	private int minionSkillPoints;
	private Timestamp minionFunctionTime;

	public MinionCommonData(int minionId, int masterObjectId, String name, String minionGrade, int minionLevel, int miniongrowthpoint) {
		switch (this.minionObjId) {
		case 0: {
			this.minionObjId = GameWorldBootstrapServices.idFactory().nextId();
			break;
		}
		default:
			do {
				if (DAOManager.getDAO(PlayerMinionsDAO.class).PlayerMinions(masterObjectId, minionObjId)) {
					this.minionObjId = GameWorldBootstrapServices.idFactory().nextId();
				}
			} while (DAOManager.getDAO(PlayerMinionsDAO.class).PlayerMinions(masterObjectId, minionObjId));
			break;
		}
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

	/** 返回 master object id / Returns the master object id */
	public int getMasterObjectId() {
		return masterObjectId;
	}

	/** 返回 minion id / Returns the minion id */
	public int getMinionId() {
		return minionId;
	}

	/** 设置 minion id / Sets the minion id */
	public int setMinionId(int minionId) {
		return this.minionId = minionId;
	}

	/** 返回 minion grade / Returns the minion grade */
	public String getMinionGrade() {
		return minionGrade;
	}

	/** 获取守护灵等级。 / Returns the minion level. */
	public int getMinionLevel() {
		return minionLevel;
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

	/** 设置 birthday / Sets the birthday */
	public void setBirthday(Timestamp birthday) {
		this.birthday = birthday;
	}

	/** 设置名称。 / Sets the name. */
	public void setName(String name) {
		this.name = name;
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

	/**
	 * @return 是否 lock / 是否 lock。 / Whether lock / Whether lock
	 */
	public boolean isLock() {
		return lock;
	}

	/** 设置 lock / Sets the lock */
	public void setLock(boolean lock) {
		this.lock = lock;
	}

	/** 返回 doping bag / Returns the doping bag */
	public MinionDopingBag getDopingBag() {
		return this.dopingBag;
	}

	/** 是否增益中 / Is Buffing. */
	public boolean IsBuffing() {
		return IsBuffing;
	}

	/** 设置 is buffing / Sets the is buffing */
	public void setIsBuffing(boolean isBuffing) {
		IsBuffing = isBuffing;
	}

	/** 设置 is looting / Sets the is looting */
	public void setIsLooting(boolean isLooting) {
		this.isLooting = isLooting;
	}

	/**
	 * @return Whether looting / Whether looting
	 */
	public boolean isLooting() {
		return this.isLooting;
	}

	/**
	 * @return the despawnTime
	 */
	public Timestamp getDespawnTime() {
		return despawnTime;
	}

	/**
	 * @param despawnTime the despawnTime to set
	 */
	public void setDespawnTime(Timestamp despawnTime) {
		this.despawnTime = despawnTime;
	}

	/**
	 * @return the minionSkillPoints
	 */
	public int getMinionSkillPoints() {
		return minionSkillPoints;
	}

	/**
	 * @param minionSkillPoints the minionSkillPoints to set
	 */
	public void setMinionSkillPoints(int minionSkillPoints) {
		this.minionSkillPoints = minionSkillPoints;
	}

	/**
	 * @return the minionFunctionTime
	 */
	public Timestamp getMinionFunctionTime() {
		return minionFunctionTime;
	}

	/**
	 * @param minionFunctionTime the minionFunctionTime to set
	 */
	public void setMinionFunctionTime(Timestamp minionFunctionTime) {
		this.minionFunctionTime = minionFunctionTime;
	}
}
