import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class Test {
    public static void main(String[] args) {
        Queue<TreeNode> queue = new PriorityQueue<>();
        addToQueue(queue, 0.19109565020170816);
        addToQueue(queue, 0.25892881518239835);
        addToQueue(queue, 0.20854377413725395);
        addToQueue(queue, 0.34143176047863955);
        addToQueue(queue, 0.8);
        addToQueue(queue, 0.4);

        for (TreeNode treeNode: queue) {
            System.out.println(treeNode.getP());
        }
    }

    public static void addToQueue(Queue<TreeNode> queue, Double p) {
        TreeNode treeNode = new TreeNode();
        treeNode.setP(p);
        queue.offer(treeNode);
    }
}
