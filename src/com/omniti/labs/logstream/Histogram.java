package com.omniti.labs.logstream;

public class Histogram {
    public static double getLogLinBin (Double val) {
        if ( val == null ) return 0;
        if ( val < 0 ) return -1;
        try {
            double vlog10 = Math.log10(val);
            double vpow10 = Math.pow(10, Math.floor(vlog10));
            double vpow10_min1 = Math.pow(10, Math.floor(vlog10) - 1);
            double fsigdig = Math.floor(val / vpow10);
            double ssigdig = Math.floor((val - (fsigdig * vpow10)) / vpow10_min1);

            if ( fsigdig < 5 && ssigdig >= 5 ) {
                return (fsigdig * vpow10) + (5 * vpow10_min1);
            }

            return fsigdig * vpow10;
        }
        catch (Exception e) {
            return 0;
        }
    }
}
