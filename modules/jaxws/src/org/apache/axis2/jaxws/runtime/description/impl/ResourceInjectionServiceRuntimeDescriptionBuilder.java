/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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
package org.apache.axis2.jaxws.runtime.description.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.runtime.description.ResourceInjectionServiceRuntimeDescription;

public class ResourceInjectionServiceRuntimeDescriptionBuilder {

    /**
     * Intentionally Private
     */
    private ResourceInjectionServiceRuntimeDescriptionBuilder() { }

    /**
     * create
     * @param opDesc
     * @param implClassName
     * @return
     */
    static public ResourceInjectionServiceRuntimeDescription create(ServiceDescription serviceDesc, 
                Class implClass) {
        ResourceInjectionServiceRuntimeDescriptionImpl desc = 
            new ResourceInjectionServiceRuntimeDescriptionImpl(getKey(implClass), serviceDesc);
        boolean value = hasResourceAnnotation(implClass);
        desc.setResourceAnnotation(value);
        return desc;
    }
    
    static public String getKey(Class implClass) {
        return "Resource Injection:" + implClass.getCanonicalName();
    }
    
    /**
     * @param implClass
     * @return true if Field or Method has a @Resource annotation 
     */
    static private boolean hasResourceAnnotation(Class implClass) {
        // Getting this information is expensive, but fortunately is cached.
        List<Field> fields =  getFields(implClass);
        for (Field field:fields) {
            if (field.getAnnotation(Resource.class) != null) {
                return true;
            }
        }
        List<Method> methods = getMethods(implClass);
        for (Method method:methods) {
            if (method.getAnnotation(Resource.class) != null) {
                return true;
            }
        }
        return false;
        
    }
    
    /**
     * Gets all of the fields in this class and the super classes
     * @param beanClass
     * @return
     */
    static private List<Field> getFields(final Class beanClass) {
        // This class must remain private due to Java 2 Security concerns
        List<Field> fields;
        fields = (List<Field>) AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {
                        List<Field> fields = new ArrayList<Field>();
                        Class cls = beanClass;
                        while(cls != null) {
                            Field[] fieldArray = cls.getDeclaredFields();
                            for (Field field:fieldArray) {
                                fields.add(field);
                            }
                            cls = cls.getSuperclass();
                        }
                        return fields; 
                    }
                }
        );
        
        return fields;
    }
    
    /**
     * Gets all of the fields in this class and the super classes
     * @param beanClass
     * @return
     */
    static private List<Method> getMethods(final Class beanClass) {
        // This class must remain private due to Java 2 Security concerns
        List<Method> methods;
        methods = (List<Method>) AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {
                        List<Method> methods = new ArrayList<Method>();
                        Class cls = beanClass;
                        while(cls != null) {
                            Method[] methodArray = cls.getDeclaredMethods();
                            for (Method method:methodArray) {
                                methods.add(method);
                            }
                            cls = cls.getSuperclass();
                        }
                        return methods; 
                    }
                }
        );
        
        return methods;
    }
}
