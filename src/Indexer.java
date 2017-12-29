import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Andreas on 08.05.2017.
 */
public class Indexer {
    private HashMap<String, ArrayList<String>> index;
    private ArrayList<String> tokens;
    private String folder_path;
    private int filecounter;


    public Indexer(String folder_path_){
        index = new HashMap<>();
        tokens  = new ArrayList<>();
        folder_path = folder_path_;
        filecounter = 0;
    }
    public void processFiles(){
        Directory dir;
        try {
            dir = FSDirectory.open((new File(folder_path+"/index")).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }



        File folder = new File(folder_path);
        File[] files_in_folder;
        ArrayList<String> actual_file = new ArrayList<>();
        String actual_filename;
        if(folder.isDirectory()){
            files_in_folder = folder.listFiles();
            for(File fd : files_in_folder){
                try {
                    if(fd.isFile() && fd.getAbsolutePath().contains(".txt")) {
                        if (Paths.get(fd.getAbsolutePath()) != null) {
                            actual_file = (ArrayList<String>) Files.readAllLines(Paths.get(fd.getAbsolutePath()), Charset.forName("Cp1252"));
                            actual_filename = fd.getAbsolutePath();
                            for (String line : actual_file) {
                                tokens.addAll(Arrays.asList(line.split(" ")));
                            }
                            if (tokens != null) {
                                for (String term : tokens) {
                                    term = term.trim();
                                    if (index.containsKey(term) == false) {
                                        index.put(term, new ArrayList<String>());
                                        index.get(term).add(actual_filename);
                                    } else {
                                        index.get(term).add(actual_filename);
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public HashMap<String, ArrayList<String>> getIndex(){
        return index;
    }
}
