package com.eve.app.ui.admin;

import com.eve.app.data.remote.EveApiService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/** Isolated transport: real repository, gate, Activity, native dialogs and tabs run. */
public final class AppContentBoundaryFixture {
    public static final List<Continuation<Object>> reads=new ArrayList<>(), writes=new ArrayList<>();
    public static final List<String> readTypes=new ArrayList<>(), writeTypes=new ArrayList<>();
    public static final List<Map<String,Object>> bodies=new ArrayList<>();
    public static void reset() {reads.clear();writes.clear();readTypes.clear();writeTypes.clear();bodies.clear();}
    private static final EveApiService api=(EveApiService)Proxy.newProxyInstance(
        EveApiService.class.getClassLoader(),new Class<?>[]{EveApiService.class},(proxy,method,args)->{
            if(method.getName().equals("getAppContent")) {
                readTypes.add((String)args[0]);reads.add(reply(args));
            } else if(method.getName().equals("updateAppContent")) {
                writeTypes.add((String)args[0]);bodies.add(body(args[1]));writes.add(reply(args));
            } else throw new AssertionError("Unexpected content request: "+method.getName());
            return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        });
    @SuppressWarnings("unchecked") private static Map<String,Object> body(Object value){return (Map<String,Object>)value;}
    @SuppressWarnings("unchecked") private static Continuation<Object> reply(Object[] args){return (Continuation<Object>)args[args.length-1];}
    public static EveApiService transport(){return api;}
}
