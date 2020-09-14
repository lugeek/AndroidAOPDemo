package com.lugeek.androidaopdemo.apt.di;

import javax.inject.Inject;

public class Teacher {

    private Book book;

    @Inject
    public Teacher(Book book) {
        this.book = book;
    }
}
