import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;

import java.io.IOException;

public class DocLenPrinter {

    public static void main(String[] args) throws IOException {
        Index index = Index.createIndex();
        DocumentIndex documentIndex = index.getDocumentIndex();
        int numberOfDocuments = index.getCollectionStatistics().getNumberOfDocuments();

        for (int i=0; i<numberOfDocuments; i++) {
            System.out.println(documentIndex.getDocumentLength(i));
        }
    }
}
