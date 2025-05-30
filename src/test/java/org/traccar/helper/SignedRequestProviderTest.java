/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import liquibase.util.MD5Util;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignedRequestProviderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignedRequestProviderTest.class);

    private final Client client = ClientBuilder.newClient();

    @Test
    public void baidu() {
        String API_KEY = "yourak";
        String API_SECRET = "yoursk";
        String API_URL = "https://api.map.baidu.com/geocoder/v2/";
        SignedRequestProvider params = new SignedRequestProvider(API_SECRET, client, API_URL);
        params.put("address", "百度大厦");
        params.put("output", "json");
        params.put("ak", API_KEY);
        params.requestWithSign("sn", true, true, true);
        assertEquals("29049c301315e35426b71e3a253d5f48", params.get("sn"));
    }

    @Test
    public void baiduOfficialSample() throws UnsupportedEncodingException {
        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，该方法会自动将key按照字母a-z顺序排序。所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。以get请求为例：https://api.map.baidu.com/geocoder/v2/?address=百度大厦&output=json&ak=yourak，paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。

        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", "百度大厦");
        paramsMap.put("ak", "yourak");
        paramsMap.put("output", "json");

        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : paramsMap.entrySet()) {
            queryString.append(pair.getKey() + "=");
            queryString.append(URLEncoder.encode((String) pair.getValue(),
                    "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        String paramsStr = queryString.toString();

        // 对paramsStr前面拼接上/geocoder/v2/?，后面直接拼接yoursk得到/geocoder/v2/?address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = new String("/geocoder/v2/?" + paramsStr + "yoursk");

        // 对上面wholeStr再作utf8编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

        LOGGER.info("tempStr: {}", tempStr);

        // 调用下面的MD5方法得到最后的sn签名7de5a22212ffaa9e326444c75a58f9a0
        assertEquals("7de5a22212ffaa9e326444c75a58f9a0", MD5Util.computeMD5(tempStr));
    }

    @Test
    public void tencent() {
        String API_KEY = "yourkey";
        String API_SECRET = "yoursecret";
        String API_URL = "https://apis.map.qq.com/ws/geocoder/v1";
        SignedRequestProvider params = new SignedRequestProvider(API_SECRET, client, API_URL);
        params.put("location", "28.7033487,115.8660847");
        params.put("key", API_KEY);
        params.requestWithSign("sig", true, false, false);
        assertEquals("7d4e89b19703ad0399c1debbe873b2ed", params.get("sig"));
    }
}
