package util;

import org.terrier.structures.Index;
import org.terrier.structures.MetaIndex;

import java.io.IOException;

public class FooBar {

    public static void main(String[] args) throws IOException {

        MetaIndex metaIndex =
                Index.createIndex().getMetaIndex();

        int docid = metaIndex.getDocument("docno", "clueweb09-enwp01-31-11362");

        System.out.printf("%d\n", docid);

    }
}
