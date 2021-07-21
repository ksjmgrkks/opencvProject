package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

public class ImageFilterActivity extends AppCompatActivity {
    //갤러리에서 이미지를 불러오고, 필터를 적용하는 액티비티입니다.
    //필터를 적용한 이미지를 저장버튼을 눌러 저장할 수 있습니다.
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }
    ImageView imageVIewInput;
    ImageView imageVIewOuput;

    private boolean myFlashOn;
    private ImageButton myImageButtonFlashOff;

    private CameraManager myCameraManager;
    private String myCameraId;

    private Mat matInput;
    private Mat matResult;

    private static final String TAG = "opencv";
    private final int GET_GALLERY_IMAGE = 200;

    boolean isReady = false;

    private int setGray;
    private int setBGR;
    private int setHSV;
    private int setLuv;
    private int setYUV;

    private final Semaphore writeLock = new Semaphore(1);
    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }
    public void releaseWriteLock() {
        writeLock.release();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            delayedFinish();
            return;
        }
        myCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        myImageButtonFlashOff = findViewById(R.id.image_button_flash_on_off);
        myImageButtonFlashOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashlight();
                myImageButtonFlashOff.setImageResource(myFlashOn ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            }
        });

        imageVIewInput = (ImageView)findViewById(R.id.imageViewInput);
        imageVIewOuput = (ImageView)findViewById(R.id.imageViewOutput);
        ImageButton ImageButtonSave = (ImageButton)findViewById(R.id.imageButton_save);
        ImageView ButtonGray = (ImageView)findViewById(R.id.button_gray);
        ButtonGray.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setBGR = 0;
                setGray = 1;
                setLuv = 0;
                setHSV = 0;
                setYUV = 0;
                imageprocess_and_showResult();
            }
        });
        ImageView ButtonLuv = (ImageView)findViewById(R.id.button_Luv);
        ButtonLuv.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setBGR = 0;
                setGray = 0;
                setLuv = 1;
                setHSV = 0;
                setYUV = 0;
                imageprocess_and_showResult();
            }
        });
        ImageView ButtonBGR = (ImageView)findViewById(R.id.button_BGR);
        ButtonBGR.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setBGR = 1;
                setGray = 0;
                setLuv = 0;
                setHSV = 0;
                setYUV = 0;
                imageprocess_and_showResult();
            }
        });
        ImageView ButtonHSV = (ImageView)findViewById(R.id.button_HSV);
        ButtonHSV.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setBGR = 0;
                setGray = 0;
                setLuv = 0;
                setHSV = 1;
                setYUV = 0;
                imageprocess_and_showResult();
            }
        });
        ImageView ButtonYUV = (ImageView)findViewById(R.id.button_YUV);
        ButtonYUV.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setBGR = 0;
                setGray = 0;
                setLuv = 0;
                setHSV = 0;
                setYUV = 1;
                imageprocess_and_showResult();
            }
        });
        imageVIewInput.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("IntentReset")
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, GET_GALLERY_IMAGE);
            }
        });
        ImageButtonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat formatNow = new SimpleDateFormat("yyyyMMddHHmmss");
                String formatDate = formatNow.format(date);

                try {
                    getWriteLock();

                    File path = new File(Environment.getExternalStorageDirectory() + "/OpenCVApp/");
                    path.mkdirs();
                    File file = new File(path,""+formatDate+".jpg");
                    //todo: 날짜와 시간으로 저장하기
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
        });

        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        isReady = true;
    }
    //갤러리에서 불러온 이미지에 필터를 적용하는 native(C++) 함수입니다.
    //JNI 를 이용해 불러올 수 있습니다.
    public native void imageprocessingGray(long inputImage, long outputImage);
    public native void imageprocessingLuv(long inputImage, long outputImage);
    public native void imageprocessingBGR(long inputImage, long outputImage);
    public native void imageprocessingHSV(long inputImage, long outputImage);
    public native void imageprocessingYUV(long inputImage, long outputImage);

    private void imageprocess_and_showResult() {
        if (isReady==false) return;
        if (matResult == null)
            matResult = new Mat();
        if(setGray == 1){
            imageprocessingGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        if(setLuv == 1){
            imageprocessingLuv(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        if(setBGR == 1){
            imageprocessingBGR(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        if(setHSV == 1){
            imageprocessingHSV(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        if(setYUV == 1){
            imageprocessingYUV(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        }
        Bitmap bitmapOutput = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, bitmapOutput);
        imageVIewOuput.setImageBitmap(bitmapOutput);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GET_GALLERY_IMAGE) {
            if (data.getData() != null) {
                Uri uri = data.getData();
                try {
                    String path = getRealPathFromURI(uri);
                    int orientation = getOrientationOfImage(path); // 런타임 퍼미션 필요
                    Bitmap temp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    Bitmap bitmap = getRotatedBitmap(temp, orientation);
                    imageVIewInput.setImageBitmap(bitmap);

                    matInput = new Mat();
                    Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Utils.bitmapToMat(bmp32, matInput);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToFirst();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        return cursor.getString(column_index);
    }
    // 출처 - http://snowdeer.github.io/android/2016/02/02/android-image-rotation/
    public int getOrientationOfImage(String filepath) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            Log.d("@@@", e.toString());
            return -1;
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        if (orientation != -1) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
        }
        return 0;
    }
    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception {
        if(bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }
    // 퍼미션 코드
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.WRITE_EXTERNAL_STORAGE"};
    private boolean hasPermissions(String[] permissions) {
        int ret = 0;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            ret = checkCallingOrSelfPermission(perms);
            if (!(ret == PackageManager.PERMISSION_GRANTED)){
                //퍼미션 허가 안된 경우
                return false;
            }
        }
        //모든 퍼미션이 허가된 경우
        return true;
    }
    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch(permsRequestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!writeAccepted )
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }
                    }
                }
                break;
        }
    }
    private void showDialogforPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(  ImageFilterActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }
            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }
    private void delayedFinish() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3500);
    }
    void flashlight(){
        if(myCameraId == null){
            try{
                for(String id : myCameraManager.getCameraIdList()){
                    CameraCharacteristics c = myCameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                    if(flashAvailable != null && flashAvailable
                            && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        myCameraId = id;
                        break;
                    }
                }
            }catch (CameraAccessException e){
                Log.d("디버그태그", "예외발생1");
                myCameraId = null;
                e.printStackTrace();
                return;
            }
        }
        myFlashOn = !myFlashOn;
        try {
            myCameraManager.setTorchMode(myCameraId, myFlashOn);
        } catch (CameraAccessException e) {
            Log.d("디버그태그", "예외싫어");
            e.printStackTrace();
        }
    }
}