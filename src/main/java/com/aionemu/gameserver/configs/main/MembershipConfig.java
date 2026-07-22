package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 会员特权门槛与标签显示相关配置。
 * Membership privilege thresholds and tag display related configuration.
 */
public class MembershipConfig {
	/**
	 * 跳过副本称号要求所需会员等级。
	 * Membership level required to skip instance title requirements.
	 */
	@Property(key = "gameserver.instances.title.requirement", defaultValue = "10")
	public static byte INSTANCES_TITLE_REQ;
	/**
	 * 跳过副本阵营要求所需会员等级。
	 * Membership level required to skip instance race requirements.
	 */
	@Property(key = "gameserver.instances.race.requirement", defaultValue = "10")
	public static byte INSTANCES_RACE_REQ;
	/**
	 * 跳过副本组队要求所需会员等级。
	 * Membership level required to skip instance group requirements.
	 */
	@Property(key = "gameserver.instances.group.requirement", defaultValue = "10")
	public static byte INSTANCES_GROUP_REQ;
	/**
	 * 跳过副本任务要求所需会员等级。
	 * Membership level required to skip instance quest requirements.
	 */
	@Property(key = "gameserver.instances.quest.requirement", defaultValue = "10")
	public static byte INSTANCES_QUEST_REQ;
	/**
	 * 使用全部个人仓库所需会员等级。
	 * Membership level required to use all personal warehouse slots.
	 */
	@Property(key = "gameserver.store.wh.all", defaultValue = "10")
	public static byte STORE_WH_ALL;
	/**
	 * 使用全部账号仓库所需会员等级。
	 * Membership level required to use all account warehouse slots.
	 */
	@Property(key = "gameserver.store.accountwh.all", defaultValue = "10")
	public static byte STORE_AWH_ALL;
	/**
	 * 使用全部军团仓库所需会员等级。
	 * Membership level required to use all legion warehouse slots.
	 */
	@Property(key = "gameserver.store.legionwh.all", defaultValue = "10")
	public static byte STORE_LWH_ALL;
	/**
	 * 无限制交易所需会员等级。
	 * Membership level required for unrestricted trading.
	 */
	@Property(key = "gameserver.trade.all", defaultValue = "10")
	public static byte TRADE_ALL;
	/**
	 * 禁用灵魂绑定限制所需会员等级。
	 * Membership level required to disable soulbind restrictions.
	 */
	@Property(key = "gameserver.disable.soulbind", defaultValue = "10")
	public static byte DISABLE_SOULBIND;
	/**
	 * 无限制外形改造所需会员等级。
	 * Membership level required for unrestricted remodeling.
	 */
	@Property(key = "gameserver.remodel.all", defaultValue = "10")
	public static byte REMODEL_ALL;
	/**
	 * 使用全部表情所需会员等级。
	 * Membership level required to use all emotions.
	 */
	@Property(key = "gameserver.emotions.all", defaultValue = "10")
	public static byte EMOTIONS_ALL;
	/**
	 * 启用额外称号所需会员等级。
	 * Membership level required to enable additional titles.
	 */
	@Property(key = "gameserver.titles.additional.enable", defaultValue = "10")
	public static byte TITLES_ADDITIONAL_ENABLE;
	/**
	 * 跳过印记栏位任务所需会员等级。
	 * Membership level required to skip stigma slot quests.
	 */
	@Property(key = "gameserver.quest.stigma.slot", defaultValue = "10")
	public static byte STIGMA_SLOT_QUEST;
	/**
	 * 禁用灵魂病所需会员等级。
	 * Membership level required to disable soul sickness.
	 */
	@Property(key = "gameserver.soulsickness.disable", defaultValue = "10")
	public static byte DISABLE_SOULSICKNESS;
	/**
	 * 自动学习印记所需会员等级。
	 * Membership level required for stigma auto-learn.
	 */
	@Property(key = "gameserver.autolearn.stigma", defaultValue = "10")
	public static byte STIGMA_AUTOLEARN;
	/**
	 * 启用额外角色栏位所需会员等级。
	 * Membership level required to enable additional character slots.
	 */
	@Property(key = "gameserver.character.additional.enable", defaultValue = "10")
	public static byte CHARACTER_ADDITIONAL_ENABLE;
	/**
	 * 启用高级好友列表所需会员等级。
	 * Membership level required to enable advanced friend list.
	 */
	@Property(key = "gameserver.advanced.friendlist.enable", defaultValue = "10")
	public static byte ADVANCED_FRIENDLIST_ENABLE;
	/**
	 * 额外角色栏位数量。
	 * Additional character slot count.
	 */
	@Property(key = "gameserver.character.additional.count", defaultValue = "8")
	public static byte CHARACTER_ADDITIONAL_COUNT;
	/**
	 * 高级好友列表容量。
	 * Advanced friend list size.
	 */
	@Property(key = "gameserver.advanced.friendlist.size", defaultValue = "90")
	public static int ADVANCED_FRIENDLIST_SIZE;

	/**
	 * 30 级特殊会员称号标签。
	 * Special membership title tag for level 30.
	 */
	@Property(key = "gameserver.player.tag.30", defaultValue = "")
	public static String PLAYER_TAG_30;
	/**
	 * 34 级特殊会员称号标签。
	 * Special membership title tag for level 34.
	 */
	@Property(key = "gameserver.player.tag.34", defaultValue = "")
	public static String PLAYER_TAG_34;

	/**
	 * 是否显示会员标签。
	 * Whether membership tags are displayed.
	 */
	@Property(key = "gameserver.membership.tag.display.enable", defaultValue = "true")
	public static boolean PREMIUM_TAG_DISPLAY_ENABLE;
	/**
	 * VIP 会员聊天标签格式。
	 * VIP membership chat tag format.
	 */
	@Property(key = "gameserver.membership.tag.vip", defaultValue = "\uE06A %s")
	public static String TAG_VIP;
	/**
	 * 高级会员聊天标签格式。
	 * Premium membership chat tag format.
	 */
	@Property(key = "gameserver.membership.tag.premium", defaultValue = "\uE055 %s")
	public static String TAG_PREMIUM;
}
