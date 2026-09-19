#!/usr/bin/env python3
"""把单体 npc-ai.xml 按 npc 条数切成多个分片（源文件形态，供并行扫描）。
Splits the monolithic npc-ai.xml into per-shard files so the scan can run in parallel.

当前仓库以分片为源：单体 `npc-ai.xml` 已移除，`npc-ai-parts/` 直接被加载器并行扫描。
The repository now treats the shards as the source: the monolithic `npc-ai.xml` has been removed and
`npc-ai-parts/` is scanned in parallel by the loader.

重新切分（例如调整分片数）时先从 git 历史取回单体文件 / To re-split (e.g. change the shard count),
fetch the monolith from git history first:
	git show HEAD:src/main/resources/aion/definitions/compact/ai/npc-ai.xml > /tmp/npc-ai.xml
	python3 scripts/split_npc_ai.py --in /tmp/npc-ai.xml

用法 / Usage:
	python3 scripts/split_npc_ai.py [--shards N] [--in FILE] [--out DIR]

规则 / Rules:
	- 分片沿用 items/NPC 模板分片既有约定：目录 + `npc-ai_<起始ID>_<结束ID>.xml` 命名，
	  按起始 ID 升序即原始顺序。
	  Shards follow the existing items/NPC template convention: a directory with
	  `npc-ai_<firstId>_<lastId>.xml`, ordered by the first id.
	- 每个分片保留根元素与命名空间声明，内容为连续的 `<npc .../>` 行，语义与单体文件完全一致。
	  Each shard keeps the root element and namespace declarations; contents are consecutive `<npc .../>`
	  lines, so the semantics match the monolithic file exactly.
	- 分片以 XML 源文件形式保留，reload/临时改 XML 的既有工作流不变。
	  Shards remain XML sources, so the existing reload / hand-edit workflow is unchanged.
"""
import argparse
import os
import re


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--shards', type=int, default=8)
	parser.add_argument('--in', dest='source', default='src/main/resources/aion/definitions/compact/ai/npc-ai.xml')
	parser.add_argument('--out', dest='target', default='src/main/resources/aion/definitions/compact/ai/npc-ai-parts')
	args = parser.parse_args()

	with open(args.source, encoding='utf-8') as handle:
		lines = handle.read().splitlines()
	header = [line for line in lines
		if not line.lstrip().startswith('<npc ') and line.strip() != '</npc_ai_mappings>']
	npcs = [line for line in lines if line.lstrip().startswith('<npc ')]
	if not npcs:
		raise SystemExit('no <npc .../> lines found')
	os.makedirs(args.target, exist_ok=True)
	per_shard = (len(npcs) + args.shards - 1) // args.shards
	written = 0
	for index in range(args.shards):
		chunk = npcs[index * per_shard:(index + 1) * per_shard]
		if not chunk:
			continue
		# 文件名用 NPC ID 区间（与 items/NPC 模板分片约定一致，便于按 ID 定位与人工编辑）。
		# File names use the NPC id range, matching the items/NPC template shard convention so a shard can be
		# located by id and edited by hand.
		first_id = re.search(r'id="(\d+)"', chunk[0]).group(1)
		last_id = re.search(r'id="(\d+)"', chunk[-1]).group(1)
		path = os.path.join(args.target, f'npc-ai_{first_id}_{last_id}.xml')
		with open(path, 'w', encoding='utf-8') as handle:
			handle.write('\n'.join(header) + '\n')
			handle.write('\n'.join(chunk) + '\n')
			handle.write('</npc_ai_mappings>\n')
		written += 1
	print(f'source={args.source} npcs={len(npcs)} shards={written} out={args.target}')


if __name__ == '__main__':
	main()
