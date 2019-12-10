package expression.cheney;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 表达式执行器
 *
 * @author cheney
 * @date 2019-12-06
 */
public abstract class ExpressionExecutor implements Executor {

    // 表达式
    protected String express;

    // 方法名
    protected String functionName;

    // 参数
    protected List<ExpressionParser.Arg> args;

    ExpressionExecutor(String express, String functionName, List<ExpressionParser.Arg> args) {
        this.express = express;
        this.functionName = functionName;
        this.args = args;
    }

    protected Object[] loadArgs(List<ExpressionParser.Arg> args, Map<String, Object> env) {
        return CollectionUtils.isEmpty(args) ? null : args.stream().map(arg -> {
            if (arg.isConstant()) {
                return arg.getValue();
            } else if (arg.isFunc()) {
                ExpressionParser.ParseResult parseResult = (ExpressionParser.ParseResult) arg.getValue();
                return executeFunc(parseResult.getFuncName(), parseResult.getArgs(), env);
            } else {
                return env.get((String) arg.getValue());
            }
        }).toArray();
    }

    public abstract Object execute(Map<String, Object> env);

    /**
     * 提供额外的表达式执行方法
     *
     * @param functionName 方法名
     * @param args         参数
     * @param env          变量
     * @return 表达式执行结果
     */
    protected abstract Object executeFunc(String functionName, List<ExpressionParser.Arg> args, Map<String, Object> env);

}
