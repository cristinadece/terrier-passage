package util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.IndexUtil;
import org.terrier.utility.ApplicationSetup;

import gnu.trove.TIntObjectHashMap;
import matching.RankedOr;
import models.BM25Pexp;
import uk.ac.gla.terrier.jtreceval.trec_eval;

/*
 * java -server -Xmx32G -cp target/terrier-passage-1.0-SNAPSHOT-jar-with-dependencies.jar \
 * -Dterrier.index.path=/home/muntean/cw09b_urls_blocks_nostem \
 * -Dterrier.index.prefix=cw09b_urls_blocks_nostem \
 * -Dtrec.topics=/home/muntean/cw09b_urls_blocks_nostem/eval/wtall.txt \
 * -Dtrec.topics.parser=SingleLineTRECQuery \
 * util.Optimizer -c -M1000 -m all_trec /home/muntean/cw09b_urls_blocks_nostem/eval/wtall.qrels
 */
public class Optimizer extends TRECQuerying {

    public Optimizer() {

        super();

    }

	public static void main(String[] args) throws IOException {

        //SETUP
        Optimizer optimizer = new Optimizer();

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

        //THE REAL THING
        for (int w1=0; w1<6; w1++)
        	for (int w2=0; w2<6; w2++)
        		for (int w3=0; w3<6; w3++)
        			for (int w4=0; w4<6; w4++)
        				for (int w5=0; w5<6; w5++)
        					for (int w6=0; w6<6; w6++)
        						optimizer.runQueries(qid2request, args, w1, w2, w3, w4, w5, w6);
    }

	private void runQueries(TIntObjectHashMap<SearchRequest> qid2request, String[] trec_eval_args, int w1, int w2, int w3, int w4, int w5, int w6) throws IOException {
    	
    	BM25Pexp model = new BM25Pexp();
    	model.setCollectionStatistics(index.getCollectionStatistics());
    	model.w1 = w1;
    	model.w2 = w2;
    	model.w3 = w3;
    	model.w4 = w4;
    	model.w5 = w5;
    	model.w6 = w6;
    	IndexUtil.configure(index, model);
    	    	
    	File tmpResFile = File.createTempFile("terrier", ".tmp");
    	PrintWriter printWriter = new PrintWriter(tmpResFile);
    	
    	RankedOr ror = new RankedOr();
    	ror.setup(index, model);
    	
    	for (int qid : qid2request.keys()) {
    		
    		Request srq = (Request) qid2request.get(qid);
    		ResultSet results = ror.match(srq);    		
    		//ok, we are done processing the query (they are in reverse order, sort them)
    		results.sort();
    		srq.setResultSet(results);
    		printer.printResults(printWriter, srq, method, "Q0", 
    				Integer.parseInt(ApplicationSetup.getProperty("trec.output.format.length", "1000")));
    		
    		
    	}
    	printWriter.close();
    	
    	trec_eval te = new trec_eval();

    	String[] param = new String[trec_eval_args.length+1];
    	System.arraycopy(trec_eval_args, 0, param, 0, trec_eval_args.length);
    	param[trec_eval_args.length] = tmpResFile.toPath().toString();
    	String[][] output = te.runAndGetOutput(param);
    	    	
    	System.out.printf("*** %d,%d,%d,%d,%d,%d,%s\n", w1, w2, w3, w4, w5, w6, Arrays.deepToString(output));
    	
    	tmpResFile.delete();
	}
}
