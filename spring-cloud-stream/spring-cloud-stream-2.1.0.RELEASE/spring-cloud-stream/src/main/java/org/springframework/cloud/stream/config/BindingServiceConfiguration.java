/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.BinderType;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderFactory;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.Bindable;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.ContextStartAfterRefreshListener;
import org.springframework.cloud.stream.binding.DynamicDestinationsBindable;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.cloud.stream.binding.MessageChannelStreamListenerResultAdapter;
import org.springframework.cloud.stream.binding.OutputBindingLifecycle;
import org.springframework.cloud.stream.binding.StreamListenerAnnotationBeanPostProcessor;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.micrometer.DestinationPublishingMetricsAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;


/**
 * Configuration class that provides necessary beans for {@link MessageChannel} binding.
 *
 * @author Dave Syer
 * @author David Turanski
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Vinicius Carvalho
 * @author Artem Bilan
 * @author Oleg Zhurakousky
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties({BindingServiceProperties.class, SpringIntegrationProperties.class, StreamFunctionProperties.class})
@Import({DestinationPublishingMetricsAutoConfiguration.class, SpelExpressionConverterConfiguration.class})
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnBean(value = BinderTypeRegistry.class, search = SearchStrategy.CURRENT)
public class BindingServiceConfiguration {

    public static final String STREAM_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_NAME = "streamListenerAnnotationBeanPostProcessor";

    @Autowired(required = false)
    private Collection<DefaultBinderFactory.Listener> binderFactoryListeners;

    @Bean
    public BindingHandlerAdvise BindingHandlerAdvise(@Nullable MappingsProvider[] providers, @Nullable Validator validator) {
        Map<ConfigurationPropertyName, ConfigurationPropertyName> additionalMappings = new HashMap<>();
        if (!ObjectUtils.isEmpty(providers)) {
            for (int i = 0; i < providers.length; i++) {
                MappingsProvider mappingsProvider = providers[i];
                additionalMappings.putAll(mappingsProvider.getDefaultMappings());
            }
        }
        return new BindingHandlerAdvise(additionalMappings, validator);
    }

    /**
     * @param binderTypeRegistry 来自配置类 BinderFactoryConfiguration#binderTypeRegistry，
     *                           内部就一个map，保存着 META-INF/spring.binders 配置文件的key和value
     * @param bindingServiceProperties spring.cloud.stream 前缀的配置
     */
    @Bean
    @ConditionalOnMissingBean(BinderFactory.class)
    public BinderFactory binderFactory(BinderTypeRegistry binderTypeRegistry, BindingServiceProperties bindingServiceProperties) {
        Map<String, BinderConfiguration> configurationMap = getBinderConfigurations(binderTypeRegistry, bindingServiceProperties);
        DefaultBinderFactory binderFactory = new DefaultBinderFactory(configurationMap, binderTypeRegistry);
        // spring.cloud.stream.defaultBinder=rabbit 手动指定一个全局默认的binder，如果未配置，这里就是未null
        binderFactory.setDefaultBinder(bindingServiceProperties.getDefaultBinder());
        binderFactory.setListeners(binderFactoryListeners);
        return binderFactory;
    }

    /**
     * 解析 spring.cloud.stream.binders 前缀的配置内容，简单封装到 Map<String, BinderConfiguration> 中返回
     */
    private static Map<String, BinderConfiguration> getBinderConfigurations(BinderTypeRegistry binderTypeRegistry,
        BindingServiceProperties bindingServiceProperties) {

        Map<String, BinderConfiguration> binderConfigurations = new HashMap<>();
        // 配置文件 binders 前缀的配置内容
        Map<String, BinderProperties> declaredBinders = bindingServiceProperties.getBinders();
        boolean defaultCandidatesExist = false;

        // spring.cloud.stream.binders.<configurationName>.defaultCandidate=false
        Iterator<Map.Entry<String, BinderProperties>> binderPropertiesIterator = declaredBinders.entrySet().iterator();

        // 只要碰到一个true这里就会跳出while，不配置默认就true，第一个binders 默认就为true
        while (!defaultCandidatesExist && binderPropertiesIterator.hasNext()) {
            defaultCandidatesExist = binderPropertiesIterator.next().getValue().isDefaultCandidate();
        }

        List<String> existingBinderConfigurations = new ArrayList<>();
        for (Map.Entry<String, BinderProperties> binderEntry : declaredBinders.entrySet()) {
            BinderProperties binderProperties = binderEntry.getValue();
            if (binderTypeRegistry.get(binderEntry.getKey()) != null) {
                binderConfigurations.put(binderEntry.getKey(), new BinderConfiguration(
                                            binderEntry.getKey(),
                                            binderProperties.getEnvironment(),
                                            binderProperties.isInheritEnvironment(),
                                            binderProperties.isDefaultCandidate()));
                existingBinderConfigurations.add(binderEntry.getKey());
            } else {
                // 该binders配置没有找到对应的binder
                Assert.hasText(binderProperties.getType(), "No 'type' property present for custom binder " + binderEntry.getKey());
                binderConfigurations.put(binderEntry.getKey(), new BinderConfiguration(
                                            binderProperties.getType(),
                                            binderProperties.getEnvironment(),
                                            binderProperties.isInheritEnvironment(),
                                            binderProperties.isDefaultCandidate()));
                existingBinderConfigurations.add(binderEntry.getKey());
            }
        }

        // 感觉这里和上面while循环有重复的意思
        for (Map.Entry<String, BinderConfiguration> configurationEntry : binderConfigurations.entrySet()) {
            if (configurationEntry.getValue().isDefaultCandidate()) {
                defaultCandidatesExist = true;
            }
        }

        // 如果都显示配置 defaultCandidate = false，才会进入这里的if
        if (!defaultCandidatesExist) {
            for (Map.Entry<String, BinderType> binderEntry : binderTypeRegistry.getAll().entrySet()) {
                if (!existingBinderConfigurations.contains(binderEntry.getKey())) {
                    binderConfigurations.put(binderEntry.getKey(), new BinderConfiguration(binderEntry.getKey(), new HashMap<>(), true, true));
                }
            }
        }
        return binderConfigurations;
    }

    @Bean
    public MessageChannelStreamListenerResultAdapter messageChannelStreamListenerResultAdapter() {
        return new MessageChannelStreamListenerResultAdapter();
    }

    @Bean(name = STREAM_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_NAME)
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    public static StreamListenerAnnotationBeanPostProcessor streamListenerAnnotationBeanPostProcessor() {
        return new StreamListenerAnnotationBeanPostProcessor();
    }

    /**
     * @param binderFactory 上面的方法注入
     * @param taskScheduler 这个bean是在哪里注入的？？？
     */
    @Bean
    // This conditional is intentionally not in an autoconfig (usually a bad idea) because
    // it is used to detect a BindingService in the parent context (which we know
    // already exists).
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    public BindingService bindingService(BindingServiceProperties bindingServiceProperties, BinderFactory binderFactory, TaskScheduler taskScheduler) {
        return new BindingService(bindingServiceProperties, binderFactory, taskScheduler);
    }


    /**
     *
     * @param bindingService 在上面创建
     * @param bindables @EnableBinding 注解的value值会被解析成 Bindable 类型注入到容器中，下面也默认注入了 DynamicDestinationsBindable
     *
     * 实现了 Lifecycle 接口的bean 会在指定的时机被回调
     */
    @Bean
    @DependsOn("bindingService")
    public OutputBindingLifecycle outputBindingLifecycle(BindingService bindingService, Map<String, Bindable> bindables) {
        return new OutputBindingLifecycle(bindingService, bindables);
    }

    @Bean
    @DependsOn("bindingService")
    public InputBindingLifecycle inputBindingLifecycle(BindingService bindingService, Map<String, Bindable> bindables) {
        return new InputBindingLifecycle(bindingService, bindables);
    }

    // 不知道有什么用？
    @Bean
    @DependsOn("bindingService")
    public ContextStartAfterRefreshListener contextStartAfterRefreshListener() {
        return new ContextStartAfterRefreshListener();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public BinderAwareChannelResolver binderAwareChannelResolver(BindingService bindingService,
        AbstractBindingTargetFactory<? extends MessageChannel> bindingTargetFactory,
        DynamicDestinationsBindable dynamicDestinationsBindable,
        @Nullable BinderAwareChannelResolver.NewDestinationBindingCallback callback) {
        return new BinderAwareChannelResolver(bindingService, bindingTargetFactory, dynamicDestinationsBindable, callback);
    }

    @Bean
    public DynamicDestinationsBindable dynamicDestinationsBindable() {
        return new DynamicDestinationsBindable();
    }

    @SuppressWarnings("deprecation")
    @Bean
    @ConditionalOnMissingBean
    public org.springframework.cloud.stream.binding.BinderAwareRouterBeanPostProcessor binderAwareRouterBeanPostProcessor(
        @Autowired(required = false) AbstractMappingMessageRouter[] routers,
        @Autowired(required = false) DestinationResolver<MessageChannel> channelResolver) {
        return new org.springframework.cloud.stream.binding.BinderAwareRouterBeanPostProcessor(routers, channelResolver);
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> appListener(SpringIntegrationProperties springIntegrationProperties) {
        return new ApplicationListener<ContextRefreshedEvent>() {
            @Override
            public void onApplicationEvent(ContextRefreshedEvent event) {
                event.getApplicationContext().getBeansOfType(AbstractReplyProducingMessageHandler.class).values()
                    .forEach(mh -> mh.addNotPropagatedHeaders(springIntegrationProperties.getMessageHandlerNotPropagatedHeaders()));
            }
        };
    }

}
