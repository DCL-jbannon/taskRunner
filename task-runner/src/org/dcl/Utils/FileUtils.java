package org.dcl.Utils;

import java.io.File;
import java.io.IOException;

public class FileUtils 
{

	public String getFileExtension(String filename)
	{
		int pos = filename.lastIndexOf('.');
		if(pos == -1)
		{
			return "";
		}
		String ext = filename.substring(pos+1);
		return ext;
	}

	public boolean copyFilesByPath(String sourceNameFile, String destNameFile) throws IOException 
	{
		File srcFile = new File(sourceNameFile);
		File destFile = new File(destNameFile);
		if(!srcFile.exists())
		{
			throw new IOException("Source File does not exists or cannot be read it");
		}
		
		if(!destFile.exists())
		{
			if(!destFile.createNewFile())
			{
				throw new IOException("Dest File cannot be creates");
			}
		}
		org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
		return true;
	}
}