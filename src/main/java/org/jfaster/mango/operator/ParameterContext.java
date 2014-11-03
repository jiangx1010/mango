/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.operator;

import org.jfaster.mango.exception.IncorrectParameterCountException;
import org.jfaster.mango.exception.IncorrectParameterTypeException;
import org.jfaster.mango.exception.NotReadableParameterException;
import org.jfaster.mango.exception.NotReadablePropertyException;
import org.jfaster.mango.jdbc.JdbcUtils;
import org.jfaster.mango.reflect.BeanInfoCache;
import org.jfaster.mango.util.Strings;
import org.jfaster.mango.reflect.ParameterDescriptor;
import org.jfaster.mango.reflect.TypeWrapper;
import org.jfaster.mango.reflect.Types;

import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author ash
 */
public class ParameterContext {

    private final List<ParameterDescriptor> parameterDescriptors;
    private final Map<String, Type> typeMap = new HashMap<String, Type>();
    private final Map<String, List<String>> propertyMap = new HashMap<String, List<String>>();
    private final Map<String, Type> cache = new HashMap<String, Type>();

    public ParameterContext(List<ParameterDescriptor> parameterDescriptors,
                            NameProvider nameProvider, OperatorType operatorType) {
        if (operatorType == OperatorType.BATCHUPDATYPE) {
            if (parameterDescriptors.size() != 1) {
                throw new IncorrectParameterCountException("batch update expected one and " +
                        "only one parameter but " + parameterDescriptors.size()); // 批量更新只能有一个参数
            }
            ParameterDescriptor pd = parameterDescriptors.get(0);
            TypeWrapper tw = new TypeWrapper(pd.getType());
            Class<?> mappedClass = tw.getMappedClass();
            if (mappedClass == null || !tw.isIterable()) {
                throw new IncorrectParameterTypeException("parameter of batch update expected array or " +
                        "implementations of java.util.List or implementations of java.util.Set " +
                        "but " + pd.getType()); // 批量更新的参数必须可迭代
            }
            parameterDescriptors = new ArrayList<ParameterDescriptor>(1);
            parameterDescriptors.add(new ParameterDescriptor(0, mappedClass, mappedClass, pd.getAnnotations(), pd.getName()));
        }
        this.parameterDescriptors = parameterDescriptors;
        for (int i = 0; i < parameterDescriptors.size(); i++) {
            ParameterDescriptor parameterDescriptor = parameterDescriptors.get(i);
            String parameterName = nameProvider.getParameterName(i);
            typeMap.put(parameterName, parameterDescriptor.getType());

            Class<?> parameterRawType = parameterDescriptor.getRawType();
            TypeWrapper tw = new TypeWrapper(parameterDescriptor.getType());
            if (!JdbcUtils.isSingleColumnClass(parameterRawType) // 方法参数不是单列
                    && !tw.isIterable()) { // 方法参数不可迭代
                List<PropertyDescriptor> propertyDescriptors =
                        BeanInfoCache.getPropertyDescriptors(parameterRawType);
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    String propertyName = propertyDescriptor.getName();
                    if (!nameProvider.isParameterName(propertyName)) { // 属性名和参数名相同则不扩展
                        List<String> oldParameterNames = propertyMap.get(propertyName);
                        if (oldParameterNames == null) {
                            oldParameterNames = new ArrayList<String>();
                            propertyMap.put(propertyName, oldParameterNames);
                        }
                        oldParameterNames.add(parameterName);
                    }

                }
            }

        }
    }

    public Type getPropertyType(String parameterName, String propertyPath) {
        String key = getCacheKey(parameterName, propertyPath);
        Type cachedType = cache.get(key);
        if (cachedType != null) { // 缓存命中，直接返回
            return cachedType;
        }
        Type type = typeMap.get(parameterName);
        if (type == null ) {
            throw new NotReadableParameterException("parameter :" + parameterName + " is not readable");
        }
        if (Strings.isNotEmpty(propertyPath)) {
            Types.Result tr = Types.getPropertyType(type, propertyPath);
            if (tr.isError()) {
                String fullName = Strings.getFullName(parameterName, tr.getPath());
                String parentFullName = Strings.getFullName(parameterName, tr.getParentPath());
                Type parentType = tr.getParentType();
                throw new NotReadablePropertyException("property " + fullName + " is not readable, " +
                        "the type of " + parentFullName + " is " + parentType + ", please check it's get method");
            }
            type = tr.getType();
        }
        cache.put(key, type);
        return type;
    }

    @Nullable
    public String getParameterNameBySubPropertyName(String propertyName) {
        List<String> parameterNames = propertyMap.get(propertyName);
        if (parameterNames == null) {
            return null;
        }
        if (parameterNames.size() != 1) {
            throw new IllegalArgumentException("parameters " + parameterNames +
                    " has the same property '" + propertyName + "', so can't expand");
        }
        return parameterNames.get(0);
    }

    public List<ParameterDescriptor> getParameterDescriptors() {
        return parameterDescriptors;
    }

    private String getCacheKey(String parameterName, String propertyPath) {
        return propertyPath.isEmpty() ? parameterName : parameterName + "." + propertyPath;
    }

}
