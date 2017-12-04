import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class Test {
    public static void main(String[] args) {
        double a = 0;
        BigDecimal bd = new BigDecimal("1.12345");
        System.out.println(bd.unscaledValue());
    }
}
