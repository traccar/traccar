package org.traccar.reports.common;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.jxls.expression.ExpressionEvaluator;
import org.jxls.expression.JexlExpressionEvaluator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ExpressionEvaluatorFactory implements org.jxls.expression.ExpressionEvaluatorFactory {

    private final JexlPermissions permissions = new JexlPermissions() {
        @Override
        public boolean allow(Package pack) {
            return true;
        }

        @Override
        public boolean allow(Class<?> clazz) {
            return true;
        }

        @Override
        public boolean allow(Constructor<?> ctor) {
            return true;
        }

        @Override
        public boolean allow(Method method) {
            return true;
        }

        @Override
        public boolean allow(Field field) {
            return true;
        }

        @Override
        public JexlPermissions compose(String... src) {
            return this;
        }
    };

    @Override
    public ExpressionEvaluator createExpressionEvaluator(String expression) {
        JexlExpressionEvaluator expressionEvaluator = expression == null
                ? new JexlExpressionEvaluator()
                : new JexlExpressionEvaluator(expression);
        expressionEvaluator.setJexlEngine(new JexlBuilder()
                .silent(true)
                .strict(false)
                .permissions(permissions)
                .create());
        return expressionEvaluator;
    }
}
