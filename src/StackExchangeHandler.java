import org.apache.commons.text.StringEscapeUtils;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by Andreas.
 */
public class StackExchangeHandler extends DefaultHandler {
    int id = 0;
    final String row = "row";
    final String Tags = "Tags";
    final String Title = "Title";
    final String body = "Body";
    HashMap<String, HashMap<String, Integer>> co_occurrences = new HashMap<>();
    String node_content;
    IndexWriter writer;

    /**
     * Sets the IndexWriter
     * @param writer The IndexWriter for builing the Index
     */
    public StackExchangeHandler(IndexWriter writer){
        this.writer = writer;
    }

    /**
     * Only this method has to be overwritten for handling the StackExchange XML-Files
     * because all necessary data are stored in the attributes of the tags.
     * Also every element contains the information for exactly one page.
     * StringEscapeUtils are used for unescaping the HTML/XML special characters.
     * @param uri the namespace uri String for the element
     * @param localName the localname of an element without the prefix
     * @param qName the name of an element with the prefix
     * @param attributes the attributes of an element
     * @throws SAXException throws an exception if XML-File is inconsistent
     */
    @Override
    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) throws SAXException {
        WikiPage page = new WikiPage();
        if(qName.equals(row)) {
            page.setContent(StringEscapeUtils.unescapeHtml4(attributes.getValue(body)));
            page.setTitle(StringEscapeUtils.unescapeHtml4(attributes.getValue(Title)));
            String tags = attributes.getValue(Tags);
            if(tags != null)
                tags = StringEscapeUtils.unescapeHtml4(tags.replace("<", "").replace(">", ";"));

            if(page.getContent() != null && page.getTitle() != null ) {
                org.apache.lucene.document.Document lucene_doc = new org.apache.lucene.document.Document();
                FieldType field_type = new FieldType();
                field_type.setStored(true);
                field_type.setTokenized(true);
                field_type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                field_type.setStoreTermVectors(true);
                field_type.setStoreTermVectorOffsets(true);
                field_type.setStoreTermVectorPositions(true);
                field_type.freeze();

                Field content_field = new Field("content", page.getContent(), field_type);
                StoredField title_field = new StoredField("title", page.getTitle());
                StoredField categorie_field = new StoredField("categorie", page.getCategory());
                StoredField out_links = new StoredField("outlinks", page.getOutlinksString().toLowerCase());
                StoredField parent_categories = new StoredField("parent_categories", page.getParentCategoriesString());
                if(tags == null)
                    tags = new String();
                StoredField tags_field = new StoredField("tags", tags);
                for(String tag : tags.split(";")){
                    ArrayList<String> tags_arr = new ArrayList<>(Arrays.asList(tags.split(";")));
                    tags_arr.remove(tag);
                    for(String tag_1 : tags_arr) {
                        HashMap<String, Integer> new_map = new HashMap<>();
                        new_map.put(tag_1, 1);
                        HashMap<String, Integer> value = co_occurrences.putIfAbsent(tag, new_map);
                        if(value != null) {
                            Integer co_occurrence_counter = value.putIfAbsent(tag_1, 1);
                            if(co_occurrence_counter != null)
                                value.put(tag_1, co_occurrence_counter+1);
                        }
                    }
                }

                lucene_doc.add(content_field);
                lucene_doc.add(title_field);
                lucene_doc.add(categorie_field);
                lucene_doc.add(parent_categories);
                lucene_doc.add(out_links);
                lucene_doc.add(tags_field);
                try {
                    writer.addDocument(lucene_doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        id++;
        if((id % 10000) == 0)
        System.out.println("Indexed doc: " + id);
    }

    /**
     * @param uri the namespace uri String for a element
     * @param localName the localname of an element without the prefix
     * @param qName the name of an element with the prefix
     */
    @Override
    public void endElement(String uri, String localName, String qName){

    }

    public HashMap<String, HashMap<String, Integer>> getCo_occurrences() {
        return co_occurrences;
    }
}

