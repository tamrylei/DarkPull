package com.tamry.test;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.tengban.sdk.base.utils.AndroidUtil;

public class MainActivity extends AppCompatActivity {

    private EditText mUrlEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidUtil.checkAndRequestPermission(this, Manifest.permission.READ_PHONE_STATE);

        mUrlEdit = findViewById(R.id.url_edt);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void startTestActivity(View view) {
    }
}
