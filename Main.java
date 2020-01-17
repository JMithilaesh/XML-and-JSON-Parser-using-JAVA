/* Author : Mithilaesh Jayakumar
   GNumber : G01206238
   Program to parse the given XML file consisting of concepts and 
   compute the number of concepts , average number of parent concepts , number of leaf concepts and
   compute the path of a concept which is of maximum length from the root  
*/

/* Importing all the required java packages for the program */

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.json.*;

/* Defining a class "Operations" for performing the required parsing and computations */

class Operations
  {
    /* Declaring and Initializing the variables used for the purpose of parsing and computations*/

    private String xml_file_name;
    private String json_file_name;
    private String statistics_file_name;  
    private Map <String, List<String>> concept_tree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private List <List <String>> possible_paths = new ArrayList<>();
    private int concept_count = 0;
    private int leaf_count = 0;
    private double parent_average = 0.0;
    private int max_path_length = 0;

    /* Parameterized constructor for initializing the String values with required File names */

    Operations(String xml_file_name, String json_file_name, String statistics_file_name)
      {
        this.xml_file_name = xml_file_name;
        this.json_file_name = json_file_name;
        this.statistics_file_name = statistics_file_name;
      }

    /* Function to parse the given XML to obtain the Concepts and Parent values */

    public void xml_Parser() throws Exception
      {
        /* Opening the XML file and parsing using Java DOM Parser */

        File xml_file = new File(xml_file_name);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(xml_file);    

        /* Fetching the name from "Concept" elements and iterating within each "Concept" elements */

        NodeList concepts = d.getElementsByTagName("Concept");
        for (int i = 0; i < concepts.getLength(); i++) 
          {
            Node concept_Node = concepts.item(i);
            if (concept_Node.getNodeType() == Node.ELEMENT_NODE) 
              {
                Element concept_name = (Element) concept_Node;

                /* Fetching the "DirectSuperConcepts" elements which contain the parents within each "Concept" elements */

                NodeList parents = concept_name.getElementsByTagName("DirectSuperConcepts");
                for(int j = 0; j < parents.getLength(); j++)
                  {
                    Node parent_Node = parents.item(j);
                    if (parent_Node.getNodeType() == Node.ELEMENT_NODE) 
                      {
                        Element parent_reference = (Element) parent_Node;

                        /* Fetching the "ConceptReference" elements within each "DirectSuperConcepts" which contain the name of the parents */

                        NodeList parent_concept = parent_reference.getElementsByTagName("ConceptReference");

                        /* Adding the name of the parents to a list. Incase if the concept is a root then add the value "ROOT" to the list */

                        if(parent_concept.getLength() == 0)
                          {
                            List <String> parent = new ArrayList<>();
                            parent.add("ROOT");  
                            concept_tree.put(concept_name.getAttribute("name"),parent);
                          }
                        else
                          {
                            List <String> parent = new ArrayList<>();
                            for(int k = 0; k < parent_concept.getLength(); k++)
                              {
                                Node parent_concept_Node = parent_concept.item(k);
                                if (parent_concept_Node.getNodeType() == Node.ELEMENT_NODE) 
                                  {
                                    Element parent_concept_name = (Element) parent_concept_Node;
                                    parent.add(parent_concept_name.getAttribute("name")); 
                                  }
                              }
                            
                            /* Sort the Parent list and add it to a "concept_tree" TreeMap where concept name acts as the Key with parent list as Value */

                            Collections.sort(parent);
                            concept_tree.put(concept_name.getAttribute("name"),parent);
                          }
                      }
                  }
              }
          }
      }

    /* Function to create JSON Objects of Concepts and Parents then write it to a file */

    public void json_Parser() throws Exception
      {
        /* Opening a file to write JSON Objects */

        FileWriter file_json= new FileWriter(json_file_name);

        /* Iterate through the "concept_tree" TreeMap */

        for(Map.Entry<String, List<String>> entry : concept_tree.entrySet())  
          {
            String key = entry.getKey();
            if(!key.isEmpty())
              {
                List<String> value = entry.getValue();
                
                /* Create a JSON Object for each iteration and add the names of Concept and Parent to it */

                JSONObject concept_Details = new JSONObject();
                concept_Details.put("name", key);
                concept_Details.put("parents", value);
                
                /* Perform String Formatting and writing to the File*/

                String JSON_Buffer = concept_Details.toString();
                JSON_Buffer = JSON_Buffer.replace("{","{\n");
                JSON_Buffer = JSON_Buffer.replace("}","\n}\n");
                JSON_Buffer = JSON_Buffer.replaceFirst(",",",\n");
                file_json.write(JSON_Buffer);
              }
          }
        file_json.close();
      }
    
    /* Function to perform the Statistics computation on the data */
    public void compute_statistics()
      {
        double parent_counter = 0.0;
        
        /* Creating a temporary "child_tree" TreeMap to compute the number of leaf concepts and initializing it */

        Map <String , Integer> child_tree = new TreeMap<>();
        for(Map.Entry<String, List<String>> entry : concept_tree.entrySet())
          {
            String key = entry.getKey();
            child_tree.put(key,0);    
          }
        
        /* Looping through the "concept_tree" TreeMap */

        for(Map.Entry<String,List<String>> entry : concept_tree.entrySet()) 
          {
            /*Counting the number of concepts */

            concept_count = concept_count + 1;

            /* Counting the number of parents */
            if(entry.getValue().get(0).equals("ROOT") != true)  
              {
                parent_counter = parent_counter + entry.getValue().size();
                
                /* Create another loop to count the number of Children for each concept and store it in "child_tree" TreeMap */

                for(int i = 0;i < entry.getValue().size(); i++)
                  {
                    String parent = entry.getValue().get(i);
                    int value = child_tree.get(parent);  
                    child_tree.put(parent,(value + 1));
                  }
              }
          }

        /* Compute the average number of parents for each concept */

        parent_average = parent_counter/concept_count;
        List <String> leafs = new ArrayList<>();
        
        /* Loop through "child_tree" TreeMap and store the concepts whose child count is 0 in a list named "leafs" */

        for(Map.Entry<String,Integer> entry : child_tree.entrySet())
          {
            if(entry.getValue() == 0)
              {
                leaf_count = leaf_count + 1;
                leafs.add(entry.getKey());
              }
          }
        
        /* For each leaf concept create a loop to compute its path to root */

        for (int i = 0; i < leafs.size(); i++)
          {
            List <String> traversed = new ArrayList<>();
            traversed.add(leafs.get(i));
            int j = 0;
            
            /* Iteratively check in "concept_tree" TreeMap for the given leaf concept until it reaches root */

            while(traversed.get(j).equals("ROOT") == false)
              { 

                /* Add all the concepts which lead to root to a list "traversed" */

                if(concept_tree.get(traversed.get(j)).size() > 1)
                  {
                    traversed.add((concept_tree.get(traversed.get(j))).get(1));
                    j++;
                  }
                else
                  {
                    traversed.add((concept_tree.get(traversed.get(j))).get(0));
                    j++;
                  }
              }
            
            /* Add "traversed" list to another list "possible_paths" before re-initializing for the next leaf node */ 

            possible_paths.add(traversed);

            /* Compute the path which has the maximum length */
            
            if (max_path_length < possible_paths.get(i).size())
              {
                max_path_length = possible_paths.get(i).size();  
              }
          }
      }

    /* Function to write the computed Statistics on to a file */

    public void statistics_Parser() throws Exception
      {
        FileWriter file_statistics = new FileWriter (statistics_file_name);
        file_statistics.write("total number of concepts: "+ concept_count);
        file_statistics.write("\naverage number of parents: "+ parent_average);
        file_statistics.write("\nnumber of leaf concepts: "+ leaf_count);
        file_statistics.write("\nlongest paths from a root to a parent concept: "+ (max_path_length - 1));
        
        /* Writing the path with maximum length to the file */

        for(int i = 0; i < possible_paths.size(); i++)
          {
            if(possible_paths.get(i).size() == max_path_length)
              {
                file_statistics.write("\n-");
                int j = max_path_length - 2;
                while (j > 0)
                  {
                    file_statistics.write(possible_paths.get(i).get(j));
                    file_statistics.write("-->");
                    j--;
                  } 
                file_statistics.write(possible_paths.get(i).get(j));
              }
          }
        file_statistics.close();
      }
  }

/* Creating a class "Main" which contains the main method */

public class Main 
  {
    public static void main(String[] args) 
      {
        /* Initializing the names of Files used in the program */

        String xml_file_name = "ontology.xml";
        String json_file_name = "JSONOutput.json";
        String statistics_file_name = "Statistics.txt";
        
        /* Creating an Object and Passing the File names to the constructor of "Operations" class */

        Operations op_object = new Operations(xml_file_name, json_file_name, statistics_file_name);
        try 
          {
            /* Calling the methods of "Operations" class */

            op_object.xml_Parser();
            op_object.json_Parser();
            op_object.compute_statistics();
            op_object.statistics_Parser();
          }

        /* Catching the exceptions thrown while calling the methods of "Operations" class */
        
        catch (Exception e)
          {
            e.printStackTrace();
          }
      }
  }

