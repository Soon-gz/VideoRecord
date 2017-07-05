package com.example.administrator.videorecorddemo;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button;
    String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.btn_record_video);
        videoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/customVideo/";
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = VedioRecordActivity.startRecordActivity(videoPath,MainActivity.this);
                startActivity(intent);
            }
        });
    }
}
