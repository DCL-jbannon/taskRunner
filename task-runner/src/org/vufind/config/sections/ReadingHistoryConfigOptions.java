package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum ReadingHistoryConfigOptions implements I_ConfigOption {
    APP_ID(ConfigMethods::fillSimpleString, false),
    LOAD_PRINT(ConfigMethods::fillBool, false),
    LOAD_ECONTENT(ConfigMethods::fillBool, false),
    LOAD_OVERDRIVE(ConfigMethods::fillBool, false),
    SHOULD_NOTIFY_STRANDS(ConfigMethods::fillBool, false),

    ;

    final private Function fillFunction;
    final private boolean isList;

    ReadingHistoryConfigOptions(Function fillFunction, boolean isList) {
        this.isList = isList;
        this.fillFunction = fillFunction;
    }

    public Function getFillFunction() {
        return fillFunction;
    }

    @Override
    public boolean isList() {
        return this.isList;
    }
}
