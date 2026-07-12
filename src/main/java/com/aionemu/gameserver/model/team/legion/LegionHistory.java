package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;
import lombok.Getter;

/**
 * 军团历史记录。
 * Legion history entry.
 *
 * @author Simple, xTz
 */
@Getter
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

}
