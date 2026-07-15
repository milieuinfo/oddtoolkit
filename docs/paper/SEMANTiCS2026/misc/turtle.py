# -*- coding: utf-8 -*-
"""
    pygments.lexers.ttl
    ~~~~~~~~~~~~~~~~~~~
    Lexer for Turtle semantic web and RDF language
"""

import re

from pygments.lexer import RegexLexer, bygroups, default
from pygments.token import Keyword, Punctuation, String, Number, Operator, Generic, \
    Whitespace, Name, Literal, Comment, Text

__all__ = ['TurtleFixLexer']


class TurtleFixLexer(RegexLexer):
    """
    Lexer for `Turtle <http://www.w3.org/TR/turtle/>` data language.
    """

    name = 'TurtleFix'
    aliases = ['turtle-fix']
    filenames = ['*.ttl']
    mimetypes = ['text/turtle', 'application/x-turtle']

    flags = re.IGNORECASE

    patterns = {
        'PNAME_NS': r'((?:[a-zA-Z][\w-]*)?\:)',  # Namespace Prefix
        'IRIREF': r'(<[^<>"{}|^`\\\x00-\x20]*>)' # IRI reference
    }

    # PNAME_NS PN_LOCAL (with simplified character range)
    patterns['PrefixedName'] = r'%(PNAME_NS)s([a-z][\w-]*)' % patterns

    tokens = {
        'root': [
            # Whitespace
            (r'\s+', Whitespace),

            # Base IRI
            (r'(@base|BASE)(\s+)%(IRIREF)s(\s*)(\.?)' % patterns,
             bygroups(Keyword, Whitespace, Name.Variable, Whitespace,
                      Punctuation)),

            # Prefix IRI
            (r'(@prefix|PREFIX)(\s+)%(PNAME_NS)s(\s+)%(IRIREF)s(\s*)(\.?)' % patterns,
             bygroups(Keyword, Whitespace, Name.Namespace, Whitespace,
                      Name.Variable, Whitespace, Punctuation)),

            # The shorthand predicate 'a'
            (r'(?<=\s)a(?=\s)', Keyword.Type),

            # IRI Reference
            (r'%(IRIREF)s' % patterns, Name.Variable),

            # Prefix:Name
            (r'%(PrefixedName)s' % patterns,
             bygroups(Name.Namespace, Name.Tag)),

            # Comment
            (r'#[^\n]+', Comment),

            # Boolean values
            (r'\b(true|false)\b', Literal),

            # Numbers
            (r'[+\-]?\d*\.\d+', Number.Float),
            (r'[+\-]?\d*(:?\.\d+)?E[+\-]?\d+', Number.Float),
            (r'[+\-]?\d+', Number.Integer),

            # Punctuation
            (r'[\[\](){}.;,:^]', Punctuation),

            # Strings
            (r'"""', String, 'triple-double-quoted-string'),
            (r'"', String, 'single-double-quoted-string'),
            (r"'''", String, 'triple-single-quoted-string'),
            (r"'", String, 'single-single-quoted-string'),
        ],
        'triple-double-quoted-string': [
            (r'"""', String, 'end-of-string'),
            (r'[^"\\]+', String),
            (r'\\', String, 'string-escape'),
        ],
        'single-double-quoted-string': [
            (r'"', String, 'end-of-string'),
            (r'[^"\\\n]+', String),
            (r'\\', String, 'string-escape'),
        ],
        'triple-single-quoted-string': [
            (r"'''", String, 'end-of-string'),
            (r"[^'\\]+", String),
            (r'\\', String, 'string-escape'),
        ],
        'single-single-quoted-string': [
            (r"'", String, 'end-of-string'),
            (r"[^'\\\n]+", String),
            (r'\\', String, 'string-escape'),
        ],
        'string-escape': [
            (r'.', String, '#pop'),
        ],
        'end-of-string': [

            # Language Tag
            (r'(@)([a-zA-Z]+(:?-[a-zA-Z0-9]+)*)',
             bygroups(Operator, Generic.Emph), '#pop:2'),

            # Datatype
            (r'(\^\^)%(IRIREF)s' % patterns, bygroups(Operator, Generic.Emph), '#pop:2'),
            (r'(\^\^)%(PrefixedName)s' % patterns,
             bygroups(Operator, Generic.Emph, Generic.Emph), '#pop:2'),

            default('#pop:2'),

        ],
    }
# END TtlLexer