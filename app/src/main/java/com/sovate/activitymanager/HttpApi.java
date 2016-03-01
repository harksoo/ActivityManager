package com.sovate.activitymanager;

import android.app.Activity;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

/**
 * Created by harks on 2016-03-01.
 */
public class HttpApi {

    // Networking
    public static final String BASE_URL = "http://192.168.0.6:8080";

    // Data
    public class ActivityDevice {
        String name = "";
        String mac = "";
        String alias = "";
        String description = "";

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getMac() {
            return mac;
        }
        public void setMac(String mac) {
            this.mac = mac;
        }
        public String getAlias() {
            return alias;
        }
        public void setAlias(String alias) {
            this.alias = alias;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
    }


    // Interface
    public interface ActivityService {

        @GET("/healthcare/activity/devices")
        Call<List<ActivityDevice>> getDevices();
    }


    static MainActivity main;
    // function
    public static void setMainActivity(MainActivity main)
    {
        HttpApi.main = main;
    }


    public static void getDevicesRequest() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ActivityService activityService = retrofit.create(ActivityService.class);

        Call<List<ActivityDevice>> call = activityService.getDevices();

        call.enqueue(new Callback<List<ActivityDevice>>() {
            @Override
            public void onResponse(Call<List<ActivityDevice>> call, Response<List<ActivityDevice>> response) {

                System.out.println("Response status code: " + response.code());

                // isSuccess is true if response code => 200 and <= 300
                if (!response.isSuccess()) {
                    // print response body if unsuccessful
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        // do nothing
                    }
                    return;
                }

                HttpApi.main.setListActivityDevice(response.body());

            }

            @Override
            public void onFailure(Call<List<ActivityDevice>> call, Throwable t) {
                System.out.println("onFailure");
                System.out.println(t.getMessage());
            }
        });


    }
}
