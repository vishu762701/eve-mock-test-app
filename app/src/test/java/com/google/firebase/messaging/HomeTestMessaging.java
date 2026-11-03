package com.google.firebase.messaging;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.internal.UnsafeAllocator;

/** Test SDK overrides; allocation skips all push-service construction and registration. */
public final class HomeTestMessaging extends FirebaseMessaging {
    private HomeTestMessaging(){super(null,null,null,null,null,null,null);}
    public static FirebaseMessaging create(){
        try{return UnsafeAllocator.INSTANCE.newInstance(HomeTestMessaging.class);}
        catch(Exception error){throw new AssertionError(error);}
    }
    @Override public Task<Void> subscribeToTopic(String topic){return Tasks.forResult(null);}
    @Override public Task<String> getToken(){return Tasks.forResult("synthetic-push-token");}
}
