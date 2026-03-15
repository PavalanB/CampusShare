package com.campusshare.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.campusshare.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReportActivity extends AppCompatActivity {

    private Button btnExport;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        btnExport = findViewById(R.id.btn_export);
        tvStatus = findViewById(R.id.tv_status);

        btnExport.setOnClickListener(v -> exportToSDCard());
    }

    private void exportToSDCard() {
        String fileName = "CampusShare_Report.txt";
        String content = "CampusShare Transaction Report\n" +
                        "User: Student\n" +
                        "Credits Earned: 50\n" +
                        "Tools Shared: 3\n" +
                        "Date: " + System.currentTimeMillis();

        // Requirement 8: Writing data into SD card
        File path = getExternalFilesDir(null); // Recommended way for modern Android
        File file = new File(path, fileName);

        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(content.getBytes());
            tvStatus.setText("File saved to: " + file.getAbsolutePath());
            showNotification("Download Complete", "Report saved to SD Card");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotification(String title, String message) {
        // Requirement 8: Notification Manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "report_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Reports", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
