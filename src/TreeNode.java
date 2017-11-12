import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TreeNode implements Comparable<TreeNode>{
    public static Integer count = 0;

    private TreeNode left;
    private TreeNode right;
    private String code;
    private List<Map.Entry<String, Double>> values;
    private Double p;

    public TreeNode() {
    }

    public TreeNode(TreeNode left, TreeNode right, String code, List<Map.Entry<String, Double>> values) {
        this.left = left;
        this.right = right;
        this.code = code;
        this.values = values;
    }

    public TreeNode(Map.Entry<String, Double> entry) {
        this.values = Collections.singletonList(entry);
        this.p = entry.getValue();
    }

    public Double getP() {
        return p;
    }

    public void setP(Double p) {
        this.p = p;
    }

    public TreeNode getLeft() {
        return left;
    }

    public void setLeft(TreeNode left) {
        this.left = left;
    }

    public TreeNode getRight() {
        return right;
    }

    public void setRight(TreeNode right) {
        this.right = right;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Map.Entry<String, Double>> getValues() {
        return values;
    }

    public void setValues(List<Map.Entry<String, Double>> values) {
        this.values = values;
    }

    public boolean isLeaf() {
        return (left == null && right == null);
    }

    @Override
    public int compareTo(TreeNode o) {
        return p.compareTo(o.p);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return values.get(0).getKey().replace("\n", "\\n").replace("\r", "\\r") + ": " + values.get(0).getValue();
        }
        return p.toString();
    }

    public TreeNode merge(TreeNode treeNode) {
        TreeNode parent = new TreeNode();

        this.code = "0";
        parent.left = this;

        treeNode.code = "1";
        parent.right = treeNode;

        parent.values = new ArrayList<>();
        parent.values.addAll(this.values);
        parent.values.addAll(parent.right.values);

        parent.setP(this.p + treeNode.p);

        return parent;
    }
}
