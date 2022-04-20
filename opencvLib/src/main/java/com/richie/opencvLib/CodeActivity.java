package com.richie.opencvLib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.*;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.richie.opencvLib.databinding.ActivityZxingCodeBinding;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.*;

/**
 * Created by lylaut on 2022/04/07
 */
public class CodeActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_PHOTO_PICK = 2;
    private ActivityZxingCodeBinding binding;
    private boolean torchOpen = false;
    private int navigationBarHeight = 0;
    private long scanTime = -1;
    private long qrCodeScanCount = 0;

    public interface CodeActivityResultListener {
        void callback(List<String> results, String message);
    }

    private static CodeActivityResultListener mResultListener;

    public static void setResultListener(CodeActivityResultListener resultListener) {
        CodeActivity.mResultListener = resultListener;
    }

    private void playComplete() {
        MediaPlayer mp = new MediaPlayer();
        try {
            AssetFileDescriptor afd = this.getAssets().openFd("scan_completed.mp3");
            mp.setDataSource(afd.getFileDescriptor() , afd.getStartOffset(), afd.getLength());
            mp.prepare();
            mp.start();
            afd.close();
        } catch (Exception e) {
            //
        }
    }

    private void handleRegMat(Mat rgbMat, boolean fromPhoto) {
        if (isFinishing()) {
            rgbMat.release();
            return;
        }
        Mat mGray = new Mat();
        Imgproc.cvtColor(rgbMat, mGray, Imgproc.COLOR_BGR2GRAY);
        rgbMat.release();

        if (isFinishing()) {
            mGray.release();
            return;
        }

        List<String> barcodeInfos = new ArrayList<>();
        List<Integer> barcodeTypes = new ArrayList<>();
        if (OpenCVUtils.getBarcodeDetector().detectAndDecode(mGray, barcodeInfos, barcodeTypes)) {
            if (!fromPhoto) {
                long scanTime = System.currentTimeMillis();
                if (CodeActivity.this.scanTime != -1 && scanTime < CodeActivity.this.scanTime + 200) {
                    return;
                }
                CodeActivity.this.scanTime = scanTime;
            }

            qrCodeScanCount = 0;

            runOnUiThread(() -> {
                binding.cameraView.stop();
                playComplete();
                finish();
            });
            if (CodeActivity.mResultListener != null) {
                CodeActivity.mResultListener.callback(barcodeInfos, null);
            }
            return;
        }

        List<Mat> matList = new ArrayList<>();
        List<String> strings = OpenCVUtils.getWeChatQRCode().detectAndDecode(mGray, matList);
        if (strings != null && strings.size() > 0) {
            if (fromPhoto || ++qrCodeScanCount > 5) {
                qrCodeScanCount = 0;

                for (Mat mat : matList) {
                    MatOfPoint pt = new MatOfPoint(mat);
                    System.out.println("size: " + pt.toList());
                }
                runOnUiThread(() -> {
                    binding.cameraView.stop();
                    playComplete();
                    finish();
                });
                if (CodeActivity.mResultListener != null) {
                    CodeActivity.mResultListener.callback(strings, null);
                }
            }
            for (Mat mat : matList) {
                mat.release();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Window window = getWindow();
        View decorView = window.getDecorView();
        decorView.setOnApplyWindowInsetsListener((v, insets) -> {
            WindowInsets defaultInsets = v.onApplyWindowInsets(insets);

            navigationBarHeight = defaultInsets.getSystemWindowInsetTop();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.backBtn.getLayoutParams();
            layoutParams.setMargins(0, navigationBarHeight + dp2px(15), 0, 0);
            binding.backBtn.setLayoutParams(layoutParams);

            return defaultInsets.replaceSystemWindowInsets(
                    defaultInsets.getSystemWindowInsetLeft(),
                    0,
                    defaultInsets.getSystemWindowInsetRight(),
                    defaultInsets.getSystemWindowInsetBottom());
        });
        ViewCompat.requestApplyInsets(decorView);
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        super.onCreate(savedInstanceState);
        binding = ActivityZxingCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cameraView.setListener(rgbMat -> handleRegMat(rgbMat, false));

        binding.backBtn.setOnClickListener(view -> {
            CodeActivity.this.finish();
            if (CodeActivity.mResultListener != null) {
                CodeActivity.mResultListener.callback(null, "取消");
            }
        });

        binding.torchBtn.setOnClickListener(view -> binding.cameraView.openTorch(torchOpen = !torchOpen));

        binding.photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                CodeActivity.this.startActivityForResult(intent, REQUEST_PHOTO_PICK);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        } else {
            binding.cameraView.setCameraPermissionGranted();
            binding.cameraView.enableView();
            configAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (CodeActivity.mResultListener != null) {
            CodeActivity.mResultListener.callback(null, "取消");
        }
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private int dp2px(float dpValue) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void configAnimation() {
        // 模拟的mPreviewView的左右上下坐标坐标
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int bottom = outMetrics.heightPixels;


        AnimationSet animationSet = new AnimationSet(true);
      /*
          Animation还有几个方法
          setFillAfter(boolean fillAfter)
          如果fillAfter的值为真的话，动画结束后，控件停留在执行后的状态
          setFillBefore(boolean fillBefore)
          如果fillBefore的值为真的话，动画结束后，控件停留在动画开始的状态
          setStartOffset(long startOffset)
          设置动画控件执行动画之前等待的时间
          setRepeatCount(int repeatCount)
          设置动画重复执行的次数
       */
        TranslateAnimation translateAnimation = new TranslateAnimation(
                //X轴初始位置
                Animation.RELATIVE_TO_SELF, 0.0f,
                //X轴移动的结束位置
                Animation.RELATIVE_TO_SELF,0f,
                //y轴开始位置
                Animation.RELATIVE_TO_SELF,-0.5f,
                //y轴移动后的结束位置
                Animation.RELATIVE_TO_SELF,0.5f);

        //3秒完成动画
        translateAnimation.setDuration(3000);
        translateAnimation.setRepeatMode(Animation.RESTART);
        translateAnimation.setRepeatCount(Animation.INFINITE);
        //将AlphaAnimation这个已经设置好的动画添加到 AnimationSet中
        animationSet.addAnimation(translateAnimation);

        // 播放动画
        binding.scanHorizontalLineView.setAnimation(animationSet);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            } else {
                binding.cameraView.setCameraPermissionGranted();
                binding.cameraView.enableView();
                configAnimation();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_PHOTO_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(CodeActivity.this.getContentResolver(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bitmap != null) {
                        Mat bitmapMat = new Mat();
                        Utils.bitmapToMat(bitmap, bitmapMat);
                        bitmap.recycle();
                        handleRegMat(bitmapMat, true);
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
