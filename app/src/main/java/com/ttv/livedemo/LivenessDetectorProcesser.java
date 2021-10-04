package com.ttv.livedemo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.LivenessInfo;
import com.ttv.face.MaskInfo;

import java.util.ArrayList;
import java.util.List;

import io.fotoapparat.parameter.Size;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;

/**
 * {@link FrameProcessor} which detects faces on camera frames.
 * <p>
 * Use {@link #with(Context)} to create a new instance.
 */
public class LivenessDetectorProcesser implements FrameProcessor {

    private static Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    private final OnFacesDetectedListener listener;
    private FaceEngine faceEngine;

    private LivenessDetectorProcesser(Builder builder) {
        faceEngine = FaceEngine.getInstance(builder.context);
        listener = builder.listener;
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    @Override
    public void processFrame(Frame frame) {
        FaceSDK faceSDK = faceEngine.getFaceVideoSDK();
        List<FaceInfo> faceInfoList = new ArrayList<>();
        List<LivenessInfo> livenessInfoList = new ArrayList<>();
        List<MaskInfo> maskInfoList = new ArrayList<>();
        faceSDK.detectFaces(frame.image, frame.size.width, frame.size.height, FaceSDK.CP_PAF_NV21, faceInfoList);
        if(faceInfoList.size() > 0) {

            FaceSDK liveSDK = faceEngine.getFaceImageSDK();
            int flCode = liveSDK.process(frame.image, frame.size.width, frame.size.height, FaceSDK.CP_PAF_NV21, faceInfoList, FaceSDK.TTV_LIVENESS | FaceSDK.TTV_MASK_DETECT);
            if(flCode == ErrorInfo.MOK) {
                liveSDK.getLiveness(livenessInfoList);
                liveSDK.getMask(maskInfoList);
            }
        }

        MAIN_THREAD_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onFacesDetected(faceInfoList, livenessInfoList, maskInfoList, frame.size);
            }
        });
    }

    /**
     * Notified when faces are detected.
     */
    public interface OnFacesDetectedListener {

        /**
         * Null-object for {@link OnFacesDetectedListener}.
         */
        OnFacesDetectedListener NULL = new OnFacesDetectedListener() {
            @Override
            public void onFacesDetected(List<FaceInfo> faces, List<LivenessInfo> livenessInfos, List<MaskInfo> maskInfos, Size frameSize) {
                // Do nothing
            }
        };

        /**
         * Called when faces are detected. Always called on the main thread.
         *
         * @param faces detected faces. If no faces were detected - an empty list.
         */
        void onFacesDetected(List<FaceInfo> faces, List<LivenessInfo> livenessInfos, List<MaskInfo> maskInfos, Size frameSize);

    }

    /**
     * Builder for {@link LivenessDetectorProcesser}.
     */
    public static class Builder {

        private final Context context;
        private OnFacesDetectedListener listener = OnFacesDetectedListener.NULL;

        private Builder(Context context) {
            this.context = context;
        }

        /**
         * @param listener which will be notified when faces are detected.
         */
        public Builder listener(OnFacesDetectedListener listener) {
            this.listener = listener != null
                    ? listener
                    : OnFacesDetectedListener.NULL;

            return this;
        }

        public LivenessDetectorProcesser build() {
            return new LivenessDetectorProcesser(this);
        }

    }

}
