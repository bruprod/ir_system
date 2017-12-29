import java.util.Comparator;

/**
 * Created by Andreas.
 * The comparator for comparing two documents
 */
public class DocCompare implements Comparator<Doc> {
    @Override
    public int compare(Doc o1, Doc o2) {
        if(o1.getScore() == o2.getScore())
            return 0;
        else if (o1.getScore() > o2.getScore())
            return 1;
        else
            return -1;
    }
}
