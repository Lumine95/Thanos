package com.mm.thanos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private CircularProgressView progress;
    private ImageView ivSnap;

    private String sdcard = Environment.getExternalStorageState();
    private String state = Environment.MEDIA_MOUNTED;
    private StatFs statFs;
    private TextView tvTotal;
    private TextView tvAvailable;
    private ProgressBar pbLoading;
    private List<File> fileList = new ArrayList<>();
    ;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(granted -> {
            if (!granted) {
                finish();
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
            }
        });

        initView();
    }

    private void initView() {
        progress = findViewById(R.id.progress);
        pbLoading = findViewById(R.id.pb_loading);
        ivSnap = findViewById(R.id.iv_snap);
        tvTotal = findViewById(R.id.tv_total_size);
        tvAvailable = findViewById(R.id.tv_available_size);
        File file = Environment.getExternalStorageDirectory();
        statFs = new StatFs(file.getPath());
        float ratio = getAvailableSize() / getTotalSize();
        progress.setProgress(ratio);
        Log.d("fileSize  ", getAvailableSize() + " —————— " + getTotalSize() + "---" + ratio);

        ivSnap.setOnClickListener(v -> {
            fileList.clear();
            pbLoading.setVisibility(View.VISIBLE);
            new WorkThread().start();
        });
        // Log.d("getAllFile", "size = " + allFile.size());
    }

    private float getTotalSize() {
        if (sdcard.equals(state)) {
            //获得sdcard上 block的总数
            long blockCount = statFs.getBlockCount();
            //获得sdcard上每个block 的大小
            long blockSize = statFs.getBlockSize();
            //计算标准大小使用：1024，当然使用1000也可以
            tvTotal.setText("Total " + getFormatSize(blockCount * blockSize));
            return blockCount * blockSize / 1000 / 1000;
        } else {
            return -1;
        }
    }

    private float getAvailableSize() {
        if (sdcard.equals(state)) {
            //获得Sdcard上每个block的size
            long blockSize = statFs.getBlockSize();
            //获取可供程序使用的Block数量
            long blockavailable = statFs.getAvailableBlocks();
            //计算标准大小使用：1024，当然使用1000也可以
            tvAvailable.setText("Available " + getFormatSize(blockSize * blockavailable));
            return blockSize * blockavailable / 1000 / 1000;
        } else {
            return -1;
        }
    }

    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            // return size + "Byte";
            return "0K";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB";
    }

    public List<File> getAllFile(File dir) {
        //得到某个文件夹下所有的文件
        File[] files = dir.listFiles();
        //文件为空
        if (files == null) {
            return fileList;
        }
        //遍历当前文件下的所有文件
        for (File file : files) {
            //如果是文件夹
            if (file.isDirectory()) {
                //则递归(方法自己调用自己)继续遍历该文件夹
                getAllFile(file);
            } else { //如果不是文件夹 则是文件
                fileList.add(file);
            }
        }
        return fileList;
    }


    public class WorkThread extends Thread {
        @Override
        public void run() {
            getAllFile(Environment.getExternalStorageDirectory());
            Log.d("getAllFile", "size = " + fileList.size());
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                Log.d("getAllFile", "size = " + fileList.size());
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Thanos Warning")
//                        .setMessage("一共扫描出" + fileList.size() + "个文件\n确定要随机删除一半吗？")
                        .setMessage(fileList.size() + " files were scanned.\nAre you sure you want to delete half at random?")
                        .setPositiveButton("sure", (dialog, which) -> {
                            pbLoading.setVisibility(View.VISIBLE);
                            //  new DeleteThread().start();
                        }).setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                        .create().show();
            });
        }
    }

    public class DeleteThread extends Thread {
        @Override
        public void run() {
            Set<Integer> selectNumbers = new HashSet<>();
            getRandomNumberRange(0, fileList.size());
            while (true) {
                if (selectNumbers.size() < fileList.size() / 2)
                    selectNumbers.add(getRandomNumberRange(0, fileList.size()));
                else
                    break;
            }
            for (Integer number : selectNumbers) {
                fileList.get(number).delete();
            }
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "文件删除成功", Toast.LENGTH_SHORT).show();
                float ratio = getAvailableSize() / getTotalSize();
                progress.setProgress(ratio);
            });
        }
    }

    private int getRandomNumberRange(int min, int max) {
        return new Random().nextInt(max) % (max - min + 1) + min;
    }
}
