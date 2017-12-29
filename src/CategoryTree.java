import java.util.HashMap;

/**
 * Created by Andreas.
 */
public class CategoryTree {
    HashMap<String, CategoryNode> category_map = new HashMap<>();
    CategoryNode root_category;

    /**
     * The initialization for the CategoryTree
     * @param category_map the HashMap containing all mappings from a category name to its node object
     * @param root the root node
     */
    public CategoryTree(HashMap category_map, CategoryNode root){
        this.category_map = category_map;
        root_category = root;

    }

    /**
     * @return returns the root node
     */
    public CategoryNode getRootCategory(){
        return root_category;
    }

    /**
     * @param name the name of the category
     * @return returns the CategoryNode to a category name
     */
    public CategoryNode getCategory(String name){
        return category_map.get(name);
    }




}
