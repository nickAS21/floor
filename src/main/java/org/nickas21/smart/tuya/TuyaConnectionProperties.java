/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.nickas21.smart.tuya;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("connector.tuya")
public class TuyaConnectionProperties {
    private TuyaRegion region = TuyaRegion.EU;
    private String ak;
    private String sk;
    private String[] deviceIds;
    private String userUid;

    enum TuyaRegion {

        /**
         * China
         */
        CN("https://openapi.tuyacn.com", "pulsar+ssl://mqe.tuyacn.com:7285/"),
        /**
         * US WEST
         */
        US("https://openapi.tuyaus.com", "pulsar+ssl://mqe.tuyaus.com:7285/"),
        /**
         * US EAST
         */
        US_EAST("https://openapi-ueaz.tuyaus.com", "pulsar+ssl://mqe.tuyaus.com:7285/"),
        /**
         * European
         */
        EU("https://openapi.tuyaeu.com", "pulsar+ssl://mqe.tuyaeu.com:7285/"),
        /**
         * Europe West
         */
        EU_WEST("https://openapi-weaz.tuyaeu.com", "pulsar+ssl://mqe.tuyaeu.com:7285/"),
        /**
         * India
         */
        IN("https://openapi.tuyain.com", "pulsar+ssl://mqe.tuyain.com:7285/");

        private final String apiUrl;

        private final String msgUrl;

        TuyaRegion(String apiUrl, String msgUrl) {
            this.apiUrl = apiUrl;
            this.msgUrl = msgUrl;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getMsgUrl() {
            return msgUrl;
        }
    }
}

