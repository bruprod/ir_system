import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Andreas.
 */
public class LeakageHeuristics {
    HashSet<String> leaked_terms = new HashSet<>();
    ArrayList<Doc> result_set_cosine = new ArrayList<>();
    ArrayList<Doc> result_set_prf = new ArrayList<>();
    double leaked_terms_percentage = 0;


    public void setResultSets(ArrayList cosine, ArrayList prf){
        result_set_cosine.addAll(cosine);
        result_set_prf.addAll(prf);
    }

    /**
     * Extracts all terms from the ArrayList of Docs.
     * @param docs ArrayList containing the Docs from which the terms should be extracted
     * @return returns a HashSet of the extracted terms
     */
    public HashSet<String> extractTerms(ArrayList<Doc> docs){
        HashSet<String> terms = new HashSet<>();
        for(Doc doc : docs){
            terms.addAll(doc.getTfidfVector().keySet());
        }
        return terms;
    }

    public HashSet<String> getLeakedTerms(){
        return leaked_terms;
    }

    /**
     * Extracts all leaked terms by building the union of the cosine similarity results and
     * the pseudo relevance feedback results and removes from the union the terms of the
     * intersection between those two and also the terms from the cosine similarity.4
     * Also calculates the percentage of the leaked terms.
     */
    public void retrieveLeakedTerms(){
        HashSet<String> terms_cosine = extractTerms(result_set_cosine);
        HashSet<String> terms_prf = extractTerms(result_set_prf);
        HashSet<String> union = new HashSet<>();
        HashSet<String> intersection = new HashSet<>();
        union.addAll(terms_cosine);
        union.addAll(terms_prf);
        intersection.addAll(terms_cosine);
        intersection.retainAll(terms_prf);
        union.removeAll(intersection);
        union.removeAll(terms_cosine);
        leaked_terms = union;
        leaked_terms_percentage = leaked_terms.size() / (union.size()+0.00001);
    }

    public double getLeakedTermsPercentage() {
        return leaked_terms_percentage;
    }
}
