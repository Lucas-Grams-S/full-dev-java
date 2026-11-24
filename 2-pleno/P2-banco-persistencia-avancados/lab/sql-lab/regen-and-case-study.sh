#!/usr/bin/env bash
# Recria o banco com o volume (10-big-data.sql, ~1 min) e roda o estudo de caso (exp8). ATENÇÃO: reset apaga o banco 'payflow'.
cd "$(dirname "$0")"
source ./run.sh
reset
$PSQL -f - < 10-big-data.sql 2>&1 | grep -v "^Time: [0-9.]* ms$" > out-big-data.txt
./exp8-case-study.sh > out-exp8.txt 2>&1
echo finished
