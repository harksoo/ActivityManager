package com.sovate.activitymanager;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by harks on 2016-03-01.
 */
public class HttpClient {

    private static final String TAG = "UartService";
    private static final String HostName = "http://192.168.0.6:8080/healthcare";

    // device의 정보를 얻는 API
    // 개별 request의 정보 확인 요망.
    public static JSONArray getDevice() throws Exception {

        // TODO 데이터의 형식이 올바른지 확인 코드 필요.
        URL url = new URL(HostName + "/activity/devices");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String response;
        JSONArray responseJSON = null;


        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            // stream의 읽기의 처리
            //readStream(in);

            byte[] byteBuffer = new byte[1024];
            byte[] byteData;
            int nLength = 0;
            while((nLength = in.read(byteBuffer, 0, byteBuffer.length)) != -1) {
                baos.write(byteBuffer, 0, nLength);
            }
            byteData = baos.toByteArray();

            response = new String(byteData);

            responseJSON = new JSONArray(response);
//            Boolean result = (Boolean) responseJSON.get("result");
//            String age = (String) responseJSON.get("age");
//            String job = (String) responseJSON.get("job");

            Log.i(TAG, "DATA response = " + response);


        }
        catch (Exception e) {
            Log.i(TAG, "Exception " + e.getMessage());
        }
        finally {
            urlConnection.disconnect();
        }

        return  responseJSON;
    }

}
