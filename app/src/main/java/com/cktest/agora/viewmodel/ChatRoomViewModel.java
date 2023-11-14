package com.cktest.agora.viewmodel;

import static com.cktest.agora.Constants.REQUESTED_PERMISSIONS;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cktest.agora.Constants;
import com.cktest.agora.OfflineData;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class ChatRoomViewModel extends ViewModel {
    public MutableLiveData<Boolean> isJoined = new MutableLiveData<>(false);
    private RtcEngine agoraEngine;

    public Boolean checkSelfPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }


    public RtcEngine setupVideoSDKEngine(Activity act) {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = act.getBaseContext();
            config.mAppId = Constants.appId(act);
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                // Listen for the remote host joining the channel to get the uid of the host.
                public void onUserJoined(int uid, int elapsed) {
                    // Set the remote video view
                    setupRemoteVideoLiveData.postValue(uid);
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    isJoined.setValue(true);
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    OfflineData data = new OfflineData();
                    data.reason = reason;
                    data.uuid = uid;
                    onUserOffline.postValue(data);
                }
            };

            agoraEngine = RtcEngine.create(config);
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine.enableVideo();
            return agoraEngine;
        } catch (Exception e) {
            showMessage(act, e.toString());
        }
        return agoraEngine;
    }

    public MutableLiveData<Integer> setupRemoteVideoLiveData = new MutableLiveData<>(0);
    public MutableLiveData<OfflineData> onUserOffline = new MutableLiveData<>();
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the remote host joining the channel to get the uid of the host.
        public void onUserJoined(int uid, int elapsed) {
            // Set the remote video view
            setupRemoteVideoLiveData.postValue(uid);
//            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            isJoined.setValue(true);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            OfflineData data = new OfflineData();
            data.reason = reason;
            data.uuid = uid;
            onUserOffline.postValue(data);
        }
    };


    public void showMessage(Activity activity, String message) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity.getApplication(), message, Toast.LENGTH_SHORT).show());
    }


}
