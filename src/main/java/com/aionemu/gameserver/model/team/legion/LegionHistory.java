package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 军团历史记录。
 * Legion history entry.
 * @author Simple, xTz
 */
@Getter
@AllArgsConstructor
public class LegionHistory {

	private final LegionHistoryType legionHistoryType;
	private String name = "";
	private final Timestamp time;
	private final int tabId;
	private String description = "";
}
