package com.aionemu.gameserver.model.team2;

import java.util.Collection;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.team2.group.PlayerFilters;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * 临时玩家团队，用于团队2相关逻辑。
 * Temporary Player Team for team 2 logic.
 *
 * @author ATracer
 */
public abstract class TemporaryPlayerTeam<TM extends TeamMember<Player>> extends GeneralTeam<Player, TM> {

	private LootGroupRules lootGroupRules = new LootGroupRules();

	public TemporaryPlayerTeam(Integer objId) {
		super(objId);
	}

	/**
	 * 队伍中经验最低的玩家等级。
	 * Level of the player with lowest exp.
	 */
	public abstract int getMinExpPlayerLevel();

	/**
	 * 队伍中经验最高的玩家等级。
	 * Level of the player with highest exp.
	 */
	public abstract int getMaxExpPlayerLevel();

	/** 获取种族。 / Returns the race. */
	@Override
	public Race getRace() {
		return getLeader().getObject().getRace();
	}

	/** 发送数据包。 / Send packet. */
	@Override
	public void sendPacket(AionServerPacket packet) {
		applyOnMembers(new TeamMessageSender(packet, Predicates.<Player>alwaysTrue()));
	}

	/** 发送数据包。 / Send packet. */
	@Override
	public void sendPacket(AionServerPacket packet, Predicate<Player> predicate) {
		applyOnMembers(new TeamMessageSender(packet, predicate));
	}

	/** 在线成员数 / online Members. */
	@Override
	public final int onlineMembers() {
		return getOnlineMembers().size();
	}

	/** 返回在线成员集合 / Returns the online members */
	@Override
	public final Collection<Player> getOnlineMembers() {
		return filterMembers(PlayerFilters.ONLINE);
	}

	protected final void initializeTeam(TM leader) {
		setLeader(leader);
	}

	/** 返回战利品分配规则 / Returns the loot group rules */
	public final LootGroupRules getLootGroupRules() {
		return lootGroupRules;
	}

	/** 设置战利品分配规则 / Sets the loot group rules */
	public void setLootGroupRules(LootGroupRules lootGroupRules) {
		this.lootGroupRules = lootGroupRules;
		if (lootGroupRules != null && lootGroupRules.getLootRule() == LootRuleType.FREEFORALL) {
			applyOnMembers(new TeamPacketGroupSender(PlayerFilters.HAS_LOOT_PET,
					SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE03, new SM_PET(13, false)));
		}
	}

	public static final class TeamPacketGroupSender implements Predicate<Player> {

		private final AionServerPacket[] packets;
		private final Predicate<Player> predicate;

		public TeamPacketGroupSender(Predicate<Player> predicate, AionServerPacket... packets) {
			this.packets = packets;
			this.predicate = predicate;
		}

		/** 应用。 / Apply. */
		@Override
		public boolean apply(Player player) {
			if (predicate.apply(player)) {
				for (AionServerPacket packet : packets) {
					PacketSendUtility.sendPacket(player, packet);
				}
			}
			return true;
		}
	}

	public static final class TeamMessageSender implements Predicate<Player> {

		private final AionServerPacket packet;
		private final Predicate<Player> predicate;

		public TeamMessageSender(AionServerPacket packet, Predicate<Player> predicate) {
			this.packet = packet;
			this.predicate = predicate;
		}

		/** 应用。 / Apply. */
		@Override
		public boolean apply(Player player) {
			if (predicate.apply(player)) {
				PacketSendUtility.sendPacket(player, packet);
			}
			return true;
		}
	}
}
