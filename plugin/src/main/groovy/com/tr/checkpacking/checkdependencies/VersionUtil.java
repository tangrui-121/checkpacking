package com.tr.checkpacking.checkdependencies;

public class VersionUtil {

    /**
     * 版本号比较 降级了返回true
     * * update的才会走到这里
     * 1.0.5-20210727.075435-11
     * 先比对大版本号，降级了直接返回
     * 大版本号一样的话说明是至少一方是快照
     * 如果都是快照比对最后一位
     * 如果一个快照一个正式，这时确定不了是否是降级，直接返回0
     * <p>
     * 1 - 降级
     * 0 - 大版本号一致，一个正式一个快照
     * -1 - 升级
     */
    public static int isVersionDown(String version1, String version2) {
        String _version1 = version1;
        if (version1.contains("-")) {
            _version1 = version1.substring(0, version1.indexOf("-"));
        }
        String _version2 = version2;
        if (version2.contains("-")) {
            _version2 = version2.substring(0, version2.indexOf("-"));
        }

        String[] v1 = _version1.split("\\.");
        String[] v2 = _version2.split("\\.");
        int len1 = v1.length;
        int len2 = v2.length;
        int lim = Math.min(len1, len2);
        int k = 0;
        while (k < lim) {
            String c1 = v1[k];
            String c2 = v2[k];
            if (Integer.parseInt(c1) != Integer.parseInt(c2)) {
                if (Integer.parseInt(c1) - Integer.parseInt(c2) > 0) {
                    return 1;
                } else {
                    return -1;
                }
            }
            k++;
        }
        // 降级了
        if (len1 - len2 > 0) {
            return 1;
        }
        // 升级了
        if (len1 - len2 < 0) {
            return -1;
        }
        // 大版本号一致，比较快照
        if (version1.contains("-") != version2.contains("-")) {
            return 0;
        }
        int lastVersion1 = Integer.parseInt(version1.substring(version1.lastIndexOf("-") + 1));
        int lastVersion2 = Integer.parseInt(version2.substring(version2.lastIndexOf("-") + 1));
        if (lastVersion1 > lastVersion2) {
            return 1;
        } else {
            return -1;
        }
    }
}

