package com.cktest.agora;

import static com.cktest.agora.Constants.PERMISSION_REQ_ID;
import static com.cktest.agora.Constants.REQUESTED_PERMISSIONS;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.cktest.agora.databinding.ActivityMainBinding;
import com.cktest.agora.viewmodel.ChatRoomViewModel;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;

public class MainActivity extends AppCompatActivity {

    private String channelName = "cktest";
    // An integer that identifies the local user.
    private int uid = 0;
    //    private boolean isJoined = false;
    private ChatRoomViewModel viewModel;
    private RtcEngine agoraEngine;
    private ActivityMainBinding binding;
    //SurfaceView to render local video in a Container.
    private SurfaceView localSurfaceView;
    //SurfaceView to render Remote video in a Container.
    private SurfaceView remoteSurfaceView;

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatRoomViewModel.class);

        viewModel.setupRemoteVideoLiveData.observe(this, res -> {
            if (uid != 0) {
                setupRemoteVideo(uid);
            }
        });

        viewModel.onUserOffline.observe(this, offlineData -> {
            runOnUiThread(() -> remoteSurfaceView.setVisibility(View.GONE));
        });
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = binding.remoteVideoViewContainer;
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        // Display RemoteSurfaceView.
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }

    private void setupLocalVideo() {
        FrameLayout container = binding.localVideoViewContainer;
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = new SurfaceView(getBaseContext());
        container.addView(localSurfaceView);
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    public void joinChannel(View view) {
        if (viewModel.checkSelfPermission(this)) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            agoraEngine = viewModel.setupVideoSDKEngine(this);
            if (agoraEngine != null) {
                // For a Video call, set the channel profile as COMMUNICATION.
                options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
                // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                // Display LocalSurfaceView.
                setupLocalVideo();
                localSurfaceView.setVisibility(View.VISIBLE);
                // Start local preview.
                agoraEngine.startPreview();
                // Join the channel with a temp token.
                // You need to specify the user ID yourself, and ensure that it is unique in the channel.
                if (binding.etChannelName.getText().toString().length() > 0) {
                    channelName = binding.etChannelName.getText().toString();
                }
                agoraEngine.joinChannel(com.cktest.agora.Constants.token(this), channelName, uid, options);
            }

        } else {
            viewModel.showMessage(this, "Permissions was not granted");
        }
    }

    public void leaveChannel(View view) {
        viewModel.isJoined.observe(this, res -> {
            if (!res) {
                viewModel.showMessage(this, "Join a channel first");
            } else {
                agoraEngine.leaveChannel();
                viewModel.showMessage(this, "You left the channel");
                // Stop remote video rendering.
                if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
                // Stop local video rendering.
                if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
                viewModel.isJoined.setValue(false);
            }
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initViewModel();

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!viewModel.checkSelfPermission(this)) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

}