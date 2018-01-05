import java.util.ArrayList;

/**
 * Created by Andreas.
 */
public class WikiPage {
    String content = new String();
    String title = new String();
    String category = new String("");
    ArrayList<String> parent_categories = new ArrayList<>();
    ArrayList<String> outlinks = new ArrayList<>();

    /**
     * Sets the content of the wikipedia page
     * @param content contains a String with the content of a wikipedia page
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Sets the title of the wikipedia page
     * @param title contains a String with the wikipedia page title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return returns the wikipedia page title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return returns the content of the wikipedia page
     */
    public String getContent() {
        return content;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    /**
     * @return returns a csv String containing all parent categories
     */
    public String getParentCategoriesString() {
        return arraylistToCsv(parent_categories);
    }

    /**
     * Sets the ArrayList which contains all parent categories of the wikipedia page
     * @param parent_categories ArrayList containing the parent categories of the page
     */
    public void setParentCategories(ArrayList<String> parent_categories) {
        this.parent_categories = parent_categories;
    }

    /**
     * Sets the outlinks which are all outgoing links from the wikipedia page
     * @param outlinks ArrayList containing the outgoing links of the page
     */
    public void setOutlinks(ArrayList<String> outlinks) {
        this.outlinks = outlinks;
    }

    /**
     * @return returns the csv String containing all outgoing links
     */
    public String getOutlinksString() {
        return arraylistToCsv(outlinks);
    }

    /**
     * Converts a ArrayList to a comma seperated value String
     * @param arr_list ArrayList of Strings to convert to CSV-String
     * @return returns the csv String containing all ArrayList strings
     */
    public String arraylistToCsv(ArrayList<String> arr_list){
        if(arr_list.size() > 0) {
            String pc_string = new String();
            pc_string = arr_list.get(0);
            arr_list.remove(0);
            for (String cat : arr_list)
                pc_string += ";" + cat;

            return pc_string;
        }
        else
            return "";
    }
}
