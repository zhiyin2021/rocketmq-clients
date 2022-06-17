/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.client.impl;

import io.grpc.Metadata;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.ClientException;
import org.apache.rocketmq.client.misc.MixAll;
import org.apache.rocketmq.client.remoting.Credentials;
import org.apache.rocketmq.client.remoting.CredentialsProvider;
import org.apache.rocketmq.client.remoting.TlsHelper;
import org.apache.rocketmq.utility.MetadataUtils;
import org.apache.rocketmq.utility.RequestIdGenerator;

public class Signature {
    public static final String TENANT_ID_KEY = "x-mq-tenant-id";
    public static final String NAMESPACE_KEY = "x-mq-namespace";
    public static final String AUTHORIZATION_KEY = "authorization";
    public static final String DATE_TIME_KEY = "x-mq-date-time";

    public static final String SESSION_TOKEN_KEY = "x-mq-session-token";

    public static final String REQUEST_ID_KEY = "x-mq-request-id";
    public static final String LANGUAGE_KEY = "x-mq-language";
    public static final String CLIENT_VERSION_KEY = "x-mq-client-version";
    public static final String PROTOCOL_VERSION = "x-mq-protocol";

    public static final String ALGORITHM = "MQv2-HMAC-SHA1";
    public static final String CREDENTIAL = "Credential";
    public static final String SIGNED_HEADERS = "SignedHeaders";
    public static final String SIGNATURE = "Signature";
    public static final String DATE_TIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

    private Signature() {
    }

    public static Metadata sign(ClientConfig config)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, ClientException {

        Metadata metadata = new Metadata();
        final String tenantId = config.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            metadata.put(Metadata.Key.of(TENANT_ID_KEY, Metadata.ASCII_STRING_MARSHALLER), tenantId);
        }

        metadata.put(Metadata.Key.of(LANGUAGE_KEY, Metadata.ASCII_STRING_MARSHALLER), "JAVA");
        metadata.put(Metadata.Key.of(PROTOCOL_VERSION, Metadata.ASCII_STRING_MARSHALLER),
                     MixAll.getProtocolVersion());
        metadata.put(Metadata.Key.of(CLIENT_VERSION_KEY, Metadata.ASCII_STRING_MARSHALLER), MetadataUtils.getVersion());

        final String namespace = config.getNamespace();
        if (StringUtils.isNotBlank(namespace)) {
            metadata.put(Metadata.Key.of(NAMESPACE_KEY, Metadata.ASCII_STRING_MARSHALLER), namespace);
        }

        String dateTime = new SimpleDateFormat(DATE_TIME_FORMAT).format(new Date());
        metadata.put(Metadata.Key.of(DATE_TIME_KEY, Metadata.ASCII_STRING_MARSHALLER), dateTime);

        final String requestId = RequestIdGenerator.getInstance().next();
        metadata.put(Metadata.Key.of(REQUEST_ID_KEY, Metadata.ASCII_STRING_MARSHALLER), requestId);

        final CredentialsProvider provider = config.getCredentialsProvider();
        if (null == provider) {
            return metadata;
        }
        final Credentials credentials = provider.getCredentials();
        if (null == credentials) {
            return metadata;
        }

        final String securityToken = credentials.getSecurityToken();
        if (StringUtils.isNotBlank(securityToken)) {
            metadata.put(Metadata.Key.of(SESSION_TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER), securityToken);
        }

        final String accessKey = credentials.getAccessKey();
        final String accessSecret = credentials.getAccessSecret();

        if (StringUtils.isBlank(accessKey)) {
            return metadata;
        }

        if (StringUtils.isBlank(accessSecret)) {
            return metadata;
        }

        final String regionId = config.getRegionId();
        final String serviceName = config.getServiceName();

        String sign = TlsHelper.sign(accessSecret, dateTime);

        final String authorization = ALGORITHM
                                     + " "
                                     + CREDENTIAL
                                     + "="
                                     + accessKey
                                     + "/"
                                     + regionId
                                     + "/"
                                     + serviceName
                                     + ", "
                                     + SIGNED_HEADERS
                                     + "="
                                     + DATE_TIME_KEY
                                     + ", "
                                     + SIGNATURE
                                     + "="
                                     + sign;

        metadata.put(Metadata.Key.of(AUTHORIZATION_KEY, Metadata.ASCII_STRING_MARSHALLER), authorization);
        return metadata;
    }
}