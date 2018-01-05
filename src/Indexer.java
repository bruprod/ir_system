import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Andreas.
 */
public class Indexer {
    Directory dir = null;
    IndexWriter writer = null;
    DocumentBuilder doc_builder = null;
    org.w3c.dom.Document xml_document = null;
    Path index_path;
    IndexReader reader;
    DefaultHandler handler;

    /**
     * The constructor for the Indexer class for indexing corporas during runtime of the Information Retrieval System.
     * @param path This parameter contains the path to the file which should be indexed.
     * @param handler This Parameter contains the XML-Handler which is used for parsing.
     * @param writer The IndexWriter, which is responsible for writing to the index of the Information Retrieval System.
     * @throws IOException Throws an IOException if any error occurs while parsing.
     */
    public Indexer(String path, DefaultHandler handler, IndexWriter writer) throws IOException {
        try {
            index_path = new File(Paths.get("").toAbsolutePath().toString() + "\\index").toPath();
            dir = FSDirectory.open((index_path));

        } catch (IOException e) {
            e.printStackTrace();
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(path);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        InputSource in_source = new InputSource(is);
        try {
            doc_builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            System.out.println("Start parsing");
            long start = System.currentTimeMillis();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            SAXParser parser = factory.newSAXParser();
            parser.parse(in_source, handler);
            if(handler instanceof StackExchangeHandler)
                retrieveCooccurrences((StackExchangeHandler) handler);
            else
                new File(Paths.get("").toAbsolutePath() + "\\index\\co_occurrences.txt").delete();
            long end = System.currentTimeMillis();
            System.out.println("Parsing took " + (end - start) / 1000 + "." + (end - start) % 1000 + " seconds");
            System.out.println(" Sax Parser done");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        reader = DirectoryReader.open(dir);
        System.out.println("Indexing done");
    }

    /**
     * The Indexer used before initializing the vector space model. Runs only if new index is already written, with the
     * small artificial intelligence StackExchange corpus.
     * @param path
     * @throws IOException Throws an IOException if any error occurs while parsing.
     */
    public Indexer(String path) throws IOException {
        try {
            index_path = new File(Paths.get("").toAbsolutePath().toString() + "\\index").toPath();
            dir = FSDirectory.open((index_path));

        } catch (IOException e) {
            e.printStackTrace();
        }
        FileInputStream is = null;
        try {
             is = new FileInputStream(new File(Paths.get("").toAbsolutePath().toString()+"\\index_source\\Posts.xml"));
        } catch (FileNotFoundException e) {
            System.out.println("No such File!");
            e.printStackTrace();
        }
        InputSource in_source = new InputSource(is);
        if(!DirectoryReader.indexExists(FSDirectory.open(index_path))) {
            try {
                IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                conf.setRAMBufferSizeMB(1024);

                writer = new IndexWriter(dir, conf);
                doc_builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                System.out.println("Start parsing");
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                SAXParser parser = factory.newSAXParser();
                //WikipediaHandler handler = new WikipediaHandler(writer);
                handler = new StackExchangeHandler(writer);

                parser.parse(in_source, (DefaultHandler) handler);
                retrieveCooccurrences((StackExchangeHandler) handler);
                System.out.println(" Sax Parser done");
                //xml_document = doc_builder.parse(in_source);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //extractDocuments();
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Here");
        }

        reader = DirectoryReader.open(dir);
        System.out.println("Indexing done");
    }


    /**
     * Can be used when indexing small XML-Files with only some Documents.
     * When used with many documents and big XML-Files it will cause an
     * OutOfMemory error
     * @return returns an ArrayList with Lucene documents
     */
    public ArrayList<Document> extractDocuments() throws OutOfMemoryError{
        ArrayList docs = new ArrayList();
        NodeList xml_pages = xml_document.getElementsByTagName("page");
        for(int i = 0; i < xml_pages.getLength(); i++){
            org.apache.lucene.document.Document lucene_doc = new org.apache.lucene.document.Document();
            NodeList page_childs = xml_pages.item(i).getChildNodes();
            String title = null;
            String content = null;
            if(i % 1000 == 0)
                System.out.println("Indexed doc " + i + " from " + xml_pages.getLength());

            for(int j = 0; j < page_childs.getLength(); j++){
                switch (page_childs.item(j).getNodeName()){
                    case "title":
                        title = page_childs.item(j).getTextContent();
                        break;
                    case "revision":
                        for(int k = 0; k < page_childs.item(j).getChildNodes().getLength(); k++){
                            if(page_childs.item(j).getChildNodes().item(k).getNodeName().equals("text"))
                                content = page_childs.item(j).getChildNodes().item(k).getTextContent();
                        }
                }
            }
            FieldType field_type = new FieldType();
            field_type.setStored(true);
            field_type.setTokenized(true);
            field_type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            field_type.setStoreTermVectors(true);
            field_type.setStoreTermVectorOffsets(true);
            field_type.setStoreTermVectorPositions(true);

            Field content_field = new Field("content", content, field_type);
            StoredField title_field = new StoredField("title", title);
            lucene_doc.add(content_field);
            lucene_doc.add(title_field);
            docs.add(lucene_doc);
            try {
                writer.addDocument(lucene_doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return docs;
    }

    public DocumentBuilder getDocbuilder() {
        return doc_builder;
    }

    /**
     * @return returns the IndexReader
     */
    public IndexReader getReader() {
        return reader;
    }

    /**
     * Searches the most frequent co-occurrence tag for each tag and writes it to the co_occurrences.txt file
     * @param handler The StackExchangeHandler object which holds all co-occurrences of all tags.
     */
    public void retrieveCooccurrences(StackExchangeHandler handler) {
        HashMap<String, HashMap<String, Integer>> co_occurrences  = handler.getCo_occurrences();
        String co_occurrence_string = "";
        for(String tag: (new ArrayList<String>(co_occurrences.keySet()))) {
            HashMap<String, Integer> value =  co_occurrences.get(tag);
            String best_key = "";
            Integer best_value = 0;
            if(value != null) {
                for (String key : value.keySet()) {
                    if (best_key.length() == 0) {
                        best_key = key;
                        best_value = value.get(key);
                    } else if (value.get(key) > best_value) {
                        best_value = value.get(key);
                        best_key = key;
                    }
                }
                co_occurrence_string += tag + "|" + best_key + ";";
            }
        }
        try {
            FileOutputStream os = new FileOutputStream(Paths.get("").toAbsolutePath() + "\\index\\co_occurrences.txt", false);
            os.write(co_occurrence_string.getBytes(), 0, co_occurrence_string.length());
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("cooccurrences" + co_occurrence_string);
    }
}
