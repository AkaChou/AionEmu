package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 入仓真端表来源 hash 门（计划 §4.6.2）：逐行校验 table-source-provenance.tsv。
 * Source-hash gate for ingested retail tables (plan §4.6.2): verifies every table-source-provenance.tsv row.
 * <p>
 * 仓库侧 hash 被本测试强制（防无声改写）；BYTE_IDENTICAL 行额外要求 repo hash == 源 hash 且保留 UTF-16 BOM。
 * The repo-side hash is enforced here (silent edits must fail); BYTE_IDENTICAL rows additionally require
 * repo hash == source hash and a preserved UTF-16 BOM.
 */
class TableSourceProvenanceGateTest {

	private static final String MANIFEST = "aion/data/static_data/quest/retail/table-source-provenance.tsv";
	private static final String TABLE_ROOT = "aion/data/static_data/quest/";

	@Test
	void everyProvenanceRowMatchesRepoBytes() throws Exception {
		Map<String, String[]> rows = readManifest();
		// 13 = 10 张已转换表（P0a token 语义等价 9 张 + 精简批转码的 HtmlPages.xml）+ 3 张 P0b byte 级表。
		// 13 = 10 converted tables (9 P0a token-semantic-equal + HtmlPages.xml from the simplify batch)
		// + 3 P0b byte-identical tables.
		assertEquals(13, rows.size());
		for (Map.Entry<String, String[]> entry : rows.entrySet()) {
			String[] cols = entry.getValue();
			String sourceSha = cols[2];
			String repoPath = TABLE_ROOT + cols[3];
			String repoSha = cols[4];
			String encoding = cols[5];
			String transformation = cols[6];
			URL resource = getClass().getClassLoader().getResource(repoPath);
			assertTrue(resource != null, "missing ingested table " + repoPath);
			byte[] bytes;
			try (InputStream input = resource.openStream()) {
				bytes = input.readAllBytes();
			}
			assertEquals(repoSha, sha256(bytes), "repo bytes drifted for " + entry.getKey());
			if ("BYTE_IDENTICAL".equals(transformation)) {
				assertEquals(sourceSha, repoSha, "byte-identical row must equal its source hash: " + entry.getKey());
				assertTrue(bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe,
						"byte-identical row must keep the UTF-16LE BOM: " + entry.getKey());
				assertTrue(encoding.startsWith("UTF-16"), entry.getKey());
			}
		}
	}

	@Test
	void p0bByteIdenticalTablesArePresent() {
		Map<String, String[]> rows = readManifest();
		for (String table : new String[] {"challenge_task.xml", "quest_random_rewards.xml",
				"npcfactions_quest.xml"}) {
			String[] cols = rows.get(table);
			assertTrue(cols != null, "P0b table missing from provenance manifest: " + table);
			assertEquals("BYTE_IDENTICAL", cols[6], table);
		}
		// HtmlPages.xml 已按精简批转为 CONVERTED_UTF8_WHITESPACE_NORMALIZED（字符级语义等价，
		// 字节域与源脱钩）——钉死该行不得回退为 BYTE_IDENTICAL，否则与盘上 UTF-8 副本矛盾。
		// HtmlPages.xml is now CONVERTED_UTF8_WHITESPACE_NORMALIZED (char-level semantic equal, byte domain
		// decoupled from the source) — pin it so it can never silently regress to BYTE_IDENTICAL.
		String[] htmlPages = rows.get("HtmlPages.xml");
		assertTrue(htmlPages != null, "HtmlPages.xml missing from provenance manifest");
		assertTrue(htmlPages[6].startsWith("CONVERTED_UTF8_WHITESPACE_NORMALIZED"), htmlPages[6]);
		assertEquals("UTF-8", htmlPages[5], "HtmlPages.xml encoding");
	}

	private Map<String, String[]> readManifest() {
		URL url = getClass().getClassLoader().getResource(MANIFEST);
		assertTrue(url != null, "missing " + MANIFEST);
		String text;
		try (InputStream input = url.openStream()) {
			text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + MANIFEST, e);
		}
		Map<String, String[]> rows = new LinkedHashMap<>();
		for (String line : text.split("\r?\n")) {
			if (line.isBlank() || line.startsWith("table\t")) {
				continue;
			}
			String[] cols = line.split("\t", -1);
			assertTrue(cols.length >= 8, "malformed provenance row: " + line);
			assertTrue(rows.put(cols[0], cols) == null, "duplicate provenance row: " + cols[0]);
		}
		return rows;
	}

	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
