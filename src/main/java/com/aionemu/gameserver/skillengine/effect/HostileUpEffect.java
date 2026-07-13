package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 仇恨提升效果：立即增加目标对施法者的仇恨值。
 * Hostile-up effect: immediately increases the target's hate toward the caster.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HostileUpEffect")
public class HostileUpEffect extends EffectTemplate {
	@XmlAttribute(name = "timed_delta")
	private int timedDelta;
	@XmlAttribute(name = "timed_value")
	private int timedValue;
	@XmlAttribute(name = "timed_duration")
	private int timedDuration;
	@XmlAttribute(name = "split_totem_hate")
	private boolean splitTotemHate;

	public int getTimedDelta() {
		return timedDelta;
	}

	public int getTimedValue() {
		return timedValue;
	}

	public int getTimedDuration() {
		return timedDuration;
	}

	public boolean isSplitTotemHate() {
		return splitTotemHate;
	}

	/**
	 * 提升目标对施法者的仇恨。
	 * Increases the target's hate toward the caster.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effected = effect.getEffected();
		if (effected instanceof Npc npc) {
			addHate(npc, effect.getEffector(), effect.getTauntHate(), 0);
			addHate(npc, effect.getEffector(), timedValue + timedDelta * effect.getSkillLevel(), timedDuration);
		}
	}

	private void addHate(Npc target, Creature source, int hate, int duration) {
		if (hate == 0) {
			return;
		}
		if (splitTotemHate && source instanceof Servant servant
				&& servant.getNpcObjectType() == NpcObjectType.TOTEM && servant.getCreator() != null) {
			int servantHate = totemHate(hate);
			applyHate(target, servant, servantHate, duration);
			applyHate(target, servant.getCreator(), hate - servantHate, duration);
			return;
		}
		applyHate(target, source, hate, duration);
	}

	private static void applyHate(Npc target, Creature source, int hate, int duration) {
		if (duration > 0) {
			target.getAggroList().addHate(source, hate, duration);
		} else {
			target.getAggroList().addHate(source, hate);
		}
	}

	static int totemHate(int hate) {
		return (int) (hate * 0.99f);
	}

	/**
	 * 计算本效果是否成功命中/生效，并写入效果上下文。
	 * Calculates whether this effect succeeds and writes into the effect context.
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, null, null)) {
			return;
		}
		effect.setTauntHate(value + delta * effect.getSkillLevel());
	}
}
