/**
 * Copyright 2018 Netifi Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netifi.proteus.spring.core;

import io.rsocket.rpc.AbstractRSocketService;
import io.rsocket.rpc.annotations.internal.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;

/**
 * Handles post processing of custom Proteus bean definitions.
 */
public class ProteusBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProteusBeanDefinitionRegistryPostProcessor.class);

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) registry;

        for (String serviceServerBeanName : beanFactory.getBeanNamesForType(AbstractRSocketService.class)) {
            try {
                BeanDefinition beanDefinition = registry.getBeanDefinition(serviceServerBeanName);
                Class<?> clazz = null;

                if (beanDefinition.getBeanClassName() != null) {
                    clazz =
                        ClassUtils.forName(beanDefinition.getBeanClassName(), beanDefinition.getClass().getClassLoader());
                } else if (beanDefinition instanceof AnnotatedBeanDefinition) {
                    MethodMetadata metadata =
                            ((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata();

                    if (metadata != null) {
                        clazz =
                            ClassUtils.forName(metadata.getReturnTypeName(), beanDefinition.getClass().getClassLoader());
                    } else {
                        continue;
                    }
                }

                if (clazz != null && clazz.isAnnotationPresent(Generated.class)) {
                    Generated proteusGeneratedAnnotation = clazz.getAnnotation(Generated.class);
                    Class<?> idlClazz = proteusGeneratedAnnotation.idlClass();

                    // Remove any AbstractProteusService beans that do not have an implementation of their
                    // underlying service in the bean registry.
                    if(beanFactory.getBeanNamesForType(idlClazz).length <= 0) {
                        LOGGER.info("Removing {} because no IDL implementation for {} was found", serviceServerBeanName, idlClazz.getCanonicalName());
                        registry.removeBeanDefinition(serviceServerBeanName);
                    }
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error during post processing of Proteus beans", e);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}