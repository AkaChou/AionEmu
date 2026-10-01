package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * {@link HtmlPagesRegistry} 门测试：真端数据形状 + fail-closed 负例（计划 §4.6.1/§6.3）。
 * Gate tests for {@link HtmlPagesRegistry}: retail data shape plus fail-closed negatives (plan §4.6.1/§6.3).
 */
class HtmlPagesRegistryTest {

	private static final int EXPECTED_ROWS = 5904;

	@Test
	void loadsIngestedRetailPageTable() throws IOException {
		HtmlPagesRegistry registry = HtmlPagesRegistry.load(getClass().getClassLoader());
		// 行数与 P0a 审计/溯源清单一致（table-source-provenance.tsv: rows=5904）。
		// The row count matches the P0a audit and the provenance manifest (table-source-provenance.tsv).
		assertEquals(EXPECTED_ROWS, registry.size());
	}

	@Test
	void exposesConstantNameAndPageFileName() {
		HtmlPagesRegistry registry = HtmlPagesRegistry.instance();
		HtmlPagesRegistry.HtmlPage nullPage = registry.find(0).orElseThrow();
		assertEquals("HTML_PAGE_NULL", nullPage.name());
		// 该行未声明 htmlpagename（真端仅 35 行如此）。 / This row declares no htmlpagename (only 35 retail rows do).
		assertTrue(nullPage.htmlPagename() == null || nullPage.htmlPagename().isBlank());
		HtmlPagesRegistry.HtmlPage menuDialog = registry.find(3).orElseThrow();
		assertEquals("HTML_PAGE_MENU_DIALOG", menuDialog.name());
		assertEquals("menu_dialog", menuDialog.htmlPagename());
		assertEquals(3, registry.idByHtmlPagename("menu_dialog").orElseThrow());
		assertTrue(registry.find(3).isPresent());
	}

	@Test
	void requireFailsClosedOnUnknownPage() {
		HtmlPagesRegistry registry = HtmlPagesRegistry.instance();
		// 动态找一个确实缺声明的 id，避免测试自己发明 id。
		// Derive an actually unregistered id instead of inventing one in the test.
		int absent = IntStream.rangeClosed(0, registry.size() + 1000).filter(id -> registry.find(id).isEmpty())
				.findFirst().orElseThrow();
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> registry.require(absent));
		assertTrue(e.getMessage().contains("NATIVE_PAGE_UNREGISTERED"));
	}

	@Test
	void parsesInternalDtdEntitySubset() throws IOException {
		String xml = """
				<?xml version="1.0" encoding="UTF-16" ?>
				<!DOCTYPE strings [<!ENTITY e "Entity">]>
				<htmlpages>
					<htmlpage><id>1</id><name>HTML_&e;</name><htmlpagename>some_page</htmlpagename></htmlpage>
				</htmlpages>
				""";
		HtmlPagesRegistry registry = HtmlPagesRegistry.parse(new ByteArrayInputStream(utf16(xml)));
		assertEquals("HTML_Entity", registry.require(1).name());
	}

	@Test
	void rejectsDuplicateIdDuplicatePageNameMissingNameWrongRootAndBadNumber() throws IOException {
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(duplicateId()).orElseThrow());
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(duplicatePageName()).orElseThrow());
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(missingName()).orElseThrow());
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(wrongRoot()).orElseThrow());
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(nonNumericId()).orElseThrow());
		assertEquals("NATIVE_TABLE_PARSE_FAILED", errorCodeOf(emptyTable()).orElseThrow());
		assertFalse(errorCodeOf(valid()).isPresent());
	}

	private Optional<String> errorCodeOf(String xml) throws IOException {
		try {
			HtmlPagesRegistry.parse(new ByteArrayInputStream(utf16(xml)));
			return Optional.empty();
		} catch (IllegalStateException e) {
			return Optional.of(e.getMessage().split(":")[0]);
		}
	}

	private String duplicateId() {
		return """
				<htmlpages>
					<htmlpage><id>1</id><name>A</name></htmlpage>
					<htmlpage><id>1</id><name>B</name></htmlpage>
				</htmlpages>
				""";
	}

	private String duplicatePageName() {
		return """
				<htmlpages>
					<htmlpage><id>1</id><name>A</name><htmlpagename>same</htmlpagename></htmlpage>
					<htmlpage><id>2</id><name>B</name><htmlpagename>same</htmlpagename></htmlpage>
				</htmlpages>
				""";
	}

	private String missingName() {
		return """
				<htmlpages>
					<htmlpage><id>1</id></htmlpage>
				</htmlpages>
				""";
	}

	private String wrongRoot() {
		return """
				<notpages>
					<htmlpage><id>1</id><name>A</name></htmlpage>
				</notpages>
				""";
	}

	private String nonNumericId() {
		return """
				<htmlpages>
					<htmlpage><id>x</id><name>A</name></htmlpage>
				</htmlpages>
				""";
	}

	private String emptyTable() {
		return "<htmlpages></htmlpages>";
	}

	private String valid() {
		return """
				<htmlpages>
					<htmlpage><id>1</id><name>A</name></htmlpage>
				</htmlpages>
				""";
	}

	/** UTF-16LE + BOM，模拟真端表字节形态。 / UTF-16LE + BOM, mirroring the retail table bytes. */
	private static byte[] utf16(String xml) {
		return ('\uFEFF' + xml).getBytes(StandardCharsets.UTF_16LE);
	}
}
