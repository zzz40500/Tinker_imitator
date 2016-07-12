package com.dim.tinkerape;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.dim.library.Tinker;

import java.util.List;

public class InstallDexActivity extends AppCompatActivity {

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_dex);
        tv = (TextView) findViewById(R.id.resultTv);
        new InstallDexTask().execute();

    }

    /**
     * 安装热更新的dex
     */
    class InstallDexTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {
            return Tinker.install();
        }


        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            StringBuffer sb = new StringBuffer();
            for (String string : strings) {
                sb.append("成功安装: " + string + "\n");
            }
            sb.append("3秒后退出app");
            tv.setText(sb);
            tv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 3000);
        }
    }
}
