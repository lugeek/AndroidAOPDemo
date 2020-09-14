# AOP
### apt di 依赖注入 demo
1. 创建两个Java Library
	apt-annotation : 存放注解类
	apt-compiler ： 存放AnnotationProsessor，处理注解类

2. 创建Android Library
	apt-api : 创建基础类，主要用于处理生成类，可选。

3. 注解类型RetentionPolicy
	SOURCE(源代码), CLASS(编译), RUNTIME(运行，范围最大，会同时包含3个过程)

4. 注解位置Target
	TYPE,FIELD,METHOD,PARAMETER,CONSTRUCTOR,LOCAL_VARIABLE,ANNOTATION_TYPE, PACKAGE,TYPE_PARAMETER,TYPE_USE;

5. 使用Android注解
	依赖’com.android.support:support-annotations:28.0.0’到apt-annotation库中。

6. apt-compiler 依赖
    ```groovy
    implementation 'com.google.auto.service:auto-service:1.0-rc7'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc7'
    implementation 'com.squareup:javapoet:1.10.0'
    implementation 'com.google.auto:auto-common:0.11'
    implementation 'com.google.dagger:dagger:2.29.1'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.29.1'
    ```
   * auto-service:用来标注Processor进行引用；
   * javapoet用来生成java代码；
   * auto-common用来处理processor中element的工具类；
   * dagger进行依赖注入
    

7. AndroidAOPDemo中apt例子讲解
* 这是一个依赖注入的例子，参考了dagger
* 支持@Inject注解构造方法，然后解析所有@Inject，生成XXX_Provider类
* 支持@DIComponent自定义注解，添加在interface上，生成Component类，并在构造方法中自动生成依赖关系构造序列。
* 难点1: 因为各个类存在依赖关系，所以需要深度优先创建各个Provider，但是Processor的process顺序是不确定的，需要将某些Element延后处理，等到依赖的Element都处理完了再处理，在auto-common提供的BasicAnnotationProcessor中提供了延后功能。
* 难点2: 依赖关系存储和创建，在创建类时，需要先创建依赖的Provider，所以需要存储依赖关系，然后通过递归方式，深度优先构造依赖关系。