package com.aionemu.gameserver.model.team.legion;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.model.bonus_service.ServiceBuff;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 军团，用于团队相关逻辑。
 * Legion for team logic.
 *
 * @author Simple
 */
@Getter
@Setter
public class Legion {

	/** 军团信息。 / Legion information. */
	private ServiceBuff serviceBuff;
	/**
	 * @param legionId the legionId to set
	 */
	private int legionId = 0;
	/**
	 * @param legionName the legionName to set
	 */
	private String legionName = "";
	/**
	 * @return the legionLevel
	 */
	private int legionLevel = 1;
	/**
	 * @param legionRank the legionRank to set
	 */
	private int legionRank = 0;
	/**
	 * @param contributionPoints
	 */
	private long contributionPoints = 0;
	/**
	 * @return the legionMembers
	 */
	private List<Integer> legionMembers = new ArrayList<Integer>();
	/**
	 * @return the deputyPermission
	 */
	private short deputyPermission = 0x1E0C;
	/**
	 * @return the centurionPermission
	 */
	private short centurionPermission = 0x1C08;
	/**
	 * @return the legionarPermission
	 */
	private short legionaryPermission = 0x1800;
	/**
	 * @return the volunteerPermission
	 */
	private short volunteerPermission = 0x800;
	/**
	 * @param disbandTime the disbandTime to set
	 */
	private int disbandTime;
	/**
	 * @return the announcementList
	 */
	private TreeMap<Timestamp, String> announcementList = new TreeMap<Timestamp, String>();
	/**
	 * @param legionEmblem the legionEmblem to set
	 */
	private LegionEmblem legionEmblem = new LegionEmblem();
	/**
	 * @param legionWarehouse the legionWarehouse to set
	 */
	private LegionWarehouse legionWarehouse;
	private final SortedSet<LegionHistory> legionHistory;
	private final AtomicBoolean hasBonus = new AtomicBoolean(false);
	/** 返回加入申请映射。 / Returns the join request map. */
	private final Map<Integer, LegionJoinRequest> joinRequestMap = new LinkedHashMap<Integer, LegionJoinRequest>();
	/** 设置描述。 / Sets the description. */
	private String description = "";
	/** 设置最小加入等级。 / Sets the min join level. */
	private int minJoinLevel = 0;
	/** 设置加入类型。 / Sets the join type. */
	private int joinType = 0;
	/** 返回领地。 / Returns the territory. */
	private LegionTerritory territory;

	/**
	 * 仅在创建军团时调用！
	 * Only called when a legion is created!
	 */
	public Legion(int legionId, String legionName) {
		this();
		this.legionId = legionId;
		this.legionName = legionName;
	}

	/**
	 * 仅在加载军团时调用！ / Only called when a legion is loaded!
	 */
	public Legion() {
		this.legionWarehouse = new LegionWarehouse(this);
		this.legionHistory = new TreeSet<LegionHistory>(new Comparator<LegionHistory>() {

			/** 比较。 / Compare. */
			@Override
			public int compare(LegionHistory o1, LegionHistory o2) {
				return o1.getTime().getTime() < o2.getTime().getTime() ? 1 : -1;
			}
		});
	}

	/**
	 * @param legionMembers the legionMembers to set
	 */
	public void setLegionMembers(ArrayList<Integer> legionMembers) {
		this.legionMembers = legionMembers;
	}

	/**
	 * @return the online legionMembers
	 */
	public ArrayList<Player> getOnlineLegionMembers() {
		ArrayList<Player> onlineLegionMembers = new ArrayList<Player>();
		for (int legionMemberObjId : legionMembers) {
			Player onlineLegionMember = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(legionMemberObjId);
			if (onlineLegionMember != null) {
				onlineLegionMembers.add(onlineLegionMember);
			}
		}
		return onlineLegionMembers;
	}

	/**
	 * 将成员对象 ID 添加到成员列表。
	 * Adds a member object id to the members list.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 */
	public boolean addLegionMember(int playerObjId) {
		if (canAddMember()) {
			legionMembers.add(playerObjId);
			return true;
		}
		return false;
	}

	/**
	 * 将成员对象 ID 从成员列表移除。
	 * Removes a member object id from the members list.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 */
	public void deleteLegionMember(int playerObjId) {
		legionMembers.remove(Integer.valueOf(playerObjId));
	}

	/**
	 * 设置各职位权限。
	 * Sets the permissions for all ranks.
	 *
	 * @param deputyPermission 副官权限 / deputy permission
	 * @param centurionPermission 百夫长权限 / centurion permission
	 * @param legionaryPermission 军团兵权限 / legionary permission
	 * @param volunteerPermission 志愿兵权限 / volunteer permission
	 * @return 是否设置成功 / true or false
	 */
	public boolean setLegionPermissions(short deputyPermission, short centurionPermission, short legionaryPermission,
			short volunteerPermission) {
		this.deputyPermission = deputyPermission;
		this.centurionPermission = centurionPermission;
		this.legionaryPermission = legionaryPermission;
		this.volunteerPermission = volunteerPermission;
		return true;
	}

	/**
	 * @param contributionPoints the contributionPoints to set
	 */
	public void addContributionPoints(long contributionPoints) {
		this.contributionPoints += contributionPoints;
	}

	/**
	 * 检查军团是否拥有足够的成员升级。
	 * Checks whether the legion has enough members to level up.
	 *
	 * @return true or false
	 */
	public boolean hasRequiredMembers() {
		int memberSize = getLegionMembers().size();
		switch (getLegionLevel()) {
		case 1:
			return memberSize >= LegionConfig.LEGION_LEVEL2_REQUIRED_MEMBERS;
		case 2:
			return memberSize >= LegionConfig.LEGION_LEVEL3_REQUIRED_MEMBERS;
		case 3:
			return memberSize >= LegionConfig.LEGION_LEVEL4_REQUIRED_MEMBERS;
		case 4:
			return memberSize >= LegionConfig.LEGION_LEVEL5_REQUIRED_MEMBERS;
		case 5:
			return memberSize >= LegionConfig.LEGION_LEVEL6_REQUIRED_MEMBERS;
		case 6:
			return memberSize >= LegionConfig.LEGION_LEVEL7_REQUIRED_MEMBERS;
		case 7:
			return memberSize >= LegionConfig.LEGION_LEVEL8_REQUIRED_MEMBERS;
		}
		return false;
	}

	/**
	 * 返回升级所需的基纳价格。
	 * Returns the kinah price required to level up.
	 *
	 * @return 基纳价格 / kinah price
	 */
	public int getKinahPrice() {
		switch (getLegionLevel()) {
		case 1:
			return LegionConfig.LEGION_LEVEL2_REQUIRED_KINAH;
		case 2:
			return LegionConfig.LEGION_LEVEL3_REQUIRED_KINAH;
		case 3:
			return LegionConfig.LEGION_LEVEL4_REQUIRED_KINAH;
		case 4:
			return LegionConfig.LEGION_LEVEL5_REQUIRED_KINAH;
		case 5:
			return LegionConfig.LEGION_LEVEL6_REQUIRED_KINAH;
		case 6:
			return LegionConfig.LEGION_LEVEL7_REQUIRED_KINAH;
		case 7:
			return LegionConfig.LEGION_LEVEL8_REQUIRED_KINAH;
		}
		return 0;
	}

	/**
	 * 返回升级所需的贡献点数。
	 * Returns the contribution points required to level up.
	 *
	 * @return 贡献点数 / contribution points
	 */
	public int getContributionPrice() {
		switch (getLegionLevel()) {
		case 1:
			return LegionConfig.LEGION_LEVEL2_REQUIRED_CONTRIBUTION;
		case 2:
			return LegionConfig.LEGION_LEVEL3_REQUIRED_CONTRIBUTION;
		case 3:
			return LegionConfig.LEGION_LEVEL4_REQUIRED_CONTRIBUTION;
		case 4:
			return LegionConfig.LEGION_LEVEL5_REQUIRED_CONTRIBUTION;
		case 5:
			return LegionConfig.LEGION_LEVEL6_REQUIRED_CONTRIBUTION;
		case 6:
			return LegionConfig.LEGION_LEVEL7_REQUIRED_CONTRIBUTION;
		case 7:
			return LegionConfig.LEGION_LEVEL8_REQUIRED_CONTRIBUTION;
		}
		return 0;
	}

	/**
	 * 若军团可添加成员则返回 true。
	 * Returns true if the legion is able to add a member.
	 *
	 * @return 是否可添加成员 / whether a member can be added
	 */
	private boolean canAddMember() {
		int memberSize = getLegionMembers().size();
		switch (getLegionLevel()) {
		case 1:
			return memberSize < LegionConfig.LEGION_LEVEL1_MAX_MEMBERS;
		case 2:
			return memberSize < LegionConfig.LEGION_LEVEL2_MAX_MEMBERS;
		case 3:
			return memberSize < LegionConfig.LEGION_LEVEL3_MAX_MEMBERS;
		case 4:
			return memberSize < LegionConfig.LEGION_LEVEL4_MAX_MEMBERS;
		case 5:
			return memberSize < LegionConfig.LEGION_LEVEL5_MAX_MEMBERS;
		case 6:
			return memberSize < LegionConfig.LEGION_LEVEL6_MAX_MEMBERS;
		case 7:
			return memberSize < LegionConfig.LEGION_LEVEL7_MAX_MEMBERS;
		case 8:
			return memberSize < LegionConfig.LEGION_LEVEL8_MAX_MEMBERS;
		}
		return false;
	}

	/**
	 * @param announcementList the announcementList to set
	 */
	public void setAnnouncementList(TreeMap<Timestamp, String> announcementList) {
		this.announcementList = announcementList;
	}

	/**
	 * 将新公告添加到公告列表。
	 * Adds a new announcement to the list.
	 *
	 * @param unixTime 公告时间戳 / announcement timestamp
	 * @param announcement 公告内容 / announcement text
	 */
	public void addAnnouncementToList(Timestamp unixTime, String announcement) {
		this.announcementList.put(unixTime, announcement);
	}

	/**
	 * 移除最早的一条公告。
	 * Removes the first entry.
	 */
	public void removeFirstEntry() {
		this.announcementList.remove(this.announcementList.firstEntry().getKey());
	}

	/**
	 * @return the currentAnnouncement
	 */
	public Entry<Timestamp, String> getCurrentAnnouncement() {
		if (this.announcementList.size() > 0) {
			return this.announcementList.lastEntry();
		}
		return null;
	}

	/**
	 * @return true if currently disbanding
	 */
	public boolean isDisbanding() {
		return disbandTime > 0;
	}

	/**
	 * 检查对象 ID 是否在成员列表中。
	 * Checks whether the object id is in the members list.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 是否为成员 / whether a member
	 */
	public boolean isMember(int playerObjId) {
		return legionMembers.contains(playerObjId);
	}

	/**
	 * 获取仓库槽位数。
	 * Gets the warehouse slots.
	 *
	 * @return 仓库槽位数 / warehouse slots
	 */
	public int getWarehouseSlots() {
		switch (getLegionLevel()) {
		case 1:
			return LegionConfig.LWH_LEVEL1_SLOTS;
		case 2:
			return LegionConfig.LWH_LEVEL2_SLOTS;
		case 3:
			return LegionConfig.LWH_LEVEL3_SLOTS;
		case 4:
			return LegionConfig.LWH_LEVEL4_SLOTS;
		case 5:
			return LegionConfig.LWH_LEVEL5_SLOTS;
		case 6:
			return LegionConfig.LWH_LEVEL6_SLOTS;
		case 7:
			return LegionConfig.LWH_LEVEL7_SLOTS;
		case 8:
			return LegionConfig.LWH_LEVEL8_SLOTS;
		}
		return LegionConfig.LWH_LEVEL1_SLOTS;
	}

	/** 获取仓库等级。 / Returns the warehouse level. */
	public int getWarehouseLevel() {
		return getLegionLevel() - 1;
	}

	/**
	 * @return the legionHistory
	 */
	public Collection<LegionHistory> getLegionHistory() {
		return legionHistory;
	}

	/** 按标签 ID 返回军团历史。 / Returns the legion history by tab id. */
	public Collection<LegionHistory> getLegionHistoryByTabId(int tabType) {
		if (legionHistory.isEmpty()) {
			return legionHistory;
		}
		List<LegionHistory> result = new ArrayList<LegionHistory>();
		for (LegionHistory history : legionHistory) {
			if (history.getTabId() == tabType) {
				result.add(history);
			}
		}
		return result;
	}

	/**
	 * @param history
	 */
	public void addHistory(LegionHistory history) {
		this.legionHistory.add(history);
	}

	/** 添加加成。 / Adds bonus. */
	public void addBonus() {
		ArrayList<Player> members = getOnlineLegionMembers();
		// 军团仓库是军团成员共用的仓库，成员可放入或取出物品。
		// The legion warehouse is shared by all legion members, who may deposit or withdraw items.
		// 即使尚未灵魂刻印的武器、防具、各类消耗品乃至基纳，都可供军团成员共用，非常便利。
		// Even non-soulbound weapons, armor, consumables and kinah can be shared among members; a very convenient system.
		// 军团创建后即可使用仓库，其使用方式与普通仓库相同。
		// The warehouse is available right after the legion is founded and works like the regular warehouse.
		// 军团等级越高，军团仓库的槽位越多。
		// The higher the legion level, the more warehouse slots become available.
		if (members.size() >= 2 && members.size() <= 9) {
			if (hasBonus.compareAndSet(false, true)) {
				for (Player member : members) {
					serviceBuff = new ServiceBuff(1);
					serviceBuff.applyEffect(member, 1);
				}
			}
		} else if (members.size() >= 10 && members.size() <= 240) {
			if (hasBonus.compareAndSet(false, true)) {
				for (Player member : members) {
					serviceBuff = new ServiceBuff(6);
					serviceBuff.applyEffect(member, 6);
					serviceBuff.endEffect(member, 1);
				}
			}
		}
	}

	/** 移除加成。 / Removes bonus. */
	public void removeBonus() {
		ArrayList<Player> members = getOnlineLegionMembers();
		if (members.size() < 2) {
			if (hasBonus.compareAndSet(true, false)) {
				for (Player member : members) {
					serviceBuff = new ServiceBuff(1);
					serviceBuff.endEffect(member, 1);
				}
			}
		} else if (members.size() < 10) {
			if (hasBonus.compareAndSet(true, false)) {
				for (Player member : members) {
					serviceBuff = new ServiceBuff(6);
					serviceBuff.endEffect(member, 6);
					serviceBuff.applyEffect(member, 1);
				}
			}
		}
	}

	/** 是否拥有加成。 / Whether a bonus is active. */
	public boolean hasBonus() {
		return hasBonus.get();
	}

	/** 是否相等。 / Equality check. */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Legion legion = (Legion) o;
		return legionId == legion.legionId;
	}

	/** 返回哈希码。 / Returns hash code. */
	@Override
	public int hashCode() {
		return legionId;
	}

	/** 获取军团描述。 / Returns the legion description. */
	public String getLegionDescription() {
		return description;
	}

	/** 返回军团加入类型。 / Returns the legion join type. */
	public int getLegionJoinType() {
		return joinType;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minJoinLevel;
	}

	/** 按玩家 ID 返回加入申请。 / Returns the join request by player id. */
	public LegionJoinRequest getJoinRequestByPlayerId(int playerId) {
		return joinRequestMap.get(playerId);
	}

	/**
	 * 删除玩家的加入申请。
	 * Deletes the join request of a player.
	 *
	 * @param playerId 玩家 ID / player id
	 */
	public void deleteJoinRequest(int playerId) {
		joinRequestMap.remove(playerId);
	}

	/** 添加加入申请。 / Adds a join request. */
	public void addJoinRequest(LegionJoinRequest joinRequest) {
		if (!joinRequestMap.containsKey(joinRequest.getPlayerId())) {
			this.joinRequestMap.put(joinRequest.getPlayerId(), joinRequest);
		}
	}

	/** 清除领地。 / Clears the territory. */
	public void clearTerritory() {
		setTerritory(new LegionTerritory(0));
	}

	/** 是否拥有领地。 / Whether the legion owns a territory. */
	public boolean ownsTerretory() {
		return getTerritory().getId() > 0;
	}
}
