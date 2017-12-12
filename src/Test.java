import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class Test {
    public static void main(String[] args) {
        double[] a = new double[]{0.774,0.204,0.0214,0.001,0.00003,0.0000003};
//        double[] a = new double[]{0.00076,0.002,0.024,0.14,0.39,0.44};
        double ent = 0D;
        for (int i = 0; i < a.length; i++) {
            double p = a[i];
            double subEnt = p * (Math.log(p) / Math.log(2D));
            ent += subEnt;
        }

        System.out.println(-ent);

        double p = 0D;
        for (int i = 2; i < a.length; i++) {
            p += a[i];
        }

        System.out.println(p);
        double i = -(Math.log(p) / Math.log(2D));
        System.out.println(i);
    }
}
