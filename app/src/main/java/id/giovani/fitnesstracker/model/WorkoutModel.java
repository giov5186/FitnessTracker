package id.giovani.fitnesstracker.model;

public class WorkoutModel {
    private String namaLatihan;
    private String durasi;
    private String tanggal;
    private int langkah;
    private int kalori;
    private int bpm;

    public WorkoutModel(String namaLatihan, String durasi, String tanggal,
                        int langkah, int kalori, int bpm) {
        this.namaLatihan = namaLatihan;
        this.durasi      = durasi;
        this.tanggal     = tanggal;
        this.langkah     = langkah;
        this.kalori      = kalori;
        this.bpm         = bpm;
    }

    public String getNamaLatihan() { return namaLatihan; }
    public String getDurasi()      { return durasi; }
    public String getTanggal()     { return tanggal; }
    public int getLangkah()        { return langkah; }
    public int getKalori()         { return kalori; }
    public int getBpm()            { return bpm; }
}