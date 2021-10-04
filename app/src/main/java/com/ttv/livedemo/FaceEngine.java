package com.ttv.livedemo;

import android.content.Context;

import com.ttv.face.FaceSDK;
import com.ttv.face.LivenessParam;
import com.ttv.face.enums.DetectFaceOrientPriority;
import com.ttv.face.enums.DetectMode;

public class FaceEngine {

    private static FaceEngine instance;

    private FaceSDK fvEngine;
    private FaceSDK fiEngine;
    private Context mContext;
    private FaceEngine(Context context) {
        mContext = context;
    }

    public static synchronized FaceEngine getInstance(Context context) {
        if (instance == null) {
            instance = new FaceEngine(context);
        }
        return instance;
    }

    public String getCurrentHWID() {
        FaceSDK faceSDK = new FaceSDK(mContext);
        return faceSDK.getCurrentHWID();
    }

    public int setActivation(String license) {
        FaceSDK faceSDK = new FaceSDK(mContext);
        return faceSDK.setActivation(license);
    }

    public void init() {
        if(instance == null)
            return;

        fvEngine = new FaceSDK(mContext);
        fvEngine.init(mContext, DetectMode.TTV_DETECT_MODE_VIDEO, DetectFaceOrientPriority.TTV_OP_ALL_OUT,
                1, FaceSDK.TTV_FACE_DETECT);

        LivenessParam livenessParam = new LivenessParam(0.50f, 0.70f, 0.65f);
//        fvEngine.setLivenessParam(livenessParam);

        fiEngine = new FaceSDK(mContext);
        fiEngine.init(mContext, DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_ALL_OUT,
                1, FaceSDK.TTV_LIVENESS | FaceSDK.TTV_MASK_DETECT);
//        fiEngine.setLivenessParam(livenessParam);
    }

    public void unInit() {
        if(instance == null)
            return;

        fvEngine.unInit();
        fiEngine.unInit();
    }

    public FaceSDK getFaceImageSDK() {
        return fiEngine;
    }

    public FaceSDK getFaceVideoSDK() {
        return fvEngine;
    }
}
