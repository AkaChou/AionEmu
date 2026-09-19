#!/usr/bin/env python3
"""把单体 npc-ai.xml 按 npc 条数切成多个分片（源文件形态，供并行扫描）。
Splits the monolithic npc-ai.xml into per-shard files so the scan can run in parallel.

当前仓库已回滚到单体源：`npc-ai.xml` 是运行时唯一来源，加载器不再扫描分片目录；本脚本保留用于再次
尝试分片（默认输出 `npc-ai-parts/`），只有把加载器重新指向该目录后才生效。
The repository is back on the monolithic source: `npc-ai.xml` is the runtime source of truth and the loader
no longer scans a shard directory. This script is kept for re-trying shards (default output
`npc-ai-parts/`) and only takes effect once the loader points back at that directory.

重新切分（例如换分片数）直接对当前单体文件执行 / To re-split (e.g. try another shard count), run it on
the monolithic file in place:
	python3 scripts/split_npc_ai.py --shards 16

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
