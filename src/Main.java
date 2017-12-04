import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {
    private static long lng;
    private static String code = "";
    private static String left = "";
    private static Boolean prevR = false;
    private static int txt = 0;
    private static final Integer[] textLength = {0}; //кол-во символов в тексте
    private static Integer neededTextLenght;


    public static void main(String[] args) throws IOException {
        neededTextLenght = 1000;
        System.out.println("Требуемое кол-во символов: " + neededTextLenght);

        SortedMap<String, Double> chars = new TreeMap<>(); // мап для обычной энтропии
        SortedMap<String, Integer> charsMap = new TreeMap<>(); // мап для обычной энтропии
        SortedMap<String, Double> dMap = new TreeMap<>(); // для условной энтропии с учетом 1-го пред. символа
        SortedMap<String, Double> dMapUsl = new TreeMap<>();
        SortedMap<String, Double> trMap = new TreeMap<>(); // для условной энтропии с учетом 2-ух пред. символов

        try (FileReader reader = new FileReader("in.txt")) {
            int c;
            String prev = null; // хранит пред. символ для dMap
            String prev2 = ""; // хранит 2 пред символа для trMap
            while ((c = reader.read()) != -1  && (neededTextLenght == null || textLength[0] < neededTextLenght)) {
                String textChar = String.valueOf((char) c);

                chars.putIfAbsent(textChar, 0D); // если в мапе нет значения для такого ключа, кладем туда 0
                chars.put(textChar, chars.get(textChar) + 1); // в итоге получим кол-во вхождений каждого символа

                charsMap.putIfAbsent(textChar, 0);
                charsMap.put(textChar, charsMap.get(textChar) + 1); // в итоге получим кол-во вхождений каждого символа


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

        dMap.forEach((key, value) -> dMap.put(key, value / (textLength[0] - 1)));// ключ - аб, получаем P(ба) = N(aб) / кол-во парных сочетаний

        dMap.forEach((key, value) -> { // на вход приходит строка аб допустим
            Double pbla = value / chars.get(key.substring(0, 1)); // chars содержит вероятности каждого символа (P(б|а) = P(ба) делится на P(a))
            dMapUsl.put(key, pbla);
            uslEntropy[0] += value * (Math.log(pbla) / Math.log(2D));
        });

        System.out.println("Условная энтропия с учетом одного пред. символа: " + -uslEntropy[0]);


        final Double[] entropy2 = {0D};
        trMap.forEach((key, value) -> {
            Double pvba = value / (textLength[0] - 2); //P(вба) = N(абв) / кол-во тройных сочетаний
            Double pvlba = pvba / dMap.get(key.substring(0, 2)); // P(в|ба) = P(вба) / P(ба)
            entropy2[0] += pvba * (Math.log(pvlba) / Math.log(2D));
        });

        System.out.println("Условная энтропия с учетом двух пред. символов: " + -entropy2[0]);

//        System.out.println("Кодирование Шеннон-Фано учет вероятности отдельных букв");
//        codeDecodeShenonFano(chars, 1,"ShenonFano_1");
//        System.out.println("Кодирование Шеннон-Фано учет условной вероятности букв");
//        codeDecodeShenonFano(dMap, 2,  "ShenonFano_2" );
//
//        System.out.println("Кодирование Хаффман учет вероятности отдельных букв");
//        codeDecodeHaffman(chars, 1, "Haffman_1");
//        System.out.println("Кодирование Хаффман учет условной вероятности букв");
//        codeDecodeHaffman(dMap, 2, "Haffman_2");

        arithmeticEncoding(charsMap);
//        adaptiveEncoding(chars);
    }

    public static void adaptiveEncoding(SortedMap<String, Double> chars) {
        ExecutorService threadPool = Executors.newFixedThreadPool(8);
        SortedMap<String, Double> charsCopy = new TreeMap<>(chars);
        SortedMap<String, Double> countMap = new TreeMap<>(chars);
        SortedMap<String, MathNode> mMap = new TreeMap<>();

        final Integer[] charCount = {chars.size()};
        charsCopy.forEach((key, value) -> {
            charsCopy.put(key, 1D / charCount[0]);
            countMap.put(key, 1D);
        });

        BigDecimal oldLow = new BigDecimal(0);
        BigDecimal oldHigh = new BigDecimal(1);

        StringBuilder res = new StringBuilder();

        try (FileReader reader = new FileReader("in.txt")) {
            int c;
            int count = 0;
            while ((c = reader.read()) != -1 && count < textLength[0]) {
                final BigDecimal[] prev = {new BigDecimal(0)};
                charsCopy.forEach((key, value) -> {
                    BigDecimal high = prev[0].add(new BigDecimal(value)).setScale(16, BigDecimal.ROUND_HALF_UP);
                    mMap.put(key, new MathNode(prev[0], high));
                    prev[0] = high;
                });

                String textChar = String.valueOf((char) c);

                count++;

                BigDecimal dif = oldHigh.subtract(oldLow);

                BigDecimal newLow = getLow(oldLow, oldHigh, mMap, textChar);
                BigDecimal newHigh = getHigh(oldLow, oldHigh, mMap, textChar);

                String similar = getEqualsPart(newLow, newHigh);
                if (StringUtils.isNotBlank(similar)) {
                    res.append(similar);

                    BigDecimal toMult = new BigDecimal(pow(10, similar.length()));
                    BigDecimal toMinus = new BigDecimal(similar);
                    newLow = newLow.multiply(toMult).subtract(toMinus).setScale(16, BigDecimal.ROUND_HALF_UP);
                    newHigh = newHigh.multiply(toMult).subtract(toMinus).setScale(16, BigDecimal.ROUND_HALF_UP);
                }

                oldLow = newLow;
                oldHigh = newHigh;

                charCount[0]++;
                countMap.put(textChar, countMap.get(textChar) + 1);

                charsCopy.forEach((key, value) -> {
                    charsCopy.put(key, countMap.get(key) / charCount[0]);
                });
            }

            File file = new File("adaptive_" + textLength[0] + ".bin");
            res.append(oldLow.toString().substring(2));

            writeResToFile(res.toString(), file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void arithmeticEncoding(SortedMap<String, Integer> chars) {
        SortedMap<String, MathNode> mMap = new TreeMap<>();

        final BigDecimal[] prev = {new BigDecimal(0)};

        chars.forEach((key, value) -> {
            BigDecimal high = prev[0].add(new BigDecimal(value).divide(new BigDecimal(textLength[0]), 16, BigDecimal.ROUND_HALF_UP));
            mMap.put(key, new MathNode(prev[0], high));
            prev[0] = high;
        });
        BigDecimal oldLow = new BigDecimal(0);
        BigDecimal oldHigh = new BigDecimal(1);
        BigDecimal newLow;
        BigDecimal newHigh;

//        StringBuilder res = new StringBuilder();

        try (FileReader reader = new FileReader("in.txt")) {
            int c;
            int count = 0;
            while ((c = reader.read()) != -1 && count < textLength[0]) {
                String textChar = String.valueOf((char) c);
                count++;

                BigDecimal dif = oldHigh.subtract(oldLow);
                newLow = oldLow.add(dif.multiply(mMap.get(textChar).getLow()));
                newHigh = oldLow.add(dif.multiply(mMap.get(textChar).getHigh()));

                oldLow = newLow;
                oldHigh = newHigh;
            }

            String similar = getEqualsPart(oldLow, oldHigh);
            oldLow = oldLow.setScale(similar.length() + 3, BigDecimal.ROUND_HALF_UP);

            System.out.println(similar.length() + 3);
            File file = new File("arithmetic_" + textLength[0] + ".bin");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(oldLow.unscaledValue().toByteArray());
            System.out.println("Арифметическое кодирование (" + textLength[0] + " символов ): " + (double)(file.length() * 8) / textLength[0]);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void writeResToFile(String res, File file, boolean isArithmetic) {
//            try (FileWriter dos = new FileWriter(file)) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            long lng = 0L;
            String resStr = res;
            System.out.println(res.length());
            int lastInd = 0;
            while (StringUtils.isNotBlank(resStr)) {
                boolean catchError = false;

                for (int i = 1; i < resStr.length() + 1; i++) {
                    lastInd = i;
                    try {
                        lng = Long.parseLong(resStr.substring(0, i));
                    } catch (Exception e) {
                        catchError = true;
                        break;
                    }
                }

                dos.writeLong(lng);
                resStr = resStr.substring(lastInd - 1);
                lng = 0;

                if (!catchError) {
                    break;
                }
            }

//                dos.write(resStr);
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Long fileLenBits = file.length() * 8;
        if (isArithmetic) {
            System.out.println("Арифметическое кодирование (" + textLength[0] + " символов). Бит на символ = " + fileLenBits.doubleValue() / textLength[0]);
        } else {
            System.out.println("Адаптивное кодирование (" + textLength[0] + " символов). Бит на символ = " + fileLenBits.doubleValue() / textLength[0]);
        }
    }

    private static int pow(int osn, int len) {
        int res = osn;
        for (int i = 1; i < len; i++) {
            res *= osn;
        }

        return res;
    }

    private static BigDecimal getHigh(BigDecimal oldLow, BigDecimal dif, Map<String, MathNode> map, String s) {
//        BigDecimal dif = oldHigh.subtract(oldLow).setScale(16, BigDecimal.ROUND_HALF_UP);
//        return oldLow.add(dif.multiply(map.get(s).getHigh())).setScale(16, BigDecimal.ROUND_HALF_UP);

        return oldLow.add(dif.multiply(map.get(s).getHigh()));
    }

    private static BigDecimal getLow(BigDecimal oldLow, BigDecimal dif, Map<String, MathNode> map, String s) {
//        BigDecimal dif = oldHigh.subtract(oldLow).setScale(16, BigDecimal.ROUND_HALF_UP);
//        return oldLow.add(dif.multiply(map.get(s).getLow())).setScale(16, BigDecimal.ROUND_HALF_UP);

        return oldLow.add(dif.multiply(map.get(s).getLow()));
    }

    private static String getEqualsPart(BigDecimal bd1, BigDecimal bd2) {
        String bd1Str = bd1.toString().substring(2);
        String bd2Str = bd2.toString().substring(2);

        return StringUtils.getCommonPrefix(new String[]{bd1Str, bd2Str});
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

//        codes.forEach((key, value) -> System.out.println(key.replace("\n", "\\n").replace("\r", "\\r") + ": " + value));

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
        txt = 0;
        try (FileReader reader = new FileReader("in.txt")) {
            File codedFile = new File("coded" + fileName + ".bin");
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(codedFile))) {
                int c;
                String tmpTextChar = "";
                while ((c = reader.read()) != -1 && txt < textLength[0]) {
                    String textChar = tmpTextChar + String.valueOf((char) c);
                    if (textChar.length() < len) {
                        tmpTextChar += textChar;
                        continue;
                    }

                    txt++;

//                    if (txt % 100000 == 0) {
//                        System.out.println(txt);
//                    }

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

//                System.out.println(txt);
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

                System.out.println("Бит на символ = " + bits.doubleValue() / textLength[0]);
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

    static void thMethod () throws IOException {
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> letterMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("in.txt"))));
        char prev = (char) reader.read();
        int c;
        int letterCount = 0;
        StringBuilder sb = new StringBuilder();
        while ((c = reader.read()) != -1) {
            char ch = (char) c;
            sb.append(String.valueOf(ch));
            String s = String.valueOf(prev) + String.valueOf(ch);
            if (map.containsKey(s)) {
                map.put(s, map.get(s) + 1);
            } else {
                map.put(s, 1);
            }
            String stringPrev = String.valueOf(prev);
            if (letterMap.containsKey(stringPrev)) {
                letterMap.put(stringPrev, letterMap.get(stringPrev) + 1);
            } else {
                letterMap.put(stringPrev, 1);
            }

            prev = ch;
            letterCount++;
        }
        Map<String, BigDecimal> letterProbabilityMap = new HashMap<>();
        Set<String> letterKeySet = letterMap.keySet();
        for (String let : letterKeySet) {
            BigDecimal letterProbability = new BigDecimal(letterMap.get(let)).divide(new BigDecimal(letterCount), 32, BigDecimal.ROUND_DOWN);
            letterProbabilityMap.put(let, letterProbability);
        }
        List<String> lettersList = new ArrayList<>();
        lettersList.addAll(letterKeySet);
        String str = sb.toString();
        BigDecimal start = new BigDecimal(0);
        BigDecimal end = new BigDecimal(0);
        BigDecimal range = new BigDecimal(1);
        for (int i = 0; i < 1000; i++) {
            String letter = String.valueOf(str.charAt(i));

            BigDecimal probability = letterProbabilityMap.get(lettersList.get(0));
            BigDecimal theNumber = probability.multiply(range);

            int j = 0;
            if (!lettersList.get(0).equals(letter)) {
                j = 1;
                for (; j < lettersList.size() && !lettersList.get(j).equals(letter); j++) {
                    theNumber = theNumber.add(letterProbabilityMap.get(lettersList.get(j)).multiply(range));
                }
            }
            if (!lettersList.get(0).equals(letter)) {
                start = start.add(theNumber);
            }

            end = start.add(letterProbabilityMap.get(lettersList.get(j)).multiply(range));
            range =  end.subtract(start);
        }
        System.out.println(start);
        System.out.println(end);
        int i = 0;
        for (; range.compareTo(new BigDecimal(1)) < 0; i++) {
            range = range.multiply(new BigDecimal(10));
        }
        BigDecimal result = start.round(new MathContext(i));
        byte[] arr = result.unscaledValue().toByteArray();
//        System.out.println(arr.length);
        File f1 = new File("arifm");
        FileOutputStream fos1 = new FileOutputStream(f1);
        fos1.write(arr);
        fos1.close();
        System.out.println("Арифметическая. Бит/символ: " + ((double)f1.length())/125);

        Map<String, Integer> slogMap = new HashMap<>();

        String prevLetter = String.valueOf(str.charAt(0));
        for (int j = 1; j < str.length(); j++) {
            String letter = String.valueOf(str.charAt(j));
            String combination = prevLetter + letter;
            if (slogMap.containsKey(combination)) {
                Integer num = slogMap.get(combination);
                slogMap.put(combination, num + 1);
            } else {
                slogMap.put(combination, 1);
            }
            prevLetter = letter;
        }

        Map<String, BigDecimal> slogProbabilityMap = new HashMap<>();
        for (String slog : slogMap.keySet()) {
            int count = 0;
            for (String curSlog : slogMap.keySet()) {
                if (curSlog.charAt(0) == slog.charAt(0)) {
                    count += slogMap.get(curSlog);
                }
            }
            slogProbabilityMap.put(slog, new BigDecimal(slogMap.get(slog)).divide(new BigDecimal(count), 32, BigDecimal.ROUND_DOWN));
        }

        lettersList = new ArrayList<>();
        lettersList.addAll(slogMap.keySet());
        Map<String, List<String>> lettersListMap = new HashMap<>();
        for (String s : slogMap.keySet()) {
            if (lettersListMap.containsKey(String.valueOf(s.charAt(0)))) {
                lettersListMap.get(String.valueOf(s.charAt(0))).add(String.valueOf(s.charAt(1)));
            } else {
                List<String> l = new ArrayList<>();
                l.add(String.valueOf(s.charAt(1)));
                lettersListMap.put(String.valueOf(s.charAt(0)), l);
            }
        }

        start = new BigDecimal(0);
        end = new BigDecimal(0);
        range = new BigDecimal(1);

        for (int k = 1; k < 1000; k++) {
            String letter = String.valueOf(str.charAt(k));
            String previousLetter = String.valueOf(str.charAt(k - 1));
            List<String> theList = lettersListMap.get(previousLetter);
            BigDecimal probability = slogProbabilityMap.get(previousLetter + theList.get(0));
            BigDecimal theNumber = probability.multiply(range);

            int j = 0;
            if (!theList.get(0).equals(letter)) {
                j = 1;
                for (; j < lettersListMap.get(previousLetter).size() && !lettersListMap.get(previousLetter).get(j).equals(letter); j++) {
                    theNumber = theNumber.add(slogProbabilityMap.get(previousLetter + lettersListMap.get(previousLetter).get(j)).multiply(range));
                }
            }
            if (!lettersListMap.get(previousLetter).get(0).equals(letter)) {
                start = start.add(theNumber);
            }

            end = start.add(slogProbabilityMap.get(previousLetter + lettersListMap.get(previousLetter).get(j)).multiply(range));
            range =  end.subtract(start);
        }
        System.out.println(start);
        System.out.println(end);
        i = 0;
        for (; range.compareTo(new BigDecimal(1)) < 0; i++) {
            range = range.multiply(new BigDecimal(10));
        }
        result = start.round(new MathContext(i));
        arr = result.unscaledValue().toByteArray();

        File f = new File("arifm2");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(arr);
        fos.close();
        System.out.println("С учетом пред. символа. Бит/символ: " + ((double)f.length())/125);

        letterCount = letterProbabilityMap.keySet().size();
        Map<String, BigDecimal> letterCountMap = new HashMap<>();
        for (String letter : letterProbabilityMap.keySet()) {
            letterCountMap.put(letter, new BigDecimal(1));
        }
        for (String letter : letterProbabilityMap.keySet()) {
            letterProbabilityMap.put(letter, letterCountMap.get(letter).divide(new BigDecimal(letterCount), 32, BigDecimal.ROUND_DOWN));
        }
        start = new BigDecimal(0);
        end = new BigDecimal(0);
        range = new BigDecimal(1);
        lettersList.clear();
        lettersList.addAll(letterCountMap.keySet());
        for (int r = 0; r < 1001; r++) {
            String letter = String.valueOf(str.charAt(r));

            letterCountMap.put(letter, letterCountMap.get(letter).add(new BigDecimal(1)));
            letterCount++;
            for (String letter1 : letterProbabilityMap.keySet()) {
                letterProbabilityMap.put(letter1, letterCountMap.get(letter1).divide(new BigDecimal(letterCount), 32, BigDecimal.ROUND_DOWN));
            }
            BigDecimal probability = letterProbabilityMap.get(lettersList.get(0));
            BigDecimal theNumber = probability.multiply(range);

            int j = 0;
            if (!lettersList.get(0).equals(letter)) {
                j = 1;
                for (; j < lettersList.size() && !lettersList.get(j).equals(letter); j++) {
                    theNumber = theNumber.add(letterProbabilityMap.get(lettersList.get(j)).multiply(range));
                }
            }
            if (!lettersList.get(0).equals(letter)) {
                start = start.add(theNumber);
            }

            end = start.add(letterProbabilityMap.get(lettersList.get(j)).multiply(range));
            range =  end.subtract(start);
        }
        System.out.println(start);
        System.out.println(end);
        int r = 0;
        for (; range.compareTo(new BigDecimal(1)) < 0; r++) {
            range = range.multiply(new BigDecimal(10));
        }
        result = start.round(new MathContext(r));
        arr = result.unscaledValue().toByteArray();
//        System.out.println(arr.length);
        f1 = new File("adaptive");
        fos1 = new FileOutputStream(f1);
        fos1.write(arr);
        fos1.close();
        System.out.println("Адаптивная. Бит/символ: " + ((double)f1.length())/(125));
    }
}
