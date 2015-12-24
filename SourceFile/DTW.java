/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.filters.supervised.attribute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JOptionPane;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;
import static weka.filters.Filter.runFilter;
/** 
 *
 * @author osman
 */
public class DTW extends SimpleBatchFilter implements SupervisedFilter, OptionHandler{

    protected Range m_SelectedRange = new Range("first-last");
        
    //<editor-fold defaultstate="collapsed" desc="Types Of Algorithms/Classes To Consider">
    public static final int RR=1;
    public static final int LRR=2;
    public static final int OR=3;
    public static final int LOR=4;
    public static final int KL_DIVERGENCE=5;
    
    
    public static final int DOMINANT_CLASS=1;
    public static final int ALL_CLASSES=2;
   
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Option Variables">
    private int WeightType=OR; 
    private int ClassType=DOMINANT_CLASS;
    private int no_of_classes=2;
    private double threshold=0;
    public static int num_of_records=0;
    
    
    public static  HashMap<String , Integer> class_list=new HashMap<String, Integer>(0);//to store global count of each class occurance
    HashMap<String, Word> Dictionary = new HashMap<String, Word>(0);//to Store individual words as features, along with their stats in Word type object
    private List<String> list;
    
    public static final Tag [] FILTERING_OPTIONS = {
        new Tag(RR, "RR"),
        new Tag(LRR, "LRR"),
        new Tag(OR, "OR"),
        new Tag(LOR, "LOR"),
        new Tag(KL_DIVERGENCE, "KL_DIVERGENCE")
    };
    
    
    public static final Tag [] CLASS_SELECTION = {
        new Tag(DOMINANT_CLASS, "Dominant Class Only"),
        new Tag(ALL_CLASSES, "All Classes"),

    };
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="No_of_classes">
    public int getNo_of_classes() {
        return class_list.size();
    }

    public void setNo_of_classes(Enumeration class_names) 
    {
        
        
        
        while(class_names.hasMoreElements())
        {
           
            class_list.put(class_names.nextElement().toString(),0);
        }
        

    }
    
    //<editor-fold defaultstate="collapsed" desc="Threshold">
    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="ClassType">
    public void setClassType(SelectedTag classType)
    {
        if (classType.getTags() == CLASS_SELECTION) 
        {
          ClassType = classType.getSelectedTag().getID();
        }
        
    }
    
    public SelectedTag getClassType()
    {
        return new SelectedTag(ClassType, CLASS_SELECTION);
    }
   //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Filter Type">
    public void setFilterType(SelectedTag newType) 
    {
        if (newType.getTags() == FILTERING_OPTIONS) 
        {
          WeightType = newType.getSelectedTag().getID();
        }
    }
    public SelectedTag getFilterType() 
    {
        return new SelectedTag(WeightType, FILTERING_OPTIONS);
    }
     //</editor-fold>
    
//</editor-fold >
  
    //<editor-fold defaultstate="collapsed" desc="Init Setup">
    
    @Override
    public String globalInfo() {
     String myString = "A supervised attribute filter to determine the score of words in spam and non-spam emails";
     return myString;
    }


    public Capabilities getCapabilities()
    {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        /*STW Vector Capabilites*/
        
        result.enableAllAttributes();
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enableAllClasses();
        result.enable(Capability.MISSING_CLASS_VALUES);
        result.enable(Capability.NO_CLASS);
       
        /*STW V*/
    
        /*My Own Decided Capabilites*

        
            DTWArray.enable(Capability.NUMERIC_ATTRIBUTES);

            DTWArray.enable(Capability.NUMERIC_CLASS);
            DTWArray.enable(Capability.NOMINAL_ATTRIBUTES);
            DTWArray.enable(Capability.NOMINAL_CLASS);
            //result.enable(Capability.NO_CLASS);

       /*End of my decided capabilities */
        return result;
    }

    //</editor-fold>
        
    //<editor-fold defaultstate="collapsed" desc="Main Filter">
    
    /*
        The following 2 functions are called automatically by weka
            1.determineOutputFormat
            2. Process
    */
    @Override
    
    /*
        1.DetermineOutputFormat
        
            1. will see  the number of unique classes present in dataset.
            2. will create  a feature of type <class_name>_score and add it to the output format
            3. Will return that format at the end
    */
    protected Instances determineOutputFormat(Instances inputFormat) throws Exception 
    {
        System.out.println("Setting Output Format");
        System.out.println("Setting class");
        setNo_of_classes(inputFormat.attribute("class").enumerateValues());
        
        Instances result = null;
        
        try
        {
            
            result= new Instances(inputFormat);
            //empty the dataset
            result.delete();
            result.setClassIndex(-1);

            //emtpy all the attributes
            while(result.numAttributes()!=0)
            {
                result.deleteAttributeAt(0);
            }
            
            //fetch all unique clases
            list = new ArrayList<String>(class_list.keySet());
            result.insertAttributeAt(new Attribute("class",list),result.numAttributes());
            result.setClassIndex(0);
            

            //set each <class>_score as an attribute
            Iterator it=list.iterator();
            while(it.hasNext())
            {
              result.insertAttributeAt(new Attribute(it.next().toString() + "_Score"), result.numAttributes());
            }
            
            
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null,"ERROR: "+e.getMessage());
        }
        
        
        return result;
    }

    @Override
    /*
        2.process
        
            a. Will first count occurance of each word in their respective classes
            b. Then it will compute weight of each word based on those occurances
            c. using the computed weights it will once more go to the original corpus and compute its scores for respective classes
    */
    protected Instances process(Instances inst) throws Exception 
    {

        num_of_records= inst.size();
        Instances DTWArray=null;
        DTWArray=determineOutputFormat(inst);
        inst.setClass(inst.attribute("class"));
        
        //Step a
        //<editor-fold defaultstate="collapsed" desc="Update Statistics of each Word">

        try
        {
            
           double[] row;
           String state;
           
           //first create a word object for each word, send it the HashMap of the same format as the one maintaing the global count
           for(int i=1; i<inst.numAttributes();i++)
           {
               System.out.println("Adding "+ inst.attribute(i).name());
               Dictionary.put(inst.attribute(i).name(), new Word((HashMap<String,Integer>)class_list.clone()));
           }
           
           
           
           System.out.println(inst.numInstances()+" instance");           
           
           
           for(int i=0; i<inst.numInstances();i++)
           {
               
               //for each instance get its class
               state=inst.instance(i).stringValue(0);

               row=inst.instance(i).toDoubleArray();

               //increment the class's overall count in global hashMap
               class_list.put(state, class_list.get(state)+1);
               
               
               //foreach of the word attributes in that class
               for(int j=1;j<inst.numAttributes();j++)
               {
                   //fetch the word 
                   Word w= Dictionary.get(inst.attribute(j).name());
                   
                   //if the word is present in that document ((Double)row[j]).intValue() will be 1 else it will be 0
                   //for this word, add this count 1/0 to its native hashMap
                   w.addCount(state, ((Double)row[j]).intValue());
               }
               
              
           }
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null,"ERROR in process function,\nReason: Attribute classification: " + e.getMessage());
            
        }
        //</editor-fold>        
 
        //Step b
        //<editor-fold defaultstate="collapsed" desc="Assign Weights to For Each Class">
        
        computeWordWeights();
        
	//</editor-fold>		          
        

        //Step c
        //<editor-fold defaultstate="collapsed" desc="Compute Email Scores">
        
        //iterate over all documents once more
        for(int i=0; i<inst.size();i++)
        {
            
            Instance currentInstance = inst.instance(i);
            
            //get the class of current instance
            String classAttribute = inst.get(i).stringValue(inst.classIndex());
            
            //create a new instace which will now be according to filtered format
            double[] row = new double[class_list.size()+1];     //+1 for accomodating the class attribute
            
            
            //we are iterating over all the words in this document
            for(int j=1; j<inst.numAttributes();j++)
            {
                //Fetch the details of jth word in this document
                String attribute=inst.attribute(j).name();
                Word w = Dictionary.get(attribute);
                
                
                //get the list of all the classes 
                Set<String> classes=class_list.keySet();
                Iterator it=classes.iterator();
                int k=1;//starting from index 1 because 0 is reserved for class
                
                while(it.hasNext())
                {
                    
                    String category=(String)it.next();
               
                    //inst.get(i).value(j) will be 1 if the word was present in document 0 otherwise, 
                    //so following statement will either add the word's weight for that class to the score, or will add 0
                    row[k]+= (w.getClassWeight(category,ClassType) * inst.get(i).value(j));
                    
                    
                    k++;//move to the next class for this current word
                }
                
                
            }
            
            //now we have scores for each class for this given document
            row[0]=list.indexOf(classAttribute);

            //DTW array was of the same format as returend from determineutput format function
            //we add our row to this array
            DTWArray.add(new DenseInstance(1,row));
            
            
        }
        //</editor-fold>		          
        
        //once all instances are processed, we return this array back to weka
        return DTWArray;
    }
    
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Option Handling">
    
    @Override
   public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
	"\tType Of Algorithm.\n",
	"F", 0, "-F"));

    result.addElement(new Option(
	"\tThreshold.\n",
	"T", 0, "-T"));
    

    
    return result.elements();
  }
   
   public void setOptions(String[] options) throws Exception {
    String 	fString;
    String      tString;
    String      cString;
    try
    {
        fString = Utils.getOption('F', options);
         if (fString.length() != 0)
            setFilterType(new SelectedTag(Integer.parseInt(fString), FILTERING_OPTIONS));
         else
            setFilterType(new SelectedTag("RR", FILTERING_OPTIONS));
         
        tString = Utils.getOption('T', options);
         if (tString.length() != 0)
             setThreshold(Double.parseDouble(tString));
         else
             setThreshold(0.0);
        
         
         cString = Utils.getOption('C', options);
         if (cString.length() != 0)
             setClassType(new SelectedTag(Integer.parseInt(cString), CLASS_SELECTION));
         else
             setFilterType(new SelectedTag("Dominant Class Only", CLASS_SELECTION));

        JOptionPane.showMessageDialog(null,"Type: "+ WeightType);
        
    }
    catch(Exception e)
    {
        JOptionPane.showMessageDialog(null, "ErroR: "+e.getMessage());
        
    }
    
    
    
  }
  
   public String[] getOptions() {
    
       Vector      result;
       result = new Vector();
       
       
try
{
    
    result.add("-C "+new SelectedTag(ClassType, CLASS_SELECTION).toString()); 
    result.add("-F "+new SelectedTag(WeightType, FILTERING_OPTIONS).toString()); 
    result.add("-T "+getThreshold());
}
catch(Exception e)
{
    JOptionPane.showMessageDialog(null, "ErroR2: "+e.getMessage());
}
    
    return (String[]) result.toArray(new String[result.size()]);
  }
   
   
   //</editor-fold>
   
    private void computeWordWeights() {
        
        Set<String> set=Dictionary.keySet();
        
        //get each word from our dictionary
        //compute its weights for each of the class one by one
        for(String s: set)
        {
            System.out.println(s);
            Dictionary.get(s).calculateWeights(WeightType);
        }
        
    }
    
    // <editor-fold defaultstate="collapsed" desc="Functions for Debugging only">
    private void printClassList() {
        Set<String> keySet= class_list.keySet();
        
        for(String s: keySet)
        {
            System.out.println(s+ ", "+class_list.get(s));
        }
    }

    private void printWordStats(Instances inst, HashMap<String, Word> map) {
        
        for(int j=1; j<inst.numAttributes();j++)
        {
                Word w= map.get(inst.attribute(j).name());
                System.out.println("Stats for word: "+ inst.attribute(j).name());
                
                Set<String> s=class_list.keySet();
                
                for(String k: s)
                {
                    
                    System.out.println(k+": "+w.getClassCount(k));
                }
                
                System.out.println("---------------------");
        }
    }
//</editor-fold>
    
    public static void main(String [] argv) 
    {
         runFilter(new DTW(), argv);
    }
   
}

    // <editor-fold defaultstate="collapsed" desc="Words Class">
    /*
        A Word Object typically contains 2 hashmap
            class_stats: key is the class type, value is the number of times that word appears in that class
            scores: key is the class type, value is the score that will be assigned when it is considered in that respective class
        dominantClass is the class to which this word belongs, it will be from one of the keys from the hashMap
    */
class Word
{
       
       HashMap<String,Integer> class_stats;
       HashMap<String,Double> scores;
       String dominantClass="";
       
       //Constructor
       public Word(HashMap<String,Integer> new_class_stats)
       {
           class_stats= new_class_stats;
           scores=new HashMap<String,Double>(0);
       }
       
       //Get number of instance that this word appears , for the given class name
       public Integer getClassCount(String class_name)
       {
           return class_stats.get(class_name);
       }
       
       
       //Get total number of times this word appears in all classes
       public Integer getTotalCount()
       {
           Integer c=0;
           Set<String> classes= class_stats.keySet();
           
           for(String s : classes)
           {
               c= c+ class_stats.get(s);
           }
           return c;
       }
       
       //add a certain number to the count of a given class for this word
       public  void addCount(String class_name, Integer count_to_add)
       {
           Integer new_count = class_stats.get(class_name)+count_to_add;
           class_stats.put(class_name, new_count);
       }
       
       //return the score for this word for the given class
       //if dominant class is specified, return 0 for all other classes
       //else return the respective weight for the given class
       public Double getClassWeight(String type,int option)
       {
           Double weight;
           if(option==DTW.DOMINANT_CLASS && type.contentEquals(dominantClass))
           {
               weight= scores.get(type);
           }
           else if(option==DTW.ALL_CLASSES)
           {
               weight = scores.get(type);
           }
           else
           {
               weight=0.0;
           }
           
           return weight;
       }
       
       
       //compute the weight for the given class
       public Double getWeight(int type, Double a, Double b,String classType)
       {
           double w=0.0;
           //System.out.println(a+", "+b);
           //System.out.print("Accodring to ");
           
           Double aj=Math.max(a,b);
           Double bj=Math.min(a,b);
           
           Double q1= 1-aj;
           Double q2 = 1-bj;
           
           Double q3 = 1-a;
           Double q4 = 1-b;
           
           if(a>b)
           {
               
                if(q1==0) q1 = 1.0/DTW.class_list.get(classType).doubleValue();
                if(q2==0) q2 = 1.0/(new Integer(DTW.num_of_records).doubleValue() - DTW.class_list.get(classType).doubleValue());
           }
           else
           {
               System.out.println("making a fix with a<b");
                if(q2==0) q2 = 1.0/DTW.class_list.get(classType).doubleValue();
                if(q1==0) q1 = 1.0/(new Integer(DTW.num_of_records).doubleValue() - DTW.class_list.get(classType).doubleValue());
           }
           
           System.out.println("making a fix anyways");
           if(q3==0) q3 = 1.0/DTW.class_list.get(classType).doubleValue();
           if(q4==0) q4 = 1.0/(new Integer(DTW.num_of_records).doubleValue() - DTW.class_list.get(classType).doubleValue());
           
           
           switch(type)
           {
               case (DTW.RR):
               {
                //   System.out.print("RR: ");
                    w= aj/bj;
                    break;
               }
               case (DTW.LRR):
               {
                //   System.out.print("LRR: ");
                    w= Math.log10(aj) - Math.log10(bj);
                    break;
               }
               case (DTW.OR):
               {
                //   System.out.print("OR: ");
                   
                    w= (aj/q1) /   (bj/q2);
                    break;
               }
               case (DTW.LOR):
               {
                //   System.out.print("LOR: ");
                    w= Math.log10(aj/q1) -  Math.log10(bj/q2);
                    break;
               }
               case (DTW.KL_DIVERGENCE):
               {
                   
                //   System.out.print("KL Divergence: ");
                    w= (a * Math.log10(a/b)) + (q3 * Math.log10(q3/q4)); //here we use a,b instead of aj bj because its fixedi in formula
                    break;
               }
           }
           System.out.println("Words weight was"+ w );
           return w;
                  
       }

    //calss weight calculation for each class for this word object
    void calculateWeights(int weights) 
    {
        Set<String> classes= class_stats.keySet();
        Double aj,bj,w;
        
        
        Double dominantWeight=Double.MIN_VALUE;
        
        for(String s : classes)
        {
            
            //calculate aj
            aj = (class_stats.get(s).doubleValue()+1) / (DTW.class_list.get(s).doubleValue()+1); 
            
            //calculate bj
            bj = (getTotalCount().doubleValue() - class_stats.get(s).doubleValue() + 1 )   / (new Integer(DTW.num_of_records).doubleValue() - DTW.class_list.get(s).doubleValue() + 1);
            
            //compute and store weights
            System.out.println(s + " Aj/Bj: "+ aj+" / "+bj);
            //compute
            Double weight=getWeight(weights, aj, bj,s);
            //store
            scores.put(s, weight);
            
            //update dominant class if required
            if(weight > dominantWeight)
            {
                dominantWeight=weight;
                dominantClass=s;
            }
            
           
        }
         System.out.println("Word Belongs to: "+dominantClass);
        System.out.println("-------------------");
    }
   }
  //</editor-fold>