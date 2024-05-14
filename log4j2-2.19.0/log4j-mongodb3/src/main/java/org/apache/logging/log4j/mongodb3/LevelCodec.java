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

package org.apache.logging.log4j.mongodb3;

import org.apache.logging.log4j.Level;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * A BSON Codec for Log4j {@link Level}s.
 */
public class LevelCodec implements Codec<Level> {

    /**
     * The singleton instance.
     *
     * @since 2.14.0
     */
    public static final LevelCodec INSTANCE = new LevelCodec();
    
    @Override
    public Level decode(final BsonReader reader, final DecoderContext decoderContext) {
        return Level.getLevel(reader.readString());
    }

    @Override
    public void encode(final BsonWriter writer, final Level level, final EncoderContext encoderContext) {
        writer.writeString(level.name());
    }

    @Override
    public Class<Level> getEncoderClass() {
        return Level.class;
    }

}
