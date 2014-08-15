package org.vufind;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.vufind.config.DynamicConfig;
import org.vufind.config.I_ConfigOption;

public interface IProcessHandler {
	public void doCronProcess(DynamicConfig config) throws SQLException, IOException;
    public List<I_ConfigOption> getNeededConfigOptions();
}
