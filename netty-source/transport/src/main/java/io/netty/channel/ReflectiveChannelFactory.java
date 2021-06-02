/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
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

package io.netty.channel;

import io.netty.util.internal.StringUtil;

/**
 * A {@link ChannelFactory} that instantiates a new {@link Channel} by invoking its default constructor reflectively.
 *
 * 这是一个用于创建channel的工厂类 , 通过使用反射调用默认的构造方法,创建一个新的Channel的实例
 */
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {

    private final Class<? extends T> clazz;

    // 构造仅仅完成了赋值的操作
    public ReflectiveChannelFactory(Class<? extends T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        this.clazz = clazz;
    }

    @Override
    public T newChannel() {
        try {
            //通过使用反射调用默认的构造方法
        return clazz.getConstructor().newInstance();
    } catch (Throwable t) {
        throw new ChannelException("Unable to create Channel from class " + clazz, t);
    }
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(clazz) + ".class";
    }
}
