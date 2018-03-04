/**********************************************************************************************
 * Copyright 2009 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0/
 * <p>
 * or in the "LICENSE.txt" file accompanying this file. This file is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 * <p>
 * ********************************************************************************************
 * <p>
 * Amazon Product Advertising API
 * Signed Requests Sample Code
 * <p>
 * API Version: 2009-03-31
 */

package mingzuozhibi.service.amazon;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64.Encoder;

public class SignedRequestsHelper {

    private static final String UTF8_CHARSET = "UTF-8";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String REQUEST_URI = "/onca/xml";
    private static final String REQUEST_METHOD = "GET";

    private String endpoint = null;
    private String awsAccessKeyId = null;
    private String awsSecretKey = null;
    private String associateTag = null;

    private SecretKeySpec secretKeySpec = null;
    private Mac mac = null;

    public static SignedRequestsHelper getInstance(
            String endpoint, String awsAccessKeyId, String awsSecretKey, String associateTag)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        SignedRequestsHelper instance = new SignedRequestsHelper();
        instance.endpoint = endpoint.toLowerCase();
        instance.awsAccessKeyId = awsAccessKeyId;
        instance.awsSecretKey = awsSecretKey;
        instance.associateTag = associateTag;

        byte[] secretyKeyBytes = instance.awsSecretKey.getBytes(UTF8_CHARSET);
        instance.secretKeySpec = new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM);
        instance.mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        instance.mac.init(instance.secretKeySpec);

        return instance;
    }

    private SignedRequestsHelper() {
    }

    public String sign(Map<String, String> params) {
        params.put("AWSAccessKeyId", this.awsAccessKeyId);
        params.put("Timestamp", this.timestamp());
        params.put("AssociateTag", this.associateTag);
        SortedMap<String, String> sortedParamMap = new TreeMap<>(params);
        String canonicalQS = this.canonicalize(sortedParamMap);
        String toSign = String.format("%s\n%s\n%s\n%s", REQUEST_METHOD, endpoint, REQUEST_URI, canonicalQS);
        String hmac = this.hmac(toSign);
        String sig = this.percentEncodeRfc3986(hmac);
        return "http://" + this.endpoint + REQUEST_URI + "?" + canonicalQS + "&Signature=" + sig;
    }

    private String hmac(String stringToSign) {
        String signature;
        byte[] data;
        byte[] rawHmac;
        try {
            data = stringToSign.getBytes(UTF8_CHARSET);
            rawHmac = mac.doFinal(data);
            Encoder encoder = Base64.getEncoder();
            signature = new String(encoder.encode(rawHmac));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
        }
        return signature;
    }

    private String timestamp() {
        Calendar cal = Calendar.getInstance();
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dfm.format(cal.getTime());
    }

    private String canonicalize(SortedMap<String, String> sortedParamMap) {
        if (sortedParamMap.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> kvpair = iter.next();
            builder.append(percentEncodeRfc3986(kvpair.getKey()));
            builder.append("=");
            builder.append(percentEncodeRfc3986(kvpair.getValue()));
            if (iter.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    private String percentEncodeRfc3986(String s) {
        String out;
        try {
            out = URLEncoder.encode(s, UTF8_CHARSET)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            out = s;
        }
        return out;
    }

}
