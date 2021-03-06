/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shiro.cas

import org.junit.Test

import static org.junit.Assert.*

/**
 * Unit tests for the {@link CasToken} implementation.
 *
 * @since 1.2
 * @see <a href="https://github.com/bujiio/buji-pac4j">buji-pac4j</a>
 * @deprecated replaced with Shiro integration in <a href="https://github.com/bujiio/buji-pac4j">buji-pac4j</a>.
 */
@Deprecated
class CasTokenTest {

    @Test
    void testPrincipal() {
        CasToken casToken = new CasToken("fakeTicket")
        assertNull casToken.principal
        casToken.userId = "myUserId"
        assertEquals "myUserId", casToken.principal
    }

    @Test
    void testCredentials() {
        CasToken casToken = new CasToken("fakeTicket")
        assertEquals "fakeTicket", casToken.credentials
    }

    @Test
    void testRememberMe() {
        CasToken casToken = new CasToken("fakeTicket")
        assertFalse casToken.rememberMe
        casToken.rememberMe = true
        assertTrue casToken.rememberMe
    }
}
