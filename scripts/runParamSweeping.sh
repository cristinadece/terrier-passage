for i in 1 2 3 4 5;
do
    for j in 1 2 3 4 5;
    do
        for k in 1 2 3 4 5;
        do
            for m in 1 2 3 4 5;
            do
                echo
                bin/trec_terrier.sh -r -Dtrec.model=models.BM25P -Dbm25p.w1=$m -Dbm25p.w2=$k -Dbm25p.w3=$j -Dbm25p.w4=$i -Dtrec.topics=/home/muntean/cw09b_urls_blocks_nostem/eval/wtall.txt -Dtrec.results=/home/muntean/grid_search_results -Dtrec.results.file=BM25P$m$k$j$i.res
            done;
        done;
    done;
done