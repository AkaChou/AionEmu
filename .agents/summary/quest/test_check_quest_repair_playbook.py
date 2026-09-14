#!/usr/bin/env python3
from __future__ import annotations

import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_quest_repair_playbook import validate_playbook


KNOWN_COMMIT = "0123456789abcdef0123456789abcdef01234567"
EXTRA_COMMIT = "fedcba9876543210fedcba9876543210fedcba98"


def valid_source() -> str:
    return """\
| Pattern ID | 症状关键词 | IR / owner 指纹 | 第一检查点 | 代表证明 |
|---|---|---|---|---|
| `VALID_PATTERN` | symptom | fingerprint | checkpoint | `0123456`、`SampleTest#works` |

| 提交 | 案例 | 可复用结论 |
|---|---|---|
| `0123456` | case | conclusion |

### 8.1 Valid case

- Pattern ID：`VALID_PATTERN`。
- 代表任务：1。
- 玩家可见症状：symptom。
- 根因：cause。
- 修复层：layer。
- 修改文件：file。
- 验证命令和结果：passed。
- 复用边界：boundary。
- commit：`0123456789abcdef0123456789abcdef01234567`。
"""


def resolve_known_commit(reference: str) -> str | None:
    if reference in {"0123456", KNOWN_COMMIT}:
        return KNOWN_COMMIT
    if reference in {"fedcba9", EXTRA_COMMIT}:
        return EXTRA_COMMIT
    return None


class QuestRepairPlaybookValidationTest(unittest.TestCase):

    def validate(self, source: str, test_sources: dict[str, tuple[str, ...]] | None = None):
        sources = test_sources or {"SampleTest": ("class SampleTest { void works() {} }",)}
        return validate_playbook(source, sources, resolve_known_commit)

    def test_accepts_valid_references(self) -> None:
        self.assertEqual((), self.validate(valid_source()).errors)

    def test_rejects_duplicate_pattern_id(self) -> None:
        duplicate = "| `VALID_PATTERN` | other | fingerprint | checkpoint | `0123456`、`SampleTest#works` |\n"
        result = self.validate(valid_source().replace("\n| 提交 |", f"\n{duplicate}\n| 提交 |"))
        self.assertIn("duplicate Pattern ID: VALID_PATTERN", result.errors)

    def test_rejects_unknown_commit(self) -> None:
        source = valid_source().replace("0123456", "deadbee").replace(KNOWN_COMMIT, "deadbeef" * 5)
        result = self.validate(source)
        self.assertTrue(any("unknown" in error and "commit" in error for error in result.errors))

    def test_rejects_missing_test_class(self) -> None:
        result = self.validate(valid_source().replace("SampleTest#works", "MissingTest#works"))
        self.assertIn("pattern VALID_PATTERN references missing test class MissingTest", result.errors)

    def test_rejects_missing_test_method(self) -> None:
        result = self.validate(valid_source().replace("SampleTest#works", "SampleTest#missing"))
        self.assertIn("pattern VALID_PATTERN references missing test method SampleTest#missing", result.errors)

    def test_rejects_class_only_representative_test(self) -> None:
        result = self.validate(valid_source().replace("SampleTest#works", "SampleTest"))
        self.assertIn("pattern VALID_PATTERN has no representative test method", result.errors)

    def test_rejects_representative_commit_without_pattern(self) -> None:
        extra = "| `fedcba9` | extra case | conclusion |\n"
        result = self.validate(valid_source().replace("\n### 8.1", f"\n{extra}\n### 8.1"))
        self.assertIn(f"representative commit has no Pattern fingerprint: {EXTRA_COMMIT[:12]}", result.errors)

    def test_rejects_detailed_case_without_required_field(self) -> None:
        result = self.validate(valid_source().replace("- 根因：cause。\n", ""))
        self.assertIn("case 8.1 is missing field 根因", result.errors)


if __name__ == "__main__":
    unittest.main()
