#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QE-109：retail-vs-XML IR 对拍（节点集 + 迁移三元组）。 / A/B IR diff for the flip evidence."""
from __future__ import annotations

import json
import re
from pathlib import Path

PROBE = Path(__file__).parent


def parse(path: Path) -> dict[int, dict[str, set[str]]]:
	out: dict[int, dict[str, set[str]]] = {}
	quest = None
	section = None
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('===='):
			quest = int(line.split()[-1])
			out[quest] = {'nodes': set(), 'edges': set()}
			continue
		if quest is None:
			continue
		if line.strip() == '-- nodes':
			section = 'nodes'
			continue
		if line.strip() == '-- transitions':
			section = 'edges'
			continue
		out[quest][section].add(line.strip())
	return out


def main() -> int:
	xml = parse(PROBE / 'ir-xml-owner.txt')
	retail = parse(PROBE / 'ir-retail-owner.txt')
	summary = {}
	for quest in sorted(xml):
		a, b = xml[quest], retail[quest]
		entry = {
			'nodes_xml': len(a['nodes']),
			'nodes_retail': len(b['nodes']),
			'nodes_equal': a['nodes'] == b['nodes'],
			'edges_xml': len(a['edges']),
			'edges_retail': len(b['edges']),
			'shared': len(a['edges'] & b['edges']),
			'only_xml': len(a['edges'] - b['edges']),
			'only_retail': len(b['edges'] - a['edges']),
		}
		summary[str(quest)] = entry
		print(f"{quest}: shared/onlyXml/onlyRetail = {entry['shared']}/{entry['only_xml']}/{entry['only_retail']}"
			f" nodes {entry['nodes_xml']}->{entry['nodes_retail']} equal={entry['nodes_equal']}")
	(PROBE / 'divergence-summary.json').write_text(json.dumps(summary, indent=1, sort_keys=True) + '\n', encoding='utf-8')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
