package com.lihuahua.hyperspace.utils;

import java.util.UUID;

public class IdUtil {
    
    public static String generateUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}