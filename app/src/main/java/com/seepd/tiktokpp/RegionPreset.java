package com.seepd.tiktokpp;

import java.util.Locale;

enum RegionPreset {
    US("US", "United States", "310260", "T-Mobile", 38.9072, -77.0369),
    CA("CA", "Canada", "302720", "Rogers", 45.4215, -75.6972),
    MX("MX", "Mexico", "334020", "Telcel", 19.4326, -99.1332),
    BR("BR", "Brazil", "72405", "Claro", -15.7975, -47.8919),
    AR("AR", "Argentina", "722310", "Claro", -34.6037, -58.3816),
    CL("CL", "Chile", "73001", "Entel", -33.4489, -70.6693),
    CO("CO", "Colombia", "732101", "Claro", 4.7110, -74.0721),
    PE("PE", "Peru", "71610", "Claro", -12.0464, -77.0428),
    VE("VE", "Venezuela", "73404", "Movistar", 10.4806, -66.9036),
    EC("EC", "Ecuador", "74001", "Claro", -0.1807, -78.4678),
    UY("UY", "Uruguay", "74801", "Antel", -34.9011, -56.1645),
    CR("CR", "Costa Rica", "71203", "Claro", 9.9281, -84.0907),
    PA("PA", "Panama", "71403", "Claro", 8.9824, -79.5199),
    GT("GT", "Guatemala", "70401", "Claro", 14.6349, -90.5069),
    DO("DO", "Dominican Republic", "37002", "Claro", 18.4861, -69.9312),
    GB("GB", "United Kingdom", "23410", "O2", 51.5074, -0.1278),
    IE("IE", "Ireland", "27201", "Vodafone", 53.3498, -6.2603),
    DE("DE", "Germany", "26202", "Vodafone", 52.5200, 13.4050),
    FR("FR", "France", "20801", "Orange", 48.8566, 2.3522),
    IT("IT", "Italy", "22201", "TIM", 41.9028, 12.4964),
    ES("ES", "Spain", "21403", "Vodafone", 40.4168, -3.7038),
    NL("NL", "Netherlands", "20404", "Vodafone", 52.3676, 4.9041),
    BE("BE", "Belgium", "20601", "Proximus", 50.8503, 4.3517),
    AT("AT", "Austria", "23201", "A1", 48.2082, 16.3738),
    CH("CH", "Switzerland", "22801", "Swisscom", 46.9480, 7.4474),
    PL("PL", "Poland", "26001", "Plus", 52.2297, 21.0122),
    SE("SE", "Sweden", "24001", "Telia", 59.3293, 18.0686),
    NO("NO", "Norway", "24201", "Telenor", 59.9139, 10.7522),
    DK("DK", "Denmark", "23801", "TDC", 55.6761, 12.5683),
    FI("FI", "Finland", "24405", "Elisa", 60.1699, 24.9384),
    PT("PT", "Portugal", "26801", "Vodafone", 38.7223, -9.1393),
    CZ("CZ", "Czech Republic", "23001", "T-Mobile", 50.0755, 14.4378),
    SK("SK", "Slovakia", "23102", "Telekom", 48.1486, 17.1077),
    HU("HU", "Hungary", "21630", "Telekom", 47.4979, 19.0402),
    RO("RO", "Romania", "22601", "Vodafone", 44.4268, 26.1025),
    BG("BG", "Bulgaria", "28401", "A1", 42.6977, 23.3219),
    GR("GR", "Greece", "20205", "Vodafone", 37.9838, 23.7275),
    HR("HR", "Croatia", "21901", "Hrvatski Telekom", 45.8150, 15.9819),
    RS("RS", "Serbia", "22003", "A1", 44.7866, 20.4489),
    SI("SI", "Slovenia", "29340", "A1", 46.0569, 14.5058),
    EE("EE", "Estonia", "24801", "Telia", 59.4370, 24.7536),
    LV("LV", "Latvia", "24701", "LMT", 56.9496, 24.1052),
    LT("LT", "Lithuania", "24601", "Telia", 54.6872, 25.2797),
    IS("IS", "Iceland", "27401", "Siminn", 64.1466, -21.9426),
    LU("LU", "Luxembourg", "27001", "POST", 49.6117, 6.1319),
    MT("MT", "Malta", "27801", "Epic", 35.8989, 14.5146),
    CY("CY", "Cyprus", "28001", "Cyta", 35.1856, 33.3823),
    TR("TR", "Turkey", "28601", "Turkcell", 39.9334, 32.8597),
    RU("RU", "Russia", "25001", "MTS", 55.7558, 37.6173),
    UA("UA", "Ukraine", "25501", "Vodafone", 50.4501, 30.5234),
    IL("IL", "Israel", "42501", "Partner", 31.7683, 35.2137),
    AE("AE", "United Arab Emirates", "42402", "Etisalat", 25.2048, 55.2708),
    SA("SA", "Saudi Arabia", "42001", "STC", 24.7136, 46.6753),
    QA("QA", "Qatar", "42701", "Ooredoo", 25.2854, 51.5310),
    KW("KW", "Kuwait", "41902", "Zain", 29.3759, 47.9774),
    BH("BH", "Bahrain", "42601", "Batelco", 26.2285, 50.5860),
    OM("OM", "Oman", "42202", "Omantel", 23.5880, 58.3829),
    JO("JO", "Jordan", "41601", "Zain", 31.9454, 35.9284),
    LB("LB", "Lebanon", "41501", "Alfa", 33.8938, 35.5018),
    IQ("IQ", "Iraq", "41820", "Zain", 33.3152, 44.3661),
    EG("EG", "Egypt", "60201", "Orange", 30.0444, 31.2357),
    ZA("ZA", "South Africa", "65501", "Vodacom", -26.2041, 28.0473),
    MA("MA", "Morocco", "60400", "Orange", 33.9716, -6.8498),
    DZ("DZ", "Algeria", "60301", "Mobilis", 36.7538, 3.0588),
    TN("TN", "Tunisia", "60501", "Orange", 36.8065, 10.1815),
    NG("NG", "Nigeria", "62130", "MTN", 9.0765, 7.3986),
    GH("GH", "Ghana", "62001", "MTN", 5.6037, -0.1870),
    KE("KE", "Kenya", "63902", "Safaricom", -1.2921, 36.8219),
    TZ("TZ", "Tanzania", "64004", "Vodacom", -6.7924, 39.2083),
    UG("UG", "Uganda", "64110", "MTN", 0.3476, 32.5825),
    ET("ET", "Ethiopia", "63601", "Ethio Telecom", 9.0250, 38.7469),
    SN("SN", "Senegal", "60801", "Orange", 14.7167, -17.4677),
    AU("AU", "Australia", "50501", "Telstra", -35.2809, 149.1300),
    NZ("NZ", "New Zealand", "53001", "One NZ", -41.2865, 174.7762),
    FJ("FJ", "Fiji", "54201", "Vodafone", -18.1416, 178.4419),
    IN("IN", "India", "40445", "Airtel", 28.6139, 77.2090),
    PK("PK", "Pakistan", "41001", "Jazz", 33.6844, 73.0479),
    BD("BD", "Bangladesh", "47001", "Grameenphone", 23.8103, 90.4125),
    LK("LK", "Sri Lanka", "41302", "Dialog", 6.9271, 79.8612),
    NP("NP", "Nepal", "42901", "Nepal Telecom", 27.7172, 85.3240),
    JP("JP", "Japan", "44010", "NTT DOCOMO", 35.6762, 139.6503),
    KR("KR", "South Korea", "45005", "SK Telecom", 37.5665, 126.9780),
    CN("CN", "Mainland China", "46000", "China Mobile", 39.9042, 116.4074),
    TW("TW", "Taiwan", "46692", "Chunghwa", 25.0330, 121.5654),
    HK("HK", "Hong Kong", "45400", "HKT", 22.3193, 114.1694),
    SG("SG", "Singapore", "52501", "Singtel", 1.3521, 103.8198),
    MY("MY", "Malaysia", "50212", "Maxis", 3.1390, 101.6869),
    TH("TH", "Thailand", "52001", "AIS", 13.7563, 100.5018),
    PH("PH", "Philippines", "51502", "Globe", 14.5995, 120.9842),
    ID("ID", "Indonesia", "51010", "Telkomsel", -6.2088, 106.8456),
    VN("VN", "Vietnam", "45204", "Viettel", 21.0278, 105.8342),
    KH("KH", "Cambodia", "45601", "Cellcard", 11.5564, 104.9282),
    LA("LA", "Laos", "45701", "Lao Telecom", 17.9757, 102.6331),
    MM("MM", "Myanmar", "41401", "MPT", 19.7633, 96.0785),
    MN("MN", "Mongolia", "42899", "Unitel", 47.8864, 106.9057),
    KZ("KZ", "Kazakhstan", "40101", "Beeline", 51.1694, 71.4491),
    UZ("UZ", "Uzbekistan", "43404", "Beeline", 41.2995, 69.2401),
    GE("GE", "Georgia", "28201", "Silknet", 41.7151, 44.8271),
    AM("AM", "Armenia", "28301", "Team", 40.1792, 44.4991),
    AZ("AZ", "Azerbaijan", "40001", "Azercell", 40.4093, 49.8671);

    final String code;
    final String displayName;
    final String operator;
    final String operatorName;
    final double latitude;
    final double longitude;

    RegionPreset(String code, String displayName, String operator, String operatorName,
                 double latitude, double longitude) {
        this.code = code;
        this.displayName = displayName;
        this.operator = operator;
        this.operatorName = operatorName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    String localizedDisplayName(Locale locale) {
        String localizedName = new Locale("", code).getDisplayCountry(locale);
        return localizedName.isEmpty() ? displayName : localizedName;
    }

    static RegionPreset fromCode(String code) {
        for (RegionPreset preset : values()) {
            if (preset.code.equalsIgnoreCase(code)) {
                return preset;
            }
        }
        return US;
    }
}
