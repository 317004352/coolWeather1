package com.coolweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

//访问服务器工具类
public class HttpUtil {
    public static void sendOkHttpClient(String address ,okhttp3.Callback callback){
     OkHttpClient client =new OkHttpClient();
        Request request =new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
