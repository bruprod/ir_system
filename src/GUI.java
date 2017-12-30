import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

/**
 * Created by Andreas.
 */
public class GUI {

    JFrame frame = new JFrame("Main Window");
    JPanel doc_management = new JPanel();
    JPanel search_interface = new JPanel();
    JPanel categorie_panel = new JPanel();
    JPanel search_results = new JPanel();
    JButton index_docs = new JButton("Index new Documents");
    String index_path = null;
    JButton evaluate = new JButton("Evaluate");
    JTextField search_input = new JTextField("Insert search query here");
    JButton start_search = new JButton("SEARCH");
    JTable categorie_table;
    JTable result_table;
    JComboBox<String> cb;
    JProgressBar prog_bar;
    JScrollPane scroll_pane;
    JScrollPane category_scrollpane;
    JSlider restricted_slider;
    JButton search_doc_collection = new JButton("Choose Document Collection");
    static int PRF_NUMBER_TOP_RANKED = 3;
    static int NUMBER_TOPICS = 150;
    ActionListener evaluate_listener;
    String histogram_prefix = new String();
    boolean suppress_histograms = false;
    Thread evaluation_thread;
    boolean co_occurrence_enabled = false;


    VectorSpaceModel vs;
    int font_size = 14;
    int row_height = 25;
    int width = 800;
    int height = 600;


    /**
     * GUI Constructor is initializing all GUI components.
     * @param vs VectorSpaceModel object
     */
    public GUI(VectorSpaceModel vs) {
        this.vs = vs;
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
        tcr.setHorizontalAlignment(JLabel.CENTER);
        search_input.setFont(new Font("Arial", Font.BOLD, 20));
        search_input.setHorizontalAlignment(JTextField.CENTER);
        TableModel tm_categories = new Table();
        TableModel tm_results = new Table(new Object[][]{{""}});
        CategoryNode root;
        ArrayList<CategoryNode> first_level = new ArrayList<>();
        if(vs.getCategoryTree() != null) {
            root = vs.getCategoryTree().getRootCategory();

            initializeSlider();

            first_level = root.getSubCategories();
        }
        Object[][] category_table_object = new Object[first_level.size()][2];
        if (vs.CATEGORIES_ENABLED) {
            initializeCategories(category_table_object, tm_categories, first_level, tcr);
            frame.add(restricted_slider);
        } else {
            if(vs.getCategoryTree() != null) {
                categorie_panel.add(restricted_slider);
                categorie_panel.setPreferredSize(new Dimension(width, 50));
            }
        }


        result_table = new JTable(tm_results);
        result_table.setFont(new Font("Arial", Font.CENTER_BASELINE, font_size));
        result_table.setDefaultRenderer(String.class, tcr);
        result_table.setRowHeight(row_height);
        result_table.getColumnModel().getColumn(0).setPreferredWidth(width);
        result_table.setPreferredSize(new Dimension(width, row_height));
        result_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        result_table.updateUI();

        search_input.setPreferredSize(new Dimension(width / 2, row_height));
        String[] doc_collections = {"Wikipedia", "StackExchange"};
        cb = new JComboBox<>(doc_collections);

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setLayout(new GridLayout(5, 1));
        frame.setSize(width, height);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.add(doc_management);
        frame.add(search_interface);
        frame.add(categorie_panel);
        frame.add(search_results);
        frame.setLocationRelativeTo(null);

        doc_management.setLayout(new FlowLayout());
        doc_management.add(search_doc_collection);
        doc_management.add(cb);
        doc_management.add(index_docs);
        doc_management.add(evaluate);

        search_interface.setLayout(new FlowLayout());
        search_interface.add(search_input);
        search_interface.add(start_search);


        scroll_pane = new JScrollPane(result_table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        search_results.add(scroll_pane);
        scroll_pane.setVisible(true);
        scroll_pane.setPreferredSize(new Dimension(width - 5, row_height * 8 + 10));
        search_results.setPreferredSize(new Dimension(width - 5, row_height * 8 + 15));

        ActionListener al = getSearchActionListener(category_table_object);
        search_input.addActionListener(al);
        start_search.addActionListener(al);
        evaluate_listener = addEvaluateClickListener();
        addChooseFileListener();
        addResultsMouseListener();
        addIndexClickListener();
    }

    public void setVs(VectorSpaceModel vs) {
        this.vs = vs;
    }

    /**
     * Intitalizes all GUI objects which belong to the categories section.
     * @param category_table_object content table of objects for the categories
     * @param tm_categories the categories table model
     * @param first_level first level of CategoryNodes. These are the chooseable categories.
     * @param tcr TableCellRenderer for formatting of the cell
     */
    public void initializeCategories(Object[][] category_table_object, TableModel tm_categories, ArrayList<CategoryNode> first_level, TableCellRenderer tcr) {
        int i = 0;
        for (CategoryNode node : first_level) {
            category_table_object[i][0] = node.categorie_name;
            category_table_object[i][1] = Boolean.FALSE;
            i++;
        }
        tm_categories = new Table(category_table_object);

        categorie_table = new JTable(tm_categories);
        categorie_table.setPreferredSize(new Dimension(width, row_height * first_level.size() + 20));
        category_scrollpane = new JScrollPane(categorie_table);
        category_scrollpane.setPreferredSize(new Dimension(width, row_height * first_level.size()));
        categorie_table.setFont(new Font("Arial", Font.CENTER_BASELINE, font_size));
        categorie_table.setRowHeight(row_height);
        categorie_table.setDefaultRenderer(String.class, tcr);
        categorie_panel.add(category_scrollpane);
    }

    /**
     * Intitializes all necessary Objects for the restriction slider
     */
    public void initializeSlider() {
        restricted_slider = new JSlider();
        Hashtable labels = new Hashtable();
        for (int i = 0; i <= 100; i += 10) {
            labels.put(i, new JLabel(i + "%"));
        }
        restricted_slider.setMajorTickSpacing(10);
        TitledBorder slider_border = new TitledBorder("Percentage restricted Documents");
        slider_border.setTitlePosition(TitledBorder.CENTER);
        restricted_slider.setBorder(slider_border);
        restricted_slider.setLabelTable(labels);
        restricted_slider.setPaintLabels(true);
        restricted_slider.setPaintTicks(true);
        restricted_slider.setPreferredSize(new Dimension(width, 75));
    }

    /**
     * Calls all necessary methods from the VectorSpaceModel class and writes the retrieved ranking
     * to the results table in the GUI. Each search is started in an own Thread so the GUI is not blocked
     * while searching.
     * @param category_table_object The Table object containing the root categories if categries are enabled
     * @return returns the ActionListener which is invoked when Search started
     */
    public ActionListener getSearchActionListener(Object[][] category_table_object) {
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        ArrayList<CategoryNode> restricted = new ArrayList<>();
                        for (int i = 0; i < category_table_object.length; i++) {
                            String category_name = (String) category_table_object[i][0];
                            Boolean set = (Boolean) category_table_object[i][1];
                            if (set) {
                                restricted.addAll(vs.getCategoryTree().getCategory(category_name).getAllChildsAndSetRestricted());
                            }
                        }
                        vs.restricted_categories = restricted;
                        System.out.println("Restricted " + restricted.size());
                        vs.setAmountRestricted(restricted_slider.getValue());

                        String query_string = search_input.getText();
                        QueryParser q_parser = new QueryParser("content", new StandardAnalyzer());
                        BooleanQuery query = null;
                        TermQuery t_query = null;
                        try {
                            query = (BooleanQuery) q_parser.parse(query_string);
                        } catch (ParseException e1) {
                        } catch (ClassCastException e2) {

                        } finally {
                            System.out.println("query " + query + " tquery " + t_query);
                            if (query != null)
                                vs.setQuery(query);
                            else {
                                try {
                                    t_query = (TermQuery) q_parser.parse(query_string);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                                vs.setQuery(t_query);
                            }
                            long start = System.currentTimeMillis();
                            vs.scoreDocs_cosine();
                            long end = System.currentTimeMillis();
                            System.out.println("scoring done took " + (end - start) / 1000 + "." + (end - start) % 1000 + " seconds");

                            start = System.currentTimeMillis();
                            vs.PseudoRelevanceFeedbackRocchio(PRF_NUMBER_TOP_RANKED, vs.getQueryDoc());
                            //vs.BM25Scoring(vs.getQueryDoc());
                            vs.query_doc = null;
                            end = System.currentTimeMillis();
                            System.out.println("PRF done took " + (end - start) / 1000 + "." + (end - start) % 1000 + " seconds");
                            ArrayList<Doc> docs = vs.getResultSet();
                            HashMap<String, Doc> document_map = vs.getDocuments();
                            ArrayList<Doc> results_without_restricted = new ArrayList<>();
                            int number_restricted_docs = 0;
                            for (Doc doc : docs) {
                                if (doc.isRestricted()) {
                                    number_restricted_docs++;
                                } else {
                                    results_without_restricted.add(doc);
                                }
                            }

                            Topic topic;
                            /*if (t_query != null) {
                                topic = new Topic(t_query.getTerm().text());
                                topic.retrieveTopicRelevantDocs(vs.getDocuments());
                                topic.calcAverageRanks();
                                topic.calcPrecisionAndRecall(results_without_restricted);
                                System.out.println("Topic: " + topic.getTopicName() + " AVG improvement: "
                                        + topic.getAvgImprovement());
                                System.out.println("Topic: " + topic.getTopicName()
                                        + " Precision: " + topic.getPrecision() + " Recall: " + topic.getRecall());

                                topic.evaluateTopic(vs, PRF_NUMBER_TOP_RANKED);

                            }*/
                            int table_size = docs.size() - number_restricted_docs;
                            Object[][] table_data = new Object[table_size][2];
                            int i = 0;

                            System.out.println("sorting done");
                            for (Doc doc : docs) {
                                String result_string = null;
                                /*if(doc.isRestricted())
                                    result_string = "(RESTRICTED)Document " + doc.getDocId() + " " + doc.getTitle()
                                            + " with score " + doc.getScore() ;
                                else
                                    result_string  = "Document " + doc.getDocId() + " " + doc.getTitle() + " with score " + doc.getScore();

                                table_data[i][0] = result_string;
                                table_data[i][1] = doc;
                                i++;*/
                                if (!doc.isRestricted()) {
                                    result_string = "Document " + doc.getDocId() + " " + doc.getTitle() + " with score " + doc.getScore();
                                    table_data[i][0] = result_string;
                                    table_data[i][1] = doc;
                                    i++;
                                }
                            }
                            System.out.println("writing done");

                            if (table_data.length == 0) {
                                table_data = new Object[1][2];
                                table_data[0][0] = "No matching results";
                                table_data[0][1] = -1;
                            }

                            result_table.setModel(new Table(table_data));
                            result_table.setPreferredSize(new Dimension(width - 5, row_height * table_data.length + 40));
                            scroll_pane.setPreferredSize(new Dimension(width - 5, row_height * 8 + 10));
                            search_results.setPreferredSize(new Dimension(width - 5, row_height * 8 + 15));
                            result_table.getColumnModel().getColumn(0).setPreferredWidth(width);
                            result_table.getColumnModel().getColumn(1).setPreferredWidth(0);
                        }
                        for (CategoryNode node : restricted)
                            node.setRestricted(false);

                    }
                };
                t.start();
            }
        };
        return al;
    }

    /**
     * Sets the ActionListener for a double click on a cell of the results table in the GUI.
     * After the double click a Message Box is shown to the user which contains the documents
     * content.
     */
    public void addResultsMouseListener() {
        result_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = result_table.getSelectedRow();
                    Doc doc = (Doc) result_table.getModel().getValueAt(row, 1);
                    int doc_ID = doc.getDocId();
                    System.out.println("Original ranking: " + doc.getInitRank() + " Boosted ranking: " + doc.getBoosted_rank());
                    System.out.println("Inlinks: " + vs.documents.get(doc.getTitle().toLowerCase()).getInlinks());
                    try {
                        String content = vs.reader.document(doc_ID).get("content");

                        JTextPane text_pane = new JTextPane();
                        JScrollPane scroll_pane = new JScrollPane(text_pane);
                        try {
                            text_pane.getDocument().insertString(0, content, new SimpleAttributeSet());
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }

                        scroll_pane.setPreferredSize(new Dimension(600, 400));
                        text_pane.setSize(600, 400);
                        text_pane.setPreferredSize(new Dimension(600, 400));
                        JOptionPane.showMessageDialog(null, scroll_pane);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Adds the ActionListener for the evaluation button. After clicking all necessary methods for
     * the evaluation are called. The evaluation also runs in a separate Thread to be independent
     * to the GUI Thread.
     * @return returns the ActionListener for the evaluate button, which is used later to invoke it manually,
     *         when the corresponding flag is set in the commandline
     */
    public ActionListener addEvaluateClickListener() {
        //ArrayList<String> topics = new ArrayList<String>(Arrays.asList("Graz", "John Lennon", "Arnold Schwarzenegger", "Maria Shriver", "Green Day", "Quantum Mechanics", "Vienna", "Salzburg", "Python"));
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Start evaluation!");
                System.out.println("Number topics: "+vs.getTopicsFromHashMap().size());
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        ArrayList<String> topics = new ArrayList<>();
                        HashSet<Integer> indices_used = new HashSet<>();
                        Boolean stackexchange = false;
                        ArrayList<Topic> topic_arr_list = new ArrayList<>();
                        ArrayList<Topic> topics_from_Hashmap = vs.getTopicsFromHashMap();
                        Random rand = new SecureRandom();
                        System.out.println("Hashmapsize: " + vs.getTopicsFromHashMap().size());
                        for (int i = 0; i < NUMBER_TOPICS; i++) {
                            if(vs.getTopicsFromHashMap().size() == 0) {
                                int index = rand.nextInt(vs.getDocName().size());
                                while (true) {
                                    if (!indices_used.contains(index) && !vs.getDocName().get(index).contains("wikipedia:")) {
                                        topics.add(vs.getDocName().get(index));
                                        indices_used.add(index);
                                        break;
                                    }
                                    index = rand.nextInt(vs.getDocName().size());
                                }
                            }
                            else{
                                stackexchange = true;
                                int index = rand.nextInt(topics_from_Hashmap.size());
                                while (true) {
                                    if (!indices_used.contains(index)) {
                                        topic_arr_list.add(topics_from_Hashmap.get(index));
                                        topics.add(topics_from_Hashmap.get(index).getTopicName());
                                        indices_used.add(index);
                                        break;
                                    }
                                    index = rand.nextInt(vs.getTopicsFromHashMap().size());
                                }
                            }
                        }

                        System.out.println("Number Topics: " + topics.size());
                        ArrayList<Topic> topic_list = new ArrayList<>();
                        JProgressBar prog_bar = new JProgressBar();
                        JFrame progbar_frame = new JFrame("Evaluation");
                        progbar_frame.add(prog_bar);
                        prog_bar.setBorder(BorderFactory.createTitledBorder("Evaluation progress"));
                        int width_prog = 500;
                        int height_prog = 100;
                        progbar_frame.setSize(width_prog, height_prog);
                        progbar_frame.setLocationRelativeTo(null);
                        progbar_frame.setVisible(true);
                        prog_bar.setValue(0);
                        prog_bar.setStringPainted(true);

                        double percentage_per_topic = 100 / (double) topics.size();
                        System.out.println("Percentage topic " + percentage_per_topic);
                        long begin = System.currentTimeMillis();
                        int j = 0;
                        int topic_count = 0;
                        for (String topic_name : topics) {
                            System.out.println("Topic: " + topic_name);
                            BooleanQuery query = null;
                            TermQuery t_query = null;
                            QueryParser q_parser = new QueryParser("content", new StandardAnalyzer());
                            String query_string = topic_name;
                            if(stackexchange && co_occurrence_enabled)
                                query_string += " " + vs.getCo_occurrences().get(topic_name);
                            try {
                                query = (BooleanQuery) q_parser.parse(query_string);
                            } catch (ParseException e1) {
                                System.out.println("Parsing Failed");
                            } catch (ClassCastException e2) {
                            } finally {
                                System.out.println("query " + query + " tquery " + t_query);
                                if (query != null)
                                    vs.setQuery(query);
                                else {
                                    try {
                                        t_query = (TermQuery) q_parser.parse(query_string);
                                    } catch (ParseException e1) {
                                    }
                                    vs.setQuery(t_query);
                                }
                                if(!((query == null) && (t_query == null))) {
                                    Topic topic = new Topic(topic_name.toLowerCase());
                                    if(stackexchange)
                                        topic = topic_arr_list.get(topic_count++);
                                    topic.setProgBar(prog_bar, percentage_per_topic);
                                    topic.retrieveTopicRelevantDocs(vs.getDocuments());
                                    if(topic.getTopicRelevantDocs().size() > 0) {
                                        topic.evaluateTopic(vs, PRF_NUMBER_TOP_RANKED);
                                        topic_list.add(topic);
                                    }
                                }
                                prog_bar.setValue((int)(j*percentage_per_topic));
                                j++;
                            }
                        }
                        long end = System.currentTimeMillis();
                        System.out.println("Evaluation took: " + (((end - begin) / 1000.0) + ((end - begin) % 1000) / 1000.0) + "s");
                        JFrame frame = new JFrame();
                        int size = 10;
                        double[] precisions = new double[size];
                        double[] precisions_cosine = new double[size];
                        double[] precisions_bm25 = new double[size];
                        double[] x_axis = new double[size];
                        double[] p_at_ten_cos = new double[size];
                        double[] p_at_ten_prf = new double[size];
                        double[] p_at_ten_bm25 = new double[size];
                        double[] number_leaked_terms = new double[size];
                        double[] number_leaked_terms_bm25 = new double[size];
                        double[] percentage_leaked_terms = new double[size];

                        DefaultCategoryDataset ds = new DefaultCategoryDataset();
                        DefaultCategoryDataset p_at_ten_ds = new DefaultCategoryDataset();
                        double[] x_axis_p_at_ten = {10, 20, 30, 40, 50};
                        for (Topic topic : topic_list) {
                            for (int i = 0; i < topic.getAvgRecall().size(); i++) {
                                precisions[i] += topic.getAvgPrecisionsMapPRF().get(i) / topic_list.size();
                                precisions_cosine[i] += topic.getAvgPrecisionsMapCosine().get(i) / topic_list.size();
                                precisions_bm25[i] += topic.getAvg_precisions_map_bm25().get(i) / topic_list.size();
                                p_at_ten_cos[i] += topic.getPrecisionAtTenCos().get(i) / (double)topic_list.size();
                                p_at_ten_prf[i] += topic.getGetPrecisionAtTenPrf().get(i) / (double)topic_list.size();
                                p_at_ten_bm25[i] += topic.getGetPrecision_at_ten_bm25().get(i)  / (double)topic_list.size();
                                x_axis[i] = i * 10;
                                number_leaked_terms[i] += topic.getAvgNumberLeakedTerms().get(i) / topic_list.size();
                                number_leaked_terms_bm25[i] += topic.getAvg_number_leaked_terms_bm25().get(i) / topic_list.size();
                                percentage_leaked_terms[i] += topic.getPercentageLeakedTerms().get(i) *100 / topic_list.size();
                            }
                        }
                        System.out.println("Precision at ten cos: " + p_at_ten_cos);
                        System.out.println("Precision at ten prf: " + p_at_ten_prf);
                        final String map_str = "Mean average Precision PRF";
                        final String bm25_str = "Mean average Precision BM25";
                        final String cos_str = "Mean average Precision Cosine Similarity";
                        final String p_at_ten_str_cos = "Average Precision at Ten Cosine Similarity";
                        final String p_at_ten_str_prf = "Average Precision at Ten Cosine PRF";
                        final String p_at_ten_str_bm25 = "Average Precision at Ten Cosine BM25";
                        final String avg_percentage_leaked_terms = "Average percentage of leaked terms";
                        final String percentage_leaked_terms_prf = "Percentage of leaked terms PRF";
                        final String restricted_in_percent = "Restricted Documents in %";
                        final String map_in_percent = "Mean Average Precision in %";
                        for (int i = 0; i < precisions.length; i++) {
                            ds.addValue(precisions_cosine[i] * 100, cos_str, x_axis[i] + "%");
                            ds.addValue(precisions[i] * 100, map_str, x_axis[i] + "%");

                        }

                        createHistogramm(number_leaked_terms, null, x_axis, avg_percentage_leaked_terms, "", 1,
                                restricted_in_percent, "Average Number leaked terms", "Number leaked Terms");
                        createHistogramm(percentage_leaked_terms, null, x_axis, percentage_leaked_terms_prf, "", 1,
                                restricted_in_percent, "Percentage leaked Terms PRF", percentage_leaked_terms_prf);
                        createHistogramm(p_at_ten_cos, p_at_ten_prf, x_axis, p_at_ten_str_cos,
                                p_at_ten_str_prf, 1, restricted_in_percent, "Number Relevant Documents",
                                "Average Precision at result set size 10");
                        createHistogramm(p_at_ten_cos, p_at_ten_bm25, x_axis, p_at_ten_str_cos,
                                p_at_ten_str_bm25, 1, restricted_in_percent, "Number Relevant Documents",
                                "Average Precision at result set size 10 BM25");
                        createHistogramm(precisions_cosine, precisions_bm25, x_axis, "MAP Cosine Similarity",
                                bm25_str, 100, restricted_in_percent, map_in_percent,
                                "Cosine Similarity vs BM25");
                        JFreeChart chart = ChartFactory.createBarChart("Mean Average Precision Cosine similarity vs PRF"
                                , restricted_in_percent, map_in_percent,
                                ds, PlotOrientation.VERTICAL, true, true, false);
                        showHistograms(frame, chart);
                        String chart_name = "cos_vs_prf";
                        saveAsChartAsPng(chart, chart_name);

                        progbar_frame.dispose();
                    }
                };
                t.start();
                evaluation_thread = t;
            }
        };
        evaluate.addActionListener(al);
        return al;
    }

    private void showHistograms(JFrame frame, JFreeChart chart) {
        if(!suppress_histograms) {
            ChartPanel pan = new ChartPanel(chart);
            frame.setContentPane(pan);
            frame.setSize(new Dimension(width, height));
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        }
    }

    public void createHistogramm(double[] first_arr, double[] second_arr, double[] x_axis,
                                 String first_name, String second_name, double scale,
                                 String x_axis_name, String y_axis_name, String histogram_name){
        JFrame frame = new JFrame();
        if((second_arr != null) && first_arr.length != second_arr.length) {
            System.err.println ("Arrays must be the same size");
            return;
        }
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (int i = 0; i < first_arr.length; i++) {
            ds.addValue(first_arr[i] * scale, first_name, x_axis[i] + "");
            if(second_arr != null)
                ds.addValue(second_arr[i] * scale, second_name, x_axis[i] + "");
        }
        JFreeChart chart = ChartFactory.createBarChart(histogram_name
                , x_axis_name, y_axis_name,
                ds, PlotOrientation.VERTICAL, true, true, false);
        saveAsChartAsPng(chart, histogram_name);
        showHistograms(frame, chart);
    }

    public void saveAsChartAsPng(JFreeChart chart, String chart_title){
        try {
            File evaluation_dir = new File(Paths.get("").toAbsolutePath().toString()+ "\\evaluation" );
            if(!evaluation_dir.exists())
                evaluation_dir.mkdir();
            if(histogram_prefix.length() > 0){
                File prefix_dir = new File(evaluation_dir.getAbsolutePath()+"\\"
                        +histogram_prefix.subSequence(0, histogram_prefix.length()-2));
                prefix_dir.mkdir();
                evaluation_dir = prefix_dir;
            }

            String path = evaluation_dir.getAbsolutePath() + "\\" + histogram_prefix +  chart_title +".png";
            Files.deleteIfExists(Paths.get(path));
            OutputStream os = new FileOutputStream(path);
            ChartUtilities.writeChartAsPNG(os, chart, 1920, 1080);
            os.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void addChooseFileListener(){
        search_doc_collection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser file_chooser = new JFileChooser();
                int retval = file_chooser.showOpenDialog(null);
                if(retval == JFileChooser.APPROVE_OPTION){
                    index_path = file_chooser.getSelectedFile().getAbsolutePath();
                    System.out.println("Path: " + index_path);
                }
            }
        });
    }

    public void addIndexClickListener(){
        index_docs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(index_path == null || !index_path.contains(".xml")){
                    JOptionPane.showMessageDialog(null, "Not in xml Format or no File choosen.", "File Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    Path path = new File(Paths.get("").toAbsolutePath().toString() + "\\index").toPath();
                    FSDirectory dir = FSDirectory.open((path));
                    IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
                    conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                    conf.setRAMBufferSizeMB(1024);
                    IndexWriter writer = new IndexWriter(dir, conf);
                    DefaultHandler handler;
                    if(((String)cb.getSelectedItem()).equals("Wikipedia"))
                        handler = new WikipediaHandler(writer);
                    else
                        handler = new StackExchangeHandler(writer);

                    Indexer indexer = new Indexer(index_path,handler, writer);
                    QueryParser q_parser = new QueryParser("content", new StandardAnalyzer());
                    BooleanQuery query = null;
                    TermQuery t_query = null;
                    try {
                        query = (BooleanQuery) q_parser.parse("how do you");
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    } catch (ClassCastException e2) {
                        e2.printStackTrace();
                    }finally {
                        vs = new VectorSpaceModel(query, indexer.getReader(), new JProgressBar());
                    }


                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public ActionListener getEvaluateListener() {
        return evaluate_listener;
    }

    public void setHistogramPrefix(String prefix){
        histogram_prefix = prefix;
        if(histogram_prefix.length() > 0)
            histogram_prefix += "_";
    }

    public void setSuppressHistograms(boolean suppress){
        suppress_histograms = suppress;
    }
    public Thread getEvaluationThread(){
        return evaluation_thread;
    }
}
