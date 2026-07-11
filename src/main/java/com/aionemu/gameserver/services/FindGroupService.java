package com.aionemu.gameserver.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;
import com.aionemu.commons.objects.filter.ObjectFilter;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.FindGroup;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.callback.AddPlayerToAllianceCallback;
import com.aionemu.gameserver.model.team2.alliance.callback.PlayerAllianceCreateCallback;
import com.aionemu.gameserver.model.team2.alliance.callback.PlayerAllianceDisbandCallback;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.callback.AddPlayerToGroupCallback;
import com.aionemu.gameserver.model.team2.group.callback.PlayerGroupCreateCallback;
import com.aionemu.gameserver.model.team2.group.callback.PlayerGroupDisbandCallback;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 寻找队伍服务：维护天族/魔族的招募与申请列表，并在组队/联盟变更时自动清理过期条目。
 * Find-group service: keeps Elyos/Asmodian recruit and apply listings and cleans them on group/alliance changes.
 *
 * @author cura, MrPoke
 */
public class FindGroupService {

	private static volatile ObjectProvider<FindGroupService> instanceProvider;
	/** 天族招募列表。 / Elyos recruit listings. */
	private Map<Integer, FindGroup> elyosRecruitFindGroups = new LinkedHashMap<Integer, FindGroup>();
	/** 天族申请列表。 / Elyos apply listings. */
	private Map<Integer, FindGroup> elyosApplyFindGroups = new LinkedHashMap<Integer, FindGroup>();
	/** 魔族招募列表。 / Asmodian recruit listings. */
	private Map<Integer, FindGroup> asmodianRecruitFindGroups = new LinkedHashMap<Integer, FindGroup>();
	/** 魔族申请列表。 / Asmodian apply listings. */
	private Map<Integer, FindGroup> asmodianApplyFindGroups = new LinkedHashMap<Integer, FindGroup>();

	/**
	 * 注册组队/联盟变更回调，用于自动维护寻找队伍列表。
	 * Registers group/alliance callbacks that keep find-group listings consistent.
	 */
	public FindGroupService() {

		GlobalCallbackHelper.addCallback(new FindGroupOnAddPlayerToGroupListener());
		GlobalCallbackHelper.addCallback(new FindGroupPlayerGroupdDisbandListener());
		GlobalCallbackHelper.addCallback(new FindGroupPlayerGroupdCreateListener());
		GlobalCallbackHelper.addCallback(new FindGroupOnAddPlayerToAllianceListener());
		GlobalCallbackHelper.addCallback(new FindGroupAllianceDisbandListener());
		GlobalCallbackHelper.addCallback(new FindGroupAllianceCreateListener());
	}

	/**
	 * 新增招募或申请条目，并通知发起者。
	 * Adds a recruit or apply entry and notifies the initiator.
	 *
	 * initiator
	 * @param action 动作类型（0x02 招募 / 0x06 申请） / action type (0x02 recruit / 0x06 apply)
	 * listing message
	 * group type
	 */
	public void addFindGroupList(Player player, int action, String message, int groupType) {
		AionObject object = null;
		if (player.isInTeam()) {
			object = player.getCurrentTeam();
		} else {
			object = player;
		}

		FindGroup findGroup = new FindGroup(object, message, groupType);
		int objectId = object.getObjectId();
		switch (player.getRace()) {
		case ELYOS:
			switch (action) {
			case 0x02:
				elyosRecruitFindGroups.put(objectId, findGroup);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400392));
				break;
			case 0x06:
				elyosApplyFindGroups.put(objectId, findGroup);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400393));
				break;
			}
			break;
		case ASMODIANS:
			switch (action) {
			case 0x02:
				asmodianRecruitFindGroups.put(objectId, findGroup);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400392));
				break;
			case 0x06:
				asmodianApplyFindGroups.put(objectId, findGroup);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400393));
				break;
			}
			break;
		}

		Collection<FindGroup> findGroupList = new ArrayList<FindGroup>();
		findGroupList.add(findGroup);

		PacketSendUtility.sendPacket(player,
				new SM_FIND_GROUP(action, ((int) (System.currentTimeMillis() / 1000)), findGroupList));
	}

	/**
	 * 更新指定招募条目的留言。
	 * Updates the message of a recruit listing.
	 *
	 * operator
	 * @param action 更新类型（0x03 招募 / 0x07 申请） / update type (0x03 recruit / 0x07 apply)
	 * new message
	 * listing object id
	 */
	public void updateFindGroupList(Player player, int action, String message, int objectId) {
		Map<Integer, FindGroup> groups = null;
		if (player.getRace() == Race.ELYOS) {
			groups = action == 0x03 ? elyosRecruitFindGroups : action == 0x07 ? elyosApplyFindGroups : null;
		} else if (player.getRace() == Race.ASMODIANS) {
			groups = action == 0x03 ? asmodianRecruitFindGroups : action == 0x07 ? asmodianApplyFindGroups : null;
		}
		FindGroup findGroup = groups == null ? null : groups.get(objectId);
		if (findGroup != null) {
			findGroup.setMessage(message);
		}
	}

	/**
	 * 按种族与动作类型获取寻找队伍列表副本。
	 * Returns a copy of find-group listings for the race and action.
	 *
	 * 阵营 / race
	 * action type
	 * @return 列表副本；未知动作返回 null / listing copy, or null for unknown action
	 */
	public Collection<FindGroup> getFindGroups(Race race, int action) {
		switch (race) {
		case ELYOS:
			switch (action) {
			case 0x00:
				return new ArrayList<FindGroup>(elyosRecruitFindGroups.values());
			case 0x04:
				return new ArrayList<FindGroup>(elyosApplyFindGroups.values());
			case 0xA:
				return Collections.emptyList();
			}
			break;
		case ASMODIANS:
			switch (action) {
			case 0x00:
				return new ArrayList<FindGroup>(asmodianRecruitFindGroups.values());
			case 0x04:
				return new ArrayList<FindGroup>(asmodianApplyFindGroups.values());
			case 0xA:
				return Collections.emptyList();
			}
			break;
		}
		return null;
	}

	/**
	 * 注册副本队伍（自动匹配相关通知）。
	 * Registers an instance group (auto-match related notification).
	 *
	 * 玩家 / player
	 * action type
	 * instance mask id
	 * message
	 * minimum members
	 * group type
	 */
	public void registerInstanceGroup(Player player, int action, int instanceId, String message, int minMembers,
			int groupType) {
		AutoGroupType agt = AutoGroupType.getAGTByMaskId(instanceId);
		if (agt != null) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceId, 1, 0, player.getName()));
		}
	}

	/**
	 * 向玩家发送当前种族的寻找队伍列表。
	 * Sends the current race's find-group listings to the player.
	 *
	 * 玩家 / player
	 * action type
	 */
	public void sendFindGroups(Player player, int action) {
		PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(action, (int) (System.currentTimeMillis() / 1000),
				getFindGroups(player.getRace(), action)));
	}

	/**
	 * 移除指定寻找队伍条目，并向同种族玩家广播删除。
	 * Removes a find-group entry and broadcasts the removal to same-race players.
	 *
	 * 阵营 / race
	 * action type
	 * @param playerObjId 玩家或队伍对象 ID / player or team object id
	 * @return 被移除的条目，不存在则为 null / removed entry, or null
	 */
	public FindGroup removeFindGroup(final Race race, int action, int playerObjId) {
		FindGroup findGroup = null;
		switch (race) {
		case ELYOS:
			switch (action) {
			case 0x00:
				findGroup = elyosRecruitFindGroups.remove(playerObjId);
				break;
			case 0x04:
				findGroup = elyosApplyFindGroups.remove(playerObjId);
				break;
			}
			break;
		case ASMODIANS:
			switch (action) {
			case 0x00:
				findGroup = asmodianRecruitFindGroups.remove(playerObjId);
				break;
			case 0x04:
				findGroup = asmodianApplyFindGroups.remove(playerObjId);
				break;
			}
			break;
		}
		if (findGroup != null)
			PacketSendUtility.broadcastFilteredPacket(new SM_FIND_GROUP(action + 1, playerObjId, findGroup.getUnk()),
					new ObjectFilter<Player>() {

						@Override
						public boolean acceptObject(Player object) {
							return race == object.getRace();
						}
					});
		return findGroup;
	}

	/**
	 * 清理超过一小时未更新的寻找队伍条目。
	 * Removes find-group entries that have not been updated for over one hour.
	 */
	public void clean() {
		cleanMap(elyosRecruitFindGroups, Race.ELYOS, 0x00);
		cleanMap(elyosApplyFindGroups, Race.ELYOS, 0x04);
		cleanMap(asmodianRecruitFindGroups, Race.ASMODIANS, 0x00);
		cleanMap(asmodianApplyFindGroups, Race.ASMODIANS, 0x04);
	}

	/**
	 * 清理指定映射中过期的条目。
	 * Cleans expired entries from the given map.
	 *
	 * @param map 目标映射 / target map
	 * 阵营 / race
	 * action type
	 */
	private void cleanMap(Map<Integer, FindGroup> map, Race race, int action) {
		for (FindGroup group : new ArrayList<FindGroup>(map.values())) {
			if (group.getLastUpdate() + 60 * 60 < System.currentTimeMillis() / 1000) {
				removeFindGroup(race, action, group.getObjectId());
			}
		}
	}

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final FindGroupService getInstance() {
		ObjectProvider<FindGroupService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<FindGroupService> provider) {
		instanceProvider = provider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final FindGroupService instance = new FindGroupService();
	}

	/**
	 * 玩家加入小队前清理个人寻找条目；小队满员后移除小队招募。
	 * Clears personal listings before join; removes group recruit when full.
	 */
	static class FindGroupOnAddPlayerToGroupListener extends AddPlayerToGroupCallback {

		@Override
		public void onBeforePlayerAddToGroup(PlayerGroup group, Player player) {
			GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x00, player.getObjectId());
			GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x04, player.getObjectId());
		}

		@Override
		public void onAfterPlayerAddToGroup(PlayerGroup group, Player player) {
			if (group.isFull()) {
				GameRuntimeServices.findGroupService().removeFindGroup(group.getRace(), 0, group.getObjectId());
			}
		}
	}

	/**
	 * 小队解散前移除对应寻找条目。
	 * Removes the group listing before disband.
	 */
	static class FindGroupPlayerGroupdDisbandListener extends PlayerGroupDisbandCallback {

		@Override
		public void onBeforeGroupDisband(PlayerGroup group) {
			GameRuntimeServices.findGroupService().removeFindGroup(group.getRace(), 0, group.getTeamId());
		}

		@Override
		public void onAfterGroupDisband(PlayerGroup group) {
		}
	}

	/**
	 * 小队创建后将发起者个人条目迁移为小队招募。
	 * After group create, migrates the initiator's personal listing to a group recruit.
	 */
	static class FindGroupPlayerGroupdCreateListener extends PlayerGroupCreateCallback {

		@Override
		public void onBeforeGroupCreate(Player player) {
		}

		@Override
		public void onAfterGroupCreate(Player player) {
			FindGroup inviterFindGroup = GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x00,
					player.getObjectId());
			if (inviterFindGroup == null) {
				inviterFindGroup = GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x04,
						player.getObjectId());
			}
			if (inviterFindGroup != null) {
				GameRuntimeServices.findGroupService().addFindGroupList(player, 0x02, inviterFindGroup.getMessage(),
						inviterFindGroup.getGroupType());
			}
		}
	}

	/**
	 * 联盟解散前移除对应寻找条目。
	 * Removes the alliance listing before disband.
	 */
	static class FindGroupAllianceDisbandListener extends PlayerAllianceDisbandCallback {

		@Override
		public void onBeforeAllianceDisband(PlayerAlliance alliance) {
			GameRuntimeServices.findGroupService().removeFindGroup(alliance.getRace(), 0, alliance.getTeamId());
		}

		@Override
		public void onAfterAllianceDisband(PlayerAlliance alliance) {
		}
	}

	/**
	 * 联盟创建后将发起者个人条目迁移为联盟招募。
	 * After alliance create, migrates the initiator's personal listing to an alliance recruit.
	 */
	static class FindGroupAllianceCreateListener extends PlayerAllianceCreateCallback {

		@Override
		public void onBeforeAllianceCreate(Player player) {
		}

		@Override
		public void onAfterAllianceCreate(Player player) {
			FindGroup inviterFindGroup = GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x00,
					player.getObjectId());
			if (inviterFindGroup == null) {
				inviterFindGroup = GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x04,
						player.getObjectId());
			}
			if (inviterFindGroup != null) {
				GameRuntimeServices.findGroupService().addFindGroupList(player, 0x02, inviterFindGroup.getMessage(),
						inviterFindGroup.getGroupType());
			}
		}
	}

	/**
	 * 玩家加入联盟前清理个人寻找条目；联盟满员后移除联盟招募。
	 * Clears personal listings before join; removes alliance recruit when full.
	 */
	static class FindGroupOnAddPlayerToAllianceListener extends AddPlayerToAllianceCallback {

		@Override
		public void onBeforePlayerAddToAlliance(PlayerAlliance alliance, Player player) {
			GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x00, player.getObjectId());
			GameRuntimeServices.findGroupService().removeFindGroup(player.getRace(), 0x04, player.getObjectId());
		}

		@Override
		public void onAfterPlayerAddToAlliance(PlayerAlliance alliance, Player player) {
			if (alliance.isFull()) {
				GameRuntimeServices.findGroupService().removeFindGroup(alliance.getRace(), 0, alliance.getObjectId());
			}
		}
	}
}
