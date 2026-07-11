package com.aionemu.gameserver.model.landing;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.landing.LandingTemplate;
import com.aionemu.gameserver.services.abysslandingservice.Landing;

/**
 * 登陆位置模型。
 * Landing Location model.
 */

public class LandingLocation {
	protected int siege;
	protected int commander;
	protected int artifact;
	protected int base;
	protected int monuments;
	protected int quest;
	protected int facility;
	protected Timestamp levelUpDate;
	protected int id;
	protected int level;
	protected int points;
	protected boolean isActive;
	protected Race race;
	protected LandingTemplate template;
	protected Landing<LandingLocation> activeLanding;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();
	private PersistentState persistentState;

	public LandingLocation() {
	}

	public LandingLocation(LandingTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active landing / Sets the active landing */
	public void setActiveLanding(Landing<LandingLocation> landing) {
		isActive = landing != null;
		this.activeLanding = landing;
	}

	/** 返回当前登陆点 / Returns the active landing */
	public Landing<LandingLocation> getActiveLanding() {
		return activeLanding;
	}

	/** 获取模板。 / Returns the template. */
	public final LandingTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		if (this.level == 0) {
			return this.level + 1;
		} else {
			return this.level;
		}
	}

	/** 设置等级。 / Sets the level. */
	public void setLevel(int level) {
		this.level = level;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return this.points;
	}

	/** 设置点。 / Sets the points. */
	public void setPoints(int pts) {
		this.points = pts;
	}

	/** 获取要塞点。 / Returns the siege points. */
	public int getSiegePoints() {
		return this.siege;
	}

	/** 设置要塞点。 / Sets the siege points. */
	public void setSiegePoints(int pts) {
		this.siege = pts;
	}

	/** 返回指挥官点数 / Returns the commander points */
	public int getCommanderPoints() {
		return this.commander;
	}

	/** 设置 commander points / Sets the commander points */
	public void setCommanderPoints(int pts) {
		this.commander = pts;
	}

	/** 返回神器点 / Returns the artifact points*/
	public int getArtifactPoints() {
		return this.artifact;
	}

	/** 设置 artifact points / Sets the artifact points */
	public void setArtifactPoints(int pts) {
		this.artifact = pts;
	}

	/** 获取基础点。 / Returns the base points. */
	public int getBasePoints() {
		return this.base;
	}

	/** 设置基础点。 / Sets the base points. */
	public void setBasePoints(int pts) {
		this.base = pts;
	}

	/** 获取任务点。 / Returns the quest points. */
	public int getQuestPoints() {
		return this.quest;
	}

	/** 设置任务点。 / Sets the quest points. */
	public void setQuestPoints(int pts) {
		this.quest = pts;
	}

	/** 返回设施点数 / Returns the facility points */
	public int getFacilityPoints() {
		return this.facility;
	}

	/** 设置 facility points / Sets the facility points */
	public void setFacilityPoints(int pts) {
		this.facility = pts;
	}

	/** 返回纪念碑点数 / Returns the monuments points */
	public int getMonumentsPoints() {
		return this.monuments;
	}

	/** 设置 monuments points / Sets the monuments points */
	public void setMonumentsPoints(int pts) {
		this.monuments = pts;
	}

	/** 返回等级日期 / Returns the level up date */
	public Timestamp getLevelUpDate() {
		return levelUpDate;
	}

	/** 设置 level up date / Sets the level up date */
	public Timestamp setLevelUpDate(Timestamp timestamp) {
		return levelUpDate = timestamp;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState state) {
		if (this.persistentState == PersistentState.NEW && state == PersistentState.UPDATE_REQUIRED) {
			return;
		} else {
			this.persistentState = state;
		}
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return this.race;
	}

	/** 设置种族。 / Sets the race. */
	public Race setRace(Race race) {
		return this.race = race;
	}
}
