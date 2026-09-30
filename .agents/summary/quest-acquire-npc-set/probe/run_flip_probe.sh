#!/bin/bash
# 临时翻转 23 行 → dump 生产 IR（side B）→ 还原。临时探针，用完即删。
set -euo pipefail
PROBE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$PROBE/../../../.." && pwd)"
cd "$REPO"
IDS=$(cat "$PROBE/flip-ids.txt")
RETENTION="src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"
CATALOG="src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
STASH="$PROBE/xml-stash"
mkdir -p "$STASH"

restore() {
  cd "$REPO"
  git checkout -- "$RETENTION" "$CATALOG"
  for id in $IDS; do
    [ -f "$STASH/$id.xml" ] && mv -f "$STASH/$id.xml" "src/main/resources/aion/data/static_data/quest/definitions/quests/$id.xml"
  done
  echo "RESTORED"
}
trap restore EXIT

python3 - "$RETENTION" "$CATALOG" "$PROBE/flip-ids.txt" <<'PY'
import sys, pathlib
retention, catalog, ids_file = (pathlib.Path(p) for p in sys.argv[1:4])
ids = {int(x) for x in ids_file.read_text().split()}
lines = retention.read_text(encoding="utf-8").splitlines()
out = []
for line in lines:
    if line.startswith("#") or not line.strip():
        out.append(line); continue
    p = line.split("\t")
    if int(p[0]) in ids:
        p[1], p[2], p[3] = "RETAIL_TABLE", "SimpleTalk", "OK"
        p[4] = "flip-probe"
    out.append("\t".join(p))
retention.write_text("\n".join(out) + "\n", encoding="utf-8")

cat = catalog.read_text(encoding="utf-8").splitlines()
kept = [l for l in cat if not any(f'<definition id="{i}"' in l for i in ids)]
catalog.write_text("\n".join(kept) + "\n", encoding="utf-8")
print(f"flipped={len(ids)} catalog_lines_removed={len(cat)-len(kept)}")
PY

for id in $IDS; do
  mv "src/main/resources/aion/data/static_data/quest/definitions/quests/$id.xml" "$STASH/$id.xml"
  rm -f "target/classes/aion/data/static_data/quest/definitions/quests/$id.xml"
done
rm -f target/classes/aion/data/static_data/quest/definitions/quest_definition_catalog.xml
rm -f target/classes/aion/data/static_data/quest/retail/retail-xml-retention.tsv
mvn -o -q -Dtest=ZzAcquireSetFlipProbeTest -Dflip.probe.out="$PROBE/ir-retail-owner.txt" test
