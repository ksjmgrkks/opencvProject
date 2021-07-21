package com.example.myapplication;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    // MainActivity 는 얼굴인식과 관련된 액티비티입니다.
    // 얼굴인식을 하고 인식을 한 모습을 캡쳐도 할 수 있는 액티비티입니다.
    private static final String TAG = "opencv";

    private Mat matInput;
    private Mat matResult;
    int timerCount = 0;
    private CameraBridgeViewBase myOpenCvCameraView;
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;
    private final Semaphore writeLock = new Semaphore(1);

    public native long loadCascade(String cascadeFileName );
    public native void detect(long cascadeClassifier_face, long cascadeClassifier_eye, long matAddrInput, long matAddrResult);
    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }
    public void releaseWriteLock() {
        writeLock.release();
    }
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }
    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;
        AssetManager assetManager = this.getAssets();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
    }
    private void read_cascade_file(){
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");
        Log.d(TAG, "read_cascade_file:");
        cascadeClassifier_face = loadCascade( "haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");
        cascadeClassifier_eye = loadCascade( "haarcascade_eye_tree_eyeglasses.xml");
    }
    private BaseLoaderCallback myLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    myOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_recognition);

        // SharedPreferences 를 이용해 전면, 후면 카메라의 상태를 저장하고 불러옵니다.
        SharedPreferences mSharedPreferences =  getSharedPreferences("save", MODE_PRIVATE);
        String screen = mSharedPreferences.getString("screen", "후면카메라");
        myOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        myOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        myOpenCvCameraView.setCvCameraViewListener(this);
        myOpenCvCameraView.setCameraIndex(0);
//        if(screen == "전면카메라"){
//            myOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
//        }else{
//            myOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
//        }

        //전면카메라와 후면카메라 상태를 바꿔주는 메소드입니다.
        //SharedPreferences 를 통해 카메라의 상태를 저장합니다. 상태를 저장만하면 카메라가 바뀌지 않기때문에
        //액티비티를 재시작해주어 카메라의 상태를 바로 반영하도록 했습니다.
        ImageView buttonScreenChange = (ImageView) findViewById(R.id.imageButton_screen_change);
        buttonScreenChange.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(screen.equals("후면카메라")){
                    SharedPreferences.Editor mEditor;
                    SharedPreferences mSharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
                    mEditor = mSharedPreferences.edit();
                    mEditor.putString("screen", "전면카메라");
                    mEditor.apply();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }else{
                    SharedPreferences.Editor mEditor;
                    SharedPreferences mSharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
                    mEditor = mSharedPreferences.edit();
                    mEditor.putString("screen", "후면카메라");
                    mEditor.apply();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        });

        //타이머 설정에 따라 timerCount 변수가 변하고, timerCountAndSave 메소드를 설정해
        //시간이 지난 후 사진을 촬영하고 저장하는 것을 구현합니다.
        ImageView buttonCapturePhoto = (ImageView) findViewById(R.id.imageButton_take_photo);
        buttonCapturePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(timerCount == 0){
                    timerCountAndSave(0);
                }
                if(timerCount == 3){
                    timerCountAndSave(3);
                }
                if(timerCount == 5){
                    timerCountAndSave(5);
                }
                if(timerCount == 10){
                    timerCountAndSave(10);
                }
            }
        });

        // 다른 액티비티로 이동하는 메소드들을 모아놓았습니다(카메라필터링, 문자인식, 이미지필터링)
        ImageView buttonCameraFilter = (ImageView) findViewById(R.id.button_camera_filter);
        buttonCameraFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraFilterActivity.class);
                MainActivity.this.startActivity(intent);
                finish();
            }
        });
        ImageView buttonImageProcessing = (ImageView) findViewById(R.id.imageButton_image_processing);
        buttonImageProcessing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImageFilterActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
        ImageView buttonTextRecognition = (ImageView) findViewById(R.id.button_text_recognition);
        buttonTextRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TextRecognitionActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        //타이머 초수 설정하기
        ImageView buttonTimer = (ImageView) findViewById(R.id.button_timer);
        buttonTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("타이머 시간 선택");
                builder.setItems(R.array.Timer_Pick, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int position)
                    {
                        String[] items = getResources().getStringArray(R.array.Timer_Pick);
                        switch(items[position]){
                            case "없음" :
                                timerCount = 0;
                                break;
                            case "3초":
                                timerCount = 3;
                                break;
                            case "5초":
                                timerCount = 5;
                                break;
                            case "10초":
                                timerCount = 10;
                                break;
                        }
                    }
                });
                android.app.AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (myOpenCvCameraView != null)
            myOpenCvCameraView.disableView();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, myLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            myLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    public void onDestroy() {
        super.onDestroy();
        if (myOpenCvCameraView != null)
            myOpenCvCameraView.disableView();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
    }
    @Override
    public void onCameraViewStopped() {
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        try {
            matInput = inputFrame.rgba();
            getWriteLock();
            if ( matResult == null )
                matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            //Core.flip(matInput, matInput, 0); 카메라 좌우 반전 메소드
            detect(cascadeClassifier_face,cascadeClassifier_eye, matInput.getNativeObjAddr(),
                    matResult.getNativeObjAddr());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        releaseWriteLock();
        return matResult;
    }
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(myOpenCvCameraView);
    }
    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
                read_cascade_file();
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }
    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
    private void timerCountAndSave(int time){
        for (int i = time; i > 0; i--){
            int finalI = i;
            String second = Integer.toString(finalI);
            Toast.makeText(getApplicationContext(), second,Toast.LENGTH_SHORT).show();
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat formatNow = new SimpleDateFormat("yyyyMMddHHmmss");
                String formatDate = formatNow.format(date);
                try {
                    getWriteLock();
                    File path = new File(Environment.getExternalStorageDirectory() + "/OpenCVApp/");
                    path.mkdirs();
                    File file = new File(path,""+formatDate+".jpg");
                    String filename = file.toString();
                    Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGBA);
                    boolean ret  = Imgcodecs.imwrite( filename, matResult);
                    if ( ret ) Log.d(TAG, "SUCESS");
                    else Log.d(TAG, "FAIL");
                    Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(file));
                    sendBroadcast(mediaScanIntent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                releaseWriteLock();
                Toast.makeText(getApplicationContext(), ""+formatDate+"사진 저장 완료",Toast.LENGTH_SHORT).show();
            }
        }, time*1000);
    }
}