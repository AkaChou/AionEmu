package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.controllers.FlyController;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.attack.PlayerAggroList;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.controllers.movement.PlayerMoveController;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerVarsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowList;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.SummonedObject;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank.AbyssRankUpdateType;
import com.aionemu.gameserver.model.gameobjects.player.FriendList.Status;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSettingList;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2p;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.ingameshop.InGameShop;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceGroup;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;
import com.aionemu.gameserver.model.templates.flypath.FlyPathEntry;
import com.aionemu.gameserver.model.templates.item.DisassembleItem;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.npc.AbyssNpcType;
import com.aionemu.gameserver.model.templates.ride.RideInfo;
import com.aionemu.gameserver.model.templates.stats.PlayerStatsTemplate;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamPath;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_TOLL_INFO;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.conquerors.Conqueror;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.bg.Battleground;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;
import com.aionemu.gameserver.services.protectors.Protector;
import com.aionemu.gameserver.skillengine.condition.ChainCondition;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.RebirthEffect;
import com.aionemu.gameserver.skillengine.effect.ResurrectBaseEffect;
import com.aionemu.gameserver.skillengine.model.ChainSkills;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.task.CraftingTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.rates.Rates;
import com.aionemu.gameserver.utils.rates.RegularRates;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家游戏对象。
 * Player game object.
 */

@Getter
@Setter
public class Player extends Creature {

	public RideInfo ride;
	public InRoll inRoll;
	public InGameShop inGameShop;
	public WindstreamPath windstreamPath;
	public WindstreamPath pendingWindstreamPath;
	private PlayerAppearance playerAppearance;
	/**
	 * 仅用于 Size 管理员命令。
	 * Only use for the Size admin command.
	 */
	private PlayerAppearance savedPlayerAppearance;
	private final PlayerCommonData playerCommonData;
	private final Account playerAccount;
	/**
	 * @param legionMember the legionMember to set
	 */
	private LegionMember legionMember;
	private MacroList macroList;
	private PlayerSkillList skillList;
	private PlayerEquippedStigmaList equipped_stigma;
	/**
	 * 获取 playersfriend 列表。
	 * Gets this players Friend List
	 *
	 * @return FriendList
	 */
	private FriendList friendList;
	private BlockList blockList;
	private PetList toyPetList;
	private MinionList minionList;
	private Mailbox mailbox;
	/**
	 * @return the player private store
	 */
	private PrivateStore store;
	private TitleList titleList;
	/**
	 * @return the questStatesList
	 */
	private QuestStateList questStateList;
	private final Set<Integer> pendingQuestShares = ConcurrentHashMap.newKeySet();
	private volatile NpcQuestDialogSelection npcQuestDialogSelection;
	private volatile DialogSelectRepeat dialogSelectRepeat;
	/**
	 * 是否为该玩家开启任务追踪日志；仅内存状态，不持久化。
	 * Whether quest trace logging is enabled for this player; in-memory only, not persisted.
	 */
	private volatile boolean questTraceEnabled = false;
	private RecipeList recipeList;
	private List<House> houses;
	private ResponseRequester requester;
	/**
	 * @return 该玩家是否正在寻找小队 / Is this player looking for a group, true or false
	 */
	private boolean lookingForGroup = false;
	private Storage inventory;
	private final Storage[] petBag = new Storage[StorageType.PET_BAG_MAX - StorageType.PET_BAG_MIN + 1];
	private final Storage[] cabinets = new Storage[StorageType.HOUSE_WH_MAX - StorageType.HOUSE_WH_MIN + 1];
	private Storage regularWarehouse;
	private Storage accountWarehouse;
	/**
	 * @return the inventory
	 */
	private Equipment equipment;
	private EquipmentSettingList equipmentSettingList;
	private HouseRegistry houseRegistry;
	/**
	 * @return the playerStatsTemplate
	 */
	private PlayerStatsTemplate playerStatsTemplate;
	private final AbsoluteStatOwner absStatsHolder;
	/**
	 * @return the playerSettings
	 */
	private PlayerSettings playerSettings;
	private com.aionemu.gameserver.model.team2.group.PlayerGroup playerGroup2;
	private PlayerAllianceGroup playerAllianceGroup;
	/**
	 * @return the abyssRank
	 */
	private AbyssRank abyssRank;
	/**
	 * @return the npcFactions
	 */
	private NpcFactions npcFactions;
	/**
	 * @param rates the rates to set
	 */
	private Rates rates;
	/**
	 * @return 0：普通；1：飞行；2：滑翔。 / 0: regular, 1: fly, 2: glide
	 */
	private int flyState = 0;
	/**
	 * @return the isTrading
	 */
	private boolean isTrading;
	/**
	 * @return the prisonTimer
	 */
	private long prisonTimer = 0;
	private boolean isGathering;
	/**
	 * @return the time in ms of start prison
	 */
	private long startPrison;
	/**
	 * 检查玩家是否无敌。
	 * Checks whether the player is invulnerable
	 *
	 * @return 是否无敌 / whether invulnerable
	 */
	private boolean invul;
	/**
	 * @return the flyController
	 */
	private FlyController flyController;
	/**
	 * @param craftingTask
	 */
	private CraftingTask craftingTask;
	/**
	 * @param flightTeleportId
	 */
	private int flightTeleportId;
	/**
	 * @param flightDistance
	 */
	private int flightDistance;
	/**
	 * @return the summon
	 */
	private Summon summon;
	private SummonedObject<?> summonedObj;
	/**
	 * @param toyPet the toyPet to set
	 */
	private Pet toyPet;
	/**
	 * @return the minions
	 */
	private Minion minion;
	/**
	 * @return
	 */
	private Kisk kisk;
	private boolean isResByPlayer = false;
	private int resurrectionSkill = 0;
	private boolean isFlyingBeforeDeath = false;
	/**
	 * @param isGagged the isGagged to set
	 */
	private boolean isGagged = false;
	private boolean edit_mode = false;
	private Npc questFollowingNpc = null;
	private Npc postman = null;
	private boolean isInResurrectPosState = false;
	/**
	 * @param value Resurrection Positional X value to set
	 */
	private float resPosX = 0;
	/**
	 * @param value Resurrection Positional Y value to set
	 */
	private float resPosY = 0;
	/**
	 * @param value Resurrection Positional Z value to set
	 */
	private float resPosZ = 0;
	/**
	 * @param value 禁消耗飞行值效果状态 / status of NoFpConsum Effect
	 */
	private boolean underNoFPConsum = false;
	/**
	 * @param isAdminTeleportation
	 */
	private boolean isAdminTeleportation = false;
	private boolean cooldownZero = false;
	private boolean isUnderInvulnerableWing = false;
	private boolean isFlying = false;
	private boolean isWispable = true;
	private boolean isCommandUsed = false;
	private int abyssRankListUpdateMask = 0;
	private BindPointPosition bindPoint;
	/** 物品使用冷却注册表 / Item-use cooldown registry */
	private final PlayerCooldowns cooldowns = new PlayerCooldowns();
	/**
	 * @return 传送门冷却列表 / portal cooldown list
	 */
	private PortalCooldownList portalCooldownList;
	private CraftCooldownList craftCooldownList;
	private HouseObjectCooldownList houseObjectCooldownList;
	private long nextSkillUse;
	private long nextSummonSkillUse;
	private ChainSkills chainSkills;
	private final Map<AttackStatus, Long> lastCounterSkill = new HashMap<AttackStatus, Long>();
	/**
	 * @return the dualEffectValue
	 */
	private int dualEffectValue = 0;
	private int rawKillcount = 0;
	private int spreeLevel = 0;
	private boolean hasBonus;
	private int bonusId = 0;
	private boolean hasAbyssBonus;
	private int abyssId = 0;
	/**
	 * 玩家的静态信息。
	 * Static information for players
	 */
	private static final int CUBE_SPACE = 9;
	private static final int WAREHOUSE_SPACE = 8;
	private boolean isAttackMode = false;
	private long gatherableTimer = 0;
	private long stopGatherable;
	private String captchaWord;
	private byte[] captchaImage;
	private float instanceStartPosX, instanceStartPosY, instanceStartPosZ;
	private int rebirthResurrectPercent = 1;
	private int rebirthSkill = 0;
	/**
	 * 设置 connection 玩家。
	 * Set connection of this player
	 *
	 * @param clientConnection
	 */
	private AionConnection clientConnection;
	private FlyPathEntry flyLocationId;
	private long flyStartTime;
	/**
	 * @return Returns the emotions.
	 */
	private EmotionList emotions;
	/**
	 * @return the motions
	 */
	private MotionList motions;
	/**
	 * @return the flyReuseTime
	 */
	private long flyReuseTime;
	private boolean isMentor;
	private long lastMsgTime = 0;
	private int floodMsgCount = 0;
	private long onlineTime = 0;
	private int lootingNpcOid;
	private boolean rebirthRevive;
	private int subtractedSupplementsCount;
	private int subtractedSupplementId;
	private int portAnimation;
	private boolean isInSprintMode;
	private ItemUseObserver craftObserver;
	private List<ActionObserver> rideObservers;
	private List<ActionObserver> hotTeleObservers;
	private Protector protectorList;
	private Conqueror conquerorList;
	byte buildingOwnerStates = PlayerHouseOwnerFlags.BUY_STUDIO_ALLOWED.getId();
	private int battleReturnMap;
	private float[] battleReturnCoords;
	public int speedHackCounter;
	public int abnormalHackCounter;
	public WorldPosition prevPos;
	public long prevPosUT;
	public byte prevMoveType;
	private final PlayerVarsDAO daoVars = DAOManager.getDAO(PlayerVarsDAO.class);
	private Map<String, Object> vars = new LinkedHashMap<>();
	private boolean robot = false;
	private int robotId = 0;
	public int A_STATION_TYPE = 0;
	private boolean isOnAStation = false;
	private int playersBonusId = 0;
	private int transformModelId;
	private int transformItemId;
	private int transformPanelId;
	private boolean isInWindstream = false;
	private int silenceReportCount = 0;
	private boolean isInCrazy;
	private int rndPoint = 0;
	private int crazyKillcount = 0;
	private int crazyLevel = 0;
	private F2p f2p;
	private PlayerCPList cp;
	private PlayerABList ab;
	private PlayerEventWindowList ew;
	private PlayerWardrobeList wardrobe;
	private PlayerLunaShop lunaShop;
	private PlayerSweep shugoSweep;
	private int linkedSkill;
	private int stigmaSet;
	private int goldenStarOfLodi;
	private int unkPoint1;
	private int cp_slot1 = 0, cp_slot2 = 0, cp_slot3 = 0, cp_slot4 = 0, cp_slot5 = 0, cp_slot6 = 0;
	private boolean enchantBoost;
	private boolean authorizeBoost;
	private boolean setMinionSpawned;
	/** 事件物品每日限购注册表 / Event-item daily purchase-limit registry */
	private final PlayerItemDailyLimits itemDailyLimits = new PlayerItemDailyLimits();
	/**
	 * 月华骰子游戏。
	 * Luna Dice Game
	 */
	private int LunaDiceGame;
	/** 返回 luna dice game try / Returns the luna dice game try */
	private int LunaDiceGameTry = 0;
	// PvP 系统： / Pvp System:
	/**
	 * @return 是否无规则状态 / Whether lawless
	 */
	private boolean lawless = false;
	/**
	 * @return PVP 系统 / PVP System
	 */
	private boolean bandit = false;
	/** 设置 battleground / Sets the battleground */
	private Battleground battleground = null;
	/** 返回 last action / Returns the last action */
	private long lastAction = 0;
	/** 设置 bg index / Sets the bg index */
	private int bgIndex = 0;
	/** 设置 spectating / Sets the spectating */
	private boolean isSpectating = false;
	/** 设置 total kills / Sets the total kills */
	private int totalKills = 0;
	private int arenaKillStreak = 0;
	/** 返回 bandit kill streak / Returns the bandit kill streak */
	private int banditKillStreak = 0;
	/**
	 * @return 是否离开。 / Whether afk
	 */
	private boolean isAfk;
	/** 是否处于自由混战 / Whether ffa */
	private boolean isFFA = false;
	private int hallOfTenacityCoupleId = 0;
	private int hallOfTenacityVSId = 0;
	private int hallOfTenacityOpponentId = 0;

	/** 是否决斗 / Whether in duel */
	private boolean isInDuel;
	/**
	 * 玩家技能动画列表。
	 * Player Skill Animation List
	 */
	private SkillSkinList skillSkinList;
	/** 是否盗贼 / Whether thieves*/
	private boolean isThieves = false;
	/** 是否处于盗贼复仇决斗。 / Whether in a thieves revenge duel. */
	private boolean thievesDuel;
	/** 设置 thieves / Sets the thieves */
	private ThievesStatusList thieves;
	/**
	 * 活动调用与注册。
	 * EventCaller + Event Reg
	 */
	private int checkpoints;
	/** 返回数量玩家集合 / Returns the count players */
	private int countPlayers;
	/**
	 * @return 是否已注册活动 / Whether reged event
	 */
	private boolean isRegedEvent = false;
	/**
	 * @return 活动是否已开始 / Whether event started
	 */
	private boolean isEventStarted = false;
	/** 返回 queued players / Returns the queued players */
	public List<Player> QueuedPlayers;
	/**
	 * 自定义 PvE 与 PK 系统相关变量。
	 * These variables are for the custom PvE and PK system
	 */
	private boolean isInPkMode;
	/**
	 * @return 是否处于 PvE 模式 / Whether in PvE mode
	 */
	private boolean isInPvEMode;
	// 这些变量用于自定义 RP 与 GM 系统 / These variables are for the custom RP and GM system
	/**
	 * @return GM 模式 / GM Mode
	 */
	private boolean isGmMode = false;
	private long creationDay;

	public long getCreationDate() {
		Timestamp creationDate = playerCommonData.getCreationDate();
		if (creationDate == null) {
			return 0;
		}
		return creationDate.getTime();
	}

	public void setCreationDataDay(long i) {
		this.creationDay = i;
	}

	public long getCreationDataDay() {
		return creationDay;
	}

	private Player(PlayerCommonData plCommonData) {
		super(plCommonData.getPlayerObjId(), new PlayerController(), null, plCommonData, null);
		this.playerCommonData = plCommonData;
		this.playerAccount = new Account(0);
		this.absStatsHolder = new AbsoluteStatOwner(this, 0);
	}

	public Player(PlayerController controller, PlayerCommonData plCommonData, PlayerAppearance appereance, Account account) {
		super(plCommonData.getPlayerObjId(), controller, null, plCommonData, plCommonData.getPosition());
		this.playerCommonData = plCommonData;
		this.playerAppearance = appereance;
		this.playerAccount = account;
		this.requester = new ResponseRequester(this);
		this.questStateList = new QuestStateList();
		this.titleList = new TitleList();
		this.equipmentSettingList = new EquipmentSettingList(this);
		this.portalCooldownList = new PortalCooldownList(this);
		this.craftCooldownList = new CraftCooldownList(this);
		houseObjectCooldownList = new HouseObjectCooldownList(this);
		this.toyPetList = new PetList(this);
		this.minionList = new MinionList(this);
		controller.setOwner(this);
		moveController = new PlayerMoveController(this);
		plCommonData.setBoundingRadius(new BoundRadius(0.5f, 0.5f, getPlayerAppearance().getBoundHeight()));
		setPlayerStatsTemplate(DataManager.PLAYER_STATS_DATA.getTemplate(this));
		setGameStats(new PlayerGameStats(this));
		setLifeStats(new PlayerLifeStats(this));
		inGameShop = new InGameShop();
		protectorList = new Protector(this);
		conquerorList = new Conqueror(this);
		absStatsHolder = new AbsoluteStatOwner(this, 0);
		this.setMinionSpawned = false;
	}

	public boolean isInPlayerMode(PlayerMode mode) {
		return PlayerActions.isInPlayerMode(this, mode);
	}

	public void setPlayerMode(PlayerMode mode, Object obj) {
		PlayerActions.setPlayerMode(this, mode, obj);
	}

	public void unsetPlayerMode(PlayerMode mode) {
		PlayerActions.unsetPlayerMode(this, mode);
	}

	@Override
	public PlayerMoveController getMoveController() {
		return (PlayerMoveController) super.getMoveController();
	}

	@Override
	protected final AggroList createAggroList() {
		return new PlayerAggroList(this);
	}

	public PlayerCommonData getCommonData() {
		return playerCommonData;
	}

	@Override
	public String getName() {
		return playerCommonData.getName();
	}

	public PlayerEquippedStigmaList getEquipedStigmaList() {
		return equipped_stigma;
	}

	public void setEquipedStigmaList(PlayerEquippedStigmaList list) {
		this.equipped_stigma = list;
	}

	/**
	 * @return the toyPet
	 */
	public Pet getPet() {
		return toyPet;
	}

	public void setMinionSpawned(boolean setMinionSpawned) {
		this.setMinionSpawned = setMinionSpawned;
	}

	public boolean isMinionSpawned() {
		return setMinionSpawned;
	}

	public boolean isNotGatherable() {
		return gatherableTimer != 0;
	}

	public void setGatherableTimer(long gatherableTimer) {
		if (gatherableTimer < 0) {
			gatherableTimer = 0;
		}
		this.gatherableTimer = gatherableTimer;
	}

	public final PetList getPetList() {
		return toyPetList;
	}

	public final MinionList getMinionList() {
		return minionList;
	}

	@Override
	public PlayerLifeStats getLifeStats() {
		return (PlayerLifeStats) super.getLifeStats();
	}

	@Override
	public PlayerGameStats getGameStats() {
		return (PlayerGameStats) super.getGameStats();
	}

	/**
	 * 获取该玩家的 ResponseRequester。
	 * Gets the ResponseRequester for this player
	 *
	 * @return ResponseRequester / response requester
	 */
	public ResponseRequester getResponseRequester() {
		return requester;
	}

	public boolean isOnline() {
		return getClientConnection() != null;
	}

	public void setQuestExpands(int questExpands) {
		this.playerCommonData.setQuestExpands(questExpands);
		getInventory().setLimit(getInventory().getLimit() + (questExpands + getNpcExpands()) * CUBE_SPACE);
	}

	public int getQuestExpands() {
		return this.playerCommonData.getQuestExpands();
	}

	public void setNpcExpands(int npcExpands) {
		this.playerCommonData.setNpcExpands(npcExpands);
		getInventory().setLimit(getInventory().getLimit() + (npcExpands + getQuestExpands()) * CUBE_SPACE);
	}

	public int getNpcExpands() {
		return this.playerCommonData.getNpcExpands();
	}

	public PlayerClass getPlayerClass() {
		return playerCommonData.getPlayerClass();
	}

	public Gender getGender() {
		return playerCommonData.getGender();
	}

	/**
	 * 返回该玩家的 PlayerController。
	 * Returns the PlayerController of this Player
	 *
	 * @return PlayerController / player controller
	 */
	@Override
	public PlayerController getController() {
		return (PlayerController) super.getController();
	}

	@Override
	public byte getLevel() {
		return (byte) playerCommonData.getLevel();
	}

	public EquipmentSettingList getEquipmentSettingList() {
		if (equipmentSettingList == null) {
			equipmentSettingList = new EquipmentSettingList(this);
		}
		return equipmentSettingList;
	}

	public void setEquipmentSettingList(EquipmentSettingList equipmentSettingList) {
		this.equipmentSettingList = equipmentSettingList;
		if (this.equipmentSettingList != null) {
			this.equipmentSettingList.setOwner(this);
		}
	}

	/** Records a server-issued quest-share offer for this session. */
	public void addPendingQuestShare(int questId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		pendingQuestShares.add(questId);
	}

	/** Consumes a server-issued quest-share offer exactly once. */
	public boolean consumePendingQuestShare(int questId) {
		return questId > 0 && pendingQuestShares.remove(questId);
	}

	/** 记录从 NPC 任务列表点击进入的任务对话授权。 / Records task-dialog authorization from an NPC quest-list click. */
	public void rememberNpcQuestDialogSelection(int interactionObjectId, int questId) {
		if (interactionObjectId <= 0 || questId <= 0) {
			npcQuestDialogSelection = null;
			return;
		}
		npcQuestDialogSelection = new NpcQuestDialogSelection(interactionObjectId, questId);
	}

	/** 判断任务对话是否由同一 NPC 的任务列表点击授权。 / Checks whether a quest dialog was authorized by the same NPC's quest-list click. */
	public boolean hasNpcQuestDialogSelection(int interactionObjectId, int questId) {
		NpcQuestDialogSelection selection = npcQuestDialogSelection;
		return selection != null && selection.interactionObjectId() == interactionObjectId
			&& selection.questId() == questId;
	}

	/** 返回同一 NPC 的任务列表授权任务 ID；没有授权时返回 0。 / Returns the quest id authorized by the same NPC's quest row, or 0 when absent. */
	public int getNpcQuestDialogSelectionQuestId(int interactionObjectId) {
		NpcQuestDialogSelection selection = npcQuestDialogSelection;
		return selection != null && selection.interactionObjectId() == interactionObjectId ? selection.questId() : 0;
	}

	/** 清除 NPC 任务对话授权。 / Clears NPC quest-dialog authorization. */
	public void clearNpcQuestDialogSelection() {
		npcQuestDialogSelection = null;
	}

	/** 返回上一次客户端对话选择的重发跟踪状态；没有记录时为 null。 / Returns the repeat-tracking state of the last client dialog selection, or null when absent. */
	public DialogSelectRepeat getDialogSelectRepeat() {
		return dialogSelectRepeat;
	}

	/** 记录本次客户端对话选择的重发跟踪状态。 / Stores the repeat-tracking state of the current client dialog selection. */
	public void setDialogSelectRepeat(DialogSelectRepeat repeat) {
		dialogSelectRepeat = repeat;
	}

	/** 清除客户端对话选择的重发跟踪。 / Clears client dialog-selection repeat tracking. */
	public void clearDialogSelectRepeat() {
		dialogSelectRepeat = null;
	}

	private record NpcQuestDialogSelection(int interactionObjectId, int questId) {
	}

	/**
	 * @param storage the inventory to set Inventory should be set right after player object is created
	 */
	public void setStorage(Storage storage, StorageType storageType) {
		PlayerStorageRegistry.setStorage(this, storage, storageType);
	}

	/**
	 * @param storageType
	 * @return
	 */
	public IStorage getStorage(int storageType) {
		return PlayerStorageRegistry.getStorage(this, storageType);
	}

	/**
	 * @return 来自 UPDATE_REQUIRED 仓库与装备的物品。 / Items from UPDATE_REQUIRED storages and equipment
	 */
	public List<Item> getDirtyItemsToUpdate() {
		return PlayerStorageRegistry.getDirtyItemsToUpdate(this);
	}

	public void markDirtyItemContainersStored() {
		PlayerStorageRegistry.markDirtyItemContainersStored(this);
	}

	/**
	 * @return all items in player-owned storages and equipment
	 */
	public List<Item> getAllItems() {
		return PlayerStorageRegistry.getAllItems(this);
	}

	public void setTitleList(TitleList titleList) {
		if (havePermission(MembershipConfig.TITLES_ADDITIONAL_ENABLE)) {
			titleList.addEntry(102, 0);
			titleList.addEntry(103, 0);
			titleList.addEntry(104, 0);
			titleList.addEntry(105, 0);
			titleList.addEntry(106, 0);
			titleList.addEntry(146, 0);
			titleList.addEntry(151, 0);
			titleList.addEntry(152, 0);
			titleList.addEntry(160, 0);
			titleList.addEntry(161, 0);
		}
		this.titleList = titleList;
		titleList.setOwner(this);
	}

	public PlayerGroup getPlayerGroup2() {
		return playerGroup2;
	}

	public void setPlayerGroup2(PlayerGroup playerGroup) {
		this.playerGroup2 = playerGroup;
	}

	@Override
	public PlayerEffectController getEffectController() {
		return (PlayerEffectController) super.getEffectController();
	}

	public void onLoggedIn() {
		friendList.setStatus(Status.ONLINE, getCommonData());
	}

	public void onLoggedOut() {
		requester.denyAll();
		friendList.setStatus(FriendList.Status.OFFLINE, getCommonData());
	}

	/**
	 * @return 返回若为真则有有效 LegionMember。 / Returns true if has valid LegionMember
	 */
	public boolean isLegionMember() {
		return legionMember != null;
	}

	/**
	 * @return the legion
	 */
	public Legion getLegion() {
		return legionMember != null ? legionMember.getLegion() : null;
	}

	/**
	 * 检查对象 ID 是否相同。
	 * Check whether the object ids are equal.
	 *
	 * @return 对象 ID 相同返回 true / true if the object id is the same
	 */
	public boolean sameObjectId(int objectId) {
		return this.getObjectId() == objectId;
	}

	/**
	 * @return true if a player has a store opened
	 */
	public boolean hasStore() {
		return getStore() != null;
	}

	/**
	 * 从玩家身上移除军团。
	 * Removes legion from player
	 */
	public void resetLegionMember() {
		setLegionMember(null);
	}

	public boolean isInGroup2() {
		return playerGroup2 != null;
	}

	/**
	 * @return 该玩家的访问等级。 byte / Access level of this player byte
	 */
	public byte getAccessLevel() {
		return playerAccount.getAccessLevel();
	}

	/**
	 * @return 该玩家的会员等级。 / Membership of this player
	 */
	public byte getMembership() {
		if (playerAccount == null) {
			return 0x00;
		}
		return playerAccount.getMembership();
	}

	/**
	 * @return 该玩家的账号名。 / accountName of this player int
	 */
	public String getAcountName() {
		return playerAccount.getName();
	}

	/**
	 * @return the rates
	 */
	public Rates getRates() {
		if (rates == null) {
			rates = new RegularRates();
		}
		return rates;
	}

	/**
	 * @return warehouse size
	 */
	public int getWarehouseSize() {
		return this.playerCommonData.getWarehouseSize();
	}

	/**
	 * @param warehouseSize
	 */
	public void setWarehouseSize(int warehouseSize) {
		this.playerCommonData.setWarehouseSize(warehouseSize);
		getWarehouse().setLimit(getWarehouse().getLimit() + (warehouseSize * WAREHOUSE_SPACE));
	}

	/**
	 * @return regularWarehouse
	 */
	public Storage getWarehouse() {
		return regularWarehouse;
	}

	public void setFlyState(int flyState) {
		this.flyState = flyState;
		if (flyState == 1) {
			setFlyingMode(true);
		} else if (flyState == 0) {
			setFlyingMode(false);
		}
	}

	public void setIsGathering(boolean isGathering) {
		this.isGathering = isGathering;
	}

	/**
	 * @return the isInPrison
	 */
	public boolean isInPrison() {
		return prisonTimer != 0;
	}

	/**
	 * @param prisonTimer the prisonTimer to set
	 */
	public void setPrisonTimer(long prisonTimer) {
		if (prisonTimer < 0) {
			prisonTimer = 0;
		}
		this.prisonTimer = prisonTimer;
	}

	/**
	 * @return
	 */
	public boolean isProtectionActive() {
		return isInVisualState(CreatureVisualState.BLINKING);
	}

	public int getLastOnline() {
		Timestamp lastOnline = playerCommonData.getLastOnline();
		if (lastOnline == null || isOnline()) {
			return 0;
		}
		return (int) (lastOnline.getTime() / 1000);
	}

	/**
	 * @param path
	 */
	public void setCurrentFlypath(FlyPathEntry path) {
		this.flyLocationId = path;
		if (path != null) {
			this.flyStartTime = System.currentTimeMillis();
		} else {
			this.flyStartTime = 0;
		}
	}

	/**
	 * @return
	 */
	public boolean isUsingFlyTeleport() {
		return isInState(CreatureState.FLIGHT_TELEPORT) && flightTeleportId != 0;
	}

	public boolean isGM() {
		return getAccessLevel() >= AdminConfig.GM_LEVEL;
	}

	@Override
	public boolean isEnemy(Creature creature) {
		return creature.isEnemyFrom(this);
	}

	@Override
	public boolean isEnemyFrom(Npc npc) {
		return npc.isAttackableNpc() || isAggroIconTo(npc) || isBandit();
	}

	@Override
	public boolean isEnemyFrom(Player enemy) {
		if ((this.getAdminEnmity() > 1 || enemy.getAdminEnmity() > 1)) {
			return false;
		}
		if (enemy.isInPvEMode() || this.isInPvEMode()) {
			return false;
		}
		if (this.getObjectId() == enemy.getObjectId()) {
			return false;
		}
		if (enemy.getBattleground() != null && this.getBattleground() != null) {
			return true;
		}
		if (GameFeatureServices.ffaService().isInArena(enemy) && enemy.isFFA()) {
			return true;
		}
		if (!enemy.getRace().equals(getRace()) || getController().isDueling(enemy) || enemy.isBandit()) {
			return true;
		}
		if (enemy.isInPkMode() || this.isInPkMode()) {
			return !this.isInSameTeam(enemy);
		}
		if (enemy.isBandit() || this.isBandit()) {
			return true;
		}
		return PlayerPvpRules.canPvP(this, enemy) || this.getController().isDueling(enemy);
	}

	public boolean isAggroIconTo(Player player) {
		if (getAdminEnmity() > 1 || player.getAdminEnmity() > 1) {
			return true;
		}
		if (player.isBandit() || this.isBandit()) {
			return true;
		}
		return !player.getRace().equals(getRace()) || player.getBattleground() != null || GameFeatureServices.ffaService().isInArena(player) && player.isFFA() || player.isBandit();
	}

	public boolean isInSameTeam(Player player) {
		if (isInGroup2() && player.isInGroup2()) {
			return getPlayerGroup2().getTeamId().equals(player.getPlayerGroup2().getTeamId());
		} else if (isInAlliance2() && player.isInAlliance2()) {
			return getPlayerAlliance2().getObjectId().equals(player.getPlayerAlliance2().getObjectId());
		} else if (isInLeague() && player.isInLeague()) {
			return getPlayerAllianceGroup2().getObjectId().equals(player.getPlayerAllianceGroup2().getObjectId());
		}
		return false;
	}

	public boolean canSee(Creature creature) {
		if (creature.isInVisualState(CreatureVisualState.BLINKING)) {
			return true;
		}
		if (((creature instanceof Player)) && (isInSameTeam((Player) creature))) {
			return true;
		}
		if (((creature instanceof Trap)) && (((Trap) creature).getCreator().getObjectId() == getObjectId())) {
			return true;
		}
		return creature.getVisualState() <= getSeeState();
	}

	@Override
	public TribeClass getTribe() {
		TribeClass transformTribe = getTransformModel().getTribe();
		if (transformTribe != null) {
			return transformTribe;
		}
		return getRace() == Race.ELYOS ? TribeClass.PC : TribeClass.PC_DARK;
	}

	@Override
	public boolean isAggroFrom(Npc npc) {
		if (!isAggroIconTo(npc)) {
			return false;
		}
		if (npc.getTribe().isGuard() || npc.getObjectTemplate().getAbyssNpcType() != AbyssNpcType.NONE) {
			return true;
		}
		return isWithinAggroLevelRange(npc.getLevel(), getLevel());
	}

	static boolean isWithinAggroLevelRange(int npcLevel, int playerLevel) {
		return playerLevel < npcLevel + AIConfig.AGGRO_LEVEL_IMMUNE;
	}

	/**
	 * 用于 {@code SM_NPC_INFO}，判断是否显示仇恨图标。
	 * Used by {@code SM_NPC_INFO} to decide whether to show the aggro icon.
	 *
	 * @param npc 待检查 NPC / NPC to check
	 * @return 应显示仇恨图标时为 true / true if the aggro icon should be shown
	 */
	public boolean isAggroIconTo(Npc npc) {
		Race race = npc.getRace();
		TribeClass tribe = npc.getTribe();
		if (getAdminEnmity() == 1 || getAdminEnmity() == 3) {
			return true;
		}
		// 按部落例外 / Exception by Tribe
		if (tribe == TribeClass.USEALL) {
			return false;
		}
		// AbyssType != NONE -> SiegeNpc。
		if (npc.getObjectTemplate().getAbyssNpcType() != AbyssNpcType.NONE) {
			return checkSiegeRelations(npc);
		}
		if (npc.getObjectTemplate().getNpcType().equals(NpcType.PEACE)) {
			return false;
		}
		if (npc.getObjectTemplate().getNpcType().equals(NpcType.INVULNERABLE)) {
			return false;
		}
		if (npc.getObjectTemplate().getNpcType() == NpcType.NON_ATTACKABLE && (npc.getWorldId() == 310010000 || npc.getWorldId() == 320010000)) {
			return false;
		}
		switch (getTribe()) {
		case PC:
			if (race == Race.ASMODIANS || tribe == null || tribe.isDarkGuard()) {
				return true;
			}
			return DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(tribe, TribeClass.PC);
		case PC_DARK:
			if (race == Race.ELYOS || tribe == null || tribe.isLightGuard()) {
				return true;
			}
			return DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(tribe, TribeClass.PC_DARK);
		default:
			break;
		}
		return false;
	}

	/*
	 * Siege npc relations to player
	 */
	public boolean checkSiegeRelations(Npc npc) {
		Race race = npc.getRace();
		NpcType npcType = npc.getNpcType();
		TribeClass tribe = npc.getTribe();
		// 神器不能是敌人 / Artifact can't be Enemy
		if (npc.getObjectTemplate().getAbyssNpcType().equals(AbyssNpcType.ARTIFACT)) {
			return false;
		}
		// 例外友方龙族 / Exception friendly Balaur's
		if (race == Race.DRAKAN && npcType == NpcType.NON_ATTACKABLE) {
			return false;
		}
		switch (getRace()) {
		case ELYOS:
			// 天族之门 / Elyos Gate
			if (race == Race.PC_LIGHT_CASTLE_DOOR)
				return false;
			// 天族将军 / Elyos General
			if (race == Race.GCHIEF_LIGHT)
				return false;
			// 天族传送者 / Elyos Teleporter
			if (race == Race.TELEPORTER && tribe == TribeClass.GENERAL)
				return false;
			// 天族护盾发生器 / Elyos Shield generators
			if ((race == Race.CONSTRUCT || race == Race.BARRIER) && (tribe == TribeClass.GENERAL || tribe == TribeClass.F4GUARD_LIGHT))
				return false;
			break;
		case ASMODIANS:
			// 魔族之门 / Asmo Gate
			if (race == Race.PC_DARK_CASTLE_DOOR)
				return false;
			// 魔族将军 / Asmo General
			if (race == Race.GCHIEF_DARK)
				return false;
			// 魔族传送者 / Asmo Teleporter
			if (race == Race.TELEPORTER && tribe == TribeClass.GENERAL_DARK)
				return false;
			// 天族护盾发生器 / Elyos Shield generators
			if ((race == Race.CONSTRUCT || race == Race.BARRIER) && (tribe == TribeClass.GENERAL_DARK || tribe == TribeClass.F4GUARD_DARK)) {
				return false;
			}
			break;
		default:
			break;
		}
		return getRace() != race;
	}

	/**
	 * @param limits
	 * @return
	 */
	public boolean isItemUseDisabled(ItemUseLimits limits) {
		return cooldowns.isUseDisabled(limits);
	}

	/**
	 * @param delayId
	 * @return
	 */
	public long getItemCoolDown(int delayId) {
		return cooldowns.getCoolDown(delayId);
	}

	/**
	 * @param delayId
	 * @param time
	 * @param useDelay
	 */
	public void addItemCoolDown(int delayId, long time, int useDelay) {
		cooldowns.addCoolDown(delayId, time, useDelay);
	}

	/**
	 * @param itemMask
	 */
	public void removeItemCoolDown(int itemMask) {
		cooldowns.removeCoolDown(itemMask);
	}

	/**
	 * 返回原始物品冷却表。
	 * Returns the backing item cooldown map.
	 *
	 * @return 冷却表（live 视图），未创建时为 null / live cooldown map, or null when not created
	 */
	public Map<Integer, ItemCooldown> getItemCoolDowns() {
		return cooldowns.getItemCoolDowns();
	}

	/**
	 * @return isAdminTeleportation
	 */
	public boolean getAdminTeleportation() {
		return isAdminTeleportation;
	}

	public final boolean isCoolDownZero() {
		return cooldownZero;
	}

	public final void setCoolDownZero(boolean cooldownZero) {
		this.cooldownZero = cooldownZero;
	}

	public void setPlayerResActivate(boolean isActivated) {
		this.isResByPlayer = isActivated;
	}

	public boolean getResStatus() {
		return isResByPlayer;
	}

	public void setIsFlyingBeforeDeath(boolean isActivated) {
		this.isFlyingBeforeDeath = isActivated;
	}

	public boolean getIsFlyingBeforeDeath() {
		return isFlyingBeforeDeath;
	}

	public com.aionemu.gameserver.model.team2.alliance.PlayerAlliance getPlayerAlliance2() {
		return playerAllianceGroup != null ? playerAllianceGroup.getAlliance() : null;
	}

	public PlayerAllianceGroup getPlayerAllianceGroup2() {
		return playerAllianceGroup;
	}

	public boolean isInAlliance2() {
		return playerAllianceGroup != null;
	}

	public void setPlayerAllianceGroup2(PlayerAllianceGroup playerAllianceGroup) {
		this.playerAllianceGroup = playerAllianceGroup;
	}

	public final boolean isInLeague() {
		return isInAlliance2() && getPlayerAlliance2().isInLeague();
	}

	public final boolean isInTeam() {
		return isInGroup2() || isInAlliance2();
	}

	/**
	 * @return 当前队伍（小队或联盟）或 null / current {@link PlayerGroup}, {@link PlayerAlliance} or null
	 */
	public final TemporaryPlayerTeam<? extends TeamMember<Player>> getCurrentTeam() {
		return isInGroup2() ? getPlayerGroup2() : getPlayerAlliance2();
	}

	/**
	 * @return 当前队伍（小队或联盟小队）或 null / current {@link PlayerGroup}, {@link PlayerAllianceGroup} or null
	 */
	public final TemporaryPlayerTeam<? extends TeamMember<Player>> getCurrentGroup() {
		return isInGroup2() ? getPlayerGroup2() : getPlayerAllianceGroup2();
	}

	/**
	 * @return 当前队伍 ID / current team id
	 */
	public final int getCurrentTeamId() {
		return isInTeam() ? getCurrentTeam().getTeamId() : 0;
	}

	public Protector getProtectorInfo() {
		return protectorList;
	}

	public void setProtectorInfo(Protector protector) {
		protectorList = protector;
	}

	public Conqueror getConquerorInfo() {
		return conquerorList;
	}

	public void setConquerorInfo(Conqueror conqueror) {
		conquerorList = conqueror;
	}

	public void setEditMode(boolean edit_mode) {
		this.edit_mode = edit_mode;
	}

	public boolean isInEditMode() {
		return edit_mode;
	}

	/**
	 * 任务完成。
	 * Quest completion
	 */
	public boolean isCompleteQuest(int questId) {
		QuestState qs = getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		return qs.getStatus() == QuestStatus.COMPLETE;
	}

	/**
	 * 连锁技能。
	 * chain skills
	 */
	public ChainSkills getChainSkills() {
		if (this.chainSkills == null) {
			this.chainSkills = new ChainSkills();
		}
		return this.chainSkills;
	}

	public void setLastCounterSkill(AttackStatus status) {
		long time = System.currentTimeMillis();
		// 闪避 / Dodge
		if (AttackStatus.getBaseStatus(status) == AttackStatus.DODGE && PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.WARRIOR || PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.SCOUT || PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.TECHNIST) {
			this.lastCounterSkill.put(AttackStatus.DODGE, time);
		}
		// 招架 / Parry
		else if (AttackStatus.getBaseStatus(status) == AttackStatus.PARRY && PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.WARRIOR || PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.PRIEST || PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.TECHNIST) {
			this.lastCounterSkill.put(AttackStatus.PARRY, time);
		}
		// 格挡 / Block
		else if (AttackStatus.getBaseStatus(status) == AttackStatus.BLOCK && PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.WARRIOR) {
			this.lastCounterSkill.put(AttackStatus.BLOCK, time);
		}
		// 抵抗 / Resist
		else if (AttackStatus.getBaseStatus(status) == AttackStatus.RESIST && PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.WARRIOR || PlayerClass.getStartingClassFor(getPlayerClass()) == PlayerClass.TECHNIST) {
			this.lastCounterSkill.put(AttackStatus.RESIST, time);
		}
	}

	public long getLastCounterSkill(AttackStatus status) {
		if (this.lastCounterSkill.get(status) == null) {
			return 0;
		}
		return this.lastCounterSkill.get(status);
	}

	/**
	 * @return 复活位置状态 / the Resurrection Positional State
	 */
	public boolean isInResPostState() {
		return this.isInResurrectPosState;
	}

	/**
	 * @param value Resurrection Positional State to set
	 */
	public void setResPosState(boolean value) {
		this.isInResurrectPosState = value;
	}

	public boolean isInSiegeWorld() {
		switch (getWorldId()) {
		case 400010000:
		case 400020000: // 4.7
		case 400040000: // 4.7
		case 400050000: // 4.7
		case 400060000: // 4.7
		case 600090000: // 4.7
		case 210100000: // 5.8
		case 220110000: // 5.8
			return true;
		}
		return false;
	}

	/**
	 * @return 玩家处于禁飞效果下返回 true / true if player is under NoFly Effect
	 */
	public boolean isUnderNoFly() {
		return this.getEffectController().isAbnormalSet(AbnormalState.NOFLY);
	}

	public void setInstanceStartPos(float instanceStartPosX, float instanceStartPosY, float instanceStartPosZ) {
		this.instanceStartPosX = instanceStartPosX;
		this.instanceStartPosY = instanceStartPosY;
		this.instanceStartPosZ = instanceStartPosZ;
	}

	public float getInstanceStartPosX() {
		return instanceStartPosX;
	}

	public float getInstanceStartPosY() {
		return instanceStartPosY;
	}

	public float getInstanceStartPosZ() {
		return instanceStartPosZ;
	}

	public boolean havePermission(byte perm) {
		return playerAccount.getMembership() >= perm;
	}

	@Override
	public ItemAttackType getAttackType() {
		Item weapon = getEquipment().getMainHandWeapon();
		if (weapon != null) {
			return weapon.getItemTemplate().getAttackType();
		}
		return ItemAttackType.PHYSICAL;
	}

	public FlyPathEntry getCurrentFlyPath() {
		return flyLocationId;
	}

	public void setUnWispable() {
		this.isWispable = false;
	}

	public void setWispable() {
		this.isWispable = true;
	}

	public boolean isInvulnerableWing() {
		return this.isUnderInvulnerableWing;
	}

	public void setInvulnerableWing(boolean value) {
		this.isUnderInvulnerableWing = value;
	}

	public void resetAbyssRankListUpdated() {
		this.abyssRankListUpdateMask = 0;
	}

	public void setAbyssRankListUpdated(AbyssRankUpdateType type) {
		this.abyssRankListUpdateMask |= type.value();
	}

	public boolean isAbyssRankListUpdated(AbyssRankUpdateType type) {
		return (this.abyssRankListUpdateMask & type.value()) == type.value();
	}

	public void addSalvationPoints(long points) {
		this.playerCommonData.addSalvationPoints(points);
		PacketSendUtility.sendPacket(this, new SM_STATS_INFO(this));
	}

	public long getCurrentSalvationPercent() {
		return this.playerCommonData.getCurrentSalvationPercent();
	}

	@Override
	public byte isPlayer() {
		if (this.isGM()) {
			return 2;
		} else {
			return 1;
		}
	}

	public void setTransformed(boolean value) {
		getTransformModel().setActive(value);
	}

	public boolean isTransformed() {
		return getTransformModel().isActive();
	}

	/**
	 * @param value flying mode flag to set
	 */
	public void setFlyingMode(boolean value) {
		this.isFlying = value;
	}

	/**
	 * @return 玩家处于飞行模式下返回 true / true if player is in Flying mode
	 */
	public boolean isInFlyingMode() {
		return this.isFlying;
	}

	/**
	 * 复活石使用顺序由背包最高槽位决定（若有两种类型可能用错）。
	 * Stone Use Order determined by highest inventory slot. If player has two types, wrong one might be used.
	 */
	public Item getSelfRezStone() {
		Item item = null;
		item = getReviveStone(161001001);
		item = getReviveStone(161001004);
		item = getReviveStone(161001005);
		if (item == null) {
			item = getReviveStone(161000003); // Reviving Elemental Stone.
		}
		if (item == null) {
			item = getReviveStone(161000004); // Tombstone Of Revival.
		}
		if (item == null) {
			item = getReviveStone(161000005); // Reviving Elemental Stone.
		}
		return item;
	}

	/**
	 * @param stoneId 复活石物品 ID / stone item id
	 * @return 复活石物品或 null / stoneItem or null
	 */
	private Item getReviveStone(int stoneId) {
		Item item = getInventory().getFirstItemByItemId(stoneId);
		if (item != null && isItemUseDisabled(item.getItemTemplate().getUseLimits())) {
			item = null;
		}
		return item;
	}

	/**
	 * @return 判断物品是否可用于自我复活。 / Need to find how an item is determined as able to self-rez. boolean can self rez with item
	 */
	public boolean haveSelfRezItem() {
		return (getSelfRezStone() != null);
	}

	/**
	 * @return 重生效果 ID 为 160。 / Rebirth Effect is id 160.
	 */
	public boolean haveSelfRezEffect() {
		if (getAccessLevel() >= AdminConfig.ADMIN_AUTO_RES) {
			return true;
		}
		// 存储效果信息。 / Store the effect info.
		List<Effect> effects = getEffectController().getAbnormalEffects();
		for (Effect effect : effects) {
			for (EffectTemplate template : effect.getEffectTemplates()) {
				if (template.getEffectid() == 160 && (template instanceof RebirthEffect rebirthEffect)) {
					setRebirthResurrectPercent(rebirthEffect.getResurrectPercent());
					setRebirthSkill(rebirthEffect.getSkillId());
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasResurrectBase() {
		List<Effect> effects = getEffectController().getAbnormalEffects();
		for (Effect effect : effects) {
			for (EffectTemplate template : effect.getEffectTemplates()) {
				if (template.getEffectid() == 160 && (template instanceof ResurrectBaseEffect)) {
					return true;
				}
			}
		}
		return false;
	}

	public void unsetResPosState() {
		if (isInResPostState()) {
			setResPosState(false);
			setResPosX(0);
			setResPosY(0);
			setResPosZ(0);
		}
	}

	public LootGroupRules getLootGroupRules() {
		if (isInGroup2()) {
			return getPlayerGroup2().getLootGroupRules();
		}
		if (isInAlliance2()) {
			return getPlayerAlliance2().getLootGroupRules();
		}
		return null;
	}

	public boolean isLooting() {
		return lootingNpcOid != 0;
	}

	public final boolean isMentor() {
		return isMentor;
	}

	public final void setMentor(boolean isMentor) {
		this.isMentor = isMentor;
	}

	@Override
	public Race getRace() {
		return playerCommonData.getRace();
	}

	public boolean hasVar(String key) {
		return vars.containsKey(key);
	}

	public void setVar(String key, Object value, boolean sql) {
		vars.put(key, value);
		if (sql) {
			daoVars.set(this.getObjectId(), key, value);
		}
	}

	public Object getVar(String key) {
		return this.vars.get(key);
	}

	public int getVarInt(String key) {
		Object o = this.vars.get(key);
		if (o != null) {
			return Integer.parseInt(o.toString());
		}
		return 0;
	}

	public String getVarStr(String key) {
		Object o = this.vars.get(key);
		if (o != null) {
			return o.toString();
		}
		return null;
	}

	public void setVars(Map<String, Object> map) {
		this.vars = map;
	}

	@Override
	public int getSkillCooldown(SkillTemplate template) {
		return isCoolDownZero() ? 0 : template.getCooldown();
	}

	@Override
	public int getItemCooldown(ItemTemplate template) {
		return isCoolDownZero() ? 0 : template.getUseLimits().getDelayTime();
	}

	public void setLastMessageTime() {
		if ((System.currentTimeMillis() - lastMsgTime) / 1000 < SecurityConfig.FLOOD_DELAY) {
			floodMsgCount++;
		} else {
			floodMsgCount = 0;
		}
		lastMsgTime = System.currentTimeMillis();
	}

	public int floodMsgCount() {
		return floodMsgCount;
	}

	public void setOnlineTime() {
		onlineTime = System.currentTimeMillis();
	}

	/*
	 * return online time in sec
	 */
	public long getOnlineTime() {
		return (System.currentTimeMillis() - onlineTime) / 1000;
	}

	public boolean isCommandInUse() {
		return isCommandUsed;
	}

	public boolean canUseRebirthRevive() {
		return rebirthRevive;
	}

	public void subtractSupplements(int count, int supplementId) {
		subtractedSupplementsCount = count;
		subtractedSupplementId = supplementId;
	}

	public void updateSupplements() {
		if ((subtractedSupplementId == 0) || (subtractedSupplementsCount == 0)) {
			return;
		}
		getInventory().decreaseByItemId(subtractedSupplementId, subtractedSupplementsCount);
		subtractedSupplementsCount = 0;
		subtractedSupplementId = 0;
	}

	public boolean isSkillDisabled(SkillTemplate template) {
		ChainCondition cond = template.getChainCondition();
		if (cond != null && cond.getSelfCount() > 0) {
			int chainCount = getChainSkills().getChainCount(this, template, cond.getCategory());
			if (chainCount > 0 && chainCount < cond.getSelfCount() && getChainSkills().chainSkillEnabled(cond.getCategory(), cond.getTime())) {
				return false;
			}
		}
		return super.isSkillDisabled(template);
	}

	public List<House> getHouses() {
		return PlayerHouses.getHouses(this);
	}

	/**
	 * 返回原始房屋缓存，不触发惰性加载（仅同包房屋域使用）。
	 * Returns the raw house cache without lazy loading (same-package housing domain only).
	 *
	 * @return 原始房屋缓存 / raw house cache
	 */
	List<House> getHousesOrNull() {
		return houses;
	}

	public void resetHouses() {
		PlayerHouses.resetHouses(this);
	}

	public House getActiveHouse() {
		return PlayerHouses.getActiveHouse(this);
	}

	public int getHouseOwnerId() {
		return PlayerHouses.getHouseOwnerId(this);
	}

	public boolean isBuildingInState(PlayerHouseOwnerFlags state) {
		return PlayerHouses.isBuildingInState(this, state);
	}

	public void setBuildingOwnerState(byte state) {
		PlayerHouses.setBuildingOwnerState(this, state);
	}

	public void unsetBuildingOwnerState(byte state) {
		PlayerHouses.unsetBuildingOwnerState(this, state);
	}

	public void setBattleReturnCoords(int mapId, float[] coords) {
		this.battleReturnMap = mapId;
		this.battleReturnCoords = coords;
	}

	public void setSprintMode(boolean isInSprintMode) {
		this.isInSprintMode = isInSprintMode;
	}

	public void setRideObservers(ActionObserver observer) {
		if (rideObservers == null) {
			rideObservers = new ArrayList<ActionObserver>(3);
		}
		rideObservers.add(observer);
	}

	public String getCustomTag(boolean isForChatCommands) {
		return PlayerTags.getCustomTag(this, isForChatCommands);
	}

	public int getRawKillCount() {
		return rawKillcount;
	}

	public void setRawKillCount(int count) {
		rawKillcount = count;
	}

	public AbsoluteStatOwner getAbsoluteStats() {
		return absStatsHolder;
	}

	public boolean hasBonus() {
		return hasBonus;
	}

	public void setBonus(boolean hasBonus) {
		this.hasBonus = hasBonus;
	}

	public boolean hasAbyssBonus() {
		return hasAbyssBonus;
	}

	public void setAbyssBonus(boolean hasAbyssBonus) {
		this.hasAbyssBonus = hasAbyssBonus;
	}

	public boolean isUseRobot() {
		return robot;
	}

	public void setUseRobot(boolean robot) {
		this.robot = robot;
	}

	public int getTransformedModelId() {
		return transformModelId;
	}

	public void setTransformedModelId(int id) {
		transformModelId = id;
	}

	public int getTransformedItemId() {
		return transformItemId;
	}

	public void setTransformedItemId(int id) {
		transformItemId = id;
	}

	public int getTransformedPanelId() {
		return transformPanelId;
	}

	public void setTransformedPanelId(int id) {
		transformPanelId = id;
	}

	public int getRndCrazy() {
		return rndPoint;
	}

	public void setRndCrazy(int rnd) {
		rndPoint = rnd;
	}

	public int getCrazyKillCount() {
		return crazyKillcount;
	}

	public void setCrazyKillCount(int count) {
		crazyKillcount = count;
	}

	public PlayerCPList getCP() {
		return cp;
	}

	public void setCP(PlayerCPList cp) {
		this.cp = cp;
	}

	public PlayerABList getAtreianBestiary() {
		return ab;
	}

	public void setAtreianBestiary(PlayerABList ab) {
		this.ab = ab;
	}

	public PlayerEventWindowList getEventWindow() {
		return ew;
	}

	public void setEventWindow(PlayerEventWindowList ew) {
		this.ew = ew;
	}

	public void setHotTeleObservers(ActionObserver observer) {
		if (hotTeleObservers == null) {
			hotTeleObservers = new ArrayList<ActionObserver>(3);
		}
		hotTeleObservers.add(observer);
	}

	public PlayerUpgradeArcade getUpgradeArcade() {
		return playerCommonData.getUpgradeArcade();
	}

	public void setPlayerUpgradeArcade(PlayerUpgradeArcade pua) {
	}

	public void setPlayerLunaShop(PlayerLunaShop pls) {
		this.lunaShop = pls;
	}

	public PlayerLunaShop getPlayerLunaShop() {
		return lunaShop;
	}

	public PlayerSweep getPlayerShugoSweep() {
		return shugoSweep;
	}

	public void setPlayerShugoSweep(PlayerSweep ps) {
		this.shugoSweep = ps;
	}

	public boolean isArchDaeva() {
		return getCommonData().isArchDaeva();
	}

	public int getCreativityPoint() {
		return getCommonData().getCreativityPoint();
	}

	public void setCreativityPoint(int point) {
		getCommonData().setCreativityPoint(point);
	}

	public int getCPStep() {
		return getCommonData().getCPStep();
	}

	public void setCPStep(int step) {
		getCommonData().setCPStep(step);
	}

	public int getStoneCreativityPoint() {
		return getCommonData().getStoneCreativityPoint();
	}

	public void setStoneCreativityPoint(int point) {
		getCommonData().setStoneCreativityPoint(point);
	}

	public int getCPSlot1() {
		return cp_slot1;
	}

	public void setCPSlot1(int point) {
		this.cp_slot1 = point;
	}

	public int getCPSlot2() {
		return cp_slot2;
	}

	public void setCPSlot2(int point) {
		this.cp_slot2 = point;
	}

	public int getCPSlot3() {
		return cp_slot3;
	}

	public void setCPSlot3(int point) {
		this.cp_slot3 = point;
	}

	public int getCPSlot4() {
		return cp_slot4;
	}

	public void setCPSlot4(int point) {
		this.cp_slot4 = point;
	}

	public int getCPSlot5() {
		return cp_slot5;
	}

	public void setCPSlot5(int point) {
		this.cp_slot5 = point;
	}

	public int getCPSlot6() {
		return cp_slot6;
	}

	public void setCPSlot6(int point) {
		this.cp_slot6 = point;
	}

	public void clearJoinRequest() {
		playerCommonData.setJoinRequestLegionId(0);
		playerCommonData.setJoinRequestState(LegionJoinRequestState.NONE);
		DAOManager.getDAO(PlayerDAO.class).clearJoinRequest(getObjectId());
	}

	public void setLunaConsumePoint(int point) {
		this.playerCommonData.setLunaConsumePoint(point);
	}

	public int getLunaConsumePoint() {
		return this.playerCommonData.getLunaConsumePoint();
	}

	public void setMuniKeys(int keys) {
		this.playerCommonData.setMuniKeys(keys);
	}

	public int getMuniKeys() {
		return this.playerCommonData.getMuniKeys();
	}

	public void setLunaConsumeCount(int count) {
		this.playerCommonData.setLunaConsumeCount(count);
	}

	public int getLunaConsumeCount() {
		return this.playerCommonData.getLunaConsumeCount();
	}

	/** 设置月华账号。 / Sets the luna account. */
	public void setLunaAccount(long luna) {
		if (luna < 0) {
			PacketSendUtility.sendMessage(this, "Invalid Luna balance.");
			return;
		}
		if (com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_ACCOUNT_TOLL_INFO(
				this.getClientConnection().getAccount().getToll(), luna, this.getAcountName()))) {
			this.getClientConnection().getAccount().setLuna(luna);
		} else {
			PacketSendUtility.sendMessage(this, "ls communication error.");
		}
	}

	/** 获取月华账号。 / Returns the luna account. */
	public long getLunaAccount() {
		return this.getClientConnection().getAccount().getLuna();
	}

	/** 设置衣橱槽位。 / Sets the wardrobe slot. */
	public void setWardrobeSlot(int slot) {
		this.playerCommonData.setWardrobeSlot(slot);
	}

	/** 获取衣橱槽位。 / Returns the wardrobe slot. */
	public int getWardrobeSlot() {
		return this.playerCommonData.getWardrobeSlot();
	}

	/** 添加 item max count of day / Adds item max count of day */
	public void addItemMaxCountOfDay(int itemId, int thisCount) {
		itemDailyLimits.addItemMaxCount(itemId, thisCount);
	}

	/** 返回 item max this count / Returns the item max this count */
	public int getItemMaxThisCount(int itemId) {
		return itemDailyLimits.getItemMaxCount(itemId);
	}

	/** 移除 item max this count / Removes item max this count */
	public void removeItemMaxThisCount(int itemId) {
		itemDailyLimits.removeItemMaxCount(itemId);
	}

	/** 清除物品本次数上限 / Clear item max this count */
	public void clearItemMaxThisCount() {
		itemDailyLimits.clear();
	}

	/**
	 * 返回原始物品当日次数表。
	 * Returns the backing per-day item count map.
	 *
	 * @return 当日次数表（live 视图），未创建时为 null / live map, or null when not created
	 */
	public Map<Integer, MaxCountOfDay> getItemMaxThisCounts() {
		return itemDailyLimits.getItemMaxCounts();
	}

	/** 设置 bandit / Sets the bandit */
	public void setBandit(boolean bandit) {
		this.bandit = bandit;
		if (bandit) {
			if (isInGroup2()) {
				PlayerGroupService.removePlayer(this);
			}
			if (isInAlliance2()) {
				PlayerAllianceService.removePlayer(this);
			}
		}
	}

	/** 返回 kill streak / Returns the kill streak */
	public int getKillStreak() {
		return arenaKillStreak;
	}

	/** 设置 kill streak / Sets the kill streak */
	public void setKillStreak(int killStreak) {
		arenaKillStreak = killStreak;
	}

	/** 设置强盗连杀 / setbandit Kill Streak. */
	public void setbanditKillStreak(int killStreak) {
		banditKillStreak = killStreak;
	}

	/** 设置 last action / Sets the last action */
	public void setLastAction() {
		this.lastAction = System.currentTimeMillis();
	}

	/** 返回 prev pos / Returns the prev pos */
	public WorldPosition getPrevPos() {
		if (getPosition() == null || !getPosition().isSpawned()) {
			return null;
		}
		if (prevPos == null || prevPos.getMapId() != getPosition().getMapId()) {
			prevPos = new WorldPosition(getPosition().getMapId());
			prevPos.setXYZH(getPosition().getX(), getPosition().getY(), getPosition().getZ(), getPosition().getHeading());
		}
		return prevPos;
	}

	/** 设置 lawless / Sets the lawless */
	public void setLawless(boolean lawless) {
		this.lawless = lawless;
		if (lawless) {
			if (isInGroup2()) {
				PlayerGroupService.removePlayer(this);
			}
			if (isInAlliance2()) {
				PlayerAllianceService.removePlayer(this);
			}
		}
	}

	/** 发送消息。 / Send message. */
	public void sendMessage(String string) {
		PacketSendUtility.sendMessage(this, string);
	}

	/** 设置 floor / Sets the floor */
	public void setFloor(int floor) {
		getCommonData().setFloor(floor);
	}

	/** 返回 floor / Returns the floor */
	public int getFloor() {
		return getCommonData().getFloor();
	}

	/** 设置 hot couple id / Sets the hot couple id */
	public void setHOTCoupleId(int id) {
		hallOfTenacityCoupleId = id;
	}

	/** 返回 hot couple id / Returns the hot couple id */
	public int getHOTCoupleId() {
		return hallOfTenacityCoupleId;
	}

	/** 设置 hotvs id / Sets the hotvs id */
	public void setHOTVSId(int id) {
		hallOfTenacityVSId = id;
	}

	/** 返回 hotvs id / Returns the hotvs id */
	public int getHOTVSId() {
		return hallOfTenacityVSId;
	}

	/** 设置 hot my opponent obj id / Sets the hot my opponent obj id */
	public void setHOTMyOpponentObjId(int id) {
		hallOfTenacityOpponentId = id;
	}

	/** 返回 hot my opponent obj id / Returns the hot my opponent obj id */
	public int getHOTMyOpponentObjId() {
		return hallOfTenacityOpponentId;
	}

	// 竞赛活动部分 / competiton event part
	private GoldArenaRank arenaGoldrank;
	/** 获取高塔军阶。 / Returns the tower rank. */
	private TowerOfChallengeRank towerRank;
	private Arena6V6Ranking arena6v6Rank;
	/** 返回 tenacity rank / Returns the tenacity rank */
	private ArenaOfTenacityRank tenacityRank;

	/** 返回 arena gold rank / Returns the arena gold rank */
	public GoldArenaRank getArenaGoldRank() {
		return arenaGoldrank;
	}

	/** 设置 arena gold rank / Sets the arena gold rank */
	public void setArenaGoldRank(GoldArenaRank gar) {
		this.arenaGoldrank = gar;
	}

	/** 获取6v6排名 / Get 6 v 6 Rank */
	public Arena6V6Ranking get6v6Rank() {
		return arena6v6Rank;
	}

	/** 设置6v6排名 / Set 6 v 6 Rank */
	public void set6v6Rank(Arena6V6Ranking ar) {
		this.arena6v6Rank = ar;
	}

	/** 设置 queued players / Sets the queued players */
	public void setQueuedPlayers(Player player) {
		if (QueuedPlayers == null) {
			QueuedPlayers = new ArrayList<Player>(50);
		}
		QueuedPlayers.add(player);
	}

	/** 设置 luna dice game / Sets the luna dice game */
	public void setLunaDiceGame(int dice, boolean reset) {
		if (!reset) {
			if (dice > this.LunaDiceGame) {
				this.LunaDiceGame = dice;
			} else {
			}
		} else {
			this.LunaDiceGame = dice;
		}
	}

	/** 设置技能外观列表。 / Sets the skill skin list. */
	public void setSkillSkinList(SkillSkinList skillSkinList) {
		this.skillSkinList = skillSkinList;
		skillSkinList.setOwner(this);
	}

	/** 设置 is thieves / Sets the is thieves */
	public void setIsThieves(boolean isThieves) {
		this.isThieves = isThieves;
	}

	/** 获取守护灵技能点。 / Returns the minion skill points. */
	public int getMinionSkillPoints() {
		return this.getCommonData().getMinionSkillPoints();
	}

	/** 设置守护灵技能点。 / Sets the minion skill points. */
	public void setMinionSkillPoints(int minionSkillPoints) {
		this.getCommonData().setMinionSkillPoints(minionSkillPoints);
	}

	/** 是否为魔法职业 / Whether magical type class */
	public boolean isMagicalTypeClass() {
		return playerCommonData.getPlayerClass() == PlayerClass.MUSE || playerCommonData.getPlayerClass() == PlayerClass.SONGWEAVER || playerCommonData.getPlayerClass() == PlayerClass.CLERIC || playerCommonData.getPlayerClass() == PlayerClass.SORCERER || playerCommonData.getPlayerClass() == PlayerClass.SPIRIT_MASTER || playerCommonData.getPlayerClass() == PlayerClass.AETHERTECH;
	}

	/** 返回 disassembly item lists / Returns the disassembly item lists */
	private List<DisassembleItem> disassemblyItemLists = new ArrayList<DisassembleItem>();
}
