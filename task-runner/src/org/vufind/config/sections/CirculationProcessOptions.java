package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum CirculationProcessOptions implements I_ConfigOption {
    SHOULD_EMAIL_NOTICES(ConfigMethods::fillBool, false),
    EMAIL_NOTICE_FROM_ADDRESS(ConfigMethods::fillSimpleString, false),
    EMAIL_HOST(ConfigMethods::fillSimpleString, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    CirculationProcessOptions(Function fillFunction, boolean isList) {
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
