package org.epub;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.vufind.CronLogEntry;
import org.vufind.CronProcessLogEntry;
import org.vufind.IProcessHandler;
import org.vufind.Util;

public class Reindex implements IProcessHandler {
	private String vufindUrl;
	private Integer numReindexingThreadsRunning;
	
	private class EContentReindexThread extends Thread
	{
		private String recordId;
		private Logger loggerThread;
		private CronLogEntry cronEntryThread;
		private Connection vufindConnThread;
		private long startTime;
		
		public EContentReindexThread(String econtentRecordId, Logger logger, CronLogEntry cronEntry, Connection vufindConn)
		{
			recordId = econtentRecordId;
			loggerThread = logger;
			vufindConnThread = vufindConn;
			cronEntryThread = cronEntry;
			startTime = new Date().getTime();
		}
		
		@Override
		public void run() {
			
			CronProcessLogEntry processLog = new CronProcessLogEntry(cronEntryThread.getLogEntryId(), "Reindex eContent");
			processLog.saveToDatabase(vufindConnThread, loggerThread);
			//loggerThread.info("Starting Index of the record: " + recordId);
			try {
				URL updateIndexURL = new URL(vufindUrl + "/EcontentRecord/" + recordId + "/Reindex?quick=true");
				Object updateIndexDataRaw = updateIndexURL.getContent();
				if (updateIndexDataRaw instanceof InputStream) {
					String updateIndexResponse = Util.convertStreamToString((InputStream) updateIndexDataRaw);
					long endTime = new Date().getTime();
					loggerThread.info("Indexing record " + recordId + " elapsed " + (endTime - startTime) + " response: " + updateIndexResponse);
					processLog.incUpdated();
					numReindexingThreadsRunning--;
				}
			} catch (Exception e) {
				loggerThread.info("Unable to reindex record " + recordId + " " + e.toString(), e);
			}
		}
		
	}
	
	
	@Override
	public void doCronProcess(String servername, Ini configIni, Section processSettings, Connection vufindConn, Connection econtentConn, CronLogEntry cronEntry, Logger logger) {
		
		logger.info("Reindexing eContent");
		CronProcessLogEntry processLog = new CronProcessLogEntry(cronEntry.getLogEntryId(), "Reindex eContent");
		//Load configuration
		if (!loadConfig(configIni, processSettings, logger)){
			return;
		}
		
		try {
			//TODO: Drop existing records from Solr index.
			
			//Reindexing all records
			PreparedStatement eContentRecordStmt = econtentConn.prepareStatement("SELECT id from econtent_record where status ='active' ");
			ResultSet eContentRecordRS = eContentRecordStmt.executeQuery();
			numReindexingThreadsRunning = 0;
			while (eContentRecordRS.next()){
				String econtentRecordId = eContentRecordRS.getString("id");
				Boolean msg = true;
				while (numReindexingThreadsRunning > 5)
				{
					if(msg)
					{
						logger.info("The Thread's Queue is full..... waiting, " + numReindexingThreadsRunning + " remain open");
					}
					msg = false;
					try {
						Thread.yield();
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.error("The thread was interrupted");
						break;
					}
				}
				msg = true;
				Thread EContentReindexThread = new EContentReindexThread(econtentRecordId, logger, cronEntry, vufindConn);
				EContentReindexThread.start();
				numReindexingThreadsRunning++;
			}
			processLog.setFinished();
			processLog.saveToDatabase(vufindConn, logger);
		} catch (SQLException ex) {
			// handle any errors
			logger.error("Error establishing connection to database ", ex);
			return;
		}

	}

	private boolean loadConfig(Ini configIni, Section processSettings, Logger logger) {
		vufindUrl = configIni.get("Site", "url");
		if (vufindUrl == null || vufindUrl.length() == 0) {
			logger.error("Unable to get URL for VuFind in General settings.  Please add a vufindUrl key.");
			return false;
		}
		
		return true;
	}
}
