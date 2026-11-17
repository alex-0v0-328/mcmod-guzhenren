import json
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path
from unittest import mock

import wiki_glossary as glossary


SECTIONS = (
    ('修炼', 'guzhenren', ('guzhenren.enum.aperture.rank.', 'guzhenren.display.aperture_1')),
    ('世界', 'guworld', ('dimension.guworld.',)),
)
LANGS = {
    'guzhenren': (
        {'guzhenren.enum.aperture.rank.two': '二转', 'guzhenren.enum.aperture.rank.one': '一转',
         'guzhenren.enum.aperture.rank.none': '', 'guzhenren.display.aperture_1': '第一空窍',
         'guzhenren.display.aperture_10': '不收', 'guzhenren.enum.aperture.rank.line': '%s转'},
        {'guzhenren.enum.aperture.rank.two': 'Rank II', 'guzhenren.enum.aperture.rank.one': 'Rank I',
         'guzhenren.enum.aperture.rank.none': '', 'guzhenren.display.aperture_1': 'First Aperture',
         'guzhenren.display.aperture_10': 'Left out', 'guzhenren.enum.aperture.rank.line': 'Rank %s'}),
    'guworld': ({'dimension.guworld.southern_border': '南疆'}, {'dimension.guworld.southern_border': 'Southern Border'}),
}


def write_langs(base):
    roots = {}
    for mod, (zh, en) in LANGS.items():
        root = base / mod
        folder = root / glossary.LANG_DIR.format(mod=mod)
        folder.mkdir(parents=True)
        (folder / 'zh_cn.json').write_text(json.dumps(zh, ensure_ascii=False), encoding='utf-8')
        (folder / 'en_us.json').write_text(json.dumps(en), encoding='utf-8')
        roots[mod] = root
    return roots


def run(argv, roots, wiki):
    out, err = StringIO(), StringIO()
    with redirect_stdout(out), redirect_stderr(err):
        code = glossary.main(argv, roots=roots, wiki=wiki)
    return code, out.getvalue(), err.getvalue()


@mock.patch.object(glossary, 'SECTIONS', SECTIONS)
class WikiGlossaryTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.base = Path(temp.name)
        self.roots = write_langs(self.base)
        self.wiki = self.base / 'wiki'
        self.wiki.mkdir()
        self.page = self.wiki / glossary.PAGE

    def test_write_orders_terms_and_leaves_out_blanks_and_templates(self):
        self.assertEqual(0, run(['--write'], self.roots, self.wiki)[0])
        text = self.page.read_text(encoding='utf-8')
        rows = [line for line in text.splitlines() if line.startswith('| ') and '`' in line]
        self.assertEqual([
            '| 一转 | Rank I | `guzhenren.enum.aperture.rank.one` |',
            '| 二转 | Rank II | `guzhenren.enum.aperture.rank.two` |',
            '| 第一空窍 | First Aperture | `guzhenren.display.aperture_1` |',
            '| 南疆 | Southern Border | `dimension.guworld.southern_border` |'], rows)
        self.assertLess(text.index('## 修炼'), text.index('## 世界'))
        self.assertTrue(text.startswith('## 修炼\n'))
        self.assertTrue(text.endswith(glossary.MANUAL_STUB))
        self.assertNotIn(b'\r', self.page.read_bytes())
        self.assertEqual(0, run([], self.roots, self.wiki)[0])

    def test_write_keeps_the_hand_written_section(self):
        run(['--write'], self.roots, self.wiki)
        head, _ = glossary.split(self.page.read_text(encoding='utf-8'))
        manual = f'{glossary.MANUAL_HEADING}\n\n| 中文 | English |\n|---|---|\n| 蛊师 | Gu Master |\n'
        self.page.write_text(head + manual, encoding='utf-8')
        self.assertEqual(0, run(['--write'], self.roots, self.wiki)[0])
        self.assertTrue(self.page.read_text(encoding='utf-8').endswith(manual))

    def test_check_reports_a_changed_translation_and_a_missing_page(self):
        code, _, err = run([], self.roots, self.wiki)
        self.assertEqual(1, code)
        self.assertIn('out of date', err)
        run(['--write'], self.roots, self.wiki)
        self.page.write_text(self.page.read_text(encoding='utf-8').replace('Rank II', 'Second Rank'), encoding='utf-8')
        code, _, err = run([], self.roots, self.wiki)
        self.assertEqual(1, code)
        self.assertIn('-| 二转 | Second Rank |', err)
        self.assertIn('+| 二转 | Rank II |', err)

    def test_a_selector_without_keys_or_a_key_selected_twice_fails(self):
        with mock.patch.object(glossary, 'SECTIONS', (('修炼', 'guzhenren', ('guzhenren.missing.',)),)):
            with self.assertRaises(SystemExit):
                run(['--write'], self.roots, self.wiki)
        twice = (('修炼', 'guzhenren', ('guzhenren.display.aperture_1', 'guzhenren.display.')),)
        with mock.patch.object(glossary, 'SECTIONS', twice):
            with self.assertRaises(SystemExit):
                run(['--write'], self.roots, self.wiki)


if __name__ == '__main__':
    unittest.main()
