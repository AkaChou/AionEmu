package com.aionemu.gameserver.model.templates;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.quest.CollectItems;
import com.aionemu.gameserver.model.templates.quest.InventoryItems;
import com.aionemu.gameserver.model.templates.quest.QuestBonuses;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.model.templates.quest.QuestDrop;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.QuestKill;
import com.aionemu.gameserver.model.templates.quest.QuestMentorType;
import com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle;
import com.aionemu.gameserver.model.templates.quest.QuestTargetType;
import com.aionemu.gameserver.model.templates.quest.QuestWorkItems;
import com.aionemu.gameserver.model.templates.quest.Rewards;
import com.aionemu.gameserver.model.templates.quest.XMLStartCondition;

/**
 * 任务模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Quest")

public class QuestTemplate {
	@XmlElement(name = "collect_items")
	protected CollectItems collectItems;
	@XmlElement(name = "inventory_items")
	protected InventoryItems inventoryItems;
	@XmlElement(name = "rewards")
	protected List<Rewards> rewards;
	@XmlElement(name = "bonus")
	protected List<QuestBonuses> bonus;
	@XmlElement(name = "extended_rewards")
	protected List<Rewards> extendedRewards;
	@XmlElement(name = "quest_drop")
	protected List<QuestDrop> questDrop;
	@XmlElement(name = "quest_kill")
	protected List<QuestKill> questKill;
	@XmlElement(name = "start_conditions")
	protected List<XMLStartCondition> startConds;
	@XmlList
	@XmlElement(name = "class_permitted")
	protected List<PlayerClass> classPermitted;
	@XmlElement(name = "gender_permitted")
	protected Gender genderPermitted;
	@XmlElement(name = "quest_work_items")
	protected QuestWorkItems questWorkItems;
	@XmlElement(name = "fighter_selectable_reward")
	protected List<QuestItems> fighterSelectableReward;
	@XmlElement(name = "knight_selectable_reward")
	protected List<QuestItems> knightSelectableReward;
	@XmlElement(name = "ranger_selectable_reward")
	protected List<QuestItems> rangerSelectableReward;
	@XmlElement(name = "assassin_selectable_reward")
	protected List<QuestItems> assassinSelectableReward;
	@XmlElement(name = "wizard_selectable_reward")
	protected List<QuestItems> wizardSelectableReward;
	@XmlElement(name = "elementalist_selectable_reward")
	protected List<QuestItems> elementalistSelectableReward;
	@XmlElement(name = "priest_selectable_reward")
	protected List<QuestItems> priestSelectableReward;
	@XmlElement(name = "chanter_selectable_reward")
	protected List<QuestItems> chanterSelectableReward;
	@XmlElement(name = "gunslinger_selectable_reward")
	protected List<QuestItems> gunslingerSelectableReward;
	@XmlElement(name = "songweaver_selectable_reward")
	protected List<QuestItems> songweaverSelectableReward;
	@XmlElement(name = "aethertech_selectable_reward")
	protected List<QuestItems> aethertechSelectableReward;
	@XmlAttribute(name = "id", required = true)
	protected int id;
	@XmlAttribute(name = "name")
	protected String name;
	@XmlAttribute(name = "nameId")
	protected Integer nameId;
	@XmlAttribute(name = "minlevel_permitted")
	protected Integer minlevelPermitted;
	@XmlAttribute(name = "maxlevel_permitted")
	protected int maxlevelPermitted;
	@XmlAttribute(name = "max_repeat_count")
	protected Integer maxRepeatCount;
	@XmlAttribute(name = "reward_repeat_count")
	protected Integer rewardRepeatCount;
	@XmlAttribute(name = "quest_cooltime")
	protected int questCooltime;
	@XmlAttribute(name = "rank")
	private int rank;
	@XmlAttribute(name = "max_count_limited_quest")
	protected Integer maxCountLimitedQuest;
	@XmlAttribute(name = "count_recover_limited_quest")
	protected Integer countRecoverLimitedQuest;
	@XmlAttribute(name = "cannot_share")
	protected Boolean cannotShare;
	@XmlAttribute(name = "cannot_giveup")
	protected Boolean cannotGiveup;
	@XmlAttribute(name = "bounty_reward")
	protected Boolean bountyReward;
	@XmlAttribute(name = "use_class_reward")
	protected Integer useClassReward;
	@XmlList
	@XmlAttribute(name = "race_permitted")
	protected List<Race> racePermitted;
	@XmlAttribute(name = "combineskill")
	protected Integer combineskill;
	@XmlAttribute(name = "combine_skillpoint")
	protected Integer combineSkillpoint;
	@XmlAttribute(name = "timer")
	protected Boolean timer;
	@XmlAttribute(name = "category")
	protected QuestCategory category;
	@XmlAttribute(name = "repeat_cycle")
	protected List<QuestRepeatCycle> repeatCycle;
	@XmlAttribute(name = "npcfaction_id")
	protected int npcFactionId;
	@XmlAttribute(name = "mentor_type")
	protected QuestMentorType mentorType = QuestMentorType.NONE;
	@XmlAttribute(name = "target_type")
	private QuestTargetType targetType = QuestTargetType.NONE;
	@XmlAttribute(name = "titleId")
	protected int titleId;

	 /**
	  * 获取 collectItems 属性值。
	  * Gets the value of the collectItems property
	  * @return possible object is {@link CollectItems }
	  */
	public CollectItems getCollectItems() {
		return collectItems;
	}

	/** 获取背包物品。 / Returns the inventory items. */
	public InventoryItems getInventoryItems() {
		return inventoryItems;
	}

	/**
	 * 获取 rewards 属性值。 / Gets the value of the rewards property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the rewards property. <p> For example, to add a new item, do as follows: <pre> getRewards().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link Rewards }
	 */
	public List<Rewards> getRewards() {
		if (rewards == null) {
			rewards = new ArrayList<Rewards>();
		}
		return this.rewards;
	}

	/** 返回 extended rewards / Returns the extended rewards */
	public List<Rewards> getExtendedRewards() {
		if (extendedRewards == null) {
			extendedRewards = new ArrayList<Rewards>();
		}
		return this.extendedRewards;
	}

	/** 获取加成。 / Returns the bonus. */
	public List<QuestBonuses> getBonus() {
		if (bonus == null) {
			bonus = new ArrayList<QuestBonuses>();
		}
		return this.bonus;
	}

	/**
	 * 获取 questDrop 属性值。 / Gets the value of the questDrop property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the questDrop property. <p> For example, to add a new item, do as follows: <pre> getQuestDrop().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestDrop }
	 */
	public List<QuestDrop> getQuestDrop() {
		if (questDrop == null) {
			questDrop = new ArrayList<QuestDrop>();
		}
		return this.questDrop;
	}

	/** 返回 quest kill / Returns the quest kill */
	public List<QuestKill> getQuestKill() {
		if (questKill == null) {
			questKill = new ArrayList<QuestKill>();
		}
		return this.questKill;
	}

	/** 返回 xml start conditions / Returns the xml start conditions */
	public List<XMLStartCondition> getXMLStartConditions() {
		if (startConds == null) {
			startConds = new ArrayList<XMLStartCondition>();
		}
		return startConds;
	}

	/**
	 * 获取 classPermitted 属性值。 / Gets the value of the classPermitted property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the classPermitted property. <p> For example, to add a new item, do as follows: <pre> getClassPermitted().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link PlayerClass }
	 */
	public List<PlayerClass> getClassPermitted() {
		if (classPermitted == null) {
			classPermitted = new ArrayList<PlayerClass>();
		}
		return this.classPermitted;
	}

	 /**
	  * 获取 genderPermitted 属性值。
	  * Gets the value of the genderPermitted property
	  * @return possible object is {@link Gender }
	  */
	public Gender getGenderPermitted() {
		return genderPermitted;
	}

	 /**
	  * 获取 questWorkItems 属性值。
	  * Gets the value of the questWorkItems property
	  * @return possible object is {@link QuestWorkItems }
	  */
	public QuestWorkItems getQuestWorkItems() {
		return questWorkItems;
	}

	/**
	 * 获取 fighterSelectableReward 属性值。 / Gets the value of the fighterSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the fighterSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getFighterSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getFighterSelectableReward() {
		if (fighterSelectableReward == null) {
			fighterSelectableReward = new ArrayList<QuestItems>();
		}
		return this.fighterSelectableReward;
	}

	/**
	 * 获取 knightSelectableReward 属性值。 / Gets the value of the knightSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the knightSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getKnightSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getKnightSelectableReward() {
		if (knightSelectableReward == null) {
			knightSelectableReward = new ArrayList<QuestItems>();
		}
		return this.knightSelectableReward;
	}

	/**
	 * 获取 rangerSelectableReward 属性值。 / Gets the value of the rangerSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the rangerSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getRangerSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getRangerSelectableReward() {
		if (rangerSelectableReward == null) {
			rangerSelectableReward = new ArrayList<QuestItems>();
		}
		return this.rangerSelectableReward;
	}

	/**
	 * 获取 assassinSelectableReward 属性值。 / Gets the value of the assassinSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the assassinSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getAssassinSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getAssassinSelectableReward() {
		if (assassinSelectableReward == null) {
			assassinSelectableReward = new ArrayList<QuestItems>();
		}
		return this.assassinSelectableReward;
	}

	/**
	 * 获取 wizardSelectableReward 属性值。 / Gets the value of the wizardSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the wizardSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getWizardSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getWizardSelectableReward() {
		if (wizardSelectableReward == null) {
			wizardSelectableReward = new ArrayList<QuestItems>();
		}
		return this.wizardSelectableReward;
	}

	/**
	 * 获取 elementalistSelectableReward 属性值。 / Gets the value of the elementalistSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the elementalistSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getElementalistSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getElementalistSelectableReward() {
		if (elementalistSelectableReward == null) {
			elementalistSelectableReward = new ArrayList<QuestItems>();
		}
		return this.elementalistSelectableReward;
	}

	/**
	 * 获取 priestSelectableReward 属性值。 / Gets the value of the priestSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the priestSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getPriestSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getPriestSelectableReward() {
		if (priestSelectableReward == null) {
			priestSelectableReward = new ArrayList<QuestItems>();
		}
		return this.priestSelectableReward;
	}

	/**
	 * 获取 chanterSelectableReward 属性值。 / Gets the value of the chanterSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the chanterSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getChanterSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getChanterSelectableReward() {
		if (chanterSelectableReward == null) {
			chanterSelectableReward = new ArrayList<QuestItems>();
		}
		return this.chanterSelectableReward;
	}

	/**
	 * 获取 gunslingerSelectableReward 属性值。 / Gets the value of the gunslingerSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the GunslingerSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getGunslingerSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getGunslingerSelectableReward() {
		if (gunslingerSelectableReward == null) {
			gunslingerSelectableReward = new ArrayList<QuestItems>();
		}
		return this.gunslingerSelectableReward;
	}

	/**
	 * 获取 songweaverSelectableReward 属性值。 / Gets the value of the songweaverSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the songweaverSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getSongweaverSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getSongweaverSelectableReward() {
		if (songweaverSelectableReward == null) {
			songweaverSelectableReward = new ArrayList<QuestItems>();
		}
		return this.songweaverSelectableReward;
	}

	/**
	 * 获取 aethertechSelectableReward 属性值。 / Gets the value of the aethertechSelectableReward property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the aethertechSelectableReward property. <p> For example, to add a new item, do as follows: <pre> getAethertechSelectableReward().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getAethertechSelectableReward() {
		if (aethertechSelectableReward == null) {
			aethertechSelectableReward = new ArrayList<QuestItems>();
		}
		return this.aethertechSelectableReward;
	}

	 /**
	  * 获取 id 属性值。
	  * Gets the value of the id property
	  */
	public int getId() {
		return id;
	}

	/**
	 * 获取 value 的名称 property。 / Gets the value of the name property
	 *
	 * @return possible object is {@link String }
	 */
	public String getName() {
		return name;
	}

	 /**
	  * 获取 nameId 属性值。
	  * Gets the value of the nameId property
	  * @return possible object is {@link Integer }
	  */
	public Integer getNameId() {
		return nameId;
	}

	 /**
	  * 获取 minlevelPermitted 属性值。
	  * Gets the value of the minlevelPermitted property
	  * @return possible object is {@link Integer }
	  */
	public Integer getMinlevelPermitted() {
		return minlevelPermitted;
	}

	/** 返回 maxlevel permitted / Returns the maxlevel permitted */
	public int getMaxlevelPermitted() {
		return maxlevelPermitted;
	}

	/** 返回 required rank / Returns the required rank */
	public int getRequiredRank() {
		return rank;
	}

	 /**
	  * 获取 maxRepeatCount 属性值。
	  * Gets the value of the maxRepeatCount property
	  * @return possible object is {@link Integer }
	  */
	public Integer getMaxRepeatCount() {
		if (maxRepeatCount == null || !(maxRepeatCount > 1)) {
			return 1;
		}
		return maxRepeatCount;
	}

	/** 返回达到扩展奖励所需的完成次数。 / Returns the completion count required for extended rewards. */
	public int getRewardRepeatCount() {
		if (rewardRepeatCount != null) {
			return rewardRepeatCount;
		}
		return getMaxRepeatCount();
	}

	 /**
	  * 获取 maxCountLimitedQuest 属性值。
	  * Gets the value of the maxCountLimitedQuest property
	  * @return possible object is {@link Integer }
	  */
	public Integer getMaxCountLimitedQuest() {
		if (maxCountLimitedQuest == null || !(maxCountLimitedQuest > 1)) {
			return 1;
		}
		return maxCountLimitedQuest;
	}

	 /**
	  * 获取 countRecoverLimitedQuest 属性值。
	  * Gets the value of the countRecoverLimitedQuest property
	  * @return possible object is {@link Integer }
	  */
	public Integer getCountRecoverLimitedQuest() {
		if (countRecoverLimitedQuest == null || !(countRecoverLimitedQuest > 1)) {
			return 1;
		}
		return countRecoverLimitedQuest;
	}

	 /**
	  * 获取 cannotShare 属性值。
	  * Gets the value of the cannotShare property
	  * @return possible object is {@link Boolean }
	  */
	public boolean isCannotShare() {
		if (cannotShare == null) {
			return false;
		} else {
			return cannotShare;
		}
	}

	 /**
	  * 获取 cannotGiveup 属性值。
	  * Gets the value of the cannotGiveup property
	  * @return possible object is {@link Boolean }
	  */
	public boolean isCannotGiveup() {
		if (cannotGiveup == null) {
			return false;
		} else {
			return cannotGiveup;
		}
	}

	/**
	 * @return Whether bounty reward
	 */
	public boolean isBountyReward() {
		if (bountyReward == null) {
			return false;
		} else {
			return bountyReward;
		}
	}

	/** 是否使用单条职业奖励 / Whether use single class reward */
	public boolean isUseSingleClassReward() {
		if (useClassReward == null) {
			return false;
		} else {
			return useClassReward == 1;
		}
	}

	/** 是否使用重复职业奖励 / Whether use repeated class reward */
	public boolean isUseRepeatedClassReward() {
		if (useClassReward == null) {
			return false;
		} else {
			return useClassReward == 2;
		}
	}

	/**
	 * @return Whether repeatable
	 */
	public boolean isRepeatable() {
		return getMaxRepeatCount() > 1;
	}

	/**
	 * 返回允许接取任务的种族列表。 / Returns the races allowed to acquire this quest.
	 */
	public List<Race> getRacePermitted() {
		if (racePermitted == null) {
			racePermitted = new ArrayList<Race>();
		}
		return racePermitted;
	}

	/**
	 * 判断指定种族是否允许接取任务。 / Whether the given race may acquire this quest.
	 */
	public boolean isRacePermitted(Race race) {
		return racePermitted == null || racePermitted.isEmpty() || racePermitted.contains(Race.PC_ALL) || racePermitted.contains(race);
	}

	 /**
	  * 获取 combineskill 属性值。
	  * Gets the value of the combineskill property
	  * @return possible object is {@link Integer }
	  */
	public Integer getCombineSkill() {
		return combineskill;
	}

	 /**
	  * 获取 combineSkillpoint 属性值。
	  * Gets the value of the combineSkillpoint property
	  * @return possible object is {@link Integer }
	  */
	public Integer getCombineSkillPoint() {
		return combineSkillpoint;
	}

	 /**
	  * 获取 timer 属性值。
	  * Gets the value of the timer property
	  * @return possible object is {@link Integer }
	  */

	public boolean isTimer() {
		if (timer == null) {
			return false;
		} else {
			return timer;
		}
	}

	/** 获取分类。 / Returns the category. */
	public QuestCategory getCategory() {
		if (category == null) {
			category = QuestCategory.QUEST;
		}
		return category;
	}

	/**
	 * @return the mentor
	 */
	public boolean isMentor() {
		return mentorType != QuestMentorType.NONE;
	}

	/**
	 * @return the mentor
	 */
	public QuestMentorType getMentorType() {
		return mentorType;
	}

	/** 返回目标类型 / Returns the target type*/
	public QuestTargetType getTargetType() {
		return targetType;
	}

	/** 返回 repeat cycle / Returns the repeat cycle */
	public List<QuestRepeatCycle> getRepeatCycle() {
		return repeatCycle;
	}

	/** 返回标题 ID / Returns the title id */
	public int getTitleId() {
		return titleId;
	}

	/** 返回 npc faction id / Returns the npc faction id */
	public int getNpcFactionId() {
		return npcFactionId;
	}

	/**
	 * @return Whether time based
	 */
	public boolean isTimeBased() {
		return repeatCycle != null;
	}

	/** 返回 quest cool time / Returns the quest cool time */
	public int getQuestCoolTime() {
		return questCooltime;
	}

	/**
	 * @return 是否为每日类型。 / Whether daily
	  */
	public boolean isDaily() {
		return isTimeBased() && repeatCycle.size() == 1 && repeatCycle.get(0) == QuestRepeatCycle.ALL;
	}

	/**
	 * @return Whether weekly
	 */
	public boolean isWeekly() {
		return isTimeBased() && !isDaily();
	}

	/** 是否大师 / Whether master */
	public boolean isMaster() {
		return getCombineSkillPoint() != null && getCombineSkillPoint() == 499;
	}

	/** 是否专家 / Whether expert */
	public boolean isExpert() {
		return getCombineSkillPoint() != null && getCombineSkillPoint() == 399;
	}

	/** 是否无数量 / Whether no count*/
	public boolean isNoCount() {
		return category.equals(QuestCategory.NON_COUNT) || category.equals(QuestCategory.EVENT);
	}
}
