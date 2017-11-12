import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static double charsCount = 0D;
    private static long lng;
    private static String code = "";
    private static String left = "";
    private static Boolean prevR = false;
    private static int txt = 0;
    private static final Integer[] textLength = {0}; //кол-во символов в тексте


    public static void main(String[] args) throws IOException {
        SortedMap<String, Double> chars = new TreeMap<>(); // мап для обычной энтропии
        SortedMap<String, Double> dMap = new TreeMap<>(); // для условной энтропии с учетом 1-го пред. символа
        SortedMap<String, Double> dMapUsl = new TreeMap<>();
        SortedMap<String, Double> trMap = new TreeMap<>(); // для условной энтропии с учетом 2-ух пред. символов

        try (FileReader reader = new FileReader("in.txt")) {
            int c;
            String prev = null; // хранит пред. символ для dMap
            String prev2 = ""; // хранит 2 пред символа для trMap
            while ((c = reader.read()) != -1) {
                String textChar = String.valueOf((char) c);

                chars.putIfAbsent(textChar, 0D); // если в мапе нет значения для такого ключа, кладем туда 0
                chars.put(textChar, chars.get(textChar) + 1); // в итоге получим кол-во вхождений каждого символа

                if (prev != null) { // таким образом отбрасываем итерацию для первого символа, потому что пред. отсутствует
                    String s = prev + textChar;
                    dMap.putIfAbsent(s, 0D);
                    dMap.put(s, dMap.get(s) + 1);
                }

                if (StringUtils.isNotBlank(prev2) && prev2.length() == 2) { // зайдем только тогда, когда будут записаны 2 пред. символа
                    // т.е отбрасываем 2-ве первые итерации
                    String key = prev2 + textChar;
                    trMap.putIfAbsent(key, 0D);
                    trMap.put(key, trMap.get(key) + 1);
                }

                prev2 = prev2 + textChar;

                if (prev2.length() == 3) { // для первых двух итераций, обрезать строку не надо
                    prev2 = prev2.substring(1);
                }

                prev = textChar;

                textLength[0]++;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        System.out.println("Количество символов в тексте: " + textLength[0] + "\n");
        System.out.println("Количество символов в алфавите: " + chars.size() + "\n");
//        System.out.println("Ансамбль вероятностей в алфавите:");

        chars.forEach((key, value) -> chars.put(key, value / textLength[0]));
//        chars.forEach((key, value) -> System.out.println((key.equals("\n") ? "\\n" : key) + ": " + value));
//        System.out.println(chars.keySet().stream().sorted().collect(Collectors.joining(", ")));
        final Double entropy = -chars.entrySet().stream().map(Map.Entry::getValue).mapToDouble(p -> p * (Math.log(p) / Math.log(2D))).sum();

        System.out.println("Энтропия: " + entropy);

        final Double[] uslEntropy = {0D};

        dMap.forEach((key, value) -> dMap.put(key, value / (textLength[0] - 1D)));// ключ - аб, получаем P(ба) = N(aб) / кол-во парных сочетаний

        dMap.forEach((key, value) -> { // на вход приходит строка аб допустим
            Double pbla = value / chars.get(key.substring(0, 1)); // chars содержит вероятности каждого символа (P(б|а) = P(ба) делится на P(a))
            dMapUsl.put(key, pbla);
            uslEntropy[0] += value * (Math.log(pbla) / Math.log(2D));
        });

        System.out.println("Условная энтропия с учетом одного пред. символа: " + -uslEntropy[0]);


        final Double[] entropy2 = {0D};
        trMap.forEach((key, value) -> {
            Double pvba = value / (textLength[0] - 2D); //P(вба) = N(абв) / кол-во тройных сочетаний
            Double pvlba = pvba / dMap.get(key.substring(0, 2)); // P(в|ба) = P(вба) / P(ба)
            entropy2[0] += pvba * (Math.log(pvlba) / Math.log(2D));
        });

        System.out.println("Условная энтропия с учетом двух пред. символов: " + -entropy2[0]);

        System.out.println("Кодирование Шеннон-Фано учет вероятности отдельных букв");
        codeDecodeShenonFano(chars, 1,"ShenonFano_1");
        System.out.println("Кодирование Шеннон-Фано учет условной вероятности букв");
        codeDecodeShenonFano(dMap, 2,  "ShenonFano_2" );

        System.out.println("Кодирование Хаффман учет вероятности отдельных букв");
        codeDecodeHaffman(chars, 1, "Haffman_1");
        System.out.println("Кодирование Хаффман учет условной вероятности букв");
        codeDecodeHaffman(dMap, 2, "Haffman_2");
    }

    private static void buildLong2() {
        if (code.charAt(0) == '0') {
            lng = 0L;
        } else {
            lng = 1L;
        }
        for (int i = 1; i < code.length(); i++) {
            lng <<= 1;
            if (code.charAt(i) == '1') {
                lng += 1;
            }
        }
    }

    private static void buildTree(TreeNode root, Double p, String s, Map<String, String> map) {
        if (root.getValues().size() > 2) {
            List<Map.Entry<String, Double>> leftValues = new ArrayList<>();
            List<Map.Entry<String, Double>> rightValues = new ArrayList<>();
            Double p1 = 0D;
            for (Map.Entry<String, Double> ent : root.getValues()) {
                if (p1 < p / 2) {
                    leftValues.add(ent);
                    p1 += ent.getValue();
                } else {
                    rightValues.add(ent);
                }
            }

            TreeNode left = new TreeNode();
            TreeNode right = new TreeNode();

            left.setValues(leftValues);
            left.setCode("0");

            right.setValues(rightValues);
            right.setCode("1");

            root.setLeft(left);
            root.setRight(right);

            buildTree(left, p1, s + "0", map);
            buildTree(right, p - p1, s + "1", map);
        } else {
            List<Map.Entry<String, Double>> leftValues;
            List<Map.Entry<String, Double>> rightValues;

            Iterator<Map.Entry<String, Double>> iterator = root.getValues().iterator();

            if (iterator.hasNext()) {
                leftValues = new ArrayList<>();
                Map.Entry<String, Double> val = iterator.next();
                leftValues.add(val);

                TreeNode left = new TreeNode();
                TreeNode.count++;

                left.setValues(leftValues);
                left.setCode(s + "0");

                root.setLeft(left);

                map.put(val.getKey(), s + "0");
            }

            if (iterator.hasNext()) {
                rightValues = new ArrayList<>();
                Map.Entry<String, Double> val = iterator.next();
                rightValues.add(val);

                TreeNode right = new TreeNode();
                TreeNode.count++;

                right.setValues(rightValues);
                right.setCode(s + "1");

                root.setRight(right);

                map.put(val.getKey(), s + "1");
            }
        }
    }

    private static String getCharCode(String c, TreeNode node, String res) {
        if (node.isLeaf() && node.getValues().stream().anyMatch(map -> map.getKey().equals(c))) {
            return res;
        } else {
            if (node.getLeft() != null && node.getLeft().getValues().stream().anyMatch(map -> map.getKey().equals(c))) {
                return getCharCode(c, node.getLeft(), res + node.getLeft().getCode().charAt(node.getLeft().getCode().length() - 1));
            } else if (node.getRight() != null && node.getRight().getValues().stream().anyMatch(map -> map.getKey().equals(c))) {
                return getCharCode(c, node.getRight(), res + node.getRight().getCode().charAt(node.getRight().getCode().length() - 1));
            } else {
                return null;
            }
        }
    }

    private static String getCharByCode(TreeNode node) {
        for (int i = 0; i < code.length(); i++) {
            if (node.isLeaf()) {
                code = code.substring(i);
                return node.getValues().stream().findFirst().map(Map.Entry::getKey).orElse(null);
            } else {
                char c = code.charAt(i);
                Character lastLN = node.getLeft() == null ? null : node.getLeft().getCode().charAt(node.getLeft().getCode().length() - 1); //берем последнюю цифру кода
                Character lastRN = node.getRight() == null ? null : node.getRight().getCode().charAt(node.getRight().getCode().length() - 1);

                if (lastLN != null && lastLN.equals(c)) {
                    node = node.getLeft();
                } else if (lastRN != null && lastRN.equals(c)) {
                    node = node.getRight();
                }
            }
        }

        return null;
    }

    private static void codeDecodeShenonFano(SortedMap<String, Double> chars, int len, String fileName) {
        List<Map.Entry<String, Double>> list = chars.entrySet().stream().sorted((o1, o2) -> -(o1.getValue().compareTo(o2.getValue()))).collect(Collectors.toList());
//        list.forEach(e -> System.out.println(e.getKey().replace("\n", "\\n").replace("\r", "\\r") + ": " + e.getValue()));

        TreeNode root = new TreeNode();
        root.setValues(list);

        SortedMap<String, String> codes = new TreeMap<>();

        buildTree(root, 1D, "", codes);

        codes.forEach((key, value) -> System.out.println(key.replace("\n", "\\n").replace("\r", "\\r") + ": " + value));

        encodeFile(len, fileName, root);
        code = "";
        left = "";
        decodeFile(fileName, root);
    }

    private static void codeDecodeHaffman(SortedMap<String, Double> chars, int len, String fileName) {
        Queue<TreeNode> queue = new PriorityQueue<>();
        chars.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).forEachOrdered(entry -> queue.offer(new TreeNode(entry)));
        while (queue.size() > 1) {
            TreeNode node1 = queue.poll();
            TreeNode node2 = queue.poll();

            queue.add(node1.merge(node2));
        }

        TreeNode root = queue.poll();

        encodeFile(len, fileName, root);
        code = "";
        left = "";
        decodeFile(fileName, root);
    }

    public static void encodeFile(int len, String fileName, TreeNode root) {
        try (FileReader reader = new FileReader("in.txt")) {
            File codedFile = new File("coded" + fileName + ".bin");
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(codedFile))) {
                int c;
                String tmpTextChar = "";
                while ((c = reader.read()) != -1) {
                    String textChar = tmpTextChar + String.valueOf((char) c);
                    if (textChar.length() < len) {
                        tmpTextChar += textChar;
                        continue;
                    }

                    txt++;

                    if (txt % 100000 == 0) {
                        System.out.println(txt);
                    }

                    String gettedCharCode = getCharCode(textChar, root, "");

                    code += gettedCharCode;

                    if (code.length() > 64) {
                        left = code.substring(64);
                        code = code.substring(0, 64);
                        buildLong2();
                        dos.writeLong(lng);
                        code = left;
                    }

                    tmpTextChar = "";
                }

                System.out.println(txt);
                txt = 0;

                code = left;
                if (StringUtils.isNotBlank(code)) {
                    char fc = code.charAt(0);
                    lng = fc == '0' ? 0L : 1L;
                    for (int i = 1; i < code.length(); i++) {
                        lng <<= 1;
                        if (code.charAt(i) == '1') {
                            lng += 1;
                        }
                    }
                    dos.writeLong(lng);
                }

                dos.flush();
                dos.close();

                Long bits = codedFile.length() * 8;

                System.out.println("Бит на символ = " + (bits / textLength[0] + (bits.doubleValue() % textLength[0]) / textLength[0]));
            }
        } catch (IOException ignored) {

        }
    }

    public static void decodeFile(String fileName, TreeNode root) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(new File("coded" + fileName + ".bin")))) {
            try (FileWriter dos = new FileWriter(new File("decoded" + fileName + ".txt"))) {
                long c;
                while ((c = dis.readLong()) != -1L) {
                    code += String.format("%64s", Long.toBinaryString(c)).replace(' ', '0');

                    String s = getCharByCode(root);
                    while (s != null) {
                        dos.write(s);
                        dos.flush();
                        s = getCharByCode(root);
                    }
                }
                dos.close();
            }
        } catch (IOException ignored) {

        }
    }
}
