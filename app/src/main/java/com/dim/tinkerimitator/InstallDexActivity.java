package com.dim.tinkerimitator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.dim.library.Tinker;

import java.util.List;


public class InstallDexActivity extends AppCompatActivity {


    private final int WRITE_EXTERNAL_STORAGE_CODE = 22;

    private TextView mTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_dex);
        mTv = (TextView) findViewById(R.id.resultTv);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_CODE);
        }else{
            installDex();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            installDex();
        }
    }
    private void installDex() {
        new InstallDexTask().execute();
    }

    /**
     * 安装热更新的dex
     */
    private class InstallDexTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {

            return Tinker.install();
        }


        @Override
        protected void onPostExecute(List<String> installList) {
            super.onPostExecute(installList);
            if (installList.size() > 0) {
                StringBuffer sb = new StringBuffer();
                for (String string : installList) {
                    sb.append("成功安装: " + string + "\n");
                }
                sb.append("3秒后进入后台");
                mTv.setText(sb);
                mTv.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }, 3000);
            } else {
                mTv.setText("没有新的patch安装");
            }
        }
    }
}
