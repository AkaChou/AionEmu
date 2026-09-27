#!/bin/bash
# 奖励窗自动确认通道收尾：dataDriven dump → install → 契约测试 + 门禁全量复跑。
# Auto-channel finisher: dataDriven dump → install → contract tests + gate sweep.
set -u
cd "$(dirname "$0")/../../.." || exit 1

echo "== 0. 视图可读性探测 / view probe"
if rtk mvn -o test -Dtest='QuestExclusiveSiblingAttributionTest#auditNeverReportsClassBranchRewardEdgeAsTrueAmbiguity' -DfailIfNoTests=false 2>&1 | grep -q "production quest view unreadable"; then
	echo "production quest view unreadable — 中止（等并行 faction 车道落地）"
	exit 2
fi

echo "== 1. dataDriven 指纹 dump"
rtk mvn -o test -Dtest='RetailDataDrivenGateTest' -Dretail.dataDriven.fpOut=target/fp/dd.tsv -DfailIfNoTests=false >/tmp/dd_dump.log 2>&1
wc -l target/fp/dd.tsv
if [ ! -s target/fp/dd.tsv ] || [ "$(grep -c '^[0-9]' target/fp/dd.tsv)" -lt 100 ]; then
	echo "dd.tsv dump 产物异常（缺文件或行数 <100）——中止，不安装"
	exit 3
fi

echo "== 2. install"
cp target/fp/dd.tsv src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv
cp target/fp/dd.tsv target/test-classes/quest/retail-data-driven-ir-fingerprints.tsv

echo "== 3. 契约测试（Targetless×2 / Sibling / Coverage）"
rtk mvn -o test -Dtest='Quest13830To13834TargetlessRewardTest,Quest23830To23834TargetlessRewardTest,QuestExclusiveSiblingAttributionTest,QuestReportedRewardCoverageTest' -DfailIfNoTests=false 2>&1 | grep -E "Tests run:.*Failures|BUILD"

echo "== 4. DD 门 + 家族门复跑"
rtk mvn -o test -Dtest='RetailDataDrivenGateTest,RetailSimpleHuntEquivalenceGateTest,RetailSimpleCollectItemGateTest,RetailSimpleHuntFamilyGateTest,RetailCombineTaskGateTest,RetailNonIrAxisGateTest' -DfailIfNoTests=false 2>&1 | grep -E "Tests run:.*Failures|FAILURE!|BUILD"

echo "== 5. verify retirement"
python3 scripts/verify_retirement.py 2>/dev/null || python3 .agents/summary/scriptdll-quest-driver/verify_retirement.py 2>/dev/null || echo "verify script not found at usual paths — run manually"
