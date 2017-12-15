package util;

import gnu.trove.TIntObjectHashMap;
import matching.RankedOrTFPrint;
import models.BM25Pexp;
import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.IndexUtil;
import org.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.jtreceval.trec_eval;

import java.io.*;
import java.util.Arrays;

/*
 * java -server -Xmx32G -cp target/terrier-passage-1.0-SNAPSHOT-jar-with-dependencies.jar \
 * -Dterrier.index.path=/home/muntean/cw09b_urls_blocks_nostem \
 * -Dterrier.index.prefix=cw09b_urls_blocks_nostem \
 * -Dtrec.topics=/home/muntean/cw09b_urls_blocks_nostem/eval/wtall.txt \
 * -Dtrec.topics.parser=SingleLineTRECQuery \
 * util.OptimizerNoWeight Filename
 */
public class OptimizerNoWeights extends TRECQuerying {

    public OptimizerNoWeights() {

        super();

    }

	public static void main(String[] args) throws IOException {

        //SETUP
        OptimizerNoWeights optimizer = new OptimizerNoWeights();

        //load the queries
        TIntObjectHashMap<SearchRequest> qid2request = new TIntObjectHashMap<>();
        while (optimizer.querySource.hasNext()) {

            String query = optimizer.querySource.next();
            String qidString = optimizer.querySource.getQueryId();
            int qid = Integer.parseInt(qidString);

            SearchRequest searchRequest = optimizer.queryingManager.newSearchRequest(
                    qidString, query);

            qid2request.put(qid, searchRequest);
        }

        // open file for writing
		String filenameFullPath = args[args.length-1];
		System.out.println("*** ".concat(filenameFullPath));
		BufferedWriter bw = new BufferedWriter(new FileWriter(filenameFullPath));

        //THE REAL THING
		optimizer.runQueries(qid2request, bw);

        bw.close();
    }

	private void runQueries(TIntObjectHashMap<SearchRequest> qid2request, BufferedWriter bw) throws IOException {
    	
    	BM25Pexp model = new BM25Pexp();
    	model.setCollectionStatistics(index.getCollectionStatistics());
    	IndexUtil.configure(index, model);


		RankedOrTFPrint ror = new RankedOrTFPrint();
    	ror.setup(index, model);
    	
    	for (int qid : qid2request.keys()) {
    		
    		Request srq = (Request) qid2request.get(qid);
    		ResultSet results = ror.match(srq, bw);
    	}
	}
}
