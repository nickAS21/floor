package org.nickas21.smart.tuya.constant;

public enum TuyaCategory {

    CZ("cz", "Socket"),
    DCB("dcb", "Battery Pack"),

    VOCIE_PLATFORM_ONLY("vocie_platform_only", "Voice Platform");


    private final String code;

    private final String name;


    TuyaCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

//    {
//        "result": [
//        {
//            "code": "cz",
//                "name": "Socket"
//        },
//        {
//            "code": "dcb",
//                "name": "Battery Pack"
//        },
//        {
//            "code": "pc",
//                "name": "Power Strip"
//        },
//        {
//            "code": "aqcz",
//                "name": "Metering socket"
//        },
//        {
//            "code": "wkcz",
//                "name": "Temperature control socket"
//        },
//        {
//            "code": "ckmb",
//                "name": "RGB Touch Panel"
//        },
//        {
//            "code": "znspq",
//                "name": "Adapter"
//        },
//        {
//            "code": "kg",
//                "name": "Switch"
//        },
//        {
//            "code": "tgkg",
//                "name": "Dimmer Switch"
//        },
//        {
//            "code": "clkg",
//                "name": "Curtain Switch"
//        },
//        {
//            "code": "cjkg",
//                "name": "Scenario Switch"
//        },
//        {
//            "code": "wxkg",
//                "name": "Wireless Switch"
//        },
//        {
//            "code": "fskg",
//                "name": "Fan Switch"
//        },
//        {
//            "code": "ckqdkg",
//                "name": "Card Switch"
//        },
//        {
//            "code": "wxml",
//                "name": "Wireless Doorbell"
//        },
//        {
//            "code": "tdq",
//                "name": "Breaker"
//        },
//        {
//            "code": "ckmkzq",
//                "name": "Garage Door Opener"
//        },
//        {
//            "code": "xdkzq",
//                "name": "Disinfection controller"
//        },
//        {
//            "code": "zigbeeqc",
//                "name": "Wall Socket"
//        },
//        {
//            "code": "qjdcz",
//                "name": "Scenario Light Socket"
//        },
//        {
//            "code": "zigbeekt",
//                "name": "Air Conditioner Mate"
//        },
//        {
//            "code": "znjxs",
//                "name": "Smart Manipulator Arm"
//        },
//        {
//            "code": "dz",
//                "name": "Electric Drill"
//        },
//        {
//            "code": "PDpc",
//                "name": "Power strip"
//        },
//        {
//            "code": "znjrf",
//                "name": "heating suit"
//        },
//        {
//            "code": "jlmkzq",
//                "name": "Rolling door controller"
//        },
//        {
//            "code": "zncdq",
//                "name": "Professional charger"
//        },
//        {
//            "code": "gcmkzq",
//                "name": "Barn Door"
//        },
//        {
//            "code": "hwsb",
//                "name": "Water Pump"
//        },
//        {
//            "code": "sjx",
//                "name": "data cable"
//        },
//        {
//            "code": "dgncz",
//                "name": "Temperature and humidity socket"
//        },
//        {
//            "code": "cljqr",
//                "name": "Curtain Robot"
//        },
//        {
//            "code": "wxjrq",
//                "name": "Mosquito repellent heater"
//        },
//        {
//            "code": "dbs",
//                "name": "Electric Wrench"
//        },
//        {
//            "code": "dj",
//                "name": "Light Source"
//        },
//        {
//            "code": "gqzm",
//                "name": "Public Area Lighting"
//        },
//        {
//            "code": "tyd",
//                "name": "Garden Light"
//        },
//        {
//            "code": "fwd",
//                "name": "Ambient Light"
//        },
//        {
//            "code": "zwd",
//                "name": "Grow Light"
//        },
//        {
//            "code": "cxd",
//                "name": "Magnetic Fastening Light"
//        },
//        {
//            "code": "lzd",
//                "name": "Candle Lamp"
//        },
//        {
//            "code": "gkd",
//                "name": "Bay Light"
//        },
//        {
//            "code": "tccd",
//                "name": "Parking Lot Light"
//        },
//        {
//            "code": "fbd",
//                "name": "Explosion-proof Light"
//        },
//        {
//            "code": "sfd",
//                "name": "Tri-proof Light"
//        },
//        {
//            "code": "tpd",
//                "name": "Canopy Light"
//        },
//        {
//            "code": "fgd",
//                "name": "Flood Light"
//        },
//        {
//            "code": "yjd",
//                "name": "Emergency Light"
//        },
//        {
//            "code": "ggd",
//                "name": "High-mast Light"
//        },
//        {
//            "code": "qcd",
//                "name": "Sports Light"
//        },
//        {
//            "code": "zwszd",
//                "name": "Horticulture Light"
//        },
//        {
//            "code": "gld",
//                "name": "Light Therapy Lamp"
//        },
//        {
//            "code": "qxtyd",
//                "name": "Holographic Imaging Light"
//        },
//        {
//            "code": "sjyd",
//                "name": "Salt Lamp"
//        },
//        {
//            "code": "ryd",
//                "name": "Lava Lamp"
//        },
//        {
//            "code": "bgd",
//                "name": "Fill Light"
//        },
//        {
//            "code": "zjd",
//                "name": "Batten Light"
//        },
//        {
//            "code": "zsd",
//                "name": "Chandelier"
//        },
//        {
//            "code": "xtd",
//                "name": "Linear Luminarie"
//        },
//        {
//            "code": "jqd",
//                "name": "Mirror Light"
//        },
//        {
//            "code": "bd",
//                "name": "wall light"
//        },
//        {
//            "code": "tyndc",
//                "name": "Solar String Light"
//        },
//        {
//            "code": "tynyddy",
//                "name": "Solar Power Bank"
//        },
//        {
//            "code": "zmdg",
//                "name": "LED Tube"
//        },
//        {
//            "code": "ybd",
//                "name": "Yuba Light"
//        },
//        {
//            "code": "ztd",
//                "name": "Candleholder Lamp"
//        },
//        {
//            "code": "rgbsb",
//                "name": "RGB Mouse"
//        },
//        {
//            "code": "hcsbd",
//                "name": "Dreamcolor Mouse Mat"
//        },
//        {
//            "code": "3dd",
//                "name": "Three-dimensional Lamp"
//        },
//        {
//            "code": "leddzp",
//                "name": "LED Electronic Display"
//        },
//        {
//            "code": "zbd",
//                "name": "Selfie Light"
//        },
//        {
//            "code": "tyntyd",
//                "name": "Solar Pathway Light"
//        },
//        {
//            "code": "tyngyd",
//                "name": "Solar Motion Sensor Light"
//        },
//        {
//            "code": "tynlyd",
//                "name": "Solar Camping Light"
//        },
//        {
//            "code": "zmdd",
//                "name": "Pendant Light"
//        },
//        {
//            "code": "zmcgd",
//                "name": "Cabinet Light"
//        },
//        {
//            "code": "sdt",
//                "name": "flash light"
//        },
//        {
//            "code": "xdsjd",
//                "name": "Germicidal Light"
//        },
//        {
//            "code": "gxd",
//                "name": "Fiber Optic Light"
//        },
//        {
//            "code": "hzjd",
//                "name": "Makeup Light"
//        },
//        {
//            "code": "sbd",
//                "name": "Paper Lantern"
//        },
//        {
//            "code": "tynafd",
//                "name": "Solar Security Light"
//        },
//        {
//            "code": "gf",
//                "name": "Photovoltaics"
//        },
//        {
//            "code": "xnykq",
//                "name": "Knob Remote Control"
//        },
//        {
//            "code": "cmykq",
//                "name": "Touch Remote Control"
//        },
//        {
//            "code": "yzd",
//                "name": "Farming Light"
//        },
//        {
//            "code": "jsd",
//                "name": "Classroom Light"
//        },
//        {
//            "code": "hbd",
//                "name": "Blackboard Light"
//        },
//        {
//            "code": "sxtpmtbq",
//                "name": "Camera Screen Sync Controller"
//        },
//        {
//            "code": "zmkzq",
//                "name": "LED Controller"
//        },
//        {
//            "code": "sntydj",
//                "name": "Indoor Lighting"
//        },
//        {
//            "code": "syzmqd",
//                "name": "Commercial LED Driver"
//        },
//        {
//            "code": "hdmipmtbq",
//                "name": "HDMI Screen Sync Controller"
//        },
//        {
//            "code": "hyd",
//                "name": "Flame Bulb"
//        },
//        {
//            "code": "hctyd",
//                "name": "Dreamcolor Garden Light"
//        },
//        {
//            "code": "dd",
//                "name": "Strip Lights"
//        },
//        {
//            "code": "xdd",
//                "name": "Ceiling Light"
//        },
//        {
//            "code": "gyd",
//                "name": "PIR Light"
//        },
//        {
//            "code": "wbldgyd",
//                "name": "Microwave radar sensor light"
//        },
//        {
//            "code": "fsd",
//                "name": "Ceiling Fan"
//        },
//        {
//            "code": "tyndj",
//                "name": "solar light"
//        },
//        {
//            "code": "dc",
//                "name": "String Lights"
//        },
//        {
//            "code": "td",
//                "name": "Table Lmap/Floor Lamp"
//        },
//        {
//            "code": "hcdc",
//                "name": "Colorful String Light"
//        },
//        {
//            "code": "tgq",
//                "name": "Light Modulator"
//        },
//        {
//            "code": "txd",
//                "name": "Downlight"
//        },
//        {
//            "code": "sxd",
//                "name": "Spotlight"
//        },
//        {
//            "code": "hwtydj",
//                "name": "Solution for outdoor light"
//        },
//        {
//            "code": "dsd",
//                "name": "Filament lamp"
//        },
//        {
//            "code": "zmqd",
//                "name": "Lighting Driver"
//        },
//        {
//            "code": "pjd",
//                "name": "DIY Light Panels"
//        },
//        {
//            "code": "mbd",
//                "name": "Panel Light"
//        },
//        {
//            "code": "pd",
//                "name": "PAR Light"
//        },
//        {
//            "code": "ld",
//                "name": "Street light"
//        },
//        {
//            "code": "ykq",
//                "name": "Lighting remote control"
//        },
//        {
//            "code": "dsdsoc",
//                "name": "Ceiling fan light"
//        },
//        {
//            "code": "xyd",
//                "name": "Night light"
//        },
//        {
//            "code": "xktyd",
//                "name": "Star projector"
//        },
//        {
//            "code": "djb",
//                "name": "Light"
//        },
//        {
//            "code": "g9",
//                "name": "G9 Light"
//        },
//        {
//            "code": "gu10",
//                "name": "GU10 Light"
//        },
//        {
//            "code": "kt",
//                "name": "Air Conditioner"
//        },
//        {
//            "code": "bx",
//                "name": "Refrigerator"
//        },
//        {
//            "code": "xy",
//                "name": "Washing Machine"
//        },
//        {
//            "code": "rs",
//                "name": "Water Heater"
//        },
//        {
//            "code": "bgl",
//                "name": "Boiler"
//        },
//        {
//            "code": "znrb",
//                "name": "Smart Heat Pump"
//        },
//        {
//            "code": "ydkt",
//                "name": "Portable Air Conditioner"
//        },
//        {
//            "code": "gyj",
//                "name": "Clothes Dryer"
//        },
//        {
//            "code": "xbx",
//                "name": "Mini Refrigerator"
//        },
//        {
//            "code": "sd",
//                "name": "Robot Vacuum"
//        },
//        {
//            "code": "tdjqr",
//                "name": "Automatic Mopping Robot"
//        },
//        {
//            "code": "yech",
//                "name": "Baby scale"
//        },
//        {
//            "code": "xdj",
//                "name": "Disinfection machine"
//        },
//        {
//            "code": "xfs_1",
//                "name": "Mini fan"
//        },
//        {
//            "code": "ds_1",
//                "name": "beacon ceiling fan"
//        },
//        {
//            "code": "zm_wy_fs",
//                "name": "Leaves the table fan"
//        },
//        {
//            "code": "fsyk",
//                "name": "an remote control"
//        },
//        {
//            "code": "ccjqr",
//                "name": "Window Cleaner"
//        },
//        {
//            "code": "gcj",
//                "name": "Mower"
//        },
//        {
//            "code": "xcq",
//                "name": "Vacuum Cleaner"
//        },
//        {
//            "code": "cwclt",
//                "name": "Pet Food Bucket"
//        },
//        {
//            "code": "fs",
//                "name": "Fan"
//        },
//        {
//            "code": "ks",
//                "name": "Air Cooler"
//        },
//        {
//            "code": "scxdj",
//                "name": "Hand-held Mopping Robot"
//        },
//        {
//            "code": "xxj",
//                "name": "Diffuser"
//        },
//        {
//            "code": "jsq",
//                "name": "Humidifier"
//        },
//        {
//            "code": "ycjqr",
//                "name": "Pool Robot"
//        },
//        {
//            "code": "cs",
//                "name": "Dehumidifier"
//        },
//        {
//            "code": "kj",
//                "name": "Air Purifier"
//        },
//        {
//            "code": "cwq",
//                "name": "Deodorizer"
//        },
//        {
//            "code": "qn",
//                "name": "Heater"
//        },
//        {
//            "code": "dr",
//                "name": "Electric Blanket"
//        },
//        {
//            "code": "mjj",
//                "name": "Towel rack"
//        },
//        {
//            "code": "dbl",
//                "name": "Electric fireplace"
//        },
//        {
//            "code": "dnz",
//                "name": "Electric Heating Table"
//        },
//        {
//            "code": "cwmw",
//                "name": "Pet House"
//        },
//        {
//            "code": "cwwsq",
//                "name": "Pet Feeder"
//        },
//        {
//            "code": "cwwsqipc",
//                "name": "Pet Feeder with IPC"
//        },
//        {
//            "code": "cwysj",
//                "name": "Pet Water Feeder"
//        },
//        {
//            "code": "cwwqfsq",
//                "name": "Pet Ball Thrower"
//        },
//        {
//            "code": "cwtswsq",
//                "name": "Pet Treat Feeder"
//        },
//        {
//            "code": "msp",
//                "name": "Cat Toilet"
//        },
//        {
//            "code": "dcq",
//                "name": "Pet Toys Ball"
//        },
//        {
//            "code": "szpc",
//                "name": "Aquarium Power Strip"
//        },
//        {
//            "code": "ygwsq",
//                "name": "Fish Feeder"
//        },
//        {
//            "code": "ygsb",
//                "name": "Aquarium Pump"
//        },
//        {
//            "code": "ygjrb",
//                "name": "Aquarium Heaters"
//        },
//        {
//            "code": "sz",
//                "name": "Plant Grower"
//        },
//        {
//            "code": "szj",
//                "name": "Shredder"
//        },
//        {
//            "code": "zndsj",
//                "name": "Smart TV Shelf"
//        },
//        {
//            "code": "sjz",
//                "name": "Lifting Table"
//        },
//        {
//            "code": "clykq",
//                "name": "Curtain Remote"
//        },
//        {
//            "code": "yb",
//                "name": "Bathroom Heater"
//        },
//        {
//            "code": "ysj",
//                "name": "Bathroom Mirror"
//        },
//        {
//            "code": "mwd",
//                "name": "Mosquito Lamp"
//        },
//        {
//            "code": "ktkzq",
//                "name": "Air Conditioner"
//        },
//        {
//            "code": "lyz",
//                "name": "Shower Column"
//        },
//        {
//            "code": "petfinder",
//                "name": "Pet Finder"
//        },
//        {
//            "code": "tjx",
//                "name": "Electric Baseboard Heater"
//        },
//        {
//            "code": "cwbb",
//                "name": "Pet Bag"
//        },
//        {
//            "code": "cwhgx",
//                "name": "Pet Dryer"
//        },
//        {
//            "code": "cwjwq",
//                "name": "Pet Odor Remover"
//        },
//        {
//            "code": "cwqys",
//                "name": "Pet Leash"
//        },
//        {
//            "code": "xgmjq",
//                "name": "Shoe cabinet sterilizer"
//        },
//        {
//            "code": "yyy",
//                "name": "Baby Rocking Chair"
//        },
//        {
//            "code": "yec",
//                "name": "Crib"
//        },
//        {
//            "code": "etaqzy",
//                "name": "Child Safety Seat"
//        },
//        {
//            "code": "xnq",
//                "name": "Breast pump"
//        },
//        {
//            "code": "ywhjj",
//                "name": "Wash Aid"
//        },
//        {
//            "code": "ljt",
//                "name": "Mintpass Rubbish"
//        },
//        {
//            "code": "mt",
//                "name": "Smart Toilet"
//        },
//        {
//            "code": "znnz",
//                "name": "Smart alarm clock"
//        },
//        {
//            "code": "ntq",
//                "name": "HVAC"
//        },
//        {
//            "code": "dyj",
//                "name": "Printer"
//        },
//        {
//            "code": "yug",
//                "name": "Fish Tank"
//        },
//        {
//            "code": "yyh",
//                "name": "Music Box"
//        },
//        {
//            "code": "tyykq",
//                "name": "Remote Control"
//        },
//        {
//            "code": "wk",
//                "name": "Thermostat"
//        },
//        {
//            "code": "wkf",
//                "name": "TRV"
//        },
//        {
//            "code": "xfj",
//                "name": "Ventilation System"
//        },
//        {
//            "code": "bmwk",
//                "name": "North American thermostat"
//        },
//        {
//            "code": "hwktwkq",
//                "name": "IR Air Thermostat"
//        },
//        {
//            "code": "cl",
//                "name": "Curtain"
//        },
//        {
//            "code": "zigbeecm",
//                "name": "Curtain Motor"
//        },
//        {
//            "code": "lyj",
//                "name": "Drying Rack"
//        },
//        {
//            "code": "mc",
//                "name": "Door/Window Controller"
//        },
//        {
//            "code": "xg",
//                "name": "Shoe cabinet"
//        },
//        {
//            "code": "sf",
//                "name": "Smart Sofa"
//        },
//        {
//            "code": "ttm",
//                "name": "Tatami"
//        },
//        {
//            "code": "dg",
//                "name": "Wall Cabinet"
//        },
//        {
//            "code": "jdcljqr",
//                "name": "Curtain robot"
//        },
//        {
//            "code": "szjqr",
//                "name": "Finger robot"
//        },
//        {
//            "code": "kqzg",
//                "name": "Air Fryer"
//        },
//        {
//            "code": "kfj",
//                "name": "Coffee Maker"
//        },
//        {
//            "code": "yinsj",
//                "name": "Water Dispenser"
//        },
//        {
//            "code": "xcj",
//                "name": "Vegetable cleaner"
//        },
//        {
//            "code": "dsq",
//                "name": "Timer"
//        },
//        {
//            "code": "dylg",
//                "name": "Electric Pressure Cooker"
//        },
//        {
//            "code": "dkl",
//                "name": "Electric Grill"
//        },
//        {
//            "code": "swzkfkj",
//                "name": "Vacuum Sealer"
//        },
//        {
//            "code": "df",
//                "name": "Rice Cooker"
//        },
//        {
//            "code": "mzj",
//                "name": "Sous Vide Cooker"
//        },
//        {
//            "code": "kx",
//                "name": "Oven"
//        },
//        {
//            "code": "bh",
//                "name": "Smart Kettle"
//        },
//        {
//            "code": "wb",
//                "name": "Microwave"
//        },
//        {
//            "code": "cn",
//                "name": "Milk Dispenser"
//        },
//        {
//            "code": "cyljclq",
//                "name": "Food Waste Disposer"
//        },
//        {
//            "code": "mg",
//                "name": "Rice cabinet"
//        },
//        {
//            "code": "pbj",
//                "name": "Blender"
//        },
//        {
//            "code": "ccj",
//                "name": "Cooker"
//        },
//        {
//            "code": "ggj",
//                "name": "Dehydrator"
//        },
//        {
//            "code": "js",
//                "name": "Water Purifier"
//        },
//        {
//            "code": "tnq",
//                "name": "Smart Milk Kettle"
//        },
//        {
//            "code": "zx",
//                "name": "Steam Oven"
//        },
//        {
//            "code": "xdg",
//                "name": "Sterilizer"
//        },
//        {
//            "code": "llj",
//                "name": "Food Processor"
//        },
//        {
//            "code": "xwj",
//                "name": "Dishwasher"
//        },
//        {
//            "code": "xd",
//                "name": "Disinfection Cabinet"
//        },
//        {
//            "code": "jcz",
//                "name": "Integrated Kitchen"
//        },
//        {
//            "code": "fsj",
//                "name": "Babycook"
//        },
//        {
//            "code": "swtz",
//                "name": "Food Probe"
//        },
//        {
//            "code": "yexdg",
//                "name": "Baby Sterilizer"
//        },
//        {
//            "code": "ysh",
//                "name": "Health pot"
//        },
//        {
//            "code": "nnq",
//                "name": "Bottle Warmer"
//        },
//        {
//            "code": "snj",
//                "name": "Yogurt Maker"
//        },
//        {
//            "code": "znlt",
//                "name": "Smart Faucet"
//        },
//        {
//            "code": "zzj",
//                "name": "Juicer"
//        },
//        {
//            "code": "djj",
//                "name": "Soymilk Maker"
//        },
//        {
//            "code": "mb",
//                "name": "Bread Maker"
//        },
//        {
//            "code": "jyj",
//                "name": "Drinking Water Dispensers"
//        },
//        {
//            "code": "rq",
//                "name": "Gas Stove"
//        },
//        {
//            "code": "bqlj",
//                "name": "Ice Cream Machine"
//        },
//        {
//            "code": "yyj",
//                "name": "Hood"
//        },
//        {
//            "code": "dcl",
//                "name": "Induction Cooker"
//        },
//        {
//            "code": "zbj",
//                "name": "Ice Machine"
//        },
//        {
//            "code": "znfh",
//                "name": "Bento Box"
//        },
//        {
//            "code": "cbj",
//                "name": "Tea Machine"
//        },
//        {
//            "code": "jg",
//                "name": "Wine cabinet"
//        },
//        {
//            "code": "znsjq",
//                "name": "Smart Pump"
//        },
//        {
//            "code": "afcg",
//                "name": "sensor"
//        },
//        {
//            "code": "ggkzq",
//                "name": "Lighting Controller"
//        },
//        {
//            "code": "fkq",
//                "name": "Card Issuer（Wi-Fi）"
//        },
//        {
//            "code": "dzbq",
//                "name": "Electronic Label"
//        },
//        {
//            "code": "timer",
//                "name": "Fixed Time"
//        },
//        {
//            "code": "cwy",
//                "name": "Thermometer"
//        },
//        {
//            "code": "jbq",
//                "name": "Pedometer"
//        },
//        {
//            "code": "hjjcy",
//                "name": "Environmental detector"
//        },
//        {
//            "code": "wlj",
//                "name": "Grip meter"
//        },
//        {
//            "code": "dltcq",
//                "name": "Current Detector"
//        },
//        {
//            "code": "szjcy",
//                "name": "Water quality detector"
//        },
//        {
//            "code": "dytcq",
//                "name": "Voltage Detector"
//        },
//        {
//            "code": "znykq",
//                "name": "Smart remote"
//        },
//        {
//            "code": "gykzq",
//                "name": "Motion Sensor Controller"
//        },
//        {
//            "code": "fgycgq",
//                "name": "Wind and rain sensor"
//        },
//        {
//            "code": "cjy",
//                "name": "Laser  distance meter"
//        },
//        {
//            "code": "ybq",
//                "name": "Greeter"
//        },
//        {
//            "code": "csbcgq",
//                "name": "Ultrasonic Parking Detector"
//        },
//        {
//            "code": "wsdykq",
//                "name": "Temp Humidity Remote Control"
//        },
//        {
//            "code": "wbcgq",
//                "name": "Microwave sensor"
//        },
//        {
//            "code": "zwcgq",
//                "name": "Occupancy Sensor"
//        },
//        {
//            "code": "jjjcy",
//                "name": "Alcohol Detector"
//        },
//        {
//            "code": "zdshj",
//                "name": "Vending Machine"
//        },
//        {
//            "code": "twj",
//                "name": "Body Thermometer"
//        },
//        {
//            "code": "pir",
//                "name": "Motion Detector"
//        },
//        {
//            "code": "hps",
//                "name": "Human Presence Sensor"
//        },
//        {
//            "code": "mcs",
//                "name": "Contact Sensor"
//        },
//        {
//            "code": "sos",
//                "name": "Emergency Button"
//        },
//        {
//            "code": "mhq",
//                "name": "Smart Fire Extinguisher"
//        },
//        {
//            "code": "sj",
//                "name": "Flooding Detector"
//        },
//        {
//            "code": "hzdcgq",
//                "name": "Constant Illumination Sensor"
//        },
//        {
//            "code": "zd",
//                "name": "Vibration Sensor"
//        },
//        {
//            "code": "gas_controller",
//                "name": "Gas Controller"
//        },
//        {
//            "code": "ylcg",
//                "name": "Pressure Sensor"
//        },
//        {
//            "code": "sgbj",
//                "name": "Siren"
//        },
//        {
//            "code": "wsdcg",
//                "name": "Temperature and Humidity Sensor"
//        },
//        {
//            "code": "llcgq",
//                "name": "Flow Sensor"
//        },
//        {
//            "code": "ldcg",
//                "name": "Luminance Sensor"
//        },
//        {
//            "code": "qxj",
//                "name": "Clock Weather station"
//        },
//        {
//            "code": "jqbj",
//                "name": "Formaldehyde Detector"
//        },
//        {
//            "code": "pm2.5",
//                "name": "PM2.5 Sensor"
//        },
//        {
//            "code": "co2bj",
//                "name": "CO2 Detector"
//        },
//        {
//            "code": "voc",
//                "name": "VOC Sensor"
//        },
//        {
//            "code": "zwjcy",
//                "name": "Plant monitor"
//        },
//        {
//            "code": "ywbj",
//                "name": "Smoke Detector"
//        },
//        {
//            "code": "rqbj",
//                "name": "Gas Detector"
//        },
//        {
//            "code": "jwbj",
//                "name": "Methane Detector"
//        },
//        {
//            "code": "ywcgq",
//                "name": "Liquid level Sensor"
//        },
//        {
//            "code": "cobj",
//                "name": "CO Detector"
//        },
//        {
//            "code": "rg",
//                "name": "Heat Detector"
//        },
//        {
//            "code": "dgnbj",
//                "name": "Multi-function Sensor"
//        },
//        {
//            "code": "mal",
//                "name": "Multifunctional Alarm"
//        },
//        {
//            "code": "znzy",
//                "name": "Smart Seat"
//        },
//        {
//            "code": "slm",
//                "name": "Sound level meter"
//        },
//        {
//            "code": "tzc1",
//                "name": "Body Fat Scale"
//        },
//        {
//            "code": "yyc",
//                "name": "Nutrition scale"
//        },
//        {
//            "code": "jbamy",
//                "name": "Neck massage"
//        },
//        {
//            "code": "jmq",
//                "name": "Fascial Gun"
//        },
//        {
//            "code": "mry",
//                "name": "Beauty Instrument"
//        },
//        {
//            "code": "ts",
//                "name": "Smart Rope Skipping"
//        },
//        {
//            "code": "kswes",
//                "name": "Ear Wax Removal Otoscope"
//        },
//        {
//            "code": "pltwj",
//                "name": "Ovulation Thermometer"
//        },
//        {
//            "code": "xyy",
//                "name": "Oximeter"
//        },
//        {
//            "code": "ksqhty",
//                "name": "Visual Blackhead Remover"
//        },
//        {
//            "code": "tmy",
//                "name": "hair removal instrument"
//        },
//        {
//            "code": "BDJTZC",
//                "name": "eight electrodes body fat scale"
//        },
//        {
//            "code": "tew_1",
//                "name": "Temperature Stick"
//        },
//        {
//            "code": "jsj",
//                "name": "Fitness mirror"
//        },
//        {
//            "code": "ewq",
//                "name": "Forehead thermometer"
//        },
//        {
//            "code": "gbtwj",
//                "name": "Wall-mounted thermometer"
//        },
//        {
//            "code": "dztwj",
//                "name": "electronic clinical thermometer"
//        },
//        {
//            "code": "sb",
//                "name": "Watch/Bracelet"
//        },
//        {
//            "code": "cpj",
//                "name": "Skin Scurbber"
//        },
//        {
//            "code": "xyj",
//                "name": "Blood Pressure Monitor"
//        },
//        {
//            "code": "tzc10",
//                "name": "Body weight scale"
//        },
//        {
//            "code": "bsy",
//                "name": "Moisturizing machine"
//        },
//        {
//            "code": "znhsb",
//                "name": "Smart glass"
//        },
//        {
//            "code": "amy",
//                "name": "Massage Chair"
//        },
//        {
//            "code": "zpbj",
//                "name": "Treadmill"
//        },
//        {
//            "code": "smd",
//                "name": "Sleep Band"
//        },
//        {
//            "code": "smnk",
//                "name": "Sleep Dot"
//        },
//        {
//            "code": "znc",
//                "name": "Smart Bed"
//        },
//        {
//            "code": "znzt",
//                "name": "Smart pillow"
//        },
//        {
//            "code": "bzyd",
//                "name": "White Noise Light"
//        },
//        {
//            "code": "liliao",
//                "name": "Physiotherapy Products"
//        },
//        {
//            "code": "zyp",
//                "name": "Foot Tub"
//        },
//        {
//            "code": "ys",
//                "name": "Toothbrush"
//        },
//        {
//            "code": "znyh",
//                "name": "Smart Pill Box"
//        },
//        {
//            "code": "jmy",
//                "name": "Smart cleanser"
//        },
//        {
//            "code": "txj",
//                "name": "Shaving machine"
//        },
//        {
//            "code": "znqp",
//                "name": "Smart Badminton Racket"
//        },
//        {
//            "code": "push_up_bars",
//                "name": "Push Up Bars"
//        },
//        {
//            "code": "vibration_plate",
//                "name": "Vibration Plate"
//        },
//        {
//            "code": "znwqp",
//                "name": "Smart Tennis Racket"
//        },
//        {
//            "code": "znyl",
//                "name": "Smart Dumbbells"
//        },
//        {
//            "code": "znwlq",
//                "name": "Smart Wrist Ball"
//        },
//        {
//            "code": "dgdc",
//                "name": "Indoor Cycling Bike"
//        },
//        {
//            "code": "znhlq",
//                "name": "Hula Hoop"
//        },
//        {
//            "code": "znjfl",
//                "name": "Abdominal Wheel"
//        },
//        {
//            "code": "znllq",
//                "name": "Resistance Band"
//        },
//        {
//            "code": "hcj",
//                "name": "Rowing Mchine"
//        },
//        {
//            "code": "zncyq",
//                "name": "Smart Oral Irrigator"
//        },
//        {
//            "code": "xlt",
//                "name": "Heart Rate Sticker"
//        },
//        {
//            "code": "yoga_mat",
//                "name": "Yoga Mat"
//        },
//        {
//            "code": "znmzj",
//                "name": "Beauty Mirror"
//        },
//        {
//            "code": "zfs",
//                "name": "Straight Hair Comb"
//        },
//        {
//            "code": "sp",
//                "name": "Smart Camera"
//        },
//        {
//            "code": "doorbell",
//                "name": "Smart Doorbell"
//        },
//        {
//            "code": "jz",
//                "name": "Base Station"
//        },
//        {
//            "code": "camera",
//                "name": "Smart Camera"
//        },
//        {
//            "code": "fkj",
//                "name": "Visitor machine"
//        },
//        {
//            "code": "videolock",
//                "name": "videolock"
//        },
//        {
//            "code": "kshgh",
//                "name": "Visual personal care"
//        },
//        {
//            "code": "smsxj",
//                "name": "Binocular Camera"
//        },
//        {
//            "code": "jtmspro",
//                "name": "Residential Lock PRO"
//        },
//        {
//            "code": "photolock",
//                "name": "real-time video lock"
//        },
//        {
//            "code": "ms",
//                "name": "Residential Lock"
//        },
//        {
//            "code": "gyms",
//                "name": "Business Lock"
//        },
//        {
//            "code": "hotelms",
//                "name": "Hotel Lock"
//        },
//        {
//            "code": "bxx",
//                "name": "safe"
//        },
//        {
//            "code": "znmj",
//                "name": "Access Control"
//        },
//        {
//            "code": "xcjly",
//                "name": "Car Recorder"
//        },
//        {
//            "code": "nvr",
//                "name": "NVR"
//        },
//        {
//            "code": "ms_category",
//                "name": "DoorLock Accessories"
//        },
//        {
//            "code": "mk",
//                "name": "Gating Lock"
//        },
//        {
//            "code": "small_lock",
//                "name": "Small Lock"
//        },
//        {
//            "code": "jtmsbh",
//                "name": "Smart Lock（Stand by）"
//        },
//        {
//            "code": "aihz",
//                "name": "AI Box"
//        },
//        {
//            "code": "dghsxj",
//                "name": "LE Camera"
//        },
//        {
//            "code": "wgsxj",
//                "name": "Gateway Camera"
//        },
//        {
//            "code": "smartms_pro",
//                "name": "SmartLock Pro"
//        },
//        {
//            "code": "jisentest",
//                "name": "jisentest"
//        },
//        {
//            "code": "jzq",
//                "name": "Fitting"
//        },
//        {
//            "code": "lyqwg",
//                "name": "Router"
//        },
//        {
//            "code": "bywg",
//                "name": "IoT Edge Gateway"
//        },
//        {
//            "code": "zigbee",
//                "name": "Gateway"
//        },
//        {
//            "code": "wg2",
//                "name": "Gateway"
//        },
//        {
//            "code": "dgnzk",
//                "name": "Tuya Multi-function Controller"
//        },
//        {
//            "code": "videohub",
//                "name": "videohub"
//        },
//        {
//            "code": "xnwg",
//                "name": "Virtual Gateway"
//        },
//        {
//            "code": "alexa_yyxxj",
//                "name": "diffuser with alexa built-in"
//        },
//        {
//            "code": "zxgjcp",
//                "name": "Xiao Zhi Ai Product"
//        },
//        {
//            "code": "alexa_built_in",
//                "name": "alexa_built_in"
//        },
//        {
//            "code": "zhzkp",
//                "name": "Smart Panel"
//        },
//        {
//            "code": "alexa_yywg",
//                "name": "Gateway with Alexa"
//        },
//        {
//            "code": "ldjcq",
//                "name": "Leakage Detector"
//        },
//        {
//            "code": "ups",
//                "name": "UPS"
//        },
//        {
//            "code": "MPPT",
//                "name": "MPPT controller"
//        },
//        {
//            "code": "LCRV",
//                "name": "Control Valve"
//        },
//        {
//            "code": "zcnbq",
//                "name": "String InverterInfo"
//        },
//        {
//            "code": "wxnbq",
//                "name": "Micro InverterInfo"
//        },
//        {
//            "code": "cjq",
//                "name": "data concentrator"
//        },
//        {
//            "code": "byq",
//                "name": "Transformer"
//        },
//        {
//            "code": "qccdz",
//                "name": "EV charger"
//        },
//        {
//            "code": "ddzxccdz",
//                "name": "Moped Charging Station"
//        },
//        {
//            "code": "bxsdy",
//                "name": "Portable power station"
//        },
//        {
//            "code": "zndb",
//                "name": "Smart Electric Meter"
//        },
//        {
//            "code": "znsb",
//                "name": "Smart Water Meter"
//        },
//        {
//            "code": "dlq",
//                "name": "Switch Module"
//        },
//        {
//            "code": "znqb",
//                "name": "Smart gas meter"
//        },
//        {
//            "code": "znnbq",
//                "name": "smart inverter"
//        },
//        {
//            "code": "fsjl",
//                "name": "Anemometer"
//        },
//        {
//            "code": "bpq",
//                "name": "Frequency converter"
//        },
//        {
//            "code": "cnsb",
//                "name": "Photovoltaic energy storage"
//        },
//        {
//            "code": "znjdq",
//                "name": "Relay"
//        },
//        {
//            "code": "slj",
//                "name": "Water Flowmeter"
//        },
//        {
//            "code": "gywg",
//                "name": "Industrial gateway"
//        },
//        {
//            "code": "BTU",
//                "name": "DTU"
//        },
//        {
//            "code": "ggq",
//                "name": "Irrigators"
//        },
//        {
//            "code": "sfkzq",
//                "name": "Water valve controller"
//        },
//        {
//            "code": "yxzdcgq",
//                "name": "Vibration Sensor"
//        },
//        {
//            "code": "dlcgq",
//                "name": "Current Sensor"
//        },
//        {
//            "code": "wsdcgq",
//                "name": "T&H Sensor"
//        },
//        {
//            "code": "cocgq",
//                "name": "Co Sensor"
//        },
//        {
//            "code": "co2cgq",
//                "name": "Co2 Sensor"
//        },
//        {
//            "code": "pm25cgq",
//                "name": "Pm25 Sensor"
//        },
//        {
//            "code": "ptlkzq",
//                "name": "PTL Controller"
//        },
//        {
//            "code": "ptl",
//                "name": "Pick To Light"
//        },
//        {
//            "code": "gyxhd",
//                "name": "Industrial Signal Lamp"
//        },
//        {
//            "code": "anzsd",
//                "name": "Button Indicator"
//        },
//        {
//            "code": "ysjsb",
//                "name": "Printer Device"
//        },
//        {
//            "code": "xgjcsb",
//                "name": "Solder paste testing equipment"
//        },
//        {
//            "code": "tpjsb",
//                "name": "Mounter Device"
//        },
//        {
//            "code": "hlhsb",
//                "name": "Welding Device"
//        },
//        {
//            "code": "lwcsy",
//                "name": "Furnace temperature tester"
//        },
//        {
//            "code": "aoisb",
//                "name": "AOI Device"
//        },
//        {
//            "code": "cnc",
//                "name": "CNC"
//        },
//        {
//            "code": "zsj",
//                "name": "Injection Machine"
//        },
//        {
//            "code": "ddc",
//                "name": "DDC"
//        },
//        {
//            "code": "dmq",
//                "name": "code reader"
//        },
//        {
//            "code": "znlj",
//                "name": "Intelligent material rack"
//        },
//        {
//            "code": "AGV",
//                "name": "Automated Guided Vehicle"
//        },
//        {
//            "code": "yqcgq",
//                "name": "Oxygen Sensor"
//        },
//        {
//            "code": "ylcgq",
//                "name": "Pressure Sensor"
//        },
//        {
//            "code": "iomk",
//                "name": "IO Module"
//        },
//        {
//            "code": "gylsj",
//                "name": "Cooling Water Machine"
//        },
//        {
//            "code": "pfj",
//                "name": "Fertilizer Machine"
//        },
//        {
//            "code": "yxywcgq",
//                "name": "Liquid Level Sensor"
//        },
//        {
//            "code": "phcgq",
//                "name": "Ph Sensor"
//        },
//        {
//            "code": "eccgq",
//                "name": "Ec Sensor"
//        },
//        {
//            "code": "admk",
//                "name": "AD Module"
//        },
//        {
//            "code": "ktykq",
//                "name": "Air Conditioning Controller"
//        },
//        {
//            "code": "plc",
//                "name": "plc"
//        },
//        {
//            "code": "ds",
//                "name": "TV"
//        },
//        {
//            "code": "mj",
//                "name": "Smart Mirror"
//        },
//        {
//            "code": "bjyy",
//                "name": "Background music"
//        },
//        {
//            "code": "tyy",
//                "name": "Projector"
//        },
//        {
//            "code": "ej",
//                "name": "Headphone"
//        },
//        {
//            "code": "dzrl",
//                "name": "E-calendar"
//        },
//        {
//            "code": "dzbp",
//                "name": "Signage"
//        },
//        {
//            "code": "jp",
//                "name": "Keyboard"
//        },
//        {
//            "code": "yj",
//                "name": "Glasses"
//        },
//        {
//            "code": "mkf",
//                "name": "Microphone"
//        },
//        {
//            "code": "zkp",
//                "name": "Smart Console Center"
//        },
//        {
//            "code": "wpyyzk",
//                "name": "Voice panel"
//        },
//        {
//            "code": "zwyyzs",
//                "name": "Chinese AI voice products"
//        },
//        {
//            "code": "3ddyj",
//                "name": "3D Printer"
//        },
//        {
//            "code": "loudspeaker_box",
//                "name": "Speaker"
//        },
//        {
//            "code": "etqcc",
//                "name": "Children Ride_on Car"
//        },
//        {
//            "code": "bxdyj",
//                "name": "Portable Printer"
//        },
//        {
//            "code": "twdyj",
//                "name": "Digital printer"
//        },
//        {
//            "code": "wjxc",
//                "name": "toy car"
//        },
//        {
//            "code": "yxk",
//                "name": "Cloud Frame"
//        },
//        {
//            "code": "cryp",
//                "name": "Vibrator Egg"
//        },
//        {
//            "code": "jdh",
//                "name": "set-top box"
//        },
//        {
//            "code": "ddzxc",
//                "name": "Electric Bicycle"
//        },
//        {
//            "code": "cqb",
//                "name": "Gonfiatore"
//        },
//        {
//            "code": "ddly",
//                "name": "Electric wheelchair"
//        },
//        {
//            "code": "phc",
//                "name": "Hoverboards"
//        },
//        {
//            "code": "tk",
//                "name": "Helmet"
//        },
//        {
//            "code": "bbyx",
//                "name": "Receive payment speaker"
//        },
//        {
//            "code": "sswifi",
//                "name": "Portable  Wi-Fi"
//        },
//        {
//            "code": "zfhz",
//                "name": "Payment box"
//        },
//        {
//            "code": "BatteryPack",
//                "name": "E-bike battery pack"
//        },
//        {
//            "code": "shg",
//                "name": "Vending cabinet"
//        },
//        {
//            "code": "ddzxcdc",
//                "name": "Electric Bike Battery"
//        },
//        {
//            "code": "hwcxcp",
//                "name": "Outdoor Product"
//        },
//        {
//            "code": "fusion_positioning",
//                "name": "Indoor and outdoor fusion locator"
//        },
//        {
//            "code": "qdxc",
//                "name": "ATV"
//        },
//        {
//            "code": "Others",
//                "name": "Others"
//        },
//        {
//            "code": "hbc",
//                "name": "Scooter"
//        },
//        {
//            "code": "tracker",
//                "name": "Tracker"
//        },
//        {
//            "code": "znddc",
//                "name": "Intelligent electric vehicle（BLE）"
//        },
//        {
//            "code": "fdq",
//                "name": "Burglar Alarm"
//        },
//        {
//            "code": "znyg",
//                "name": "Smoke Sensor"
//        },
//        {
//            "code": "znwg",
//                "name": "Smart temperature"
//        },
//        {
//            "code": "znylbsq",
//                "name": "Pressure Transmitters"
//        },
//        {
//            "code": "wyzxj",
//                "name": "Property Center Machine"
//        },
//        {
//            "code": "hjjc",
//                "name": "Community environmental testing"
//        },
//        {
//            "code": "xjq",
//                "name": "Inspection device"
//        },
//        {
//            "code": "rba",
//                "name": "Building Automation"
//        },
//        {
//            "code": "dzy",
//                "name": "E-cigarette"
//        },
//        {
//            "code": "dayu",
//                "name": "dayu"
//        },
//        {
//            "code": "zhxf",
//                "name": "Intelligent fire fighting"
//        },
//        {
//            "code": "gpsxt",
//                "name": "Water supply and drainage system"
//        },
//        {
//            "code": "lryxt",
//                "name": "Cold and heat source system"
//        },
//        {
//            "code": "spfxt",
//                "name": "BA ventilation system"
//        },
//        {
//            "code": "ktxt",
//                "name": "Combined air-conditioning wind cabinet"
//        },
//        {
//            "code": "dtxt",
//                "name": "BA Elevator system"
//        },
//        {
//            "code": "baznsb",
//                "name": "BA Intelligent water meter"
//        },
//        {
//            "code": "byqnhxt",
//                "name": "BA Energy consumption system"
//        },
//        {
//            "code": "xfxt",
//                "name": "BA Fire fighting system"
//        },
//        {
//            "code": "bazmxt",
//                "name": "BA Lighting System"
//        },
//        {
//            "code": "hjjkxt",
//                "name": "BA Environmental Monitoring System"
//        },
//        {
//            "code": "gsxt",
//                "name": "BA Water supply system"
//        },
//        {
//            "code": "zyktxt",
//                "name": "BA Air conditioning system"
//        },
//        {
//            "code": "sssbxt",
//                "name": "BA Facilities and equipment system"
//        },
//        {
//            "code": "balryxt",
//                "name": "BA Cold and heat source system"
//        },
//        {
//            "code": "access_point",
//                "name": "Access Point"
//        },
//        {
//            "code": "rfid_reader",
//                "name": "RFID Reader"
//        },
//        {
//            "code": "dmjkzq",
//                "name": "Multi Door Game"
//        },
//        {
//            "code": "hymp",
//                "name": "Conference Reservation Tablet"
//        },
//        {
//            "code": "qtsdk",
//                "name": "Others (SDK)"
//        },
//        {
//            "code": "wnykq",
//                "name": "Universal Remote Control"
//        },
//        {
//            "code": "zntc",
//                "name": "Smart Parking"
//        },
//        {
//            "code": "zntk",
//                "name": "Smart Elevator"
//        },
//        {
//            "code": "wfcon",
//                "name": "Connector"
//        },
//        {
//            "code": "znyxss",
//                "name": "Smart Speaker"
//        },
//        {
//            "code": "qt",
//                "name": "Others"
//        },
//        {
//            "code": "ss_sl",
//                "name": "Business license & buildings"
//        },
//        {
//            "code": "vocie_platform_only",
//                "name": "Voice Platform"
//        }
//  ],
//        "success": true,
//            "t": 1682337997921,
//            "tid": "76cfa1b2e29811ed847f92e270c72120"
//    }
//}
