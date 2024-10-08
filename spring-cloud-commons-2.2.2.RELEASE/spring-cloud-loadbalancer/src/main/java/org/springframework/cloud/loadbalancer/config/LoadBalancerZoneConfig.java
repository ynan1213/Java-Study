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

package org.springframework.cloud.loadbalancer.config;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerZoneConfig {

	/**
	 * A {@link String} representation of the <code>zone</code> used for filtering
	 * instances by zoned load-balancing implementations.
	 *
	 * 标识当前负载均衡器处于哪一个 zone
	 */
	private String zone;

	public LoadBalancerZoneConfig(String zone) {
		this.zone = zone;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

}
