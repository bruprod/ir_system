import org.apache.lucene.util.BytesRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Andreas.
 */
public class Doc implements Serializable{
    ArrayList<BytesRef> tokens;
    HashMap<String, Double> tfidf_vector = null;
    String title = null;
    boolean restricted = false;
    ArrayList<CategoryNode> categories = new ArrayList<>();
    HashSet<String> inlinks = new HashSet<>();
    ArrayList<String> outlinks = new ArrayList<>();
    int docId;
    double score;
    double vector_length;
    int init_rank = 0;
    int boosted_rank = 0;


    /**
     * Document constructor, which contains the necessary document data.
     * @param docId The document id to be set
     * @param tokens The ArrayList containing all tokens(terms) in the document
     */
    public Doc(int docId, ArrayList tokens){
        this.docId = docId;
        this.tokens = tokens;
    }

    /**
     * Sets the score and clears the token ArrayList
     * @param score the double value containing the score
     */
    public void setScore(double score){
        this.score = score;
        tokens.clear();
    }

    /**
     * @return returns the documents id
     */
    public int getDocId() {
        return docId;
    }

    /**
     * @return returns the ArrayList with the Lucene by references to the tokens
     */
    public ArrayList<BytesRef> getTokens() {
        return tokens;
    }

    /**
     * @return returns the document score
     */
    public double getScore() {
        return score;
    }

    /**
     * Sets the document title
     * @param title String containing the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return returns the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the vector with the tf-idf values for the document
     * @param tfidf_vector HashMap containing the tf-idf mappings to the tokens
     */
    public void setTfidfVector(HashMap<String, Double> tfidf_vector) {
        this.tfidf_vector = tfidf_vector;
        if(tfidf_vector != null) {
            vector_length = 0;
            for (double val : tfidf_vector.values()) {
                vector_length += val;
            }
        }else
            vector_length = 0;
    }

    /**
     * @return returns the HashMap with the tf-idf mappings
     */
    public HashMap<String, Double> getTfidfVector() {
        return tfidf_vector;
    }

    /**
     * @return returns the length of the tf-idf vector
     */
    public double getVectorLength() {
        return vector_length;
    }

    /**
     * @return returns the ArrayList which contains the parent categories
     */
    public ArrayList<CategoryNode> getCategories() {
        return categories;
    }

    /**
     * Inserts a category node to the ArrayList containing all parent categories
     * @param node The category node to be inserted
     */
    public void addCategory(CategoryNode node){
        if(node != null)
            categories.add(node);
    }

    /**
     * @return returns a boolean which decides if the document is restricted or not
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Sets the boolean which tells if the document is restricted
     * @param restricted boolean to set Doc restricted or unrestricted
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    /**
     * Sets the initial rank from the cosine similarity calculation
     * @param init_rank contains the integer rank after cosine similarity
     */
    public void setInitRank(int init_rank) {
        this.init_rank = init_rank+1;
    }

    /**
     * Sets the boosted rank after the pseudo relevance feedback calculation
     * @param boosted_rank contains the integer rank after pseudo relevance feedback
     */
    public void setBoostedRank(int boosted_rank) {
        this.boosted_rank = boosted_rank+1;
    }

    /**
     * @return returns the initial rank from cosine similarity calculation
     */
    public int getInitRank() {
        return init_rank;
    }

    /**
     * @return returns the boosted rank from pseudo relevance feedback calculation
     */
    public int getBoosted_rank() {
        return boosted_rank;
    }

    /**
     * @return returns the HashSet containing all inlinks to the document
     */
    public HashSet<String> getInlinks() {
        return inlinks;
    }

    /**
     * Adds an inlink to the HashSet
     * @param inlink containing the inlink String
     */
    public void addInlink(String inlink){
        inlinks.add(inlink);
    }

    /**
     * Adds and outlink to the Hashset
     * @param outlink containing the outlink String
     */
    public void addOutlink(String outlink){
        outlinks.add(outlink);
    }

    /**
     * @return returns the ArrayList which contains all Outlinks
     */
    public ArrayList<String> getOutlinks() {
        return outlinks;
    }
}
