package myMind.common.util;

public class FormatUtil {

    //格式化
    public static String format(String string) {
        string = space(string);
        string = punctuation(string);

        //删除特殊格式
        string = string.replaceAll("  - ", "")
                .replaceAll("### ", "")
                .replaceAll("##", "")
//                .replaceAll("- ", "")
                .replaceAll("(?<!/)\\*\\*", "")
                .replaceAll(" 。", "")
                .replaceAll("`", "")
                .replaceAll("\\d、", "")
                .replaceAll("\\d、 ", "")
                .replaceAll("\\d\\. ", "")
                .replaceAll("—*\\s*版权声明.*\\s*原文链接.*", "")
                .replaceAll("—*\\s*原文链接.*", "")
                .replaceAll("作者：.*", "")
                .replaceAll("链接：.*", "")
                .replaceAll("来源：.*", "")
                .replaceAll("著作权归作者所有。.*", "")
                .replaceAll("• ", "");

        string = newLine(string);

        return string;
    }

    /**
     * 以冒号分割字符串
     *
     */
    public static String split(String string) {
        String[] splitWithNewLine = string.split("\n");
        StringBuilder sb = new StringBuilder();

        for (String s : splitWithNewLine) {
            sb.append(s.split("：")[0]);
            sb.append("\n");
        }

        for (String s : splitWithNewLine) {
            if (s.split("：").length >= 2) {
                sb.append(s.split("：")[1]);
                sb.append("\n");
            } else {
                return string;
            }
        }
        // 删除最后一个换行符
        return sb.substring(0, string.length() - 1);
    }

    /**
     * 删除结尾的句号
     */
    public static String deletePeriod(String string) {
        // endsWith 内部有判空
        if (string.endsWith("。")) {
            return string.substring(0, string.length() - 1);
        } else {
            return string;
        }
    }

    private static String space(String string) {
        //多的空格删掉
        return string.replace("， ", "，")
                .replace("： ", "：")
                .replace(": ", ":")
                //英文逗号后面不是空格，补上
                .replaceAll(",(?!\\s)", ", ");
//        string.replace(" ", "")
//                .replace(" ", "")
//                .replace("　", "")
//                .replace("Ø", "")
//                .replace(" ", "")
//                .replace("\t", "")
//                .replace("*", "");
    }

    /**
     * 英文符号转换成中文符号
     */
    private static String punctuation(String string) {
        string = string.replaceAll("\\.\\.\\.", "……");

        char[] charArray = string.toCharArray();

        for (int i = 0; i < charArray.length - 1; i++) {
            char before = charArray[i];
            char after = charArray[i + 1];

            //中文+英文逗号
            if (isChinese(before) && after == ',') {
                charArray[i + 1] = '，';
                //中文+英文句号
            } else if (isChinese(before) && after == '.') {
                charArray[i + 1] = '。';
                //英文逗号+中文
            } else if (before == ',' && isChinese(after)) {
                charArray[i] = '，';
                //英文句号+中文
            } /*else if (before == '.' && isChinese(after)) {
                charArray[i] = '。';
            } */ else if (before == '(' && isChinese(after)) {
                //英文左括号+中文
                charArray[i] = '（';
            } else if (isChinese(before) && after == ')') {
                //中文+英文右括号
                charArray[i + 1] = '）';
            } else if (isChinese(before) && after == ';') {
                //中文+英文分号
                charArray[i + 1] = '；';
            } else if (isChinese(before) && after == ':') {
                //中文+英文冒号
                charArray[i + 1] = '：';
            } else if (before == ':' && isChinese(after)) {
                //英文冒号+中文
                charArray[i] = '：';
            } else if (isChinese(before) && after == '!') {
                //中文+英文感叹号
                charArray[i + 1] = '！';
            } else if (isChinese(before) && after == '?') {
                //中文+英文问号
                charArray[i + 1] = '?';
            }
        }

        return new String(charArray);
    }

    private static boolean isChinese(char c) {
        return (c >= '\u4e00' && c <= '\u9fff') ||  // 基本汉字
                (c >= '\u3400' && c <= '\u4dbf');   // 生僻汉字、古籍用字
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }

    /**
     * 换行
     */
    private static String newLine(String string) {
        if (string.isEmpty()) {
            return string;
        }

        string = string.replaceAll("。(?!\\n)", "。\n")
                .replaceAll("！(?!\\n)", "！\n")
                .replaceAll("？(?!\\n)", "？\n")
                .replaceAll("；(?!\\n)", "；\n")
                .replaceAll("\uF06C", "\n");

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

    private static String deleteShuang(String string) {
        return string.replace("“", "")
                .replace("”", "")
                .replace("\"", "");
    }
}