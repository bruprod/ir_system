import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Andreas.
 */
public class Topic {
    String topic_name;
    ArrayList<Doc> topic_relevant_docs;
    HashSet<Doc> topic_relevant_docs_stackexchange = new HashSet<>();
    double avg_improvement = 0;
    double precision = 0.0;
    double recall = 0.0;
    static int NUMBER_SAMPLES_PER_RESTRICTION = 1;
    double percentage_progbar;
    JProgressBar prog_bar;
    ArrayList<Double> avg_precisions;
    ArrayList<Double> avg_precisions_cosine;
    ArrayList<Double> avg_recall;
    ArrayList<Double> avg_recall_cosine;
    int resultset_size = 10;
    ArrayList<Double> avg_precisions_map_cosine = new ArrayList<>();
    ArrayList<Double> avg_precisions_map_prf = new ArrayList<>();
    ArrayList<Double> avg_precisions_map_bm25 = new ArrayList<>();

    ArrayList<Integer> precision_at_ten_cos = new ArrayList<>();
    ArrayList<Integer> getPrecision_at_ten_prf = new ArrayList<>();
    ArrayList<Integer> getPrecision_at_ten_bm25 = new ArrayList<>();
    ArrayList<Integer> avg_number_leaked_terms = new ArrayList<>();
    ArrayList<Double> percentage_leaked_terms = new ArrayList<>();

    public ArrayList<Double> getAvg_precisions_map_bm25() {
        return avg_precisions_map_bm25;
    }

    public ArrayList<Integer> getGetPrecision_at_ten_bm25() {
        return getPrecision_at_ten_bm25;
    }

    public ArrayList<Integer> getAvg_number_leaked_terms_bm25() {
        return avg_number_leaked_terms_bm25;
    }

    ArrayList<Integer> avg_number_leaked_terms_bm25 = new ArrayList<>();

    /**
     * Topic Constructor which creates a new Topic.
     * A Topic describes a query and the relevant documents to a query.
     * @param topic_name contains the Topic name and at the same time the query for the Topic.
     */
    public Topic(String topic_name) {
        this.topic_name = topic_name;
        topic_relevant_docs = new ArrayList<>();
    }

    /**
     * @return returns the ArrayList containing all relevant Documents from the Topic.
     */
    public ArrayList<Doc> getTopicRelevantDocs() {
        return topic_relevant_docs;
    }

    /**
     * extracts the relevant documents from all documents in the index
     * @param documents Contains all indexed documents
     */
    public void retrieveTopicRelevantDocs(HashMap<String, Doc> documents) {
        if(topic_relevant_docs_stackexchange.size() == 0) {
            Doc topic_doc = documents.get(topic_name.toLowerCase());
            ArrayList<String> inlinks = new ArrayList<>(topic_doc.getInlinks());
            for (String link : inlinks) {
                Doc doc = documents.get(link);
                topic_relevant_docs.add(doc);
            }
        }
        else {
            topic_relevant_docs.addAll(topic_relevant_docs_stackexchange);
        }
    }


    /**
     * This function performs the evaluation for the topic, by calculating the Average precision
     * over 10 restriction levels. With the variable NUMBER_SAMPLES_PER_RESTRICTION it is possible to change the
     * number of the amount of samples taken per restriction level for getting a better approximation.
     * The evaluation is speed up by using an own method call for
     * caulating the cosine similarity and the pseudo relevance feedback.
     * @param vs The initialized VectorSpaceModel which is responsible for the retrieval
     * @param number_top_ranked integer defining how many top ranked documents to take for pseudo relevance feedback
     */
    public void evaluateTopic(VectorSpaceModel vs, int number_top_ranked) {
        avg_precisions = new ArrayList<>();
        avg_recall = new ArrayList<>();
        avg_precisions_cosine = new ArrayList<>();
        avg_recall_cosine = new ArrayList<>();
        avg_number_leaked_terms = new ArrayList<>();
        double progress = percentage_progbar / (10*NUMBER_SAMPLES_PER_RESTRICTION);
        double progress_counter = prog_bar.getValue();
        for (int amount_restricted = 0; amount_restricted < 100; amount_restricted += 10) {
            vs.setAmountRestricted(amount_restricted);
            double sum_precision = 0;
            double sum_precision_cosine_sim = 0;
            double sum_recall = 0;
            double sum_precision_map_cosine = 0;
            double sum_precision_map_prf = 0;
            double sum_precision_map_bm25 = 0;
            for (int i = 0; i < NUMBER_SAMPLES_PER_RESTRICTION; i++) {
                ArrayList<Doc> result_set = new ArrayList<>();

                vs.scoreDocs_cosine(true);
                precision_at_ten_cos.add(precisionAtTen(vs.getResultSet()));
                result_set = vs.getResultSet();
                LeakageHeuristics leakage = new LeakageHeuristics();
                sum_precision_map_cosine += calculatingAveragePrecision(result_set) / NUMBER_SAMPLES_PER_RESTRICTION;
                ArrayList<Doc> result_set_cosine = new ArrayList<>();
                retrieveResultSet(result_set, result_set_cosine, resultset_size);

                vs.PseudoRelevanceFeedbackRocchio(number_top_ranked, vs.getQueryDoc());
                result_set.clear();
                result_set = vs.getResultSet();
                getPrecision_at_ten_prf.add(precisionAtTen(result_set));

                ArrayList<Doc> result_set_prf = new ArrayList<>();
                retrieveResultSet(result_set, result_set_prf, resultset_size);
                leakage.setResultSets(result_set_cosine, result_set_prf);
                leakage.retrieveLeakedTerms();
                HashSet<String> leaked_terms = leakage.getLeakedTerms();
                avg_number_leaked_terms.add(leaked_terms.size());
                percentage_leaked_terms.add(leakage.getLeakedTermsPercentage());
                sum_precision_map_prf += calculatingAveragePrecision(result_set) / NUMBER_SAMPLES_PER_RESTRICTION;

                vs.BM25Scoring(vs.getQueryDoc());
                result_set.clear();
                result_set = vs.getResultSet();
                getPrecision_at_ten_bm25.add(precisionAtTen(result_set));

                ArrayList<Doc> result_set_bm25 = new ArrayList<>();
                retrieveResultSet(result_set, result_set_bm25, resultset_size);
                leakage.setResultSets(result_set_cosine, result_set_bm25);
                leakage.retrieveLeakedTerms();
                HashSet<String> leaked_terms_bm25 = leakage.getLeakedTerms();
                avg_number_leaked_terms_bm25.add(leaked_terms_bm25.size());
                sum_precision_map_bm25 += calculatingAveragePrecision(result_set) / NUMBER_SAMPLES_PER_RESTRICTION;

                progress_counter += progress;
                prog_bar.setValue((int) progress_counter);
                prog_bar.setString(String.format("%.2f", progress_counter) + "%");

                //System.out.println("Number leaked terms " + leaked_terms.size());
            }
                avg_precisions.add(sum_precision / NUMBER_SAMPLES_PER_RESTRICTION);
                avg_precisions_cosine.add(sum_precision_cosine_sim / NUMBER_SAMPLES_PER_RESTRICTION);
                avg_recall.add(sum_recall / NUMBER_SAMPLES_PER_RESTRICTION);
                avg_recall_cosine.add(sum_precision_cosine_sim / NUMBER_SAMPLES_PER_RESTRICTION);
                avg_precisions_map_cosine.add(sum_precision_map_cosine);
                avg_precisions_map_prf.add(sum_precision_map_prf);
                avg_precisions_map_bm25.add(sum_precision_map_bm25);

        }
        vs.resetDocs(true);
        System.gc();
        System.out.println("AVG Precision MAP COSINE: " + avg_precisions_map_cosine);
        System.out.println("AVG Precision MAP PRF: " + avg_precisions_map_prf);
        System.out.println("AVG Precision MAP BM25: " + avg_precisions_map_bm25);
    }

    /**Old Function which should calculate the average over all ranks from the set of relevant documents.
     *
     */
    public void calcAverageRanks() {
        int sum_difference = 0;
        int number_of_ranked_docs = 0;
        for (Doc doc : topic_relevant_docs) {
            int diff = doc.getInitRank() - doc.getBoosted_rank();
            sum_difference += diff;
            if (!(doc.getInitRank() == 0))
                number_of_ranked_docs++;
            System.out.println("Difference: " + diff + " Doc " + doc.getTitle());

        }
        avg_improvement = sum_difference / (double) number_of_ranked_docs;

    }

    /**
     * @return returns the name of the topic
     */
    public String getTopicName() {
        return topic_name;
    }

    /**
     * @return returns the value of the average improvement over
     * all documents from the set of the topic relevant documents
     */
    public double getAvgImprovement() {
        return avg_improvement;
    }

    /**
     * Calculates precision and recall for the topic, after applying the cosine similarity calculation
     * or the pseudo relevance feedback calculation.
     * @param resultset contains the resultset from cosine similarity caluclation
     *                  or pseudo relevance feedback calculation
     */
    public void calcPrecisionAndRecall(ArrayList<Doc> resultset) {
        ArrayList<Doc> intersection = new ArrayList<>(resultset);
        intersection.retainAll(topic_relevant_docs);
        int intersection_size = intersection.size();
        recall = intersection_size / (double) topic_relevant_docs.size();
        precision = intersection_size / (double) resultset.size();
    }

    /**
     * @return returns the precision value
     */
    public double getPrecision() {
        return precision;
    }

    /**
     * @return returns the recall value
     */
    public double getRecall() {
        return recall;
    }

    /**
     * @return returns the ArrayList with the average precisions for pseudo relevance feedback
     */
    public ArrayList<Double> getAvgPrecisions(){
        return avg_precisions;
    }

    /**
     * @return returns the average recall
     */
    public ArrayList<Double> getAvgRecall() {
        return avg_recall;
    }

    /**
     * @return returns the ArrayList with the average Precision for cosine similarity evaluation
     */
    public ArrayList<Double> getAvgPrecisionsCosine() {
        return avg_precisions_cosine;
    }

    /**
     * @return returns the ArrayList with the average recall values for cosine similarity
     *         and pseudo relevance feedback
     */
    public ArrayList<Double> getAvgRecallCosine() {
        return avg_recall_cosine;
    }

    /**
     * Extracts the result set from the ArrayList containing all retrieved documents
     * and by not considering the restricted documents
     * @param docs ArrayList containing all documents retrieved
     * @param result_set ArrayList in which the result set should be stored
     * @param resultset_size The size how big the result set should be
     */
    public void retrieveResultSet(ArrayList<Doc> docs, ArrayList<Doc> result_set, int resultset_size ){
        int NUMBER_DOCS_IN_RESULTSET = resultset_size;
        int nd_count = 1;
        for (Doc doc : docs) {
            if (!doc.isRestricted()) {
                result_set.add(doc);
                nd_count++;
            }
            if(nd_count == NUMBER_DOCS_IN_RESULTSET)
                break;
        }
    }

    /**
     * Sets the progress bar for the Topic evaluation
     * @param progbar The JProgressBar itself which should be set
     * @param percentage The current percentage of the JProgressBar
     */
    public void setProgBar(JProgressBar progbar, double percentage){
        prog_bar = progbar;
        percentage_progbar = percentage;
    }

    /**
     * Calculates the average precision over all retrieved documents, by calculating the precision at every
     * position of a relevant document.
     * @param documents contains all retrieved documents
     * @return returns the average precision for the Mean Average precision calculation and therefore the evaluation
     */
    public double calculatingAveragePrecision(ArrayList<Doc> documents){
        HashSet<Doc> relevant_docs = new HashSet<>(topic_relevant_docs);
        ArrayList<Double> precision_points = new ArrayList<Double>();
        int rel_docs_retrieved = 0;
        for(int i = 0; i < documents.size(); i++){
            Doc doc = documents.get(i);
            if(relevant_docs.contains(doc)) {
                rel_docs_retrieved++;
                double precision = (double) rel_docs_retrieved / (i+1);
                //double recall = (double) rel_docs_retrieved / relevant_docs.size();

                precision_points.add(precision);
            }
        }
        double sum_precision_points = 0;
        for(double prec : precision_points){
            sum_precision_points += prec;
        }
        int size = (precision_points.size() > 0) ? precision_points.size() : 1;
        return sum_precision_points / size;

    }

    /**
     * @return returns the ArrayList with the average precisions for cosine similarity
     * evaluation.
     */
    public ArrayList<Double> getAvgPrecisionsMapCosine() {
        return avg_precisions_map_cosine;
    }

    /**
     * @return returns the ArrayList with the average precisions for pseudo relevance
     * feedback evaluation.
     */
    public ArrayList<Double> getAvgPrecisionsMapPRF() {
        return avg_precisions_map_prf;
    }

    public int precisionAtTen(ArrayList<Doc> resultset){
        ArrayList<Doc> top_10_docs = new ArrayList<>();
        int result_set_size = 10;
        retrieveResultSet(resultset, top_10_docs, result_set_size);
        int relevant_count = 0;
        for(Doc doc : top_10_docs){
            if(topic_relevant_docs.contains(doc)){
                relevant_count++;
            }
        }
        return relevant_count;

    }

    public ArrayList<Integer> getPrecisionAtTenCos() {
        return precision_at_ten_cos;
    }

    public ArrayList<Integer> getGetPrecisionAtTenPrf() {
        return getPrecision_at_ten_prf;
    }

    public ArrayList<Integer> getAvgNumberLeakedTerms() {
        return avg_number_leaked_terms;
    }

    public void setTopicRelevantDocsStackexchange(Doc doc){
        topic_relevant_docs_stackexchange.add(doc);
    }

    public HashSet<Doc> getTopicRelevantDocsStackexchange(){
        return topic_relevant_docs_stackexchange;
    }

    public ArrayList<Double> getPercentageLeakedTerms(){return percentage_leaked_terms;}


}
