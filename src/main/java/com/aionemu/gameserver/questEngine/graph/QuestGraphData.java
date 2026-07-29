package com.aionemu.gameserver.questEngine.graph;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.questEngine.model.ConditionOperation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 定义任务图 XML 的 JAXB 输入模型；该模型仅用于反序列化，运行时使用编译后的不可变数据。
 * Defines the JAXB input model for quest graph XML; runtime code uses the compiled immutable data instead.
 */
@XmlRootElement(name = "quest_graphs")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public final class QuestGraphData {

	/**
	 * XML 中声明的任务图。
	 * Quest graphs declared in XML.
	 */
	@XmlElement(name = "quest_graph")
	private List<GraphData> graphs = new ArrayList<>();

	/**
	 * 表示一个任务所有者的原始图定义。
	 * Represents the raw graph definition for one quest owner.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(propOrder = { "variables", "nodes" })
	@Getter
	public static final class GraphData {
		/**
		 * 任务所有者标识。
		 * Quest owner identifier.
		 */
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		/**
		 * 图格式版本。
		 * Graph format version.
		 */
		@XmlAttribute(required = true)
		private Integer version;
		/**
		 * 图状态的归属范围名称。
		 * Graph state scope name.
		 */
		@XmlAttribute(required = true)
		private String scope;
		/**
		 * 初始节点标识。
		 * Initial node identifier.
		 */
		@XmlAttribute(name = "initial_node", required = true)
		private String initialNode;
		/**
		 * XML 变量包装。
		 * XML variable wrapper.
		 */
		@XmlElement
		private VariablesData variables;
		/**
		 * 图节点定义。
		 * Graph node definitions.
		 */
		@XmlElement(name = "node", required = true)
		private List<NodeData> nodes = new ArrayList<>();

		/**
		 * 返回变量定义；XML 未声明变量时返回空列表。
		 * Returns variable definitions, or an empty list when XML declares none.
		 */
		public List<VariableData> getVariables() {
			return variables == null ? List.of() : variables.values;
		}

	}

	/**
	 * 包装 XML 中的变量列表。
	 * Wraps the variable list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class VariablesData {
		@XmlElement(name = "variable", required = true)
		private List<VariableData> values = new ArrayList<>();
	}

	/**
	 * 表示尚未类型化的 XML 变量定义。
	 * Represents an XML variable definition before type compilation.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class VariableData {
		/**
		 * 变量名称。
		 * Variable name.
		 */
		@XmlAttribute(required = true)
		private String name;
		/**
		 * 变量类型名称。
		 * Variable type name.
		 */
		@XmlAttribute(required = true)
		private String type;
		/**
		 * 变量状态范围名称。
		 * Variable state scope name.
		 */
		@XmlAttribute(required = true)
		private String scope;
		/**
		 * 变量的原始初值文本。
		 * Raw initial-value text of the variable.
		 */
		@XmlAttribute(required = true)
		private String initial;
		/**
		 * 可选的整数下界。
		 * Optional integer lower bound.
		 */
		@XmlAttribute
		private Integer min;
		/**
		 * 可选的整数上界。
		 * Optional integer upper bound.
		 */
		@XmlAttribute
		private Integer max;
	}

	/**
	 * 表示 XML 中的任务图节点。
	 * Represents a quest graph node in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class NodeData {
		/**
		 * 节点标识。
		 * Node identifier.
		 */
		@XmlAttribute(required = true)
		private String id;
		/**
		 * 该节点是否为终态。
		 * Whether this node is terminal.
		 */
		@XmlAttribute
		private boolean terminal;
		/**
		 * 节点的出转换。
		 * Outgoing transitions of the node.
		 */
		@XmlElement(name = "transition")
		private List<TransitionData> transitions = new ArrayList<>();
	}

	/**
	 * 表示 XML 中带事件、条件和动作的图转换。
	 * Represents an XML graph transition with an event, conditions, and actions.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class TransitionData {
		/**
		 * 转换标识。
		 * Transition identifier.
		 */
		@XmlAttribute(required = true)
		private String id;
		/**
		 * 转换优先级。
		 * Transition priority.
		 */
		@XmlAttribute(required = true)
		private Integer priority;
		/**
		 * 目标节点标识。
		 * Target node identifier.
		 */
		@XmlAttribute(name = "to", required = true)
		private String targetNode;
		/**
		 * JAXB 事件负载。
		 * JAXB event payload.
		 */
		@XmlElements({
			@XmlElement(name = "dialog", type = DialogEventData.class, required = true),
			@XmlElement(name = "kill", type = KillEventData.class, required = true),
			@XmlElement(name = "attack", type = AttackEventData.class, required = true),
			@XmlElement(name = "player-death", type = PlayerDeathEventData.class, required = true),
			@XmlElement(name = "kill-in-world", type = KillInWorldEventData.class, required = true),
			@XmlElement(name = "item-use", type = ItemUseEventData.class, required = true),
			@XmlElement(name = "item-dialog", type = ItemDialogEventData.class, required = true),
			@XmlElement(name = "item-obtained", type = ItemObtainedEventData.class, required = true),
			@XmlElement(name = "item-equipped", type = ItemEquippedEventData.class, required = true),
			@XmlElement(name = "house-item-use", type = HouseItemUseEventData.class, required = true),
			@XmlElement(name = "world-entered", type = WorldEnteredEventData.class, required = true),
				@XmlElement(name = "zone-entered", type = ZoneEnteredEventData.class, required = true),
				@XmlElement(name = "zone-left", type = ZoneLeftEventData.class, required = true),
				@XmlElement(name = "zone-mission-ended", type = ZoneMissionEndedEventData.class, required = true),
				@XmlElement(name = "level-up", type = LevelUpEventData.class, required = true),
				@XmlElement(name = "player-logout", type = PlayerLogoutEventData.class, required = true),
				@XmlElement(name = "quest-timer-ended", type = QuestTimerEndedEventData.class, required = true),
				@XmlElement(name = "movie-ended", type = MovieEndedEventData.class, required = true),
				@XmlElement(name = "npc-proximity", type = NpcProximityEventData.class, required = true),
				@XmlElement(name = "escort-reached-target", type = EscortReachedTargetEventData.class, required = true),
				@XmlElement(name = "escort-lost-target", type = EscortLostTargetEventData.class, required = true),
				@XmlElement(name = "ranked-player-kill", type = RankedPlayerKillEventData.class, required = true),
				@XmlElement(name = "dredgion-settled", type = DredgionSettledEventData.class, required = true),
				@XmlElement(name = "craft-failed", type = CraftFailedEventData.class, required = true),
				@XmlElement(name = "npc-aggro-listed", type = NpcAggroListedEventData.class, required = true),
				@XmlElement(name = "windstream-entered", type = WindstreamEnteredEventData.class, required = true),
				@XmlElement(name = "flying-ring-passed", type = FlyingRingPassedEventData.class, required = true),
				@XmlElement(name = "skill-used", type = SkillUsedEventData.class, required = true),
				@XmlElement(name = "interaction-eligibility", type = InteractionEligibilityEventData.class, required = true)
		})
		private Object event;
		/**
		 * XML 条件包装。
		 * XML condition wrapper.
		 */
		@XmlElement
		private ConditionsData conditions;
		/**
		 * XML 动作包装。
		 * XML action wrapper.
		 */
		@XmlElement
		private ActionsData actions;

		/**
		 * 返回条件负载；XML 未声明条件时返回空列表。
		 * Returns condition payloads, or an empty list when XML declares none.
		 */
		public List<Object> getConditions() {
			return conditions == null ? List.of() : conditions.values;
		}

		/**
		 * 返回动作负载；XML 未声明动作时返回空列表。
		 * Returns action payloads, or an empty list when XML declares none.
		 */
		public List<Object> getActions() {
			return actions == null ? List.of() : actions.values;
		}
	}

	/**
	 * 表示 NPC 对话事件的 XML 参数。
	 * Represents XML parameters for an NPC dialog event.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class DialogEventData {
		/**
		 * 对话目标 NPC 标识。
		 * Dialog target NPC identifier.
		 */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
		/**
		 * 客户端对话标识。
		 * Client dialog identifier.
		 */
		@XmlAttribute(required = true)
		private String dialog;
	}

	/**
	 * 表示 NPC 击杀事件的 XML 参数。
	 * Represents XML parameters for an NPC kill event.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class KillEventData {
		/**
		 * 被击杀 NPC 标识。
		 * Killed NPC identifier.
		 */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
	}

	/** 表示 NPC 受攻击事件的 XML 路由参数。 / Represents XML routing parameters for an NPC attack event. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class AttackEventData {
		/** 受攻击 NPC 标识。 / Attacked NPC identifier. */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
	}

	/** 表示当前玩家死亡事件；无客户端参数。 / Represents current-player death without client parameters. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class PlayerDeathEventData {
	}

	/** 表示指定世界中的玩家击杀事件。 / Represents a player kill in a specified world. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class KillInWorldEventData {
		/** 世界标识；0 表示旧注册的显式 wildcard。 / World identifier; zero is the legacy explicit wildcard. */
		@XmlAttribute(name = "world_id", required = true)
		private Integer worldId;
	}

	/** 表示使用普通物品事件的 XML 路由参数。 / Represents XML routing parameters for a regular item-use event. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ItemUseEventData {
		/** 使用物品模板标识。 / Used item template identifier. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/** 表示由服务端物品对话授权产生的客户端选择。 / Represents a client choice authorized by a server-side item dialog session. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ItemDialogEventData {
		/** 发起对话的物品模板标识。 / Item template that opened the dialog. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;

		/** 允许的客户端对话动作。 / Allowed client dialog action. */
		@XmlAttribute(required = true)
		private String dialog;
	}

	/** 表示获得物品事件的 XML 路由参数。 / Represents XML routing parameters for an item-obtained event. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ItemObtainedEventData {
		/** 获得物品模板标识。 / Obtained item template identifier. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/** 表示装备物品事件的 XML 路由参数。 / Represents XML routing parameters for an item-equipped event. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ItemEquippedEventData {
		/** 装备物品模板标识。 / Equipped item template identifier. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/** 表示使用房屋物品事件的 XML 路由参数。 / Represents XML routing parameters for a house-item-use event. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class HouseItemUseEventData {
		/** 使用房屋物品模板标识。 / Used house-item template identifier. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/** 表示玩家完成地图加载并进入世界；路由不接受客户端参数。 / Represents world entry after map load without client routing parameters. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class WorldEnteredEventData {
	}

	/** 表示进入命名区域事件的 XML 路由参数。 / Represents XML routing parameters for entering a named zone. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ZoneEnteredEventData {
		/** 规范化区域名称。 / Canonical zone name. */
		@XmlAttribute(name = "zone_name", required = true)
		private String zoneName;
	}

	/** 表示离开命名区域事件的 XML 路由参数。 / Represents XML routing parameters for leaving a named zone. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class ZoneLeftEventData {
		/** 规范化区域名称。 / Canonical zone name. */
		@XmlAttribute(name = "zone_name", required = true)
		private String zoneName;
	}

	/** 表示向当前 graph owner 定向投递的区域任务结束事件。 / Represents a zone-mission-end event targeted at the current graph owner. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ZoneMissionEndedEventData {
	}

	/** 表示服务端确认的玩家升级事件；路由不接受客户端参数。 / Represents a server-confirmed player level-up without client routing parameters. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class LevelUpEventData {
	}

	/** 表示玩家会话登出事件；路由不接受客户端参数。 / Represents a player-session logout without client routing parameters. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class PlayerLogoutEventData {
	}

	/** 表示当前 graph owner 的命名任务计时器到期。 / Represents expiry of a named quest timer owned by the current graph. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestTimerEndedEventData {
		/** 与强类型 deadline 状态对应的计时器名称。 / Timer name corresponding to typed deadline state. */
		@XmlAttribute(required = true)
		private String timer;
	}

	/** 表示客户端对服务端已发影片的权威结束确认。 / Represents an authoritative completion of a server-issued movie. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class MovieEndedEventData {
		/** 影片协议标识。 / Movie protocol identifier. */
		@XmlAttribute(name = "movie_id", required = true)
		private Integer movieId;
	}

	/** 表示服务端对象交互的只读资格查询。 / Represents a read-only eligibility query for a server object interaction. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class InteractionEligibilityEventData {
		/** 被查询对象的 NPC 模板标识。 / NPC template identifier of the queried object. */
		@XmlAttribute(name = "object_id", required = true)
		private Integer objectId;
		/** 当前生产入口证明的封闭动作名称。 / Closed action name proven by the current production entry point. */
		@XmlAttribute(required = true)
		private String action;
	}

	/** 表示玩家进入指定 NPC 服务端感知半径的事件。 / Represents server-observed player entry into an NPC proximity radius. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class NpcProximityEventData {
		/** 邻近目标 NPC 模板标识。 / Proximity target NPC template identifier. */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
	}

	/** 表示当前 graph owner 的护送 NPC 到达目标。 / Represents the escort NPC reaching its target for the current graph owner. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class EscortReachedTargetEventData {
	}

	/** 表示当前 graph owner 的护送 NPC 丢失目标。 / Represents the escort NPC losing its target for the current graph owner. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class EscortLostTargetEventData {
	}

	/** 表示击杀最低欧比斯军衔玩家的事件。 / Represents a kill of a player at or above a minimum Abyss rank. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class RankedPlayerKillEventData {
		/** 允许 credit 的最低受害者军衔 ID。 / Minimum victim rank id eligible for credit. */
		@XmlAttribute(name = "minimum_rank", required = true)
		private Integer minimumRank;
	}

	/** 表示服务端完成当前 Dredgion run 的成员结算。 / Represents server completion of member settlement for the current Dredgion run. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class DredgionSettledEventData {
	}

	/** 表示服务端确认且玩家库存仍为零的制作失败事件。 / Represents a server-confirmed craft failure whose product remains absent from inventory. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class CraftFailedEventData {
		/** 制作失败时未获得的物品模板标识。 / Item-template identifier not obtained by the failed craft. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/** 表示玩家被服务端加入指定 NPC 仇恨列表后产生的感知广播。 / Represents server perception after a player is added to an NPC aggro list. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class NpcAggroListedEventData {
		/** 产生仇恨列表信号的 NPC 模板标识。 / NPC template identifier producing the aggro-list signal. */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
	}

	/** 表示进入指定世界风道的服务端确认事件。 / Represents server-confirmed entry into a windstream in a specific world. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class WindstreamEnteredEventData {
		/** 风道所在世界。 / World containing the windstream route. */
		@XmlAttribute(name = "world_id", required = true)
		private Integer worldId;
		/** 客户端协议使用的原始 teleport ID。 / Raw teleport id used by the client protocol. */
		@XmlAttribute(name = "route_id", required = true)
		private Integer routeId;
	}

	/** 表示穿过指定世界飞行环的服务端确认事件。 / Represents server-confirmed passage through a flying ring in a specific world. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class FlyingRingPassedEventData {
		/** 飞行环所在世界。 / World containing the flying ring. */
		@XmlAttribute(name = "world_id", required = true)
		private Integer worldId;
		/** 静态数据中的规范飞行环名称。 / Canonical flying-ring name from static data. */
		@XmlAttribute(name = "ring_name", required = true)
		private String ringName;
	}

	/** 表示服务端确认的技能使用信号及 owner 重复策略。 / Represents a server-confirmed skill-use signal and its owner duplicate policy. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SkillUsedEventData {
		/** 技能模板标识。 / Skill-template identifier. */
		@XmlAttribute(name = "skill_id", required = true)
		private Integer skillId;
		/** 两个服务端入口的强类型重复策略。 / Typed duplicate policy for the two server entry points. */
		@XmlAttribute(name = "duplicate_policy", required = true)
		private String duplicatePolicy;
	}

	/**
	 * 包装 XML 中的条件列表。
	 * Wraps the condition list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ConditionsData {
		@XmlElements({
			@XmlElement(name = "quest-status", type = QuestStatusConditionData.class),
			@XmlElement(name = "quest-variable", type = QuestVariableConditionData.class),
			@XmlElement(name = "packed-counter", type = PackedCounterConditionData.class),
			@XmlElement(name = "invasion-world-active", type = InvasionWorldActiveConditionData.class),
			@XmlElement(name = "quest-repeat-available", type = QuestRepeatAvailableConditionData.class),
			@XmlElement(name = "quest-collect-items", type = QuestCollectItemsConditionData.class),
			@XmlElement(name = "recipe-learnable", type = RecipeLearnableConditionData.class),
			@XmlElement(name = "craft-skill-eligible", type = CraftSkillEligibilityConditionData.class),
			@XmlElement(name = "quest-reward", type = QuestRewardConditionData.class),
			@XmlElement(name = "quest-completion-count", type = QuestCompletionCountConditionData.class),
			@XmlElement(name = "player-level", type = PlayerLevelConditionData.class),
			@XmlElement(name = "kill-victim-level-delta", type = KillVictimLevelDeltaConditionData.class),
			@XmlElement(name = "player-race", type = PlayerRaceConditionData.class),
			@XmlElement(name = "player-class", type = PlayerClassConditionData.class),
			@XmlElement(name = "player-gender", type = PlayerGenderConditionData.class),
			@XmlElement(name = "player-title", type = PlayerTitleConditionData.class),
			@XmlElement(name = "player-abyss-rank", type = PlayerAbyssRankConditionData.class),
			@XmlElement(name = "player-inventory", type = PlayerInventoryConditionData.class),
			@XmlElement(name = "player-equipped", type = PlayerEquippedConditionData.class)
		})
		private List<Object> values = new ArrayList<>();
	}

	/**
	 * 表示当前任务整数变量的数值比较。
	 * Represents a numeric comparison against an integer variable of the current quest.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestVariableConditionData {
		/** 变量名。 / Variable name. */
		@XmlAttribute(required = true)
		private String variable;
		/** 数值比较操作。 / Numeric comparison operation. */
		@XmlAttribute(name = "op", required = true)
		private ConditionOperation operation;
		/** 比较值。 / Comparison value. */
		@XmlAttribute(required = true)
		private Integer value;
	}

	/**
	 * 表示由低位到高位整数变量组成的定基数计数器比较。
	 * Represents a fixed-radix counter comparison over low-to-high integer variables.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PackedCounterConditionData {
		/** 低位到高位变量名。 / Low-to-high variable names. */
		@XmlList
		@XmlAttribute(required = true)
		private List<String> variables = new ArrayList<>();
		/** 每位的固定基数。 / Fixed radix of each digit. */
		@XmlAttribute(required = true)
		private Integer radix;
		/** 数值比较操作。 / Numeric comparison operation. */
		@XmlAttribute(name = "op", required = true)
		private ConditionOperation operation;
		/** 比较值。 / Comparison value. */
		@XmlAttribute(required = true)
		private Integer value;
	}

	/** 表示进入世界时服务端确认的入侵通道活跃条件。 / Represents server-confirmed active invasion access on world entry. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class InvasionWorldActiveConditionData {
		/** 入侵目标世界标识。 / Invasion target world identifier. */
		@XmlAttribute(name = "world_id", required = true)
		private Integer worldId;
	}

	/** 定义当前任务开始新重复周期所需的上限和 deadline。 / Defines the cap and deadline required to start a repeat cycle. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestRepeatAvailableConditionData {
		/** 最大完成次数；255 表示无限。 / Maximum completions; 255 means unlimited. */
		@XmlAttribute(name = "max_completions", required = true)
		private Integer maxCompletions;
		/** 是否要求 history 中存在并到达 next-repeat deadline。 / Whether history must contain a reached next-repeat deadline. */
		@XmlAttribute(name = "requires_deadline", required = true)
		private Boolean requiresDeadline;
		/** 期望当前是否可重复。 / Whether repeat availability is expected. */
		@XmlAttribute(name = "expected", required = true)
		private Boolean expectedAvailable;
	}

	/** 标记玩家必须持有 quest_data 声明的全部交付物品。 / Marks that all quest_data delivery items must be present. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class QuestCollectItemsConditionData {
	}

	/** 表示 recipe typed bridge 的可学习性判断。 / Represents a learnability decision from the recipe typed bridge. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class RecipeLearnableConditionData {
		/** 配方模板标识。 / Recipe-template identifier. */
		@XmlAttribute(name = "recipe_id", required = true)
		private Integer recipeId;
		/** 期望的可学习性。 / Expected learnability. */
		@XmlAttribute(required = true)
		private Boolean expected;
	}

	/** 表示制作技能奖励在对话或影片提交阶段的 typed 资格查询。 / Represents a typed craft-reward eligibility query during dialog or movie settlement. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class CraftSkillEligibilityConditionData {
		/** 制作技能标识。 / Craft-skill identifier. */
		@XmlAttribute(name = "craft_skill_id", required = true)
		private Integer craftSkillId;

		/** 专家或大师目标等级。 / Expert or master target level. */
		@XmlAttribute(name = "target_level", required = true)
		private Integer targetLevel;

		/** 对话或影片提交使用的封闭资格策略。 / Closed eligibility policy for dialog or movie settlement. */
		@XmlAttribute(required = true)
		private String policy;
	}

	/**
	 * 表示当前或指定任务状态集合条件的 XML 参数。
	 * Represents XML parameters for a current- or referenced-quest status-set condition.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestStatusConditionData {
		/** 可选目标任务 ID；缺失时使用当前 owner。 / Optional target quest id; current owner when absent. */
		@XmlAttribute(name = "quest_id")
		private Integer questId;
		/** 状态集合比较操作。 / Status-set comparison operation. */
		@XmlAttribute(name = "op", required = true)
		private ConditionOperation operation;
		/** 空格分隔的显式状态集。 / Space-separated explicit status set. */
		@XmlList
		@XmlAttribute(name = "values", required = true)
		private List<String> statuses = new ArrayList<>();
	}

	/**
	 * 表示指定任务末次奖励索引条件的 XML 参数。
	 * Represents XML parameters for a referenced quest's last reward-index condition.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestRewardConditionData {
		/** 目标任务 ID。 / Target quest id. */
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		/** 期望的末次奖励索引。 / Expected last reward index. */
		@XmlAttribute(name = "reward_index", required = true)
		private Integer rewardIndex;
	}

	/**
	 * 表示指定任务完成次数比较的 XML 参数。
	 * Represents XML parameters for a referenced quest's completion-count comparison.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestCompletionCountConditionData {
		/** 目标任务 ID。 / Target quest id. */
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		/** 数值比较操作。 / Numeric comparison operation. */
		@XmlAttribute(name = "op", required = true)
		private ConditionOperation operation;
		/** 比较的完成次数。 / Completion-count operand. */
		@XmlAttribute(required = true)
		private Integer count;
	}

	/**
	 * 表示玩家等级闭区间条件；max 缺失时没有上限。
	 * Represents an inclusive player-level range with no upper bound when max is absent.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerLevelConditionData {
		/** 最低玩家等级。 / Minimum player level. */
		@XmlAttribute(required = true)
		private Integer min;
		/** 可选最高玩家等级。 / Optional maximum player level. */
		@XmlAttribute
		private Integer max;
	}

	/**
	 * 表示当前玩家等级减去 KILL_IN_WORLD 受害玩家等级后的可选闭区间。
	 * Represents optional inclusive bounds for current-player level minus the KILL_IN_WORLD victim-player level.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class KillVictimLevelDeltaConditionData {
		/** 可选最小等级差。 / Optional minimum level delta. */
		@XmlAttribute
		private Integer min;
		/** 可选最大等级差。 / Optional maximum level delta. */
		@XmlAttribute
		private Integer max;
	}

	/**
	 * 表示允许的玩家阵营集合。
	 * Represents the allowed player-race set.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerRaceConditionData {
		/** 空格分隔的允许阵营。 / Space-separated allowed races. */
		@XmlList
		@XmlAttribute(name = "values", required = true)
		private List<String> allowed = new ArrayList<>();
	}

	/**
	 * 表示允许的玩家职业集合。
	 * Represents the allowed player-class set.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerClassConditionData {
		/** 空格分隔的允许职业。 / Space-separated allowed classes. */
		@XmlList
		@XmlAttribute(name = "values", required = true)
		private List<String> allowed = new ArrayList<>();
	}

	/**
	 * 表示要求的玩家性别。
	 * Represents the required player gender.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerGenderConditionData {
		/** 期望性别名称。 / Expected gender name. */
		@XmlAttribute(required = true)
		private String value;
	}

	/**
	 * 表示要求玩家持有指定称号。
	 * Represents a requirement that the player owns a specific title.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerTitleConditionData {
		/** 称号模板 ID。 / Title template id. */
		@XmlAttribute(name = "title_id", required = true)
		private Integer titleId;
	}

	/**
	 * 表示要求玩家达到指定最低深渊军衔。
	 * Represents a minimum Abyss-rank requirement for the player.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerAbyssRankConditionData {
		/** 最低深渊军衔 ID。 / Minimum Abyss-rank id. */
		@XmlAttribute(required = true)
		private Integer minimum;
	}

	/**
	 * 表示玩家背包物品数量的数值比较。
	 * Represents a numeric comparison against an item count in the player's inventory.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerInventoryConditionData {
		/** 物品模板 ID。 / Item template id. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
		/** 数值比较操作符。 / Numeric comparison operator. */
		@XmlAttribute(name = "op", required = true)
		private ConditionOperation operation;
		/** 比较阈值。 / Comparison threshold. */
		@XmlAttribute(required = true)
		private Long count;
	}

	/**
	 * 表示玩家当前已装备物品条件的 XML 参数。
	 * Represents XML parameters for an item currently equipped by the player.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerEquippedConditionData {
		/** 必须已装备的物品模板 ID。 / Item template id that must be equipped. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
	}

	/**
	 * 包装 XML 中的动作列表。
	 * Wraps the action list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ActionsData {
		@XmlElements({
			@XmlElement(name = "start-quest", type = StartQuestActionData.class),
			@XmlElement(name = "start-event-quest", type = StartEventQuestActionData.class),
			@XmlElement(name = "abandon-quest", type = AbandonQuestActionData.class),
			@XmlElement(name = "set-quest-status", type = SetQuestStatusActionData.class),
			@XmlElement(name = "set-quest-variable", type = SetQuestVariableActionData.class),
			@XmlElement(name = "add-quest-variable", type = AddQuestVariableActionData.class),
			@XmlElement(name = "increment-packed-counter", type = IncrementPackedCounterActionData.class),
			@XmlElement(name = "set-completion-count", type = SetCompletionCountActionData.class),
			@XmlElement(name = "add-completion-count", type = AddCompletionCountActionData.class),
			@XmlElement(name = "give-quest-item", type = GiveQuestItemActionData.class),
			@XmlElement(name = "remove-quest-item", type = RemoveQuestItemActionData.class),
			@XmlElement(name = "remove-collected-items", type = RemoveCollectedItemsActionData.class),
			@XmlElement(name = "remove-quest-work-items", type = RemoveQuestWorkItemsActionData.class),
			@XmlElement(name = "learn-recipe", type = LearnRecipeActionData.class),
			@XmlElement(name = "delete-recipe", type = DeleteRecipeActionData.class),
			@XmlElement(name = "grant-craft-skill-reward", type = GrantCraftSkillRewardActionData.class),
			@XmlElement(name = "finish-quest", type = FinishQuestActionData.class),
			@XmlElement(name = "start-quest-timer", type = StartQuestTimerActionData.class),
			@XmlElement(name = "end-quest-timer", type = EndQuestTimerActionData.class),
			@XmlElement(name = "send-dialog", type = SendDialogActionData.class),
			@XmlElement(name = "close-dialog", type = CloseDialogActionData.class),
			@XmlElement(name = "show-quest-list", type = ShowQuestListActionData.class),
			@XmlElement(name = "sync-quest-status", type = SyncQuestStatusActionData.class),
			@XmlElement(name = "send-repeat-deadline-message", type = SendRepeatDeadlineMessageActionData.class),
			@XmlElement(name = "send-player-message", type = SendPlayerMessageActionData.class),
			@XmlElement(name = "play-movie", type = PlayMovieActionData.class),
			@XmlElement(name = "sync-craft-skill-reward", type = SyncCraftSkillRewardActionData.class),
			@XmlElement(name = "notify-recipe-rejection", type = NotifyRecipeRejectionActionData.class)
		})
		private List<Object> values = new ArrayList<>();
	}

	/**
	 * 标记当前标准任务启动动作。
	 * Marks a standard start action for the current quest.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class StartQuestActionData {
	}

	/** 表示以显式状态启动活动任务 owner。 / Represents starting an event-quest owner with an explicit status. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class StartEventQuestActionData {
		/** 目标任务 owner。 / Target quest owner. */
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		/** 目标活动任务的初始状态。 / Initial status of the target event quest. */
		@XmlAttribute(required = true)
		private String status;
	}

	/** 标记放弃当前任务并执行 typed cleanup。 / Marks abandonment of the current quest with typed cleanup. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class AbandonQuestActionData {
	}

	/** 设置 canonical 任务状态。 / Sets canonical quest status. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SetQuestStatusActionData {
		/** 目标状态。 / Target status. */
		@XmlAttribute(required = true)
		private String status;
	}

	/** 设置整数任务变量。 / Sets an integer quest variable. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SetQuestVariableActionData {
		/** 变量名。 / Variable name. */
		@XmlAttribute(required = true)
		private String variable;
		/** 写入值。 / Assigned value. */
		@XmlAttribute(required = true)
		private Integer value;
	}

	/** 增加整数任务变量。 / Increments an integer quest variable. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class AddQuestVariableActionData {
		/** 变量名。 / Variable name. */
		@XmlAttribute(required = true)
		private String variable;
		/** 非零增量。 / Non-zero delta. */
		@XmlAttribute(required = true)
		private Integer delta;
	}

	/**
	 * 表示对低位到高位强类型变量执行一次有上限的定基数递增。
	 * Represents one bounded fixed-radix increment over low-to-high typed variables.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class IncrementPackedCounterActionData {
		/** 低位到高位变量名。 / Low-to-high variable names. */
		@XmlList
		@XmlAttribute(required = true)
		private List<String> variables = new ArrayList<>();
		/** 每位的固定基数。 / Fixed radix of each digit. */
		@XmlAttribute(required = true)
		private Integer radix;
		/** 允许递增到的最大总值。 / Maximum total value the action may reach. */
		@XmlAttribute(required = true)
		private Integer maximum;
	}

	/** 设置 canonical 完成次数。 / Sets the canonical completion count. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SetCompletionCountActionData {
		/** 目标完成次数。 / Target completion count. */
		@XmlAttribute(required = true)
		private Integer count;
	}

	/** 增加 canonical 完成次数。 / Adds to the canonical completion count. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class AddCompletionCountActionData {
		/** 非零完成次数增量。 / Non-zero completion-count delta. */
		@XmlAttribute(required = true)
		private Integer delta;
	}

	/**
	 * 表示把任务物品补齐到目标总数的 XML 参数。
	 * Represents XML parameters that top a quest item up to a target total.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class GiveQuestItemActionData {
		/** 任务物品模板 ID。 / Quest-item template id. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
		/** 目标总数。 / Target total count. */
		@XmlAttribute(required = true)
		private Long count;
		/** 显式补齐模式。 / Explicit top-up mode. */
		@XmlAttribute(required = true)
		private String mode;
	}

	/**
	 * 表示从背包精确或可选精确扣除任务物品的 XML 参数。
	 * Represents XML parameters that remove an exact or optional-exact quest-item count from the inventory.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class RemoveQuestItemActionData {
		/** 任务物品模板 ID。 / Quest-item template id. */
		@XmlAttribute(name = "item_id", required = true)
		private Integer itemId;
		/** 精确扣除数量。 / Exact count to remove. */
		@XmlAttribute(required = true)
		private Long count;
		/** 显式扣除模式。 / Explicit removal mode. */
		@XmlAttribute(required = true)
		private String mode;
	}

	/** 扣除 quest_data 交付物品。 / Removes quest_data delivery items. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class RemoveCollectedItemsActionData {
	}

	/** 扣除 quest_data 工单过程物品。 / Removes work-order intermediate items declared by quest_data. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class RemoveQuestWorkItemsActionData {
	}

	/** 表示学习一个引用闭合配方。 / Represents learning a reference-closed recipe. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class LearnRecipeActionData {
		/** 配方模板标识。 / Recipe-template identifier. */
		@XmlAttribute(name = "recipe_id", required = true)
		private Integer recipeId;
	}

	/** 表示删除一个引用闭合配方。 / Represents deleting a reference-closed recipe. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class DeleteRecipeActionData {
		/** 配方模板标识。 / Recipe-template identifier. */
		@XmlAttribute(name = "recipe_id", required = true)
		private Integer recipeId;
	}

	/** 表示原子收敛制作技能等级和自动学习配方的 required 动作。 / Represents the required action that converges a craft-skill level and auto-learn recipes. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class GrantCraftSkillRewardActionData {
		/** 制作技能标识。 / Craft-skill identifier. */
		@XmlAttribute(name = "craft_skill_id", required = true)
		private Integer craftSkillId;

		/** 专家或大师目标等级。 / Expert or master target level. */
		@XmlAttribute(name = "target_level", required = true)
		private Integer targetLevel;
	}

	/** 表示固定制作晋升技能列表的 post-commit 协议投影。 / Represents the fixed post-commit craft-promotion skill-list projection. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SyncCraftSkillRewardActionData {
		/** 制作技能标识。 / Craft-skill identifier. */
		@XmlAttribute(name = "craft_skill_id", required = true)
		private Integer craftSkillId;
	}

	/** 表示 recipe eligibility 拒绝协议投影。 / Represents a recipe-eligibility rejection protocol projection. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class NotifyRecipeRejectionActionData {
		/** 配方模板标识。 / Recipe-template identifier. */
		@XmlAttribute(name = "recipe_id", required = true)
		private Integer recipeId;
	}

	/** 发放奖励并完成任务周期。 / Grants rewards and completes the quest cycle. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class FinishQuestActionData {
		/** 奖励组索引。 / Reward-group index. */
		@XmlAttribute(name = "reward_index", required = true)
		private Integer rewardIndex;
		/** 可选 repeat deadline 策略。 / Optional repeat-deadline policy. */
		@XmlAttribute(name = "repeat_kind")
		private String repeatKind;
		/** 显式时间基准。 / Explicit time basis. */
		@XmlAttribute(name = "time_basis")
		private String timeBasis;
		/** daily/weekly 重置小时或 cooldown 锚点小时。 / Daily/weekly reset hour or cooldown anchor hour. */
		@XmlAttribute(name = "reset_hour")
		private Integer resetHour;
		/** weekly 策略的星期集合。 / Weekday set for a weekly policy. */
		@XmlList
		@XmlAttribute(name = "weekdays")
		private List<String> weekdays = new ArrayList<>();
		/** anchored cooldown 秒数。 / Anchored cooldown seconds. */
		@XmlAttribute(name = "cooldown_seconds")
		private Long cooldownSeconds;
	}

	/** 启动命名任务计时器。 / Starts a named quest timer. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class StartQuestTimerActionData {
		/** 稳定计时器名称。 / Stable timer name. */
		@XmlAttribute(required = true)
		private String timer;
		/** 持续秒数。 / Duration in seconds. */
		@XmlAttribute(name = "duration_seconds", required = true)
		private Long durationSeconds;
	}

	/** 停止命名任务计时器。 / Stops a named quest timer. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class EndQuestTimerActionData {
		/** 稳定计时器名称。 / Stable timer name. */
		@XmlAttribute(required = true)
		private String timer;
	}

	/** 发送任务对话页面。 / Sends a quest dialog page. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SendDialogActionData {
		/** 对话页面 ID。 / Dialog-page id. */
		@XmlAttribute(name = "dialog_id", required = true)
		private Integer dialogId;
	}

	/** 关闭当前对话窗口。 / Closes the current dialog window. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class CloseDialogActionData {
	}

	/** 刷新 NPC 任务选择列表。 / Refreshes the NPC quest list. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ShowQuestListActionData {
	}

	/** 同步任务状态和变量。 / Synchronizes quest status and variables. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class SyncQuestStatusActionData {
	}

	/** 发送 repeat deadline 系统提示。 / Sends the repeat-deadline system message. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SendRepeatDeadlineMessageActionData {
		/** repeat deadline 策略。 / Repeat-deadline policy. */
		@XmlAttribute(name = "repeat_kind", required = true)
		private String repeatKind;
		/** 显式时间基准。 / Explicit time basis. */
		@XmlAttribute(name = "time_basis", required = true)
		private String timeBasis;
		/** daily/weekly 重置小时或 cooldown 锚点小时。 / Daily/weekly reset hour or cooldown anchor hour. */
		@XmlAttribute(name = "reset_hour", required = true)
		private Integer resetHour;
		/** weekly 策略的星期集合。 / Weekday set for a weekly policy. */
		@XmlList
		@XmlAttribute(name = "weekdays")
		private List<String> weekdays = new ArrayList<>();
		/** anchored cooldown 秒数。 / Anchored cooldown seconds. */
		@XmlAttribute(name = "cooldown_seconds")
		private Long cooldownSeconds;
	}

	/** 向玩家发送类型化频道消息。 / Sends a typed-channel player message. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class SendPlayerMessageActionData {
		/** 消息正文。 / Message text. */
		@XmlAttribute(required = true)
		private String text;
		/** 客户端频道。 / Client channel. */
		@XmlAttribute(required = true)
		private String channel;
	}

	/** 播放引用闭合的任务影片。 / Plays a reference-closed quest movie. */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayMovieActionData {
		/** 影片协议标识。 / Movie protocol identifier. */
		@XmlAttribute(name = "movie_id", required = true)
		private Integer movieId;
	}
}
