#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

using namespace cv;
using namespace std;

#include <jni.h>

#include <opencv2/opencv.hpp>

#include <android/log.h>


using namespace cv;

using namespace std;

float resize(Mat img_src, Mat &img_resize, int resize_width){
    float scale = resize_width / (float)img_src.cols ;
    if (img_src.cols > resize_width) {
        int new_height = cvRound(img_src.rows * scale);
        resize(img_src, img_resize, Size(resize_width, new_height));
    }
    else {
        img_resize = img_src;
    }
    return scale;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_myapplication_MainActivity_loadCascade(JNIEnv *env, jobject thiz,
                                                        jstring cascade_file_name) {
    const char *nativeFileNameString = env->GetStringUTFChars(cascade_file_name, 0);


    string baseDir("/storage/emulated/0/");

    baseDir.append(nativeFileNameString);

    const char *pathDir = baseDir.c_str();


    jlong ret = 0;

    ret = (jlong) new CascadeClassifier(pathDir);

    if (((CascadeClassifier *) ret)->empty()) {

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);

    }

    else

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);



    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);


    return ret;
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_MainActivity_detect(JNIEnv *env, jobject thiz,
                                                   jlong cascade_classifier_face,
                                                   jlong cascade_classifier_eye,
                                                   jlong mat_addr_input, jlong mat_addr_result) {
    Mat &img_input = *(Mat *) mat_addr_input;

    Mat &img_result = *(Mat *) mat_addr_result;


    img_result = img_input.clone();


    std::vector<Rect> faces;

    Mat img_gray;


    cvtColor(img_input, img_gray, COLOR_BGR2GRAY);

    equalizeHist(img_gray, img_gray);


    Mat img_resize;

    float resizeRatio = resize(img_gray, img_resize, 640);


    //-- Detect faces

    ((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );



    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",

                        (char *) "face %d found ", faces.size());


    for (int i = 0; i < faces.size(); i++) {

        double real_facesize_x = faces[i].x / resizeRatio;

        double real_facesize_y = faces[i].y / resizeRatio;

        double real_facesize_width = faces[i].width / resizeRatio;

        double real_facesize_height = faces[i].height / resizeRatio;


        Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);

        ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360,

                Scalar(255, 0, 255), 30, 8, 0);

        string glassesImage="sunglasses.png";
        Mat glasses=imread(glassesImage, IMREAD_UNCHANGED);
        Mat result;

        Rect face_area(real_facesize_x, real_facesize_y, real_facesize_width,real_facesize_height);

        Mat faceROI = img_gray( face_area );

        std::vector<Rect> eyes;


        //-- In each face, detect eyes

        ((CascadeClassifier *) cascade_classifier_eye)->detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );


        for ( size_t j = 0; j < eyes.size(); j++ )

        {

            Point eye_center( real_facesize_x + eyes[j].x + eyes[j].width/2, real_facesize_y + eyes[j].y + eyes[j].height/2 );

            int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );

            circle( img_result, eye_center, radius, Scalar( 255, 0, 0 ), 30, 8, 0 );

        }

    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_CameraFilterActivity_ConvertRGBAtoBGR(JNIEnv *env, jobject thiz,
                                                                     jlong mat_addr_input,
                                                                     jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2BGR);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_CameraFilterActivity_ConvertRGBAtoRGB(JNIEnv *env, jobject thiz,
                                                                     jlong mat_addr_input,
                                                                     jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2RGB);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_CameraFilterActivity_ConvertRGBtoYUV(JNIEnv *env, jobject thiz,
                                                                    jlong mat_addr_input,
                                                                    jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGB2YUV);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_CameraFilterActivity_ConvertRGBtoLuv(JNIEnv *env, jobject thiz,
                                                                    jlong mat_addr_input,
                                                                    jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGB2Luv);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_ImageFilterActivity_imageprocessingGray(JNIEnv *env, jobject thiz,
                                                                   jlong input_image,
                                                                   jlong output_image) {
    Mat &img_input = *(Mat *) input_image;

    Mat &img_output = *(Mat *) output_image;



    cvtColor( img_input, img_output, COLOR_RGB2GRAY);


//    blur( img_output, img_output, Size(th1,th1) );

//    Canny( img_output, img_output, th1, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_ImageFilterActivity_imageprocessingLuv(JNIEnv *env, jobject thiz,
                                                                      jlong input_image,
                                                                      jlong output_image) {
    Mat &img_input = *(Mat *) input_image;

    Mat &img_output = *(Mat *) output_image;



    cvtColor( img_input, img_output, COLOR_RGB2Luv);

}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_ImageFilterActivity_imageprocessingBGR(JNIEnv *env, jobject thiz,
                                                                      jlong input_image,
                                                                      jlong output_image) {
    Mat &img_input = *(Mat *) input_image;

    Mat &img_output = *(Mat *) output_image;



    cvtColor( img_input, img_output, COLOR_RGB2BGR);

}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_ImageFilterActivity_imageprocessingHSV(JNIEnv *env, jobject thiz,
                                                                      jlong input_image,
                                                                      jlong output_image) {
    Mat &img_input = *(Mat *) input_image;

    Mat &img_output = *(Mat *) output_image;



    cvtColor( img_input, img_output, COLOR_RGB2HSV);

}extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_ImageFilterActivity_imageprocessingYUV(JNIEnv *env, jobject thiz,
                                                                      jlong input_image,
                                                                      jlong output_image) {
    Mat &img_input = *(Mat *) input_image;

    Mat &img_output = *(Mat *) output_image;

    cvtColor( img_input, img_output, COLOR_RGB2YUV);
}