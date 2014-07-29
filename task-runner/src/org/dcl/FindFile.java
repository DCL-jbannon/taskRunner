package org.dcl;

import java.io.File;
import java.util.ArrayList;

public class FindFile
{
	public ArrayList<File> getListFilesExist(String path,
									ArrayList<String> filenamesToSearch
								 )
	{
		ArrayList<File> result = new ArrayList<File>();
		String fileToCompare = "";
		
		File file = new File(path);
		File[] files = file.listFiles();
		if(files == null)
		{
			return result;
		}
		for (int i = 0; i < files.length; i++)
		{
           if(!files[i].isDirectory())
           {
        	   String filename = files[i].getName();
        	   for(int j=0; j<filenamesToSearch.size();j++)
        	   {
        		   if(filename.equals(filenamesToSearch.get(j)))
        		   {
        			   result.add(files[i]);
        		   }
        	   }
           }
           else
           {
        	  ArrayList<File> ret = this.getListFilesExist(files[i].getAbsolutePath(), filenamesToSearch);
        	  if(ret.size() >0)
        	  {
        		  //http://stackoverflow.com/questions/8625247/merge-3-arraylist-to-one
        		  result.addAll(ret);
        	  }
           }
        }
		return result;
	}
}