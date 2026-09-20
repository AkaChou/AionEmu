# -*- coding: utf-8 -*-
"""严格删除“真端变体块 + 残留 legacy 块”中的 legacy 块。

只在坐标集合完全匹配、块内所有 spot 都没有 resolve_z 且块没有
initial_delay/spawn_page 时删除；否则拒绝并报告。
"""
from pathlib import Path
import re

ROOT = Path("src/main/resources/aion/data/static_data/spawns/Instances")
SPAWN_RE = re.compile(r"<spawn\b[^>]*>.*?</spawn>", re.S)
SPOT_RE = re.compile(r"<spot\b([^>]*?)/?>", re.S)
ATTR_RE = re.compile(r'([\w_]+)\s*=\s*"([^"]*)"')

TARGETS = {
    "300540000_Eternal_Bastion.xml": {
        "230753": {("604.79755", "896.1383")},
        "230756": {("402.96927", "260.2746"), ("580.26337", "491.0744")},
        "233312": {
            ("445.50754", "301.54877"), ("676.8388", "752.69904"),
            ("521.2584", "279.86615"), ("532.52716", "469.35806"),
            ("557.9122", "477.24234"),
        },
    },
    "301210000_Engulfed_Ophidan_Bridge.xml": {
        "233474": {("484.61765", "531.6245")},
        "233479": {("519.06854", "434.295")},
        "233480": {("533.65063", "428.35898")},
        "233481": {("610.57794", "559.381")},
        "233484": {("674.94977", "478.36877")},
        "233485": {("672.62286", "467.2902")},
        "233486": {("660.30194", "466.5498")},
    },
    "301390000_Drakenspire_Depths.xml": {
        "236126": {("403.2626", "238.516"), ("404.62762", "125.51749")},
        "236223": {("772.6448", "263.3362")},
    },
    "301400000_The_Shugo_Emperor_Vault.xml": {
        "235653": {
            ("551.8219", "444.63602"), ("535.0065", "500.6568"),
            ("526.0041", "602.769"), ("523.57776", "602.8369"),
        },
        "235660": {("360.03033", "757.95233")},
    },
}


def attrs(text):
    return dict(ATTR_RE.findall(text))


def remove_comment_before(text, start):
    """删除紧邻块前的单行注释，避免留下孤立的注释。"""
    line_start = text.rfind("\n", 0, start) + 1
    previous_end = line_start - 1
    if previous_end < 0 or text[previous_end] != "\n":
        return start
    previous_start = text.rfind("\n", 0, previous_end) + 1
    previous = text[previous_start:previous_end].strip()
    if previous.startswith("<!--") and previous.endswith("-->"):
        return previous_start
    return start


for file_name, targets in TARGETS.items():
    path = ROOT / file_name
    text = path.read_text(encoding="utf-8")
    matches = list(SPAWN_RE.finditer(text))
    removals = []
    for npc_id, expected in targets.items():
        candidates = []
        for match in matches:
            block = match.group(0)
            if f'npc_id="{npc_id}"' not in block.split(">", 1)[0]:
                continue
            block_attrs = attrs(block.split(">", 1)[0])
            spots = SPOT_RE.findall(block)
            coords = {(attrs(spot)["x"], attrs(spot)["y"]) for spot in spots}
            pure_legacy = all("resolve_z" not in attrs(spot) for spot in spots)
            no_variant = "initial_delay" not in block_attrs and "spawn_page" not in block_attrs
            if coords == expected and pure_legacy and no_variant:
                candidates.append(match)
        if len(candidates) != 1:
            raise SystemExit(f"REFUSE {file_name} npc={npc_id}: expected 1 matching block, got {len(candidates)}")
        match = candidates[0]
        start = remove_comment_before(text, match.start())
        removals.append((start, match.end(), npc_id))

    # 从后往前删除，避免前面的删除使后续正则偏移失效。
    # Delete back-to-front so earlier removals do not invalidate later offsets.
    previous_start = len(text) + 1
    for start, end, npc_id in sorted(removals, reverse=True):
        if end > previous_start:
            raise SystemExit(f"REFUSE {file_name} npc={npc_id}: overlapping removals")
        text = text[:start] + text[end:]
        previous_start = start
    path.write_text(text, encoding="utf-8")
    print(f"UPDATED {path}")
