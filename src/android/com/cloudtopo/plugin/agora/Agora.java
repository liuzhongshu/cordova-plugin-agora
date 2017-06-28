package com.cloudtopo.plugin.agora;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import io.agora.rtc.*;
import io.agora.rtc.video.VideoCanvas;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class Agora extends CordovaPlugin {

    public static final String TAG = "Agora";

    protected Activity appActivity;
    protected Context appContext;


    private static CallbackContext eventCallbackContext;
    private static SurfaceView localView,remoteView;

    @Override
    protected void pluginInitialize() {

        appContext = this.cordova.getActivity().getApplicationContext();
        appActivity = cordova.getActivity();

        //应用初始化
        AgoraClient.Create("1beb71b96eb04d1ca1ef9d93bc28b13a", appContext);
        AgoraClient.getInstance().getRtcEngine().enableVideo();
        AgoraClient.getInstance().getRtcEngine().setVideoProfile(Constants.VIDEO_PROFILE_360P, false); //???default
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                localView = RtcEngine.CreateRendererView(appContext);
                remoteView = RtcEngine.CreateRendererView(appContext);
                AgoraClient.getInstance().getRtcEngine().setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));

                FrameLayout.LayoutParams remoteViewParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                FrameLayout.LayoutParams localViewParams = new FrameLayout.LayoutParams(300,300);
                appActivity.addContentView(localView, localViewParams);
                appActivity.addContentView(remoteView, remoteViewParams);

                //设置这个保证webview的透明
                webView.getView().setBackgroundColor(0x00000000);
                ((ViewGroup)webView.getView()).bringToFront();
            }
        });
        super.pluginInitialize();
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, action + " Called");

        if (action.equals("prepare")) {
            return true;
        }

        if (action.equals("joinChannel")) {
            final String channelName = args.getString(0);
            final int uid = args.getInt(1);

            int result =  AgoraClient.getInstance().getRtcEngine().joinChannel(null, channelName, null, uid);

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec joinChannel failed!"));
            } else {
                callbackContext.success();
            }

            return true;
        }

        if (action.equals("leaveChannel")) {
            int result =  AgoraClient.getInstance().getRtcEngine().leaveChannel();

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec leaveChannel failed!"));
            } else {
                callbackContext.success();
            }

            return true;
        }

        if (action.equals("disableVideo")) {
            int result =  AgoraClient.getInstance().getRtcEngine().disableVideo();

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec disableVideo failed!"));
            } else {
                callbackContext.success();
            }

            return true;
        }

        if (action.equals("enableVideo")) {
            int result =  AgoraClient.getInstance().getRtcEngine().enableVideo();

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec enableVideo failed!"));
            } else {
                callbackContext.success();
            }
            return true;
        }


        if (action.equals("enableSpeakerphone")) {
            int result =  AgoraClient.getInstance().getRtcEngine().setEnableSpeakerphone(true);

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec setEnableSpeakerphone failed!"));
            } else {
                callbackContext.success();
            }
            return true;
        }

        if(action.equals("disableSpeakerphone")) {

            int result =  AgoraClient.getInstance().getRtcEngine().setEnableSpeakerphone(false);

            if(AgoraError.ERR_OK != result) {
                callbackContext.error(ClientError.Build(result, "exec setEnableSpeakerphone failed!"));
            } else {
                callbackContext.success();
            }
            return true;
        }

        if(action.equals("getCallId")) {
            String result =  AgoraClient.getInstance().getRtcEngine().getCallId();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }


        if(action.equals("startRecordingService")) {
            final String recordingKey = args.getString(0);
            int result = AgoraClient.getInstance().getRtcEngine().startRecordingService(recordingKey);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }


        if(action.equals("stopRecordingService")) {
            final String recordingKey = args.getString(0);
            int result = AgoraClient.getInstance().getRtcEngine().stopRecordingService(recordingKey);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        if (action.equals("listenEvents")) {
            eventCallbackContext = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, 0);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        return super.execute(action, args, callbackContext);
    }

    public static void notifyEvent(String event, JSONObject data) {
        //远端用户来了，自动显示
        if (event.equals("onFirstRemoteVideoDecoded") && remoteView != null) {
            try {
                int uid = data.getInt("uid");
                AgoraClient.getInstance().getRtcEngine().setupRemoteVideo(
                        new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
            } catch (JSONException e) {
            }
        }

        if (eventCallbackContext != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("event", event);
                json.put("data", data);
            } catch (JSONException ignored) {
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK, json);
            result.setKeepCallback(true);
            eventCallbackContext.sendPluginResult(result);
        }
    }
}
