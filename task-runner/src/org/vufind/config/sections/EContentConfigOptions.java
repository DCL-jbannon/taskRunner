package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum EContentConfigOptions implements I_ConfigOption {
    LIBRARY(ConfigMethods::fillSimpleString, false),
    ORIGINAL_COVER_FOLDER(ConfigMethods::fillSimpleString, false),
    ROOT_FTP_DIR(ConfigMethods::fillSimpleString, false),
    DISTRIBUTOR_ID(ConfigMethods::fillSimpleString, false),
    PACKAGING_URL(ConfigMethods::fillSimpleString, false),
    PACKAGING_FTP(ConfigMethods::fillSimpleString, false),
    ACTIVE_PACKAGING_SOURCE(ConfigMethods::fillSimpleString, false),
    ALL_PACKAGING_SOURCES(ConfigMethods::fillSimpleString, true),
    ;

    final private Function fillFunction;
    final private boolean isList;

    EContentConfigOptions(Function fillFunction, boolean isList) {
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
