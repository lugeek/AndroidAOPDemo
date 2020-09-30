package com.lugeek.aspectj_app;

import android.os.Bundle;
import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * before -> around -> after 三个方法顺序排列，否则，不能保证正常运行。
 * 结果：
 * 2020-09-30 19:12:02.137 I/aspectj: before
 * 2020-09-30 19:12:02.137 I/aspectj: around1
 * 2020-09-30 19:12:02.314 I/System.out: before test
 * 2020-09-30 19:12:02.314 I/aspectj: test
 * 2020-09-30 19:12:02.314 I/aspectj: around2
 * 2020-09-30 19:12:02.314 I/aspectj: after
 * 2020-09-30 19:12:02.314 I/aspectj: afterReturning
 */
@Aspect
public class MainActivityAspect {

    @Pointcut("execution(* com.lugeek.aspectj_app.MainActivity.onCreate(..)) && args(b)")
    public void mainActivityOnCreate(Bundle b) {}

    @Before("mainActivityOnCreate(b)")
    public void before(Bundle b) {
        Log.i("aspectj", "before");
    }

    @Around("mainActivityOnCreate(b)")
    public void around(ProceedingJoinPoint thisJoinPoint, Bundle b) throws Throwable {
        Log.i("aspectj", "around1");
        thisJoinPoint.proceed(new Object[]{b});
        Log.i("aspectj", "around2");
    }

    @After("mainActivityOnCreate(b)")
    public void after(Bundle b) {
        Log.i("aspectj", "after");
    }

    @AfterReturning("mainActivityOnCreate()")
    public void afterReturning(JoinPoint joinPoint) {
        Log.i("aspectj", "afterReturning");
    }

    @AfterThrowing("mainActivityOnCreate()")
    public void afterThrowing(JoinPoint joinPoint) {
        Log.i("aspectj", "afterThrowing");
    }

    @Before("execution(* com.lugeek.aspectj_app.AspectjTest.test(..))")
    public void testBefore(JoinPoint joinPoint) throws Throwable {
        System.out.println("before test");
    }
}
