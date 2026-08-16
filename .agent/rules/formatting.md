---
alwaysApply: false
globs: "**/*.java, **/*.xml"
---

# Java and XML Formatting Standards / Java 与 XML 格式化标准

## Shared File Rules

1. Use UTF-8, LF line endings, and one final newline. Do not leave trailing whitespace.
2. Keep formatting-only changes separate from behavior changes when practical. Do not reformat unrelated files or surrounding code merely because a formatter would change it.
3. Preserve the file's established encoding, declaration, and generated-file contract. If a file is generated, update its generator and regenerate the output instead of hand-editing the generated file.
4. Formatting must not change XML text, CDATA, attribute values, element order, Java string literals, protocol numbers, or serialized data.

## Java Formatting

1. Use tabs for leading indentation with a display width of four spaces. Do not mix leading tabs and spaces.
2. Use K&R braces: the opening brace stays on the declaration or control statement line, and the closing brace aligns with that declaration.
3. Use one statement or declaration per line. Keep short fluent calls readable; break long calls at argument or chained-call boundaries and indent continuation lines one additional level.
4. Keep package and import sections at the top, use explicit imports, and do not add wildcard imports. Preserve the repository's existing import grouping unless the touched file is being intentionally normalized.
5. Separate class members into readable groups with a blank line. Keep related overloads and lifecycle methods together; do not insert blank lines inside a compact expression or switch branch.
6. Target a maximum line length of 120 characters for new code. Break at syntactic boundaries when that improves readability, but do not split URLs, protocol literals, regular expressions, SQL, or generated constants solely to meet the limit.
7. Keep annotations directly above the declaration they annotate. Put `@Override` on every overriding method where applicable.
8. Apply the bilingual comment and Javadoc requirements from `i18n.md`; formatting rules do not replace documentation requirements.

## XML Formatting

1. Use two spaces for each indentation level. Do not use tabs for XML indentation.
2. Use double quotes for attribute values and no whitespace before `/>` in self-closing elements.
3. Keep the XML declaration at the top when the file has one. Use `encoding="UTF-8"` for hand-maintained XML declarations.
4. Keep short elements on one line. When an opening tag has many attributes, wrap one attribute per line, indent attributes one level deeper than the element, and keep `>` or `/>` on the final attribute line.
5. Preserve schema-defined child order and quest transition order. In quest XML, formatting must never reorder `metadata` fields, `event`/`conditions`/`actions`/`after-commit`, nodes, transitions, domain blocks, or reward entries.
6. Preserve meaningful whitespace in mixed text content, HTML fragments, CDATA, packet payloads, and localized text. Do not run a whole-file XML formatter on those files without a targeted diff review.
7. Generated, imported, client-derived, and large static XML must follow its owning generator or source format. Do not normalize it by hand to match quest XML style.
8. For quest XML, use the schema and `docs/quest/WRITING_GUIDE.zh-CN.md` as the semantic authority; this file only defines whitespace and layout.

## Review Checklist

Before delivery, inspect the focused diff and confirm:

- no mixed indentation or trailing whitespace was introduced;
- no XML values, ordering, or comments changed unintentionally;
- generated files were regenerated from their source rather than manually reformatted;
- Java/XML formatting changes are limited to the requested scope;
- `git diff --check` passes.
