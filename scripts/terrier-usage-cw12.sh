# edit terrier.porpoerties


# 1. Setup Terrier for using a TREC test collection by calling

bin/trec_setup.sh data/ClueWeb09_English_Sample_File.warc.gz

# 2. Index collection as found in collection.spec
# Note: If you do not need the direct index structure for e.g. for query expansion,
# then you can use bin/trec_terrier.sh -i -j for the faster single-pass indexing.
# http://terrier.org/docs/v3.5/configure_indexing.html

# block indexing - unit of text in a document
# block.size must be set

bin/trec_terrier.sh -i # we can add a prefix to the files created xxx.data

# 3. Verify your index statistics

bin/trec_terrier.sh --printstats

# 4. Query the index interactively

bin/interactive_terrier.sh

# 5. Retrieval
# First of all we have to do some configuration: etc/terrier.properties
# location of the queries - trec.topics
# weighting model (e.g. TF_IDF) - trec.model
# relevance assessments file (or qrels) for the topics - trec.qrels
# -r batch retrieval run
# If all goes well this will result in a .res file in the var/results directory

bin/trec_terrier.sh -r -Dtrec.model=BM25 -Dtrec.topics=/home/muntean/cw09b_urls_blocks_nostem/eval/wt2010-topics.queries-only
bin/trec_terrier.sh -e -Dtrec.qrels=/home/muntean/cw09b_urls_blocks_nostem/eval/10.adhoc-qrels.final

# 6. Evaluate obtained results with -e option of trec_terrier

bin/trec_terrier.sh -e -Dtrec.qrels=share/vaswani_npl/qrels

#Dterrier.index.path - where is the index
#Dterrier.index.prefix - .data e l'indice ...