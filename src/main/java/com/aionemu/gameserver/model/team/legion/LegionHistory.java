package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;

/**
 * 军团 History，用于团队相关逻辑。
 * Legion History for team logic.
 *
 * @author Simple, xTz
 */
public class LegionHistory {

	private LegionHistoryType legionHistoryType;
	private String name = "";
	private Timestamp time;
	private int tabId;
	private String description = "";

	public LegionHistory(LegionHistoryType legionHistoryType, String name, Timestamp time, int tabId,
			String description) {
		this.legionHistoryType = legionHistoryType;
		this.name = name;
		this.time = time;
		this.tabId = tabId;
		this.description = description;
	}

	/** 返回 legion history type / Returns the legion history type */
	public LegionHistoryType getLegionHistoryType() {
		return legionHistoryType;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回时间 / Returns the time*/
	public Timestamp getTime() {
		return time;
	}

	/** 返回 tab id / Returns the tab id */
	public int getTabId() {
		return tabId;
	}

	/** 获取描述。 / Returns the description. */
	public String getDescription() {
		return description;
	}
}
