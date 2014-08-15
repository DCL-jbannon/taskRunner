package org.vufind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.vufind.config.DynamicConfig;
import org.vufind.config.I_ConfigOption;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.ReindexListsOptions;

public class ReindexLists implements IProcessHandler {
	private Logger logger;
	private CronProcessLogEntry processLog;
	private String vufindUrl;
	private boolean reindexBiblio;
	private boolean reindexBiblio2;
	private String baseSolrUrl;
	
	@Override
	public void doCronProcess(DynamicConfig config) {
        CronLogEntry cronEntry = CronLogEntry.getCronLogEntry();
        Connection vufindConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.PRINT);

		processLog = new CronProcessLogEntry(cronEntry.getLogEntryId(), "Reindex Lists");
		processLog.saveToDatabase(vufindConn, logger);
		try {
			this.logger = logger;

			vufindUrl = config.getString(BasicConfigOptions.VUFIND_URL);

            reindexBiblio = config.getBool(ReindexListsOptions.REINDEX_BIBLIO);
            reindexBiblio2 = config.getBool(ReindexListsOptions.REINDEX_BIBLIO2);

			baseSolrUrl = config.getString(BasicConfigOptions.BASE_SOLR_URL);
			if (baseSolrUrl == null){
				processLog.incErrors();
				processLog.addNote("baseSolrUrl not found in configuration options, please specify as part of process settings");
				return;
			}

			
			//Get a list of all public lists
			PreparedStatement getPublicListsStmt = vufindConn.prepareStatement(
                    "SELECT user_list.id, count(user_resource.id) as num_titles " +
                    "FROM user_list INNER JOIN user_resource on list_id = user_list.id " +
                    "WHERE public = 1 " +
                    "GROUP BY user_list.id");
			ResultSet publicListsRs = getPublicListsStmt.executeQuery();
			while (publicListsRs.next()){
				Long listId = publicListsRs.getLong("id");
				//Reindex each list
				if (reindexBiblio){
					reindexList("biblio", listId);
				}
				if (reindexBiblio2){
					reindexList("biblio2", listId);
				}
			}
			publicListsRs.close();
			getPublicListsStmt.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			processLog.setFinished();
			processLog.saveToDatabase(vufindConn, logger);
		}
	}

    @Override
    public List<I_ConfigOption> getNeededConfigOptions() {
        return Arrays.asList(new I_ConfigOption[] {BasicConfigOptions.PROCESSES, ReindexListsOptions.REINDEX_BIBLIO});
    }

    private void reindexList(String string, Long listId) {
		URLPostResponse response = Util.getURL(vufindUrl + "/MyResearch/MyList/" + listId + "?myListActionHead=reindex", logger);
		if (!response.isSuccess()){
			processLog.addNote("Error reindexing list " + response.getMessage());
			processLog.incErrors();
		}else{
			processLog.incUpdated();
		}
	}

	private void clearLists(String coreName) {
		URLPostResponse response = Util.postToURL(baseSolrUrl + "/solr/" + coreName + "/update/?commit=true", "<delete><query>recordtype:list</query></delete>", logger);
		if (!response.isSuccess()){
			processLog.addNote("Error clearing existing marc records " + response.getMessage());
			processLog.incErrors();
		}
	}

}
