package org.vufind.config;

import java.util.*;

/**
 * Created by jbannon on 7/3/14.
 */
public class DynamicConfig {
    private Set<Class> filledForOptions = new HashSet<Class>();
    private HashMap<I_ConfigOption, Object> values = new HashMap<I_ConfigOption, Object>();

    public Object get(I_ConfigOption option) {
        return values.get(option);
    }

    public String getString(I_ConfigOption option) {
        return values.get(option).toString();
    }

    public boolean getBool(I_ConfigOption option) {
        return (boolean)values.get(option);
    }

    public Integer getInteger(I_ConfigOption option) {
        return (Integer)values.get(option);
    }

    public List getList(I_ConfigOption option) {
        return (List) values.get(option);
    }

    public Map getMap(I_ConfigOption option) {
        return (Map) values.get(option);
    }

    public void put(I_ConfigOption option, Object value) {
        values.put(option, value);
    }

    public boolean isFilledFor(Class clazz) {
        return filledForOptions.contains(clazz);
    }

    public void markFilledFor(Class clazz) {
        this.filledForOptions.add(clazz);
    }
}
