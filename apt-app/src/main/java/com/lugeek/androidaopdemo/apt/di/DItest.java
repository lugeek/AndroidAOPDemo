package com.lugeek.androidaopdemo.apt.di;

public class DItest {


    public School test() {
        DI_SchoolComponent component = new DI_SchoolComponent();
        School school = component.get();
        return school;
    }
}
