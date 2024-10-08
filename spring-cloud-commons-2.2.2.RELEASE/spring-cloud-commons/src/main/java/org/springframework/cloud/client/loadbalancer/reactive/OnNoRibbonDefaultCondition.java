/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.concurrent.locks.Condition;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * A {@link Condition} that verifies that Ribbon is either not present or disabled.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 */
public class OnNoRibbonDefaultCondition extends AnyNestedCondition {

	public OnNoRibbonDefaultCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled", havingValue = "false")
	static class RibbonNotEnabled {

	}

	@ConditionalOnMissingClass("org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
	static class RibbonLoadBalancerNotPresent {

	}

}
