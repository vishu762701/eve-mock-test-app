package com.eve.app.ui.admin;

import com.eve.app.data.model.AdminPremiumConfigDto;
import com.eve.app.data.remote.EveApiService;
import java.lang.reflect.Proxy;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/** Synthetic transport only, with the production Premium Activity and shared gate. */
public final class PremiumMutationBoundaryFixture {
    public static Object config, emptyList;
    public static AdminPremiumConfigDto submittedConfig;
    public static Continuation<Object> pendingWrite;
    public static int configReads,writes;
    public static void reset() { configReads=0;writes=0;submittedConfig=null;pendingWrite=null; }
    private static final EveApiService api=(EveApiService)Proxy.newProxyInstance(
        EveApiService.class.getClassLoader(),new Class<?>[]{EveApiService.class},(proxy,method,args)->{
            switch(method.getName()) {
                case "getAdminPremiumConfig": configReads++;return config;
                case "getAdminTransactions": case "getAdminPremiumUsers": return emptyList;
                case "updateAdminPremiumConfig":
                    writes++;submittedConfig=(AdminPremiumConfigDto)args[0];
                    pendingWrite=reply(args);return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                case "toString":return "Isolated Premium API";
                default:throw new AssertionError("Unexpected Premium request: "+method.getName());
            }
        });
    @SuppressWarnings("unchecked") private static Continuation<Object> reply(Object[] args) {
        return (Continuation<Object>)args[args.length-1];
    }
    public static EveApiService transport() { return api; }
}
