package com.eve.app.ui.profile;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.internal.zzac;
import com.google.gson.internal.UnsafeAllocator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** SDK boundary only: no Firebase app, tokens, network, roles or production bypass. */
public final class ProfileEditorAuthFixture {
    private static FirebaseAuth auth;
    private static FirebaseUser user;
    public static final Set<FirebaseAuth.AuthStateListener> listeners = new LinkedHashSet<>();
    private static <T> T allocate(Class<T> type) {
        try { return UnsafeAllocator.INSTANCE.newInstance(type); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    public static FirebaseAuth auth() {
        if (auth == null) auth = allocate(TestAuth.class);
        return auth;
    }
    public static void setOwner(String uid) {
        if (uid == null) user = null;
        else {
            TestUser next = allocate(TestUser.class);
            next.testUid = uid;
            user = next;
        }
        for (FirebaseAuth.AuthStateListener listener : listeners.toArray(new FirebaseAuth.AuthStateListener[0])) {
            listener.onAuthStateChanged(auth());
        }
    }
    public static void reset() { listeners.clear(); user = null; auth = null; }
    /** Allocation skips the SDK constructor; each fake user's identity stays immutable. */
    public static final class TestUser extends zzac {
        String testUid;
        private TestUser() { super(null, Collections.emptyList()); }
        @Override public String getUid() { return testUid; }
        @Override public String getDisplayName() { return "Owner"; }
        @Override public String getEmail() { return "owner@example.test"; }
    }
    /** Ordinary test overrides avoid running SDK instance methods on an uninitialized shell. */
    public static final class TestAuth extends FirebaseAuth {
        private TestAuth() { super(null,null,null,null,null,null,null,null); }
        @Override public FirebaseUser getCurrentUser() { return user; }
        @Override public String getUid() { return user == null ? null : user.getUid(); }
        @Override public void addAuthStateListener(FirebaseAuth.AuthStateListener listener) {
            listeners.add(listener); listener.onAuthStateChanged(auth());
        }
        @Override public void removeAuthStateListener(FirebaseAuth.AuthStateListener listener) { listeners.remove(listener); }
    }
    @Implements(value=FirebaseAuth.class, isInAndroidSdk=false)
    public static class ShadowAuth {
        @Implementation public static FirebaseAuth getInstance() { return auth(); }
    }
}
