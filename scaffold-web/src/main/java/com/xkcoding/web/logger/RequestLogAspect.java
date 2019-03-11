/*
 * Copyright 2019 Yangkai.Shen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.xkcoding.web.logger;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xkcoding.common.utils.ClassUtil;
import com.xkcoding.common.utils.WebUtil;
import com.xkcoding.launcher.constants.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Spring boot 控制器 请求日志，方便代码调试
 * </p>
 *
 * @package: com.xkcoding.web.logger
 * @description: Spring boot 控制器 请求日志，方便代码调试
 * @author: yangkai.shen
 * @date: Created in 2019-03-08 18:25
 * @copyright: Copyright (c) 2019
 * @version: V1.0
 * @modified: yangkai.shen
 */
@Slf4j
@Aspect
@Configuration
@Profile({AppConstant.DEV_CODE, AppConstant.TEST_CODE})
public class RequestLogAspect {

    /**
     * AOP 环切 控制器 R 返回值
     *
     * @param point JoinPoint
     * @return Object
     * @throws Throwable 异常
     */
    @Around("execution(!static com.xkcoding.common.api.R *(..)) && " + "(@within(org.springframework.stereotype.Controller) || " + "@within(org.springframework.web.bind.annotation.RestController))")
    public Object aroundApi(ProceedingJoinPoint point) throws Throwable {
        MethodSignature ms = (MethodSignature) point.getSignature();
        Method method = ms.getMethod();
        Object[] args = point.getArgs();
        final Map<String, Object> paraMap = new HashMap<>(16);
        for (int i = 0; i < args.length; i++) {
            MethodParameter methodParam = ClassUtil.getMethodParameter(method, i);
            PathVariable pathVariable = methodParam.getParameterAnnotation(PathVariable.class);
            if (pathVariable != null) {
                continue;
            }
            RequestBody requestBody = methodParam.getParameterAnnotation(RequestBody.class);
            Object object = args[i];
            // 如果是body的json则是对象
            if (requestBody != null && object != null) {
                paraMap.putAll(BeanUtil.beanToMap(object));
            } else {
                RequestParam requestParam = methodParam.getParameterAnnotation(RequestParam.class);
                String paraName;
                if (requestParam != null && StrUtil.isNotBlank(requestParam.value())) {
                    paraName = requestParam.value();
                } else {
                    paraName = methodParam.getParameterName();
                }
                paraMap.put(paraName, object);
            }
        }
        HttpServletRequest request = WebUtil.getRequest();
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();
        // 处理 参数
        List<String> needRemoveKeys = new ArrayList<>(paraMap.size());
        paraMap.forEach((key, value) -> {
            if (value instanceof HttpServletRequest) {
                needRemoveKeys.add(key);
                paraMap.putAll(((HttpServletRequest) value).getParameterMap());
            } else if (value instanceof HttpServletResponse) {
                needRemoveKeys.add(key);
            } else if (value instanceof InputStream) {
                needRemoveKeys.add(key);
            } else if (value instanceof MultipartFile) {
                String fileName = ((MultipartFile) value).getOriginalFilename();
                paraMap.put(key, fileName);
            } else if (value instanceof InputStreamSource) {
                needRemoveKeys.add(key);
            } else if (value instanceof WebRequest) {
                needRemoveKeys.add(key);
                paraMap.putAll(((WebRequest) value).getParameterMap());
            }
        });
        needRemoveKeys.forEach(paraMap::remove);
        // 构建成一条长 日志，避免并发下日志错乱
        StringBuilder logBuilder = new StringBuilder(500);
        // 日志参数
        List<Object> logArgs = new ArrayList<>();
        logBuilder.append("\n\n================  Request Start  ================\n");
        // 打印请求
        if (paraMap.isEmpty()) {
            logBuilder.append("===> {}: {}\n");
            logArgs.add(requestMethod);
            logArgs.add(requestURI);
        } else {
            logBuilder.append("===> {}: {} Parameters: {}\n");
            logArgs.add(requestMethod);
            logArgs.add(requestURI);
            logArgs.add(JSONUtil.toJsonStr(paraMap));
        }
        // 打印请求头
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
            logBuilder.append("===headers===  {} : {}\n");
            logArgs.add(headerName);
            logArgs.add(headerValue);
        }
        // 打印执行时间
        long startNs = System.nanoTime();
        try {
            Object result = point.proceed();
            logBuilder.append("===Result===  {}\n");
            logArgs.add(JSONUtil.toJsonStr(result));
            return result;
        } finally {
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            logBuilder.append("<=== {}: {} ({} ms)");
            logArgs.add(requestMethod);
            logArgs.add(requestURI);
            logArgs.add(tookMs);
            logBuilder.append("\n================   Request End   ================\n");
            log.info(logBuilder.toString(), logArgs.toArray());
        }
    }

}
