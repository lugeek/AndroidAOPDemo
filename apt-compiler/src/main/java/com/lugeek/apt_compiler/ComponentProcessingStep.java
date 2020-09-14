package com.lugeek.apt_compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.lugeek.apt_annotation.DIComponent;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

public class ComponentProcessingStep implements BasicAnnotationProcessor.Step {

    private static final Joiner CLASS_FILE_NAME_JOINER = Joiner.on('_');

    private final Messager messager;
    private final Elements elements;
    private final Types types;
    private final Filer filer;
    private final DependencyValidator validator;

    @Inject
    public ComponentProcessingStep(Messager messager, Elements elements, Types types, Filer filer, DependencyValidator validator) {
        super();
        this.messager = messager;
        this.elements = elements;
        this.types = types;
        this.filer = filer;
        this.validator = validator;
    }

    @Override
    public Set<String> annotations() {
        return ImmutableSet.of(DIComponent.class.getName());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementsByAnnotation) {
        ImmutableSet.Builder<Element> deferredElements = ImmutableSet.builder();
        elementsByAnnotation.forEach((annotation, element) -> {
            if (validator.validate(element, types, elements)) {
                create(element);
            } else {
                deferredElements.add(element);
            }
        });
        return deferredElements.build();
    }


    /**
     * generate:
     * public final class DI_SchoolComponent implements SchoolComponent {
     *   private final Provider<Book> book;
     *
     *   private final Provider<Teacher> teacher;
     *
     *   private final Provider<Book> book_1;
     *
     *   private final Provider<Student> student;
     *
     *   private final Provider<School> school;
     *
     *   public DI_SchoolComponent() {
     *     this.book = new Book_Provider();
     *     this.teacher = new Teacher_Provider(book);
     *     this.book_1 = new Book_Provider();
     *     this.student = new Student_Provider(book_1);
     *     this.school = new School_Provider(teacher,student);
     *   }
     *
     *   @Override
     *   public School get() {
     *     return school.get();
     *   }
     * }
     */
    private void create(Element element) {
        ClassName interfaceClassName = ClassName.get(MoreElements.asType(element));

        ImmutableSet<ExecutableElement> methodSet = MoreElements.getLocalAndInheritedMethods(
                MoreElements.asType(element), types, elements);
        String interfaceMethodName = methodSet.asList().get(0).getSimpleName().toString();
        TypeMirror typeMirror = methodSet.asList().get(0).getReturnType();
        ClassName returnClassName = (ClassName) ClassName.get(typeMirror);

        // public final class providerName implements Provider<xxx> {}
        TypeSpec.Builder componentBuilder =
                TypeSpec.classBuilder("DI_" + interfaceClassName.simpleName())
                        .addModifiers(PUBLIC, FINAL)
                        .addSuperinterface(interfaceClassName);

        // 构造函数
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        addStateMentToMethod(returnClassName, constructor, fieldSpecs);
        componentBuilder.addFields(fieldSpecs);
        componentBuilder.addMethod(constructor.build());

        MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder(interfaceMethodName)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(returnClassName);
        getMethodBuilder.addStatement(
                "return $1N.get()",
                UPPER_CAMEL.to(LOWER_CAMEL, returnClassName.simpleName())
        );
        componentBuilder.addMethod(getMethodBuilder.build());

        try {
            JavaFile.builder(interfaceClassName.packageName(), componentBuilder.build())
                    .addFileComment("Generated code from DI. Do not modify!")
                    .build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String addStateMentToMethod(ClassName className, MethodSpec.Builder methodBuilder, List<FieldSpec> fieldSpecs) {
        List<ClassName> subClassNames = validator.getSubClassNames(className);
        Map<ClassName, String> classNameToFieldName = new HashMap<>();
        if (subClassNames != null && !subClassNames.isEmpty()) {
            for(ClassName sub : subClassNames) {
                String fieldName = addStateMentToMethod(sub, methodBuilder, fieldSpecs);
                classNameToFieldName.put(sub, fieldName);
            }
        }
        //参数类型 Provider<T>
        ParameterizedTypeName variableTypeName = ParameterizedTypeName.get(
                ClassName.get(Provider.class),
                className);
        //参数 (Provider<T> T)
        String fieldName = UPPER_CAMEL.to(LOWER_CAMEL, className.simpleName());
        int index = 0;
        for (FieldSpec fieldSpec: fieldSpecs) {
            if (fieldSpec.name.equals(fieldName)) {
                index++;
                fieldName += ("_" + index);
            }
        }
        ParameterSpec parameter = ParameterSpec.builder(
                variableTypeName,
                fieldName).build();
        fieldSpecs.add(FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL).build());
        methodBuilder.addStatement("this.$1N = new " +
                className.simpleName()+"_Provider" +
                "(" +
                subClassNames.stream().map(className1 -> classNameToFieldName.get(className1)).collect(Collectors.joining(",")) +
                ")", fieldName);
        return fieldName;
    }

}
