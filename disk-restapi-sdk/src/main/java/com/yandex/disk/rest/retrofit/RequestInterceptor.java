/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.retrofit;

import android.support.annotation.NonNull;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.yandex.disk.rest.CustomHeader;

import java.io.IOException;
import java.util.List;

public class RequestInterceptor implements Interceptor {

    @NonNull
    private final List<CustomHeader> headers;

    public RequestInterceptor(@NonNull final List<CustomHeader> headers) {
        this.headers = headers;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull final Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        for (CustomHeader header : headers) {
            builder.addHeader(header.getName(), header.getValue());
        }
        Request request = builder.method(original.method(), original.body())
                .build();
        return chain.proceed(request);
    }
}