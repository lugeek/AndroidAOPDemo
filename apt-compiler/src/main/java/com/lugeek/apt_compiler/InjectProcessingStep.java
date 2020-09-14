package com.lugeek.apt_compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementKindVisitor8;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

public class InjectProcessingStep implements BasicAnnotationProcessor.Step {

    private final Filer filer;
    private final DependencyValidator validator;
    private final ElementVisitor<Void, Void> visitor;
    public static final ClassName PROVIDER = ClassName.get(Provider.class);

    @Inject
    public InjectProcessingStep(Filer filer, DependencyValidator validator) {
        this.filer = filer;
        this.validator = validator;
        visitor = new ElementKindVisitor8<Void, Void>() {
            @Override
            public Void visitExecutableAsConstructor(ExecutableElement executableElement, Void aVoid) {
                if (validator.validate(executableElement)) {
                    // generate
                    createProvider(executableElement);

                } else {
                    throw new TypeNotPresentException(executableElement.getParameters().toString(), null);
                }
                return null;
            }
        };
    }


    @Override
    public Set<String> annotations() {
        return ImmutableSet.of(Inject.class.getName());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementsByAnnotation) {
        ImmutableSet.Builder<Element> deferredElements = ImmutableSet.builder();
        elementsByAnnotation.forEach((annotation, element) -> {
            try {
                element.accept(visitor, null);
            } catch (TypeNotPresentException e) {
                deferredElements.add(element);
            }
        });
        return deferredElements.build();
    }


    /**
     * generate like：
     * public final class School_Provider implements Provider<School> {
     *   private final Provider<Teacher> teacher;
     *
     *   private final Provider<Student> student;
     *
     *   public School_Provider(Provider<Teacher> teacher, Provider<Student> student) {
     *     this.teacher = teacher;
     *     this.student = student;
     *   }
     *
     *   @Override
     *   public School get() {
     *     return new School(teacher.get(),student.get());
     *   }
     * }
     */
    private void createProvider(ExecutableElement executableElement) {
        TypeElement typeElement = MoreElements.asType(executableElement.getEnclosingElement());
        ClassName enclosingClassName = ClassName.get(typeElement);
        ClassName providerName = ClassName.get(enclosingClassName.packageName(), enclosingClassName.simpleName() + "_Provider");
        DeclaredType type = MoreTypes.asDeclared(typeElement.asType());

        ParameterizedTypeName superInterface = ParameterizedTypeName.get(
                ClassName.get(Provider.class),
                TypeName.get(typeElement.asType()));

        // public final class providerName implements Provider<xxx> {}
        TypeSpec.Builder factoryBuilder =
                TypeSpec.classBuilder(providerName)
                        .addModifiers(PUBLIC, FINAL)
                        .addSuperinterface(superInterface);

        // 构造函数
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        List<String> fields = new ArrayList<>();
        executableElement.getParameters().forEach(variableElement -> {
            ClassName variableClassName = (ClassName) ClassName.get(variableElement.asType());
            //参数类型 Provider<T>
            ParameterizedTypeName variableTypeName = ParameterizedTypeName.get(
                    ClassName.get(Provider.class),
                    ClassName.get(variableElement.asType()));
            //参数 (Provider<T> T)
            ParameterSpec parameter = ParameterSpec.builder(
                    variableTypeName,
                    UPPER_CAMEL.to(LOWER_CAMEL, variableClassName.simpleName())).build();

            fields.add(parameter.name);

            constructor.addParameter(parameter)
                    .addStatement("this.$1N = $1N", parameter);
            factoryBuilder.addField(FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL).build());
        });
        factoryBuilder.addMethod(constructor.build());

        // get函数
        MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder("get")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(enclosingClassName);
        getMethodBuilder.addStatement(
                "return new $1N(" +
                        fields.stream().map(s -> s + ".get()").collect(Collectors.joining(",")) +
                        ")",
                        enclosingClassName.simpleName()
                );
        factoryBuilder.addMethod(getMethodBuilder.build());

        try {
            JavaFile.builder(enclosingClassName.packageName(), factoryBuilder.build())
                    .addFileComment("Generated code from DI. Do not modify!")
                    .build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
