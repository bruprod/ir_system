import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws IOException {
        CommandLineHandler cmd = new CommandLineHandler(args);
        boolean evaluate = cmd.isEvaluate();
        boolean index = cmd.isIndex();
        boolean suppress_histograms = cmd.isSuppressHistograms();
        boolean stop_after_evaluation = cmd.isStopAfterEvaluation();
        int number_evals = cmd.getNumberEvals();
        String handler_string = cmd.getHandlerString();
        String path_string = cmd.getPathString();
        String histogram_prefix = cmd.getHistogramPrefix();

        JFrame frame = new JFrame("Initialization");
        JProgressBar prog_bar = new JProgressBar();
        prog_bar.setBorder(BorderFactory.createTitledBorder("Loading Components"));
        frame.add(prog_bar);
        frame.setVisible(true);
        int width = 500;
        int height = 100;
        frame.setSize(width,height);
        frame.setLocationRelativeTo(null);


        Path curr_path = Paths.get("");

        File index_dir = new File(Paths.get("").toAbsolutePath()+"\\index");
        if(!index_dir.exists())
            index_dir.mkdir();

        File index_source = new File(Paths.get("").toAbsolutePath()+"\\index_source");
        if(!index_source.exists()) {
            index_source.mkdir();
            try {
                InputStream index_source_stream = Main.class.getResourceAsStream("ressources/Posts.xml");
                File index_source_xml = new File(index_source.getPath()+"\\Posts.xml");
                index_source_xml.createNewFile();
                FileOutputStream os = new FileOutputStream(index_source_xml);
                int content;
                while((content = index_source_stream.read()) != -1){
                    os.write(content);
                    os.flush();
                }
                os.close();
                index_source_stream.close();
                //FileUtils.copyFileToDirectory(index_source_xml, index_source);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Indexer indexer = null;

        try {
            long start = System.currentTimeMillis();
            if(!index)
                indexer = new Indexer(curr_path+"\\index");
            else {
                DefaultHandler xml_handler;
                Path path = new File(Paths.get("").toAbsolutePath().toString() + "\\index").toPath();
                FSDirectory dir = FSDirectory.open((path));
                IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                conf.setRAMBufferSizeMB(1024);
                IndexWriter writer = new IndexWriter(dir, conf);
                if(handler_string.equals("Stackexchange"))
                    xml_handler = new StackExchangeHandler(writer);
                else
                    xml_handler = new WikipediaHandler(writer);
                indexer = new Indexer(path_string, xml_handler, writer);
            }

            long end = System.currentTimeMillis();
            System.out.println("Parsing took " + (end-start)/1000 + " seconds");

        } catch (IOException e) {
            e.printStackTrace();
        }
        prog_bar.setValue(25);
        QueryParser queryParser = new QueryParser("content", new StandardAnalyzer());
        BooleanQuery query = null;
        try {
            query = (BooleanQuery) queryParser.parse("month year day");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        VectorSpaceModel vs = new VectorSpaceModel(query, indexer.getReader(), prog_bar);
        prog_bar.setValue(50);
        vs.scoreDocs_cosine();
        prog_bar.setValue(75);
        vs.query_doc = null;

        GUI gui = new GUI(vs);
        gui.setHistogramPrefix(histogram_prefix);
        gui.setSuppressHistograms(suppress_histograms);
        prog_bar.setValue(100);
        frame.dispose();
        if(evaluate) {
            for(int i = 0; i < number_evals; i++) {
                gui.setHistogramPrefix(i+"_"+histogram_prefix);
                gui.getEvaluateListener().actionPerformed(new ActionEvent(Main.class, ActionEvent.ACTION_PERFORMED, null));
                try {
                    gui.getEvaluationThread().join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(stop_after_evaluation)
                System.exit(1);
        }


    }
}
