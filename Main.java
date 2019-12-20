

import javax.xml.parsers.*;// importing required packages
import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.json.*;

class Main //Main class
  {
    public static void main(String[] args) 
      {
        try
          {
            File xml_file = new File("ontology.xml");// opening xml file to parse
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.parse(xml_file);

            Map<String, List<String>> concepttree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);//datastructure to store the concepts & parents
            List<List<String>> path = new ArrayList<>();
            List<String> leafs = new ArrayList<>();
            
            int conceptcount = 0;//initialize all variables
            float parentcount = 0;
            int counter = 0;
            int maxpathlength = 0;
            int pathlength = 0;
            
            NodeList concepts = d.getElementsByTagName("Concept");//get the concept node from xml
            for (int i = 0; i < concepts.getLength(); i++) 
              {
                Node conceptNode = concepts.item(i);
                if (conceptNode.getNodeType() == Node.ELEMENT_NODE) 
                  {
                    Element conceptname = (Element) conceptNode;
                    NodeList parents = conceptname.getElementsByTagName("DirectSuperConcepts");//get the parents of concepts from xml
                    for(int j = 0; j < parents.getLength(); j++)
                      {
                        Node parentNode = parents.item(j);
                        if (parentNode.getNodeType() == Node.ELEMENT_NODE) 
                          {
                            Element parentref = (Element) parentNode;
                            NodeList parentconcept = parentref.getElementsByTagName("ConceptReference");//get the name of the parents 
                            if(parentconcept.getLength() == 0)
                              {
                                List<String> parent = new ArrayList<>();
                                parent.add("no parent");  //set list value root if no parent for that concept
                                concepttree.put(conceptname.getAttribute("name"),parent);
                              }
                            else
                              {
                                List<String> parent = new ArrayList<>();
                                for(int k = 0; k < parentconcept.getLength(); k++)
                                  {
                                    Node parentconceptNode = parentconcept.item(k);
                                    if (parentconceptNode.getNodeType() == Node.ELEMENT_NODE) 
                                      {
                                        Element parentconceptname = (Element) parentconceptNode;
                                        parent.add(parentconceptname.getAttribute("name")); //else add the parent name to list
                                      }
                                  }
                                Collections.sort(parent);//to sort parents in ascending
                                concepttree.put(conceptname.getAttribute("name"),parent);
                              }
                          }
                      }
                  }
              }

            FileWriter filejson= new FileWriter("JSONOutput.json");//write concepts and parents to json
            for(Map.Entry<String, List<String>> entry : concepttree.entrySet()) 
              {
                String key = entry.getKey();
                if(!key.isEmpty())
                  {
                    List<String> value = entry.getValue();
                    JSONObject conceptDetails = new JSONObject();
                    conceptcount++;// calculate the number of concepts
                    conceptDetails.put("name", key);
                    conceptDetails.put("parents", value);
                    String JSONBuffer = conceptDetails.toString();
                    JSONBuffer = JSONBuffer.replace("{","{\n");
                    JSONBuffer = JSONBuffer.replace("}","\n}\n");
                    JSONBuffer = JSONBuffer.replaceFirst(",",",\n");
                    filejson.write(JSONBuffer);
                  }
              }
            filejson.close();

            PrintWriter fileTXT = new PrintWriter(new FileWriter("Statistics.txt"));//write the statistics to a txt file
            fileTXT.write("total number of concepts: "+ conceptcount);//write number of concepts
            for(Map.Entry<String,List<String>> entry : concepttree.entrySet()) //count number of parents
              {
                if(entry.getValue().get(0).equals("no parent") != true)
                  {
                    parentcount = parentcount + entry.getValue().size();
                  }
              }
            parentcount = parentcount / conceptcount;// average of parents
            fileTXT.write("\naverage number of parents: "+ parentcount);//write average parents
            for(Map.Entry<String,List<String>> entry : concepttree.entrySet()) //compute total leaf nodes
              {
                int flag = 0;
                String key = entry.getKey();
                for(Map.Entry<String, List<String>> entry1 : concepttree.entrySet()) 
                  {
                    List<String> value = entry1.getValue();
                    for (int i = 0; i < value.size(); i++) 
                      {
                        if(key.equals(value.get(i)) )
                          {
                            flag++;
                          }
                      }
                  }
                if(flag == 0)
                  {
                    counter++;
                    leafs.add(key);//add leaf nodes to a list
                  }
              }
            fileTXT.write("\nnumber of leaf concepts: "+ counter);//write leaf count
            for (int i = 0; i < leafs.size(); i++)//compute all possible paths from root to leaf
              {
                List <String> traversed = new ArrayList<>();
                traversed.add(leafs.get(i));
                int j = 0;
                while(traversed.get(j).equals("no parent") == false)
                  {
                    if(concepttree.get(traversed.get(j)).size() > 1)
                      {
                        traversed.add((concepttree.get(traversed.get(j))).get(1));
                        j++;
                      }
                    else
                      {
                        traversed.add((concepttree.get(traversed.get(j))).get(0));
                        j++;
                      }
                  }
                path.add(traversed);//add all possible path to list
              }
            for(int i = 0 ;i < path.size();i++) //compute the maximum path length from list
              {
                if(maxpathlength < path.get(i).size())
                  {
                    maxpathlength = path.get(i).size();
                  }
              }
            pathlength = maxpathlength - 1;
            fileTXT.write("\nlongest paths from a root to a counter concept: "+ pathlength);//write the maximum path value to file
            for(int i = 0; i < path.size(); i++) //write all the paths with maximun length
              {
                if(path.get(i).size() == maxpathlength)
                  {
                    fileTXT.write("\n-");
                    int j = pathlength - 1;
                    while(j > 0)
                      {
                        fileTXT.write(path.get(i).get(j));
                        fileTXT.write("-->");
                        j--;
                      }
                    fileTXT.write(path.get(i).get(j));
                  }
              }
            fileTXT.close();
          }
        catch (Exception e)
          {
            e.printStackTrace();
          }
      }
  }
