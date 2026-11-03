package com.eve.app.ui.admin;

import com.eve.app.data.remote.EveApiService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/** Transport boundary only; actual Activity, gate, dialog and callbacks execute. */
public final class SystemMonitorBoundaryFixture {
    public static Object overview, health, operations;
    public static boolean holdOverview;
    public static final List<Continuation<Object>> overviewReads = new ArrayList<>();
    public static final List<Continuation<Object>> retries = new ArrayList<>();
    public static final List<String> retryIds = new ArrayList<>();
    public static int operationReads;
    public static void reset() {
        holdOverview=false; overviewReads.clear(); retries.clear(); retryIds.clear(); operationReads=0;
    }
    private static final EveApiService api = (EveApiService) Proxy.newProxyInstance(
        EveApiService.class.getClassLoader(), new Class<?>[]{EveApiService.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "systemOverview":
                    if (holdOverview) {
                        overviewReads.add(reply(args)); return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    }
                    return overview;
                case "systemHealth": return health;
                case "systemOperations": operationReads++; return operations;
                case "retryGenerationJob":
                    retryIds.add((String)args[0]); retries.add(reply(args));
                    return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                case "toString": return "Isolated monitor API";
                default: throw new AssertionError("Unexpected monitor request: " + method.getName());
            }
        });
    @SuppressWarnings("unchecked")
    private static Continuation<Object> reply(Object[] args) {
        return (Continuation<Object>)args[args.length-1];
    }
    public static EveApiService transport() { return api; }
}
