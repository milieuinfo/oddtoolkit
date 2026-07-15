#!/bin/bash
cd "$(dirname "$0")"
rm -f main.pdf main.aux main.bbl main.blg main.log main.fls main.fdb_latexmk
pdflatex -interaction=nonstopmode -shell-escape main.tex
bibtex main
pdflatex -interaction=nonstopmode -shell-escape main.tex
pdflatex -interaction=nonstopmode -shell-escape main.tex
