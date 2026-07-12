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

/**
 * 军团，用于团队相关逻辑。
 * Legion for team logic.
 *
 * @author Simple
 */
public class Legion {

	/** 军团信息 / Legion Information */
	private ServiceBuff serviceBuff;
	private int legionId = 0;
	private String legionName = "";
	private int legionLevel = 1;
	private int legionRank = 0;
	private long contributionPoints = 0;
	private List<Integer> legionMembers = new ArrayList<Integer>();
	private short deputyPermission = 0x1E0C;
	private short centurionPermission = 0x1C08;
	private short legionaryPermission = 0x1800;
	private short volunteerPermission = 0x800;
	private int disbandTime;
	private TreeMap<Timestamp, String> announcementList = new TreeMap<Timestamp, String>();
	private LegionEmblem legionEmblem = new LegionEmblem();
	private LegionWarehouse legionWarehouse;
	private SortedSet<LegionHistory> legionHistory;
	private AtomicBoolean hasBonus = new AtomicBoolean(false);
	private Map<Integer, LegionJoinRequest> joinRequestMap = new LinkedHashMap<Integer, LegionJoinRequest>();
	private String description = "";
	private int minJoinLevel = 0;
	private int joinType = 0;
	private LegionTerritory territory;

	/**
	 * 仅在创建军团时调用！。 / Only called when a legion is created!.
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

			/** 比较 / compare. */
			@Override
			public int compare(LegionHistory o1, LegionHistory o2) {
				return o1.getTime().getTime() < o2.getTime().getTime() ? 1 : -1;
			}

		});
	}

	/**
	 * @param legionId the legionId to set
	 */
	public void setLegionId(int legionId) {
		this.legionId = legionId;
	}

	/**
	 * @return the legionId
	 */
	public int getLegionId() {
		return legionId;
	}

	/**
	 * @param legionName the legionName to set
	 */
	public void setLegionName(String legionName) {
		this.legionName = legionName;
	}

	/**
	 * @return the legionName
	 */
	public String getLegionName() {
		return legionName;
	}

	/**
	 * @param legionMembers the legionMembers to set
	 */
	public void setLegionMembers(ArrayList<Integer> legionMembers) {
		this.legionMembers = legionMembers;
	}

	/**
	 * @return the legionMembers
	 */
	public List<Integer> getLegionMembers() {
		return legionMembers;
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
	 * 添加 legionMember 到 legionMembers 列表。 / Add a legionMember to the legionMembers list
	 *
	 * @param playerObjId
	 */
	public boolean addLegionMember(int playerObjId) {
		if (canAddMember()) {
			legionMembers.add(playerObjId);
			return true;
		}
		return false;
	}

	/**
	 * 删除 legionMember 从 legionMembers 列表。 / Delete a legionMember from the legionMembers list
	 *
	 * @param playerObjId
	 */
	public void deleteLegionMember(int playerObjId) {
		legionMembers.remove(Integer.valueOf(playerObjId));
	}

	/**
	 * 将权限。
	 * This method will set the permissions
	 *
	 * @param deputyPermission
	 * @param centurionPermission
	 * @param legionaryPermission
	 * @return true or false
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
	 * @return the deputyPermission
	 */
	public short getDeputyPermission() {
		return deputyPermission;
	}

	/**
	 * @return the centurionPermission
	 */
	public short getCenturionPermission() {
		return centurionPermission;
	}

	/**
	 * @return the legionarPermission
	 */
	public short getLegionaryPermission() {
		return legionaryPermission;
	}

	/**
	 * @return the volunteerPermission
	 */
	public short getVolunteerPermission() {
		return volunteerPermission;
	}

	/**
	 * @return the legionLevel
	 */
	public int getLegionLevel() {
		return legionLevel;
	}

	/**
	 * @param legionLevel
	 */
	public void setLegionLevel(int legionLevel) {
		this.legionLevel = legionLevel;
	}

	/**
	 * @param legionRank the legionRank to set
	 */
	public void setLegionRank(int legionRank) {
		this.legionRank = legionRank;
	}

	/**
	 * @return the legionRank
	 */
	public int getLegionRank() {
		return legionRank;
	}

	/**
	 * @param contributionPoints the contributionPoints to set
	 */
	public void addContributionPoints(long contributionPoints) {
		this.contributionPoints += contributionPoints;
	}

	/**
	 * @param contributionPoints
	 */
	public void setContributionPoints(long contributionPoints) {
		this.contributionPoints = contributionPoints;
	}

	/**
	 * @return the contributionPoints
	 */
	public long getContributionPoints() {
		return contributionPoints;
	}

	/**
	 * 将军团 hasenough 成员等级 up。
	 * This method will check whether a legion has enough members to level up
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
	 * 将 return 基纳 pricerequired 等级 up。
	 * This method will return the kinah price required to level up
	 *
	 * @return int
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
	 * 将 returncontributionpointsrequired 等级 up。
	 * This method will return the contribution points required to level up
	 *
	 * @return int
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
	 * 将 returntrue 军团 able 成员。
	 * This method will return true if a legion is able to add a member
	 *
	 * @return
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
	 * 将新公告列表。
	 * This method will add a new announcement to the list
	 */
	public void addAnnouncementToList(Timestamp unixTime, String announcement) {
		this.announcementList.put(unixTime, announcement);
	}

	/**
	 * This method removes the first entry
	 */
	public void removeFirstEntry() {
		this.announcementList.remove(this.announcementList.firstEntry().getKey());
	}

	/**
	 * @return the announcementList
	 */
	public TreeMap<Timestamp, String> getAnnouncementList() {
		return this.announcementList;
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
	 * @param disbandTime the disbandTime to set
	 */
	public void setDisbandTime(int disbandTime) {
		this.disbandTime = disbandTime;
	}

	/**
	 * @return the disbandTime
	 */
	public int getDisbandTime() {
		return disbandTime;
	}

	/**
	 * @return true if currently disbanding
	 */
	public boolean isDisbanding() {
		return disbandTime > 0;
	}

	/**
	 * 检查对象 ID 是否在列表中。 / This function checks if object id is in list.
	 */
	public boolean isMember(int playerObjId) {
		return legionMembers.contains(playerObjId);
	}

	/**
	 * @param legionEmblem the legionEmblem to set
	 */
	public void setLegionEmblem(LegionEmblem legionEmblem) {
		this.legionEmblem = legionEmblem;
	}

	/**
	 * @return the legionEmblem
	 */
	public LegionEmblem getLegionEmblem() {
		return legionEmblem;
	}

	/**
	 * @param legionWarehouse the legionWarehouse to set
	 */
	public void setLegionWarehouse(LegionWarehouse legionWarehouse) {
		this.legionWarehouse = legionWarehouse;
	}

	/**
	 * @return the legionWarehouse
	 */
	public LegionWarehouse getLegionWarehouse() {
		return legionWarehouse;
	}

	/**
	 * 获取仓库 slots。
	 * Get warehouse slots
	 *
	 * @return warehouse slots
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

	/** 按 tab id 返回 legion history / Returns the legion history by tab id */
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
		// 레기온 창고란 같은 레기온원들 끼리 공동으로 사용하는 창고의 개념이다.
		// 즉, 다른 레기온원이 이곳에 물건을 넣으면, 자신 외에 다른 권한을 가진 레기온원이 꺼내서 사용할 수 있다는 것.
		// 아직 영혼 각인하지 않은 무기, 방어구라든지 각종 소비 아이템, 심지어 키나까지도 레기온원들 끼리 공동으로 사용할 수 있게 해주는 아주
		// 편리한 시스템이다.
		// 레기온 창고는 레기온을 설립하면 바로 이용이 가능하며, 레기온 창고의 이용 방식은 일반 창고와 동일하다.
		// 레기온의 레벨이 올라갈 수록 레기온 창고의 슬롯 수를 늘릴 수 있다.
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

	/** 是否拥有加成。 / Whether bonus. */
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

	/** 返回军团加入类型 / Returns the legion join type*/
	public int getLegionJoinType() {
		return joinType;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minJoinLevel;
	}

	/** 设置描述。 / Sets the description. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** 设置最小加入等级 / Sets the min join level*/
	public void setMinJoinLevel(int minJoinLevel) {
		this.minJoinLevel = minJoinLevel;
	}

	/** 设置加入类型 / Sets the join type*/
	public void setJoinType(int joinType) {
		this.joinType = joinType;
	}

	/** 返回 join request map / Returns the join request map */
	public Map<Integer, LegionJoinRequest> getJoinRequestMap() {
		return joinRequestMap;
	}

	/** 按玩家 ID 返回 join request / Returns the join request by player id */
	public LegionJoinRequest getJoinRequestByPlayerId(int playerId) {
		return joinRequestMap.get(playerId);
	}

	/**
	 * @param playerId Deletes join request
	 */
	public void deleteJoinRequest(int playerId) {
		joinRequestMap.remove(playerId);
	}

	/** 添加 join request / Adds join request */
	public void addJoinRequest(LegionJoinRequest joinRequest) {
		if (!joinRequestMap.containsKey(joinRequest.getPlayerId())) {
			this.joinRequestMap.put(joinRequest.getPlayerId(), joinRequest);
		}
	}

	/** 清除 territory / Clear territory */
	public void clearTerritory() {
		setTerritory(new LegionTerritory(0));
	}

	/** 拥有领地 / owns Terretory. */
	public boolean ownsTerretory() {
		return getTerritory().getId() > 0;
	}

	/** 返回 territory / Returns the territory */
	public LegionTerritory getTerritory() {
		return territory;
	}

	/** 设置 territory / Sets the territory */
	public void setTerritory(LegionTerritory territory) {
		this.territory = territory;
	}
}
