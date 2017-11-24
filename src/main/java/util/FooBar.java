package util;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.MetaIndex;

import java.io.IOException;

public class FooBar {

    public static void main(String[] args) throws IOException {

        IndexOnDisk index = Index.createIndex();

        MetaIndex metaIndex = index.getMetaIndex();

        int numDocs = index.getCollectionStatistics().getNumberOfDocuments();

        TObjectIntMap<String> docno = new TObjectIntHashMap<String>();
        for (int docid = 0; docid < numDocs; docid++) {

            docno.put(metaIndex.getItem("docno", docid), docid);

        }

        int docid = docno.get("clueweb09-en0003-55-31884");

        System.out.printf("%d\n", docid);

    }
}
