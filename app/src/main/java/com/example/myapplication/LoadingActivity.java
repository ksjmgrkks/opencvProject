package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class LoadingActivity extends AppCompatActivity {
    //어플을 처음 시작할 때 로고를 보여주는 액티비티입니다.
    //로고를 3초정도 보여 준 후 얼굴인식을 할 수 있는 MainActivity 로 이동합니다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        overridePendingTransition(R.xml.fadein, R.xml.fadeout);
        startLoading();
    }
    private void startLoading() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent= new Intent(getApplicationContext(), CameraFilterActivity.class);
                startActivity(intent);

                overridePendingTransition(R.xml.fadein, R.xml.fadeout);

                finish();
            }
        }, 3000);
    }
}