package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import static android.Manifest.permission.CAMERA;

public class CameraFilterActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    // 카메라 화면에 필터를 적용하고, 사진을 찍을 수 있는 액티비티입니다.
    private final Semaphore writeLock = new Semaphore(1);
    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }
    public void releaseWriteLock() {
        writeLock.release();
    }
    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;
    // timerCount 는 타이머 초수를 나타냅니다. 타이머의 초기값은 0초입니다.
    int timerCount = 0;
    //버튼을 누를 때마다 바뀌는 정수 값입니다.
    //todo: 어떤 부분에 대한 주석인지 전반적인 설명이 필요하다.
    //예를 들어 buttonGray 를 누르면, setGray 변수는 1이되고, 나머지 변수는 0이 됩니다.
    //그 변화를 onCameraFrame 안의 C++ 필터 함수가 반영하고, 해당하는 카메라 필터를 적용하게됩니다.
    //todo : 변수 명에 대한 고민이 더 필요하다. setBasic, setGray 등
    private int setBasic;
    private int setGray;
    private int setBGR;
    private int setYUV;
    private int setLuv;
    //todo : 이미지뷰면 이미지뷰로 변수명을 설정해야 혼란이 없다.
    private ImageView buttonBGR;
    private ImageView buttonGray;
    private ImageView buttonBasic;
    private ImageView buttonYUV;
    private ImageView buttonLuv;
    private ImageView buttonFaceRecognition;
    private CameraBridgeViewBase myOpenCvCameraView;
    //카메라 필터에 해당하는 native(C++) 함수입니다.
    //필터에 개수에 따라 함수 또한 각각 만들어 주었습니다.(BGR,RGB,YUV,Luv)
    //JNI 를 이용해 불러올 수 있습니다.
    public native void ConvertRGBAtoBGR(long matAddrInput, long matAddrResult);
    public native void ConvertRGBAtoRGB(long matAddrInput, long matAddrResult);
    public native void ConvertRGBtoYUV(long matAddrInput, long matAddrResult);
    public native void ConvertRGBtoLuv(long matAddrInput, long matAddrResult);
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }
    //todo : 코드하나하나는 아니더라도 대충 어떤 역할을 하는지 정도는 알고 넘어가야함
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
        setContentView(R.layout.activity_camera_filter);

        // SharedPreferences 를 이용해 전면, 후면 카메라의 상태를 저장하고 불러옵니다.
        // todo : string 선언해줘야함
        SharedPreferences mSharedPreferences =  getSharedPreferences("save", MODE_PRIVATE);
        String screen = mSharedPreferences.getString("screen", "후면카메라");
        myOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        myOpenCvCameraView.setCvCameraViewListener(this);
        //todo : 조건문을 통해 else 가 어떤 코드인지 알 수 없음
        if(screen == "전면카메라"){
            myOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        }else{
            myOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        }

        //전면카메라와 후면카메라 상태를 바꿔주는 메소드입니다.
        //SharedPreference 를 통해 카메라의 상태를 저장합니다. 상태를 저장만하면 카메라가 바뀌지 않기때문에
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

        //buttonGray 를 누르면, setGray 변수는 1이되고, 나머지 변수는 0이 됩니다.
        //그 변화를 onCameraFrame 안의 C++ 필터 함수가 반영하고, 해당하는 카메라 필터를 적용하게됩니다.
        buttonBasic = (ImageView) findViewById(R.id.button_basic);
        buttonBasic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "필터 기본",Toast.LENGTH_SHORT).show();
                setBasic = 1;
                setBGR = 0;
                setGray = 0;
                setLuv = 0;
                setYUV = 0;
            }
        });
        buttonBGR = (ImageView) findViewById(R.id.button_BGR);
        buttonBGR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "필터 BGR",Toast.LENGTH_SHORT).show();
                setBasic = 0;
                setBGR = 1;
                setGray = 0;
                setLuv = 0;
                setYUV = 0;
            }
        });
        buttonGray = (ImageView) findViewById(R.id.button_gray);
        buttonGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "필터 gray",Toast.LENGTH_SHORT).show();
                setBasic = 0;
                setBGR = 0;
                setGray = 1;
                setLuv = 0;
                setYUV = 0;
            }
        });
        buttonLuv = (ImageView) findViewById(R.id.button_Luv);
        buttonLuv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "필터 Luv",Toast.LENGTH_SHORT).show();
                setBasic = 0;
                setBGR = 0;
                setGray = 0;
                setLuv = 1;
                setYUV = 0;
            }
        });
        buttonYUV = (ImageView) findViewById(R.id.button_HSV);
        buttonYUV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "필터 YUV",Toast.LENGTH_SHORT).show();
                setBasic = 0;
                setBGR = 0;
                setGray = 0;
                setLuv = 0;
                setYUV = 1;
            }
        });


        // 다른 액티비티로 이동하는 메소드들을 모아놓았습니다(얼굴인식, 문자인식, 이미지필터링)
        ImageView buttonImageFilter = (ImageView) findViewById(R.id.imageButton_image_processing);
        buttonImageFilter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(CameraFilterActivity.this, ImageFilterActivity.class);
                CameraFilterActivity.this.startActivity(intent);
            }
        });
        ImageView buttonTextRecognition = (ImageView) findViewById(R.id.button_text_recognition);

        buttonTextRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraFilterActivity.this, TextRecognitionActivity.class);
                CameraFilterActivity.this.startActivity(intent);
            }
        });
        buttonFaceRecognition = (ImageView) findViewById(R.id.button_face_recognition);
        buttonFaceRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraFilterActivity.this, MainActivity.class);
                CameraFilterActivity.this.startActivity(intent);
                finish();
            }
        });

        //타이머 초수 설정하기
        ImageView buttonTimer = (ImageView) findViewById(R.id.button_timer);
        buttonTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(CameraFilterActivity.this);
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
        //타이머 설정에 따라 timerCount 변수가 변하고, timerCountAndSave 메소드를 설정해
        //시간이 지난 후 사진을 촬영하고 저장하는 것을 구현합니다.
        //todo: 0,3,5,10 에 변수를 활용하는 것이 알기 편하다. 변수를 이용할 수 있으면 최대한 많이 이용해야한다.
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
        matInput = inputFrame.rgba();
        if (setBasic == 1)
        {
            matInput = inputFrame.rgba();
        }
        if (setGray == 1)
        {
            matInput = inputFrame.gray();
        }
        if (setBGR == 1)
        {
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            ConvertRGBAtoBGR(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
            return matResult;
        }
        if (setLuv == 1)
        {
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            ConvertRGBtoLuv(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
            return matResult;
        }
        if (setYUV == 1)
        {
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            ConvertRGBtoYUV(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
            return matResult;
        }
        if ( matResult == null )
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
        ConvertRGBAtoRGB(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        return matResult;
    }
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(myOpenCvCameraView);
    }
    //여기서부턴 카메라 퍼미션과 관련 메소드입니다. 필터와는 직접적인 관련이 없습니다.
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
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
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder( CameraFilterActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
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