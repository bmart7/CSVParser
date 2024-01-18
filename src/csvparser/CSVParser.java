/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csvparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;


/**
 *
 * @author bmart
 */
public class CSVParser {
    static final int TITLE_INDEX = 18;
    static final int VARIANT_INDEX = 19;
    static final int QUANTITY_INDEX = 22;
    static final int OPTIONS_INDEX = 28;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV", "csv");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(null);
        CSVParser parser;
        String completeMessage = "Complete";
        if (returnVal == JFileChooser.APPROVE_OPTION && fc.getSelectedFile().exists()){
            parser = new CSVParser(fc.getSelectedFile());
            try{
                parser.readFile();
                parser.sortArray();
                parser.writeFile();
            } catch (Exception e){
                e.printStackTrace();
                completeMessage = "An Error Occurred: " + e.getMessage();
            }
        }
        JOptionPane popup = new JOptionPane();
        popup.showMessageDialog(null,completeMessage);
    }
    
    public CSVParser(File file){
        csv = file;
    }
    
    public void readFile() throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(csv));
        String row = br.readLine();
        String[] titles = row.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
        ArrayList<String> kitItems = new ArrayList<>();
        for (int i = OPTIONS_INDEX; i < titles.length; i++){
            String kitItem = titles[i];
            if (titles[i].contains("- Option")){
                kitItem = titles[i].substring(0, titles[i].indexOf(" - "));
            }
            kitItems.add(kitItem);
        }
        while ((row = br.readLine()) != null){
            String[] attributes = row.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
            if (attributes.length != 0){
                int quantity = Integer.parseInt(attributes[QUANTITY_INDEX].trim());
                if (!attributes[TITLE_INDEX].contains("Kit")){
                    addToList(new Item(attributes[TITLE_INDEX].trim(),attributes[VARIANT_INDEX].trim(),quantity));
                } else{
                    for (int i = OPTIONS_INDEX; i < attributes.length; i++){
                        if (attributes[i].trim().compareTo("") != 0
                                && !attributes[i].trim().contains("DO NOT WANT THIS")){
                            String name = kitItems.get(i - 28);
                            addToList(new Item(name,attributes[i].trim(),1));
                        }
                    }
                }
            }
        }
        br.close();
    }
    
    public void addToList(Item lineItem){
        int loc = inArray(lineItem);
        if (loc != -1){
            items.get(loc).incrementQuantityBy(lineItem.quantity);
        } else {
            items.add(lineItem);
        }
    }
    
    public void sortArray(){
        Collections.sort(items);
    }
    
    public int inArray(Item check){
        int count = 0;
        for (Item item : items){
            if (item.options.size() == check.options.size()){
                boolean sameName = item.name.compareTo(check.name) == 0;
                boolean sameSize = item.size == check.size && item.tall == check.tall;
                boolean sameOptions = true;
                for (int i = 0; i < item.options.size(); i++){
                    if (item.options.get(i).compareTo(check.options.get(i)) != 0){
                        sameOptions = false;
                        break;
                    }
                }
                if (sameName && sameSize && sameOptions){
                    return count;
                }
            }
            count++;
        }
        return -1;
    }
    
    public void writeFile() throws IOException{
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd_HH-mm");
        LocalDateTime now = LocalDateTime.now();
        String outName = "totals_" + format.format(now) + ".csv";
        String desktopLoc = System.getProperty("user.home") + "/Desktop/";
        File outFile = new File(desktopLoc + outName);
        outFile.createNewFile();
        PrintWriter out = new PrintWriter(outFile);
        out.println("Quantity,Item Name,Size,Opt 1,Opt 2,Opt 3,Opt 4");
        for (Item item : items){
            out.println(item.toCSVString());
        }
        out.close();
    }
    
    File csv;
    ArrayList<Item> items = new ArrayList<>();
    
    private class Item implements Comparable<Item>{
        private final String[] sizes = {"One Size", "XS","Small","Medium","Large","X-Large","2XL","3XL","4XL","5XL","6XL"};
        private final static String sizeRegex = "One Size|XS|Small|Medium|Large|X-Large|2XL|3XL|4XL|5XL|6XL";
        String name;
        int size;
        boolean tall = false;
        ArrayList<String> options = new ArrayList<>();
        int quantity;
        
        public Item(String name, String lineItem, int quantity) {
            this.name = name;
            this.quantity = quantity;
            String[] details = lineItem.split("/");
            for (String detail : details){
                if (detail.trim().matches(sizeRegex)){
                    size = findSize(detail.trim());
                } else if (detail.contains("-TALL")){
                    size = findSize(detail.trim().substring(0,detail.indexOf("-TALL")));
                    tall = true;
                }
                else if (detail.trim().contains("Quantity")){
                    this.quantity = Integer.parseInt(detail.replace("Quantity","").trim());
                } else{
                    options.add(detail.trim());
                }
            }
        }
        
        public void incrementQuantityBy(int increment){
            quantity += increment;
        }
        
        public String toCSVString() {
            String result = quantity + "," + name + "," + sizes[size] + (tall ? "-Tall" : "");
            for (String opts : options) {
                result += "," + opts;
            }
            return result;
        }
        
                
        private int findSize(String find){
            int count = 0;
            for (String size : sizes){
                if (find.compareTo(size) == 0){
                    return count;
                }
                count++;
            }
            return -1;
        }
        
        @Override
        public int compareTo(Item i2){
            int comp1 = this.name.compareTo(i2.name);
            if (comp1 != 0){
                return comp1;
            } else {
                return (this.size < i2.size ? -1 : 
                    (this.size == i2.size && this.tall == false && i2.tall == true ? -1:
                    (this.size == i2.size && this.tall == i2.tall ? 0 : 1))); 
            }
        }
    }
}
