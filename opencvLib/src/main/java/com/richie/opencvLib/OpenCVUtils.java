package com.richie.opencvLib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.barcode.BarcodeDetector;
import org.opencv.wechat_qrcode.WeChatQRCode;

import java.io.*;

/**
 * Created by lylaut on 2022/04/18
 */
public class OpenCVUtils {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static WeChatQRCode mWeChatQRCode;
    @SuppressLint("StaticFieldLeak")
    private static BarcodeDetector mBarcodeDetector;

    public static WeChatQRCode getWeChatQRCode() {
        return mWeChatQRCode;
    }

    public static BarcodeDetector getBarcodeDetector() {
        return mBarcodeDetector;
    }

    private static final LoaderCallbackInterface openCVLoaderCallback = new LoaderCallbackInterface() {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV", "OpenCV loaded successfully");
                String detectTxt = OpenCVUtils.copyFile(mContext, "detect.prototxt");
                String detectModel = OpenCVUtils.copyFile(mContext, "detect.caffemodel");
                String srTxt = OpenCVUtils.copyFile(mContext, "sr.prototxt");
                String srModel = OpenCVUtils.copyFile(mContext, "sr.caffemodel");
                mWeChatQRCode = new WeChatQRCode(detectTxt, detectModel, srTxt, srModel);
                mBarcodeDetector = new BarcodeDetector(srTxt, srModel);
            }
        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {
            Log.d("OpenCV", "onPackageInstall " + operation);
        }
    };

    public static void init(Context context) {
        mContext = context;
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, openCVLoaderCallback);
            if (!success)
                Log.e("OpenCV", "Asynchronous initialization failed!");
            else
                Log.d("OpenCV", "Asynchronous initialization succeeded!");
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            openCVLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * 拷贝文件到指定目录
     * @param context context
     * @param filename filename
     * @return 返回指定目录文件路径
     */
    private static String copyFile(Context context, String filename) {
        InputStream in = null;
        FileOutputStream out = null; // path为指定目录
        String destDir = context.getApplicationContext().getFilesDir().getAbsolutePath();
        String path = destDir + "/" + filename; // data/data目录
        File file = new File(path);
        if (!file.exists()) {
            try {
                in = context.getAssets().open(filename); // 从assets目录下复制
                out = new FileOutputStream(file);
                int length = -1;
                byte[] buf = new byte[1024];
                while ((length = in.read(buf)) != -1) {
                    out.write(buf, 0, length);
                }
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return path;
    }

    public static Bitmap saveAndGetBitmap(Context context, Bitmap bitmap) {
        String filePath = context.getApplicationContext().getFilesDir().getAbsolutePath() + File.separator + "opencv_capture_view.jpg";
        try {
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            FileInputStream fis = new FileInputStream(filePath);
            Bitmap resBitmap  = BitmapFactory.decodeStream(fis);
            fis.close();
            return resBitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
