package com.lugeek.androidaopdemo.apt.di;

import javax.inject.Inject;

public class Student {

    private Book book;

    @Inject
    public Student(Book book) {
        this.book = book;
    }
}
