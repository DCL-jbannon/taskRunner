package org.vufind;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.DynamicConfig;
import org.vufind.config.I_ConfigOption;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.BookCoverCleanupOptions;
import org.vufind.config.sections.ReindexListsOptions;

public class BookcoverCleanup implements IProcessHandler {
    private Logger logger = LoggerFactory.getLogger(BookcoverCleanup.class);

	public void doCronProcess(DynamicConfig config) {

        CronLogEntry cronEntry = CronLogEntry.getCronLogEntry();
        Connection vufindConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.PRINT);

		CronProcessLogEntry processLog = new CronProcessLogEntry(cronEntry.getLogEntryId(), "Bookcover Cleanup");
		processLog.saveToDatabase(vufindConn, logger);

		String coverPath = config.getString(BookCoverCleanupOptions.COVER_PATH);
		String[] coverPaths = new String[] { "/small", "/medium", "/large" };
		Long currentTime = new Date().getTime();

		for (String path : coverPaths) {
			int numFilesDeleted = 0;

			String fullPath = coverPath + path;
			File coverDirectoryFile = new File(fullPath);
			if (!coverDirectoryFile.exists()) {
				processLog.incErrors();
				processLog.addNote("Directory " + coverDirectoryFile.getAbsolutePath() + " does not exist.  Please check configuration file.");
				processLog.saveToDatabase(vufindConn, logger);
			} else {
				processLog.addNote("Cleaning up covers in " + coverDirectoryFile.getAbsolutePath());
				processLog.saveToDatabase(vufindConn, logger);
				File[] filesToCheck = coverDirectoryFile.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith("jpg") || name.toLowerCase().endsWith("png");
					}
				});
                //TODO this should use DirectoryStream from NIO since we are dealing with so many files
                int daysTillCoversExpire = config.getInteger(BookCoverCleanupOptions.DAYS_TILL_COVERS_EXPIRE);
                DateTime now = new DateTime();
                DateTime cuttoffDate = now.minusDays(daysTillCoversExpire);
				for (File curFile : filesToCheck) {
					if (cuttoffDate.isAfter(curFile.lastModified())) {
						if (curFile.delete()){
							numFilesDeleted++;
							processLog.incUpdated();
						}else{
							processLog.incErrors();
							processLog.addNote("Unable to delete file " + curFile.toString());
						}
					}
				}
				if (numFilesDeleted > 0) {
					processLog.addNote("\tRemoved " + numFilesDeleted + " files from " + fullPath + ".");
				}
			}
		}
		processLog.setFinished();
		processLog.saveToDatabase(vufindConn, logger);
	}

    @Override
    public List<I_ConfigOption> getNeededConfigOptions() {
        return Arrays.asList(new I_ConfigOption[]{BasicConfigOptions.values()[0], BookCoverCleanupOptions.values()[0]});
    }
}
