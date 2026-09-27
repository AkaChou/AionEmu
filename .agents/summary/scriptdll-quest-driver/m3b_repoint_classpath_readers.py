#!/usr/bin/env python3
"""M3-b 收尾批次 2：把仍按 classpath 直读生产 quest XML 的测试改为
`QuestXmlFixtures.openResource(path)`（生产资源优先 → 退役冻结副本回落）。

背景：`mvn test` 重新拷贝资源后，`target/classes` 里已没有 1581 个退役 SimpleTalk XML；
这些测试此前靠陈旧 classpath 副本"侥幸"仍是绿的。判据不变：读到的 XML 内容与冻结副本逐字节相同。
"""
import pathlib
import re
import sys

BASE = pathlib.Path("src/test/java/com/aionemu/gameserver/questEngine")
CALL = re.compile(
	r'(?P<cls>(?:getClass\(\)|[A-Za-z_][A-Za-z0-9_]*\.class)'
	r'\.getClassLoader\(\)\.getResourceAsStream'
	r'|(?:getClass\(\)|[A-Za-z_][A-Za-z0-9_]*\.class)\.getResourceAsStream)\('
	r'\s*(?P<arg>'
	r'"/aion/data/static_data/quest_definition/quests/[^"]*"'
	r'|"aion/data/static_data/quest_definition/quests/[^"]*"'
	r'|"/aion/data/static_data/quest_definition/quests/"\s*\+\s*[A-Za-z_][A-Za-z0-9_]*\s*\+\s*"\.xml"'
	r'|"aion/data/static_data/quest_definition/quests/"\s*\+\s*[A-Za-z_][A-Za-z0-9_]*\s*\+\s*"\.xml"'
	r'|"/aion/data/static_data/quest_definition/quests/"\s*\+\s*[A-Za-z_][A-Za-z0-9_]*'
	r'|[A-Za-z_][A-Za-z0-9_]*)\s*\)', re.DOTALL)


def patch(path: pathlib.Path) -> int:
	text = path.read_text()
	count = 0

	def replace(match):
		nonlocal count
		count += 1
		return f"QuestXmlFixtures.openResource({match.group('arg')})"

	new = CALL.sub(replace, text)
	if count == 0:
		return 0
	# 跨包调用需要显式 import（同包 definition 不需要）。
	if path.parent.name != "definition" and "import com.aionemu.gameserver.questEngine.definition.QuestXmlFixtures;" not in new:
		anchor = "import org.junit.jupiter.api.Test;\n"
		if anchor not in new:
			raise SystemExit(f"{path}: cannot locate import anchor")
		new = new.replace(anchor, anchor + "\nimport com.aionemu.gameserver.questEngine.definition.QuestXmlFixtures;\n", 1)
	path.write_text(new)
	return count


def main() -> int:
	targets = [line.strip() for line in pathlib.Path(sys.argv[1]).read_text().splitlines() if line.strip()]
	total = 0
	for fqn in targets:
		path = BASE / pathlib.Path(fqn.replace(".", "/") + ".java").relative_to("com/aionemu/gameserver/questEngine")
		if not path.exists():
			print(f"skip missing {path}")
			continue
		n = patch(path)
		total += n
		print(f"{n:2d}  {path.name}")
	print(f"replaced call sites: {total}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
