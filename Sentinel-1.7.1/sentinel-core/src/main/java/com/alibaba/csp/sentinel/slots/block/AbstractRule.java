/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.slots.block;

/**
 * Abstract rule entity.
 *
 * @author youji.zj
 * @author Eric Zhao
 */
public abstract class AbstractRule implements Rule {

    /**
     * Resource name.
     */
    private String resource;

    /**
     * <p>
     * Application name that will be limited by origin.
     * The default limitApp is {@code default}, which means allowing all origin apps.
     * </p>
     * <p>
     * For authority rules, multiple origin name can be separated with comma (',').
     * </p>
     *
     * 以下说明只针对 FlowRule
     * 1. default：表示不区分调用者，来自任何调用者的请求都将进行限流统计。如果这个资源名的调用总和超过了这条规则定义的阈值，则触发限流。
     * 比如 app1 请求了resourceA，app2也请求了resourceA资源，limitApp 配置成default，代表不区分app1和app2，对resourceA的所有请求都做统计。
     * 这种情况下是对 DefaultNode中的 clusterNode 进行统计
     *
     * 2. {some_origin_name}：表示针对特定的调用者，只有来自这个调用者的请求才会进行流量控制。
     * 比如 app1 请求了resourceA，app2也请求了resourceA资源，limitApp 配置成 app1，表示只对 app1的请求做统计。
     * 这种情况是对 context.getOriginNode()进行统计，clusterNode 是以resource进行全局统计，OriginNode是以resource和origin进行统计，比clusterNode
     * 细分一个层次，并且是挂在clusterNode下面
     * 3. other：表示针对除 {some_origin_name} 以外的其余调用方的流量进行流量控制。例如，资源NodeA配置了一条针对调用者 caller1 的限流规则，
     * 同时又配置了一条调用者为 other 的规则，那么任意来自非 caller1 对 NodeA 的调用，都不能超过 other 这条规则定义的阈值。
     */
    private String limitApp;

    public String getResource() {
        return resource;
    }

    public AbstractRule setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public String getLimitApp() {
        return limitApp;
    }

    public AbstractRule setLimitApp(String limitApp) {
        this.limitApp = limitApp;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractRule)) {
            return false;
        }

        AbstractRule that = (AbstractRule) o;

        if (resource != null ? !resource.equals(that.resource) : that.resource != null) {
            return false;
        }
        if (!limitAppEquals(limitApp, that.limitApp)) {
            return false;
        }
        return true;
    }

    private boolean limitAppEquals(String str1, String str2) {
        if ("".equals(str1)) {
            return RuleConstant.LIMIT_APP_DEFAULT.equals(str2);
        } else if (RuleConstant.LIMIT_APP_DEFAULT.equals(str1)) {
            return "".equals(str2) || str2 == null || str1.equals(str2);
        }
        if (str1 == null) {
            return str2 == null || RuleConstant.LIMIT_APP_DEFAULT.equals(str2);
        }
        return str1.equals(str2);
    }

    public <T extends AbstractRule> T as(Class<T> clazz) {
        return (T) this;
    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        if (!("".equals(limitApp) || RuleConstant.LIMIT_APP_DEFAULT.equals(limitApp) || limitApp == null)) {
            result = 31 * result + limitApp.hashCode();
        }
        return result;
    }
}
