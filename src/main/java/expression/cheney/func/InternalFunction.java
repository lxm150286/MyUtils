package expression.cheney.func;

import com.alibaba.fastjson.JSON;
import jsonUtils.JsonUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 表达式执行器内置函数
 *
 * @author cheney
 * @date 2019-12-13
 */
public class InternalFunction {

    public final static String OUT_PUT_FUNC_NAME = "output";

    public static void println(Object obj) {
        System.out.println(obj);
    }

    public static void print(Object obj) {
        System.out.print(obj);
    }

    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static Map<String, Object> jsonToMap(String json) {
        return JSON.parseObject(json);
    }

    public static List<Object> jsonToList(String json) {
        return JSON.parseArray(json);
    }

    public static String substring(String text, int beginIndex, int len) {
        return text.substring(beginIndex, beginIndex + len);
    }

    public static String date_format(Date date, String format) {
        return DateFormatUtils.format(date, format);
    }

    public static Object ifs(Object... objs) {
        System.out.println(JsonUtils.toJson(objs));
        for (int i = 0; i < objs.length; i++) {
            if ((i & 1) == 0 && (boolean) objs[i]) {
                return objs[i + 1];
            }
        }
        return "error";
    }

    public static boolean contains(String text, String content) {
        return text.contains(content);
    }

    public static String replace(String text, String oldChar, String replacement) {
        return text.replace(oldChar, replacement);
    }

    public static Object output(Object object) {
        return object;
    }

}
