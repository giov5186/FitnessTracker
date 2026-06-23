package id.giovani.fitnesstracker.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import id.giovani.fitnesstracker.HeartRateDetector;
import id.giovani.fitnesstracker.R;

public class HomeFragment extends Fragment implements SensorEventListener {

    // Views
    private TextView tvSteps, tvVoiceResult, tvKalori, tvBpm;
    private EditText etDurasi;
    private LinearLayout btnVoice;
    private Button btnSimpan, btnReset;

    // Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int stepCount = 0;
    private float lastX, lastY, lastZ;
    private boolean initialized = false;
    private static final float STEP_THRESHOLD = 12f;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREF_NAME = "FitnessTrackerPrefs";
    private static final String KEY_STEPS = "total_steps";

    // Speech Recognition
    private String namaLatihan = "";
    private ActivityResultLauncher<Intent> speechLauncher;

    // Heart Rate
    private HeartRateDetector heartRateDetector;
    private boolean isDetecting = false;
    private static final int REQ_CAMERA = 201;

    // Kalori
    private static final float KALORI_PER_LANGKAH = 0.05f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init Views
        tvSteps       = view.findViewById(R.id.tv_steps);
        tvVoiceResult = view.findViewById(R.id.tv_voice_result);
        etDurasi      = view.findViewById(R.id.et_durasi);
        btnVoice      = view.findViewById(R.id.btn_voice);
        btnSimpan     = view.findViewById(R.id.btn_simpan);
        btnReset      = view.findViewById(R.id.btn_reset_steps);
        tvKalori      = view.findViewById(R.id.tv_kalori);
        tvBpm         = view.findViewById(R.id.tv_bpm);

        // SharedPreferences — load langkah tersimpan
        prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        stepCount = prefs.getInt(KEY_STEPS, 0);
        tvSteps.setText(stepCount + " langkah");
        updateKalori();

        // Sensor Accelerometer
        sensorManager = (SensorManager) requireActivity()
                .getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Speech Recognition Launcher
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            namaLatihan = matches.get(0);
                            tvVoiceResult.setText("✅ " + namaLatihan);
                            Toast.makeText(getContext(),
                                    "Latihan: " + namaLatihan,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Tombol Voice Input
        btnVoice.setOnClickListener(v -> startSpeechRecognition());

        // Card BPM — tap untuk ukur heart rate
        View cardBpm = view.findViewById(R.id.card_bpm);
        if (cardBpm != null) {
            cardBpm.setOnClickListener(v -> toggleHeartRate());
        }

        // Tombol Reset Langkah
        btnReset.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Reset Langkah")
                        .setMessage("Yakin ingin mereset hitungan langkah?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            stepCount = 0;
                            tvSteps.setText("0 langkah");
                            tvKalori.setText("0");
                            prefs.edit().putInt(KEY_STEPS, 0).apply();
                            Toast.makeText(getContext(),
                                    "Langkah direset!",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show()
        );

        // Tombol Simpan Latihan
        btnSimpan.setOnClickListener(v -> simpanLatihan(view));
    }

    // ─── Speech Recognition ───────────────────────────────────────────────────

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama latihan...");
        speechLauncher.launch(intent);
    }

    // ─── Simpan Latihan ───────────────────────────────────────────────────────

    private void simpanLatihan(View view) {
        String durasi = etDurasi.getText().toString().trim();

        if (namaLatihan.isEmpty()) {
            Snackbar.make(view, "⚠️ Harap input nama latihan via suara dulu!",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (durasi.isEmpty()) {
            Snackbar.make(view, "⚠️ Harap isi durasi latihan!",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        String tanggal = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                Locale.getDefault()).format(new Date());

        int kalori = (int)(stepCount * KALORI_PER_LANGKAH);
        int bpmVal = 0;
        try {
            bpmVal = Integer.parseInt(tvBpm.getText().toString());
        } catch (NumberFormatException ignored) {}

        String data = namaLatihan + "|"
                + durasi + " menit|"
                + tanggal + "|"
                + stepCount + "|"
                + kalori + "|"
                + bpmVal + "\n";

        try {
            FileOutputStream fos = requireActivity()
                    .openFileOutput("workout_log.txt", Context.MODE_APPEND);
            fos.write(data.getBytes());
            fos.close();

            prefs.edit().putInt(KEY_STEPS, stepCount).apply();

            Snackbar.make(view, "✅ Latihan berhasil disimpan!", Snackbar.LENGTH_LONG)
                    .setAction("Lihat Riwayat", v ->
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container,
                                            new HistoryFragment())
                                    .commit())
                    .show();

            // Reset input
            namaLatihan = "";
            tvVoiceResult.setText("Tekan mic lalu sebutkan latihan...");
            etDurasi.setText("");

        } catch (IOException e) {
            Toast.makeText(getContext(),
                    "Gagal menyimpan: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Kalori ───────────────────────────────────────────────────────────────

    private void updateKalori() {
        int kalori = (int)(stepCount * KALORI_PER_LANGKAH);
        if (tvKalori != null) {
            tvKalori.setText(String.valueOf(kalori));
        }
    }

    // ─── Heart Rate ───────────────────────────────────────────────────────────

    private void toggleHeartRate() {
        if (isDetecting) {
            stopHeartRate();
        } else {
            startHeartRate();
        }
    }

    private void startHeartRate() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            Toast.makeText(getContext(),
                    "Izin kamera diperlukan untuk mengukur BPM",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isDetecting = true;
        tvBpm.setText("...");
        Toast.makeText(getContext(),
                "Tempelkan jari ke kamera belakang (10 detik)",
                Toast.LENGTH_LONG).show();

        heartRateDetector = new HeartRateDetector(requireContext(),
                new HeartRateDetector.HeartRateCallback() {
                    @Override
                    public void onHeartRateDetected(int bpm) {
                        if (getActivity() == null) return;
                        requireActivity().runOnUiThread(() -> {
                            tvBpm.setText(String.valueOf(bpm));
                            isDetecting = false;
                            heartRateDetector.stop();
                            Toast.makeText(getContext(),
                                    "Detak jantung: " + bpm + " BPM",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() == null) return;
                        requireActivity().runOnUiThread(() -> {
                            tvBpm.setText("--");
                            isDetecting = false;
                            Toast.makeText(getContext(),
                                    message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        heartRateDetector.start();
    }

    private void stopHeartRate() {
        if (heartRateDetector != null) {
            heartRateDetector.stop();
            heartRateDetector = null;
        }
        isDetecting = false;
        if (tvBpm != null) tvBpm.setText("--");
    }

    // ─── Sensor Accelerometer ─────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (initialized) {
                float deltaX = Math.abs(x - lastX);
                float deltaY = Math.abs(y - lastY);
                float deltaZ = Math.abs(z - lastZ);

                if (deltaX + deltaY + deltaZ > STEP_THRESHOLD) {
                    stepCount++;
                    tvSteps.setText(stepCount + " langkah");
                    updateKalori();
                }
            }

            lastX = x; lastY = y; lastZ = z;
            initialized = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopHeartRate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopHeartRate();
    }
}