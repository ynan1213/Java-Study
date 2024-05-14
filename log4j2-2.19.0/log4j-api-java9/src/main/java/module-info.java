/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
import org.apache.logging.log4j.util.EnvironmentPropertySource;
import org.apache.logging.log4j.util.PropertySource;
import org.apache.logging.log4j.util.SystemPropertiesPropertySource;

module org.apache.logging.log4j {
    requires java.base;

    exports org.apache.logging.log4j;
    exports org.apache.logging.log4j.message;
    exports org.apache.logging.log4j.simple;
    exports org.apache.logging.log4j.spi;
    exports org.apache.logging.log4j.status;
    exports org.apache.logging.log4j.util;

    uses org.apache.logging.log4j.spi.Provider;
    uses org.apache.logging.log4j.util.PropertySource;
    uses org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory;

    provides PropertySource with EnvironmentPropertySource, SystemPropertiesPropertySource;
}
