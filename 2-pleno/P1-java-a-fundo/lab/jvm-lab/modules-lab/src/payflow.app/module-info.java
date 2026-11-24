module payflow.app {
    requires payflow.ledger;
    requires payflow.fee.spi;
    uses com.payflow.fee.FeePolicy;
}
