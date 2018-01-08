#!/bin/bash

# this script filter files

CORES=32
COMMAND="time python param-swap-query.py"

for QUERY in `seq 158 200`; 
do
	echo $QUERY
	sem -j $CORES $COMMAND $QUERY > outputs/output-query-$QUERY.txt 
done
sem --wait
exit 0
