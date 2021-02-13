package com.blogcode.sqslistener.message;

import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.web.method.ControllerAdviceBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SqsMessageHandler extends QueueMessageHandler {
    private final Set<String> destinationSet;

    public SqsMessageHandler(List<MessageConverter> messageConverters, Set<String> destinationSet) {
        super(messageConverters);
        this.destinationSet = destinationSet;
    }

    @Override
    public void afterPropertiesSet() {
        this.initControllerAdviceCache();
        super.afterPropertiesSet();
    }

    private void initControllerAdviceCache() {
        if (this.getApplicationContext() == null) {
            return;
        }
        final List<ControllerAdviceBean> controllerAdvice = ControllerAdviceBean.findAnnotatedBeans(this.getApplicationContext());
        AnnotationAwareOrderComparator.sort(controllerAdvice);
        for (final MessagingAdviceBean bean : MessagingControllerAdviceBean.createFromList(controllerAdvice)) {
            final Class<?> beanType = bean.getBeanType();
            if (beanType != null) {
                final AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(beanType);
                if (resolver.hasExceptionMappings()) {
                    this.registerExceptionHandlerAdvice(bean, resolver);
                }
            }
        }
    }

    /*
     * 메시지 처리 listener 필터링 후 등록
     * */
    @Override
    protected void registerHandlerMethod(Object handler, Method method, MappingInformation mapping) {
        if (!getDirectLookupDestinations(mapping).isEmpty()) {
            super.registerHandlerMethod(handler, method, mapping);
        }
    }

    @Override
    protected Set<String> getDirectLookupDestinations(MappingInformation mapping) {
        return mapping.getLogicalResourceIds().stream()
                .filter(destinationSet::contains)
                .collect(Collectors.toSet());
    }

    private static class MessagingControllerAdviceBean implements MessagingAdviceBean {

        private final ControllerAdviceBean adviceBean;

        private MessagingControllerAdviceBean(final ControllerAdviceBean adviceBean) {
            this.adviceBean = adviceBean;
        }

        public static List<MessagingAdviceBean> createFromList(final List<ControllerAdviceBean> controllerAdvice) {
            final List<MessagingAdviceBean> messagingAdvice = new ArrayList<>(controllerAdvice.size());
            for (final ControllerAdviceBean bean : controllerAdvice) {
                messagingAdvice.add(new MessagingControllerAdviceBean(bean));
            }
            return messagingAdvice;
        }

        @Override
        public Class<?> getBeanType() {
            return adviceBean.getBeanType();
        }

        @Override
        public Object resolveBean() {
            return adviceBean.resolveBean();
        }

        @Override
        public boolean isApplicableToBeanType(final Class<?> beanType) {
            return adviceBean.isApplicableToBeanType(beanType);
        }

        @Override
        public int getOrder() {
            return adviceBean.getOrder();
        }
    }
}
