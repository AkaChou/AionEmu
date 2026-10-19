package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 雕刻印记效果：造成物理伤害并叠加/刷新目标印记层数。
 * Carve-signet effect: deals physical damage and stacks/refreshes signet levels on the target.
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CarveSignetEffect")
public class CarveSignetEffect extends DamageEffect {
	/**
	 * 每次刻印向上推进的层数（真端 R10 carveSignetHopLevel），非起始层数。
	 * Signet levels gained per carve (retail R10 carveSignetHopLevel), not a starting level.
	 */
	@XmlAttribute(required = true)
	protected int signetlvlstart;

	/**
	 * 可刻印到的最高层数（真端 R14 maxCarveSignetLevel）。
	 * Highest signet level this skill can carve up to (retail R14 maxCarveSignetLevel).
	 */
	@XmlAttribute(required = true)
	protected int signetlvl;

	@XmlAttribute(required = true)
	protected int signetid;

	@XmlAttribute(name = "signet_type")
	protected int signetType;
	@XmlAttribute
	protected String signet;

	@XmlAttribute
	protected int prob = 100;
	@XmlAttribute(name = "prob_delta")
	protected int probDelta;

	/**
	 * 应用伤害，并按概率叠加下一级印记技能。
	 * Applies damage and, by chance, stacks the next signet skill level.
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		if (!Rnd.chance(prob + probDelta * effect.getSkillLevel())) {
			return;
		}
		Effect placedSignet = signetType == 0 ? effect.getEffected().getEffectController().getAnormalEffect(signet)
				: effect.getEffected().getEffectController().getAbnormalEffects().stream()
						.filter(active -> active.getEffectTemplates().stream()
								.anyMatch(template -> template instanceof SignetEffect signetEffect
										&& signetEffect.getSignetType() == signetType))
						.findFirst().orElse(null);
		int nextSignetLevel = nextSignetLevel(0, signetlvl, signetlvlstart);
		if (placedSignet != null) {
			for (EffectTemplate template : placedSignet.getEffectTemplates()) {
				if (template instanceof SignetEffect signetEffect
						&& (signetType == 0 || signetEffect.getSignetType() == signetType)) {
					int currentLevel = signetEffect.getSignetLevel() > 0
							? signetEffect.getSignetLevel() : placedSignet.getSkillLevel();
					nextSignetLevel = nextSignetLevel(currentLevel, signetlvl, signetlvlstart);
					break;
				}
			}
			placedSignet.endEffect();
		}
		effect.setCarvedSignet(nextSignetLevel);
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(nextSignetSkillId(signetid, signetlvl, nextSignetLevel));
		Effect newEffect = new Effect(effect.getEffector(), effect.getEffected(), template, effect.getCarvedSignet(),
				0);
		newEffect.initialize();
		newEffect.applyEffect();
	}

	static int nextSignetSkillId(int maxSignetSkillId, int maxSignetLevel, int nextSignetLevel) {
		return maxSignetSkillId - maxSignetLevel + nextSignetLevel;
	}

	/**
	 * 按真端公式推进纹样层数：当前层数加上本次刻印步进，封顶于最高层数。
	 * Advances the signet level by the retail formula: current level plus this carve's hop, capped at max level.
	 * <p>
	 * 真端 {@code SkillEffectCarveSignet_Instant::BeginEffect}：
	 * {@code next = min(maxCarveSignetLevel, carveSignetHopLevel + current)}，
	 * 其中 current 为已挂纹样层数与刻印进度两者取大；无纹样时 current 为 0。
	 * Retail caps at R14 and steps by R10, taking the larger of the placed signet level and carve progress.
	 * @param currentLevel 当前已挂纹样层数，无纹样为 0 / current placed signet level, 0 when none
	 * @param maxLevel 本次可刻印到的最高层数（R14）/ highest level this skill can carve to (R14)
	 * @param hop 本次刻印的层数步进（R10）/ levels gained per carve (R10)
	 * @return 本次刻印后的层数，至少为 1 / resulting level, at least 1
	 */
	static int nextSignetLevel(int currentLevel, int maxLevel, int hop) {
		return Math.max(1, Math.min(maxLevel, currentLevel + hop));
	}

	/**
	 * 按物理伤害结算命中，命中后才登记为成功效果。
	 * Resolves the physical hit; only a successful hit is registered as a success effect.
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.PHYSICAL);
	}
}
