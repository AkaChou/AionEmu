package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 模板分片写出器测试：均衡字节大小、ID 区间命名、陈旧分片清理与源变更检测。
 * Template shard writer tests: balanced byte sizes, id-range naming, stale cleanup and source-change detection.
 */
class TemplateShardWriterTest {

	@TempDir
	Path tempDir;

	@Test
	void splitsTemplatesIntoBalancedShardsWithIdRangeNames() throws Exception {
		File source = writeSource("npc_template.xml", """
			<?xml version="1.0" encoding="UTF-8"?>
			<npc_templates>
				<npc_template npc_id="250003" level="3"><stats maxHp="10"/></npc_template>
				<npc_template npc_id="250001" level="1"/>
				<npc_template npc_id="250002" level="2"/>
				<npc_template npc_id="250004" level="4"/>
				<npc_template npc_id="250005" level="5"/>
				<npc_template npc_id="250006" level="6"/>
				<npc_template npc_id="250007" level="7"/>
			</npc_templates>
			""");
		File shardDir = tempDir.resolve("npc_shards").toFile();

		List<File> shards = TemplateShardWriter.writeShards(List.of(source), "npc_templates", "npc_template",
			"npc_id", shardDir, "npc_template", 3);

		assertEquals(3, shards.size());
		assertEquals("npc_template_250001_250003.xml", shards.get(0).getName());
		assertEquals("npc_template_250004_250006.xml", shards.get(1).getName());
		assertEquals("npc_template_250007_250007.xml", shards.get(2).getName());
		long smallestShard = shards.stream().mapToLong(File::length).min().orElseThrow();
		long largestShard = shards.stream().mapToLong(File::length).max().orElseThrow();
		assertTrue(largestShard < smallestShard * 2,
			"source shards should be balanced by XML size, not capped by template count");
		String firstShard = Files.readString(shards.get(0).toPath(), StandardCharsets.UTF_8);
		assertTrue(firstShard.startsWith("<?xml"));
		assertTrue(firstShard.contains("<npc_templates>"));
		// XMLEventWriter 会把自闭合子元素展开为 <stats maxHp="10"></stats>，断言按展开形式匹配
		// XMLEventWriter expands self-closing children to <stats maxHp="10"></stats>; assert the expanded form
		assertTrue(firstShard.contains("<stats maxHp=\"10\">"), "nested child elements must survive the split");
		int total = 0;
		for (File shard : shards) {
			total += countElements(shard, "npc_template");
		}
		assertEquals(7, total);
		assertTrue(TemplateShardWriter.isUpToDate(List.of(source), shardDir, "npc_template"));
	}

	@Test
	void choosesTheClosestLegalByteBoundary() throws Exception {
		String largeValue = "x".repeat(1_000);
		File source = writeSource("weighted.xml", """
			<npc_templates>
				<npc_template npc_id="1"/>
				<npc_template npc_id="2"/>
				<npc_template npc_id="3" name="%s"/>
				<npc_template npc_id="4"/>
			</npc_templates>
			""".formatted(largeValue));
		File shardDir = tempDir.resolve("weighted_shards").toFile();

		List<File> shards = TemplateShardWriter.writeShards(List.of(source), "npc_templates", "npc_template",
			"npc_id", shardDir, "npc_template", 2);

		assertEquals(List.of("npc_template_1_2.xml", "npc_template_3_4.xml"),
			shards.stream().map(File::getName).toList());
	}

	@Test
	void regenerationReplacesStaleShardsAndLeftoverTempFiles() throws Exception {
		File source = writeSource("npc_template.xml", """
			<npc_templates>
				<npc_template npc_id="100"/>
				<npc_template npc_id="200"/>
				<npc_template npc_id="300"/>
			</npc_templates>
			""");
		File shardDir = tempDir.resolve("npc_shards").toFile();
		TemplateShardWriter.writeShards(List.of(source), "npc_templates", "npc_template", "npc_id", shardDir,
			"npc_template", 3);
		assertEquals(3, TemplateShardWriter.listShards(shardDir, "npc_template").size());

		// 模拟陈旧残留：旧命名分片与中断的临时文件
		// Simulate stale leftovers: an old-named shard and an aborted temporary file.
		Files.writeString(shardDir.toPath().resolve("npc_template_999_999.xml"), "<npc_templates/>",
			StandardCharsets.UTF_8);
		Files.writeString(shardDir.toPath().resolve("npc_template_100_100.xml.tmp"), "<npc_templates/>",
			StandardCharsets.UTF_8);

		Files.writeString(source.toPath(), """
			<npc_templates>
				<npc_template npc_id="111"/>
				<npc_template npc_id="222"/>
			</npc_templates>
			""", StandardCharsets.UTF_8);
		List<File> regenerated = TemplateShardWriter.writeShards(List.of(source), "npc_templates", "npc_template",
			"npc_id", shardDir, "npc_template", 2);

		assertEquals(2, regenerated.size());
		assertEquals(List.of("npc_template_111_111.xml", "npc_template_222_222.xml"),
			regenerated.stream().map(File::getName).toList());
		assertEquals(regenerated, TemplateShardWriter.listShards(shardDir, "npc_template"),
			"stale shards and temp files must be cleaned after regeneration");
	}

	@Test
	void newerSourceInvalidatesShards() throws Exception {
		File source = writeSource("npc_template.xml", "<npc_templates><npc_template npc_id=\"1\"/></npc_templates>");
		File shardDir = tempDir.resolve("npc_shards").toFile();
		TemplateShardWriter.writeShards(List.of(source), "npc_templates", "npc_template", "npc_id", shardDir,
			"npc_template", 100);
		assertTrue(TemplateShardWriter.isUpToDate(List.of(source), shardDir, "npc_template"));

		assertTrue(source.setLastModified(System.currentTimeMillis() + 60_000));

		assertFalse(TemplateShardWriter.isUpToDate(List.of(source), shardDir, "npc_template"));
	}

	@Test
	void multipleSourcesSpanShardBoundariesInOrder() throws Exception {
		File first = writeSource("a.xml", """
			<npc_templates>
				<npc_template npc_id="1"/>
				<npc_template npc_id="2"/>
				<npc_template npc_id="3"/>
			</npc_templates>
			""");
		File second = writeSource("b.xml", """
			<npc_templates>
				<npc_template npc_id="4"/>
				<npc_template npc_id="5"/>
			</npc_templates>
			""");
		File shardDir = tempDir.resolve("npc_shards").toFile();

		List<File> shards = TemplateShardWriter.writeShards(List.of(first, second), "npc_templates", "npc_template",
			"npc_id", shardDir, "npc_template", 2);

		assertEquals(List.of("npc_template_1_4.xml", "npc_template_5_5.xml"),
			shards.stream().map(File::getName).toList());
	}

	@Test
	void missingIdAttributeFailsFast() throws Exception {
		File source = writeSource("npc_template.xml", """
			<npc_templates>
				<npc_template level="1"/>
			</npc_templates>
			""");
		File shardDir = tempDir.resolve("npc_shards").toFile();

		assertThrows(XMLStreamException.class, () -> TemplateShardWriter.writeShards(List.of(source),
			"npc_templates", "npc_template", "npc_id", shardDir, "npc_template", 1));
	}

	private File writeSource(String name, String xml) throws Exception {
		Path source = tempDir.resolve(name);
		Files.writeString(source, xml, StandardCharsets.UTF_8);
		return source.toFile();
	}

	private static int countElements(File shard, String elementName) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		int count = 0;
		try (var input = java.nio.file.Files.newInputStream(shard.toPath())) {
			XMLStreamReader reader = factory.createXMLStreamReader(input);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals(elementName)) {
						count++;
					}
				}
			} finally {
				reader.close();
			}
		}
		return count;
	}
}
