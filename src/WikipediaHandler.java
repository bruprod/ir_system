import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Andreas.
 */

public class WikipediaHandler extends DefaultHandler {
    int id = 0;
    WikiPage wiki_page;
    final String title = "title";
    final String page = "page";
    final String text = "text";
    final String content = "content";
    boolean escape = false;
    boolean categorie_page = false;
    String node_content;
    IndexWriter writer;

    /**
     * Sets the IndexWriter
     * @param writer The IndexWriter for building the Index
     */
    public WikipediaHandler(IndexWriter writer){
        this.writer = writer;
    }

    /**
     * Initializes new WikiPage element each time a page starts in the XML-File,
     * also resets the boolean variables for the escaping of a page and if it is a category page.
     * @param uri the namespace uri String for the element
     * @param localName the localname of an element without the prefix
     * @param qName the name of an element with the prefix
     * @param attributes the attributes of an element
     * @throws SAXException SAXException throws an exception if XML-File is inconsistent
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        node_content = new String();
        if(qName.equals(page)){
            wiki_page = new WikiPage();
            escape = false;
            categorie_page = false;
        }
        else if(qName.equals("redirect"))
            escape = true;
    }

    /**
     * When the tag is closed all information have been extracted for the tag.
     * Here the necessary information from a page in the XML get saved in the wikipage
     * element. Also the boolean variables are set for escaping a page and for extracting the categories.
     * Here also the processing for the parent categories and inlinks take place.
     * @param uri the namespace uri String for a element
     * @param localName the localname of an element without the prefix
     * @param qName the name of an element with the prefix
     * @throws SAXException throws an exception if XML-File is inconsistent
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(qName.equals(title)){
            wiki_page.setTitle(node_content);
            if(node_content.contains("User talk:"))
                escape = true;
            else if(node_content.contains("User:"))
                escape = true;
            else if(node_content.contains("Wikipedia talk:"))
                escape = true;
            else if(node_content.contains("Category talk:"))
                escape = true;
            else if(node_content.contains("Talk:"))
                escape = true;
            else if(node_content.contains("Template talk"))
                escape = true;
            else if(node_content.contains("Category:")){
                wiki_page.setCategory(node_content.split(":")[1]);
                escape = true;
            }
            else if(node_content.contains("Template:"))
                escape = true;
        }
        else if(qName.equals(text)){
            ArrayList<String> parent_categories = new ArrayList<>();
            ArrayList<String> out_links = new ArrayList<>();
            String categorie = "[[Category:";
            String link = "[[";
            extractLinks(parent_categories, categorie);
            extractLinks(out_links, link);
            node_content.replaceAll("\\[\\[(\\w|\\s)+:(\\w|\\s)+\\.(\\w|\\s)+(\\||\\w|\\s)+\\]\\]", "");
            wiki_page.setContent(node_content);
            wiki_page.setParentCategories(parent_categories);
            wiki_page.setOutlinks(out_links);
        }
        else if(qName.equals(page) && !escape){
            try {
                org.apache.lucene.document.Document lucene_doc = new org.apache.lucene.document.Document();
                FieldType field_type = new FieldType();
                field_type.setStored(true);
                field_type.setTokenized(true);
                field_type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                field_type.setStoreTermVectors(true);
                field_type.setStoreTermVectorOffsets(true);
                field_type.setStoreTermVectorPositions(true);
                field_type.freeze();


                Field content_field = new Field("content", wiki_page.getContent(), field_type);
                StoredField title_field = new StoredField(title, wiki_page.getTitle());
                StoredField categorie_field = new StoredField("categorie", wiki_page.getCategory());
                StoredField out_links = new StoredField("outlinks", wiki_page.getOutlinksString().toLowerCase());
                StoredField parent_categories = new StoredField("parent_categories", wiki_page.getParentCategoriesString());
                lucene_doc.add(content_field);
                lucene_doc.add(title_field);
                lucene_doc.add(categorie_field);
                lucene_doc.add(parent_categories);
                lucene_doc.add(out_links);
                writer.addDocument(lucene_doc);
                id++;
                if((id % 10000) == 0)
                    System.out.println("Indexed wikipedia document " + id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extends the String which holds the content of an element with the characters read.
     * @param chars the characters which have been read while running through an elements content
     * @param start the offset from the beginning of the String
     * @param len the length of the character array
     * @throws SAXException throws an exception if XML-File is inconsistent
     */
    @Override
    public void characters(char[] chars, int start, int len) throws SAXException {
        node_content += new String(chars, start, len);
    }

    /**
     * Extracts the links and the categories from the String which contains all links or categories.
     * @param parent_categories The ArrayList containing the links or categories after processing
     * @param categorie The String which determines the beginning of a link or category in the content
     */
    public void extractLinks(ArrayList<String> parent_categories, String categorie){
        int index = 0;
        String content = new String(node_content);
        while((index = content.indexOf(categorie)) != -1){
            boolean escape_string = false;
            content = content.substring(index + categorie.length());
            if(content.indexOf("]]") == -1) {
                System.out.println(wiki_page.title);
                continue;
            }
            String cat = content.substring(0, content.indexOf("]]"));
            if(cat.contains(":")){
                escape_string = true;
            }
            else if(cat.contains("|")){
                cat = cat.substring(0, cat.indexOf("|"));
            }
            if(!escape_string)
                parent_categories.add(cat);
        }
    }
}
