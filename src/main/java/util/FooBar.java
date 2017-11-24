package util;


import gnu.trove.TObjectIntHashMap;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.MetaIndex;

import java.io.IOException;

public class FooBar {

    public static void main(String[] args) throws IOException {

        IndexOnDisk index = Index.createIndex();

        MetaIndex metaIndex = index.getMetaIndex();

        int numDocs = index.getCollectionStatistics().getNumberOfDocuments();

        TObjectIntHashMap docno2docid = new TObjectIntHashMap();

        for (int docid = 0; docid < numDocs; docid++) {

            String s;
            docno2docid.put(s = metaIndex.getItem("docno", docid), docid);
            if (docid % 100000 == 0) System.out.println(s);
        }

        int docid = docno2docid.get("clueweb09-en0003-55-31884");

        System.out.printf("%d\n", docid);

    }
}
