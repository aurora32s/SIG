package kr.ac.a20141280.kumoh.ce.connectdevice;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Round on 2017-04-03.
 */
public class FirebaseInstanceIDService extends FirebaseInstanceIdService{

    private static final String TAG = "FirebaseIDService";
    private String token;
    RequestQueue queue;

    @Override
    public void onTokenRefresh(){

        token = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG,"Refresh Token : "+token);

        //sendToken()

    }

    public void sendToken(){

        String url=null; // token url?

        StringRequest postRequest = new StringRequest(Request.Method.POST,url,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response){
                        Log.i("Response",response);
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        Log.d("Error Response",error.getMessage().toString());
                    }
                }
        ){
            @Override
            protected Map<String,String> getParams(){

                Map<String, String> params = new HashMap<String,String>();
                params.put("fcm_token",token);

                return params;
            }
        };

        queue.add(postRequest);
    }
}
