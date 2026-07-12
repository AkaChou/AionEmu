package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.SystemMessageId;

/**
 * 向客户端发送系统消息（含大量静态工厂方法对应各消息码）。
 * Server packet delivering system messages to the client (with many static factories per message code).
 */
public class SM_SYSTEM_MESSAGE extends AionServerPacket {
	/**
	 * 你对 %0 造成了 %num1 点伤害。 / You inflicted %num1 damage on %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_ATTACK(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1200000, num1, value0);
	}

	/**
	 * 暴击！你对 %0 造成 %num1 点暴击伤害。 / Critical Hit! You inflicted %num1 critical damage on %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_CRITICAL(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1200001, num1, value0);
	}

	/**
	 * %0 对 %1 造成 %num2 点伤害。 / %0 inflicted %num2 damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_SUMMONED_ATTACK(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1200002, value0, num2, value1);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_SUMMONED_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1200003, value0, num2, value1);
	}

	/**
	 * %1 受到来自 %0 的 %num2 点伤害。 / %1 received %num2 damage from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_SUMMONED_ENEMY_ATTACK(String value1, int num2, String value0) {
		return new SM_SYSTEM_MESSAGE(1200004, value1, num2, value0);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_SUMMONED_ENEMY_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1200005, value0, num2, value1);
	}

	/**
	 * @param value0 %0 已被解散。 / %0 has been dismissed
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_UNSUMMONED(int value0) {
		return new SM_SYSTEM_MESSAGE(1200006, calculateNameId(value0));
	}

	/**
	 * @param value0 %0 处于待机模式。 / %0 is in Stand-by mode
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_STAY_MODE(String value0) {
		return new SM_SYSTEM_MESSAGE(1200007, value0);
	}

	/**
	 * @param value0 %0 开始攻击敌人。 / %0 starts to attack the enemy
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_ATTACK_MODE(int value0) {
		return new SM_SYSTEM_MESSAGE(1200008, calculateNameId(value0));
	}

	/**
	 * @param value0 %0 处于守卫模式。 / %0 is in Guard mode
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_GUARD_MODE(int value0) {
		return new SM_SYSTEM_MESSAGE(1200009, calculateNameId(value0));
	}

	/**
	 * @param value0 %0 处于休息模式。 / %0 is in Resting mode
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_REST_MODE(int value0) {
		return new SM_SYSTEM_MESSAGE(1200010, calculateNameId(value0));
	}

	/**
	 * @param value0 你解除了召唤 %0。 / You unsummon %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_UNSUMMON_FOLLOWER(int value0) {
		return new SM_SYSTEM_MESSAGE(1200011, calculateNameId(value0));
	}

	/**
	 * Yousummon%0Spirit.Cooldowntimebegins 当其为 unsummoned ,并 takeslonger 当 spirit 为 killed 按 enemy。 / You summon %0 Spirit. Cooldown time begins when it is unsummoned, and takes longer when the spirit is killed by an enemy
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_COOLDOWN(String value0) {
		return new SM_SYSTEM_MESSAGE(1200012, value0);
	}

	/**
	 * 你正在流血。 / You are bleeding
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BLEED_BEGIN = new SM_SYSTEM_MESSAGE(1200214);

	/**
	 * 你不再流血。 / You are no longer bleeding
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BLEED_END = new SM_SYSTEM_MESSAGE(1200215);

	/**
	 * 你无法看见。 / You cannot see
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BLIND_BEGIN = new SM_SYSTEM_MESSAGE(1200216);

	/**
	 * 你：can see again。 / You can see again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BLIND_END = new SM_SYSTEM_MESSAGE(1200217);

	/**
	 * 你被魅惑了。 / You are charmed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CHARM_BEGIN = new SM_SYSTEM_MESSAGE(1200218);

	/**
	 * 你不再被魅惑。 / You are no longer charmed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CHARM_END = new SM_SYSTEM_MESSAGE(1200219);

	/**
	 * 你陷入混乱。 / You are confused
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CONFUSE_BEGIN = new SM_SYSTEM_MESSAGE(1200220);

	/**
	 * 你不再混乱。 / You are no longer confused
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CONFUSE_END = new SM_SYSTEM_MESSAGE(1200221);

	 /**
	  * 已创建将所受伤害转化为生命值的防御墙。
	  * A defense wall has been created to convert received damage into HP
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CONVERT_HEAL_BEGIN = new SM_SYSTEM_MESSAGE(1200222);

	 /**
	  * 将所受伤害转化为生命值的防御墙已失效。
	  * The defense wall that converts received damage into HP has expired
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CONVERT_HEAL_END = new SM_SYSTEM_MESSAGE(1200223);

	 /**
	  * 已创建吸收伤害的防御墙。
	  * A defense wall that absorbs damage has been created
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SHIELD_MAGIC_BEGIN = new SM_SYSTEM_MESSAGE(1200224);

	 /**
	  * 吸收伤害的防御墙已失效。
	  * The defense wall that absorbs damage has expired
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SHIELD_MAGIC_END = new SM_SYSTEM_MESSAGE(1200225);

	/**
	 * 你被诅咒了。 / You are cursed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CURSE_BEGIN = new SM_SYSTEM_MESSAGE(1200226);

	/**
	 * 你不再诅咒。 / You are no longer cursed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_CURSE_END = new SM_SYSTEM_MESSAGE(1200227);

	/**
	 * 你患病了。 / You are diseased
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_DISEASE_BEGIN = new SM_SYSTEM_MESSAGE(1200228);

	/**
	 * 你从疾病中恢复了。 / You recovered from the disease
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_DISEASE_END = new SM_SYSTEM_MESSAGE(1200229);

	/**
	 * 你陷入恐惧。 / You are struck by fear
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_FEAR_BEGIN = new SM_SYSTEM_MESSAGE(1200230);

	/**
	 * 你从恐惧中恢复。 / You recovered from your fear
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_FEAR_END = new SM_SYSTEM_MESSAGE(1200231);

	/**
	 * 你处于隐身状态。 / You are invisible
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_INVISIBLE_BEGIN = new SM_SYSTEM_MESSAGE(1200232);

	/**
	 * 你不再隐身。 / You are no longer invisible
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_INVISIBLE_END = new SM_SYSTEM_MESSAGE(1200233);

	/**
	 * 你被麻痹了。 / You are paralyzed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PARALYZE_BEGIN = new SM_SYSTEM_MESSAGE(1200234);

	/**
	 * 你不再麻痹。 / You are no longer paralyzed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PARALYZE_END = new SM_SYSTEM_MESSAGE(1200235);

	/**
	 * 你被石化了。 / You are petrified
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PETRIFICATION_BEGIN = new SM_SYSTEM_MESSAGE(1200236);

	/**
	 * 你不再石化。 / You are no longer petrified
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PETRIFICATION_END = new SM_SYSTEM_MESSAGE(1200237);

	/**
	 * 你处于中毒状态。 / You are poisoned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_POISON_BEGIN = new SM_SYSTEM_MESSAGE(1200238);

	/**
	 * 你不再中毒。 / You are no longer poisoned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_POISON_END = new SM_SYSTEM_MESSAGE(1200239);

	/**
	 * 你处于定身状态。 / You are immobilized
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_ROOT_BEGIN = new SM_SYSTEM_MESSAGE(1200240);

	/**
	 * 你不再定身。 / You are no longer immobilized
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_ROOT_END = new SM_SYSTEM_MESSAGE(1200241);

	/**
	 * 你：fell asleep。 / You fell asleep
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SLEEP_BEGIN = new SM_SYSTEM_MESSAGE(1200242);

	/**
	 * 你醒来了。 / You woke up
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SLEEP_END = new SM_SYSTEM_MESSAGE(1200243);

	/**
	 * 你：have been stunned。 / You have been stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STUN_BEGIN = new SM_SYSTEM_MESSAGE(1200244);

	/**
	 * 你不再眩晕。 / You are no longer stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STUN_END = new SM_SYSTEM_MESSAGE(1200245);

	/**
	 * 你处于沉默状态。 / You are silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SILENCE_BEGIN = new SM_SYSTEM_MESSAGE(1200246);

	/**
	 * 你不再沉默。 / You are no longer silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SILENCE_END = new SM_SYSTEM_MESSAGE(1200247);

	/**
	 * 你处于空中束缚状态。 / You are snared in mid-air
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_OPEN_AERIAL_BEGIN = new SM_SYSTEM_MESSAGE(1200248);

	/**
	 * 你从空中束缚中解脱。 / You are released from the Aerial Snare
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_OPEN_AERIAL_END = new SM_SYSTEM_MESSAGE(1200249);

	 /**
	  * 你的移动速度已降低。
	  * 你的移动速度 已降低。 / Your movement speed has decreased
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SNARE_BEGIN = new SM_SYSTEM_MESSAGE(1200250);

	/**
	 * 你：have normal movement speed。 / You have normal movement speed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SNARE_END = new SM_SYSTEM_MESSAGE(1200251);

	/**
	 * 你的攻击速度降低。 / Your attack speed is decreased
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SLOW_BEGIN = new SM_SYSTEM_MESSAGE(1200252);

	/**
	 * 你：have normal attack speed。 / You have normal attack speed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SLOW_END = new SM_SYSTEM_MESSAGE(1200253);

	/**
	 * 你处于因冲击而旋转状态。 / You are spinning from shock
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SPIN_BEGIN = new SM_SYSTEM_MESSAGE(1200254);

	/**
	 * 你不再感电。 / You are no longer in shock
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_SPIN_END = new SM_SYSTEM_MESSAGE(1200255);

	/**
	 * 你：fell down from shock。 / You fell down from shock
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STUMBLE_BEGIN = new SM_SYSTEM_MESSAGE(1200256);

	/**
	 * 你不再感电。 / You are no longer in shock
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STUMBLE_END = new SM_SYSTEM_MESSAGE(1200257);

	/**
	 * 你处于眩晕状态。 / You are stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STAGGER_BEGIN = new SM_SYSTEM_MESSAGE(1200258);

	/**
	 * 你不再眩晕。 / You are no longer stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_STAGGER_END = new SM_SYSTEM_MESSAGE(1200259);

	/**
	 * 你处于束缚状态。 / You are bound
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BIND_BEGIN = new SM_SYSTEM_MESSAGE(1200260);

	/**
	 * 你不再束缚。 / You are no longer bound
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_BIND_END = new SM_SYSTEM_MESSAGE(1200261);

	/**
	 * 你处于被拉拽状态。 / You are being pulled
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PULLED_BEGIN = new SM_SYSTEM_MESSAGE(1200262);

	/**
	 * 你不再被拉拽。 / You are no longer being pulled
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_EFFECT_PULLED_END = new SM_SYSTEM_MESSAGE(1200263);

	/**
	 * @param skillname 你：became blinded after using [%SkillName]。 / You became blinded after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200277, skillname);
	}

	/**
	 * @param skillname 你：became confused after using [%SkillName]。 / You became confused after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200278, skillname);
	}

	/**
	 * @param skillname 你：became diseased after using [%SkillName]。 / You became diseased after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200279, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 后陷入恐惧。 / You are struck with fear after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200280, skillname);
	}

	/**
	 * @param skillname 你：became paralyzed after using [%SkillName]。 / You became paralyzed after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200281, skillname);
	}

	/**
	 * @param skillname 你：became immobilized after using [%SkillName]。 / You became immobilized after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200282, skillname);
	}

	/**
	 * @param skillname 你：became silenced after using [%SkillName]。 / You became silenced after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200283, skillname);
	}

	/**
	 * @param skillname 你：fell asleep after using [%SkillName]。 / You fell asleep after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200284, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 后陷入旋转。 / You are spinning after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200285, skillname);
	}

	/**
	 * @param skillname 你：were knocked back from a shock after using [%SkillName]。 / You were knocked back from a shock after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200286, skillname);
	}

	/**
	 * @param skillname 你：fell down from shock after using [%SkillName]。 / You fell down from shock after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200287, skillname);
	}

	/**
	 * @param skillname 你：became stunned after using [%SkillName]。 / You became stunned after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200288, skillname);
	}

	/**
	 * 你：can see again。 / You can see again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200289);

	/**
	 * 你不再混乱。 / You are no longer confused
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200290);

	/**
	 * 你不再患病。 / You are no longer diseased
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200291);

	/**
	 * 你从恐惧中恢复。 / You recovered from your fear
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200292);

	/**
	 * 你不再麻痹。 / You are no longer paralyzed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200293);

	/**
	 * 你不再定身。 / You are no longer immobilized
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200294);

	/**
	 * 你不再沉默。 / You are no longer silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200295);

	/**
	 * 你醒来了。 / You woke up
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200296);

	/**
	 * 你停止了旋转。 / You have stopped spinning
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200297);

	/**
	 * 你不再踉跄。 / You are no longer staggering
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200298);

	/**
	 * 你不再感电。 / You are no longer shocked
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200299);

	/**
	 * 你不再眩晕。 / You are no longer stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200300);

	 /**
	  * 因使用 [%SkillName]，你的掉落率已提高。。
	  * 你的掉落率 has increased because you used [%SkillName]。 / Your loot rate has increased because you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostDropRate_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200301, skillname);
	}

	 /**
	  * 因使用 [%SkillName]，你的视野已缩小。。
	  * 你的视野 has reduced because you used [%SkillName]。 / Your visual range has reduced because you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OutofSight_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200302, skillname);
	}

	/**
	 * @param skillname 你：exchanged your enmity with the spirit's by using [%SkillName]。 / You exchanged your enmity with the spirit's by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHostile_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200303, skillname);
	}

	/**
	 * @param skillname 你：used [%SkillName]。 / You used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ReturnHome_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200304, skillname);
	}

	/**
	 * @param skillname 你：began using [%SkillName]。 / You began using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200305, skillname);
	}

	/**
	 * @param skillname 你：stopped using [%SkillName]。 / You stopped using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_END_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200306, skillname);
	}

	/**
	 * @param skillname 你：used [%SkillName] and became bound。 / You used [%SkillName] and became bound
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200307, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 后正在流血。 / You are bleeding after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200308, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 后被诅咒。 / You are cursed after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200309, skillname);
	}

	/**
	 * @param skillname 你因使用 [%SkillName] 而无法飞行。 / You are unable to fly because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200310, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 后被空中束缚。 / You are snared in mid-air after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200311, skillname);
	}

	/**
	 * @param skillname 你：became petrified after using [%SkillName]。 / You became petrified after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200312, skillname);
	}

	/**
	 * @param skillname 你：became poisoned after using [%SkillName]。 / You became poisoned after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200313, skillname);
	}

	 /**
	  * 使用 [%SkillName] 后，你的攻击速度已降低。。
	  * 你的攻击速度 has decreased after you used [%SkillName]。 / Your attack speed has decreased after you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200314, skillname);
	}

	 /**
	  * 使用 [%SkillName] 后，你的移动速度已降低。。
	  * 你的移动速度 has decreased after you used [%SkillName]。 / Your movement speed has decreased after you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200315, skillname);
	}

	/**
	 * 你不再束缚 / You are no longer bound
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200316);

	/**
	 * 你不再流血。 / You are no longer bleeding
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200317);

	/**
	 * 你从诅咒状态中解脱。 / You are released from the cursed state
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200318);

	/**
	 * 你处于可再次飞行状态。 / You are able to fly again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200319);

	/**
	 * 你从空中束缚中解脱。 / You are released from the Aerial Snare
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200320);

	/**
	 * 你不再石化。 / You are no longer petrified
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200321);

	/**
	 * 你不再中毒。 / You are no longer poisoned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200322);

	/**
	 * 你的攻击速度恢复正常。 / Your attack speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200323);

	/**
	 * 你的移动速度恢复正常。 / Your movement speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_ME_TO_SELF = new SM_SYSTEM_MESSAGE(1200324);

	/**
	 * @param skillname 你：boosted your block by using [%SkillName]。 / You boosted your block by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysBlock_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200325, skillname);
	}

	/**
	 * @param skillname 你：boosted your evasion by using [%SkillName]。 / You boosted your evasion by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysDodge_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200326, skillname);
	}

	/**
	 * @param skillname 你：boosted your accuracy by using [%SkillName]。 / You boosted your accuracy by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysHit_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200327, skillname);
	}

	/**
	 * @param skillname 你：removed your elemental defense by using [%SkillName]。 / You removed your elemental defense by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysNoResist_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200328, skillname);
	}

	/**
	 * @param skillname 你：boosted your parry by using [%SkillName]。 / You boosted your parry by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysParry_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200329, skillname);
	}

	/**
	 * @param skillname 你：boosted your elemental defense by using [%SkillName]。 / You boosted your elemental defense by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysResist_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200330, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 强化了恢复。 / You boosted your recovery by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHealEffect_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200331, skillname);
	}

	/**
	 * @param skillname 你：changed your casting speed by using [%SkillName]。 / You changed your casting speed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCastingTime_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200332, skillname);
	}

	/**
	 * @param skillname 你：changed your MP consumption by using [%SkillName]。 / You changed your MP consumption by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCost_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200333, skillname);
	}

	/**
	 * @param skillname 你：reduced your MP consumption for mantra skills by using [%SkillName]。 / You reduced your MP consumption for mantra skills by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillToggleCost_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200334, skillname);
	}

	/**
	 * @param skillname 你：boosted your spell skill by using [%SkillName]。 / You boosted your spell skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSpellAttackEffect_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200335, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对自己造成 %num0 点伤害。 / You inflicted %num0 damage on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BackDashATK_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200336, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] %0d 仇恨。 / You %0d your enmity by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHate_ME_TO_SELF(String value0d, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200337, value0d, skillname);
	}

	/**
	 * 你：inflicted %num0 damage and the rune carve effect on yourself by using [%SkillName]。 / You inflicted %num0 damage and the rune carve effect on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CarveSignet_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200338, num0, skillname);
	}

	/**
	 * @param skillname 你：received the HP recovery effect by using [%SkillName]。 / You received the HP recovery effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200339, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200340, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] %0d 仇恨。 / You %0d your enmity by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ChangeHateOnAttacked_ME_TO_SELF(String value0d, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200341, value0d, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 解除了空中束缚。 / You are released from the Aerial Snare by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CloseAerial_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200342, skillname);
	}

	/**
	 * @param skillname 你：recovered from the transformation by using [%SkillName]。 / You recovered from the transformation by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200343, skillname);
	}

	/**
	 * 你：recovered HP by %num0 by using [%SkillName]。 / You recovered HP by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200344, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对自己造成 %num0 点伤害。 / You inflicted %num0 damage on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DashATK_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200345, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对自己造成 %num0 点伤害。 / You inflicted %num0 damage on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeathBlow_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200346, num0, skillname);
	}

	 /**
	  * 使用 [%SkillName] 后，你的恢复量已改变。。
	  * 你的恢复量 changed after using [%SkillName]。 / Your recovery amount changed after using [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeboostHealAmount_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200347, skillname);
	}

	/**
	 * 你：transformed yourself into a(n) %0 by using [%SkillName]。 / You transformed yourself into a(n) %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Deform_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200348, value0, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 减少了自身飞行时间。 / You decreased your own flight time by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200349, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使飞行时间减少 %num0。 / You reduced your flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_ME_TO_SELF(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200350, num0, skillname);
	}

	/**
	 * @param skillname 你：will inflict damage on yourself in a moment because you used [%SkillName]。 / You will inflict damage on yourself in a moment because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200351, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使飞行时间减少 %num0。 / You reduced your flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_ME_TO_SELF(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200352, num0, skillname);
	}

	/**
	 * @param skillname 你：dispelled the magic effect by using [%SkillName]。 / You dispelled the magic effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Dispel_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200353, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 驱散了魔法增益。 / You dispelled magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuff_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200354, skillname);
	}

	/**
	 * 你：suffered %num0 damage and dispelled some of the magical buffs by using [%SkillName]。 / You suffered %num0 damage and dispelled some of the magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuffCounterATK_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200355, num0, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 驱散了魔法减益。 / You dispelled magical debuffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuff_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200356, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 移除了异常精神状态。 / You removed abnormal mental conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffMental_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200357, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 移除了异常身体状态。 / You removed abnormal physical conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffPhysical_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200358, skillname);
	}

	/**
	 * 你：transferred %num0 DP by using [%SkillName]。 / You transferred %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPTransfer_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200359, num0, skillname);
	}

	/**
	 * @param skillname 你：expanded the range of mantra by using [%SkillName]。 / You expanded the range of mantra by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ExtendAuraRange_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200360, skillname);
	}

	/**
	 * @param skillname 你：were forced to crash by using [%SkillName]。 / You were forced to crash by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fall_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200361, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 减少了自身飞行时间。 / You decreased your own flight time by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200362, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使飞行时间减少 %num0。 / You reduced your flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200363, num0, skillname);
	}

	 /**
	  * 因使用 [%SkillName]，你的飞行时间增加了 %num0。。
	  * Your flight time has increased by %num0 because you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200364, num0, skillname);
	}

	 /**
	  * 你的飞行时间已由 [%SkillName] 恢复。。
	  * 你的飞行时间 has been restored by [%SkillName]。 / Your flight time has been restored by [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200365, skillname);
	}

	/**
	 * 你：increased your flight time by %num0 by using [%SkillName]。 / You increased your flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200366, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使飞行时间增加 %num0。 / You increased the flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200367, num0, skillname);
	}

	/**
	 * @param skillname 你因 [%SkillName] 持续恢复 HP。 / You are continuously recovering HP because of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200368, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200369, num0, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200370, num0, skillname);
	}

	/**
	 * @param skillname 你：converted damage to healing by using [%SkillName]。 / You converted damage to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200371, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_ME_TO_SELF(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200372, num0, skillname);
	}

	/**
	 * @param skillname 你：converted death to healing by using [%SkillName]。 / You converted death to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200373, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_ME_TO_SELF(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200374, num0, skillname);
	}

	/**
	 * @param skillname 你：hid yourself by using [%SkillName]。 / You hid yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Hide_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200375, skillname);
	}

	/**
	 * 你使用 [%SkillName] %0d 仇恨。 / You %0d your enmity by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HostileUp_ME_TO_SELF(String value0d, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200376, value0d, skillname);
	}

	/**
	 * @param skillname 你：made a magical counterattack by using [%SkillName]。 / You made a magical counterattack by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200377, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对自己造成 %num0 点伤害。 / You inflicted %num0 damage on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200378, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MoveBehindATK_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200379, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP after using [%SkillName]。 / You recovered %num0 MP after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200380, num0, skillname);
	}

	/**
	 * @param skillname 你：recovered MP by using [%SkillName]。 / You recovered MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200381, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200382, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP after using [%SkillName]。 / You recovered %num0 MP after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200383, num0, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 强化了恢复。 / You boosted your recovery by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostHealEffect_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200384, skillname);
	}

	/**
	 * @param skillname 你：boosted your skill by using [%SkillName]。 / You boosted your skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillAttack_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200385, skillname);
	}

	/**
	 * @param skillname 你：boosted your critical hit skill by using [%SkillName]。 / You boosted your critical hit skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillCritical_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200386, skillname);
	}

	/**
	 * @param skillname 你：boosted your skill by using [%SkillName]。 / You boosted your skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeTypeBoostSkillLevel_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200387, skillname);
	}

	/**
	 * @param skillname 你：made the spirit use its skills by using [%SkillName]。 / You made the spirit use its skills by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUseUltraSkill_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200388, skillname);
	}

	/**
	 * 你使用 [%SkillName] 变身为 %0。 / You have transformed into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Polymorph_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200389, value0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200390, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_Ratio_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200391, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使飞行时间增加 %num0。 / You increased the flight time by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCFPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200392, num0, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200393, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP after using [%SkillName]。 / You recovered %num0 MP after using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCMPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200394, num0, skillname);
	}

	/**
	 * @param skillname 你：protected yourself by using [%SkillName]。 / You protected yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200395, skillname);
	}

	/**
	 * 你使用 [%SkillName] 抵挡了 %num0 点伤害。 / You protected yourself from %num0 damage by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200396, num0, skillname);
	}

	/**
	 * @param skillname 你：received the effect by using [%SkillName]。 / You received the effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Provoker_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200397, skillname);
	}

	/**
	 * 你：inflicted %num0 damage and the pull effect on yourself by using [%SkillName]。 / You inflicted %num0 damage and the pull effect on yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Pulled_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200398, num0, skillname);
	}

	/**
	 * @param skillname 你：teleported yourself by using [%SkillName]。 / You teleported yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_RandomMoveLoc_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200399, skillname);
	}

	/**
	 * @param skillname 你：ensured resurrection by using [%SkillName]。 / You ensured resurrection by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Rebirth_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200400, skillname);
	}

	/**
	 * @param skillname 你：gave yourself the reflection effect by using [%SkillName]。 / You gave yourself the reflection effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200401, skillname);
	}

	/**
	 * 你：reflected %num0 damage by the effect of [%SkillName]。 / You reflected %num0 damage by the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200402, num0, skillname);
	}

	/**
	 * @param skillname 你：resurrected yourself by using [%SkillName]。 / You resurrected yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Resurrect_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200403, skillname);
	}

	/**
	 * @param skillname 你：resurrected and telerported yourself by using [%SkillName]。 / You resurrected and telerported yourself by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectPositional_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200404, skillname);
	}

	/**
	 * @param skillname 你：received the see-through effect by using [%SkillName]。 / You received the see-through effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Search_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200405, skillname);
	}

	/**
	 * 你使用 [%SkillName] 变身为 %0。 / You have transformed into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ShapeChange_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200406, value0, skillname);
	}

	/**
	 * @param skillname 你：gave yourself a defense shield by using [%SkillName]。 / You gave yourself a defense shield by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200407, skillname);
	}

	/**
	 * 你使用 [%SkillName] 抵挡了 %num0 点伤害。 / You protected yourself from %num0 damage by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200408, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SignetBurst_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200409, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATK_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200410, num0, skillname);
	}

	/**
	 * 你：absorb %num0 HP by using [%SkillName]。 / You absorb %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200411, num0, skillname);
	}

	/**
	 * @param skillname 你：received continuous damage due to [%SkillName]。 / You received continuous damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200412, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200413, num0, skillname);
	}

	/**
	 * 你：absorb %num0 HP by using [%SkillName]。 / You absorb %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200414, num0, skillname);
	}

	/**
	 * 你：absorb %num0 HP by using [%SkillName]。 / You absorb %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200415, num0, skillname);
	}

	 /**
	  * 使用 [%SkillName] 后，你的移动速度已提高。
	  * 你的移动速度 因使用 [%SkillName] 已提高。 / Your movement speed has been increased by using [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sprint_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200416, skillname);
	}

	 /**
	  * 你的%0 has been weakened by using [%SkillName]。
	  * 你的%0 has been weakened by using [%SkillName]。 / Your %0 has been weakened by using [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatDown_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200417, value0, skillname);
	}

	 /**
	  * 你的%0 has been boosted by using [%SkillName]。
	  * 你的%0 has been boosted by using [%SkillName]。 / Your %0 has been boosted by using [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatUp_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200418, value0, skillname);
	}

	 /**
	  * 你的%0 resistance effects are weakened after using [%SkillName]。
	  * 你的%0 resistance effects are weakened after using [%SkillName]。 / Your %0 resistance effects are weakened after using [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeBoostResist_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200419, value0, skillname);
	}

	/**
	 * 你：changed the duration of %0 skills by using [%SkillName]。 / You changed the duration of %0 skills by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeExtendDuration_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200420, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Summon_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200421, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonBindingGroupGate_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200422, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonGroupGate_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200423, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonHoming_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200424, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonServant_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200425, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTotem_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200426, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤了 %0。 / You summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTrap_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200427, value0, skillname);
	}

	/**
	 * @param skillname 你：exchanged your MP with your HP by using [%SkillName]。 / You exchanged your MP with your HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHPMP_Instant_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200428, skillname);
	}

	/**
	 * 使用 [%SkillName] 改变了 %0。 / %0 was changed using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_WeaponStatUp_ME_TO_SELF(String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200429, value0, skillname);
	}

	/**
	 * 你：blinded [%SkillTarget] by using [%SkillName]。 / You blinded [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200430, skilltarget, skillname);
	}

	/**
	 * 你：confused [%SkillTarget] by using [%SkillName]。 / You confused [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200431, skilltarget, skillname);
	}

	/**
	 * 你：diseased [%SkillTarget] by using [%SkillName]。 / You diseased [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200432, skilltarget, skillname);
	}

	/**
	 * @return 你：made [%SkillTarget] afraid by using [%SkillName]。 / You made [%SkillTarget] afraid by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200433, skilltarget, skillname);
	}

	/**
	 * 你：paralyzed [%SkillTarget] by using [%SkillName]。 / You paralyzed [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200434, skilltarget, skillname);
	}

	/**
	 * 你：immobilized [%SkillTarget] by using [%SkillName]。 / You immobilized [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200435, skilltarget, skillname);
	}

	/**
	 * 你：silenced [%SkillTarget] by using [%SkillName]。 / You silenced [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200436, skilltarget, skillname);
	}

	/**
	 * 你：put [%SkillTarget] to sleep by using [%SkillName]。 / You put [%SkillTarget] to sleep by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200437, skilltarget, skillname);
	}

	/**
	 * 你：span [%SkillTarget] around by using [%SkillName]。 / You span [%SkillTarget] around by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200438, skilltarget, skillname);
	}

	/**
	 * 你：knocked [%SkillTarget] back by using [%SkillName]。 / You knocked [%SkillTarget] back by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200439, skilltarget, skillname);
	}

	/**
	 * 你：knocked [%SkillTarget] over by using [%SkillName]。 / You knocked [%SkillTarget] over by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200440, skilltarget, skillname);
	}

	/**
	 * 你：stunned [%SkillTarget] by using [%SkillName]。 / You stunned [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200441, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] 不再失明。
	  * [%SkillTarget] 不再失明。 / [%SkillTarget] is no longer blind
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200442, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再混乱。
	  * [%SkillTarget] 不再混乱。 / [%SkillTarget] is no longer confused
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200443, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再疾病。
	  * [%SkillTarget] 不再疾病。 / [%SkillTarget] is no longer diseased
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200444, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再恐惧。
	  * [%SkillTarget] 不再恐惧。 / [%SkillTarget] is no longer afraid
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200445, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再麻痹。
	  * [%SkillTarget] 不再麻痹。 / [%SkillTarget] is no longer paralyzed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200446, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再定身。
	  * [%SkillTarget] 不再定身。 / [%SkillTarget] is no longer immobilized
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200447, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再沉默。
	  * [%SkillTarget] 不再沉默。 / [%SkillTarget] is no longer silenced
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200448, skilltarget);
	}

	 /**
	  * [%SkillTarget] 已醒来。
	  * [%SkillTarget] 已醒来。 / [%SkillTarget] woke up
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200449, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再旋转。
	  * [%SkillTarget] 不再旋转。 / [%SkillTarget] is no longer spinning
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200450, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再踉跄。
	  * [%SkillTarget] 不再踉跄。 / [%SkillTarget] is no longer staggering
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200451, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再冲击。
	  * [%SkillTarget] 不再冲击。 / [%SkillTarget] is no longer shocked
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200452, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再眩晕。
	  * [%SkillTarget] 不再眩晕。 / [%SkillTarget] is no longer stunned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200453, skilltarget);
	}

	/**
	 * 你使用 [%SkillName] 复活了 [%SkillTarget]。 / [%SkillTarget] was resurrected as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostDropRate_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200454, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 视野范围降低。 / [%SkillTarget]'s visual range has decreased because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OutofSight_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200455, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你：exchanged [%SkillTarget]'s enmity with the spirit's by using [%SkillName]。 / You exchanged [%SkillTarget]'s enmity with the spirit's by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHostile_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200456, skilltarget, skillname);
	}

	/**
	 * @param skillname 你：used [%SkillName]。 / You used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ReturnHome_ME_TO_B(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200457, skillname);
	}

	/**
	 * @param skillname 你：start using [%SkillName]。 / You start using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_ME_TO_B(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200458, skillname);
	}

	/**
	 * @param skillname 你：stop using [%SkillName]。 / You stop using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_END_ME_TO_B(String skillname) {
		return new SM_SYSTEM_MESSAGE(1200459, skillname);
	}

	/**
	 * 你：used [%SkillName] and [%SkillTarget] became bound。 / You used [%SkillName] and [%SkillTarget] became bound
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_ME_TO_B(String skillname, String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200460, skillname, skilltarget);
	}

	/**
	 * 你：caused [%SkillTarget] to bleed by using [%SkillName]。 / You caused [%SkillTarget] to bleed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200461, skilltarget, skillname);
	}

	/**
	 * 你：cursed [%SkillTarget] by using [%SkillName]。 / You cursed [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200462, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] 因你使用 [%SkillName] 而无法飞行。
	  * [%SkillTarget] 因你使用 [%SkillName] 而无法飞行。 / [%SkillTarget] is unable to fly because you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200463, skilltarget, skillname);
	}

	/**
	 * @return 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 被空中束缚。 / [%SkillTarget] became snared in mid-air because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200464, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你：petrified [%SkillTarget] by using [%SkillName]。 / You petrified [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200465, skilltarget, skillname);
	}

	/**
	 * 你：poisoned [%SkillTarget] by using [%SkillName]。 / You poisoned [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200466, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 攻击速度降低。 / [%SkillTarget]'s attack speed has decreased because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200467, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 移动速度降低。 / [%SkillTarget]'s movement speed decreased as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200468, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] 不再束缚。
	  * [%SkillTarget] 不再束缚。 / [%SkillTarget] is no longer bound
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200469, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再流血。
	  * [%SkillTarget] 不再流血。 / [%SkillTarget] is no longer bleeding
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200470, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 从诅咒状态中恢复。 / [%SkillTarget] recovered from the cursed state
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200471, skilltarget);
	}

	 /**
	  * [%SkillTarget] able to fly again。
	  * [%SkillTarget] is able to fly again
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200472, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 从空中束缚中解脱。 / [%SkillTarget] is released from the Aerial Snare
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200473, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再石化。
	  * [%SkillTarget] 不再石化。 / [%SkillTarget] is no longer petrified
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200474, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再中毒。
	  * [%SkillTarget] 不再中毒。 / [%SkillTarget] is no longer poisoned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200475, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 的攻击速度恢复正常。 / [%SkillTarget]'s attack speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200476, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 的移动速度恢复正常。 / [%SkillTarget]'s movement speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_ME_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200477, skilltarget);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s block by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysBlock_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200478, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s evasion by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysDodge_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200479, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s parry by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysHit_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200480, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了 [%SkillTarget] 的元素防御。 / [%Skillcaster] removed [%SkillTarget]'s elemental defense by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysNoResist_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200481, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s parry by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysParry_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200482, skillcaster, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入元素最大防御状态。 / [%SkillTarget] is in the elemental maximum defense state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysResist_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200483, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 强化了 [%SkillTarget] 的恢复技能。 / [%SkillCaster] has boosted [%SkillTarget]'s recovery skill by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHealEffect_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200484, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 施法时间增加。 / [%SkillTarget]'s casting time increased as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCastingTime_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200485, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 移动速度降低。 / [%SkillTarget]'s movement speed decreased as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCost_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200486, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 移动速度降低。 / [%SkillTarget]'s movement speed decreased as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillToggleCost_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200487, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s spell skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSpellAttackEffect_ME_TO_B(String skillcaster,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200488, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BackDashATK_ME_TO_B(int num0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200489, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 的仇恨 %0d %0。 / [%SkillCaster] %0d [%SkillTarget]'s enmity %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHate_ME_TO_B(String skillcaster, String value0d,
			String skilltarget, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200490, skillcaster, value0d, skilltarget, value0, skillname);
	}

	/**
	 * 你：inflicted %num0 damage and the rune carve effect on [%SkillTarget] by using [%SkillName]。 / You inflicted %num0 damage and the rune carve effect on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CarveSignet_ME_TO_B(int num0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200491, num0, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] recovering HP as you used [%SkillName]。
	  * [%SkillTarget] is recovering HP as you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200492, skilltarget, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200493, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 的仇恨 %0d %0。 / [%SkillCaster] %0d [%SkillTarget]'s enmity %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ChangeHateOnAttacked_ME_TO_B(String skillcaster, String value0d,
			String skilltarget, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200494, skillcaster, value0d, skilltarget, value0, skillname);
	}

	/**
	 * 你：released [%SkillTarget] from the Aerial Snare by using [%SkillName]。 / You released [%SkillTarget] from the Aerial Snare by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CloseAerial_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200495, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 获得变身恢复效果。 / [%SkillTarget] received the transformation recovery effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200496, skilltarget, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200497, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DashATK_ME_TO_B(int num0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200498, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeathBlow_ME_TO_B(int num0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200499, num0, skilltarget, skillname);
	}

	/**
	 * 你：changed [%SkillTarget]'s recovery amount by using [%SkillName]。 / You changed [%SkillTarget]'s recovery amount by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeboostHealAmount_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200500, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 将 [%SkillTarget] 变身为 %0。 / You transformed [%SkillTarget] into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Deform_ME_TO_B(String skilltarget, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200501, skilltarget, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 后，[%SkillTarget] 的飞行时间即将减少。 / In a moment, [%SkillTarget]'s flight time will decrease because you used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200502, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间减少 %num0。 / [%SkillTarget]'s flight time decreased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200503, skilltarget, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 获得延迟爆发效果。 / [%SkillTarget] received the Delayed Blast effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200504, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间减少 %num0。 / [%SkillTarget]'s flight time decreased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_ME_TO_B(String skilltarget,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200505, skilltarget, num0, skillname);
	}

	/**
	 * 你：dispelled the magic effect from [%SkillTarget] by using [%SkillName]。 / You dispelled the magic effect from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Dispel_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200506, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法增益。 / You dispelled magical buffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuff_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200507, skilltarget, skillname);
	}

	/**
	 * 你：inflicted %num0 damage on [%SkillTarget] and dispelled some of its magical buffs by using [%SkillName]。 / You inflicted %num0 damage on [%SkillTarget] and dispelled some of its magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuffCounterATK_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200508, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法减益。 / You dispelled magical debuffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuff_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200509, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 移除了 [%SkillTarget] 的异常精神状态。 / You removed abnormal mental conditions from [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffMental_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200510, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 移除了 [%SkillTarget] 的异常身体状态。 / You removed abnormal physical conditions from [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffPhysical_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200511, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] in the DP recovery state because you used [%SkillName]。
	  * [%SkillTarget] is in the DP recovery state because you used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPTransfer_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200512, skilltarget, skillname);
	}

	/**
	 * 你：expanded [%SkillTarget]'s mantra range by using [%SkillName]。 / You expanded [%SkillTarget]'s mantra range by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ExtendAuraRange_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200513, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 获得强制坠落效果。 / [%SkillTarget] received the forced crash effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fall_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200514, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 后，[%SkillTarget] 的飞行时间即将减少。 / In a moment, [%SkillTarget]'s flight time will decrease because you used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200515, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间减少 %num0。 / [%SkillTarget]'s flight time decreased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200516, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillTarget]'s flight time has decreased by %num0 because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_Instant_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200517, skilltarget, num0, skillname);
	}

	/**
	 * 你：restored [%SkillTarget]'s flight time by using [%SkillName]。 / You restored [%SkillTarget]'s flight time by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200518, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间增加 %num0。 / [%SkillTarget]'s flight time increased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200519, skilltarget, num0, skillname);
	}

	/**
	 * 因你使用 [%SkillName]，[%SkillTarget] 飞行时间增加 %num0。 / [%SkillTarget]'s flight time has increased by %num0 because you used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200520, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillCaster] has caused [%SkillTarget] to recover HP over time by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200521, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] recovered its HP by %num0 due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200522, skilltarget, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_Instant_HEAL_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200523, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] converted [%SkillTarget]'s damage to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200524, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] has recovered %num0 HP due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200525, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillTarget] 使用 [%SkillName] 将死亡转化为治疗。 / [%SkillTarget] converted death to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200526, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] has recovered %num0 HP due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200527, skilltarget, num0, skillname);
	}

	/**
	 * @return 你：hid [%SkillTarget] by using [%SkillName]。 / You hid [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Hide_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200528, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 的仇恨 %0d %0。 / [%SkillCaster] %0d [%SkillTarget]'s enmity %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HostileUp_ME_TO_B(String skillcaster, String value0d,
			String skilltarget, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200529, skillcaster, value0d, skilltarget, value0, skillname);
	}

	/**
	 * @return 你：granted [%SkillTarget] a magical counterattack by using [%SkillName]。 / You granted [%SkillTarget] a magical counterattack by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200530, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200531, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MoveBehindATK_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200532, num0, skilltarget, skillname);
	}

	/**
	 * 你：reduced [%SkillTarget]'s MP by %num0 by using [%SkillName]。 / You reduced [%SkillTarget]'s MP by %num0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_Instant_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200533, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200534, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200535, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200536, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 强化了 [%SkillTarget] 的恢复技能。 / [%SkillCaster] has boosted [%SkillTarget]'s recovery skill by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostHealEffect_ME_TO_B(String skillcaster,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200537, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] received the boost skill effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillAttack_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200538, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 获得暴击效果。 / [%SkillTarget] received the critical hit effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillCritical_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200539, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] received the boost skill effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeTypeBoostSkillLevel_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200540, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 后，精灵对 [%SkillTarget] 使用技能。 / Your spirit uses its skills on [%SkillTarget] as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUseUltraSkill_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200541, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 将 [%SkillTarget] 变身为 %0。 / You transformed [%SkillTarget] into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Polymorph_ME_TO_B(String skilltarget, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200542, skilltarget, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200543, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_Ratio_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200544, num0, skilltarget, skillname);
	}

	/**
	 * 因你使用 [%SkillName]，[%SkillTarget] 飞行时间增加 %num0。 / [%SkillTarget]'s flight time has increased by %num0 because you used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCFPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200545, skilltarget, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCHeal_Instant_HEAL_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200546, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] recovered MP by %num0 due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCMPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200547, skilltarget, num0, skillname);
	}

	/**
	 * 你：protected [%SkillTarget] by using [%SkillName]。 / You protected [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200548, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200549, skilltarget, num0, skillname);
	}

	/**
	 * 你：affected [%SkillTarget] by using [%SkillName]。 / You affected [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Provoker_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200550, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 受到 %num0 点伤害并被拉拽。 / [%SkillTarget] received %num0 damage and the pull effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Pulled_ME_TO_B(String skilltarget, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200551, skilltarget, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 传送。 / [%SkillTarget] teleported as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_RandomMoveLoc_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200552, skilltarget, skillname);
	}

	/**
	 * 你：placed [%SkillTarget] in the reserved resurrection state as by using [%SkillName]。 / You placed [%SkillTarget] in the reserved resurrection state as by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Rebirth_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200553, skilltarget, skillname);
	}

	/**
	 * 你：gave [%SkillTarget] the reflection effect by using [%SkillName]。 / You gave [%SkillTarget] the reflection effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200554, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] reflected %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200555, skilltarget, num0, skillname);
	}

	/**
	 * [%SkillTarget] has resurrected as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Resurrect_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200556, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入复活状态。 / [%SkillTarget] is in the resurrection state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectPositional_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200557, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入透视状态。 / [%SkillTarget] is in the see-through state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Search_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200558, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你使用 [%SkillName] 将 [%SkillTarget] 变身为 %0。 / You transformed [%SkillTarget] into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ShapeChange_ME_TO_B(String skilltarget, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200559, skilltarget, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 使 [%SkillTarget] 获得防御护盾效果。 / [%SkillTarget] received the defense shield effect as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200560, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200561, skilltarget, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SignetBurst_ME_TO_B(int num0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200562, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATK_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200563, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200564, num0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成持续伤害。 / You inflicted continuous damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200565, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200566, num0, skilltarget, skillname);
	}

	/**
	 * 你：start to absorb [%SkillTarget]'s HP by using [%SkillName]。 / You start to absorb [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200567, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200568, num0, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入移动速度提升状态。 / [%SkillTarget] is in the movement speed increase state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sprint_ME_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200569, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has weakened [%SkillTarget]'s %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatDown_ME_TO_B(String skillcaster, String skilltarget,
			String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200570, skillcaster, skilltarget, value0, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s Physical Def by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatUp_ME_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200571, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 削弱了 [%SkillTarget] 的 %0 抗性效果。 / [%SkillTarget]'s %0 resistance effects were weakened as you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeBoostResist_ME_TO_B(String skilltarget, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200572, skilltarget, value0, skillname);
	}

	/**
	 * 你：changed [%SkillTarget]'s %0 skill duration by using [%SkillName]。 / You changed [%SkillTarget]'s %0 skill duration by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeExtendDuration_ME_TO_B(String skilltarget, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200573, skilltarget, value0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 向 [%SkillTarget] 召唤了 %0。 / You summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Summon_ME_TO_B(String value0, String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200574, value0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 向 [%SkillTarget] 召唤了 %0。 / You summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonBindingGroupGate_ME_TO_B(String value0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200575, value0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 向 [%SkillTarget] 召唤了 %0。 / You summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonGroupGate_ME_TO_B(String value0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200576, value0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 召唤 %0 攻击 [%SkillTarget]。 / You summoned %0 by using [%SkillName] to let it attack [%SkillTarget]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonHoming_ME_TO_B(String value0, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200577, value0, skillname, skilltarget);
	}

	/**
	 * 你使用 [%SkillName] 召唤 %0 攻击 [%SkillTarget]。 / You summoned %0 by using [%SkillName] to let it attack [%SkillTarget]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonServant_ME_TO_B(String value0, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200578, value0, skillname, skilltarget);
	}

	/**
	 * 你使用 [%SkillName] 向 [%SkillTarget] 召唤了 %0。 / You summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTotem_ME_TO_B(String value0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200579, value0, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 向 [%SkillTarget] 召唤了 %0。 / You summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTrap_ME_TO_B(String value0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200580, value0, skilltarget, skillname);
	}

	/**
	 * 你：caused [%SkillTarget] to exchange MP with HP by using [%SkillName]。 / You caused [%SkillTarget] to exchange MP with HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHPMP_Instant_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200581, skilltarget, skillname);
	}

	/**
	 * 你：changed [%SkillTarget]'s %0 by using [%SkillName]。 / You changed [%SkillTarget]'s %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_WeaponStatUp_ME_TO_B(String skilltarget, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200582, skilltarget, value0, skillname);
	}

	/**
	 * [%SkillCaster] has blinded you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200583, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has confused you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200584, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has diseased you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200585, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has made you afraid by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200586, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has diseased you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200587, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has immobilized you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200588, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has silenced you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200589, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 有放入 you 到 sleep 按 using[%SkillName]。 / [%SkillCaster] has put you to sleep by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200590, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has spun you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200591, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has knocked you back by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200592, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has knocked you down by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200593, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has stunned you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200594, skillcaster, skillname);
	}

	/**
	 * 你：can see again。 / You can see again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200595);

	/**
	 * 你不再混乱。 / You are no longer confused
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200596);

	/**
	 * 你不再患病。 / You are no longer diseased
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200597);

	/**
	 * 你从恐惧中恢复。 / You recovered from your fear
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200598);

	/**
	 * 你不再麻痹。 / You are no longer paralyzed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200599);

	/**
	 * 你不再定身。 / You are no longer immobilized
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200600);

	/**
	 * 你不再沉默。 / You are no longer silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200601);

	/**
	 * 你醒来了。 / You woke up
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200602);

	/**
	 * 你停止了旋转。 / You have stopped spinning
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200603);

	/**
	 * 你不再踉跄。 / You are no longer staggering
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200604);

	/**
	 * 你不再感电。 / You are no longer shocked
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200605);

	/**
	 * 你不再眩晕。 / You are no longer stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200606);

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的掉落率已提高。
	  * 你的掉落率 因 [%SkillCaster] 使用 [%SkillName] 已提高。 / Your loot rate has increased because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostDropRate_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200607, skillcaster, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的视野已缩小。
	  * 你的视野 因 [%SkillCaster] 使用 [%SkillName] 已降低。 / Your visual range has decreased because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OutofSight_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200608, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] caused you to exchange your enmity with the spirit's by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHostile_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200609, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 对你使用了 [%SkillName]。 / [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ReturnHome_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200610, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 开始对你使用 [%SkillName]。 / [%SkillCaster] started using [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200611, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 停止对你使用 [%SkillName]。 / [%SkillCaster] stops using [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_END_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200612, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，You became bound。 / You became bound because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200613, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] caused you to bleed by using [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200614, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has cursed you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200615, skillcaster, skillname);
	}

	/**
	 * 你因 [%SkillCaster] 对你使用 [%SkillName] 而无法飞行。 / You are unable to fly because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200616, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has snared you in mid-air by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200617, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has petrified you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200618, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has poisoned you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200619, skillcaster, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的攻击速度已降低。
	  * 你的攻击速度 因 [%SkillCaster] 使用 [%SkillName] 已降低。 / Your attack speed has decreased because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200620, skillcaster, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的移动速度已降低。
	  * 你的移动速度 因 [%SkillCaster] 使用 [%SkillName] 已降低。 / Your movement speed has decreased because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200621, skillcaster, skillname);
	}

	/**
	 * 你不再束缚 / You are no longer bound
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200622);

	/**
	 * 你不再流血。 / You are no longer bleeding
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200623);

	/**
	 * 你从诅咒状态中解脱。 / You are released from the cursed state
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200624);

	/**
	 * 你处于可再次飞行状态。 / You are able to fly again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200625);

	/**
	 * 你从空中束缚中解脱。 / You are released from the Aerial Snare
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200626);

	/**
	 * 你不再石化。 / You are no longer petrified
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200627);

	/**
	 * 你不再中毒。 / You are no longer poisoned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200628);

	/**
	 * 你的攻击速度恢复正常。 / Your attack speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200629);

	/**
	 * 你的移动速度恢复正常。 / Your movement speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_A_TO_ME = new SM_SYSTEM_MESSAGE(1200630);

	/**
	 * [%SkillCaster] has boosted your block by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysBlock_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200631, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your evasion by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysDodge_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200632, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，You received the boost accuracy effect。 / You received the boost accuracy effect because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysHit_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200633, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has removed your elemental defense by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysNoResist_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200634, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your parry by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysParry_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200635, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has maximized your elemental defense by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysResist_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200636, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your recovery skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHealEffect_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200637, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has changed your casting speed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCastingTime_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200638, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has changed your MP consumption by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCost_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200639, skillcaster, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的真言技能 MP 消耗已改变。
	  * Your mantra skill MP consumption has changed because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillToggleCost_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200640, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your spell skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSpellAttackEffect_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200641, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BackDashATK_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200642, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对你施加仇恨 %0。 / [%SkillCaster] inflicted enmity %0 on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHate_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200643, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] inflicted %num0 damage and the rune carve effect on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CarveSignet_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200644, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_A_TO_ME(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200645, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200646, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对你施加仇恨 %0。 / [%SkillCaster] inflicted enmity %0 on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ChangeHateOnAttacked_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200647, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] released you from the aerial snare by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CloseAerial_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200648, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] restored you from the transformation by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200649, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200650, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DashATK_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200651, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeathBlow_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200652, skillcaster, num0, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的恢复量已改变。
	  * 你的恢复量 因 [%SkillCaster] 使用 [%SkillName] 已改变。 / Your recovery amount has changed because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeboostHealAmount_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200653, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] transformed you into a(n) %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Deform_A_TO_ME(String skillcaster, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200654, skillcaster, value0, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间即将减少。 / In a moment, [%SkillCaster] will decrease your flight time because they used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200655, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间增加 %num0。 / Your flight time has increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200656, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted a Delayed Blast on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200657, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 飞行时间减少 %num0。 / [%SkillCaster]'s flight time has decreased by %num0 because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_A_TO_ME(String skillcaster,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200658, skillcaster, num0, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，Your magic effect was dispelled。 / Your magic effect was dispelled because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Dispel_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200659, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的魔法增益被驱散。 / Your magical buffs were dispelled because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuff_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200660, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，You suffered %num0 damage and lost some of your magical buffs。 / You suffered %num0 damage and lost some of your magical buffs because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuffCounterATK_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200661, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的魔法减益被驱散。 / Your magical debuffs were dispelled because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuff_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200662, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的异常精神状态被移除。 / Your abnormal mental conditions were removed because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffMental_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200663, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的异常身体状态被移除。 / Your abnormal physical conditions were removed because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffPhysical_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200664, skillcaster, skillname);
	}

	/**
	 * 你：received %num0 DP because [%SkillCaster] used [%SkillName]。 / You received %num0 DP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPTransfer_A_TO_ME(int num0, String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200665, num0, skillcaster, skillname);
	}

	 /**
	  * 因 [%SkillCaster] 使用 [%SkillName]，你的光环范围已扩大。
	  * 你的光环范围 因 [%SkillCaster] 使用 [%SkillName] 已扩大。 / Your aura range has expanded because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ExtendAuraRange_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200666, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] forced you to crash by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fall_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200667, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间即将减少。 / In a moment, [%SkillCaster] will decrease your flight time because they used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200668, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用的 [%SkillName] 效果，你的飞行时间减少 %num0。 / Your flight time decreased by %num0 due to the effect of [%SkillName] used by [%SkillCaster]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_A_TO_ME(int num0, String skillname,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1200669, num0, skillname, skillcaster);
	}

	/**
	 * 因 [%SkillCaster] 使用的 [%SkillName] 效果，你的飞行时间减少 %num0。 / Your flight time decreased by %num0 due to the effect of [%SkillName] used by [%SkillCaster]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_Instant_A_TO_ME(int num0, String skillname,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1200670, num0, skillname, skillcaster);
	}

	/**
	 * [%SkillCaster] restored your flight time by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200671, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间增加 %num0。 / Your flight time has increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200672, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间增加 %num0。 / Your flight time has increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200673, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is continuously restoring your HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200674, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200675, num0, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200676, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has converted damage dealt to you to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200677, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200678, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has converted death dealt to you to healing by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200679, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200680, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has hidden you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Hide_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200681, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对你施加仇恨 %0。 / [%SkillCaster] inflicted enmity %0 on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HostileUp_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200682, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has given you a magical counterattack by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200683, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200684, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MoveBehindATK_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200685, skillcaster, num0, skillname);
	}

	 /**
	  * 你的MP has decreased by %num0 because [%SkillCaster] used [%SkillName]。
	  * 你的MP has decreased by %num0 because [%SkillCaster] used [%SkillName]。 / Your MP has decreased by %num0 because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_Instant_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200686, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has restored your MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200687, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 MP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 MP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200688, num0, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200689, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your recovery skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostHealEffect_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200690, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你获得技能强化效果。 / You received the boost skill effect because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillAttack_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200691, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your parry by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillCritical_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200692, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你获得技能强化效果。 / You received the boost skill effect because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeTypeBoostSkillLevel_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200693, skillcaster, skillname);
	}

	 /**
	  * spirit uses its skills because [%SkillCaster] used [%SkillName]。
	  * The spirit uses its skills because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUseUltraSkill_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200694, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has transformed you into a(n) %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Polymorph_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200695, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200696, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_Ratio_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200697, skillcaster, num0, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，你的飞行时间增加 %num0。 / Your flight time has increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCFPHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200698, num0, skillcaster, skillname);
	}

	/**
	 * 你：recovered %num0 HP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 HP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200699, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCMPHeal_Instant_HEAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200700, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has protected you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200701, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因 [%SkillName] 效果格挡了 %num0 点伤害。 / [%SkillCaster] blocked %num0 damage through the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200702, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 的 [%SkillName] 影响了你。 / [%SkillCaster]'s [%SkillName] affected you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Provoker_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200703, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage and pulled you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Pulled_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200704, skillcaster, num0, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，You teleported。 / You teleported because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_RandomMoveLoc_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200705, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，You entered the reserved resurrection state。 / You entered the reserved resurrection state because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Rebirth_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200706, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has cast a reflector on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200707, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因 [%SkillName] 效果反弹了 %num0 点伤害。 / [%SkillCaster] reflected %num0 damage through to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200708, skillcaster, num0, skillname);
	}

	/**
	 * 你：resurrected as [%SkillCaster] used [%SkillName]。 / You resurrected as [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Resurrect_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200709, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has decreased your movement speed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectPositional_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200710, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 有放入 see - througheffect 在 you 按 using[%SkillName]。 / [%SkillCaster] has put a see-through effect on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Search_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200711, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has transformed you into a(n) %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ShapeChange_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200712, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 有放入 defenseshield 在 you 按 using[%SkillName]。 / [%SkillCaster] has put a defense shield on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200713, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因 [%SkillName] 效果格挡了 %num0 点伤害。 / [%SkillCaster] blocked %num0 damage through the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200714, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SignetBurst_A_TO_ME(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200715, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATK_Instant_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200716, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200717, skillcaster, num0, skillname);
	}

	/**
	 * 你：received continuous damage because [%SkillCaster] used [%SkillName]。 / You received continuous damage because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200718, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_Instant_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200719, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has begun draining your HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200720, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has inflicted %num0 damage on you by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_A_TO_ME(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200721, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] has increased your movement speed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sprint_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200722, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has weakened [%SkillTarget]'s %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatDown_A_TO_ME(String skillcaster, String skilltarget,
			String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200723, skillcaster, skilltarget, value0, skillname);
	}

	/**
	 * [%SkillCaster] has boosted your %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatUp_A_TO_ME(String skillcaster, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200724, skillcaster, value0, skillname);
	}

	 /**
	  * 你的%0 resistance effect was weakened because [%SkillCaster] used [%SkillName]。
	  * 你的%0 resistance effect was weakened because [%SkillCaster] used [%SkillName]。 / Your %0 resistance effect was weakened because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeBoostResist_A_TO_ME(String value0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200725, value0, skillcaster, skillname);
	}

	 /**
	  * 你的%0 skill duration has changed because [%SkillCaster] used [%SkillName]。
	  * 你的%0 skill duration has changed because [%SkillCaster] used [%SkillName]。 / Your %0 skill duration has changed because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeExtendDuration_A_TO_ME(String value0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200726, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Summon_A_TO_ME(String skillcaster, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200727, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonBindingGroupGate_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200728, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonGroupGate_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200729, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonHoming_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200730, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonServant_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200731, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTotem_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200732, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has caused you to summon %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTrap_A_TO_ME(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200733, skillcaster, value0, skillname);
	}

	/**
	 * 你：exchanged HP with MP because [%SkillCaster] used [%SkillName]。 / You exchanged HP with MP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHPMP_Instant_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200734, skillcaster, skillname);
	}

	 /**
	  * 你的%0 has changed because [%SkillCaster] used [%SkillName]。
	  * 你的%0 has changed because [%SkillCaster] used [%SkillName]。 / Your %0 has changed because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_WeaponStatUp_A_TO_ME(String value0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200735, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而目盲。 / [%SkillCaster] became blinded because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200736, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而混乱。 / [%SkillCaster] became confused because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200737, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而患病。 / [%SkillCaster] became diseased because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200738, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入恐惧状态。 / [%SkillCaster] was put in the fear state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200739, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而麻痹。 / [%SkillCaster] became paralyzed because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200740, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而定身。 / [%SkillCaster] became immobilized because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200741, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而沉默。 / [%SkillCaster] became silenced because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200742, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而入睡。 / [%SkillCaster] fell asleep because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200743, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而旋转。 / [%SkillCaster] is spinning because it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200744, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 因感电被击退。 / [%SkillTarget] was knocked back from shock because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200745, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 而感电倒地。 / [%SkillCaster] fell down from shock because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200746, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] became stunned because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200747, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] 不再失明。
	  * [%SkillTarget] 不再失明。 / [%SkillTarget] is no longer blind
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200748, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再混乱。
	  * [%SkillTarget] 不再混乱。 / [%SkillTarget] is no longer confused
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200749, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再疾病。
	  * [%SkillTarget] 不再疾病。 / [%SkillTarget] is no longer diseased
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200750, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再恐惧。
	  * [%SkillTarget] 不再恐惧。 / [%SkillTarget] is no longer afraid
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200751, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再麻痹。
	  * [%SkillTarget] 不再麻痹。 / [%SkillTarget] is no longer paralyzed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200752, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再定身。
	  * [%SkillTarget] 不再定身。 / [%SkillTarget] is no longer immobilized
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200753, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再沉默。
	  * [%SkillTarget] 不再沉默。 / [%SkillTarget] is no longer silenced
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200754, skilltarget);
	}

	 /**
	  * [%SkillTarget] 已醒来。
	  * [%SkillTarget] 已醒来。 / [%SkillTarget] woke up
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200755, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再旋转。
	  * [%SkillTarget] 不再旋转。 / [%SkillTarget] is no longer spinning
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200756, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再踉跄。
	  * [%SkillTarget] 不再踉跄。 / [%SkillTarget] is no longer staggering
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200757, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再冲击。
	  * [%SkillTarget] 不再冲击。 / [%SkillTarget] is no longer shocked
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200758, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再眩晕。
	  * [%SkillTarget] 不再眩晕。 / [%SkillTarget] is no longer stunned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200759, skilltarget);
	}

	/**
	 * [%SkillCaster]'s loot rate has increased because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostDropRate_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200760, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s visual range has reduced because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OutofSight_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200761, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] exchanged its enmity with the spirit's by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHostile_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200762, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 对你使用了 [%SkillName]。 / [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ReturnHome_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200763, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] started using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200764, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 停止对你使用 [%SkillName]。 / [%SkillCaster] stops using [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_END_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200765, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 并进入束缚状态。 / [%SkillCaster] used [%SkillName] and became bound
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200766, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is bleeding because it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200767, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is cursed because it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200768, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is unable to fly because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200769, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] became snared in mid-air because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200770, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] became petrified because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200771, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] became poisoned because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200772, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s attack speed has decreased because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200773, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s movement speed has decreased because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200774, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] 不再束缚。
	  * [%SkillTarget] 不再束缚。 / [%SkillTarget] is no longer bound
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200775, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再流血。
	  * [%SkillTarget] 不再流血。 / [%SkillTarget] is no longer bleeding
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200776, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 解除诅咒状态。 / [%SkillTarget] is released from the cursed state
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200777, skilltarget);
	}

	 /**
	  * [%SkillTarget] able to fly again。
	  * [%SkillTarget] is able to fly again
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200778, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 从空中束缚中解脱。 / [%SkillTarget] is released from the Aerial Snare
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200779, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再石化。
	  * [%SkillTarget] 不再石化。 / [%SkillTarget] is no longer petrified
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200780, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再中毒。
	  * [%SkillTarget] 不再中毒。 / [%SkillTarget] is no longer poisoned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200781, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 攻击速度已恢复。 / [%SkillTarget] restored its attack speed
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200782, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 移动速度已恢复。 / [%SkillTarget] restored its movement speed
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_A_TO_SELF(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200783, skilltarget);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入格挡强化状态。 / [%SkillTarget] is in the boost block state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysBlock_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200784, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入闪避提升状态。 / [%SkillCaster] is in the boost evasion state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysDodge_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200785, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入命中提升状态。 / [%SkillCaster] is in the boost accuracy state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysHit_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200786, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入无元素防御状态。 / [%SkillTarget] is in the no elemental defense state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysNoResist_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200787, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入招架提升状态。 / [%SkillCaster] is in the boost parry state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysParry_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200788, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入元素最大防御状态。 / [%SkillTarget] is in the elemental maximum defense state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysResist_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200789, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入恢复技能强化状态。 / [%SkillCaster] is in the boost recovery skill state because he used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHealEffect_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200790, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s casting speed has changed because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCastingTime_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200791, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s MP consumption has changed because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCost_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200792, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s mantra skill MP consumption has decreased because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillToggleCost_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200793, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入恢复技能强化状态。 / [%SkillCaster] is in the boost recovery skill state because he used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSpellAttackEffect_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200794, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BackDashATK_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200795, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入仇恨 %0 状态。 / [%SkillCaster] is in the enmity %0 state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHate_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200796, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] inflicted %num0 damage and the rune carve effect on themselves by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CarveSignet_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200797, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] recovered HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200798, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200799, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入仇恨 %0 状态。 / [%SkillCaster] is in the enmity %0 state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ChangeHateOnAttacked_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200800, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] was released from the aerial snare by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CloseAerial_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200801, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered from the transformation by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200802, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200803, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DashATK_A_TO_SELF(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200804, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeathBlow_A_TO_SELF(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200805, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] changed his own recovery amount by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeboostHealAmount_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200806, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has transformed into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Deform_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200807, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName]，即将受到飞行时间减少效果。 / [%SkillCaster] will receive the decrease flight time effect in a moment because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200808, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间减少 %num0。 / [%SkillCaster]'s flight time decreased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200809, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] will receive damage in a moment because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200810, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间减少 %num0。 / [%SkillCaster]'s flight time decreased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_A_TO_SELF(String skillcaster,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200811, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] dispelled its magic effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Dispel_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200812, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了自身魔法增益。 / [%SkillCaster] dispelled its magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuff_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200813, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] suffered %num0 damage and dispelled some of its magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuffCounterATK_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200814, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了自身魔法减益。 / [%SkillCaster] dispelled its magical debuffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuff_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200815, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了自身异常精神状态。 / [%SkillCaster] removed its abnormal mental conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffMental_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200816, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了自身异常身体状态。 / [%SkillCaster] removed its abnormal physical conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffPhysical_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200817, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] transferred %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPTransfer_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200818, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] boosted his mantra range by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ExtendAuraRange_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200819, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入强制坠落状态。 / [%SkillCaster] is in the forced crash state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fall_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200820, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName]，即将受到飞行时间减少效果。 / [%SkillCaster] will receive the decrease flight time effect in a moment because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200821, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间减少 %num0。 / [%SkillCaster]'s flight time decreased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200822, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间减少 %num0。 / [%SkillCaster]'s flight time decreased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200823, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] is in the flight time recovery state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200824, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间增加 %num0。 / [%SkillCaster]'s flight time increased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200825, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间增加 %num0。 / [%SkillCaster]'s flight time increased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200826, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] is in the continuous healing state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200827, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200828, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200829, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] is in the convert damage healing state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200830, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200831, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] is in the convert death healing state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200832, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_A_TO_SELF(String skillcaster,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200833, skillcaster, num0, skillname);
	}

	/**
	 * @return [%SkillCaster] 因使用 [%SkillName] 进入隐身状态。 / [%SkillCaster] is in the hide state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Hide_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200834, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入仇恨 %0 状态。 / [%SkillCaster] is in the enmity %0 state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HostileUp_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200835, skillcaster, value0, skillname);
	}

	/**
	 * @return [%SkillCaster] 因使用 [%SkillName] 进入魔法反击状态。 / [%SkillCaster] is in the magical counterattack state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200836, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200837, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MoveBehindATK_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200838, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] ' sMP 曾 reduced 按 %num0 作为其 used[%SkillName]。 / [%SkillCaster]'s MP was reduced by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200839, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] is in the Mana Treatment state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200840, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200841, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200842, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入恢复技能强化状态。 / [%SkillCaster] is in the boost recovery skill state because he used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostHealEffect_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200843, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is in the boost skill state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillAttack_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200844, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is in the critical hit state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillCritical_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200845, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] is in the boost skill state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeTypeBoostSkillLevel_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200846, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] caused the spirit to use its skill by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUseUltraSkill_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200847, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has transformed into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Polymorph_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200848, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200849, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_Ratio_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200850, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使飞行时间增加 %num0。 / [%SkillCaster]'s flight time increased by %num0 as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCFPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200851, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200852, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCMPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200853, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入保护状态。 / [%SkillCaster] is in the protection state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200854, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 格挡了 %num0 点伤害。 / [%SkillCaster] blocked %num0 damage by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200855, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 受到自身 [%SkillName] 影响。 / [%SkillCaster] was affected by its own [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Provoker_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200856, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] received %num0 damage and was put in the pull state because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Pulled_A_TO_SELF(String skillcaster, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200857, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入传送状态。 / [%SkillCaster] is in the teleport state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_RandomMoveLoc_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200858, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 为在 reservedresurrectionstate 作为其 used[%SkillName]。 / [%SkillCaster] is in the reserved resurrection state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Rebirth_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200859, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入反射状态。 / [%SkillCaster] is in the reflection state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200860, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] reflected %num0 damage by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200861, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入复活状态。 / [%SkillCaster] is in the resurrection state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Resurrect_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200862, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入召唤复活状态。 / [%SkillCaster] is in the summon-resurrection state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectPositional_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200863, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入透视状态。 / [%SkillTarget] is in the see-through state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Search_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200864, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] has transformed into %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ShapeChange_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200865, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 进入防御护盾状态。 / [%SkillCaster] is in the defense shield state as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200866, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 格挡了 %num0 点伤害。 / [%SkillCaster] blocked %num0 damage by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200867, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SignetBurst_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200868, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATK_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200869, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200870, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] received the continuous damage effect because he used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200871, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] received %num0 damage as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200872, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] absorbed %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200873, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] absorbed %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200874, skillcaster, num0, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入移动速度提升状态。 / [%SkillTarget] is in the movement speed increase state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sprint_A_TO_SELF(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200875, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入弱化 %0状态。 / [%SkillTarget] is in the weaken %0 state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatDown_A_TO_SELF(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200876, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入强化 %0状态。 / [%SkillTarget] is in the boost %0 state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatUp_A_TO_SELF(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200877, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因使用 [%SkillName] 使 %0 抗性效果被削弱。 / [%SkillCaster]'s %0 resistance effects are weakened as it used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeBoostResist_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200878, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] changed his %0 skill duration by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeExtendDuration_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200879, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Summon_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200880, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonBindingGroupGate_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200881, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonGroupGate_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200882, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonHoming_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200883, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonServant_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200884, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTotem_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200885, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTrap_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200886, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] exchanged his HP and MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHPMP_Instant_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200887, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] changed his %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_WeaponStatUp_A_TO_SELF(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200888, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillTarget] became blinded because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200889, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became confused because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200890, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became diseased because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200891, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the fear state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the fear state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200892, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became paralyzed because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200893, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 无法飞行。 / [%SkillTarget] is unable to fly because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200894, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became silenced because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200895, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] fell asleep because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200896, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] spinning because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is spinning because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200897, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 因感电被击退。 / [%SkillTarget] was knocked back from shock because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200898, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] fell down from shock because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200899, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became stunned because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200900, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] 不再失明。
	  * [%SkillTarget] 不再失明。 / [%SkillTarget] is no longer blind
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200901, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再混乱。
	  * [%SkillTarget] 不再混乱。 / [%SkillTarget] is no longer confused
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200902, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再疾病。
	  * [%SkillTarget] 不再疾病。 / [%SkillTarget] is no longer diseased
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200903, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再恐惧。
	  * [%SkillTarget] 不再恐惧。 / [%SkillTarget] is no longer afraid
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200904, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再麻痹。
	  * [%SkillTarget] 不再麻痹。 / [%SkillTarget] is no longer paralyzed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200905, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再定身。
	  * [%SkillTarget] 不再定身。 / [%SkillTarget] is no longer immobilized
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200906, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再沉默。
	  * [%SkillTarget] 不再沉默。 / [%SkillTarget] is no longer silenced
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200907, skilltarget);
	}

	 /**
	  * [%SkillTarget] 已醒来。
	  * [%SkillTarget] 已醒来。 / [%SkillTarget] woke up
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200908, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再旋转。
	  * [%SkillTarget] 不再旋转。 / [%SkillTarget] is no longer spinning
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200909, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再踉跄。
	  * [%SkillTarget] 不再踉跄。 / [%SkillTarget] is no longer staggering
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200910, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再冲击。
	  * [%SkillTarget] 不再冲击。 / [%SkillTarget] is no longer shocked
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200911, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再眩晕。
	  * [%SkillTarget] 不再眩晕。 / [%SkillTarget] is no longer stunned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200912, skilltarget);
	}

	/**
	 * [%SkillTarget]'s loot rate has increased because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostDropRate_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200913, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 视野范围降低。 / [%SkillTarget]'s visual range has decreased because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OutofSight_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200914, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] exchanged his enmity toward [%SkillTarget] with his spirit's by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHostile_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200915, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ReturnHome_A_TO_B(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200916, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] started using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_A_TO_B(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200917, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] stopped using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Aura_END_A_TO_B(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200918, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became bound because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200919, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] bleeding because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is bleeding because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200920, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] cursed because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is cursed because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200921, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 无法飞行。 / [%SkillTarget] is unable to fly because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200922, skilltarget, skillcaster, skillname);
	}

	/**
	 * @return 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 被空中束缚。 / [%SkillTarget] became snared in mid-air because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200923, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became petrified because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200924, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] became poisoned because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200925, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 攻击速度降低。 / [%SkillTarget]'s attack speed has decreased because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200926, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 攻击速度降低。 / [%SkillTarget]'s attack speed has decreased because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200927, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] 不再束缚。
	  * [%SkillTarget] 不再束缚。 / [%SkillTarget] is no longer bound
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200928, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再流血。
	  * [%SkillTarget] 不再流血。 / [%SkillTarget] is no longer bleeding
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200929, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 从诅咒状态中恢复。 / [%SkillTarget] recovered from the cursed state
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200930, skilltarget);
	}

	 /**
	  * [%SkillTarget] able to fly again。
	  * [%SkillTarget] is able to fly again
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200931, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 解除空中陷阱。 / [%SkillTarget] is released from the aerial snare
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200932, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再石化。
	  * [%SkillTarget] 不再石化。 / [%SkillTarget] is no longer petrified
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200933, skilltarget);
	}

	 /**
	  * [%SkillTarget] 不再中毒。
	  * [%SkillTarget] 不再中毒。 / [%SkillTarget] is no longer poisoned
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200934, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 的攻击速度恢复正常。 / [%SkillTarget]'s attack speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200935, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 的移动速度恢复正常。 / [%SkillTarget]'s movement speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_A_TO_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1200936, skilltarget);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入格挡强化状态。 / [%SkillTarget] is in the boost block state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysBlock_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200937, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the boost evasion state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the boost evasion state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysDodge_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200938, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the boost accuracy state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the boost accuracy state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysHit_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200939, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入无元素防御状态。 / [%SkillTarget] is in the no elemental defense state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysNoResist_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200940, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the boost parry state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the boost parry state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysParry_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200941, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入元素最大防御状态。 / [%SkillTarget] is in the elemental maximum defense state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_AlwaysResist_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200942, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入恢复技能强化状态。 / [%SkillTarget] is in the boost recovery skill state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHealEffect_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200943, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] changed [%SkillTarget]'s casting speed by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCastingTime_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200944, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] changed [%SkillTarget]'s MP consumption by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillCost_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200945, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget]'s mantra skill MP consumption has decreased because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSkillToggleCost_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200946, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the boost spell skill state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the boost spell skill state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostSpellAttackEffect_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200947, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BackDashATK_A_TO_B(String skillcaster, int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200948, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入旋转状态。 / [%SkillTarget] is in the spinning state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_BoostHate_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200949, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] inflicted %num0 damage on [%SkillTarget] and caused the Rune Carve effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CarveSignet_A_TO_B(String skillcaster, int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200950, skillcaster, num0, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] recovering HP because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is recovering HP because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200951, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200952, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入旋转状态。 / [%SkillTarget] is in the spinning state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ChangeHateOnAttacked_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200953, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] was released from the aerial snare because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CloseAerial_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200954, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the transformation recovery state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the transformation recovery state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200955, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200956, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DashATK_A_TO_B(String skillcaster, int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200957, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeathBlow_A_TO_B(String skillcaster, int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200958, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] changed [%SkillTarget]'s recovery amount by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DeboostHealAmount_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200959, skillcaster, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 变身为 %0。 / [%SkillTarget] has transformed into %0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Deform_A_TO_B(String skilltarget, String value0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200960, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 即将受到飞行时间减少效果。 / [%SkillTarget] will receive the decrease flight time effect in a moment because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200961, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间减少 %num0。 / [%SkillTarget]'s flight time has decreased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200962, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] received the Delayed Blast effect because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_A_TO_B(String skilltarget,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200963, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间减少 %num0。 / [%SkillTarget]'s flight time has decreased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200964, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] dispelled the magic effect from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Dispel_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200965, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法增益。 / [%SkillCaster] dispelled the magical buffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuff_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200966, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] inflicted %num0 damage on [%SkillTarget] and dispelled some of its magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelBuffCounterATK_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200967, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法减益。 / [%SkillCaster] dispelled the magical debuffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuff_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200968, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了 [%SkillTarget] 的异常精神状态。 / [%SkillCaster] removed abnormal mental conditions from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffMental_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200969, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了 [%SkillTarget] 的异常身体状态。 / [%SkillCaster] removed abnormal physical conditions from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelDeBuffPhysical_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200970, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] transferred %num0 DP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPTransfer_A_TO_B(String skilltarget, int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200971, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] boosted [%SkillTarget]'s mantra range by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ExtendAuraRange_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200972, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] was put in the forced crash state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fall_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200973, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 即将受到飞行时间减少效果。 / [%SkillTarget] will receive the decrease flight time effect in a moment because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200974, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间减少 %num0。 / [%SkillTarget]'s flight time has decreased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200975, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间减少 %num0。 / [%SkillTarget]'s flight time has decreased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_Instant_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200976, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the flight time recovery state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the flight time recovery state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200977, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间增加 %num0。 / [%SkillTarget]'s flight time increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200978, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间增加 %num0。 / [%SkillTarget]'s flight time increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200979, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the continuous healing state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the continuous healing state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200980, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200981, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200982, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the convert damage healing state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the convert damage healing state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200983, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200984, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the convert death healing state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the convert death healing state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200985, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200986, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入旋转状态。 / [%SkillTarget] is in the spinning state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Hide_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200987, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入旋转状态。 / [%SkillTarget] is in the spinning state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HostileUp_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200988, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] was put in the magical counterattack state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200989, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200990, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MoveBehindATK_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200991, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget]'s MP was reduced by %num0 because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_Instant_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200992, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入 Mana Treatment 状态。 / [%SkillTarget] is in the Mana Treatment state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200993, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200994, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200995, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入恢复技能强化状态。 / [%SkillTarget] is in the boost recovery skill state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostHealEffect_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1200996, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入技能强化状态。 / [%SkillTarget] is in the boost skill state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillAttack_A_TO_B(String skilltarget,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200997, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the critical hit state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the critical hit state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeBoostSkillCritical_A_TO_B(String skilltarget,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200998, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入技能强化状态。 / [%SkillTarget] is in the boost skill state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OneTimeTypeBoostSkillLevel_A_TO_B(String skilltarget,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1200999, skilltarget, skillcaster, skillname);
	}

	 /**
	  * spirit used a skill on [%SkillTarget] because [%SkillCaster] used [%SkillName]。
	  * The spirit used a skill on [%SkillTarget] because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUseUltraSkill_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201000, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 变身为 %0。 / [%SkillTarget] has transformed into %0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Polymorph_A_TO_B(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201001, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201002, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_Ratio_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201003, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 飞行时间增加 %num0。 / [%SkillTarget]'s flight time increased by %num0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCFPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201004, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 HP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201005, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PROCMPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201006, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the protection state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the protection state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201007, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201008, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] was affected because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Provoker_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201009, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] received %num0 damage and was put in the pull state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Pulled_A_TO_B(String skilltarget, int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201010, skilltarget, num0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the teleport state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the teleport state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_RandomMoveLoc_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201011, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] entered the reserved resurrection state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Rebirth_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201012, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the reflection state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the reflection state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201013, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] reflected %num0 damage because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201014, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入复活状态。 / [%SkillTarget] is in the resurrection state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Resurrect_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201015, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the resurrection summoning state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the resurrection summoning state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectPositional_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201016, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入透视状态。 / [%SkillTarget] is in the see-through state because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Search_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201017, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 变身为 %0。 / [%SkillTarget] has transformed into %0 because [%SkillCaster] used [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ShapeChange_A_TO_B(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201018, skilltarget, value0, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] in the defense shield state because [%SkillCaster] used [%SkillName]。
	  * [%SkillTarget] is in the defense shield state because [%SkillCaster] used [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201019, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201020, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SignetBurst_A_TO_B(String skillcaster, int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201021, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATK_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201022, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201023, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 施加持续伤害效果。 / [%SkillCaster] used [%SkillName] to inflict the continuous damage effect on [%SkillTarget]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_A_TO_B(String skillcaster, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201024, skillcaster, skillname, skilltarget);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201025, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] absorbs [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201026, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201027, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget]'s movement speed increased because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sprint_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201028, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入弱化 %0状态。 / [%SkillTarget] is in the weaken %0 state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatDown_A_TO_B(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201029, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 使用 [%SkillName]，[%SkillTarget] 进入强化 %0状态。 / [%SkillTarget] is in the boost %0 state because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_StatUp_A_TO_B(String skilltarget, String value0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201030, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget]'s %0 resistance effects were weakened because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeBoostResist_A_TO_B(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201031, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget]'s %0 skill durations changed because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SubTypeExtendDuration_A_TO_B(String skilltarget, String value0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201032, skilltarget, value0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Summon_A_TO_B(String skillcaster, String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201033, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤了 %0。 / [%SkillCaster] summoned %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonBindingGroupGate_A_TO_B(String skillcaster, String value0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201034, skillcaster, value0, skillname);
	}

	/**
	 * [%SkillCaster] has summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonGroupGate_A_TO_B(String skillcaster, String value0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201035, skillcaster, value0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤 %0 攻击 [%SkillTarget]。 / [%SkillCaster] has summoned %0 to attack [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonHoming_A_TO_B(String skillcaster, String value0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201036, skillcaster, value0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 召唤 %0 攻击 [%SkillTarget]。 / [%SkillCaster] has summoned %0 to attack [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonServant_A_TO_B(String skillcaster, String value0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201037, skillcaster, value0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTotem_A_TO_B(String skillcaster, String value0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201038, skillcaster, value0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has summoned %0 to [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonTrap_A_TO_B(String skillcaster, String value0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201039, skillcaster, value0, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] exchanged [%SkillTarget]'s HP and MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SwitchHPMP_Instant_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201040, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] changed [%SkillTarget]'s %0 by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_WeaponStatUp_A_TO_B(String skillcaster, String skilltarget,
			String value0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201041, skillcaster, skilltarget, value0, skillname);
	}

	/**
	 * [%SkillTarget] received %num0 bleeding damage after you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_INTERVAL_TO_B(String skilltarget, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201042, skilltarget, num0, skillname);
	}

	/**
	 * 你：received %num0 bleeding damage due to the effect of [%SkillName]。 / You received %num0 bleeding damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201043, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_HEAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201044, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_CaseHeal_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201045, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_HEAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201046, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201047, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_HEAL_MP_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201048, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ConvertHeal_INTERVAL_HEAL_MP_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201049, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间减少 %num0。 / [%SkillTarget]'s flight time decreased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201050, skilltarget, num0, skillname);
	}

	/**
	 * 因 [%SkillName] 效果，你的飞行时间减少 %num0。 / Your flight time decreased by %num0 due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedFPATK_Instant_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201051, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果受到 %num0 点伤害。 / [%SkillTarget] received %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201052, skilltarget, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You received %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSpellATK_Instant_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201053, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间减少 %num0。 / [%SkillTarget]'s flight time decreased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_TO_B(String skilltarget, int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201054, skilltarget, num0, skillname);
	}

	/**
	 * 因 [%SkillName] 效果，你的飞行时间减少 %num0。 / Your flight time decreased by %num0 due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPATK_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201055, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果飞行时间增加 %num0。 / [%SkillTarget]'s flight time increased by %num0 due to the effect of [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_HEAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201056, skilltarget, num0, skillname);
	}

	 /**
	  * 因 [%SkillName] 的效果，你的飞行时间增加了 %num0。
	  * Your flight time increased by %num0 due to the effect of [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_FPHeal_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201057, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_HEAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201058, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Heal_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201059, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_HEAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201060, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnAttacked_INTERVAL_HEAL_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201061, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_HEAL_TO_B(int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201062, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_HealCastorOnTargetDead_INTERVAL_HEAL_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201063, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果受到 %num0 点伤害。 / [%SkillTarget] received %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201064, skilltarget, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You receive %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MagicCounterATK_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201065, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_HEAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201066, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPHeal_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201067, num0, skillname);
	}

	/**
	 * [%SkillTarget] received %num0 poisoning damage after you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201068, skilltarget, num0, skillname);
	}

	/**
	 * 你：received %num0 poisoning damage due to the effect of [%SkillName]。 / You received %num0 poisoning damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201069, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage through the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201070, skilltarget, num0, skillname);
	}

	/**
	 * 你：blocked %num0 damage through the effect of [%SkillName]。 / You blocked %num0 damage through the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Protect_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201071, num0, skillname);
	}

	/**
	 * [%SkillTarget] 反弹了 %num0 点伤害。 / [%SkillTarget] reflected %num0 damage
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_TO_B(String skilltarget, int num0) {
		return new SM_SYSTEM_MESSAGE(1201072, skilltarget, num0);
	}

	/**
	 * @param num0 你：reflected %num0 damage。 / You reflected %num0 damage
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_INTERVAL_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1201073, num0);
	}

	/**
	 * [%SkillTarget] 格挡了 %num0 点伤害。 / [%SkillTarget] blocked %num0 damage
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_TO_B(String skilltarget, int num0) {
		return new SM_SYSTEM_MESSAGE(1201074, skilltarget, num0);
	}

	/**
	 * @param num0 你：blocked %num0 damage。 / You blocked %num0 damage
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_INTERVAL_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1201075, num0);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_INTERVAL_HEAL_TO_B(int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201076, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_INTERVAL_HEAL_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201077, num0, skillname);
	}

	/**
	 * [%SkillTarget] absorbed [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201078, skilltarget, skillname);
	}

	/**
	 * [%SkillName] conflicted with [%SkillTarget]'s existing skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CONFLICT_ME_TO_B(String skillname, String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201079, skillname, skilltarget);
	}

	/**
	 * @param value0 %0 闪避了攻击。 / %0 evaded the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_DODGED_ME_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1201080, value0);
	}

	/**
	 * [%SkillTarget] resisted [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201081, skilltarget, skillname);
	}

	 /**
	  * [%SkillTarget] immune to your [%SkillName]。
	  * [%SkillTarget] is immune to your [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201082, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillTarget] 效果过多，[%SkillName] 被取消。 / [%SkillName] was cancelled as [%SkillTarget] is under too many effects
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_NO_AVAILABLE_SLOT_ME_TO_B(String skillname, String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201083, skillname, skilltarget);
	}

	/**
	 * @param value0 %0 格挡了攻击。 / %0 blocked the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_BLOCK_ME_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1201084, value0);
	}

	/**
	 * @param value0 %0 招架了攻击。 / %0 parried the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARRY_ME_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1201085, value0);
	}

	/**
	 * 你：absorbed [%SkillCaster]'s [%SkillName]。 / You absorbed [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201086, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster]'s [%SkillName] conflicted with your existing skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CONFLICT_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201087, skillcaster, skillname);
	}

	/**
	 * @param value0 你闪避了 %0 的攻击。 / You evaded %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_DODGED_A_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1201088, value0);
	}

	/**
	 * 你：resisted [%SkillCaster]'s [%SkillName]。 / You resisted [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201089, skillcaster, skillname);
	}

	/**
	 * 你对 [%SkillCaster] 的 [%SkillName] 免疫。 / You are immune to [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201090, skillcaster, skillname);
	}

	/**
	 * 因你效果过多，[%SkillCaster] 的 [%SkillName] 被取消。 / [%SkillCaster]'s [%SkillName] was cancelled as you are under too many effects
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_NO_AVAILABLE_SLOT_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201091, skillcaster, skillname);
	}

	/**
	 * @param value0 你格挡了 %0 的攻击。 / You blocked %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_BLOCK_A_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1201092, value0);
	}

	/**
	 * @param value0 你招架了 %0 的攻击。 / You parried %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARRY_A_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1201093, value0);
	}

	/**
	 * [%SkillTarget] 受到 [%SkillCaster] 的 [%SkillName] 影响。 / [%SkillTarget] was affected by [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201094, skilltarget, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 的 [%SkillName] 与 [%SkillTarget] 已有技能冲突。 / [%SkillCaster]'s [%SkillName] conflicted with [%SkillTarget]'s existing skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CONFLICT_A_TO_B(String skillcaster, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201095, skillcaster, skillname, skilltarget);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_DODGED_A_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1201096, value0, value1);
	}

	/**
	 * [%SkillTarget] resisted [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201097, skilltarget, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] immune to [%SkillCaster]'s [%SkillName]。
	  * [%SkillTarget] is immune to [%SkillCaster]'s [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_A_TO_B(String skilltarget, String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201098, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillTarget] 效果过多，[%SkillCaster] 的 [%SkillName] 被取消。 / [%SkillCaster]'s [%SkillName] was cancelled as [%SkillTarget] is under too many effects
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_NO_AVAILABLE_SLOT_A_TO_B(String skillcaster, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201099, skillcaster, skillname, skilltarget);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_BLOCK_A_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1201100, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARRY_A_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1201101, value0, value1);
	}

	/**
	 * [%SkillTarget] evaded [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_DODGED_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201102, skilltarget, skillname);
	}

	/**
	 * 你：evaded [%SkillCaster]'s [%SkillName]。 / You evaded [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_DODGED_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201103, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] evaded [%SkillCaster]'s [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_DODGED_A_TO_B(String skilltarget, String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201104, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你：can see again。 / You can see again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_ME = new SM_SYSTEM_MESSAGE(1201105);

	/**
	 * 你不再混乱。 / You are no longer confused
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_ME = new SM_SYSTEM_MESSAGE(1201106);

	/**
	 * 你不再患病。 / You are no longer diseased
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_ME = new SM_SYSTEM_MESSAGE(1201107);

	/**
	 * 你从恐惧中恢复。 / You recovered from your fear
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_ME = new SM_SYSTEM_MESSAGE(1201108);

	/**
	 * 你不再麻痹。 / You are no longer paralyzed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_ME = new SM_SYSTEM_MESSAGE(1201109);

	/**
	 * 你不再定身。 / You are no longer immobilized
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_ME = new SM_SYSTEM_MESSAGE(1201110);

	/**
	 * 你不再沉默。 / You are no longer silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_ME = new SM_SYSTEM_MESSAGE(1201111);

	/**
	 * 你醒来了。 / You woke up
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_ME = new SM_SYSTEM_MESSAGE(1201112);

	/**
	 * 你停止了旋转。 / You have stopped spinning
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_ME = new SM_SYSTEM_MESSAGE(1201113);

	/**
	 * 你不再踉跄。 / You are no longer staggering
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_ME = new SM_SYSTEM_MESSAGE(1201114);

	/**
	 * 你不再感电。 / You are no longer shocked
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_ME = new SM_SYSTEM_MESSAGE(1201115);

	/**
	 * 你不再眩晕。 / You are no longer stunned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_ME = new SM_SYSTEM_MESSAGE(1201116);

	/**
	 * 你不再束缚。 / You are no longer bound
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_ME = new SM_SYSTEM_MESSAGE(1201117);

	/**
	 * 你不再流血。 / You are no longer bleeding
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_ME = new SM_SYSTEM_MESSAGE(1201118);

	/**
	 * 你：recovered from the cursed state。 / You recovered from the cursed state
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_ME = new SM_SYSTEM_MESSAGE(1201119);

	/**
	 * 你处于可再次飞行状态。 / You are able to fly again
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_ME = new SM_SYSTEM_MESSAGE(1201120);

	/**
	 * 你从空中束缚中解脱。 / You are released from the Aerial Snare
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_ME = new SM_SYSTEM_MESSAGE(1201121);

	/**
	 * 你不再石化。 / You are no longer petrified
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_ME = new SM_SYSTEM_MESSAGE(1201122);

	/**
	 * 你不再中毒。 / You are no longer poisoned
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_ME = new SM_SYSTEM_MESSAGE(1201123);

	/**
	 * 你的攻击速度恢复正常。 / Your attack speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_ME = new SM_SYSTEM_MESSAGE(1201124);

	/**
	 * 你的移动速度恢复正常。 / Your movement speed is restored to normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_ME = new SM_SYSTEM_MESSAGE(1201125);

	/**
	 * @param value0 %0 不再处于目盲状态。 / %0 is no longer blinded
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Blind_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201126, value0);
	}

	/**
	 * @param value0 %0 不再处于混乱状态。 / %0 is no longer confused
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Confuse_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201127, value0);
	}

	/**
	 * @param value0 %0 不再处于疾病状态。 / %0 is no longer diseased
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Disease_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201128, value0);
	}

	/**
	 * @param value0 %0 不再处于恐惧状态。 / %0 is no longer afraid
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Fear_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201129, value0);
	}

	/**
	 * @param value0 %0 不再处于麻痹状态。 / %0 is no longer paralyzed
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Paralyze_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201130, value0);
	}

	/**
	 * @param value0 %0 不再处于定身状态。 / %0 is no longer immobilized
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Root_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201131, value0);
	}

	/**
	 * @param value0 %0 不再处于沉默状态。 / %0 is no longer silenced
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Silence_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201132, value0);
	}

	/**
	 * @param value0 %0 醒来了。 / %0 woke up
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Sleep_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201133, value0);
	}

	/**
	 * @param value0 %0 不再处于旋转状态。 / %0 is no longer spinning
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Spin_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201134, value0);
	}

	/**
	 * @param value0 %0 不再处于踉跄状态。 / %0 is no longer staggering
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stagger_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201135, value0);
	}

	/**
	 * @param value0 %0 不再处于感电状态。 / %0 is no longer shocked
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stumble_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201136, value0);
	}

	/**
	 * @param value0 %0 不再处于眩晕状态。 / %0 is no longer stunned
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Stun_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201137, value0);
	}

	/**
	 * @param value0 %0 不再处于束缚状态。 / %0 is no longer bound
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bind_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201138, value0);
	}

	/**
	 * @param value0 %0 不再处于流血状态。 / %0 is no longer bleeding
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Bleed_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201139, value0);
	}

	/**
	 * @param value0 %0 从诅咒状态中恢复。 / %0 recovered from the cursed state
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Curse_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201140, value0);
	}

	 /**
	  * %0 is able to fly again。
	  * %0 is able to fly again
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoFly_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201141, value0);
	}

	/**
	 * @param value0 %0 解除空中陷阱。 / %0 is released from the Aerial Snare
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_OpenAerial_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201142, value0);
	}

	/**
	 * @param value0 %0 从石化状态中恢复。 / %0 recovered from the petrified state
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Petrification_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201143, value0);
	}

	/**
	 * @param value0 %0 不再处于中毒状态。 / %0 is no longer poisoned
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Poison_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201144, value0);
	}

	/**
	 * @param value0 %0 的攻击速度恢复正常。 / %0's attack speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Slow_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201145, value0);
	}

	/**
	 * @param value0 %0 的移动速度恢复正常。 / %0's movement speed is restored to normal
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Snare_END_A(String value0) {
		return new SM_SYSTEM_MESSAGE(1201146, value0);
	}

	/**
	 * @param skillname 你：use [%SkillName]。 / You use [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonSkillArea_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201147, skillname);
	}

	/**
	 * [%SkillCaster] uses [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonSkillArea_ME_TO_B(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201148, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] uses [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonSkillArea_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201149, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] uses [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonSkillArea_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201150, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] uses [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SummonSkillArea_A_TO_B(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201151, skillcaster, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果受到 %num0 点伤害。 / [%SkillTarget] received %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201152, skilltarget, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You receive %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATK_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201153, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_INTERVAL_HEAL_TO_B(int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201154, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_INTERVAL_HEAL_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201155, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_INTERVAL_HEAL_MP_TO_B(String skilltarget,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201156, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_Instant_INTERVAL_HEAL_MP_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201157, num0, skillname);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果受到 %num0 点伤害。 / [%SkillTarget] received %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201158, skilltarget, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You receive %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201159, num0, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s HP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_HEAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201160, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 HP by using [%SkillName]。 / You recovered %num0 HP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_HEAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201161, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_HEAL_MP_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201162, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SpellATKDrain_INTERVAL_HEAL_MP_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201163, num0, skillname);
	}

	/**
	 * Youreceived%num0damage 作为 [%SkillName]youused 在 [%SkillTarget] 曾 reflectedback 在 you。 / You received %num0 damage as the [%SkillName] you used on [%SkillTarget] was reflected back at you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_SKILL_ME_to_B(int num0, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201164, num0, skillname, skilltarget);
	}

	/**
	 * [%SkillCaster] 通过反射 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by reflecting [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_SKILL_A_to_ME(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201165, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] 通过反射 [%SkillName] 对 [%SkillCaster] 造成 %num0 点伤害。 / [%SkillTarget] inflicted %num0 damage on [%SkillCaster] by reflecting [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_SKILL_A_to_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201166, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 你对 [%SkillTarget] 的攻击被反射，你受到 %num0 点伤害。 / Your attack on [%SkillTarget] was reflected and inflicted %num0 damage on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_ME_to_B(String skilltarget, int num0) {
		return new SM_SYSTEM_MESSAGE(1201167, skilltarget, num0);
	}

	/**
	 * [%SkillTarget] 通过反弹攻击对 [%SkillCaster] 造成 %num0 点伤害。 / [%SkillTarget] inflicted %num0 damage on [%SkillCaster] by reflecting the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_A_to_ME(String skilltarget, int num0,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201168, skilltarget, num0, skillcaster);
	}

	/**
	 * [%SkillTarget] 通过反弹攻击对 [%SkillCaster] 造成 %num0 点伤害。 / [%SkillTarget] inflicted %num0 damage on [%SkillCaster] by reflecting the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Reflector_PROTECT_A_to_B(String skilltarget, int num0,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201169, skilltarget, num0, skillcaster);
	}

	/**
	 * [%SkillName] 被施加于 [%SkillTarget] 的防护盾效果格挡。 / [%SkillName] was blocked by the protective shield effect cast on [%SkillTarget]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_SKILL_ME_to_B(String skillname, String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201170, skillname, skilltarget);
	}

	/**
	 * 你：blocked the [%SkillName] used by [%SkillCaster] with the protective shield effect。 / You blocked the [%SkillName] used by [%SkillCaster] with the protective shield effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_SKILL_A_to_ME(String skillname, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201171, skillname, skillcaster);
	}

	/**
	 * [%SkillTarget] 以防护盾效果格挡了 [%SkillCaster] 的 [%SkillName]。 / [%SkillTarget] blocked the [%SkillName] used by [%SkillCaster] with the protective shield effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_SKILL_A_to_B(String skilltarget, String skillname,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201172, skilltarget, skillname, skillcaster);
	}

	/**
	 * @param skilltarget 攻击被施加在 [%SkillTarget] 上的防护盾效果格挡。 / The attack was blocked by the protective shield effect cast on [%SkillTarget]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_ME_to_B(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201173, skilltarget);
	}

	/**
	 * @param skillcaster 你：blocked [%SkillCaster]'s attack with the protective shield effect。 / You blocked [%SkillCaster]'s attack with the protective shield effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_A_to_ME(String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201174, skillcaster);
	}

	/**
	 * [%SkillTarget] 以防护盾效果格挡了 [%SkillCaster] 的攻击。 / [%SkillTarget] blocked [%SkillCaster]'s attack with the protective shield effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Shield_PROTECT_A_to_B(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201175, skilltarget, skillcaster);
	}

	/**
	 * Youreceived%num0damageinflicted 在 [%SkillTarget] 按 [%SkillCaster] ' s[%SkillName] , because 的 protectioneffectyoucast 在其。 / You received the %num0 damage inflicted on [%SkillTarget] by [%SkillCaster]'s [%SkillName], because of the protection effect you cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_SKILL_A_to_B(int num0, String skilltarget,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201176, num0, skilltarget, skillcaster, skillname);
	}

	/**
	 * Youreceived%num0damageinflicted 在 [%SkillTarget] 按 [%SkillCaster] , because 的 protectioneffectyoucast 在其。 / You received the %num0 damage inflicted on [%SkillTarget] by [%SkillCaster], because of the protection effect you cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_A_to_B(int num0, String skilltarget,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201177, num0, skilltarget, skillcaster);
	}

	/**
	 * 因保护效果，[%Protector] 承受了 [%SkillName] 对 [%SkillTarget] 造成的 %num0 点伤害。 / [%Protector] received the %num0 damage inflicted on [%SkillTarget] by a [%SkillName], because of the protection effect cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_SKILL_HEAL_ME_to_B(String protector, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201178, protector, num0, skilltarget, skillname);
	}

	/**
	 * 因保护效果，[%Protector] 承受了 [%SkillCaster] 的 [%SkillName] 造成的 %num0 点伤害。 / [%Protector] received the %num0 damage inflicted by [%SkillCaster] 's [%SkillName], because of the protection effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_SKILL_HEAL_A_to_ME(String protector, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201179, protector, num0, skillcaster, skillname);
	}

	/**
	 * 因保护效果，[%Protector] 承受了 [%SkillCaster] 的 [%SkillName] 对 [%SkillTarget] 造成的 %num0 点伤害。 / [%Protector] received the %num0 damage inflicted on [%SkillTarget] by [%SkillCaster]'s [%SkillName], because of the protection effect cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_SKILL_HEAL_A_to_B(String protector, int num0,
			String skilltarget, String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201180, protector, num0, skilltarget, skillcaster, skillname);
	}

	/**
	 * 因保护效果，[%Protector] 承受了对 [%SkillTarget] 造成的 %num0 点伤害。 / [%Protector] received %num0 damage inflicted on [%SkillTarget], because of the protection effect cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_HEAL_ME_to_B(String protector, int num0,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201181, protector, num0, skilltarget);
	}

	/**
	 * 因保护效果，[%Protector] 承受了 [%SkillCaster] 对你造成的 %num0 点伤害。 / [%Protector] received %num0 damage inflicted on you by [%SkillCaster], because of the protection effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_HEAL_A_to_ME(String protector, int num0,
			String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201182, protector, num0, skillcaster);
	}

	/**
	 * 因保护效果，[%Protector] 承受了 [%SkillCaster] 对 [%SkillTarget] 造成的 %num0 点伤害。 / [%Protector] received %num0 damage inflicted on [%SkillTarget] by [%SkillCaster], because of the protection effect cast on it
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_protect_PROTECT_HEAL_A_to_B(String protector, int num0,
			String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201183, protector, num0, skilltarget, skillcaster);
	}

	/**
	 * @param num0 你：recovered %num0 MP。 / You recovered %num0 MP
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MPHeal_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1201196, num0);
	}

	/**
	 * @param num0 你：restored your flight time by %num0。 / You restored your flight time by %num0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FPHeal_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1201197, num0);
	}

	/**
	 * @param num0 你：recovered %num0 HP。 / You recovered %num0 HP
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Heal_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1201198, num0);
	}

	 /**
	  * %0 restored %num1 MP。
	  * %0 restored %num1 MP
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_MPHeal_TO_OTHER(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1201199, value0, num1);
	}

	/**
	 * %0 恢复了 %num1 飞行时间。 / %0 restored his flight time by %num1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FPHeal_TO_OTHER(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1201200, value0, num1);
	}

	/**
	 * [%SkillTarget] 因 [%SkillName] 效果受到 %num0 点伤害。 / [%SkillTarget] received %num0 damage due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201201, skilltarget, num0, skillname);
	}

	/**
	 * 你因 [%SkillName] 受到 %num0 点伤害。 / You receive %num0 damage due to [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcATK_Instant_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201202, num0, skillname);
	}

	 /**
	  * %0 restored %num1 HP。
	  * %0 restored %num1 HP
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_Heal_TO_OTHER(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1201203, value0, num1);
	}

	/**
	 * 你：released [%SkillTarget]'s spirit by using [%SkillName]。 / You released [%SkillTarget]'s spirit by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUnSummon_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201204, skilltarget, skillname);
	}

	/**
	 * 你的精灵因 [%SkillCaster] 的 [%SkillName] 效果被解除召唤。 / Your spirit was unsummoned by the effect of [%SkillName] used by [%SkillCaster]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUnSummon_A_TO_ME(String skillname, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201205, skillname, skillcaster);
	}

	/**
	 * [%SkillCaster] released [%SkillTarget]'s spirit by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_PetOrderUnSummon_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201206, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成持续伤害。 / You inflicted continuous damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201207, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has reduced your MP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201208, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] gave [%SkillTarget] the continuous MP reduction effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201209, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget]'s MP decreased by %num0 due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_INTERVAL_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201210, skilltarget, num0, skillname);
	}

	 /**
	  * 你的MP decreased by %num0 due to the effect of [%SkillName]。
	  * Your MP decreased by %num0 due to the effect of [%SkillName]
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_MPAttack_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201211, num0, skillname);
	}

	/**
	 * @param skillname 你：gave yourself an XP bonus by using [%SkillName]。 / You gave yourself an XP bonus by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_XPBoost_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201212, skillname);
	}

	/**
	 * 你：gave [%SkillTarget] an XP bonus by using [%SkillName]。 / You gave [%SkillTarget] an XP bonus by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_XPBoost_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201213, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] gave you an XP bonus by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_XPBoost_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201214, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] gave themselves an XP bonus by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_XPBoost_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201215, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] gave [%SkillTarget] an XP bonus by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_XPBoost_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201216, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你：made yourself more resistant to crashing and prohibitions on flying by using [%SkillName]。 / You made yourself more resistant to crashing and prohibitions on flying by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InvulnerableWing_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201217, skillname);
	}

	/**
	 * 你：gave [%SkillTarget] the crash and flying prohibition resistance effects by using [%SkillName]。 / You gave [%SkillTarget] the crash and flying prohibition resistance effects by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InvulnerableWing_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201218, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] made you more resistant to crashing and prohibitions on flying by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InvulnerableWing_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201219, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] made themselves more resistant to crashing and prohibitions on flying by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InvulnerableWing_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201220, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] made [%SkillTarget] more resistant to crashing and prohibitions on flying by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InvulnerableWing_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201221, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 DP by using [%SkillName]。 / You recovered %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201222, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 DP because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201223, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 DP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 DP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201224, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201225, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 DP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201226, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * @param skillname 你：recovered some DP by using [%SkillName]。 / You recovered some DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_HEAL_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201227, skillname);
	}

	/**
	 * 你：restored some of [%SkillTarget]'s DP by using [%SkillName]。 / You restored some of [%SkillTarget]'s DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_HEAL_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201228, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] has boosted [%SkillTarget]'s DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_HEAL_A_TO_ME(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201229, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] restored some DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_HEAL_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201230, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] restored some of [%SkillTarget]'s DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_HEAL_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201231, skillcaster, skilltarget, skillname);
	}

	/**
	 * 你：restored %num0 of [%SkillTarget]'s DP by using [%SkillName]。 / You restored %num0 of [%SkillTarget]'s DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_INTERVAL_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201232, num0, skilltarget, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DPHeal_INTERVAL_TO_ME(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201233, num0, skillname);
	}

	/**
	 * 你：recovered %num0 DP by using [%SkillName]。 / You recovered %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcDPHeal_Instant_HEAL_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201234, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 DP because you used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcDPHeal_Instant_HEAL_ME_TO_B(String skilltarget, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201235, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 DP because [%SkillCaster] used [%SkillName] on you。 / You recovered %num0 DP because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcDPHeal_Instant_HEAL_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201236, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] recovered %num0 DP by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcDPHeal_Instant_HEAL_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201237, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 DP because [%SkillCaster] used [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ProcDPHeal_Instant_HEAL_A_TO_B(String skilltarget, int num0,
			String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201238, skilltarget, num0, skillcaster, skillname);
	}

	/**
	 * 你：caused [%SkillTarget] to forcibly resurrect at the bind point by using [%SkillName]。 / You caused [%SkillTarget] to forcibly resurrect at the bind point by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectBase_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201239, skilltarget, skillname);
	}

	/**
	 * 你因 [%SkillCaster] 对你使用 [%SkillName]，被迫在绑定点复活。 / You are forced to resurrect at the bind point because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectBase_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201240, skillcaster, skillname);
	}

	 /**
	  * [%SkillTarget] forced to resurrect at the bind point because [%SkillCaster] used [%SkillName] on it。
	  * [%SkillTarget] is forced to resurrect at the bind point because [%SkillCaster] used [%SkillName] on it
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_ResurrectBase_A_TO_B(String skilltarget, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201241, skilltarget, skillcaster, skillname);
	}

	/**
	 * 你：requested [%SkillTarget] to be summoned by using [%SkillName]。 / You requested [%SkillTarget] to be summoned by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Recall_Instant_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201242, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] requested you to be summoned by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Recall_Instant_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201243, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] requested [%SkillTarget] to be summoned by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_Recall_Instant_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201244, skillcaster, skilltarget, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 驱散了魔法增益。 / You dispelled magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCBuff_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201245, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 驱散了魔法减益。 / You dispelled magical debuffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuff_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201246, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 移除了异常精神状态。 / You removed abnormal mental conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffMental_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201247, skillname);
	}

	/**
	 * @param skillname 你使用 [%SkillName] 移除了异常身体状态。 / You removed abnormal physical conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffPhysical_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201248, skillname);
	}

	/**
	 * 你使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法增益。 / You dispelled magical buffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCBuff_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201249, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法减益。 / You dispelled magical debuffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuff_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201250, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 移除了 [%SkillTarget] 的异常精神状态。 / You removed abnormal mental conditions from [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffMental_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201251, skilltarget, skillname);
	}

	/**
	 * 你使用 [%SkillName] 移除了 [%SkillTarget] 的异常身体状态。 / You removed abnormal physical conditions from [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffPhysical_ME_TO_B(String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201252, skilltarget, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的魔法增益被驱散。 / Your magical buffs were dispelled because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCBuff_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201253, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的魔法减益被驱散。 / Your magical debuffs were dispelled because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuff_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201254, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的异常精神状态被移除。 / Your abnormal mental conditions were removed because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffMental_A_TO_ME(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201255, skillcaster, skillname);
	}

	/**
	 * 因 [%SkillCaster] 对你使用 [%SkillName]，你的异常身体状态被移除。 / Your abnormal physical conditions were removed because [%SkillCaster] used [%SkillName] on you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffPhysical_A_TO_ME(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201256, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了自身魔法增益。 / [%SkillCaster] dispelled its magical buffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCBuff_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201257, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了自身魔法减益。 / [%SkillCaster] dispelled its magical debuffs by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuff_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201258, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了自身异常精神状态。 / [%SkillCaster] removed its abnormal mental conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffMental_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201259, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了自身异常身体状态。 / [%SkillCaster] removed its abnormal physical conditions by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffPhysical_A_TO_SELF(String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201260, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法增益。 / [%SkillCaster] dispelled the magical buffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCBuff_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201261, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 驱散了 [%SkillTarget] 的魔法减益。 / [%SkillCaster] dispelled the magical debuffs from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuff_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201262, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了 [%SkillTarget] 的异常精神状态。 / [%SkillCaster] removed abnormal mental conditions from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffMental_A_TO_B(String skillcaster, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201263, skillcaster, skilltarget, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 移除了 [%SkillTarget] 的异常身体状态。 / [%SkillCaster] removed abnormal physical conditions from [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DispelNPCDeBuffPhysical_A_TO_B(String skillcaster,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201264, skillcaster, skilltarget, skillname);
	}

	/**
	 * @param skillname 你：received a delayed chain effect by using [%SkillName]。 / You received a delayed chain effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSkill_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201265, skillname);
	}

	/**
	 * [%SkillTarget] 受到你使用的 [%SkillName] 的延迟连锁效果。 / [%SkillTarget] received a delayed chain effect from [%SkillName] used by you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSkill_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201266, skilltarget, skillname);
	}

	/**
	 * 你：received a delayed chain effect from [%SkillName] used by [%SkillCaster]。 / You received a delayed chain effect from [%SkillName] used by [%SkillCaster]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSkill_A_TO_ME(String skillname, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201267, skillname, skillcaster);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 并获得延迟连锁效果。 / [%SkillCaster] used [%SkillName] and received a delayed chain effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSkill_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201268, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使 [%SkillTarget] 获得延迟连锁效果。 / [%SkillCaster] used [%SkillName] to give [%SkillTarget] a delayed chain effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_DelayedSkill_A_TO_B(String skillcaster, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201269, skillcaster, skillname, skilltarget);
	}

	/**
	 * @param skillname 你：received a periodic chain effect by using [%SkillName]。 / You received a periodic chain effect by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InteralSkill_ME_TO_SELF(String skillname) {
		return new SM_SYSTEM_MESSAGE(1201270, skillname);
	}

	/**
	 * [%SkillTarget] 受到你使用的 [%SkillName] 的周期连锁效果。 / [%SkillTarget] received a periodic chain effect from [%SkillName] used by you
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InteralSkill_ME_TO_B(String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201271, skilltarget, skillname);
	}

	/**
	 * 你：received a periodic chain effect from [%SkillName] used by [%SkillCaster]。 / You received a periodic chain effect from [%SkillName] used by [%SkillCaster]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InteralSkill_A_TO_ME(String skillname, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1201272, skillname, skillcaster);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 并获得周期连锁效果。 / [%SkillCaster] used [%SkillName] and received a periodic chain effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InteralSkill_A_TO_SELF(String skillcaster, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201273, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 使 [%SkillTarget] 获得周期连锁效果。 / [%SkillCaster] used [%SkillName] to give [%SkillTarget] a periodic chain effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_InteralSkill_A_TO_B(String skillcaster, String skillname,
			String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1201274, skillcaster, skillname, skilltarget);
	}

	/**
	 * 你：suffer %num0 damage from [%SkillName]。 / You suffer %num0 damage from [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoReduceSpellATK_Instant_ME_TO_SELF(int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201275, num0, skillname);
	}

	/**
	 * 你使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / You inflicted %num0 damage on [%SkillTarget] by using [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoReduceSpellATK_Instant_ME_TO_B(int num0, String skilltarget,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201276, num0, skilltarget, skillname);
	}

	/**
	 * 你：receive %num0 damage from [%SkillCaster]'s [%SkillName] effect。 / You receive %num0 damage from [%SkillCaster]'s [%SkillName] effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoReduceSpellATK_Instant_A_TO_ME(int num0, String skillcaster,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201277, num0, skillcaster, skillname);
	}

	/**
	 * [%SkillCaster] 因 [%SkillName] 受到 %num0 点伤害。 / [%SkillCaster] suffers %num0 damage from [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoReduceSpellATK_Instant_A_TO_SELF(String skillcaster, int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201278, skillcaster, num0, skillname);
	}

	/**
	 * [%SkillCaster] 使用 [%SkillName] 对 [%SkillTarget] 造成 %num0 点伤害。 / [%SkillCaster] inflicted %num0 damage on [%SkillTarget] by using [%SkillName].
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_NoReduceSpellATK_Instant_A_TO_B(String skillcaster, int num0,
			String skilltarget, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201279, skillcaster, num0, skilltarget, skillname);
	}

	/**
	 * [%SkillTarget] recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_INTERVAL_HEAL_MP_TO_B(String skilltarget,
			int num0, String skillname) {
		return new SM_SYSTEM_MESSAGE(1201280, skilltarget, num0, skillname);
	}

	/**
	 * 你：recovered %num0 MP due to the effect of [%SkillName]。 / You recovered %num0 MP due to the effect of [%SkillName]
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SUCC_SkillATKDrain_Instant_INTERVAL_HEAL_MP_TO_ME(int num0,
			String skillname) {
		return new SM_SYSTEM_MESSAGE(1201281, num0, skillname);
	}

	/**
	 * @param value0 你格挡了 %0 的攻击。 / You blocked %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_BLOCK(String value0) {
		return new SM_SYSTEM_MESSAGE(1210000, value0);
	}

	/**
	 * @param value0 你招架了 %0 的攻击。 / You parried %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_PARRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1210001, value0);
	}

	/**
	 * @param value0 你闪避了 %0 的攻击。 / You evaded %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_DODGE(String value0) {
		return new SM_SYSTEM_MESSAGE(1210002, value0);
	}

	/**
	 * @param skillcaster 你：resisted [%SkillCaster]'s magic。 / You resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_MY(String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1210003, skillcaster);
	}

	/**
	 * @param skillcaster 你对 [%SkillCaster] 的魔法免疫。 / You are immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_MY(String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1210004, skillcaster);
	}

	/**
	 * @param skillcaster 你：absorbed [%SkillCaster]'s magic。 / You absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_MY(String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1210005, skillcaster);
	}

	/**
	 * 你：received %num1 damage from %0。 / You received %num1 damage from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_ENEMY_ATTACK(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1210006, num1, value0);
	}

	/**
	 * 暴击！你受到来自 %0 的 %num1 点伤害。 / Critical Hit! You received %num1 damage from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_ENEMY_CRITICAL(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1210007, num1, value0);
	}

	/**
	 * @param value0 %0 格挡了你的攻击。 / %0 blocked your attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_ENEMY_BLOCK(String value0) {
		return new SM_SYSTEM_MESSAGE(1210224, value0);
	}

	/**
	 * @param value0 %0 招架了你的攻击。 / %0 parried your attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_ENEMY_PARRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1210225, value0);
	}

	/**
	 * @param value0 %0 闪避了你的攻击。 / %0 evaded your attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_ENEMY_DODGE(String value0) {
		return new SM_SYSTEM_MESSAGE(1210226, value0);
	}

	/**
	 * @param skilltarget [%SkillTarget] 抵抗了你的魔法。 / [%SkillTarget] resisted your magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_TARGET(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1210227, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 免疫你的魔法。 / [%SkillTarget] is immune to your magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_TARGET(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1210228, skilltarget);
	}

	/**
	 * @param skilltarget [%SkillTarget] 吸收了你的魔法。 / [%SkillTarget] absorbed your magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_TARGET(String skilltarget) {
		return new SM_SYSTEM_MESSAGE(1210229, skilltarget);
	}

	/**
	 * %0 对 %1 造成 %num2 点伤害。 / %0 inflicted %num2 damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ATTACK(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1220000, value0, num2, value1);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1220001, value0, num2, value1);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_BLOCK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1220219, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_PARRY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1220220, value0, value1);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_DODGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1220221, value0, value1);
	}

	/**
	 * [%SkillTarget] 抵抗了 [%SkillCaster] 的魔法。 / [%SkillTarget] resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_TO_MAGIC_PARTY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1220222, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 免疫 [%SkillCaster] 的魔法。 / [%SkillTarget] is immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_PARTY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1220223, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 吸收了 [%SkillCaster] 的魔法。 / [%SkillTarget] absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_PARTY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1220224, skilltarget, skillcaster);
	}

	/**
	 * %1 受到来自 %0 的 %num2 点伤害。 / %1 received %num2 damage from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ENEMY_ATTACK(String value1, int num2, String value0) {
		return new SM_SYSTEM_MESSAGE(1230000, value1, num2, value0);
	}

	/**
	 * 暴击！%1 受到来自 %0 的 %num2 点暴击伤害。 / Critical Hit! %1 received %num2 critical damage from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ENEMY_CRITICAL(String value1, int num2, String value0) {
		return new SM_SYSTEM_MESSAGE(1230001, value1, num2, value0);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ENEMY_BLOCK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1230218, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ENEMY_PARRY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1230219, value0, value1);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_PARTY_ENEMY_DODGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1230220, value0, value1);
	}

	/**
	 * [%SkillTarget] 抵抗了 [%SkillCaster] 的魔法。 / [%SkillTarget] resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_PARTY_ENEMY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1230221, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 免疫 [%SkillCaster] 的魔法。 / [%SkillTarget] is immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_PARTY_ENEMY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1230222, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 吸收了 [%SkillCaster] 的魔法。 / [%SkillTarget] absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_PARTY_ENEMY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1230223, skilltarget, skillcaster);
	}

	/**
	 * %0 对 %1 造成 %num2 点伤害。 / %0 inflicted %num2 damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_FRIENDLY_ATTACK(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1240000, value0, num2, value1);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_FRIENDLY_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1240001, value0, num2, value1);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_FRIENDLY_BLOCK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1240217, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_FRIENDLY_PARRY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1240218, value0, value1);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_FRIENDLY_DODGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1240219, value0, value1);
	}

	/**
	 * [%SkillTarget] 抵抗了 [%SkillCaster] 的魔法。 / [%SkillTarget] resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_OTHER_FRIENDLY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1240220, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 免疫 [%SkillCaster] 的魔法。 / [%SkillTarget] is immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_OTHER_FRIENDLY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1240221, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 吸收了 [%SkillCaster] 的魔法。 / [%SkillTarget] absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_OTHER_FRIENDLY(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1240222, skilltarget, skillcaster);
	}

	/**
	 * %0 对 %1 造成 %num2 点伤害。 / %0 inflicted %num2 damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_HOSTILE_ATTACK(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1250000, value0, num2, value1);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_HOSTILE_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1250001, value0, num2, value1);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_HOSTILE_BLOCK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1250217, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_HOSTILE_PARRY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1250218, value0, value1);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_HOSTILE_DODGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1250219, value0, value1);
	}

	/**
	 * [%SkillTarget] 抵抗了 [%SkillCaster] 的魔法。 / [%SkillTarget] resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_OTHER_HOSTILE(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1250220, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 免疫 [%SkillCaster] 的魔法。 / [%SkillTarget] is immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_OTHER_HOSTILE(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1250221, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 吸收了 [%SkillCaster] 的魔法。 / [%SkillTarget] absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_OTHER_HOSTILE(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1250222, skilltarget, skillcaster);
	}

	/**
	 * %0 对 %1 造成 %num2 点伤害。 / %0 inflicted %num2 damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_NPC_ATTACK(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1260000, value0, num2, value1);
	}

	/**
	 * 暴击！%0 对 %1 造成 %num2 点暴击伤害。 / Critical Hit! %0 inflicted %num2 critical damage on %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_NPC_CRITICAL(String value0, int num2, String value1) {
		return new SM_SYSTEM_MESSAGE(1260001, value0, num2, value1);
	}

	/**
	 * %0 格挡了 %1 的攻击。 / %0 blocked %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_NPC_BLOCK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1260217, value0, value1);
	}

	/**
	 * %0 招架了 %1 的攻击。 / %0 parried %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_NPC_PARRY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1260218, value0, value1);
	}

	/**
	 * %0 闪避了 %1 的攻击。 / %0 evaded %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_OTHER_NPC_DODGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1260219, value0, value1);
	}

	/**
	 * [%SkillTarget] 抵抗了 [%SkillCaster] 的魔法。 / [%SkillTarget] resisted [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_RESISTED_MAGIC_OTHER_NPC(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1260220, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 免疫 [%SkillCaster] 的魔法。 / [%SkillTarget] is immune to [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_IMMUNED_MAGIC_OTHER_NPC(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1260221, skilltarget, skillcaster);
	}

	/**
	 * [%SkillTarget] 吸收了 [%SkillCaster] 的魔法。 / [%SkillTarget] absorbed [%SkillCaster]'s magic
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSORBED_OTHER_NPC(String skilltarget, String skillcaster) {
		return new SM_SYSTEM_MESSAGE(1260222, skilltarget, skillcaster);
	}

	 /**
	  * weapon has been changed。
	  * The weapon has been changed
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHANGE_WEAPON = new SM_SYSTEM_MESSAGE(1300000);

	/**
	 * 你：can use it after registering it on the Quickbar。 / You can use it after registering it on the Quickbar
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NEED_TO_REGIST_SHORTCUT = new SM_SYSTEM_MESSAGE(1300001);

	/**
	 * 你：do not have much flight time left. Please land on a secure place。 / You do not have much flight time left. Please land on a secure place
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WARNING_FLY = new SM_SYSTEM_MESSAGE(1300002);

	/**
	 * 警告！你的飞行时间所剩不多。 / Warning! You do not have much flight time left
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WARNING_FLY_Notice = new SM_SYSTEM_MESSAGE(1300003);

	/**
	 * Yousuffereddamage 作为 you 有 submergeddeep 在 water.Please 获取 out 的 water。 / You suffered damage as you have submerged deep in the water. Please get out of the water
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WARNING_Swim = new SM_SYSTEM_MESSAGE(1300004);

	/**
	 * 警告！你因潜入深水而受到伤害。 / Warning! You suffered damage as you have submerged deep in the water
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WARNING_Swim_Notice = new SM_SYSTEM_MESSAGE(1300005);

	/**
	 * No target has been selected
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_TARGET = new SM_SYSTEM_MESSAGE(1300006);

	/**
	 * Invalid 目标 .You 可仅 use 此在 objects。 / Invalid target. You can only use this on objects
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_OBJECT_ONLY = new SM_SYSTEM_MESSAGE(1300007);

	/**
	 * Invalid 目标 .You 可仅 use 此在 NPCs。 / Invalid target. You can only use this on NPCs
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_NPC_ONLY = new SM_SYSTEM_MESSAGE(1300008);

	/**
	 * Invalid 目标 .You 可仅 use 此仅在 otherplayers。 / Invalid target. You can only use this only on other players
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_PC_ONLY = new SM_SYSTEM_MESSAGE(1300009);

	/**
	 * Invalid 目标 .You 可仅 use 此在 spirits。 / Invalid target. You can only use this on spirits
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_PET_ONLY = new SM_SYSTEM_MESSAGE(1300010);

	/**
	 * Invalid 目标 .You 可仅 use 此在 group 成员。 / Invalid target. You can only use this on group members
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_PARTY_ONLY = new SM_SYSTEM_MESSAGE(1300011);

	/**
	 * 你：can only use this on living targets。 / You can only use this on living targets
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TARGET_IS_NOT_ALIVE = new SM_SYSTEM_MESSAGE(1300012);

	/**
	 * Invalid target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TARGET_IS_NOT_VALID = new SM_SYSTEM_MESSAGE(1300013);

	/**
	 * 你：do not have enough health to use that skill。 / You do not have enough health to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NOT_ENOUGH_HP = new SM_SYSTEM_MESSAGE(1300014);

	/**
	 * 你：do not have enough mana to use that skill。 / You do not have enough mana to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NOT_ENOUGH_MP = new SM_SYSTEM_MESSAGE(1300015);

	/**
	 * 你：do not have enough DP to use that skill。 / You do not have enough DP to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NOT_ENOUGH_DP = new SM_SYSTEM_MESSAGE(1300016);

	/**
	 * You 无法 learndesignbecauseyourskilllevel 为 nothigh 足够。 / You cannot learn the design because your skill level is not high enough
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NOT_ENOUGH_DP_LEVEL = new SM_SYSTEM_MESSAGE(1300017);

	/**
	 * @param value0 你：do not have enough %0 necessary to use the skill。 / You do not have enough %0 necessary to use the skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_NOT_ENOUGH_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300018, value0);
	}

	/**
	 * 你：need to equip another weapon to use that skill。 / You need to equip another weapon to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_WEAPON = new SM_SYSTEM_MESSAGE(1300019);

	/**
	 * @param value0 你：have not learned the %0 skill。 / You have not learned the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_NOT_LEARNED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300020, value0);
	}

	/**
	 * 你处于尚未准备好使用该技能状态。 / You are not ready to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NOT_READY = new SM_SYSTEM_MESSAGE(1300021);

	/**
	 * 距离过远：the target to use that skill。 / You are too far from the target to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TOO_FAR = new SM_SYSTEM_MESSAGE(1300022);

	 /**
	  * skill was cancelled。
	  * The skill was cancelled
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANCELED = new SM_SYSTEM_MESSAGE(1300023);

	/**
	 * 你：have failed to use the skill because the target disappeared。 / You have failed to use the skill because the target disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TARGET_LOST = new SM_SYSTEM_MESSAGE(1300024);

	/**
	 * 你处于同时使用了过多技能状态。 / You are using too many skills simultaneously
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TOO_MANY_COOLING = new SM_SYSTEM_MESSAGE(1300025);

	/**
	 * @param value0 你：cannot do that while you are %0。 / You cannot do that while you are %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300026, value0);
	}

	/**
	 * 你：can use the skill only during combat。 / You can use the skill only during combat
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_NOT_IN_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1300027);

	/**
	 * That skill does not exist
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_NO_SUCH_SKILL = new SM_SYSTEM_MESSAGE(1300028);

	 /**
	  * skill has failed。
	  * The skill has failed
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_FAILED = new SM_SYSTEM_MESSAGE(1300029);

	/**
	 * 你：cannot use that because there is an obstacle in the way。 / You cannot use that because there is an obstacle in the way
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_OBSTACLE = new SM_SYSTEM_MESSAGE(1300030);

	/**
	 * 你：do not have a proper target for that skill。 / You do not have a proper target for that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_FIND_VALID_TARGET = new SM_SYSTEM_MESSAGE(1300031);

	 /**
	  * target is too far away。
	  * The target is too far away
	  */
	public static final SM_SYSTEM_MESSAGE STR_ATTACK_TOO_FAR_FROM_TARGET = new SM_SYSTEM_MESSAGE(1300032);

	/**
	 * You 无法 attack 作为 there 为 obstacle 在 way。 / You cannot attack as there is an obstacle in the way
	 */
	public static final SM_SYSTEM_MESSAGE STR_ATTACK_OBSTACLE_EXIST = new SM_SYSTEM_MESSAGE(1300033);

	/**
	 * 你：cannot attack。 / You cannot attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_ATTACK_CANT_FINT_VALID_TARGET = new SM_SYSTEM_MESSAGE(1300034);

	/**
	 * @param value0 你：acquired the %0 title as a quest reward。 / You acquired the %0 title as a quest reward
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_GET_REWARD_TITLE(int value0) {
		return new SM_SYSTEM_MESSAGE(1300035, new DescriptionId(value0));
	}

	 /**
	  * survey has arrived. Click the icon to open the survey window。
	  * A survey has arrived. Click the icon to open the survey window
	  */
	public static final SM_SYSTEM_MESSAGE STR_GMPOLL_GOT_POLL = new SM_SYSTEM_MESSAGE(1300036);

	/**
	 * 没有可参与的调查。 / There is no remaining survey to take part in
	 */
	public static final SM_SYSTEM_MESSAGE STR_GMPOLL_NO_POLL_REMAINED = new SM_SYSTEM_MESSAGE(1300037);

	/**
	 * @param value0 %0 正在逃跑。 / %0 is running away
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_COMBAT_NPC_FLEE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300038, value0);
	}

	/**
	 * @param nameId %0 放弃追击。 / %0 gives up the pursuit
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_COMBAT_NPC_RETURN(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300039, calculateNameId(nameId));
	}

	/**
	 * @param subzone 你：have discovered [%subzone]。 / You have discovered [%subzone]
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_DISCOVERY_NEWZONE(String subzone) {
		return new SM_SYSTEM_MESSAGE(1300040, subzone);
	}

	/**
	 * You 无法移除 equipped 物品 becauseinventory 为 full。 / You cannot remove the equipped item because the inventory is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_UI_INVENTORY_FULL = new SM_SYSTEM_MESSAGE(1300042);

	/**
	 * 你：left the group。 / You left the group
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_SECEDE = new SM_SYSTEM_MESSAGE(1300043);

	/**
	 * 无法邀请已死亡的玩家加入小队。 / A dead person cannot be invited to a group
	 */
	public static final SM_SYSTEM_MESSAGE STR_UI_PARTY_DEAD = new SM_SYSTEM_MESSAGE(1300044);

	/**
	 * You 无法检查 information 在 characters 的 anotherrace。 / You cannot check the information on characters of another race
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASK_PCINFO_OTHER_RACE = new SM_SYSTEM_MESSAGE(1300045);

	/**
	 * 该玩家未登录。 / That person is not logged on
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASK_PCINFO_LOGOFF = new SM_SYSTEM_MESSAGE(1300046);

	/**
	 * @param value0 你：cannot leave the group in %0。 / You cannot leave the group in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_LEAVE_PARTY_DURING_PATH_FLYING(String value0) {
		return new SM_SYSTEM_MESSAGE(1300047, value0);
	}

	/**
	 * @param descriptionId You 无法 use 物品 while%0。 / You cannot use an item while %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_USE_ITEM_DURING_PATH_FLYING(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300048, descriptionId);
	}

	/**
	 * @param value0 %0 放弃攻击。 / %0 gives up the attack
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_COMBAT_NPC_RETURN_NOMOVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300049, value0);
	}

	/**
	 * 你：learned %0 (Level %1)。 / You learned %0 (Level %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_LEARNED_NEW_SKILL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300050, value0, value1);
	}

	/**
	 * @param value0 你：stopped using %0。 / You stopped using %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_TOGGLE_SKILL_TURNED_OFF(String value0) {
		return new SM_SYSTEM_MESSAGE(1300051, value0);
	}

	/**
	 * That skill is not being used
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TOGGLE_SKILL_ALREADY_TURNED_OFF = new SM_SYSTEM_MESSAGE(1300052);

	/**
	 * @param value0 你：stopped using %0 skill。 / You stopped using %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_MAINTAIN_SKILL_TURNED_OFF(String value0) {
		return new SM_SYSTEM_MESSAGE(1300053, value0);
	}

	/**
	 * That skill is not being used
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_MAINTAIN_SKILL_ALREADY_TURNED_OFF = new SM_SYSTEM_MESSAGE(1300054);

	 /**
	  * %0 skill effect has been removed。
	  * The %0 skill effect has been removed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSTATUS_SKILL_TURNED_OFF(String value0) {
		return new SM_SYSTEM_MESSAGE(1300055, value0);
	}

	 /**
	  * %0 skill effect cannot be removed。
	  * The %0 skill effect cannot be removed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSTATUS_SKILL_CAN_NOT_BE_TURNED_OFF_BY_TARGET(String value0) {
		return new SM_SYSTEM_MESSAGE(1300056, value0);
	}

	 /**
	  * %0 skill effect cannot be removed。
	  * The %0 skill effect cannot be removed
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABSTATUS_SKILL_CAN_NOT_BE_TURNED_OFF_BY_CASTOR(String value0) {
		return new SM_SYSTEM_MESSAGE(1300057, value0);
	}

	/**
	 * @param value0 你：cured the altered state caused by %0 skill。 / You cured the altered state caused by %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_TURN_OFF_ABNORMAL_STATUS(String value0) {
		return new SM_SYSTEM_MESSAGE(1300058, value0);
	}

	/**
	 * %0skill 曾 cancelled 作为 %1 为已经 undermorepowerfulskill。 / The %0 skill was cancelled as %1 is already under a more powerful skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CONFLICT_WITH_OTHER_SKILL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300059, value0, value1);
	}

	/**
	 * 你：have not learned the skill to equip this weapon。 / You have not learned the skill to equip this weapon
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_WEAPON_MASTERY_SKILL = new SM_SYSTEM_MESSAGE(1300060);

	/**
	 * 你：have not learned the skill to equip this armor。 / You have not learned the skill to equip this armor
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_ARMOR_MASTERY_SKILL = new SM_SYSTEM_MESSAGE(1300061);

	/**
	 * 你：cannot cast spells while silenced。 / You cannot cast spells while silenced
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_MAGIC_SKILL_WHILE_SILENCED = new SM_SYSTEM_MESSAGE(
			1300062);

	/**
	 * You 无法 usephysicalskillswhile 在 state 的 fear 或 restraint。 / You cannot use physical skills while in a state of fear or restraint
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_PHYSICAL_SKILL_IN_FEAR = new SM_SYSTEM_MESSAGE(1300063);

	/**
	 * 你：cannot use the skill while in an Altered State。 / You cannot use the skill while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(1300064);

	/**
	 * 变身/异常状态下行动受限。 / Your actions are limited while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_ACT_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1300065);

	/**
	 * 你：cannot attack while in an Altered State。 / You cannot attack while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_ATTACK_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1300066);

	/**
	 * 你：cannot gather while in an Altered State。 / You cannot gather while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_GATHER_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1300067);

	/**
	 * You 无法 use 物品 while 在 AlteredState。 / You cannot use the item while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_ITEM_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1300068);

	/**
	 * You 无法 equip 物品 while 在 AlteredState。 / You cannot equip the item while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_EQUIP_ITEM_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1300069);

	/**
	 * @param value0 %0skillfailed 作为 there 为已经 toomanyskills 在 effect。 / The %0 skill failed as there are already too many skills in effect
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_SLOT_FULL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300070, value0);
	}

	/**
	 * @param value0 You 无法 use%0skill 在 your 当前 stance。 / You cannot use the %0 skill in your current stance
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_THIS_SKILL_IN_CURRENT_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300071, value0);
	}

	/**
	 * 你：already have a spirit following you。 / You already have a spirit following you
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER = new SM_SYSTEM_MESSAGE(1300072);

	/**
	 * 作为 spirit 为 toofar , yoursummon 有已 forciblycanceled。 / As the spirit is too far, your summon has been forcibly canceled
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_UNSUMMON_BY_TOO_DISTANCE = new SM_SYSTEM_MESSAGE(1300073);

	/**
	 * 你处于距离精灵过远，无法下达命令状态。 / You are too far from the spirit is to issue an order
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_CANT_ORDER_BY_TOO_DISTANCE = new SM_SYSTEM_MESSAGE(1300074);

	/**
	 * 你：have not learned the Advanced Dual-Wielding skill。 / You have not learned the Advanced Dual-Wielding skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_WEAPON_DUEL_SKILL = new SM_SYSTEM_MESSAGE(1300075);

	/**
	 * 目标无法为 charmed。 / The target cannot be charmed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_ENSLAVE_TARGET_CANT_BE_ENSLAVED = new SM_SYSTEM_MESSAGE(1300076);

	/**
	 * 你：have failed to charm the target。 / You have failed to charm the target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_ENSLAVE_FAILED_TO_ENSLAVE = new SM_SYSTEM_MESSAGE(1300077);

	/**
	 * 你：have charmed the target。 / You have charmed the target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_ENSLAVE_SUCCEDED_TO_ENSLAVE = new SM_SYSTEM_MESSAGE(1300078);

	/**
	 * 你：have no dead pets。 / You have no dead pets
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_RESURRECT_PET_DONT_HAVE_DEAD_STONE = new SM_SYSTEM_MESSAGE(
			1300079);

	/**
	 * Youdonot 有足够基纳到 resurrectCharmStone。 / You do not have enough Kinah to resurrect the Charm Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_RESURRECT_PET_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300080);

	/**
	 * 你距离 NPC 过远，无法复活。 / You are too far from the NPC to resurrect it
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_RESURRECT_PET_TOO_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300081);

	/**
	 * 你：have resurrected the pet。 / You have resurrected the pet
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_RESURRECT_PET_SUCCEEDED = new SM_SYSTEM_MESSAGE(1300082);

	/**
	 * Please try again after you have closed other dialog boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_RESURRECT_PET_RETRY_WHEN_CLOSE_OTHER_QUESTION_WND = new SM_SYSTEM_MESSAGE(
			1300083);

	/**
	 * 你：cannot transfer XP。 / You cannot transfer XP
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_GIVE_EXP_TO_PET_DONT_HAVE_PET = new SM_SYSTEM_MESSAGE(1300084);

	/**
	 * amount 的 XPyou 有 transferred 到 spirit 为 not 足够。 / The amount of XP you have transferred to the spirit is not enough
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENSLAVE_GIVE_EXP_TO_PET_NOT_ENOUGH_EXP = new SM_SYSTEM_MESSAGE(1300085);

	 /**
	  * %0 has reached level %1。
	  * %0 has reached level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_ENSLAVE_PET_LEVEL_CHANGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300086, value0, value1);
	}

	/**
	 * There is no target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_NO_TARGET = new SM_SYSTEM_MESSAGE(1300087);

	/**
	 * Invalid target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_SUMMON_IS_NOT_VALID = new SM_SYSTEM_MESSAGE(1300088);

	/**
	 * 你：cannot use this on enemies。 / You cannot use this on enemies
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_NOTENEMY_ONLY = new SM_SYSTEM_MESSAGE(1300089);

	/**
	 * 你：can only use this on enemies。 / You can only use this on enemies
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_INVALID_TARGET_ENEMY_ONLY = new SM_SYSTEM_MESSAGE(1300090);

	/**
	 * @param value0 你：cannot duel with %0。 / You cannot duel with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_PARTNER_INVALID(String value0) {
		return new SM_SYSTEM_MESSAGE(1300091, value0);
	}

	/**
	 * @param value0 %0 已在与他人决斗。 / %0 is already fighting a duel with another opponent
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_PARTNER_IN_DUEL_ALREADY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300092, value0);
	}

	/**
	 * 你已在与其他对手决斗。 / You are already fighting a duel with another opponent
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_YOU_ARE_IN_DUEL_ALREADY = new SM_SYSTEM_MESSAGE(1300093);

	/**
	 * @param value0 你：challenged %0 to a duel。 / You challenged %0 to a duel
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_REQUEST_TO_PARTNER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300094, value0);
	}

	/**
	 * @param value0 %0 正在响应其他请求，无法回复。 / %0 is answering another request and cannot respond
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_CANT_REQUEST_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300095, value0);
	}

	/**
	 * 没有可决斗的对象。 / There is no one for you to challenge to a duel
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_NO_USER_TO_REQUEST = new SM_SYSTEM_MESSAGE(1300096);

	/**
	 * @param value0 %0 拒绝了你的决斗挑战。 / %0 declined your challenge
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_HE_REJECT_DUEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300097, value0);
	}

	/**
	 * @param value0 你：won the duel against %0。 / You won the duel against %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_YOU_WIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300098, value0);
	}

	/**
	 * @param value0 你：lost the duel against %0。 / You lost the duel against %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_YOU_LOSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300099, value0);
	}

	/**
	 * @param value0 与 %0 的决斗因超时结束。 / The duel with %0 ended due to the time limit
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300100, value0);
	}

	/**
	 * @param value0 你距离 %0 过远，无法开始决斗。 / You are too far from %0 to start a duel
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_PARTNER_TOO_FAR_FOR_START(String value0) {
		return new SM_SYSTEM_MESSAGE(1300101, value0);
	}

	/**
	 * 你：cannot find the user you have challenged to a duel。 / You cannot find the user you have challenged to a duel
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_LOST_REQUEST_DUEL_PARTNER = new SM_SYSTEM_MESSAGE(1300102);

	/**
	 * 你尚未准备好开始决斗。 / You are not ready to start a duel
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_NOT_READY_TO_START_DUEL = new SM_SYSTEM_MESSAGE(1300103);

	/**
	 * @param value0 与 %0 的决斗因超时结束。 / The duel with %0 ended due to the time limit
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_TIMEOUT_WITHOUT_PARTNER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300104, value0);
	}

	/**
	 * 你：cannot use that on your target。 / You cannot use that on your target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_TO_CURRENT_TARGET = new SM_SYSTEM_MESSAGE(1300105);

	/**
	 * You 可 use 其仅当 you 为在 Counterattackmode。 / You can use it only when you are in Counterattack mode
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_IN_NONE_COUNTER_STATUS = new SM_SYSTEM_MESSAGE(1300106);

	/**
	 * 此 skill 可仅为 used 作为 part 的 ChainSkill。 / This skill can only be used as part of a Chain Skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_IN_NONE_CHAINSKILL_STATUS = new SM_SYSTEM_MESSAGE(
			1300107);

	/**
	 * You 可仅 cast 在 group 成员谁为 usingSpecialAttack。 / You can only cast that on a group member who is using a Special Attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_CAST_ONLY_TO_MY_PARTY_CASTING_ULTRASKILL = new SM_SYSTEM_MESSAGE(
			1300108);

	/**
	 * 你：interrupted the target's skill。 / You interrupted the target's skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TARGET_SKILL_CANCELED = new SM_SYSTEM_MESSAGE(1300109);

	/**
	 * 你：must be equipped with a shield to use this skill。 / You must be equipped with a shield to use this skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NEED_SHIELD = new SM_SYSTEM_MESSAGE(1300110);

	/**
	 * 你：must be equipped with an Off-hand Weapon to use the skill。 / You must be equipped with an Off-hand Weapon to use the skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NEED_DUAL_WEAPON = new SM_SYSTEM_MESSAGE(1300111);

	/**
	 * 此 skill 可仅为 used 从 rear 的 youropponent。 / This skill can only be used from the rear of your opponent
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_USE_TO_TARGETS_BACK_ONLY = new SM_SYSTEM_MESSAGE(1300112);

	/**
	 * 你：can use this skill only while flying。 / You can use this skill only while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_RESTRICTION_FLY_ONLY = new SM_SYSTEM_MESSAGE(1300113);

	/**
	 * 你：cannot use a skill while you are flying。 / You cannot use a skill while you are flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_RESTRICTION_NO_FLY = new SM_SYSTEM_MESSAGE(1300114);

	/**
	 * attacker 或目标为在 differentarea。 / The attacker or the target is in a different area
	 */
	public static final SM_SYSTEM_MESSAGE STR_ATTACK_INVALID_POSITION = new SM_SYSTEM_MESSAGE(1300115);

	/**
	 * Invalid target
	 */
	public static final SM_SYSTEM_MESSAGE STR_ATTACK_IMPROPER_TARGET = new SM_SYSTEM_MESSAGE(1300116);

	/**
	 * 你：cannot equip the shield as you have not learned the Equip Shield skill。 / You cannot equip the shield as you have not learned the Equip Shield skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_NO_SHIELD_MASTERY_SKILL = new SM_SYSTEM_MESSAGE(1300117);

	/**
	 * You 无法 attack 在 your 当前 stance。 / You cannot attack in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_ATTACK_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300118);

	/**
	 * You 无法 gather 在 your 当前 stance。 / You cannot gather in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_GATHER_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300119);

	/**
	 * You 无法 use 物品在 your 当前 stance。 / You cannot use that item in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_ITEM_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300120);

	/**
	 * You 无法 changeyourequipment 在 your 当前 stance。 / You cannot change your equipment in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_EQUIP_ITEM_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300121);

	/**
	 * You 无法 craft 在 your 当前 stance。 / You cannot craft in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_COMBINE_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300122);

	/**
	 * You 无法 useskill 在 your 当前 stance。 / You cannot use that skill in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_CAST_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(1300123);

	/**
	 * You 无法 changemode 在 your 当前 stance。 / You cannot change mode in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_CHANGE_MODE__WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300124);

	/**
	 * 你距离目标过近，无法使用该技能。 / You are too close to the target to use that skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_TOO_CLOSE = new SM_SYSTEM_MESSAGE(1300125);

	/**
	 * 你：cannot use the magic passage while flying。 / You cannot use the magic passage while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_GROUPGATE_WHEN_FLYING = new SM_SYSTEM_MESSAGE(1300126);

	/**
	 * 你：can only use that when you have a spirit。 / You can only use that when you have a spirit
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_USE_THIS_SKILL_WITHOUT_A_PET = new SM_SYSTEM_MESSAGE(1300127);

	/**
	 * You 可仅 use 其当 yourspirit 为在 attackmode。 / You can only use it when your spirit is in attack mode
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_USE_THIS_SKILL_WHEN_PET_IS_NOT_ATTACK_MODE = new SM_SYSTEM_MESSAGE(
			1300128);

	/**
	 * 你：use the skill on yourself instead of the currently selected target。 / You use the skill on yourself instead of the currently selected target
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_AUTO_CHANGE_TARGET_TO_MY = new SM_SYSTEM_MESSAGE(1300129);

	 /**
	  * 你的精灵没有技能。
	  * 你的精灵 没有技能。 / Your spirit has no skill
	  */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_USE_THIS_SKILL_TO_A_PET_THAT_HAS_NO_ULTRASKILL = new SM_SYSTEM_MESSAGE(
			1300130);

	/**
	 * 你：cannot use this skill during combat。 / You cannot use this skill during combat
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_IN_COMBAT_STATE = new SM_SYSTEM_MESSAGE(1300131);

	/**
	 * @param value0 %0 正在逃跑。 / %0 is running away
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_COMBAT_NPC_FLEE_ORG(String value0) {
		return new SM_SYSTEM_MESSAGE(1300132, value0);
	}

	/**
	 * @param value0 %0 放弃追击。 / %0 gives up the pursuit
	 */
	public static SM_SYSTEM_MESSAGE STR_UI_COMBAT_NPC_RETURN_ORG(String value0) {
		return new SM_SYSTEM_MESSAGE(1300133, value0);
	}

	 /**
	  * %0 has withdrawn the challenge for a duel。
	  * %0 has withdrawn the challenge for a duel
	  */
	public static SM_SYSTEM_MESSAGE STR_DUEL_REQUESTER_WITHDRAW_REQUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300134, value0);
	}

	/**
	 * @param value0 你：have withdrawn the challenge to %0 for a duel。 / You have withdrawn the challenge to %0 for a duel
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_WITHDRAW_REQUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300135, value0);
	}

	 /**
	  * duel between %0 and %1 has started。
	  * A duel between %0 and %1 has started
	  */
	public static SM_SYSTEM_MESSAGE STR_DUEL_START_BROADCAST(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300136, value0, value1);
	}

	/**
	 * %0 在决斗中击败了 %1。 / %0 defeated %1 in a duel
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_STOP_BROADCAST(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300137, value0, value1);
	}

	/**
	 * 与 %0 和 %1 的决斗以平局结束。 / The duel between %0 and %1 was a draw
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_TIMEOUT_BROADCAST(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300138, value0, value1);
	}

	/**
	 * @param value0 决斗将在 %0 秒后结束。 / The duel ends in %0 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_TIMEOUT_NOTIFY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300139, value0);
	}

	/**
	 * @param value0 You 无法 requestduel 到 %0 作为玩家为 currentlybusy。 / You cannot request a duel to %0 as the player is currently busy
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_START_OTHER_IS_BUSY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300140, value0);
	}

	/**
	 * 你：cannot learn this skill。 / You cannot learn this skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILLLEARNBOOK_CANT_USE_NO_SKILL = new SM_SYSTEM_MESSAGE(1300141);

	/**
	 * 你：have already learned this skill。 / You have already learned this skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILLLEARNBOOK_CANT_USE_ALREADY_HAS_SKILL = new SM_SYSTEM_MESSAGE(
			1300142);

	/**
	 * You 无法 use 物品 here。 / You cannot use that item here
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_ITEM_IN_CURRENT_POSITION = new SM_SYSTEM_MESSAGE(
			1300143);

	/**
	 * 你：cannot craft here。 / You cannot craft here
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_CAN_NOT_COMBINE_IN_CURRENT_POSITION = new SM_SYSTEM_MESSAGE(
			1300144);

	/**
	 * 你：cannot gather here。 / You cannot gather here
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_GATHER_IN_CURRENT_POSTION = new SM_SYSTEM_MESSAGE(1300145);

	/**
	 * 你：cannot use the skill here。 / You cannot use the skill here
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CANT_CAST_IN_CURRENT_POSTION = new SM_SYSTEM_MESSAGE(1300146);

	/**
	 * You 无法 takeoff 在 your 当前 stance。 / You cannot take off in your current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_TAKE_OFF__WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1300147);

	 /**
	  * %0 has logged out。
	  * %0 has logged out
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ASK_OTHER_HAS_LOGOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300148, value0);
	}

	/**
	 * 你：cannot use this skill while transformed。 / You cannot use this skill while transformed
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_CAST_IN_SHAPECHANGE = new SM_SYSTEM_MESSAGE(1300149);

	/**
	 * 你：have no right to use the selected Magic Passage。 / You have no right to use the selected Magic Passage
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_GROUPGATE_NO_RIGHT = new SM_SYSTEM_MESSAGE(1300150);

	/**
	 * 此 skill 可仅为 used 在 Abyss。 / This skill can only be used in the Abyss
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_CAST_IN_NOT_ABYSS_WORLD = new SM_SYSTEM_MESSAGE(1300151);

	/**
	 * 你：cannot invite any more group members。 / You cannot invite any more group members
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_ADD_NEW_MEMBER = new SM_SYSTEM_MESSAGE(1300152);

	/**
	 * 仅小队长可将权限转让他人。 / Only the group leader can transfer authority to another person
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ONLY_LEADER_CAN_CHANGE_LEADER = new SM_SYSTEM_MESSAGE(1300153);

	 /**
	  * %0 has become the new group leader。
	  * %0 has become the new group leader
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_IS_NEW_LEADER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300154, value0);
	}

	/**
	 * 你：have become the new group leader。 / You have become the new group leader
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_YOU_BECOME_NEW_LEADER = new SM_SYSTEM_MESSAGE(1300155);

	/**
	 * Only the group leader can change the item distribution method
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ONLY_LEADER_CAN_CHANGE_LOOTING = new SM_SYSTEM_MESSAGE(1300156);

	/**
	 * 物品 distributionmethod 的 group 有已 changed 到 Manual。 / The item distribution method of the group has been changed to Manual
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_LOOTING_CHANGED_TO_MANUAL = new SM_SYSTEM_MESSAGE(1300157);

	/**
	 * 物品 distributionmethod 的 group 有已 changed 到 Auto。 / The item distribution method of the group has been changed to Auto
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_LOOTING_CHANGED_TO_AUTO = new SM_SYSTEM_MESSAGE(1300158);

	/**
	 * 你邀请入队的玩家当前离线。 / The user you invited to the group is currently offline
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_NO_USER_TO_INVITE = new SM_SYSTEM_MESSAGE(1300159);

	/**
	 * Only group leader can invite
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ONLY_LEADER_CAN_INVITE = new SM_SYSTEM_MESSAGE(1300160);

	 /**
	  * %0 has declined your invitation。
	  * %0 has declined your invitation
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_REJECT_INVITATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300161, value0);
	}

	/**
	 * 你：cannot invite yourself to a group。 / You cannot invite yourself to a group
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CAN_NOT_INVITE_SELF = new SM_SYSTEM_MESSAGE(1300162);

	/**
	 * 你：cannot issue an invitation while you are dead。 / You cannot issue an invitation while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_INVITE_WHEN_DEAD = new SM_SYSTEM_MESSAGE(1300163);

	/**
	 * selectedgroup 成员为 currently 离线。 / The selected group member is currently offline
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_OFFLINE_MEMBER = new SM_SYSTEM_MESSAGE(1300164);

	/**
	 * Only the group leader can kick a member out
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ONLY_LEADER_CAN_BANISH = new SM_SYSTEM_MESSAGE(1300165);

	/**
	 * You 有已踢出 out 的 group。 / You have been kicked out of the group
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_YOU_ARE_BANISHED = new SM_SYSTEM_MESSAGE(1300166);

	 /**
	  * group has been disbanded。
	  * The group has been disbanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_IS_DISPERSED = new SM_SYSTEM_MESSAGE(1300167);

	 /**
	  * %0 has left your group。
	  * %0 has left your group
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_LEAVE_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300168, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 anothergroup。 / %0 is already a member of another group
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_IS_ALREADY_MEMBER_OF_OTHER_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300169, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 yourgroup。 / %0 is already a member of your group
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_IS_ALREADY_MEMBER_OF_OUR_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300170, value0);
	}

	/**
	 * 你不在任何小队中。 / You are not in any group
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_YOU_ARE_NOT_PARTY_MEMBER = new SM_SYSTEM_MESSAGE(1300171);

	/**
	 * 你不是小队成员。 / You are not a group member
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_NOT_PARTY_MEMBER = new SM_SYSTEM_MESSAGE(1300172);

	/**
	 * @param value0 你：have invited %0 to join your group。 / You have invited %0 to join your group
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_INVITED_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300173, value0);
	}

	/**
	 * @param value0 当前 %0 无法接受你的组队邀请。 / Currently, %0 cannot accept your group invitation
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300174, value0);
	}

	 /**
	  * %0 has been disconnected。
	  * %0 has been disconnected
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_BECOME_OFFLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300175, value0);
	}

	/**
	 * @param value0 %0 离线过久，已自动移出小队。 / %0 has been offline for too long and is automatically excluded from the group
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_BECOME_OFFLINE_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300176, value0);
	}

	/**
	 * @param value0 %0 有已踢出 out 的 yourgroup。 / %0 has been kicked out of your group
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_IS_BANISHED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300177, value0);
	}

	/**
	 * 小队稀有物品分配方式已改为自由拾取。 / The rare item distribution method of the group has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_RARE_LOOTING_CHANGED_TO_MANUAL = new SM_SYSTEM_MESSAGE(1300178);

	/**
	 * rare 物品 distributionmethod 的 group 有已 changed 到 Auto。 / The rare item distribution method of the group has been changed to Auto
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_RARE_LOOTING_CHANGED_TO_AUTO = new SM_SYSTEM_MESSAGE(1300179);

	/**
	 * rare 物品 distributionmethod 的 group 有已 changed 到 DiceRoll。 / The rare item distribution method of the group has been changed to Dice Roll
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_RARE_LOOTING_CHANGED_TO_DICE = new SM_SYSTEM_MESSAGE(1300180);

	/**
	 * 战利品分配完成前无法踢出队员。 / A group member cannot be kicked out before the completion of loot distribution
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANNOT_BANISH_ITEMPOOL_NOT_EMPTY = new SM_SYSTEM_MESSAGE(1300181);

	/**
	 * %0 掷骰得到 %num1。 / %0 rolled the dice and got a %num1
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ITEM_DICE(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1300182, value0, num1);
	}

	/**
	 * You 可 rolldiceoncemore 若 rolled 编号为 lessthan100。 / You can roll the dice once more if the rolled number is less than 100
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ITEM_DICE_AGAIN = new SM_SYSTEM_MESSAGE(1300183);

	/**
	 * 物品 distributionmethod 的 group 有已 changed 到 Free -用于-全部。 / The item distribution method of the group has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_LOOTING_CHANGED_TO_FREEFORALL = new SM_SYSTEM_MESSAGE(1300184);

	/**
	 * 物品 distributionmethod 的 group 有已 changed 到 Round - robin。 / The item distribution method of the group has been changed to Round-robin
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_LOOTING_CHANGED_TO_ROUNDROBIN = new SM_SYSTEM_MESSAGE(1300185);

	/**
	 * 物品 distributionmethod 的 group 有已 changed 到 GroupLeader。 / The item distribution method of the group has been changed to Group Leader
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_LOOTING_CHANGED_TO_LEADERONLY = new SM_SYSTEM_MESSAGE(1300186);

	 /**
	  * %0 rolled the dice and got a %num1 (max. %num2)。
	  * %0 rolled the dice and got a %num1 (max. %num2)
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ITEM_DICE_CUSTOM(String value0, int num1, int num2) {
		return new SM_SYSTEM_MESSAGE(1300187, value0, num1, num2);
	}

	/**
	 * You 无法 invite 成员的 otherrace。 / You cannot invite members of other race
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_INVITE_OTHER_RACE = new SM_SYSTEM_MESSAGE(1300188);

	/**
	 * @param value0 你：have invited %0's group to the alliance。 / You have invited %0's group to the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_INVITED_HIS_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300189, value0);
	}

	 /**
	  * %0 has declined your invitation to join the alliance。
	  * %0 has declined your invitation to join the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_REJECT_INVITATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300190, value0);
	}

	/**
	 * @param value0 目前 %0 无法接受你的联盟邀请。 / Currently, %0 cannot accept your invitation to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300191, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 anotheralliance。 / %0 is already a member of another alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_IS_ALREADY_MEMBER_OF_OTHER_ALLIANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300192, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 youralliance。 / %0 is already a member of your alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_IS_ALREADY_MEMBER_OF_OUR_ALLIANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300193, value0);
	}

	/**
	 * @param value0 You 无法 invite%0 到 alliance 作为 he 或 she 为 notgroupleader。 / You cannot invite %0 to the alliance as he or she is not a group leader
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CAN_NOT_INVITE_HIM_HE_IS_NOT_PARTY_LEADER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300194, value0);
	}

	/**
	 * @param value0 你：cannot invite %0 to the alliance。 / You cannot invite %0 to the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CAN_NOT_INVITE_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300195, value0);
	}

	/**
	 * You 无法 inviteanymore 作为 alliance 为 full。 / You cannot invite any more as the alliance is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CANT_ADD_NEW_MEMBER = new SM_SYSTEM_MESSAGE(1300196);

	/**
	 * Only the group leader can leave the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_ONLY_PARTY_LEADER_CAN_LEAVE_ALLIANCE = new SM_SYSTEM_MESSAGE(
			1300197);

	/**
	 * 你的小队不属于联盟。 / Your group is not part of an alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_YOUR_PARTY_IS_NOT_ALLIANCE_MEMBER = new SM_SYSTEM_MESSAGE(
			1300198);

	/**
	 * %0's group has left the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HIS_PARTY_LEAVE_ALLIANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300199, value0);
	}

	 /**
	  * 你的小队已离开联盟。
	  * 你的小队 has left the 联盟。 / Your group has left the alliance
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_MY_PARTY_LEAVE_ALLIANCE = new SM_SYSTEM_MESSAGE(1300200);

	 /**
	  * alliance has been disbanded。
	  * The alliance has been disbanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_DISPERSED = new SM_SYSTEM_MESSAGE(1300201);

	 /**
	  * %0 has left the alliance。
	  * %0 has left the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_LEAVED_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300202, value0);
	}

	/**
	 * @param value0 %0 离线过久，已自动移出小队与联盟。 / %0 has been offline for too long and has been automatically kicked out of the group and the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_LEAVED_PARTY_OFFLINE_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300203, value0);
	}

	/**
	 * @param value0 %0 有已踢出 out 的 group 并 thusalliance。 / %0 has been kicked out of the group and thus the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_IS_BANISHED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300204, value0);
	}

	 /**
	  * %0 has become the new group leader。
	  * %0 has become the new group leader
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_BECOME_PARTY_LEADER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300205, value0);
	}

	/**
	 * 联盟物品分配方式已改为自由拾取。 / The item distribution method of the alliance has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_LOOTING_CHANGED_TO_FREE = new SM_SYSTEM_MESSAGE(1300206);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Auto。 / The item distribution method of the alliance has been changed to Auto
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_LOOTING_CHANGED_TO_RANDOM = new SM_SYSTEM_MESSAGE(1300207);

	/**
	 * @param value0 %0 有已经 requested 物品 distributionmethod 到为 changed。 / %0 has already requested the item distribution method to be changed
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_PROCESSING_HIS_REQUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300208, value0);
	}

	 /**
	  * 你更改物品分配方式的请求正在处理中。
	  * Your request to change the item distribution method is being processed
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_PROCESSING_YOUR_REQUEST = new SM_SYSTEM_MESSAGE(
			1300209);

	 /**
	  * %0 denied %1's request to change the item distribution method。
	  * %0 denied %1's request to change the item distribution method
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_HE_DENIED_HIS_ASK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300210, value0, value1);
	}

	/**
	 * @param value0 %0 ' srequest 到 change 物品 distributionmethod 有已 denied。 / %0's request to change the item distribution method has been denied
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_HE_DENIED_MY_ASK(String value0) {
		return new SM_SYSTEM_MESSAGE(1300211, value0);
	}

	/**
	 * 你：asked the alliance Captain to change the item distribution method。 / You asked the alliance Captain to change the item distribution method
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_ASK_SUBMITTED = new SM_SYSTEM_MESSAGE(1300212);

	/**
	 * %0's request to change the item distribution method timed out
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_CHANGE_LOOT_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300213, value0);
	}

	/**
	 * @param value0 你：asked the alliance Captain for permission to pick up %0。 / You asked the alliance Captain for permission to pick up %0
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_ASK_SUBMITTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300214, value0);
	}

	/**
	 * @param value0 你拾取 %0 的权限请求正在处理。 / Your request for permission to pick up %0 is being processed
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_PROCESSING_YOUR_REQUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300215, value0);
	}

	/**
	 * %0 拾取 %1 的权限请求已批准。 / %0's request for permission to pick up %1 was approved
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_ALL_ACCEPT_HIM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300216, value0, value1);
	}

	/**
	 * %0 拒绝了 %1 拾取 %2 的权限请求。 / %0 denied %1's request for permission to pick up %2
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_HE_DENIED(String value0, String value1,
			String value2) {
		return new SM_SYSTEM_MESSAGE(1300217, value0, value1, value2);
	}

	/**
	 * %0 拒绝了你拾取 %1 的权限请求。 / %0 denied your request for permission to pick up %1
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_HE_DENIED_MY_ASK(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300218, value0, value1);
	}

	/**
	 * %0 拾取 %1 的权限请求已超时。 / %0's request for permission to pick up %1 timed out
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_PICKUP_ITEM_TIMEOUT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300219, value0, value1);
	}

	/**
	 * 请求无法处理，待处理请求过多。 / The request cannot be processed, as there are already too many requests pending approval by the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_TOO_MANY_VOTE = new SM_SYSTEM_MESSAGE(1300220);

	/**
	 * 你不是联盟成员。 / You are not a member of an alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_YOU_ARE_NOT_PARTY_ALLIANCE_MEMBER = new SM_SYSTEM_MESSAGE(1300221);

	 /**
	  * %0 has been disconnected。
	  * %0 has been disconnected
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_BECOME_OFFLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300222, value0);
	}

	/**
	 * Only the alliance captain can change the item distribution method
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_ONLY_LEADER_CAN_CHANGE_LOOTING = new SM_SYSTEM_MESSAGE(
			1300223);

	/**
	 * 联盟物品分配方式已改为自由拾取。 / The item distribution method of the alliance has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_LOOTING_CHANGED_TO_FREEFORALL = new SM_SYSTEM_MESSAGE(
			1300224);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Round - robin。 / The item distribution method of the alliance has been changed to Round-robin
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_LOOTING_CHANGED_TO_ROUNDROBIN = new SM_SYSTEM_MESSAGE(
			1300225);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Captain。 / The item distribution method of the alliance has been changed to Captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_LOOTING_CHANGED_TO_LEADERONLY = new SM_SYSTEM_MESSAGE(
			1300226);

	/**
	 * You 无法 inviteselected 玩家作为 he 或 she 为 toobusy。 / You cannot invite the selected player as he or she is too busy
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_INVITE_OTHER_IS_BUSY = new SM_SYSTEM_MESSAGE(1300227);

	/**
	 * That name is invalid. Please try another
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_INVALID_GUILD_NAME = new SM_SYSTEM_MESSAGE(1300228);

	/**
	 * 你距离 NPC 过远，无法创建军团。 / You are too far from the NPC to create a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_TOO_FAR_FROM_CREATOR_NPC = new SM_SYSTEM_MESSAGE(1300229);

	/**
	 * Please try again after you have closed other dialog boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_RETRY_WHEN_CLOSE_OTHER_QUESTION_WND = new SM_SYSTEM_MESSAGE(
			1300230);

	/**
	 * Youdonothaveenough 基纳创建军团 / You do not have enough Kinah to create a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300231);

	/**
	 * You 无法创建军团作为 you 为已经成员的 another 军团。 / You cannot create a Legion as you are already a member of another Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_ALREADY_BELONGS_TO_GUILD = new SM_SYSTEM_MESSAGE(1300232);

	/**
	 * That name is invalid. Please try another
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_SAME_GUILD_EXIST = new SM_SYSTEM_MESSAGE(1300233);

	/**
	 * You 无法创建新军团作为 graceperiodbetweencreatingLegions 有 notexpired。 / You cannot create a new Legion as the grace period between creating Legions has not expired
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_LAST_DAY_CHECK = new SM_SYSTEM_MESSAGE(1300234);

	 /**
	  * %0 Legion has been created。
	  * The %0 Legion has been created
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CREATED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300235, value0);
	}

	/**
	 * 你：cannot leave your Legion during a war。 / You cannot leave your Legion during a war
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_CANT_LEAVE_WHILE_WAR = new SM_SYSTEM_MESSAGE(1300236);

	/**
	 * You 无法 leaveyour 军团 whileusing 军团 Warehouse。 / You cannot leave your Legion while using the Legion Warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_CANT_LEAVE_GUILD_WHILE_USING_WAREHOUSE = new SM_SYSTEM_MESSAGE(
			1300237);

	/**
	 * You 无法 leaveyour 军团 unlessyoutransfer 军团长 authority 到 someone 否则。 / You cannot leave your Legion unless you transfer Brigade General authority to someone else
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_MASTER_CANT_LEAVE_BEFORE_CHANGE_MASTER = new SM_SYSTEM_MESSAGE(
			1300238);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300239);

	/**
	 * @param value0 %0 有 left 军团。 / %0 has left the Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_HE_LEFT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300240, value0);
	}

	/**
	 * @param value0 你：have left the %0 Legion。 / You have left the %0 Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_DONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300241, value0);
	}

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300242);

	/**
	 * You 无法 kickyourselfout 从军团。 / You cannot kick yourself out from a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_CANT_BANISH_SELF = new SM_SYSTEM_MESSAGE(1300243);

	/**
	 * Youdonot 有 authority 到 kickout 军团成员。 / You do not have the authority to kick out a Legion member
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_DONT_HAVE_RIGHT_TO_BANISH = new SM_SYSTEM_MESSAGE(1300244);

	/**
	 * You 无法 kick 军团成员 outduringwar。 / You cannot kick a Legion member out during a war
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_CANT_BAN_MEMBER_WHILE_WAR = new SM_SYSTEM_MESSAGE(1300245);

	/**
	 * @param value0 你：have been kicked out of the %0 Legion。 / You have been kicked out of the %0 Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_BANISHIED_FROM_GUILD_BY_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300246, value0);
	}

	 /**
	  * %0 kicked %1 out of the Legion。
	  * %0 kicked %1 out of the Legion
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_BANSIH_HE_BANISHED_HIM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300247, value0, value1);
	}

	/**
	 * @param value0 %0 为 not 成员的 your 军团。 / %0 is not a member of your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_BANISH_HE_IS_NOT_MY_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300248, value0);
	}

	/**
	 * You 无法 kickout 军团军团长。 / You cannot kick out the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_CAN_BANISH_MASTER = new SM_SYSTEM_MESSAGE(1300249);

	/**
	 * You 无法 issue 军团 invitationwhileyou 为 dead。 / You cannot issue a Legion invitation while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CANT_INVITE_WHEN_DEAD = new SM_SYSTEM_MESSAGE(1300250);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300251);

	/**
	 * You 有 noauthority 到 inviteothers 到军团。 / You have no authority to invite others to the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_DONT_HAVE_RIGHT_TO_INVITE = new SM_SYSTEM_MESSAGE(1300252);

	/**
	 * There 为 nouser 到 invite 到 your 军团。 / There is no user to invite to your Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_NO_USER_TO_INVITE = new SM_SYSTEM_MESSAGE(1300253);

	/**
	 * You 无法 inviteyourself 到军团。 / You cannot invite yourself to a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_INVITE_SELF = new SM_SYSTEM_MESSAGE(1300254);

	/**
	 * @param value0 %0 为已经成员的 your 军团。 / %0 is already a member of your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_HE_IS_MY_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300255, value0);
	}

	/**
	 * @param value0 %0 为成员的 another 军团。 / %0 is a member of another Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_HE_IS_OTHER_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300256, value0);
	}

	/**
	 * There 为 noroom 在军团用于 more 成员。 / There is no room in the Legion for more members
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE = new SM_SYSTEM_MESSAGE(1300257);

	/**
	 * @param value0 你：have sent a Legion invitation to %0。 / You have sent a Legion invitation to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_SENT_INVITE_MSG_TO_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300258, value0);
	}

	 /**
	  * %0 has declined your Legion invitation。
	  * %0 has declined your Legion invitation
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_HE_REJECTED_INVITATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300259, value0);
	}

	/**
	 * @param value0 %0 有加入 your 军团。 / %0 has joined your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_HE_JOINED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300260, value0);
	}

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(
			1300261);

	/**
	 * You 无法 changeranks 的军团成员 becauseyou 为 not 军团军团长。 / You cannot change the ranks of Legion members because you are not the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300262);

	/**
	 * 军团军团长无法 change 其拥有 rank。 / The Legion Brigade General cannot change its own rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_ERROR_SELF = new SM_SYSTEM_MESSAGE(1300263);

	/**
	 * There is no one to change rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_NO_USER = new SM_SYSTEM_MESSAGE(1300264);

	/**
	 * @param value0 %0 为 not 成员的 your 军团。 / %0 is not a member of your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_HE_IS_NOT_MY_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300265, value0);
	}

	/**
	 * @param value0 %0 有 become 军团军团长。 / %0 has become the Legion Brigade General
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONE_1_GUILD_MASTER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300266, value0);
	}

	 /**
	  * %0 has become a Legion Centurion。
	  * %0 has become a Legion Centurion
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONE_2_GUILD_OFFICER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300267, value0);
	}

	 /**
	  * %0 has become a Legionary。
	  * %0 has become a Legionary
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONE_3_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300268, value0);
	}

	/**
	 * 你：do not have the authority to change the Legion Brigade General。 / You do not have the authority to change the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300269);

	/**
	 * You 无法 transferyour 军团长 authority 到离线 user。 / You cannot transfer your Brigade General authority to an offline user
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_NO_SUCH_USER = new SM_SYSTEM_MESSAGE(1300270);

	/**
	 * 你已是军团长。 / You are already the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_ERROR_SELF = new SM_SYSTEM_MESSAGE(1300271);

	/**
	 * @param value0 %0 为 not 成员的 your 军团。 / %0 is not a member of your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_NOT_MY_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300272, value0);
	}

	/**
	 * @param value0 %0 有 become 军团军团长。 / %0 has become the Legion Brigade General
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_DONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300273, value0);
	}

	/**
	 * 你：cannot join the alliance because you are not the Legion Brigade General。 / You cannot join the alliance because you are not the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_JOIN_CLAN_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300274);

	/**
	 * You 无法 leavealliancebecauseyou 为 not 军团军团长。 / You cannot leave the alliance because you are not the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEAVE_CLAN_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300275);

	/**
	 * Youdonot 有 authority 到 modify 军团公告。 / You do not have the authority to modify the Legion Announcement
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WRITE_NOTICE_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300276);

	/**
	 * 军团公告有已 modified。 / The Legion Announcement has been modified
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WRITE_NOTICE_DONE = new SM_SYSTEM_MESSAGE(1300277);

	/**
	 * Youmust 为军团成员到 use 军团 warehouse。 / You must be a Legion member to use the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_NO_GUILD_TO_DEPOSIT = new SM_SYSTEM_MESSAGE(1300278);

	/**
	 * 你：cannot use the Legion warehouse。 / You cannot use the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_USE_GUILD_STORAGE = new SM_SYSTEM_MESSAGE(1300279);

	/**
	 * Another 军团成员为 usingwarehouse.Pleasetryagainlater。 / Another Legion member is using the warehouse. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WAREHOUSE_IN_USE = new SM_SYSTEM_MESSAGE(1300280);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WRITE_INTRO_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300281);

	 /**
	  * 你的Character Information has been modified。
	  * 你的Character Information has been modified。 / Your Character Information has been modified
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WRITE_INTRO_DONE = new SM_SYSTEM_MESSAGE(1300282);

	/**
	 * 你：have no authority to change the Legion authority settings。 / You have no authority to change the Legion authority settings
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300283);

	 /**
	  * Legion authority has been modified。
	  * The Legion authority has been modified
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_RIGHT_DONE = new SM_SYSTEM_MESSAGE(1300284);

	/**
	 * @param value0 军团信息0 / Legion Information: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INTRO(String value0) {
		return new SM_SYSTEM_MESSAGE(1300285, value0);
	}

	/**
	 * Youdonot 有足够基纳用于 cancellation。 / You do not have enough Kinah for cancellation
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CREATE_NOT_ENOUGH_MONEY_1 = new SM_SYSTEM_MESSAGE(1300286);

	/**
	 * There 为 noroom 在军团用于 more 成员。 / There is no room in the Legion for more members
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_JOIN_TO_GUILD_BY_SIZE_LIMIT = new SM_SYSTEM_MESSAGE(
			1300287);

	/**
	 * 你：cannot join the Legion。 / You cannot join the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_JOIN_TO_GUILD = new SM_SYSTEM_MESSAGE(1300288);

	/**
	 * You 无法 join 军团作为玩家谁 invitedyou 为 dead。 / You cannot join the Legion as the player who invited you is dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_JOIN_TO_GUILD_INVITOR_IS_DEAD = new SM_SYSTEM_MESSAGE(
			1300289);

	/**
	 * Currently , selected 玩家无法为 invited 到 joinyour 军团。 / Currently, the selected player cannot be invited to join your Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CANT_INVITE_WHEN_HE_IS_QUESTION_ASKED = new SM_SYSTEM_MESSAGE(
			1300290);

	/**
	 * 目标为 not 有效 .Pleaseselect 玩家。 / The target is not valid. Please select a player
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_INCORRECT_TARGET = new SM_SYSTEM_MESSAGE(1300291);

	/**
	 * This authority cannot be granted to the rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_RIGHT_CANT_GIVE_RIGHT = new SM_SYSTEM_MESSAGE(1300292);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300293);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(
			1300294);

	/**
	 * Please try again after you have closed other dialog boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_RETRY_WHEN_CLOSE_OTHER_QUESTION_WND = new SM_SYSTEM_MESSAGE(
			1300295);

	/**
	 * 距离过远：the NPC to cancel the Legion disbanding。 / You are too far from the NPC to cancel the Legion disbanding
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_TOO_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(
			1300296);

	/**
	 * 你：cannot disband the Legion during a war。 / You cannot disband the Legion during a war
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_CANT_DISPERSE_WHILE_WAR = new SM_SYSTEM_MESSAGE(1300297);

	/**
	 * You 无法 disbandyour 军团 whileyou 为 using 军团 warehouse。 / You cannot disband your Legion while you are using the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_CANT_DISPERSE_GUILD_WHILE_USING_WAREHOUSE = new SM_SYSTEM_MESSAGE(
			1300298);

	/**
	 * You 无法 disband 军团有 fortress 或 hideout。 / You cannot disband a Legion that has a fortress or hideout
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_CANT_DISPERSE_GULILD_HAVING_HOUSE = new SM_SYSTEM_MESSAGE(
			1300299);

	/**
	 * 你：have no authority to disband the Legion。 / You have no authority to disband the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_ONLY_MASTER_CAN_DISPERSE = new SM_SYSTEM_MESSAGE(1300300);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(1300301);

	 /**
	  * %0 Legion has been disbanded。
	  * The %0 Legion has been disbanded
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_DONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300302, value0);
	}

	/**
	 * 军团长已申请解散军团。 / The Brigade General has requested to disband the Legion. The expected time of disbanding is %DATETIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_REQUESTED(String datetime0) {
		return new SM_SYSTEM_MESSAGE(1300303, datetime0);
	}

	/**
	 * 你：have already requested to disband the Legion。 / You have already requested to disband the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_ALREADY_REQUESTED = new SM_SYSTEM_MESSAGE(1300304);

	/**
	 * 你距离 NPC 过远，无法解散军团。 / You are too far from the NPC to disband the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_TOO_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300305);

	/**
	 * You 无法删除 character 加入军团 .Pleasetryagainafter 其有 left 军团或军团为 disbanded。 / You cannot delete a character that joined a Legion. Please try again after it has left the Legion or the Legion is disbanded
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_1 = new SM_SYSTEM_MESSAGE(1300306);

	 /**
	  * Legion disbanding mode has been cancelled。
	  * The Legion disbanding mode has been cancelled
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_CANCEL = new SM_SYSTEM_MESSAGE(1300307);

	/**
	 * @param durationday0 解散剩余时间：%DURATIONDAY0 / Time remaining until disbanding: %DURATIONDAY0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_TIME(String durationday0) {
		return new SM_SYSTEM_MESSAGE(1300308, durationday0);
	}

	/**
	 * 仅军团军团长可 canceldisbandingmode。 / Only the Legion Brigade General can cancel the disbanding mode
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_ONLY_MASTER_CAN_CANCEL = new SM_SYSTEM_MESSAGE(
			1300309);

	/**
	 * 军团为 notwaiting 到为 disbanded。 / The Legion is not waiting to be disbanded
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_STAYMODE_CANCEL_YOUR_GUILD_IS_NOT_DISPERS_REQUESTED = new SM_SYSTEM_MESSAGE(
			1300310);

	/**
	 * You 无法 invite 成员的 otherrace。 / You cannot invite members of other race
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE = new SM_SYSTEM_MESSAGE(1300311);

	/**
	 * 你不是军团成员。 / You are not a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_NICKNAME_I_AM_NOT_BELONG_TO_GUILD = new SM_SYSTEM_MESSAGE(
			1300312);

	/**
	 * 你：have no authority to bestow a title。 / You have no authority to bestow a title
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_NICKNAME_DONT_HAVE_RIGHT_TO_CHANGE_NICKNAME = new SM_SYSTEM_MESSAGE(
			1300313);

	/**
	 * @param value0 %0 为 not 成员的 your 军团。 / %0 is not a member of your Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_NICKNAME_HE_IS_NOT_MY_GUILD_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300314, value0);
	}

	/**
	 * 仅军团军团长可 request 到 raiselevel。 / Only the Legion Brigade General can request to raise the level
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1300315);

	/**
	 * 军团为已经在 highestlevel。 / The Legion is already at the highest level
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_CANT_LEVEL_UP = new SM_SYSTEM_MESSAGE(1300316);

	/**
	 * 你：do not have enough Contribution Points。 / You do not have enough Contribution Points
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_POINT = new SM_SYSTEM_MESSAGE(1300317);

	/**
	 * Your 军团 doesnot 有足够成员。 / Your Legion does not have enough members
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_MEMBER = new SM_SYSTEM_MESSAGE(1300318);

	/**
	 * 你：do not have enough Kinah。 / You do not have enough Kinah
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300319);

	/**
	 * @param value0 Your 军团为 now 在 level%0。 / Your Legion is now at level %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_DONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300320, value0);
	}

	/**
	 * Please try again after you have closed other input boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_RETRY_WHEN_CLOSE_OTHER_QUESTION_WND = new SM_SYSTEM_MESSAGE(
			1300321);

	/**
	 * 你：do not have the authority to use the Legion warehouse。 / You do not have the authority to use the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WAREHOUSE_NO_RIGHT = new SM_SYSTEM_MESSAGE(1300322);

	 /**
	  * Legion warehouse is now loading. Please try again later。
	  * The Legion warehouse is now loading. Please try again later
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WAREHOUSE_IN_LOADING = new SM_SYSTEM_MESSAGE(1300323);

	/**
	 * Your 军团 doesnot 有足够 funds。 / Your Legion does not have enough funds
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WAREHOUSE_NOT_ENOUGH_FUND = new SM_SYSTEM_MESSAGE(1300324);

	/**
	 * 目标为 busy 并无法为 invited 在 moment。 / The target is busy and cannot be invited at the moment
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_OTHER_IS_BUSY = new SM_SYSTEM_MESSAGE(1300325);

	/**
	 * 你距离 NPC 过远，无法提升军团等级。 / You are too far from the NPC to raise the Legion level
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_LEVEL_TOO_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300326);

	/**
	 * 你已是军团成员。 / You are already a member of a Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_YOU_ARE_ALREADY_BELONGS_TO_GUILD = new SM_SYSTEM_MESSAGE(
			1300327);

	/**
	 * You 无法 joinanother 军团 whilewaiting 用于 your 军团到为 created。 / You cannot join another Legion while waiting for your Legion to be created
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_YOU_ARE_WAITING_FOR_GUILD_CREATE = new SM_SYSTEM_MESSAGE(
			1300328);

	/**
	 * 军团 you 曾到 joinnolonger 存在。 / The Legion you were to join no longer exists
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_JOIN_NOT_EXISTS = new SM_SYSTEM_MESSAGE(1300329);

	/**
	 * @param value0 你：nominated %0 as the next Legion Brigade General。 / You nominated %0 as the next Legion Brigade General
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_SENT_OFFER_MSG_TO_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300330, value0);
	}

	/**
	 * You 无法 requestselected 玩家到 become 军团军团长。 / You cannot request the selected player to become the Legion Brigade General
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_SENT_CANT_OFFER_WHEN_HE_IS_QUESTION_ASKED = new SM_SYSTEM_MESSAGE(
			1300331);

	/**
	 * @param value0 %0 有 declined 到 become 军团军团长。 / %0 has declined to become the Legion Brigade General
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MASTER_HE_DECLINE_YOUR_OFFER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300332, value0);
	}

	/**
	 * 你：cannot use the Legion warehouse during the disbandment waiting period。 / You cannot use the Legion warehouse during the disbandment waiting period
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE = new SM_SYSTEM_MESSAGE(1300333);

	/**
	 * Limitededition 物品为全部 soldout。 / Limited edition items are all sold out
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_ITEM_SOLD_OUT = new SM_SYSTEM_MESSAGE(1300334);

	/**
	 * 你：cannot buy this item。 / You cannot buy this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_USER_BUY_FAILED = new SM_SYSTEM_MESSAGE(1300335);

	/**
	 * @param value0 %0 不出售物品。 / %0 does not sell items
	 */
	public static SM_SYSTEM_MESSAGE STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300336, value0);
	}

	/**
	 * @param value0 %0 不收购物品。 / %0 does not buy items
	 */
	public static SM_SYSTEM_MESSAGE STR_BUY_SELL_HE_DOES_NOT_BUY_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300337, value0);
	}

	/**
	 * 你距离过远，无法交易。 / You are too away to trade
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_TOO_FAR_TO_TRADE = new SM_SYSTEM_MESSAGE(1300338);

	/**
	 * Youdonot 有足够基纳到 buy 物品。 / You do not have enough Kinah to buy the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_NOT_ENOUGH_MONEY_TO_BUY_ITEM = new SM_SYSTEM_MESSAGE(1300339);

	/**
	 * 你：have bought the item。 / You have bought the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_USER_BOUGHT_ITEMS = new SM_SYSTEM_MESSAGE(1300340);

	/**
	 * 销售完成。 / Sales complete
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_USER_SELL_ITEMS = new SM_SYSTEM_MESSAGE(1300341);

	/**
	 * 无法出售已装备物品。 / You cannot sell equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_CAN_NOT_SELL_EQUIPED_ITEM = new SM_SYSTEM_MESSAGE(1300342);

	/**
	 * 物品价格已变更。请确认价格后再次购买。 / The price of the item has changed. Please try buying it again after you have checked the changed price
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_PRICE_CHANGED_RETRY_PLEASE = new SM_SYSTEM_MESSAGE(1300343);

	/**
	 * @param value0 %0 为 not 物品可 sold。 / %0 is not an item that can be sold
	 */
	public static SM_SYSTEM_MESSAGE STR_BUY_SELL_ITEM_CAN_NOT_BE_SELLED_TO_NPC(String value0) {
		return new SM_SYSTEM_MESSAGE(1300344, value0);
	}

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300345);

	/**
	 * 你距离过远，无法交谈。 / You are too far to have a conversation
	 */
	public static final SM_SYSTEM_MESSAGE STR_DIALOG_TOO_FAR_TO_TALK = new SM_SYSTEM_MESSAGE(1300346);

	/**
	 * 你已在与他人交易。 / You are already trading with someone else
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_YOU_ARE_ALREADY_EXCHANGING = new SM_SYSTEM_MESSAGE(1300347);

	/**
	 * 你：cannot trade while you are invisible。 / You cannot trade while you are invisible
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_EXCHANGE_WHILE_INVISIBLE = new SM_SYSTEM_MESSAGE(1300348);

	/**
	 * 你：cannot trade with an invisible player。 / You cannot trade with an invisible player
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_EXCHANGE_WITH_INVISIBLE_USER = new SM_SYSTEM_MESSAGE(
			1300349);

	/**
	 * You 无法 trade 作为 you 为 overburdened 带物品。 / You cannot trade as you are overburdened with items
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_TOO_HEAVY_TO_TRADE = new SM_SYSTEM_MESSAGE(1300350);

	/**
	 * 你：have no one to trade with。 / You have no one to trade with
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_NO_ONE_TO_EXCHANGE = new SM_SYSTEM_MESSAGE(1300351);

	/**
	 * 距离过远：the target to trade。 / You are too far from the target to trade
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_TOO_FAR_TO_EXCHANGE = new SM_SYSTEM_MESSAGE(1300352);

	/**
	 * @param value0 你：sent a trade message to %0。 / You sent a trade message to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EXCHANGE_ASKED_EXCHANGE_TO_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300353, value0);
	}

	/**
	 * @param value0 %0 拒绝了你的交易请求。 / %0 declined your trade offer
	 */
	public static SM_SYSTEM_MESSAGE STR_EXCHANGE_HE_REJECTED_EXCHANGE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300354, value0);
	}

	/**
	 * 目标为已经 trading 带 someone 否则。 / The target is already trading with someone else
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_PARTNER_IS_EXCHANGING_WITH_OTHER = new SM_SYSTEM_MESSAGE(
			1300355);

	/**
	 * @param value0 %0 当前无法交易。 / %0 cannot trade at the moment
	 */
	public static SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_ASK_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300356, value0);
	}

	/**
	 * You 无法 trade 带目标作为目标为 carryingtoomany 物品。 / You cannot trade with the target as the target is carrying too many items
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTNER_TOO_HEAVY_TO_EXCHANGE = new SM_SYSTEM_MESSAGE(1300357);

	 /**
	  * %0 is not a tradable item。
	  * %0 is not a tradable item
	  */
	public static SM_SYSTEM_MESSAGE STR_EXCHANGE_ITEM_CANNOT_BE_EXCHANGED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300358, value0);
	}

	/**
	 * You 无法 trade 带目标作为 you 为 carryingtoomany 物品。 / You cannot trade with the target as you are carrying too many items
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_EXCHANGE_HEAVY_TO_ADD_EXCHANGE_ITEM = new SM_SYSTEM_MESSAGE(
			1300359);

	/**
	 * @param value0 You 无法 trade 作为目标已经有 limitedpossession 物品 %0。 / You cannot trade as the target already has the limited possession item %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_EXCHANGE_PARTNER_HAS_LORE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300360, value0);
	}

	/**
	 * 交易完成。 / The trade is complete
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_COMPLETE = new SM_SYSTEM_MESSAGE(1300361);

	/**
	 * other 玩家有 pressedLock 列表 button。 / The other player has pressed the Lock List button
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_OTHER_PRESSED_CHECK = new SM_SYSTEM_MESSAGE(1300362);

	 /**
	  * other player has pressed the Final Confirmation button。
	  * The other player has pressed the Final Confirmation button
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_OTHER_PRESSED_OK = new SM_SYSTEM_MESSAGE(1300363);

	 /**
	  * trade has been cancelled。
	  * The trade has been cancelled
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CANCELED = new SM_SYSTEM_MESSAGE(1300364);

	/**
	 * 无法出售已装备物品。 / You cannot sell equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CANT_SELL_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(1300365);

	/**
	 * You 无法 tradeanymore 作为 yourinventory 为 full。 / You cannot trade any more as your inventory is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_FULL_INVENTORY = new SM_SYSTEM_MESSAGE(1300366);

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300367);

	/**
	 * 此物品无法为 registered。 / This item cannot be registered
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_CAN_NOT_REGISTER_ITEM = new SM_SYSTEM_MESSAGE(1300368);

	/**
	 * This is not a tradable item
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_ITEM_CAN_NOT_BE_EXCHANGED = new SM_SYSTEM_MESSAGE(1300369);

	/**
	 * @param i 你：cannot use the selected item until you reach the %0 rank。 / You cannot use the selected item until you reach the %0 rank
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_RANK(int i) {
		return new SM_SYSTEM_MESSAGE(1300370, new DescriptionId(i));
	}

	 /**
	  * 你的Class cannot use the selected item。
	  * 你的Class cannot use the selected item。 / Your Class cannot use the selected item
	  */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_CLASS = new SM_SYSTEM_MESSAGE(1300371);

	/**
	 * 你：cannot use %1 until you reach level %0。 / You cannot use %1 until you reach level %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_TOO_LOW_LEVEL_MUST_BE_THIS_LEVEL(int value1, int value0) {
		return new SM_SYSTEM_MESSAGE(1300372, value0, new DescriptionId(value1));
	}

	/**
	 * Yourrace 无法 use 此物品。 / Your race cannot use this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_RACE = new SM_SYSTEM_MESSAGE(1300373);

	 /**
	  * 你的国籍使你无法使用该物品。
	  * Your nationality prevents you from using this item
	  */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_NATION = new SM_SYSTEM_MESSAGE(1300374);

	/**
	 * 此物品无法为 used 按 yourgender。 / This item cannot be used by your gender
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_GENDER = new SM_SYSTEM_MESSAGE(1300375);

	/**
	 * 你负重过高，无法再拾取物品。 / You are too overburdened to pick up any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_HEAVY = new SM_SYSTEM_MESSAGE(1300376);

	/**
	 * Another 玩家有 firstchance 到 pickup 此物品。 / Another player has the first chance to pick up this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_PICKUP_ITEM_FAILED_NOT_MY_ITEM = new SM_SYSTEM_MESSAGE(1300377);

	/**
	 * 你距离过远，无法拾取物品。 / You are too far away to pick up the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_PICKUP_ITEM_FAILED_TOO_FAR = new SM_SYSTEM_MESSAGE(1300378);

	/**
	 * You 无法放入 downanymore 物品在此 place。 / You cannot put down any more items at this place
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_DROP_THE_LOC = new SM_SYSTEM_MESSAGE(1300379);

	/**
	 * 你：cannot discard equipped items。 / You cannot discard equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_DROP_WORN = new SM_SYSTEM_MESSAGE(1300380);

	/**
	 * @param value0 你：cannot discard %0。 / You cannot discard %0
	 */
	public static SM_SYSTEM_MESSAGE STR_UNBREAKABLE_ITEM(DescriptionId value0) {
		return new SM_SYSTEM_MESSAGE(1300381, value0);
	}

	/**
	 * @param value0 %0 当前拒绝接收物品。 / %0 is currently refusing to accept items
	 */
	public static SM_SYSTEM_MESSAGE STR_IS_NOT_WILLING_TO_RECEIVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300382, value0);
	}

	/**
	 * 你：cannot give equipped items。 / You cannot give equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_GIVE_WORN = new SM_SYSTEM_MESSAGE(1300383);

	/**
	 * 你负重过高，无法战斗。 / You are too overburdened to fight
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_HEAVY_TO_ATTACK = new SM_SYSTEM_MESSAGE(1300384);

	/**
	 * @param value0 你：ate %0。 / You ate %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EAT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300385, value0);
	}

	/**
	 * @param value0 你：cannot equip %0。 / You cannot equip %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_EQUIP(String value0) {
		return new SM_SYSTEM_MESSAGE(1300386, value0);
	}

	/**
	 * @param value0 你：do not buy %0。 / You do not buy %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SELL_TO_NPC_NO_INTEREST_IN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300387, value0);
	}

	/**
	 * 你：do not have enough Kinah。 / You do not have enough Kinah
	 */
	public static final SM_SYSTEM_MESSAGE STR_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300388);

	/**
	 * @param value0 %0 无法丢弃。 / %0 cannot be discarded
	 */
	public static SM_SYSTEM_MESSAGE STR_ITEM_CANNOT_BE_DROPPED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300389, value0);
	}

	/**
	 * @param value0 %0 无法给予他人。 / %0 cannot be given to others
	 */
	public static SM_SYSTEM_MESSAGE STR_ITEM_CANNOT_BE_GIVEN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300390, value0);
	}

	/**
	 * %0 给了你 %1。 / %0 gave you %1
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_TO_YOU(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300391, value0, value1);
	}

	/**
	 * 该物品每人限持有一个，你背包中已有。 / That item is limited to one per person, and you already have one in your inventory
	 */
	public static final SM_SYSTEM_MESSAGE STR_CAN_NOT_BUY_LORE_ITEM = new SM_SYSTEM_MESSAGE(1300392);

	/**
	 * @param value0 你：cannot use %0。 / You cannot use %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_USE_ITEM(DescriptionId value0) {
		return new SM_SYSTEM_MESSAGE(1300393, value0);
	}

	/**
	 * @param value0 你：cannot use %0 as you have already used it to its maximum usage count。 / You cannot use %0 as you have already used it to its maximum usage count
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_USE_ITEM_OUT_OF_USABLE_COUNT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300394, value0);
	}

	/**
	 * @param value0 You 无法 use%0 到 maximumusage 数量作为物品为 currentlyequipped。 / You cannot use %0 to the maximum usage count as the item is currently equipped
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_USE_WORN_ITEM_OUT_OF_USABLE_COUNT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300395, value0);
	}

	/**
	 * @param value0 你获得了 %0。 / You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300396, value0);
	}

	/**
	 * 你：do not have a weapon to modify the appearance of。 / You do not have a weapon to modify the appearance of
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_CHANGE_WEAPON_SKIN__THERE_IS_NO_WEAPON = new SM_SYSTEM_MESSAGE(
			1300398);

	/**
	 * You 无法 modifyappearance 的 selected 物品作为其为 notweapon。 / You cannot modify the appearance of the selected item as it is not a weapon
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_CHANGE_WEAPON_SKIN__SELECTED_ITEM_IS_NOT_WEAPON = new SM_SYSTEM_MESSAGE(
			1300399);

	/**
	 * 你：can only modify the appearance of the weapon to another of the same type。 / You can only modify the appearance of the weapon to another of the same type
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_CHANGE_WEAPON_SKIN__DIFFERENT_WEAPON_TYPE = new SM_SYSTEM_MESSAGE(
			1300400);

	/**
	 * 你：have equipped the Stigma Stone and acquired the %0 skill (Level %1)。 / You have equipped the Stigma Stone and acquired the %0 skill (Level %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_YOU_CAN_USE_THIS_SKILL_BY_STIGMA_STONE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300401, value0, value1);
	}

	/**
	 * You 有 removedStigmaStone , butyou 可仍然 use%0skill ( Level%1 )作为 you 为 equipped 带 anotherstone。 / You have removed the Stigma Stone, but you can still use the %0 skill (Level %1) as you are equipped with another stone
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_AFTER_UNEQUIP_STONE_YOU_CAN_USE_THIS_SKILL_LEVEL_BY_OTHER_STONE(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300402, value0, value1);
	}

	/**
	 * @param value0 你：have removed the Stigma Stone and can no longer use the %0 skill。 / You have removed the Stigma Stone and can no longer use the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_YOU_CANNOT_USE_THIS_SKILL_AFTER_UNEQUIP_STIGMA_STONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300403, value0);
	}

	/**
	 * 你：need help from a Stigma Master to equip the Stigma Stone。 / You need help from a Stigma Master to equip the Stigma Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_TO_EQUIP_STONE_TALK_WITH_STIGMA_NPC = new SM_SYSTEM_MESSAGE(
			1300404);

	/**
	 * 你：need help from a Stigma Master to remove the Stigma Stone。 / You need help from a Stigma Master to remove the Stigma Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_TO_UNEQUIP_STONE_TALK_WITH_STIGMA_NPC = new SM_SYSTEM_MESSAGE(
			1300405);

	/**
	 * @param value0 你：need %0 Stigma Shard(s) to equip this Stone。 / You need %0 Stigma Shard(s) to equip this Stone
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_CANNT_EQUIP_STONE_OUT_OF_AVAILABLE_STIGMA_POINT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300406, value0);
	}

	/**
	 * @param value0 你：cannot equip that Stigma Stone because you have not learned the %0 skill。 / You cannot equip that Stigma Stone because you have not learned the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_CANNT_EQUIP_STONE_YOU_DO_NOT_HAVE_THIS_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300407, value0);
	}

	/**
	 * There 为 noStigma 槽位可用。 / There is no Stigma slot available
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_SLOT_IS_NOT_OPENED = new SM_SYSTEM_MESSAGE(1300408);

	/**
	 * @param value0 %0 无法装备：品阶超过烙印之石槽位上限。 / %0 cannot be equipped as its rank exceeds the maximum rank of the Stigma slot
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_TOO_HIGH_STONE_RANK_FOR_SLOT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300409, value0);
	}

	/**
	 * You 无法移除 StigmaStonebecause%1 为前置条件用于 %0thStigmaStone。 / You cannot remove the Stigma Stone because %1 is a prerequisite for the %0th Stigma Stone
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_CANNT_UNEQUIP_STONE_OTHER_STONE_NEED_ITS_SKILL(String value1,
			String value0) {
		return new SM_SYSTEM_MESSAGE(1300410, value1, value0);
	}

	/**
	 * @param num0sp 你：have spent %num0sp。 / You have spent %num0sp
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_EXHAUST(int num0sp) {
		return new SM_SYSTEM_MESSAGE(1300411, num0sp);
	}

	/**
	 * %num0sp has been returned to you
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_RETURN(int num0sp) {
		return new SM_SYSTEM_MESSAGE(1300412, num0sp);
	}

	/**
	 * Youdonot 有足够基纳到 equipStigmaStone。 / You do not have enough Kinah to equip the Stigma Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300413);

	/**
	 * @param value0 你：can no longer use the %0 skill acquired through the Stigma Stone。 / You can no longer use the %0 skill acquired through the Stigma Stone
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_NOT_USABLE_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300414, value0);
	}

	/**
	 * This Stigma Stone cannot be equipped
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_CANNOT_EQUIP_STONE = new SM_SYSTEM_MESSAGE(1300415);

	/**
	 * 你携带物品过多。 / You are carrying too many items
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_TOO_MANY_ITEMS_INVENTORY = new SM_SYSTEM_MESSAGE(1300416);

	/**
	 * There is no space in the warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_TOO_MANY_ITEMS_WAREHOUSE = new SM_SYSTEM_MESSAGE(1300417);

	/**
	 * 你：cannot store this in the warehouse。 / You cannot store this in the warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_CANT_DEPOSIT_ITEM = new SM_SYSTEM_MESSAGE(1300418);

	/**
	 * 距离过远：the NPC。 / You are too far from the NPC
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_TOO_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300419);

	/**
	 * Equipped items cannot be stored in the warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_DEPOSIT_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(1300420);

	/**
	 * There is no space in the warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_DEPOSIT_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300421);

	/**
	 * @param descriptionId You 无法有此物品作为 you 已经有 limitedpossession 物品 %0%。 / You cannot have this item as you already have the limited possession item %0%
	 */
	public static SM_SYSTEM_MESSAGE STR_CAN_NOT_GET_LORE_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300422, descriptionId);
	}

	/**
	 * @param value0 你：have used %0。 / You have used %0
	 */
	public static SM_SYSTEM_MESSAGE STR_USE_ITEM(DescriptionId value0) {
		return new SM_SYSTEM_MESSAGE(1300423, value0);
	}

	 /**
	  * %0 has been destroyed。
	  * %0 has been destroyed
	  */
	public static SM_SYSTEM_MESSAGE STR_BREAK_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300424, value0);
	}

	/**
	 * 附近没有掉落物品。 / There are no dropped items nearby
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_NO_DROP_ITEM = new SM_SYSTEM_MESSAGE(1300425);

	/**
	 * You 无法 use 物品 here。 / You cannot use that item here
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_LOCATION = new SM_SYSTEM_MESSAGE(1300426);

	/**
	 * @param id 你：have cancelled using the item。 / You have cancelled using the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_CANCELED(DescriptionId id) {
		return new SM_SYSTEM_MESSAGE(1300427, id);
	}

	/**
	 * other 玩家为 carryingtoomany 物品。 / The other player is carrying too many items
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_PARTNER_HAS_TOON_MANY_ITEMS_INVENTORY = new SM_SYSTEM_MESSAGE(
			1300428);

	/**
	 * 对方交易窗口物品过多，交易后将超出你的背包容量。 / There are too many items in the target's trade window. The total number of items will exceed the size of your inventory after trading
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_EXCHANGE_RESULT_WILL_BE_OVER_YOUR_INVENTORY_SIZE = new SM_SYSTEM_MESSAGE(
			1300429);

	 /**
	  * 你的背包无法再扩展。
	  * 你的背包 cannot be further expanded。 / Your cube cannot be further expanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXTEND_INVENTORY_CANT_EXTEND_MORE = new SM_SYSTEM_MESSAGE(1300430);

	 /**
	  * %0 spaces have been added to your cube。
	  * %0 spaces have been added to your cube
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_INVENTORY_SIZE_EXTENDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300431, value0);
	}

	 /**
	  * 你的个人仓库无法再扩展。
	  * 你的个人仓库 无法再扩展。 / Your private warehouse cannot be further expanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXTEND_CHAR_WAREHOUSE_CANT_EXTEND_MORE = new SM_SYSTEM_MESSAGE(1300432);

	 /**
	  * %0 spaces have been added to your personal warehouse。
	  * %0 spaces have been added to your personal warehouse
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_CHAR_WAREHOUSE_SIZE_EXTENDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300433, value0);
	}

	 /**
	  * 你的账号仓库无法再扩展。
	  * 你的账号 仓库 cannot be further expanded。 / Your account warehouse cannot be further expanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXTEND_ACCOUNT_WAREHOUSE_CANT_EXTEND_MORE = new SM_SYSTEM_MESSAGE(
			1300434);

	 /**
	  * %0 spaces have been added to your account warehouse。
	  * %0 spaces have been added to your account warehouse
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_ACCOUNT_WAREHOUSE_SIZE_EXTENDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300435, value0);
	}

	 /**
	  * %0 can only upgrade cubes of level %1 or higher。
	  * %0 can only upgrade cubes of level %1 or higher
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_INVENTORY_CANT_EXTEND_DUE_TO_MINIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300436, value0, value1);
	}

	 /**
	  * %0 can only upgrade cubes to level %1。
	  * %0 can only upgrade cubes to level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_INVENTORY_CANT_EXTEND_MORE_DUE_TO_MAXIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300437, value0, value1);
	}

	 /**
	  * %0 can only upgrade private warehouses of level %1 or higher。
	  * %0 can only upgrade private warehouses of level %1 or higher
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_CHAR_WAREHOUSE_CANT_EXTEND_DUE_TO_MINIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300438, value0, value1);
	}

	 /**
	  * %0 can only upgrade warehouses to level %1。
	  * %0 can only upgrade warehouses to level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_CHAR_WAREHOUSE_CANT_EXTEND_MORE_DUE_TO_MAXIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300439, value0, value1);
	}

	 /**
	  * %0 can only upgrade warehouses of level %1 or higher。
	  * %0 can only upgrade warehouses of level %1 or higher
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_ACCOUNT_WAREHOUSE_CANT_EXTEND_DUE_TO_MINIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300440, value0, value1);
	}

	 /**
	  * %0 can only upgrade warehouses to level %1。
	  * %0 can only upgrade warehouses to level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_EXTEND_ACCOUNT_WAREHOUSE_CANT_EXTEND_MORE_DUE_TO_MAXIMUM_EXTEND_LEVEL_BY_THIS_NPC(
			String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300441, value0, value1);
	}

	 /**
	  * 你的背包无法再通过任务升级。
	  * 你的背包 cannot be upgraded any further through quests。 / Your cube cannot be upgraded any further through quests
	  */
	public static final SM_SYSTEM_MESSAGE STR_EXTEND_INVENTORY_CANT_EXTEND_MORE_BY_QUEST = new SM_SYSTEM_MESSAGE(
			1300442);

	/**
	 * 你距离过远，无法查看背包。 / You are too far away to view the inventory
	 */
	public static final SM_SYSTEM_MESSAGE STR_VIEW_OTHER_INVENTORY_TOO_FAR_FROM_TARGET = new SM_SYSTEM_MESSAGE(1300443);

	/**
	 * 无法移除烙印之石：须先卸下相关装备。 / The Stigma Stone cannot be removed: All items currently equipped via the skills acquired through this Stigma Stone must be removed first
	 */
	public static final SM_SYSTEM_MESSAGE STR_STIGMA_CANNT_UNEQUIP_STONE_FIRST_UNEQUIP_CURRENT_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(
			1300444);

	/**
	 * Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300445);

	 /**
	  * %0 is not an extractable item。
	  * %0 is not an extractable item
	  */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_IT_CAN_NOT_BE_DECOMPOSED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300446, value0);
	}

	/**
	 * Youmust 有在 least 一个空 space 在 yourcubebeforeyou 可 extract 物品。 / You must have at least one empty space in your cube before you can extract an item
	 */
	public static final SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1300447);

	/**
	 * @param value0 你：have failed to extract from %0。 / You have failed to extract from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_FAILED(int value0) {
		return new SM_SYSTEM_MESSAGE(1300448, new DescriptionId(value0));
	}

	/**
	 * @param value0 你：have successfully extracted from %0。 / You have successfully extracted from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_SUCCEED(int value0) {
		return new SM_SYSTEM_MESSAGE(1300449, new DescriptionId(value0));
	}

	/**
	 * @param nameId 你：have cancelled the extraction from %0。 / You have cancelled the extraction from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_CANCELED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300450, new DescriptionId(nameId));
	}

	/**
	 * @param value0 你：cannot extract item in %0。 / You cannot extract item in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOSE_ITEM_INVALID_STANCE(int value0) {
		return new SM_SYSTEM_MESSAGE(1300451, new DescriptionId(value0));
	}

	/**
	 * Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300452);

	/**
	 * @param value0 %0 无法强化。 / %0 cannot be enchanted
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_IT_CAN_NOT_BE_ENCHANTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300453, value0);
	}

	/**
	 * @param nameId %0 无法再强化。 / %0 cannot be enchanted any more
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_IT_CAN_NOT_BE_ENCHANTED_MORE_TIME(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300454, new DescriptionId(nameId));
	}

	/**
	 * @param nameId 你：have successfully enchanted %0。 / You have successfully enchanted %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_SUCCEED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300455, new DescriptionId(nameId));
	}

	/**
	 * @param descriptionId 你：have failed to enchant %0。 / You have failed to enchant %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_FAILED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300456, descriptionId);
	}

	/**
	 * @param nameId 你：have cancelled the enchanting of %0。 / You have cancelled the enchanting of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_CANCELED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300457, new DescriptionId(nameId));
	}

	/**
	 * @param value0 You 无法 enchant 物品在 %0。 / You cannot enchant items in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ENCHANT_ITEM_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300458, value0);
	}

	/**
	 * Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300459);

	/**
	 * @param value0 %0 无法镶嵌魔石。 / %0 cannot be socketed with manastones
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_IT_CAN_NOT_BE_GIVEN_OPTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300460, value0);
	}

	 /**
	  * %0 cannot be socketed with Manastone。
	  * %0 cannot be socketed with Manastone
	  */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_IT_CAN_NOT_BE_GIVEN_OPTION_MORE_TIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300461, value0);
	}

	/**
	 * @param descriptionId 你：have succeeded in the manastone socketing of %0。 / You have succeeded in the manastone socketing of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_SUCCEED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300462, descriptionId);
	}

	/**
	 * @param descriptionId 你：have failed in the manastone socketing of %0。 / You have failed in the manastone socketing of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_FAILED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300463, descriptionId);
	}

	/**
	 * @param value0 你：have cancelled the manastone socketing of %0。 / You have cancelled the manastone socketing of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_CANCELED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300464, value0);
	}

	/**
	 * @param value0 全部 manastones 曾 socketed 在 %0 有 disappeared。 / All manastones that were socketed in %0 have disappeared
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_ALL_OPTION_REMOVED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300465, value0);
	}

	/**
	 * @param value0 你：cannot socket manastones while %0。 / You cannot socket manastones while %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_OPTION_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300466, value0);
	}

	/**
	 * 距离过远：the NPC to remove the manastone。 / You are too far from the NPC to remove the manastone
	 */
	public static final SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300467);

	/**
	 * Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300468);

	/**
	 * @param value0 Manastonesocketing / removal 为 notpossible 用于物品 %0。 / Manastone socketing / removal is not possible for the item %0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_IT_CAN_NOT_BE_GIVEN_OPTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300469, value0);
	}

	 /**
	  * %0 is not socketed with a manastone。
	  * %0 is not socketed with a manastone
	  */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_NO_OPTION_TO_REMOVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300470, value0);
	}

	/**
	 * @param value0 目标槽位 on0notsocketed 魔石 / The target slot on %0 is not socketed with a manastone
	 */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_INVALID_OPTION_SLOT_NUMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300471, value0);
	}

	/**
	 * @param value0 Youdonot 有足够基纳到移除 manastone 从 %0。 / You do not have enough Kinah to remove the manastone from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_NOT_ENOUGH_GOLD(String value0) {
		return new SM_SYSTEM_MESSAGE(1300472, value0);
	}

	/**
	 * @param value0 你：have removed the manastone from %0。 / You have removed the manastone from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_SUCCEED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300473, value0);
	}

	/**
	 * @param value0 You 无法移除 manastones 从物品在 %0。 / You cannot remove manastones from items in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMOVE_ITEM_OPTION_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300474, value0);
	}

	/**
	 * 距离过远：the NPC to modify the appearance of the item。 / You are too far from the NPC to modify the appearance of the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300475);

	/**
	 * 你：must be at least level 20 before you can modify the appearance of items。 / You must be at least level 20 before you can modify the appearance of items
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_PC_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(1300476);

	/**
	 * Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300477);

	/**
	 * @param value0 无法修改 %0 的外观。 / The appearance of %0 cannot be modified
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NOT_SKIN_CHANGABLE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300478, value0);
	}

	/**
	 * 无法修改 %0 的外观。 / The appearance of %0 cannot be modified into %1 and vice versa as they are different types of item
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NOT_SAME_EQUIP_SLOT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300479, value0, value1);
	}

	/**
	 * 无法修改 %0 的外观。 / The appearance of %0 cannot be modified into %1 and vice versa as they are different type of items
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NOT_COMPATIBLE(DescriptionId descriptionId,
			DescriptionId descriptionId2) {
		return new SM_SYSTEM_MESSAGE(1300480, descriptionId, descriptionId2);
	}

	/**
	 * @param descriptionId Youdonot 有足够基纳到 modifyappearance 的 %0。 / You do not have enough Kinah to modify the appearance of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NOT_ENOUGH_GOLD(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300481, descriptionId);
	}

	/**
	 * @param value0 You 有 failed 到 modifyappearance 的物品作为 youcouldnot 移除 skin 物品 %0。 / You have failed to modify the appearance of the item as you could not remove the skin item %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_CAN_NOT_REMOVE_SKIN_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300482, value0);
	}

	/**
	 * @param descriptionId 你：have modified the appearance of %0。 / You have modified the appearance of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_SUCCEED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300483, descriptionId);
	}

	/**
	 * @param value0 You 无法 modifyappearance 的物品在 %0。 / You cannot modify the appearance of items in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300484, value0);
	}

	/**
	 * @param value0 你：have successfully soul-bound %0。 / You have successfully soul-bound %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SOUL_BOUND_ITEM_SUCCEED(int value0) {
		return new SM_SYSTEM_MESSAGE(1300485, new DescriptionId(value0));
	}

	/**
	 * @param value0 你：have failed to soul-bind %0。 / You have failed to soul-bind %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SOUL_BOUND_ITEM_FAILED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300486, value0);
	}

	/**
	 * @param value0 你：cancelled the soul-binding of %0。 / You cancelled the soul-binding of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SOUL_BOUND_ITEM_CANCELED(int value0) {
		return new SM_SYSTEM_MESSAGE(1300487, new DescriptionId(value0));
	}

	/**
	 * Please try the soul-binding again after you have closed other input boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_SOUL_BOUND_CLOSE_OTHER_MSG_BOX_AND_RETRY = new SM_SYSTEM_MESSAGE(1300488);

	/**
	 * @param value0 You 无法 soul -绑定物品 while%0。 / You cannot soul-bind an item while %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SOUL_BOUND_INVALID_STANCE(int value0) {
		return new SM_SYSTEM_MESSAGE(1300489, new DescriptionId(value0));
	}

	/**
	 * 你：do not have a Power Shard equipped。 / You do not have a Power Shard equipped
	 */
	public static final SM_SYSTEM_MESSAGE STR_WEAPON_BOOST_NO_BOOSTER_EQUIPED = new SM_SYSTEM_MESSAGE(1300490);

	/**
	 * 你：activate the Power Shard。 / You activate the Power Shard
	 */
	public static final SM_SYSTEM_MESSAGE STR_WEAPON_BOOST_BOOST_MODE_STARTED = new SM_SYSTEM_MESSAGE(1300491);

	/**
	 * 你：deactivate the Power Shard。 / You deactivate the Power Shard
	 */
	public static final SM_SYSTEM_MESSAGE STR_WEAPON_BOOST_BOOST_MODE_ENDED = new SM_SYSTEM_MESSAGE(1300492);

	/**
	 * 你：cannot use the item。 / You cannot use the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_IS_NOT_USABLE = new SM_SYSTEM_MESSAGE(1300493);

	/**
	 * You 无法 use 物品作为其 cooldowntime 有 notexpiredyet。 / You cannot use the item as its cooldown time has not expired yet
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_CANT_USE_UNTIL_DELAY_TIME = new SM_SYSTEM_MESSAGE(1300494);

	/**
	 * Youmust 为 next 到 postbox 并 click 其到 usepostservice。 / You must be next to a postbox and click it to use the post service
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_CLICK_POSTBOX_TO_USE = new SM_SYSTEM_MESSAGE(1300495);

	/**
	 * 你：cannot mail equipped items。 / You cannot mail equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_SEND_CAN_NOT_SEND_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(1300496);

	/**
	 * You 无法 mail 物品为 nottradable。 / You cannot mail items that are not tradable
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_SEND_ITEM_CAN_NOT_BE_EXCHANGED = new SM_SYSTEM_MESSAGE(1300497);

	/**
	 * You 无法 mail 物品 you 有已经 used。 / You cannot mail items that you have already used
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_SEND_USED_ITEM = new SM_SYSTEM_MESSAGE(1300498);

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_SEND_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300499);

	 /**
	  * selected NPC cannot add ability to the item。
	  * The selected NPC cannot add ability to the item
	  */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GIVE_ITEM_PROC_CANT_GIVE_PROC_BY_THIS_NPC = new SM_SYSTEM_MESSAGE(
			1300500);

	/**
	 * 距离过远：the NPC to add abilities to the item。 / You are too far from the NPC to add abilities to the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GIVE_ITEM_PROC_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300501);

	/**
	 * Failed 到 find 目标物品到添加 ability 到。 / Failed to find the target item to add the ability to
	 */
	public static final SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1300502);

	/**
	 * You 无法添加 ability 到 equipped 物品。 / You cannot add an ability to equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_CANNOT_GIVE_PROC_TO_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(
			1300503);

	/**
	 * @param value0 %0 为 not 物品 you 可添加 ability 到。 / %0 is not an item you can add ability to
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NOT_PROC_GIVABLE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300504, value0);
	}

	/**
	 * 无法 find 物品到添加 ability 到。 / Cannot find the item to add ability to
	 */
	public static final SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NO_PROC_GIVE_ITEM = new SM_SYSTEM_MESSAGE(1300505);

	/**
	 * @param value0 %0 为 not 物品 you 可添加 ability 到。 / %0 is not an item you can add ability to
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NOT_PROC_GIVE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300506, value0);
	}

	/**
	 * @param descriptionId Youdonot 有足够基纳到添加 ability 到 %0。 / You do not have enough Kinah to add ability to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NOT_ENOUGH_MONEY(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300507, descriptionId);
	}

	/**
	 * @param descriptionId 你：have successfully added ability to %0。 / You have successfully added ability to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_ENCHANTED_TARGET_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300508, descriptionId);
	}

	/**
	 * @param value0 你：cannot socket godstones while %0。 / You cannot socket godstones while %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300509, value0);
	}

	/**
	 * @param value0 你：have removed the dye from %0。 / You have removed the dye from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ITEM_COLOR_REMOVE_SUCCEED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300510, value0);
	}

	/**
	 * 你：have dyed %0 %1。 / You have dyed %0 %1
	 */
	public static SM_SYSTEM_MESSAGE STR_ITEM_COLOR_CHANGE_SUCCEED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300511, value0, value1);
	}

	/**
	 * @param value0 %0 无法染色。 / %0 cannot be dyed
	 */
	public static SM_SYSTEM_MESSAGE STR_ITEM_COLOR_CHANGE_ERROR_CANNOTDYE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300512, value0);
	}

	/**
	 * 物品有 not 已 dyed。 / The item has not been dyed
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_COLOR_REMOVE_ERROR_CANNOTREMOVE = new SM_SYSTEM_MESSAGE(1300513);

	/**
	 * 物品无法为 found。 / The item cannot be found
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_COLOR_ERROR = new SM_SYSTEM_MESSAGE(1300514);

	/**
	 * 你：cannot dye equipped items。 / You cannot dye equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_COLOR_CANNOT_CHANGE_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(1300515);

	/**
	 * 目标为 busy 并无法 trade 在 moment。 / The target is busy and cannot trade at the moment
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXCHANGE_START_OHER_IS_BUSY = new SM_SYSTEM_MESSAGE(1300516);

	/**
	 * %0's Reputation has increased by %1 point(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_POINTUP(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300517, value0, value1);
	}

	/**
	 * %0's Reputation has fallen by %1 point(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_POINTDOWN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300518, value0, value1);
	}

	/**
	 * %0's Reputation has become Hostile
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_HOSTIL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300519, value0);
	}

	/**
	 * %0's Reputation has become Confrontational
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_OPPOSITE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300520, value0);
	}

	/**
	 * %0's Reputation has become Neutral
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_NEUTRAL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300521, value0);
	}

	/**
	 * %0's Reputation has become Friendly
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_FREINDSHIP(String value0) {
		return new SM_SYSTEM_MESSAGE(1300522, value0);
	}

	/**
	 * %0's Reputation has become Alliance mode
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_ALLY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300523, value0);
	}

	/**
	 * @param value0 恭喜！你已加入 %0。 / Congratulations! You have joined %0
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_JOIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300524, value0);
	}

	/**
	 * 你已经a member。 / You are already a member
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_CAN_NOT_JOIN = new SM_SYSTEM_MESSAGE(1300525);

	/**
	 * @param value0 你：have left %0。 / You have left %0
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_LEAVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300526, value0);
	}

	/**
	 * 耶里迪斯 / The Jeridises
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_ZERIDITH = new SM_SYSTEM_MESSAGE(1300527);

	/**
	 * 布鲁贡 / The Brugons
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_BRUGON = new SM_SYSTEM_MESSAGE(1300528);

	/**
	 * 提莫里斯 / The Timoris
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_TIMORITH = new SM_SYSTEM_MESSAGE(1300529);

	/**
	 * 无法连接申诉服务器，请稍后再试。 / A connection cannot be established with the Petition Server. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1300537);

	/**
	 * 请求内容过短。支持请求至少需要 5 个词。 / The petition is too short. A Support Petition must be at least 5 words in length
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_SHORT_PETITION_MSG = new SM_SYSTEM_MESSAGE(1300538);

	/**
	 * @param value0 SupportPetition 有已 received.receipt 编号为 %0。 / The Support Petition has been received. The receipt number is %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_OK(String value0) {
		return new SM_SYSTEM_MESSAGE(1300539, value0);
	}

	/**
	 * 这是你的第 %num0 份请求。今日还可提交 %num1 份支持请求。 / This is your %num0th petition. You may make %num1 more Support Petitions today
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_QUOTA_LEFT(int num0th, int num1) {
		return new SM_SYSTEM_MESSAGE(1300540, num0th, num1);
	}

	/**
	 * @param num0 当前有 %num0 名用户在排队提交客服申诉。 / There are %num0 users waiting in the queue to lodge Support Petitions
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_WAITING_COUNT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300541, num0);
	}

	 /**
	  * %0 has received a request from the GM for consultation。
	  * %0 has received a request from the GM for consultation
	  */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_OK_TO_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300542, value0);
	}

	/**
	 * %0 已收到 GM 代提交的申诉，编号 %1。 / %0 has received a proxy petition generated by the GM. The petition number is %1
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_OK_TO_GM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300543, value0, value1);
	}

	/**
	 * @param value0 已收到 GM 代提交申诉，但用户离线。编号 %0。 / A proxy petition by the GM has been received, but the user is offline. The petition number is %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_OK_TO_GM_USER_OFFLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300544, value0);
	}

	/**
	 * 客服服务在 %0 至 %1 点不可用。若卡死请使用返回技能。 / The Support service cannot be used in the hours between %0 and %1 o' clock. Please use the Return skill if you are locked in an impassable area
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_FAILED_NOT_SERVICE_TIME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300545, value0, value1);
	}

	 /**
	  * 你的Support request has failed. Please try again later。
	  * 你的Support request has failed. Please try again later。 / Your Support request has failed. Please try again later
	  */
	public static final SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_FAILED = new SM_SYSTEM_MESSAGE(1300546);

	/**
	 * You 有 usedupyourdailyquota 的 %num0SupportPetitions.You 无法 makeanymoreinquiries 带此 accounttoday。 / You have used up your daily quota of %num0 Support Petitions. You cannot make any more inquiries with this account today
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_FAILED_OUT_OF_QUOTA(int num0) {
		return new SM_SYSTEM_MESSAGE(1300547, num0);
	}

	/**
	 * SupportPetition 有已经已 received.Pleasewait 用于 reply。 / A Support Petition has already been received. Please wait for a reply
	 */
	public static final SM_SYSTEM_MESSAGE STR_SUBMIT_PETITION_FAILED_ALREADY_SUBMITTED = new SM_SYSTEM_MESSAGE(1300548);

	/**
	 * @param value0 代提交申诉失败：%0 已有客服申诉。 / Your proxy petition request has failed. %0 has already received the Support Petition
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_FAILED_ALREADY_SUBMITTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300549, value0);
	}

	 /**
	  * 你对 %0 的代理请求失败，错误代码为 %1。
	  * Your proxy petition request for %0 has failed. The error code is %1
	  */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_FAILED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300550, value0, value1);
	}

	/**
	 * @param value0 代提交申诉失败（用户离线）。错误码 %0。 / The request for a proxy petition has failed. (The user is currently offline.) The error code is %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SUBMIT_GM_PETITION_FAILED_OFFLINE_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300551, value0);
	}

	/**
	 * Petition No. %0 has been cancelled
	 */
	public static SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_OK1(String value0) {
		return new SM_SYSTEM_MESSAGE(1300552, value0);
	}

	/**
	 * @param num1 申诉已取消。今日剩余客服申诉次数：%num1。 / The petition has been cancelled. You have %num1 Support Petitions left for today
	 */
	public static SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_OK2(int num1) {
		return new SM_SYSTEM_MESSAGE(1300553, num1);
	}

	/**
	 * @param value0 你：cancelled the proxy petition request for %0。 / You cancelled the proxy petition request for %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_OK_TO_GM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300554, value0);
	}

	/**
	 * Failed to cancel the petition. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_FAIL = new SM_SYSTEM_MESSAGE(1300555);

	/**
	 * 客服申诉正在处理中。 / The Support Petition is already being processed
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_FAIL_FORBIDDEN = new SM_SYSTEM_MESSAGE(1300556);

	/**
	 * 当前无法提交客服申诉。 / Support Petitions cannot be submitted at the moment
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_FAIL_NOT_SERVICE_TIME = new SM_SYSTEM_MESSAGE(1300557);

	/**
	 * 取消向 %0 的代提交申诉失败。错误码 %1。 / Failed to cancel the request for a proxy petition to %0. The error code is %1
	 */
	public static SM_SYSTEM_MESSAGE STR_CANCEL_PETITION_FAIL_TO_GM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300558, value0, value1);
	}

	 /**
	  * User (%0) is not in the game server。
	  * The User (%0) is not in the game server
	  */
	public static SM_SYSTEM_MESSAGE STR_PETITION_USER_NOT_FOUND(String value0) {
		return new SM_SYSTEM_MESSAGE(1300559, value0);
	}

	 /**
	  * 你与 GM（%0）的咨询已开始。
	  * Your consultation with the GM (%0) has started
	  */
	public static SM_SYSTEM_MESSAGE STR_PETITION_CHAT_STARTED_TO_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300560, value0);
	}

	/**
	 * @param value0 GM（%0）已回复完毕，请稍后评价客服申诉服务。 / The GM (%0)'s response is complete. Please evaluate the Support Petition service in a moment
	 */
	public static SM_SYSTEM_MESSAGE STR_PETITION_CHAT_ENDED_TO_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300561, value0);
	}

	/**
	 * 你不in a consultation with the GM。 / You are not in a consultation with the GM
	 */
	public static final SM_SYSTEM_MESSAGE STR_NOT_IN_PETITION_CHAT = new SM_SYSTEM_MESSAGE(1300562);

	/**
	 * 向 GM 传输对话记录时出错，请稍后再试。 / An error has occurred while transmitting the conversation log to the GM. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_CHAT_ERROR = new SM_SYSTEM_MESSAGE(1300563);

	/**
	 * %0 : %1
	 */
	public static SM_SYSTEM_MESSAGE STR_PETITION_GM_CHAT_MSG(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300564, value0, value1);
	}

	/**
	 * @param value0 This 消息 GM0 / This is a message from the GM: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_PETITION_GM_LEAVE_MSG(String value0) {
		return new SM_SYSTEM_MESSAGE(1300565, value0);
	}

	/**
	 * Only the group leader can receive this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_PARTY_LEADER_ONLY = new SM_SYSTEM_MESSAGE(1300566);

	/**
	 * @param value0 You 可仅 receive 此 quest 当 yourgroup 有 %0 或 more 成员。 / You can only receive this quest when your group has %0 or more members
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_PARTY_SIZE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300567, value0);
	}

	/**
	 * 仅军团军团长可 receivequest。 / Only the Legion Brigade General can receive the quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_GUILD_MASTER_ONLY = new SM_SYSTEM_MESSAGE(1300568);

	/**
	 * @param value0 You 可仅 receive 此 quest 当 level 的 your 军团为 %0 或 above。 / You can only receive this quest when the level of your Legion is %0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_GUILD_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300569, value0);
	}

	/**
	 * @param num0 You 可仅 receive 此 quest 当军团 Point 的 your 军团为 %num0 或 above。 / You can only receive this quest when the Legion Point of your Legion is %num0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_GUILD_EXP(int num0) {
		return new SM_SYSTEM_MESSAGE(1300570, num0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 yourlevel 为 %0 或 above。 / You can only receive this quest when your level is %0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300571, value0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 yourlevel 为 %0 或 below。 / You can only receive this quest when your level is %0 or below
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300572, value0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 yourrank 为 %0 或 above。 / You can only receive this quest when your rank is %0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MIN_RANK(int value0) {
		return new SM_SYSTEM_MESSAGE(1300573, new DescriptionId(value0));
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 yourproductionjobrank 为 %0 或 above。 / You can only receive this quest when your production job rank is %0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_TS_RANK(String value0) {
		return new SM_SYSTEM_MESSAGE(1300574, value0);
	}

	 /**
	  * 你的种族无法接取该任务。
	  * 你的种族 无法接取该任务。 / Your race cannot receive this quest
	  */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_RACE = new SM_SYSTEM_MESSAGE(1300575);

	 /**
	  * 你的国籍使你无法接取该任务。
	  * Your nationality prevents you from receiving this quest
	  */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_NATION = new SM_SYSTEM_MESSAGE(1300576);

	/**
	 * Only males can receive this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MAN_ONLY = new SM_SYSTEM_MESSAGE(1300577);

	/**
	 * Only females can receive this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_WOMAN_ONLY = new SM_SYSTEM_MESSAGE(1300578);

	 /**
	  * 你的性别使你无法接取该任务。
	  * 你的性别 使你无法接取该任务。 / Your gender prevents you from receiving this quest
	  */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_GENDER = new SM_SYSTEM_MESSAGE(1300579);

	 /**
	  * 你的职业使你无法接取该任务。
	  * 你的职业 使你无法接取该任务。 / Your class prevents you from receiving this quest
	  */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_CLASS = new SM_SYSTEM_MESSAGE(1300580);

	/**
	 * @param value0 你：must have the %0 voice to receive this quest。 / You must have the %0 voice to receive this quest
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_VOICE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300581, value0);
	}

	/**
	 * @param num0 You 可仅 receive 此 quest 当 youneed%num0 或 lessXP 到 reachnextlevel。 / You can only receive this quest when you need %num0 or less XP to reach the next level
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_EXP_TO_NEXT_LEVEL(int num0) {
		return new SM_SYSTEM_MESSAGE(1300582, num0);
	}

	/**
	 * @param num0 You 可仅 receive 此 quest 当 yourStigmaPoint 为 %num0 或 above。 / You can only receive this quest when your Stigma Point is %num0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_STIGMA_PT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300583, num0);
	}

	/**
	 * @param num0 You 可仅 receive 此 quest 当 yourPVPpoint 为 %num0 或 more。 / You can only receive this quest when your PVP point is %num0 or more
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_PVP_PT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300584, num0);
	}

	/**
	 * You 可仅 receive 此 quest 当 %0 ' sfavortowardyou 为 %num1 或 above。 / You can only receive this quest when %0's favor toward you is %num1 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_FAVOR(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1300585, value0, num1);
	}

	/**
	 * You 可仅 receive 此 quest 当 yourFaction 带 %0 为 %num1 或 higher。 / You can only receive this quest when your Faction with %0 is %num1 or higher
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_FACTION(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1300586, value0, num1);
	}

	/**
	 * @param num0 You 可仅 receive 此 quest 当 yourNationalContributePoint 为 %num0 或 more。 / You can only receive this quest when your National Contribute Point is %num0 or more
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_NATION_PT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300587, num0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 you 有 %0title。 / You can only receive this quest when you have the %0 title
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_TITLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300588, value0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 you 有 %0skill。 / You can only receive this quest when you have the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300589, value0);
	}

	/**
	 * You 可仅 accept 此 quest 当 you 有 level%1%0skill。 / You can only accept this quest when you have level %1 %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_SKILL_LEVEL(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1300590, value1, value0);
	}

	/**
	 * 你不in the altered state required to receive this quest。 / You are not in the altered state required to receive this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_ABNORMAL_STATUS = new SM_SYSTEM_MESSAGE(1300591);

	/**
	 * @param num0 Youmust 有 played 用于 total 的在 least%num0hours 到 receive 此 quest。 / You must have played for a total of at least %num0 hours to receive this quest
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_PLAY_TIME(int num0) {
		return new SM_SYSTEM_MESSAGE(1300592, num0);
	}

	/**
	 * @param descriptionId You 可仅 receive 此 quest 当 you 为 equipped 带 %0。 / You can only receive this quest when you are equipped with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_EQUIP_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300593, descriptionId);
	}

	/**
	 * @param descriptionId You 可仅 receive 此 quest 当 you 有 %0 在 yourinventory。 / You can only receive this quest when you have %0 in your inventory
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300594, descriptionId);
	}

	/**
	 * @param value0 Youmust 为成员的 %0NPC 军团到 receive 此 quest。 / You must be a member of the %0 NPC Legion to receive this quest
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_NPC_GUILD(String value0) {
		return new SM_SYSTEM_MESSAGE(1300595, value0);
	}

	/**
	 * @param value0 You 可仅 receive 此 quest 当 you 有 completed%0quest。 / You can only receive this quest when you have completed the %0 quest
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_FINISHED_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300596, value0);
	}

	/**
	 * You 无法 receivequestyou 为已经 working 在。 / You cannot receive a quest that you are already working on
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST = new SM_SYSTEM_MESSAGE(1300597);

	/**
	 * 你：cannot receive quests while you are dead。 / You cannot receive quests while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_DIE = new SM_SYSTEM_MESSAGE(1300598);

	/**
	 * @param value0 你：can do the %0 quest only once。 / You can do the %0 quest only once
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300599, value0);
	}

	/**
	 * 你：can do the %0 quest only %1 times。 / You can do the %0 quest only %1 times
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MAX_REPEAT_COUNT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300600, value0, value1);
	}

	/**
	 * You 无法获取 questrewardwhileyou 为 dead。 / You cannot get a quest reward while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_GET_REWARD_ERROR_DEAD = new SM_SYSTEM_MESSAGE(1300601);

	/**
	 * You 无法获取 questreward 作为 you 不有 %0%0s。 / You cannot get the quest reward as you don't have %0 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_GET_REWARD_ERROR_NO_QUEST_ITEM_SINGLE(String value0, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300602, value0, value0s);
	}

	/**
	 * You 无法 receivequestreward 作为 youdonot 有 %1%0。 / You cannot receive the quest reward as you do not have %1 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_GET_REWARD_ERROR_NO_QUEST_ITEM_MULTIPLE(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1300603, value1, value0);
	}

	/**
	 * @param value0 You 无法 destroy%0because 其为 used 在" %1 " quest 无法为 abandonedoncestarted。 / You cannot destroy %0 because it is used in the "%1" quest which cannot be abandoned once started
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM_IMPOSSIBLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300604, value0);
	}

	/**
	 * 请关闭其他对话框后再次尝试销毁任务物品。 / Please try destroying the quest item again after you have closed other dialog boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM_RETRY = new SM_SYSTEM_MESSAGE(
			1300605);

	/**
	 * No Quest selected
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_NO_QUEST = new SM_SYSTEM_MESSAGE(1300606);

	/**
	 * Quest Indicator
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUIEST_INDICATOR = new SM_SYSTEM_MESSAGE(1300607);

	/**
	 * 任务 / Quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_QUEST = new SM_SYSTEM_MESSAGE(1300608);

	/**
	 * @return %1[获得]%2 %0 / %1[acquire]%2 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_QIMSG_ACQUIRE() {
		return new SM_SYSTEM_MESSAGE(1300609);
	}

	/**
	 * @return %1[失败]%2 %0 / %1[fail]%2 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_QIMSG_GIVEUP() {
		return new SM_SYSTEM_MESSAGE(1300610);
	}

	/**
	 * @return %1[更新]%2 %0 / %1[update]%2 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_QIMSG_UPDATE() {
		return new SM_SYSTEM_MESSAGE(1300611);
	}

	/**
	 * @return %1[完成]%2 %0 / %1[complete]%2 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_QIMSG_COMPLETE() {
		return new SM_SYSTEM_MESSAGE(1300612);
	}

	/**
	 * Quest acquired: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_ACQUIRE_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300613, value0);
	}

	/**
	 * Quest failed: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_GIVEUP_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300614, value0);
	}

	/**
	 * Quest updated: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_UPDATE_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300615, value0);
	}

	/**
	 * Quest complete: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_COMPLETE_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300616, value0);
	}

	/**
	 * Quest acquired: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_ACQUIRE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300617, value0);
	}

	/**
	 * Quest failed: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_GIVEUP(String value0) {
		return new SM_SYSTEM_MESSAGE(1300618, value0);
	}

	/**
	 * Quest updated: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_UPDATE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300619, value0);
	}

	/**
	 * Quest complete: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_QUEST_SYSTEMMSG_COMPLETE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300620, value0);
	}

	/**
	 * 你：cannot learn this design。 / You cannot learn this design
	 */
	public static final SM_SYSTEM_MESSAGE STR_RECIPEITEM_CANT_USE_NO_RECIPE = new SM_SYSTEM_MESSAGE(1300621);

	/**
	 * 你：cannot receive any more quests。 / You cannot receive any more quests
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ACQUIRE_ERROR_MAX_NORMAL = new SM_SYSTEM_MESSAGE(1300622);

	 /**
	  * %0 has been banned。
	  * %0 has been banned
	  */
	public static SM_SYSTEM_MESSAGE STR_USER_BANNED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300623, value0);
	}

	 /**
	  * %0 has been disconnected from the server。
	  * %0 has been disconnected from the server
	  */
	public static SM_SYSTEM_MESSAGE STR_USER_KICKED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300624, value0);
	}

	/**
	 * @param value0 没有名为 %0 的用户。 / There is no user named %0
	 */
	public static SM_SYSTEM_MESSAGE STR_NO_USER_NAMED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300625, value0);
	}

	/**
	 * 即将发布重要 GM 公告，请稍候。 / There is going to be an important announcement from the GM. Please be patient for a while
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_CHAT_DURING_NOTIFICATION = new SM_SYSTEM_MESSAGE(1300626);

	/**
	 * @param value0 %0 未在游戏中。 / %0 is not playing the game
	 */
	public static SM_SYSTEM_MESSAGE STR_NO_SUCH_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300627, value0);
	}

	 /**
	  * %0 has blocked you。
	  * %0 has blocked you
	  */
	public static SM_SYSTEM_MESSAGE STR_YOU_EXCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300628, value0);
	}

	/**
	 * @param value0 %0 当前不接受密语。 / %0 is currently not accepting any Whispers
	 */
	public static SM_SYSTEM_MESSAGE STR_WHISPER_REFUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300629, value0);
	}

	/**
	 * 什么也没发生。 / Nothing happened
	 */
	public static final SM_SYSTEM_MESSAGE STR_NOTHING_HAPPEN = new SM_SYSTEM_MESSAGE(1300630);

	/**
	 * 你：cannot use teleport here。 / You cannot use teleport here
	 */
	public static final SM_SYSTEM_MESSAGE STR_NO_TELEPORT = new SM_SYSTEM_MESSAGE(1300631);

	/**
	 * @param value0 你：have unblocked %0。 / You have unblocked %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ONE_INCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300632, value0);
	}

	/**
	 * @param value0 你：have blocked %0。 / You have blocked %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ONE_EXCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300633, value0);
	}

	/**
	 * @param num0 已屏蔽用户：%num0 人 / Blocked users: %num0 users
	 */
	public static SM_SYSTEM_MESSAGE STR_CURRENT_EXCLUDES(int num0) {
		return new SM_SYSTEM_MESSAGE(1300634, num0);
	}

	/**
	 * 没有已屏蔽用户。 / There are no blocked users
	 */
	public static final SM_SYSTEM_MESSAGE STR_NO_EXCLUDES = new SM_SYSTEM_MESSAGE(1300635);

	/**
	 * @param value0 你：have excluded %0。 / You have excluded %0
	 */
	public static SM_SYSTEM_MESSAGE STR_ONE_NO_LONGER_INCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300636, value0);
	}

	/**
	 * @param value0 你：have listed %0 as a friend。 / You have listed %0 as a friend
	 */
	public static SM_SYSTEM_MESSAGE STR_ONE_IS_INCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300637, value0);
	}

	/**
	 * 你：have too many users listed as friends。 / You have too many users listed as friends
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_MANY_INCLUDE = new SM_SYSTEM_MESSAGE(1300638);

	/**
	 * @param num0 好友列表：%num0 人 / Users listed as friends: %num0 users
	 */
	public static SM_SYSTEM_MESSAGE STR_CURRENT_INCLUDES(int num0) {
		return new SM_SYSTEM_MESSAGE(1300639, num0);
	}

	/**
	 * 你：have no chat friends。 / You have no chat friends
	 */
	public static final SM_SYSTEM_MESSAGE STR_NO_INCLUDES = new SM_SYSTEM_MESSAGE(1300640);

	/**
	 * @param value0 当前用户：%0 / Current users: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_LIST_USER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300641, value0);
	}

	 /**
	  * server is due to shut down in %0 seconds. Please quit the game。
	  * The server is due to shut down in %0 seconds. Please quit the game
	  */
	public static SM_SYSTEM_MESSAGE STR_SERVER_SHUTDOWN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300642, value0);
	}

	/**
	 * @param value0m 请勿刷屏。禁言 %0 分钟。 / Please do not flood chat. Blocked for %0m
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_DISABLED_FOR(String value0m) {
		return new SM_SYSTEM_MESSAGE(1300643, value0m);
	}

	/**
	 * 你：may now chat again。 / You may now chat again
	 */
	public static final SM_SYSTEM_MESSAGE STR_CAN_CHAT_NOW = new SM_SYSTEM_MESSAGE(1300644);

	/**
	 * 请勿刷屏。已短暂禁言。 / Please do not flood chat. Blocked for a short while
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLOBAL_CHAT_DISABLED_FOR = new SM_SYSTEM_MESSAGE(1300645);

	/**
	 * You 无法创建 generalchannel 在 yourdiscretion。 / You cannot create a general channel at your discretion
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_MAKE_GENERALCHANNEL = new SM_SYSTEM_MESSAGE(1300646);

	/**
	 * @param nameId %0 有已经已 sold。 / %0 has already been sold
	 */
	public static SM_SYSTEM_MESSAGE STR_VENDOR_SOLD_OUT(int nameId) {
		return new SM_SYSTEM_MESSAGE(1300647, new DescriptionId(nameId));
	}

	/**
	 * Youdonot 有足够基纳到 payfee。 / You do not have enough Kinah to pay the fee
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1300648);

	/**
	 * You 无法 registeranymore 物品作为 there 为 nospace 可用。 / You cannot register any more items as there is no space available
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_FULL_ITEM = new SM_SYSTEM_MESSAGE(1300649);

	/**
	 * You 无法 register 物品有已经已 used。 / You cannot register items that have already been used
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_REGISTER_USED_ITEM = new SM_SYSTEM_MESSAGE(1300650);

	/**
	 * 你：cannot register equipped items。 / You cannot register equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_REGISTER_EQUIPPED_ITEM = new SM_SYSTEM_MESSAGE(1300651);

	/**
	 * You 无法列表 untradeable 物品。 / You cannot list an untradeable item
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_REGISTER_CANNOT_BE_EXCHANGED = new SM_SYSTEM_MESSAGE(1300652);

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_REGISTER_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300653);

	/**
	 * You 无法 continuetrading 作为 yourinventory 为 full。 / You cannot continue trading as your inventory is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_FULL_INVENTORY = new SM_SYSTEM_MESSAGE(1300654);

	/**
	 * 你：cannot register this item。 / You cannot register this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_CAN_NOT_REGISTER_ITEM = new SM_SYSTEM_MESSAGE(1300655);

	/**
	 * 此物品为已经 registered。 / This item is already registered
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_ALREADY_REGISTERED = new SM_SYSTEM_MESSAGE(1300656);

	/**
	 * Items for Sale! The best value around!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DEFAULT_ADVERTISE_MSG = new SM_SYSTEM_MESSAGE(1300657);

	/**
	 * 你：start doing business at your private store。 / You start doing business at your private store
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_START = new SM_SYSTEM_MESSAGE(1300658);

	/**
	 * 你：stop doing business at your private store。 / You stop doing business at your private store
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_END = new SM_SYSTEM_MESSAGE(1300659);

	/**
	 * 无法出售已装备物品。 / You cannot sell equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CAN_NOT_SELL_EQUIPED_ITEM = new SM_SYSTEM_MESSAGE(1300660);

	/**
	 * You 无法 sell 物品无法为 traded 带 otherusers。 / You cannot sell items that cannot be traded with other users
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CANNOT_BE_EXCHANGED = new SM_SYSTEM_MESSAGE(1300661);

	/**
	 * 你：cannot sell used items。 / You cannot sell used items
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CAN_NOT_SELL_USED_ITEM = new SM_SYSTEM_MESSAGE(1300662);

	/**
	 * 你：cannot open a private store while fighting。 / You cannot open a private store while fighting
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DISABLED_IN_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1300663);

	/**
	 * 战斗中无法开设个人商店，将自动关闭。 / As you cannot open a private store while fighting, it will be closed automatically
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CLOSED_FOR_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1300664);

	/**
	 * @param value0 %0 有已经已 sold。 / %0 has already been sold
	 */
	public static SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_SOLD_OUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300665, value0);
	}

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300666);

	/**
	 * 你：have not opened Quickbar No.2。 / You have not opened Quickbar No.2
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUICKBAR_NOT_OPEN_SECONDBAR = new SM_SYSTEM_MESSAGE(1300667);

	/**
	 * 你：have not opened Quickbar No.3。 / You have not opened Quickbar No.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUICKBAR_NOT_OPEN_THIRDBAR = new SM_SYSTEM_MESSAGE(1300668);

	/**
	 * 未登记绑定点，将在城镇复活。 / As there is no registered bind point, you will resurrect in the city
	 */
	public static final SM_SYSTEM_MESSAGE STR_DEATH_NOT_REGISTERED_RESURRECT_POINT = new SM_SYSTEM_MESSAGE(1300669);

	/**
	 * @param subzone 你已绑定在 [%subzone]。 / You are now bound at [%subzone]
	 */
	public static SM_SYSTEM_MESSAGE STR_DEATH_REGISTER_RESURRECT_POINT(String subzone) {
		return new SM_SYSTEM_MESSAGE(1300670, subzone);
	}

	/**
	 * Please try again after you have closed other dialog boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_ASK_RECOVER_EXPERIENCE_BY_OTHER_QUESTION = new SM_SYSTEM_MESSAGE(
			1300671);

	/**
	 * Youdonot 有足够基纳到 recoveryourXP。 / You do not have enough Kinah to recover your XP
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_RECOVER_EXPERIENCE_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1300672);

	/**
	 * 距离过远：a healer to receive Soul Healing。 / You are too far from a healer to receive Soul Healing
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_RECOVER_EXPERIENCE_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300673);

	/**
	 * 你：received Soul Healing。 / You received Soul Healing
	 */
	public static final SM_SYSTEM_MESSAGE STR_SUCCESS_RECOVER_EXPERIENCE = new SM_SYSTEM_MESSAGE(1300674);

	/**
	 * 你距离过远，无法更改 PVP 区域。 / You are too far to change the PVP zone
	 */
	public static final SM_SYSTEM_MESSAGE STR_PVPZONE_CANNOT_MOVE_PVPZONE_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300675);

	/**
	 * 你：cannot change the PVP zone because you have no means to move。 / You cannot change the PVP zone because you have no means to move
	 */
	public static final SM_SYSTEM_MESSAGE STR_PVPZONE_CANNOT_MOVE_PVPZONE_NPC_NOT_CORRECT = new SM_SYSTEM_MESSAGE(
			1300676);

	/**
	 * 目标为 invalid.Pleaseselect 玩家。 / The target is invalid. Please select a player
	 */
	public static final SM_SYSTEM_MESSAGE STR_ASSISTKEY_INCORRECT_TARGET = new SM_SYSTEM_MESSAGE(1300677);

	 /**
	  * person you want to assist does not have a target。
	  * The person you want to assist does not have a target
	  */
	public static final SM_SYSTEM_MESSAGE STR_ASSISTKEY_NO_USER = new SM_SYSTEM_MESSAGE(1300678);

	 /**
	  * person you want to assist is too far from the target。
	  * The person you want to assist is too far from the target
	  */
	public static final SM_SYSTEM_MESSAGE STR_ASSISTKEY_TOO_FAR = new SM_SYSTEM_MESSAGE(1300679);

	/**
	 * @param value0 你正在协助 %0 所选的目标。 / You are assisting the target %0 has selected
	 */
	public static SM_SYSTEM_MESSAGE STR_ASSISTKEY_ASSIST_FOR_SOMEONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300680, value0);
	}

	 /**
	  * map is now loading。
	  * The map is now loading
	  */
	public static final SM_SYSTEM_MESSAGE STR_WORLDMAP_INFO = new SM_SYSTEM_MESSAGE(1300681);

	/**
	 * 你：do not have any XP to recover。 / You do not have any XP to recover
	 */
	public static final SM_SYSTEM_MESSAGE STR_DONOT_HAVE_RECOVER_EXPERIENCE = new SM_SYSTEM_MESSAGE(1300682);

	/**
	 * 你：can respond to the survey only in this server。 / You can respond to the survey only in this server
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_POLL_ANSWER_IS_NOT_ORG_SERVER = new SM_SYSTEM_MESSAGE(1300683);

	/**
	 * 当前没有进行中的调查。 / There is no survey underway
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_FIND_POLL = new SM_SYSTEM_MESSAGE(1300684);

	/**
	 * You 有已经 responded 到此 survey。 / You have already responded to this survey
	 */
	public static final SM_SYSTEM_MESSAGE STR_ALREADY_ANSWER_THIS_POLL = new SM_SYSTEM_MESSAGE(1300685);

	/**
	 * Youdonot 有足够基纳到 register 此 location 作为绑定 point。 / You do not have enough Kinah to register this location as a bind point
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_RESURRECT_POINT_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(
			1300686);

	/**
	 * You 无法绑定从 here。 / You cannot bind from here
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_RESURRECT_POINT_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(
			1300687);

	/**
	 * You 有已经 bound 在此 location。 / You have already bound at this location
	 */
	public static final SM_SYSTEM_MESSAGE STR_ALREADY_REGISTER_THIS_RESURRECT_POINT = new SM_SYSTEM_MESSAGE(1300688);

	/**
	 * Youdonot 有足够基纳用于 teleport。 / You do not have enough Kinah for teleport
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1300689);

	/**
	 * You 无法 use 其作为所需 quest 有 not 已 completed。 / You cannot use it as the required quest has not been completed
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST = new SM_SYSTEM_MESSAGE(1300690);

	/**
	 * 你：cannot move to that destination。 / You cannot move to that destination
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE = new SM_SYSTEM_MESSAGE(1300691);

	/**
	 * 所选 NPC 无法为你传送。 / The NPC you selected does not have the ability to teleport you
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_MOVE_TO_AIRPORT_WRONG_NPC = new SM_SYSTEM_MESSAGE(1300692);

	/**
	 * 距离过远：the NPC to teleport。 / You are too far from the NPC to teleport
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_MOVE_TO_AIRPORT_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300693);

	/**
	 * 你：can bind here by clicking the Obelisk。 / You can bind here by clicking the Obelisk
	 */
	public static final SM_SYSTEM_MESSAGE STR_NOTIFY_RESURRECT_POINT = new SM_SYSTEM_MESSAGE(1300694);

	/**
	 * 你已处于复活效果中。 / You are already experiencing the resurrection effect
	 */
	public static final SM_SYSTEM_MESSAGE STR_OTHER_USER_USE_RESURRECT_SKILL_ALREADY = new SM_SYSTEM_MESSAGE(1300695);

	/**
	 * You 无法 teleport 到绑定 pointwhileflying。 / You cannot teleport to a bind point while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_AIRPORT_WHEN_FLYING = new SM_SYSTEM_MESSAGE(1300696);

	/**
	 * server 为 beingshutdown 用于更新。 / The server is being shut down for an update
	 */
	public static final SM_SYSTEM_MESSAGE STR_SHUTDOWN_REASON_UPDATE = new SM_SYSTEM_MESSAGE(1300697);

	/**
	 * @param value0 正在离开阿特雷亚。\n\n 请等待 %0 秒。 / Leaving Atreia.\n\n Please wait %0 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_WAIT_TO_QUIT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300698, value0);
	}

	 /**
	  * account usage time has expired。
	  * The account usage time has expired
	  */
	public static final SM_SYSTEM_MESSAGE STR_KICK_TIME_EXPIRED = new SM_SYSTEM_MESSAGE(1300699);

	/**
	 * Another user has tried to log in
	 */
	public static final SM_SYSTEM_MESSAGE STR_KICK_ANOTHER_USER_TRY_LOGIN = new SM_SYSTEM_MESSAGE(1300700);

	/**
	 * Youdonot 有足够基纳到 useartifact。 / You do not have enough Kinah to use the artifact
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ARTIFACT_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1300701);

	/**
	 * 当前无法使用神器。 / The Artifact cannot be used at this time
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ARTIFACT_OUT_OF_ORDER = new SM_SYSTEM_MESSAGE(1300702);

	/**
	 * 你：have no authority to use the Artifact。 / You have no authority to use the Artifact
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ARTIFACT_HAVE_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(1300703);

	/**
	 * 你：cannot use the Artifact from this place。 / You cannot use the Artifact from this place
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ARTIFACT_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300704);

	/**
	 * 这不是可使用的神器。 / This is not a usable Artifact
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_ARTIFACT_IS_NOT_ARTIFACT = new SM_SYSTEM_MESSAGE(1300705);

	/**
	 * 你：have no authority to go through the door。 / You have no authority to go through the door
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DOOR_HAVE_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(1300706);

	/**
	 * 你：cannot use the door from here。 / You cannot use the door from here
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DOOR_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300707);

	 /**
	  * 你的任务追踪栏已满。
	  * 你的任务追踪栏 已满。 / Your quest tracker is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_IND_EXCESS = new SM_SYSTEM_MESSAGE(1300708);

	/**
	 * 宏取消 / Macro canceled
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANCEL = new SM_SYSTEM_MESSAGE(1300709);

	/**
	 * Macro complete
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_END = new SM_SYSTEM_MESSAGE(1300710);

	/**
	 * Mail has arrived
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RECEIVE_MAIL = new SM_SYSTEM_MESSAGE(1300711);

	/**
	 * 全部物品为已经 confirmed.You 无法 registeranymore 物品。 / All items are already confirmed. You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCHANGE_ALREADY_READY = new SM_SYSTEM_MESSAGE(1300712);

	/**
	 * You 无法 split 物品在 inventoryduringtrade。 / You cannot split items in the inventory during a trade
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVENTORY_SPLIT_DURING_TRADE = new SM_SYSTEM_MESSAGE(1300713);

	/**
	 * 你：cannot open the private store on a moving object。 / You cannot open the private store on a moving object
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DISABLED_IN_MOVING_OBJECT = new SM_SYSTEM_MESSAGE(1300714);

	/**
	 * 此裂隙不可用。 / This Rift is not usable
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_NO_PORTAL = new SM_SYSTEM_MESSAGE(1300715);

	/**
	 * 你：cannot use a Rift here。 / You cannot use a Rift here
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300716);

	/**
	 * 你：cannot use a Rift at your level。 / You cannot use a Rift at your level
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(1300717);

	/**
	 * Rift 有已经 hadmaximum 编号的 peopletravelthrough 其。 / The Rift has already had the maximum number of people travel through it
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_USE_COUNT_LIMIT = new SM_SYSTEM_MESSAGE(1300718);

	/**
	 * @param value0 剩余游戏时间 %*0。 / The remaining playing time is %*0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMAIN_PLAYTIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300719, value0);
	}

	/**
	 * @param value0 正在应用预付时长，剩余游戏时间 %*0。 / Pre-paid credit is being applied. The remaining playing time is %*0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_REMAIN_PLAYTIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300720, value0);
	}

	/**
	 * 请在门附近使用钥匙。 / Please use the key near the door
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_KEY_ITEM = new SM_SYSTEM_MESSAGE(1300721);

	/**
	 * @param value0 你：need %0 to open the door。 / You need %0 to open the door
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_OPEN_DOOR_NEED_NAMED_KEY_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300722, value0);
	}

	/**
	 * 你：need a key to open the door。 / You need a key to open the door
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_OPEN_DOOR_NEED_KEY_ITEM = new SM_SYSTEM_MESSAGE(1300723);

	/**
	 * 交易经纪人 / Trade Broker
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_RETURN_MAIL_FROM = new SM_SYSTEM_MESSAGE(1300724);

	/**
	 * Your 物品有已 returned 作为 salesperiod 有 ended。 / Your item has been returned as the sales period has ended
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_RETURN_MAIL_CONTENT = new SM_SYSTEM_MESSAGE(1300725);

	/**
	 * 你：have no authority to use it as you are not a member of the Conquering Legion。 / You have no authority to use it as you are not a member of the Conquering Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DOOR_REPAIR_HAVE_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(1300726);

	/**
	 * You 无法 use 作为 cooldowntime 有 notexpiredyet。 / You cannot use that as the cooldown time has not expired yet
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DOOR_REPAIR_OUT_OF_COOLTIME = new SM_SYSTEM_MESSAGE(1300727);

	/**
	 * 你：do not have enough items needed for repair. The fee is %0 (per %1)。 / You do not have enough items needed for repair. The fee is %0 (per %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_DOOR_REPAIR_NOT_ENOUGH_FEE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300728, value0, value1);
	}

	 /**
	  * %0 has used level %1 %2。
	  * %0 has used level %1 %2
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_MONSTER_SKILL(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1300729, value0, value1, value2);
	}

	/**
	 * 正在前往 EE2 区域 / Moving to Area EE2
	 */
	public static final SM_SYSTEM_MESSAGE STR_TP_EE2_TP0_L_TOEE2 = new SM_SYSTEM_MESSAGE(1300730);

	/**
	 * 正在前往地城 D3 / Moving to Dungeon D3
	 */
	public static final SM_SYSTEM_MESSAGE STR_TP_EE2_TP0_L_TOD3_DUN = new SM_SYSTEM_MESSAGE(1300731);

	/**
	 * 正在前往副本 / Moving to the Instanced Dungeon
	 */
	public static final SM_SYSTEM_MESSAGE STR_TP_EE2_TP0_L_TOINSTANT_DUN = new SM_SYSTEM_MESSAGE(1300732);

	/**
	 * 正在前往艾特南 / Moving to Eltnen
	 */
	public static final SM_SYSTEM_MESSAGE STR_TP_LF1A_TP0_L_TOLF2 = new SM_SYSTEM_MESSAGE(1300733);

	/**
	 * 正在前往贝特隆 / Moving to Verteron
	 */
	public static final SM_SYSTEM_MESSAGE STR_TP_LF2_TP0_L_TOLF1A = new SM_SYSTEM_MESSAGE(1300734);

	/**
	 * 你：cannot use any items while flying。 / You cannot use any items while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLYING_DISABLE_1 = new SM_SYSTEM_MESSAGE(1300735);

	/**
	 * 你：cannot use the skill while flying。 / You cannot use the skill while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLYING_DISABLE_2 = new SM_SYSTEM_MESSAGE(1300736);

	/**
	 * 你：have died。 / You have died
	 */
	public static final SM_SYSTEM_MESSAGE STR_DEATH_MESSAGE_ME = new SM_SYSTEM_MESSAGE(1300737);

	/**
	 * 你：have resurrected。 / You have resurrected
	 */
	public static final SM_SYSTEM_MESSAGE STR_REBIRTH_MASSAGE_ME = new SM_SYSTEM_MESSAGE(1300738);

	 /**
	  * %0 has defeated %1。
	  * %0 has defeated %1
	  */
	public static SM_SYSTEM_MESSAGE STR_KILLMSG(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300739, value0, value1);
	}

	/**
	 * @param value0 复活等待时间：%0 秒 / Resurrection wait time: %0 sec
	 */
	public static SM_SYSTEM_MESSAGE STR_WATINGTIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300740, value0);
	}

	/**
	 * Use a skill to resurrect
	 */
	public static final SM_SYSTEM_MESSAGE STR_RESURRECT_DIALOG__SKILL = new SM_SYSTEM_MESSAGE(1300741);

	/**
	 * Use an item to resurrect
	 */
	public static final SM_SYSTEM_MESSAGE STR_RESURRECT_DIALOG__ITEM = new SM_SYSTEM_MESSAGE(1300742);

	/**
	 * 按“确定”复活 / Press "OK" to resurrect
	 */
	public static final SM_SYSTEM_MESSAGE STR_RESURRECT_DIALOG__BIND = new SM_SYSTEM_MESSAGE(1300743);

	/**
	 * @param value0 其 will 为 cancelled 若 youdonotpress 其在 %0minutes。 / It will be cancelled if you do not press it in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_RESURRECTOTHER_DIALOG__5MIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300744, value0);
	}

	/**
	 * @param value0 其 will 为 cancelled 若 youdonotpress 其在 %0minutes。 / It will be cancelled if you do not press it in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_RESURRECT_DIALOG__5MIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300745, value0);
	}

	/**
	 * @param value0 Youwill 为 resurrected 在 registered 绑定 point 若 youdonotpress 其在 %0minutes。 / You will be resurrected at the registered bind point if you do not press it in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_RESURRECT_DIALOG__30MIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300746, value0);
	}

	/**
	 * 位于难以发现的位置。 / It is at a hard-to-find location
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIND_POS_UNKNOWN_NAME = new SM_SYSTEM_MESSAGE(1300747);

	 /**
	  * %0 is at the position indicated on the map。
	  * %0 is at the position indicated on the map
	  */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_SUBZONE_FOUND(String value0) {
		return new SM_SYSTEM_MESSAGE(1300748, value0);
	}

	/**
	 * @param value0 %0 在地图所示位置，但找不到通往那里的路径。 / %0 is where it is indicated on the map, but the path leading to it cannot be found
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_TOO_FAR_FROM_SUBZONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300749, value0);
	}

	 /**
	  * %0 is at the position indicated on the map。
	  * %0 is at the position indicated on the map
	  */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_NPC_FOUND(String value0) {
		return new SM_SYSTEM_MESSAGE(1300750, value0);
	}

	/**
	 * @param value0 %0 在地图所示位置，但找不到通往那里的路径。 / %0 is where it is indicated on the map, but the path leading to it cannot be found
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_TOO_FAR_FROM_NPC(String value0) {
		return new SM_SYSTEM_MESSAGE(1300751, value0);
	}

	/**
	 * @param value0 %0 位于难以发现的位置。 / %0 is at a hard-to-find location
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_NO_NPC_IN_THIS_WORLD(String value0) {
		return new SM_SYSTEM_MESSAGE(1300752, value0);
	}

	/**
	 * Searching for the location. Please wait (max. 30 seconds)
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIND_POS_FINDING_PLEASE_WAIT = new SM_SYSTEM_MESSAGE(1300753);

	/**
	 * %0 在 %1 区域。 / %0 is in the %1 region
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_NPC_FOUND_IN_OTHER_WORLD(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300754, value0, value1);
	}

	/**
	 * 你：cannot quit the game during the battle。 / You cannot quit the game during the battle
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_QUIT = new SM_SYSTEM_MESSAGE(1300755);

	 /**
	  * one-way Rift into Elysea has appeared。
	  * A one-way Rift into Elysea has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIGHT_SIDE_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1300756);

	/**
	 * @param num0 你：spent %num0 Kinah。 / You spent %num0 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_USEMONEY(int num0) {
		return new SM_SYSTEM_MESSAGE(1300757, num0);
	}

	/**
	 * 交易失败 / Trade Failed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TITLE_TRADE_FAIL = new SM_SYSTEM_MESSAGE(1300758);

	/**
	 * 你：do not have enough Kinah。 / You do not have enough Kinah
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300759);

	/**
	 * @param num0 You 可列表 up 到 %num0 物品。 / You can list up to %num0 items
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_MAX_ITEM_COUNT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300760, num0);
	}

	/**
	 * 此物品无法为 traded。 / This item cannot be traded
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_EXCHANGE = new SM_SYSTEM_MESSAGE(1300761);

	/**
	 * You 无法 trade 作为 yourinventory 为 full。 / You cannot trade as your inventory is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FULL_INVENTORY = new SM_SYSTEM_MESSAGE(1300762);

	/**
	 * 无法再登记更多物品。 / You cannot register any more items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FULL_BASKET = new SM_SYSTEM_MESSAGE(1300763);

	/**
	 * 你：already have this limited possession item。 / You already have this limited possession item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_OWNED_LORE_ITEM = new SM_SYSTEM_MESSAGE(1300764);

	/**
	 * 确认登记 / Confirm Registration
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_MSG_TITLE_REGISTER_OK = new SM_SYSTEM_MESSAGE(1300765);

	/**
	 * 登记失败 / Registration Failed
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_MSG_TITLE_REGISTER_ERROR = new SM_SYSTEM_MESSAGE(1300766);

	/**
	 * Youdonot 有足够基纳到 payfee。 / You do not have enough Kinah to pay the fee
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_MSG_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1300767);

	/**
	 * 你：entered the PvP zone。 / You entered the PvP zone
	 */
	public static final SM_SYSTEM_MESSAGE STR_PVP_ZONE_ENTERED = new SM_SYSTEM_MESSAGE(1300768);

	/**
	 * 你：left the PvP zone。 / You left the PvP zone
	 */
	public static final SM_SYSTEM_MESSAGE STR_PVP_ZONE_EXITED = new SM_SYSTEM_MESSAGE(1300769);

	/**
	 * 决斗开始！ / Start Duel!
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_START = new SM_SYSTEM_MESSAGE(1300770);

	/**
	 * 决斗结束 / Stop Duel
	 */
	public static final SM_SYSTEM_MESSAGE STR_DUEL_STOP = new SM_SYSTEM_MESSAGE(1300771);

	/**
	 * 你：cannot destroy equipped items。 / You cannot destroy equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_DESTROY_EQUIP_ITEM = new SM_SYSTEM_MESSAGE(1300772);

	/**
	 * Permanently Acquired Title
	 */
	public static final SM_SYSTEM_MESSAGE STR_TITLE_PERMANENT = new SM_SYSTEM_MESSAGE(1300773);

	/**
	 * Temporarily Acquired Title
	 */
	public static final SM_SYSTEM_MESSAGE STR_TITLE_TEMPORARY = new SM_SYSTEM_MESSAGE(1300774);

	/**
	 * 0技能等级1 / %0 skill (Level %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_TITLE_BONUS_SKILL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300775, value0, value1);
	}

	/**
	 * 你已在销毁另一件物品。 / You are already destroying another item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ALREADY_DESTROY_ITEM = new SM_SYSTEM_MESSAGE(1300776);

	/**
	 * @param value0 开始与 %0 决斗 / Starting the duel with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DUEL_START(String value0) {
		return new SM_SYSTEM_MESSAGE(1300777, value0);
	}

	 /**
	  * World Map is currently being prepared。
	  * The World Map is currently being prepared
	  */
	public static final SM_SYSTEM_MESSAGE STR_WORLDMAP_UNDER_CONSTRUCTION = new SM_SYSTEM_MESSAGE(1300778);

	/**
	 * @param value0 当前截图已保存至 %0 / The current screenshot was saved in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_PRINT_SCREEN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300779, value0);
	}

	/**
	 * 你已在与他人聊天。 / You are already chatting with someone
	 */
	public static final SM_SYSTEM_MESSAGE STR_ALREADY_TALKING_TO_SOMEONE = new SM_SYSTEM_MESSAGE(1300780);

	/**
	 * %0 掷骰得到 %num1。 / %0 rolled the dice and got a %num1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_ROLLED(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1300781, value0, num1);
	}

	/**
	 * @param value0 %0 放弃掷骰。 / %0 gave up rolling the dice
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_PASSED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300782, value0);
	}

	/**
	 * @param value0 %0 无权掷骰。 / %0 does not have the right to roll the dice
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_UNAUTHORIZED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300783, value0);
	}

	/**
	 * @param value0 你：have purchased %0。 / You have purchased %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_BUY_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300784, value0);
	}

	/**
	 * 你：have purchased %1 %0s。 / You have purchased %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_BUY_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300785, value1, value0s);
	}

	/**
	 * @param value0 你：have sold %0。 / You have sold %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SELL_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300786, value0);
	}

	/**
	 * 你：have sold %1 %0s。 / You have sold %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SELL_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300787, value1, value0s);
	}

	/**
	 * @param value0 你：have crafted %0。 / You have crafted %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBINE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300788, value0);
	}

	/**
	 * 你：have crafted %1 %0s。 / You have crafted %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBINE_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300789, value1, value0s);
	}

	/**
	 * @param value0 %0 有已 sold。 / %0 has been sold
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SOLDOUT_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300790, value0);
	}

	/**
	 * 已售出 %1 个 %0。 / %1 %0s have been sold
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SOLDOUT_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300791, value1, value0s);
	}

	/**
	 * @param value0 你：have discarded %0。 / You have discarded %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DISCARD_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300792, value0);
	}

	/**
	 * 你：have discarded %1 %0s。 / You have discarded %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DISCARD_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1300793, value1, value0s);
	}

	 /**
	  * 你的建造者等级过低，无法打开所选窗口。
	  * Your builder level is too low to open the selected window
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_OPEN_DIALOG_BY_BUILDER_LEVEL = new SM_SYSTEM_MESSAGE(1300794);

	/**
	 * @param value0 当前 %0 无法接收好友请求。 / Currently, %0 cannot receive any friend requests
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDY_CANT_ADD_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1300795, value0);
	}

	/**
	 * 当前不接受申诉。 / Petitions are not accepted right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PETITION_STATUS_NO_IDLE = new SM_SYSTEM_MESSAGE(1300796);

	/**
	 * 不在申诉提交时段。 / It is outside the petition submission hours
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PETITION_STATUS_NO_SUBMIT = new SM_SYSTEM_MESSAGE(1300797);

	/**
	 * 你：cannot open a private store while flying。 / You cannot open a private store while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DISABLED_IN_FLY_MODE = new SM_SYSTEM_MESSAGE(1300798);

	/**
	 * 你：cannot open a private store while hiding。 / You cannot open a private store while hiding
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DISABLED_IN_HIDDEN_MODE = new SM_SYSTEM_MESSAGE(1401969);

	/**
	 * 因你处于隐身状态，个人商店已自动关闭。 / Your private store closed automatically because you are currently hiding
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CLOSED_FOR_HIDDEN_MODE = new SM_SYSTEM_MESSAGE(1401970);

	/**
	 * 你：cannot use this Kisk。 / You cannot use this Kisk
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_BINDSTONE_HAVE_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(
			1300799);

	/**
	 * 你：cannot use the Kisk here。 / You cannot use the Kisk here
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_BINDSTONE_FAR_FROM_NPC = new SM_SYSTEM_MESSAGE(1300800);

	/**
	 * 你：cannot use the Kisk。 / You cannot use the Kisk
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_BINDSTONE_NOT_BINDSTONE = new SM_SYSTEM_MESSAGE(1300801);

	 /**
	  * Kisk has been destroyed。
	  * The Kisk has been destroyed
	  */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_IS_DESTROYED = new SM_SYSTEM_MESSAGE(1300802);

	 /**
	  * Kisk has been dismantled。
	  * The Kisk has been dismantled
	  */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_IS_REMOVED = new SM_SYSTEM_MESSAGE(1300803);

	/**
	 * You 无法 installKisk 作为其为 tooclose 到 Artifact。 / You cannot install the Kisk as it is too close to an Artifact
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_BINDSTONE_ITEM_NOT_PROPER_AREA = new SM_SYSTEM_MESSAGE(
			1300804);

	/**
	 * You 可仅 useKisk 当 PvP 为在。 / You can only use the Kisk when the PvP is On
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_BINDSTONE_ITEM_NOT_PROPER_TIME = new SM_SYSTEM_MESSAGE(
			1300805);

	/**
	 * 你：cannot use a Kisk while flying。 / You cannot use a Kisk while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_BINDSTONE_ITEM_WHILE_FLYING = new SM_SYSTEM_MESSAGE(1300806);

	/**
	 * @param value0 你因下列原因，%0 分钟内禁止移动。 / You are not allowed to move for %0 minutes for the following reason(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_ENABLE_NO_MOVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300807, value0);
	}

	/**
	 * @param value0 你因下列原因，%0 分钟内禁止聊天。 / You are not allowed to chat for %0 minutes for the following reason(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_ENABLE_NO_CHAT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300808, value0);
	}

	/**
	 * @param value0 你因下列原因，%0 分钟内禁止开设个人商店。 / You are not allowed to open the private store for %0 minutes for the following reason(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_ENABLE_NO_SHOP(String value0) {
		return new SM_SYSTEM_MESSAGE(1300809, value0);
	}

	/**
	 * 你现已可以移动。 / You are now allowed to move
	 */
	public static final SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_DISABLE_NO_MOVE = new SM_SYSTEM_MESSAGE(1300810);

	/**
	 * 你现已可以聊天。 / You are now allowed to chat
	 */
	public static final SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_DISABLE_NO_CHAT = new SM_SYSTEM_MESSAGE(1300811);

	/**
	 * 你：now allowed to open a private store。 / You now allowed to open a private store
	 */
	public static final SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_DISABLE_NO_SHOP = new SM_SYSTEM_MESSAGE(1300812);

	/**
	 * @param value0 你当前无法移动，禁言/禁足剩余 %0 分钟。 / You are currently unable to move. There are %0 minute(s) left in your ban
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_IN_NO_MOVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300813, value0);
	}

	/**
	 * @param value0 你当前无法聊天，剩余 %0 分钟。 / You are currently unable to chat. There are %0 minute(s) left in your ban
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_IN_NO_CHAT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300814, value0);
	}

	/**
	 * @param value0 你当前无法开设个人商店，限制剩余 %0 分钟。 / You are currently unable to open a private store. There are %0 minute(s) left on your ban
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAME_BLOCK_IN_NO_SHOP(String value0) {
		return new SM_SYSTEM_MESSAGE(1300815, value0);
	}

	/**
	 * 未知错误 / Unknown Error
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_UNKNOWN = new SM_SYSTEM_MESSAGE(1300816);

	/**
	 * Database Error
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_DATABASE_FAIL = new SM_SYSTEM_MESSAGE(1300817);

	/**
	 * Please select the type of your petition
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_NO_SELECT = new SM_SYSTEM_MESSAGE(1300818);

	/**
	 * 请输入申诉内容。 / Please enter your petition
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_NO_INPUT = new SM_SYSTEM_MESSAGE(1300819);

	/**
	 * 你无权评价客服请求的回复。 / You are not allowed to evaluate the reply of a Support Petition
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_CANT_EVAL = new SM_SYSTEM_MESSAGE(1300820);

	/**
	 * 有未完成的回复。 / There are unfinished replies
	 */
	public static final SM_SYSTEM_MESSAGE STR_GM_POLL_ANSWERS_NOT_COMPLETED = new SM_SYSTEM_MESSAGE(1300821);

	/**
	 * Abandon Selected Quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ABANDON = new SM_SYSTEM_MESSAGE(1300822);

	/**
	 * Invalid target
	 */
	public static final SM_SYSTEM_MESSAGE STR_INVALID_TARGET = new SM_SYSTEM_MESSAGE(1300823);

	/**
	 * You 无法 use 此 menu 当 you 为 dead。 / You cannot use this menu when you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_IN_DEAD_STATE = new SM_SYSTEM_MESSAGE(1300824);

	/**
	 * You 无法 destroy 物品 whileyou 为 corpse。 / You cannot destroy items while you are a corpse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DEAD_BODY_CANT_DESTROY_ITEM = new SM_SYSTEM_MESSAGE(1300825);

	/**
	 * 你：cannot use certain chat functions while you are dead。 / You cannot use certain chat functions while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_CHAT_IN_DEAD_STATE = new SM_SYSTEM_MESSAGE(1300826);

	/**
	 * /CreateChannel [ChannelName] [OptionalPassword]: Creates a private channel.
	 * /JoinChannel [ChannelName] [Password]: Enters an existing private channel.
	 * Password required if one was set. /LeaveChannel [ChannelNumber]: Leaves a
	 * private channel. /ChannelMemberInfo [ChannelNumber]: Shows who's in a
	 * 频道 / channel.Info [ChannelNumber]: Shows information for a channel you
	 * are in. /ChannelBanInfo [ChannelNumber]: Shows who is banned from a channel.
	 * /BanFromChannel [ChannelNumber] [CharacterName]: The channel owner can
	 * permanently remove a character from the channel. /UnbanFromChannel
	 * [ChannelNumber] [CharacterName]: The channel owner can reinstate a banned
	 * character's access to a channel. /ChangeChannelLeader [ChannelNumber]
	 * [CharacterName]: The channel owner makes another character the channel owner.
	 * /ChangeChannelPassword [ChannelNumber] [Password]: The channel owner can
	 * change the password. /ChannelHelp: Shows the commands available for channels.
	 */
	public static final SM_SYSTEM_MESSAGE STR_CURRENT_STANCE_DOES_NOT_SUPPORTS = new SM_SYSTEM_MESSAGE(1300827);

	/**
	 * 你距离过近，无法攻击。 / You are too close to attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_CLOSE_TO_ATTACK = new SM_SYSTEM_MESSAGE(1300828);

	/**
	 * 已有人在拾取该物品。 / Someone is already looting that
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOOT_FAIL_ONLOOTING = new SM_SYSTEM_MESSAGE(1300829);

	/**
	 * 距离过远：the target。 / You are too far from the target
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOOT_FAIL_TOO_FAR = new SM_SYSTEM_MESSAGE(1300830);

	/**
	 * Youdonot 有足够基纳到 expandcube。 / You do not have enough Kinah to expand the cube
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_EXPAND_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1300831);

	/**
	 * Cannot find the emblem.bmp file in the Aion Game folder
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WARN_NO_EMBLEM_FILE = new SM_SYSTEM_MESSAGE(1300832);

	 /**
	  * %0 cannot be socketed with Manastone。
	  * %0 cannot be socketed with Manastone
	  */
	public static SM_SYSTEM_MESSAGE STR_GIVE_ITEM_PROC_NOT_ADD_PROC(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1300833, descriptionId);
	}

	/**
	 * 你：must pass the Expert promotion test in order to be promoted。 / You must pass the Expert promotion test in order to be promoted
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_CANT_EXTEND_MONEY = new SM_SYSTEM_MESSAGE(1300834);

	/**
	 * Upload 的军团 emblemfile 到 server 成功。 / Upload of the Legion emblem file to the server successful
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WARN_SUCCESS_UPLOAD_EMBLEM = new SM_SYSTEM_MESSAGE(1300835);

	/**
	 * Upload of the Legion emblem file to the server failed
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WARN_FAILURE_UPLOAD_EMBLEM = new SM_SYSTEM_MESSAGE(1300836);

	/**
	 * Failed to read the Legion emblem file
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WARN_CORRUPT_EMBLEM_FILE = new SM_SYSTEM_MESSAGE(1300837);

	 /**
	  * size of the Legion emblem file is not 24bit 256X256。
	  * The size of the Legion emblem file is not 24bit 256X256
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_WARN_IMPROPER_SIZE_EMBLEM_FILE = new SM_SYSTEM_MESSAGE(1300838);

	/**
	 * Skill Penalty
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_PENALTY_TITLE = new SM_SYSTEM_MESSAGE(1300839);

	/**
	 * 降低闪避、招架与格挡。 / Reduces Evasion, Parry, and Block
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_PENALTY_ACTIVED_EFEND = new SM_SYSTEM_MESSAGE(1300840);

	/**
	 * 降低物理防御与魔法抗性。 / Reduces Physical Defense and Magical Resistance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_PENALTY_DEFEND = new SM_SYSTEM_MESSAGE(1300841);

	/**
	 * @param value0 你：have joined the %0 Channel。 / You have joined the %0 Channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_JOIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300842, value0);
	}

	/**
	 * @param value0 你：have left the %0 Channel。 / You have left the %0 Channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_LEAVE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300843, value0);
	}

	/**
	 * @param value0 你：have created the %0 Channel。 / You have created the %0 Channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_CREATE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300844, value0);
	}

	 /**
	  * 你的私人频道“%1”已作为频道 %0 打开。
	  * Your private channel "%1"% is open as Channel No. %0
	  */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CREATE_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1300845, value0);
	}

	/**
	 * 你：cannot open or join any more private channels。 / You cannot open or join any more private channels
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CREATE_FAILED_MAXROOM = new SM_SYSTEM_MESSAGE(1300846);

	/**
	 * 密码错误。 / Incorrect password
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CREATE_FAILED_WRONG_PASSWORD = new SM_SYSTEM_MESSAGE(1300847);

	/**
	 * Youdonot 有足够 DP 用于 conversion。 / You do not have enough DP for conversion
	 */
	public static final SM_SYSTEM_MESSAGE STR_CONVERT_SKILL_NOT_ENOUGH_DP = new SM_SYSTEM_MESSAGE(1300848);

	/**
	 * 你：must have learned the skill to activate it。 / You must have learned the skill to activate it
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_MSG_CANT_WORK = new SM_SYSTEM_MESSAGE(1300849);

	/**
	 * You 有加入 privatechannel%1 在 Channel%0.Confirm 若 youselectedchannel 在 chattingtaboption。 / You have joined the private channel %1 at Channel %0. Confirm if you selected the channel in the chatting tab option
	 */
	public static SM_SYSTEM_MESSAGE STR_CAHT_ROOM_JOIN_SUCCESS(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1300850, value1, value0);
	}

	/**
	 * That private channel does not exist
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_FAIL_ROOM_NOT_FOUND = new SM_SYSTEM_MESSAGE(1300851);

	/**
	 * You 无法 enterprivatechannel 作为其为 full。 / You cannot enter the private channel as it is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_FAIL_TOO_MANY_ROOM = new SM_SYSTEM_MESSAGE(1300852);

	/**
	 * An expelled character cannot enter the same private channel again
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_FAIL_BANNED_USER = new SM_SYSTEM_MESSAGE(1300853);

	/**
	 * Youneed 到 enterpassword 到 joinprivatechannel%0.Pleaseenter 其 accurately ,在 format 的' /JoinChannel[ChannelName][password] '。 / You need to enter a password to join the private channel %0. Please enter it accurately, in the format of '/JoinChannel [ChannelName] [password]'
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_FAIL_WRONG_PASSWORD(String value0) {
		return new SM_SYSTEM_MESSAGE(1300854, value0);
	}

	/**
	 * @param value0 你：cannot enter the private channel (%0) as it is full。 / You cannot enter the private channel (%0) as it is full
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_FAIL_ROOM_FULL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300855, value0);
	}

	 /**
	  * %0 has entered the private channel。
	  * %0 has entered the private channel
	  */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_NOTIFY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300856, value0);
	}

	/**
	 * You 有加入 privatechannel%1 在 Channel%0.Confirm 若 youselectedchannel 在 chattingtaboption。 / You have joined the private channel %1 at Channel %0. Confirm if you selected the channel in the chatting tab option
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_JOIN_NOTIFY_SELF(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1300857, value1, value0);
	}

	/**
	 * @param value0 你不in the private channel %0。 / You are not in the private channel %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_LEAVE_FAIL_NOT_A_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300858, value0);
	}

	 /**
	  * %0 has left the private channel。
	  * %0 has left the private channel
	  */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_LEAVE_NOTIFY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300859, value0);
	}

	/**
	 * @param value0 你：have left the private channel %0。 / You have left the private channel %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_LEAVE_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1300860, value0);
	}

	 /**
	  * %0 is the new channel leader。
	  * %0 is the new channel leader
	  */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_NEW_ADMIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1300861, value0);
	}

	/**
	 * 你：have no authority。 / You have no authority
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_ADMIN_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(1300862);

	/**
	 * Cannot find the character in this private channel
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_MEMBER_VOID = new SM_SYSTEM_MESSAGE(1300863);

	/**
	 * @param value0 %0 有已踢出 out 的 privatechannel。 / %0 has been kicked out of the private channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_BAN_MEMBER_BANNED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300864, value0);
	}

	/**
	 * @param value0 你：have been kicked out of the private channel %0。 / You have been kicked out of the private channel %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_BAN_SELF_BANNED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300865, value0);
	}

	 /**
	  * password of the private channel has been changed to %0。
	  * The password of the private channel has been changed to %0
	  */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_PASSWORD_CHANGED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300866, value0);
	}

	/**
	 * @param value0 密码最长 %0 个字符。 / The maximum password length is %0 characters
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_PASSWORD_TOO_LONG(String value0) {
		return new SM_SYSTEM_MESSAGE(1300867, value0);
	}

	 /**
	  * name of that private channel is invalid。
	  * The name of that private channel is invalid
	  */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_INVALID_CHANNEL_NAME = new SM_SYSTEM_MESSAGE(1300868);

	/**
	 * You 有已踢出 out。 / You have been kicked out
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_KICKED_OUT = new SM_SYSTEM_MESSAGE(1300869);

	/**
	 * @param value0 %0 有已 deleted 从 Ban 列表。 / %0 has been deleted from the Ban List
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_UNBAN_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1300870, value0);
	}

	/**
	 * channel 有已设置作为 public。 / The channel has been set as public
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_PROPERTYCHANGE_TOPUBLIC = new SM_SYSTEM_MESSAGE(1300871);

	/**
	 * channel 有已设置作为 private。 / The channel has been set as private
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_PROPERTYCHANGE_TOPRIVATE = new SM_SYSTEM_MESSAGE(1300872);

	/**
	 * @param value0 允许的最大用户数现设为 %0。 / Maximum number of users allowed is now set to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_PROPERTYCHANGE_MAXMEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300873, value0);
	}

	/**
	 * 你未加入任何频道。 / You are not participating in any channels
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_NO_JOINED_CHATROOM = new SM_SYSTEM_MESSAGE(1300874);

	/**
	 * /CreateChannel [ChannelName] [OptionalPassword]: Creates a private channel.
	 * /JoinChannel [ChannelName] [Password]: Enters an existing private channel.
	 * Password required if one was set. /LeaveChannel [ChannelNumber]: Leaves a
	 * private channel. /ChannelMemberInfo [ChannelNumber]: Shows who's in a
	 * 频道 / channel.Info [ChannelNumber]: Shows information for a channel you
	 * are in. /ChannelBanInfo [ChannelNumber]: Shows who is banned from a channel.
	 * /BanFromChannel [ChannelNumber] [CharacterName]: The channel owner can
	 * permanently remove a character from the channel. /UnbanFromChannel
	 * [ChannelNumber] [CharacterName]: The channel owner can reinstate a banned
	 * character's access to a channel. /ChangeChannelLeader [ChannelNumber]
	 * [CharacterName]: The channel owner makes another character the channel owner.
	 * /ChangeChannelPassword [ChannelNumber] [Password]: The channel owner can
	 * change the password. /ChannelHelp: Shows the commands available for channels.
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_HELP = new SM_SYSTEM_MESSAGE(1300875);

	/**
	 * 你：cannot learn a design written in an incomprehensible language。 / You cannot learn a design written in an incomprehensible language
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFTRECIPE_RACE_CHECK = new SM_SYSTEM_MESSAGE(1300876);

	/**
	 * An express courier has already arrived
	 */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_ALREADY_SUMMONED = new SM_SYSTEM_MESSAGE(1300877);

	/**
	 * Please wait for a while before you call for the courier again
	 */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_UNABLE_IN_COOLTIME = new SM_SYSTEM_MESSAGE(1300878);

	/**
	 * 你：cannot call a courierwhile flying。 / You cannot call a courierwhile flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_UNABLE_IN_FLIGHT = new SM_SYSTEM_MESSAGE(1300879);

	/**
	 * 你：cannot call a courier here。 / You cannot call a courier here
	 */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_UNABLE_POSITION = new SM_SYSTEM_MESSAGE(1300880);

	/**
	 * That character does not exist
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_CHARACTER_NONEXISIT = new SM_SYSTEM_MESSAGE(1300881);

	/**
	 * 该玩家未登录。 / That person is not logged on
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_NO_OFFLINE_CHARACTER = new SM_SYSTEM_MESSAGE(1300882);

	/**
	 * character 为已经在 yourFriends 列表。 / The character is already on your Friends List
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_ALREADY_IN_LIST = new SM_SYSTEM_MESSAGE(1300883);

	 /**
	  * blocked character cannot also be a Friend。
	  * A blocked character cannot also be a Friend
	  */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_NO_BLOCKED_CHARACTER = new SM_SYSTEM_MESSAGE(1300884);

	/**
	 * @param value0 你：have added %0 to your Friends List。 / You have added %0 to your Friends List
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDYLIST_ADD_BUDDY_ACCEPTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300885, value0);
	}

	/**
	 * @param value0 %0 拒绝了你的好友请求。 / %0 declined your friend request
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDYLIST_ADD_BUDDY_REJECTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300886, value0);
	}

	 /**
	  * 你的Friends List is full。
	  * 你的Friends List已满。 / Your Friends List is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_LIST_FULL = new SM_SYSTEM_MESSAGE(1300887);

	/**
	 * @param value0 你：have removed %0 from your Friends List。 / You have removed %0 from your Friends List
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDYLIST_REMOVE_CHARACTER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300888, value0);
	}

	 /**
	  * character is not on your Friends List。
	  * The character is not on your Friends List
	  */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_NOT_IN_LIST = new SM_SYSTEM_MESSAGE(1300889);

	 /**
	  * 你的好友 %0 已登录。
	  * 你的好友 %0 已登录。 / Your friend %0 has logged in
	  */
	public static SM_SYSTEM_MESSAGE STR_BUDDYLIST_BUDDY_LOGON(String value0) {
		return new SM_SYSTEM_MESSAGE(1300890, value0);
	}

	/**
	 * You 无法 blockcharacter 谁为 currently 在 yourFriends 列表。 / You cannot block a character who is currently on your Friends List
	 */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_NO_BUDDY = new SM_SYSTEM_MESSAGE(1300891);

	/**
	 * @param value0 你：have blocked %0。 / You have blocked %0
	 */
	public static SM_SYSTEM_MESSAGE STR_BLOCKLIST_ADD_BLOCKED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300892, value0);
	}

	/**
	 * That character does not exist
	 */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_CHARACTER_NONEXIST = new SM_SYSTEM_MESSAGE(1300893);

	/**
	 * That character is already blocked
	 */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_ALREADY_BLOCKED = new SM_SYSTEM_MESSAGE(1300894);

	/**
	 * Enter the name of the character you want to block
	 */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_ENTER_CHARACTER_NAME = new SM_SYSTEM_MESSAGE(1300895);

	/**
	 * @param value0 你：have unblocked %0。 / You have unblocked %0
	 */
	public static SM_SYSTEM_MESSAGE STR_BLOCKLIST_REMOVE_FROM_LIST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300896, value0);
	}

	 /**
	  * character is not blocked。
	  * The character is not blocked
	  */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_NOT_IN_LIST = new SM_SYSTEM_MESSAGE(1300897);

	/**
	 * 你：must level up to raise your skill level。 / You must level up to raise your skill level
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_INFO_MAXPOINT_UP = new SM_SYSTEM_MESSAGE(1300898);

	/**
	 * Express mail has arrived
	 */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_NOTIFY = new SM_SYSTEM_MESSAGE(1300899);

	/**
	 * 频道信息0名称1Password2Usersnum3 / Channel information: %0, Name: %1, Password: %2, Users: %num3
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_INFO_FORMAT(String value0, String value1, String value2, int num3) {
		return new SM_SYSTEM_MESSAGE(1300900, value0, value1, value2, num3);
	}

	/**
	 * 频道信息0名称1Usersnum2 / Channel information: %0, Name: %1, Users: %num2
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_INFO_FORMAT_NOPASSWORD(String value0, String value1, int num2) {
		return new SM_SYSTEM_MESSAGE(1300901, value0, value1, num2);
	}

	/**
	 * 你：have been disconnected from the server。 / You have been disconnected from the server
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_AUTH_CONNECTION_LOST = new SM_SYSTEM_MESSAGE(1300902);

	/**
	 * privatechannel 带相同名称已经存在。 / A private channel with the same name already exists
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_ROOM_EXISTS = new SM_SYSTEM_MESSAGE(1300903);

	/**
	 * That private channel already exists
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CHANNEL_EXISTS = new SM_SYSTEM_MESSAGE(1300904);

	/**
	 * 你：have already joined the private channel。 / You have already joined the private channel
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_ALREADY_JOINED_CHANNEL = new SM_SYSTEM_MESSAGE(1300905);

	 /**
	  * character has been banned from this channel。
	  * The character has been banned from this channel
	  */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_ALREADY_BANNED_MEMBER = new SM_SYSTEM_MESSAGE(1300906);

	/**
	 * 你：cannot kick yourself out of the channel。 / You cannot kick yourself out of the channel
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CANNOT_BAN_SELF = new SM_SYSTEM_MESSAGE(1300907);

	/**
	 * 你：cannot nominate yourself as a room master。 / You cannot nominate yourself as a room master
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CANNOT_PROMOTE_SELF = new SM_SYSTEM_MESSAGE(1300908);

	 /**
	  * character is not on the Ban List。
	  * The character is not on the Ban List
	  */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_NOT_A_BANNED_CHARACTER = new SM_SYSTEM_MESSAGE(1300909);

	/**
	 * Requires the %0 Equip Skill
	 */
	public static SM_SYSTEM_MESSAGE STR_TOOLTIP_NEED_MASTERY_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1300910, value0);
	}

	 /**
	  * %0 has sent you a friend request。
	  * %0 has sent you a friend request
	  */
	public static SM_SYSTEM_MESSAGE STR_BUDDY_REQUEST_TO_ADD(String value0) {
		return new SM_SYSTEM_MESSAGE(1300911, value0);
	}

	 /**
	  * 你的Block List is full。
	  * 你的Block List已满。 / Your Block List is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_LIST_FULL = new SM_SYSTEM_MESSAGE(1300912);

	/**
	 * 你：cannot block yourself。 / You cannot block yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_BLOCKLIST_CANNOT_BLOCK_SELF = new SM_SYSTEM_MESSAGE(1300913);

	/**
	 * 你：cannot use a Macro yet。 / You cannot use a Macro yet
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_READY_TO_USE = new SM_SYSTEM_MESSAGE(1300914);

	 /**
	  * %0 has logged in。
	  * %0 has logged in
	  */
	public static SM_SYSTEM_MESSAGE STR_NOTIFY_LOGIN_BUDDY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300915, value0);
	}

	 /**
	  * %0 has logged out。
	  * %0 has logged out
	  */
	public static SM_SYSTEM_MESSAGE STR_NOTIFY_LOGOFF_BUDDY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300916, value0);
	}

	/**
	 * @param value0 %0 有 deletedyou 从其 Friends 列表。 / %0 has deleted you from their Friends List
	 */
	public static SM_SYSTEM_MESSAGE STR_NOTIFY_DELETE_BUDDY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300917, value0);
	}

	 /**
	  * selected character is already dead。
	  * The selected character is already dead
	  */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_DEAD = new SM_SYSTEM_MESSAGE(1300918);

	/**
	 * 建造命令 %0 需要 %1 个参数。 / Builder Command %0 requires %1 parameters
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_BC_NOT_ENOUGH_PARAMETER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300919, value0, value1);
	}

	/**
	 * You 无法 equip 或移除物品 while 在 action。 / You cannot equip or remove items while in action
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_EQUIP_ITEM_IN_ACTION = new SM_SYSTEM_MESSAGE(1300920);

	/**
	 * 0等级12 / %0: Level %1 (%2)
	 */
	public static SM_SYSTEM_MESSAGE STR_MACRO_MSG_PROCESS(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1300921, value0, value1, value2);
	}

	/**
	 * Macro: Cannot find the skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_FIND_SKILL = new SM_SYSTEM_MESSAGE(1300922);

	/**
	 * 已收到你的申诉。\n 回执号 %0。\n 等待列表 %1 人，预计等待约 %2。\n 今日已提交 %3 次，还可提交 %4 次。谢谢！ / Your petition has been received.\nThe receipt number is %0.\nThere are %1 users on the waiting list, and the approximate waiting time is %2.\nYou have submitted %3 petitions today, and there are %4 more petitions left. Thank you!
	 */
	public static SM_SYSTEM_MESSAGE STR_PETITION_SUBMIT_MESSAGE() {
		return new SM_SYSTEM_MESSAGE(1300923);
	}

	/**
	 * 你的申诉有回复。 / There is a reply to your petition
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOOLTIP_PETITION_MESSAGE_NOTICE = new SM_SYSTEM_MESSAGE(1300924);

	/**
	 * 你in a chat with the GM。 / You are in a chat with the GM
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_CHAT_MESSAGE = new SM_SYSTEM_MESSAGE(1300925);

	/**
	 * 宏:无法 find 物品。 / Macro: Cannot find the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_FIND_ITEM = new SM_SYSTEM_MESSAGE(1300926);

	/**
	 * 你：do not have enough Abyss Points。 / You do not have enough Abyss Points
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_ABYSSPOINT = new SM_SYSTEM_MESSAGE(1300927);

	/**
	 * 你：cannot change the channel during a battle。 / You cannot change the channel during a battle
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_CHANGE_CHANNEL_IN_COMBAT = new SM_SYSTEM_MESSAGE(1300928);

	/**
	 * 你：cannot change the channel now。 / You cannot change the channel now
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_CHANGE_CHANNEL_NOW = new SM_SYSTEM_MESSAGE(1300929);

	/**
	 * Campaign quest acquired: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MISSION_SYSTEMMSG_ACQUIRE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300930, value0);
	}

	/**
	 * 开始处罚 / Start Punishment
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAR_PUNISH_START_TIME = new SM_SYSTEM_MESSAGE(1300931);

	/**
	 * 结束处罚 / End Punishment
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAR_PUNISH_END_TIME = new SM_SYSTEM_MESSAGE(1300932);

	/**
	 * 宏: There 为 no 物品 registered 在 Quickbar。 / Macro: There is no item registered in the Quickbar
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_FIND_SHORTCUT = new SM_SYSTEM_MESSAGE(1300933);

	/**
	 * 宏:无法 find 目标。 / Macro: Cannot find the target
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_FIND_TARGET = new SM_SYSTEM_MESSAGE(1300934);

	/**
	 * Macro: The sentence cannot be parsed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_PARSE = new SM_SYSTEM_MESSAGE(1300935);

	/**
	 * @param value0m 请勿刷屏。禁言 %0 分钟。 / Please do not flood chat. Blocked for %0m
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_FLOODING_BLOCKED_1(String value0m) {
		return new SM_SYSTEM_MESSAGE(1300936, value0m);
	}

	/**
	 * 你：can use the Channel: %0 only once every %1 seconds. Time Remaining: %2 seconds。 / You can use the Channel: %0 only once every %1 seconds. Time Remaining: %2 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_FLOODING_BLOCKED_2(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1300937, value0, value1, value2);
	}

	/**
	 * @param value0 本地与交易频道已移至 %0 区域。 / Both local and trade channels were moved to the %0 area
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_LEVEL_CHANGED(String value0) {
		return new SM_SYSTEM_MESSAGE(1300938, value0);
	}

	 /**
	  * channel name must be between 2 and 10 characters。
	  * The channel name must be between 2 and 10 characters
	  */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_CHANNELNAME_SIZE_LIMIT = new SM_SYSTEM_MESSAGE(1300939);

	/**
	 * You 无法 resurrect 目标 due 到其 insufficientAbyssPoints。 / You cannot resurrect the target due to its insufficient Abyss Points
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_RESURRECT_FAILED = new SM_SYSTEM_MESSAGE(1300940);

	/**
	 * 你：have too few Abyss points to continue the battle。 / You have too few Abyss points to continue the battle
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_RESURRECT = new SM_SYSTEM_MESSAGE(1300941);

	/**
	 * 相同物品为已经 registered。 / The same item is already registered
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_ALREAY_REGIST_ITEM = new SM_SYSTEM_MESSAGE(1300942);

	/**
	 * 你：cannot register items in the private store while equipped with Stigma。 / You cannot register items in the private store while equipped with Stigma
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CANNOT_REGIST_DURING_STIGMA = new SM_SYSTEM_MESSAGE(
			1300943);

	/**
	 * You 无法 register 物品作为 you 为已经 sellingother 物品。 / You cannot register items as you are already selling other items
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_CANNOT_REGIST_DURING_SELLING = new SM_SYSTEM_MESSAGE(
			1300944);

	/**
	 * @param value0 Youreceived%0 物品作为 reward 用于 survey。 / You received %0 item as reward for the survey
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_POLL_REWARD_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300945, value0);
	}

	/**
	 * Youreceived%num1%0 物品作为 reward 用于 survey。 / You received %num1 %0 items as reward for the survey
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_POLL_REWARD_ITEM_MULTI(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1300946, num1, value0);
	}

	/**
	 * @param num0 Youreceived%num0 基纳作为 reward 用于 survey。 / You received %num0 Kinah as reward for the survey
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_POLL_REWARD_MONEY(int num0) {
		return new SM_SYSTEM_MESSAGE(1300947, num0);
	}

	/**
	 * 开始语音聊天 / Starting the voice chatting
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_START_SUCCESS = new SM_SYSTEM_MESSAGE(1300948);

	/**
	 * 启动语音聊天失败 / Failed to start the voice chatting
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_START_FAILED = new SM_SYSTEM_MESSAGE(1300949);

	/**
	 * 结束语音聊天 / Ending the voice chatting
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_FINISH_SUCCESS = new SM_SYSTEM_MESSAGE(1300950);

	/**
	 * 结束语音聊天失败 / Failed to end the voice chatting
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_FINISH_FAILED = new SM_SYSTEM_MESSAGE(1300951);

	/**
	 * 你：cannot use private channels before you change your Class。 / You cannot use private channels before you change your Class
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_REQUIREMENT_UNFULLFILLED = new SM_SYSTEM_MESSAGE(1300952);

	/**
	 * @param value0 %0 开始语音聊天 / %0 starts the voice chatting
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_START_NOTIFY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300953, value0);
	}

	/**
	 * @param value0 %0 结束语音聊天 / %0 ends the voice chatting
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_ROOM_VOICE_FINISH_NOTIFY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300954, value0);
	}

	public static final SM_SYSTEM_MESSAGE STR_MAIL_CASHITEM_BUY(int itemId) {
		return new SM_SYSTEM_MESSAGE(1300956, "[item:" + itemId + "]");
	}

	public static final SM_SYSTEM_MESSAGE STR_MAIL_CASHITEM_GIFT(int itemId) {
		return new SM_SYSTEM_MESSAGE(1300957, "[item:" + itemId + "]");
	}

	/**
	 * 此私人频道密码已移除，可直接加入。 / The password for this private channel has been removed. You can now join the channel without entering the password
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_ROOM_PASSWORD_DELETED = new SM_SYSTEM_MESSAGE(1300955);

	/**
	 * You 可仅发送 mails 到 otherusers 的 yourrace。 / You can only send mails to other users of your race
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_MSG_DIFFERENT_RACE = new SM_SYSTEM_MESSAGE(1300958);

	/**
	 * You 无法 fly 在此 area。 / You cannot fly in this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLYING_FORBIDDEN_ZONE = new SM_SYSTEM_MESSAGE(1300959);

	/**
	 * You 无法 fly 在此 area。 / You cannot fly in this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLYING_FORBIDDEN_HERE = new SM_SYSTEM_MESSAGE(1300960);

	/**
	 * Flight cooldown time has not expired yet
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLYING_TIME_NOT_READY = new SM_SYSTEM_MESSAGE(1300961);

	/**
	 * 你的申诉正在处理。 / Your petition is being processed
	 */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_OPEN_MESSAGE = new SM_SYSTEM_MESSAGE(1300962);

	/**
	 * 部分选项需重启游戏后生效。 / Some options are applied when the game is restarted
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_APPLY_OPTION_WHEN_RESTART = new SM_SYSTEM_MESSAGE(1300963);

	/**
	 * You 无法 usespecialcharacters 在 channel 名称并 password。 / You cannot use special characters in channel name and password
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_INVALID_CHANNEL_NAME_SPECIAL_LETTER = new SM_SYSTEM_MESSAGE(1300964);

	/**
	 * @param num0 你：used %num0 Abyss Points。 / You used %num0 Abyss Points
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_USE_ABYSSPOINT(int num0) {
		return new SM_SYSTEM_MESSAGE(1300965, num0);
	}

	/**
	 * @param value0 你已邀请 %0 加入联盟。 / You have invited %0 to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITE_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300966, value0);
	}

	/**
	 * @param value0 你：have received an alliance invitation from %0。 / You have received an alliance invitation from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITE_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300967, value0);
	}

	/**
	 * You 有 invited%0 ' sgroup 到 alliance.%0 ' sgroup 有 total 的 %1 成员。 / You have invited %0's group to the alliance. %0's group has a total of %1 members
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITE_PARTY(String value0, String value2, String value1) {
		return new SM_SYSTEM_MESSAGE(1300968, value0, value2, value1);
	}

	/**
	 * %0 的小队长是 %1。 / The leader of %0's group is %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITE_PARTY_HIM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300969, value0, value1);
	}

	 /**
	  * 你的小队收到了来自 %0 的联盟邀请。
	  * 你的小队 has received an 联盟 invitation from %0。 / Your group has received an alliance invitation from %0
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITE_PARTY_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300970, value0);
	}

	/**
	 * @param value0 你：have declined %0's invitation to join the alliance。 / You have declined %0's invitation to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_REJECT_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300971, value0);
	}

	 /**
	  * %0 has declined your invitation to join the alliance。
	  * %0 has declined your invitation to join the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_REJECT_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300972, value0);
	}

	/**
	 * %0's group has declined your invitation to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_REJECT_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1300973, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 anotheralliance。 / %0 is already a member of another alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ALREADY_OTHER_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1300974, value0);
	}

	/**
	 * 联盟人数已满。 / There is not enough room in the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_INVITE_FAILED_NOT_ENOUGH_SLOT = new SM_SYSTEM_MESSAGE(1300975);

	/**
	 * 你：have no authority in the alliance。 / You have no authority in the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_RIGHT_NOT_HAVE = new SM_SYSTEM_MESSAGE(1300976);

	/**
	 * 你：have left the alliance。 / You have left the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LEAVE_ME = new SM_SYSTEM_MESSAGE(1300977);

	 /**
	  * %0 has left the alliance。
	  * %0 has left the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_LEAVE_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1300978, value0);
	}

	 /**
	  * %0 has kicked you out of the alliance。
	  * %0 has kicked you out of the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_BAN_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1300979, value0);
	}

	 /**
	  * %0 has kicked out %1 of the alliance。
	  * %0 has kicked out %1 of the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_BAN_HIM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1300980, value0, value1);
	}

	 /**
	  * %0 has left the alliance due to a prolonged absence。
	  * %0 has left the alliance due to a prolonged absence
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_LEAVE_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300981, value0);
	}

	/**
	 * @param value0 %0 现为联盟队长。 / %0 is now Captain of the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_CHANGE_LEADER_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1300982, value0);
	}

	/**
	 * alliance 有 disbandeddue 到 lack 的成员。 / The alliance has disbanded due to a lack of members
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_DISPERSED = new SM_SYSTEM_MESSAGE(1300983);

	/**
	 * @param value0 %0 现为联盟副队长。 / %0 is now vice Captain of the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_PROMOTE_MANAGER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300984, value0);
	}

	/**
	 * @param value0 %0 有已 demoted 到成员从 viceCaptain。 / %0 has been demoted to member from vice Captain
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_DEMOTE_MANAGER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300985, value0);
	}

	 /**
	  * %0 has promoted %1. From now on, %1 is the alliance captain。
	  * %0 has promoted %1. From now on, %1 is the alliance captain
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_CHANGE_LEADER(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1300986, value0, value1, value2);
	}

	/**
	 * You 有 failed 到 changealliancegroup 作为 anotherperson 为已经 trying 到 change 其 .Pleasetryagainlater。 / You have failed to change the alliance group as another person is already trying to change it. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_GROUP_FAILED_ALREADY_CHANGED = new SM_SYSTEM_MESSAGE(1300987);

	/**
	 * 你：have failed to change the group because there was no group to change。 / You have failed to change the group because there was no group to change
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_GROUP_FAILED_ALREADY_LEAVE = new SM_SYSTEM_MESSAGE(1300988);

	/**
	 * Checking the readiness of the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_START = new SM_SYSTEM_MESSAGE(1300989);

	 /**
	  * %0 has requested to check the combat readiness。
	  * %0 has requested to check the combat readiness
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_CHECK_REQUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1300990, value0);
	}

	/**
	 * 全部 alliance 成员为 ready。 / All alliance members are ready
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_COMPLETE = new SM_SYSTEM_MESSAGE(1300991);

	/**
	 * 当前缺席： / Currently Absent:
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_OUT = new SM_SYSTEM_MESSAGE(1300992);

	/**
	 * 就绪： / Ready:
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_READY = new SM_SYSTEM_MESSAGE(1300993);

	/**
	 * 未就绪： / Not Ready:
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_DENIED = new SM_SYSTEM_MESSAGE(1300994);

	/**
	 * 你：cancelled the request to check the readiness of the alliance。 / You cancelled the request to check the readiness of the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CHECK_CANCEL = new SM_SYSTEM_MESSAGE(1300995);

	/**
	 * You 无法 inviteanymore 成员到 alliance。 / You cannot invite any more members to the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_ADD_NEW_MEMBER = new SM_SYSTEM_MESSAGE(1300996);

	/**
	 * 仅联盟队长可转让队长职位。 / Only the alliance Captain can make another person the Captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ONLY_LEADER_CAN_CHANGE_LEADER = new SM_SYSTEM_MESSAGE(1300997);

	/**
	 * @param value0 %0 现为联盟队长。 / %0 is now the alliance captain
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_IS_NEW_LEADER(String value0) {
		return new SM_SYSTEM_MESSAGE(1300998, value0);
	}

	/**
	 * 你现已成为联盟队长。 / You are now the alliance captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_YOU_BECOME_NEW_LEADER = new SM_SYSTEM_MESSAGE(1300999);

	/**
	 * Only the alliance captain can change the item distribution method
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ONLY_LEADER_CAN_CHANGE_LOOTING = new SM_SYSTEM_MESSAGE(1301000);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Manual。 / The item distribution method of the alliance has been changed to Manual
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LOOTING_CHANGED_TO_MANUAL = new SM_SYSTEM_MESSAGE(1301001);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Auto。 / The item distribution method of the alliance has been changed to Auto
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LOOTING_CHANGED_TO_AUTO = new SM_SYSTEM_MESSAGE(1301002);

	/**
	 * There 为 no 目标到 invite 到 alliance。 / There is no target to invite to the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_NO_USER_TO_INVITE = new SM_SYSTEM_MESSAGE(1301003);

	/**
	 * Only the alliance Captain and vice Captain can invite people to the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ONLY_LEADER_CAN_INVITE = new SM_SYSTEM_MESSAGE(1301004);

	 /**
	  * %0 has declined your invitation to join the alliance。
	  * %0 has declined your invitation to join the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_REJECT_INVITATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1301005, value0);
	}

	/**
	 * 你：cannot invite yourself to the alliance。 / You cannot invite yourself to the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CAN_NOT_INVITE_SELF = new SM_SYSTEM_MESSAGE(1301006);

	/**
	 * 你：cannot issue invitations while you are dead。 / You cannot issue invitations while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_INVITE_WHEN_DEAD = new SM_SYSTEM_MESSAGE(1301007);

	/**
	 * selectedalliance 成员为 currently 离线。 / The selected alliance member is currently offline
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_OFFLINE_MEMBER = new SM_SYSTEM_MESSAGE(1301008);

	/**
	 * Only the alliance captain can kick out a member
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ONLY_LEADER_CAN_BANISH = new SM_SYSTEM_MESSAGE(1301009);

	/**
	 * You 有已踢出 out 的 alliance。 / You have been kicked out of the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_YOU_ARE_BANISHED = new SM_SYSTEM_MESSAGE(1301010);

	 /**
	  * alliance has been disbanded。
	  * The alliance has been disbanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_IS_DISPERSED = new SM_SYSTEM_MESSAGE(1301011);

	 /**
	  * %0 has left the alliance。
	  * %0 has left the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_LEAVE_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1301012, value0);
	}

	 /**
	  * %0 is a member of another alliance。
	  * %0 is a member of another alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_IS_ALREADY_MEMBER_OF_OTHER_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1301013, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 youralliance。 / %0 is already a member of your alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_IS_ALREADY_MEMBER_OF_OUR_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1301014, value0);
	}

	/**
	 * 你不in an alliance。 / You are not in an alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_YOU_ARE_NOT_FORCE_MEMBER = new SM_SYSTEM_MESSAGE(1301015);

	/**
	 * 你不an alliance member。 / You are not an alliance member
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_NOT_FORCE_MEMBER = new SM_SYSTEM_MESSAGE(1301016);

	/**
	 * @param value0 你已邀请 %0 加入联盟。 / You have invited %0 to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_INVITED_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1301017, value0);
	}

	/**
	 * @param value0 目前 %0 无法接受你的联盟邀请。 / Currently, %0 cannot accept your invitation to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1301018, value0);
	}

	 /**
	  * %0 has been disconnected。
	  * %0 has been disconnected
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_BECOME_OFFLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1301019, value0);
	}

	/**
	 * @param value0 %0 离线过久，已自动移出联盟。 / %0 has been offline for too long and had been automatically kicked out of the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_BECOME_OFFLINE_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1301020, value0);
	}

	/**
	 * @param value0 %0 有已踢出 out 的 alliance。 / %0 has been kicked out of the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_IS_BANISHED(String value0) {
		return new SM_SYSTEM_MESSAGE(1301021, value0);
	}

	/**
	 * 联盟物品分配方式已改为自由拾取。 / The rare item distribution method of the alliance has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_RARE_LOOTING_CHANGED_TO_MANUAL = new SM_SYSTEM_MESSAGE(1301022);

	/**
	 * rare 物品 distributionmethod 的 alliance 有已 changed 到 Auto。 / The rare item distribution method of the alliance has been changed to Auto
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_RARE_LOOTING_CHANGED_TO_AUTO = new SM_SYSTEM_MESSAGE(1301023);

	/**
	 * 联盟物品分配方式已更改。 / The rare item distribution method of the alliance has been changed to Dice Roll
	  */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_RARE_LOOTING_CHANGED_TO_DICE = new SM_SYSTEM_MESSAGE(1301024);

	/**
	 * 物品分配完成前无法踢出联盟成员。 / An alliance member cannot be kicked out before the items have been distributed
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANNOT_BANISH_ITEMPOOL_NOT_EMPTY = new SM_SYSTEM_MESSAGE(1301025);

	/**
	 * %0 掷骰得到 %num1。 / %0 rolled the dice and got a %num1
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ITEM_DICE(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1301026, value0, num1);
	}

	/**
	 * You 可 rolldiceoncemore 若 rolled 编号为 lessthan100。 / You can roll the dice once more if the rolled number is less than 100
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ITEM_DICE_AGAIN = new SM_SYSTEM_MESSAGE(1301027);

	/**
	 * 联盟物品分配方式已改为自由拾取。 / The item distribution method of the alliance has been changed to Free-for-All
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LOOTING_CHANGED_TO_FREEFORALL = new SM_SYSTEM_MESSAGE(1301028);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Round - robin。 / The item distribution method of the alliance has been changed to Round-robin
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LOOTING_CHANGED_TO_ROUNDROBIN = new SM_SYSTEM_MESSAGE(1301029);

	/**
	 * 物品 distributionmethod 的 alliance 有已 changed 到 Captain。 / The item distribution method of the alliance has been changed to Captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_LOOTING_CHANGED_TO_LEADERONLY = new SM_SYSTEM_MESSAGE(1301030);

	/**
	 * @param value0 %0 有已踢出 out 的 arena。 / %0 has been kicked out of the arena
	 */
	public static SM_SYSTEM_MESSAGE STR_PvPZONE_OUT_MESSAGE(String value0) {
		return new SM_SYSTEM_MESSAGE(1301031, value0);
	}

	/**
	 * 你：cannot fly while you are banned from flying。 / You cannot fly while you are banned from flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_FLY_NOW_DUE_TO_NOFLY = new SM_SYSTEM_MESSAGE(1301032);

	/**
	 * %0 的 %1 正在激活 %2 神器。 / The %0 %1 is activating the %2 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_CASTING(DescriptionId race, String value1,
			DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301033, race, value1, descriptionId);
	}

	 /**
	  * %1 Artifact core in %0 possession has been deactivated。
	  * The %1 Artifact core in %0 possession has been deactivated
	  */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_CORE_CASTING(DescriptionId race, DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301034, race, descriptionId);
	}

	 /**
	  * activation of the %1 Artifact in %0 possession has been cancelled。
	  * The activation of the %1 Artifact in %0 possession has been cancelled
	  */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_CANCELED(DescriptionId race, DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301035, race, descriptionId);
	}

	 /**
	  * %0 %1 has succeeded in activating the %2 Artifact。
	  * The %0 %1 has succeeded in activating the %2 Artifact
	  */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_FIRE(DescriptionId race, String value1, DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301036, race, value1, descriptionId);
	}

	/**
	 * %0 军团 lost%1。 / %0 Legion lost %1
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_GUILD_CASTLE_TAKEN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1301037, value0, value1);
	}

	 /**
	  * %0 has conquered %1。
	  * %0 has conquered %1
	  */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_GUILD_WIN_CASTLE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1301038, value0, value1);
	}

	/**
	 * %0 成功攻占了 %1。 / %0 succeeded in conquering %1
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_WIN_CASTLE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1301039, value0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_ABYSS_WIN_CASTLE_GROUP(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1404542, value0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_ABYSS_LOSE_CASTLE_GROUP(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1404543, value0, value1);
	}

	/**
	 * @param descriptionId %0 现可攻击。 / %0 is now vulnerable
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_PVP_ON(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301040, descriptionId);
	}

	/**
	 * @param descriptionId %0 不再易受攻击。 / %0 is no longer vulnerable
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_PVP_OFF(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1301041, descriptionId);
	}

	 /**
	  * Dredgion has disgorged a horde of Balaur troopers。
	  * The Dredgion has disgorged a horde of Balaur troopers
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_CARRIER_DROP_DRAGON = new SM_SYSTEM_MESSAGE(1301042);

	 /**
	  * Balaur Teleport Raiders appeared。
	  * The Balaur Teleport Raiders appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_WARP_DRAGON = new SM_SYSTEM_MESSAGE(1301043);

	 /**
	  * dredgion has appeared。
	  * A dredgion has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_CARRIER_SPAWN = new SM_SYSTEM_MESSAGE(1301044);

	/**
	 * 无法 find 目标到 use 物品。 / Cannot find the target to use the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_CANT_FIND_VALID_TARGET = new SM_SYSTEM_MESSAGE(1301045);

	/**
	 * %0 未能守住 %1。 / %0 failed to defend %1
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_CASTLE_TAKEN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1301046, value0, value1);
	}

	/**
	 * @param nameId %0 物品有已 sold 按 broker。 / The %0 item has been sold by the broker
	 */
	public static SM_SYSTEM_MESSAGE STR_VENDOR_REGISTER_SOLD_OUT(int nameId) {
		return new SM_SYSTEM_MESSAGE(1301047, new DescriptionId(nameId));
	}

	/**
	 * %0 的 %1 摧毁了奥德力场发生器。 / %1 of the %0 killed the Aetheric Field Generator
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_SHIELD_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1301048, value1, value0);
	}

	/**
	 * %0 的 %1 摧毁了城门。 / %1 of the %0 destroyed the Castle Gate
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_DOOR_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1301049, value1, value0);
	}

	/**
	 * 城门正遭受攻击。 / The Castle Gate is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DOOR_ATTACKED = new SM_SYSTEM_MESSAGE(1301050);

	/**
	 * 城门危急。 / The Castle Gate is in danger
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DOOR_ATSTAKE = new SM_SYSTEM_MESSAGE(1301051);

	/**
	 * Aetheric Field Generator 正遭受攻击。 / The Aetheric Field Generator is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_SHIELD_ATTACKED = new SM_SYSTEM_MESSAGE(1301052);

	/**
	 * 城门守护石正遭受攻击。 / The Gate Guardian Stone is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_REPAIR_ATTACKED = new SM_SYSTEM_MESSAGE(1301053);

	/**
	 * %0 的 %1 摧毁了城门守护石。 / %1 of the %0 destroyed the Gate Guardian Stone
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_REPAIR_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1301054, value1, value0);
	}

	 /**
	  * Guardian General is under attack。
	  * The Guardian General is under attack
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_BOSS_ATTACKED = new SM_SYSTEM_MESSAGE(1301055);

	/**
	 * You 无法 startgliding 作为 you 为 movingtooslowly。 / You cannot start gliding as you are moving too slowly
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLIDE_NOT_ENOUGH_SPEED_FOR_GLIDE = new SM_SYSTEM_MESSAGE(1301056);

	/**
	 * 你：cannot start gliding while in an Altered State。 / You cannot start gliding while in an Altered State
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLIDE_CANNOT_GLIDE_ABNORMAL_STATUS = new SM_SYSTEM_MESSAGE(1301057);

	/**
	 * 你：cannot change to the combat mode while gliding。 / You cannot change to the combat mode while gliding
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLIDE_CANNOT_GLIDE_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1301058);

	/**
	 * 你：can glide when you become a Daeva。 / You can glide when you become a Daeva
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLIDE_ONLY_DEVA_CAN = new SM_SYSTEM_MESSAGE(1301059);

	/**
	 * @param value0 你：do not have enough mana to continue using %0。 / You do not have enough mana to continue using %0
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_INSUFFICIENT_COST_FOR_TOGGLE_SKILLL(String value0) {
		return new SM_SYSTEM_MESSAGE(1301060, value0);
	}

	/**
	 * You 无法 appointanymorevicecaptains.alliance 可有 maximum 的 4。 / You cannot appoint any more vice captains. The alliance can have a maximum of 4
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANNOT_PROMOTE_MANAGER = new SM_SYSTEM_MESSAGE(1301061);

	 /**
	  * %0 has been activated。
	  * %0 has been activated
	  */
	public static SM_SYSTEM_MESSAGE STR_SKILL_PROC_EFFECT_OCCURRED(int value0) {
		return new SM_SYSTEM_MESSAGE(1301062, new DescriptionId(value0));
	}

	/**
	 * 你已处于相同效果中。 / You are already under the same effect
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_SAME_EFFECT_ALREADY_TAKEN = new SM_SYSTEM_MESSAGE(1301063);

	/**
	 * @param value0 你：declined %0's challenge for a duel。 / You declined %0's challenge for a duel
	 */
	public static SM_SYSTEM_MESSAGE STR_DUEL_REJECT_DUEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1301064, value0);
	}

	 /**
	  * %0 has challenged you to a duel。
	  * %0 has challenged you to a duel
	  */
	public static SM_SYSTEM_MESSAGE STR_DUEL_REQUESTED(String value0) {
		return new SM_SYSTEM_MESSAGE(1301065, value0);
	}

	/**
	 * 你当前无法聊天。 / You are currently unable to chat
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_DISABLED = new SM_SYSTEM_MESSAGE(1310000);

	/**
	 * 你因无关聊天干扰游戏，2 分钟内无法聊天。 / You are unable to chat for 2 minutes as you interrupted the game play through unnecessary chatting
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLOODING = new SM_SYSTEM_MESSAGE(1310001);

	/**
	 * Characters under level %0 cannot chat
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_CHAT_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1310002, value0);
	}

	/**
	 * 你：have too many users blocked from chatting with you。 / You have too many users blocked from chatting with you
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOO_MANY_EXCLUDE = new SM_SYSTEM_MESSAGE(1310003);

	/**
	 * Characters under level %0 cannot send whispers
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_WHISPER_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1310004, value0);
	}

	 /**
	  * NPC server is down. Please restore it soon。
	  * The NPC server is down. Please restore it soon
	  */
	public static final SM_SYSTEM_MESSAGE STR_NPC_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1310005);

	/**
	 * 与缓存服务器的连接已断开，请尽快恢复。 / The connection with the cache server has been severed. Please restore it soon
	 */
	public static final SM_SYSTEM_MESSAGE STR_CACHE_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1310006);

	/**
	 * 与认证服务器的连接已断开，请尽快恢复。 / The connection with the authorization server has been severed. Please restore it soon
	 */
	public static final SM_SYSTEM_MESSAGE STR_AUTH_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1310007);

	/**
	 * 与计费服务器的连接已断开，请尽快恢复。 / The connection with the ittem billing server has been severed. Please restore it soon
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_BILLING_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1310008);

	/**
	 * @param value0 你：disabled chatting for %0。 / You disabled chatting for %0
	 */
	public static SM_SYSTEM_MESSAGE STR_DISABLED_OTHER_CHAT(String value0) {
		return new SM_SYSTEM_MESSAGE(1310009, value0);
	}

	/**
	 * 此为 Assist 目标 Key.Use 其 afteryou 有 selected 目标。 / This is an Assist Target Key. Use it after you have selected a target
	 */
	public static final SM_SYSTEM_MESSAGE STR_ASSISTKEY_THIS_IS_ASSISTKEY = new SM_SYSTEM_MESSAGE(1310010);

	/**
	 * PleaseuserightNPC 用于 yourrace 到 register 物品。 / Please use the right NPC for your race to register items
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_RACECHECK = new SM_SYSTEM_MESSAGE(1310011);

	/**
	 * 你：cannot chat while you are dead。 / You cannot chat while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_CHAT_AT_DIE = new SM_SYSTEM_MESSAGE(1310012);

	/**
	 * @param value0 你：cannot quit during a battle. Canceling in %0 seconds。 / You cannot quit during a battle. Canceling in %0 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_QUIT_DURING_BATTLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1310013, value0);
	}

	/**
	 * An error has occurred while restoring the login list on the Billing server
	 */
	public static final SM_SYSTEM_MESSAGE STR_KICK_BILLGATES_ERROR = new SM_SYSTEM_MESSAGE(1310014);

	 /**
	  * dual login error has occurred while trying to enter the world。
	  * A dual login error has occurred while trying to enter the world
	  */
	public static final SM_SYSTEM_MESSAGE STR_KICK_DUAL_LOGIN_ON_ABOUT_TO_PLAY = new SM_SYSTEM_MESSAGE(1310015);

	 /**
	  * 你的账号已被封禁。
	  * 你的账号已被封禁。 / Your account has been banned
	  */
	public static final SM_SYSTEM_MESSAGE STR_KICK_BANNED = new SM_SYSTEM_MESSAGE(1310016);

	/**
	 * 你：have been disconnected from the server。 / You have been disconnected from the server
	 */
	public static final SM_SYSTEM_MESSAGE STR_KICK_CHARACTER = new SM_SYSTEM_MESSAGE(1310017);

	 /**
	  * 你的World access time limit has been exceeded。
	  * 你的World access time limit has been exceeded。 / Your World access time limit has been exceeded
	  */
	public static final SM_SYSTEM_MESSAGE STR_KICK_ABOUT_TO_PLAY_TIMER_EXPIRED = new SM_SYSTEM_MESSAGE(1310018);

	 /**
	  * requested target no longer exists。
	  * The requested target no longer exists
	  */
	public static final SM_SYSTEM_MESSAGE STR_SEARCH_NOT_EXIST = new SM_SYSTEM_MESSAGE(1310019);

	/**
	 * 你请求的用户当前离线。 / The user you requested is currently offline
	 */
	public static final SM_SYSTEM_MESSAGE STR_SEARCH_DISCONNECT = new SM_SYSTEM_MESSAGE(1310020);

	/**
	 * 仅守护者可使用。 / Only Daevas can use that
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_USE_GROUPGATE_BEFORE_CHANGE_CLASS = new SM_SYSTEM_MESSAGE(
			1310021);

	/**
	 * 你：do not have enough credit left in the account。 / You do not have enough credit left in the account
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_NOT_PAID = new SM_SYSTEM_MESSAGE(1310022);

	/**
	 * Invalid session info
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INVALID_SESSION = new SM_SYSTEM_MESSAGE(1310023);

	 /**
	  * server list info in the server is incorrect。
	  * The server list info in the server is incorrect
	  */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_SERVERLIST_INCORRECT = new SM_SYSTEM_MESSAGE(1310024);

	/**
	 * Failed to create the character due to a World DB error
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_WORLD_DB_FAIL = new SM_SYSTEM_MESSAGE(1310025);

	/**
	 * 你已与游戏服务器断开连接。 / You are disconnected from the game server
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_CONNECTION_LOST = new SM_SYSTEM_MESSAGE(1310026);

	/**
	 * Failed to connect to the game server
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_CONNECTION_FAIL = new SM_SYSTEM_MESSAGE(1310027);

	 /**
	  * client version is not compatible with the game server。
	  * The client version is not compatible with the game server
	  */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_VERSION_FAIL = new SM_SYSTEM_MESSAGE(1310028);

	/**
	 * Charactersdifferentracesexistin 相同服务端 / Characters of different races exist in the same server
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_HAS_MULTIPLE_RACE = new SM_SYSTEM_MESSAGE(1310029);

	 /**
	  * NPC script version is not compatible with the game server。
	  * The NPC script version is not compatible with the game server
	  */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_NPC_SCRIPT_VERSION_FAIL = new SM_SYSTEM_MESSAGE(1310030);

	/**
	 * An unknown error has occurred while checking the game server version
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_UNKNOWN_VERSION_FAIL = new SM_SYSTEM_MESSAGE(1310031);

	/**
	 * Failed to delete the character
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_DEL_CHAR_FAIL = new SM_SYSTEM_MESSAGE(1310032);

	/**
	 * Cannot connect to the login server
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_CONNECTION_FAIL = new SM_SYSTEM_MESSAGE(1310033);

	/**
	 * That character does not exist
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_CHAR_NOT_EXIST = new SM_SYSTEM_MESSAGE(1310034);

	/**
	 * character 为已经设置到为 deleted。 / That character is already set to be deleted
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_CHAR_ALREADY_DELETED = new SM_SYSTEM_MESSAGE(1310035);

	/**
	 * Failed to create the character
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_FAILED_TO_CREATE_CHAR = new SM_SYSTEM_MESSAGE(1310036);

	/**
	 * character 带名称已经存在。 / A character with that name already exists
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_CHARACTER_EXIST = new SM_SYSTEM_MESSAGE(1310037);

	/**
	 * You 无法创建 anymorecharacters 在此 server。 / You cannot create any more characters on this server
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_NO_AVAILABLE_SLOT = new SM_SYSTEM_MESSAGE(1310038);

	/**
	 * Invalid server ID
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INVALID_SERVERID = new SM_SYSTEM_MESSAGE(1310039);

	/**
	 * Too many users on the game server. You cannot access the game
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_TOO_MANY_USER = new SM_SYSTEM_MESSAGE(1310040);

	 /**
	  * game server memory is full. You cannot access the game。
	  * The game server memory is full. You cannot access the game
	  */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_OUT_OF_MEMORY = new SM_SYSTEM_MESSAGE(1310041);

	 /**
	  * selected character is already playing on the selected server。
	  * The selected character is already playing on the selected server
	  */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_ALREADY_PLAYING = new SM_SYSTEM_MESSAGE(1310042);

	/**
	 * You 无法创建 anymorecharacters 在 account。 / You cannot create any more characters on that account
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_MAX_CHAR_COUNT = new SM_SYSTEM_MESSAGE(1310043);

	/**
	 * Invalid character name
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INVALID_NAME = new SM_SYSTEM_MESSAGE(1310044);

	/**
	 * Invalid character gender
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INVALID_GENDER = new SM_SYSTEM_MESSAGE(1310045);

	/**
	 * Invalid character class
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INVALID_CLASS = new SM_SYSTEM_MESSAGE(1310046);

	 /**
	  * game server is down。
	  * The game server is down
	  */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_SERVER_DOWN = new SM_SYSTEM_MESSAGE(1310047);

	 /**
	  * Billing server is down。
	  * The Billing server is down
	  */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_BILLGATES_DOWN = new SM_SYSTEM_MESSAGE(1310048);

	/**
	 * Internalgame 服务端错误 / Internal game server error
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOGIN_ERROR_INTERNAL_SERVER_ERROR = new SM_SYSTEM_MESSAGE(1310049);

	/**
	 * You 有已 disconnected 从 server 按 request 的 PlayNCHomepage。 / You have been disconnected from the server by request of the PlayNC Homepage
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_KICKED_BY_WEB = new SM_SYSTEM_MESSAGE(1310050);

	/**
	 * 你年龄未满，无法进入游戏。 / You are not old enough to play the game
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_UNDER_AGE = new SM_SYSTEM_MESSAGE(1310051);

	/**
	 * 检测到重复登录尝试。 / Double login attempts have been detected
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_KICKED_DOUBLE_LOGIN = new SM_SYSTEM_MESSAGE(1310052);

	/**
	 * 你已登录。 / You are already logged in
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_ALREADY_PLAY_GAME = new SM_SYSTEM_MESSAGE(1310053);

	/**
	 * Sorry, the queue is full. Please try another server
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_LIMIT_EXCEED = new SM_SYSTEM_MESSAGE(1310054);

	 /**
	  * server is currently unavailable. Please try again later。
	  * The server is currently unavailable. Please try again later
	  */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_SEVER_CHECK = new SM_SYSTEM_MESSAGE(1310055);

	/**
	 * Please login to the game after you have changed your password
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_MODIFY_PASSWORD = new SM_SYSTEM_MESSAGE(1310056);

	/**
	 * 使用期限已过或暂时连接困难。详情请联系管理员或客服中心。 / Either the usage period has expired or we are experiencing a temporary connection difficulty. For more information, please contact the administrators or our customer center
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_NOT_PAID = new SM_SYSTEM_MESSAGE(1310057);

	/**
	 * You 有 usedupyourallocatedtime 并 there 为 notimeleft 在此 account。 / You have used up your allocated time and there is no time left on this account
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_NO_SPECIFICTIME = new SM_SYSTEM_MESSAGE(1310058);

	/**
	 * System error
	 */
	public static final SM_SYSTEM_MESSAGE STR_L2AUTH_S_SYSTEM_ERROR = new SM_SYSTEM_MESSAGE(1310059);

	/**
	 * You 无法 openprivate 商店在 arena。 / You cannot open a private store in the arena
	 */
	public static final SM_SYSTEM_MESSAGE STR_PvPZONE_CANNOT_OPEN_MARKET = new SM_SYSTEM_MESSAGE(1310060);

	/**
	 * 你：cannot continue unless you stop flying。 / You cannot continue unless you stop flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_CANNOT_PROCESS_IN_FLIGHT = new SM_SYSTEM_MESSAGE(1310061);

	/**
	 * @param num0 你：have gained %num0 Abyss Points。 / You have gained %num0 Abyss Points
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN(int num0) {
		return new SM_SYSTEM_MESSAGE(1320000, num0);
	}

	 /**
	  * one-way Rift into Asmodae has appeared。
	  * A one-way Rift into Asmodae has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DARK_SIDE_DIRECT_PORTAL_OPEN_01 = new SM_SYSTEM_MESSAGE(1320001);

	 /**
	  * Volatile Rift has opened from Asmodae to Elysea。
	  * A Volatile Rift has opened from Asmodae to Elysea
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DARK_SIDE_DIRECT_PORTAL_OPEN_02 = new SM_SYSTEM_MESSAGE(1402875);

	/**
	 * %1 of %0 has captured the %2 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_WIN_ARTIFACT(String value1, String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1320002, value1, value0, value2);
	}

	 /**
	  * %0 has conquered %1。
	  * %0 has conquered %1
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_WIN_FORT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1320003, value0, value1);
	}

	 /**
	  * %0 Artifact has been lost to %1。
	  * The %0 Artifact has been lost to %1
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_LOSE_ARTIFACT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1320004, value0, value1);
	}

	/**
	 * %0 军团 lost%1。 / %0 Legion lost %1
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_LOSE_FORT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1320005, value0, value1);
	}

	/**
	 * 已有人在采集。 / Someone is already gathering it
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_OUCCPIED_BY_OTHER = new SM_SYSTEM_MESSAGE(1330000);

	 /**
	  * 你的%0 skill level is not high enough。
	  * 你的%0 skill level is not high enough。 / Your %0 skill level is not high enough
	  */
	public static SM_SYSTEM_MESSAGE STR_GATHER_OUT_OF_SKILL_POINT(String value0) {
		return new SM_SYSTEM_MESSAGE(1330001, value0);
	}

	/**
	 * 距离过远：the object to gather it。 / You are too far from the object to gather it
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_TOO_FAR_FROM_GATHER_SOURCE = new SM_SYSTEM_MESSAGE(1330002);

	/**
	 * You 无法 gather 作为 there 为 obstaclesblockingway。 / You cannot gather as there are obstacles blocking the way
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_OBSTACLE_EXIST = new SM_SYSTEM_MESSAGE(1330003);

	/**
	 * @param value0 你：have learned the %0 skill。 / You have learned the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_LEARNED_NEW_GATHER_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330004, value0);
	}

	 /**
	  * 你的%0 skill has been upgraded to %1 points。
	  * 你的%0 skill has been upgraded to %1 points。 / Your %0 skill has been upgraded to %1 points
	  */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SKILL_POINT_UP(String skillName, int newLevel) {
		return new SM_SYSTEM_MESSAGE(1330005, skillName, newLevel);
	}

	/**
	 * 你：do not have the basic gathering tools。 / You do not have the basic gathering tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_NO_TOOL_1_BASIC = new SM_SYSTEM_MESSAGE(1330006);

	/**
	 * 你：do not have the harvesting tools。 / You do not have the harvesting tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_NO_TOOL_2_GATHER = new SM_SYSTEM_MESSAGE(1330007);

	/**
	 * 你：do not have the mining tools。 / You do not have the mining tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_NO_TOOL_3_MINING = new SM_SYSTEM_MESSAGE(1330008);

	/**
	 * 你：do not have the fishing tools。 / You do not have the fishing tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_NO_TOOL_4_FISHING = new SM_SYSTEM_MESSAGE(1330009);

	/**
	 * 你：do not have the forestry tools。 / You do not have the forestry tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_NO_TOOL_5_FORESTRY = new SM_SYSTEM_MESSAGE(1330010);

	/**
	 * @param value0 你正在采集 %0。 / You are gathering %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_START_1_BASIC(String value0) {
		return new SM_SYSTEM_MESSAGE(1330011, value0);
	}

	/**
	 * @param value0 你正在收获 %0。 / You are harvesting %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_START_2_GATHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1330012, value0);
	}

	/**
	 * @param value0 你正在采矿 %0。 / You are mining %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_START_3_MINING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330013, value0);
	}

	/**
	 * @param value0 你正在钓鱼 %0。 / You are fishing %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_START_4_FISHING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330014, value0);
	}

	/**
	 * @param value0 你正在伐木 %0。 / You are felling %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_START_5_FORESTRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1330015, value0);
	}

	/**
	 * @param value0 你：have gathered %0。 / You have gathered %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SUCCESS_1_BASIC(String value0) {
		return new SM_SYSTEM_MESSAGE(1330016, value0);
	}

	/**
	 * @param value0 你：have harvested %0。 / You have harvested %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SUCCESS_2_GATHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1330017, value0);
	}

	/**
	 * @param value0 你：have mined %0。 / You have mined %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SUCCESS_3_MINING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330018, value0);
	}

	/**
	 * @param value0 你：have caught %0。 / You have caught %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SUCCESS_4_FISHING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330019, value0);
	}

	/**
	 * @param value0 你获得了 %0。 / You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_SUCCESS_5_FORESTRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1330020, value0);
	}

	/**
	 * @param value0 你：have failed to gather %0。 / You have failed to gather %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_FAIL_1_BASIC(String value0) {
		return new SM_SYSTEM_MESSAGE(1330021, value0);
	}

	/**
	 * @param value0 你：have failed to harvest %0。 / You have failed to harvest %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_FAIL_2_GATHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1330022, value0);
	}

	/**
	 * @param value0 你：have failed to mine %0。 / You have failed to mine %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_FAIL_3_MINING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330023, value0);
	}

	/**
	 * @param value0 你：have failed to catch %0。 / You have failed to catch %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_FAIL_4_FISHING(String value0) {
		return new SM_SYSTEM_MESSAGE(1330024, value0);
	}

	/**
	 * @param value0 你：have failed to acquire %0。 / You have failed to acquire %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_FAIL_5_FORESTRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1330025, value0);
	}

	/**
	 * 你：have stopped gathering。 / You have stopped gathering
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANCEL_1_BASIC = new SM_SYSTEM_MESSAGE(1330026);

	/**
	 * 你：have stopped harvesting。 / You have stopped harvesting
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANCEL_2_GATHER = new SM_SYSTEM_MESSAGE(1330027);

	/**
	 * 你：have stopped mining。 / You have stopped mining
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANCEL_3_MINING = new SM_SYSTEM_MESSAGE(1330028);

	/**
	 * 你：have stopped fishing。 / You have stopped fishing
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANCEL_4_FISHING = new SM_SYSTEM_MESSAGE(1330029);

	/**
	 * 你：have stopped felling。 / You have stopped felling
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANCEL_5_FORESTRY = new SM_SYSTEM_MESSAGE(1330030);

	/**
	 * 你：must be equipped with the basic gathering tools。 / You must be equipped with the basic gathering tools
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_EQUIP_1_BASIC = new SM_SYSTEM_MESSAGE(1330031);

	/**
	 * 你：must be equipped with a hoe。 / You must be equipped with a hoe
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_EQUIP_2_GATHER = new SM_SYSTEM_MESSAGE(1330032);

	/**
	 * 你：must be equipped with a pick。 / You must be equipped with a pick
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_EQUIP_3_MINING = new SM_SYSTEM_MESSAGE(1330033);

	/**
	 * 你：must be equipped with a fishing rod。 / You must be equipped with a fishing rod
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_EQUIP_4_FISHING = new SM_SYSTEM_MESSAGE(1330034);

	/**
	 * 你：must be equipped with an axe。 / You must be equipped with an axe
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_EQUIP_5_FORESTRY = new SM_SYSTEM_MESSAGE(1330035);

	/**
	 * Youmust 有在 least 一个 freespace 在 yourcube 到 gather。 / You must have at least one free space in your cube to gather
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1330036);

	/**
	 * Youmust 有在 least 一个 freespace 在 yourcube 到 craft。 / You must have at least one free space in your cube to craft
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1330037);

	/**
	 * 你：cannot craft while in an altered state。 / You cannot craft while in an altered state
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_CAN_NOT_COMBINE_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1330038);

	/**
	 * 你已在制作中。 / You are already crafting
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_ALREADY_COMBINING = new SM_SYSTEM_MESSAGE(1330039);

	/**
	 * @param value0 距离过远：%0 to craft。 / You are too far from %0 to craft
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_TOO_FAR_FROM_TOOL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330040, value0);
	}

	/**
	 * @param value0 You 无法 craft 作为 youdonot 有 %0。 / You cannot craft as you do not have %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_DO_NOT_HAVE_TOOL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330041, value0);
	}

	/**
	 * @param value0 你：cannot start crafting as you have not learned the %0 skill。 / You cannot start crafting as you have not learned the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_CANT_USE(String value0) {
		return new SM_SYSTEM_MESSAGE(1330042, value0);
	}

	/**
	 * 找不到设计图。 / Cannot find the design
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_CAN_NOT_FIND_RECIPE = new SM_SYSTEM_MESSAGE(1330043);

	 /**
	  * 你的%0 skill is not good enough yet。
	  * 你的%0 skill is not good enough yet。 / Your %0 skill is not good enough yet
	  */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_OUT_OF_SKILL_POINT(String value0) {
		return new SM_SYSTEM_MESSAGE(1330044, value0);
	}

	/**
	 * 你：cannot craft as you do not have a required item。 / You cannot craft as you do not have a required item
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_NO_COMPONENT_ITEM_IN_RECIPE = new SM_SYSTEM_MESSAGE(1330045);

	/**
	 * @param value0 You 无法 craft 作为 youdonot 有 %0。 / You cannot craft as you do not have %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_NO_COMPONENT_ITEM_SINGLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1330046, value0);
	}

	/**
	 * You 无法 craft 作为 youdonot 有 %num1%0。 / You cannot craft as you do not have %num1 %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_NO_COMPONENT_ITEM_MULTIPLE(int num1, String value0) {
		return new SM_SYSTEM_MESSAGE(1330047, num1, value0);
	}

	/**
	 * @param value0 你正在制作 %0。 / You are crafting %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_START(String value0) {
		return new SM_SYSTEM_MESSAGE(1330048, value0);
	}

	/**
	 * @param value0 你：have crafted %0。 / You have crafted %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1330049, value0);
	}

	/**
	 * @param value0 你：have failed to craft %0。 / You have failed to craft %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_FAIL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330050, value0);
	}

	/**
	 * 你：stopped crafting。 / You stopped crafting
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMBINE_CANCEL = new SM_SYSTEM_MESSAGE(1330051);

	/**
	 * @param value0 你：must have learned the %0 skill to use this tool。 / You must have learned the %0 skill to use this tool
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_CANT_USE_TOOL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330052, value0);
	}

	 /**
	  * 你的%0 skill has been upgraded to %1 points。
	  * 你的%0 skill has been upgraded to %1 points。 / Your %0 skill has been upgraded to %1 points
	  */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_SKILL_POINT_UP(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1330053, value0, value1);
	}

	/**
	 * @param value0 你：must learn the %0 skill to start gathering。 / You must learn the %0 skill to start gathering
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHER_LEARN_SKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1330054, value0);
	}

	/**
	 * You 无法 startcrafting 作为 there 为 obstaclesblockingway。 / You cannot start crafting as there are obstacles blocking the way
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_OBSTACLE_EXIST = new SM_SYSTEM_MESSAGE(1330055);

	/**
	 * 你：cannot craft while in combat。 / You cannot craft while in combat
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_DISABLED_IN_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1330056);

	/**
	 * 战斗中无法制作，将自动关闭。 / As you cannot craft while in combat mode, it will be closed automatically
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_CLOSED_FOR_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1330057);

	/**
	 * 你：have gathered successfully。 / You have gathered successfully
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHERING_SUCCESS_GETEXP = new SM_SYSTEM_MESSAGE(1330058);

	/**
	 * 你：have crafted successfully。 / You have crafted successfully
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_SUCCESS_GETEXP = new SM_SYSTEM_MESSAGE(1330059);

	/**
	 * 你：have already learned this design。 / You have already learned this design
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_RECIPE_LEARNED_ALREADY = new SM_SYSTEM_MESSAGE(1330060);

	/**
	 * 你：have learned %0。 / You have learned %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CRAFT_RECIPE_LEARN(int value0, String name) {
		return new SM_SYSTEM_MESSAGE(1330061, "[recipe_ex:" + value0 + ";" + name + "]");
	}

	/**
	 * @param i 你：cannot learn the design because you have not learned the %0 skill。 / You cannot learn the design because you have not learned the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_CRAFT_RECIPE_CANT_LEARN_SKILL(int i) {
		return new SM_SYSTEM_MESSAGE(1330062, new DescriptionId(i));
	}

	/**
	 * You 无法 learndesignbecauseyourskilllevel 为 nothigh 足够。 / You cannot learn the design because your skill level is not high enough
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_RECIPE_CANT_LEARN_SKILLPOINT = new SM_SYSTEM_MESSAGE(1330063);

	/**
	 * Maximum skill level of %0 has been upgraded to Level %1
	 */
	public static SM_SYSTEM_MESSAGE STR_CRAFT_INFO_UPGRADE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1330064, value0, value1);
	}

	/**
	 * 仅守护者可制作。 / Only Daevas can craft it
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_MSG_CAN_WORK_ONLY_DEVA = new SM_SYSTEM_MESSAGE(1330065);

	/**
	 * 你a Daeva now. Leave this resource for Humans to use。 / You are a Daeva now. Leave this resource for Humans to use
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_INCORRECT_SKILL = new SM_SYSTEM_MESSAGE(1330066);

	/**
	 * Maximum skill level of %0 has been upgraded to Level %1
	 */
	public static SM_SYSTEM_MESSAGE STR_GATHERING_INFO_UPGRADE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1330067, value0, value1);
	}

	/**
	 * Maximum skill level of %0 has been upgraded to Level %1
	 */
	public static SM_SYSTEM_MESSAGE STR_AERIALGATHERING_INFO_UPGRADE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1330068, value0, value1);
	}

	/**
	 * 你：cannot be promoted any more。 / You cannot be promoted any more
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_COMBINE_CBT_CAP = new SM_SYSTEM_MESSAGE(1330069);

	/**
	 * 你：cannot be promoted any more。 / You cannot be promoted any more
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GATHER_CBT_CAP = new SM_SYSTEM_MESSAGE(1330070);

	/**
	 * 你：cannot gather while afflicted with an altered state。 / You cannot gather while afflicted with an altered state
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_EXTRACT_GATHER_WHILE_IN_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(
			1330071);

	/**
	 * You 无法 gatherwhile 在当前 stance。 / You cannot gather while in the current stance
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_EXTRACT_GATHER_WHILE_IN_CURRENT_STANCE = new SM_SYSTEM_MESSAGE(
			1330072);

	/**
	 * 你：cannot gather while in the current position。 / You cannot gather while in the current position
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_EXTRACT_GATHER_IN_CURRENT_POSITION = new SM_SYSTEM_MESSAGE(
			1330073);

	/**
	 * Someone else is gathering that object
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_OCCUPIED_BY_OTHER = new SM_SYSTEM_MESSAGE(1330074);

	/**
	 * 距离过远：the target to gather it。 / You are too far from the target to gather it
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_TOO_FAR_FROM_GATHER_SOURCE = new SM_SYSTEM_MESSAGE(
			1330075);

	/**
	 * 你：cannot gather because an obstacle is in the way。 / You cannot gather because an obstacle is in the way
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_OBSTACLE_EXIST = new SM_SYSTEM_MESSAGE(1330076);

	/**
	 * @param nameId 你：have started gathering %0。 / You have started gathering %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_START_1_BASIC(DescriptionId nameId) {
		return new SM_SYSTEM_MESSAGE(1330077, nameId);
	}

	/**
	 * @param nameId 你：have gathered %0。 / You have gathered %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_SUCCESS_1_BASIC(DescriptionId nameId) {
		return new SM_SYSTEM_MESSAGE(1330078, nameId);
	}

	/**
	 * @param nameId 你：have failed to gather %0。 / You have failed to gather %0
	 */
	public static SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_FAIL_1_BASIC(DescriptionId nameId) {
		return new SM_SYSTEM_MESSAGE(1330079, nameId);
	}

	/**
	 * 你：have stopped gathering。 / You have stopped gathering
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_CANCEL_1_BASIC = new SM_SYSTEM_MESSAGE(1330080);

	/**
	 * 你：cannot gather unless there is at least one free space in your cube。 / You cannot gather unless there is at least one free space in your cube
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHER_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1330081);

	/**
	 * 你：have gained experience from gathering。 / You have gained experience from gathering
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHERING_SUCCESS_GETEXP = new SM_SYSTEM_MESSAGE(1330082);

	/**
	 * You 无法 use 物品 until 其 gatheringtimerexpires。 / You cannot use the item until its gathering timer expires
	 */
	public static final SM_SYSTEM_MESSAGE STR_EXTRACT_GATHERING_CANT_USE_UNTIL_DELAY_TIME = new SM_SYSTEM_MESSAGE(
			1330083);

	/**
	 * 你：have died。 / You have died
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_DEATH = new SM_SYSTEM_MESSAGE(1340000);

	/**
	 * @param value0 你：were killed by %0's attack。 / You were killed by %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PvPZONE_MY_DEATH_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1340001, value0);
	}

	/**
	 * @param value0 你：were killed by %0's attack。 / You were killed by %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_DEATH_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1340002, value0);
	}

	/**
	 * @param value0 %0 有 died。 / %0 has died
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_FRIENDLY_DEATH(String value0) {
		return new SM_SYSTEM_MESSAGE(1350000, value0);
	}

	/**
	 * %0 被 %1 的攻击击杀。 / %0 was killed by %1's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_FRIENDLY_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1350001, value0, value1);
	}

	/**
	 * @param value0 %0 有 died。 / %0 has died
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_HOSTILE_DEATH(String value0) {
		return new SM_SYSTEM_MESSAGE(1360000, value0);
	}

	/**
	 * @param value0 你：have defeated %0。 / You have defeated %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PvPZONE_HOSTILE_DEATH_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1360001, value0);
	}

	 /**
	  * %0 has defeated %1。
	  * %0 has defeated %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_PvPZONE_HOSTILE_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1360002, value0, value1);
	}

	/**
	 * @param value0 你：have defeated %0。 / You have defeated %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_HOSTILE_DEATH_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1360003, value0);
	}

	 /**
	  * %0 has defeated %1。
	  * %0 has defeated %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_HOSTILE_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1360004, value0, value1);
	}

	/**
	 * 你：have gained %num1 XP from %0。 / You have gained %num1 XP from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP(String value0, long num1) {
		return new SM_SYSTEM_MESSAGE(1370000, value0, num1);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_DESC(DescriptionId value0, long num1) {
		return new SM_SYSTEM_MESSAGE(1370000, value0, num1);
	}

	/**
	 * @param value0 你：have earned %0 XP。 / You have earned %0 XP
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN(String value0) {
		return new SM_SYSTEM_MESSAGE(1370001, value0);
	}

	/**
	 * @param num0 你：have gained %num0 XP。 / You have gained %num0 XP
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP2(long num0) {
		return new SM_SYSTEM_MESSAGE(1370002, num0);
	}

	 /**
	  * %0 has received %num1 XP。
	  * %0 has received %num1 XP
	  */
	public static SM_SYSTEM_MESSAGE STR_ENSLAVE_GIVE_EXP_TO_PET_GET_EXP(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1370003, value0, num1);
	}

	 /**
	  * %0 has lost %num1 XP。
	  * %0 has lost %num1 XP
	  */
	public static SM_SYSTEM_MESSAGE STR_ENSLAVE_PET_LOSS_EXP(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1370004, value0, num1);
	}

	/**
	 * Youdistributed%1 基纳 each 到 %0 成员。 / You distributed %1 Kinah each to %0 members
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_DISTRIBUTE_GOLD(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1380000, value1, value0);
	}

	/**
	 * @param num0 你：have earned %num0 Kinah。 / You have earned %num0 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GETMONEY(int num0) {
		return new SM_SYSTEM_MESSAGE(1380001, num0);
	}

	/**
	 * @param num0 你：received a refund of %num0 Kinah。 / You received a refund of %num0 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REFUND_MONEY_SYSTEM(int num0) {
		return new SM_SYSTEM_MESSAGE(1380002, num0);
	}

	/**
	 * @param value0 你获得了 %0。 / You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_ITEM1(String value0) {
		return new SM_SYSTEM_MESSAGE(1390000, value0);
	}

	 /**
	  * %0 has acquired %1。
	  * %0 has acquired %1
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ITEM_WIN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390001, value0, value1);
	}

	 /**
	  * %0 has acquired %1。
	  * %0 has acquired %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE(String value0, DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1390002, value0, descriptionId);
	}

	 /**
	  * %0 has acquired %1。
	  * %0 has acquired %1
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ITEM_WIN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390003, value0, value1);
	}

	/**
	 * @param value0 你获得了 %0。 / You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1390004, value0);
	}

	/**
	 * 你：have acquired %num1 %0(s)。 / You have acquired %num1 %0(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_MULTI(int num1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1390005, num1, value0s);
	}

	/**
	 * 你：cannot close the Craft window while crafting。 / You cannot close the Craft window while crafting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_CLOSE_MAKING_DIALOG_DURING_COMBINE = new SM_SYSTEM_MESSAGE(
			1390105);

	/**
	 * 你：cannot change target while crafting。 / You cannot change target while crafting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_SELECT_TARGET_DURING_COMBINE = new SM_SYSTEM_MESSAGE(1390106);

	/**
	 * 你：cannot open a private store while fighting。 / You cannot open a private store while fighting
	 */
	public static final SM_SYSTEM_MESSAGE STR_PERSONAL_SHOP_DISABLED_IN_EXCHANGE = new SM_SYSTEM_MESSAGE(1390107);

	/**
	 * 小队成员无法组建联盟。 / Group members cannot organize an alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_MEMBER_CANT_ORGANIZE_FORCE = new SM_SYSTEM_MESSAGE(1390108);

	/**
	 * You 无法 organizealliance 按 invitingyour 拥有 group 成员。 / You cannot organize an alliance by inviting your own group members
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_ORGANIZE_FORCE_INVITED_PARTY_MEMBER = new SM_SYSTEM_MESSAGE(
			1390109);

	/**
	 * Please select a target
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NEED_TARGET = new SM_SYSTEM_MESSAGE(1390110);

	/**
	 * Invalid name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_CORRECT_CHAR_NAME = new SM_SYSTEM_MESSAGE(1390111);

	 /**
	  * character name does not exist. Please check the recipient again。
	  * The character name does not exist. Please check the recipient again
	  */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_MSG_RECIPIENT_UNKNOWN = new SM_SYSTEM_MESSAGE(1390112);

	/**
	 * @param value0 You 无法发送 mail 到 %0becausehis / hermailbox 为 full。 / You cannot send a mail to %0 because his/her mailbox is full
	 */
	public static SM_SYSTEM_MESSAGE STR_MAIL_MSG_RECIPIENT_MAILBOX_FULL(String value0) {
		return new SM_SYSTEM_MESSAGE(1390113, value0);
	}

	/**
	 * @param value0 %0 当前拒绝查看详情。 / %0 is currently refusing the View Detail access
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_WATCH(String value0) {
		return new SM_SYSTEM_MESSAGE(1390114, value0);
	}

	/**
	 * @param value0 %0 当前拒绝交易请求。 / %0 is currently rejecting trade requests
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_TRADE(String value0) {
		return new SM_SYSTEM_MESSAGE(1390115, value0);
	}

	/**
	 * @param value0 %0 当前拒绝组队邀请。 / %0 is currently rejecting group invitations
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_INVITE_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1390116, value0);
	}

	/**
	 * @param value0 %0 当前拒绝联盟邀请。 / %0 is currently rejecting alliance invitations
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_INVITE_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1390117, value0);
	}

	 /**
	  * %0 is currently rejecting Legion invitations。
	  * %0 is currently rejecting Legion invitations
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_INVITE_GUILD(String value0) {
		return new SM_SYSTEM_MESSAGE(1390118, value0);
	}

	/**
	 * @param value0 %0 当前不接受好友请求。 / %0 is not currently accepting friend requests
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_FRIEND(String value0) {
		return new SM_SYSTEM_MESSAGE(1390119, value0);
	}

	/**
	 * @param value0 %0 当前不接受决斗请求。 / %0 is not currently accepting duel requests
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECTED_DUEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1390120, value0);
	}

	/**
	 * @param value0 你：started using the %0 skill。 / You started using the %0 skill
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_TOGGLE_SKILL_TURNED_ON(String value0) {
		return new SM_SYSTEM_MESSAGE(1390121, value0);
	}

	/**
	 * @param value0 你：have entered zone channel %0。 / You have entered zone channel %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TELEPORT_ZONECHANNEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1390122, value0);
	}

	 /**
	  * reply to your petition has arrived。
	  * A reply to your petition has arrived
	  */
	public static final SM_SYSTEM_MESSAGE STR_PETITION_GOT_MESSAGE = new SM_SYSTEM_MESSAGE(1390123);

	/**
	 * @param value0 你的备注：%0 / Your Note: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_READ_TODAY_WORDS(String value0) {
		return new SM_SYSTEM_MESSAGE(1390124, value0);
	}

	/**
	 * 你：did not set Your Note。 / You did not set Your Note
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOSET_TODAY_WORDS = new SM_SYSTEM_MESSAGE(1390125);

	 /**
	  * 你的Note has been cleared。
	  * 你的Note has been cleared。 / Your Note has been cleared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CLEAR_TODAY_WORDS = new SM_SYSTEM_MESSAGE(1390126);

	/**
	 * Youdidnot 设置军团公告。 / You did not set the Legion Announcement
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOSET_GUILD_NOTICE = new SM_SYSTEM_MESSAGE(1390127);

	/**
	 * 军团公告有已 cleared。 / Legion Announcement has been cleared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CLEAR_GUILD_NOTICE = new SM_SYSTEM_MESSAGE(1390128);

	/**
	 * Youdidnot 设置自我介绍。 / You did not set the Self Intro
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOSET_GUILD_MEMBER_INTRO = new SM_SYSTEM_MESSAGE(1390129);

	/**
	 * Your 自我介绍有已 cleared。 / Your Self Intro has been cleared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CLEAR_GUILD_MEMBER_INTRO = new SM_SYSTEM_MESSAGE(1390130);

	/**
	 * @param value0 %0 抵抗了你的攻击。 / %0 resisted your attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_RESISTED_ME_TO_B(String value0) {
		return new SM_SYSTEM_MESSAGE(1390131, value0);
	}

	/**
	 * @param value0 你：resisted %0's attack。 / You resisted %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_RESISTED_A_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390132, value0);
	}

	/**
	 * %1 抵抗了 %0 的攻击。 / %1 resisted %0's attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_RESISTED_A_TO_B(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1390133, value1, value0);
	}

	/**
	 * @param value0 你：changed the connection status to %0。 / You changed the connection status to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CONNECTION_STATUS(String value0) {
		return new SM_SYSTEM_MESSAGE(1390134, value0);
	}

	/**
	 * @param value0 你：changed the group to the %0 state。 / You changed the group to the %0 state
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MY_PARTY_STATE(String value0) {
		return new SM_SYSTEM_MESSAGE(1390135, value0);
	}

	/**
	 * 你：have no authority to modify the Legion emblem。 / You have no authority to modify the Legion emblem
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_EMBLEM_DONT_HAVE_RIGHT = new SM_SYSTEM_MESSAGE(1390136);

	 /**
	  * Legion emblem has been changed。
	  * The Legion emblem has been changed
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_EMBLEM = new SM_SYSTEM_MESSAGE(1390137);

	/**
	 * Pleaseenter 名称的成员到 changerank。 / Please enter the name of the member to change the rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_NO_NAME = new SM_SYSTEM_MESSAGE(1390138);

	 /**
	  * rank to change is incorrect。
	  * The rank to change is incorrect
	  */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_INCORRECT_RIGHT = new SM_SYSTEM_MESSAGE(1390139);

	/**
	 * 你：cannot use a Rift while flying。 / You cannot use a Rift while flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_WHILE_FLYING = new SM_SYSTEM_MESSAGE(1390140);

	/**
	 * 累计游戏时间 %0 小时 %1 分；累计休息时间 %2 小时 %3 分。 / Your accumulated play time is %0 hour(s) %1 minute(s). Your accumulated rest time is %2 hour(s) %3 minute(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_NORMAL_REMAIN_PLAYTIME(String value0, String value1, String value2,
			String value3) {
		return new SM_SYSTEM_MESSAGE(1390141, value0, value1, value2, value3);
	}

	 /**
	  * 你的累计休息时间为 %0 小时 %1 分钟。
	  * Your accumulated rest time is %0 hour(s) %1 minute(s)
	  */
	public static SM_SYSTEM_MESSAGE STR_HEALTH_REMAIN_PLAYTIME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390142, value0, value1);
	}

	/**
	 * 你Tired, and the XP or item rewards gained are reduced to 50% of normal. Please log out and take a break for your health。 / You are Tired, and the XP or item rewards gained are reduced to 50% of normal. Please log out and take a break for your health
	 */
	public static final SM_SYSTEM_MESSAGE STR_TIRED_REMAIN_PLAYTIME = new SM_SYSTEM_MESSAGE(1390143);

	/**
	 * 你Exhausted, and the XP or item rewards gained are reduced to 0%. Please log out and take a break for your health. It will be returned to normal when the accumulated logout time reaches 5 hours。 / You are Exhausted, and the XP or item rewards gained are reduced to 0%. Please log out and take a break for your health. It will be returned to normal when the accumulated logout time reaches 5 hours
	 */
	public static final SM_SYSTEM_MESSAGE STR_PENALTY_REMAIN_PLAYTIME = new SM_SYSTEM_MESSAGE(1390144);

	/**
	 * 现实时间：%0 %1 / Real Time: %0 %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOCAL_TIME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390145, value0, value1);
	}

	/**
	 * 游戏时间：%0 %1 / Game Time: %0 %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GAME_TIME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390146, value0, value1);
	}

	/**
	 * Youdonot 有足够基纳到 payfee。 / You do not have enough Kinah to pay the fee
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SEND_MAIL_NOT_ENOUGH_FEE = new SM_SYSTEM_MESSAGE(1390147);

	/**
	 * 你：do not have the authority to use the Alert Chat。 / You do not have the authority to use the Alert Chat
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NO_AUTHORITY = new SM_SYSTEM_MESSAGE(1390148);

	/**
	 * 你：do not have enough space in the inventory。 / You do not have enough space in the inventory
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_FULL_INVENTORY = new SM_SYSTEM_MESSAGE(1390149);

	/**
	 * 你：cannot use items while crafting。 / You cannot use items while crafting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_USE_ITEM_DURING_COMBINE = new SM_SYSTEM_MESSAGE(1390150);

	/**
	 * 你：cannot use the entrance to the enemy territory。 / You cannot use the entrance to the enemy territory
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_TELEPORT_OPPOSITE_RACIAL = new SM_SYSTEM_MESSAGE(1390151);

	/**
	 * Youmustfirstcomplete 欧比斯 Entry 任务 / You must first complete the Abyss Entry Quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_TELEPORT_TO_ABYSS = new SM_SYSTEM_MESSAGE(1390152);

	/**
	 * @param value0 名称 must 为 entered 在 form 的 [%0character 名称 ]。 / The name must be entered in the form of [%0 character name]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CHAT_CMD_NEED_NAME_FIELD(String value0) {
		return new SM_SYSTEM_MESSAGE(1390153, value0);
	}

	/**
	 * You 无法 useskill 在当前 form。 / You cannot use the skill in the current form
	 */
	public static final SM_SYSTEM_MESSAGE STR_SKILL_CAN_NOT_CAST_IN_THIS_FORM = new SM_SYSTEM_MESSAGE(1390154);

	/**
	 * %0 的 %1 在 %2 使用 %3。 / %1 of %0 uses %3 in %2
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_ABYSS_SKILL_IS_FIRED(Player player, DescriptionId skill) {
		return new SM_SYSTEM_MESSAGE(1390155, player.getRace().getRaceDescriptionId(), player.getName(),
				"%SubZone:" + player.getPosition().getMapId() + " " + player.getPosition().getX() + " "
						+ player.getPosition().getY() + " " + player.getPosition().getZ(),
				skill);
	}

	/**
	 * Youcouldnot 移除 skilleffect 作为 yourDispelskilllevel 为 toolow。 / You could not remove the skill effect as your Dispel skill level is too low
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_DISPELLEVEL = new SM_SYSTEM_MESSAGE(1390156);

	/**
	 * Youcouldnot 移除全部 skilleffects 作为 youdonot 有 sufficientDispelskill 数量。 / You could not remove all the skill effects as you do not have sufficient Dispel skill count
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_DISPELCOUNT = new SM_SYSTEM_MESSAGE(1390157);

	/**
	 * Kiskyouregistered 作为 resurrection 绑定 point 有已 destroyed。 / The Kisk you registered as a resurrection bind point has been destroyed
	 */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_DESTROYED = new SM_SYSTEM_MESSAGE(1390158);

	/**
	 * Youregistered 当前 location 作为 resurrection 绑定 point。 / You registered the current location as a resurrection bind point
	 */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_REGISTER = new SM_SYSTEM_MESSAGE(1390159);

	/**
	 * You 可 install 仅一个 Kisk 在 time。 / You can install only one Kisk at a time
	 */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_ALREADY_INSTALLED = new SM_SYSTEM_MESSAGE(1390160);

	/**
	 * You 有已经 bound 在此 location。 / You have already bound at this location
	 */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_ALREADY_REGISTERED = new SM_SYSTEM_MESSAGE(1390161);

	/**
	 * 你：rolled the dice and got %0 (max. %num1)。 / You rolled the dice and got %0 (max. %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_RESULT_ME(int value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1390162, value0, num1);
	}

	 /**
	  * %0 rolled the dice and got %1 (max. %num2)。
	  * %0 rolled the dice and got %1 (max. %num2)
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_RESULT_OTHER(String value0, int value1, int num2) {
		return new SM_SYSTEM_MESSAGE(1390163, value0, value1, num2);
	}

	/**
	 * 你：gave up rolling the dice。 / You gave up rolling the dice
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DICE_GIVEUP_ME = new SM_SYSTEM_MESSAGE(1390164);

	/**
	 * @param value0 %0 放弃掷骰。 / %0 gave up rolling the dice
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_GIVEUP_OTHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1390165, value0);
	}

	/**
	 * Kisk you registered 正遭受攻击。 / The Kisk you registered is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_BINDSTONE_IS_ATTACKED = new SM_SYSTEM_MESSAGE(1390166);

	/**
	 * 小队优质物品分配门槛已改为上品及以上。 / Items subjected to the group's quality item distribution have been changed to Superior rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_RARE_QUALITY = new SM_SYSTEM_MESSAGE(
			1390167);

	/**
	 * 联盟优质物品分配门槛已改为上品及以上。 / Items subjected to the alliance's quality item distribution have been changed to Superior rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_RARE_QUALITY = new SM_SYSTEM_MESSAGE(
			1390168);

	/**
	 * 小队优质物品分配门槛已改为英雄及以上。 / Items subjected to the group's quality item distribution have been changed to Heroic rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_LEGEND_QUALITY = new SM_SYSTEM_MESSAGE(
			1390169);

	/**
	 * 联盟优质物品分配门槛已改为英雄及以上。 / Items subjected to the alliance's quality item distribution have been changed to Heroic rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_LEGEND_QUALITY = new SM_SYSTEM_MESSAGE(
			1390170);

	/**
	 * 小队优质物品分配门槛已改为传说及以上。 / Items subjected to the group's quality item distribution have been changed to Fabled rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_UNIQUE_QUALITY = new SM_SYSTEM_MESSAGE(
			1390171);

	/**
	 * 联盟优质物品分配门槛已改为传说及以上。 / Items subjected to the alliance's quality item distribution have been changed to Fabled rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_UNIQUE_QUALITY = new SM_SYSTEM_MESSAGE(
			1390172);

	/**
	 * You 无法添加 anymore 在 quality 物品 distribution 列表 .Pleasetryagainlater。 / You cannot add any more on the quality item distribution list. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_LIMIT_NUMBER = new SM_SYSTEM_MESSAGE(1390173);

	/**
	 * group ' squality 物品 distributionrules 有已 changed 到 Normal。 / The group's quality item distribution rules have been changed to Normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_DEFAULT = new SM_SYSTEM_MESSAGE(1390174);

	/**
	 * alliance ' squality 物品 distributionrules 有已 changed 到 Normal。 / The alliance's quality item distribution rules have been changed to Normal
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_DEFAULT = new SM_SYSTEM_MESSAGE(1390175);

	/**
	 * group ' squality 物品 distributionrules 有已 changed 到 DiceRoll。 / The group's quality item distribution rules have been changed to Dice Roll
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_DICE = new SM_SYSTEM_MESSAGE(1390176);

	/**
	 * 联盟优质物品分配规则已改为掷骰。 / The alliance's quality item distribution rules have been changed to Dice Roll
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_DICE = new SM_SYSTEM_MESSAGE(1390177);

	/**
	 * group ' squality 物品 distributionrules 有已 changed 到 Bidding。 / The group's quality item distribution rules have been changed to Bidding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_PAY = new SM_SYSTEM_MESSAGE(1390178);

	/**
	 * alliance ' squality 物品 distributionrules 有已 changed 到 Bidding。 / The alliance's quality item distribution rules have been changed to Bidding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_PAY = new SM_SYSTEM_MESSAGE(1390179);

	/**
	 * @param descriptionId 你现已成为 %0 的所有者。 / You are now the owner of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_GET_ITEM_ME(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1390180, descriptionId);
	}

	/**
	 * %0 现为 %1 的所有者。 / %0 is now the owner of %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_GET_ITEM_OTHER(String value0, DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1390181, value0, descriptionId);
	}

	/**
	 * You 无法 acquire 物品 becausethere 为 nospace 在 inventory。 / You cannot acquire the item because there is no space in the inventory
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DICE_INVEN_ERROR = new SM_SYSTEM_MESSAGE(1390182);

	 /**
	  * account was instantly settled。
	  * The account was instantly settled
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PAY_RESULT_ME = new SM_SYSTEM_MESSAGE(1390183);

	 /**
	  * %0 settled the account instantly。
	  * %0 settled the account instantly
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_PAY_RESULT_OTHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1390184, value0);
	}

	/**
	 * @param highestValue Yourbid 曾成功并 %num0 基纳有已 deducted。 / Your bid was successful and %num0 Kinah has been deducted
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PAY_ACCOUNT_ME(long highestValue) {
		return new SM_SYSTEM_MESSAGE(1390185, highestValue);
	}

	/**
	 * It was won by %0 for %num1 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PAY_ACCOUNT_OTHER(String value0, long highestValue) {
		return new SM_SYSTEM_MESSAGE(1390186, value0, highestValue);
	}

	/**
	 * %num0 基纳为 distributed%num2 基纳 each 到 %1 成员。 / %num0 Kinah is distributed %num2 Kinah each to %1 members
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PAY_DISTRIBUTE(long highestValue, int num2, long distributeKinah) {
		return new SM_SYSTEM_MESSAGE(1390187, highestValue, num2, distributeKinah);
	}

	/**
	 * @param value0 你：pause %0 temporarily。 / You pause %0 temporarily
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_PAUSE_START_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390188, value0);
	}

	/**
	 * %0 暂时暂停 %1。 / %0 pauses %1 temporarily
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_PAUSE_START_OTHER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390189, value0, value1);
	}

	/**
	 * %0 结束了暂停状态。 / %0 ended the pause state of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_PAUSE_END_ME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390190, value0, value1);
	}

	/**
	 * %0 取消暂停 %1。 / %0 unpauses %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_PAUSE_END_OTHER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390191, value0, value1);
	}

	/**
	 * 暂停时间结束，分配继续。 / The distribution resumes as the pause time is over
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOT_PAUSE_CALCEL = new SM_SYSTEM_MESSAGE(1390192);

	/**
	 * @param value0 你：rolled the dice and got a %0 (1~%1)。 / You rolled the dice and got a %0 (1~%1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_RESULT_EX_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390193, value0);
	}

	/**
	 * %0 掷骰得到 %1（1~%2）。 / %0's rolled the dice and got a %1 (1~%2)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_RESULT_EX_OTHER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390194, value0, value1);
	}

	/**
	 * %1 of the %0 killed the Guardian General
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_BOSS_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1390195, value1, value0);
	}

	/**
	 * %1 of the %0 has destroyed the Balaur Battleship Dredgion
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_CARRIER_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1390196, value1, value0);
	}

	 /**
	  * %0 seconds remain until you can cast it again。
	  * %0 seconds remain until you can cast it again
	  */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_INITIAL_TIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390197, value0);
	}

	/**
	 * 你：can use it only after the cooldown time is over。 / You can use it only after the cooldown time is over
	 */
	public static final SM_SYSTEM_MESSAGE STR_ARTIFACT_COOL_TIME = new SM_SYSTEM_MESSAGE(1390198);

	 /**
	  * Balaur have killed the Guardian General。
	  * The Balaur have killed the Guardian General
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DRAGON_BOSS_KILLED = new SM_SYSTEM_MESSAGE(1390199);

	/**
	 * 龙族摧毁了城门。 / The Balaur have destroyed the Castle Gate
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DRAGON_DOOR_BROKEN = new SM_SYSTEM_MESSAGE(1390200);

	/**
	 * 龙族摧毁了城门守护石。 / The Balaur have destroyed the Gate Guardian Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DRAGON_REPAIR_BROKEN = new SM_SYSTEM_MESSAGE(1390201);

	 /**
	  * Balaur have killed the Aetheric Field Generator。
	  * The Balaur have killed the Aetheric Field Generator
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_DRAGON_SHIELD_BROKEN = new SM_SYSTEM_MESSAGE(1390202);

	/**
	 * %0 占领了 %1 神器。 / %0 captured the %1 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_EVENT_WIN_ARTIFACT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390203, value0, value1);
	}

	/**
	 * %0 失去了 %1 神器。 / %0 lost the %1 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_EVENT_LOSE_ARTIFACT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390204, value0, value1);
	}

	 /**
	  * dredgion has vanished。
	  * The dredgion has vanished
	  */
	public static final SM_SYSTEM_MESSAGE STR_ABYSS_CARRIER_DESPAWN = new SM_SYSTEM_MESSAGE(1390205);

	/**
	 * %1 of the %0 has destroyed the Balaur Battleship Dredgion
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_GUILD_CARRIER_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1390206, value1, value0);
	}

	/**
	 * @param value0 你：have captured the %0 Artifact。 / You have captured the %0 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_WIN_FORT_TO_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390207, value0);
	}

	/**
	 * %1 of %0 has captured the %2 Artifact
	 */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_WIN_FORT(String value1, String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1390208, value1, value0, value2);
	}

	 /**
	  * %0 Artifact has been lost to %1。
	  * The %0 Artifact has been lost to %1
	  */
	public static SM_SYSTEM_MESSAGE STR_ARTIFACT_LOSE_FORT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390209, value0, value1);
	}

	/**
	 * 开始杂项物品自动分配。 / Starts the auto-distribution of miscellaneous items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_JUNK_DISTRIBUTE_ON = new SM_SYSTEM_MESSAGE(1390210);

	/**
	 * 结束杂项物品自动分配。 / Ends the auto-distribution of miscellaneous items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_JUNK_DISTRIBUTE_OF = new SM_SYSTEM_MESSAGE(1390211);

	/**
	 * You 无法 disbandyour 军团 whileyou 有物品或 moneyleft 在军团 warehouse。 / You cannot disband your Legion while you have items or money left in the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_DISPERSE_CANT_DISPERSE_GUILD_STORE_ITEM_IN_WAREHOUSE = new SM_SYSTEM_MESSAGE(
			1390212);

	/**
	 * @param value0 游戏时间：%0 / Playing Time: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PLAYING_TIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390213, value0);
	}

	/**
	 * @param value0 你：have played for %0 hour(s). Please take a break。 / You have played for %0 hour(s). Please take a break
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_NOTIFY_PLAYING_TIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1390214, value0);
	}

	/**
	 * @param value0 你：have joined the %0 Legion。 / You have joined the %0 Legion
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_INVITE_I_JOINED(String value0) {
		return new SM_SYSTEM_MESSAGE(1390215, value0);
	}

	/**
	 * @param num0 你：recovered %num0 HP。 / You recovered %num0 HP
	 */
	public static SM_SYSTEM_MESSAGE _STR_MSG_Heal_TO_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1390216, num0);
	}

	/**
	 * @param value0 You 可仅 buy 一个 %0。 / You can only buy one %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CAN_BUY_ONLY_ONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1390217, value0);
	}

	/**
	 * @param value0 Registering%0 在 quality 物品 distribution 列表。 / Registering %0 on the quality item distribution list
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_LISTING_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1390218, value0);
	}

	/**
	 * @param descriptionId %0 是待分配的优质物品之一。 / %0 is one of the quality items waiting to be distributed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LOOT_ALREADY_DISTRIBUTING_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1390219, descriptionId);
	}

	/**
	 * 你：do not have the ownership of this item。 / You do not have the ownership of this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOT_ANOTHER_OWNER_ITEM = new SM_SYSTEM_MESSAGE(1390220);

	/**
	 * @param descriptionId %0 技能难度过低，等级不再提升。 / The skill level for the %0 skill does not increase as the difficulty is too low
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_GET_PRODUCTION_EXP(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1390221, descriptionId);
	}

	/**
	 * 小队优质物品分配门槛已改为普通及以上。 / Items subjected to the group's quality item distribution have been changed to Common rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_TO_COMMON_QUALITY = new SM_SYSTEM_MESSAGE(
			1390222);

	/**
	 * 联盟优质物品分配门槛已改为普通及以上。 / Items subjected to the alliance's quality item distribution have been changed to Common rank or above
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_TO_COMMON_QUALITY = new SM_SYSTEM_MESSAGE(
			1390223);

	/**
	 * @param value0 你：have sent a friend request to %0。 / You have sent a friend request to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDY_REQUEST_ADD(String value0) {
		return new SM_SYSTEM_MESSAGE(1390224, value0);
	}

	/**
	 * 你：cannot connect to the game during the character reservation period。 / You cannot connect to the game during the character reservation period
	 */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_CONNECTION_FAIL_BY_CHAR_RES = new SM_SYSTEM_MESSAGE(1390225);

	/**
	 * 所有人放弃了掷骰。 / Everyone gave up the Dice Roll
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DICE_ALL_GIVEUP = new SM_SYSTEM_MESSAGE(1390226);

	/**
	 * 所有人放弃了竞价。 / Everyone gave up the Bidding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PAY_ALL_GIVEUP = new SM_SYSTEM_MESSAGE(1390227);

	/**
	 * 你：gave up the Bidding。 / You gave up the Bidding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PAY_GIVEUP_ME = new SM_SYSTEM_MESSAGE(1390228);

	/**
	 * @param value0 %0 放弃了竞价。 / %0 gave up the Bidding
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PAY_GIVEUP_OTHER(String value0) {
		return new SM_SYSTEM_MESSAGE(1390229, value0);
	}

	/**
	 * @param value0 你：cannot use this function in %0。 / You cannot use this function in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DISABLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1390230, value0);
	}

	/**
	 * @param num0 已登记的 Kisk 还可复活 %num0 次。 / The registered Kisk can resurrect %num0 times more
	 */
	public static SM_SYSTEM_MESSAGE STR_BINDSTONE_CAPACITY_LIMITTED_ALARM(int num0) {
		return new SM_SYSTEM_MESSAGE(1390231, num0);
	}

	 /**
	  * 你的Abyss Rank has changed to %0。
	  * 你的Abyss Rank has changed to %0。 / Your Abyss Rank has changed to %0
	  */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_CHANGE_RANK(String value0) {
		return new SM_SYSTEM_MESSAGE(1390232, value0);
	}

	/**
	 * You 无法为 promoted 作为 yourskilllevel 为 toolow。 / You cannot be promoted as your skill level is too low
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_RANK_UP = new SM_SYSTEM_MESSAGE(1390233);

	/**
	 * 专家无法接取工作订单。 / An Expert cannot take on a Work Order
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_GET_COMBINETASK = new SM_SYSTEM_MESSAGE(1390234);

	/**
	 * @param value0 你的欧比斯军阶已变更为 %0。请在欧比斯排名窗口查看。 / Your Abyss Rank has been changed to %0. Check the changed ranking on the Abyss Ranking Window
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_CHANGE_RANK_THIS_WEEK(String value0) {
		return new SM_SYSTEM_MESSAGE(1390235, value0);
	}

	/**
	 * 你：have learned the skill, %0 (Level - %1)。 / You have learned the skill, %0 (Level - %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_SKILL_LEARNED_ABYSS_SKILL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390236, value0, value1);
	}

	/**
	 * 仅联盟可用。 / Only available to alliances
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_SPLIT_FORCE = new SM_SYSTEM_MESSAGE(1390237);

	/**
	 * Please enter the amount of Kinah to distribute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTER_SPLIT_GOLD = new SM_SYSTEM_MESSAGE(1390238);

	/**
	 * 你：give up the Bidding as you do not have enough Kinah。 / You give up the Bidding as you do not have enough Kinah
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PAY_NOT_ENOUGH_MONEY = new SM_SYSTEM_MESSAGE(1390239);

	/**
	 * You 无法 join 军团作为玩家谁 invitedyou 为 nolonger 成员的军团。 / You cannot join the Legion as the player who invited you is no longer a member of the Legion
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_INVITE_CAN_NOT_JOIN_TO_GUILD_INVITOR_IS_LEFT = new SM_SYSTEM_MESSAGE(
			1390240);

	/**
	 * You 无法 kickout 军团成员的 equal 或 higherrank。 / You cannot kick out a Legion member of equal or higher rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_BANISH_CAN_NOT_BANISH_SAME_MEMBER_RANK = new SM_SYSTEM_MESSAGE(
			1390241);

	/**
	 * @param value0 你：have acquired the %0 title。 / You have acquired the %0 title
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_CASH_TITLE(int value0) {
		return new SM_SYSTEM_MESSAGE(1390242, new DescriptionId(value0));
	}

	/**
	 * @param value0 你：have acquired the %0 emote。 / You have acquired the %0 emote
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_CASH_SOCIALACTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1390243, value0);
	}

	 /**
	  * usage time of %0 title has expired。
	  * The usage time of %0 title has expired
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_TITLE_BY_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1390244, value0);
	}

	/**
	 * Usage time for the %0 emote has expired
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_SOCIALACTION_BY_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1390245, value0);
	}

	/**
	 * Usage time for the [Lodas Amulet] Bonus 20%% XP has expired
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_XPBOOST_BY_TIMEOUT = new SM_SYSTEM_MESSAGE(1390246);

	/**
	 * Youdistributed%num0 基纳到 %num1people , givingeach%num2 基纳。 / You distributed %num0 Kinah to %num1 people, giving each %num2 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SPLIT_ME_TO_B(int num0, int num1, int num2) {
		return new SM_SYSTEM_MESSAGE(1390247, num0, num1, num2);
	}

	 /**
	  * %0 distributed %num1 Kinah among %num2 people, giving %num3 Kinah each。
	  * %0 distributed %num1 Kinah among %num2 people, giving %num3 Kinah each
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_SPLIT_B_TO_ME(String value0, int num1, int num2, int num3) {
		return new SM_SYSTEM_MESSAGE(1390248, value0, num1, num2, num3);
	}

	 /**
	  * search found %num0 characters (max. 110)。
	  * The search found %num0 characters (max. 110)
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_WHO_DIALOG_RESULT(int num0) {
		return new SM_SYSTEM_MESSAGE(1390249, num0);
	}

	/**
	 * 小队拾取策略现为 %0。%1 物品将由 %2 分配。 / Group loot policy is now %0. %1 items will be distributed by %2
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PARTY_LOOTING_CHANGED_RULE(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1390250, value0, value1, value2);
	}

	/**
	 * 联盟拾取策略现为 %0。%1 物品将由 %2 分配。 / Alliance loot policy is now %0. %1 items will be distributed by %2
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_LOOTING_CHANGED_RULE(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1390251, value0, value1, value2);
	}

	/**
	 * 你：cannot be promoted anymore as you are an Expert。 / You cannot be promoted anymore as you are an Expert
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_RANK_UP_MASTER = new SM_SYSTEM_MESSAGE(1390252);

	/**
	 * 你：cannot be promoted anymore as you are at the highest rank。 / You cannot be promoted anymore as you are at the highest rank
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_RANK_UP_GATHERING = new SM_SYSTEM_MESSAGE(1390253);

	/**
	 * 你：have not acquired this quest。 / You have not acquired this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_OPEN_QUEST_LINK = new SM_SYSTEM_MESSAGE(1390254);

	/**
	 * 你已是专家，低等级制作不再提升技能等级。 / Your skill level does not increase with low level crafting as you are an Expert
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_GET_COMBINE_EXP = new SM_SYSTEM_MESSAGE(1390255);

	/**
	 * 此区域仅小队可进入。 / This area is only accessible to groups
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTER_ONLY_PARTY_DON = new SM_SYSTEM_MESSAGE(1390256);

	/**
	 * 你：do not have enough Medals。 / You do not have enough Medals
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_MEDAL = new SM_SYSTEM_MESSAGE(1390257);

	/**
	 * 已收到对角色 %0 的举报。剩余自动狩猎举报次数：%1。 / A report for the character %0 has been received. You have %1 auto hunting reports remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_SUBMIT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1390258, value0, value1);
	}

	/**
	 * @param value 距上次举报仅过 % 分钟。 / Only % minutes have passes since the last report
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANNOT_SUBMIT(String value) {
		return new SM_SYSTEM_MESSAGE(1390259, value);
	}

	/**
	 * @param value0 账号有已付费物品。是否让 %0 领取？ / There is a charged item issued to the account. Do you want %0 to have the charged item?
	 */
	public static SM_SYSTEM_MESSAGE STR_LOGIN_WARNING_GET_ITEM1(String value0) {
		return new SM_SYSTEM_MESSAGE(1390260, value0);
	}

	/**
	 * 物品发放后其他角色不可用。确定保留给 %0？ / Once the item is given, it cannot be used by other characters. Are you sure you want to keep it in %0?
	 */
	public static SM_SYSTEM_MESSAGE STR_LOGIN_WARNING_GET_ITEM2(String value0) {
		return new SM_SYSTEM_MESSAGE(1390261, value0);
	}

	/**
	 * 你：have joined the group。 / You have joined the group
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ENTERED_PARTY = new SM_SYSTEM_MESSAGE(1390262);

	/**
	 * 你：have joined the alliance。 / You have joined the alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_ENTERED_FORCE = new SM_SYSTEM_MESSAGE(1390263);

	/**
	 * Please complete your current quest first
	 */
	public static final SM_SYSTEM_MESSAGE STR_QUEST_ANOTHER_SINGLE_STEP_NOT_COMPLETED = new SM_SYSTEM_MESSAGE(1390264);

	/**
	 * 你：cannot join once it has started。 / You cannot join once it has started
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_LOCKED = new SM_SYSTEM_MESSAGE(1390265);

	/**
	 * %0 掷出最高（%0 为 %1，你为 %2）。 / %0 rolled the highest (%0 rolled %1, while you rolled %2)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE(String value0, String value3, String value1,
			String value2) {
		return new SM_SYSTEM_MESSAGE(1390266, value0, value3, value1, value2);
	}

	/**
	 * %0 掷出最高（%0 为 %1，你放弃）。 / %0 rolled the highest (%0 rolled %1, while you passed)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE_GIVEUP_ROLL(String value0, String value2,
			String value1) {
		return new SM_SYSTEM_MESSAGE(1390267, value0, value2, value1);
	}

	/**
	 * 你：rolled the highest。 / You rolled the highest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE_WIN = new SM_SYSTEM_MESSAGE(1390268);

	/**
	 * 所有人放弃掷骰。 / Everyone passed on rolling the dice
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE_GIVEUP_ROLL_ALL = new SM_SYSTEM_MESSAGE(
			1390269);

	/**
	 * 已断线玩家掷出最高（胜者 %1，你为 %1）。 / A now-disconnected player rolled the highest (they winner rolled %1, while you rolled %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE_OFFLINE_WINNER(String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1390270, value1, value2);
	}

	/**
	 * @param value1 已断线玩家掷出最高（胜者 %1，你放弃）。 / A now-disconnected player rolled the highest (they winner rolled %1, while you passed)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ITEM_PARTYNOTICE_DICE_GIVEUP_ROLL_OFFLINE_WINNER(String value1) {
		return new SM_SYSTEM_MESSAGE(1390271, value1);
	}

	/**
	 * 所选副本冷却时间无法重置。 / The selected Instanced Zone's cooldown time can't be reset
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_COOL_TIME_INIT = new SM_SYSTEM_MESSAGE(1390272);

	/**
	 * @param value0 %0 大喊： / %0 Shouts:
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_SHOUT_OUTPUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400000, value0);
	}

	/**
	 * %0 正在向 %1 求助。 / %0 is asking for help from %1
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_SHOUT_OUTPUT1(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400001, value0, value1);
	}

	/**
	 * @param value0 %0 大喊： / %0 Shouts:
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_SHOUT_OUTPUT_NPC(String value0) {
		return new SM_SYSTEM_MESSAGE(1400002, value0);
	}

	/**
	 * %0 正在向 %1 求助。 / %0 is asking for help from %1
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_SHOUT_OUTPUT1_NPC(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400003, value0, value1);
	}

	/**
	 * 你：shout "%0"。 / You shout "%0"
	 */
	public static final SM_SYSTEM_MESSAGE STR_CMD_SHOUT_INPUT = new SM_SYSTEM_MESSAGE(1400004);

	/**
	 * 你：shout for help。 / You shout for help
	 */
	public static final SM_SYSTEM_MESSAGE STR_CMD_SHOUT_INPUT1 = new SM_SYSTEM_MESSAGE(1400005);

	/**
	 * @param value0 %0 密语： / %0 Whispers:
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_WHISHPER_OUTPUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400006, value0);
	}

	/**
	 * @param value0 %0 密语： / %0 Whispers:
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_WHISHPER_OUTPUT_NPC(String value0) {
		return new SM_SYSTEM_MESSAGE(1400007, value0);
	}

	/**
	 * 你：Whisper to %1: %0。 / You Whisper to %1: %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_WHISHPER_INPUT(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400008, value1, value0);
	}

	 /**
	  * %0 has joined your group。
	  * %0 has joined your group
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_HE_ENTERED_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400009, value0);
	}

	 /**
	  * 你的小队已加入 %0 的联盟。
	  * 你的小队 has joined %0's 联盟。 / Your group has joined %0's alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ENTER_WITH_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400010, value0);
	}

	 /**
	  * %0 has joined the alliance。
	  * %0 has joined the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ENTER_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400011, value0);
	}

	/**
	 * %0's group has joined the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ENTER_WITH_HIS_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400012, value0);
	}

	 /**
	  * %0 has joined the alliance。
	  * %0 has joined the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_FORCE_HE_ENTERED_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400013, value0);
	}

	/**
	 * %0's group has joined the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HIS_PARTY_ENTERED_ALLIANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400014, value0);
	}

	 /**
	  * 你的小队已加入联盟。
	  * 你的小队 has joined the 联盟。 / Your group has joined the alliance
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_MY_PARTY_ENTERED_ALLIANCE = new SM_SYSTEM_MESSAGE(1400015);

	/**
	 * 你：have joined a group belonging to an alliance。 / You have joined a group belonging to an alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_ENTERY_PARTY_AND_ALLIANCE = new SM_SYSTEM_MESSAGE(1400016);

	 /**
	  * %0 has joined the alliance。
	  * %0 has joined the alliance
	  */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_HE_ENTERED_ALLIANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400017, value0);
	}

	/**
	 * @param value0 你：have joined %0's allliance。 / You have joined %0's allliance
	 */
	public static SM_SYSTEM_MESSAGE STR_FORCE_ENTER_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1400018, value0);
	}

	/**
	 * Legion Message: %0 %DATETIME1
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_NOTICE(String value0, long i) {
		return new SM_SYSTEM_MESSAGE(1400019, value0, i, 2);
	}

	/**
	 * Please report after you select a character from the same race
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DO_NOT_ACCUSE = new SM_SYSTEM_MESSAGE(1400020);

	 /**
	  * %0 killed the Guardian General。
	  * %0 killed the Guardian General
	  */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_NPC_BOSS_KILLED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400021, value0);
	}

	/**
	 * 你：cannot use a Rift。 / You cannot use a Rift
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL = new SM_SYSTEM_MESSAGE(1400022);

	/**
	 * %0%1%2 有 died 在 %3。 / %0 %1 %2 has died in %3
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_ORDER_RANKER_DIE(Player victim, DescriptionId rankDescId) {
		return new SM_SYSTEM_MESSAGE(1400023, victim.getRace().getRaceDescriptionId(), rankDescId, victim.getName(),
				"%SubZone:" + victim.getPosition().getMapId() + " " + victim.getPosition().getX() + " "
						+ victim.getPosition().getY() + " " + victim.getPosition().getZ());
	}

	/**
	 * You 无法 continuebattle 作为 you 有 insufficientAbyssPoints.Youwill 为 resurrected 在 %1 若 nothing 为 enteredwithin%0minutes。 / You cannot continue the battle as you have insufficient Abyss Points. You will be resurrected at %1 if nothing is entered within %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_RESURRECT_HERE_BY_ABYSS_POINT_ZERO(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400024, value1, value0);
	}

	 /**
	  * %0 has used the Gate Guardian Stone to repair the castle gate by %1。
	  * %0 has used the Gate Guardian Stone to repair the castle gate by %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_REPAIR_ABYSS_DOOR(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400025, value0, value1);
	}

	/**
	 * @param value0 你：have obtained %0 from the Internet Cafe Event。 / You have obtained %0 from the Internet Cafe Event
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_PCBANG_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400026, value0);
	}

	/**
	 * %WORLDNAME0% 区域限制进入。若全员离开或你退出小队，%1 小时内无法再进。 / %WORLDNAME0% region restricts access. You cannot reenter the region for %1 hour(s) if all your group members left the region or if you left the current group
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_COOL_TIME_HOUR(String worldname0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400027, worldname0, value1);
	}

	/**
	 * %WORLDNAME0% 区域限制进入。若全员离开或你退出小队，%1 分钟内无法再进。 / %WORLDNAME0% region restricts access. You cannot reenter the region for %1 minute(s) if all your group members left the region or if you left the current group
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_COOL_TIME_MIN(String worldname0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400028, worldname0, value1);
	}

	/**
	 * 你：may enter %WORLDNAME0 again after %1 hour(s)。 / You may enter %WORLDNAME0 again after %1 hour(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ENTER_INSTANCE_COOL_TIME_HOUR(String worldname0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400029, worldname0, value1);
	}

	/**
	 * 你：may enter %WORLDNAME0 again after %1 minute(s)。 / You may enter %WORLDNAME0 again after %1 minute(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ENTER_INSTANCE_COOL_TIME_MIN(String worldname0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400030, worldname0, value1);
	}

	/**
	 * @param value0 你：can enter %0 area now。 / You can enter %0 area now
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CAN_ENTER_INSTANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400031, value0);
	}

	/**
	 * @return %0: %1(%2)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CHECK_INSTANCE_COOL_TIME() {
		return new SM_SYSTEM_MESSAGE(1400032);
	}

	/**
	 * 为方便访问网站，游戏偏好已改为伪全屏模式。 / Changing Game Preferences to Pseudo Full Screen mode for convenient access of the website
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TEMP_PSEUDO_FULLSCREEN = new SM_SYSTEM_MESSAGE(1400033);

	/**
	 * Usage time for %0 has expired
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_ITEM_BY_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400034, value0);
	}

	/**
	 * You 无法 gainanyAbyssPoints 用于 while 作为 you 有 gainedtoomanyAbyssPoints 在 tooshortperiod 的 time。 / You cannot gain any Abyss Points for a while as you have gained too many Abyss Points in too short a period of time
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GET_AP_TIMEBASE_LIMIT = new SM_SYSTEM_MESSAGE(1400035);

	/**
	 * 试用已结束。%1，感谢游玩永恒之塔！请购买正式版继续游戏。 / Your trial has ended. %1, We hope you've enjoyed playing Aion! To continue to play, purchase Aion. Go to AionOnline.com to buy now!
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LEVEL_LIMIT_FREE_TIME(String value1) {
		return new SM_SYSTEM_MESSAGE(1400036, value1);
	}

	/**
	 * 你：may enter %0 again after %1 hour(s)。 / You may enter %0 again after %1 hour(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ENTER_INSTANCE_COOL_TIME_HOUR_CLIENT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400037, value0, value1);
	}

	/**
	 * 你：may enter %0 again after %1 minute(s)。 / You may enter %0 again after %1 minute(s)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ENTER_INSTANCE_COOL_TIME_MIN_CLIENT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400038, value0, value1);
	}

	 /**
	  * Macro has been registered。
	  * The Macro has been registered
	  */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_REGIST = new SM_SYSTEM_MESSAGE(1400039);

	/**
	 * 你：cannot register any more Macro。 / You cannot register any more Macro
	 */
	public static final SM_SYSTEM_MESSAGE STR_MACRO_MSG_CANNOT_REGIST = new SM_SYSTEM_MESSAGE(1400040);

	/**
	 * You 无法获取 anyAbyssPoint 从当前目标用于 while。 / You cannot get any Abyss Point from the current target for a while
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GET_AP_TARGET_LIMIT = new SM_SYSTEM_MESSAGE(1400041);

	/**
	 * 你不是该副本所属小队成员，即将离开副本。 / As you are not currently a member of the group for the Instanced Zone, you will be leaving the zone shortly
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEAVE_INSTANCE_NOT_PARTY = new SM_SYSTEM_MESSAGE(1400042);

	/**
	 * 区域已重置。重置后需等待再进时间。可用 /CheckEntry 查看。 / The zone has been reset. Once reset, you cannot enter the zone again until the reentry time expires. You can check the reentry time by typing '/CheckEntry'
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME = new SM_SYSTEM_MESSAGE(1400043);

	/**
	 * @param value0 你已离开副本区域。该区域将在 %0 分钟后重置。 / You have exited the Instanced Zone. This zone will be reset in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LEAVE_INSTANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400044, value0);
	}

	/**
	 * @param value0 你已离开副本区域。该区域将在 %0 分钟后重置。 / You have exited the Instanced Zone. This zone will be reset in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LEAVE_INSTANCE_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400045, value0);
	}

	/**
	 * @param value0 你已离开副本区域。该区域将在 %0 分钟后重置。 / You have exited the Instanced Zone. This zone will be reset in %0 minutes
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LEAVE_INSTANCE_FORCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400046, value0);
	}

	/**
	 * 本账号因未支付网吧费用被暂停。 / This account has been suspended for not paying the Internet Cafe usage charge
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BLOCK_PC_ROOM_COMPLAIN = new SM_SYSTEM_MESSAGE(1400047);

	/**
	 * @return 剩余游戏时间 %*0，预付额度剩余 %1。 / The playing time remaining is %*0, and there are %1 items of pre-paid credits left
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_PLAYTIME_WITH_RESERVATION(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400048, value0, value1);
	}

	/**
	 * 游戏时间将于 %*0 后到期。请在 Plaync 计费页续费。 / The playing time will expire in %*0. If you wish to continue using the service, please make additional payments on the Billing Page of the Plaync website
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ALARM_REMAIN_PLAYTIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400049, value0);
	}

	/**
	 * @param value0 你：have %*0 playing time remaining. Pre-paid credit will be applied afterward。 / You have %*0 playing time remaining. Pre-paid credit will be applied afterward
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ALARM_REMAIN_PLAYTIME_WITH_RESERVATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400050, value0);
	}

	 /**
	  * playing time has expired, and the game will end automatically in %*0。
	  * The playing time has expired, and the game will end automatically in %*0
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_COUNT_REMAIN_PLAYTIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400051, value0);
	}

	/**
	 * @param value0 游戏时间将于 %*0 后到期，到期后将使用预付额度。 / The playing time will expire in %*0. Pre-paid credit will be applied after it expires
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COUNT_REMAIN_PLAYTIME_WITH_RESERVATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400052, value0);
	}

	/**
	 * 站立 / standing
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_STANDING = new SM_SYSTEM_MESSAGE(1400053);

	/**
	 * 飞行中 / flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_PATH_FLYING = new SM_SYSTEM_MESSAGE(1400054);

	/**
	 * 飞行中 / flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_FREE_FLYING = new SM_SYSTEM_MESSAGE(1400055);

	/**
	 * 骑乘 / riding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_RIDING = new SM_SYSTEM_MESSAGE(1400056);

	/**
	 * 休息中 / resting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_SITTING = new SM_SYSTEM_MESSAGE(1400057);

	/**
	 * 坐下 / sitting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_SITTING_ON_CHAIR = new SM_SYSTEM_MESSAGE(1400058);

	/**
	 * 死亡 / dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_DEAD = new SM_SYSTEM_MESSAGE(1400059);

	/**
	 * 死亡 / dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_FLY_DEAD = new SM_SYSTEM_MESSAGE(1400060);

	/**
	 * 开设个人商店 / running a Private Store
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_PERSONAL_SHOP = new SM_SYSTEM_MESSAGE(1400061);

	/**
	 * 拾取中 / looting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_LOOTING = new SM_SYSTEM_MESSAGE(1400062);

	/**
	 * 拾取中 / looting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_FLY_LOOTING = new SM_SYSTEM_MESSAGE(1400063);

	/**
	 * in your current status
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACT_STATE_DEFAULT = new SM_SYSTEM_MESSAGE(1400064);

	/**
	 * You 无法 register 物品的 otherraces。 / You cannot register items of other races
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_OTHER_RACE = new SM_SYSTEM_MESSAGE(1400065);

	/**
	 * 本账号因未支付网吧费用被举报。若有误请联系客服。 / This account has been reported for not paying an internet caf? usage charge. If you believe this is an error, please contact customer support
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BLOCK_PC_ROOM_COMPLAIN2 = new SM_SYSTEM_MESSAGE(1400066);

	 /**
	  * Stigma is already equipped。
	  * The Stigma is already equipped
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_STIGMA_ALREADY_EQUIP_STONE = new SM_SYSTEM_MESSAGE(1400067);

	/**
	 * 你：must wait %DURATIONTIME0 to use the channel change function. Time Remaining: %DURATIONTIME1。 / You must wait %DURATIONTIME0 to use the channel change function. Time Remaining: %DURATIONTIME1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_CHANGE_CHANNEL_COOLTIME(String durationtime0, String durationtime1) {
		return new SM_SYSTEM_MESSAGE(1400068, durationtime0, durationtime1);
	}

	/**
	 * Youentered 到 Phase%num0Restrictionstatebecauseautohuntingreports 有 accumulated.You 可检查 RestrictionPhase 并 ReleaseTime 按 typing ' /Restriction ' command。 / You entered into the Phase %num0 Restriction state because the auto hunting reports have accumulated. You can check the Restriction Phase and the Release Time by typing the '/Restriction' command
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_UPGRADE_LEVEL(int num0) {
		return new SM_SYSTEM_MESSAGE(1400069, num0);
	}

	/**
	 * 因一段时间正当游玩，限制阶段已降至 %num0。请继续规范游戏。 / Your restriction phase has been lowered to %num0 as you played fair for a certain period of time. Please continue to play the game in a proper manner
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_DEGRADE_LEVEL(int num0) {
		return new SM_SYSTEM_MESSAGE(1400070, num0);
	}

	/**
	 * 你当前at Phase 1 Restriction State, and will be released in %0 minutes. While not affecting your game play in anyway, a continued accumulation of reports will however raise the Restriction Phase and will limit your gaining of XP and items。 / You are currently at Phase 1 Restriction State, and will be released in %0 minutes. While not affecting your game play in anyway, a continued accumulation of reports will however raise the Restriction Phase and will limit your gaining of XP and items
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_INFO_1_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400071, value0);
	}

	/**
	 * 你当前in Phase 2 Restriction State, and will be downgraded to Phase 1 in %0 minutes. You now receive less XP, Kinah and Abyss Points, and the chance of successful gathering and extraction has been decreased. You will face greater restrictions if reports continue to accumulate。 / You are currently in Phase 2 Restriction State, and will be downgraded to Phase 1 in %0 minutes. You now receive less XP, Kinah and Abyss Points, and the chance of successful gathering and extraction has been decreased. You will face greater restrictions if reports continue to accumulate
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_INFO_2_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400072, value0);
	}

	/**
	 * 你当前in Phase 3 Restriction State and will be downgraded to Phase 2 in %0 minutes. You cannot acquire any loot, and you now receive less XP, Kinah and Abyss Points, and the chance of successful gathering and extraction has been significantly decreased. You are banned from joining a Group or Alliance. You will face greater restrictions if reports continue to accumulate。 / You are currently in Phase 3 Restriction State and will be downgraded to Phase 2 in %0 minutes. You cannot acquire any loot, and you now receive less XP, Kinah and Abyss Points, and the chance of successful gathering and extraction has been significantly decreased. You are banned from joining a Group or Alliance. You will face greater restrictions if reports continue to accumulate
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_INFO_3_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400073, value0);
	}

	/**
	 * 你当前at Phase 4 Restriction State and will be downgraded to Phase 3 in %0 minutes. You cannot acquire any loot, XP, Kinah, or Abyss Points, and are unable to gather or extract any items. You are also banned from joining a Group or Alliance。 / You are currently at Phase 4 Restriction State and will be downgraded to Phase 3 in %0 minutes. You cannot acquire any loot, XP, Kinah, or Abyss Points, and are unable to gather or extract any items. You are also banned from joining a Group or Alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_INFO_4_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400074, value0);
	}

	/**
	 * 你：have consumed all equipped Power Shards。 / You have consumed all equipped Power Shards
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAPON_BOOST_MODE_BURN_OUT = new SM_SYSTEM_MESSAGE(1400075);

	/**
	 * 你in normal state。 / You are in normal state
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_INFO_NORMAL = new SM_SYSTEM_MESSAGE(1400076);

	/**
	 * You 无法删除 letterbecause 物品或基纳为 attached。 / You cannot delete the letter because items or Kinah are attached
	 */
	public static final SM_SYSTEM_MESSAGE STR_MAIL_ITEM_DEL_DENIED = new SM_SYSTEM_MESSAGE(1400077);

	/**
	 * 你：cannot open a private store while trading。 / You cannot open a private store while trading
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_OPEN_STORE_DURING_CRAFTING = new SM_SYSTEM_MESSAGE(1400078);

	/**
	 * 战斗中 / in combat
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASF_COMBAT = new SM_SYSTEM_MESSAGE(1400079);

	/**
	 * 移动中 / moving
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASF_MOVE_TYPE_WALK = new SM_SYSTEM_MESSAGE(1400080);

	/**
	 * using 技能 / using a skill
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASF_CASTING_SKILL = new SM_SYSTEM_MESSAGE(1400081);

	/**
	 * 滑翔中 / gliding
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ASF_GLIDE = new SM_SYSTEM_MESSAGE(1400082);

	/**
	 * Youreturned 到 normalstate 作为 youplayedfair 用于 certainperiod 的 time.Pleasecontinue 到 playgame 在 propermanner。 / You returned to the normal state as you played fair for a certain period of time. Please continue to play the game in a proper manner
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_DEGRADE_NORMAL_LEVEL = new SM_SYSTEM_MESSAGE(1400083);

	 /**
	  * %0 is not an appearance-modified item。
	  * %0 is not an appearance-modified item
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_NOT_SKIN_CHANGED_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400084, value0);
	}

	/**
	 * 你：cannot send auto hunting reports right now。 / You cannot send auto hunting reports right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ACCUSE = new SM_SYSTEM_MESSAGE(1400085);

	/**
	 * You 无法 reportautohunting 在当前 region。 / You cannot report auto hunting in the current region
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ACCUSE_CITY = new SM_SYSTEM_MESSAGE(1400086);

	/**
	 * @param value0 你：cannot issue commands in %0。 / You cannot issue commands in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SUMMON_CANT_ORDER_BY_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400087, value0);
	}

	/**
	 * 你：have already learned this emote。 / You have already learned this emote
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SOCIALACTION_ALREADY_HAS_SKILL = new SM_SYSTEM_MESSAGE(1400088);

	/**
	 * 此物品有 not 已 appearancemodified。 / This item has not been appearance modified
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHANGE_ITEM_SKIN_CANNOT_INVALID_ITEM = new SM_SYSTEM_MESSAGE(1400089);

	/**
	 * You 无法 report 作为 you 有 exceeded 编号的 autohuntingreportsallowed。 / You cannot report as you have exceeded the number of auto hunting reports allowed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANT_SUBMIT_BY_NO_COUNT = new SM_SYSTEM_MESSAGE(1400090);

	/**
	 * @param value0 你：currently have %0 auto hunting reports left。 / You currently have %0 auto hunting reports left
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_COUNT_INFO(String value0) {
		return new SM_SYSTEM_MESSAGE(1400091, value0);
	}

	/**
	 * selecteduser 无法为 invited 到 group 或 force。 / The selected user cannot be invited to a group or a force
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANT_BE_INVITED = new SM_SYSTEM_MESSAGE(1400092);

	/**
	 * You 有已 reportedtoomanytimes ,并无法 issueinvitation。 / You have been reported too many times, and cannot issue an invitation
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANT_INVITE_OTHER = new SM_SYSTEM_MESSAGE(1400093);

	/**
	 * You 无法 joingroup 作为 you 有已 reportedtoomanytimes 用于 autohunting。 / You cannot join the group as you have been reported too many times for auto hunting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANT_JOIN_PARTY = new SM_SYSTEM_MESSAGE(1400094);

	/**
	 * You 无法 joinAlliance 作为 you 有已 reportedtoomanytimes 用于 autohunting。 / You cannot join the Alliance as you have been reported too many times for auto hunting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_CANT_JOIN_FORCE = new SM_SYSTEM_MESSAGE(1400095);

	/**
	 * You 无法 usemanastone 在 selected 物品作为 manastonelevel 为 toohigh。 / You cannot use the manastone on the selected item as the manastone level is too high
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GIVE_ITEM_OPTION_CANT_FOR_TOO_HIGH_LEVEL = new SM_SYSTEM_MESSAGE(
			1400096);

	/**
	 * @param value0 当前无法召唤 %0。 / %0 cannot be summoned right now
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_CANNOT_ACCEPT_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400097, value0);
	}

	/**
	 * @param value0 确认等待超时，召唤 %0 已取消。 / Summoning of %0 is cancelled as the confirmation stand-by time has been exceeded
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_DONOT_ACCEPT_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400098, value0);
	}

	/**
	 * @param value0 你：declined %0's summoning。 / You declined %0's summoning
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_Reject_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400099, value0);
	}

	/**
	 * @param value0 %0 拒绝了你的召唤。 / %0 declined your summoning
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_Rejected_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400100, value0);
	}

	/**
	 * @param value0 召唤 %0 已取消。 / Summoning of %0 is cancelled
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_CANCEL_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400101, value0);
	}

	/**
	 * @param value0 You 无法 summon%0 作为 you 为已经 under 相同 effect。 / You cannot summon %0 as you are already under the same effect
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_Recall_DUPLICATE_EFFECT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400102, value0);
	}

	 /**
	  * %0 is currently unable to join a group or a force。
	  * %0 is currently unable to join a group or a force
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_OTHER_IS_BANISHED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400103, value0);
	}

	 /**
	  * gift has been delivered successfully。
	  * The gift has been delivered successfully
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_GIFT_SUCCESS = new SM_SYSTEM_MESSAGE(1400104);

	/**
	 * 你：have failed to purchase the item。 / You have failed to purchase the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_ERROR = new SM_SYSTEM_MESSAGE(1400105);

	/**
	 * 你：have chosen an invalid target to give the gift。 / You have chosen an invalid target to give the gift
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_NO_USER_TO_GIFT = new SM_SYSTEM_MESSAGE(1400106);

	/**
	 * 物品为 not 在列表。 / The item is not on the list
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_INVALID_GOODS = new SM_SYSTEM_MESSAGE(1400107);

	/**
	 * 你：do not have enough Cash Points。 / You do not have enough Cash Points
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_NOT_ENOUGH_POINT = new SM_SYSTEM_MESSAGE(1400108);

	 /**
	  * 你的种族无法购买所选物品。
	  * 你的种族 无法购买所选物品。 / Your race cannot purchase the selected item
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_INVALID_RACE = new SM_SYSTEM_MESSAGE(1400109);

	 /**
	  * 你的性别无法购买所选物品。
	  * 你的性别 无法购买所选物品。 / Your gender cannot purchase the selected item
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_INVALID_GENDER = new SM_SYSTEM_MESSAGE(1400110);

	 /**
	  * 你的Class cannot purchase the selected item。
	  * 你的Class cannot purchase the selected item。 / Your Class cannot purchase the selected item
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_INVALID_CLASS = new SM_SYSTEM_MESSAGE(1400111);

	/**
	 * 你：already have the selected title。 / You already have the selected title
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_DUPLICATED_TITLE = new SM_SYSTEM_MESSAGE(1400112);

	/**
	 * 你：already have the selected emote。 / You already have the selected emote
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_DUPLICATED_SOCIAL = new SM_SYSTEM_MESSAGE(1400113);

	/**
	 * 你：have purchased the cube expansion item。 / You have purchased the cube expansion item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_DUPLICATED_CUBE = new SM_SYSTEM_MESSAGE(1400114);

	/**
	 * @param value0 You 无法 register 作为 you 为 not%0。 / You cannot register as you are not %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_BINDSTONE_CANNOT_FOR_INVALID_RIGHT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400115, value0);
	}

	/**
	 * 你：cannot give gifts to yourself。 / You cannot give gifts to yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_CANNOT_GIVE_TO_ME = new SM_SYSTEM_MESSAGE(1400116);

	/**
	 * @param value0 You 无法发送 letter 到 %0becauseyou 有已 blocked 按玩家。 / You cannot send the letter to %0 because you have been blocked by the player
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MAIL_CANT_FOR_YOU_EXCLUDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400117, value0);
	}

	/**
	 * Network Status: %0 ms
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PING_RESULT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400118, value0);
	}

	/**
	 * 你：cannot remove a registered item。 / You cannot remove a registered item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCHANGE_CANNOT_UNREGISTER_ITEM = new SM_SYSTEM_MESSAGE(1400119);

	/**
	 * You 无法 registeramount 的基纳为 lowerthanregisteredamount。 / You cannot register an amount of Kinah that is lower than the registered amount
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCHANGE_CANNOT_DECREASE_MONEY = new SM_SYSTEM_MESSAGE(1400120);

	 /**
	  * client's regional code is not compatible with the game server。
	  * The client's regional code is not compatible with the game server
	  */
	public static final SM_SYSTEM_MESSAGE STR_ERROR_WORLD_LOCAL_CODE_FAIL = new SM_SYSTEM_MESSAGE(1400121);

	/**
	 * You 无法获取 anyPVPXP 从当前目标用于 while。 / You cannot get any PVP XP from the current target for a while
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GET_PVP_EXP_TARGET_LIMIT = new SM_SYSTEM_MESSAGE(1400122);

	/**
	 * You 无法获取 anyPVPXP 用于 while 作为 you 有 gainedtoomanyPVPXP 在 tooshortperiod 的 time。 / You cannot get any PVP XP for a while as you have gained too many PVP XP in too short a period of time
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GET_PVP_EXP_TIMEBASE_LIMIT = new SM_SYSTEM_MESSAGE(1400123);

	/**
	 * You 无法 register 目标作为 yourFriend 作为 you 有已 blocked 按玩家。 / You cannot register the target as your Friend as you have been blocked by the player
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_CANNOT_BLOCK_ME = new SM_SYSTEM_MESSAGE(1400124);

	/**
	 * 你：rolled the dice and got a %num0 (max. %num1)。 / You rolled the dice and got a %num0 (max. %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ITEM_DICE_CUSTOM_ME(int num0, int num1) {
		return new SM_SYSTEM_MESSAGE(1400125, num0, num1);
	}

	/**
	 * 你：rolled the dice and got a %num0 (max. %num1)。 / You rolled the dice and got a %num0 (max. %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_CUSTOM_ME(int num0, int num1) {
		return new SM_SYSTEM_MESSAGE(1400126, num0, num1);
	}

	 /**
	  * %0 rolled the dice and got a %num1 (max. %num2)。
	  * %0 rolled the dice and got a %num1 (max. %num2)
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DICE_CUSTOM_OTHER(String value0, int num1, int num2) {
		return new SM_SYSTEM_MESSAGE(1400127, value0, num1, num2);
	}

	/**
	 * You 无法 invite 玩家到 force 作为 groupleader 的玩家为在 InstancedZone。 / You cannot invite the player to the force as the group leader of the player is in an Instanced Zone
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_INVITE_WHEN_HE_IS_IN_INSTANCE = new SM_SYSTEM_MESSAGE(1400128);

	/**
	 * You 无法 useselectedfunction 在当前 restrictionphase。 / You cannot use the selected function in the current restriction phase
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_TARGET_IS_NOT_VALID = new SM_SYSTEM_MESSAGE(1400129);

	/**
	 * You 无法 preview 此物品作为其可仅为 used 按 oppositesex ,。 / You cannot preview this item as it can only be used by the opposite sex,
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PREVIEW_INVALID_GENDER = new SM_SYSTEM_MESSAGE(1400130);

	/**
	 * You 有物品( s ) left 到 settle 在 Broker。 / You have item(s) left to settle at the Broker
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_VENDOR_ACCOUNT_IS_NOT_EMPTY = new SM_SYSTEM_MESSAGE(1400131);

	/**
	 * 你：cannot use a Rift until the curse is removed。 / You cannot use a Rift until the curse is removed
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_BY_SLAYER = new SM_SYSTEM_MESSAGE(1400132);

	 /**
	  * %0 has logged in。
	  * %0 has logged in
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_NOTIFY_LOGIN_GUILD(String value0) {
		return new SM_SYSTEM_MESSAGE(1400133, value0);
	}

	/**
	 * @param value0 你：have sold %0。 / You have sold %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PERSONAL_SHOP_SELL_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400134, value0);
	}

	/**
	 * 你：have sold %num1 %0s。 / You have sold %num1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PERSONAL_SHOP_SELL_ITEM_MULTI(int num1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400135, num1, value0s);
	}

	/**
	 * 你：can now use the chatting functions again。 / You can now use the chatting functions again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAN_CHAT_NOW = new SM_SYSTEM_MESSAGE(1400136);

	/**
	 * 你因在敌对领地击杀过多低等级目标，现处于神族之主 %0 级诅咒。 / You are now under level %0 curse of the Empyrean Lords for killing too many lower level targets in the opposition territory
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_UPGRADE_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400137, value0);
	}

	/**
	 * 因一段时间未击杀低等级目标，天族/魔族之主诅咒降至 %0 级。 / The curse of the Empyrean Lords has been reduced to %0 level because you haven't slaughtered the lower level targets for a certain time
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DEGRADE_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400138, value0);
	}

	/**
	 * 因一段时间未击杀低等级目标，天族/魔族之主诅咒已移除。 / The curse of Empyrean Lord has been removed because you haven't slaughtered the lower level targets for a certain time
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DEGRADE_TO_NOMAL_LEVEL = new SM_SYSTEM_MESSAGE(1400139);

	/**
	 * Brave %0 has defeated notorious %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400140, value0, value1);
	}

	/**
	 * 魔族英雄 %0 击杀了受神罚的入侵者 %1。 / Hero of Asmodian %0 killed the Divinely Punished Intruder %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_LIGHT_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400141, value0, value1);
	}

	/**
	 * 天族英雄 %0 击杀了受神罚的入侵者 %1。 / Hero of Elyos %0 killed the Divinely Punished Intruder %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DARK_DEATH_TO_B(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400142, value0, value1);
	}

	/**
	 * 你因在敌对领地击杀过多低等级目标，现处于 %0 状态。 / You are now in %0 state because you've killed too many lower level targets in the opposition territory
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_UP_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400143, value0);
	}

	/**
	 * 因一段时间未击杀低等级目标，%0 已降至 %1。 / %0 has been lowered to %1 because you haven't killed the lower level targets for a certain time
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DOWN_LEVEL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400144, value0, value1);
	}

	/**
	 * @param value0 因一段时间未击杀低等级目标，%0 已移除。 / %0 is removed because you haven't killed the lower level targets for a certain time
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SLAYER_DOWN_TO_NOMAL_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400145, value0);
	}

	/**
	 * %0 正在制作 %1。 / %0 is crafting %1
	 */
	public static SM_SYSTEM_MESSAGE STR_OTHER_combine_START(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400146, value0, value1);
	}

	/**
	 * %0 成功制作了 %1。 / %0 successfully crafted %1
	 */
	public static SM_SYSTEM_MESSAGE STR_OTHER_combine_SUCCESS(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400147, value0, value1);
	}

	/**
	 * %0 制作 %1 失败。 / %0 failed to craft %1
	 */
	public static SM_SYSTEM_MESSAGE STR_OTHER_combine_FAIL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400148, value0, value1);
	}

	/**
	 * @param value0 你：cannot use a Rift until the %0 is removed。 / You cannot use a Rift until the %0 is removed
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_BY_SLAYER_GRADE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400149, value0);
	}

	/**
	 * 仅军团军团长可 changehis 军团名称。 / Only the Legion Brigade General can change his Legion name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_ERROR_ONLY_MASTER_CAN_CHANGE_NAME = new SM_SYSTEM_MESSAGE(
			1400150);

	/**
	 * Invalid character name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_CHAR_NAME_ERROR_WRONG_INPUT = new SM_SYSTEM_MESSAGE(1400151);

	/**
	 * Invalid Legion name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_ERROR_WRONG_INPUT = new SM_SYSTEM_MESSAGE(1400152);

	/**
	 * character 名称为已经在 use.Enteranother 名称。 / The character name is already in use. Enter another name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_CHAR_NAME_ERROR_SAME_YOUR_NAME = new SM_SYSTEM_MESSAGE(1400153);

	/**
	 * 军团名称为已经在 use.Enteranother 名称。 / The Legion name is already in use. Enter another name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_ERROR_SAME_YOUR_NAME = new SM_SYSTEM_MESSAGE(1400154);

	/**
	 * character 为 using 名称 .Enteranother 名称。 / A character is using the name. Enter another name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_CHAR_NAME_ALREADY_EXIST = new SM_SYSTEM_MESSAGE(1400155);

	/**
	 * 军团为 using 名称 .Enteranother 名称。 / A Legion is using the name. Enter another name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_ALREADY_EXIST = new SM_SYSTEM_MESSAGE(1400156);

	/**
	 * @param value0 character 名称有已 changed 到 %0。 / The character name has been changed to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EDIT_CHAR_NAME_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1400157, value0);
	}

	/**
	 * @param value0 军团名称有已 changed 到 %0。 / The Legion name has been changed to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_SUCCESS(String value0) {
		return new SM_SYSTEM_MESSAGE(1400158, value0);
	}

	/**
	 * Failed to change the name. Error code is %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EDIT_NAME_ERROR_DEFAULT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400159, value0);
	}

	/**
	 * You 无法 change 军团名称 whileoccupyingfortress 或 Artifact。 / You cannot change the Legion name while occupying the fortress or Artifact
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_CANT_FOR_HAVING_HOUSE = new SM_SYSTEM_MESSAGE(
			1400160);

	/**
	 * You 可 neithertalk 带 NPCsnoruseanyusefulfunctions 在 your 当前 RestrictionPhase。 / You can neither talk with NPCs nor use any useful functions in your current Restriction Phase
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOT_CANNOT_USE_NPC_UTILITY = new SM_SYSTEM_MESSAGE(1400161);

	/**
	 * You 无法 trade 带 othercharacters 在 your 当前 RestrictionPhase。 / You cannot trade with other characters in your current Restriction Phase
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOT_CANNOT_USE_PC_TRADE = new SM_SYSTEM_MESSAGE(1400162);

	/**
	 * 你因挂机举报累计达上限，已自动被移出小队。 / You are automatically excluded from the group because the auto hunting reports have accumulated to the limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_BANISHED_FROM_PARTY = new SM_SYSTEM_MESSAGE(1400163);

	/**
	 * 你因挂机举报累计达上限，已自动被移出团队。 / You are automatically excluded from the force because the auto hunting reports have accumulated to the limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ACCUSE_BANISHED_FROM_FORCE = new SM_SYSTEM_MESSAGE(1400164);

	/**
	 * 当前限制阶段下安息能量无效。 / The Energy of Repose is ineffective in your current Restriction Phase
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOT_CANNOT_RECEIVE_VITAL_BONUS = new SM_SYSTEM_MESSAGE(1400165);

	/**
	 * 所选用户当前无法交易。 / The selected user cannot do any trading at the moment
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_USE_PC_TRADE_TO_BOT = new SM_SYSTEM_MESSAGE(1400166);

	/**
	 * You 无法 glide 在此 area。 / You cannot glide in this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOGLIDE_AREA = new SM_SYSTEM_MESSAGE(1400167);

	/**
	 * 你已进入禁滑翔区域，被迫停止滑翔。 / You are forced to stop gliding because you've entered the no glide area
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOGLIDE_AREA_STOP = new SM_SYSTEM_MESSAGE(1400168);

	/**
	 * @param durationtime0 已登记 Kisk 剩余有效时间：%DURATIONTIME0 / The remaining active time of the registered Kisk is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_BINDSTONE_WARNING_REMAIN_TIME(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400169, durationtime0);
	}

	/**
	 * You 无法 change 名称的军团 duringdisbandingmode。 / You cannot change the name of the Legion during the disbanding mode
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EDIT_GUILD_NAME_CANT_FOR_DISPERSING_GUILD = new SM_SYSTEM_MESSAGE(
			1400170);

	/**
	 * You 无法 reportautohunting 在当前 region。 / You cannot report auto hunting in the current region
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ACCUSE_IN_THIS_ZONE = new SM_SYSTEM_MESSAGE(1400171);

	/**
	 * 你：have purchased the warehouse expansion item。 / You have purchased the warehouse expansion item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INGAMESHOP_DUPLICATED_WAREHOUSE = new SM_SYSTEM_MESSAGE(1400172);

	/**
	 * 频道 Host / Channel Host
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_HOST = new SM_SYSTEM_MESSAGE(1400173);

	/**
	 * 作为 yourcharacter 名称有 changed , you 为 removed 从全部加入 channels。 / As your character name has changed, you are removed from all joined channels
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_CHAR_NAME_CHANGED1 = new SM_SYSTEM_MESSAGE(1400174);

	/**
	 * 角色名已变更，你已从所有已加入频道（含私人频道）移除。 / As your character name has changed, you are removed from all joined channels. (including any participating private channels)
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_CHAR_NAME_CHANGED2 = new SM_SYSTEM_MESSAGE(1400175);

	/**
	 * 小队 / Group
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_PARTY = new SM_SYSTEM_MESSAGE(1400176);

	/**
	 * @param value0 You 可 nolongeruse%0 作为编号的 allowedusage 有已 reached。 / You can no longer use %0 as the number of allowed usage has been reached
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_USE_DUPLICATED_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400177, value0);
	}

	/**
	 * 你：may reenter %WORLDNAME1 after %DURATIONTIME0 has passed。 / You may reenter %WORLDNAME1 after %DURATIONTIME0 has passed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_COOL_TIME_REMAIN(String worldname1, String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400178, worldname1, durationtime0);
	}

	/**
	 * 你：cannot enter the selected Instanced Zone at your level。 / You cannot enter the selected Instanced Zone at your level
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ENTER_LEVEL = new SM_SYSTEM_MESSAGE(1400179);

	/**
	 * %num0 玩家 limit 的 %WORLDNAME1 有已 exceeded。 / The %num0 player limit of %WORLDNAME1 has been exceeded
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(int num0, String worldname1) {
		return new SM_SYSTEM_MESSAGE(1400180, num0, worldname1);
	}

	/**
	 * @param worldname0 你：have already applied to enter %WORLDNAME0。 / You have already applied to enter %WORLDNAME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400181, worldname0);
	}

	/**
	 * 仅战团队长、副队长或小队长可申请小队进入。 / Only the force captain, vice captain or group leader can apply for group entry
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_NOT_LEADER = new SM_SYSTEM_MESSAGE(1400182);

	/**
	 * @param value0 你：aborted entering %0. You can apply again after 10 seconds。 / You aborted entering %0. You can apply again after 10 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REGISTER_CANCELED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400183, value0);
	}

	/**
	 * @param value0 Youabortedentering0Youcan 尝试再次后10seconds / You aborted entering %0. You can try again after 10 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_ENTER_GIVEUP(String value0) {
		return new SM_SYSTEM_MESSAGE(1400184, value0);
	}

	/**
	 * 你当前无法进入副本区域。 / You are not able to enter the Instanced Zone right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ENTER_STATE = new SM_SYSTEM_MESSAGE(1400185);

	/**
	 * 你：have failed to make an entry application。 / You have failed to make an entry application
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ENTER_NOTICE = new SM_SYSTEM_MESSAGE(1400186);

	/**
	 * @param value0 %0 当前无法进入副本。 / %0 is not able to enter the Instanced Zone right now
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ENTER_MEMBER(String value0) {
		return new SM_SYSTEM_MESSAGE(1400187, value0);
	}

	/**
	 * @param value0 因 %0 频道删除，你的私人频道编号可能已变更。 / The number of your private channel may have been changed with the deletion of the %0 Channel
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CHANGE_CHANNEL5(String value0) {
		return new SM_SYSTEM_MESSAGE(1400188, value0);
	}

	/**
	 * @param value0 你：have applied to join %0's group。 / You have applied to join %0's group
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_JUST_SENT_APPLY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400189, value0);
	}

	/**
	 * @param value0 你：have invited %0 to join your group。 / You have invited %0 to join your group
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_JUST_INVITE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400190, value0);
	}

	/**
	 * @param value0 你：have applied to join %0's alliance。 / You have applied to join %0's alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_JUST_SENT_APPLY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400191, value0);
	}

	/**
	 * @param value0 你已邀请 %0 加入联盟。 / You have invited %0 to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_JUST_INVITE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400192, value0);
	}

	/**
	 * 玩家为已经 beingresurrected。 / That player is already being resurrected
	 */
	public static final SM_SYSTEM_MESSAGE STR_OTHER_USER_USE_RESURRECTDEBUFF_SKILL_ALREADY = new SM_SYSTEM_MESSAGE(
			1400193);

	/**
	 * 你：have successfully made an entry application。 / You have successfully made an entry application
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REGISTER_SUCCESS = new SM_SYSTEM_MESSAGE(1400194);

	/**
	 * @param durationtime0 剩余攻击时间 %DURATIONTIME0。 / The attack time remaining is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REMAIN_TIME(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400195, durationtime0);
	}

	/**
	 * 暗波埃塔渗透任务开始。 / Infiltration of Dark Poeta now commences
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_START_IDLF1 = new SM_SYSTEM_MESSAGE(1400196);

	/**
	 * @param value0 %0 正遭受攻击。 / %0 is under attack
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_BOSS_ATTACKED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400198, value0);
	}

	 /**
	  * %0 has destroyed %1。
	  * %0 has destroyed %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_ROOM_DESTROYED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400199, value0, value1);
	}

	 /**
	  * group or force no longer exists。
	  * The group or force no longer exists
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_NOT_EXIST = new SM_SYSTEM_MESSAGE(1400200);

	/**
	 * %num0 玩家 limit 的 %WORLDNAME1 有已 exceeded。 / The %num0 player limit of %WORLDNAME1 has been exceeded
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_ENTER_INSTANCE_MAX_COUNT(int num0, String worldname1) {
		return new SM_SYSTEM_MESSAGE(1400201, num0, worldname1);
	}

	/**
	 * @param durationtime0 剩余攻击时间 %DURATIONTIME0。 / The attack time remaining is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REMAIN_TIME_60(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400202, durationtime0);
	}

	/**
	 * @param durationtime0 剩余攻击时间 %DURATIONTIME0。 / The attack time remaining is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REMAIN_TIME_30(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400203, durationtime0);
	}

	/**
	 * @param durationtime0 剩余攻击时间 %DURATIONTIME0。 / The attack time remaining is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REMAIN_TIME_10(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400204, durationtime0);
	}

	/**
	 * @param durationtime0 剩余攻击时间 %DURATIONTIME0。 / The attack time remaining is %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REMAIN_TIME_5(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400205, durationtime0);
	}

	 /**
	  * effective time has expired and the link is no longer active。
	  * The effective time has expired and the link is no longer active
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CMD_LINK_EXPIRED = new SM_SYSTEM_MESSAGE(1400206);

	/**
	 * @param value0 %0 已是联盟成员，无法申请加入所选小队。 / %0 can't apply to join the selected group as he or she is already a member of an alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_CANT_USE_PARTY_MATCH(String value0) {
		return new SM_SYSTEM_MESSAGE(1400207, value0);
	}

	/**
	 * You 无法 preview 此物品作为其可仅为 used 按 otherrace。 / You cannot preview this item as it can only be used by the other race
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PREVIEW_INVALID_RACE = new SM_SYSTEM_MESSAGE(1400208);

	/**
	 * You 无法 preview 此物品作为 you 可' tuse 此 appearancemodifying 物品。 / You cannot preview this item as you can't use this appearance modifying item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PREVIEW_INVALID_COSMETIC = new SM_SYSTEM_MESSAGE(1400209);

	/**
	 * You 无法 preview 此物品作为 there 为 noappearanceimage。 / You cannot preview this item as there is no appearance image
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PREVIEW_NO_EXIST_COSMETIC_DATA = new SM_SYSTEM_MESSAGE(1400210);

	/**
	 * There 为 noGreaterStigma 槽位可用。 / There is no Greater Stigma slot available
	 */
	public static final SM_SYSTEM_MESSAGE STR_ENHANCED1_STIGMA_SLOT_IS_NOT_OPENED = new SM_SYSTEM_MESSAGE(1400211);

	/**
	 * You 无法 useinvite , leave 或 kickcommandsrelated 到 yourgroup 或 force 在此 region。 / You cannot use invite, leave or kick commands related to your group or force in this region
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_OPERATE_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1400212);

	/**
	 * @param value0 Youmustfirstlearn 前置条件 skill 到 equip%0。 / You must first learn the prerequisite skill to equip %0
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_TO_EQUIP_STONE_LEARN_PRESKILL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400213, value0);
	}

	/**
	 * 你：cannot deactivate %0 as it is a prerequisite skill of %1。 / You cannot deactivate %0 as it is a prerequisite skill of %1
	 */
	public static SM_SYSTEM_MESSAGE STR_STIGMA_CANT_UNEQUIP_STONE_FOR_AFTERSKILL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400214, value0, value1);
	}

	/**
	 * @param value0 You 已经 applied 到 join%0 ' sgroup.Youmayapply 用于 RecruitGrouponceevery15seconds。 / You already applied to join %0's group. You may apply for Recruit Group once every 15 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_ALREADY_SENT_APPLY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400215, value0);
	}

	/**
	 * You 已经 applied 到 join%0 ' sforce.Youmayapply 用于 RecruitAllianceonceevery15seconds。 / You already applied to join %0's force. You may apply for Recruit Alliance once every 15 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_ALREADY_SENT_APPLY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400216, value0);
	}

	/**
	 * 很遗憾，下次再一起玩吧。祝你在 Aion 玩得开心！ / It's a shame, but let's play together next time. Have a good time in Aion!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_DECLINED = new SM_SYSTEM_MESSAGE(1400217);

	/**
	 * 很遗憾，下次再一起玩吧。祝你在 Aion 玩得开心！ / It's a shame, but let's play together next time. Have a good time in Aion!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_DECLINED = new SM_SYSTEM_MESSAGE(1400218);

	/**
	 * 你：cannot enter as you do not have the required item。 / You cannot enter as you do not have the required item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM = new SM_SYSTEM_MESSAGE(1400219);

	/**
	 * You 不有物品 needed 用于 entry.Please 检查,并 ask 用于 accessagain。 / You don't have the item needed for entry. Please check, and ask for access again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM_TRY_LATER = new SM_SYSTEM_MESSAGE(
			1401056);

	/**
	 * %DURATIONTIME0 remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_TIME(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400220, durationtime0);
	}

	/**
	 * %DURATIONTIME0 remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_TIME_60(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400221, durationtime0);
	}

	/**
	 * %DURATIONTIME0 remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_TIME_30(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400222, durationtime0);
	}

	/**
	 * %DURATIONTIME0 remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_TIME_10(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400223, durationtime0);
	}

	/**
	 * %DURATIONTIME0 remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_TIME_5(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400224, durationtime0);
	}

	/**
	 * Characters under level %0 cannot use Channel Chat
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_CHANNELCHAT_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400225, value0);
	}

	 /**
	  * Portside Defense Shield has been generated at the Ready Room 1。
	  * The Portside Defense Shield has been generated at the Ready Room 1
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEFTWALL_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400226);

	 /**
	  * Starboard Defense Shield has been generated at the Ready Room 2。
	  * The Starboard Defense Shield has been generated at the Ready Room 2
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RIGHTWALL_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400227);

	 /**
	  * Portside Central Teleporter has been generated at the Escape Hatch。
	  * A Portside Central Teleporter has been generated at the Escape Hatch
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEFTTELEPORTER_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(
			1400228);

	/**
	 * 右舷中央传送器已在副逃生舱口生成。 / A Starboard Central Teleporter has been generated at the Secondary Escape Hatch
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RIGHTTELEPORTER_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(
			1400229);

	 /**
	  * Portside Door of Captain's Cabin has been destroyed。
	  * The Portside Door of Captain's Cabin has been destroyed
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEFTDOOR_DESTROYED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400230);

	 /**
	  * Starboard Door of Captain's Cabin has been destroyed。
	  * The Starboard Door of Captain's Cabin has been destroyed
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RIGHTDOOR_DESTROYED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400231);

	/**
	 * 剩余 %num1 个 %0 / %num1 %0(s) remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_LEFT(int num1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400232, num1, value0s);
	}

	/**
	 * 准备战斗！ / Prepare for Battle!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_PREPARE_TIME = new SM_SYSTEM_MESSAGE(1400233);

	/**
	 * 船长室传送装置已在中央通道尽头生成，可用 3 分钟。 / A Captain's Cabin Teleport Device that can be used for 3 minutes has been generated at the end of the Central Passage
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSSTELEPORTER_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(
			1400234);

	/**
	 * @param worldname0 You 无法 enter%WORLDNAME0 作为 entrytime 有 expired。 / You cannot enter %WORLDNAME0 as the entry time has expired
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_ENTER_FOR_TIMEOVER(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400235, worldname0);
	}

	/**
	 * @param num0 超过 %num0 分！ / Exceeded %num0 points!
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_SCORE_ALARM(int num0) {
		return new SM_SYSTEM_MESSAGE(1400236, num0);
	}

	/**
	 * 你：have gained %num1 points from %0。 / You have gained %num1 points from %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_SCORE(int score, int objectNameId) {
		return new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(objectNameId * 2 + 1), score);
	}

	/**
	 * You 无法 openprivate 商店在此 region。 / You cannot open a private store in this region
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_OPEN_STORE_IN_THIS_ZONE = new SM_SYSTEM_MESSAGE(1400238);

	/**
	 * @param value0 你：have joined the %0 region channel。 / You have joined the %0 region channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_JOIN_ZONE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400239, value0);
	}

	/**
	 * @param value0 你：have joined the %0 trade channel。 / You have joined the %0 trade channel
	 */
	public static SM_SYSTEM_MESSAGE STR_CHAT_CHANNEL_JOIN_TRADE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400240, value0);
	}

	/**
	 * @param value0 你：have already sent an Unavailable message to %0。 / You have already sent an Unavailable message to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PARTY_MATCH_ALREADY_SENT_DECLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400241, value0);
	}

	/**
	 * @param value0 你：have already sent a Reject Alliance message to %0。 / You have already sent a Reject Alliance message to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FORCE_MATCH_ALREADY_SENT_DECLINE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400242, value0);
	}

	 /**
	  * protective magic ward of Balaur has been activated。
	  * The protective magic ward of Balaur has been activated
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_START_IDABRE = new SM_SYSTEM_MESSAGE(1400243);

	/**
	 * All the treasure chests of Balaur have disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TREASUREBOX_DESPAWN_ALL = new SM_SYSTEM_MESSAGE(1400244);

	/**
	 * One treasure chest of Balaur has disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TREASUREBOX_DESPAWN_ONE = new SM_SYSTEM_MESSAGE(1400245);

	/**
	 * %0 已开启，现在可进入 %1。 / %0 is open and you can now access %1
	 */
	public static SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_OPEN_DOOR(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400246, value0, value1);
	}

	/**
	 * You 无法 registerbecauselimit 的 characters 可 register 在 Kisk 有已 reached。 / You cannot register because the limit of characters that can register on the Kisk has been reached
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_REGISTER_BINDSTONE_FULL = new SM_SYSTEM_MESSAGE(1400247);

	/**
	 * 格罗格特保险箱门已开，可进入。 / Grogget's Safe door is open and you can now access Grogget's Safe
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_OPEN_DOOR_01 = new SM_SYSTEM_MESSAGE(1400248);

	/**
	 * 禁闭室门已开，可进入。 / The Brig door is open and you can now access The Brig
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_OPEN_DOOR_02 = new SM_SYSTEM_MESSAGE(1400249);

	/**
	 * 发生器室门已开，可进入德拉纳发生器室。 / The Generator Chamber access door is open and you can now access the Drana Generator Chamber
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_OPEN_DOOR_03 = new SM_SYSTEM_MESSAGE(1400250);

	/**
	 * 大炮甲板门已开，可进入。 / The Large Gun Deck door is open and you can now access the Large Gun Deck
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_OPEN_DOOR_04 = new SM_SYSTEM_MESSAGE(1400251);

	/**
	 * 进入战舰的渗透路线已开放。 / The infiltration route into Dredgion is open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400252);

	 /**
	  * Abyss Gate will operate for 5 minutes only。
	  * The Abyss Gate will operate for 5 minutes only
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_PORTAL_TIME = new SM_SYSTEM_MESSAGE(1400253);

	/**
	 * @param value0 你：may only battle %0 within the given time limit。 / You may only battle %0 within the given time limit
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_BATTLE_TIME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400254, value0);
	}

	 /**
	  * %0 has left the battle。
	  * %0 has left the battle
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_BATTLE_END(String value0) {
		return new SM_SYSTEM_MESSAGE(1400255, value0);
	}

	/**
	 * You 无法 gainanymoreAbyssPointsbecauseyoureachedmaximumAbyssPointsyou 可获取用于 your 当前 level。 / You cannot gain any more Abyss Points because you reached the maximum Abyss Points you can get for your current level
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_GET_AP_LEVELBASE_LIMIT = new SM_SYSTEM_MESSAGE(1400256);

	/**
	 * 你：may only battle Tahabata Pyrelord within the given time limit。 / You may only battle Tahabata Pyrelord within the given time limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_S_RANK_BATTLE_TIME = new SM_SYSTEM_MESSAGE(1400257);

	/**
	 * Tahabata Pyrelord has left the battle
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_S_RANK_BATTLE_END = new SM_SYSTEM_MESSAGE(1400258);

	/**
	 * 你：may only battle Lord of Flame Calindi within the given time limit。 / You may only battle Lord of Flame Calindi within the given time limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_A_RANK_BATTLE_TIME = new SM_SYSTEM_MESSAGE(1400259);

	/**
	 * Lord of Flame Calindi has left the battle
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_A_RANK_BATTLE_END = new SM_SYSTEM_MESSAGE(1400260);

	/**
	 * @param durationtime0 连接将于 %DURATIONTIME0 后超时，请休息。 / Connection will time out in %DURATIONTIME0. Please take a break
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_USER_KICKED_BY_TIMEOUT(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400261, durationtime0);
	}

	/**
	 * 钢须海盗开始藏匿钥匙箱。 / The Steel Beard Pirates have begun hiding the Key Boxes
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_TIMER_START = new SM_SYSTEM_MESSAGE(1400262);

	/**
	 * All the Key Boxes have disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDSHULACKSHIP_TIMER_END = new SM_SYSTEM_MESSAGE(1400263);

	/**
	 * 敌方已退出战舰渗透任务。任务将在 %DURATIONTIME0% 后结束并离开。 / The opposition has withdrawn from the Dredgion infiltration mission. The mission will stop in %DURATIONTIME0% and you will leave the Dredgion
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ALARM_COLD_GAME_IDAB1_DREADGION(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400264, durationtime0);
	}

	 /**
	  * Nuclear Control Room Teleporter has been created at the Emergency Exit。
	  * A Nuclear Control Room Teleporter has been created at the Emergency Exit
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NUCLEARTELEPORTER_CREATED_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(
			1400265);

	/**
	 * Characters under level %0 cannot send Alliance invitations
	 */
	public static SM_SYSTEM_MESSAGE STR_PARTY_ALLIANCE_TOO_LOW_LEVEL_TO_INVITE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400266, value0);
	}

	/**
	 * Only those at or under level %0 can use %1
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_TOO_HIGH_LEVEL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400267, value0, value1);
	}

	/**
	 * @param durationtime0 You 曾 poisonedduringextraction 并无法 extract 用于 %DURATIONTIME0。 / You were poisoned during extraction and cannot extract for %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_RESTRICTED(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400268, durationtime0);
	}

	/**
	 * 你：have recovered from poisoning and can extract again。 / You have recovered from poisoning and can extract again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_RECOVERED = new SM_SYSTEM_MESSAGE(1400269);

	/**
	 * 你：chanted a spell to cleanse the poison from your body. You can now extract again。 / You chanted a spell to cleanse the poison from your body. You can now extract again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_UNRESTRICT = new SM_SYSTEM_MESSAGE(1400270);

	/**
	 * @param value0 咒语错误，净化毒素失败。剩余尝试：%0。 / Your incantation was incorrect; you failed to purify the poison. You have %0 attempts left
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_UNRESTRICT_FAILED_RETRY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400271, value0);
	}

	/**
	 * 咒语错误，净化毒素失败。 / Your incantation was incorrect; you failed to purify the poison
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_UNRESTRICT_FAILED = new SM_SYSTEM_MESSAGE(1400272);

	/**
	 * @param durationtime0 你当前中毒，无法采集（剩余时间：%DURATIONTIME0）。 / You are currently poisoned and unable to extract. (Time remaining: %DURATIONTIME0)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_REMAIN_RESTRICT_TIME(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400273, durationtime0);
	}

	/**
	 * 你可以采集。 / You are able to extract
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAPTCHA_NOT_RESTRICTED = new SM_SYSTEM_MESSAGE(1400274);

	 /**
	  * dimensional corridor that leads to the Indratu Fortress has appeared。
	  * A dimensional corridor that leads to the Indratu Fortress has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_PORTAL_OPEN_IDLF3_Castle_Indratoo = new SM_SYSTEM_MESSAGE(
			1400275);

	 /**
	  * dimensional corridor that leads to the Draupnir Cave has appeared。
	  * A dimensional corridor that leads to the Draupnir Cave has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_PORTAL_OPEN_IDDF3_Dragon = new SM_SYSTEM_MESSAGE(1400276);

	/**
	 * @param num0 你：gained %num0 points。 / You gained %num0 points
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_SCORE_FOR_ENEMY(int num0) {
		return new SM_SYSTEM_MESSAGE(1400277, num0);
	}

	/**
	 * 你：cannot fly while your pet is banned from flying。 / You cannot fly while your pet is banned from flying
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_FLY_NOW_DUE_TO_NOFLY_FROM_PET = new SM_SYSTEM_MESSAGE(1400278);

	/**
	 * You 无法 extract 从 equipped 物品。 / You cannot extract from equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_DECOMPOSE_EQUIP_ITEM_CAN_NOT_BE_DECOMPOSED = new SM_SYSTEM_MESSAGE(
			1400279);

	/**
	 * @param value0 剩余游戏时间 %*0。 / The remaining playing time is %*0
	 */
	public static SM_SYSTEM_MESSAGE STR_REMAIN_PLAYTIME_CENTER_DISPLAY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400280, value0);
	}

	/**
	 * 你：do not have enough Jewels of Eternity to buy the item。 / You do not have enough Jewels of Eternity to buy the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUY_SELL_NOT_ENOUGH_AIONJEWELS_TO_BUY_ITEM = new SM_SYSTEM_MESSAGE(
			1400281);

	/**
	 * 因 %0 与 %1 的性别或种族要求不同，无法完成外观修改。 / This modification cannot be completed as the gender or race requirements for %0 and %1 are different
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_CHANGE_OPPOSITE_ITEM_SKIN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400282, value0, value1);
	}

	/**
	 * @param value0 %0 不能用于外观修改。 / %0 cannot be used for modification
	 */
	public static SM_SYSTEM_MESSAGE STR_CHANGE_ITEM_SKIN_NOT_SKIN_EXTRACTABLE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400283, value0);
	}

	/**
	 * 你：must pass the Expert test in order to be promoted。 / You must pass the Expert test in order to be promoted
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHER_CANT_EXTEND_MASTER = new SM_SYSTEM_MESSAGE(1400284);

	/**
	 * 你：must pass the Artisan test in order to be promoted。 / You must pass the Artisan test in order to be promoted
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_CANT_EXTEND_HIGH_MASTER = new SM_SYSTEM_MESSAGE(1400285);

	/**
	 * 你：must pass the Master test in order to be promoted。 / You must pass the Master test in order to be promoted
	 */
	public static final SM_SYSTEM_MESSAGE STR_CRAFT_CANT_EXTEND_GRAND_MASTER = new SM_SYSTEM_MESSAGE(1400286);

	/**
	 * Crafting %0 has used up the recipe
	 */
	public static SM_SYSTEM_MESSAGE STR_COMBINE_USAGE_OVER(String value0) {
		return new SM_SYSTEM_MESSAGE(1400287, value0);
	}

	/**
	 * 用于合成的物品等级须高于被提取物品。 / The level of the item to be combined must be higher than that of the one to be extracted
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_MAIN_REQUIRE_HIGHER_LEVEL = new SM_SYSTEM_MESSAGE(1400288);

	/**
	 * @param i %0 无法合成。 / %0 cannot be combined
	 */
	public static SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_NOT_AVAILABLE(int i) {
		return new SM_SYSTEM_MESSAGE(1400289, new DescriptionId(i));
	}

	/**
	 * 因 %0 与 %1 的装备要求不同，无法完成外观修改。 / This modification cannot be completed as the equipment requirements for %0 and %1 are different
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_CHANGE_SKIN_OPPOSITE_REQUIREMENT(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400290, value0, value1);
	}

	/**
	 * %0 的外观维持时间已到期，外观修改效果已移除。 / The appearance maintain time for %0 has expired and the appearance modification effect has been removed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SKIN_CHANGE_TIME_EXPIRED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400291, value0);
	}

	/**
	 * 因格森与吉尔克马罗斯的所有要塞已变为可占领状态。 / All fortresses in Inggison and Gelkmaros have changed to the Capturable State
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_PVP_ON = new SM_SYSTEM_MESSAGE(1400292);

	/**
	 * @param value0 %0 不再易受攻击。 / %0 is no longer vulnerable
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_PVP_OFF(String value0) {
		return new SM_SYSTEM_MESSAGE(1400293, value0);
	}

	 /**
	  * Guardian General is under attack。
	  * The Guardian General is under attack
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_BOSS_ATTACKED = new SM_SYSTEM_MESSAGE(1400294);

	/**
	 * %1 of the %0 killed the Guardian General
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_BOSS_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400295, value1, value0);
	}

	 /**
	  * Balaur have killed the Guardian General。
	  * The Balaur have killed the Guardian General
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DRAGON_BOSS_KILLED = new SM_SYSTEM_MESSAGE(1400296);

	 /**
	  * %0 has conquered %1。
	  * %0 has conquered %1
	  */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_GUILD_WIN_CASTLE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400297, value0, value1);
	}

	/**
	 * %0 成功攻占了 %1。 / %0 succeeded in conquering %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_WIN_CASTLE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400298, value0, value1);
	}

	/**
	 * %0 军团 lost%1。 / %0 Legion lost %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_GUILD_CASTLE_TAKEN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400299, value0, value1);
	}

	/**
	 * %0 未能守住 %1。 / %0 failed to defend %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_CASTLE_TAKEN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400300, value0, value1);
	}

	/**
	 * %0 的 %1 获得了神器 %2。 / %1 of %0 obtained the Artifact %2
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_WIN_FIELDARTIFACT(String value1, String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1400301, value1, value0, value2);
	}

	/**
	 * %1 失去了神器 %0。 / %1 lost the Artifact %0
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_LOSE_FIELDARTIFACT(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400302, value1, value0);
	}

	/**
	 * 城门正遭受攻击。 / The Castle Gate is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DOOR_ATTACKED = new SM_SYSTEM_MESSAGE(1400303);

	/**
	 * 城门危急。 / The Castle Gate is in danger
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DOOR_ATSTAKE = new SM_SYSTEM_MESSAGE(1400304);

	/**
	 * %0 的 %1 摧毁了城门。 / %1 of the %0 destroyed the Castle Gate
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_DOOR_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400305, value1, value0);
	}

	/**
	 * 龙族摧毁了城门。 / The Balaur have destroyed the Castle Gate
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DRAGON_DOOR_BROKEN = new SM_SYSTEM_MESSAGE(1400306);

	/**
	 * 城门守护石正遭受攻击。 / The Gate Guardian Stone is under attack
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_REPAIR_ATTACKED = new SM_SYSTEM_MESSAGE(1400307);

	/**
	 * %0 的 %1 摧毁了城门守护石。 / %1 of the %0 destroyed the Gate Guardian Stone
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_REPAIR_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400308, value1, value0);
	}

	/**
	 * 龙族摧毁了城门守护石。 / The Balaur have destroyed the Gate Guardian Stone
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DRAGON_REPAIR_BROKEN = new SM_SYSTEM_MESSAGE(1400309);

	 /**
	  * Balaur Dredgion has appeared。
	  * The Balaur Dredgion has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_CARRIER_SPAWN = new SM_SYSTEM_MESSAGE(1400310);

	 /**
	  * Dredgion has dropped Balaur Troopers。
	  * The Dredgion has dropped Balaur Troopers
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_CARRIER_DROP_DRAGON = new SM_SYSTEM_MESSAGE(1400311);

	 /**
	  * Balaur Dredgion has disappeared。
	  * The Balaur Dredgion has disappeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_CARRIER_DESPAWN = new SM_SYSTEM_MESSAGE(1400312);

	/**
	 * %0 的 %1 正在激活神器 %2。 / %1 of %0 is activating the Artifact %2
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDARTIFACT_CASTING(String value1, String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1400313, value1, value0, value2);
	}

	 /**
	  * Artifact %1 core of %0 has been ejected。
	  * The Artifact %1 core of %0 has been ejected
	  */
	public static SM_SYSTEM_MESSAGE STR_FIELDARTIFACT_CORE_CASTING(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400314, value1, value0);
	}

	 /**
	  * activation of the Artifact %1 of %0 was canceled。
	  * The activation of the Artifact %1 of %0 was canceled
	  */
	public static SM_SYSTEM_MESSAGE STR_FIELDARTIFACT_CANCELED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400315, value1, value0);
	}

	/**
	 * %1 of %0 has activated the Artifact %2
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDARTIFACT_FIRE(String value1, String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1400316, value1, value0, value2);
	}

	/**
	 * Kaisinel's Agent Veille has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTBOSS_SPAWN = new SM_SYSTEM_MESSAGE(1400317);

	/**
	 * Marchutan's Agent Mastarius has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKBOSS_SPAWN = new SM_SYSTEM_MESSAGE(1400318);

	/**
	 * Kaisinel's Agent Veille has disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTBOSS_DESPAWN = new SM_SYSTEM_MESSAGE(1400319);

	/**
	 * Marchutan's Agent Mastarius has disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKBOSS_DESPAWN = new SM_SYSTEM_MESSAGE(1400320);

	/**
	 * 凯希内尔代理人维勒遭受攻击！ / Kaisinel's Agent Veille is under attack!
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTBOSS_ATTACKED = new SM_SYSTEM_MESSAGE(1400321);

	/**
	 * 玛尔库坦代理人玛斯塔里乌斯遭受攻击！ / Marchutan's Agent Mastarius is under attack!
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKBOSS_ATTACKED = new SM_SYSTEM_MESSAGE(1400322);

	/**
	 * %1 of %0 has killed Marchutan's Agent Mastarius
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKBOSS_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400323, value1, value0);
	}

	/**
	 * %1 of %0 has killed Kaisinel's Agent Veille
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTBOSS_KILLED(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400324, value1, value0);
	}

	/**
	 * @param value0 %0 正遭受攻击。 / %0 is under attack
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_BARRIER_ATTACKED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400325, value0);
	}

	/**
	 * @param value0value %0% 处于危险！ / %0% is in danger!
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_BARRIER_ATSTAKE(String value0value) {
		return new SM_SYSTEM_MESSAGE(1400326, value0value);
	}

	/**
	 * %1 of %0 has destroyed %SUBZONE2
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_BARRIER_BROKEN(String value1, String value0, String subzone2) {
		return new SM_SYSTEM_MESSAGE(1400327, value1, value0, subzone2);
	}

	/**
	 * 西伦特拉西门（因格森通往西伦特拉峡谷）已开启。 / Silentera Westgate, the entrance from Inggison to Silentera Canyon, has opened
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTUNDERPASS_SPAWN = new SM_SYSTEM_MESSAGE(1400328);

	/**
	 * 西伦特拉东门（吉尔克马罗斯通往西伦特拉峡谷）已开启。 / Silentera Eastgate, the entrance from Gelkmaros to Silentera Canyon, has opened
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKUNDERPASS_SPAWN = new SM_SYSTEM_MESSAGE(1400329);

	/**
	 * 西伦特拉西门（因格森通往西伦特拉峡谷）已关闭。 / Silentera Westgate, the entrance from Inggison to Silentera Canyon, has closed
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_LIGHTUNDERPASS_DESPAWN = new SM_SYSTEM_MESSAGE(1400330);

	/**
	 * 西伦特拉东门（吉尔克马罗斯通往西伦特拉峡谷）已关闭。 / Silentera Eastgate, the entrance from Gelkmaros to Silentera Canyon, has closed
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DARKUNDERPASS_DESPAWN = new SM_SYSTEM_MESSAGE(1400331);

	/**
	 * You 无法 extractbecauseyoudonot 有物品所需用于 Essencetapping。 / You cannot extract because you do not have the item required for Essencetapping
	 */
	public static final SM_SYSTEM_MESSAGE STR_GATHERING_REQUIRE_ITEM = new SM_SYSTEM_MESSAGE(1400332);

	/**
	 * @param value0 你：used %0。 / You used %0
	 */
	public static SM_SYSTEM_MESSAGE STR_USE_CASH_TYPE_ITEM1(String value0) {
		return new SM_SYSTEM_MESSAGE(1400333, value0);
	}

	/**
	 * 你：used %1 %0s。 / You used %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_USE_CASH_TYPE_ITEM2(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400334, value1, value0s);
	}

	 /**
	  * ability combined with %0 has been removed。
	  * The ability combined with %0 has been removed
	  */
	public static SM_SYSTEM_MESSAGE STR_COMPOUNDED_ITEM_DECOMPOUND_SUCCESS(int i) {
		return new SM_SYSTEM_MESSAGE(1400335, new DescriptionId(i));
	}

	/**
	 * %1 has been combined with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_COMPOUND_SUCCESS(int i, int j) {
		return new SM_SYSTEM_MESSAGE(1400336, new DescriptionId(i), new DescriptionId(j));
	}

	/**
	 * Youdonot 有足够基纳到 combine%0 并 %1。 / You do not have enough Kinah to combine %0 and %1
	 */
	public static SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_NOT_ENOUGH_MONEY(int i, int j) {
		return new SM_SYSTEM_MESSAGE(1400337, new DescriptionId(i), new DescriptionId(j));
	}

	 /**
	  * target is immune to %0。
	  * The target is immune to %0
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_WRONG_TARGET_CLASS(String value0) {
		return new SM_SYSTEM_MESSAGE(1400338, value0);
	}

	 /**
	  * target is immune to %0。
	  * The target is immune to %0
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_WRONG_TARGET_RACE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400339, value0);
	}

	/**
	 * Characters under level %0 cannot send letters
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_MAIL_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400340, value0);
	}

	/**
	 * Characters under level %0 cannot use the search function
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_WHO_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400341, value0);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Energy of Repose %num2)。 / You have gained %num1 XP from %0 (Energy of Repose %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_BONUS(String value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1400342, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_BONUS_DESC(DescriptionId value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1400342, value0, num1, num2);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Energy of Salvation %num2)。 / You have gained %num1 XP from %0 (Energy of Salvation %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_MAKEUP_BONUS(String value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1400343, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_MAKEUP_BONUS_DESC(DescriptionId value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1400343, value0, num1, num2);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Energy of Repose %num2, Energy of Salvation %num3)。 / You have gained %num1 XP from %0 (Energy of Repose %num2, Energy of Salvation %num3)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_MAKEUP_BONUS(String value0, long num1, long num2, long num3) {
		return new SM_SYSTEM_MESSAGE(1400344, value0, num1, num2, num3);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_MAKEUP_BONUS_DESC(DescriptionId value0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1400344, value0, num1, num2, num3);
	}

	/**
	 * 你：have gained %0 (Energy of Repose %num1)。 / You have gained %0 (Energy of Repose %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN_VITAL_BONUS(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1400345, value0, num1);
	}

	/**
	 * 你：have gained %0 (Energy of Salvation %num1)。 / You have gained %0 (Energy of Salvation %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN_MAKEUP_BONUS(String value0, int num1) {
		return new SM_SYSTEM_MESSAGE(1400346, value0, num1);
	}

	/**
	 * 你：have gained %0 (Energy of Repose %num1, Energy of Salvation %num2)。 / You have gained %0 (Energy of Repose %num1, Energy of Salvation %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN_VITAL_MAKEUP_BONUS(String value0, int num1, int num2) {
		return new SM_SYSTEM_MESSAGE(1400347, value0, num1, num2);
	}

	/**
	 * 你：have gained %num0 XP (Energy of Repose %num1)。 / You have gained %num0 XP (Energy of Repose %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_VITAL_BONUS(long num0, long num1) {
		return new SM_SYSTEM_MESSAGE(1400348, num0, num1);
	}

	/**
	 * 你：have gained %num0 XP (Energy of Salvation %num1)。 / You have gained %num0 XP (Energy of Salvation %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_MAKEUP_BONUS(long num0, long num1) {
		return new SM_SYSTEM_MESSAGE(1400349, num0, num1);
	}

	/**
	 * 你：have gained %num0 XP (Energy of Repose %num1, Energy of Salvation %num2)。 / You have gained %num0 XP (Energy of Repose %num1, Energy of Salvation %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_VITAL_MAKEUP_BONUS(long num0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1400350, num0, num1, num2);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Energy of Repose %num2 and Golden Vinna Scent %num3)。 / You have gained %num1 XP from %0 (Energy of Repose %num2 and Golden Vinna Scent %num3)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_ABSOLUTEEXP_BONUS_DESC(DescriptionId value0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402617, value0, num1, num2, num3);
	}

	/**
	 * You 有 gained%num1XP 从 %0 ( Energy 的 Repose%num2 , Energy 的 Relief%num3 ,并 GoldenVinnaScent%num4 )。 / You have gained %num1 XP from %0 (Energy of Repose %num2, Energy of Relief %num3, and Golden Vinna Scent %num4)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_MAKEUP_ABSOLUTEEXP_BONUS(DescriptionId value0, long num1,
			long num2, long num3, long num4) {
		return new SM_SYSTEM_MESSAGE(1402619, value0, num1, num2, num3, num4);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Golden Vinna Scent %num2)。 / You have gained %num1 XP from %0 (Golden Vinna Scent %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_ABSOLUTEEXP_BONUS_DESC(DescriptionId value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402616, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_VITAL_ABSOLUTEEXP_BONUS(String value0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402617, value0, num1, num2, num3);
	}

	/**
	 * 你：have gained %num1 XP from %0 (Golden Vinna Scent %num2)。 / You have gained %num1 XP from %0 (Golden Vinna Scent %num2)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_ABSOLUTEEXP_BONUS(String value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402616, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_ABSOLUTEEXP_BONUS(String value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402630, value0, num1, num2);
	}

	/**
	 * 你：have gained %num0 XP (Golden Vinna Scent %num1)。 / You have gained %num0 XP (Golden Vinna Scent %num1)
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_ABSOLUTEEXP_BONUS(long num0, long num1) {
		return new SM_SYSTEM_MESSAGE(1402630, num0, num1);
	}

	/**
	 * 你：have selected more items than there are remaining。 / You have selected more items than there are remaining
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIMITED_SALE_CANT_SELECT_OVER_ITEMS = new SM_SYSTEM_MESSAGE(1400351);

	/**
	 * 此物品为 nolonger 可用。 / This item is no longer available
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIMITED_SALE_CANT_SELECT_NO_ITEMS = new SM_SYSTEM_MESSAGE(1400352);

	/**
	 * You 无法 purchase 物品 becauseyou 有 exceededpurchaselimit。 / You cannot purchase the item because you have exceeded the purchase limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS = new SM_SYSTEM_MESSAGE(1400353);

	/**
	 * 你：have selected more than the purchase limit of the item。 / You have selected more than the purchase limit of the item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIMITED_BUYING_CANT_SELECT_OVER_ITEMS = new SM_SYSTEM_MESSAGE(
			1400354);

	/**
	 * Youcannotstorethis 物品 in 军团仓库 / You cannot store this item in the Legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WAREHOUSE_CANT_LEGION_DEPOSIT = new SM_SYSTEM_MESSAGE(1400355);

	/**
	 * Youcannotstorethis 物品 in 账号仓库 / You cannot store this item in the account warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT = new SM_SYSTEM_MESSAGE(1400356);

	/**
	 * %WORLDNAME1 (difficulty: %2) with a %num0 player limit has opened
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_WITH_DIFFICULTY_OPENED(String worldname1, String value2,
			int num0) {
		return new SM_SYSTEM_MESSAGE(1400357, worldname1, value2, num0);
	}

	/**
	 * %WORLDNAME1 with a %num0 player limit has opened
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_OPENED(String worldname1, int num0) {
		return new SM_SYSTEM_MESSAGE(1400358, worldname1, num0);
	}

	/**
	 * %WORLDNAME1 (difficulty: %2) with a %num0 player limit is currently open
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_WITH_DIFFICULTY_OPENED_INFO(String worldname1,
			String value2, int num0) {
		return new SM_SYSTEM_MESSAGE(1400359, worldname1, value2, num0);
	}

	/**
	 * %WORLDNAME1 with a %num0 player limit is currently open
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_OPENED_INFO(String worldname1, int num0) {
		return new SM_SYSTEM_MESSAGE(1400360, worldname1, num0);
	}

	/**
	 * 你：can only enter after the Group Leader has created the instance。 / You can only enter after the Group Leader has created the instance
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED = new SM_SYSTEM_MESSAGE(
			1400361);

	/**
	 * 你：can only use this item in a cube。 / You can only use this item in a cube
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_REQUIRE_IN_INVENTORY = new SM_SYSTEM_MESSAGE(1400362);

	 /**
	  * 你的背包已满。
	  * 你的背包已满。 / Your cube is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DECOMPRESS_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1400363);

	/**
	 * 你：cannot combine different weapon types。 / You cannot combine different weapon types
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_DIFFERENT_TYPE = new SM_SYSTEM_MESSAGE(1400364);

	/**
	 * 此物品无法为 registered 用于 comparison。 / This item cannot be registered for comparison
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_NOT_COMPARABLE_ITEM = new SM_SYSTEM_MESSAGE(1400365);

	 /**
	  * Seal of Uniformity has been weakened。
	  * The Seal of Uniformity has been weakened
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTP_FANATIC_Die_Keynamed = new SM_SYSTEM_MESSAGE(1400366);

	/**
	 * 你：can now enter the Chamber of Unity。 / You can now enter the Chamber of Unity
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTP_FANATIC_DieAll_Keynamed = new SM_SYSTEM_MESSAGE(1400367);

	 /**
	  * %0 has blocked all Whispers from characters under level %1。
	  * %0 has blocked all Whispers from characters under level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECT_WHISPER_FROM_LOW_LEVEL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400368, value0, value1);
	}

	 /**
	  * %0 has blocked all mail from characters under level %1。
	  * %0 has blocked all mail from characters under level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_REJECT_MAIL_FROM_LOW_LEVEL(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400369, value0, value1);
	}

	/**
	 * 仓库中 %0 的外观维持时间已到期，外观修改效果已移除。 / The appearance maintain time for %0 in the warehouse has expired and the appearance modification effect has been removed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SKIN_CHANGE_TIME_EXPIRED_IN_WAREHOUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400370, value0);
	}

	/**
	 * 此 modification 无法为 completed 作为 %0 并 %1 有相同 appearance。 / This modification cannot be completed as %0 and %1 have the same appearance
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_CHANGE_SAME_ITEM_SKIN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400371, value0, value1);
	}

	 /**
	  * appearance modification effect of %0 has been removed。
	  * The appearance modification effect of %0 has been removed
	  */
	public static SM_SYSTEM_MESSAGE STR_UNCHANGE_ITEM_SKIN_SUCCEED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400372, value0);
	}

	 /**
	  * %0 is not a combined item。
	  * %0 is not a combined item
	  */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOUND_ERROR_NOT_AVAILABLE(int i) {
		return new SM_SYSTEM_MESSAGE(1400373, new DescriptionId(i));
	}

	/**
	 * @param value0 %0 正在出售稀有物品。 / %0 is now selling rare items
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_TEST_DESC01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400374, value0);
	}

	/**
	 * You 有 senttoomanymails 在 once 并有已 termporarilyblocked.Pleasetryagainlater。 / You have sent too many mails at once and have been termporarily blocked. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_SEND_OVER_MAILS = new SM_SYSTEM_MESSAGE(1400375);

	/**
	 * @param value0 你：do not have enough %0 to gather。 / You do not have enough %0 to gather
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_GATHERING_B_ITEM_CHECK(String value0) {
		return new SM_SYSTEM_MESSAGE(1400376, value0);
	}

	/**
	 * Characters under level %0 cannot shout
	 */
	public static SM_SYSTEM_MESSAGE STR_CANT_SHOUT_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400377, value0);
	}

	/**
	 * 已开启要塞战优化以保障流畅，附近角色以简化形态显示。 / Optimize Fortress Battle function has been toggled on for smooth game play. Characters in the vicinity are displayed in simplified forms
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHAR_HIDE_AUTO_ON = new SM_SYSTEM_MESSAGE(1400378);

	/**
	 * 你正被强风吹走。 / You are being blown away by the wind!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WindPathIN = new SM_SYSTEM_MESSAGE(1400379);

	/**
	 * Manadar's hidden trap has been tripped!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_BombDrakan_TargetMSG = new SM_SYSTEM_MESSAGE(
			1400380);

	/**
	 * 被征服的灵魂已释放！ / The Subjugated Souls have been released!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_Spectre_Buff = new SM_SYSTEM_MESSAGE(1400381);

	/**
	 * 拉哈拉船长正在准备最终一击！ / Captain Lakhara is preparing his final strike!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_TombDrakan = new SM_SYSTEM_MESSAGE(1400382);

	/**
	 * 坚定者伊斯巴里亚正释放真正力量！ / Isbariya the Resolute is tapping into his true power!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest2_01 = new SM_SYSTEM_MESSAGE(1400383);

	/**
	 * 未在时限内消灭怪物，宝箱消失。 / The treasure chest vanished because you did not destroy the monsters within the time limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_Oops_Reward_Is_Gone = new SM_SYSTEM_MESSAGE(1400384);

	/**
	 * 风力过强——无法挣脱！ / The wind is too strong--you can't break away!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WindPathNoOUT = new SM_SYSTEM_MESSAGE(1400385);

	/**
	 * 唤魂者双眼闪烁！ / Soulcaller's eyes glimmer!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_Summoner_Reflect = new SM_SYSTEM_MESSAGE(1400386);

	/**
	 * Soulcaller casts the Powerful Smite skill!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_Summoner_DeadlyCasting = new SM_SYSTEM_MESSAGE(
			1400387);

	/**
	 * Flarestorm is unleashing an unknown power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ElementalFire_Buff = new SM_SYSTEM_MESSAGE(1400388);

	/**
	 * @param value0 %0 正在出售稀有物品。 / %0 is now selling rare items
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_TEST_DESC02(String value0) {
		return new SM_SYSTEM_MESSAGE(1400389, value0);
	}

	/**
	 * 本村有人在出售独特物品。 / Someone in this village is selling unique items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_TEST_DESC03 = new SM_SYSTEM_MESSAGE(1400390);

	/**
	 * @param value0 此消息为用于 testing%0 ' slimitedsale。 / This message is for testing %0's limited sale
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_TEST_DESC04(String value0) {
		return new SM_SYSTEM_MESSAGE(1400391, value0);
	}

	/**
	 * Yourrequest 有已 registered 在 RecruitGroup 成员列表。 / Your request has been registered on the Recruit Group Member List
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_OFFER_PARTY_POSTED = new SM_SYSTEM_MESSAGE(1400392);

	/**
	 * Yourrequest 有已 registered 在 Apply 用于 Group 列表。 / Your request has been registered on the Apply For Group List
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_SEEK_PARTY_POSTED = new SM_SYSTEM_MESSAGE(1400393);

	 /**
	  * 你的Find Group request was removed because it has not been updated。
	  * Your Find Group request was removed because it has not been updated
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_POST_DELETED_TOO_OLD = new SM_SYSTEM_MESSAGE(1400394);

	/**
	 * 因你已加入小队或联盟，寻找小队请求已移除。 / Your Find Group request was removed because you have joined a Group or Alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_POST_DELETED_ENTERED_PARTY = new SM_SYSTEM_MESSAGE(1400395);

	 /**
	  * 你的Find Group request was removed because your Group or Alliance is full。
	  * Your Find Group request was removed because your Group or Alliance is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_POST_DELETED_PARTY_FULL = new SM_SYSTEM_MESSAGE(1400396);

	 /**
	  * 你的Find Group request was removed because the Group or Alliance disbanded。
	  * Your Find Group request was removed because the Group or Alliance disbanded
	  */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_POST_DELETED_PARTY_BROKE = new SM_SYSTEM_MESSAGE(1400397);

	/**
	 * Characters under level %0 who are using a free trial cannot use the Broker
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_VENDOR(String value0) {
		return new SM_SYSTEM_MESSAGE(1400398, value0);
	}

	/**
	 * @param value0 试用账号且等级低于 %0 不能开设个人商店。 / Characters under level %0 who are using a free trial cannot open a private store
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_OPEN_PERSONAL_SHOP(String value0) {
		return new SM_SYSTEM_MESSAGE(1400399, value0);
	}

	/**
	 * Characters under level %0 who are playing a free trial cannot trade
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_TRADE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400400, value0);
	}

	/**
	 * @param value0 试用账号且等级低于 %0 不能发送含物品或金钱的邮件。 / Characters under level %0 who are using a free trial cannot send mail containing items or money
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_SEND_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400401, value0);
	}

	/**
	 * @param value0 试用账号且等级低于 %0 不能使用个人仓库。 / Characters under level %0 who are using a free trial cannot use the private warehouse
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_WAREHOUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400402, value0);
	}

	/**
	 * @param value0 试用账号且等级低于 %0 不能使用军团仓库。 / Characters under level %0 who are using a free trial cannot use the Legion warehouse
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_GUILD_WAREHOUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400403, value0);
	}

	/**
	 * @param value0 试用账号且等级低于 %0 不能使用账号仓库。 / Characters under level %0 who are using a free trial cannot use the Account warehouse
	 */
	public static SM_SYSTEM_MESSAGE STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_ACCOUNT_WAREHOUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400404, value0);
	}

	/**
	 * Captain Adhati has appeared in the Captain's Cabin
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDAB1_DREADGION = new SM_SYSTEM_MESSAGE(1400405);

	/**
	 * Usage time for %0 in the warehouse has expired
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_ITEM_BY_TIMEOUT_IN_WAREHOUSE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400406, value0);
	}

	/**
	 * 已找到符合搜索条件的匹配。 / Matches meeting your search conditions have been found
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_SEARCH_FOUND = new SM_SYSTEM_MESSAGE(1400407);

	/**
	 * 已有 10 人汇聚力量。 / 10 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_10 = new SM_SYSTEM_MESSAGE(1400408);

	/**
	 * 已有 20 人汇聚力量。 / 20 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_20 = new SM_SYSTEM_MESSAGE(1400409);

	/**
	 * 已有 30 人汇聚力量。 / 30 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_30 = new SM_SYSTEM_MESSAGE(1400410);

	/**
	 * 已有 40 人汇聚力量。 / 40 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_40 = new SM_SYSTEM_MESSAGE(1400411);

	/**
	 * 已有 50 人汇聚力量。 / 50 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_50 = new SM_SYSTEM_MESSAGE(1400412);

	/**
	 * 已有 60 人汇聚力量。 / 60 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_60 = new SM_SYSTEM_MESSAGE(1400413);

	/**
	 * 已有 70 人汇聚力量。 / 70 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_70 = new SM_SYSTEM_MESSAGE(1400414);

	/**
	 * 已有 80 人汇聚力量。 / 80 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_80 = new SM_SYSTEM_MESSAGE(1400415);

	/**
	 * 已有 90 人汇聚力量。 / 90 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_90 = new SM_SYSTEM_MESSAGE(1400416);

	/**
	 * 已有 91 人汇聚力量。 / 91 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_91 = new SM_SYSTEM_MESSAGE(1400417);

	/**
	 * 已有 92 人汇聚力量。 / 92 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_92 = new SM_SYSTEM_MESSAGE(1400418);

	/**
	 * 已有 93 人汇聚力量。 / 93 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_93 = new SM_SYSTEM_MESSAGE(1400419);

	/**
	 * 已有 94 人汇聚力量。 / 94 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_94 = new SM_SYSTEM_MESSAGE(1400420);

	/**
	 * 95 persons have gathered their power. The Empyrean Avatar has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_95 = new SM_SYSTEM_MESSAGE(1400421);

	/**
	 * 已有 96 人汇聚力量。 / 96 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_96 = new SM_SYSTEM_MESSAGE(1400422);

	/**
	 * 已有 97 人汇聚力量。 / 97 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_97 = new SM_SYSTEM_MESSAGE(1400423);

	/**
	 * 已有 98 人汇聚力量。 / 98 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_98 = new SM_SYSTEM_MESSAGE(1400424);

	/**
	 * 已有 99 人汇聚力量。 / 99 persons have gathered their power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_99 = new SM_SYSTEM_MESSAGE(1400425);

	/**
	 * 已有 100 人汇聚力量。现在可使用天族/魔族化身。 / 100 persons have gathered their power. You can now use the Empyrean Avatar
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_COUNT_100 = new SM_SYSTEM_MESSAGE(1400426);

	/**
	 * You 有 failed 到 useEmpyreanAvatar.Youwillneed 到 gatherpower 并 summon 其 again。 / You have failed to use the Empyrean Avatar. You will need to gather power and summon it again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_DEATHBLOW_FAIL = new SM_SYSTEM_MESSAGE(1400427);

	 /**
	  * first Sphere of Mirage has been activated。
	  * The first Sphere of Mirage has been activated
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_FIRST_OBJECT_ON = new SM_SYSTEM_MESSAGE(1400428);

	/**
	 * 第二幻影球已激活。凯希内尔代理人维勒准备施放主神祝福。 / The second Sphere of Mirage has been activated. Kaisinel's Agent Veille prepares to cast the Empyrean Lord's blessing
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_SECOND_OBJECT_ON = new SM_SYSTEM_MESSAGE(1400429);

	/**
	 * 你：may use the Sphere of Mirage again。 / You may use the Sphere of Mirage again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_CAN_USE_OBJECT = new SM_SYSTEM_MESSAGE(1400430);

	/**
	 * 你：need more people to activate the Sphere of Mirage。 / You need more people to activate the Sphere of Mirage
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_CANT_USE_OBJECT_NOT_ENOUGH_MEMBER = new SM_SYSTEM_MESSAGE(
			1400431);

	/**
	 * 你当前为忙碌状态。请在系统设置中重置以接受邀请。 / You are marked as Unavailable. Please reset the setting in System Preferences to accept the invitation
	 */
	public static final SM_SYSTEM_MESSAGE STR_INFORM_INVITE_REJECT_STATE = new SM_SYSTEM_MESSAGE(1400434);

	 /**
	  * %0 has succeeded in enchanting %1 to Level 15。
	  * %0 has succeeded in enchanting %1 to Level 15
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ENCHANT_ITEM_SUCCEEDED_15(String playerName, int nameId) {
		return new SM_SYSTEM_MESSAGE(1400435, playerName, new DescriptionId(nameId));
	}

	 /**
	  * %0 has succeeded in enchanting %1 to Level 20。
	  * %0 has succeeded in enchanting %1 to Level 20
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ENCHANT_ITEM_SUCCEEDED_20(String playerName, int nameId) {
		return new SM_SYSTEM_MESSAGE(1402285, playerName, new DescriptionId(nameId));
	}

	/**
	 * @param value0 %0 正在出售用于提取活力与奥德的物品。 / %0 is selling items to extract vitality and Aether
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_GATHERING_DESC01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400436, value0);
	}

	/**
	 * 你：cannot join this race。 / You cannot join this race
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_JOIN_ERROR_RACE = new SM_SYSTEM_MESSAGE(1400437);

	/**
	 * @param value0 You 可仅 join 当 yourlevel 为 %0 或 above。 / You can only join when your level is %0 or above
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_JOIN_ERROR_MIN_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400438, value0);
	}

	/**
	 * @param value0 %0 为 sellingmaterials 到创建物品的 CraftingMasters。 / %0 is selling materials to create the items of Crafting Masters
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_M_EPIC_SHOP_DESC01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400439, value0);
	}

	/**
	 * @param value0 %0 为 sellingmaterials 到创建物品的 CraftingMasters。 / %0 is selling materials to create the items of Crafting Masters
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_M_EPIC_SHOP_DESC02(String value0) {
		return new SM_SYSTEM_MESSAGE(1400440, value0);
	}

	/**
	 * @param value0 %0 正在出售大师制作专用材料。 / %0 is selling special materials for Master Crafting
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LIMIT_SALE_EPIC_SHOP_MATERIAL_DESC01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400441, value0);
	}

	/**
	 * Devoted Anurati has appeared in the Great Chapel
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTP_FANATIC_DrakanNamed_SpawnMSG = new SM_SYSTEM_MESSAGE(1400442);

	/**
	 * Malicious Obscura exhausts the HP of nearby enemies!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Normal_Stalker_DrainHealth = new SM_SYSTEM_MESSAGE(
			1400443);

	/**
	 * 误导的奥布斯库拉蹲伏！ / Misguiding Obscura crouches!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Normal_Stalker_Sanctuary = new SM_SYSTEM_MESSAGE(1400444);

	/**
	 * 墓地史莱姆一分为二！ / Grave Slime is splitting in two!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Normal_Slime_Isolation = new SM_SYSTEM_MESSAGE(1400445);

	/**
	 * 不死者瑟尔宗停止攻击并开始复活。 / Thurzon the Undying stops its assault and begins reviving
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_BoneDrake_Sanctuary = new SM_SYSTEM_MESSAGE(1400446);

	/**
	 * 因你已加入小队或联盟，申请组队列表请求已删除。 / Your Apply For Group List request was deleted because you have joined a Group or Alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_MATCH_SEEK_POST_DELETED_ENTERED_PARTY = new SM_SYSTEM_MESSAGE(
			1400447);

	/**
	 * 你：used %1 %0s。 / You used %1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_USE_ITEM_MULTI(String value1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400448, value1, value0s);
	}

	/**
	 * 你in normal state。 / You are in normal state
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_INFO_0_LEVEL = new SM_SYSTEM_MESSAGE(1400449);

	/**
	 * 因游戏时间过长，已施加 1 级疲劳惩罚。请下线休息。 / A Level 1 Fatigue Penalty has been applied because you have played too long. Please log out and take a break
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_INFO_1_LEVEL = new SM_SYSTEM_MESSAGE(1400450);

	/**
	 * 因游戏时间过长，已施加 2 级疲劳惩罚。请下线休息。 / A Level 2 Fatigue Penalty has been applied because you have played too long. Please log out and take a break
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_INFO_2_LEVEL = new SM_SYSTEM_MESSAGE(1400451);

	/**
	 * @param value0 你：have opened the %0。 / You have opened the %0
	 */
	public static SM_SYSTEM_MESSAGE STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400452, value0);
	}

	/**
	 * @param value0 你：have stopped opening the %0 bundle。 / You have stopped opening the %0 bundle
	 */
	public static SM_SYSTEM_MESSAGE STR_UNCOMPRESS_COMPRESSED_ITEM_CANCELED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400453, value0);
	}

	/**
	 * The Divine Artifact has been activated!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_Artifact_Light = new SM_SYSTEM_MESSAGE(
			1400454);

	/**
	 * The Magic Artifact has been activated!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_Artifact_Dark = new SM_SYSTEM_MESSAGE(
			1400455);

	/**
	 * 伊斯巴里亚引动力量引发大爆炸！ / Isbariya taps into his power to cause a massive explosion!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_Artifact_LightBoom = new SM_SYSTEM_MESSAGE(
			1400456);

	/**
	 * 伊斯巴里亚释放魔法力量！ / Isbariya releases his magical power!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_Artifact_DarkBoom = new SM_SYSTEM_MESSAGE(
			1400457);

	 /**
	  * %0 has given up following because the distance between you is too great。
	  * %0 has given up following because the distance between you is too great
	  */
	public static SM_SYSTEM_MESSAGE STR_MERCENARY_FOLLOWING_CANCELED_BY_TOO_DISTANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400458, value0);
	}

	/**
	 * Isbariya the Resolute has boosted his attack power!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_2phase = new SM_SYSTEM_MESSAGE(1400459);

	/**
	 * Isbariya the Resolute has boosted his recovery power!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_3phase = new SM_SYSTEM_MESSAGE(1400460);

	/**
	 * 坚定者伊斯巴里亚释放强烈力量。 / Isbariya the Resolute unleashes an intense power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_4phase = new SM_SYSTEM_MESSAGE(1400461);

	/**
	 * Isbariya the Resolute has summoned a Bodyguard Commissioned Officer
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_5phase = new SM_SYSTEM_MESSAGE(1400462);

	/**
	 * 坚定者伊斯巴里亚施加毁灭诅咒。 / Isbariya the Resolute inflicts a devastating curse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_Boss_ArchPriest_6phase = new SM_SYSTEM_MESSAGE(1400463);

	/**
	 * 因游戏时间过长，已施加 %0 级疲劳惩罚。可用 /Fatigue 查看。 / A Level %0 Fatigue Penalty has been applied because you have played too long. Monitor your fatigue level with the '/Fatigue' command
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_UPGRADE_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400464, value0);
	}

	 /**
	  * Warrior Monument has been destroyed. Ahbana the Wicked is on alert。
	  * The Warrior Monument has been destroyed. Ahbana the Wicked is on alert
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdSpecter_Spawn = new SM_SYSTEM_MESSAGE(1400465);

	/**
	 * 马昆贝洛的力量正在减弱。 / Macunbello's power is weakening
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdLich_weakness1 = new SM_SYSTEM_MESSAGE(1400466);

	/**
	 * Macunbello's power has weakened
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdLich_weakness2 = new SM_SYSTEM_MESSAGE(1400467);

	/**
	 * Macunbello has been crippled
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdLich_weakness3 = new SM_SYSTEM_MESSAGE(1400468);

	/**
	 * Macunbello has left his sanctuary
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdLich_Leave = new SM_SYSTEM_MESSAGE(1400469);

	/**
	 * Ahbana the Wicked has appeared in the Watcher's Nexus
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdSpecter_Start = new SM_SYSTEM_MESSAGE(1400470);

	/**
	 * Hiding Lupukin has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_NmdShulack_Rufukin = new SM_SYSTEM_MESSAGE(1400471);

	/**
	 * 奥德力场激活石遭受攻击！ / The Aetheric Field Activation Stone is under attack!
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_SHIELD_ATTACKED = new SM_SYSTEM_MESSAGE(1400472);

	/**
	 * %0 的 %1 摧毁了城门。 / %1 of %0 destroyed the castle gate
	 */
	public static SM_SYSTEM_MESSAGE STR_FIELDABYSS_SHIELD_BROKEN(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400473, value1, value0);
	}

	 /**
	  * Balaur have destroyed the Aetheric Field Activation Stone。
	  * The Balaur have destroyed the Aetheric Field Activation Stone
	  */
	public static final SM_SYSTEM_MESSAGE STR_FIELDABYSS_DRAGON_SHIELD_BROKEN = new SM_SYSTEM_MESSAGE(1400474);

	/**
	 * 茧在蠕动——里面有东西！ / The cocoons are wriggling--something's inside!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_COCOON_INFO = new SM_SYSTEM_MESSAGE(1400475);

	/**
	 * 莫斯夸女王的卵表面出现裂纹。 / Cracks appear on the surface of Queen Mosqua's egg
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_EGG_BREAK = new SM_SYSTEM_MESSAGE(1400476);

	/**
	 * 卵所在处升起上升气流，展开双翼可垂直飞升。 / An ascending air current is rising from the spot where the egg was. You can fly vertically up by spreading your wings and riding the current
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_WIND_INFO = new SM_SYSTEM_MESSAGE(1400477);

	/**
	 * 你当前无法获得物品。 / You are unable to obtain items at the current time
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RESTRICTED_STATE_CANT_GET_ITEM = new SM_SYSTEM_MESSAGE(1400478);

	/**
	 * 你当前无法获得物品，也无法参与 Roll 点。 / You are unable to obtain items at the current time, and cannot participate in the roll
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RESTRICTED_STATE_CANT_THROW_DICE = new SM_SYSTEM_MESSAGE(1400479);

	 /**
	  * Seal Protector has fallen. The Rift Orb shines while the seal weakens。
	  * The Seal Protector has fallen. The Rift Orb shines while the seal weakens
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCatacombs_BigOrb_Spawn = new SM_SYSTEM_MESSAGE(1400480);

	/**
	 * %0 使用时间还剩 %1。 / %1 remains before the usage time for %0 expires
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CASH_ITEM_TIME_LEFT(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400481, value1, value0);
	}

	/**
	 * %0 外观变更时间还剩 %1。 / %1 remains on the appearance change time of %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SKIN_CHANGE_TIME_LEFT(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400482, value1, value0);
	}

	/**
	 * 你：can only acquire daily quests once per day。 / You can only acquire daily quests once per day
	 */
	public static final SM_SYSTEM_MESSAGE STR_FACTION_CAN_NOT_RECEIVE_QUEST_TWICE_A_DAY = new SM_SYSTEM_MESSAGE(
			1400483);

	/**
	 * @param value0 距离过远：%0 to issue an order。 / You are too far from %0 to issue an order
	 */
	public static SM_SYSTEM_MESSAGE STR_MERCENARY_CANT_ORDER_BY_TOO_DISTANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400484, value0);
	}

	/**
	 * 间歇泉喷出水柱。 / Water erupts from the geyser
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_JUMP_TRIGGER_ON_INFO = new SM_SYSTEM_MESSAGE(1400485);

	/**
	 * 一阵强风爆发。 / A gust of air bursts forth
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WINDBOX_TRIGGER_ON_INFO = new SM_SYSTEM_MESSAGE(1400486);

	/**
	 * 塞玛塔里乌克斯施放防御魔法。2 小时后将离开其巢穴。 / Sematariux has cast defensive magic. You will be removed from Sematariux's Hideout in 2 hours
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_120M = new SM_SYSTEM_MESSAGE(1400487);

	/**
	 * 你：will be removed from Sematariux's Hideout in 1 hour and 30 minutes。 / You will be removed from Sematariux's Hideout in 1 hour and 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_90M = new SM_SYSTEM_MESSAGE(1400488);

	/**
	 * 你：will be removed from Sematariux's Hideout in 1 hour。 / You will be removed from Sematariux's Hideout in 1 hour
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_60M = new SM_SYSTEM_MESSAGE(1400489);

	/**
	 * 你：will be removed from Sematariux's Hideout in 30 minutes。 / You will be removed from Sematariux's Hideout in 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_30M = new SM_SYSTEM_MESSAGE(1400490);

	/**
	 * 你：will be removed from Sematariux's Hideout in 15 minutes。 / You will be removed from Sematariux's Hideout in 15 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_15M = new SM_SYSTEM_MESSAGE(1400491);

	/**
	 * 你：will be removed from Sematariux's Hideout in 10 minutes。 / You will be removed from Sematariux's Hideout in 10 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_10M = new SM_SYSTEM_MESSAGE(1400492);

	/**
	 * 你：will be removed from Sematariux's Hideout in 5 minutes。 / You will be removed from Sematariux's Hideout in 5 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_5M = new SM_SYSTEM_MESSAGE(1400493);

	/**
	 * 你：will be removed from Sematariux's Hideout in 3 minutes。 / You will be removed from Sematariux's Hideout in 3 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_3M = new SM_SYSTEM_MESSAGE(1400494);

	/**
	 * 你：will be removed from Sematariux's Hideout in 2 minutes。 / You will be removed from Sematariux's Hideout in 2 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_2M = new SM_SYSTEM_MESSAGE(1400495);

	/**
	 * 你：will be removed from Sematariux's Hideout in 1 minute。 / You will be removed from Sematariux's Hideout in 1 minute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_1M = new SM_SYSTEM_MESSAGE(1400496);

	/**
	 * 你：will be removed from Sematariux's Hideout in 30 seconds。 / You will be removed from Sematariux's Hideout in 30 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_30S = new SM_SYSTEM_MESSAGE(1400497);

	/**
	 * 你：will be removed from Sematariux's Hideout in 15 seconds。 / You will be removed from Sematariux's Hideout in 15 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_15S = new SM_SYSTEM_MESSAGE(1400498);

	/**
	 * 你：will be removed from Sematariux's Hideout in 10 seconds。 / You will be removed from Sematariux's Hideout in 10 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_10S = new SM_SYSTEM_MESSAGE(1400499);

	/**
	 * 你：will be removed from Sematariux's Hideout in 5 seconds。 / You will be removed from Sematariux's Hideout in 5 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_5S = new SM_SYSTEM_MESSAGE(1400500);

	/**
	 * 你：will be removed from Sematariux's Hideout in 4 seconds。 / You will be removed from Sematariux's Hideout in 4 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_4S = new SM_SYSTEM_MESSAGE(1400501);

	/**
	 * 你：will be removed from Sematariux's Hideout in 3 seconds。 / You will be removed from Sematariux's Hideout in 3 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_3S = new SM_SYSTEM_MESSAGE(1400502);

	/**
	 * 你：will be removed from Sematariux's Hideout in 2 seconds。 / You will be removed from Sematariux's Hideout in 2 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_2S = new SM_SYSTEM_MESSAGE(1400503);

	/**
	 * 你：will be removed from Sematariux's Hideout in 1 second。 / You will be removed from Sematariux's Hideout in 1 second
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_1S = new SM_SYSTEM_MESSAGE(1400504);

	/**
	 * 你：have been forcibly removed from Sematariux's Hideout by Sematariux's defensive magic。 / You have been forcibly removed from Sematariux's Hideout by Sematariux's defensive magic
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_OUT_TIMER_0S = new SM_SYSTEM_MESSAGE(1400505);

	/**
	 * 帕德玛拉什卡施放防御魔法。2 小时后将离开其洞穴。 / Padmarashka has cast defensive magic. You will be removed from Padmarashka's Cave in 2 hours
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_120M = new SM_SYSTEM_MESSAGE(1400506);

	/**
	 * 你：will be removed from Padmarashka's Cave in 1 hour and 30 minutes。 / You will be removed from Padmarashka's Cave in 1 hour and 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_90M = new SM_SYSTEM_MESSAGE(1400507);

	/**
	 * 你：will be removed from Padmarashka's Cave in 1 hour。 / You will be removed from Padmarashka's Cave in 1 hour
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_60M = new SM_SYSTEM_MESSAGE(1400508);

	/**
	 * 你：will be removed from Padmarashka's Cave in 30 minutes。 / You will be removed from Padmarashka's Cave in 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_30M = new SM_SYSTEM_MESSAGE(1400509);

	/**
	 * 你：will be removed from Padmarashka's Cave in 15 minutes。 / You will be removed from Padmarashka's Cave in 15 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_15M = new SM_SYSTEM_MESSAGE(1400510);

	/**
	 * 你：will be removed from Padmarashka's Cave in 10 minutes。 / You will be removed from Padmarashka's Cave in 10 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_10M = new SM_SYSTEM_MESSAGE(1400511);

	/**
	 * 你：will be removed from Padmarashka's Cave in 5 minutes。 / You will be removed from Padmarashka's Cave in 5 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_5M = new SM_SYSTEM_MESSAGE(1400512);

	/**
	 * 你：will be removed from Padmarashka's Cave in 3 minutes。 / You will be removed from Padmarashka's Cave in 3 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_3M = new SM_SYSTEM_MESSAGE(1400513);

	/**
	 * 你：will be removed from Padmarashka's Cave in 2 minutes。 / You will be removed from Padmarashka's Cave in 2 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_2M = new SM_SYSTEM_MESSAGE(1400514);

	/**
	 * 你：will be removed from Padmarashka's Cave in 1 minute。 / You will be removed from Padmarashka's Cave in 1 minute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_1M = new SM_SYSTEM_MESSAGE(1400515);

	/**
	 * 你：will be removed from Padmarashka's Cave in 30 seconds。 / You will be removed from Padmarashka's Cave in 30 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_30S = new SM_SYSTEM_MESSAGE(1400516);

	/**
	 * 你：will be removed from Padmarashka's Cave in 15 seconds。 / You will be removed from Padmarashka's Cave in 15 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_15S = new SM_SYSTEM_MESSAGE(1400517);

	/**
	 * 你：will be removed from Padmarashka's Cave in 10 seconds。 / You will be removed from Padmarashka's Cave in 10 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_10S = new SM_SYSTEM_MESSAGE(1400518);

	/**
	 * 你：will be removed from Padmarashka's Cave in 5 seconds。 / You will be removed from Padmarashka's Cave in 5 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_5S = new SM_SYSTEM_MESSAGE(1400519);

	/**
	 * 你：will be removed from Padmarashka's Cave in 4 seconds。 / You will be removed from Padmarashka's Cave in 4 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_4S = new SM_SYSTEM_MESSAGE(1400520);

	/**
	 * 你：will be removed from Padmarashka's Cave in 3 seconds。 / You will be removed from Padmarashka's Cave in 3 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_3S = new SM_SYSTEM_MESSAGE(1400521);

	/**
	 * 你：will be removed from Padmarashka's Cave in 2 seconds。 / You will be removed from Padmarashka's Cave in 2 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_2S = new SM_SYSTEM_MESSAGE(1400522);

	/**
	 * 你：will be removed from Padmarashka's Cave in 1 second。 / You will be removed from Padmarashka's Cave in 1 second
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_1S = new SM_SYSTEM_MESSAGE(1400523);

	/**
	 * 你：have been forcibly removed from Padmarashka's Cave by Padmarashka's defensive magic。 / You have been forcibly removed from Padmarashka's Cave by Padmarashka's defensive magic
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_OUT_TIMER_0S = new SM_SYSTEM_MESSAGE(1400524);

	/**
	 * 塞玛塔里乌克斯即将产卵。 / Sematariux is about to lay eggs
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_LAY_EGG = new SM_SYSTEM_MESSAGE(1400525);

	/**
	 * 帕德玛拉什卡即将产卵。 / Padmarashka is about to lay eggs
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_LAY_EGG = new SM_SYSTEM_MESSAGE(1400526);

	/**
	 * 你们这些卑微的守护者也敢？ / Lowly Daevas such as you would dare?
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_START_1 = new SM_SYSTEM_MESSAGE(1400527);

	/**
	 * 你：have leapt into certain death!。 / You have leapt into certain death!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_START_1 = new SM_SYSTEM_MESSAGE(1400528);

	/**
	 * Kaisinel's Agent Veille has engaged in battle to defend Inggison
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_GODELITE_START_1 = new SM_SYSTEM_MESSAGE(1400529);

	/**
	 * Kaisinel's Agent Veille has engaged in battle to defend Inggison
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_GODELITE_START_2 = new SM_SYSTEM_MESSAGE(1400530);

	/**
	 * Kaisinel's Agent Veille has engaged in battle to defend Inggison
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_GODELITE_START_3 = new SM_SYSTEM_MESSAGE(1400531);

	/**
	 * Kaisinel's Agent Veille has engaged in battle to defend Inggison
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_GODELITE_START_4 = new SM_SYSTEM_MESSAGE(1400532);

	/**
	 * Marchutan's Agent Mastarius has engaged in battle to defend Gelkmaros
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_GODELITE_START_1 = new SM_SYSTEM_MESSAGE(1400533);

	/**
	 * Marchutan's Agent Mastarius has engaged in battle to defend Gelkmaros
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_GODELITE_START_2 = new SM_SYSTEM_MESSAGE(1400534);

	/**
	 * Marchutan's Agent Mastarius has engaged in battle to defend Gelkmaros
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_GODELITE_START_3 = new SM_SYSTEM_MESSAGE(1400535);

	/**
	 * Marchutan's Agent Mastarius has engaged in battle to defend Gelkmaros
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_GODELITE_START_4 = new SM_SYSTEM_MESSAGE(1400536);

	/**
	 * 我哀叹自己未能成为龙！ / I grieve for I couldn't become a dragon!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_START_2 = new SM_SYSTEM_MESSAGE(1400537);

	/**
	 * 我从不在意繁殖的责任！ / I never cared much for the responsibility of breeding!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_START_3 = new SM_SYSTEM_MESSAGE(1400538);

	/**
	 * 我嘲笑你们这些以为能击败我的可悲守护者！ / I laugh at you pathetic Daevas who think you can defeat me!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_START_4 = new SM_SYSTEM_MESSAGE(1400539);

	/**
	 * 繁殖的责任是我的意志！ / The responsibility of breeding is my will!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_START_2 = new SM_SYSTEM_MESSAGE(1400540);

	/**
	 * 我必须保护这些卵！ / I must protect the eggs!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_START_3 = new SM_SYSTEM_MESSAGE(1400541);

	/**
	 * 你：will never see the light of day again!。 / You will never see the light of day again!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_START_4 = new SM_SYSTEM_MESSAGE(1400542);

	/**
	 * %0's buddy list is full
	 */
	public static SM_SYSTEM_MESSAGE STR_BUDDYLIST_BUDDYS_LIST_FULL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400543, value0);
	}

	/**
	 * 你：must be in an Alliance to access this area。 / You must be in an Alliance to access this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTER_ONLY_FORCE_DON = new SM_SYSTEM_MESSAGE(1400544);

	/**
	 * 你：must be in an League to access this area。 / You must be in an League to access this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTER_ONLY_UNION_DON = new SM_SYSTEM_MESSAGE(1401251);

	// 完成转职后才能升至 10 级。 / You can advance to level 10 only after you have completed the class change
	// 任务。 / quest.
	public static final SM_SYSTEM_MESSAGE STR_LEVEL_LIMIT_QUEST_NOT_FINISHED1 = new SM_SYSTEM_MESSAGE(1400545);
	// 你已达到可加入军团的等级。 / You’ve reached the level where you can join a legion.
	// 使用军团搜索查找你想要的军团。 / Use the legion search to find the legion you want.
	public static final SM_SYSTEM_MESSAGE STR_MSG_GUILD_CAN_JOIN_LEVEL = new SM_SYSTEM_MESSAGE(1403389);
	// 可通过转职任务成为守护者。 / You can become a Daeva through the class change mission. Once you complete
	// 完成任务后将达到 10 级，与当前经验无关。 / the mission, you will reach level 10, regardless of your EXP.
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAN_QUEST_DEVA = new SM_SYSTEM_MESSAGE(1403390);
	// 可通过转职任务成为高阶守护者。一旦你 / You can become an Archdaeva through the class change mission. Once you
	// 完成任务后将达到 66 级，与当前经验无关。 / complete the mission, you will reach level 66, regardless of your EXP.
	public static final SM_SYSTEM_MESSAGE STR_MSG_CAN_QUEST_HIGHDEVA = new SM_SYSTEM_MESSAGE(1403391);

	/**
	 * %0 位于 %1。 / %0 is located at %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_SUBZONE_FOUND_DEV(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400546, value0, value1);
	}

	/**
	 * 找不到通往 %0 的路径。%0 位于 %1。 / Cannot find the path to %0. %0 is located at %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_TOO_FAR_FROM_SUBZONE_DEV(String value0, String value2, String value1) {
		return new SM_SYSTEM_MESSAGE(1400547, value0, value2, value1);
	}

	/**
	 * %0 位于 %1。 / %0 is located at %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_NPC_FOUND_DEV(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400548, value0, value1);
	}

	/**
	 * 找不到通往 %0 的路径。%0 位于 %1。 / Cannot find the path to %0. %0 is located at %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_TOO_FAR_FROM_NPC_DEV(String value0, String value2, String value1) {
		return new SM_SYSTEM_MESSAGE(1400549, value0, value2, value1);
	}

	/**
	 * %0 位于 %1 的 %2。 / %0 is located at %2 in %1
	 */
	public static SM_SYSTEM_MESSAGE STR_FIND_POS_NPC_FOUND_IN_OTHER_WORLD_DEV(String value0, String value2,
			String value1) {
		return new SM_SYSTEM_MESSAGE(1400550, value0, value2, value1);
	}

	/**
	 * Cannot perform path finding--the cooldown timer has not expired
	 */
	public static final SM_SYSTEM_MESSAGE STR_FIND_POS_CANT_USE_UNTIL_DELAYTIME = new SM_SYSTEM_MESSAGE(1400551);

	/**
	 * @param value0 与 %0 交易剩余 1 分钟。 / There is 1 minute left to trade with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ALARM_REMAIN_ONE_MINUTE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400552, value0);
	}

	/**
	 * %0's temporary trade time has expired. %0 can no longer be traded
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_END_OF_EXCHANGE_TIME(int nameId, int timeMin) {
		return new SM_SYSTEM_MESSAGE(1400553, new DescriptionId(nameId), timeMin);
	}

	/**
	 * %0 为 not 目标 you 可 trade%1 带。 / %0 is not a target you can trade %1 with
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_WRONG_EXCHANGE_TARGET(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400554, value0, value1);
	}

	/**
	 * %0's temporary trade time has expired and can no longer be traded
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EXCHANGE_TIME_OVER(int value0) {
		return new SM_SYSTEM_MESSAGE(1400555, new DescriptionId(value0));
	}

	 /**
	  * %0 has acquired %1。
	  * %0 has acquired %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMPLETE_EXCHANGE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400556, value0, value1);
	}

	/**
	 * @param value0 你：do not have enough %0。 / You do not have enough %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_TRADE_MONEY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400557, value0);
	}

	/**
	 * You 有 invited%0 ' salliance 到 AllianceLeague.%0 ' salliance 有 total 的 %1 成员。 / You have invited %0's alliance to the Alliance League. %0's alliance has a total of %1 members
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_INVITE_HIM(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400558, value0, value1);
	}

	/**
	 * %0 所属联盟的队长是 %1。 / The alliance captain of the alliance %0 belongs to is %1
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_INVITE_HIS_LEADER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400559, value0, value1);
	}

	 /**
	  * 你的联盟已加入 %0 的战团。
	  * 你的联盟 has joined %0's 联盟 League。 / Your alliance has joined %0's Alliance League
	  */
	public static SM_SYSTEM_MESSAGE STR_UNION_ENTER_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400560, value0);
	}

	/**
	 * %0's alliance has joined the Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_ENTER_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400561, value0);
	}

	/**
	 * @param value0 你：have declined %0's invitation to join the Alliance League。 / You have declined %0's invitation to join the Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_REJECT_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400562, value0);
	}

	/**
	 * %0's alliance has declined your invitation to join the Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_REJECT_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400563, value0);
	}

	/**
	 * @param value0 %0 为已经成员的 anotherAllianceLeague。 / %0 is already a member of another Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_ALREADY_OTHER_UNION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400564, value0);
	}

	/**
	 * You 无法 inviteanymore 作为 AllianceLeague 为 full。 / You cannot invite anymore as the Alliance League is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_ADD_NEW_MEMBER = new SM_SYSTEM_MESSAGE(1400565);

	/**
	 * You 有不有 permission 到 invitepeople 到 League。 / You have don't have permission to invite people to the League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_LEADER_CAN_INVITE = new SM_SYSTEM_MESSAGE(1400566);

	/**
	 * @param value0 目前 %0 无法接受你的联盟邀请。 / Currently, %0 cannot accept your invitation to join the alliance
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400567, value0);
	}

	/**
	 * 你：cannot invite your own alliance。 / You cannot invite your own alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_INVITE_SELF = new SM_SYSTEM_MESSAGE(1400568);

	/**
	 * 玩家 youinvited 到 AllianceLeague 为 currently 离线。 / The player you invited to the Alliance League is currently offline
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_OFFLINE_MEMBER = new SM_SYSTEM_MESSAGE(1400569);

	/**
	 * 你：cannot use the Alliance League invitation function while you are dead。 / You cannot use the Alliance League invitation function while you are dead
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_INVITE_WHEN_DEAD = new SM_SYSTEM_MESSAGE(1400570);

	/**
	 * 你：have left the Alliance League。 / You have left the Alliance League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_LEAVE_ME = new SM_SYSTEM_MESSAGE(1400571);

	/**
	 * %0's alliance has left the Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_LEAVE_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400572, value0);
	}

	/**
	 * Only an alliance captain can leave the Alliance League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_LEADER_CAN_LEAVE = new SM_SYSTEM_MESSAGE(1400573);

	/**
	 * @param value0 你：have expelled %0's alliance from the Alliance League。 / You have expelled %0's alliance from the Alliance League
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_BAN_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400574, value0);
	}

	 /**
	  * %0 has expelled %1's alliance from the Alliance League。
	  * %0 has expelled %1's alliance from the Alliance League
	  */
	public static SM_SYSTEM_MESSAGE STR_UNION_BAN_HIS_LEADER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400575, value0, value1);
	}

	 /**
	  * %0 has expelled your alliance from the Alliance League。
	  * %0 has expelled your alliance from the Alliance League
	  */
	public static SM_SYSTEM_MESSAGE STR_UNION_BAN_ME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400576, value0);
	}

	/**
	 * Only the league leader can kick out an alliance from the Alliance League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_LEADER_CAN_BAN = new SM_SYSTEM_MESSAGE(1400577);

	/**
	 * You 无法移除 your 拥有 Alliance。 / You cannot remove your own Alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_BAN_SELF = new SM_SYSTEM_MESSAGE(1400578);

	/**
	 * AllianceLeague 有 disbandeddue 到 insufficient 编号的 alliances。 / The Alliance League has disbanded due to an insufficient number of alliances
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_DISPERSED = new SM_SYSTEM_MESSAGE(1400579);

	/**
	 * 你：transferred the league leadership to %0. From now on, %0 is the league leader。 / You transferred the league leadership to %0. From now on, %0 is the league leader
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_CHANGE_LEADER(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400580, value0, value1);
	}

	/**
	 * %0 将战团领导权托付给 %1。此后 %1 为战团领袖。 / %0 entrusted %1 with the league leadership authority. From now on, %1 is the league leader
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_HE_IS_NEW_LEADER(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1400581, value0, value1, value2);
	}

	/**
	 * 你现已成为战团领袖。 / You are now the League leader
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_YOU_BECOME_NEW_LEADER = new SM_SYSTEM_MESSAGE(1400582);

	/**
	 * 仅战团领袖可将领导权转让给联盟队长。 / Only the league leader can transfer the league leader authority to an alliance captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_LEADER_CAN_CHANGE_LEADER = new SM_SYSTEM_MESSAGE(1400583);

	/**
	 * 你：cannot transfer the league leadership to someone who isn't an alliance captain。 / You cannot transfer the league leadership to someone who isn't an alliance captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_CAN_CHANGE_LEADER_TO_FORCE_LEADER = new SM_SYSTEM_MESSAGE(
			1400584);

	/**
	 * 无法将队长转让给自己。 / You cannot transfer leadership to yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_CHANGE_LEADER_SELF = new SM_SYSTEM_MESSAGE(1400585);

	/**
	 * You 无法 transferleadership 到玩家 outsideyourLeague。 / You cannot transfer the leadership to a player outside your League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_CHANGE_LEADER_OTHER_UNION = new SM_SYSTEM_MESSAGE(1400586);

	/**
	 * 你现已成为战团领袖。 / You are now the League leader
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_YOU_BECOME_NEW_LEADER_TIMEOUT = new SM_SYSTEM_MESSAGE(1400587);

	/**
	 * @param value0 %0 被自动授予战团领袖权限。 / %0 was automatically entrusted with the league leader authority
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_CHANGE_LEADER_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400588, value0);
	}

	/**
	 * @param num0 alliance 编号有已 changed 到 %num0。 / The alliance number has been changed to %num0
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_CHANGE_FORCE_NUMBER_ME(int num0) {
		return new SM_SYSTEM_MESSAGE(1400589, num0);
	}

	/**
	 * alliance 编号的 %0 有已 changed 到 %num0。 / The alliance number of %0 has been changed to %num0
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_CHANGE_FORCE_NUMBER_HIM(String value0, int num0) {
		return new SM_SYSTEM_MESSAGE(1400590, value0, num0);
	}

	/**
	 * You 无法 change 其到 unclaimedalliance 编号。 / You cannot change it to an unclaimed alliance number
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_CHANGE_FORCE_NUMBER = new SM_SYSTEM_MESSAGE(1400591);

	/**
	 * 你：cannot change the league leader's alliance number。 / You cannot change the league leader's alliance number
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_CANT_CHANGE_LEADER_NUMBER = new SM_SYSTEM_MESSAGE(1400592);

	/**
	 * 仅战团领袖可更改联盟编号。 / Only the League leader can change the Alliance number
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_LEADER_CAN_CHANGE_FORCE_NUMBER = new SM_SYSTEM_MESSAGE(
			1400593);

	 /**
	  * 你至少需要达到 %0 级才能晋升。
	  * Your must be at least level %0 to be promoted
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_LEVELLOW_RANK_UP(String value0) {
		return new SM_SYSTEM_MESSAGE(1400594, value0);
	}

	/**
	 * 隔板已激活，第一军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the First Armory and Gravity Control has been sealed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SHIELD_A_SPAWN_IDAB1_Dreadgion01 = new SM_SYSTEM_MESSAGE(1400595);

	/**
	 * 隔板已激活，第二军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the Second Armory and Gravity Control has been sealed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SHIELD_B_SPAWN_IDAB1_Dreadgion01 = new SM_SYSTEM_MESSAGE(1400596);

	/**
	 * 你：can use Screen Capture once every %0 seconds. Time Remaining: %1 seconds。 / You can use Screen Capture once every %0 seconds. Time Remaining: %1 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMAIN_PRINT_SCREEN_COOLTIME(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400600, value0, value1);
	}

	/**
	 * 你不in an League。 / You are not in an League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_YOU_ARE_NOT_UNION_MEMBER = new SM_SYSTEM_MESSAGE(1400601);

	/**
	 * 该队长不属于本战团。 / The captain is not part of the League
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_CAN_BAN_FORCE_LEADER = new SM_SYSTEM_MESSAGE(1400602);

	/**
	 * @param value0 selected 目标为已经成员的 anotherforceleague。 / The selected target is already a member of another force league
	 */
	public static SM_SYSTEM_MESSAGE STR_UNION_ALREADY_MY_UNION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400603, value0);
	}

	/**
	 * 隔板已激活，第一军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the First Armory and Gravity Control has been sealed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SHIELD_A_SPAWN_IDDreadgion02 = new SM_SYSTEM_MESSAGE(1400604);

	/**
	 * 隔板已激活，第二军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the Second Armory and Gravity Control has been sealed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SHIELD_B_SPAWN_IDDreadgion02 = new SM_SYSTEM_MESSAGE(1400605);

	/**
	 * 欧米伽召唤生物。 / Omega summons a creature
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_RaidShowTime_Phase1 = new SM_SYSTEM_MESSAGE(1400606);

	/**
	 * 欧米伽召唤强大生物。 / Omega summons a powerful creature
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_RaidShowTime_Phase2 = new SM_SYSTEM_MESSAGE(1400607);

	/**
	 * 欧米伽召唤治疗生物。 / Omega summons a healing creature
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_RaidShowTime_Phase3 = new SM_SYSTEM_MESSAGE(1400608);

	/**
	 * 欧米伽召唤制造屏障的生物。 / Omega summons a creature that creates barriers
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_RaidShowTime_Phase4 = new SM_SYSTEM_MESSAGE(1400609);

	/**
	 * 毒素与麻痹攻击开始。 / Attack of poison and paralysis begins
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_RaidShowTime_Phase1 = new SM_SYSTEM_MESSAGE(1400610);

	/**
	 * 限制物理与魔���攻击的攻势开始。 / Attack that restricts physical and magical assaults begins
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_RaidShowTime_Phase2 = new SM_SYSTEM_MESSAGE(1400611);

	/**
	 * 诸神黄昏的酸液出现。 / Ragnarok's acidic fluid appears
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_RaidShowTime_Phase3 = new SM_SYSTEM_MESSAGE(1400612);

	/**
	 * 强力持续攻击与反射开始。 / Powerful continuous attacks and reflections begin
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_RaidShowTime_Phase4 = new SM_SYSTEM_MESSAGE(1400613);

	/**
	 * You 无法 invitesomeone 谁 doesn ' tbelong 到 alliance 到 league。 / You cannot invite someone who doesn't belong to an alliance to the league
	 */
	public static final SM_SYSTEM_MESSAGE STR_UNION_ONLY_INVITE_FORCE_MEMBER = new SM_SYSTEM_MESSAGE(1400614);

	/**
	 * @param value0 %0 拍拍肚子表示吃饱了。 / %0 Pats its tummy and indicates that its full
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_PET_FULL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400615, value0);
	}

	/**
	 * %0 吃了 %1。 / %0 eats %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_START_EATING(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400616, value0, value1);
	}

	/**
	 * 停止给 %0 喂食 %1。 / Stop feeding %1 to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_STOP_EATING(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400617, value1, value0);
	}

	/**
	 * %0 吐出 %1 并做鬼脸。 / %0 spits out %1 and makes a face
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR(String petName, int nameId) {
		return new SM_SYSTEM_MESSAGE(1400618, petName, new DescriptionId(nameId));
	}

	/**
	 * %0 感激地送给你 %1（剩余次数：%2/%3）。 / %0 is grateful and gives you a %1 as a present (Times remaining: %2/%3)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_CASH_REWARD(String value0, String value1, String value2,
			String value3) {
		return new SM_SYSTEM_MESSAGE(1400619, value0, value1, value2, value3);
	}

	 /**
	  * %0 has enjoyed eating %1。
	  * %0 has enjoyed eating %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_ATE_FOOD_1(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400620, value0, value1);
	}

	 /**
	  * %0 has enjoyed eating %1 and looks pleased。
	  * %0 has enjoyed eating %1 and looks pleased
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_ATE_FOOD_2(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400621, value0, value1);
	}

	 /**
	  * %0 has enjoyed eating %1 and looks happy。
	  * %0 has enjoyed eating %1 and looks happy
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_ATE_FOOD_3(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400622, value0, value1);
	}

	 /**
	  * %0 has finished eating %1。
	  * %0 has finished eating %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_ATE_ALL_FOOD(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400623, value0, value1);
	}

	/**
	 * %0 感谢并给予你 %2。 / %0 is thankful and gives you %2
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_COMMON_REWARD(String value0, String value2) {
		return new SM_SYSTEM_MESSAGE(1400624, value0, value2);
	}

	/**
	 * 仅在联盟战团中可用。 / Only available while in an Alliance League
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_SPLIT_UNION = new SM_SYSTEM_MESSAGE(1400625);

	 /**
	  * Alliance League's looting method has changed to %0, %1 %2。
	  * The Alliance League's looting method has changed to %0, %1 %2
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_UNION_LOOTING_CHANGED_RULE(String value0, String value1, String value2) {
		return new SM_SYSTEM_MESSAGE(1400626, value0, value1, value2);
	}

	/**
	 * An infiltration passage into the Chantra Dredgion has opened
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDDREADGION_02 = new SM_SYSTEM_MESSAGE(1400628);

	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDDREADGION_03 = new SM_SYSTEM_MESSAGE(1401398);

	/**
	 * 敌方已放弃钱特拉战舰渗透任务。任务将在 %DURATIONTIME0 后结束并离开。 / The opposition has abandoned the Chantra Dredgion infiltration mission. You will leave the Chantra Dredgion when the mission ends in %DURATIONTIME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ALARM_COLD_GAME_IDDREADGION_02(String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400629, durationtime0);
	}

	/**
	 * You 可保存一个的 twoReiansimprisoned 在 cocoon。 / You can save one of the two Reians imprisoned in the cocoon
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_Cocoon_Yell = new SM_SYSTEM_MESSAGE(1400630);

	/**
	 * Supplies Storage teleport device has been created at Escape Hatch
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WAREHOUSETELEPORTER_CREATED_IDDREADGION_02_01 = new SM_SYSTEM_MESSAGE(
			1400631);

	/**
	 * Captain Zanata has appeared in the Captain's Cabin
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDREADGION_02 = new SM_SYSTEM_MESSAGE(1400632);

	/**
	 * Officer Kamanya has appeared in Gravity Control
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BONUSNPC_SPAWN_IDDREADGION_02 = new SM_SYSTEM_MESSAGE(1400633);

	 /**
	  * treasure chest will appear if you defeat Ebonsoul within one minute。
	  * A treasure chest will appear if you defeat Ebonsoul within one minute
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdC_Light_Die = new SM_SYSTEM_MESSAGE(1400634);

	 /**
	  * treasure chest will appear if you defeat Rukril within one minute。
	  * A treasure chest will appear if you defeat Rukril within one minute
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdC_Dark_Die = new SM_SYSTEM_MESSAGE(1400635);

	 /**
	  * treasure chest has appeared。
	  * A treasure chest has appeared
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdC_BoxSpawn = new SM_SYSTEM_MESSAGE(1400636);

	/**
	 * 雅门尼斯打开召唤门并开始召唤仆从。 / Yamennes opens the Spawn Gate and begins to summon his minions
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdD_SummonStart = new SM_SYSTEM_MESSAGE(1400637);

	/**
	 * 宠物袋没有空间。 / There is no space in the Pet Pouch
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_TOO_MANY_ITEMS_TOYPET_WAREHOUSE = new SM_SYSTEM_MESSAGE(
			1400638);

	 /**
	  * 你的Favorites list is full。
	  * 你的Favorites list已满。 / Your Favorites list is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_COMBINE_FAVORIT_LIST_FULL = new SM_SYSTEM_MESSAGE(1400639);

	/**
	 * @param worldname0 你：have entered %WORLDNAME0. Your allies are barred from joining you。 / You have entered %WORLDNAME0. Your allies are barred from joining you
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400640, worldname0);
	}

	/**
	 * 你：have successfully entered the area, consuming one of your permitted entries。 / You have successfully entered the area, consuming one of your permitted entries
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_COUNT_USE = new SM_SYSTEM_MESSAGE(1402084);

	/**
	 * 物资仓库传送装置已在副逃生舱口生成。 / Supplies Storage teleport device has been created at the Secondary Escape Hatch
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WAREHOUSETELEPORTER_CREATED_IDDREADGION_02_02 = new SM_SYSTEM_MESSAGE(
			1400641);

	/**
	 * 这不是可用的宠物蛋。 / This is not a usable pet egg
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PET_NOT_PET_COUPON = new SM_SYSTEM_MESSAGE(1400642);

	/**
	 * That name is invalid. Please try another
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PET_NOT_AVALIABE_NAME = new SM_SYSTEM_MESSAGE(1400643);

	/**
	 * @param value0 你：abandoned %0。 / You abandoned %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_ABANDON_PET_COMPLETE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400644, value0);
	}

	/**
	 * @param value0 你：summoned %0。 / You summoned %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_SUMMONED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400645, value0);
	}

	/**
	 * @param value0 %0 已被解散。 / %0 has been dismissed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_UNSUMMONED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400646, value0);
	}

	/**
	 * You 无法放入此物品在 PetPouch。 / You cannot put this item in the Pet Pouch
	 */
	public static final SM_SYSTEM_MESSAGE STR_WAREHOUSE_CANT_DEPOSIT_ITEM_TOYPET_WAREHOUSE = new SM_SYSTEM_MESSAGE(
			1400647);

	/**
	 * 你：cannot combine equipped items。 / You cannot combine equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_EQUIPED_ITEM = new SM_SYSTEM_MESSAGE(1400648);

	/**
	 * 生命能量开始在你体内流淌。 / Life energy begins to course through your body
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BARD_BUFF_LIFE = new SM_SYSTEM_MESSAGE(1400649);

	/**
	 * 你：feel all your muscles becoming harder。 / You feel all your muscles becoming harder
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BARD_BUFF_PROTECTION = new SM_SYSTEM_MESSAGE(1400650);

	/**
	 * 你：already have the selected pet。 / You already have the selected pet
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_ALREADY_TAMED_PET = new SM_SYSTEM_MESSAGE(1400651);

	/**
	 * 船长室传送装置已在中央通道尽头生成。 / Captain's Cabin teleport device has been created at the end of the Central Passage
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSSTELEPORTER_CREATED_IDDREDAGION_02 = new SM_SYSTEM_MESSAGE(
			1400652);

	/**
	 * There is an object of great power nearby
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_SKILL = new SM_SYSTEM_MESSAGE(1400653);

	/**
	 * 你：can use a Silver Blade Rotan to destroy the rock door leading to the Temple Vault。 / You can use a Silver Blade Rotan to destroy the rock door leading to the Temple Vault
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_DOOR = new SM_SYSTEM_MESSAGE(1400654);

	/**
	 * 你：have acquired the 'Cool Water' effect from the garden fountain。 / You have acquired the 'Cool Water' effect from the garden fountain
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_BUFF_01 = new SM_SYSTEM_MESSAGE(1400655);

	/**
	 * 你：have acquired the 'Sweet Fruit' effect from the fruit basket。 / You have acquired the 'Sweet Fruit' effect from the fruit basket
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_BUFF_02 = new SM_SYSTEM_MESSAGE(1400656);

	/**
	 * 你：have acquired the 'Tasty Meat' effect from the Porgus Barbecue。 / You have acquired the 'Tasty Meat' effect from the Porgus Barbecue
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_BUFF_03 = new SM_SYSTEM_MESSAGE(1400657);

	/**
	 * 你：have acquired the 'Prophet's Blessing' effect from the Prophet's Tower。 / You have acquired the 'Prophet's Blessing' effect from the Prophet's Tower
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_BUFF_04 = new SM_SYSTEM_MESSAGE(1400658);

	/**
	 * 你：sense a movement in Taloc's Roots. You won't be able to meet him unless you hurry。 / You sense a movement in Taloc's Roots. You won't be able to meet him unless you hurry
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_HYAS_SPAWN_INFO = new SM_SYSTEM_MESSAGE(1400659);

	/**
	 * 烟雾正在释放。接触烟雾将破坏金奎德的屏障。 / Smoke is being discharged. Exposure to smoke will destroy Kinquid's Barrier
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDELIM_GAS_INFO = new SM_SYSTEM_MESSAGE(1400660);

	/**
	 * @param worldname0 你：must leave your group or alliance to enter %WORLDNAME0。 / You must leave your group or alliance to enter %WORLDNAME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_NEED_SOLO(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1403080, worldname0);
	}

	/**
	 * 塞玛塔里乌克斯已死亡。30 分钟后将离开其巢穴。 / Sematariux has died. You will be removed from Sematariux's Hideout in 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_30M = new SM_SYSTEM_MESSAGE(1400661);

	/**
	 * 你：will be removed from Sematariux's Hideout in 25 minutes。 / You will be removed from Sematariux's Hideout in 25 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_25M = new SM_SYSTEM_MESSAGE(1400662);

	/**
	 * 你：will be removed from Sematariux's Hideout in 20 minutes。 / You will be removed from Sematariux's Hideout in 20 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_20M = new SM_SYSTEM_MESSAGE(1400663);

	/**
	 * 你：will be removed from Sematariux's Hideout in 15 minutes。 / You will be removed from Sematariux's Hideout in 15 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_15M = new SM_SYSTEM_MESSAGE(1400664);

	/**
	 * 你：will be removed from Sematariux's Hideout in 10 minutes。 / You will be removed from Sematariux's Hideout in 10 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_10M = new SM_SYSTEM_MESSAGE(1400665);

	/**
	 * 你：will be removed from Sematariux's Hideout in 5 minutes。 / You will be removed from Sematariux's Hideout in 5 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_5M = new SM_SYSTEM_MESSAGE(1400666);

	/**
	 * 你：will be removed from Sematariux's Hideout in 4 minutes。 / You will be removed from Sematariux's Hideout in 4 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_4M = new SM_SYSTEM_MESSAGE(1400667);

	/**
	 * 你：will be removed from Sematariux's Hideout in 3 minutes。 / You will be removed from Sematariux's Hideout in 3 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_3M = new SM_SYSTEM_MESSAGE(1400668);

	/**
	 * 你：will be removed from Sematariux's Hideout in 2 minutes。 / You will be removed from Sematariux's Hideout in 2 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_2M = new SM_SYSTEM_MESSAGE(1400669);

	/**
	 * 你：will be removed from Sematariux's Hideout in 1 minute。 / You will be removed from Sematariux's Hideout in 1 minute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_1M = new SM_SYSTEM_MESSAGE(1400670);

	/**
	 * 你：will be removed from Sematariux's Hideout in 30 seconds。 / You will be removed from Sematariux's Hideout in 30 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_30S = new SM_SYSTEM_MESSAGE(1400671);

	/**
	 * 你：will be removed from Sematariux's Hideout in 15 seconds。 / You will be removed from Sematariux's Hideout in 15 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_15S = new SM_SYSTEM_MESSAGE(1400672);

	/**
	 * 你：will be removed from Sematariux's Hideout in 10 seconds。 / You will be removed from Sematariux's Hideout in 10 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_10S = new SM_SYSTEM_MESSAGE(1400673);

	/**
	 * 你：will be removed from Sematariux's Hideout in 5 seconds。 / You will be removed from Sematariux's Hideout in 5 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_KILLED_OUT_TIMER_5S = new SM_SYSTEM_MESSAGE(1400674);

	/**
	 * 帕德玛拉什卡已死亡。30 分钟后将离开其洞穴。 / Padmarashka has died. You will be removed from Padmarashka's Cave in 30 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_30M = new SM_SYSTEM_MESSAGE(1400675);

	/**
	 * 你：will be removed from Padmarashka's Cave in 25 minutes。 / You will be removed from Padmarashka's Cave in 25 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_25M = new SM_SYSTEM_MESSAGE(1400676);

	/**
	 * 你：will be removed from Padmarashka's Cave in 20 minutes。 / You will be removed from Padmarashka's Cave in 20 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_20M = new SM_SYSTEM_MESSAGE(1400677);

	/**
	 * 你：will be removed from Padmarashka's Cave in 15 minutes。 / You will be removed from Padmarashka's Cave in 15 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_15M = new SM_SYSTEM_MESSAGE(1400678);

	/**
	 * 你：will be removed from Padmarashka's Cave in 10 minutes。 / You will be removed from Padmarashka's Cave in 10 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_10M = new SM_SYSTEM_MESSAGE(1400679);

	/**
	 * 你：will be removed from Padmarashka's Cave in 5 minutes。 / You will be removed from Padmarashka's Cave in 5 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_5M = new SM_SYSTEM_MESSAGE(1400680);

	/**
	 * 你：will be removed from Padmarashka's Cave in 4 minutes。 / You will be removed from Padmarashka's Cave in 4 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_4M = new SM_SYSTEM_MESSAGE(1400681);

	/**
	 * 你：will be removed from Padmarashka's Cave in 3 minutes。 / You will be removed from Padmarashka's Cave in 3 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_3M = new SM_SYSTEM_MESSAGE(1400682);

	/**
	 * 你：will be removed from Padmarashka's Cave in 2 minutes。 / You will be removed from Padmarashka's Cave in 2 minutes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_2M = new SM_SYSTEM_MESSAGE(1400683);

	/**
	 * 你：will be removed from Padmarashka's Cave in 1 minute。 / You will be removed from Padmarashka's Cave in 1 minute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_1M = new SM_SYSTEM_MESSAGE(1400684);

	/**
	 * 你：will be removed from Padmarashka's Cave in 30 seconds。 / You will be removed from Padmarashka's Cave in 30 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_30S = new SM_SYSTEM_MESSAGE(1400685);

	/**
	 * 你：will be removed from Padmarashka's Cave in 15 seconds。 / You will be removed from Padmarashka's Cave in 15 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_15S = new SM_SYSTEM_MESSAGE(1400686);

	/**
	 * 你：will be removed from Padmarashka's Cave in 10 seconds。 / You will be removed from Padmarashka's Cave in 10 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_10S = new SM_SYSTEM_MESSAGE(1400687);

	/**
	 * 你：will be removed from Padmarashka's Cave in 5 seconds。 / You will be removed from Padmarashka's Cave in 5 seconds
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_KILLED_OUT_TIMER_5S = new SM_SYSTEM_MESSAGE(1400688);

	/**
	 * The destruction of the Huge Aether Fragment has destabilized the artifact!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_Artifact_Die_01 = new SM_SYSTEM_MESSAGE(1400689);

	/**
	 * 巨大奥德碎片被摧毁，神器守护者进入警戒！ / The destruction of the Huge Aether Fragment has put the artifact protector on alert!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_Artifact_Die_02 = new SM_SYSTEM_MESSAGE(1400690);

	/**
	 * 巨大奥德碎片被摧毁导致神器异常，神器守护者暴怒！ / The destruction of the Huge Aether Fragment has caused abnormality on the artifact. The artifact protector is furious!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_Artifact_Die_03 = new SM_SYSTEM_MESSAGE(1400691);

	/**
	 * 你：may change a pet's name once every %DURATIONTIME1 (Time remaining: %DURATIONTIME0)。 / You may change a pet's name once every %DURATIONTIME1 (Time remaining: %DURATIONTIME0)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_NAME_CHANGE_DELAY(String durationtime1, String durationtime0) {
		return new SM_SYSTEM_MESSAGE(1400692, durationtime1, durationtime0);
	}

	 /**
	  * pet has been renamed。
	  * The pet has been renamed
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_NAME_CHANGED = new SM_SYSTEM_MESSAGE(1400693);

	/**
	 * You 已经有 pet 的相同名称 .Pleasechooseanother 名称。 / You already have a pet of the same name. Please choose another name
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_EXISTING_NAME = new SM_SYSTEM_MESSAGE(1400694);

	/**
	 * 你：cannot feed it right now。 / You cannot feed it right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_CANT_NOW = new SM_SYSTEM_MESSAGE(1400695);

	/**
	 * @param value0 %0 表示它不饿。 / %0 indicates that it is not hungry
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_LIMIT_LOVE_COUNT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400696, value0);
	}

	/**
	 * 你：received %0: %1。 / You received %0: %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_PET_TAME_COMPLETE(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400697, value0, value1);
	}

	/**
	 * @param value0 你：cannot feed your pet %0。 / You cannot feed your pet %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSGBOX_TOYPET_FEED_CANT_FEED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400698, value0);
	}

	/**
	 * 距离过远：your pet to feed it。 / You are too far from your pet to feed it
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSGBOX_TOYPET_FEED_CANT_FEED_TOO_FAR = new SM_SYSTEM_MESSAGE(1400699);

	/**
	 * 你：cannot feed your pet while moving。 / You cannot feed your pet while moving
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSGBOX_TOYPET_FEED_CANT_FEED_WHEN_MOVING = new SM_SYSTEM_MESSAGE(1400700);

	/**
	 * You 有 obtainedobject 带 greatpower. 用于 quickaccess , drag 物品从 yourCube 到 yourQuickbar。 / You have obtained an object with great power. For quick access, drag the item from your Cube to your Quickbar
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDCROMEDE_SKILL_01 = new SM_SYSTEM_MESSAGE(1400701);

	/**
	 * @param value0 %0 无法进食。 / %0 cannot eat any food
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_PET_CANT_EAT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400702, value0);
	}

	/**
	 * You 有 obtained 新物品从 selected 目标。 / You have obtained a new item from the selected target
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GET_QUEST_ITEM = new SM_SYSTEM_MESSAGE(1400703);

	/**
	 * 你：must destroy the enemies of Taloc. It allows you to acquire objects with great power。 / You must destroy the enemies of Taloc. It allows you to acquire objects with great power
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NOTICE_LOOT_SKILL_ITEM = new SM_SYSTEM_MESSAGE(1400704);

	/**
	 * 你：cannot kick yourself out of the channel。 / You cannot kick yourself out of the channel
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_BAN_SELF = new SM_SYSTEM_MESSAGE(1400705);

	/**
	 * 你：cannot kick yourself out of the channel。 / You cannot kick yourself out of the channel
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_BAN_SELF = new SM_SYSTEM_MESSAGE(1400706);

	/**
	 * 无法将队长转让给自己。 / You cannot transfer leadership to yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_CHANGE_LEADER_SELF = new SM_SYSTEM_MESSAGE(1400707);

	/**
	 * 无法将队长转让给自己。 / You cannot transfer leadership to yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_CHANGE_LEADER_SELF = new SM_SYSTEM_MESSAGE(1400708);

	/**
	 * You 无法 use 其 becauseversion 的 yourpackage 为 toolow。 / You cannot use it because the version of your package is too low
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NO_RIGHT_PACKAGE_VERSION = new SM_SYSTEM_MESSAGE(1400709);

	/**
	 * Items stored in the surrendered pet's bag have been returned to your cube
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_RETURN_MASTER_ITEM = new SM_SYSTEM_MESSAGE(1400710);

	 /**
	  * 你的宠物时间已到，%0 已离开。
	  * 你的宠物's 时间已到，%0 已离开。 / Your pet's time is up. %0 has gone
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_ABANDON_EXPIRE_TIME_COMPLETE(String value0) {
		return new SM_SYSTEM_MESSAGE(1401194, value0);
	}

	/**
	 * 你：must defeat the protector within the time limit to wake Padmarashka from the Protective Slumber。 / You must defeat the protector within the time limit to wake Padmarashka from the Protective Slumber
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_GUARDIAN_START = new SM_SYSTEM_MESSAGE(1400711);

	/**
	 * Padmarashka has summoned the protector once again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_GUARDIAN_FAIL = new SM_SYSTEM_MESSAGE(1400712);

	/**
	 * Hamerun has dropped a treasure chest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDNOVICE_HAMEROON_TREASUREBOX_SPAWN = new SM_SYSTEM_MESSAGE(1400713);

	/**
	 * @param value0 你：have failed to reclaim %0。 / You have failed to reclaim %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_RETURN_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400714, value0);
	}

	/**
	 * 你：have failed to reclaim %num1 %0s。 / You have failed to reclaim %num1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_RETURN_ITEM_MULTI(int num1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400715, num1, value0s);
	}

	/**
	 * %0 感激地送给你 %1 作为礼物。 / %0 is grateful and gives you a %1 as a present
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_FEED_CASH_REWARD_CASH_UNLIMITED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400716, value0, value1);
	}

	/**
	 * 无法 find 物品到 combine。 / Cannot find the item to combine
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ITEM_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1400717);

	/**
	 * 无法 find 物品到移除 combination 从。 / Cannot find the item to remove the combination from
	 */
	public static final SM_SYSTEM_MESSAGE STR_DECOMPOUND_ITEM_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1400718);

	/**
	 * 你：cannot expel the alliance captain。 / You cannot expel the alliance captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_BANISH_LEADER = new SM_SYSTEM_MESSAGE(1400719);

	/**
	 * 你：cannot appoint yourself as an alliance vice captain。 / You cannot appoint yourself as an alliance vice captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_PROMOTE_MANAGER_SELF = new SM_SYSTEM_MESSAGE(1400720);

	/**
	 * 你已是联盟副队长。 / You are already appointed as an alliance vice captain
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_PROMOTE_MANAGER_AGAIN = new SM_SYSTEM_MESSAGE(1400721);

	/**
	 * You 无法 demoteyourself 到 alliance 成员。 / You cannot demote yourself to an alliance member
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_DEMOTE_MANAGER_SELF = new SM_SYSTEM_MESSAGE(1400722);

	/**
	 * 你：cannot demote an alliance member。 / You cannot demote an alliance member
	 */
	public static final SM_SYSTEM_MESSAGE STR_FORCE_CANT_DEMOTE_MANAGER_AGAIN = new SM_SYSTEM_MESSAGE(1400723);

	/**
	 * Thunder Storm has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_SUMMON_THUNDER = new SM_SYSTEM_MESSAGE(1400724);

	/**
	 * Terra Blast has appeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_SUMMON_EARTH = new SM_SYSTEM_MESSAGE(1400725);

	/**
	 * Acid mist has covered some areas
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_SUMMON_POISON = new SM_SYSTEM_MESSAGE(1400726);

	/**
	 * Sematariux has awoken from the Protective Slumber
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_DRAMATA_AWAKENING = new SM_SYSTEM_MESSAGE(1400727);

	/**
	 * Padmarashka has awoken from the Protective Slumber
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_DRAMATA_AWAKENING = new SM_SYSTEM_MESSAGE(1400728);

	/**
	 * Yamennes's threat level has been reset!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdD_ResetAggro = new SM_SYSTEM_MESSAGE(1400729);

	/**
	 * 被召唤的拉皮利玛正在治疗雅门尼斯！ / A summoned Lapilima is healing Yamennes!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdD_Heal = new SM_SYSTEM_MESSAGE(1400730);

	/**
	 * Yamennes Blindsight has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdD_Wakeup = new SM_SYSTEM_MESSAGE(1400731);

	/**
	 * Yamennes Painflare has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDAbRe_Core_NmdDH_Wakeup = new SM_SYSTEM_MESSAGE(1400732);

	/**
	 * @param value0 你：cannot summon a pet in %0。 / You cannot summon a pet in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_SUMMON_STATE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400733, value0);
	}

	/**
	 * 你in an altered state and cannot summon a pet。 / You are in an altered state and cannot summon a pet
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_SUMMON_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(1400734);

	/**
	 * @param value0 你：cannot dismiss a pet in %0。 / You cannot dismiss a pet in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_UNSUMMON_STATE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400735, value0);
	}

	/**
	 * 你in an altered state and cannot dismiss a pet。 / You are in an altered state and cannot dismiss a pet
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_UNSUMMON_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(1400736);

	/**
	 * @param value0 你：must be at least level %0 to perform extraction。 / You must be at least level %0 to perform extraction
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_GATHERING_B_LEVEL_CHECK(String value0) {
		return new SM_SYSTEM_MESSAGE(1400737, value0);
	}

	/**
	 * 物品 couldn ' t 为 registereddue 到 change 在 fees。 / Item couldn't be registered due to a change in the fees
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_CAN_NOT_REGISTER_ITEM_FEE_CHANGED = new SM_SYSTEM_MESSAGE(1400738);

	 /**
	  * first Sphere of Destiny has been activated。
	  * The first Sphere of Destiny has been activated
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_FIRST_OBJECT_ON_DF = new SM_SYSTEM_MESSAGE(1400739);

	/**
	 * 第二命运球已激活。玛尔库坦代理人玛斯塔里乌斯准备施放主神祝福。 / The second Sphere of Destiny has been activated. Marchutan's Agent Mastarius prepares to cast the Empyrean Lord's blessing
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_SECOND_OBJECT_ON_DF = new SM_SYSTEM_MESSAGE(1400740);

	/**
	 * 你：may use the Sphere of Destiny again。 / You may use the Sphere of Destiny again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_CAN_USE_OBJECT_DF = new SM_SYSTEM_MESSAGE(1400741);

	/**
	 * 你：need more people to activate the Sphere of Destiny。 / You need more people to activate the Sphere of Destiny
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_BUFF_CANT_USE_OBJECT_NOT_ENOUGH_MEMBER_DF = new SM_SYSTEM_MESSAGE(
			1400742);

	/**
	 * Such basic crafting doesn't affect your skill level, Master
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_GET_COMBINE_EXP_GRAND_MASTER = new SM_SYSTEM_MESSAGE(1400743);

	/**
	 * 你：cannot modify equipped items。 / You cannot modify equipped items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHANGE_ITEM_SKIN_CANT_CHANGE_EQUIPED_ITEM_SKIN = new SM_SYSTEM_MESSAGE(
			1400744);

	/**
	 * You 无法 preview 物品 you 无法 equip。 / You cannot preview an item that you cannot equip
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHANGE_ITEM_SKIN_PREVIEW_INVALID_COSMETIC = new SM_SYSTEM_MESSAGE(
			1400745);

	/**
	 * High Elder Roamim's threat level has reset!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Underpass_Nephilim_Raid_ResetAggro = new SM_SYSTEM_MESSAGE(1400746);

	/**
	 * 大长老罗阿米姆暴怒！ / High Elder Roamim is furious!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Underpass_Nephilim_Raid_Rage = new SM_SYSTEM_MESSAGE(1400747);

	/**
	 * High Elder Roamim has summoned players
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Underpass_Nephilim_Raid_Recall = new SM_SYSTEM_MESSAGE(1400748);

	/**
	 * 你：do not have the authority to make this decision。 / You do not have the authority to make this decision
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PARTY_FORCE_NO_RIGHT_TO_DECIDE = new SM_SYSTEM_MESSAGE(1400749);

	/**
	 * You 无法 buy 物品 you 有 registeredyourself。 / You cannot buy an item you have registered yourself
	 */
	public static final SM_SYSTEM_MESSAGE STR_VENDOR_CAN_NOT_BUY_MY_REGISTER_ITEM = new SM_SYSTEM_MESSAGE(1400750);

	/**
	 * Commander Bakarma has appeared at Beritra's Oracle
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDF3_DRAGON = new SM_SYSTEM_MESSAGE(1400751);

	/**
	 * 背包中有强大物品。使用塔洛克果实可变为强力形态。 / An object of great power waits in your cube. Transform into a mighty being with Taloc's Fruit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_KASPAFRUIT_INFO = new SM_SYSTEM_MESSAGE(1400752);

	/**
	 * 背包中有强大物品。使用塔洛克之泪可发动强力空中攻击。 / An object of great power waits in your cube. Launch a powerful aerial attack with Taloc's Tears
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_KASPATEAR_INFO = new SM_SYSTEM_MESSAGE(1400753);

	/**
	 * 希希尔尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Shishir's carcass. Obtain it, then register it in the skill window
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SHISHIR_INFO = new SM_SYSTEM_MESSAGE(1400754);

	/**
	 * 盖尔玛尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Gellmar's carcass. Obtain it, then register it in the skill window
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GELMAR_INFO = new SM_SYSTEM_MESSAGE(1400755);

	/**
	 * 奈斯尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Neith's carcass. Obtain it, then register it in the skill window
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RAGOS_INFO = new SM_SYSTEM_MESSAGE(1400756);

	/**
	 * 你：must kill Afrane, Saraswati, Lakshmi, and Nimbarka to make Commander Bakarma appear。 / You must kill Afrane, Saraswati, Lakshmi, and Nimbarka to make Commander Bakarma appear
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDF3_DRAGON_1 = new SM_SYSTEM_MESSAGE(1400757);

	/**
	 * 你：must kill 3 more Adjutants to make Commander Bakarma appear。 / You must kill 3 more Adjutants to make Commander Bakarma appear
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDF3_DRAGON_2 = new SM_SYSTEM_MESSAGE(1400758);

	/**
	 * 你：must kill 2 more Adjutants to make Commander Bakarma appear。 / You must kill 2 more Adjutants to make Commander Bakarma appear
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDF3_DRAGON_3 = new SM_SYSTEM_MESSAGE(1400759);

	/**
	 * 你：must kill 1 more Adjutant to make Commander Bakarma appear。 / You must kill 1 more Adjutant to make Commander Bakarma appear
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BOSS_SPAWN_IDDF3_DRAGON_4 = new SM_SYSTEM_MESSAGE(1400760);

	 /**
	  * %0 sealed by the heat of high summer has been accumulated。
	  * The %0 sealed by the heat of high summer has been accumulated
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_HCOIN_01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400761, value0);
	}

	/**
	 * 你现在a Mentor。 / You are now a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_START = new SM_SYSTEM_MESSAGE(1400762);

	/**
	 * @param value0 %0 现为导师。 / %0 is now a Mentor
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MENTOR_START_PARTYMSG(String value0) {
		return new SM_SYSTEM_MESSAGE(1400763, value0);
	}

	/**
	 * 你no longer a Mentor。 / You are no longer a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_END = new SM_SYSTEM_MESSAGE(1400764);

	/**
	 * @param value0 %0 不再处于 a Mentor 状态。 / %0 is no longer a Mentor
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MENTOR_END_PARTYMSG(String value0) {
		return new SM_SYSTEM_MESSAGE(1400765, value0);
	}

	/**
	 * @param worldname0 你：cannot enter %WORLDNAME0 with a Mentor in your group。 / You cannot enter %WORLDNAME0 with a Mentor in your group
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MENTOR_CANT_ENTER(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400766, worldname0);
	}

	/**
	 * @param worldname0 你：were forced to leave %WORLDNAME0 because you stopped Mentoring。 / You were forced to leave %WORLDNAME0 because you stopped Mentoring
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_MENTOR_END_BANISH(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400767, worldname0);
	}

	 /**
	  * %0 was forced to leave %WORLDNAME1 because he or she stopped Mentoring。
	  * %0 was forced to leave %WORLDNAME1 because he or she stopped Mentoring
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_MENTOR_END_BANISH_PARTYMSG(String value0, String worldname1) {
		return new SM_SYSTEM_MESSAGE(1400768, value0, worldname1);
	}

	/**
	 * @param worldname0 你：cannot become a Mentor in %WORLDNAME0。 / You cannot become a Mentor in %WORLDNAME0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_BE_MENTOR(String worldname0) {
		return new SM_SYSTEM_MESSAGE(1400769, worldname0);
	}

	/**
	 * @param value0 你：have left %0 because of the level limit。 / You have left %0 because of the level limit
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_LEAVE_BY_LEVEL_LIMIT(int value0) {
		return new SM_SYSTEM_MESSAGE(1400770, new DescriptionId(value0));
	}

	/**
	 * @param value0 None 的 yourgroup 成员 meetlevelrequirement 用于 %0。 / None of your group members meet the level requirement for %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_KILL_COUNT_BY_WRONG_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400771, value0);
	}

	/**
	 * @param value0 None 的 yourgroup 成员 meetlevelrequirement 用于 %0。 / None of your group members meet the level requirement for %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_DROP_ITEM_BY_WRONG_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400772, value0);
	}

	/**
	 * @param value1 你：cannot use %1 while Mentoring。 / You cannot use %1 while Mentoring
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_USE_ITEM_BY_NOT_MENTOR(String value1) {
		return new SM_SYSTEM_MESSAGE(1400773, value1);
	}

	/**
	 * @param value1 你：cannot use %1 while Mentoring。 / You cannot use %1 while Mentoring
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DONT_USE_ITEM_BY_NOT_MENTEE(String value1) {
		return new SM_SYSTEM_MESSAGE(1400774, value1);
	}

	/**
	 * 受污的伊尼娜现已开放。 / The Tainted Inina is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_01 = new SM_SYSTEM_MESSAGE(1400775);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels10 - 19。 / You cannot open it because there are no users of levels 10 - 19
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_02 = new SM_SYSTEM_MESSAGE(1400776);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_03 = new SM_SYSTEM_MESSAGE(1400777);

	/**
	 * 伪装成龙奴卫兵与凯丹大祭司对话可获得贡品箱。 / Speak to the Kaidan Head Priest while disguised as a Draconute Guard to receive a Tribute Chest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_04 = new SM_SYSTEM_MESSAGE(1400778);

	/**
	 * 贡品箱仅伪装成龙族使者者可开启。 / The Tribute Chest can only be opened by someone disguised as a Drakan Envoy
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_05 = new SM_SYSTEM_MESSAGE(1400779);

	/**
	 * 勒法利斯特革命军藏身处现已开放。 / The Lepharist Revolutionary hideout is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_06 = new SM_SYSTEM_MESSAGE(1400780);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels20 - 29。 / You cannot open it because there are no users of levels 20 - 29
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_07 = new SM_SYSTEM_MESSAGE(1400781);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_08 = new SM_SYSTEM_MESSAGE(1400782);

	/**
	 * 魔族藏身处现已开放。 / The Asmodian hideout is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_09 = new SM_SYSTEM_MESSAGE(1400783);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels30 - 39。 / You cannot open it because there are no users of levels 30 - 39
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_10 = new SM_SYSTEM_MESSAGE(1400784);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_11 = new SM_SYSTEM_MESSAGE(1400785);

	/**
	 * 守护宝珠现已开放。 / The Ward Orb is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_01 = new SM_SYSTEM_MESSAGE(1400786);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels10 - 19。 / You cannot open it because there are no users of levels 10 - 19
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_02 = new SM_SYSTEM_MESSAGE(1400787);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_03 = new SM_SYSTEM_MESSAGE(1400788);

	/**
	 * 伪装成龙奴卫兵与猫人（Mau）大祭司对话可获得贡品。 / Speak to the Mau High Priest while disguised as a Draconute Guard to receive a Tribute
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_04 = new SM_SYSTEM_MESSAGE(1400789);

	/**
	 * 贡品箱仅伪装成高级龙族使者者可开启。 / The Tribute Chest can only be opened by someone disguised as a High Rank Drakan Envoy
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_05 = new SM_SYSTEM_MESSAGE(1400790);

	/**
	 * 守护球体现已开放。 / The Ward Globe is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_06 = new SM_SYSTEM_MESSAGE(1400791);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels20 - 29。 / You cannot open it because there are no users of levels 20 - 29
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_07 = new SM_SYSTEM_MESSAGE(1400792);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_08 = new SM_SYSTEM_MESSAGE(1400793);

	/**
	 * 莫尔海姆天文台辅助装置现已开放。 / The Morheim Observatory Auxiliary Device is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_09 = new SM_SYSTEM_MESSAGE(1400794);

	/**
	 * You 无法 open 其 becausethere 为 nousers 的 levels30 - 39。 / You cannot open it because there are no users of levels 30 - 39
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_10 = new SM_SYSTEM_MESSAGE(1400795);

	/**
	 * 没有导师无法打开。 / You cannot open that without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_11 = new SM_SYSTEM_MESSAGE(1400796);

	 /**
	  * %0 has been sealed。
	  * %0 has been sealed
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_START_DONE(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400797, new DescriptionId(nameId));
	}

	/**
	 * Canceled sealing %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_START_CANCEL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400798, new DescriptionId(nameId));
	}

	/**
	 * 确定要封印吗？ / Are you sure you want to seal it?
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_CONFIRM_START = new SM_SYSTEM_MESSAGE(1400799);

	/**
	 * 确定要解除封印吗？ / Are you sure you want to unseal it?
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_CONFIRM_UNSEAL = new SM_SYSTEM_MESSAGE(1400800);

	/**
	 * 封印将立即移除。 / The seal will be removed immediately
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_CONFIRM_UNSEALINSTANT = new SM_SYSTEM_MESSAGE(1400801);

	/**
	 * Sealed Item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS = new SM_SYSTEM_MESSAGE(1400802);

	/**
	 * 解除封印等待中 / Unseal Pending
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_UNSEALWAIT = new SM_SYSTEM_MESSAGE(1400803);

	/**
	 * @param value0 完全解除封印需要 %0 天。 / It takes %0 days to remove the seal completely
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_DURATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400804, value0);
	}

	/**
	 * @param value0 You 可有仅 %0 物品 pendingunsealing 在 time。 / You can have only %0 items pending unsealing at a time
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_UNSEALMAX(String value0) {
		return new SM_SYSTEM_MESSAGE(1400805, value0);
	}

	/**
	 * @param nameId %0 已进入解除封印等待，需 7 天。 / %0 is now pending unsealing. This will take 7 days
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_UNSEALWAIT_START(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400806, new DescriptionId(nameId));
	}

	/**
	 * %0 已进入解除封印等待，需 %DURATIONDAY1 天。 / %0 is now pending unsealing. This will take %DURATIONDAY1 days
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_UNSEALPROGRESS(String value0, String durationday1) {
		return new SM_SYSTEM_MESSAGE(1400807, value0, durationday1);
	}

	/**
	 * @param nameId %0 已解除封印。 / %0 is unsealed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_UNSEALDONE(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400808, new DescriptionId(nameId));
	}

	/**
	 * You 无法 trade , enhance , destroy , sell , extract ,或 soulbindsealed 物品,并其 takes%0days 到 unsealthemcompletely。 / You cannot trade, enhance, destroy, sell, extract, or soulbind sealed items, and it takes %0 days to unseal them completely
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_START(String value0) {
		return new SM_SYSTEM_MESSAGE(1400809, value0);
	}

	/**
	 * You 无法 seal 物品为已经 sealed。 / You cannot seal an item that is already sealed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_RESEAL = new SM_SYSTEM_MESSAGE(1400810);

	/**
	 * 你：cannot trade sealed items。 / You cannot trade sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_TRADE = new SM_SYSTEM_MESSAGE(1400811);

	/**
	 * You 无法 sellsealed 物品在 private 商店。 / You cannot sell sealed items in a private store
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_SHOP = new SM_SYSTEM_MESSAGE(1400812);

	/**
	 * You 无法 sellsealed 物品在 Broker。 / You cannot sell sealed items at the Broker
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_AUCTION = new SM_SYSTEM_MESSAGE(1400813);

	/**
	 * 你：cannot mail sealed items。 / You cannot mail sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_MAIL = new SM_SYSTEM_MESSAGE(1400814);

	/**
	 * 你：cannot store sealed items in the account warehouse。 / You cannot store sealed items in the account warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_ACCOUNT = new SM_SYSTEM_MESSAGE(1400815);

	/**
	 * 你：cannot store sealed items in the legion warehouse。 / You cannot store sealed items in the legion warehouse
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_GUILD = new SM_SYSTEM_MESSAGE(1400816);

	/**
	 * You 无法 enhancesealed 物品带 enchantmentstones。 / You cannot enhance sealed items with enchantment stones
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_UP = new SM_SYSTEM_MESSAGE(1400817);

	/**
	 * You 无法 enhancesealed 物品带 manastones。 / You cannot enhance sealed items with manastones
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_UP_MSTONE = new SM_SYSTEM_MESSAGE(1400818);

	/**
	 * You 无法 enhancesealed 物品带 godstones。 / You cannot enhance sealed items with godstones
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_UP_GSTONE = new SM_SYSTEM_MESSAGE(1400819);

	/**
	 * You 无法 modifyappearance 的 sealed 物品。 / You cannot modify the appearance of sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_LOOKCHANGE = new SM_SYSTEM_MESSAGE(1400820);

	/**
	 * 你：cannot combine sealed items。 / You cannot combine sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_MERGE = new SM_SYSTEM_MESSAGE(1400821);

	/**
	 * 你：cannot destroy sealed items。 / You cannot destroy sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_DESTROY = new SM_SYSTEM_MESSAGE(1400822);

	/**
	 * 你：cannot sell sealed items。 / You cannot sell sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_SELL = new SM_SYSTEM_MESSAGE(1400823);

	/**
	 * 你：cannot extract sealed items。 / You cannot extract sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_EXTRACT = new SM_SYSTEM_MESSAGE(1400824);

	/**
	 * You 无法移除 manastones 从 sealed 物品。 / You cannot remove manastones from sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_REMOVE = new SM_SYSTEM_MESSAGE(1400825);

	/**
	 * 你：must unseal your items to complete that quest。 / You must unseal your items to complete that quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_QUESTFINISH = new SM_SYSTEM_MESSAGE(1400826);

	/**
	 * 你：cannot stack sealed items。 / You cannot stack sealed items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_OVERLAP = new SM_SYSTEM_MESSAGE(1400827);

	/**
	 * @param nameId You 无法 use%0whileyou 为 waiting 用于 morethan3 物品到为 unsealed。 / You cannot use %0 while you are waiting for more than 3 items to be unsealed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_MAXWAIT(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400828, new DescriptionId(nameId));
	}

	/**
	 * Canceled unsealing %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_UNSEALCANCEL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400829, new DescriptionId(nameId));
	}

	/**
	 * @param nameId You 无法 use%0 在物品为 pendingunsealing。 / You cannot use %0 on an item that is pending unsealing
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_ALREADYUNSEAL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400830, new DescriptionId(nameId));
	}

	 /**
	  * %0 is not a sealed item。
	  * %0 is not a sealed item
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_NOTSEALED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400831, new DescriptionId(nameId));
	}

	/**
	 * Cancel the pending unsealing of another item, and then try again
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_TRYAGAIN = new SM_SYSTEM_MESSAGE(1400832);

	/**
	 * @param value0 已获得 %0 张高级破封卷轴。60 分钟后或下线将消失。 / %0 Premium Seal Breaking Scrolls have arrived. They will vanish in 60 minutes or if you log out
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_SCROLLGET(String value0) {
		return new SM_SYSTEM_MESSAGE(1400833, value0);
	}

	 /**
	  * Seal Obliterator has vanished。
	  * The Seal Obliterator has vanished
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_SCROLLDESTROY = new SM_SYSTEM_MESSAGE(1400834);

	/**
	 * 你：cannot use sealed consumable items。 / You cannot use sealed consumable items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_CANTUSE = new SM_SYSTEM_MESSAGE(1400835);

	/**
	 * Promotion Item Test: You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_ALL_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400836, value0);
	}

	/**
	 * 小队长为导师时不可使用队长拾取。 / Group Leader Loot is not available when the group leader is a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_SELECT_LEADER_LOOTING_BY_MENTOR = new SM_SYSTEM_MESSAGE(1400837);

	/**
	 * You 无法为 Mentorbecause 全部 otherGroup 成员为 Mentors。 / You cannot be a Mentor because all the other Group Members are Mentors
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_BE_MENTOR_BY_LAST_MENTEE = new SM_SYSTEM_MESSAGE(1400838);

	/**
	 * 小队长为导师时不可用。 / Not available when the Group Leader is a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEADER_LOOTING_IS_UNAVAILABLE = new SM_SYSTEM_MESSAGE(1400839);

	/**
	 * You 有 stoppedMentoringbecauselowestlevelgroup 成员 must 为在 least10levelslowerthanyou。 / You have stopped Mentoring because the lowest level group member must be at least 10 levels lower than you
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_BE_MENTOR_BY_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(1400840);

	/**
	 * 你：have stopped Mentoring because no group members need your help。 / You have stopped Mentoring because no group members need your help
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_PARTY_END_BY_LEAVE_ALL_MENTEE = new SM_SYSTEM_MESSAGE(1400841);

	 /**
	  * Mentor group has been converted to an alliance。
	  * The Mentor group has been converted to an alliance
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_PARTY_END_BY_CONVERT_BY_FORCE = new SM_SYSTEM_MESSAGE(1400842);

	/**
	 * You 无法 joingroup/alliancebecauseyourcharacter 名称为 invalid。 / You cannot join the group/alliance because your character name is invalid
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_JOIN_PARTY_FORCE_NOT_NORMAL_CHAR_NAME = new SM_SYSTEM_MESSAGE(
			1400843);

	/**
	 * @param nameId 你：cannot seal %0。 / You cannot seal %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_START_FAIL1(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400844, new DescriptionId(nameId));
	}

	/**
	 * @param nameId 你：cannot seal %0 while it is equipped。 / You cannot seal %0 while it is equipped
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_START_FAIL2(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400845, new DescriptionId(nameId));
	}

	/**
	 * 你：cannot seal a quest item。 / You cannot seal a quest item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_START_FAIL3 = new SM_SYSTEM_MESSAGE(1400846);

	/**
	 * @param nameId %0 无法解除组合。 / %0 cannot break the combination
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_DICOMPOSITION(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400847, new DescriptionId(nameId));
	}

	/**
	 * You 可仅 Mentorgroup 成员在 least10levelsbelowyou。 / You can only Mentor a group member at least 10 levels below you
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_CANT_START_WITHOUT_MENTOR_TARGET = new SM_SYSTEM_MESSAGE(
			1400848);

	/**
	 * You 可仅为 Mentor 当 you ' re 在 group。 / You can only be a Mentor when you're in a group
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MENTOR_CANT_START_WHEN_NOT_IN_PARTY = new SM_SYSTEM_MESSAGE(1400849);

	/**
	 * Youbelong 到 MentorGroup.Use[/RecruitMentor[yourtexthere]]command 到 post 消息到 <RecruitMentorGroup>。 / You belong to a Mentor Group. Use the [/Recruit Mentor [your text here]] command to post a message to <Recruit Mentor Group>
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_REGISTER_NORMAL_PARTY_IN_MENTOR_PARTY = new SM_SYSTEM_MESSAGE(
			1400850);

	/**
	 * 你已在联盟中，无法发布<招募小队>。请用 /RecruitAllianceMember 与<招募联盟>窗口。 / Because you belong to an Alliance, you cannot post a <Recruit Group> message. Use /RecruitAllianceMember and the <Recruit Alliance> window instead
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_REGISTER_NORMAL_PARTY_IN_FORCE = new SM_SYSTEM_MESSAGE(1400851);

	/**
	 * 因你已在小队或联盟中，寻找小队窗口的发布已取消。 / Your posting to the Find Group window was canceled because you already belong to a Group or Alliance
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_REGISTER_APPLY_IN_PARTY_OR_FORCE = new SM_SYSTEM_MESSAGE(
			1400852);

	/**
	 * You 无法 combine 物品为 temporarilytradable。 / You cannot combine items that are temporarily tradable
	 */
	public static final SM_SYSTEM_MESSAGE STR_COMPOUND_ERROR_TEMPORARY_EXCHANGE_ITEM = new SM_SYSTEM_MESSAGE(1400853);

	/**
	 * 你：can receive the daily quest。 / You can receive the daily quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_QUEST_LIMIT_RESET_DAILY = new SM_SYSTEM_MESSAGE(1400854);

	/**
	 * @param value0 你：can receive the daily quest again at %0 in the morning。 / You can receive the daily quest again at %0 in the morning
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_QUEST_LIMIT_START_DAILY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400855, value0);
	}

	/**
	 * 你：can receive the weekly quest。 / You can receive the weekly quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_QUEST_LIMIT_RESET_WEEK = new SM_SYSTEM_MESSAGE(1400856);

	/**
	 * 你：can receive the weekly quest again at %1 in the morning on %0。 / You can receive the weekly quest again at %1 in the morning on %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_QUEST_LIMIT_START_WEEK(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400857, value1, value0);
	}

	/**
	 * 你：cannot soul bind a sealed item。 / You cannot soul bind a sealed item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_CANT_SOUL_BIND = new SM_SYSTEM_MESSAGE(1400858);

	/**
	 * @param value0 %0 无法解除组合。 / %0 cannot break the combination
	 */
	public static SM_SYSTEM_MESSAGE STR_DECOMPOUND_ERROR_NOT_DECOMPOUNDABLE_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400859, value0);
	}

	/**
	 * @param value0 你：cannot continue the quest with %0 equipped。 / You cannot continue the quest with %0 equipped
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_QUEST_ERROR_UNEQUIP_QUEST_ITEM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400860, value0);
	}

	/**
	 * That doesn't work without a user of level 35-45
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_12 = new SM_SYSTEM_MESSAGE(1400861);

	/**
	 * 没有导师无法使用。 / That doesn't work without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Li_13 = new SM_SYSTEM_MESSAGE(1400862);

	/**
	 * That doesn't work without a user of level 25 - 35
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_12 = new SM_SYSTEM_MESSAGE(1400863);

	/**
	 * 没有导师无法使用。 / That doesn't work without a Mentor
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Da_13 = new SM_SYSTEM_MESSAGE(1400864);

	/**
	 * @param value0 你：check how %0 feels。 / You check how %0 feels
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_CARE_01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400865, value0);
	}

	/**
	 * @param value0 %0 感觉好了一些。 / %0 feels a little better
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_UP_01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400866, value0);
	}

	/**
	 * @param value0 %0 感觉好多了。 / %0 feels much better
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_UP_02(String value0) {
		return new SM_SYSTEM_MESSAGE(1400867, value0);
	}

	/**
	 * @param value0 %0 感觉好很多。 / %0 feels a lot better
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_UP_02_01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400868, value0);
	}

	/**
	 * @param value0 你：ask %0 to search the area。 / You ask %0 to search the area
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_SEARCH_01(String value0) {
		return new SM_SYSTEM_MESSAGE(1400869, value0);
	}

	/**
	 * 你：cannot issue a command when your cube is full。 / You cannot issue a command when your cube is full
	 */
	public static final SM_SYSTEM_MESSAGE STR_PET_CONDITION_REWARD_FULL_INVEN = new SM_SYSTEM_MESSAGE(1400870);

	/**
	 * %0 为你挖出了埋藏的 %1。 / %0 unearthed a buried %1 for you
	 */
	public static SM_SYSTEM_MESSAGE STR_PET_CONDITION_REWARD_GET(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400871, value0, value1);
	}

	/**
	 * 你：cannot glide while you are transformed。 / You cannot glide while you are transformed
	 */
	public static final SM_SYSTEM_MESSAGE STR_GLIDE_CANNOT_GLIDE_POLYMORPH_STATUS = new SM_SYSTEM_MESSAGE(1400872);

	/**
	 * 你：cannot fly while you are transformed。 / You cannot fly while you are transformed
	 */
	public static final SM_SYSTEM_MESSAGE STR_FLY_CANNOT_FLY_POLYMORPH_STATUS = new SM_SYSTEM_MESSAGE(1400873);

	/**
	 * Pets cannot use this item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_CANNOT_USE = new SM_SYSTEM_MESSAGE(1400874);

	/**
	 * You 无法列表 unusable 物品。 / You cannot list an unusable item
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_MESSAGE02 = new SM_SYSTEM_MESSAGE(1400875);

	/**
	 * 宠物将代你自动拾取，需确认的物品（如掷骰）除外。 / The pet will now automatically loot items on your behalf, except for items that require confirmation (such as Dice Roll items.)
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_MESSAGE01 = new SM_SYSTEM_MESSAGE(1400876);

	/**
	 * 宠物无法拾取可与其他队员共享的物品。 / The pet can't pick up items that can be shared with other group members
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_MESSAGE02 = new SM_SYSTEM_MESSAGE(1400877);

	/**
	 * You 无法 usePetLootfunction 当 group 为 usingFree -用于-全部 lootsetting。 / You cannot use the Pet Loot function when the group is using the Free-for-All loot setting
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_MESSAGE03 = new SM_SYSTEM_MESSAGE(1400878);

	/**
	 * 宠物自动增益已启用。旧增益到期时宠物会自动使用增益袋物品。 / Pet Auto-Buffing activated. Your pet automatically uses Buff Bag items to buff you as old buffs expire
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_USE_START_MESSAGE = new SM_SYSTEM_MESSAGE(1400879);

	/**
	 * 停止宠物自动增益 / Stop Pet Auto-Buffing
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_USE_STOP_MESSAGE = new SM_SYSTEM_MESSAGE(1400880);

	/**
	 * 停用宠物拾取功能。 / Deactivates the Pet Loot function
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_USE_STOP_MESSAGE = new SM_SYSTEM_MESSAGE(1400881);

	/**
	 * 宠物只能吃食物。 / The pet can only eat food
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_USE_CATEGORY_FOOD = new SM_SYSTEM_MESSAGE(1400882);

	/**
	 * 宠物只能喝饮料。 / The pet can only drink beverages
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_USE_CATEGORY_DRINK = new SM_SYSTEM_MESSAGE(1400883);

	/**
	 * 宠物只能使用卷轴。 / The pet can only use scrolls
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_USE_CATEGORY_SCROLL = new SM_SYSTEM_MESSAGE(1400884);

	/**
	 * 宠物无法拾取需你确认的物品。 / Your pet cannot pick up items that require your confirmation
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_ITEM_REMAIN = new SM_SYSTEM_MESSAGE(1400885);

	/**
	 * 宠物无法拾取须与其他队员共享的物品。 / Your pet cannot pick up items that you must share with other Group Members
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LOOTING_PET_ITEM_REMAIN02 = new SM_SYSTEM_MESSAGE(1400886);

	 /**
	  * %0 has been conditioned to level %1。
	  * %0 has been conditioned to level %1
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_SUCCESS(DescriptionId descriptionId, int level) {
		return new SM_SYSTEM_MESSAGE(1400887, descriptionId, level);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE2_SUCCESS(DescriptionId descriptionId, int level) {
		return new SM_SYSTEM_MESSAGE(1401335, descriptionId, level);
	}

	/**
	 * @param value0 你：cannot condition %0。 / You cannot condition %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_FAIL_NOT_CHARGEABLE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400888, value0);
	}

	/**
	 * %0 已调谐至上限，无法调谐至 %1 级。 / %0 has been conditioned as much as it can be. You cannot condition it to Level %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_FAIL_ALREADY_CHARGED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400889, value0, value1);
	}

	/**
	 * 所有已装备物品均已调谐，无法继续。 / All equipped items are already conditioned. You cannot condition them further
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_ALL_FAIL_ALREADY_CHARGED = new SM_SYSTEM_MESSAGE(1400890);

	 /**
	  * trade has been cancelled。
	  * The trade has been cancelled
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_CANCELED = new SM_SYSTEM_MESSAGE(1400891);

	/**
	 * Successfully conditioned equipped item(s)
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_ALL_COMPLETE = new SM_SYSTEM_MESSAGE(1400892);

	/**
	 * %0 有已 conditioned 到 level%1 ,并物品' sstats 有 changed。 / %0 has been conditioned to level %1, and the item's stats have changed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_LEVEL_DOWN(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400893, value0, value1);
	}

	/**
	 * 所有已装备物品已达调谐上限，无法调谐至 %0 级。 / All equipped items have been conditioned to their maximum level, and cannot be conditioned to level %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_ALL_FAIL_EQUIPED_ALREADY_CHARGED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400894, value0);
	}

	/**
	 * 已装备物品中没有可调谐的。 / None of the equipped items are conditionable
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_ALL_FAIL_NO_CHARGEABLE_EQUIPMENT = new SM_SYSTEM_MESSAGE(
			1400895);

	/**
	 * 宠物自动增益启用时，无法更改增益袋内容。 / While the Pet Auto-Buffing is active, you cannot change the contents of the Buff Bag
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DOPING_PET_MESSAGE03 = new SM_SYSTEM_MESSAGE(1400896);

	/**
	 * 请退出游戏并休息。 / Please log out of the game and take a break
	 */
	public static final SM_SYSTEM_MESSAGE STR_TIRED_REMAIN_PLAYTIME_CHINA_1 = new SM_SYSTEM_MESSAGE(1400897);

	/**
	 * 游戏内收益已降至正常值的 50%。请下线休息。 / Your in-game gains have been reduced to 50% of normal values. Please log out and taking a break
	 */
	public static final SM_SYSTEM_MESSAGE STR_TIRED_REMAIN_PLAYTIME_CHINA_2 = new SM_SYSTEM_MESSAGE(1400898);

	/**
	 * 你：can't acquire any quest while fatigued. Please take a break until your fatigue level decreases, and then resume play。 / You can't acquire any quest while fatigued. Please take a break until your fatigue level decreases, and then resume play
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_ACQUIRE_QUEST_FATIGUE = new SM_SYSTEM_MESSAGE(1400899);

	/**
	 * 疲劳状态下无法继续或完成任务。请下线休息至疲劳降低。 / Quests can't be continued or completed while you are fatigued. Please log out and take a break until your fatigue level decreases
	 */
	public static final SM_SYSTEM_MESSAGE STR_CANT_PROCEED_QUEST_FATIGUE = new SM_SYSTEM_MESSAGE(1400900);

	/**
	 * @param nameId %0 有已 sealed 按 GM.You 无法移除此 seal。 / %0 has been sealed by a GM. You cannot remove this seal
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_STATUS_GMSEAL_UNSEAL_IMPOSSIBLE(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400901, new DescriptionId(nameId));
	}

	 /**
	  * %0 has become the Legion Deputy。
	  * %0 has become the Legion Deputy
	  */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONE_GUILD_SUBMASTER(String value0) {
		return new SM_SYSTEM_MESSAGE(1400902, value0);
	}

	/**
	 * @param value0 %0 有 become 军团成员。 / %0 has become a Legion Member
	 */
	public static SM_SYSTEM_MESSAGE STR_GUILD_CHANGE_MEMBER_RANK_DONE_GUILD_NEWBIE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400903, value0);
	}

	/**
	 * Youmay 为 unable 到 usecertainskills 或物品在此 area。 / You may be unable to use certain skills or items in this area
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTERED_SKILL_ITEM_RESTRICTED_AREA = new SM_SYSTEM_MESSAGE(1400904);

	/**
	 * 你：cannot use %1 in %0。 / You cannot use %1 in %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_SKILL_ITEM_RESTRICTED_AREA(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400905, value1, value0);
	}

	/**
	 * 你：cannot use %1 until you reach level %0。 / You cannot use %1 until you reach level %0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_TOO_LOW_GUILD_LEVEL(String value1, String value0) {
		return new SM_SYSTEM_MESSAGE(1400906, value1, value0);
	}

	/**
	 * 你：have already learned this motion。 / You have already learned this motion
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CUSTOMANIMATION_ALREADY_HAS_MOTION = new SM_SYSTEM_MESSAGE(1400907);

	/**
	 * @param value0 %0 有已 conditioned.You 无法 condition 其 further。 / %0 has been conditioned. You cannot condition it further
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_FAIL_ALREADY_FULLY_CHARGED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400908, value0);
	}

	/**
	 * 外层防护墙已消失，武器 H 正从休眠中苏醒。 / The Outer Protective Wall is gone, and Weapon H is waking from its dormant state
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_HugenNM_00 = new SM_SYSTEM_MESSAGE(1400909);

	/**
	 * 能量发生器变得不稳定。 / The Energy Generator is becoming unstable
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_HugenNM_01 = new SM_SYSTEM_MESSAGE(1400910);

	/**
	 * 能量发生器已被摧毁，防护盾强度降低。 / The Energy Generator has been destroyed and the power of the Protective Shield has been reduced
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_HugenNM_02 = new SM_SYSTEM_MESSAGE(1400911);

	/**
	 * 能量发生器已被摧毁，防护盾强度降低。 / The Energy Generator has been destroyed and the power of the Protective Shield has been greatly reduced
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_HugenNM_03 = new SM_SYSTEM_MESSAGE(1400912);

	/**
	 * 能量发生器已被摧毁，防护盾强度降低。 / The Energy Generator has been destroyed and the Protective Shield has disappeared
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_HugenNM_04 = new SM_SYSTEM_MESSAGE(1400913);

	/**
	 * Youpet 为 sulky 并可' tfeelbetteruntilyouinteract 带其。 / You pet is sulky and can't feel better until you interact with it
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PET_CONDITION_CARE_END = new SM_SYSTEM_MESSAGE(1400914);

	/**
	 * 你：need the aid of a Mentor to open it。 / You need the aid of a Mentor to open it
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Ask_Mentor = new SM_SYSTEM_MESSAGE(1400915);

	/**
	 * Youneedaid 的 lowerlevelGroup 成员到 open 其。 / You need the aid of a lower level Group Member to open it
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DailyQuest_Ask_Mentee = new SM_SYSTEM_MESSAGE(1400916);

	 /**
	  * %0 motion has expired and can no longer be used。
	  * The %0 motion has expired and can no longer be used
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_CASH_CUSTOMANIMATION_BY_TIMEOUT(String value0) {
		return new SM_SYSTEM_MESSAGE(1400917, value0);
	}

	 /**
	  * Bridge to the Drana Production Lab has been raised。
	  * The Bridge to the Drana Production Lab has been raised
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_01 = new SM_SYSTEM_MESSAGE(1400918);

	/**
	 * 击败所有德拉纳生产实验室区段管理员以打开实验室院子门。 / Defeat all Drana Production Lab Section Managers to open the Laboratory Yard door
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_02 = new SM_SYSTEM_MESSAGE(1400919);

	/**
	 * 实验室院子门现已打开。 / The door to the Laboratory Yard is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_03 = new SM_SYSTEM_MESSAGE(1400920);

	/**
	 * 实验室空调室门现已打开。 / The door to the Laboratory Air Conditioning Room is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_04 = new SM_SYSTEM_MESSAGE(1400921);

	/**
	 * 实验室空调室通风口现已打开。 / The Laboratory Air Conditioning Room Ventilator is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_05 = new SM_SYSTEM_MESSAGE(1400922);

	/**
	 * 德拉纳生产实验室通道现已开放。 / The Drana Production Lab walkway is now open
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_06 = new SM_SYSTEM_MESSAGE(1400923);

	 /**
	  * outer wall of the Bio Lab has collapsed。
	  * The outer wall of the Bio Lab has collapsed
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_07 = new SM_SYSTEM_MESSAGE(1400924);

	/**
	 * 飞空艇武器已出现在背包中。登记到技能窗口即可使用。 / The Airship Weapon has appeared in your cube. Register it to the Skill Window to use it
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Zone3_Morph_01 = new SM_SYSTEM_MESSAGE(1400925);

	/**
	 * 充能器正为你全身注入能量，似乎在增强！ / The Recharger is filling your whole body with energy. It seems to be increasing!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Doping_01 = new SM_SYSTEM_MESSAGE(1400926);

	/**
	 * 舒拉克饮料正在为你补充能量！ / The Shulack Drink is energizing you!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Doping_02 = new SM_SYSTEM_MESSAGE(1400927);

	/**
	 * @param round 第 %0 轮开始！ / Round %0 begins!
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_START_ROUND_IDARENA(int round) {
		return new SM_SYSTEM_MESSAGE(1400928, round);
	}

	/**
	 * @param round 你：have eliminated all enemies in Round %0。 / You have eliminated all enemies in Round %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_COMPLETE_ROUND_IDARENA(int round) {
		return new SM_SYSTEM_MESSAGE(1400929, round);
	}

	/**
	 * @param stage 你：have passed Stage %0!。 / You have passed Stage %0!
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_COMPLETE_STAGE_IDARENA(int stage) {
		return new SM_SYSTEM_MESSAGE(1400930, stage);
	}

	/**
	 * 你：join Stage %0 Round %1!。 / You join Stage %0 Round %1!
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_JOIN_ROUND_IDARENA(int stage, int round) {
		return new SM_SYSTEM_MESSAGE(1400931, stage, round);
	}

	/**
	 * 你：failed the training and have been sent to the Ready Room。 / You failed the training and have been sent to the Ready Room
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_MOVE_BIRTHAREA_ME_IDARENA = new SM_SYSTEM_MESSAGE(1400932);

	 /**
	  * %0 failed the training and has been sent to the Ready Room。
	  * %0 failed the training and has been sent to the Ready Room
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_MOVE_BIRTHAREA_FRIENDLY_IDARENA(String value0) {
		return new SM_SYSTEM_MESSAGE(1400933, value0);
	}

	/**
	 * @param value0 你获得了 %0。 / You have acquired %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_GET_COIN_IDARENA(String value0) {
		return new SM_SYSTEM_MESSAGE(1400934, value0);
	}

	/**
	 * 你：cannot use that because the wind has weakened。 / You cannot use that because the wind has weakened
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WindPathOff = new SM_SYSTEM_MESSAGE(1400935);

	/**
	 * 你：cannot re-enter the Crucible until the cooldown time has expired。 / You cannot re-enter the Crucible until the cooldown time has expired
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_REENTER_INSTANCE_IDARENA = new SM_SYSTEM_MESSAGE(1400936);

	/**
	 * 你：have acquired %num1 %0s。 / You have acquired %num1 %0s
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_GET_COIN_MULTI_IDARENA(int num1, String value0s) {
		return new SM_SYSTEM_MESSAGE(1400937, num1, value0s);
	}

	/**
	 * 你：have %0 kinah left in your daily sell limit, and this transaction would exceed that。 / You have %0 kinah left in your daily sell limit, and this transaction would exceed that
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_SELL_NPC(long value0) {
		return new SM_SYSTEM_MESSAGE(1400938, value0);
	}

	 /**
	  * %0 has crystallized in your cube。
	  * The %0 has crystallized in your cube
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_HCOIN_02(String value0) {
		return new SM_SYSTEM_MESSAGE(1400939, value0);
	}

	 /**
	  * item you bought has arrived。
	  * The item you bought has arrived
	  */
	public static final SM_SYSTEM_MESSAGE STR_POSTMAN_NOTIFY_CASH = new SM_SYSTEM_MESSAGE(1400940);

	/**
	 * 交易对象已达每日个人商店交易上限，无法完成交易。 / Your trading partner has reached the daily Private Store trading limit, so the trade cannot be completed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_PARTNER_SHOP = new SM_SYSTEM_MESSAGE(1400941);

	/**
	 * @param value0 You 有 reacheddailyPrivate 商店 tradinglimit 的 %0 基纳, sotrade 无法为 completed。 / You have reached the daily Private Store trading limit of %0 Kinah, so the trade cannot be completed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_OWN_SHOP(String value0) {
		return new SM_SYSTEM_MESSAGE(1400942, value0);
	}

	/**
	 * 交易对象已达每日交易上限，无法完成交易。 / Your trading partner has reached the daily Trading limit, so the trade cannot be completed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_PARTNER_TRADE = new SM_SYSTEM_MESSAGE(1400943);

	/**
	 * @param value0 You 有 reacheddailyTradinglimit 的 %0 基纳, sotrade 无法为 completed。 / You have reached the daily Trading limit of %0 Kinah, so the trade cannot be completed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_OWN_TRADE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400944, value0);
	}

	/**
	 * @param value0 You 有 reacheddailyBrokerlimit 的 %0 基纳, so 物品无法为 listed。 / You have reached the daily Broker limit of %0 Kinah, so the item cannot be listed
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_SELL_AUCTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400945, value0);
	}

	/**
	 * @param value0 You 有 reacheddailyMailattachmentlimit 的 %0 基纳, so 基纳无法为 sent。 / You have reached the daily Mail attachment limit of %0 Kinah, so the kinah cannot be sent
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_SEND_MAIL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400946, value0);
	}

	/**
	 * @param value0 You 有 reacheddailyMailattachmentlimit 的 %0 基纳, soyou 无法 receive 此基纳。 / You have reached the daily Mail attachment limit of %0 Kinah, so you cannot receive this kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_RECEIVE_MAIL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400947, value0);
	}

	 /**
	  * %0 has declined your invitation to the Mini Fortress Battle。
	  * %0 has declined your invitation to the Mini Fortress Battle
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_REJECT_INVITATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400948, value0);
	}

	 /**
	  * %0 has joined the Mini Fortress Battle。
	  * %0 has joined the Mini Fortress Battle
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_ENTERED_PARTY(String value0) {
		return new SM_SYSTEM_MESSAGE(1400949, value0);
	}

	/**
	 * 交易对象已达每日交易上限，交易已取消。 / Your trading partner has reached the daily Trading limit, so the trade has been canceled
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_PARTNER_TRADE_LIMIT = new SM_SYSTEM_MESSAGE(1400950);

	/**
	 * 你：have reached the daily Trading limit, so the trade cannot be completed。 / You have reached the daily Trading limit, so the trade cannot be completed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_OWN_TRADE_LIMIT = new SM_SYSTEM_MESSAGE(1400951);

	/**
	 * @param value0 你：have invited %0 to join the Mini Game。 / You have invited %0 to join the Mini Game
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_INVITED_HIM(String value0) {
		return new SM_SYSTEM_MESSAGE(1400952, value0);
	}

	/**
	 * @param value0 %0 当前无法接受你的小游戏邀请。 / %0 cannot accept your Mini Game invitation right now
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1400953, value0);
	}

	/**
	 * @param value0 %0 正在参加另一场迷你要塞战。 / %0 is participating in another Mini Fortress Battle
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_ALREADY_MEMBER_OF_OTHER_GAME(String value0) {
		return new SM_SYSTEM_MESSAGE(1400954, value0);
	}

	 /**
	  * power binding the soul of %0 has weakened, and %1 has vanished。
	  * The power binding the soul of %0 has weakened, and %1 has vanished
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_ITEM_CHANGE_TO_PUBLIC(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400955, value0, value1);
	}

	 /**
	  * power binding the soul of %0 with %1 has weakened。
	  * The power binding the soul of %0 with %1 has weakened
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_WILL_DELETE_ITEM_CHANGE_TO_PUBLIC(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400956, value0, value1);
	}

	/**
	 * 未在时限内消灭怪物，宝箱已消失。 / The treasure chest has disappeared because you failed to destroy the monsters within the time limit
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDABRECORE_OOPS_REWARD_IS_GONE = new SM_SYSTEM_MESSAGE(1400957);

	/**
	 * The Dredgion Generator has been destroyed. Its Protector is coming soon!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Zone2_Tower_01 = new SM_SYSTEM_MESSAGE(1400958);

	/**
	 * 找不到补充剂。 / Cannot find the Supplements
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_ENCHANT_ASSISTANT_CANNOT_FIND = new SM_SYSTEM_MESSAGE(1400959);

	/**
	 * 你：cannot use sealed Supplements。 / You cannot use sealed Supplements
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_ENCHANT_ASSISTANT_SEALED = new SM_SYSTEM_MESSAGE(1400960);

	/**
	 * 你：cannot use those Supplements。 / You cannot use those Supplements
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_ENCHANT_ASSISTANT_NO_RIGHT_ITEM = new SM_SYSTEM_MESSAGE(1400961);

	/**
	 * @param player %0 退出训练并离开了天族 / 魔族试炼场。 / %0 dropped out of training and left the Empyrean Crucible
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FRIENDLY_LEAVE_IDARENA(String player) {
		return new SM_SYSTEM_MESSAGE(1400962, player);
	}

	/**
	 * 训练进行中。须留在准备室直至可加入。 / Training is in progress. You must stay in the Ready Room until you can join
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ENTERED_BIRTHAREA_IDARENA = new SM_SYSTEM_MESSAGE(1400963);

	 /**
	  * %0 has reentered the Illusion Stadium。
	  * %0 has reentered the Illusion Stadium
	  */
	public static SM_SYSTEM_MESSAGE STR_MSG_FRIENDLY_MOVE_COMBATAREA_IDARENA(String player) {
		return new SM_SYSTEM_MESSAGE(1400964, player);
	}

	/**
	 * Youdonot 有足够基纳到 condition 物品。 / You do not have enough kinah to condition that item
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_CHARGE_NOT_ENOUGH_GOLD = new SM_SYSTEM_MESSAGE(1400965);

	/**
	 * All fatigue is gone. You have %0 Fatigue Recovery remaining
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_STATE_RECOVERED(String value0) {
		return new SM_SYSTEM_MESSAGE(1400966, value0);
	}

	/**
	 * You 有 reachedmaximumFatigue ,并 so 可 obtain 仅 limitedXP , AP ,并物品。 / You have reached maximum Fatigue, and so can obtain only limited XP, AP, and items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_STATE_INFO_STATE_CHANGE = new SM_SYSTEM_MESSAGE(1400967);

	/**
	 * 你非常疲劳，因此仅能获得有限的经验、AP 与物品。 / You are very Fatigued, and so can obtain only limited XP, AP, and items
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_STATE_INFO = new SM_SYSTEM_MESSAGE(1400968);

	/**
	 * 你：feel refreshed, and your Fatigue is gone。 / You feel refreshed, and your Fatigue is gone
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_FATIGUE_RESET = new SM_SYSTEM_MESSAGE(1400969);

	/**
	 * @param value0 你：cannot extract items while %0。 / You cannot extract items while %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_CHARGE_INVALID_STANCE(String value0) {
		return new SM_SYSTEM_MESSAGE(1400970, value0);
	}

	/**
	 * %0 退出迷你要塞战，%1 成为队长。 / %0 quit the Mini Fortress Battle, and %1 became the leader
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_TOWER_DEFENCE_HOST_MOVED(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400971, value0, value1);
	}

	/**
	 * 已拾取！ / Looted!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PET_LOOTING_DIALOG = new SM_SYSTEM_MESSAGE(1400972);

	/**
	 * 为主人施加增益！ / Buff the Master!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_PET_DOPING_DIALOG = new SM_SYSTEM_MESSAGE(1400973);

	/**
	 * %0 成功制作了 %1。 / %0 succeeded in crafting %1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_COMBINE_BROADCAST_COMBINE_SUCCESS(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1400974, value0, value1);
	}

	 /**
	  * Worthiness Ticket Box has appeared in the Illusion Stadium。
	  * A Worthiness Ticket Box has appeared in the Illusion Stadium
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S1_ResurBox1_01 = new SM_SYSTEM_MESSAGE(1400975);

	 /**
	  * Worthiness Ticket Box has appeared in the Ready Room。
	  * A Worthiness Ticket Box has appeared in the Ready Room
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S3_ResurBox1_01 = new SM_SYSTEM_MESSAGE(1400976);

	 /**
	  * Worthiness Ticket Box has appeared in the Ready Room。
	  * A Worthiness Ticket Box has appeared in the Ready Room
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S6_ResurBox1_01 = new SM_SYSTEM_MESSAGE(1400977);

	/**
	 * 你：can earn an additional reward if you catch the Saam King。 / You can earn an additional reward if you catch the Saam King
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S2_SAAM_CTRL_01 = new SM_SYSTEM_MESSAGE(1400978);

	/**
	 * 萨阿姆王将在 30 秒后消失！ / King Saam will disappear in 30 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S2_Saam1_01 = new SM_SYSTEM_MESSAGE(1400979);

	/**
	 * 萨阿姆王将在 10 秒后消失！ / King Saam will disappear in 10 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S2_Saam1_02 = new SM_SYSTEM_MESSAGE(1400980);

	/**
	 * 萨阿姆王将在 5 秒后消失！ / King Saam will disappear in 5 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S2_Saam1_03 = new SM_SYSTEM_MESSAGE(1400981);

	/**
	 * 幼龙即将出现！ / The Drakies will appear soon!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S4_Draky_CTRL_01 = new SM_SYSTEM_MESSAGE(1400982);

	/**
	 * The Ornate Treasure Chest has appeared in the Illusion Stadium!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S7_BookBox_01 = new SM_SYSTEM_MESSAGE(1400983);

	/**
	 * Lightning Drakie has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S9_DuskDraky_55_Ah_01 = new SM_SYSTEM_MESSAGE(1400984);

	/**
	 * @param value0 你：must be under level %0 to join。 / You must be under level %0 to join
	 */
	public static SM_SYSTEM_MESSAGE STR_FACTION_JOIN_ERROR_MAX_LEVEL(String value0) {
		return new SM_SYSTEM_MESSAGE(1400985, value0);
	}

	/**
	 * Administrator Arminos has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S3_Elemeltal_CTRL_01 = new SM_SYSTEM_MESSAGE(1400986);

	/**
	 * Administrator Arminos has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S4_Draky_CTRL_00 = new SM_SYSTEM_MESSAGE(1400987);

	/**
	 * 3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S4_Draky_CTRL_02 = new SM_SYSTEM_MESSAGE(1400988);

	/**
	 * 2
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S4_Draky_CTRL_03 = new SM_SYSTEM_MESSAGE(1400989);

	/**
	 * 1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S4_Draky_CTRL_04 = new SM_SYSTEM_MESSAGE(1400990);

	/**
	 * Administrator Arminos has appeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S6_Ghost_55_Ah_01 = new SM_SYSTEM_MESSAGE(1400991);

	/**
	 * Lightning Drakie has disappeared!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_S9_DuskDraky_55_Ah_02 = new SM_SYSTEM_MESSAGE(1400992);

	/**
	 * 你：must have a Mentor with you in order to complete this quest。 / You must have a Mentor with you in order to complete this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_KILL_COUNT_WITHOUT_MENTOR = new SM_SYSTEM_MESSAGE(1400993);

	/**
	 * 你：must be Mentoring someone in order to complete this quest。 / You must be Mentoring someone in order to complete this quest
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_KILL_COUNT_WITHOUT_MENTEE = new SM_SYSTEM_MESSAGE(1400994);

	/**
	 * Cannot find the location for the selected quest step
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NEW_MAP_QUEST_CANT_FIND_NPC = new SM_SYSTEM_MESSAGE(1400995);

	 /**
	  * Surkana Supplier has overloaded。
	  * The Surkana Supplier has overloaded
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_08 = new SM_SYSTEM_MESSAGE(1400996);

	 /**
	  * Surkana Steam Jet has generated an updraft。
	  * The Surkana Steam Jet has generated an updraft
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_09 = new SM_SYSTEM_MESSAGE(1400997);

	/**
	 * 管理总监苏拉玛使用崩塌大地。 / Management Director Surama uses Collapsing Earth
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_10 = new SM_SYSTEM_MESSAGE(1400998);

	 /**
	  * large number of Balaur Troopers descend from the Dredgion。
	  * A large number of Balaur Troopers descend from the Dredgion
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_01 = new SM_SYSTEM_MESSAGE(1400999);

	/**
	 * 卡玛拉爆炸。 / Kamara explodes
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_02 = new SM_SYSTEM_MESSAGE(1401000);

	/**
	 * 诺里斯双眼变红。 / Norris's eyes turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_03 = new SM_SYSTEM_MESSAGE(1401001);

	/**
	 * 康西尔德王双眼变红。 / The eyes of King Consierd turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_04 = new SM_SYSTEM_MESSAGE(1401002);

	/**
	 * 恐怖的塔昆双眼变红。 / The eyes of Takun the Terrible turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_05 = new SM_SYSTEM_MESSAGE(1401003);

	/**
	 * 戈吉拉双眼变红。 / The eyes of Gojira turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_06 = new SM_SYSTEM_MESSAGE(1401004);

	/**
	 * 安德烈双眼变红。 / The eyes of Andre turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_07 = new SM_SYSTEM_MESSAGE(1401005);

	/**
	 * 卡玛拉双眼变红。 / The eyes of Kamara turn red
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_08 = new SM_SYSTEM_MESSAGE(1401006);

	/**
	 * Unlimited Battle Temporary System Message
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_09 = new SM_SYSTEM_MESSAGE(1401007);

	/**
	 * Unlimited Battle Temporary System Message
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_10 = new SM_SYSTEM_MESSAGE(1401008);

	 /**
	  * Worthiness Ticket has appeared in your cube。
	  * A Worthiness Ticket has appeared in your cube
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ResurBox = new SM_SYSTEM_MESSAGE(1401009);

	/**
	 * 精灵将在 30 秒后消失！ / Spirits will disappear in 30 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S3_Bonus_01 = new SM_SYSTEM_MESSAGE(1401010);

	/**
	 * 精灵将在 10 秒后消失！ / Spirits will disappear in 10 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S3_Bonus_02 = new SM_SYSTEM_MESSAGE(1401011);

	/**
	 * 精灵将在 5 秒后消失！ / Spirits will disappear in 5 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S3_Bonus_03 = new SM_SYSTEM_MESSAGE(1401012);

	/**
	 * 幼龙将在 30 秒后消失！ / Drakies will disappear in 30 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S4_Bonus_01 = new SM_SYSTEM_MESSAGE(1401013);

	/**
	 * 幼龙将在 10 秒后消失！ / Drakies will disappear in 10 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S4_Bonus_02 = new SM_SYSTEM_MESSAGE(1401014);

	/**
	 * 幼龙将在 5 秒后消失！ / Drakies will disappear in 5 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S4_Bonus_03 = new SM_SYSTEM_MESSAGE(1401015);

	/**
	 * 管理员阿尔米诺斯将在 30 秒后消失！ / Administrator Arminos will disappear in 30 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S6_Bonus_01 = new SM_SYSTEM_MESSAGE(1401016);

	/**
	 * 管理员阿尔米诺斯将在 10 秒后消失！ / Administrator Arminos will disappear in 10 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S6_Bonus_02 = new SM_SYSTEM_MESSAGE(1401017);

	/**
	 * 管理员阿尔米诺斯将在 5 秒后消失！ / Administrator Arminos will disappear in 5 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S6_Bonus_03 = new SM_SYSTEM_MESSAGE(1401018);

	/**
	 * 闪电幼龙将在 30 秒后消失！ / Lightning Drakie will disappear in 30 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S9_Bonus_01 = new SM_SYSTEM_MESSAGE(1401019);

	/**
	 * 闪电幼龙将在 10 秒后消失！ / Lightning Drakie will disappear in 10 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S9_Bonus_02 = new SM_SYSTEM_MESSAGE(1401020);

	/**
	 * 闪电幼龙将在 5 秒后消失！ / Lightning Drakie will disappear in 5 seconds!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_S9_Bonus_03 = new SM_SYSTEM_MESSAGE(1401021);

	/**
	 * 你已在联盟中，无法发布<招募导师小队>。请用 /RecruitAllianceMember 与<招募联盟>窗口。 / Because you belong to an Alliance, you cannot post a <Recruit Mentor Group> message. Use /RecruitAllianceMember and the <Recruit Alliance> window instead
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_REGISTER_MENTOR_PARTY_IN_FORCE = new SM_SYSTEM_MESSAGE(1401022);

	/**
	 * 你：failed to purchase the item. Please try again later。 / You failed to purchase the item. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CASH_PURCHASE_ERROR_FAILED_RECEIVING_PRODUCT = new SM_SYSTEM_MESSAGE(
			1401025);

	/**
	 * That item is invalid
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CASH_PURCHASE_ERROR_INVALID_PRODUCT = new SM_SYSTEM_MESSAGE(1401026);

	/**
	 * @param value0 你：have acquired the %0 motion。 / You have acquired the %0 motion
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_CASH_CUSTOMIZE_MOTION(String value0) {
		return new SM_SYSTEM_MESSAGE(1401029, value0);
	}

	/**
	 * 某一分配对象已达每日交易上限，无法完成交易。 / One of the Distribution Targets has reached the daily Trading limit, so the trade cannot be completed
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DAY_CANNOT_SHARE_TRADE_LIMIT = new SM_SYSTEM_MESSAGE(1401034);

	/**
	 * Dalia Charlands has vanished
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_11 = new SM_SYSTEM_MESSAGE(1401036);

	 /**
	  * Surkana Supplier has been broken。
	  * The Surkana Supplier has been broken
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF4Re_Drana_12 = new SM_SYSTEM_MESSAGE(1401037);

	/**
	 * System error. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CASH_PURCHASE_ERROR_SYSTEM_ERROR = new SM_SYSTEM_MESSAGE(1401038);

	 /**
	  * 你的背包已满。
	  * 你的背包已满。 / Your cube is full
	  */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CASH_PURCHASE_ERROR_NOT_ENOUGH_SPACE = new SM_SYSTEM_MESSAGE(1401039);

	/**
	 * 你：cannot summon a pet here。 / You cannot summon a pet here
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_SUMMON_MOVING_STATE = new SM_SYSTEM_MESSAGE(1401040);

	/**
	 * 你：cannot fight a duel here。 / You cannot fight a duel here
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DUEL_CANT_IN_THIS_ZONE = new SM_SYSTEM_MESSAGE(1401047);

	/**
	 * 你：have been disconnected from the Bid Withdrawal Server. Please try again later。 / You have been disconnected from the Bid Withdrawal Server. Please try again later
	 */
	public static final SM_SYSTEM_MESSAGE STR_ITEM_BILLING_SERVER_DOWN_SA2 = new SM_SYSTEM_MESSAGE(1401051);

	/**
	 * 你：can not verify Pet Status in the current state。 / You can not verify Pet Status in the current state
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_SHOPOPEN_STATE = new SM_SYSTEM_MESSAGE(1401052);

	/**
	 * 你：have already requested entry into %WORLDNAME0 (Difficulty: %1)。 / You have already requested entry into %WORLDNAME0 (Difficulty: %1)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED_WITH_DIFFICULTY(String worldname0,
			String value1) {
		return new SM_SYSTEM_MESSAGE(1401053, worldname0, value1);
	}

	/**
	 * 你：cannot make more entry requests。 / You cannot make more entry requests
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_INSTANCE_NO_MORE_REGISTER = new SM_SYSTEM_MESSAGE(1401054);

	/**
	 * 你：have aborted entering %0 (Difficulty: %1). You may apply again in 10 seconds。 / You have aborted entering %0 (Difficulty: %1). You may apply again in 10 seconds
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_REGISTER_CANCELED_WTH_DIFFICULTY(String value0, String value1) {
		return new SM_SYSTEM_MESSAGE(1401055, value0, value1);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF(int worldId) {
		return new SM_SYSTEM_MESSAGE(1400640, worldId);
	}

	/**
	 * 新 / NEW
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_NOTICE_BLANK = new SM_SYSTEM_MESSAGE(1401057);

	/**
	 * 招募队员时间已过，无法再招募。 / The time for group member recruitment has expired. You cannot recruit more group members
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_REINFORCE_MEMBER = new SM_SYSTEM_MESSAGE(1401058);

	/**
	 * You 无法 usecommandsInvite 到 Group 或 Invite 到军团 rightnow。 / You cannot use the commands Invite to Group or Invite to Legion right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_INVITE_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1401059);

	/**
	 * 你：cannot use the commands Assign Group Leader and Make Alliance Captain right now。 / You cannot use the commands Assign Group Leader and Make Alliance Captain right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_CHANGE_LEADER_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(
			1401060);

	/**
	 * 你：cannot use the commands Leave Group or Leave Alliance right now。 / You cannot use the commands Leave Group or Leave Alliance right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_QUIT_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1401061);

	/**
	 * 你：cannot use the commands Ban Group or Ban from Alliance right now。 / You cannot use the commands Ban Group or Ban from Alliance right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_BANISH_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1401062);

	/**
	 * You 无法 usecommandsDistributeCash 到 Group 或 DistributeCash 到 Alliancesettingsrightnow。 / You cannot use the commands Distribute Cash to Group or Distribute Cash to Alliance settings right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_LOOT_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1401063);

	/**
	 * You 无法 changegroup 或 alliance 成员 rightnow。 / You cannot change group or alliance members right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_ARRANGE_MEMBER_FORCE_COMMAND = new SM_SYSTEM_MESSAGE(
			1401064);

	/**
	 * 波比正逃离杜卡基厨师。消灭他们并帮助波比到达避难处。 / Poppy is running from the Dukaki Cooks. Eliminate them and help Poppy to reach the refuge
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_START_BROADCAST = new SM_SYSTEM_MESSAGE(1401067);

	/**
	 * 还剩 5 名杜卡基厨师。 / There are 5 Dukaki Cooks remaining
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_5Dead_BROADCAST = new SM_SYSTEM_MESSAGE(1401068);

	/**
	 * 小心！波比生命值很低。 / Careful! Poppy's health is very low
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_AllDead_BROADCAST = new SM_SYSTEM_MESSAGE(1401069);

	/**
	 * Poppy has almost reached the refuge. Just a little bit further!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_HideNear_BROADCAST = new SM_SYSTEM_MESSAGE(1401070);

	/**
	 * Poppy has reached the refuge safely. A successful rescue!。
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_HideSucc_BROADCAST = new SM_SYSTEM_MESSAGE(1401071);

	/**
	 * You 有 eliminated 全部的 DukakiCooks 并 successfullyrescuedPoppy !。 / You have eliminated all of the Dukaki Cooks and successfully rescued Poppy!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_Succ_BROADCAST = new SM_SYSTEM_MESSAGE(1401072);

	/**
	 * 你：already have a pet of this type。 / You already have a pet of this type
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_CANT_USE_ALREADY_HAS_PET = new SM_SYSTEM_MESSAGE(1401073);

	/**
	 * You 已经有 packpet 带此 functionality。 / You already have a pack pet with this functionality
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TOYPET_ALREADY_SAME_WAREHOUSE_PET = new SM_SYSTEM_MESSAGE(1401074);

	/**
	 * 波比被杜卡基厨师抓住……整只烤了！ / Poppy was captured by the Dukaki Cooks... and roasted whole!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_Failed_BROADCAST = new SM_SYSTEM_MESSAGE(1401075);

	/**
	 * 杜卡基厨师攻击并打伤了波比！ / The Dukaki Cooks attacked and wounded Poppy!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_LowHP_BROADCAST = new SM_SYSTEM_MESSAGE(1401082);

	/**
	 * 波比遭到杜卡基厨师攻击。他们打算把波比烤了当晚餐！ / Poppy was attacked by the Dukaki Cooks. They're planning to roast Poppy for dinner!
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDArena_Solo_SB1_LowHP2_BROADCAST = new SM_SYSTEM_MESSAGE(1401083);

	/**
	 * 砸碎肉桶以引诱并消灭饥饿的卡尼夫。 / Smash the Meat Barrel to lure and destroy the Starved Karnifs
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_IDArena_Solo_S4_System1 = new SM_SYSTEM_MESSAGE(1401084);

	/**
	 * 砸碎奥德桶以引诱并消灭干渴的精灵。 / Smash the Aether Barrel to lure and destroy the Thirsty Spirits
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_IDArena_Solo_S4_System2 = new SM_SYSTEM_MESSAGE(1401085);

	/**
	 * 阻止戈姆朱进行无意义的屠杀！ / Stop Gomju from perpetrating a senseless massacre!
	 */
	public static final SM_SYSTEM_MESSAGE STR_CHAT_IDArena_Solo_S4_System5 = new SM_SYSTEM_MESSAGE(1401086);

	/**
	 * 你：cannot use the commands /RecruitGroupMember or /RecruitAllianceMember right now。 / You cannot use the commands /RecruitGroupMember or /RecruitAllianceMember right now
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_CANT_POST_PARTY_COMMAND = new SM_SYSTEM_MESSAGE(1401098);

	/**
	 * This emblem is already registered
	 */
	public static final SM_SYSTEM_MESSAGE STR_GUILD_ALREADY_POSTED_THIS_EMBLEM = new SM_SYSTEM_MESSAGE(1401142);

	/**
	 * 大师无法接取工作订单。 / A Master cannot take Work Orders
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DONT_GET_COMBINETASK_MASTER = new SM_SYSTEM_MESSAGE(1401182);

	/**
	 * 目标无法为 inviteddue 到 dungeonentryrequirements。 / The target cannot be invited due to dungeon entry requirements
	 */
	public static final SM_SYSTEM_MESSAGE STR_PARTY_CANT_ADD_INSTANCE_MAX_MEMBER = new SM_SYSTEM_MESSAGE(1404119);

	/**
	 * UI 区域消息 / UI Zone messages
	 */
	/**
	 * 你：have already learned this title。 / You have already learned this title
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOOLTIP_LEARNED_TITLE = new SM_SYSTEM_MESSAGE(901714);

	/**
	 * @param value0 你：do not have enough %0。 / You do not have enough %0
	 */
	public static SM_SYSTEM_MESSAGE STR_INGAMESHOP_NOT_ENOUGH_CASH(String value0) {
		return new SM_SYSTEM_MESSAGE(901706, value0);
	}

	/**
	 * 你：have already learned this emote。 / You have already learned this emote
	 */
	public static final SM_SYSTEM_MESSAGE STR_TOOLTIP_LEARNED_EMOTION = new SM_SYSTEM_MESSAGE(901713);

	/**
	 * You 可仅 use 此当 you 有 PlasticSurgeryTicket。 / You can only use this when you have a Plastic Surgery Ticket
	 */
	public static final SM_SYSTEM_MESSAGE STR_EDIT_CHAR_ALL_CANT_NO_ITEM = new SM_SYSTEM_MESSAGE(901752);

	/**
	 * You 可仅 use 此当 you 有 GenderSwitchTicket。 / You can only use this when you have a Gender Switch Ticket
	 */
	public static final SM_SYSTEM_MESSAGE STR_EDIT_CHAR_GENDER_CANT_NO_ITEM = new SM_SYSTEM_MESSAGE(901754);

	/**
	 * 当前位置坐标：%WORLDNAME0 区域，X=%1 Y=%2 Z=%3 / Coordinates of current location: %WORLDNAME0 Region, X=%1 Y=%2 Z=%3
	 */
	public static SM_SYSTEM_MESSAGE STR_CMD_LOCATION_DESC(int worldId, float x, float y, float z) {
		return new SM_SYSTEM_MESSAGE(230038, worldId, x, y, z);
	}

	/**
	 * 游戏中忙碌 / Busy in game
	 */
	public static final SM_SYSTEM_MESSAGE STR_BUDDYLIST_BUSY = new SM_SYSTEM_MESSAGE(900847);

	/**
	 * @param num0 You 不有足够基纳 . 其 costs%num0 基纳。 / You don't have enough Kinah. It costs %num0 Kinah
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_NOT_ENOUGH_KINA(long num0) {
		return new SM_SYSTEM_MESSAGE(901285, num0);
	}

	public static SM_SYSTEM_MESSAGE STR_GUILD_EVENT_LEVELUP(int newLevel) {
		return new SM_SYSTEM_MESSAGE(900700, newLevel);
	}

	/**
	 * 你：cannot use it because you belong to a different race。 / You cannot use it because you belong to a different race
	 */
	public static final SM_SYSTEM_MESSAGE STR_MOVE_PORTAL_ERROR_INVALID_RACE = new SM_SYSTEM_MESSAGE(901354);

	/**
	 * 你无权检查该尸体。 / You are not authorized to examine the corpse
	 */
	public static final SM_SYSTEM_MESSAGE STR_LOOT_NO_RIGHT = new SM_SYSTEM_MESSAGE(901338);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_INS_OWN_SUCCESS = new SM_SYSTEM_MESSAGE(1401275);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_ENTER_NO_RIGHT2 = new SM_SYSTEM_MESSAGE(1401364);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_ENTER_HAVE_TO_RECREATE = new SM_SYSTEM_MESSAGE(1401365);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_TOO_FAR_TO_USE = new SM_SYSTEM_MESSAGE(1401297);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_IS_ONLY_FOR_OWNER_VALID = new SM_SYSTEM_MESSAGE(
			1401298);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_ALL_CANT_USE = new SM_SYSTEM_MESSAGE(1401296);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_ACHIEVE_USE_COUNT = new SM_SYSTEM_MESSAGE(1401295);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANNOT_USE_FLOWERPOT_COOLTIME = new SM_SYSTEM_MESSAGE(
			1401280);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ALREADY_OPEN_ANOTHER_STORAGE = new SM_SYSTEM_MESSAGE(1401282);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_CANT_USE_PER_DAY = new SM_SYSTEM_MESSAGE(1401260);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_OCCUPIED_BY_OTHER = new SM_SYSTEM_MESSAGE(1401256);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_USE_HOUSE_OBJECT_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(
			1401200);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_CANCEL_USE = new SM_SYSTEM_MESSAGE(1401258);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_ORDER_OPEN_DOOR = new SM_SYSTEM_MESSAGE(1401379);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_ORDER_CLOSE_DOOR_WITHOUT_FRIENDS = new SM_SYSTEM_MESSAGE(
			1401380);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_ORDER_CLOSE_DOOR_ALL = new SM_SYSTEM_MESSAGE(1401381);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_ORDER_OUT_WITHOUT_FRIENDS = new SM_SYSTEM_MESSAGE(1401382);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_ORDER_OUT_ALL = new SM_SYSTEM_MESSAGE(1401383);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_REQUEST_OUT = new SM_SYSTEM_MESSAGE(1401384);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CHANGE_OWNER = new SM_SYSTEM_MESSAGE(1401385);
	public static final SM_SYSTEM_MESSAGE STR_HOUSING_TELEPORT_NEED_HOUSE = new SM_SYSTEM_MESSAGE(1401357);
	public static final SM_SYSTEM_MESSAGE STR_HOUSING_ENTER_NEED_HOUSE = new SM_SYSTEM_MESSAGE(1401359);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_MODE_CANNOT_START = new SM_SYSTEM_MESSAGE(1401358);
	public static final SM_SYSTEM_MESSAGE STR_HOUSING_TELEPORT_CANT_USE = new SM_SYSTEM_MESSAGE(1401244);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_BID_CANCEL = new SM_SYSTEM_MESSAGE(1401266);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_TIMEOUT = new SM_SYSTEM_MESSAGE(1401274);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_AUCTION_TIMEOUT = new SM_SYSTEM_MESSAGE(1401308);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_INS_CANT_OWN_MORE_HOUSE = new SM_SYSTEM_MESSAGE(1401276);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_EXCESS_ACCOUNT = new SM_SYSTEM_MESSAGE(1401497);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_MY_HOUSE = new SM_SYSTEM_MESSAGE(1401221);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_SUCC_BID_HOUSE = new SM_SYSTEM_MESSAGE(1401222);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_OTHER_HOUSE = new SM_SYSTEM_MESSAGE(1401223);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_GRACE_HOUSE = new SM_SYSTEM_MESSAGE(1401224);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OVERDUE = new SM_SYSTEM_MESSAGE(1401226);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_SEQUESTRATE = new SM_SYSTEM_MESSAGE(1401227);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_OVERDUE = new SM_SYSTEM_MESSAGE(1401349);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F2P_CASH_HOUSE_FEE_FREE = new SM_SYSTEM_MESSAGE(1401445);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_AUCTION_OVERDUE = new SM_SYSTEM_MESSAGE(1401317);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_SCRIPT_OVERFLOW = new SM_SYSTEM_MESSAGE(1401399);
	public static final SM_SYSTEM_MESSAGE STR_MSG_NO_RELATIONSHIP_RECENTLY = new SM_SYSTEM_MESSAGE(1401504);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_AUCTION_FAIL_ALREADY_REGISTED = new SM_SYSTEM_MESSAGE(
			1401372);

	public static final SM_SYSTEM_MESSAGE STR_MSG_UNEQUIP_RANKITEM_TIMER_10M(int value0) {
		return new SM_SYSTEM_MESSAGE(1401327, new DescriptionId(value0));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_UNEQUIP_RANKITEM_TIMER_1M(int value0) {
		return new SM_SYSTEM_MESSAGE(1401328, new DescriptionId(value0));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_UNEQUIP_RANKITEM(int value0) {
		return new SM_SYSTEM_MESSAGE(1401329, new DescriptionId(value0));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_USE_HOUSE_OBJECT_ITEM_EQUIP(DescriptionId id) {
		return new SM_SYSTEM_MESSAGE(1401294, id);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CANT_USE_HOUSE_OBJECT_ITEM_CHECK(DescriptionId id) {
		return new SM_SYSTEM_MESSAGE(1401199, id);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_USE_ALREADY_HAVE_REWARD_ITEM(int rewardNameId, int objectNameId) {
		return new SM_SYSTEM_MESSAGE(1401396, new DescriptionId(rewardNameId), calculateNameId(objectNameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_USE(int objectNameId) {
		return new SM_SYSTEM_MESSAGE(1401257, calculateNameId(objectNameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_DELETE_EXPIRE_TIME(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401261, calculateNameId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_DELETE_USE_COUNT(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401262, calculateNameId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_REWARD_ITEM(int objectNameId, int rewardItemNameId) {
		return new SM_SYSTEM_MESSAGE(1401263, calculateNameId(objectNameId), new DescriptionId(rewardItemNameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_FLOWERPOT_GOAL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401333, calculateNameId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_HOUSING_OBJECT_DELETE_USE_COUNT_FINAL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401470, calculateNameId(nameId));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_BID_SUCCESS(int address) {
		return new SM_SYSTEM_MESSAGE(1401265, address);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_BID_WIN(int address) {
		return new SM_SYSTEM_MESSAGE(1401267, address);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_AUCTION_MY_HOUSE(int address) {
		return new SM_SYSTEM_MESSAGE(1401268, address);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_AUCTION_SUCCESS(int address) {
		return new SM_SYSTEM_MESSAGE(1401269, address);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_AUCTION_FAIL(int address) {
		return new SM_SYSTEM_MESSAGE(1401270, address);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(int questId) {
		return new SM_SYSTEM_MESSAGE(1401277, questId);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_PRICE_CHANGE(long kinah) {
		return new SM_SYSTEM_MESSAGE(1401324, kinah);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_HOUSING_CANT_BID_LOW_LEVEL(int minLevel) {
		return new SM_SYSTEM_MESSAGE(1401225, minLevel);
	}

	/**
	 * 萨潘（古代气流激活器） / Sarpan (Ancient Windstream Activator)
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_LDF4a_windpath163_Ready = new SM_SYSTEM_MESSAGE(1401330);
	public static SM_SYSTEM_MESSAGE STR_MSG_LDF4a_windpath163_Open = new SM_SYSTEM_MESSAGE(1401331);
	public static SM_SYSTEM_MESSAGE STR_MSG_LDF4a_windpath163_Close = new SM_SYSTEM_MESSAGE(1401332);

	/**
	 * 次元漩涡 3.5 / Dimensional Vortex 3.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DARK_SIDE_INVADE_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1401446);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIGHT_SIDE_INVADE_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1401447);
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_INVADE_DIRECT_PORTAL_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(
			1401448);
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_INVADE_DIRECT_PORTAL_USE_COUNT_LIMIT = new SM_SYSTEM_MESSAGE(
			1401449);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER = new SM_SYSTEM_MESSAGE(1401450);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT = new SM_SYSTEM_MESSAGE(
			1401451);
	public static final SM_SYSTEM_MESSAGE STR_MSG_COMPULSION_TELEPORT_NOT_FORCE = new SM_SYSTEM_MESSAGE(1401452);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE = new SM_SYSTEM_MESSAGE(1401453);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE = new SM_SYSTEM_MESSAGE(1401454);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_OUT_NOTICE = new SM_SYSTEM_MESSAGE(1401455);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_OUT_COMPULSION = new SM_SYSTEM_MESSAGE(1401474);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_DEFENSE_FORCE_OUT = new SM_SYSTEM_MESSAGE(
			1401476);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_90M = new SM_SYSTEM_MESSAGE(1401478);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_60M = new SM_SYSTEM_MESSAGE(1401479);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_30M = new SM_SYSTEM_MESSAGE(1401480);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_15M = new SM_SYSTEM_MESSAGE(1401481);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_10M = new SM_SYSTEM_MESSAGE(1401482);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_5M = new SM_SYSTEM_MESSAGE(1401483);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_3M = new SM_SYSTEM_MESSAGE(1401484);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_2M = new SM_SYSTEM_MESSAGE(1401485);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_1M = new SM_SYSTEM_MESSAGE(1401486);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_30S = new SM_SYSTEM_MESSAGE(1401487);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_15S = new SM_SYSTEM_MESSAGE(1401488);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_10S = new SM_SYSTEM_MESSAGE(1401489);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_5S = new SM_SYSTEM_MESSAGE(1401490);
	public static final SM_SYSTEM_MESSAGE STR_CHAT_INVADEPORTL_KEEPER_SYSTEM_MSG01 = new SM_SYSTEM_MESSAGE(1401493);
	public static final SM_SYSTEM_MESSAGE STR_CHAT_INVADEPORTL_KEEPER_SYSTEM_MSG02 = new SM_SYSTEM_MESSAGE(1401494);
	public static final SM_SYSTEM_MESSAGE STR_CHAT_INVADEPORTL_KEEPER_SYSTEM_MSG03 = new SM_SYSTEM_MESSAGE(1401495);

	/**
	 * 龙主避难所 3.5 / Dragon Lord Refuge 3.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_START_LIGHT = new SM_SYSTEM_MESSAGE(1401531);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_START_DARK = new SM_SYSTEM_MESSAGE(1401532);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_CLOSE_CRACK = new SM_SYSTEM_MESSAGE(1401533);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_CLOSE_RAGE = new SM_SYSTEM_MESSAGE(1401534);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_CLOSE_GRAVITY = new SM_SYSTEM_MESSAGE(1401535);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_CLOSE_CRYSTAL = new SM_SYSTEM_MESSAGE(1401536);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_2PHASE_CLOSE_ALL = new SM_SYSTEM_MESSAGE(1401537);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_KAISINEL_2PHASE_DEADLYATK = new SM_SYSTEM_MESSAGE(1401538);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_MARCHUTAN_2PHASE_DEADLYATK = new SM_SYSTEM_MESSAGE(1401539);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_KAISINEL_2PHASE_GROGGY = new SM_SYSTEM_MESSAGE(1401540);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_MARCHUTAN_2PHASE_GROGGY = new SM_SYSTEM_MESSAGE(1401541);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_TIAMAT_DEADLYHOWLING = new SM_SYSTEM_MESSAGE(1401542);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTIAMAT_KALYNDI_SURKANA_SPAWN = new SM_SYSTEM_MESSAGE(1401543);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_COUNTDOWN_START = new SM_SYSTEM_MESSAGE(1401547);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_GOD_HP_LOWER_THAN_50p = new SM_SYSTEM_MESSAGE(1401548);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_GOD_HP_LOWER_THAN_15p = new SM_SYSTEM_MESSAGE(1401549);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_DRAKAN_BUFF_MSG = new SM_SYSTEM_MESSAGE(1401550);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_DRAKAN_ON_DIE = new SM_SYSTEM_MESSAGE(1401551);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_ANNOUNCE_DMG_SHRING = new SM_SYSTEM_MESSAGE(1401552);
	public static final SM_SYSTEM_MESSAGE STR_IDTIAMAT_TIAMAT_WARNING_MSG = new SM_SYSTEM_MESSAGE(1401553);
	public static final SM_SYSTEM_MESSAGE STR_IDTIAMAT_TIAMAT_SPAWN_BLACKHOLE = new SM_SYSTEM_MESSAGE(1401554);
	public static final SM_SYSTEM_MESSAGE IDTIAMAT_TIAMAT_COUNTDOWN_OVER = new SM_SYSTEM_MESSAGE(1401563);

	public static final SM_SYSTEM_MESSAGE STR_MSG_EMERGENCY_ESCAPE = new SM_SYSTEM_MESSAGE(1401663);

	/**
	 * @param value0 返回结果已应用于 %0。 / The returned results have been applied to %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_REIDENTIFY_APPLY_YES(String value0) {
		return new SM_SYSTEM_MESSAGE(1401910, value0);
	}

	/**
	 * 战舰 + 战场 / Dredgion + Battlefield
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDDREADGION_04 = new SM_SYSTEM_MESSAGE(1401894);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDLDF5_Under_01_War = new SM_SYSTEM_MESSAGE(1401947);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDLDF5_Under_02_War = new SM_SYSTEM_MESSAGE(1403403);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDLDF5_Fortress_War = new SM_SYSTEM_MESSAGE(1403404);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Dreadgion_Start_L = new SM_SYSTEM_MESSAGE(1403804);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Dreadgion_Start_D = new SM_SYSTEM_MESSAGE(1403805);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDTM_Lobby01 = new SM_SYSTEM_MESSAGE(1403979);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDTM_Lobbyp_01 = new SM_SYSTEM_MESSAGE(1404384);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDTM_Lobbyp_02 = new SM_SYSTEM_MESSAGE(1404385);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDTM_LobbyE_01 = new SM_SYSTEM_MESSAGE(1404386);
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDRun = new SM_SYSTEM_MESSAGE(1404465);

	public static final SM_SYSTEM_MESSAGE STR_GUILD_LEVEL_UP_CHALLENGE_TASK(int currentLevel) {
		return new SM_SYSTEM_MESSAGE(904452, currentLevel);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_PAINT_REMOVE_SUCCEED(int objectNameId) {
		return new SM_SYSTEM_MESSAGE(1401435, calculateNameId(objectNameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_PAINT_SUCCEED(int objectNameId, int colorItemNameId) {
		return new SM_SYSTEM_MESSAGE(1401436, calculateNameId(objectNameId), new DescriptionId(colorItemNameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_PAINT_ERROR_NOTOWNER = new SM_SYSTEM_MESSAGE(1401438);
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_PAINT_ERROR_CANNOTPAINT = new SM_SYSTEM_MESSAGE(1401439);
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_PAINT_ERROR_CANNOTREMOVE = new SM_SYSTEM_MESSAGE(1401440);

	/**
	 * @param num0 荣耀点数 4.5 / Glory Points 4.5
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GLORY_POINT_GAIN(int num0) {
		return new SM_SYSTEM_MESSAGE(1402081, num0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GLORY_POINT_LOSE_COMMON(int num0) {
		return new SM_SYSTEM_MESSAGE(1402082, num0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GLORY_POINT_LOSE_PERSONAL(int num0) {
		return new SM_SYSTEM_MESSAGE(1402209, num0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GLORY_POINT_LOSE(int num0) {
		return new SM_SYSTEM_MESSAGE(1402219, num0);
	}

	/**
	 * @param nameId Idian 强化43 / Idian Enchant 4.3
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_POLISH_SUCCEED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401650, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_POLISH_CANCELED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401653, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CANNOT_POLISH_ITEM(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401648, new DescriptionId(nameId));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_POLISH_WRONG_LEVEL = new SM_SYSTEM_MESSAGE(1401649);
	public static final SM_SYSTEM_MESSAGE STR_MSG_POLISH_NEED_IDENTIFY = new SM_SYSTEM_MESSAGE(1401750);

	/**
	 * @param nameId 淬炼 / Tempering
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_CANCEL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1402147, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_SUCCEEDED(int paramInt1, int paramInt2) {
		return new SM_SYSTEM_MESSAGE(1402148, new DescriptionId(paramInt1), paramInt2);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_FAILED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1402149, new DescriptionId(nameId));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_STATUS_GOOD = new SM_SYSTEM_MESSAGE(1402150);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_STATUS_NORMAL = new SM_SYSTEM_MESSAGE(1402151);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_STATUS_DANGER = new SM_SYSTEM_MESSAGE(1402152);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_STATUS_LIMIT = new SM_SYSTEM_MESSAGE(1402153);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_SUCCEEDED_MAX = new SM_SYSTEM_MESSAGE(1402154);

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_CANT_MORE_AUTHORIZE(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402155, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_FAILED_NO_PENALTY(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403386, new DescriptionId(nameId));
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_CANT_AUTHORIZE_ITEM = new SM_SYSTEM_MESSAGE(1402156);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_LIMIT_AUTHORIZE_ITEM = new SM_SYSTEM_MESSAGE(1402157);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_CANT_RESET = new SM_SYSTEM_MESSAGE(1402158);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_CANCEL = new SM_SYSTEM_MESSAGE(1402159);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_COMPLETED = new SM_SYSTEM_MESSAGE(1402160);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_INVALID_STANCE = new SM_SYSTEM_MESSAGE(1402161);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_INVALID_ITEM = new SM_SYSTEM_MESSAGE(1402162);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_NEED_IDENTIFY = new SM_SYSTEM_MESSAGE(1402163);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_INVALID_STANCE = new SM_SYSTEM_MESSAGE(1402165);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_INVALID_ITEM = new SM_SYSTEM_MESSAGE(1402166);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_WRONG_LEVEL = new SM_SYSTEM_MESSAGE(1402168);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_WRONG_QUALITY = new SM_SYSTEM_MESSAGE(1402169);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_NEED_OPEN_PACK = new SM_SYSTEM_MESSAGE(1402170);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_AUTHORIZE_RESET_NEED_OPEN_PACK = new SM_SYSTEM_MESSAGE(1402171);

	/**
	 * 现场派对音乐厅 4.3 / Live Party Concert Hall 4.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1401775);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_OPEN_NOTICE = new SM_SYSTEM_MESSAGE(1401789);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_90M = new SM_SYSTEM_MESSAGE(1401776);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_60M = new SM_SYSTEM_MESSAGE(1401777);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_30M = new SM_SYSTEM_MESSAGE(1401778);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_15M = new SM_SYSTEM_MESSAGE(1401779);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_10M = new SM_SYSTEM_MESSAGE(1401780);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_5M = new SM_SYSTEM_MESSAGE(1401781);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_3M = new SM_SYSTEM_MESSAGE(1401782);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_2M = new SM_SYSTEM_MESSAGE(1401783);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_1M = new SM_SYSTEM_MESSAGE(1401784);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_30S = new SM_SYSTEM_MESSAGE(1401785);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_15S = new SM_SYSTEM_MESSAGE(1401786);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_10S = new SM_SYSTEM_MESSAGE(1401787);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_5S = new SM_SYSTEM_MESSAGE(1401788);

	/**
	 * 你：successfully enchanted %0 by + %num1。 / You successfully enchanted %0 by + %num1
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_ENCHANT_ITEM_SUCCEED_NEW(DescriptionId descriptionId, int value0) {
		return new SM_SYSTEM_MESSAGE(1401681, descriptionId, value0);
	}

	/**
	 * %1 无法用 %0 包装。 / %1 cannot be wrapped with %0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_PACK_ITEM_WRONG_TARGET_ITEM_CATEGORY(int parentNameId, int targetNameId) {
		return new SM_SYSTEM_MESSAGE(1402018, parentNameId, targetNameId);
	}

	/**
	 * [代理人之战] 4.7 / [Agent's Fight] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_GodElite = new SM_SYSTEM_MESSAGE(1402543);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_GodElite_time_01 = new SM_SYSTEM_MESSAGE(1402544);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_GodElite_time_02 = new SM_SYSTEM_MESSAGE(1402545);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_GodElite_time_03 = new SM_SYSTEM_MESSAGE(1402546);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_TimeAttack_Start = new SM_SYSTEM_MESSAGE(1402625);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GODELITE_TimeAttack_Fail = new SM_SYSTEM_MESSAGE(1402626);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_Hp50 = new SM_SYSTEM_MESSAGE(1401208);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_Hp10 = new SM_SYSTEM_MESSAGE(1401209);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_Reward = new SM_SYSTEM_MESSAGE(1401218);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_Spawned = new SM_SYSTEM_MESSAGE(1401219);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_OdSpawn = new SM_SYSTEM_MESSAGE(1401206);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Jusin_OdStart = new SM_SYSTEM_MESSAGE(1401207);

	/**
	 * [贝里特拉入侵] 4.7 / [Beritra Invasion] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_01 = new SM_SYSTEM_MESSAGE(1402383);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_02 = new SM_SYSTEM_MESSAGE(1402384);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_03 = new SM_SYSTEM_MESSAGE(1402385);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_04 = new SM_SYSTEM_MESSAGE(1402386);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_01 = new SM_SYSTEM_MESSAGE(1402387);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_02 = new SM_SYSTEM_MESSAGE(1402388);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_03 = new SM_SYSTEM_MESSAGE(1402389);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_04 = new SM_SYSTEM_MESSAGE(1402390);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_05 = new SM_SYSTEM_MESSAGE(1402391);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_MESSAGE_DIE_06 = new SM_SYSTEM_MESSAGE(1402392);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_INVADE_VRITRA = new SM_SYSTEM_MESSAGE(1402459);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_INVADE_VRITRA_SPECIAL = new SM_SYSTEM_MESSAGE(1402460);

	/**
	 * [热点位置] 4.7 / [Hotspot Location] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOTSPOT_NOT_ENOUGH_COST = new SM_SYSTEM_MESSAGE(1402443);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOTSPOT_CANT_MOVE_THIS_STANCE = new SM_SYSTEM_MESSAGE(1402444);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOTSPOT_CANT_MOVE_NOW = new SM_SYSTEM_MESSAGE(1402445);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HOTSPOT_CANCEL_MOVE = new SM_SYSTEM_MESSAGE(1402446);

	/**
	 * [莱文肖尔声晶] 4.7 / [Levinshor Vocolith] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_FNamed_Fail = new SM_SYSTEM_MESSAGE(1402539);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_FNamed_Spawn = new SM_SYSTEM_MESSAGE(1402540);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_FNamed_Spawn_Item = new SM_SYSTEM_MESSAGE(1402541);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_Summon_Named_Time = new SM_SYSTEM_MESSAGE(1402602);

	/**
	 * [狂暴阿诺哈] 4.7/5.3 / [Berserk Anoha] 4.7/5.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Named_Spawn = new SM_SYSTEM_MESSAGE(1402483);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Named_Spawn_Item = new SM_SYSTEM_MESSAGE(1402484);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Anoha_Spawn = new SM_SYSTEM_MESSAGE(1402503);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Anoha_Die = new SM_SYSTEM_MESSAGE(1402504);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Anoha_DeSpawn = new SM_SYSTEM_MESSAGE(1402505);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Named_Spawn_System = new SM_SYSTEM_MESSAGE(1402584);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Anoha_01 = new SM_SYSTEM_MESSAGE(1403715);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Anoha_02 = new SM_SYSTEM_MESSAGE(1403716);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Anoha_03 = new SM_SYSTEM_MESSAGE(1403717);

	/**
	 * [Purifier Item] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_ERROR = new SM_SYSTEM_MESSAGE(1402398);
	public static final SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_NO_IDENTIFY = new SM_SYSTEM_MESSAGE(
			1402421);
	public static final SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_NEED_AP = new SM_SYSTEM_MESSAGE(1402571);
	public static final SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_NEED_QINA = new SM_SYSTEM_MESSAGE(
			1402572);

	public static SM_SYSTEM_MESSAGE STR_ITEM_UPGRADE_MSG_UPGRADE_SUCCESS(DescriptionId nameId, DescriptionId nameId2) {
		return new SM_SYSTEM_MESSAGE(1402579, new Object[] { nameId, nameId2 });
	}

	public static SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402397, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_REGISTER_ITEM_MSG_UPGRADE_CANNOT_02(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1403043, descriptionId);
	}

	/**
	 * [链门铸造厂] 4.7 / [Linkgate Foundry] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4Re_01_DoorOpen_01 = new SM_SYSTEM_MESSAGE(1402440);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4Re_01_DoorOpen_02 = new SM_SYSTEM_MESSAGE(1402441);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4Re_01_DoorOpen_03 = new SM_SYSTEM_MESSAGE(1402442);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_01 = new SM_SYSTEM_MESSAGE(1402453);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_02 = new SM_SYSTEM_MESSAGE(1402454);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_03 = new SM_SYSTEM_MESSAGE(1402455);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_04 = new SM_SYSTEM_MESSAGE(1402456);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_05 = new SM_SYSTEM_MESSAGE(1402457);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_06 = new SM_SYSTEM_MESSAGE(1402458);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_Time_07 = new SM_SYSTEM_MESSAGE(1402461);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF4_Re_01_secret_room_03 = new SM_SYSTEM_MESSAGE(1402603);

	/**
	 * [帕内斯特拉] 4.7 / [Panesterra] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Gab1_START01 = new SM_SYSTEM_MESSAGE(1402609);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Gab1_START02 = new SM_SYSTEM_MESSAGE(1402610);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Gab1_START03 = new SM_SYSTEM_MESSAGE(1402611);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Gab1_START04 = new SM_SYSTEM_MESSAGE(1402612);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Gab1_START05 = new SM_SYSTEM_MESSAGE(1402613);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User = new SM_SYSTEM_MESSAGE(1402283);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User02 = new SM_SYSTEM_MESSAGE(1402284);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User03 = new SM_SYSTEM_MESSAGE(1402291);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User04 = new SM_SYSTEM_MESSAGE(1402364);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User05 = new SM_SYSTEM_MESSAGE(1402370);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User06 = new SM_SYSTEM_MESSAGE(1402382);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_GAb1_User07 = new SM_SYSTEM_MESSAGE(1402420);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End01 = new SM_SYSTEM_MESSAGE(1402620);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End02 = new SM_SYSTEM_MESSAGE(1402621);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End03 = new SM_SYSTEM_MESSAGE(1402622);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End04 = new SM_SYSTEM_MESSAGE(1402623);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End05 = new SM_SYSTEM_MESSAGE(1402624);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End06 = new SM_SYSTEM_MESSAGE(1402982);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End07 = new SM_SYSTEM_MESSAGE(1402983);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End08 = new SM_SYSTEM_MESSAGE(1402984);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End09 = new SM_SYSTEM_MESSAGE(1402985);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End10 = new SM_SYSTEM_MESSAGE(1402986);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Gab1_End11 = new SM_SYSTEM_MESSAGE(1403013);
	public static final SM_SYSTEM_MESSAGE STR_Gab1_ARTIFACT_CASTING = new SM_SYSTEM_MESSAGE(1402547);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_INVADE_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1402399);
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_SVS_DIRECT_PORTAL_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(1402400);
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_SVS_DIRECT_PORTAL_USE_COUNT_LIMIT = new SM_SYSTEM_MESSAGE(
			1402401);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER = new SM_SYSTEM_MESSAGE(1402402);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_COMPULSION = new SM_SYSTEM_MESSAGE(1402403);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_OPEN_NOTICE = new SM_SYSTEM_MESSAGE(1402404);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT = new SM_SYSTEM_MESSAGE(
			1402405);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_90M = new SM_SYSTEM_MESSAGE(1402406);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_60M = new SM_SYSTEM_MESSAGE(1402407);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_30M = new SM_SYSTEM_MESSAGE(1402408);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_15M = new SM_SYSTEM_MESSAGE(1402409);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_10M = new SM_SYSTEM_MESSAGE(1402410);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_5M = new SM_SYSTEM_MESSAGE(1402411);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_3M = new SM_SYSTEM_MESSAGE(1402412);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_2M = new SM_SYSTEM_MESSAGE(1402413);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_1M = new SM_SYSTEM_MESSAGE(1402414);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_30S = new SM_SYSTEM_MESSAGE(1402415);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_15S = new SM_SYSTEM_MESSAGE(1402416);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_10S = new SM_SYSTEM_MESSAGE(1402417);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_5S = new SM_SYSTEM_MESSAGE(1402418);

	/**
	 * [卡尔多] 4.7 / [Kaldor] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_RuneElite = new SM_SYSTEM_MESSAGE(1402542);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Li_Center = new SM_SYSTEM_MESSAGE(1402462);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Li_Up = new SM_SYSTEM_MESSAGE(1402463);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Li_Down = new SM_SYSTEM_MESSAGE(1402464);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Da_Center = new SM_SYSTEM_MESSAGE(1402465);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Da_Up = new SM_SYSTEM_MESSAGE(1402466);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Da_Down = new SM_SYSTEM_MESSAGE(1402467);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Dr_Center = new SM_SYSTEM_MESSAGE(1402468);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Dr_Up = new SM_SYSTEM_MESSAGE(1402469);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF5_Fortress_Dr_Down = new SM_SYSTEM_MESSAGE(1402470);

	/**
	 * [韦尔休堡垒] 4.7 / [Wealhtheow's Keep] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_01_1_Item = new SM_SYSTEM_MESSAGE(1402492);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_01_2_Item = new SM_SYSTEM_MESSAGE(1402493);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_02_1_Item = new SM_SYSTEM_MESSAGE(1402494);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_02_2_Item = new SM_SYSTEM_MESSAGE(1402495);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_03_1_Item = new SM_SYSTEM_MESSAGE(1402496);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_03_2_Item = new SM_SYSTEM_MESSAGE(1402497);
	public static final SM_SYSTEM_MESSAGE STR_MSG_7011_Mercenary_Chief_03_3_Item = new SM_SYSTEM_MESSAGE(1402498);

	/**
	 * [伊杰尔穹顶] 4.7 / [Idgel Dome] 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDLDF5_Fortress_Re = new SM_SYSTEM_MESSAGE(1402192);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLdf5_fortress_re_ReturnToSp = new SM_SYSTEM_MESSAGE(1402578);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLdf5_fortress_re_Boss_AttackSTART = new SM_SYSTEM_MESSAGE(1402597);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLdf5_fortress_re_Boss_Spawn = new SM_SYSTEM_MESSAGE(1402598);

	/**
	 * 网吧经验 / PC Bang Xp
	 */
	public static SM_SYSTEM_MESSAGE STR_GET_EXP_PCBANG_VITAL_BONUS(String value0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402213, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_PCBANG_VITAL_MAKEUP_BONUS(String value0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402214, value0, num1, num2, num3);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN_PCBANG_VITAL_BONUS(String value0, long num1) {
		return new SM_SYSTEM_MESSAGE(1402215, value0, num1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_COMBAT_MY_EXP_GAIN_PCBANG_VITAL_MAKEUP_BONUS(String value0, long num1,
			long num2) {
		return new SM_SYSTEM_MESSAGE(1402216, value0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_PCBANG_VITAL_BONUS(long num0, long num1) {
		return new SM_SYSTEM_MESSAGE(1402217, num0, num1);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_PCBANG_VITAL_MAKEUP_BONUS(long num0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402218, num0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_PCBANG_VITAL_ABSOLUTEEXP_BONUS(String value0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402628, value0, num1, num2, num3);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP_PCBANG_VITAL_MAKEUP_ABSOLUTEEXP_BONUS(String value0, long num1,
			long num2, long num3, long num4) {
		return new SM_SYSTEM_MESSAGE(1402629, value0, num1, num2, num3, num4);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_ABSOLUTEEXP_BONUS_DESC(DescriptionId value0, long num0, long num1) {
		return new SM_SYSTEM_MESSAGE(1402630, value0, num0, num1);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_VITAL_ABSOLUTEEXP_BONUS(long num0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402631, num0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_MAKEUP_ABSOLUTEEXP_BONUS(long num0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402632, num0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_VITAL_MAKEUP_ABSOLUTEEXP_BONUS(long num0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402633, num0, num1, num2, num3);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_PCBANG_VITAL_ABSOLUTEEXP_BONUS(long num0, long num1, long num2) {
		return new SM_SYSTEM_MESSAGE(1402634, num0, num1, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_GET_EXP2_PCBANG_VITAL_MAKEUP_ABSOLUTEEXP_BONUS(long num0, long num1, long num2,
			long num3) {
		return new SM_SYSTEM_MESSAGE(1402635, num0, num1, num2, num3);
	}

	/**
	 * F2P
	 */
	public static SM_SYSTEM_MESSAGE STR_F2P_MSG_NOTICE_PACK_EXPIRE_TIME(String durationtime0, String value1) {
		return new SM_SYSTEM_MESSAGE(1700040, durationtime0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_F2P_MSG_WARNING_PACK_EXPIRE_TIME(String durationtime0, String value1) {
		return new SM_SYSTEM_MESSAGE(1700041, durationtime0, value1);
	}

	public static final SM_SYSTEM_MESSAGE STR_F2P_MSG_PACK_EXPIRED = new SM_SYSTEM_MESSAGE(1700042);

	/**
	 * @param nameId 你：have opened the %0。 / You have opened the %0
	 */
	public static SM_SYSTEM_MESSAGE STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400452, new DescriptionId(nameId));
	}

	/**
	 * @param nameId 你：have stopped opening the %0 bundle。 / You have stopped opening the %0 bundle
	 */
	public static SM_SYSTEM_MESSAGE STR_UNCOMPRESS_COMPRESSED_ITEM_CANCELED(int nameId) {
		return new SM_SYSTEM_MESSAGE(1400453, new DescriptionId(nameId));
	}

	/**
	 * 骑乘限制 / Ride Restriction
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ATTACK_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401093);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401094);
	public static final SM_SYSTEM_MESSAGE STR_MSG_PERSONAL_SHOP_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401095);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GATHER_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401096);
	public static final SM_SYSTEM_MESSAGE STR_MSG_COMBINE_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401097);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_RIDE_INVALID_LOCATION = new SM_SYSTEM_MESSAGE(1401099);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SKILL_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1401132);
	public static final SM_SYSTEM_MESSAGE STR_MSG_NORIDE_AREA_STOP = new SM_SYSTEM_MESSAGE(1401170);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_SELL_WHILE_IN_RIDE = new SM_SYSTEM_MESSAGE(1401210);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_RIDE = new SM_SYSTEM_MESSAGE(1401211);
	public static final SM_SYSTEM_MESSAGE STR_MSG_NORIDE_TIME_EXPIRE = new SM_SYSTEM_MESSAGE(1401220);
	public static final SM_SYSTEM_MESSAGE STR_MSG_UNRIDE_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(1401254);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_RIDE_ABNORMAL_STATE = new SM_SYSTEM_MESSAGE(1401255);
	public static final SM_SYSTEM_MESSAGE STR_MSG_UNRIDE_DESTROY_ITEM = new SM_SYSTEM_MESSAGE(1401426);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_RIDE_NEARBY_CEILING = new SM_SYSTEM_MESSAGE(1402950);

	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_ENCHANT_ITEM = new SM_SYSTEM_MESSAGE(1403363);

	/**
	 * 阿特雷亚护照 4.7 / Atreian Passport 4.7
	 */
	public static final SM_SYSTEM_MESSAGE STR_ATTEND_MSG_ATTEND_REWARD_GET = new SM_SYSTEM_MESSAGE(1402601);

	public static SM_SYSTEM_MESSAGE STR_MSG_ATTEND_REWARD_REMOVE_EXCESS(int value0) {
		return new SM_SYSTEM_MESSAGE(1402627, new DescriptionId(value0));
	}

	/**
	 * Illusion Godstone 4.7
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_BREAK_PROC_REMAIN_START(String durationtime0, String value1) {
		return new SM_SYSTEM_MESSAGE(1402536, durationtime0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BREAK_PROC_REMAIN_MIN(String durationtime0, String value1) {
		return new SM_SYSTEM_MESSAGE(1402537, durationtime0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BREAK_PROC_REMAIN_SEC(String durationtime0, String value1) {
		return new SM_SYSTEM_MESSAGE(1402538, durationtime0, value1);
	}

	/**
	 * Amplification Item 4.7.5.2
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_CANNOT_01(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402650, descriptionId);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_CANNOT_02 = new SM_SYSTEM_MESSAGE(1402651);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_CANNOT_03 = new SM_SYSTEM_MESSAGE(1402652);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_CANNOT_04 = new SM_SYSTEM_MESSAGE(1402653);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_INVALID_STANCE = new SM_SYSTEM_MESSAGE(1402654);

	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_NO_TARGET_ITEM(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402655, descriptionId);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_EXCEED_ALREADY = new SM_SYSTEM_MESSAGE(1402656);

	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_SUCCEED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402657, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_ENCHANT_CANNOT_01(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402658, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ENCHANT_ITEM_SUCCEEDED_EXCEED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402659, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CAN_EXCEED_ENCHANT_LEVEL(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402660, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_ENCHANT_CANNOT_02(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402661, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_SKILL_ENCHANT(DescriptionId num0, int num1, int num2) {
		return new SM_SYSTEM_MESSAGE(1402662, num0, num2);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EXCEED_SKILL_DELETE(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402663, descriptionId);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_AB1_PCTank_NoItem = new SM_SYSTEM_MESSAGE(1402191);

	/**
	 * [混沌裂隙] 4.8 / [Chaos Rift] 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIGHT_SIDE_CHAOS_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1402875);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DARK_SIDE_CHAOS_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1402876);

	/**
	 * 军团 Rift48 / [Legion Rift] 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LIGHT_SIDE_LEGION_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1402877);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DARK_SIDE_LEGION_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1402878);

	/**
	 * [伊迪安深渊] 4.8 / [Idian Depths] 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_Under_User = new SM_SYSTEM_MESSAGE(1402945);

	/**
	 * 烙印之石48 / [Stigma] 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_STIGMA_OPEN_NORMAL_SLOT = new SM_SYSTEM_MESSAGE(1402933);
	public static final SM_SYSTEM_MESSAGE STR_MSG_STIGMA_OPEN_ENHANCED1_SLOT = new SM_SYSTEM_MESSAGE(1402934);
	public static final SM_SYSTEM_MESSAGE STR_MSG_STIGMA_OPEN_ENHANCED2_SLOT = new SM_SYSTEM_MESSAGE(1402935);
	public static final SM_SYSTEM_MESSAGE STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST = new SM_SYSTEM_MESSAGE(1402942);

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_CANT_ENCHANT(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402896, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_GET_LINKED_SKILL(DescriptionId descriptionId, int skilllvl) {
		return new SM_SYSTEM_MESSAGE(1402891, descriptionId, skilllvl);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_DELETE_LINKED_SKILL(DescriptionId descriptionId, int skilllvl) {
		return new SM_SYSTEM_MESSAGE(1402895, descriptionId, skilllvl);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_ENCHANT_LINKED_SKILL(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402944, descriptionId);
	}

	/**
	 * @param descriptionId 烙印之石强化48 / [Stigma Enchant] 4.8
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_ENCHANT_SUCCESS(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402930, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_ENCHANT_FAIL(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402931, descriptionId);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_STIGMA_ENCHANT_CANCEL(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402936, descriptionId);
	}

	/**
	 * [Quest Cooltime]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_QUEST_COOLTIME_START_LIMITED_DURATION(String value0) {
		return new SM_SYSTEM_MESSAGE(1402676, value0);
	}

	/**
	 * [导师] / [Mentor]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_BE_MENTEE_BY_LEVEL_LIMIT = new SM_SYSTEM_MESSAGE(1402835);

	/**
	 * [被占领的达努阿尔圣所] / [Seized Danuar Sanctuary]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_Door_01 = new SM_SYSTEM_MESSAGE(1401838);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_Door_02 = new SM_SYSTEM_MESSAGE(1401839);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_NeedKey = new SM_SYSTEM_MESSAGE(1401928);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_Canon = new SM_SYSTEM_MESSAGE(1401929);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5U2_NeedWeaponKey = new SM_SYSTEM_MESSAGE(1401932);

	/**
	 * [六角道] / [The Hexway]
	 */
	public static final SM_SYSTEM_MESSAGE STR_Underpass_IDUnderpassRe_Open = new SM_SYSTEM_MESSAGE(1401605);

	/**
	 * [永恒堡垒] / [The Eternal Bastion]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5B_TD_Tank = new SM_SYSTEM_MESSAGE(1401680);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5B_TD_DEFWeapon = new SM_SYSTEM_MESSAGE(1401679);

	/**
	 * [炼狱/光明方尖碑] / [Infernal/Illuminary Obelisk]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_Hard_Door_Open = new SM_SYSTEM_MESSAGE(1402423);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_CHARGE_01 = new SM_SYSTEM_MESSAGE(1402194);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_CHARGE_02 = new SM_SYSTEM_MESSAGE(1402195);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_CHARGE_03 = new SM_SYSTEM_MESSAGE(1402196);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_CHARGE_04 = new SM_SYSTEM_MESSAGE(1402197);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_CHARGE_END = new SM_SYSTEM_MESSAGE(1402203);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_FINAL_CHARGE_01 = new SM_SYSTEM_MESSAGE(1402198);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_FINAL_CHARGE_02 = new SM_SYSTEM_MESSAGE(1402199);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_FINAL_CHARGE_03 = new SM_SYSTEM_MESSAGE(1402200);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_OBJ_FINAL_CHARGE_04 = new SM_SYSTEM_MESSAGE(1402201);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_DEFENCE_01_ATTACKED = new SM_SYSTEM_MESSAGE(1402220);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_DEFENCE_02_ATTACKED = new SM_SYSTEM_MESSAGE(1402221);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_DEFENCE_03_ATTACKED = new SM_SYSTEM_MESSAGE(1402222);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_U3_DEFENCE_04_ATTACKED = new SM_SYSTEM_MESSAGE(1402223);

	/**
	 * [奥菲丹桥] / [Ophidan Bridge]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Wi_Start = new SM_SYSTEM_MESSAGE(1402848);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Wi_Point_01 = new SM_SYSTEM_MESSAGE(1402849);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Wi_Point_02 = new SM_SYSTEM_MESSAGE(1402850);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Wi_Point_03 = new SM_SYSTEM_MESSAGE(1402851);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Pr_Start = new SM_SYSTEM_MESSAGE(1402852);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Pr_Point_01 = new SM_SYSTEM_MESSAGE(1402853);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Pr_Point_02 = new SM_SYSTEM_MESSAGE(1402854);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_Pr_Point_03 = new SM_SYSTEM_MESSAGE(1402855);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_As_Start = new SM_SYSTEM_MESSAGE(1402856);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_As_Point_01 = new SM_SYSTEM_MESSAGE(1402857);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_As_Point_02 = new SM_SYSTEM_MESSAGE(1402858);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_U_01_RA_As_Point_03 = new SM_SYSTEM_MESSAGE(1402859);

	/**
	 * [修道院] / [Abbey]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Telepoter_Arena_Clobby_User = new SM_SYSTEM_MESSAGE(1402673);

	/**
	 * [特兰西迪姆附楼] / [Transidium Annex]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_TANK_A_ATTACKED = new SM_SYSTEM_MESSAGE(1402258);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_TANK_B_ATTACKED = new SM_SYSTEM_MESSAGE(1402259);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_TANK_C_ATTACKED = new SM_SYSTEM_MESSAGE(1402260);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_TANK_D_ATTACKED = new SM_SYSTEM_MESSAGE(1402261);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_PORTAL_DEST_69_ATTACKED = new SM_SYSTEM_MESSAGE(1402266);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_PORTAL_DEST_70_ATTACKED = new SM_SYSTEM_MESSAGE(1402267);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_PORTAL_DEST_71_ATTACKED = new SM_SYSTEM_MESSAGE(1402268);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_PORTAL_DEST_72_ATTACKED = new SM_SYSTEM_MESSAGE(1402269);
	public static final SM_SYSTEM_MESSAGE STR_CANNOT_USE_DIRECT_PORTAL_NOT_TITLE = new SM_SYSTEM_MESSAGE(1402286);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_01 = new SM_SYSTEM_MESSAGE(1402252);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_02 = new SM_SYSTEM_MESSAGE(1402253);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_03 = new SM_SYSTEM_MESSAGE(1402254);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_04 = new SM_SYSTEM_MESSAGE(1402255);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_05 = new SM_SYSTEM_MESSAGE(1402256);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_06 = new SM_SYSTEM_MESSAGE(1402257);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_07 = new SM_SYSTEM_MESSAGE(1402586);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_08 = new SM_SYSTEM_MESSAGE(1402587);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_11 = new SM_SYSTEM_MESSAGE(1402639);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GAB1_SUB_ALARM_12 = new SM_SYSTEM_MESSAGE(1402640);

	/**
	 * [奥利尔/佩尔农] / [Oriel/Pernon]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_HF_SpringAgrintAppear = new SM_SYSTEM_MESSAGE(1401246);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HF_SummerAgrintAppear = new SM_SYSTEM_MESSAGE(1401247);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HF_FallAgrintAppear = new SM_SYSTEM_MESSAGE(1401248);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HF_WinterAgrintAppear = new SM_SYSTEM_MESSAGE(1401249);
	public static final SM_SYSTEM_MESSAGE STR_MSG_HF_ShugoCaravanAppear = new SM_SYSTEM_MESSAGE(1401250);

	/**
	 * [Escort Quest]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ESCORT_Start = new SM_SYSTEM_MESSAGE(1401164);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ESCORT_Distance_out = new SM_SYSTEM_MESSAGE(1401165);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ESCORT_Time_out = new SM_SYSTEM_MESSAGE(1401166);

	/**
	 * [基地] / [Base]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v01 = new SM_SYSTEM_MESSAGE(1402506);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v02 = new SM_SYSTEM_MESSAGE(1402507);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v03 = new SM_SYSTEM_MESSAGE(1402508);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v04 = new SM_SYSTEM_MESSAGE(1402509);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v05 = new SM_SYSTEM_MESSAGE(1402510);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v06 = new SM_SYSTEM_MESSAGE(1402511);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v07 = new SM_SYSTEM_MESSAGE(1402512);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v08 = new SM_SYSTEM_MESSAGE(1402513);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v09 = new SM_SYSTEM_MESSAGE(1402514);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v10 = new SM_SYSTEM_MESSAGE(1402515);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v11 = new SM_SYSTEM_MESSAGE(1402516);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v12 = new SM_SYSTEM_MESSAGE(1402517);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_chief_v13 = new SM_SYSTEM_MESSAGE(1402518);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v01 = new SM_SYSTEM_MESSAGE(1402519);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v02 = new SM_SYSTEM_MESSAGE(1402520);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v03 = new SM_SYSTEM_MESSAGE(1402521);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v04 = new SM_SYSTEM_MESSAGE(1402522);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v05 = new SM_SYSTEM_MESSAGE(1402523);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v06 = new SM_SYSTEM_MESSAGE(1402524);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v07 = new SM_SYSTEM_MESSAGE(1402525);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v08 = new SM_SYSTEM_MESSAGE(1402526);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v09 = new SM_SYSTEM_MESSAGE(1402527);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v10 = new SM_SYSTEM_MESSAGE(1402528);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v11 = new SM_SYSTEM_MESSAGE(1402529);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v12 = new SM_SYSTEM_MESSAGE(1402530);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LDF4_Advance_killer_v13 = new SM_SYSTEM_MESSAGE(1402531);

	/**
	 * [卡玛尔战场 4.3] / [Kamar Battlfield 4.3]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDKamar = new SM_SYSTEM_MESSAGE(1401730);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDKamar_DrakanGeneral_Hit = new SM_SYSTEM_MESSAGE(1401845);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDKamar_LightGeneral_Hit = new SM_SYSTEM_MESSAGE(1401848);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDKamar_DarkGeneral_Hit = new SM_SYSTEM_MESSAGE(1401850);

	/**
	 * [铁壁战线] 4.5 / [Iron Wall Warfront] 4.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_INSTANCE_OPEN_IDF5_TD_war = new SM_SYSTEM_MESSAGE(1402032);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_01 = new SM_SYSTEM_MESSAGE(1402089);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_02 = new SM_SYSTEM_MESSAGE(1402090);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_03 = new SM_SYSTEM_MESSAGE(1402091);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_04 = new SM_SYSTEM_MESSAGE(1402092);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_05 = new SM_SYSTEM_MESSAGE(1402093);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_06 = new SM_SYSTEM_MESSAGE(1402094);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_07 = new SM_SYSTEM_MESSAGE(1402095);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_08 = new SM_SYSTEM_MESSAGE(1402096);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_09 = new SM_SYSTEM_MESSAGE(1402097);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Li_10 = new SM_SYSTEM_MESSAGE(1402098);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_01 = new SM_SYSTEM_MESSAGE(1402099);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_02 = new SM_SYSTEM_MESSAGE(1402100);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_03 = new SM_SYSTEM_MESSAGE(1402101);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_04 = new SM_SYSTEM_MESSAGE(1402102);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_05 = new SM_SYSTEM_MESSAGE(1402103);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_06 = new SM_SYSTEM_MESSAGE(1402104);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_07 = new SM_SYSTEM_MESSAGE(1402105);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_08 = new SM_SYSTEM_MESSAGE(1402106);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_09 = new SM_SYSTEM_MESSAGE(1402107);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_TD_War_Officer_Da_10 = new SM_SYSTEM_MESSAGE(1402108);

	/**
	 * [守护者/征服者 + 攻城守护者] 4.8 / [Protector/Conqueror + Siege Protector] 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_SLAYER_UP_1LEVEL = new SM_SYSTEM_MESSAGE(1402842);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SLAYER_UP_2LEVEL = new SM_SYSTEM_MESSAGE(1402843);
	public static final SM_SYSTEM_MESSAGE STR_MSG_SLAYER_UP_3LEVEL = new SM_SYSTEM_MESSAGE(1402844);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GUARD_UP_1LEVEL = new SM_SYSTEM_MESSAGE(1402845);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GUARD_UP_2LEVEL = new SM_SYSTEM_MESSAGE(1402846);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GUARD_UP_3LEVEL = new SM_SYSTEM_MESSAGE(1402847);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GUARD_UP_LEGION = new SM_SYSTEM_MESSAGE(1402879);

	/**
	 * [佐尔希夫战舰] / [Zorshiv Dredgion]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Public_Quest_Accept = new SM_SYSTEM_MESSAGE(1401252);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Public_Quest_Reward = new SM_SYSTEM_MESSAGE(1401253);

	/**
	 * [炼狱] 达努阿尔圣物匣 4.3/4.7/4.9.1 / [Infernal] Danuar Reliquary 4.3/4.7/4.9.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_Rune_User_Kill = new SM_SYSTEM_MESSAGE(1401893);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_Rune_Boss_Reset_Nor = new SM_SYSTEM_MESSAGE(1403145);

	/**
	 * 助威 4.8 / Cheering 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_BUFF_PET_USE_START_MESSAGE = new SM_SYSTEM_MESSAGE(1402606);
	public static final SM_SYSTEM_MESSAGE STR_MSG_BUFF_PET_USE_STOP_MESSAGE = new SM_SYSTEM_MESSAGE(1402607);
	public static final SM_SYSTEM_MESSAGE STR_MSG_BUFF_PET_USE_STOP_MESSAGE_03 = new SM_SYSTEM_MESSAGE(1402672);

	/**
	 * 基地增益 / Base Buff
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_LIGHT_GAIN = new SM_SYSTEM_MESSAGE(1402588);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_LIGHT_GET_OUT_AREA = new SM_SYSTEM_MESSAGE(1402589);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_LIGHT_MIST_OFF = new SM_SYSTEM_MESSAGE(1402590);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_LIGHT_WARNING = new SM_SYSTEM_MESSAGE(1402591);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_DARK_GAIN = new SM_SYSTEM_MESSAGE(1402592);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_DARK_GET_OUT_AREA = new SM_SYSTEM_MESSAGE(1402593);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_DARK_MIST_OFF = new SM_SYSTEM_MESSAGE(1402594);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WEAK_RACE_BUFF_DARK_WARNING = new SM_SYSTEM_MESSAGE(1402595);

	/**
	 * [占领] 伦图斯基地 4.8 / [Occupied] Rentus Base 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDYun_Rasta_Spawn_01 = new SM_SYSTEM_MESSAGE(1402775);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDYun_Rasta_Spawn_02 = new SM_SYSTEM_MESSAGE(1402776);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDYun_Rasta_SUCCEED_01 = new SM_SYSTEM_MESSAGE(1402883);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDYun_Kuhara_StatDown = new SM_SYSTEM_MESSAGE(1401101);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDYun_Kuhara_Barrel_Spawn = new SM_SYSTEM_MESSAGE(1401100);

	/**
	 * 龙脊深渊 4.8 / Drakenspire Depths 4.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Twin_06 = new SM_SYSTEM_MESSAGE(1402687);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Vritra_Human_01 = new SM_SYSTEM_MESSAGE(1402716);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Vritra_Human_02 = new SM_SYSTEM_MESSAGE(1402717);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Vritra_Human_03 = new SM_SYSTEM_MESSAGE(1402718);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Vritra_Human_04 = new SM_SYSTEM_MESSAGE(1402719);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDSeal_Vritra_Human_05 = new SM_SYSTEM_MESSAGE(1402720);

	/**
	 * [克罗坦/基西斯/米伦] 4.9 / [Krotan/Kysis/Miren] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Crotan_Named_Spawn = new SM_SYSTEM_MESSAGE(1403049);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Crotan_Named_Spawn_In = new SM_SYSTEM_MESSAGE(1403142);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Crotan_Named_Spawn_System = new SM_SYSTEM_MESSAGE(1403050);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Crotan_Named_Spawn_Item = new SM_SYSTEM_MESSAGE(1403051);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_Crotan = new SM_SYSTEM_MESSAGE(1403112);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_Crotan = new SM_SYSTEM_MESSAGE(1403113);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_Crotan = new SM_SYSTEM_MESSAGE(1403114);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Lamiren_Named_Spawn = new SM_SYSTEM_MESSAGE(1403052);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Lamiren_Named_Spawn_In = new SM_SYSTEM_MESSAGE(1403143);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Lamiren_Named_Spawn_System = new SM_SYSTEM_MESSAGE(1403053);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Lamiren_Named_Spawn_Item = new SM_SYSTEM_MESSAGE(1403054);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_lamiren = new SM_SYSTEM_MESSAGE(1403115);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_lamiren = new SM_SYSTEM_MESSAGE(1403116);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_lamiren = new SM_SYSTEM_MESSAGE(1403117);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Dkisas_Named_Spawn = new SM_SYSTEM_MESSAGE(1403055);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Dkisas_Named_Spawn_In = new SM_SYSTEM_MESSAGE(1403144);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Dkisas_Named_Spawn_System = new SM_SYSTEM_MESSAGE(1403056);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Dkisas_Named_Spawn_Item = new SM_SYSTEM_MESSAGE(1403057);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_dkisas = new SM_SYSTEM_MESSAGE(1403118);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_dkisas = new SM_SYSTEM_MESSAGE(1403119);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_dkisas = new SM_SYSTEM_MESSAGE(1403120);
	// ============================================================================================================//
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_05 = new SM_SYSTEM_MESSAGE(1404046);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_03 = new SM_SYSTEM_MESSAGE(1404047);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1221_01 = new SM_SYSTEM_MESSAGE(1404048);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_05 = new SM_SYSTEM_MESSAGE(1404049);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_03 = new SM_SYSTEM_MESSAGE(1404050);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1231_01 = new SM_SYSTEM_MESSAGE(1404051);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_05 = new SM_SYSTEM_MESSAGE(1404052);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_03 = new SM_SYSTEM_MESSAGE(1404053);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_BossNamed_SpawnAlarm_1241_01 = new SM_SYSTEM_MESSAGE(1404054);

	/**
	 * [埃雷什基伽尔入侵] 4.9 / [Ereshkigal Invasion] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_Ere_MESSAGE_01 = new SM_SYSTEM_MESSAGE(1403069);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_Ere_MESSAGE_02 = new SM_SYSTEM_MESSAGE(1403070);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_Ere_MESSAGE_03 = new SM_SYSTEM_MESSAGE(1403071);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_Ere_MESSAGE_04 = new SM_SYSTEM_MESSAGE(1403072);
	public static final SM_SYSTEM_MESSAGE STR_MSG_WORLDRAID_Ere_MESSAGE_DIE_01 = new SM_SYSTEM_MESSAGE(1403073);

	/**
	 * [Abyss Landing] 4.9.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Fortress_Entrance_In01 = new SM_SYSTEM_MESSAGE(1403044);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Fortress_Entrance_In02 = new SM_SYSTEM_MESSAGE(1403045);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_Fail01 = new SM_SYSTEM_MESSAGE(1403046);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_Fail02 = new SM_SYSTEM_MESSAGE(1403047);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_Fail03 = new SM_SYSTEM_MESSAGE(1403048);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ABYSS_OP_LEVEL_UP_LIGHT = new SM_SYSTEM_MESSAGE(1403077);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ABYSS_OP_LEVEL_DOWN = new SM_SYSTEM_MESSAGE(1403078);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ABYSS_OP_LEVEL_UP_DARK = new SM_SYSTEM_MESSAGE(1403107);

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_POINT_QUEST_GAIN(int value0) {
		return new SM_SYSTEM_MESSAGE(1403172, value0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_QUEST(String value0) {
		return new SM_SYSTEM_MESSAGE(1403173, value0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY(DescriptionId value0,
			DescriptionId value1) {
		return new SM_SYSTEM_MESSAGE(1403174, value0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_KILL_NPC(DescriptionId value0,
			DescriptionId value1) {
		return new SM_SYSTEM_MESSAGE(1403175, value0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_DESTROY(DescriptionId value0,
			DescriptionId value1) {
		return new SM_SYSTEM_MESSAGE(1403176, value0, value1);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY_BASECAMP(DescriptionId value0,
			String value1) {
		return new SM_SYSTEM_MESSAGE(1403186, value0, value1);
	}

	/**
	 * [征服供奉] 4.9 / [Conquest Offering] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF4_Rotation_Spawn = new SM_SYSTEM_MESSAGE(1402928);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF4_Rotation_Spawn = new SM_SYSTEM_MESSAGE(1402929);
	public static final SM_SYSTEM_MESSAGE STR_MSG_BF4_Rotation_Buff_NPC_01 = new SM_SYSTEM_MESSAGE(1402890);

	/**
	 * [封印的达努阿尔秘术馆] 4.9 / [Sealed Danuar Mysticarium] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_Mini_01_Item01 = new SM_SYSTEM_MESSAGE(1402987);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF5_Mini_01_Item02 = new SM_SYSTEM_MESSAGE(1402988);

	/**
	 * [无限碎片] 4.3 / [Infinity Shard] 4.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDRuneWP_Charging = new SM_SYSTEM_MESSAGE(1401790);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDRuneWP_Charger1_Completed = new SM_SYSTEM_MESSAGE(1401791);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDRuneWP_Charger2_Completed = new SM_SYSTEM_MESSAGE(1401792);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDRuneWP_Charger3_Completed = new SM_SYSTEM_MESSAGE(1401793);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDRuneWP_Charger4_Completed = new SM_SYSTEM_MESSAGE(1401794);

	/**
	 * [西奥波莫斯实验室] 4.9 / [Theobomos Lab] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_TimeAttack_01 = new SM_SYSTEM_MESSAGE(1403033);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_Boss_Skill_01 = new SM_SYSTEM_MESSAGE(1403027);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_Boss_Skill_02 = new SM_SYSTEM_MESSAGE(1403036);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_Boss_Skill_03 = new SM_SYSTEM_MESSAGE(1403037);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_End_01 = new SM_SYSTEM_MESSAGE(1403034);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Teo_T_End_02 = new SM_SYSTEM_MESSAGE(1403042);

	/**
	 * [R.v.R] 4.9
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DIRECT_PORTAL_OPEN = new SM_SYSTEM_MESSAGE(1403215);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DIRECT_PORTAL_OPEN_NOTICE = new SM_SYSTEM_MESSAGE(1403222);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DIRECT_PORTAL = new SM_SYSTEM_MESSAGE(1403223);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_L_WIN = new SM_SYSTEM_MESSAGE(1403224);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_D_WIN = new SM_SYSTEM_MESSAGE(1403225);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DIRECT_PORTAL_OUT_NOTICE = new SM_SYSTEM_MESSAGE(1403228);
	public static final SM_SYSTEM_MESSAGE STR_CONFIRM_RVR_DIRECT_PORTAL_OUT = new SM_SYSTEM_MESSAGE(1403229);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403248);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403249);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403250);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_KILL_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403251);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_KILL_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403252);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_LF3_BOSS_KILL_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403253);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403254);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403255);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403256);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_KILL_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403257);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_KILL_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403258);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DF3_BOSS_KILL_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403259);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403275);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403276);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403277);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_04 = new SM_SYSTEM_MESSAGE(1403278);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_05 = new SM_SYSTEM_MESSAGE(1403279);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_TIMER_NOTICE_06 = new SM_SYSTEM_MESSAGE(1403280);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_CLOSING_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403281);
	public static final SM_SYSTEM_MESSAGE STR_MSG_RVR_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT = new SM_SYSTEM_MESSAGE(
			1403282);

	public static final SM_SYSTEM_MESSAGE STR_MSG_LEGION_APPLICATION_DENIED = new SM_SYSTEM_MESSAGE(1403109);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEGION_APPLICATION_ACCEPTED = new SM_SYSTEM_MESSAGE(1403110);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LEGION_JOIN_SERVER_CHANGE = new SM_SYSTEM_MESSAGE(1403111);

	/**
	 * @param value0 [高阶守护者] 5.0 / [ArchDeva] 5.0
	 */
	public static SM_SYSTEM_MESSAGE STR_CANNOT_USE_ITEM_INVALID_HIGHDEVA(String value0) {
		return new SM_SYSTEM_MESSAGE(1300896, value0);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ENCHANT_TYPE1_ENCHANT_FAIL(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1402674, descriptionId);
	}

	/**
	 * [龙视者之巢] 5.0 / [Drakenseer's Lair] 5.0
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_Dragon_Attack_Tower = new SM_SYSTEM_MESSAGE(1403378);
	public static final SM_SYSTEM_MESSAGE STR_MSG_NoticeFlyingArea = new SM_SYSTEM_MESSAGE(1401526);

	/**
	 * [战舰入侵] 5.0 / [Warship Invasion] 5.0
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_B_G2_Spawn = new SM_SYSTEM_MESSAGE(1403150);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_B2_G2_Spawn = new SM_SYSTEM_MESSAGE(1403151);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_C_G2_Spawn = new SM_SYSTEM_MESSAGE(1403152);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_D_G2_Spawn = new SM_SYSTEM_MESSAGE(1403153);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_E_G2_Spawn = new SM_SYSTEM_MESSAGE(1403154);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_F_G2_Spawn = new SM_SYSTEM_MESSAGE(1403155);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_F2_G2_Spawn = new SM_SYSTEM_MESSAGE(1403156);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G_G2_Spawn = new SM_SYSTEM_MESSAGE(1403157);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_H_G2_Spawn = new SM_SYSTEM_MESSAGE(1403158);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_I_G2_Spawn = new SM_SYSTEM_MESSAGE(1403159);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_B_G2_Spawn = new SM_SYSTEM_MESSAGE(1403160);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_B2_G2_Spawn = new SM_SYSTEM_MESSAGE(1403161);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_C_G2_Spawn = new SM_SYSTEM_MESSAGE(1403162);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_D_G2_Spawn = new SM_SYSTEM_MESSAGE(1403163);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_E_G2_Spawn = new SM_SYSTEM_MESSAGE(1403164);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_F_G2_Spawn = new SM_SYSTEM_MESSAGE(1403165);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_F2_G2_Spawn = new SM_SYSTEM_MESSAGE(1403166);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G_G2_Spawn = new SM_SYSTEM_MESSAGE(1403167);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_H_G2_Spawn = new SM_SYSTEM_MESSAGE(1403168);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_I_G2_Spawn = new SM_SYSTEM_MESSAGE(1403169);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Spawn_01 = new SM_SYSTEM_MESSAGE(1403178);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Spawn_02 = new SM_SYSTEM_MESSAGE(1403179);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Spawn_03 = new SM_SYSTEM_MESSAGE(1403180);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Spawn_04 = new SM_SYSTEM_MESSAGE(1403181);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Spawn_05 = new SM_SYSTEM_MESSAGE(1403170);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Spawn_01 = new SM_SYSTEM_MESSAGE(1403182);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Spawn_02 = new SM_SYSTEM_MESSAGE(1403183);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Spawn_03 = new SM_SYSTEM_MESSAGE(1403184);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Spawn_04 = new SM_SYSTEM_MESSAGE(1403185);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Spawn_05 = new SM_SYSTEM_MESSAGE(1403171);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G2_Event_Start = new SM_SYSTEM_MESSAGE(1403190);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G2_Event_Start = new SM_SYSTEM_MESSAGE(1403191);
	public static final SM_SYSTEM_MESSAGE STR_DF6_Event_G1_Defence_Failed = new SM_SYSTEM_MESSAGE(1403204);
	public static final SM_SYSTEM_MESSAGE STR_LF6_Event_G1_Defence_Failed = new SM_SYSTEM_MESSAGE(1403205);
	public static final SM_SYSTEM_MESSAGE STR_DF6_Event_G2_Start_01 = new SM_SYSTEM_MESSAGE(1403216);
	public static final SM_SYSTEM_MESSAGE STR_DF6_Event_G2_Start_02 = new SM_SYSTEM_MESSAGE(1403217);
	public static final SM_SYSTEM_MESSAGE STR_DF6_Event_G2_Start_03 = new SM_SYSTEM_MESSAGE(1403218);
	public static final SM_SYSTEM_MESSAGE STR_LF6_Event_G2_Start_01 = new SM_SYSTEM_MESSAGE(1403219);
	public static final SM_SYSTEM_MESSAGE STR_LF6_Event_G2_Start_02 = new SM_SYSTEM_MESSAGE(1403220);
	public static final SM_SYSTEM_MESSAGE STR_LF6_Event_G2_Start_03 = new SM_SYSTEM_MESSAGE(1403221);
	public static final SM_SYSTEM_MESSAGE STR_DF6_Event_G1_Wave_Start_01 = new SM_SYSTEM_MESSAGE(1403226);
	public static final SM_SYSTEM_MESSAGE STR_LF6_Event_G1_Wave_Start_01 = new SM_SYSTEM_MESSAGE(1403227);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Invasion_3rd_Bonus_01 = new SM_SYSTEM_MESSAGE(1403271);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Invasion_3rd_Bonus_02 = new SM_SYSTEM_MESSAGE(1403272);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Invasion_3rd_Bonus_03 = new SM_SYSTEM_MESSAGE(1403273);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Invasion_3rd_Bonus_04 = new SM_SYSTEM_MESSAGE(1403274);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Boss_Spawn_01 = new SM_SYSTEM_MESSAGE(1403290);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Boss_Spawn_01 = new SM_SYSTEM_MESSAGE(1403292);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_Evett_G1_Time_End_01 = new SM_SYSTEM_MESSAGE(1403367);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_Evett_G1_Time_End_02 = new SM_SYSTEM_MESSAGE(1403368);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_Evett_G1_Time_End_03 = new SM_SYSTEM_MESSAGE(1403369);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_Evett_G1_Time_End_01 = new SM_SYSTEM_MESSAGE(1403370);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_Evett_G1_Time_End_02 = new SM_SYSTEM_MESSAGE(1403371);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_Evett_G1_Time_End_03 = new SM_SYSTEM_MESSAGE(1403372);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Event_G1_Po_Time_Start_01 = new SM_SYSTEM_MESSAGE(1403373);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G1_Windpath_Off_01 = new SM_SYSTEM_MESSAGE(1403293);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G1_Windpath_Off_01 = new SM_SYSTEM_MESSAGE(1403291);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Windpath_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403288);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Windpath_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403289);
	// =========================================================================================================//
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_B_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404080);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_B2_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404081);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_C_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404082);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_D_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404083);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_E_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404084);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_F_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404085);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_F2_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404086);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_G_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404087);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_H_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404088);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_I_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404089);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_B_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404090);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_B2_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404091);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_C_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404092);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_D_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404093);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_E_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404094);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_F_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404095);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_F2_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404096);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_G_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404097);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_H_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404098);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_I_G2_Spawn_Chat_MSG = new SM_SYSTEM_MESSAGE(1404099);

	/**
	 * [永恒档案] 5.0 / [Archives Of Eternity] 5.0
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Shield = new SM_SYSTEM_MESSAGE(1403210);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Shield_Remove = new SM_SYSTEM_MESSAGE(1403211);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Artifact_Turn = new SM_SYSTEM_MESSAGE(1403212);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Artifact_Turn_On = new SM_SYSTEM_MESSAGE(1403213);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Artifact_Turn_Off = new SM_SYSTEM_MESSAGE(1403214);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Road_Set = new SM_SYSTEM_MESSAGE(1403235);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Artifact_Countdown = new SM_SYSTEM_MESSAGE(1403236);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Cannon = new SM_SYSTEM_MESSAGE(1403237);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Magic_Die = new SM_SYSTEM_MESSAGE(1403269);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Phy_Die = new SM_SYSTEM_MESSAGE(1403270);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_01_Cannon_Die = new SM_SYSTEM_MESSAGE(1403285);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_Warning = new SM_SYSTEM_MESSAGE(1403314);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Cannon_Warning = new SM_SYSTEM_MESSAGE(1403315);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_01_Turn_On = new SM_SYSTEM_MESSAGE(1403316);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_01_Turn_Off = new SM_SYSTEM_MESSAGE(1403317);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_02_Turn_On = new SM_SYSTEM_MESSAGE(1403318);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_02_Turn_Off = new SM_SYSTEM_MESSAGE(1403319);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_03_Turn_On = new SM_SYSTEM_MESSAGE(1403320);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_03_Turn_Off = new SM_SYSTEM_MESSAGE(1403321);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_04_Turn_On = new SM_SYSTEM_MESSAGE(1403322);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Artifact_04_Turn_Off = new SM_SYSTEM_MESSAGE(1403323);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Start = new SM_SYSTEM_MESSAGE(1403340);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_01_On = new SM_SYSTEM_MESSAGE(1403341);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_01_Off = new SM_SYSTEM_MESSAGE(1403342);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_02_On = new SM_SYSTEM_MESSAGE(1403343);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_02_Off = new SM_SYSTEM_MESSAGE(1403344);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_03_On = new SM_SYSTEM_MESSAGE(1403345);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_03_Off = new SM_SYSTEM_MESSAGE(1403346);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_04_On = new SM_SYSTEM_MESSAGE(1403347);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_01_Summon_Ctrl_04_Off = new SM_SYSTEM_MESSAGE(1403348);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_Teleport_MSG_01 = new SM_SYSTEM_MESSAGE(1404408);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_Teleport_MSG_02 = new SM_SYSTEM_MESSAGE(1404409);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_Teleport_MSG_03 = new SM_SYSTEM_MESSAGE(1404410);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_Teleport_MSG_04 = new SM_SYSTEM_MESSAGE(1404412);

	/**
	 * [永恒摇篮] 5.1 / [Cradle Of Eternity] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_01 = new SM_SYSTEM_MESSAGE(1403501);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_02 = new SM_SYSTEM_MESSAGE(1403502);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_03 = new SM_SYSTEM_MESSAGE(1403503);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_04 = new SM_SYSTEM_MESSAGE(1403504);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_11 = new SM_SYSTEM_MESSAGE(1403511);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_12 = new SM_SYSTEM_MESSAGE(1403512);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_13 = new SM_SYSTEM_MESSAGE(1403513);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_14 = new SM_SYSTEM_MESSAGE(1403514);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_34 = new SM_SYSTEM_MESSAGE(1403547);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_37 = new SM_SYSTEM_MESSAGE(1403589);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_SYSTEM_MSG_41 = new SM_SYSTEM_MESSAGE(1403606);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_Nepilim_Summon_MSG_01 = new SM_SYSTEM_MESSAGE(1403582);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_Nepilim_Summon_MSG_02 = new SM_SYSTEM_MESSAGE(1403583);
	public static final SM_SYSTEM_MESSAGE STR_IDEternity_02_Nepilim_Summon_MSG_03 = new SM_SYSTEM_MESSAGE(1403584);

	/**
	 * [奥菲丹战道] 5.1 / [Ophidan Warpath] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_war_Li_Da_Change = new SM_SYSTEM_MESSAGE(1403560);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_war_Da_Li_Change = new SM_SYSTEM_MESSAGE(1403561);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDLDF5_Under_02_war_neu_01 = new SM_SYSTEM_MESSAGE(1403586);

	/**
	 * [埃斯蒂玛] 5.1 / [Estima] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_WARNING_EQUIP_ITEM_ENCHANT_CPSTONE = new SM_SYSTEM_MESSAGE(1403639);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CPSTONE_OPEN_SLOT = new SM_SYSTEM_MESSAGE(1403684);

	/**
	 * [创造力面板] 5.1 / [Creativity Panel] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GIVE_CP_ENCHANT = new SM_SYSTEM_MESSAGE(1403230);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GIVE_CP_ENCHANT_CANNOT = new SM_SYSTEM_MESSAGE(1403231);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GIVE_CP_ENCHANT_NOT_ENOUGH = new SM_SYSTEM_MESSAGE(1403232);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GET_CP_LINK = new SM_SYSTEM_MESSAGE(1403233);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANNOT_USE_CP_UI_START_MENU = new SM_SYSTEM_MESSAGE(1403234);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GET_CP = new SM_SYSTEM_MESSAGE(1403350);

	public static SM_SYSTEM_MESSAGE STR_MSG_GET_CP(int point) {
		return new SM_SYSTEM_MESSAGE(1403350, point);
	}

	/**
	 * [Unbinding Item] 5.0
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_REMOVE_SOULBIND_SUCCEED(DescriptionId descriptionId) {
		return new SM_SYSTEM_MESSAGE(1403332, descriptionId);
	}

	/**
	 * @param points [伯丁之星 5.1] / [Berdin's Star 5.1]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_EXP_ABSOLUTEEXP_BONUS_50(long points) {
		return new SM_SYSTEM_MESSAGE(1403335, points);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_CHARGE_ABSOLUTEEXPPOINT_50(int percent) {
		return new SM_SYSTEM_MESSAGE(1403336, percent);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEEXP_BONUS_50_END = new SM_SYSTEM_MESSAGE(1403401);

	public static SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEEXP_BONUS_50_BOOST_ON(int percent, int boost) {
		return new SM_SYSTEM_MESSAGE(1403399, percent, boost);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEEXP_BONUS_50_BOOST_OFF(int percent) {
		return new SM_SYSTEM_MESSAGE(1403400, percent);
	}

	/**
	 * @param percent 欧比斯 Favor53 / [Abyss Favor 5.3]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_CHARGE_ABSOLUTEAPPOINT(int percent) {
		return new SM_SYSTEM_MESSAGE(1404028, percent);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEAP_BONUS_END = new SM_SYSTEM_MESSAGE(1404031);

	public static SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEAP_BONUS_BOOST_ON(int percent, int boost) {
		return new SM_SYSTEM_MESSAGE(1404029, percent, boost);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_ABSOLUTEAP_BONUS_BOOST_OFF(int percent) {
		return new SM_SYSTEM_MESSAGE(1404030, percent);
	}

	/**
	 * [Siege Occupy Count] 4.9.1
	 */
	public static SM_SYSTEM_MESSAGE STR_ABYSS_OCCUPY_COUNT_MAX(int value0) {
		return new SM_SYSTEM_MESSAGE(1402532, new DescriptionId(value0));
	}

	public static SM_SYSTEM_MESSAGE STR_ABYSS_OCCUPY_CHANGE_DRAGON(int value0) {
		return new SM_SYSTEM_MESSAGE(1402533, new DescriptionId(value0));
	}

	/**
	 * [露娜商店] 5.1 / [Luna Shop] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LUNA_REWARD_RESTRICTION_RIDE = new SM_SYSTEM_MESSAGE(1403487);
	public static final SM_SYSTEM_MESSAGE STR_LUNA_CRAFT_MATERIAL_INVENTORY_IS_FULL = new SM_SYSTEM_MESSAGE(1403585);

	public static SM_SYSTEM_MESSAGE STR_MSG_LUNA_REWARD_GOTCHA_ITEM_MULTI(long value1, int nameId) {
		return new SM_SYSTEM_MESSAGE(1403486, value1, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_LUNA_REWARD_GOTCHA_ITEM(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403485, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GETLUNA(String playerName, int count) {
		return new SM_SYSTEM_MESSAGE(1403973, playerName, count);
	}

	/**
	 * [陨落波埃塔] 5.1 / [Fallen Poeta] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Start_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403414);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Start_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403415);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Start_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403416);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Start_NOTICE_04 = new SM_SYSTEM_MESSAGE(1403417);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Play_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403418);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_End_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403419);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_End_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403420);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_End_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403421);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Tension_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403422);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Tension_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403423);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Tension_NOTICE_03 = new SM_SYSTEM_MESSAGE(1403424);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Retry_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403442);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Retry_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403443);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Success_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403444);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_Success_NOTICE_02 = new SM_SYSTEM_MESSAGE(1403445);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDF6_LF1_AnuHelp_NOTICE_01 = new SM_SYSTEM_MESSAGE(1403456);

	/**
	 * [遗忘裂隙] 5.1 / [Fissure Of Oblivion] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTransform_SavePoint_01 = new SM_SYSTEM_MESSAGE(1403667);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTransform_SavePoint_02 = new SM_SYSTEM_MESSAGE(1403668);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTransform_SavePoint_03 = new SM_SYSTEM_MESSAGE(1403669);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTransform_Sys_MSG_01 = new SM_SYSTEM_MESSAGE(1403698);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDTransform_Sys_MSG_02 = new SM_SYSTEM_MESSAGE(1403699);

	/**
	 * [Reduction Level] 5.1
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_WRONG_SEAL = new SM_SYSTEM_MESSAGE(1403593);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_WRONG_PACK = new SM_SYSTEM_MESSAGE(1403594);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_INVALID_STANCE = new SM_SYSTEM_MESSAGE(1403595);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_NO_TARGET_ITEM = new SM_SYSTEM_MESSAGE(1403596);
	public static final SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_NEED_IDENTIFY = new SM_SYSTEM_MESSAGE(1403597);

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_CANNOT(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403591, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_WRONG_MAX(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403592, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_SUCCEED(int nameId, int count) {
		return new SM_SYSTEM_MESSAGE(1403598, new DescriptionId(nameId), count);
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_FAIL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403599, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_CANCEL(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403600, new DescriptionId(nameId));
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_EQUIPLEVEL_ADJ_SUCCEED_MAX(int nameId) {
		return new SM_SYSTEM_MESSAGE(1403603, new DescriptionId(nameId));
	}

	/**
	 * [成长光环] / [Aura Of Growth]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_CHARGE_EXP_POINT = new SM_SYSTEM_MESSAGE(1403189);

	public static SM_SYSTEM_MESSAGE STR_MSG_CHARGE_EXP_POINT_NORMAL(int percent) {
		return new SM_SYSTEM_MESSAGE(1403374, percent);
	}

	/**
	 * 阿图拉姆空中要塞：击杀指挥区龙人后出现指挥官 / Aturam Sky Fortress
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Doping_01_AD = new SM_SYSTEM_MESSAGE(1401023);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_Doping_02_AD = new SM_SYSTEM_MESSAGE(1401024);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_A_FOBJ_SanctuaryATK = new SM_SYSTEM_MESSAGE(1402751);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_A_Zone_Explain_01 = new SM_SYSTEM_MESSAGE(1402755);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDStation_B_Zone_Explain_01 = new SM_SYSTEM_MESSAGE(1402756);

	/**
	 * [封印/解封] / [Seal/Unseal]
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_POLISH = new SM_SYSTEM_MESSAGE(1401659);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_IDENTIFY = new SM_SYSTEM_MESSAGE(1401660);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_AP_EXTRACTION = new SM_SYSTEM_MESSAGE(1401662);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_AUTHORIZE = new SM_SYSTEM_MESSAGE(1402164);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_AUTHORIZE_RESET = new SM_SYSTEM_MESSAGE(1402167);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ITEM_SEAL_WARNING_UP_TYPE_OPTION = new SM_SYSTEM_MESSAGE(1402246);

	/**
	 * 暴怒守护者 5.3 / Enraged Guardian 5.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_In_01 = new SM_SYSTEM_MESSAGE(1403756);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_In_02 = new SM_SYSTEM_MESSAGE(1403757);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Ab1_Named_Spawn_In_03 = new SM_SYSTEM_MESSAGE(1403758);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_01 = new SM_SYSTEM_MESSAGE(1403759);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_01 = new SM_SYSTEM_MESSAGE(1403760);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_01 = new SM_SYSTEM_MESSAGE(1403761);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_02 = new SM_SYSTEM_MESSAGE(1403762);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_02 = new SM_SYSTEM_MESSAGE(1403763);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_02 = new SM_SYSTEM_MESSAGE(1403764);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawn_03 = new SM_SYSTEM_MESSAGE(1403765);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Despawn_03 = new SM_SYSTEM_MESSAGE(1403766);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Die_03 = new SM_SYSTEM_MESSAGE(1403767);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawnmsg_01 = new SM_SYSTEM_MESSAGE(1403768);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawnmsg_02 = new SM_SYSTEM_MESSAGE(1403769);
	public static final SM_SYSTEM_MESSAGE STR_Ab1_BossNamed_65_Al_Spawnmsg_03 = new SM_SYSTEM_MESSAGE(1403770);

	/**
	 * 战场联合 5.3 / Battlefield Union 5.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Spawn_Li = new SM_SYSTEM_MESSAGE(1403828);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Spawn_Li = new SM_SYSTEM_MESSAGE(1403829);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Spawn_Li = new SM_SYSTEM_MESSAGE(1403830);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Spawn_Li = new SM_SYSTEM_MESSAGE(1403831);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Spawn_Li = new SM_SYSTEM_MESSAGE(1403832);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Spawn_Li = new SM_SYSTEM_MESSAGE(1403833);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Spawn_Da = new SM_SYSTEM_MESSAGE(1403834);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Spawn_Da = new SM_SYSTEM_MESSAGE(1403835);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Spawn_Da = new SM_SYSTEM_MESSAGE(1403836);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Spawn_Da = new SM_SYSTEM_MESSAGE(1403837);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Spawn_Da = new SM_SYSTEM_MESSAGE(1403838);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Spawn_Da = new SM_SYSTEM_MESSAGE(1403839);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403840);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403841);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403842);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403843);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403844);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403845);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Die_Li = new SM_SYSTEM_MESSAGE(1403846);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Die_Li = new SM_SYSTEM_MESSAGE(1403847);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Die_Li = new SM_SYSTEM_MESSAGE(1403848);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Die_Li = new SM_SYSTEM_MESSAGE(1403849);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Die_Li = new SM_SYSTEM_MESSAGE(1403850);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Die_Li = new SM_SYSTEM_MESSAGE(1403851);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Die_Da = new SM_SYSTEM_MESSAGE(1403852);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Die_Da = new SM_SYSTEM_MESSAGE(1403853);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Die_Da = new SM_SYSTEM_MESSAGE(1403854);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Die_Da = new SM_SYSTEM_MESSAGE(1403855);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Die_Da = new SM_SYSTEM_MESSAGE(1403856);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Die_Da = new SM_SYSTEM_MESSAGE(1403857);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_01_Die_Dr = new SM_SYSTEM_MESSAGE(1403858);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_02_Die_Dr = new SM_SYSTEM_MESSAGE(1403859);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_03_Die_Dr = new SM_SYSTEM_MESSAGE(1403860);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_04_Die_Dr = new SM_SYSTEM_MESSAGE(1403861);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_05_Die_Dr = new SM_SYSTEM_MESSAGE(1403862);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_commander_06_Die_Dr = new SM_SYSTEM_MESSAGE(1403863);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Spawn_Li = new SM_SYSTEM_MESSAGE(1403864);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Spawn_Li = new SM_SYSTEM_MESSAGE(1403865);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Spawn_Li = new SM_SYSTEM_MESSAGE(1403866);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Spawn_Li = new SM_SYSTEM_MESSAGE(1403867);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Spawn_Li = new SM_SYSTEM_MESSAGE(1403868);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Spawn_Li = new SM_SYSTEM_MESSAGE(1403869);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Spawn_Da = new SM_SYSTEM_MESSAGE(1403870);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Spawn_Da = new SM_SYSTEM_MESSAGE(1403871);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Spawn_Da = new SM_SYSTEM_MESSAGE(1403872);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Spawn_Da = new SM_SYSTEM_MESSAGE(1403873);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Spawn_Da = new SM_SYSTEM_MESSAGE(1403874);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Spawn_Da = new SM_SYSTEM_MESSAGE(1403875);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403876);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403877);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403878);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403879);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403880);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Spawn_Dr = new SM_SYSTEM_MESSAGE(1403881);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Die_Li = new SM_SYSTEM_MESSAGE(1403882);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Die_Li = new SM_SYSTEM_MESSAGE(1403883);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Die_Li = new SM_SYSTEM_MESSAGE(1403884);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Die_Li = new SM_SYSTEM_MESSAGE(1403885);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Die_Li = new SM_SYSTEM_MESSAGE(1403886);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Die_Li = new SM_SYSTEM_MESSAGE(1403887);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Die_Da = new SM_SYSTEM_MESSAGE(1403888);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Die_Da = new SM_SYSTEM_MESSAGE(1403889);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Die_Da = new SM_SYSTEM_MESSAGE(1403890);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Die_Da = new SM_SYSTEM_MESSAGE(1403891);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Die_Da = new SM_SYSTEM_MESSAGE(1403892);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Die_Da = new SM_SYSTEM_MESSAGE(1403893);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_01_Die_Dr = new SM_SYSTEM_MESSAGE(1403894);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_02_Die_Dr = new SM_SYSTEM_MESSAGE(1403895);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_03_Die_Dr = new SM_SYSTEM_MESSAGE(1403896);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_04_Die_Dr = new SM_SYSTEM_MESSAGE(1403897);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_05_Die_Dr = new SM_SYSTEM_MESSAGE(1403898);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_commander_06_Die_Dr = new SM_SYSTEM_MESSAGE(1403899);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_100th_01_Dr = new SM_SYSTEM_MESSAGE(1403908);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_100th_02_Dr = new SM_SYSTEM_MESSAGE(1403909);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_100th_03_Dr = new SM_SYSTEM_MESSAGE(1403910);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_100th_04_Dr = new SM_SYSTEM_MESSAGE(1403911);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_Spawn_01_Dr = new SM_SYSTEM_MESSAGE(1403943);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_Spawn_02_Dr = new SM_SYSTEM_MESSAGE(1403944);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_Spawn_03_Dr = new SM_SYSTEM_MESSAGE(1403945);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Dkisas_Position_Spawn_04_Dr = new SM_SYSTEM_MESSAGE(1403946);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1221_Boss_hide_Dispel = new SM_SYSTEM_MESSAGE(1403962);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1231_Boss_hide_Dispel = new SM_SYSTEM_MESSAGE(1403963);
	public static final SM_SYSTEM_MESSAGE STR_MSG_1241_Boss_hide_Dispel = new SM_SYSTEM_MESSAGE(1403964);

	/**
	 * 术古谈判者 5.3 / Shugo Negociator 5.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_War_Soon = new SM_SYSTEM_MESSAGE(1403729);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_01 = new SM_SYSTEM_MESSAGE(1403730);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_02 = new SM_SYSTEM_MESSAGE(1403731);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_03 = new SM_SYSTEM_MESSAGE(1403732);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_04 = new SM_SYSTEM_MESSAGE(1403733);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_05 = new SM_SYSTEM_MESSAGE(1403734);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuL_06 = new SM_SYSTEM_MESSAGE(1403735);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_01 = new SM_SYSTEM_MESSAGE(1403736);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_02 = new SM_SYSTEM_MESSAGE(1403737);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_03 = new SM_SYSTEM_MESSAGE(1403738);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_04 = new SM_SYSTEM_MESSAGE(1403739);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_05 = new SM_SYSTEM_MESSAGE(1403740);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuD_06 = new SM_SYSTEM_MESSAGE(1403741);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_01 = new SM_SYSTEM_MESSAGE(1403742);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_02 = new SM_SYSTEM_MESSAGE(1403743);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_03 = new SM_SYSTEM_MESSAGE(1403744);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_04 = new SM_SYSTEM_MESSAGE(1403745);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_05 = new SM_SYSTEM_MESSAGE(1403746);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_Buff_06 = new SM_SYSTEM_MESSAGE(1403747);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_L_01 = new SM_SYSTEM_MESSAGE(1403750);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_L_02 = new SM_SYSTEM_MESSAGE(1403751);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_L_03 = new SM_SYSTEM_MESSAGE(1403752);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_D_01 = new SM_SYSTEM_MESSAGE(1403753);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_D_02 = new SM_SYSTEM_MESSAGE(1403754);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoSoldier_D_03 = new SM_SYSTEM_MESSAGE(1403755);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_01 = new SM_SYSTEM_MESSAGE(1403806);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_02 = new SM_SYSTEM_MESSAGE(1403807);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_03 = new SM_SYSTEM_MESSAGE(1403808);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_04 = new SM_SYSTEM_MESSAGE(1403809);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_05 = new SM_SYSTEM_MESSAGE(1403810);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_OccuDr_06 = new SM_SYSTEM_MESSAGE(1403811);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_01 = new SM_SYSTEM_MESSAGE(1403812);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_02 = new SM_SYSTEM_MESSAGE(1403813);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_03 = new SM_SYSTEM_MESSAGE(1403814);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_04 = new SM_SYSTEM_MESSAGE(1403815);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_05 = new SM_SYSTEM_MESSAGE(1403816);
	public static final SM_SYSTEM_MESSAGE STR_MSG_ShugoShip_AtkDr_06 = new SM_SYSTEM_MESSAGE(1403817);

	/**
	 * 古代机械 5.5 / Ancient Machines 5.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_LF6_5minute_MSG = new SM_SYSTEM_MESSAGE(1404113);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_DF6_5minute_MSG = new SM_SYSTEM_MESSAGE(1404114);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_Start_MSG = new SM_SYSTEM_MESSAGE(1404115);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_End_MSG = new SM_SYSTEM_MESSAGE(1404116);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Warning_MSG = new SM_SYSTEM_MESSAGE(1404126);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_BossSpawn__MSG = new SM_SYSTEM_MESSAGE(1404127);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_LF6_Attack_MSG = new SM_SYSTEM_MESSAGE(1404161);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_DF6_Attack_MSG = new SM_SYSTEM_MESSAGE(1404162);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Spawn_Start_Dark_MSG = new SM_SYSTEM_MESSAGE(1404199);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_ST_BossSpawn_MSG = new SM_SYSTEM_MESSAGE(1404206);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_ST_Dark_BossSpawn_MSG = new SM_SYSTEM_MESSAGE(1404207);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Start_LF6 = new SM_SYSTEM_MESSAGE(1404241);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Start_DF6 = new SM_SYSTEM_MESSAGE(1404242);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_InvasionStart_Light = new SM_SYSTEM_MESSAGE(1404243);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_InvasionStart_Dark = new SM_SYSTEM_MESSAGE(1404244);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Sumatta01_Light = new SM_SYSTEM_MESSAGE(1404263);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_Sumatta01_Dark = new SM_SYSTEM_MESSAGE(1404264);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill01_Light = new SM_SYSTEM_MESSAGE(1404266);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill01_Dark = new SM_SYSTEM_MESSAGE(1404267);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill02_Dark = new SM_SYSTEM_MESSAGE(1404268);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill03_Dark = new SM_SYSTEM_MESSAGE(1404269);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill04_Dark = new SM_SYSTEM_MESSAGE(1404270);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill02_Light = new SM_SYSTEM_MESSAGE(1404271);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill03_Light = new SM_SYSTEM_MESSAGE(1404272);
	public static final SM_SYSTEM_MESSAGE STR_MSG_F6_Raid_SumKill04_Light = new SM_SYSTEM_MESSAGE(1404273);

	/**
	 * 升级街机 / Upgrade Arcade
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_GACHA_ITEM_CHECK = new SM_SYSTEM_MESSAGE(1402581,
			"[item:" + 186000389 + "]");

	public static SM_SYSTEM_MESSAGE STR_MSG_GACHA_ITEM_REWARD(int itemId) {
		return new SM_SYSTEM_MESSAGE(1402582, "[item:" + itemId + "]");
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GACHA_ITEM_REWARD_MULTI(int itemId, int count) {
		return new SM_SYSTEM_MESSAGE(1402583, "[item:" + itemId + "]", count);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_GACHA_FEVERTIME_START = new SM_SYSTEM_MESSAGE(1402608);
	public static final SM_SYSTEM_MESSAGE STR_MSG_GACHA_TIME_END = new SM_SYSTEM_MESSAGE(14025858);

	public static SM_SYSTEM_MESSAGE STR_MSG_GACHA_FEVER_ITEM_REWARD(int itemId) {
		return new SM_SYSTEM_MESSAGE(1403137, "[item:" + itemId + "]");
	}

	public static SM_SYSTEM_MESSAGE STR_MSG_GACHA_FEVER_ITEM_REWARD_MULTI(int itemId, int count) {
		return new SM_SYSTEM_MESSAGE(1403138, "[item:" + itemId + "]", count);
	}

	/**
	 * [永风峡谷] 5.5 / [Evergale Canyon] 5.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_01 = new SM_SYSTEM_MESSAGE(1404164);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_02 = new SM_SYSTEM_MESSAGE(1404165);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_03 = new SM_SYSTEM_MESSAGE(1404166);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_04 = new SM_SYSTEM_MESSAGE(1404167);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_05 = new SM_SYSTEM_MESSAGE(1404168);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_06 = new SM_SYSTEM_MESSAGE(1404169);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_07 = new SM_SYSTEM_MESSAGE(1404170);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_08 = new SM_SYSTEM_MESSAGE(1404171);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_09 = new SM_SYSTEM_MESSAGE(1404193);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_10 = new SM_SYSTEM_MESSAGE(1404194);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_11 = new SM_SYSTEM_MESSAGE(1404195);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_War_tele_12 = new SM_SYSTEM_MESSAGE(1404196);

	/**
	 * [永恒试炼] 5.5 / [Trials Of Eternity] 5.5
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone1_book_01 = new SM_SYSTEM_MESSAGE(1404076);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone2_book_01 = new SM_SYSTEM_MESSAGE(1404077);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone3_book_01 = new SM_SYSTEM_MESSAGE(1404078);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone4_book_01 = new SM_SYSTEM_MESSAGE(1404079);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone1_book_02 = new SM_SYSTEM_MESSAGE(1404100);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone2_book_02 = new SM_SYSTEM_MESSAGE(1404101);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone3_book_02 = new SM_SYSTEM_MESSAGE(1404102);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone4_book_02 = new SM_SYSTEM_MESSAGE(1404103);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone1_book_03 = new SM_SYSTEM_MESSAGE(1404104);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone2_book_03 = new SM_SYSTEM_MESSAGE(1404105);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone3_book_03 = new SM_SYSTEM_MESSAGE(1404106);
	public static final SM_SYSTEM_MESSAGE STR_MSG_junk_ideternity03_zone4_book_03 = new SM_SYSTEM_MESSAGE(1404107);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_teleporter_1 = new SM_SYSTEM_MESSAGE(1404156);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_teleporter_2 = new SM_SYSTEM_MESSAGE(1404157);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_teleporter_3 = new SM_SYSTEM_MESSAGE(1404158);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_teleporter_4 = new SM_SYSTEM_MESSAGE(1404159);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_teleporter_5 = new SM_SYSTEM_MESSAGE(1404160);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_Dimension_01 = new SM_SYSTEM_MESSAGE(1404259);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_Dimension_02 = new SM_SYSTEM_MESSAGE(1404260);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_Dimension_03 = new SM_SYSTEM_MESSAGE(1404300);
	public static final SM_SYSTEM_MESSAGE STR_MSG_IDEternity_03_Dimension_04 = new SM_SYSTEM_MESSAGE(1404379);

	/**
	 * [阿特雷亚图鉴] 5.3 / [Atreian Bestiary] 5.3
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_MONSTER_ACHIEVEMENT_GET = new SM_SYSTEM_MESSAGE(1404072);
	public static final SM_SYSTEM_MESSAGE STR_MSG_MONSTER_ACHIEVEMENT_COMPLETION = new SM_SYSTEM_MESSAGE(1404073);
	public static final SM_SYSTEM_MESSAGE STR_MSG_MONSTER_ACHIEVEMENT_COMPLETION2 = new SM_SYSTEM_MESSAGE(1404074);

	/**
	 * @param exp [随从 5.5] / [Minions 5.5]
	 */
	public static SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_RESULT_GROWTH(int exp) {
		return new SM_SYSTEM_MESSAGE(1404367, exp);
	}

	public static SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_GET_FAMILIAR(int minionNameid) {
		return new SM_SYSTEM_MESSAGE(1404316, new DescriptionId(minionNameid));
	}

	public static SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_DELETE_CONFIRM(String minionName) {
		return new SM_SYSTEM_MESSAGE(1404341, minionName);
	}

	public static SM_SYSTEM_MESSAGE STR_FAMILIAR_EVOLVE_MSG_EVOLVE_RESULT(int minionNameid, int level) {
		return new SM_SYSTEM_MESSAGE(1404316, new DescriptionId(minionNameid), level);
	}

	/**
	 * @param skillname Skin 技能 / [Skin Skill]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_DELETE_COSTUME_SKILL_BY_TIMEOUT(String skillname) {
		return new SM_SYSTEM_MESSAGE(1403682, skillname);
	}

	public static final SM_SYSTEM_MESSAGE STR_MSG_COSTUME_SKILL_ALREADY_HAS_COSTUME = new SM_SYSTEM_MESSAGE(1403683);

	/**
	 * @param nameId [神圣能量] / [Holy Energy]
	 */
	public static SM_SYSTEM_MESSAGE STR_MSG_GET_HCOIN_07(int nameId) {
		return new SM_SYSTEM_MESSAGE(1401496, new DescriptionId(nameId));
	}

	/**
	 * 伊卢玛/诺斯沃尔德神器 5.8 / Iluma/Norsvold Artifact 5.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_Occupy_All_Start_MSG = new SM_SYSTEM_MESSAGE(1404553);
	public static final SM_SYSTEM_MESSAGE STR_MSG_LF6_Occupy_All_End_MSG = new SM_SYSTEM_MESSAGE(1404554);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_Occupy_All_Start_MSG = new SM_SYSTEM_MESSAGE(1404555);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DF6_Occupy_All_End_MSG = new SM_SYSTEM_MESSAGE(1404556);

	/**
	 * 提亚玛兰塔之眼 5.8 / Tiamaranta's Eye 5.8
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_TIAMATAVATAR_WAKEUP = new SM_SYSTEM_MESSAGE(1401467);
	public static final SM_SYSTEM_MESSAGE STR_MSG_TIAMATDOWN_USERKICK_MESSAGE = new SM_SYSTEM_MESSAGE(1401469);

	/**
	 * 阿特雷亚护照 / Atreian Passport
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_NEW_PASSPORT_AVAIBLE = new SM_SYSTEM_MESSAGE(1402601);

	/**
	 * 随从 / Minions
	 */
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_GET_FAMILIAR = new SM_SYSTEM_MESSAGE(1404316);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_COMBAT_MODE = new SM_SYSTEM_MESSAGE(1404317);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_RIDE_MODE = new SM_SYSTEM_MESSAGE(1404318);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_CURRENT_STATE = new SM_SYSTEM_MESSAGE(
			1404319);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_CURRENT_POSITION = new SM_SYSTEM_MESSAGE(
			1404320);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_CURRENT_MOTION = new SM_SYSTEM_MESSAGE(
			1404321);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_CONTRACT_BY_MAXUNIT = new SM_SYSTEM_MESSAGE(1404322);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANCEL_CONTRACT = new SM_SYSTEM_MESSAGE(1404323);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CALL_FAMILIAR = new SM_SYSTEM_MESSAGE(1404324);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_DISMISS_FAMILIAR = new SM_SYSTEM_MESSAGE(1404325);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_CANNOT_USE_FSKILL_BY_LACK_FENERGY = new SM_SYSTEM_MESSAGE(
			1404326);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FENERGY_CHARGE = new SM_SYSTEM_MESSAGE(1404327);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FENERGY_AUTOCHARGING = new SM_SYSTEM_MESSAGE(1404328);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FENERGY_AUTOCHARGING_NOTICE = new SM_SYSTEM_MESSAGE(1404329);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FENERGY_AUTOCHARGING_FAIL_BY_GOLD = new SM_SYSTEM_MESSAGE(
			1404330);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FENERGY_CHARGE_FAIL_BY_AUTOCHARGE = new SM_SYSTEM_MESSAGE(
			1404331);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FFUNCTION_END = new SM_SYSTEM_MESSAGE(1404332);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FFUNCTION_AUTOCONTINUE_NOTICE = new SM_SYSTEM_MESSAGE(
			1404333);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FFUNCTION_AUTOCONTINUE_FAIL_BY_GOLD = new SM_SYSTEM_MESSAGE(
			1404334);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FAIL_CHANGE_NAME_OVERLAB = new SM_SYSTEM_MESSAGE(1404335);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FAIL_CHANGE_NAME_OVERLENGTH = new SM_SYSTEM_MESSAGE(1404336);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FAIL_CHANGE_NAME_CALLING = new SM_SYSTEM_MESSAGE(1404337);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FAIL_CHANGE_NAME_CONFIRM = new SM_SYSTEM_MESSAGE(1404338);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_UNLOCK_CONFIRM = new SM_SYSTEM_MESSAGE(1404339);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FAIL_DELETE_CALLING = new SM_SYSTEM_MESSAGE(1404340);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_DELETE_CONFIRM = new SM_SYSTEM_MESSAGE(1404341);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_MSG_FFUNCTION_USE_FAIL_BY_GOLD = new SM_SYSTEM_MESSAGE(1404342);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_GROWTH_MSG_OVERSELECT = new SM_SYSTEM_MESSAGE(1404343);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_GROWTH_MSG_OVERGROWTH = new SM_SYSTEM_MESSAGE(1404344);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_GROWTH_MSG_NOTSELECT = new SM_SYSTEM_MESSAGE(1404345);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_GROWTH_MSG_NOGOLD = new SM_SYSTEM_MESSAGE(1404346);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_EVOLVE_MSG_LACK_ITEM = new SM_SYSTEM_MESSAGE(1404347);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_EVOLVE_MSG_NOGOLD = new SM_SYSTEM_MESSAGE(1404348);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_EVOLVE_MSG_NOEVOLVE = new SM_SYSTEM_MESSAGE(1404349);
	public static final SM_SYSTEM_MESSAGE STR_FAMILIAR_EVOLVE_MSG_EVOLVE_RESULT = new SM_SYSTEM_MESSAGE(1404350);
	public static final SM_SYSTEM_MESSAGE STR_MSG_CANT_ENCHANT_EQUIPED = new SM_SYSTEM_MESSAGE(1403358);

	/**
	 * 游戏内自定义消息（多语言 L10n）。 / Кастомные сообщения в Игре - Чтобы сообщения были на разном языке (L10n)
	 */
	public static final SM_SYSTEM_MESSAGE STR_MSG_Sunayaka_Spawn_v1 = new SM_SYSTEM_MESSAGE(1404567);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Sunayaka_DeSpawn_v1 = new SM_SYSTEM_MESSAGE(1404568);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Welcome_message_v1 = new SM_SYSTEM_MESSAGE(1404569);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DIFLODOX_Spawn_v1 = new SM_SYSTEM_MESSAGE(1404570);
	public static final SM_SYSTEM_MESSAGE STR_MSG_DIFLODOX_DeSpawn_v1 = new SM_SYSTEM_MESSAGE(1404571);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Diflonax_Spawn_v1 = new SM_SYSTEM_MESSAGE(1404572);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Diflonax_DeSpawn_v1 = new SM_SYSTEM_MESSAGE(1404573);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Menotios_Spawn_v1 = new SM_SYSTEM_MESSAGE(1404574);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Menotios_DeSpawn_v1 = new SM_SYSTEM_MESSAGE(1404575);
	public static final SM_SYSTEM_MESSAGE STR_MSG_TiaOko_Spawn_v1 = new SM_SYSTEM_MESSAGE(1404576);
	public static final SM_SYSTEM_MESSAGE STR_MSG_TiaOko_DeSpawn_v1 = new SM_SYSTEM_MESSAGE(1404577);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Haramel_BossSpawn_v1 = new SM_SYSTEM_MESSAGE(1404578);
	public static final SM_SYSTEM_MESSAGE STR_MSG_Haramel_FinishInstance_v1 = new SM_SYSTEM_MESSAGE(1404579);

	/**
	 * 游戏内自定义消息（多语言 L10n）。 / Кастомные сообщения в Игре - Чтобы сообщения были на разном языке (L10n)
	 */

	private final int code;
	private final Object[] params;
	private boolean npcShout = false;
	private int npcObjId = 0;
	private int textColorId = 26;

	private static DescriptionId calculateNameId(int id) {
		return new DescriptionId(id * 2 + 1);
	}

	public SM_SYSTEM_MESSAGE(int code, Object... params) {
		this.code = code;
		this.params = params;
	}

	public SM_SYSTEM_MESSAGE(boolean npcShout, int code, int npcObjId, int textColorId, Object... params) {
		this.npcShout = npcShout;
		this.code = code;
		this.npcObjId = npcObjId;
		this.textColorId = textColorId;
		this.params = params;
	}

	public SM_SYSTEM_MESSAGE(int code, Creature creature, Object... params) {
		this.code = code;
		this.npcObjId = creature.getObjectId();
		this.params = params;
	}

	public SM_SYSTEM_MESSAGE(SystemMessageId sm, Object... params) {
		this.code = sm.getId();
		this.params = params;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(textColorId);
		writeC(0x00);
		writeD(npcObjId);
		writeD(code);
		writeC(params.length);
		for (Object param : params) {
			if (param instanceof DescriptionId) {
				writeH(0x24);
				writeD(((DescriptionId) param).getValue());
				writeH(0x00);
			} else {
				writeS(String.valueOf(param));
			}
		}
		if (npcShout) {
			writeC(0x01);
		} else {
			writeC(0x00);
		}
	}
}
