package com.richie.androidwechatqrdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.richie.androidwechatqrdemo.databinding.ActivityMainBinding;
import com.richie.opencvLib.CodeActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CodeActivity.setResultListener(new CodeActivity.CodeActivityResultListener() {
            @Override
            public void callback(String result, String message) {
                if (message == null) {
                    System.out.println("result:" + result);
                } else {
                    System.out.println("cancel");
                }
            }
        });

        binding.openCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent(MainActivity.this, CodeActivity.class);
                MainActivity.this.startActivity(it);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
