#!/bin/bash
set -e
cd "$(dirname "$0")"
rm -f main.pdf main.aux main.bbl main.blg main.log main.fls main.fdb_latexmk build.log

run_step() {
	"$@" >> build.log 2>&1 || {
		cat build.log
		exit 1
	}
}

run_step pdflatex -interaction=nonstopmode -shell-escape main.tex
run_step bibtex main
run_step pdflatex -interaction=nonstopmode -shell-escape main.tex
run_step pdflatex -interaction=nonstopmode -shell-escape main.tex

if grep -E '(^!|LaTeX Warning|Package .* Warning|Class .* Warning|Citation .* undefined|Reference .* undefined|undefined references|Emergency stop|Fatal error)' main.log; then
	exit 1
fi

if grep -E '(Warning--|^I couldn|There (was|were) [1-9][0-9]* warning)' main.blg; then
	exit 1
fi

echo "Build completed cleanly."
