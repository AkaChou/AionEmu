#!/usr/bin/env python3
"""M3-b 收尾：把仍按生产路径直读 quest XML 的测试改为生产优先 + 退役冻结副本回落。

判据：这些测试在 HEAD 上读取的 XML 内容与退役冻结副本逐字节相同，因此改走
QuestXmlFixtures.open(...) 后语义不变（只是来源从生产目录换成"生产优先、冻结回落"）。
"""
import pathlib

BASE = pathlib.Path("src/test/java/com/aionemu/gameserver/questEngine/definition")

DIR_CONST = '''\tprivate static final Path {name} = Path.of(
\t\t"src/main/resources/aion/data/static_data/quest/definitions/quests");
'''
DIR_CONST_ONE = '''\tprivate static final Path {name} = Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests");
'''

EDITS = {
    "GrowthQuestDialogPageAlignmentTest.java": [
        (DIR_CONST.format(name="QUEST_DIRECTORY"), ""),
        ('Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))',
         'QuestXmlFixtures.open(questId)'),
    ],
    "LegacyTemplateMirrorRouteRegressionTest.java": [
        (DIR_CONST.format(name="QUEST_DIRECTORY"), ""),
        ('Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))',
         'QuestXmlFixtures.open(questId)'),
    ],
    "QuestKaligaCollectionClientDialogAlignmentTest.java": [
        (DIR_CONST.format(name="QUEST_DIR"), ""),
        ('Files.newInputStream(QUEST_DIR.resolve(questId + ".xml"))',
         'QuestXmlFixtures.open(questId)'),
    ],
    "QuestCharmedEventDefinitionTest.java": [
        (DIR_CONST_ONE.format(name="DIR"), ""),
        ('Files.newInputStream(DIR.resolve(questId + ".xml"))', 'QuestXmlFixtures.open(questId)'),
    ],
    "QuestEventQuestBatchDefinitionTest.java": [
        (DIR_CONST_ONE.format(name="DIR"), ""),
        ('Files.newInputStream(DIR.resolve(questId + ".xml"))', 'QuestXmlFixtures.open(questId)'),
    ],
    "QuestLunarEventDefinitionTest.java": [
        (DIR_CONST_ONE.format(name="DIR"), ""),
        ('Files.newInputStream(DIR.resolve(questId + ".xml"))', 'QuestXmlFixtures.open(questId)'),
    ],
    "QuestMovieEventDefinitionTest.java": [
        (DIR_CONST_ONE.format(name="DIR"), ""),
        ('Files.newInputStream(DIR.resolve(questId + ".xml"))', 'QuestXmlFixtures.open(questId)'),
    ],
    "QuestBatchReportNpcAlignmentTest.java": [
        ('\t\tPath path = Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests/" + qid + ".xml");\n', ""),
        ('Files.newInputStream(path)', 'QuestXmlFixtures.open(Integer.parseInt(qid))'),
    ],
}

for quest_id in (30312, 30314, 30315):
    EDITS[f"Quest{quest_id}RetailAlignmentTest.java"] = [
        (f'\t\tPath path = Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests/{quest_id}.xml");\n', ""),
        ('Files.newInputStream(path)', f'QuestXmlFixtures.open({quest_id})'),
    ]

for name, edits in EDITS.items():
    path = BASE / name
    text = path.read_text()
    original = text
    for old, new in edits:
        count = text.count(old)
        if count != 1:
            raise SystemExit(f"{name}: expected 1 occurrence of {old[:60]!r}, found {count}")
        text = text.replace(old, new)
    # 清理不再使用的导入（每个文件里 Files/Path 只出现在被替换的装载器里）
    for unused in ("import java.nio.file.Files;\n", "import java.nio.file.Path;\n"):
        if unused in text:
            remaining = text.replace(unused, "")
            if "Files." in remaining or "Path." in remaining or "Path " in remaining:
                raise SystemExit(f"{name}: {unused.strip()} still referenced")
            text = remaining
    if text == original:
        raise SystemExit(f"{name}: no change applied")
    path.write_text(text)
    print(f"patched {name}")

print("done")
