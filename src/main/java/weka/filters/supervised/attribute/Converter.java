package weka.filters.supervised.attribute; 
 
import java.io.File; 
import java.io.FilenameFilter; 
import weka.core.converters.ConverterUtils.DataSource; 
import weka.core.converters.XRFFSaver; 
import weka.core.Instances; 
import java.io.BufferedReader; 
import java.io.FileReader; 
import static weka.filters.Filter.runFilter;
/* 
 * Converter.java 
 * 
 *
 * 
 * To change this template, choose Tools | Template Manager 
 * and open the template in the editor. 
 */ 
 
/** 
 * 
 * @author Peng 
 */ 
public class Converter { 
    
    public static void main(String [] argv) 
    {
        Converter type1=new Converter();
        
        
    }
     
    /** Creates a new instance of Converter */ 
    public Converter() { 
    } 
     
    void convertDirs(String arffPath, String txtPath, String xrffPath) throws Exception { 
        System.out.println("Source ARFF file path is " + arffPath); 
        System.out.println("Source weight file path is "+ txtPath); 
        System.out.println("Target XRFF file path is " + xrffPath); 
        System.out.println(""); 
         
        File arffLister = new File(arffPath); 
        String[] Arffs = arffLister.list(new arffFilter()); //using file filter to get .arff files list 
        System.out.println("Found " + Arffs.length + " ARFF(s): "); 
        for(int i=0; i<Arffs.length; i++){ 
            System.out.println(Arffs[i]); 
        } 
        System.out.println(""); 
         
        File txtLister = new File(txtPath); 
        String[] Txts = txtLister.list(new txtFilter()); //using file filter to get .txt files list 
        System.out.println("Found " + Txts.length + " weight file(s): "); 
        for(int i=0; i<Txts.length; i++){ 
            System.out.println(Txts[i]); 
        } 
        System.out.println(""); 
         
        for(int i =0; i< Arffs.length; i++){ 
            for(int n= 0; n< Txts.length; n++){ 
                try{ 
                    String weightFileName = Txts[n].substring(0,Arffs[i].lastIndexOf(".")); 
                    if(Arffs[i].substring(0, Arffs[i].lastIndexOf(".")).equalsIgnoreCase(weightFileName)){ 
                        convertFiles(arffPath + Arffs[i], txtPath +Txts[0], xrffPath + Txts[n].substring(0,Txts[n].lastIndexOf("."))+".xrff"); 
                    } 
                }catch(Exception e){ 
                     
                } 
            } 
        } 
         
        System.out.println(""); 
        System.out.println("Done"); 
    } 
     
    void convertFiles(String arff, String txt, String xrff) throws Exception { 
        System.out.println("Converting "+ arff + " " + txt + " ..."); 
        // load data 
        DataSource source = new DataSource(arff); 
        Instances data = source.getDataSet(); 
        if (data.classIndex() == -1) 
            data.setClassIndex(data.numAttributes() - 1); 
         
        // read and set weights 
        BufferedReader reader = new BufferedReader(new FileReader(txt)); 
        for (int i = 0; i < data.numInstances(); i++) { 
            String line = reader.readLine(); 
            double weight = Double.parseDouble(line); 
            data.instance(i).setWeight(weight); 
        } 
        reader.close(); 
         
        // save data 
        XRFFSaver saver = new XRFFSaver(); 
        saver.setFile(new File(xrff)); 
        saver.setInstances(data); 
        saver.writeBatch(); 
         
        System.out.println("Generated " + xrff); 
    } 
     
} 
class txtFilter implements FilenameFilter { 
     
    public boolean accept( File dir, String name ) { 
        if ( new File( dir, name ).isDirectory() ) { 
            return false; 
        } 
        name = name.toLowerCase(); 
        return name.endsWith( ".txt" ); 
    } 
} 
 
class arffFilter implements FilenameFilter { 
     
    public boolean accept( File dir, String name ) { 
        if ( new File( dir, name ).isDirectory() ) { 
            return false; 
        } 
        name = name.toLowerCase(); 
        return name.endsWith( ".arff" ); 
    } 
} 

    