package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 会员特权门槛与标签显示相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Membership privilege thresholds and tag display related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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
	 * 跳过副本等级要求所需会员等级。
	 * Membership level required to skip instance level requirements.
	 */
	@Property(key = "gameserver.instances.level.requirement", defaultValue = "10")
	public static byte INSTANCES_LEVEL_REQ;
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
	 * 跳过副本冷却所需会员等级。
	 * Membership level required to skip instance cooldown.
	 */
	@Property(key = "gameserver.instances.cooldown", defaultValue = "10")
	public static byte INSTANCES_COOLDOWN;
	/**
	 * 跳过副本物品要求所需会员等级。
	 * Membership level required to skip instance item requirements.
	 */
	@Property(key = "gameserver.instances.item.requirement", defaultValue = "10")
	public static byte INSTANCES_ITEM_REQ;
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
	@Property(key = "gameserver.membership.tag.vip", defaultValue = " %s")
	public static String TAG_VIP;
	/**
	 * 高级会员聊天标签格式。
	 * Premium membership chat tag format.
	 */
	@Property(key = "gameserver.membership.tag.premium", defaultValue = " %s")
	public static String TAG_PREMIUM;

	/**
	 * 副本门槛绑定树 / instance-threshold binding tree.
	 */
	@Getter
	private final InstancesProperties instances = new InstancesProperties();
	/**
	 * 仓库绑定树 / warehouse binding tree.
	 */
	@Getter
	private final StoreProperties store = new StoreProperties();
	/**
	 * 交易与限制绑定树 / trade-and-restriction binding tree.
	 */
	@Getter
	private final TradeProperties trade = new TradeProperties();
	/**
	 * 灵魂绑定绑定树 / soulbind binding tree.
	 */
	@Getter
	private final DisableProperties disable = new DisableProperties();
	/**
	 * 外形改造绑定树 / remodel binding tree.
	 */
	@Getter
	private final RemodelProperties remodel = new RemodelProperties();
	/**
	 * 表情绑定树 / emotion binding tree.
	 */
	@Getter
	private final EmotionsProperties emotions = new EmotionsProperties();
	/**
	 * 称号绑定树 / title binding tree.
	 */
	@Getter
	private final TitlesProperties titles = new TitlesProperties();
	/**
	 * 印记栏位绑定树 / stigma-slot binding tree.
	 */
	@Getter
	private final QuestProperties quest = new QuestProperties();
	/**
	 * 灵魂病绑定树 / soul-sickness binding tree.
	 */
	@Getter
	private final SoulsicknessProperties soulsickness = new SoulsicknessProperties();
	/**
	 * 自动学习绑定树 / auto-learn binding tree.
	 */
	@Getter
	private final AutolearnProperties autolearn = new AutolearnProperties();
	/**
	 * 角色栏位绑定树 / character-slot binding tree.
	 */
	@Getter
	private final CharacterProperties character = new CharacterProperties();
	/**
	 * 好友列表绑定树 / friend-list binding tree.
	 */
	@Getter
	private final AdvancedProperties advanced = new AdvancedProperties();
	/**
	 * 玩家标签绑定树 / player-tag binding tree.
	 */
	@Getter
	private final PlayerProperties player = new PlayerProperties();
	/**
	 * 会员标签绑定树 / membership-tag binding tree.
	 */
	@Getter
	private final MembershipProperties membership = new MembershipProperties();

	/**
	 * 副本门槛节点：绑定 gameserver.instances.*。
	 * Instance-threshold node binding gameserver.instances.*.
	 */
	@Getter
	public static class InstancesProperties {
		private final TitleProperties title = new TitleProperties();
		private final RaceProperties race = new RaceProperties();
		private final LevelProperties level = new LevelProperties();
		private final GroupProperties group = new GroupProperties();
		private final QuestRequirementProperties quest = new QuestRequirementProperties();
		private final ItemProperties item = new ItemProperties();

		public byte getCooldown() {
			return INSTANCES_COOLDOWN;
		}

		public void setCooldown(byte cooldown) {
			INSTANCES_COOLDOWN = cooldown;
		}
	}

	/**
	 * 副本称号父节点 / Instance-title parent node.
	 */
	@Getter
	public static class TitleProperties {
		public byte getRequirement() {
			return INSTANCES_TITLE_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_TITLE_REQ = requirement;
		}
	}

	/**
	 * 副本阵营父节点 / Instance-race parent node.
	 */
	@Getter
	public static class RaceProperties {
		public byte getRequirement() {
			return INSTANCES_RACE_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_RACE_REQ = requirement;
		}
	}

	/**
	 * 副本等级父节点 / Instance-level parent node.
	 */
	@Getter
	public static class LevelProperties {
		public byte getRequirement() {
			return INSTANCES_LEVEL_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_LEVEL_REQ = requirement;
		}
	}

	/**
	 * 副本组队父节点 / Instance-group parent node.
	 */
	@Getter
	public static class GroupProperties {
		public byte getRequirement() {
			return INSTANCES_GROUP_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_GROUP_REQ = requirement;
		}
	}

	/**
	 * 副本任务父节点 / Instance-quest parent node.
	 */
	@Getter
	public static class QuestRequirementProperties {
		public byte getRequirement() {
			return INSTANCES_QUEST_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_QUEST_REQ = requirement;
		}
	}

	/**
	 * 副本物品父节点 / Instance-item parent node.
	 */
	@Getter
	public static class ItemProperties {
		public byte getRequirement() {
			return INSTANCES_ITEM_REQ;
		}

		public void setRequirement(byte requirement) {
			INSTANCES_ITEM_REQ = requirement;
		}
	}

	/**
	 * 仓库节点：绑定 gameserver.store.*。
	 * Warehouse node binding gameserver.store.*.
	 */
	@Getter
	public static class StoreProperties {
		private final WhProperties wh = new WhProperties();
		private final AccountwhProperties accountwh = new AccountwhProperties();
		private final LegionwhProperties legionwh = new LegionwhProperties();
	}

	/**
	 * 个人仓库节点 / Personal warehouse node.
	 */
	@Getter
	public static class WhProperties {
		public byte getAll() {
			return STORE_WH_ALL;
		}

		public void setAll(byte all) {
			STORE_WH_ALL = all;
		}
	}

	/**
	 * 账号仓库节点 / Account warehouse node.
	 */
	@Getter
	public static class AccountwhProperties {
		public byte getAll() {
			return STORE_AWH_ALL;
		}

		public void setAll(byte all) {
			STORE_AWH_ALL = all;
		}
	}

	/**
	 * 军团仓库节点 / Legion warehouse node.
	 */
	@Getter
	public static class LegionwhProperties {
		public byte getAll() {
			return STORE_LWH_ALL;
		}

		public void setAll(byte all) {
			STORE_LWH_ALL = all;
		}
	}

	/**
	 * 交易节点：绑定 gameserver.trade.all。
	 * Trade node binding gameserver.trade.all.
	 */
	public static class TradeProperties {
		public byte getAll() {
			return TRADE_ALL;
		}

		public void setAll(byte all) {
			TRADE_ALL = all;
		}
	}

	/**
	 * 灵魂绑定父节点：对应配置元素 gameserver.disable。
	 * Soulbind parent node matching the gameserver.disable element.
	 */
	@Getter
	public static class DisableProperties {
		public byte getSoulbind() {
			return DISABLE_SOULBIND;
		}

		public void setSoulbind(byte soulbind) {
			DISABLE_SOULBIND = soulbind;
		}
	}

	/**
	 * 外形改造节点：绑定 gameserver.remodel.all。
	 * Remodel node binding gameserver.remodel.all.
	 */
	public static class RemodelProperties {
		public byte getAll() {
			return REMODEL_ALL;
		}

		public void setAll(byte all) {
			REMODEL_ALL = all;
		}
	}

	/**
	 * 表情节点：绑定 gameserver.emotions.all。
	 * Emotion node binding gameserver.emotions.all.
	 */
	public static class EmotionsProperties {
		public byte getAll() {
			return EMOTIONS_ALL;
		}

		public void setAll(byte all) {
			EMOTIONS_ALL = all;
		}
	}

	/**
	 * 称号父节点：对应配置元素 gameserver.titles。
	 * Title parent node matching the gameserver.titles element.
	 */
	@Getter
	public static class TitlesProperties {
		private final AdditionalProperties additional = new AdditionalProperties();
	}

	/**
	 * 额外称号节点：绑定 gameserver.titles.additional.enable。
	 * Additional-title node binding gameserver.titles.additional.enable.
	 */
	public static class AdditionalProperties {
		public byte getEnable() {
			return TITLES_ADDITIONAL_ENABLE;
		}

		public void setEnable(byte enable) {
			TITLES_ADDITIONAL_ENABLE = enable;
		}
	}

	/**
	 * 印记栏位父节点：对应配置元素 gameserver.quest。
	 * Stigma-slot parent node matching the gameserver.quest element.
	 */
	@Getter
	public static class QuestProperties {
		private final StigmaProperties stigma = new StigmaProperties();
	}

	/**
	 * 印记栏位父节点：对应配置元素 gameserver.quest.stigma。
	 * Stigma-slot parent node matching the gameserver.quest.stigma element.
	 */
	@Getter
	public static class StigmaProperties {
		public byte getSlot() {
			return STIGMA_SLOT_QUEST;
		}

		public void setSlot(byte slot) {
			STIGMA_SLOT_QUEST = slot;
		}
	}

	/**
	 * 灵魂病节点：绑定 gameserver.soulsickness.disable。
	 * Soul-sickness node binding gameserver.soulsickness.disable.
	 */
	public static class SoulsicknessProperties {
		public byte getDisable() {
			return DISABLE_SOULSICKNESS;
		}

		public void setDisable(byte disable) {
			DISABLE_SOULSICKNESS = disable;
		}
	}

	/**
	 * 自动学习父节点：对应配置元素 gameserver.autolearn。
	 * Auto-learn parent node matching the gameserver.autolearn element.
	 */
	@Getter
	public static class AutolearnProperties {
		public byte getStigma() {
			return STIGMA_AUTOLEARN;
		}

		public void setStigma(byte stigma) {
			STIGMA_AUTOLEARN = stigma;
		}
	}

	/**
	 * 角色栏位父节点：对应配置元素 gameserver.character。
	 * Character-slot parent node matching the gameserver.character element.
	 */
	@Getter
	public static class CharacterProperties {
		private final CharacterAdditionalProperties additional = new CharacterAdditionalProperties();
	}

	/**
	 * 角色栏位节点：绑定 gameserver.character.additional.*。
	 * Character-slot node binding gameserver.character.additional.*.
	 */
	public static class CharacterAdditionalProperties {
		public byte getEnable() {
			return CHARACTER_ADDITIONAL_ENABLE;
		}

		public void setEnable(byte enable) {
			CHARACTER_ADDITIONAL_ENABLE = enable;
		}

		public byte getCount() {
			return CHARACTER_ADDITIONAL_COUNT;
		}

		public void setCount(byte count) {
			CHARACTER_ADDITIONAL_COUNT = count;
		}
	}

	/**
	 * 好友列表父节点：对应配置元素 gameserver.advanced。
	 * Friend-list parent node matching the gameserver.advanced element.
	 */
	@Getter
	public static class AdvancedProperties {
		private final FriendlistProperties friendlist = new FriendlistProperties();
	}

	/**
	 * 好友列表节点：绑定 gameserver.advanced.friendlist.*。
	 * Friend-list node binding gameserver.advanced.friendlist.*.
	 */
	public static class FriendlistProperties {
		public byte getEnable() {
			return ADVANCED_FRIENDLIST_ENABLE;
		}

		public void setEnable(byte enable) {
			ADVANCED_FRIENDLIST_ENABLE = enable;
		}

		public int getSize() {
			return ADVANCED_FRIENDLIST_SIZE;
		}

		public void setSize(int size) {
			ADVANCED_FRIENDLIST_SIZE = size;
		}
	}

	/**
	 * 玩家标签父节点：对应配置元素 gameserver.player。
	 * Player-tag parent node matching the gameserver.player element.
	 */
	@Getter
	public static class PlayerProperties {
		private final TagProperties tag = new TagProperties();
	}

	/**
	 * 玩家标签节点：绑定 gameserver.player.tag.*。
	 * Player-tag node binding gameserver.player.tag.*.
	 */
	public static class TagProperties {
		public String get30() {
			return PLAYER_TAG_30;
		}

		public void set30(String tag30) {
			PLAYER_TAG_30 = tag30;
		}

		public String get34() {
			return PLAYER_TAG_34;
		}

		public void set34(String tag34) {
			PLAYER_TAG_34 = tag34;
		}
	}

	/**
	 * 会员标签父节点：对应配置元素 gameserver.membership。
	 * Membership-tag parent node matching the gameserver.membership element.
	 */
	@Getter
	public static class MembershipProperties {
		private final MembershipTagProperties tag = new MembershipTagProperties();
	}

	/**
	 * 会员标签节点：绑定 gameserver.membership.tag.*。
	 * Membership-tag node binding gameserver.membership.tag.*.
	 */
	@Getter
	public static class MembershipTagProperties {
		private final DisplayProperties display = new DisplayProperties();

		public String getVip() {
			return TAG_VIP;
		}

		public void setVip(String vip) {
			TAG_VIP = vip;
		}

		public String getPremium() {
			return TAG_PREMIUM;
		}

		public void setPremium(String premium) {
			TAG_PREMIUM = premium;
		}
	}

	/**
	 * 会员标签显示父节点：对应配置元素 gameserver.membership.tag.display。
	 * Membership-tag display parent node matching the gameserver.membership.tag.display element.
	 */
	@Getter
	public static class DisplayProperties {
		public boolean isEnable() {
			return PREMIUM_TAG_DISPLAY_ENABLE;
		}

		public void setEnable(boolean enable) {
			PREMIUM_TAG_DISPLAY_ENABLE = enable;
		}
	}
}
