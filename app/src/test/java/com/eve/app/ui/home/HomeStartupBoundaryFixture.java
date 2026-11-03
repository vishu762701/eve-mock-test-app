package com.eve.app.ui.home;

import android.content.Context;
import com.eve.app.EveApplication;
import com.eve.app.data.remote.ApiClient;
import com.eve.app.data.remote.ApiResponse;
import com.eve.app.data.remote.EveApiService;
import com.eve.app.data.model.AppConfig;
import com.eve.app.util.ReminderScheduler;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/** Only application startup services, push SDK and HTTP boundaries; real Home code runs. */
public final class HomeStartupBoundaryFixture {
    @Implements(value=EveApplication.class,isInAndroidSdk=false)
    public static class ApplicationBoundary extends org.robolectric.shadows.ShadowApplication {
        @RealObject EveApplication application;
        @Implementation public void onCreate() throws Exception {
            java.lang.reflect.Field field=EveApplication.class.getDeclaredField("instance");
            field.setAccessible(true);field.set(null,application);
        }
    }
    @Implements(value=ApiClient.class,isInAndroidSdk=false)
    public static class ApiBoundary {
        private static final EveApiService API=(EveApiService)java.lang.reflect.Proxy.newProxyInstance(
            EveApiService.class.getClassLoader(),new Class<?>[]{EveApiService.class},(proxy,method,args)->{
                if(method.getName().equals("getAppConfig"))return new Gson().fromJson(
                    "{\"success\":true,\"data\":{\"minimum_supported_version_code\":0,\"maintenance_mode\":false,\"maintenance_message\":\"Synthetic\",\"default_publish_mode\":\"paused\"}}",
                    TypeToken.getParameterized(ApiResponse.class,AppConfig.class).getType());
                if(method.getName().equals("getExams"))return new Gson().fromJson(
                    "{\"success\":true,\"data\":[{\"id\":\"exam\",\"examName\":\"Synthetic exam\"}]}",
                    TypeToken.getParameterized(ApiResponse.class,TypeToken.getParameterized(java.util.List.class,com.eve.app.data.model.Exam.class).getType()).getType());
                throw new IllegalStateException("Synthetic offline transport: "+method.getName());
            });
        @Implementation public EveApiService getApi(){return API;}
        @Implementation public EveApiService getApiService(){return API;}
    }
    @Implements(FirebaseMessaging.class)
    public static class PushBoundary {
        private static final FirebaseMessaging MESSAGING=com.google.firebase.messaging.HomeTestMessaging.create();
        @Implementation public static FirebaseMessaging getInstance(){return MESSAGING;}
    }
    @Implements(value=ReminderScheduler.class,isInAndroidSdk=false)
    public static class SchedulerBoundary {
        @Implementation public void applySavedState(Context context){}
    }
}
