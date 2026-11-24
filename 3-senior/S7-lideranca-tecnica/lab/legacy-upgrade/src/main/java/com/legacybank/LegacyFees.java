package com.legacybank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LegacyFees {

    public static List<Integer> tiers() {
        List<Integer> tiers = new ArrayList<Integer>();
        tiers.add(new Integer(100));
        tiers.add(new Integer(500));
        return Collections.unmodifiableList(tiers);
    }

    public static String describe(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            return "text:" + s.trim();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("other:").append(value);
        return sb.toString();
    }
}
