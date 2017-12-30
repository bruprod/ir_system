
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import javax.swing.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.frequency;
import static org.apache.lucene.index.MultiFields.getTerms;

/**
 * Created by Andreas.
 */
public class VectorSpaceModel {
    boolean CATEGORIES_ENABLED = false;
    int[] seeds = {42, 49, 7582, 483, 798, 198, 16565, 4561, 46513, 98723};
    ArrayList<ArrayList<Integer>> docs_restricted = new ArrayList<>();
    BooleanQuery query = null;
    TermQuery t_query = null;
    Doc query_doc = null;
    IndexReader reader;
    ArrayList<Doc> docs;
    ArrayList<BytesRef> collection_terms = new ArrayList<>();
    HashMap<BytesRef, Integer> document_frequencies = new HashMap<>();
    HashMap<BytesRef, Double> inverse_doc_frequencies = new HashMap<>();
    HashMap<String, CategoryNode> categories = new HashMap<>();
    CategoryTree category_tree;
    HashMap<String, CategoryNode> possible_root_categories = new HashMap<>();
    HashMap<String, Doc> documents = new HashMap<>();
    ArrayList<CategoryNode> restricted_categories = new ArrayList<>();
    HashMap<BytesRef, String> terms_string_map = new HashMap<>();
    ArrayList<String> doc_name = new ArrayList<>();
    JProgressBar prog_bar;
    ArrayList<Doc> result_set;
    String content = "content";
    HashMap<String, Topic> tag_to_topic = new HashMap<>();
    HashMap<String, String> co_occurrences = new HashMap<>();
    int amount_restricted = 0;
    int number_docs;
    int number_terms = 0;

    /**
     * The Constructor of the VectorSpaceModel, which initializes a document frequency map which holds the mapping
     * to the document frequency of a term. Also a ArrayList with Doc objects of all pages in the index is initialized
     * and therefore also the inlinks for a doc are set.
     * @param query initializing query to fill the cache
     * @param reader the IndexReader
     * @param prog_bar a progress bar object to display the current progress of initialization
     */
    public VectorSpaceModel(BooleanQuery query, IndexReader reader, JProgressBar prog_bar){
        this.prog_bar = prog_bar;
        System.out.println("Initializing vector space model");
        this.reader = reader;
        this.query = query;
        docs = new ArrayList<>();
        number_docs = reader.maxDoc();
        TermsEnum coll_terms_enum = null;

        /*for(int i = 10; i < 100;i += 10 ){
            Random rand_gen = new Random(seeds[i/10]);
            int doc_count = 0;
            int restricted_docs = number_docs / i;
            HashSet<Integer> already_drawn = new HashSet<>();
            ArrayList<Integer> restricted_docs_list = new ArrayList<>();
            while(doc_count < restricted_docs){
                int rand_num = rand_gen.nextInt(number_docs);
                if(!already_drawn.contains(rand_num)){
                    restricted_docs_list.add(rand_num);
                    already_drawn.add(rand_num);
                    doc_count++;
                }
            }
            docs_restricted.add(restricted_docs_list);
        }*/

        //System.out.println(docs_restricted.get(0));

        try {

            BytesRef ref = null;
            if(getTerms(reader, content) != null)
                coll_terms_enum = getTerms(reader, content).iterator();
            while((ref = coll_terms_enum.next()) != null){
                BytesRef save = new BytesRef(ref.utf8ToString());
                //collection_terms.add(save);
                terms_string_map.put(save, ref.utf8ToString());
                int doc_frequency = reader.docFreq(new Term(content, save.utf8ToString()));
                double epsilon = 0.0000000000000000001;
                double idf = Math.log(number_docs / (doc_frequency+epsilon));
                inverse_doc_frequencies.put(save, idf);
                number_terms++;
                //document_frequencies.put(save, reader.docFreq(new Term(content, save.utf8ToString())));
            }
            prog_bar.setValue(prog_bar.getValue()+5);
            System.out.println("Collection terms: " + collection_terms.size());

            for(int i = 0; i < number_docs; i++){
                Document index_doc = reader.document(i);
                String title = index_doc.get("title").toLowerCase();
                Doc title_doc = new Doc(i, new ArrayList());
                title_doc.setTitle(title);
                doc_name.add(title);
                documents.put(title, title_doc);




                String outlinks_string = index_doc.get("outlinks");
                String[] outlinks = outlinks_string.split(";");

                for(String link : outlinks ){
                    title_doc.addOutlink(link.toLowerCase());
                }

                String tags_string = index_doc.get("tags");
                if(tags_string != null) {
                    String[] tags = tags_string.split(";");

                    for (String tag : tags) {
                        Topic topic = tag_to_topic.get(tag);
                        if (topic == null) {
                            topic = new Topic(tag);
                            tag_to_topic.put(tag, topic);
                        }
                        topic.setTopicRelevantDocsStackexchange(title_doc);
                    }
                }
            }

            for(Doc doc : documents.values()){
                ArrayList<String> outlinks = doc.getOutlinks();
                for(String link : outlinks){
                    Doc inlink_doc = documents.get(link);
                    if(inlink_doc != null)
                        inlink_doc.addInlink(doc.getTitle().toLowerCase());
                }
                outlinks.clear();
            }
            prog_bar.setValue(prog_bar.getValue()+5);

            File co_occurrence_file = new File(Paths.get("").toAbsolutePath() + "\\index\\co_occurrences.txt");
            if(co_occurrence_file.exists()){
                FileInputStream fs;
                try {
                    fs = new FileInputStream(co_occurrence_file);
                    String co_occurrences_string;
                    BufferedReader ir = new BufferedReader(new InputStreamReader(fs));
                    while((co_occurrences_string = ir.readLine()) != null){
                        //System.out.println(co_occurrences_string);
                        String[] co_oc_arr = co_occurrences_string.split(";");
                        System.out.println("cooc size " + co_oc_arr.length);
                        for(String co_oc : co_oc_arr) {
                            String  delimiter = Pattern.quote("|");
                            String from = co_oc.split(delimiter)[0];
                            String to =  co_oc.split(delimiter)[1];
                            System.out.println("from: " + from + " to: " + to);
                            co_occurrences.put(from, to);
                        }
                    }
                    fs.close();

                } catch (FileNotFoundException e) {
                    //e.printStackTrace();
                    //System.exit(2);
                }
            }
            System.out.println("Cooc map size: " + co_occurrences.size());

            prog_bar.setValue(prog_bar.getValue()+10);

            System.out.println("Documents size: " + documents.size());

            category_tree = new CategoryTree(categories, new CategoryNode("not enabled", new ArrayList<>()));
            if(CATEGORIES_ENABLED) {
                buildCategoryTree();
                System.out.println("Contents childs " + categories.get("Contents").getSubCategories().size());
                for (CategoryNode node : categories.get("Contents").getSubCategories()) {
                    System.out.println("Child: " + node.categorie_name);
                }

                CategoryNode root_node = categories.get("Contents");
                category_tree = new CategoryTree(categories, root_node);
                System.out.println("Category size " + categories.size());
                System.out.println("Possible roots size " + possible_root_categories.size());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scoring all documents by summing up the tfidf values of a document.
     */
    public void scoreDocs(){
        ArrayList<String> query_terms = new ArrayList<>();
        if(query != null) {
            List<BooleanClause> query_clauses = query.clauses();
            for (BooleanClause clause : query_clauses)
                query_terms.add(clause.toString().split(":")[1]);
        }
        else{
            query_terms.add(t_query.getTerm().toString());
        }

        for(Doc doc : docs){
            int sum = 0;
            for(String term : query_terms)
                //sum += calcTfIdf(new BytesRef(term), doc);
            doc.setScore(sum);
        }
    }

    /**
     * Wrapper method for normal call of scoreDocs_cosine with standard argument of evaluation boolean set to false
     */
    public void scoreDocs_cosine(){
        scoreDocs_cosine(false);
    }

    /**
     * Scores all documents by calculating the angle between the query and each document.
     * Also resets all documents before start of calculation, so the previous search has no influence.
     * This method also takes automatically the percentage of the restriction slider into account when
     * calculating.
     * @param evaluation the bool for evaluation mode or not, speeds up the evaluation,
     *                   but can also lead to OutOfMemoryError when too many documents loaded.
     */
    public void scoreDocs_cosine(boolean evaluation){
        resetDocs();
        ArrayList<BytesRef> query_terms = new ArrayList<>();
        if(query != null) {
            List<BooleanClause> query_clauses = query.clauses();
            for (BooleanClause clause : query_clauses) {
                BytesRef ref = new BytesRef(clause.toString().split(":")[1]);
                //System.out.println("bytesref " + ref + " string " + ref.utf8ToString());
                if(ref != null)
                    query_terms.add(ref);
            }
        }
        else{
            //System.out.println("Tquery" + t_query.toString());
            BytesRef ref = new BytesRef(t_query.getTerm().text());
            if(ref != null)
                query_terms.add(ref);


        }
        Doc query = null;
        if(query_doc != null)
            query = query_doc;
        else
            query = new Doc(-1, query_terms);

        query_doc = query;

        HashSet<Integer> posting_docs = new HashSet<>();

        for(BytesRef term : query_terms) {
            try {
                PostingsEnum doc_enum = MultiFields.getTermDocsEnum(reader, content, term, 8);
                if(doc_enum != null) {
                    while (doc_enum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                        posting_docs.add(doc_enum.docID());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            int number_docs_to_restrict = posting_docs.size() * amount_restricted / 100;
            int number_restricted = 0;
            int index = amount_restricted/10;
            Random rand_gen = new Random(seeds[index]);
            while(number_restricted < number_docs_to_restrict){
                int random_num = rand_gen.nextInt(number_docs);
                if(posting_docs.contains(random_num)){
                    documents.get(doc_name.get(random_num)).setRestricted(true);
                    number_restricted++;
                }

            }
            for (Integer i : posting_docs) {
                boolean to_restrict = false;
                /*double random_number = Math.random();
                if(random_number < ((amount_restricted+5)/100.0) && number_restricted < number_docs_to_restrict) {
                    to_restrict = true;
                    number_restricted++;
                }*/
                //Document document = reader.document(i);
                Doc doc = documents.get(doc_name.get(i));
                double angle;
                if(evaluation)
                    angle = cosine_similarity(query, doc, evaluation);
                else
                    angle = cosine_similarity(query, doc);
                doc.setScore(angle);

                String[] category_names;
                if(CATEGORIES_ENABLED) {
                    Document document = reader.document(i);
                    category_names = document.get("parent_categories").split(";");
                    for (String category_name : category_names) {
                        CategoryNode node = categories.get(category_name);
                        doc.addCategory(node);
                    }
                }


                if(CATEGORIES_ENABLED) {
                    for (CategoryNode node : doc.getCategories()) {
                        if (node.isRestricted()) {
                            to_restrict = true;
                            break;
                        }
                    }
                }
                if(to_restrict) {
                    doc.setRestricted(true);
                }
                docs.add(doc);
            }
            /*System.out.println("Number of Docs restricted: " + number_restricted
                    + " Number docs should be restricted: " + number_docs_to_restrict
                    + " Number docs in result: " + posting_docs.size());*/
            ArrayList<Doc> docs_without_restricted = new ArrayList<>();

            for(int i = 0; i < docs.size(); i++){
                Doc doc = docs.get(i);
                if(!doc.isRestricted())
                    docs_without_restricted.add(doc);
            }
            Collections.sort(docs, new DocCompare());
            Collections.sort(docs_without_restricted, new DocCompare());
            result_set = docs_without_restricted;
            for(int i = 0; i < docs_without_restricted.size(); i++)
                docs_without_restricted.get(i).setInitRank(i);
        }catch (IOException e){

        }
    }

    /**
     * Returns the documents frequency depending to a term.
     * @param token the token for which the document frequency should be retrieved.
     * @return returns the integer document frequency value for the given term
     */
    public int getDocFreq(BytesRef token){
        Integer df;

        df = document_frequencies.get(token);//reader.docFreq(term);
        if(df == null)
            df = 0;

        if(Double.isNaN(df))
            System.err.println("Document Frequency is NaN!");
        return df;
    }

    /**
     * @param token the BytesRef token for which the inverse document frequency should be retrieved
     * @return returns the inverse document frequency for a token
     */
    public double inverseDocFreq(BytesRef token){
        Double idf = inverse_doc_frequencies.get(token);
        if(idf == null)
            idf = 0.0;

        return idf;
    }


    /**
     * @return returns the ArrayList with all documents.
     */
    public ArrayList<Doc> getDocs() {
        return docs;
    }

    /**
     * Calculates the tfidf value depending on the term and the document.
     * @param token the token for which the tfidf weight should be calculated
     * @param doc the document to which the token belongs
     * @param docs the postingsenum containing the posting list for the token from the index
     * @return returns the tfidf value which belongs to the term, document pair.
     */
    public double calcTfIdf(BytesRef token, Doc doc, PostingsEnum docs){
        //PostingsEnum docs = null;
        long tf = 0;
        if(doc.getDocId() == -1) {
            tf = frequency(doc.getTokens(), token);
        }
        else {
            try {
                if(docs != null) {
                    while (docs.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                        if (docs.docID() == doc.getDocId()) {
                            tf = docs.freq();
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        double tfidf = tf * inverseDocFreq(token);

        return tfidf;
    }

    /**
     * Overloaded method for setting the query if the query is a BooleanQuery
     * @param query contains the BooleanQuery consisting of several terms
     */
    public void setQuery(BooleanQuery query) {
        this.query = query;
    }

    /**
     * Overloaded method for setting the query if the query is a TermQuery.
     * @param query contains the TermQuery consisting of a single term.
     */
    public void setQuery(TermQuery query){
        this.query = null;
        t_query = query;
    }

    /**
     * Wrapper function for calling the cosine similarity with standard of evaluation bool on false
     * @param doc1 The first of the two Docs where the similarity should be calculated
     * @param doc2 The second Doc
     * @return returns the angle between the two documents
     */
    public double cosine_similarity(Doc doc1, Doc doc2){
        return cosine_similarity(doc1, doc2, false);
    }

    /**
     * Method for calculating the cosine similarity between two documents.
     * Therefore the tfidf vectors are retrieved for the documents and
     * then the angle between the two vectors are calculated.
     * @param doc1 The first of the two Docs where the similarity should be calculated
     * @param doc2 The second Doc
     * @param evaluation evaluation boolean, when set to true, speed up calculation is done
     * @return returns the angle between the two documents vectors
     */
    public double cosine_similarity(Doc doc1, Doc doc2, boolean evaluation){
        TermsEnum te = null;
        HashMap<String, Double> tf_vec = new HashMap<>();
        HashMap<String, Double> tf_vec_2 = new HashMap<>();

        double sum_doc1 = 0;
        if(doc1.getDocId() == -1)
        {
            if(doc1.getTfidfVector() == null) {
                ArrayList<BytesRef> tokens = doc1.getTokens();
                for(int i = 0; i < tokens.size(); i++) {
                    BytesRef ref = tokens.get(i);
                    int freq = Collections.frequency(doc1.getTokens(), ref);
                    double idf = inverseDocFreq(ref);
                    tf_vec.put(ref.utf8ToString(), freq * idf);
                    sum_doc1 += freq * idf;
                }
            }
            else {
                tf_vec = new HashMap<>(doc1.getTfidfVector());
                sum_doc1 = doc1.getVectorLength();
            }
        }
        else{
            System.err.println("Should never come here");
            assert null != null;
            /*try {
                te = reader.getTermVector(doc1.docId, content).iterator();
                BytesRef term;
                PostingsEnum pe = null;
                while((term = te.next()) != null){
                    try {
                        pe = te.postings(pe, PostingsEnum.FREQS);
                        pe.nextDoc();
                        double idf = inverseDocFreq(term);
                        tf_vec.put(terms_string_map.get(term), (pe.freq() * idf));
                        sum_doc1 += pe.freq() * idf;
                    }catch (Exception e){
                        System.err.println(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
        double sum_doc2 = 0;
        if(doc2.getTfidfVector() != null){
            tf_vec_2 = doc2.getTfidfVector();
            sum_doc2 = doc2.getVectorLength();
        }else {
            try {
                te = reader.getTermVector(doc2.docId, content).iterator();
                BytesRef term;
                PostingsEnum pe = null;

                while ((term = te.next()) != null) {
                    try {
                        pe = te.postings(pe, PostingsEnum.FREQS);
                        pe.nextDoc();
                        double idf = inverseDocFreq(term);
                        tf_vec_2.put(terms_string_map.get(term), (pe.freq() * idf));
                        sum_doc2 += pe.freq() * idf;
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(evaluation)
                doc2.setTfidfVector(new HashMap<>(tf_vec_2));
        }
        HashSet<String> key_list = new HashSet<>(tf_vec.keySet());
        key_list.retainAll(tf_vec_2.keySet());
        double scalar = 0;

        for(String key : key_list){
            double tfidf_1 = tf_vec.get(key);
            double tfidf_2 = tf_vec_2.get(key);
            tfidf_1 /= sum_doc1;
            tfidf_2 /= sum_doc2;
            scalar += tfidf_1 * tfidf_2;
        }

        double retval = 0;

        retval = Math.toDegrees(Math.acos(scalar));

        //System.out.println("Scalar " + scalar + " angle " + Math.acos(scalar) + " Sum doc1 " + sum_doc1 ) ;
        return retval;

    }



    /**
     * The standard method call without setting the weightings for the relevant documents and the query.
     * @param number_top_ranked the number of top ranked documents which should be used for prf
     * @param query the query document from the initial query
     */
    public void PseudoRelevanceFeedbackRocchio(int number_top_ranked, Doc query){
        PseudoRelevanceFeedbackRocchio(number_top_ranked, query, 1.0, 1.0, 0);
    }

    /**
     * Calculated the new query by using rocchios pseudo relevance feedback, with the set weights for the
     * query, the relevant documents and the irrelevant documents.
     * @param number_top_ranked the number of top ranked documents which should be used for prf
     * @param query the query document from the initial query
     * @param alpha the weight for the initial query
     * @param beta the weight for the relevant documents
     * @param gamma the weight for the irrelevant documents
     */
    public void PseudoRelevanceFeedbackRocchio(int number_top_ranked, Doc query, double alpha, double beta, double gamma){

        Doc[] top_scored_docs = new Doc[number_top_ranked];
        if(number_top_ranked > docs.size())
            return;

        for(int i = 0; i < number_top_ranked; i++)
            top_scored_docs[i] = docs.get(i);

        HashMap<String, Double> tfidf_query = new HashMap<>();

        for (BytesRef term : query.getTokens()) {
            int freq = Collections.frequency(query.getTokens(), term);
            double idf = inverseDocFreq(term);
            tfidf_query.put(terms_string_map.get(term), freq * idf * alpha);
        }

        TermsEnum te = null;
        ArrayList<HashMap<String, Double>> tfidf_doc_vecs = new ArrayList<>();
        ArrayList<Double> vec_lengths = new ArrayList<>();

        for(Doc doc : top_scored_docs){
            try {
                if(reader == null)
                    System.out.println("Reader null");
                else if(doc == null)
                    System.out.println("Doc null");
                te = reader.getTermVector(doc.docId, content).iterator();
                BytesRef term;
                PostingsEnum pe = null;
                HashMap<String, Double> doc_vec = new HashMap<>();
                while((term = te.next()) != null){
                    try {
                        pe = te.postings(pe, PostingsEnum.FREQS);
                        pe.nextDoc();
                        double idf = inverseDocFreq(term);
                        doc_vec.put(terms_string_map.get(term), (pe.freq() * idf));
                        vec_lengths.add(pe.freq() * idf);
                    }catch (Exception e){
                    }
                }
                tfidf_doc_vecs.add(doc_vec);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        HashSet<String> term_base = new HashSet<>();

        for(HashMap doc_map : tfidf_doc_vecs)
            term_base.addAll(doc_map.keySet());

        for(String term : term_base){
            double sum = 0;
            int i = 0;
            for(HashMap<String, Double> doc_map : tfidf_doc_vecs) {
                if(doc_map == null)
                    System.out.println("DocMap null");
                else if(vec_lengths == null)
                    System.out.println("Veclengths null");
                Double val = doc_map.get(term);
                if(val == null)
                    val = 0.0;
                sum +=  val / vec_lengths.get(i);
                i++;
            }
            Double old_val;
            if((old_val = tfidf_query.putIfAbsent(term, sum)) != null) {
                old_val += sum * beta / number_top_ranked;
                tfidf_query.put(term, old_val);
            }

        }
        //System.out.println(tfidf_query);
        query.setTfidfVector(tfidf_query);

        for(int i = 0; i < docs.size(); i++) {
            Doc doc = docs.get(i);
            doc.setScore(cosine_similarity(query, doc));
        }

        ArrayList<Doc> docs_without_restricted = new ArrayList<>();


        for(int i = 0; i < docs.size(); i++){
            Doc doc = docs.get(i);
            if(!doc.isRestricted())
                docs_without_restricted.add(doc);
        }
        Collections.sort(docs_without_restricted, new DocCompare());
        result_set = docs_without_restricted;

        for(int i = 0; i < docs_without_restricted.size(); i++) {
            docs_without_restricted.get(i).setBoostedRank(i);
        }
    }

    /**
     * @return returns the query document which has been set
     */
    public Doc getQueryDoc() {
        return query_doc;
    }

    /**
     * @return returns the CategoryTree
     */
    public CategoryTree getCategoryTree(){
        return category_tree;
    }

    public void addProgBar(JProgressBar prog_bar){
        this.prog_bar = prog_bar;
    }

    /**
     * Builds up the CategoryTree by running through all documents parents,
     * which have been extracted while indexing and by using the link graph file
     * which has been extracted from the database, when Categories are enabled.
     * @throws IOException exception is thrown, when reading from the link_graph file fails
     */
    public void buildCategoryTree() throws IOException {
        int modulo = 10;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(Paths.get("link_graph.txt").toFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        }

        String category_graph = new String();
        int character;
        int k = 0;
        byte[] bytes = new byte[32768];
        while((character = fs.read(bytes)) != -1){
            category_graph += new String(bytes, 0, character);
        }
        String[] links;
        links = category_graph.split(";");
        for(int i = 0; i < reader.maxDoc(); i++){
            Document doc = reader.document(i);
            String category = doc.get("categorie");
            String parent_categories = doc.get("parent_categories");
            String title = doc.get("title");
            String[] parent_category_names = parent_categories.split(";");
            CategoryNode actual_node;

            if(!category.equals(""))
                actual_node = new CategoryNode(category, new ArrayList<CategoryNode>());
            else
                actual_node = new CategoryNode(title, new ArrayList<>());

            categories.putIfAbsent(category, actual_node);

            for(String cat_name : parent_category_names){
                CategoryNode parent = new CategoryNode(cat_name, new ArrayList<>());
                CategoryNode parent_in_map = categories.putIfAbsent(cat_name, parent);

                setSubCategories(actual_node, parent, parent_in_map);
            }
            if((i % modulo) == 0){
                int val = prog_bar.getValue() + 2;
                prog_bar.setValue(val);
            }
        }

        for(String link : links){
            String[] link_arr = link.split(":");
            if(link_arr.length == 2) {
                String sub_category = link.split(":")[0];
                String cat = link.split(":")[1];
                cat = cat.replace("_", " ");
                sub_category = sub_category.replace("_", " ");
                CategoryNode parent = new CategoryNode(cat, new ArrayList<>());
                CategoryNode parent_in_map = categories.putIfAbsent(cat, parent);
                CategoryNode sub_cat = categories.get(sub_category);
                if (sub_cat == null)
                    sub_cat = new CategoryNode(sub_category, new ArrayList<>());
                setSubCategories(sub_cat, parent, parent_in_map);
            }
            k++;
        }
    }

    private void setSubCategories(CategoryNode actual_node, CategoryNode parent, CategoryNode parent_in_map) {
        if(parent_in_map != null){
            parent_in_map.addSubCategory(actual_node);
            actual_node.addParentCategory(parent_in_map);
        }
        else{
            parent.addSubCategory(actual_node);
            actual_node.addParentCategory(parent);
        }
    }

    /**
     * Sets the amount of restricted documents
     * @param amount_restricted the amount of restricted documents
     */
    public void setAmountRestricted(int amount_restricted) {
        this.amount_restricted = amount_restricted;
    }

    /**
     * @return returns the HashMap containing the document name, document mappings
     */
    public HashMap<String, Doc> getDocuments() {
        return documents;
    }

    /**
     * Resets the documents from the previous search, when evaluation boolean is set,
     * then also tfidf vector is set to null.
     * @param evaluation boolean to determine if the tf-idf vector should be reset
     */
    public void resetDocs(boolean evaluation){
        for(int i = 0; i < docs.size(); i++){
            Doc doc = docs.get(i);
            doc.setInitRank(0);
            doc.setBoostedRank(0);
            doc.setRestricted(false);
            doc.setScore(0);
            if(evaluation)
                doc.setTfidfVector(null);
        }
        docs.clear();
    }

    /**
     * Wrapper function for calling reset docs with standard value false for evaluation.
     */
    public void resetDocs(){
        resetDocs(false);
    }

    /**
     * @return returns the result set of a search
     */
    public ArrayList<Doc> getResultSet() {
        return result_set;
    }

    /**
     * @return returns the ArrayList with all documents names
     */
    public ArrayList<String> getDocName(){
        return doc_name;
    }

    public ArrayList<Topic> getTopicsFromHashMap(){
        ArrayList<Topic> topics = new ArrayList<>(tag_to_topic.values());
        return  topics;
    }

    /**
     * This method implements the BM25 scoring method, which takes with respect to cosine similarity the
     * length of the documents into account.
     * @param query A Doc Object which contains the terms of the query.
     */
    public void BM25Scoring(Doc query){
        ArrayList<BytesRef> query_terms = query.getTokens();
        double k = 1.2;
        double b = 0.75;

        for(Doc doc : docs) {
            TermsEnum te;
            double score = 0;
            try {
                te = reader.getTermVector(doc.docId, content).iterator();
                BytesRef term_of_doc;
                PostingsEnum pe = null;
                int term_count = 0;
                ArrayList<Double> idfs = new ArrayList<>();
                ArrayList<Double> tfs = new ArrayList<>();
                while ((term_of_doc = te.next()) != null) {
                    try {
                        pe = te.postings(pe, PostingsEnum.FREQS);
                        pe.nextDoc();
                        double idf = inverseDocFreq(term_of_doc);
                        double tf = pe.freq();
                        term_count++;
                        if(query_terms.contains(term_of_doc)){
                            idfs.add(idf);
                            tfs.add(tf);
                        }


                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
                for(int i = 0; i < idfs.size(); i++){
                    double idf = idfs.get(i);
                    double tf = tfs.get(i);
                    double avg_doc_len = (double)number_terms/(double)number_docs;
                    score += (idf*tf*(k+1))/(tf+(k*(1-b+b*(term_count/avg_doc_len))));
                    //System.out.println("Idf: " + idf + " Tf: " + tf + " avg_doc_len: " + avg_doc_len + " terms_count " + term_count);
                }
                doc.setScore(score);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Doc> docs_without_restricted = new ArrayList<>();
        for(Doc doc : docs){
            if(!doc.isRestricted())
                docs_without_restricted.add(doc);
        }
        Collections.sort(docs_without_restricted, new DocCompareBM());
        result_set = docs_without_restricted;
        query_doc = null;

    }

    public HashMap<String, String> getCo_occurrences() {
        return co_occurrences;
    }
}
