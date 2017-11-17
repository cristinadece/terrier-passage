WORKING_DIR=/home/muntean/grid_search_results
COMMAND="./trec_eval -c -M1000 -m all_trec /home/muntean/cw09b_urls_blocks_nostem/eval/qrels.adhoc.10.11.12.txt"

for LINE in `ls $WORKING_DIR/*.res`
do
    echo $LINE
    OUTPUT_NAME=`basename $LINE | cut -d'.' -f1`
    echo $OUTPUT_NAME
    $COMMAND $LINE  2>&1 > $WORKING_DIR/$OUTPUT_NAME.txt
done
exit