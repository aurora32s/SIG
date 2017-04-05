package kr.ac.a20141280.kumoh.ce.connectdevice;

import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Round on 2017-04-03.
 */
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService{

    private static final String TAG = "FirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){

        Log.i(TAG,"RemoteMessage >> "+remoteMessage);
    }
}
