package com.study.directory.sdcarddirectory;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter<String> dirListAdapter;
    private final int SDCARD_PERMISSION = 0;
    private final int rSDCARD_PERMISSION = 1;
    private String sdPath = null;
    private ArrayList<String> pathList;
    private MainActivity that = this;
    private String mCurrent = "";
    private ActionBar ab;
    Stack<String> before;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mCurrent = savedInstanceState.getString("path");
        }
        initWidget();
        isMountedSDcard();
        requestPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("path", mCurrent);
    }

    @Override
    public void onBackPressed() {
        if (mCurrent.equals(sdPath)) {
            Toast.makeText(this, "더 이상 뒤로 갈 수 없습니다", Toast.LENGTH_SHORT).show();
        } else {
            mCurrent = before.pop();
            refreshFileList();
        }
    }

    /**
     * 퍼미션 허가
     */
    public void requestPermission() {
        ActivityCompat.requestPermissions(that, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SDCARD_PERMISSION) ;
        ActivityCompat.requestPermissions(that, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, rSDCARD_PERMISSION) ;
    }

    /**
     * 외장 SD 카드 디렉토리 구해오는 메소드
     *
     * @return storageDirectories
     */
    public String[] getExternalStorageDirectories() {

        List<String> results = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
//            File[] externalDirs = getExternalFilesDirs(null);
            File[] externalDirs = getExternalFilesDirs(null);

            for (File file : externalDirs) {
                String path = file.getPath().split("/Android")[0];

                boolean addPath = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addPath = Environment.isExternalStorageRemovable(file);
                } else {
                    addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                }

                if (addPath) {
                    results.add(path);
                }
            }
        }

        if (results.isEmpty()) { //Method 2 for all versions
            // better variation of: http://stackoverflow.com/a/40123073/5002496
            String output = "";
            try {
                final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
                        .redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    output = output + new String(buffer);
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if (!output.trim().isEmpty()) {
                String devicePoints[] = output.split("\n");
                for (String voldPoint : devicePoints) {
                    results.add(voldPoint.split(" ")[2]);
                }
            }
        }

        //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                    results.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
                    results.remove(i--);
                }
            }
        }

        String[] storageDirectories = new String[results.size()];
        for (int i = 0; i < results.size(); ++i) storageDirectories[i] = results.get(i);

        return storageDirectories;
    }
//end

    /**
     * 위젯 init
     */
    private void initWidget() {
        ab = getSupportActionBar();
        before = new Stack<String>();
        ListView dirList = (ListView) findViewById(R.id.dirList);
        pathList = new ArrayList<String>();
        dirListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pathList);
        dirList.setAdapter(dirListAdapter);
        dirList.setOnItemClickListener(onItemClickListener);
    }

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String name = pathList.get(position);
            if (name.startsWith("[") && name.endsWith("]")) {
                name = name.substring(1, name.length() - 1);
            } else {
                File f = new File(mCurrent+ "/" + name);
//                openFile(f);

            }
            before.push(mCurrent);
            String thisPath = mCurrent + "/" + name;
            File f = new File(thisPath);
            if (f.isDirectory()) {
                mCurrent = thisPath;
            }
            refreshFileList();
        }
    };

    /**
     * sd카드 마운트 여부
     */
    private void isMountedSDcard() {

        String sdcard = Environment.getExternalStorageState();
        if (!sdcard.equals(Environment.MEDIA_MOUNTED)) {
            // SD카드가 마운트되어있지 않음
            sdPath = Environment.getRootDirectory().getAbsolutePath();

        } else {
            // SD카드가 마운트되어있음
            if (mCurrent.equals("")) {
                sdPath = getExternalStorageDirectories()[0];
                mCurrent = sdPath;
                before.push(mCurrent);
            }
        }
    }

    /**
     * 리스트 업데이트
     */
    private void refreshFileList() {
        pathList.clear();
        File current = new File(mCurrent);
        String[] files = current.list();
        if (files != null) {
            for (String file : files) {
                String path = mCurrent + "/" + file;
                String name = "";
                File f = new File(path);
                if (f.isDirectory()) {
                    name = "[" + file + "]";
                } else {
                    name = file;
                }
                pathList.add(0, name);
            }
        }
        dirListAdapter.notifyDataSetChanged();
        ab.setTitle("SD카드 파일목록");
        ab.setSubtitle(mCurrent);
    }
    /** * 파일의 확장자 조회 *
     *
     *  @param fileStr
     *  @return */
    public static String getExtension(String fileStr) { return fileStr.substring(fileStr.lastIndexOf(".") + 1, fileStr.length()); }

    private void openFile(File file){
        String filetype = getExtension(file.getName());
        String mimetype = MimeTypeUtil.getType(filetype);
        if(mimetype==null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setDataAndType(Uri.fromFile(file), mimetype);
            that.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshFileList();
        } else {
//            Toast.makeText(that, "권한 설정을 허용해주세요", Toast.LENGTH_SHORT).show();
        }
    }
}
