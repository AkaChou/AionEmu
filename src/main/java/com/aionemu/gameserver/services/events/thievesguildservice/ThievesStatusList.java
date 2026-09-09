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

@NoArgsConstructor
@AllArgsConstructor
public class ThievesStatusList {

	@Getter
	@Setter
	private int playerId;
	@Getter
	@Setter
	private int rankId;
	@Getter
	@Setter
	private int thievesCount;
	@Getter
	@Setter
	private Long lastThievesKinah;
	@Getter
	@Setter
	private int prisonCount;
	@Getter
	@Setter
	private String revengeName;
	@Getter
	@Setter
	private int revengeCount;
	@Getter
	@Setter
	private Timestamp revengeDate;
}
