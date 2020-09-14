package com.lugeek.apt_compiler;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Singleton
public final class DependencyValidator {

    private static final Joiner CLASS_FILE_NAME_JOINER = Joiner.on('_');

    private Elements elements;

    private Map<ClassName, List<ClassName>> dependencies;

    @Inject
    public DependencyValidator(Elements elements) {
        this.elements = elements;
        this.dependencies = new HashMap<>();
    }

    public boolean validate(ExecutableElement constructorElement) {
        List<ClassName> deps = new ArrayList<>();
        for (VariableElement parameter : constructorElement.getParameters()) {
            TypeMirror requestType = parameter.asType();
            DeclaredType declaredType = MoreTypes.asDeclared(requestType);
            TypeElement typeElement = MoreElements.asType(declaredType.asElement());

            ClassName className = generatedMonitoringModuleName(typeElement);
            TypeElement factoryElement = checkTypePresent(className.toString());

            if (factoryElement == null) {
                return false;
            }

            deps.add((ClassName) ClassName.get(requestType));
        }

        TypeElement typeElement = MoreElements.asType(constructorElement.getEnclosingElement());
        ClassName enclosingClassName = ClassName.get(typeElement);
        dependencies.put(enclosingClassName, deps);

        return true;
    }

    public boolean validate(Element component, Types types, Elements elements) {
        ImmutableSet<ExecutableElement> methodSet = MoreElements.getLocalAndInheritedMethods(
                MoreElements.asType(component), types, elements);
        if (methodSet.size() > 1) {
            throw new IllegalArgumentException("Component abstract method numbers only one");
        }
        TypeMirror typeMirror = methodSet.asList().get(0).getReturnType();
        ClassName returnProviderClassName = generateProviderName((ClassName) ClassName.get(typeMirror));
        TypeElement returnProviderTypeElement = checkTypePresent(returnProviderClassName.toString());

        if (returnProviderTypeElement == null) {
            return false;
        }

        return true;
    }

    public List<ClassName> getSubClassNames(ClassName className) {
        return dependencies.get(className);
    }

    public static ClassName generatedMonitoringModuleName(TypeElement componentElement) {
        return siblingClassName(componentElement, "_Provider");
    }

    private static ClassName siblingClassName(TypeElement typeElement, String suffix) {
        ClassName className = ClassName.get(typeElement);
        return className.topLevelClassName().peerClass(classFileName(className) + suffix);
    }

    private static ClassName generateProviderName(ClassName className) {
        return className.topLevelClassName().peerClass(classFileName(className) + "_Provider");
    }

    public static String classFileName(ClassName className) {
        return CLASS_FILE_NAME_JOINER.join(className.simpleNames());
    }

    public TypeElement checkTypePresent(String typeName) {
        TypeElement type = elements.getTypeElement(typeName);
//        if (type == null) {
//            throw new TypeNotPresentException(typeName, null);
//        }
        return type;
    }

}
