package org.dcl.acs;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.epub.ImportResult;
import org.json.JSONObject;
import org.vufind.Util;

public class ACSPackage
{
	private String vufindUrl;
	private String msgError;
	
	public ACSPackage(String url)
	{
		this.vufindUrl = url;
	}
	
	public String addFile(File sourceFile, String availableCopies)
	{
		//Call an API on vufind to make this easier and promote code reuse
		try 
		{
			URL apiUrl = new URL(vufindUrl + "/API/ItemAPI?method=addFileToAcsServer&availableCopies="+availableCopies+"&absPath=1&filename=" + URLEncoder.encode(sourceFile.getAbsolutePath(), "utf8"));			
			String responseJson = Util.convertStreamToString((InputStream)apiUrl.getContent());
			JSONObject responseData = new JSONObject(responseJson);
			JSONObject resultObject = responseData.getJSONObject("result");
			
			if (!resultObject.has("success"))
			{
				this.msgError = resultObject.getString("error");
				return null;
			}
			if(resultObject.getString("success") == "false")
			{
				this.msgError = resultObject.toString();
				return null;
			}
			if (resultObject.has("acsId"))
			{
				return resultObject.getString("acsId");
			}
			else
			{
				this.msgError = resultObject.toString();
				return null;
			}
		} 
		catch (Exception e) 
		{
			this.msgError = e.getMessage();
			return null;
		}
	}
	
	public String getMsgError()
	{
		return this.msgError;
	}
}