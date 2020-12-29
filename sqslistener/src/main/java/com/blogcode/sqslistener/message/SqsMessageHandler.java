package com.blogcode.sqslistener.message;

import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.web.method.ControllerAdviceBean;

import java.util.ArrayList;
import java.util.List;

public class SqsMessageHandler extends QueueMessageHandler {

    public SqsMessageHandler(List<MessageConverter> messageConverters) {
        super(messageConverters);
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
