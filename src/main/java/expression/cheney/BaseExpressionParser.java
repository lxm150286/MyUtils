package expression.cheney;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static expression.cheney.BaseExpressionParser.Arg.ORIGIN;
import static expression.cheney.CharConstants.*;

/**
 * 表达式解析器抽象接口,提供基础的解析方法实现
 * <p>
 * 表达式解析支持:
 * 1.解析方法表达式(funcA(xx));
 * 2:解析''为分隔符的字符串(funcA('xx'));
 * 3.解析变量(funA(a),a将视为变量);
 * 4.嵌套函数解析(例如：funcA(funcB(funcC()))，通过递归实现)
 * <p>
 * 解析的结果封装为{@link ParseResult},一个方法表达式为一个ParseResult，
 * 例如funcA(funcB()，解析结果ParseResult实体中，其成员变量args{@link ParseResult.args}为单个元素的{@link Arg}，
 * 元素Arg中的成员变量{@link Arg.value}为funB解析结果的实体{@link ParseResult}，形成嵌套。
 *
 * 1.1 新增支持运算符嵌套函数解析,见{@link BaseExpressionParser#createArg(List, Arg, Object, short)} }
 *
 * @version 1.1
 * @author cheney
 * @date 2019-12-07
 */
public abstract class BaseExpressionParser implements ExpressionParser {

    public abstract ExpressionExecutor parseExpression(String expression);

    /**
     * 表达式解析结果
     * 例如: ifs(a>b,c) 则
     * args字段：a>b与c {@link Arg}
     * funcName字段：ifs
     * noFunc字段:当表达式不为函数时，该字段为true
     */
    @Data
    @AllArgsConstructor
    protected static class ParseResult {
        private String funcName;
        private List<Arg> args;
        private short type;
        // 类型枚举值
        public final static short FUNC = 1;
        public final static short ORIGIN = 2;
        public final static short OPERATOR_FUNC = 3;

        public static ParseResult origin() {
            return new ParseResult(null, null, ORIGIN);
        }

        public static ParseResult func(String funcName, List<Arg> args) {
            return new ParseResult(funcName, args, FUNC);
        }

        public static ParseResult funcOperator(Arg arg) {
            return new ParseResult(null, Collections.singletonList(arg), OPERATOR_FUNC);
        }

        public boolean isOrigin() {
            return ORIGIN == type;
        }

        public boolean isFunc() {
            return FUNC == type;
        }

        public boolean isOperatorFunc() {
            return OPERATOR_FUNC == type;
        }
    }

    /**
     * 参数段类实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Arg {
        // 值
        private Object value;
        // 类型：0:常量,1:函数,2:运算,3:函数嵌套运算
        private short type;
        // 类型枚举值
        public final static short CONSTANT = 0;
        public final static short FUNC = 1;
        public final static short ORIGIN = 2;
        public final static short OPERATOR_FUNC = 3;
        public final static short OPERATOR = 4;
    }

    /**
     * 解析方法表达式
     *
     * @param expression 表达式
     * @return 解析结果 ParseResult实体
     */
    protected static ParseResult parse(String expression) {
        if (StringUtils.isEmpty(expression)) {
            throw new ExpressionParseException("expression can not be empty");
        }
        int start = expression.indexOf("(");
        int length = expression.length();
        if (start == -1 || start == 0) {
            // 不包含(则不为函数
            return ParseResult.origin();
        }
        List<Arg> args = parseArg(expression.substring(start + 1, length - 1));

        return ParseResult.func(expression.substring(0, start), args);
    }

    /**
     * 解析方法表达式中的参数段类
     *
     * @param expression 方法的参数段类,例:ifs(a>b,c)-->'a>b,c'
     * @return 参数解析结果 Arg集合
     */
    private static List<Arg> parseArg(String expression) {
        char[] chars = expression.toCharArray();
        int length = chars.length;
        int endIndex = length - 1;
        // 语句开始位置
        Integer startIndex = null;
        // 语句结尾检查字符
        Character endCheck = null;
        int count = 0;
        List<Arg> result = new ArrayList<>();
        Arg partLast = null;
        for (int i = 0; i < length; i++) {
            char c = chars[i];
            if (SPACE_CHAR == c) {
                // 跳过无用空格
                continue;
            }
            if (startIndex == null && COMMA_CHAR != c) {
                // 段落开始
                startIndex = i;
            }
            if (APOSTROPHE_CHAR == c && (endCheck == null || endCheck == APOSTROPHE_CHAR)) {
                // 当前char为'
                endCheck = APOSTROPHE_CHAR;
                count++;
            }
            if (BRACKETS_LEFT_CHAR == c && (endCheck == null || endCheck == BRACKETS_RIGHT_CHAR)) {
                // 当前char为(,每遇到一个(加一，没遇到一个)减一，直到最后一个)视为结束
                if (endCheck == null) {
                    endCheck = BRACKETS_RIGHT_CHAR;
                }
                count++;
            }
            boolean end = i == endIndex;
            if ((ArrayUtils.contains(END_CHAR, c) || end) && startIndex != null) {
                // 结束
                if (endCheck != null) {
                    if (c == endCheck) {
                        if (APOSTROPHE_CHAR == endCheck) {
                            if (count == 2) {
                                // 出现第二个'结束
                                partLast = createArg(result, partLast, expression.substring(startIndex + 1, i), Arg.CONSTANT);
                                count = 0;
                            }
                        } else if (BRACKETS_RIGHT_CHAR == endCheck) {
                            if (count == 1) {
                                // 匹配')'时只有一个未匹配的'('结束
                                partLast = createArg(result, partLast, expression.substring(startIndex, i + 1), Arg.FUNC);
                            }
                            count--;
                        }
                    }
                    if (end && count != 0) {
                        // 到达结尾时，无法匹配完endCheck时抛出异常
                        throw new ExpressionParseException("end char miss \"" + endCheck + "\"");
                    }
                } else {
                    // 不需要endCheck
                    if (end) {
                        i++;
                    } else if (startIndex == i) {
                        continue;
                    }
                    partLast = createArg(result, partLast, expression.substring(startIndex, i), ORIGIN);
                }
                if (count == 0) {
                    // 匹配结束符并且count为0时,标识段落结束
                    startIndex = null;
                    endCheck = null;
                    if (c == COMMA_CHAR) {
                        // 段落结束
                        partLast = null;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 创建arg/添加arg，并返回最新的arg
     * <p>
     * version1.1新增
     *
     * @param argResult arg集合
     * @param partLast  此段落上一个arg
     * @param value     新arg值
     * @param type      新arg类型
     * @return 最新arg
     */
    @SuppressWarnings("unchecked")
    private static Arg createArg(List<Arg> argResult, Arg partLast, Object value, short type) {
        boolean createNew = true;
        if (Arg.FUNC == type) {
            // 参数为函数
            String function = (String) value;
            String funcName = function.substring(0, function.indexOf("(")).trim();
            if (OPERATOR_PATTERN.matcher(funcName).matches()) {
                type = ORIGIN;
                value = ((String) value).trim();
            } else if (OPERATOR_START_PATTERN.matcher(funcName).matches()) {
                /* 方法名包含运算符，则将arg解析为一个List用来存'运算符嵌套函数':
                 * List中按源运算表达式顺序存放两种arg实体,一种为运算符OPERATOR(type:4),一种存函数FUNC(type:1)*/
                Object[] results = extractOperators(funcName);
                List<String> operators = (List<String>) results[0];
                // 运算符存为OPERATOR(type:4)
                List<Arg> operatorArgs = operators.stream().map(operator -> new Arg(operator, Arg.OPERATOR)).collect(Collectors.toList());
                // 解析去除运算符后的函数表达式
                ParseResult func = parse(function.substring((Integer) results[1]).trim());
                // 解析结果存为函数FUNC(type:1)
                Arg funcArg = new Arg(func, Arg.FUNC);
                /* 最后将新生成的函数arg与运算符arg放入对应的List中
                 * 1:partLast(此段落前一个arg)不为空，则以partLast为List存入运算符arg与函数arg
                   2:partLast为空，则新建List存入运算符arg与函数arg作为新arg*/
                if (partLast != null) {
                    // 此段落前一个arg不为空
                    createNew = false;
                    Object partLastValue = partLast.getValue();
                    short partLastType = partLast.getType();
                    ArrayList<Arg> args;
                    if (partLastValue.getClass() == ArrayList.class) {
                        args = (ArrayList<Arg>) partLastValue;
                    } else if (partLastValue.getClass() == ParseResult.class) {
                        // 必须为函数才可嵌套运算符
                        args = new ArrayList<>();
                        args.add(new Arg(partLastValue, partLastType));
                        partLast.setValue(args);
                    } else {
                        throw new ExpressionParseException("error type of last Arg:" + partLastValue);
                    }
                    partLast.setType(Arg.OPERATOR_FUNC);
                    args.addAll(operatorArgs);
                    args.add(funcArg);
                } else {
                    // 此段落的前一个arg为空，则新建一个List存放运算符与函数
                    type = Arg.OPERATOR_FUNC;
                    operatorArgs.add(funcArg);
                    value = operatorArgs;
                }
            } else {
                // 参数为单独一个函数表达式,执行方法表达式解析
                value = parse(function);
            }
        } else if (type != Arg.CONSTANT) {
            // 不为方法不为常类的arg，去除前后的空格
            value = ((String) value).trim();
        }

        if (createNew) {
            Arg newArg = new Arg(value, type);
            argResult.add(newArg);
            return newArg;
        }
        return partLast;
    }

    /**
     * 提取表达式开头的运算符,用List存放
     * <p>
     * version1.1新增
     *
     * @param expression 表达式
     * @return 0:List<Arg> 1:运算符结束index
     */
    private static Object[] extractOperators(String expression) {
        List<String> operators = new ArrayList<>();
        int index = 0;
        char[] chars = expression.toCharArray();
        Character lastChar = null;
        for (; index < chars.length; index++) {
            // 包含运算符
            char currentChar = chars[index];
            if (ArrayUtils.contains(OPERATORS, currentChar)) {
                if (lastChar == null) {
                    lastChar = currentChar;
                    // 第一个运算符可能与后面的成为整体:例如&&,>=
                    continue;
                }
                String a = String.valueOf(lastChar);
                String b = String.valueOf(currentChar);
                if (lastChar == currentChar || (currentChar == EQUAL && (lastChar == LE || lastChar == GE))) {
                    operators.add(a + b);
                } else {
                    operators.add(a);
                    operators.add(b);
                }
                lastChar = null;
            } else {
                if (lastChar != null) {
                    operators.add(String.valueOf(lastChar));
                    lastChar = null;
                }
                if (currentChar != SPACE_CHAR) {
                    break;
                }
            }
        }
        return new Object[]{operators, index};
    }

}
