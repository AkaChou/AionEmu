package com.aionemu.gameserver.configs.main;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.boot.i18n.I18n;

/**
 * 军团创建、升级、仓库与邀请相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Legion creation, upgrade, warehouse and invite related configuration supporting Spring Boot
 * ConfigurationProperties and static facade access.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "gameserver")
public class LegionConfig {
	/**
	 * 军团名称正则表达式。
	 * Regular expression for legion names.
	 */
	@Property(key = "gameserver.legion.pattern", defaultValue = "[a-zA-Z ]{2,32}")
	public static Pattern LEGION_NAME_PATTERN = Pattern.compile("[a-zA-Z ]{2,32}");
	/**
	 * 军团自我介绍正则表达式。
	 * Regular expression for legion self-intro text.
	 */
	@Property(key = "gameserver.legion.self.intro.pattern", defaultValue = ".{1,32}")
	public static Pattern SELF_INTRO_PATTERN = Pattern.compile(".{1,32}");
	/**
	 * 军团昵称正则表达式。
	 * Regular expression for legion nicknames.
	 */
	@Property(key = "gameserver.legion.nick.name.pattern", defaultValue = ".{1,10}")
	public static Pattern NICKNAME_PATTERN = Pattern.compile(".{1,10}");
	/**
	 * 军团公告正则表达式。
	 * Regular expression for legion announcements.
	 */
	@Property(key = "gameserver.legion.announcement.pattern", defaultValue = ".{1,256}")
	public static Pattern ANNOUNCEMENT_PATTERN = Pattern.compile(".{1,256}");
	/**
	 * 军团解散等待时间（秒）。
	 * Legion disband wait time in seconds.
	 */
	@Property(key = "gameserver.legion.disband.time", defaultValue = "86400")
	public static int LEGION_DISBAND_TIME;
	/**
	 * 创建军团所需基纳。
	 * Kinah required to create a legion.
	 */
	@Property(key = "gameserver.legion.creation.required.kinah", defaultValue = "10000")
	public static int LEGION_CREATE_REQUIRED_KINAH;
	/**
	 * 军团徽记所需基纳。
	 * Kinah required for a legion emblem.
	 */
	@Property(key = "gameserver.legion.emblem.required.kinah", defaultValue = "10000")
	public static int LEGION_EMBLEM_REQUIRED_KINAH;
	/**
	 * 是否启用军团任务升级要求。
	 * Whether legion task requirements are enabled for upgrades.
	 */
	@Property(key = "gameserver.legion.task.requirement.enable", defaultValue = "true")
	public static boolean ENABLE_GUILD_TASK_REQ;
	/**
	 * 升至 2 级所需基纳。
	 * Kinah required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.kinah", defaultValue = "100000")
	public static int LEGION_LEVEL2_REQUIRED_KINAH;
	/**
	 * 升至 3 级所需基纳。
	 * Kinah required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.kinah", defaultValue = "1000000")
	public static int LEGION_LEVEL3_REQUIRED_KINAH;
	/**
	 * 升至 4 级所需基纳。
	 * Kinah required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.kinah", defaultValue = "2000000")
	public static int LEGION_LEVEL4_REQUIRED_KINAH;
	/**
	 * 升至 5 级所需基纳。
	 * Kinah required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.kinah", defaultValue = "6000000")
	public static int LEGION_LEVEL5_REQUIRED_KINAH;
	/**
	 * 升至 6 级所需基纳。
	 * Kinah required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.kinah", defaultValue = "50000000")
	public static int LEGION_LEVEL6_REQUIRED_KINAH;
	/**
	 * 升至 7 级所需基纳。
	 * Kinah required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.kinah", defaultValue = "75000000")
	public static int LEGION_LEVEL7_REQUIRED_KINAH;
	/**
	 * 升至 8 级所需基纳。
	 * Kinah required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.kinah", defaultValue = "100000000")
	public static int LEGION_LEVEL8_REQUIRED_KINAH;
	/**
	 * 升至 2 级所需成员数。
	 * Members required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.members", defaultValue = "10")
	public static int LEGION_LEVEL2_REQUIRED_MEMBERS;
	/**
	 * 升至 3 级所需成员数。
	 * Members required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.members", defaultValue = "20")
	public static int LEGION_LEVEL3_REQUIRED_MEMBERS;
	/**
	 * 升至 4 级所需成员数。
	 * Members required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.members", defaultValue = "30")
	public static int LEGION_LEVEL4_REQUIRED_MEMBERS;
	/**
	 * 升至 5 级所需成员数。
	 * Members required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.members", defaultValue = "40")
	public static int LEGION_LEVEL5_REQUIRED_MEMBERS;
	/**
	 * 升至 6 级所需成员数。
	 * Members required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.members", defaultValue = "50")
	public static int LEGION_LEVEL6_REQUIRED_MEMBERS;
	/**
	 * 升至 7 级所需成员数。
	 * Members required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.members", defaultValue = "60")
	public static int LEGION_LEVEL7_REQUIRED_MEMBERS;
	/**
	 * 升至 8 级所需成员数。
	 * Members required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.members", defaultValue = "70")
	public static int LEGION_LEVEL8_REQUIRED_MEMBERS;
	/**
	 * 升至 2 级所需贡献值。
	 * Contribution required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.contribution", defaultValue = "0")
	public static int LEGION_LEVEL2_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 3 级所需贡献值。
	 * Contribution required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.contribution", defaultValue = "20000")
	public static int LEGION_LEVEL3_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 4 级所需贡献值。
	 * Contribution required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.contribution", defaultValue = "100000")
	public static int LEGION_LEVEL4_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 5 级所需贡献值。
	 * Contribution required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.contribution", defaultValue = "500000")
	public static int LEGION_LEVEL5_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 6 级所需贡献值。
	 * Contribution required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.contribution", defaultValue = "25000000")
	public static int LEGION_LEVEL6_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 7 级所需贡献值。
	 * Contribution required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.contribution", defaultValue = "12500000")
	public static int LEGION_LEVEL7_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 8 级所需贡献值。
	 * Contribution required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.contribution", defaultValue = "62500000")
	public static int LEGION_LEVEL8_REQUIRED_CONTRIBUTION;
	/**
	 * 1 级军团最大成员数。
	 * Max members for legion level 1.
	 */
	@Property(key = "gameserver.legion.level1.max.members", defaultValue = "30")
	public static int LEGION_LEVEL1_MAX_MEMBERS;
	/**
	 * 2 级军团最大成员数。
	 * Max members for legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.max.members", defaultValue = "60")
	public static int LEGION_LEVEL2_MAX_MEMBERS;
	/**
	 * 3 级军团最大成员数。
	 * Max members for legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.max.members", defaultValue = "90")
	public static int LEGION_LEVEL3_MAX_MEMBERS;
	/**
	 * 4 级军团最大成员数。
	 * Max members for legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.max.members", defaultValue = "120")
	public static int LEGION_LEVEL4_MAX_MEMBERS;
	/**
	 * 5 级军团最大成员数。
	 * Max members for legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.max.members", defaultValue = "150")
	public static int LEGION_LEVEL5_MAX_MEMBERS;
	/**
	 * 6 级军团最大成员数。
	 * Max members for legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.max.members", defaultValue = "180")
	public static int LEGION_LEVEL6_MAX_MEMBERS;
	/**
	 * 7 级军团最大成员数。
	 * Max members for legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.max.members", defaultValue = "210")
	public static int LEGION_LEVEL7_MAX_MEMBERS;
	/**
	 * 8 级军团最大成员数。
	 * Max members for legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.max.members", defaultValue = "240")
	public static int LEGION_LEVEL8_MAX_MEMBERS;
	/**
	 * 是否启用军团仓库。
	 * Whether legion warehouse is enabled.
	 */
	@Property(key = "gameserver.legion.warehouse.enable", defaultValue = "true")
	public static boolean LEGION_WAREHOUSE;
	/**
	 * 是否允许邀请其他阵营成员。
	 * Whether inviting members of the other faction is allowed.
	 */
	@Property(key = "gameserver.legion.invite.other.faction", defaultValue = "false")
	public static boolean LEGION_INVITEOTHERFACTION;
	/**
	 * 1 级军团仓库格数。
	 * Legion warehouse slots at level 1.
	 */
	@Property(key = "gameserver.legion.warehouse.level1.slots", defaultValue = "24")
	public static int LWH_LEVEL1_SLOTS;
	/**
	 * 2 级军团仓库格数。
	 * Legion warehouse slots at level 2.
	 */
	@Property(key = "gameserver.legion.warehouse.level2.slots", defaultValue = "32")
	public static int LWH_LEVEL2_SLOTS;
	/**
	 * 3 级军团仓库格数。
	 * Legion warehouse slots at level 3.
	 */
	@Property(key = "gameserver.legion.warehouse.level3.slots", defaultValue = "40")
	public static int LWH_LEVEL3_SLOTS;
	/**
	 * 4 级军团仓库格数。
	 * Legion warehouse slots at level 4.
	 */
	@Property(key = "gameserver.legion.warehouse.level4.slots", defaultValue = "48")
	public static int LWH_LEVEL4_SLOTS;
	/**
	 * 5 级军团仓库格数。
	 * Legion warehouse slots at level 5.
	 */
	@Property(key = "gameserver.legion.warehouse.level5.slots", defaultValue = "56")
	public static int LWH_LEVEL5_SLOTS;
	/**
	 * 6 级军团仓库格数。
	 * Legion warehouse slots at level 6.
	 */
	@Property(key = "gameserver.legion.warehouse.level6.slots", defaultValue = "64")
	public static int LWH_LEVEL6_SLOTS;
	/**
	 * 7 级军团仓库格数。
	 * Legion warehouse slots at level 7.
	 */
	@Property(key = "gameserver.legion.warehouse.level7.slots", defaultValue = "72")
	public static int LWH_LEVEL7_SLOTS;
	/**
	 * 8 级军团仓库格数。
	 * Legion warehouse slots at level 8.
	 */
	@Property(key = "gameserver.legion.warehouse.level8.slots", defaultValue = "80")
	public static int LWH_LEVEL8_SLOTS;

	/**
	 * 军团绑定树 / legion binding tree.
	 */
	@Getter
	private final LegionProperties legion = new LegionProperties();

	/**
	 * 军团域节点：绑定 gameserver.legion.*。
	 * Legion domain node binding gameserver.legion.*.
	 */
	@Getter
	public static class LegionProperties {
		private final DisbandProperties disband = new DisbandProperties();
		private final SelfProperties self = new SelfProperties();
		private final NickProperties nick = new NickProperties();
		private final AnnouncementProperties announcement = new AnnouncementProperties();
		private final CreationProperties creation = new CreationProperties();
		private final EmblemProperties emblem = new EmblemProperties();
		private final TaskProperties task = new TaskProperties();
		private final InviteProperties invite = new InviteProperties();
		private final WarehouseProperties warehouse = new WarehouseProperties();
		private final Level1Properties level1 = new Level1Properties();
		private final Level2Properties level2 = new Level2Properties();
		private final Level3Properties level3 = new Level3Properties();
		private final Level4Properties level4 = new Level4Properties();
		private final Level5Properties level5 = new Level5Properties();
		private final Level6Properties level6 = new Level6Properties();
		private final Level7Properties level7 = new Level7Properties();
		private final Level8Properties level8 = new Level8Properties();

		/**
		 * 兼容 String 形式的军团名称正则，绑定后立即编译。
		 * Binds the legion name regex as String and compiles it immediately.
		 */
		public String getPattern() {
			return LEGION_NAME_PATTERN.pattern();
		}

		public void setPattern(String pattern) {
			try {
				LEGION_NAME_PATTERN = Pattern.compile(pattern);
			} catch (RuntimeException ex) {
				log.warn(I18n.get("log.a3f757c1aaab", pattern), ex);
			}
		}

	}

	/**
	 * 解散等待时间节点：绑定 gameserver.legion.disband.time。
	 * Disband-wait node binding gameserver.legion.disband.time.
	 */
	public static class DisbandProperties {
		public int getTime() {
			return LEGION_DISBAND_TIME;
		}

		public void setTime(int time) {
			LEGION_DISBAND_TIME = time;
		}
	}

	/**
	 * 自我介绍父节点：对应配置元素 gameserver.legion.self。
	 * Self-intro parent node matching the gameserver.legion.self element.
	 */
	@Getter
	public static class SelfProperties {
		private final IntroProperties intro = new IntroProperties();

	}

	/**
	 * 军团自我介绍正则节点：绑定 gameserver.legion.self.intro.pattern。
	 * Self-intro regex node binding gameserver.legion.self.intro.pattern.
	 */
	public static class IntroProperties {
		public String getPattern() {
			return SELF_INTRO_PATTERN.pattern();
		}

		public void setPattern(String pattern) {
			try {
				SELF_INTRO_PATTERN = Pattern.compile(pattern);
			} catch (RuntimeException ex) {
				log.warn(I18n.get("log.b12ca770b287", pattern), ex);
			}
		}
	}

	/**
	 * 昵称父节点：对应配置元素 gameserver.legion.nick。
	 * Nickname parent node matching the gameserver.legion.nick element.
	 */
	@Getter
	public static class NickProperties {
		private final NameProperties name = new NameProperties();

	}

	/**
	 * 军团昵称正则节点：绑定 gameserver.legion.nick.name.pattern。
	 * Nickname regex node binding gameserver.legion.nick.name.pattern.
	 */
	public static class NameProperties {
		public String getPattern() {
			return NICKNAME_PATTERN.pattern();
		}

		public void setPattern(String pattern) {
			try {
				NICKNAME_PATTERN = Pattern.compile(pattern);
			} catch (RuntimeException ex) {
				log.warn(I18n.get("log.7a0dd9fc7a75", pattern), ex);
			}
		}
	}

	/**
	 * 军团公告正则节点：绑定 gameserver.legion.announcement.pattern。
	 * Announcement regex node binding gameserver.legion.announcement.pattern.
	 */
	public static class AnnouncementProperties {
		public String getPattern() {
			return ANNOUNCEMENT_PATTERN.pattern();
		}

		public void setPattern(String pattern) {
			try {
				ANNOUNCEMENT_PATTERN = Pattern.compile(pattern);
			} catch (RuntimeException ex) {
				log.warn(I18n.get("log.7b3550049281", pattern), ex);
			}
		}
	}

	/**
	 * 创建军团节点：绑定 gameserver.legion.creation.*。
	 * Creation node binding gameserver.legion.creation.*.
	 */
	@Getter
	public static class CreationProperties {
		private final RequiredKinahProperties required = new RequiredKinahProperties() {
			@Override
			public int getKinah() {
				return LEGION_CREATE_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_CREATE_REQUIRED_KINAH = kinah;
			}
		};

	}

	/**
	 * 军团徽记节点：绑定 gameserver.legion.emblem.*。
	 * Emblem node binding gameserver.legion.emblem.*.
	 */
	@Getter
	public static class EmblemProperties {
		private final RequiredKinahProperties required = new RequiredKinahProperties() {
			@Override
			public int getKinah() {
				return LEGION_EMBLEM_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_EMBLEM_REQUIRED_KINAH = kinah;
			}
		};

	}

	/**
	 * 所需基纳节点基类：由各域子类覆写指向各自静态字段。
	 * Base required-kinah node; per-domain subclasses point at their own static fields.
	 */
	public static class RequiredKinahProperties {
		public int getKinah() {
			return 0;
		}

		public void setKinah(int kinah) {
		}
	}

	/**
	 * 军团任务节点：绑定 gameserver.legion.task.*。
	 * Task node binding gameserver.legion.task.*.
	 */
	@Getter
	public static class TaskProperties {
		private final RequirementProperties requirement = new RequirementProperties();

	}

	/**
	 * 任务要求节点：绑定 gameserver.legion.task.requirement.enable。
	 * Requirement node binding gameserver.legion.task.requirement.enable.
	 */
	public static class RequirementProperties {
		public boolean isEnable() {
			return ENABLE_GUILD_TASK_REQ;
		}

		public void setEnable(boolean enable) {
			ENABLE_GUILD_TASK_REQ = enable;
		}
	}

	/**
	 * 邀请节点：绑定 gameserver.legion.invite.*。
	 * Invite node binding gameserver.legion.invite.*.
	 */
	@Getter
	public static class InviteProperties {
		private final OtherProperties other = new OtherProperties();

	}

	/**
	 * 跨阵营邀请节点：绑定 gameserver.legion.invite.other.faction。
	 * Other-faction invite node binding gameserver.legion.invite.other.faction.
	 */
	public static class OtherProperties {
		public boolean isFaction() {
			return LEGION_INVITEOTHERFACTION;
		}

		public void setFaction(boolean faction) {
			LEGION_INVITEOTHERFACTION = faction;
		}
	}

	/**
	 * 军团仓库域节点：绑定 gameserver.legion.warehouse.*。
	 * Warehouse domain node binding gameserver.legion.warehouse.*.
	 */
	@Getter
	public static class WarehouseProperties {
		private final WarehouseLevel1Properties level1 = new WarehouseLevel1Properties();
		private final WarehouseLevel2Properties level2 = new WarehouseLevel2Properties();
		private final WarehouseLevel3Properties level3 = new WarehouseLevel3Properties();
		private final WarehouseLevel4Properties level4 = new WarehouseLevel4Properties();
		private final WarehouseLevel5Properties level5 = new WarehouseLevel5Properties();
		private final WarehouseLevel6Properties level6 = new WarehouseLevel6Properties();
		private final WarehouseLevel7Properties level7 = new WarehouseLevel7Properties();
		private final WarehouseLevel8Properties level8 = new WarehouseLevel8Properties();

		public boolean isEnable() {
			return LEGION_WAREHOUSE;
		}

		public void setEnable(boolean enable) {
			LEGION_WAREHOUSE = enable;
		}

	}

	/**
	 * 1 级仓库节点：绑定 gameserver.legion.warehouse.level1.slots。
	 * Warehouse level-1 node binding gameserver.legion.warehouse.level1.slots.
	 */
	public static class WarehouseLevel1Properties {
		public int getSlots() {
			return LWH_LEVEL1_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL1_SLOTS = slots;
		}
	}

	/**
	 * 2 级仓库节点：绑定 gameserver.legion.warehouse.level2.slots。
	 * Warehouse level-2 node binding gameserver.legion.warehouse.level2.slots.
	 */
	public static class WarehouseLevel2Properties {
		public int getSlots() {
			return LWH_LEVEL2_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL2_SLOTS = slots;
		}
	}

	/**
	 * 3 级仓库节点：绑定 gameserver.legion.warehouse.level3.slots。
	 * Warehouse level-3 node binding gameserver.legion.warehouse.level3.slots.
	 */
	public static class WarehouseLevel3Properties {
		public int getSlots() {
			return LWH_LEVEL3_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL3_SLOTS = slots;
		}
	}

	/**
	 * 4 级仓库节点：绑定 gameserver.legion.warehouse.level4.slots。
	 * Warehouse level-4 node binding gameserver.legion.warehouse.level4.slots.
	 */
	public static class WarehouseLevel4Properties {
		public int getSlots() {
			return LWH_LEVEL4_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL4_SLOTS = slots;
		}
	}

	/**
	 * 5 级仓库节点：绑定 gameserver.legion.warehouse.level5.slots。
	 * Warehouse level-5 node binding gameserver.legion.warehouse.level5.slots.
	 */
	public static class WarehouseLevel5Properties {
		public int getSlots() {
			return LWH_LEVEL5_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL5_SLOTS = slots;
		}
	}

	/**
	 * 6 级仓库节点：绑定 gameserver.legion.warehouse.level6.slots。
	 * Warehouse level-6 node binding gameserver.legion.warehouse.level6.slots.
	 */
	public static class WarehouseLevel6Properties {
		public int getSlots() {
			return LWH_LEVEL6_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL6_SLOTS = slots;
		}
	}

	/**
	 * 7 级仓库节点：绑定 gameserver.legion.warehouse.level7.slots。
	 * Warehouse level-7 node binding gameserver.legion.warehouse.level7.slots.
	 */
	public static class WarehouseLevel7Properties {
		public int getSlots() {
			return LWH_LEVEL7_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL7_SLOTS = slots;
		}
	}

	/**
	 * 8 级仓库节点：绑定 gameserver.legion.warehouse.level8.slots。
	 * Warehouse level-8 node binding gameserver.legion.warehouse.level8.slots.
	 */
	public static class WarehouseLevel8Properties {
		public int getSlots() {
			return LWH_LEVEL8_SLOTS;
		}

		public void setSlots(int slots) {
			LWH_LEVEL8_SLOTS = slots;
		}
	}

	/**
	 * 1 级军团节点：绑定 gameserver.legion.level1.*。
	 * Legion level-1 node binding gameserver.legion.level1.*.
	 */
	@Getter
	public static class Level1Properties {
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL1_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL1_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 升级所需资源节点基类：由各级子类覆写指向本级静态字段。
	 * Base required-resources node; per-level subclasses point at their own static fields.
	 */
	public static class RequiredProperties {
		public int getKinah() {
			return 0;
		}

		public void setKinah(int kinah) {
		}

		public int getMembers() {
			return 0;
		}

		public void setMembers(int members) {
		}

		public int getContribution() {
			return 0;
		}

		public void setContribution(int contribution) {
		}
	}

	/**
	 * 最大成员数节点基类：由各级子类覆写指向本级静态字段。
	 * Base max-members node; per-level subclasses point at their own static fields.
	 */
	public static class MaxProperties {
		public int getMembers() {
			return 0;
		}

		public void setMembers(int members) {
		}
	}

	/**
	 * 2 级军团节点：绑定 gameserver.legion.level2.*。
	 * Legion level-2 node binding gameserver.legion.level2.*.
	 */
	@Getter
	public static class Level2Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL2_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL2_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL2_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL2_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL2_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL2_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL2_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL2_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 3 级军团节点：绑定 gameserver.legion.level3.*。
	 * Legion level-3 node binding gameserver.legion.level3.*.
	 */
	@Getter
	public static class Level3Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL3_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL3_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL3_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL3_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL3_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL3_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL3_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL3_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 4 级军团节点：绑定 gameserver.legion.level4.*。
	 * Legion level-4 node binding gameserver.legion.level4.*.
	 */
	@Getter
	public static class Level4Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL4_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL4_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL4_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL4_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL4_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL4_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL4_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL4_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 5 级军团节点：绑定 gameserver.legion.level5.*。
	 * Legion level-5 node binding gameserver.legion.level5.*.
	 */
	@Getter
	public static class Level5Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL5_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL5_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL5_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL5_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL5_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL5_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL5_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL5_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 6 级军团节点：绑定 gameserver.legion.level6.*。
	 * Legion level-6 node binding gameserver.legion.level6.*.
	 */
	@Getter
	public static class Level6Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL6_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL6_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL6_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL6_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL6_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL6_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL6_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL6_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 7 级军团节点：绑定 gameserver.legion.level7.*。
	 * Legion level-7 node binding gameserver.legion.level7.*.
	 */
	@Getter
	public static class Level7Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL7_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL7_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL7_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL7_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL7_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL7_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL7_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL7_MAX_MEMBERS = members;
			}
		};

	}

	/**
	 * 8 级军团节点：绑定 gameserver.legion.level8.*。
	 * Legion level-8 node binding gameserver.legion.level8.*.
	 */
	@Getter
	public static class Level8Properties {
		private final RequiredProperties required = new RequiredProperties() {
			@Override
			public int getKinah() {
				return LEGION_LEVEL8_REQUIRED_KINAH;
			}

			@Override
			public void setKinah(int kinah) {
				LEGION_LEVEL8_REQUIRED_KINAH = kinah;
			}

			@Override
			public int getMembers() {
				return LEGION_LEVEL8_REQUIRED_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL8_REQUIRED_MEMBERS = members;
			}

			@Override
			public int getContribution() {
				return LEGION_LEVEL8_REQUIRED_CONTRIBUTION;
			}

			@Override
			public void setContribution(int contribution) {
				LEGION_LEVEL8_REQUIRED_CONTRIBUTION = contribution;
			}
		};
		private final MaxProperties max = new MaxProperties() {
			@Override
			public int getMembers() {
				return LEGION_LEVEL8_MAX_MEMBERS;
			}

			@Override
			public void setMembers(int members) {
				LEGION_LEVEL8_MAX_MEMBERS = members;
			}
		};

	}
}
