package com.tr.checkpacking.utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import groovy.json.JsonOutput;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DingDingUtils {
    // App群聊
    private static String DING_TALK_ACCESS_TOKEN = "********************************************";
    // 不可修改，"打包检查结果："为创建的机器人关键字，两边不统一则无法发送
    private static String DING_TALK_KEYWORD = "打包检查结果：";

    // 测试群聊
    private static final String DEBUG_DING_TALK_ACCESS_TOKEN = "********************************************";

    private static final String ENV_DING_TALK_ACCESS_TOKEN = "CHECKPACKING_DINGTALK_ACCESSTOKEN";
    private static final String ENV_DING_TALK_KEYWORD = "CHECKPACKING_DINGTALK_KEYWORD";

    private static final String DING_TALK_URL = "https://oapi.dingtalk.com/robot/send?access_token=%s";

    static {
        String token = SystemUtils.getenv(ENV_DING_TALK_ACCESS_TOKEN);
        if (StringUtils.isNotEmpty(token)) {
            DING_TALK_ACCESS_TOKEN = token;
        }
        String keyword = SystemUtils.getenv(ENV_DING_TALK_KEYWORD);
        if (StringUtils.isNotEmpty(keyword)) {
            DING_TALK_KEYWORD = keyword;
        }
    }

    private static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS).build();
    }

    public static void send2DingDing(String msg) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("msgtype", "markdown");
        DDContentModel contentModel = new DDContentModel();
        contentModel.setTitle(DING_TALK_KEYWORD);// 不可修改，"打包检查结果："为创建的机器人关键字，两边不统一则无法发送
        contentModel.setText(msg);
        map.put("markdown", contentModel);
        String json = JsonOutput.toJson(map);
        LoggerUtils.info("send to Dingding request json：" + json);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), json);
        Request request = new Request.Builder()
                .addHeader("Connection", "Keep-Alive")
                .addHeader("Charset", "UTF-8")
                .url(String.format(DING_TALK_URL, DING_TALK_ACCESS_TOKEN))
                .post(requestBody)
                .build();
        try {
            Response response = getOkHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String result = response.body().string();
                LoggerUtils.info("send to Dingding result：" + result);
            } else {
                LoggerUtils.warn("send to Dingding failure");
            }
        } catch (Exception e) {
            LoggerUtils.warn("send to Dingding failure " + e);
        }
    }

    private static class DDContentModel {
        private String title;
        private String text;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}