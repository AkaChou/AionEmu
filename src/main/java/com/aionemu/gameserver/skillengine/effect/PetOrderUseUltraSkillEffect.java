package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_USESKILL;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物释放奥义指令：命令召唤物使用超级技能，可选释放后解散。
 * Pet ultra-skill order: commands the summon to use an ultra skill; optional release.
 *
 * @author ATracer
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetOrderUseUltraSkillEffect")
public class PetOrderUseUltraSkillEffect extends EffectTemplate {

	@XmlAttribute
	protected boolean release;

	/**
	 * 命令宠物使用奥义技能。
	 * Orders the pet to use its ultra skill.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Player effector = (Player) effect.getEffector();

		if (effector.getSummon() == null) {
			return;
		}

		int effectorId = effector.getSummon().getObjectId();

		int npcId = effector.getSummon().getNpcId();
		int orderSkillId = effect.getSkillId();

		int petUseSkillId = DataManager.PET_SKILL_DATA.getPetOrderSkill(orderSkillId, npcId);
		int targetId = effect.getEffected().getObjectId();

		// 若技能需要则处理自动释放 / Handle automatic release if skill expects so
		if (release) {
			SummonController controller = effector.getSummon().getController();
			if ((controller instanceof SummonController)) {
				effector.getSummon().getController().setReleaseAfterSkill(petUseSkillId);
			}
		}
		PacketSendUtility.sendPacket(effector, new SM_SUMMON_USESKILL(effectorId, petUseSkillId, 1, targetId));
	}

	/**
	 * 校验宠物奥义指令是否可执行。
	 * Validates whether the pet ultra-skill order can run.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffector() instanceof Player && effect.getEffected() != null) {
			super.calculate(effect, null, null);
		}
	}
}
