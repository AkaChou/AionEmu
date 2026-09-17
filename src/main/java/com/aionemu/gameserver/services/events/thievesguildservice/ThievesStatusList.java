package com.aionemu.gameserver.services.events.thievesguildservice;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 盗贼状态条目，保存盗贼公会相关玩家状态数据。
 * Thieves status entry holding thieves-guild related player state data.
 *
 * @author Rinzler (Encom)
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThievesStatusList {

	private int playerId;
	private int rankId;
	private int thievesCount;
	private Long lastThievesKinah;
	private int prisonCount;
	private String revengeName;
	private int revengeCount;
	private Timestamp revengeDate;
}
