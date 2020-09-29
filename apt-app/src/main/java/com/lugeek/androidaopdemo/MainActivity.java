package com.lugeek.androidaopdemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lugeek.androidaopdemo.apt.di.DItest;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DItest test = new DItest();
        test.test();
    }
}