/*
 * Copyright 2019 Yangkai.Shen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.xkcoding.log.event;


import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * <p>
 * 错误日志事件
 * </p>
 *
 * @package: com.xkcoding.log.event
 * @description: 错误日志事件
 * @author: yangkai.shen
 * @date: Created in 2019-03-08 13:32
 * @copyright: Copyright (c) 2019
 * @version: V1.0
 * @modified: yangkai.shen
 */
public class ErrorLogEvent extends ApplicationEvent {

    public ErrorLogEvent(Map<String, Object> source) {
        super(source);
    }

}
