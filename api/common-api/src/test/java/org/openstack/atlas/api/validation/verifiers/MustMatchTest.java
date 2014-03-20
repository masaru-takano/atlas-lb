package org.openstack.atlas.api.validation.verifiers;

import java.util.regex.Pattern;

import org.openstack.atlas.api.validation.SimpleBean;
import org.openstack.atlas.api.validation.context.HttpRequestType;
import org.openstack.atlas.api.validation.Validator;
import org.openstack.atlas.api.validation.ValidatorBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.openstack.atlas.api.validation.ValidatorBuilder.*;
import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class MustMatchTest {

    public static class WhenVerifyingRegularExpressions {

        private SimpleBean testObjectA;
        private SimpleBean testObjectB;

        @Before
        public void standUp() {
            testObjectA = new SimpleBean("1", "2", "3", 1, null, 3);
            testObjectB = new SimpleBean("0", "2", "3", null, 2, 3);
        }

        @Test
        public void shouldValidateCompiledPatternsAgainstStrings() {
            final Validator<SimpleBean> validator = build(new ValidatorBuilder<SimpleBean>(SimpleBean.class) {

                {
                    result(validationTarget().getStringProperty1()).must().match(Pattern.compile("1"));
                }
            });

            assertTrue(validator.validate(testObjectA, HttpRequestType.POST).passedValidation());
            assertFalse(validator.validate(testObjectB, HttpRequestType.POST).passedValidation());
        }

        @Test
        public void shouldValidateCompiledPatternsAgainstOtherObjects() {
            final Validator<SimpleBean> validator = build(new ValidatorBuilder<SimpleBean>(SimpleBean.class) {

                {
                    result(validationTarget().getIntProperty1()).must().match(Pattern.compile("1"));
                }
            });

            assertTrue(validator.validate(testObjectA, HttpRequestType.POST).passedValidation());
            assertFalse(validator.validate(testObjectB, HttpRequestType.POST).passedValidation());
        }
    }
}
