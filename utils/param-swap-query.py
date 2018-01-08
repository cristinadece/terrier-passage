import itertools
import numpy as np
import gzip
import io
import math
import json
import time
from collections import defaultdict
import operator
import tempfile
import sys
import pytrec_eval


# Some helper tools
def pairwise(iterable):
    "s -> (s0,s1), (s1,s2), (s2, s3), ..."
    a, b = itertools.tee(iterable)
    next(b, None)
    return zip(a, b)

def get_all_combinations():
    all_combi = list(itertools.product(range(6), repeat=6))
    return all_combi


# Weights and metrics
def weighted_tf(weights, tfs):
    return sum([a*b for a,b in zip(weights,tfs)])

def idf(N, df):
    return math.log((N - df + 0.5) / (df + 0.5))

def bm25(tf, df, doclen, N, avg_doclen, k1=1.2, b=0.75):
    return idf(N, df) * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (doclen / avg_doclen))))


# Load data
def loadQueryTF(query_tf_filename, all_combi):
    """
    # Read doc txt: TAB separated
    # qid,
    # N,
    # Arrays.toString(documentFrequencies),
    # avgDocLen,
    # docid,
    # docno,
    # doclen,
    # tf_q1, tf_q2, tf_q3, tf_q4, tf_q5, tf_q6
    """
    
    N = 50220423
    AVG_LEN = 963.90334

    bm25Matrix_aslist = list()
    docno_aslist = list()

    print("Starting loading file: ", time.ctime())

    counter = 0
    with open(query_tf_filename, "r") as inputFile:
        for line in inputFile:
            counter+=1
            
            # parse line
            data = line.replace("\n","").split("\t")
            qid = int(data[0])
            df_list = json.loads(data[2])
            docid = int(data[4])
            docno = data[5]
            doclen = int(data[6])
            tf_list = np.array(json.loads(data[7]))
     
            # compute BM25 for current doc for all combinations
            bm25Array = np.zeros(len(all_combi))
            for combo_index, combo in enumerate(all_combi):
                doc_bm25 = 0
                for i, term_tf_list in enumerate(tf_list):  
                    new_tf = weighted_tf(combo, term_tf_list)
                    bm25classic =  bm25(new_tf, df_list[i], doclen, N, AVG_LEN)
                    doc_bm25 += bm25classic
                bm25Array[combo_index] = doc_bm25
            
            if counter%100==0:
                print("Loaded query {} and {} docs. Time: {}".format(qid, counter, time.ctime()))
            
            # append to lists
            bm25Matrix_aslist.append(bm25Array)
            docno_aslist.append(docno)      
        
    print("Finished loading file: ", time.ctime())
    return np.array(bm25Matrix_aslist), docno_aslist


# Prepping for Trec_eval
def rankDocumentsInQuery(matrix, docno_list, weight_index):
    """
    Sort per column
    This should be inside an iteration on all queries
    """
    doc2score = [(a,b) for a, b in zip(docno_list, list(matrix[:,weight_index]))]
    doc2score.sort(key=operator.itemgetter(1), reverse=True)
    return doc2score

def prepareData(qid, doc2score, weight_index, weight_combo):
    """
    prepare data for rankeval - top 1000 docs after ranking according to BM25 score
    # line: 1 Q0 clueweb09-en0010-57-32595 18 26.023832769770642 BM25P
    """ 
    for i, item in enumerate(doc2score[:1000]):
        line = " ".join([qid, "Q0", item[0], str(i), str(item[1]), "BM25P"+"".join([str(x) for x in weight_combo])])
        yield line + "\n"        
        
def createTempFile(matrix, docno_list, weight_index, weight_combo, qid):
    tp = tempfile.NamedTemporaryFile("w+")

    doc2score = rankDocumentsInQuery(matrix, docno_list, weight_index)
    for line in prepareData(qid, doc2score, weight_index, weight_combo):
        tp.write(line)
    tp.flush()
    return tp


# Trec_eval run with weight combo
def format_line(measure, scope, value):
    return '{}:{}:{:.6f}'.format(measure, scope, value)

def trec_eval_weight_combo(tempFile, qrels):
    # Loading data
    with open(tempFile.name, 'r') as f_run:
        run = pytrec_eval.parse_run(f_run)
    
    # Eval
    evaluator = pytrec_eval.RelevanceEvaluator(
        qrels, pytrec_eval.supported_measures)
    results = evaluator.evaluate(run)

    # Compute measures
    measure_list = list()
    for query_id, query_measures in sorted(results.items()):
        for measure, value in sorted(query_measures.items()):
            m = format_line(measure, query_id, value)
            measure_list.append(m)
    return measure_list


# Put it all together    
def main(args):
    
    print("Started processing! ..........", time.ctime())
    
    #0. Parse args and setup 
    queryid = args[1]
    
    query_tf_filename = "/home/muntean/terrier-passage/tfs-per-qid/all-matches-fields-tfs-qid-"+queryid+"-filtered.txt"
    qrel_filename = "/home/muntean/cw09b_urls_blocks_nostem/eval/wt-qid/wt-query-"+queryid+".qrels"
    output_file = "/home/muntean/terrier-passage/tfs-per-qid/trec-eval-all-weights-qid-"+queryid+".txt"
    
    #1. Generate weight combinations
    all_combi = get_all_combinations()
    
    #2. Load data from cluster TF file
    query2BM25Matrix, query2docno = loadQueryTF(query_tf_filename, all_combi)
    
    #3. Trec eval for weight combination 
    with open(qrel_filename, 'r') as f_qrel:
        qrels = pytrec_eval.parse_qrel(f_qrel)
    
    print("Started processing combinations: ", time.ctime())
    with open(output_file, "w") as outputFile:
        for weight_index, weight_combo in enumerate(all_combi):
            tempFile = createTempFile(query2BM25Matrix, query2docno, weight_index, weight_combo, queryid)
            measure_list = trec_eval_weight_combo(tempFile, qrels)
            outputFile.write("".join([str(x) for x in weight_combo]) + "\t" + ",".join(measure_list) + "\n")
            if weight_index%1000==0:
                print("Processed combinations: ", weight_index)
    print("Finished processing combinations: ", time.ctime())
        
if __name__ == "__main__":
    main(sys.argv)        
    