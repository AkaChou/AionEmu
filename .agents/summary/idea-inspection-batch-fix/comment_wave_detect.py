#!/usr/bin/env python3
"""静态检测 javadoc 注释问题（悬空/非法标签/空说明/错误标签/空白行）。

对应 IDEA 规则: DanglingJavadoc / JavadocDeclaration / JavadocBlankLines / JavadocReference。
输出 JSON: [{file, line, kind, text, context}]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DECL_RE = re.compile(
    r'^\s*(?:@[\w.]+\s+(?:public|private|protected|static|final|abstract|synchronized|native|default|sealed|non-sealed)\s+'
    r'|\s*(?:public|private|protected|static|final|abstract|synchronized|native|default|sealed|non-sealed)\s+'
    r'|(?:@\w+)'
    r'|(?:class|interface|enum|record)\s)'
)
# 可能接续 javadoc 的声明起始（粗匹配：修饰符/注解/类型关键字）
DECL_START = re.compile(
    r'^\s*(?:@[\w.]+|public|private|protected|static|final|abstract|synchronized|native|default|transient|volatile|'
    r'class|interface|enum|record|sealed|non-sealed|[A-Z][\w.<>\[\], ]*\s+[a-zA-Z_]\w*\s*[({;=]|[A-Z][\w.<>\[\], ]*\s+\w+\s*\()'
)
MODIFIERS = {'public', 'private', 'protected', 'static', 'final', 'abstract', 'synchronized', 'native', 'default',
             'transient', 'volatile', 'strictfp', 'sealed', 'non-sealed'}
TYPE_KEYWORDS = {'class', 'interface', 'enum', 'record'}
KNOWN_TAGS = {'param', 'return', 'throws', 'exception', 'author', 'version', 'since', 'see', 'deprecated', 'serial',
              'serialField', 'serialData', 'inheritDoc', 'docRoot', 'link', 'linkplain', 'value', 'code', 'literal',
              'index', 'summary', 'systemProperty', 'hide', 'apiNote', 'implNote', 'implSpec', 'spec', 'hidden'}


def strip_strings(line: str) -> str:
    line = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
    line = re.sub(r"'(?:\\.|[^'\\])*'", "''", line)
    line = re.sub(r'//.*$', '', line)
    return line


def find_javadocs(lines):
    """返回 [(start_idx, end_idx)]，索引 0-based，含 /** 与 */ 行。"""
    blocks = []
    i = 0
    n = len(lines)
    while i < n:
        # 行内注释需排除字符串; 简化: 只查裸 /** 且不在行级 // 后
        code = strip_strings(lines[i])
        pos = code.find('/**')
        if pos >= 0:
            start = i
            end = i
            body = code[pos + 3:]
            if '*/' in body:
                end = i
            else:
                j = i + 1
                while j < n:
                    if '*/' in lines[j]:
                        end = j
                        break
                    j += 1
                else:
                    break  # 未闭合，跳过
            blocks.append((start, end))
            i = end + 1
        else:
            i += 1
    return blocks


def brace_depth_before(lines, idx):
    """计算 lines[0:idx] 的花括号深度（忽略字符串/行注释/块注释）。"""
    depth = 0
    in_block_comment = False
    for line in lines[:idx]:
        code = strip_strings(line)
        if in_block_comment:
            endpos = code.find('*/')
            if endpos < 0:
                continue
            code = code[endpos + 2:]
            in_block_comment = False
        # 去掉行内的 /* ... */ 与 // 已处理; 检查未闭合 /*
        m = re.search(r'/\*(?!\*).*?(?:\*/|$)', code)
        if m and not m.group(0).endswith('*/'):
            in_block_comment = True
            code = code[:m.start()]
        depth += code.count('{') - code.count('}')
    return depth


def target_kind(lines, end_idx):
    """javadoc 块之后第一个非空代码行判断目标类型。
    返回 (is_class, sig, depth)。"""
    n = len(lines)
    j = end_idx + 1
    # 收集注解行
    while j < n:
        stripped = lines[j].strip()
        if stripped == '' or stripped.startswith('//'):
            j += 1
            continue
        if stripped.startswith('@'):
            j += 1
            continue
        break
    if j >= n:
        return False, '', brace_depth_before(lines, end_idx)
    sig = lines[j].strip()
    depth = brace_depth_before(lines, end_idx)  # javadoc 所在深度 == 成员层级深度
    # 类/接口/枚举声明
    if re.search(r'\b(class|interface|enum|record)\s+\w+', strip_strings(sig)):
        return True, sig, depth
    return False, sig, depth


def extract_params(sig_text):
    """从签名文本提取平衡括号内的参数名列表。失败返回 None。"""
    start = sig_text.find('(')
    if start < 0:
        return None, False
    depth = 0
    end = -1
    for i in range(start, len(sig_text)):
        c = sig_text[i]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                end = i
                break
    if end < 0:
        return None, False  # 跨行未闭合 → 交给 IDEA 判定
    inner = sig_text[start + 1:end]
    params = []
    # 按顶层逗号切分
    part = []
    d = 0
    for c in inner:
        if c in '(<[':
            d += 1
        elif c in ')>]':
            d -= 1
        if c == ',' and d == 0:
            params.append(''.join(part))
            part = []
        else:
            part.append(c)
    if part:
        params.append(''.join(part))
    names = []
    for p in params:
        # 去注解（@Anno(...) / @Anno）
        p = re.sub(r'@\w+(?:\([^()]*(?:\([^()]*\)[^()]*)*\))?', ' ', p)
        toks = p.split()
        if not toks:
            return None, False
        tok = toks[-1]
        if '...' in tok:
            # 变长参数 String...args（无空格）/ String... args
            tok = tok.rsplit('...', 1)[-1]
            if not tok and len(toks) > 1:
                tok = toks[-2]
            tok = tok.strip()
        names.append(tok.lstrip('.'))
    return names, True


def javadoc_tag_lines(lines, start, end):
    """返回块内 tag 行: [(abs_line_idx, tag, rest)]"""
    tags = []
    for i in range(start, end + 1):
        text = lines[i]
        if i == end and '*/' in text:
            text = text.split('*/')[0]
        m = re.match(r'^\s*\*?\s*@(\w+)\s*(.*)$', text)
        if m:
            tags.append((i, m.group(1), m.group(2).strip()))
    return tags


def collect_signature(lines, start_idx):
    """从 start_idx 起聚合代码行（跳过注解/空行），直到出现平衡的 '(...)' 或 ';/={'. 返回聚合文本。"""
    n = len(lines)
    j = start_idx
    # 跳过注解行与空行
    while j < n:
        stripped = lines[j].strip()
        if stripped == '' or stripped.startswith('@') or stripped.startswith('//'):
            j += 1
            continue
        break
    buf = []
    depth = 0
    seen_open = False
    while j < n and len(buf) < 60:
        text = strip_strings(lines[j])
        buf.append(text)
        for c in text:
            if c == '(':
                depth += 1
                seen_open = True
            elif c == ')':
                depth -= 1
        joined = ' '.join(buf)
        if seen_open and depth <= 0:
            return joined
        if not seen_open and re.search(r'[;={]', joined):
            return joined
        j += 1
    return ' '.join(buf)



MODIFIERS_D = {'public', 'private', 'protected', 'static', 'final', 'abstract', 'synchronized', 'native', 'default',
               'transient', 'volatile', 'strictfp', 'sealed', 'non-sealed'}
CONTROL_D = {'if', 'for', 'while', 'switch', 'return', 'break', 'continue', 'try', 'do', 'else', 'case', 'throw',
             'throws', 'new', 'assert', 'catch', 'finally'}


def is_declaration_start(line):
    s = strip_strings(line).strip()
    if not s:
        return False
    first = re.match(r'([A-Za-z_$][\w$]*)', s)
    if first and first.group(1) in CONTROL_D:
        return False
    if s.startswith(('}', '{', ')', ';', '*', '/', '+', '-')):
        return False
    if first and first.group(1) in MODIFIERS_D:
        return True
    if first and first.group(1) in {'class', 'interface', 'enum', 'record'}:
        return True
    if re.match(r'[A-Z][\w$]*\s*[\(<{]', s):
        return True
    if re.match(r'[A-Z][\w$]*\s*[;,]', s):
        return True
    if re.fullmatch(r'[A-Za-z_$][\w$]*', s):
        return True
    if re.match(r'[A-Za-z_$][\w$.]*(?:\s*<[^=;{}]*?>)?(?:\s*\[\s*\])*\s+[A-Za-z_$][\w$]*\s*(?:\(|=|;|,)', s):
        return True
    return False


def next_code_line(lines, start_idx):
    """返回 start_idx 起第一个有效代码行 idx（跳过空行/注释行/含跨行括号的注解行）。"""
    n = len(lines)
    j = start_idx
    while j < n:
        st = lines[j].strip()
        if st == '' or st.startswith('//') or st.startswith('/*') or st.startswith('*'):
            j += 1
            continue
        if st.startswith('@'):
            depth = st.count('(') + st.count('{') - st.count(')') - st.count('}')
            j += 1
            while j < n and depth > 0:
                depth += lines[j].count('(') + lines[j].count('{') - lines[j].count(')') - lines[j].count('}')
                j += 1
            continue
        return j
    return n


def scan_file(path):
    rel = str(path)
    try:
        raw = path.read_text(encoding='utf-8')
    except Exception:
        return []
    lines = raw.split('\n')
    findings = []
    blocks = find_javadocs(lines)
    for (s, e) in blocks:
        # 1) 悬空: 块后第一个有效代码行不是声明（复用 fixer 的 next_code_line）
        j2 = next_code_line(lines, e + 1)
        n2 = len(lines)
        if j2 < n2 and not is_declaration_start(lines[j2]):
            findings.append({'file': rel, 'line': s + 1, 'kind': 'dangling',
                             'text': ' '.join(x.strip() for x in lines[s:e + 1])[:120]})
            continue
        # 2) 空白行
        for i in range(s, e + 1):
            text = lines[i]
            if i == e:
                text = text.split('*/')[0]
            m = re.match(r'^\s*\*\s*$', text)
            if m:
                findings.append({'file': rel, 'line': i + 1, 'kind': 'blank_line', 'text': ''})
        # 3) 标签合法性
        is_class, first_sig, _depth = target_kind(lines, e)
        params = None
        is_method = False
        if not is_class:
            joined = collect_signature(lines, e + 1)
            params, is_method = extract_params(joined)
            if '(' in joined and params is None:
                # 跨行/复杂签名解析失败 → 不做 param 判定，交 IDEA 兜底
                is_method = 'unknown'
            elif not is_method:
                params = None
        for (li, tag, rest) in javadoc_tag_lines(lines, s, e):
            if tag not in KNOWN_TAGS:
                findings.append({'file': rel, 'line': li + 1, 'kind': 'bad_tag', 'tag': tag, 'text': rest})
                continue
            if tag == 'param':
                pm = re.match(r'<(\S+)>(?:\s+(.*))?$', rest)
                if pm:
                    # 类型参数 @param <T>（类级/方法级均合法）
                    if not (pm.group(2) or '').strip():
                        findings.append({'file': rel, 'line': li + 1, 'kind': 'empty_desc', 'tag': 'param',
                                         'text': rest})
                    continue
                pm = re.match(r'(\S+)\s*(.*)$', rest)
                if not pm:
                    continue
                pname, desc = pm.group(1), pm.group(2).strip()
                if is_class:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'illegal_tag', 'tag': 'param', 'text': rest})
                    continue
                if is_method is False:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'illegal_tag', 'tag': 'param', 'text': rest})
                    continue
                if is_method == 'unknown':
                    continue
                if not desc:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'empty_desc', 'tag': 'param', 'text': rest})
                    continue
                if params is not None and pname not in params:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'param_mismatch', 'tag': pname,
                                     'text': rest, 'actual': params})
                continue
            if tag == 'return':
                if is_class or is_method is False:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'illegal_tag', 'tag': 'return', 'text': rest})
                elif not rest:
                    findings.append({'file': rel, 'line': li + 1, 'kind': 'empty_desc', 'tag': 'return', 'text': rest})
    return findings


def main():
    out = []
    count = 0
    for base in ['src/main/java', 'src/test/java']:
        for p in (ROOT / base).rglob('*.java'):
            count += 1
            out.extend(scan_file(p))
    dst = Path(__file__).parent / 'comment_wave_findings.json'
    dst.write_text(json.dumps(out, ensure_ascii=False, indent=0))
    from collections import Counter
    c = Counter(f['kind'] for f in out)
    files = {f['file'] for f in out}
    print(f'scanned {count} files, findings {len(out)}, files {len(files)}')
    for k, v in c.most_common():
        print(f'  {k}: {v}')


if __name__ == '__main__':
    main()
