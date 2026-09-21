from collections import defaultdict
from pathlib import Path
import re

source_root = Path("src/main/java/com/aionemu/gameserver/ai")
npc_template_root = Path("src/main/resources/aion/data/static_data/npcs")

annotations = defaultdict(list)
for path in sorted(source_root.rglob("*.java")):
    match = re.search(r'@AIName\("([^"]+)"\)', path.read_text())
    if match:
        annotations[match.group(1)].append(path)

npc_references = set()
for path in sorted(npc_template_root.glob("npc_template_*.xml")):
    npc_references.update(re.findall(r'\bai="([^"]+)"', path.read_text(encoding="utf-8", errors="ignore")))
npc_references.update({
    "fearful_beast",
    "siege_teleporter",
    "dummy",
    "retail_pattern",
    "retail_direct_portal",
})

duplicates = {name: paths for name, paths in annotations.items() if len(paths) > 1}
missing = npc_references - set(annotations)
print(f"annotation_classes={sum(map(len, annotations.values()))}")
print(f"annotation_names={len(annotations)}")
print(f"npc_references={len(npc_references)}")
print(f"duplicate_names={len(duplicates)}")
print(f"missing_npc_references={len(missing)}")
for name, paths in sorted(duplicates.items()):
    print(f"DUPLICATE {name}")
    for path in paths:
        print(f"  {path}")
for name in sorted(missing):
    print(f"MISSING {name}")
