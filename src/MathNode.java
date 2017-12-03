import java.math.BigDecimal;

public class MathNode {
    private BigDecimal low;
    private BigDecimal high;

    public MathNode(BigDecimal low, BigDecimal high) {
        this.low = low;
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getHigh() {
        return high;
    }


}
