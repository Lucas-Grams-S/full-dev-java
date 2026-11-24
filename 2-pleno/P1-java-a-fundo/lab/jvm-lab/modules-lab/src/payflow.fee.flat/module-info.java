module payflow.fee.flat {
    requires payflow.fee.spi;
    provides com.payflow.fee.FeePolicy with com.payflow.fee.flat.FlatFee;
}
