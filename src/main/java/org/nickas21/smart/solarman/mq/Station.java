package org.nickas21.smart.solarman.mq;

import lombok.Data;

@Data
public class Station {
       Long id;             //": 60422108,
       String name;         //": "Dacha_Mostische",
       double locationLat;   //": 50.31023634165624,
       double locationLng;   //": 30.019779655026674,
       String locationAddress;  // ": "Фастівський (Макарівський) р-н, с. Мостище, Садівниче Товариство \"Механізатор\", вул. Лісова, 29",
       Integer regionNationId;  //": 232,
       Integer regionLevel1;    //": 3425,
       Integer regionLevel2;    //": 40874,
       Integer regionLevel3;    //": null,
       Integer regionLevel4;    //": null,
       Integer regionLevel5;    //": null,
       String regionTimezone;  //": "EET",
       String type;    //": "HOUSE_ROOF",
       String gridInterconnectionType; //": "BATTERY_BACKUP",
       Float installedCapacity; //": 12000.0,
       Long startOperatingTime; //": 1681991815.000000000,
       String stationImage; //":  "https://img1.solarmanpv.com/default/13bf3dbf964c438bbd3957bff61acbde1682669717978.jpg",
       Long createdDate;    //": 1681992378.000000000,
       Float batterySoc;    //": 99.0,
       String networkStatus;    //": "NORMAL",
       Float generationPower;   //": 5446.0,
       Long lastUpdateTime; //": 1682665825.000000000,
       String contactPhone; //": null,
       String ownerName;    //": null
}
