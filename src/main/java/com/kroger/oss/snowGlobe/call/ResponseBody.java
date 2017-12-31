/*
 * Snow-Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.oss.snowGlobe.call;

public class ResponseBody {

    private String cluster;
    private int instance;
    private RequestToService request;
    private String rawResponse;

    // This is used by jackson to help convert from JSON -> POJO.
    public ResponseBody() {

    }

    public ResponseBody(String rawResponse) {
        this.rawResponse = rawResponse;
        request = new RequestToService();
    }

    public static ResponseBody buildDirectResponseFromRp(String body) {
        return new ResponseBody(body);
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public RequestToService getRequest() {
        return request;
    }

    public void setRequest(RequestToService request) {
        this.request = request;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
