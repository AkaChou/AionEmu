package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.effect.AbsoluteSlowEffect;
import com.aionemu.gameserver.skillengine.effect.AbsoluteSnareEffect;
import com.aionemu.gameserver.skillengine.effect.BuffStunEffect;
import com.aionemu.gameserver.skillengine.effect.DispelEffect;
import com.aionemu.gameserver.skillengine.effect.EscapeEffect;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.HealEffect;
import com.aionemu.gameserver.skillengine.effect.HostileUpEffect;
import com.aionemu.gameserver.skillengine.effect.InvulnerableWingEffect;
import com.aionemu.gameserver.skillengine.effect.MPHealInstantEffect;
import com.aionemu.gameserver.skillengine.effect.NoReduceSpellATKInstantEffect;
import com.aionemu.gameserver.skillengine.effect.ParalyzeEffect;
import com.aionemu.gameserver.skillengine.effect.PetrificationEffect;
import com.aionemu.gameserver.skillengine.effect.ProcAtkInstantEffect;
import com.aionemu.gameserver.skillengine.effect.ProvokerEffect;
import com.aionemu.gameserver.skillengine.effect.ReturnPointEffect;
import com.aionemu.gameserver.skillengine.effect.ShapeChangeEffect;
import com.aionemu.gameserver.skillengine.effect.ShieldEffect;
import com.aionemu.gameserver.skillengine.effect.SimpleRootEffect;
import com.aionemu.gameserver.skillengine.effect.SnareEffect;
import com.aionemu.gameserver.skillengine.effect.SwitchHostileEffect;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class RetailSkillTemplateFileTest {

	@Test
	void generatedRetailTemplatesValidateAndLoadIntoSkillData() throws Exception {
		String source = System.getProperty("retail.skill.templates");
		Assumptions.assumeTrue(source != null, "set -Dretail.skill.templates to run generated-data integration test");
		Path sourcePath = Path.of(source);

		SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
			.newSchema(Path.of("src/main/resources/aion/definitions/schemas/skills.xsd").toFile())
			.newValidator()
			.validate(new StreamSource(sourcePath.toFile()));

		SkillData data = (SkillData) JAXBContext.newInstance(SkillData.class)
			.createUnmarshaller()
			.unmarshal(sourcePath.toFile());

		assertEquals(Integer.getInteger("retail.skill.count", 14457).intValue(), data.size());
		assertEquals(Integer.getInteger("retail.shapechange.count", 531).longValue(), countEffects(data, ShapeChangeEffect.class));
		assertEquals(Integer.getInteger("retail.snare.count", 495).longValue(), countEffects(data, SnareEffect.class));
		assertEquals(Integer.getInteger("retail.shield.count", 486).longValue(), countEffects(data, ShieldEffect.class));
		assertEquals(Integer.getInteger("retail.dispel.count", 338).longValue(), countEffects(data, DispelEffect.class));
		assertEquals(Integer.getInteger("retail.procatk.count", 316).longValue(), countEffects(data, ProcAtkInstantEffect.class));
		assertEquals(Integer.getInteger("retail.hostileup.count", 308).longValue(), countEffects(data, HostileUpEffect.class));
		assertEquals(Integer.getInteger("retail.heal.count", 249).longValue(), countEffects(data, HealEffect.class));
		assertEquals(Integer.getInteger("retail.noreducespellatk.count", 236).longValue(),
			countEffects(data, NoReduceSpellATKInstantEffect.class));
		assertEquals(Integer.getInteger("retail.provoker.count", 276).longValue(), countEffects(data, ProvokerEffect.class));
		assertEquals(Integer.getInteger("retail.mphealinstant.count", 215).longValue(),
			countEffects(data, MPHealInstantEffect.class));
		assertEquals(Integer.getInteger("retail.paralyze.count", 181).longValue(),
			countEffects(data, ParalyzeEffect.class));
		assertEquals(Integer.getInteger("retail.petrification.count", 2).longValue(),
			countEffects(data, PetrificationEffect.class));
		assertEquals(Integer.getInteger("retail.simpleroot.count", 1).longValue(),
			countEffects(data, SimpleRootEffect.class));
		assertEquals(Integer.getInteger("retail.invulnerablewing.count", 1).longValue(),
			countEffects(data, InvulnerableWingEffect.class));
		assertEquals(Integer.getInteger("retail.escape.count", 1).longValue(),
			countEffects(data, EscapeEffect.class));
		assertEquals(Integer.getInteger("retail.returnpoint.count", 1).longValue(),
			countEffects(data, ReturnPointEffect.class));
		assertEquals(Integer.getInteger("retail.switchhostile.count", 2).longValue(),
			countEffects(data, SwitchHostileEffect.class));
		assertEquals(Integer.getInteger("retail.absoluteslow.count", 1).longValue(),
			countEffects(data, AbsoluteSlowEffect.class));
		assertEquals(Integer.getInteger("retail.absolutesnare.count", 20).longValue(),
			countEffects(data, AbsoluteSnareEffect.class));
		assertEquals(Integer.getInteger("retail.buffstun.count", 2).longValue(),
			countEffects(data, BuffStunEffect.class));
		assertNotNull(data.getSkillTemplate(1).getRetailFields());

		HealEffect heal = assertInstanceOf(HealEffect.class,
			data.getSkillTemplate(1727).getEffects().getEffects().getFirst());
		assertEquals(220, heal.getValue());
		assertEquals(14, heal.getDelta());
		assertEquals(true, data.getSkillTemplate(1727).isHealBoostApplied());
		assertEquals(false, data.getSkillTemplate(274).isHealBoostApplied());

		NoReduceSpellATKInstantEffect noReduce = assertInstanceOf(NoReduceSpellATKInstantEffect.class,
			data.getSkillTemplate(8700).getEffects().getEffects().getFirst());
		assertEquals(true, noReduce.isPercent());
		assertEquals(25, noReduce.getValue());
		assertEquals(1_000_000, noReduce.getMaxDamage());

		ProvokerEffect provoker = assertInstanceOf(ProvokerEffect.class,
			data.getSkillTemplate(9189).getEffects().getEffects().getFirst());
		assertEquals(9180, provoker.getTriggeredSkillId());
		assertEquals(ProvokeTarget.OPPONENT, provoker.getProvokeTarget());
		assertEquals(1, provoker.getTriggeredSkillLevel(75));
		assertEquals(50, provoker.getRadius());
		assertEquals(Race.GCHIEF_DRAGON, provoker.getTriggerRace());
		assertEquals(1000, provoker.getHitTypeProbability(75));

		MPHealInstantEffect mpHeal = assertInstanceOf(MPHealInstantEffect.class,
			data.getSkillTemplate(4372).getEffects().getEffects().getFirst());
		assertEquals(465, mpHeal.getValue());
		assertEquals(8, mpHeal.getDelta());
		assertEquals(false, mpHeal.isPercent());
		assertEquals(true, data.getSkillTemplate(4372).isMpHealBoostApplied());
		assertEquals(false, data.getSkillTemplate(249).isMpHealBoostApplied());
		MPHealInstantEffect percentMpHeal = data.getSkillTemplate(386).getEffects().getEffects().stream()
			.filter(MPHealInstantEffect.class::isInstance)
			.map(MPHealInstantEffect.class::cast)
			.findFirst()
			.orElseThrow();
		assertEquals(true, percentMpHeal.isPercent());
		assertEquals(100, percentMpHeal.getValue());

		ParalyzeEffect paralyze = data.getSkillTemplate(2726).getEffects().getEffects().stream()
			.filter(ParalyzeEffect.class::isInstance)
			.map(ParalyzeEffect.class::cast)
			.findFirst()
			.orElseThrow();
		assertEquals(100, paralyze.getValue());
		assertEquals(0, paralyze.getDelta());
		assertEquals(1000, paralyze.getDuration2());
		assertEquals("1", paralyze.getPreEffect());
		assertNotNull(data.getSkillTemplate(20675).getEffects().getEffects().stream()
			.filter(ParalyzeEffect.class::isInstance)
			.map(ParalyzeEffect.class::cast)
			.findFirst()
			.orElseThrow()
			.getEffectConditions());

		PetrificationEffect petrification = assertInstanceOf(PetrificationEffect.class,
			data.getSkillTemplate(16492).getEffects().getEffects().getFirst());
		assertEquals(3000, petrification.getDuration2());
		assertEquals(20004, petrification.getEffectid());
		assertEquals(StatEnum.PHYSICAL_DEFENSE, petrification.getChange().getFirst().getStat());
		assertEquals(Func.PERCENT, petrification.getChange().getFirst().getFunc());
		assertEquals(10, petrification.getChange().getFirst().getValue());
		assertEquals(10, petrification.getChange().getFirst().getDelta());
		PetrificationEffect solidify = assertInstanceOf(PetrificationEffect.class,
			data.getSkillTemplate(19862).getEffects().getEffects().getFirst());
		assertEquals(5000, solidify.getDuration2());
		assertEquals(100, solidify.getChange().getFirst().getValue());
		assertEquals(0, solidify.getChange().getFirst().getDelta());

		SimpleRootEffect simpleRoot = assertInstanceOf(SimpleRootEffect.class,
			data.getSkillTemplate(8219).getEffects().getEffects().getFirst());
		assertEquals(1000, simpleRoot.getDuration1());
		assertEquals(0, simpleRoot.getDuration2());
		assertEquals(20003, simpleRoot.getEffectid());

		InvulnerableWingEffect invulnerableWing = assertInstanceOf(InvulnerableWingEffect.class,
			data.getSkillTemplate(3128).getEffects().getEffects().get(1));
		assertEquals(120000, invulnerableWing.getDuration2());
		assertEquals(105372, invulnerableWing.getEffectid());
		assertEquals("1", invulnerableWing.getPreEffect());

		assertInstanceOf(EscapeEffect.class,
			data.getSkillTemplate(302).getEffects().getEffects().getFirst());
		assertInstanceOf(ReturnPointEffect.class,
			data.getSkillTemplate(8198).getEffects().getEffects().getFirst());
		assertInstanceOf(SwitchHostileEffect.class,
			data.getSkillTemplate(3739).getEffects().getEffects().getFirst());
		assertInstanceOf(SwitchHostileEffect.class,
			data.getSkillTemplate(11604).getEffects().getEffects().getFirst());

		AbsoluteSlowEffect absoluteSlow = assertInstanceOf(AbsoluteSlowEffect.class,
			data.getSkillTemplate(21559).getEffects().getEffects().get(1));
		assertEquals(10000, absoluteSlow.getDuration2());
		assertEquals(10188451, absoluteSlow.getEffectid());
		assertEquals("1", absoluteSlow.getPreEffect());
		assertNull(absoluteSlow.getChange());

		AbsoluteSnareEffect absoluteSnare = assertInstanceOf(AbsoluteSnareEffect.class,
			data.getSkillTemplate(22735).getEffects().getEffects().getFirst());
		assertEquals(11000, absoluteSnare.getValue());
		assertEquals(0, absoluteSnare.getDelta());
		assertEquals(8000, absoluteSnare.getDuration2());
		assertNull(absoluteSnare.getChange());

		BuffStunEffect buffStun = assertInstanceOf(BuffStunEffect.class,
			data.getSkillTemplate(19580).getEffects().getEffects().get(1));
		assertEquals(8000, buffStun.getDuration2());
		assertEquals(20200, buffStun.getEffectid());
		assertEquals(200, buffStun.getBasicLvl());
		assertEquals("1", buffStun.getPreEffect());

		SnareEffect snare = assertInstanceOf(SnareEffect.class,
			data.getSkillTemplate(303).getEffects().getEffects().getFirst());
		assertEquals(StatEnum.SPEED, snare.getChange().getFirst().getStat());
		assertEquals(Func.PERCENT, snare.getChange().getFirst().getFunc());
		assertEquals(-50, snare.getChange().getFirst().getValue());
		assertEquals(0, snare.getChange().getFirst().getDelta());

		ShieldEffect shield = assertInstanceOf(ShieldEffect.class,
			data.getSkillTemplate(17785).getEffects().getEffects().getFirst());
		assertEquals(60, shield.getHitValue(1));
		assertEquals(1000, shield.getHitTypeProbability(1));
		assertEquals(10000, shield.getValue());
		assertEquals(5000, shield.getDelta());

		DispelEffect dispel = assertInstanceOf(DispelEffect.class,
			data.getSkillTemplate(258).getEffects().getEffects().getFirst());
		assertEquals(255, dispel.getMaxTargets(1));
		assertEquals(100, dispel.getDispelLevel(1));
		assertEquals(20, dispel.getDispelPower(1));

		ProcAtkInstantEffect proc = assertInstanceOf(ProcAtkInstantEffect.class,
			data.getSkillTemplate(9211).getEffects().getEffects().getFirst());
		assertEquals(9000, proc.getValue());
		assertEquals(0, proc.getDelta());
		assertEquals(100, proc.getWeaponBoost());
		assertEquals(true, proc.isCheckProtector());

		ProcAtkInstantEffect stagger = assertInstanceOf(ProcAtkInstantEffect.class,
			data.getSkillTemplate(8344).getEffects().getEffects().getFirst());
		assertEquals(false, stagger.isCheckProtector());
		assertNotNull(stagger.getSubEffect());
		assertEquals(8217, stagger.getSubEffect().getSkillId());

		HostileUpEffect hostileUp = assertInstanceOf(HostileUpEffect.class,
			data.getSkillTemplate(21930).getEffects().getEffects().getFirst());
		assertEquals(25000, hostileUp.getValue());
		assertEquals(0, hostileUp.getDelta());
		assertEquals(75000, hostileUp.getTimedValue());
		assertEquals(0, hostileUp.getTimedDelta());
		assertEquals(2500, hostileUp.getTimedDuration());
		assertEquals(true, hostileUp.isSplitTotemHate());
	}

	private static long countEffects(SkillData data, Class<? extends EffectTemplate> type) {
		return data.getSkillTemplates().stream()
			.map(SkillTemplate::getEffects)
			.filter(effects -> effects != null)
			.flatMap(effects -> effects.getEffects().stream())
			.filter(effect -> effect.getClass() == type)
			.count();
	}
}
