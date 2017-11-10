package com.riverlet.scratchcard;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ScratchCardView scratchCardView = findViewById(R.id.scratch_card_01);
        scratchCardView.setDownLayer(R.drawable.down);
        scratchCardView.setUpLayer(R.drawable.up);
        scratchCardView.setOnCompleteListener(new ScratchCardView.OnCompleteListener() {
            @Override
            public void onComplete() {
                Log.d(TAG,"完成00");
            }
        });

        scratchCardView = findViewById(R.id.scratch_card_02);
        scratchCardView.setDownLayer("你中奖了！！！");
        scratchCardView.setUpLayerColor(Color.GRAY);
        scratchCardView.setOnCompleteListener(new ScratchCardView.OnCompleteListener() {
            @Override
            public void onComplete() {
                Log.d(TAG,"完成01");
            }
        });

        scratchCardView = findViewById(R.id.scratch_card_03);
        scratchCardView.setDownLayer(Color.GREEN, "你怎么可能会中奖！！！");
        scratchCardView.setUpLayer(Color.GRAY, "刮开看看");
        scratchCardView.setOnCompleteListener(new ScratchCardView.OnCompleteListener() {
            @Override
            public void onComplete() {
                Log.d(TAG,"完成02");
            }
        });

        scratchCardView = findViewById(R.id.scratch_card_04);
        scratchCardView.setDownLayerColor(Color.GREEN);
        scratchCardView.setUpLayerColor(Color.GRAY);
        scratchCardView.setOnCompleteListener(new ScratchCardView.OnCompleteListener() {
            @Override
            public void onComplete() {
                Log.d(TAG,"完成03");
            }
        });
    }

}
