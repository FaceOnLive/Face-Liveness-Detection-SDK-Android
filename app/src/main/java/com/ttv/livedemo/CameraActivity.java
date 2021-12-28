package com.ttv.livedemo;


import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ttv.face.FaceEngine;
import com.ttv.face.FaceResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.FotoapparatSwitcher;
import io.fotoapparat.parameter.LensPosition;
import io.fotoapparat.parameter.Size;
import io.fotoapparat.parameter.selector.SelectorFunction;
import io.fotoapparat.parameter.selector.SizeSelectors;
import io.fotoapparat.result.PendingResult;
import io.fotoapparat.view.CameraView;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.lensPosition;

public class CameraActivity extends AppCompatActivity {

    private final PermissionsDelegate permissionsDelegate = new PermissionsDelegate(this);
    private boolean hasPermission;
    private CameraView cameraView;
    private FaceRectView rectanglesView;

    private FotoapparatSwitcher fotoapparatSwitcher;
    private Fotoapparat frontFotoapparat;
    private Fotoapparat backFotoapparat;
    private FaceRectTransformer faceRectTransformer;

    ImageView back;
    TextView resultView,partial;
    boolean mSwitchCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        resultView=findViewById(R.id.resultView);
        partial=findViewById(R.id.partial);
        cameraView = (CameraView) findViewById(R.id.camera_view);
        rectanglesView = (FaceRectView) findViewById(R.id.rectanglesView);
        hasPermission = permissionsDelegate.hasPermissions();

        FaceEngine.createInstance(this);
        if (hasPermission) {
            FaceEngine.getInstance().init();
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestPermissions();
        }

        frontFotoapparat = createFotoapparat(LensPosition.FRONT);
        backFotoapparat = createFotoapparat(LensPosition.BACK);
        fotoapparatSwitcher = FotoapparatSwitcher.withDefault(frontFotoapparat);

        View switchCameraButton = findViewById(R.id.switchCamera);
        switchCameraButton.setVisibility(
                canSwitchCameras()
                        ? View.VISIBLE
                        : View.GONE
        );
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        resultView.setText(R.string.liveness_detection);
        rectanglesView.setMode(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean canSwitchCameras() {
        return frontFotoapparat.isAvailable() == backFotoapparat.isAvailable();
    }

    private Fotoapparat createFotoapparat(LensPosition position) {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .lensPosition(lensPosition(position))
                .frameProcessor(
                        LivenessDetectorProcesser.with(this)
                                .listener(new LivenessDetectorProcesser.OnFacesDetectedListener() {
                                    @Override
                                    public void onFacesDetected(List<FaceResult> faces, Size frameSize) {

                                        LensPosition lensPosition;
                                        if (fotoapparatSwitcher.getCurrentFotoapparat() == frontFotoapparat) {
                                            lensPosition = LensPosition.FRONT;
                                        } else {
                                            lensPosition = LensPosition.BACK;
                                        }

                                        if(faceRectTransformer == null || mSwitchCamera == true)
                                        {
                                            mSwitchCamera =false;
                                            int displayOrientation = 0;
                                            ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(cameraView,
                                                    cameraView, rectanglesView,
                                                    new Size(frameSize.width, frameSize.height), displayOrientation, 1.0f);

                                            faceRectTransformer = new FaceRectTransformer(
                                                    frameSize.height, frameSize.width,
                                                    cameraView.getLayoutParams().width, cameraView.getLayoutParams().height,
                                                    displayOrientation, lensPosition, false,
                                                    false,
                                                    false);
                                        }

                                        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
                                        for(int i = 0; i < faces.size(); i ++) {
                                            Rect rect = faceRectTransformer.adjustRect(new Rect(faces.get(i).left, faces.get(i).top, faces.get(i).right, faces.get(i).bottom));

                                            FaceRectView.DrawInfo drawInfo;
                                            if(faces.get(i).livenessScore > 0.5)
                                                drawInfo = new FaceRectView.DrawInfo(rect, 0, 0, 1, Color.GREEN, "", faces.get(i).livenessScore, -1);
                                            else if(faces.get(i).livenessScore < 0)
                                                drawInfo = new FaceRectView.DrawInfo(rect, 0, 0, -1, Color.YELLOW, "", faces.get(i).livenessScore, -1);
                                            else
                                                drawInfo = new FaceRectView.DrawInfo(rect, 0, 0, 0, Color.RED, "", faces.get(i).livenessScore, -1);
                                            drawInfo.setMaskInfo(faces.get(i).mask);
                                            drawInfoList.add(drawInfo);
                                        }

                                        rectanglesView.clearFaceInfo();
                                        rectanglesView.addFaceInfo(drawInfoList);
                                    }
                                })
                                .build()
                )
                .previewSize(new SelectorFunction<Size>() {
                    @Override
                    public Size select(Collection<Size> collection) {
                        return new Size(1280, 720);
                    }
                })
                .build();
    }

    private void switchCamera() {
        if (fotoapparatSwitcher.getCurrentFotoapparat() == frontFotoapparat) {
            fotoapparatSwitcher.switchTo(backFotoapparat);
        } else {
            fotoapparatSwitcher.switchTo(frontFotoapparat);
        }

        mSwitchCamera = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermission) {
            fotoapparatSwitcher.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(permissionsDelegate.hasPermissions() && hasPermission == false) {
            hasPermission = true;

            FaceEngine.getInstance().init();
            fotoapparatSwitcher.start();
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasPermission) {
            try {
                fotoapparatSwitcher.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private ViewGroup.LayoutParams adjustPreviewViewSize(View rgbPreview, View previewView, FaceRectView faceRectView, Size previewSize, int displayOrientation, float scale) {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        int measuredWidth = previewView.getMeasuredWidth();
        int measuredHeight = previewView.getMeasuredHeight();

        layoutParams.width = measuredWidth;
        layoutParams.height = measuredHeight;
        previewView.setLayoutParams(layoutParams);
        faceRectView.setLayoutParams(layoutParams);
        return layoutParams;
    }
}