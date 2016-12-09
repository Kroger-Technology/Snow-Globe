package com.kroger.rp.util.call;

public class ResponseBody {

    private String cluster;
    private int instance;
    private RequestToService request;
    private String rawResponse;

    public ResponseBody() {

    }

    public ResponseBody(String rawResponse) {
        this.rawResponse = rawResponse;
        request = new RequestToService();
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

    public static ResponseBody buildDirectResponseFromRp(String body) {
        return new ResponseBody(body);
    }
}
