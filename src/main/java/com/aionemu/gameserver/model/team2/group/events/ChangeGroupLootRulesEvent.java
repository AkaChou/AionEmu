package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * Change 队伍 LootRules 活动，用于团队2相关逻辑。
 * Change Group Loot Rules Event for team 2 logic.
 *
 * @author ATracer
 */
public class ChangeGroupLootRulesEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {

	private final PlayerGroup group;
	private final LootGroupRules lootGroupRules;

	public ChangeGroupLootRulesEvent(PlayerGroup group, LootGroupRules lootGroupRules) {
		this.group = group;
		this.lootGroupRules = lootGroupRules;
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		PacketSendUtility.sendPacket(member, new SM_GROUP_INFO(group));
		return true;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		group.setLootGroupRules(lootGroupRules);
		group.applyOnMembers(this);
	}
}
