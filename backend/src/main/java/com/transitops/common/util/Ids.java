package com.transitops.common.util;

import java.util.UUID;

public final class Ids {

    private Ids() {}

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}