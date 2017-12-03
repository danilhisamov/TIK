import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class Test {
    public static void main(String[] args) {
        double a = 0;
        BigDecimal bd = new BigDecimal(0);
        for (int i = 0; i < 9; i++) {
            a += 0.1;

            bd = bd.add(new BigDecimal(0.1));
        }
        System.out.println(a);
        System.out.println(bd);
    }
}
