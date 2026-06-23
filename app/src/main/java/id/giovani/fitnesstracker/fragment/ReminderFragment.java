package id.giovani.fitnesstracker.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import android.os.Build;
import id.giovani.fitnesstracker.R;
import id.giovani.fitnesstracker.receiver.AlarmReceiver;

public class ReminderFragment extends Fragment {

    private EditText etJam, etMenit;
    private TextView tvStatus;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etJam    = view.findViewById(R.id.et_jam);
        etMenit  = view.findViewById(R.id.et_menit);
        tvStatus = view.findViewById(R.id.tv_alarm_status);

        Button btnSet    = view.findViewById(R.id.btn_set_alarm);
        Button btnCancel = view.findViewById(R.id.btn_cancel_alarm);

        alarmManager = (AlarmManager) requireActivity()
                .getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        btnSet.setOnClickListener(v -> setAlarm());

        btnCancel.setOnClickListener(v -> {
            alarmManager.cancel(pendingIntent);
            tvStatus.setText("❌ Pengingat dibatalkan");
            Toast.makeText(getContext(), "Pengingat dibatalkan", Toast.LENGTH_SHORT).show();
        });
    }

    private void setAlarm() {
        String jamStr   = etJam.getText().toString().trim();
        String menitStr = etMenit.getText().toString().trim();

        if (jamStr.isEmpty() || menitStr.isEmpty()) {
            Toast.makeText(getContext(), "Isi jam dan menit dulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        int jam   = Integer.parseInt(jamStr);
        int menit = Integer.parseInt(menitStr);

        if (jam < 0 || jam > 23 || menit < 0 || menit > 59) {
            Toast.makeText(getContext(), "Format jam/menit tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, jam);
        calendar.set(Calendar.MINUTE, menit);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Jika waktu sudah lewat hari ini, set besok
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager)
                requireActivity().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Pilih metode sesuai versi Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ — gunakan setExactAndAllowWhileIdle
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                Toast.makeText(getContext(),
                        "Izinkan exact alarm di pengaturan!", Toast.LENGTH_LONG).show();
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-11
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            // Android 5 ke bawah
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }

        String menitStr2 = menit < 10 ? "0" + menit : String.valueOf(menit);
        tvStatus.setText("✅ Pengingat aktif setiap hari jam " + jamStr + ":" + menitStr2);
        Toast.makeText(getContext(), "Pengingat berhasil diset!", Toast.LENGTH_SHORT).show();
    }
}