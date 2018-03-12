package util;

import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class LexiconIDF {

    public static void main(String[] args) throws IOException {
        Index i = Index.createIndex();
        int numberOfDocuments = i.getCollectionStatistics().getNumberOfDocuments();
        Iterator<Map.Entry<String, LexiconEntry>> iterator = i.getLexicon().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, LexiconEntry> entry = iterator.next();
            String term = entry.getKey();
            double idf = WeightingModelLibrary.log(numberOfDocuments / entry.getValue().getDocumentFrequency() + 1.0D);
            System.out.println(String.format("%s %.5f", term, idf));
        }
    }

}
