package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.RepeatedAbnormalStatusImmuneTemplate;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;

/**
 * 固化原版「重复异常状态免疫表」的加载语义：数值逐位一致、STUN 等表外状态不参与、
 * 窗口公式与非法配置 fail-fast。
 * Pins the retail repeated-abnormal immunity table: value-for-value tiers, untracked states
 * (STUN included) excluded, and the window formula plus fail-fast behaviour on invalid input.
 */
class RepeatedAbnormalStatusImmuneDataTest {

	private static final Path PRODUCTION_XML = Path
			.of("src/main/resources/aion/data/static_data/repeated_abnormal_status_immune.xml");
	private static final Path PRODUCTION_XSD = Path
			.of("src/main/resources/aion/data/static_data/repeated_abnormal_status_immune.xsd");
	private static final int[] RETAIL_RESIST_TIERS = { 0, 0, 200, 400, 1000 };
	private static final int[] RETAIL_TIME_TIERS = { 100, 90, 85, 80, 0 };

	@Test
	void loadsRetailParalyzeSleepAndFearTiers() throws Exception {
		RepeatedAbnormalStatusImmuneData data = loadProduction();

		assertEquals(3, data.size());
		assertRetailTiers(data.getTemplate(AbnormalState.PARALYZE), 0, 0);
		assertRetailTiers(data.getTemplate(AbnormalState.SLEEP), 1, 0);
		assertRetailTiers(data.getTemplate(AbnormalState.FEAR), 2, 2000);
	}

	@Test
	void mapsResistStatsAndExcludesUntrackedStates() throws Exception {
		RepeatedAbnormalStatusImmuneData data = loadProduction();

		assertEquals(AbnormalState.SLEEP, data.getTemplate(StatEnum.SLEEP_RESISTANCE).getAbnormalState());
		assertEquals(AbnormalState.PARALYZE, data.getTemplate(StatEnum.PARALYZE_RESISTANCE).getAbnormalState());
		assertEquals(AbnormalState.FEAR, data.getTemplate(StatEnum.FEAR_RESISTANCE).getAbnormalState());
		// 晕厥等表外状态没有条目（原版表不含 STUN）/ untracked states (STUN included) resolve to no entry
		assertNull(data.getTemplate(StatEnum.STUN_RESISTANCE));
		assertNull(data.getTemplate(StatEnum.ROOT_RESISTANCE));
		assertNull(data.getTemplate(AbnormalState.STUN));
	}

	@Test
	void computesRetailHitWindow() throws Exception {
		RepeatedAbnormalStatusImmuneData data = loadProduction();

		assertEquals(3_000L, data.getTemplate(AbnormalState.SLEEP).getWindowMillis(3_000));
		assertEquals(3_000L, data.getTemplate(AbnormalState.PARALYZE).getWindowMillis(3_000));
		// FEAR 的 holding_time2=2000，窗口多 2 秒 / FEAR adds its 2000 ms holding_time2
		assertEquals(5_000L, data.getTemplate(AbnormalState.FEAR).getWindowMillis(3_000));
		assertEquals(0L, data.getTemplate(AbnormalState.SLEEP).getWindowMillis(0));
	}

	@Test
	void unknownStatusNameFailsFast(@TempDir Path tempDir) throws Exception {
		Path source = tempDir.resolve("bogus.xml");
		Files.writeString(source, tableXml("BOGUS", "0,0,200,400,1000", "100,90,85,80,0"), StandardCharsets.UTF_8);

		Exception failure = assertThrows(Exception.class, () -> load(source));

		assertTrue(messages(failure).contains("BOGUS"));
	}

	@Test
	void mismatchedTierLengthsFailFast(@TempDir Path tempDir) throws Exception {
		Path source = tempDir.resolve("mismatched.xml");
		Files.writeString(source, tableXml("SLEEP", "0,0,200", "100,90,85,80,0"), StandardCharsets.UTF_8);

		Exception failure = assertThrows(Exception.class, () -> load(source));

		assertTrue(messages(failure).contains("SLEEP"));
		assertTrue(messages(failure).contains("Mismatched"));
	}

	@Test
	void productionXmlValidatesAgainstItsSchema() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		assertDoesNotThrow(() -> schemaFactory.newSchema(PRODUCTION_XSD.toFile()).newValidator()
				.validate(new StreamSource(PRODUCTION_XML.toFile())));
	}

	private static void assertRetailTiers(RepeatedAbnormalStatusImmuneTemplate template, int index, int holdingTime2) {
		assertNotNull(template);
		assertEquals(index, template.getIndex());
		assertEquals(1, template.getHoldingTime1());
		assertEquals(holdingTime2, template.getHoldingTime2());
		for (int step = 1; step <= RETAIL_TIME_TIERS.length; step++) {
			assertEquals(RETAIL_RESIST_TIERS[step - 1], template.getResistValue(step));
			assertEquals(RETAIL_TIME_TIERS[step - 1], template.getTimeValue(step));
		}
	}

	private static RepeatedAbnormalStatusImmuneData loadProduction() throws Exception {
		return load(PRODUCTION_XML);
	}

	private static RepeatedAbnormalStatusImmuneData load(Path source) throws Exception {
		return (RepeatedAbnormalStatusImmuneData) JAXBContext.newInstance(RepeatedAbnormalStatusImmuneData.class)
				.createUnmarshaller().unmarshal(source.toFile());
	}

	private static String tableXml(String name, String resistValue, String timeValue) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<repeated_abnormal_status_immune>\n"
				+ "    <abnormal_status name=\"" + name + "\" holding_time1=\"1\" holding_time2=\"0\" resist_value=\""
				+ resistValue + "\" time_value=\"" + timeValue + "\"/>\n"
				+ "</repeated_abnormal_status_immune>\n";
	}

	private static String messages(Throwable failure) {
		StringBuilder text = new StringBuilder();
		for (Throwable current = failure; current != null; current = current.getCause()) {
			text.append(current.getMessage()).append('\n');
		}
		return text.toString();
	}
}
