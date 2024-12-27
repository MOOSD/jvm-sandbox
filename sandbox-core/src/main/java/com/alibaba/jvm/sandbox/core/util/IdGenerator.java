package com.alibaba.jvm.sandbox.core.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Base64;

public class IdGenerator {
    public static String genId(String ... keywords){
        String idString = String.join(":", keywords);
        byte[] md5 = DigestUtils.md5(idString);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md5);
    }
}
