package com.psd.entities;

import java.math.BigInteger;

public class Share {
    private final BigInteger shareholder;
    private final BigInteger share;

    public Share(BigInteger shareholder, BigInteger share) {
        this.shareholder = shareholder;
        this.share = share;
    }

    public BigInteger getShare() {
        return share;
    }

    public BigInteger getShareholder() {
        return shareholder;
    }
}
