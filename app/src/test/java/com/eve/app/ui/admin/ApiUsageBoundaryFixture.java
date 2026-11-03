package com.eve.app.ui.admin;

import com.eve.app.data.remote.EveApiService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/** Transport boundary only: real gate, repository, Activity and widgets execute. */
public final class ApiUsageBoundaryFixture {
    public static final List<Continuation<Object>> reads = new ArrayList<>();
    public static final List<String> months = new ArrayList<>();
    public static void reset() { reads.clear(); months.clear(); }
    private static final EveApiService api = (EveApiService) Proxy.newProxyInstance(
        EveApiService.class.getClassLoader(), new Class<?>[]{EveApiService.class}, (proxy, method, args) -> {
            if (!method.getName().equals("getApiUsage")) throw new AssertionError("Unexpected usage request: " + method.getName());
            months.add((String)args[0]); reads.add(reply(args));
            return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        });
    @SuppressWarnings("unchecked")
    private static Continuation<Object> reply(Object[] args) { return (Continuation<Object>)args[args.length-1]; }
    public static EveApiService transport() { return api; }
}
