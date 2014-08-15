package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum ReindexListsOptions implements I_ConfigOption {
    REINDEX_BIBLIO(ConfigMethods::fillBool, false),
    REINDEX_BIBLIO2(ConfigMethods::fillBool, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    ReindexListsOptions(Function fillFunction, boolean isList) {
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
