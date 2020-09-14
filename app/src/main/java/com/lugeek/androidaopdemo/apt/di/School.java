package com.lugeek.androidaopdemo.apt.di;

import javax.inject.Inject;

public class School {

    private Teacher teacher;
    private Student student;

    @Inject
    public School(Teacher teacher, Student student) {
        this.teacher = teacher;
        this.student = student;
    }
}
