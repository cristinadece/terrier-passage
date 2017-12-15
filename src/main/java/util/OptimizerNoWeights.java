package util;

import gnu.trove.TIntObjectHashMap;
import matching.RankedOrTFPrint;
import models.BM25Pexp;
import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.IndexUtil;


import java.io.*;
import java.util.zip.GZIPOutputStream;

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
//		BufferedWriter bw = new BufferedWriter(new FileWriter(filenameFullPath));


        //THE REAL THING
		optimizer.runQueries(qid2request, filenameFullPath);


    }

	private void runQueries(TIntObjectHashMap<SearchRequest> qid2request, String outputFilename) throws IOException {
    	
    	BM25Pexp model = new BM25Pexp();
    	model.setCollectionStatistics(index.getCollectionStatistics());
    	IndexUtil.configure(index, model);


		RankedOrTFPrint ror = new RankedOrTFPrint();
    	ror.setup(index, model);
    	
    	for (int qid : qid2request.keys()) {
    		String filename = String.format("%s%s%d%s", outputFilename, "-qid-", qid, ".txt.gz");
			System.out.println("*** ".concat(filename));
			GZIPOutputStream gzipOS = new GZIPOutputStream(new FileOutputStream(filename));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(gzipOS, "UTF-8"));
    		
    		Request srq = (Request) qid2request.get(qid);
    		ResultSet results = ror.match(srq, bw);

			bw.close();
    	}
	}
}
