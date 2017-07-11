package org.chromium.chrome.browser.vnc.reg;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("httpInvoker");
    private static final NameValuePair[] EMPTY_NAMEVALUE_PAIRS = new NameValuePair[] {};
    private static final String DEFAULT_CHARSET = "UTF-8";

    private static MultiThreadedHttpConnectionManager connectionManager;
    private static HttpClient httpClient;

    static {
        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.getParams().setConnectionTimeout(3000);
        connectionManager.getParams().setSoTimeout(3000);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(5);
        httpClient = new HttpClient(connectionManager);
    }

    public static String getQuietly(String url) {
        try {
            return get(url);
        } catch (Exception ex) {
            // 捕获异常，但不返回给调用方
        }
        return null;
    }

    public static String get(String url) throws Exception {
        return executeMethod(new GetMethod(url));
    }

    public static String get(String url, Map<String, String> parameters) throws Exception {
        GetMethod getMethod = new GetMethod(url);
        getMethod.setQueryString(buildNameValuePair(parameters));
        return executeMethod(getMethod);
    }

    public static byte[] getResultByte(String url) throws Exception {
        return executeMethodResultByte(new GetMethod(url));
    }

    public static String postQuietly(String url, Map<String, String> parameters) {
        try {
            return post(url, parameters, null, null, null);
        } catch (Exception ex) {
            // ignore exception
        }
        return null;
    }

    public static String postQuietly(String url, Map<String, String> parameters, String contentType, String charset,
            String requestBody) {
        try {
            return post(url, parameters, contentType, charset, requestBody);
        } catch (Exception ex) {
            // ignore exception
        }
        return null;
    }

    public static String post(String url, Map<String, String> parameters) throws Exception {
        return post(url, parameters, null, null, null);
    }

    public static String post(String url, String requestBody, String contentType) throws Exception {
        PostMethod post = new PostMethod(url);
    	if (requestBody != null) {
            try {
				post.setRequestEntity(new StringRequestEntity(requestBody, contentType, "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
                LOGGER.error("The Character Encoding is not supported", ex);
                throw ex;
			}
        }
        return executeMethod(post);
    }

    public static String post(String url, Map<String, String> parameters, String contentType, String charset,
            String requestBody) throws Exception {
        PostMethod post = new PostMethod(url);
        if (requestBody != null) {
            post.setQueryString(buildNameValuePair(parameters));
            try {
                post.setRequestEntity(
                        new StringRequestEntity(requestBody, contentType, charset == null ? DEFAULT_CHARSET : charset));
            } catch (UnsupportedEncodingException ex) {
                LOGGER.error("", ex);
                throw new Exception(ex);
            }
        } else {
            post.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, DEFAULT_CHARSET);
            post.setRequestBody(buildNameValuePair(parameters));
        }
        return executeMethod(post);
    }

    public static String post(String url, Map<String, String> headers, byte[] content) throws Exception {
        PostMethod post = new PostMethod(url);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.setRequestHeader(entry.getKey(), entry.getValue());
            }
        }
        if (content != null) {
            post.setRequestEntity(new ByteArrayRequestEntity(content));
        }
        return executeMethod(post);
    }

    /**
     * 文件上传
     *
     * @param url
     *            上传接口url
     * @param file
     *            上传文件
     * @param fileName
     *            上传文件name
     * @param parameters
     *            上传参数
     * @return 上传接口返回结果
     */
    public static String uploadFile(String url, File file, String fileName, Map<String, String> parameters)
            throws Exception {
        PostMethod post = new PostMethod(url);
        Part[] parts = buildFileAndValuePart(file, fileName, parameters);
        MultipartRequestEntity mre = new MultipartRequestEntity(parts, post.getParams());
        post.setRequestEntity(mre);
        return executeMethod(post);
    }

    private static String executeMethod(HttpMethod method) throws Exception {
        try {
            return new String(executeMethodResultByte(method), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Exception(e);
        }
    }

    private static byte[] executeMethodResultByte(HttpMethod method) throws Exception {
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }

        long startTime = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        long elapsedTime = 0;

        try {
            method.setRequestHeader("Connection", "close");
            statusCode = httpClient.executeMethod(method);
            elapsedTime = System.currentTimeMillis() - startTime;

            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.error("调用http请求失败: " + method.getURI() + ",耗时：" + elapsedTime + "ms, 响应码: " + statusCode);
                throw new Exception("code: " + statusCode + ",调用http服务返回响应错误, url: " + method.getURI() + ",响应码：" + statusCode);
            } else {
                LOGGER.info("调用http请求成功: " + method.getURI() + ",耗时：" + elapsedTime + "ms, 响应码: " + statusCode);
            }
            return IOUtils.toByteArray(method.getResponseBodyAsStream());
        } catch (Exception ex) {
            statusCode = 499;
            try {
                LOGGER.info(
                        "调用http请求异常: " + method.getURI() + ",耗时：" + elapsedTime + "ms, exception:" + ex.getMessage());
            } catch (URIException uriex) {
                // ignore this exception
            }
            if (ex instanceof Exception) {
                throw (Exception) ex;
            } else {
                throw new Exception(ex);
            }
        } finally {
            method.releaseConnection();
        }
    }

    private static NameValuePair[] buildNameValuePair(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return EMPTY_NAMEVALUE_PAIRS;
        }
        NameValuePair[] nameValuePairs = new NameValuePair[parameters.size()];

        List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(parameters.size());
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            nameValuePairList.add(new NameValuePair(entry.getKey(), entry.getValue()));
        }
        nameValuePairList.toArray(nameValuePairs);
        return nameValuePairs;
    }

    private static Part[] buildFileAndValuePart(File file, String fileName, Map<String, String> parameters) throws Exception {
        Part filePart = null;
        try {
            filePart = new FilePart(fileName, file);
        } catch (FileNotFoundException ex) {
            LOGGER.error("", ex);
            throw new Exception(ex);
        }
        if (parameters == null || parameters.isEmpty()) {
            return new Part[] { filePart };
        }
        Part[] parts = new Part[parameters.size() + 1];
        List<Part> partList = new ArrayList<Part>(parameters.size() + 1);
        partList.add(filePart);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            partList.add(new StringPart(entry.getKey(), entry.getValue()));
        }
        partList.toArray(parts);
        return parts;
    }

    public static void main(String[] args) throws Exception {
        // for(int i = 0;i < 10000; i++){
        // get("http://report.vivotest.com/login.jsp");
        // System.out.println(i);
        // }
        for (int i = 0; i < 100; i++) {
            // if(!InetAddress.getByName("report.vivo-test.com").getHostAddress().equals("192.168.2.70")){
            // break;
            // }
            // System.out.println(i);
            System.out.println(get("http://report.vivo-test.com/testReport/report"));
        }
    }

}
