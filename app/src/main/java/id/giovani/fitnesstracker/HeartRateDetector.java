package id.giovani.fitnesstracker;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HeartRateDetector {

    public interface HeartRateCallback {
        void onHeartRateDetected(int bpm);
        void onError(String message);
    }

    private Context context;
    private HeartRateCallback callback;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private List<Long> beatTimes = new ArrayList<>();
    private long lastBeatTime = 0;
    private static final int SAMPLE_DURATION = 10000; // 10 detik
    private long startTime;

    public HeartRateDetector(Context context, HeartRateCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void start() {
        startBackgroundThread();
        openCamera();
        startTime = System.currentTimeMillis();
        beatTimes.clear();
    }

    public void stop() {
        closeCamera();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("HeartRateThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void openCamera() {
        CameraManager manager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null &&
                        facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null) {
                callback.onError("Kamera tidak ditemukan");
                return;
            }

            imageReader = ImageReader.newInstance(320, 240,
                    ImageFormat.YUV_420_888, 5);
            imageReader.setOnImageAvailableListener(
                    reader -> {
                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            processImage(image);
                            image.close();
                        }
                    }, backgroundHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice device) {
                    cameraDevice = device;
                    createCaptureSession();
                }
                @Override
                public void onDisconnected(@NonNull CameraDevice device) {
                    device.close();
                }
                @Override
                public void onError(@NonNull CameraDevice device, int error) {
                    device.close();
                    callback.onError("Error kamera: " + error);
                }
            }, backgroundHandler);

        } catch (CameraAccessException | SecurityException e) {
            callback.onError("Tidak bisa akses kamera: " + e.getMessage());
        }
    }

    private void createCaptureSession() {
        try {
            CaptureRequest.Builder builder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            // Aktifkan flash/torch
            builder.set(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH);

            cameraDevice.createCaptureSession(
                    Collections.singletonList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(
                                @NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(
                                        builder.build(), null,
                                        backgroundHandler);
                            } catch (CameraAccessException e) {
                                callback.onError(e.getMessage());
                            }
                        }
                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession session) {
                            callback.onError("Konfigurasi kamera gagal");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            callback.onError(e.getMessage());
        }
    }

    private long lastAvg = 0;
    private static final long BEAT_THRESHOLD = 15;

    private void processImage(Image image) {
        // Ambil channel Y (brightness) dari frame YUV
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        long sum = 0;
        int count = data.length / 4;
        for (int i = 0; i < data.length; i += 4) {
            sum += (data[i] & 0xFF);
        }
        long avg = count > 0 ? sum / count : 0;

        long now = System.currentTimeMillis();

        // Deteksi beat berdasarkan perubahan brightness
        if (lastAvg > 0) {
            long diff = Math.abs(avg - lastAvg);
            if (diff > BEAT_THRESHOLD &&
                    (now - lastBeatTime) > 300) {
                beatTimes.add(now);
                lastBeatTime = now;
            }
        }
        lastAvg = avg;

        // Hitung BPM setelah 10 detik
        long elapsed = now - startTime;
        if (elapsed >= SAMPLE_DURATION) {
            calculateBPM();
        }
    }

    private void calculateBPM() {
        if (beatTimes.size() < 2) {
            callback.onError("Tempelkan jari ke kamera dengan lebih kuat");
            return;
        }
        // Hitung rata-rata interval antar beat
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < beatTimes.size(); i++) {
            intervals.add(beatTimes.get(i) - beatTimes.get(i - 1));
        }
        long totalInterval = 0;
        for (long interval : intervals) {
            totalInterval += interval;
        }
        long avgInterval = totalInterval / intervals.size();
        int bpm = (int) (60000 / avgInterval);

        // Filter nilai yang masuk akal (40-200 bpm)
        if (bpm >= 40 && bpm <= 200) {
            callback.onHeartRateDetected(bpm);
        } else {
            callback.onError("Gagal baca. Coba lagi.");
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}