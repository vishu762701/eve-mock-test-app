package com.eve.app.ui.admin;

import com.eve.app.data.remote.ApiClient;
import com.eve.app.data.remote.EveApiService;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** One shadow type for the Kotlin singleton across privileged Activity test suites. */
public final class AdminApiTransportFixture {
    public static EveApiService current;
    @Implements(value=ApiClient.class,isInAndroidSdk=false)
    public static class Transport {
        @Implementation public EveApiService getApi() {
            if(current==null) throw new AssertionError("No isolated admin transport installed");
            return current;
        }
    }
}
