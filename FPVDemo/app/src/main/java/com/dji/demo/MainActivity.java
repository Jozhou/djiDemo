package com.dji.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.common.util.LocationUtils;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn;
    private Button btnSetting;
    private Button btnTakeOff;
    private Button btnLand;

    private Button btnTakeOff2;
    private Button btnLand2;

    private ProgressDialog mDialog;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(com.dji.demo.R.layout.activity_main);
        onHardwareAccelerated();
        handler = new Handler();

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                    }
                }
            });

            camera.setMediaFileCallback(new MediaFile.Callback() {
                @Override
                public void onNewFile(@NonNull MediaFile mediaFile) {
                    LogcatUtils.d(TAG, "onNewFile");
                    LogcatUtils.d(TAG, "fileName:" + mediaFile.getFileName() + ",size:" + mediaFile.getFileSize()/1024);
                    camera.getMediaManager().fetchMediaData(mediaFile, new File(FileUtils.getCachePath()), mediaFile.getFileName(), new MediaManager.DownloadListener<String>() {
                        @Override
                        public void onStart() {
                            LogcatUtils.d(TAG, "DownloadListener - " + "onStart");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDialog != null) {
                                        mDialog.show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onRateUpdate(long l, long l1, long l2) {
                            LogcatUtils.d(TAG, "DownloadListener - " + "onRateUpdate");

                        }

                        @Override
                        public void onProgress(long l, long l1) {
                            LogcatUtils.d(TAG, "DownloadListener - " + "onProgress");

                        }

                        @Override
                        public void onSuccess(String s) {
                            LogcatUtils.d(TAG, "DownloadListener - " + "onSuccess-" + s);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDialog != null) {
                                        mDialog.dismiss();
                                    }
                                    Toast.makeText(MainActivity.this, "下载成功！", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(final DJIError djiError) {
                            LogcatUtils.d(TAG, "DownloadListener - " + "onFailure - " + djiError.toString());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDialog != null) {
                                        mDialog.dismiss();
                                    }
                                    Toast.makeText(MainActivity.this, "下载失败！" + djiError.toString(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            });

        }
        switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
    }

    /**
     * 开启硬件加速
     */
    @SuppressLint("InlinedApi")
    protected void onHardwareAccelerated() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("正在下载");
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(com.dji.demo.R.id.video_previewer_surface);
        mCaptureBtn = (Button) findViewById(com.dji.demo.R.id.btn_capture);
        btnSetting = (Button) findViewById(R.id.btn_setting);
        btnTakeOff = (Button) findViewById(R.id.btn_takeoff);
        btnLand = (Button) findViewById(R.id.btn_land);
        btnTakeOff2 = (Button) findViewById(R.id.btn_takeoff2);
        btnLand2 = (Button) findViewById(R.id.btn_land2);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        mCaptureBtn.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
        btnTakeOff.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnTakeOff2.setOnClickListener(this);
        btnLand2.setOnClickListener(this);
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(com.dji.demo.R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case com.dji.demo.R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_setting:{
                onSettingClick();
                break;
            }
            case R.id.btn_takeoff:{
                onTakeOffClick();
                break;
            }
            case R.id.btn_land:{
                onLandClick();
                break;
            }
            case R.id.btn_takeoff2:{
                startWaypointMission();
                break;
            }
            case R.id.btn_land2:{
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }

    private void onSettingClick() {
        Intent intent = new Intent(this, SelectActivity.class);
        startActivityForResult(intent, 100);
    }

    private void onTakeOffClick() {
        LogcatUtils.e(TAG, "onSettingClick");
        BaseProduct product = FPVDemoApplication.getProductInstance();
        Aircraft aircraft = null;
        if (product != null) {
            aircraft = ((Aircraft)product);
        } else {
            return;
        }
        if (aircraft.getFlightController().getState().isFlying()) {
            LogcatUtils.e(TAG, "onSettingClick - " + "飞机已经起飞");
            Toast.makeText(MainActivity.this, "飞机已经起飞", Toast.LENGTH_SHORT).show();
            return;
        }
        mDialog.setMessage("正在起飞。。。");
        mDialog.show();
        aircraft.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            LogcatUtils.e(TAG, "onSettingClick - " + "起飞成功");
                            mDialog.dismiss();
                            Toast.makeText(MainActivity.this, "起飞成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else{
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            LogcatUtils.e(TAG, "onSettingClick - " + "起飞失败");
                            mDialog.dismiss();
                            Toast.makeText(MainActivity.this, "起飞失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void onLandClick() {
        BaseProduct product = FPVDemoApplication.getProductInstance();
        Aircraft aircraft = null;
        if (product != null) {
            aircraft = ((Aircraft)product);
        } else {
            return;
        }
        if (!aircraft.getFlightController().getState().isFlying()) {
            Toast.makeText(MainActivity.this, "飞机还未起飞", Toast.LENGTH_SHORT).show();
            return;
        }
        mDialog.setMessage("正在降落。。。");
        mDialog.show();
        aircraft.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.dismiss();
                        Toast.makeText(MainActivity.this, "降落成功", Toast.LENGTH_SHORT).show();
                        btnTakeOff.setEnabled(false);
                    }
                });
            }
        });
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

//                    if (error == null) {
//                        showToast("Switch Camera Mode Succeeded");
//                    } else {
//                        showToast(error.getDescription());
//                    }
                }
            });
            }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                LogcatUtils.d(TAG, "take photo success");
                                                showToast("拍照成功");
                                            } else {
                                                LogcatUtils.d(TAG, "take photo failed." + djiError.getDescription());
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 2000);
                        }
                    }
            });
        }
    }

    ArrayList<LatLng> latLngArrayList;
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    public static WaypointMission.Builder waypointMissionBuilder;
    private List<Waypoint> waypointList = new ArrayList<>();
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            if (resultCode == RESULT_OK) {
                latLngArrayList = data.getParcelableArrayListExtra("latlngs");
                addd();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        configWayPointMission();
                    }
                }, 1000);
            }
        }
    }

    private void addd() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder();
        }
        clear();
        for (int i = 0; i < latLngArrayList.size(); i ++) {
            LatLng point = latLngArrayList.get(i);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            waypointList.add(mWaypoint);
        }
        waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
    }

    private void clear() {
        waypointList.clear();
        waypointMissionBuilder.waypointList(waypointList);
    }

    private void startWaypointMission(){
        LogcatUtils.e(TAG, "startWaypointMission");
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                final String str = (error == null ? "路线设置飞行成功" : "路线设置飞行失败 - " + error.getDescription());
                LogcatUtils.e(TAG, str);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void stopWaypointMission(){
        LogcatUtils.e(TAG, "stopWaypointMission");
        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                final String str = (error == null ? "停止飞行成功" : "停止飞行失败 - " + error.getDescription());
                LogcatUtils.e(TAG, str);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void configWayPointMission(){
        LogcatUtils.e(TAG, "configWayPointMission");

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            LogcatUtils.e(TAG, "Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            LogcatUtils.e(TAG, "loadWaypoint succeeded");
        } else {
            LogcatUtils.e(TAG, "loadWaypoint failed " + error.getDescription());
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                uploadWayPointMission();
            }
        }, 1000);

    }

    private void uploadWayPointMission(){
        LogcatUtils.e(TAG, "uploadWayPointMission");

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    LogcatUtils.e(TAG, "Mission upload successfully!");
                } else {
                    LogcatUtils.e(TAG, "Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

}
