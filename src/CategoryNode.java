import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Andreas.
 */
public class CategoryNode {
    String categorie_name;
    ArrayList<CategoryNode> sub_categories;
    ArrayList<CategoryNode> parent_nodes;
    boolean restricted = false;

    /**
     * IntelliJ auto generated equals method
     * @param o Object to be compared
     * @return returns the boolean if two CategoryNode objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryNode)) return false;

        CategoryNode that = (CategoryNode) o;

        if (restricted != that.restricted) return false;
        return categorie_name.equals(that.categorie_name);

    }

    /**
     * IntelliJ auto generated hashCode calculation
     * @return returns the hashCode to a CategoryNode
     */
    @Override
    public int hashCode() {
        int result = categorie_name.hashCode();
        result = 31 * result + (restricted ? 1 : 0);
        return result;
    }

    /**
     * @return returns if a node is restricted or not
     */
    public boolean isRestricted() {
        return restricted;

    }

    /**
     * Sets the CategoryNode to restricted
     * @param restricted boolean to set the node restricted
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    /**
     * Constructor which sets CategoryName and the parent categories to a category
     * @param name the name of the Category
     * @param parent_node the ArrayList containing the parent categories
     */
    public CategoryNode(String name, ArrayList<CategoryNode> parent_node){
        categorie_name = name;
        sub_categories = new ArrayList<>();
        this.parent_nodes = parent_node;
    }

    /**
     * Adds a subcategory to the subcategory ArrayList
     * @param node the CategoryNode to add
     */
    public void addSubCategory(CategoryNode node){
        sub_categories.remove(node);
        sub_categories.add(node);
    }

    /**
     * @return returns the subcategories of a category
     */
    public ArrayList<CategoryNode> getSubCategories() {
        return sub_categories;
    }

    /**
     * Adds a parentcategory to the parentcategory ArrayList
     * @param node the CategoryNode to add
     */
    public void addParentCategory(CategoryNode node){
        parent_nodes.remove(node);
        parent_nodes.add(node);
    }

    /**
     * @return returns the parent nodes to a category
     */
    public ArrayList<CategoryNode> getParentNodes() {
        return parent_nodes;
    }

    /**
     * Checks if a category has parents
     * @return returns true or false if the category has parents
     */
    public boolean hasParents(){
        if(parent_nodes.size()>0)
            return true;
        else
            return false;
    }

    /**
     * Non recursive BFS algorithm to walk the tree and gather the childs of a node
     * @return walks the tree and returns a ArrayList with all CategoryNodes which are childs of
     *         any grade to the node
     */
    public ArrayList<CategoryNode> getAllChilds(){
        HashSet<CategoryNode> visited_nodes = new HashSet<>();
        visited_nodes.add(this);
        ArrayList<CategoryNode> nodes_to_visit = new ArrayList<>(sub_categories);
        for(int i = 0; i < nodes_to_visit.size(); i++){
            CategoryNode node = nodes_to_visit.get(i);
            visited_nodes.add(node);
            for(CategoryNode node1 : node.sub_categories){
                if(!visited_nodes.contains(node1)){
                    nodes_to_visit.add(node1);
                }
            }
        }
        return new ArrayList<>(visited_nodes);
    }

    /**
     * Non recursive BFS algorithm to walk the tree and gather the childs of a node
     * @return walks the tree and returns a ArrayList with all CategoryNodes which are childs of
     *         any grade to the node and sets them restricted
     */
    public ArrayList<CategoryNode> getAllChildsAndSetRestricted(){
        long begin = System.currentTimeMillis();
        HashSet<CategoryNode> visited_nodes = new HashSet<>();
        visited_nodes.add(this);
        ArrayList<CategoryNode> nodes_to_visit = new ArrayList<>(sub_categories);
        for(int i = 0; i < nodes_to_visit.size(); i++){
            CategoryNode node = nodes_to_visit.get(i);
            node.setRestricted(true);
            visited_nodes.add(node);
            for(CategoryNode node1 : node.sub_categories){
                if(!visited_nodes.contains(node1)){
                    nodes_to_visit.add(node1);
                }
            }
            if(visited_nodes.size() == 200000)
                break;
        }
        long end = System.currentTimeMillis();
        System.out.println("Treewalking takes: " + (end-begin)/1000 + "." + (end-begin) %1000 + " seconds");
        return new ArrayList<>(visited_nodes);
    }
}
