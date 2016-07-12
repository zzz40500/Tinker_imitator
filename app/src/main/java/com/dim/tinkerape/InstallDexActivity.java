package com.dim.tinkerape;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.dim.library.Tinker;

public class InstallDexActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_dex);
        Tinker.install();
    }
}
