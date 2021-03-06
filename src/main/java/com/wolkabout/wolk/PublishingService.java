/*
 * Copyright 2016 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wolkabout.wolk;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

class PublishingService {

    private static final String TOPIC = "sensors/";
    private static final MQTT mqtt = new MQTT();
    private static final String FACTORY_TYPE = "X.509";
    private static final String CERTIFICATE_NAME = "ca.crt";

    private String HOST = "ssl://wolksense.com:8883";
    private final Device device;

    PublishingService(Device device) {
        this.device = device;
        initMqtt();
    }

    public void setHOST(String HOST) {
        this.HOST = HOST;
    }

    private void initMqtt() {
        try {
            final Certificate certificate = getCertificate();
            final TrustManagerFactory trustManagerFactory = getTrustManagerFactory(certificate);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            mqtt.setSslContext(sslContext);
            mqtt.setConnectAttemptsMax(2);
            mqtt.setHost(HOST);
            mqtt.setUserName(device.getSerialId());
            mqtt.setPassword(device.getPassword());
        } catch (Exception e) {
            System.out.print("Unable to instantiate MQTT.");
        }
    }

    private Certificate getCertificate() throws Exception {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance(FACTORY_TYPE);
        try (InputStream certificateString = getClass().getClassLoader().getResourceAsStream(CERTIFICATE_NAME)) {
            return certificateFactory.generateCertificate(certificateString);
        }
    }

    private TrustManagerFactory getTrustManagerFactory(final Certificate certificate) throws Exception {
        // creating a KeyStore containing our trusted CAs
        final String keyStoreType = KeyStore.getDefaultType();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", certificate);

        // creating a TrustManager that trusts the CAs in our KeyStore
        final String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
    }

    void publish(String message) throws Exception {
        final BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();
        try {
            connection.publish(TOPIC + device.getSerialId(), message.getBytes(), QoS.AT_LEAST_ONCE, false);
        } finally {
            connection.disconnect();
        }
    }

}
