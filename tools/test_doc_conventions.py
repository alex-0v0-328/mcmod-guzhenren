import tempfile
import unittest
from pathlib import Path

import doc_conventions as docs
import project_runtime as env


def put(root, name, text):
    (root / name).write_bytes(text.encode('utf-8'))



class TableTests(unittest.TestCase):

    def test_tables(self):
        assert docs.display_width('包名 ok') == 7
        assert docs.display_width('≈0.5 – ·') == 8
        assert docs.split_cells('| `a|b` | c\\|d | e |') == [' `a|b` ', ' c\\|d ', ' e ']

        out_of_line = ['| Stage | What |',
                       '|-------|------|',
                       '| `build` | Compile |',
                       '| 宝黄天 | x |']
        fixed, off = docs.align_tables('\n'.join(out_of_line) + '\n')
        assert off == [1], off
        assert fixed.split('\n')[:4] == ['| Stage   | What    |',
                                         '|---------|---------|',
                                         '| `build` | Compile |',
                                         '| 宝黄天  | x       |'], fixed
        again, off = docs.align_tables(fixed)
        assert again == fixed and off == [], 'an aligned table stays as it is'

        compact = '| a | b | c |\n|---|---:|---|\n| long cell | 12 | x |\n'
        assert docs.align_tables(compact) == (compact, []), 'compact tables, colons included, are left alone'

        spaced = ['| jar | 作用 |',
                  '| ----- | ---- |',
                  '| sodium `0.6` | 渲染 |',
                  '| iris | 光影与远景 |']
        fixed_spaced = docs.align_tables('\n'.join(spaced))[0].split('\n')
        assert fixed_spaced == ['| jar          | 作用       |',
                                '| -------------- | ------------ |',
                                '| sodium `0.6` | 渲染       |',
                                '| iris         | 光影与远景 |'], fixed_spaced

        colons = '| n | value |\n|:----|------:|\n| one | 1 |\n'
        assert docs.align_tables(colons)[0].split('\n')[1] == '|:----|------:|'

        crlf = '| a | b |\r\n|-----|-----|\r\n| ccc | d |\r\n'
        assert '\r\n' in docs.align_tables(crlf)[0] and docs.align_tables(crlf)[1] == [1]

        wide = '|        |        |\n|--------|--------|\n| a      | b      |\n| cc     | dd     |\n'
        assert docs.align_tables(wide) == (wide, []), 'a table padded wider than needed still lines up'
        wide_spaced = '| a      | b      |\n| -------- | -------- |\n| cc     | dd     |\n'
        assert docs.align_tables(wide_spaced) == (wide_spaced, [])
        torn = '| a      | b      |\n|--------|--------|\n| a much longer cell | x |\n'
        assert docs.align_tables(torn)[1] == [1], 'a row past its column is out of line'

        repository = Path(__file__).resolve().parents[1]
        for live in ('AGENTS.md', 'README.md', 'README.en.md'):
            assert docs.align_tables((repository / live).read_text(encoding='utf-8'))[1] == [], live


def rules(problems):
    """Each problem without its path:line prefix."""
    return [problem.split(': ', 1)[1] for problem in problems]


class ProseTests(unittest.TestCase):

    def setUp(self):
        env.TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        self.tmp = tempfile.TemporaryDirectory(dir=env.TEMP_ROOT)
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_prose_skips_frontmatter_and_fences_and_masks_quoted_text(self):
        text = '---\ntags: [a~b]\n---\nkeep\n````md\n```java\nnested 1-2\n```\n````\n~~~\nskip\n~~~\nlast\n'
        assert list(docs.prose(text)) == [(4, 'keep'), (13, 'last')]
        assert list(docs.prose('---\nnot frontmatter\n')) == [(1, '---'), (2, 'not frontmatter')]
        mask = docs.MASK
        assert docs.masked('a `x` [[p|q]] [t](u.md) https://e.org/x?y b') == f'a {mask} {mask} [t{mask} {mask} b'

    def test_typography(self):
        put(self.root, 'p.md', '用了3次~5秒, 范围1-2转。\n版本 1.21-2.1.0、1.21-40-NEOFORGE、日期 2026-10-26、2026-10、'
                               '路径 ~/.claude、~~删~~、`代码1-2`、[[蛊真人MOD 1.0.0 总表]]、约 5 秒、1–2 转、（注）：好\n')
        assert rules(docs.typography_problems(self.root / 'p.md')) == [
            "typography: space between Chinese and Latin letters or digits ('了3')",
            "typography: space between Chinese and Latin letters or digits ('3次')",
            "typography: space between Chinese and Latin letters or digits ('5秒')",
            "typography: space between Chinese and Latin letters or digits ('围1')",
            "typography: space between Chinese and Latin letters or digits ('2转')",
            "typography: full-width punctuation next to Chinese ('秒,')",
            "typography: 约 instead of a bare ~ ('~')",
            "typography: – in a number range ('1-2')"]

    def test_headings(self):
        put(self.root, 'page.md', '# page\n\n## 1. 开始\n\n## 2.1 细节\n\n## 一、总览\n\n## 第二章 后来\n\n'
                                  '## 1.0.0 的边界\n\n### 26 秒仪式\n\n## Plain\n\n见 §2\n\n`§` in code\n')
        assert rules(docs.heading_problems(self.root / 'page.md')) == [
            'heading: the H1 repeats the file name, which Obsidian already shows as the title',
            'heading: numbered (## 1. 开始)', 'heading: numbered (## 2.1 细节)', 'heading: numbered (## 一、总览)',
            'heading: numbered (## 第二章 后来)', 'heading: a cross-reference names the heading, never §']
        put(self.root, 'other.md', '# A different title\n')
        assert docs.heading_problems(self.root / 'other.md') == []

    def test_carriers(self):
        (self.root / '玩家向').mkdir()
        (self.root / '开发向').mkdir()
        body = ('由 `PlungeImpactTest.clips` 和 WikiItemsGameTests 钉住；GameTest、GameTestServer、runGameTestServer 不算。'
                '（Alex 2026-11-13 定）`PlungePayload` `mobGriefing` `hope_gu` `ComplementaryUnbound_r5.8.1.zip`\n')
        for page in ('玩家向/p.md', '开发向/p.md', '开发向/维护说明.md'):
            put(self.root, page, body)
        assert rules(docs.carrier_problems(self.root / '玩家向/p.md', self.root)) == [
            'carriers: a test class outside 维护说明; list the untested behavior instead (PlungeImpactTest)',
            'carriers: a test class outside 维护说明; list the untested behavior instead (WikiItemsGameTests)',
            'carriers: 「Alex 定」 carries no date (Alex 2026-11-13)',
            'carriers: a Java name on a 玩家向 page (PlungeImpactTest.clips)',
            'carriers: a Java name on a 玩家向 page (PlungePayload)']
        assert len(docs.carrier_problems(self.root / '开发向/p.md', self.root)) == 3
        assert rules(docs.carrier_problems(self.root / '开发向/维护说明.md', self.root)) == [
            'carriers: 「Alex 定」 carries no date (Alex 2026-11-13)']

    def test_links(self):
        wiki = self.root / 'wiki'
        (wiki / 'sub').mkdir(parents=True)
        (wiki / '.obsidian').mkdir()
        put(wiki, 'home.md', '## 总览\n\n[[sub/page#细节]] [[page#细节#更深]] [[page.md]] ![[pic.png]] [[#总览]] '
                             '[[page#^block]] | [[page\\|别名]] |\n```\n[[in code]]\n```\n`[[in span]]` [[gone]] '
                             '[[page#没有]] [[#也没有]]\n')
        put(wiki / 'sub', 'page.md', '## 细节\n\n### 更深 ##\n')
        put(wiki, 'pic.png', 'png')
        put(wiki / '.obsidian', 'hidden.md', '## x\n')
        vault = docs.Vault(wiki)
        assert sorted(vault.pages) == ['home', 'page'] and vault.files == {'pic.png'}
        assert vault.headings('page') == {'细节', '更深'}
        assert rules(docs.link_problems(wiki / 'home.md', vault)) == [
            'link gone: no wiki page or file "gone"', 'link page#没有: "page" has no heading "没有"',
            'link #也没有: "home" has no heading "也没有"']

        put(self.root, 'CLAUDE.md', 'See wiki《page#细节》 and 《home》, not `《code》`; [[gone]] is wiki syntax.\n'
                                    '《page#缺》《missing》\n')
        assert rules(docs.link_problems(self.root / 'CLAUDE.md', vault)) == [
            'link page#缺: "page" has no heading "缺"', 'link missing: no wiki page or file "missing"']

    def test_qualified_links_resolve_the_requested_duplicate_name(self):
        (self.root / '开发向').mkdir()
        (self.root / '玩家向/1.0.0').mkdir(parents=True)
        put(self.root, '开发向/蛊虫.md', '## 实现\n')
        put(self.root, '玩家向/1.0.0/蛊虫.md', '## 用法\n')
        vault = docs.Vault(self.root)
        assert vault.problem('玩家向/1.0.0/蛊虫#用法') is None
        assert vault.problem('开发向/蛊虫#实现') is None
        assert vault.problem('不存在/蛊虫') is not None

    def test_unqualified_duplicate_name_requires_a_path(self):
        (self.root / '开发向').mkdir()
        (self.root / '玩家向/1.0.0').mkdir(parents=True)
        put(self.root, '开发向/蛊虫.md', '## 实现\n')
        put(self.root, '玩家向/1.0.0/蛊虫.md', '## 用法\n')
        assert 'ambiguous' in docs.Vault(self.root).problem('蛊虫')

    def test_local_heading_uses_the_actual_page_path(self):
        (self.root / '开发向').mkdir()
        (self.root / '玩家向/1.0.0').mkdir(parents=True)
        put(self.root, '开发向/蛊虫.md', '## 实现\n\n[[#实现]]\n')
        put(self.root, '玩家向/1.0.0/蛊虫.md', '## 用法\n\n[[#用法]]\n')
        vault = docs.Vault(self.root)
        assert docs.link_problems(self.root / '开发向/蛊虫.md', vault) == []
        assert docs.link_problems(self.root / '玩家向/1.0.0/蛊虫.md', vault) == []

    def test_default_targets(self):
        assert all(path.suffix == '.md' and '.obsidian' not in path.parts for path in docs.wiki_pages())
        names = [path.relative_to(env.REPOSITORY_ROOT).as_posix() for path in docs.rules_docs()
                 if path.is_relative_to(env.REPOSITORY_ROOT)]
        assert {'CLAUDE.md', 'AGENTS.md', '.claude/skills/commit-push/SKILL.md'} <= set(names), names
        assert '.claude/skills/i-have-adhd/SKILL.md' not in names, 'upstream skills keep their own conventions'
        assert env.REPOSITORY_ROOT / 'README.md' in docs.readmes()


if __name__ == '__main__':
    unittest.main()
