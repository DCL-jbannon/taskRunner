package org.dcl.file;

import java.io.File;
import java.util.ArrayList;

public class FindFile
{
	
	/**
	 * INPUT:
	 * 	filenamesToSearch[0] String fileNameToSearch
	 * 	filenamesToSearch[1] Object Whatever
	 * @param path
	 * @param filenamesToSearch
	 * @return ArrayList<ArrayList<Object>>
	 * 
	 * RETURN
	 * 
	 * 	filenamesToSearch[0] String fileNameToSearch
	 * 	filenamesToSearch[1] Object Whatever
	 *  filenamesToSearch[3] File|null
	 */
	
	public ArrayList<ArrayList<Object>> getListFilesExist(String path,
														  ArrayList<ArrayList<Object>> filenamesToSearch
								 )
	{
		File file = new File(path);
		File[] files = file.listFiles();
		if(files == null)
		{
			return filenamesToSearch;
		}
		
		for (int i = 0; i < files.length; i++)
		{
			
	       if(!files[i].isDirectory())
	       {
	    	   String filename = files[i].getName();
	    	   
	    	   for(int j=0; j<filenamesToSearch.size();j++)
	    	   {
	    		   
	    		   if(filename.equalsIgnoreCase((String) filenamesToSearch.get(j).get(0)))
	    		   {
	    			   
	    			   if(this.hasThreeValues(j, filenamesToSearch ))
	    			   {
	    				   //Could be null
	    				   filenamesToSearch= this.removeThirdValue(j, filenamesToSearch );
	    			   }
	    			   filenamesToSearch.get(j).add(files[i]);
	    		   }
	    		   else
	    		   {
	    			   if(!this.hasThreeValues(j, filenamesToSearch )) //Preserve the value. could be null or File object
	    			   {
	    				   filenamesToSearch.get(j).add(null);
	    			   }
	    		   }
	    	   }
	       }
	       else
	       {
	    	   filenamesToSearch = this.getListFilesExist(files[i].getAbsolutePath(), filenamesToSearch);
	       }
	    }
		return filenamesToSearch;
	}
	
	private boolean hasThreeValues(int index, ArrayList<ArrayList<Object>> filenamesToSearch)
	{
		if(filenamesToSearch.get(index).size()>2)
	    {
		   return true;
	    }
		return false;
	}
	
	private ArrayList<ArrayList<Object>> removeThirdValue(int index, ArrayList<ArrayList<Object>> filenamesToSearch)
	{
		filenamesToSearch.get(index).remove(2);
		return filenamesToSearch;
	}
}