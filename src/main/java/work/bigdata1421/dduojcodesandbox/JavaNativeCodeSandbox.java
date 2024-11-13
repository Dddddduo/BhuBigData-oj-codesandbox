package work.bigdata1421.dduojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import org.springframework.util.StopWatch;
import work.bigdata1421.dduojcodesandbox.model.ExecuteCodeRequest;
import work.bigdata1421.dduojcodesandbox.model.ExecuteCodeResponse;
import work.bigdata1421.dduojcodesandbox.model.ExecuteMessage;
import work.bigdata1421.dduojcodesandbox.model.JudgeInfo;
import work.bigdata1421.dduojcodesandbox.security.MySecurityManager;
import work.bigdata1421.dduojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox{

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final long TIME_OUT = 10000L;
    public static final String SECURITY_MANAGER_PATH ="C:\\Users\\ZDY\\Desktop\\dduoj-code-sandbox\\src\\main\\resources\\security\\MySecurityManager.class";
    private static final List<String> blackList= Arrays.asList("Files","exec");
    private static final WordTree wordTree;

    static {
        // 初始化字典树
        wordTree = new WordTree();
        wordTree.addWords(blackList);
    }
    public static void main(String[] args) {

        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();

        // 搭建请求体
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        // 把请求体反馈给接口 得到响应
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);

        // 输出查看 是否拿到了代码
        // 这边输出的是网站响应
        System.out.println(executeCodeResponse);

    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.setSecurityManager(new MySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 用字典树校验代码
        // 校验代码中 是否有黑名单命令
        FoundWord foundWord = wordTree.matchWord(code);
        if(foundWord !=null){
                System.out.println(foundWord.getFoundWord());
            return null;
        }

        String userDir =System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        // 1.判断全局代码目录是否存在 没有则新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译代码 得到class文件
        String compileCmd =   String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess= Runtime.getRuntime().exec(compileCmd);
            // 等待程序执行 获取错误码
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return getErrorResponse(e);
        }

        // 3.执行代码 获得输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main 1 3", userCodeParentPath, inputArgs);
//            String runCmd = String.format("java -Xmx256m -Djava.security.manager=MySecurityManager %s Main ", userCodeParentPath,SECURITY_MANAGER_PATH ,inputArgs);
            try {
                stopWatch.start();
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() ->{
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了 中断");
                        runProcess.destroy();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                        throw new RuntimeException();
                    }
                }).start();
                // 等待程序执行 获取错误码
                ExecuteMessage runExecuteMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(runExecuteMessage);
                executeMessageList.add(runExecuteMessage);
            } catch (Exception  e) {
                return getErrorResponse(e);
            }
        }

        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值 便于判断是否超时
        long maxTime = 0 ;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                // 执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time =executeMessage.getTime();
            if(time !=null){
                maxTime=Math.max(maxTime,time);
            }
        }
        // 正常运行完成
        if(outputList.size()==executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        // 组装信息
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();

//        for (ExecuteMessage executeMessage : executeMessageList) {
//
//        }

        judgeInfo.setTime(maxTime);
        // 要借助第三方库去实现 非常麻烦
//        judgeInfo.setMemoryLimit();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        
        // 5.文件清理
        // 防止服务器空间不足
        if(userCodeFile.getParentFile()!=null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
        }

        // 6.错误处理 提升程序的健壮性


        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     * @param throwable
     * @return ExecuteCodeResponse
     */
    private ExecuteCodeResponse getErrorResponse(Throwable throwable){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(throwable.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

}
