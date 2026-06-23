package id.giovani.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import id.giovani.fitnesstracker.R;
import id.giovani.fitnesstracker.model.WorkoutModel;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private List<WorkoutModel> list;

    public WorkoutAdapter(List<WorkoutModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutModel w = list.get(position);
        holder.tvNama.setText(w.getNamaLatihan());
        holder.tvDurasi.setText("⏱️ " + w.getDurasi());
        holder.tvLangkah.setText("👟 " + w.getLangkah() + " langkah");
        holder.tvTanggal.setText("📅 " + w.getTanggal());
        holder.tvKalori.setText("🔥 " + w.getKalori() + " kal");
        holder.tvBpm.setText("❤️ " + (w.getBpm() > 0 ? w.getBpm() + " bpm" : "--"));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvDurasi, tvLangkah, tvTanggal, tvKalori, tvBpm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama    = itemView.findViewById(R.id.tv_nama);
            tvDurasi  = itemView.findViewById(R.id.tv_durasi);
            tvLangkah = itemView.findViewById(R.id.tv_langkah);
            tvTanggal = itemView.findViewById(R.id.tv_tanggal);
            tvKalori  = itemView.findViewById(R.id.tv_kalori_item);
            tvBpm     = itemView.findViewById(R.id.tv_bpm_item);
        }
    }
}