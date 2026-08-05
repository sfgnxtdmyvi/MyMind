package myMind.common.util;

public class FormatUtil {

    /**
     * 格式化
     */
    public static String format(String string) {
        string = punctuation(string);
        string = space(string);
        string = delete(string);
        return newLine(string);
    }

    /**
     * 以冒号分割字符串
     */
    public static String[] split(String string) {
        String[] splitWithNewLine = string.split("\n");
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();

        for (String s : splitWithNewLine) {
            String[] split = s.split("：");
            before.append(split[0]);
            before.append("\n");
            if (split.length >= 2) {
                after.append(split[1].replaceAll("(.*)。", "$1"));
                after.append("\n");
            }
        }

        if(after.isEmpty()){
            return null;
        }
        return new String[]{before.substring(0, before.length() - 1), after.substring(0, after.length() - 1)};
    }

    /**
     * 英文符号转换成中文符号
     */
    private static String punctuation(String string) {
        string = string.replace("…", "……").
                replaceAll("\"(\\p{IsHan}+)\"", "“$1”");

        char[] charArray = string.toCharArray();

        for (int i = 0; i < charArray.length - 1; i++) {
            char before = charArray[i];
            char after = charArray[i + 1];
            // 中文在前，符号在后
            if (isChinese(before)) {
                switch (after) {
                    case ',' -> charArray[i + 1] = '，';
                    case '.' -> charArray[i + 1] = '。';
                    case ')' -> charArray[i + 1] = '）';
                    case ';' -> charArray[i + 1] = '；';
                    case ':' -> charArray[i + 1] = '：';
                    case '!' -> charArray[i + 1] = '！';
                    case '?' -> charArray[i + 1] = '?';
                }
            }
            // 符号在前，中文在后
            else if (isChinese(after)) {
                switch (before) {
                    case ',' -> charArray[i] = '，';
                    case '(' -> charArray[i] = '（';
                    case ':' -> charArray[i] = '：';
                }
            }
        }
        return new String(charArray);
    }

    /**
     * 处理空格
     */
    private static String space(String string) {
        //多的空格删掉
        return string.replace("， ", "，")
                .replace("： ", "：")
                .replace(": ", ":")
                //英文逗号后面不是空格，补上
                .replaceAll(",(?!\\s)", ", ");
//                .replace(" ", "")
//                .replace("　", "")
//                .replace("Ø", "")
//                .replace(" ", "")
//                .replace("\t", "")
    }

    /**
     * 删除特殊格式
     */
    private static String delete(String string) {
        return string.replace("  - ", "")
                .replace("### ", "")
                .replace("##", "")
                .replace("• ", "")
                .replace(" 。", "")
                .replace("`", "")
                .replaceAll("(?<!/)\\*\\*", "")
                .replaceAll("\\d、", "")
                .replaceAll("\\d、 ", "")
                .replaceAll("\\d\\. ", "")
                .replaceAll("—*\\s*版权声明.*\\s*原文链接.*", "")
                .replaceAll("—*\\s*原文链接.*", "")
                .replaceAll("作者：.*", "")
                .replaceAll("链接：.*", "")
                .replaceAll("来源：.*", "")
                .replaceAll("著作权归作者所有。.*", "");
    }

    /**
     * 换行
     */
    private static String newLine(String string) {
        string = string.replaceAll("。(?!\\n)", "。\n")
                .replaceAll("！(?!\\n)", "！\n")
                .replaceAll("？(?!\\n)", "？\n")
                .replaceAll("；(?!\\n)", "；\n")
                .replace("\uF06C", "\n");

        StringBuilder sb = new StringBuilder();
        //中文数量
        int chineseCount = 0;

        //太长的句子，中间逗号换行，只算中文的长度
        for (int i = 0; i < string.length() - 1; i++) {
            char current = string.charAt(i);
            char next = string.charAt(i + 1);
            sb.append(current);

            boolean isChinese = isChinese(current);
            //如果中文和英文连在一起，中间增加空格
            if ((isChinese && isLetter(next)) || (isChinese(next) && isLetter(current))) {
                sb.append(" ");
            }

            if (isChinese) {
                chineseCount++;
            } else if (current == '\n') {
                chineseCount = 0;
            }

            if (current == '，' && next != '\n' && chineseCount >= 35) {
                sb.append("\n");
                chineseCount = 0;
            }
        }
        sb.append(string.charAt(string.length() - 1));

        string = sb.toString();
        //删除最后的换行
        if ('\n' == string.charAt(string.length() - 1)) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    private static boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

}