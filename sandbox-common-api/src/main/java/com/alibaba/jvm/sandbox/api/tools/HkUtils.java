package com.alibaba.jvm.sandbox.api.tools;

public class HkUtils {

    public static String getUrl(String host, String port, String uri){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("http://").append(host).append(':').append(port);

        if(!uri.startsWith("/")){
            stringBuilder.append('/');
        }
        stringBuilder.append(uri);
        return stringBuilder.toString();
    }


}
