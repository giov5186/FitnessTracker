package id.giovani.fitnesstracker.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import id.giovani.fitnesstracker.R;
import id.giovani.fitnesstracker.adapter.WorkoutAdapter;
import id.giovani.fitnesstracker.model.WorkoutModel;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistory = view.findViewById(R.id.rv_history);
        tvEmpty   = view.findViewById(R.id.tv_empty);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        List<WorkoutModel> list = bacaDataLatihan();

        if (list.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            WorkoutAdapter adapter = new WorkoutAdapter(list);
            rvHistory.setAdapter(adapter);
        }
    }

    private List<WorkoutModel> bacaDataLatihan() {
        List<WorkoutModel> list = new ArrayList<>();
        try {
            FileInputStream fis = requireActivity().openFileInput("workout_log.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|");

                String nama    = parts.length > 0 ? parts[0] : "-";
                String durasi  = parts.length > 1 ? parts[1] : "-";
                String tanggal = parts.length > 2 ? parts[2] : "-";
                int langkah    = 0;
                int kalori     = 0;
                int bpm        = 0;

                if (parts.length > 3) {
                    try { langkah = Integer.parseInt(parts[3].trim()); }
                    catch (NumberFormatException ignored) {}
                }
                if (parts.length > 4) {
                    try { kalori = Integer.parseInt(parts[4].trim()); }
                    catch (NumberFormatException ignored) {}
                }
                if (parts.length > 5) {
                    try { bpm = Integer.parseInt(parts[5].trim()); }
                    catch (NumberFormatException ignored) {}
                }

                list.add(0, new WorkoutModel(nama, durasi, tanggal, langkah, kalori, bpm));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}