package org.vufind;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.I_ConfigOption;
import org.vufind.config.sections.BasicConfigOptions;

public class Cron {

	private static Logger logger = LoggerFactory.getLogger(Cron.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*if (args.length == 0){
			System.out.println("The name of the server to run cron for must be provided as the first parameter.");
			System.exit(1);
		}
		serverName = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);*/

        File configFolder = null;
        if (args.length < 1) {
            System.out.println("Warning, did not set a config folder. Assuming current working directory");
            configFolder = new File(".");
        } else {
            configFolder = new File(args[0]);
        }

        DynamicConfig config = new DynamicConfig();
        fillNeededConfig(config, Arrays.asList(new I_ConfigOption[]{BasicConfigOptions.BASE_SOLR_URL}), configFolder);
        Map<String, String> argumentMap = new HashMap();
        for(String arg: args) {
            String[] argsSplit = arg.split("=");
            if(argsSplit.length==2) {
                argumentMap.put(argsSplit[0], argsSplit[1]);
            }
        }
        config.put(BasicConfigOptions.CMD_ARGUMENTS, argumentMap);

        Date currentTime = new Date();

        logger.info(currentTime.toString() + ": Starting Cron");

        Connection vufindConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.PRINT);
        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

        //Create a log entry for the cron process
        CronLogEntry cronEntry = CronLogEntry.getCronLogEntry();
        if (!cronEntry.saveToDatabase(vufindConn, logger)) {
            logger.error("Could not save log entry to database, quitting");
            return;
        }

        //TODO decide if the tasks are scheduled to be run yet

        List<IProcessHandler> processHandlers = loadProcessHandlers(config);

        for (IProcessHandler processHandler : processHandlers) {
            currentTime = new Date();
            logger.info(currentTime.toString() + ": Running Process " + processHandler.getClass().getCanonicalName());
            cronEntry.addNote("Starting cron process " + processHandler.getClass().getCanonicalName());

            try {
                processHandler.doCronProcess(config);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Log how long the process took
            Date endTime = new Date();
            long elapsedMillis = endTime.getTime() - currentTime.getTime();
            float elapsedMinutes = (elapsedMillis) / 60000;
            logger.info("Finished process " +  processHandler.getClass().getCanonicalName() + " in " + elapsedMinutes + " minutes (" + elapsedMillis + " milliseconds)");
            cronEntry.addNote("Finished process " +  processHandler.getClass().getCanonicalName() + " in " + elapsedMinutes + " minutes (" + elapsedMillis + " milliseconds)");
            //TODO Update that the process was run.
            currentTime = new Date();

        }
		
		cronEntry.setFinished();
		cronEntry.addNote("Cron run finished");
		cronEntry.saveToDatabase(vufindConn, logger);
	}

    private static void fillNeededConfig(DynamicConfig config, List<I_ConfigOption> options, File configFolder) {
        for(I_ConfigOption option: options) {
            ConfigFiller.fill(
                    config,
                    Arrays.asList((I_ConfigOption[])((Enum)option).getDeclaringClass().getEnumConstants()),
                    configFolder);
        }
        config.put(BasicConfigOptions.CONFIG_FOLDER, configFolder);
    }

    //TODO incorporate this
	private static ArrayList<ProcessToRun> loadProcessesToRun(Ini cronIni, Section processes) {
		ArrayList<ProcessToRun> processesToRun = new ArrayList<ProcessToRun>();
		Date currentTime = new Date();
		for (String processName : processes.keySet()) {
			String processHandler = cronIni.get("Processes", processName);
			// Each process has its own configuration section which can include:
			// - time last run
			// - interval to run the process
			// - additional configuration information for the process
			// Check to see when the process was last run
			String lastRun = cronIni.get(processName, "lastRun");
			boolean runProcess = false;
			String frequencyHours = cronIni.get(processName, "frequencyHours");
			if (frequencyHours == null || frequencyHours.length() == 0){
				//If the frequency isn't set, automatically run the process 
				runProcess = true;
			}else if (frequencyHours.trim().compareTo("-1") == 0) {
				// Process has to be run manually
				runProcess = false;
				logger.info("Skipping Process " + processName + " because it must be run manually.");
			}else{
				//Frequency is a number of hours.  See if we should run based on the last run. 
				if (lastRun == null || lastRun.length() == 0) {
					runProcess = true;
				} else {
					// Check the interval to see if the process should be run
					try {
						long lastRunTime = Long.parseLong(lastRun);
						if (frequencyHours.trim().compareTo("0") == 0) {
							// There should not be a delay between cron runs
							runProcess = true;
						} else {
							int frequencyHoursInt = Integer.parseInt(frequencyHours);
							if ((double) (currentTime.getTime() - lastRunTime) / (double) (1000 * 60 * 60) >= frequencyHoursInt) {
								// The elapsed time is greater than the frequency to run
								runProcess = true;
							}else{
								logger.info("Skipping Process " + processName + " because it has already run in the specified interval.");
							}
	
						}
					} catch (NumberFormatException e) {
						logger.warn("Warning: the lastRun setting for " + processName + " was invalid. " + e.toString());
					}
				}
			}
			if (runProcess) {
				logger.info("Running process " + processName);
				processesToRun.add(new ProcessToRun(processName, processHandler));
			}
		}
		return processesToRun;
	}

    static private List<IProcessHandler> loadProcessHandlers(DynamicConfig config) {
        List<Class> processorClasses = (List<Class>)config.get(BasicConfigOptions.PROCESSES);
        List<IProcessHandler> processors = new ArrayList();

        for(Class processorClass: processorClasses) {
            Object instance = null;
            try {
                instance = processorClass.newInstance();
                if(instance instanceof IProcessHandler) {
                    IProcessHandler processor = (IProcessHandler)instance;
                    List<I_ConfigOption> neededConfigs = processor.getNeededConfigOptions();
                    fillNeededConfig(config, neededConfigs, new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));

                    processors.add(processor);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return processors;
    }
}
