package com.eve.app.ui.home;

import com.eve.app.data.remote.ApiClient;
import com.eve.app.data.remote.EveApiService;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Offline transport boundary: no Firebase token or real Worker request. */
public final class AttemptHandoffBoundaryFixture {
    public static boolean holdLock;
    public static kotlin.coroutines.Continuation<Object> pendingLock;
    public static void reset() { holdLock=false; pendingLock=null; }
    @Implements(value=ApiClient.class,isInAndroidSdk=false)
    public static class OfflineApi {
        private static final EveApiService API = (EveApiService) java.lang.reflect.Proxy.newProxyInstance(
            EveApiService.class.getClassLoader(), new Class<?>[]{EveApiService.class},
            (proxy, method, args) -> {
                if (holdLock && method.getName().equals("checkAttemptLock")) {
                    pendingLock=(kotlin.coroutines.Continuation<Object>)args[args.length-1];
                    return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
                }
                throw new IllegalStateException("Synthetic offline transport");
            });
        @Implementation public EveApiService getApi() { return API; }
        @Implementation public EveApiService getApiService() { return API; }
    }
}
