#!/usr/bin/env bash
# 完成判据自检（DoD §7.1 + 缺口计数 §7.3）。
set -u
cd /Users/mc/IdeaProjects/AionEmu-test
R=src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv
echo "== 判据① SEMANTIC_GAP 行数（目标 0）"
total=$(awk -F'\t' '!/^#/ && $4 ~ /^SEMANTIC_GAP/ {n++} END {print n+0}' "$R")
echo "SEMANTIC_GAP=$total"
echo "== 判据② 保留行 reason/evidence 齐备（ADJUDICATED 行数 + 空证据列检查）"
echo "ADJUDICATED=$(awk -F'\t' '!/^#/ && $4 ~ /^ADJUDICATED/ {n++} END {print n+0}' "$R")"
awk -F'\t' '!/^#/ && ($2=="XML_RETENTION"||$2=="SERVER_ONLY") && ($4=="" || $5=="" || $5=="-") {print "EMPTY-EVIDENCE:", $0}' "$R" | head -5
echo "SCRIPTED=$(awk -F'\t' '!/^#/ && $4=="SCRIPTED"' "$R" | wc -l | tr -d ' ') NO_TABLE=$(awk -F'\t' '!/^#/ && $4=="NO_TABLE"' "$R" | wc -l | tr -d ' ') FAMILY_PENDING=$(awk -F'\t' '!/^#/ && $4=="FAMILY_PENDING"' "$R" | wc -l | tr -d ' ')"
echo "== 判据③ EXPECTED_TSV_COUNT（目标 22）"
grep -n "EXPECTED_TSV_COUNT" src/test/java/com/aionemu/gameserver/questEngine/retail/RetailTsvManifestGateTest.java | head -1
echo "== 总量复算（6224 = RETAIL_TABLE + 保留）"
awk -F'\t' '!/^#/ {o[$2]++} END {for (k in o) print k, o[k]}' "$R"
echo "== 工作树/残留检查"
git status --porcelain | head -5
ls -d /private/tmp/aion-t3-warm 2>/dev/null || echo "warm-copy: none"
git worktree list
