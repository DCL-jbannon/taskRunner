package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum BookCoverCleanupOptions implements I_ConfigOption {
    COVER_PATH(ConfigMethods::fillSimpleString, false),
    DAYS_TILL_COVERS_EXPIRE(ConfigMethods::fillInteger, false);
    ;

    final private Function fillFunction;
    final private boolean isList;

    BookCoverCleanupOptions(Function fillFunction, boolean isList) {
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
