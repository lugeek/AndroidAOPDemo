package com.lugeek.apt_compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;

import javax.annotation.CheckReturnValue;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.lugeek.apt_annotation.DIComponent"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ComponentProcessor extends BasicAnnotationProcessor {

    @Inject
    ImmutableList<Step> processingSteps;

    @Override
    protected Iterable<? extends Step> steps() {
        ProcessorComponent.factory().create(processingEnv).inject(this);
        return processingSteps;
    }

    @Singleton
    @Component(
            modules = {
                    ProcessingStepsModule.class,
                    ProcessingEnvironmentModule.class,
            })
    interface ProcessorComponent {
        void inject(ComponentProcessor processor);

        static Factory factory() {
            return DaggerComponentProcessor_ProcessorComponent.factory();
//            return null;
        }

        @Component.Factory
        interface Factory {
            @CheckReturnValue
            ProcessorComponent create(
                    @BindsInstance ProcessingEnvironment processingEnv);
        }
    }

    @Module
    interface ProcessingStepsModule {
        @Provides
        static ImmutableList<Step> processingSteps(
                ComponentProcessingStep componentProcessingStep,
                InjectProcessingStep injectProcessingStep) {
            return ImmutableList.of(
                    componentProcessingStep,
                    injectProcessingStep);
        }
    }

    @Module
    interface ProcessingEnvironmentModule {

        @Provides
        static Messager messager(ProcessingEnvironment processingEnvironment) {
            return processingEnvironment.getMessager();
        }

        @Provides
        static Types types(ProcessingEnvironment processingEnvironment) {
            return processingEnvironment.getTypeUtils();
        }

        @Provides
        static SourceVersion sourceVersion(ProcessingEnvironment processingEnvironment) {
            return processingEnvironment.getSourceVersion();
        }

        @Provides
        static Elements elements(ProcessingEnvironment processingEnvironment) {
            return processingEnvironment.getElementUtils();
        }

        @Provides
        static Filer filer(ProcessingEnvironment processingEnvironment) {
            return processingEnvironment.getFiler();
        }

    }


}
