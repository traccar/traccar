/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.util.CharsetUtil;

public final class Authorization {

    private Authorization() {
    }

    public static final String HEADER = "Authorization";
    public static final String SCHEME = "Basic";
    public static final String REGEX = SCHEME + " ";
    public static final String REPLACEMENT = "";
    public static final String TOKENIZER = ":";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static Map<String, String> parse(String authorization) {
        Map<String, String> authMap = new HashMap<>();
        final String encodedUsernameAndPassword = authorization.replaceFirst(REGEX, REPLACEMENT);
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedUsernameAndPassword, CharsetUtil.UTF_8);
        String usernameAndPassword = Base64.decode(buffer).toString(CharsetUtil.UTF_8);
        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, TOKENIZER);
        authMap.put(USERNAME, tokenizer.nextToken());
        authMap.put(PASSWORD, tokenizer.nextToken());
        return authMap;
    }
}
