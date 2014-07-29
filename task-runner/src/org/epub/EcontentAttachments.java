package org.epub;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dcl.acs.ACSPackage;
import org.dcl.db.DBeContentRecordServices;
import org.dcl.file.FindFile;
import org.dcl.Utils.FileUtils;
import org.dcl.Utils.ISBNUtils;
import org.ini4j.BasicMultiMap;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.vufind.CronLogEntry;
import org.vufind.CronProcessLogEntry;
import org.vufind.IProcessHandler;


public class EcontentAttachments implements IProcessHandler 
{
	private DBeContentRecordServices dbEcontentRecordServices;
	private ISBNUtils isbnUtils;
	private FindFile findFile;
	private FileUtils fileUtils;
	private ACSPackage acsPackage;
	private FileUtils dclFileutils;
	
	private ArrayList<String> fileExtToAttachACS = new ArrayList<String>(Arrays.asList("epub", "pdf"));
	private ArrayList<String> fileExtImages = new ArrayList<String>(Arrays.asList("jpg", "png"));
	
	private ArrayList<ArrayList<Object>> listEpubPdf;
	private ArrayList<ArrayList<Object>> listImages;
	private String sourcePath;
	private String source;
	private String destPath;
	private String originalFolder;
	private boolean testing = false;
	
	//Log
	private int numUpdates = 0;
	private int numErrors = 0;;
	private int numCovers = 0;

	private Connection conn;
	private String vufindUrl;
	private long logEntryId = -1;
	private CronProcessLogEntry processLog;
	private Logger logger;
	
	public EcontentAttachments(
			DBeContentRecordServices dbEcontentRecordServices,
			ISBNUtils isbnUtils, FindFile findFile,
			FileUtils fileUtils, ACSPackage acsPackage)
	{//Testing Mode
		this.dbEcontentRecordServices = dbEcontentRecordServices;
		this.isbnUtils = isbnUtils;
		this.findFile = findFile;
		this.fileUtils = fileUtils;
		this.acsPackage = acsPackage;
		this.testing = true;
	}
	
	public EcontentAttachments(){}
	public void doCronProcess(String servername, 
							  Ini configIni, 
							  Section processSettings, 
							  Connection vufindConn, 
							  Connection econtentConn, 
							  CronLogEntry cronEntry, 
							  Logger logger) throws SQLException, IOException 
	{
		String vuFindUrl = configIni.get("Site", "url");
		String source = processSettings.get("source");
		String sourceDirectory = processSettings.get("sourcePath");
		String libraryDirectory = configIni.get("EContent", "library");
		String originalFolder = configIni.get("Site", "coverPath") + "/original";
		
		this.logger = logger;
		this.processLog = new CronProcessLogEntry(cronEntry.getLogEntryId(), "Attach eContent Items");
		
		this.prepareRun(econtentConn, vuFindUrl, source, sourceDirectory, libraryDirectory, originalFolder);
	}
	public void prepareRun(Connection conn, String vufindUrl,  String source, 
											String sourcePath, String destACSPath, 
											String destOriginalFolder              ) throws SQLException, IOException
	{
		this.conn = conn;
		this.vufindUrl = vufindUrl;
		
		this.dbEcontentRecordServices = new DBeContentRecordServices(this.conn);
		this.isbnUtils = new ISBNUtils();
		this.findFile = new FindFile();
		this.fileUtils = new FileUtils();
		this.acsPackage = new ACSPackage(this.vufindUrl);
		
		this.runACSAttachments(source, sourcePath, destACSPath, destOriginalFolder);
	}
	
	public void runACSAttachments(String source, String sourcePath, String destACSPath, String destOriginalFolder) throws SQLException, IOException
	{
		this.source = source;
		this.sourcePath = sourcePath;
		this.destPath = destACSPath;
		this.originalFolder = destOriginalFolder;
		this.listEpubPdf = new ArrayList<ArrayList<Object>>();
		this.listImages = new ArrayList<ArrayList<Object>>();
		
		this.creatLogEntry(); //Not in Testing Mode	
		
		ArrayList<ArrayList<String>> resultItemLess = this.dbEcontentRecordServices.getACSEcontentItemLessBySource(this.source);
		
		this.addNote(resultItemLess.size() + " eContent Record itemless");
		
		ArrayList<ArrayList<String>> resultCoverLess = this.dbEcontentRecordServices.getEcontentNoCoverBySource(this.source);
		
		this.listEpubPdf = this.getListFoundFilesOnPath(resultItemLess, this.listEpubPdf, this.fileExtToAttachACS);
		this.attachContentToACS();
		
		this.listImages = this.getListFoundFilesOnPath(resultCoverLess, this.listImages, this.fileExtImages);
		this.attachCovers();
		
		this.addNote("EPUB/PDF Attach process finished");
		this.updateLogEntry();
		this.markEntryFinished();
	}

	private ArrayList<ArrayList<Object>> getListFoundFilesOnPath(ArrayList<ArrayList<String>> result,
																 ArrayList<ArrayList<Object>> listFiles,
																 ArrayList<String> extensions) throws SQLException
	{
		if(result.size() == 0 )
		{
			return listFiles;
		}
		for (int i = 0; i<result.size(); i++)
		{
			ArrayList<String> eRecord = result.get(i);
			String isbn = this.isbnUtils.detectGetISBN(eRecord.get(3));

			if(!isbn.isEmpty())
			{
				for(int k = 0; k < extensions.size(); k++)
				{	
					String filename = isbn + "." + extensions.get(k);
					listFiles.add(this.getElement(filename, eRecord));
				}
			}
		}
		return listFiles;
	}
	
	@SuppressWarnings("unchecked")
	private void attachCovers() throws IOException, SQLException
	{
		this.addNote("<strong>COVERS</strong>");
		
		this.listImages = this.findFile.getListFilesExist(this.sourcePath,this.listImages);
		if(this.listImages.size() > 0)
		{
			for(int z=0; z < this.listImages.size(); z++)
			{
				ArrayList<Object> object = this.listImages.get(z);
				if(object.size()>2)
				{
					if(object.get(2) instanceof File)
					{
						File imageToAttach = (File) object.get(2);
						String absolutePath = imageToAttach.getAbsolutePath();
						String nameFileToAttach = imageToAttach.getName();
						ArrayList<String> record = (ArrayList<String>) object.get(1);
						
						this.fileUtils.copyFilesByPath(absolutePath, this.originalFolder + "/" + nameFileToAttach);
						this.dbEcontentRecordServices.updateEcontentCover(record.get(0), nameFileToAttach);
						this.numCovers++; //Not Testing Mode
						
						this.addNote("Attached cover : " + nameFileToAttach);
						this.updateLogEntry();
					}
				}
			}
		}
		
		this.addNote("Number of covers attached: " + this.numCovers);
	}
	
	@SuppressWarnings("unchecked")
	private void attachContentToACS() throws SQLException, IOException
	{
		dclFileutils = new FileUtils();
		this.listEpubPdf = this.findFile.getListFilesExist(this.sourcePath,this.listEpubPdf);
		
		if(this.listEpubPdf.size() > 0)
		{
			for(int z=0; z < this.listEpubPdf.size(); z++)
			{
		
				ArrayList<Object> object = this.listEpubPdf.get(z);
				if(object.size()>2)
				{
					if(object.get(2) instanceof File)
					{
						File fileToAttach = (File) object.get(2);
						String absolutePath = fileToAttach.getAbsolutePath();
						String nameFileToAttach = fileToAttach.getName();
						
						this.addNote("Attaching File " + absolutePath + " to ACS");
						ArrayList<String> record = (ArrayList<String>) object.get(1);
						
						String acsId = this.acsPackage.addFile(fileToAttach , record.get(2));
						if(acsId != null)
						{
							this.addNote("The file has been attached successfully: " + nameFileToAttach);
							this.numUpdates++; //Not Testing Mode
							
							String ext = dclFileutils.getFileExtension(nameFileToAttach);
							String fileNameEcontentItem = this.source + "_" + nameFileToAttach;
							this.dbEcontentRecordServices.insertEcontentItem(fileNameEcontentItem, acsId, record.get(0), ext);
							this.fileUtils.copyFilesByPath(absolutePath, this.destPath + "/" + fileNameEcontentItem);
						}
						else
						{
							this.numErrors++; //Not Testing Mode
							this.addNote("Cannot Attach the file " + fileToAttach + " to ACS Server");
							this.addNote("ACS Result: " + this.acsPackage.getMsgError());
						}
					}
				}
				this.updateLogEntry();
			}
		}
	}
	
	private ArrayList<Object> getElement(String name, Object object)
	{
		ArrayList<Object> element = new ArrayList<Object>();
		element.add(name);
		element.add(object);
		return element;
	}
	
	private void println(String mesg)
	{
		if(this.testing)
		{
			System.out.println(mesg);
		}
	}
	
	private void addNote(String msg)
	{
		if(!this.testing)
		{
			this.logger.info(msg);
			this.processLog.addNote(msg);
		}
	}
	
	private void creatLogEntry() throws SQLException
	{
		if(!this.testing)
		{	
			PreparedStatement createLogEntry; createLogEntry = this.conn.prepareStatement("INSERT INTO econtent_attach (sourcePath, dateStarted, status, source, numCovers) VALUES (?, ?, 'running', ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			//Add a log entry to indicate that the source folder is being processed
			this.addNote("Source Path: '" + this.sourcePath + "'");
			createLogEntry.setString(1, this.sourcePath);
			createLogEntry.setLong(2, new Date().getTime() / 1000);
			createLogEntry.setString(3, this.source);
			createLogEntry.setLong(4, 0);
			createLogEntry.executeUpdate();
			ResultSet logResult = createLogEntry.getGeneratedKeys();
			if (logResult.next())
			{
				this.logEntryId = logResult.getLong(1);
			}
		}
	}
	
	private void updateLogEntry() throws SQLException
	{
		if(!this.testing)
		{	
			PreparedStatement updateRecordsProcessed = this.conn.prepareStatement("UPDATE econtent_attach SET recordsProcessed = ?, numErrors = ?, numCovers = ? WHERE id = ?");
			updateRecordsProcessed.setLong(1, this.numUpdates);
			updateRecordsProcessed.setLong(2, this.numErrors);
			updateRecordsProcessed.setLong(3, this.numCovers);
			updateRecordsProcessed.setLong(4, this.logEntryId);
			updateRecordsProcessed.executeUpdate();
		}
	}
	
	private void markEntryFinished() throws SQLException
	{
		if(!this.testing)
		{
			PreparedStatement markLogEntryFinished = null;
			markLogEntryFinished = this.conn.prepareStatement("UPDATE econtent_attach SET dateFinished = ?, recordsProcessed = ?, numErrors =?, notes =?, status = 'finished' WHERE id = ?");
			markLogEntryFinished.setLong(1, new Date().getTime() / 1000);
			markLogEntryFinished.setLong(2, this.numUpdates);
			markLogEntryFinished.setLong(3, this.numErrors);
			markLogEntryFinished.setString(4, this.processLog.getNotesHtml());
			markLogEntryFinished.setLong(5, this.logEntryId);
			markLogEntryFinished.executeUpdate();
		}
	}
}