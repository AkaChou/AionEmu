package com.aionemu.gameserver.configs.administration;

import com.aionemu.commons.configuration.Property;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员 / GM 权限与外观相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * GM privilege and appearance related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "gameserver")
public class AdminConfig {

	/**
	 * 可使用 GM 特殊技能的最低权限等级。
	 * Minimum access level required for GM special skills.
	 */
	@Property(key = "gameserver.administration.gmskills", defaultValue = "3")
	public static int GM_SPECIAL_SKILLS;

	/**
	 * 判定为 GM 的最低权限等级。
	 * Minimum access level treated as GM.
	 */
	@Property(key = "gameserver.administration.gmlevel", defaultValue = "3")
	public static int GM_LEVEL;

	/**
	 * 可打开 GM 面板的最低权限等级。
	 * Minimum access level required to open the GM panel.
	 */
	@Property(key = "gameserver.administration.gmpanel", defaultValue = "3")
	public static int GM_PANEL;

	/**
	 * 可自由飞行的最低权限等级。
	 * Minimum access level required for free flight.
	 */
	@Property(key = "gameserver.administration.flight.freefly", defaultValue = "3")
	public static int GM_FLIGHT_FREE;

	/**
	 * 可无限飞行的最低权限等级。
	 * Minimum access level required for unlimited flight.
	 */
	@Property(key = "gameserver.administration.flight.unlimited", defaultValue = "3")
	public static int GM_FLIGHT_UNLIMITED;

	/**
	 * 可强制开门的最低权限等级。
	 * Minimum access level required to open doors forcibly.
	 */
	@Property(key = "gameserver.administration.doors.opening", defaultValue = "3")
	public static int DOORS_OPEN;

	/**
	 * 管理员死亡后自动复活的最低权限等级。
	 * Minimum access level for admin auto-resurrection.
	 */
	@Property(key = "gameserver.administration.auto.res", defaultValue = "3")
	public static int ADMIN_AUTO_RES;

	/**
	 * 可忽略副本进入条件的最低权限等级。
	 * Minimum access level to bypass instance enter requirements.
	 */
	@Property(key = "gameserver.administration.instancereq", defaultValue = "3")
	public static int INSTANCE_REQ;

	/**
	 * 可查看其他玩家详情的最低权限等级。
	 * Minimum access level to view other players' details.
	 */
	@Property(key = "gameserver.administration.view.player", defaultValue = "3")
	public static int ADMIN_VIEW_DETAILS;

	/**
	 * GM 登录时是否默认隐身。
	 * Whether GMs connect in invisible mode by default.
	 */
	@Property(key = "gameserver.administration.invis.gm.connection", defaultValue = "false")
	public static boolean INVISIBLE_GM_CONNECTION;

	/**
	 * GM 登录时的敌对模式（如 Normal）。
	 * Enmity mode applied when a GM connects (e.g. Normal).
	 */
	@Property(key = "gameserver.administration.enemity.gm.connection", defaultValue = "Normal")
	public static String ENEMITY_MODE_GM_CONNECTION;

	/**
	 * GM 登录时是否默认无敌。
	 * Whether GMs connect invulnerable by default.
	 */
	@Property(key = "gameserver.administration.invul.gm.connection", defaultValue = "false")
	public static boolean INVULNERABLE_GM_CONNECTION;

	/**
	 * GM 登录时是否默认开启特视。
	 * Whether GMs connect with special vision enabled by default.
	 */
	@Property(key = "gameserver.administration.vision.gm.connection", defaultValue = "false")
	public static boolean VISION_GM_CONNECTION;

	/**
	 * GM 登录时是否默认拒收密语。
	 * Whether GMs connect with whisper blocked by default.
	 */
	@Property(key = "gameserver.administration.whisper.gm.connection", defaultValue = "false")
	public static boolean WHISPER_GM_CONNECTION;

	/**
	 * GM 登录时是否默认进入 GM 模式。
	 * Whether GMs connect in GM mode by default.
	 */
	@Property(key = "gameserver.administration.gm.mode.connection", defaultValue = "false")
	public static boolean GM_MODE_CONNECTION;

	/**
	 * 是否启用管理员交易物品限制。
	 * Whether admin trade-item restriction is enabled.
	 */
	@Property(key = "gameserver.administration.trade.item.restriction", defaultValue = "false")
	public static boolean ENABLE_TRADEITEM_RESTRICTION;

	/**
	 * 是否启用管理员名称前缀标签。
	 * Whether admin name tags are enabled.
	 */
	@Property(key = "gameserver.admin.tag.enable", defaultValue = "true")
	public static boolean ADMIN_TAG_ENABLE;

	/**
	 * 权限等级 1 的名称前缀（%s 为角色名）。
	 * Name tag for access level 1 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.1", defaultValue = "<Support> %s")
	public static String ADMIN_TAG_1;

	/**
	 * 权限等级 2 的名称前缀（%s 为角色名）。
	 * Name tag for access level 2 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.2", defaultValue = "<Jr-GM> %s")
	public static String ADMIN_TAG_2;

	/**
	 * 权限等级 3 的名称前缀（%s 为角色名）。
	 * Name tag for access level 3 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.3", defaultValue = "<GM> %s")
	public static String ADMIN_TAG_3;

	/**
	 * 权限等级 4 的名称前缀（%s 为角色名）。
	 * Name tag for access level 4 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.4", defaultValue = "<Head-GM> %s")
	public static String ADMIN_TAG_4;

	/**
	 * 权限等级 5 的名称前缀（%s 为角色名）。
	 * Name tag for access level 5 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.5", defaultValue = "<Admin> %s")
	public static String ADMIN_TAG_5;

	/**
	 * 登录时全服通告的权限等级列表（* 表示全部）。
	 * Access levels announced on login (* means all).
	 */
	@Property(key = "gameserver.admin.announce.levels", defaultValue = "*")
	public static String ANNOUNCE_LEVEL_LIST;

	private final AdministrationProperties administration = new AdministrationProperties();
	private final AdminProperties admin = new AdminProperties();

	@Getter
	public static class AdministrationProperties {
		private final FlightProperties flight = new FlightProperties();
		private final DoorsProperties doors = new DoorsProperties();
		private final AutoProperties auto = new AutoProperties();
		private final ViewProperties view = new ViewProperties();
		private final InvisProperties invis = new InvisProperties();
		private final EnemityProperties enemity = new EnemityProperties();
		private final InvulProperties invul = new InvulProperties();
		private final VisionProperties vision = new VisionProperties();
		private final WhisperProperties whisper = new WhisperProperties();
		private final GmProperties gm = new GmProperties();
		private final TradeProperties trade = new TradeProperties();

		public int getGmskills() {
			return GM_SPECIAL_SKILLS;
		}

		public void setGmskills(int gmskills) {
			GM_SPECIAL_SKILLS = gmskills;
		}

		public int getGmlevel() {
			return GM_LEVEL;
		}

		public void setGmlevel(int gmlevel) {
			GM_LEVEL = gmlevel;
		}

		public int getGmpanel() {
			return GM_PANEL;
		}

		public void setGmpanel(int gmpanel) {
			GM_PANEL = gmpanel;
		}

		public int getInstancereq() {
			return INSTANCE_REQ;
		}

		public void setInstancereq(int instancereq) {
			INSTANCE_REQ = instancereq;
		}

		public static class FlightProperties {
			public int getFreefly() {
				return GM_FLIGHT_FREE;
			}

			public void setFreefly(int freefly) {
				GM_FLIGHT_FREE = freefly;
			}

			public int getUnlimited() {
				return GM_FLIGHT_UNLIMITED;
			}

			public void setUnlimited(int unlimited) {
				GM_FLIGHT_UNLIMITED = unlimited;
			}
		}

		public static class DoorsProperties {
			public int getOpening() {
				return DOORS_OPEN;
			}

			public void setOpening(int opening) {
				DOORS_OPEN = opening;
			}
		}

		public static class AutoProperties {
			public int getRes() {
				return ADMIN_AUTO_RES;
			}

			public void setRes(int res) {
				ADMIN_AUTO_RES = res;
			}
		}

		public static class ViewProperties {
			public int getPlayer() {
				return ADMIN_VIEW_DETAILS;
			}

			public void setPlayer(int player) {
				ADMIN_VIEW_DETAILS = player;
			}
		}

		@Getter
		public static class InvisProperties {
			private final ConnectionProperties gm = new ConnectionProperties();

			public static class ConnectionProperties {
				public boolean isConnection() {
					return INVISIBLE_GM_CONNECTION;
				}

				public void setConnection(boolean connection) {
					INVISIBLE_GM_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class EnemityProperties {
			private final ConnectionProperties gm = new ConnectionProperties();

			public static class ConnectionProperties {
				public String getConnection() {
					return ENEMITY_MODE_GM_CONNECTION;
				}

				public void setConnection(String connection) {
					ENEMITY_MODE_GM_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class InvulProperties {
			private final ConnectionProperties gm = new ConnectionProperties();

			public static class ConnectionProperties {
				public boolean isConnection() {
					return INVULNERABLE_GM_CONNECTION;
				}

				public void setConnection(boolean connection) {
					INVULNERABLE_GM_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class VisionProperties {
			private final ConnectionProperties gm = new ConnectionProperties();

			public static class ConnectionProperties {
				public boolean isConnection() {
					return VISION_GM_CONNECTION;
				}

				public void setConnection(boolean connection) {
					VISION_GM_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class WhisperProperties {
			private final ConnectionProperties gm = new ConnectionProperties();

			public static class ConnectionProperties {
				public boolean isConnection() {
					return WHISPER_GM_CONNECTION;
				}

				public void setConnection(boolean connection) {
					WHISPER_GM_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class GmProperties {
			private final ModeProperties mode = new ModeProperties();

			public static class ModeProperties {
				public boolean isConnection() {
					return GM_MODE_CONNECTION;
				}

				public void setConnection(boolean connection) {
					GM_MODE_CONNECTION = connection;
				}
			}
		}

		@Getter
		public static class TradeProperties {
			private final ItemProperties item = new ItemProperties();

			public static class ItemProperties {
				public boolean isRestriction() {
					return ENABLE_TRADEITEM_RESTRICTION;
				}

				public void setRestriction(boolean restriction) {
					ENABLE_TRADEITEM_RESTRICTION = restriction;
				}
			}
		}
	}

	public static class AdminProperties {
		private final TagProperties tag = new TagProperties();
		private final AnnounceProperties announce = new AnnounceProperties();

		public TagProperties getTag() {
			return tag;
		}

		public AnnounceProperties getAnnounce() {
			return announce;
		}

		public static class TagProperties {
			public boolean isEnable() {
				return ADMIN_TAG_ENABLE;
			}

			public void setEnable(boolean enable) {
				ADMIN_TAG_ENABLE = enable;
			}

			public String get1() {
				return ADMIN_TAG_1;
			}

			public void set1(String tag1) {
				ADMIN_TAG_1 = tag1;
			}

			public String get2() {
				return ADMIN_TAG_2;
			}

			public void set2(String tag2) {
				ADMIN_TAG_2 = tag2;
			}

			public String get3() {
				return ADMIN_TAG_3;
			}

			public void set3(String tag3) {
				ADMIN_TAG_3 = tag3;
			}

			public String get4() {
				return ADMIN_TAG_4;
			}

			public void set4(String tag4) {
				ADMIN_TAG_4 = tag4;
			}

			public String get5() {
				return ADMIN_TAG_5;
			}

			public void set5(String tag5) {
				ADMIN_TAG_5 = tag5;
			}

			public String getTag1() {
				return ADMIN_TAG_1;
			}

			public void setTag1(String tag1) {
				ADMIN_TAG_1 = tag1;
			}

			public String getTag2() {
				return ADMIN_TAG_2;
			}

			public void setTag2(String tag2) {
				ADMIN_TAG_2 = tag2;
			}

			public String getTag3() {
				return ADMIN_TAG_3;
			}

			public void setTag3(String tag3) {
				ADMIN_TAG_3 = tag3;
			}

			public String getTag4() {
				return ADMIN_TAG_4;
			}

			public void setTag4(String tag4) {
				ADMIN_TAG_4 = tag4;
			}

			public String getTag5() {
				return ADMIN_TAG_5;
			}

			public void setTag5(String tag5) {
				ADMIN_TAG_5 = tag5;
			}
		}

		public static class AnnounceProperties {
			public String getLevels() {
				return ANNOUNCE_LEVEL_LIST;
			}

			public void setLevels(String levels) {
				ANNOUNCE_LEVEL_LIST = levels;
			}
		}
	}
}
