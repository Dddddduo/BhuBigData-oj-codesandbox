package com.yupi.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/**
 * Java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 2000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        System.out.println("看看我执行了吗");

        // 走这里
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

//        2. 编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println("编译成功,得到class文件"+compileFileExecuteMessage);

//        3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList ,compileFileExecuteMessage);

//        4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        System.out.println(outputResponse);

//        5. 文件清理
//        boolean b = deleteFile(userCodeFile);
//        if (!b) {
//            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
//        }
        return outputResponse;
//        return null;
    }


    /**
     * 1. 把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2、编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            // 如果编译错误程序退出状态是1
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                executeMessage.setErrorMessage("编译错误");
//                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3、执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */

    // Process 写法
//    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList ,ExecuteMessage compileExecuteMessage) {
//        // 如果编译错误 直接退出
//        if(compileExecuteMessage.getErrorMessage()!=null&&compileExecuteMessage.getErrorMessage().equals("编译错误")){
//            return null;
//        }
//        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
//        System.out.println("编译后的文件的路径是"+userCodeParentPath);
//        List<ExecuteMessage> executeMessageList = new ArrayList<>();
//        for (String inputArgs : inputList) {
//            try {
//                // 构造命令，不传递参数
//                String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp \"%s\" Main", userCodeParentPath);
//                System.out.println("执行的代码是"+runCmd);
//                // 启动进程
//                Process runProcess = Runtime.getRuntime().exec(runCmd);
//                // 在每一个用例里面 去处理 创建一个新的线程 先睡2000s 时间一到就销毁主线程 实现超时处理
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(TIME_OUT);
//                        System.out.println("超时了，中断");
//                        runProcess.destroy();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();
//                // 通过工具类获取输入输出
//                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行" ,inputArgs);
//                System.out.println(executeMessage);
//                executeMessageList.add(executeMessage);
//            } catch (Exception e) {
//                throw new RuntimeException("执行错误", e);
//            }
//        }
//        return executeMessageList;
//    }

    // ProcessBuilder 写法
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList, ExecuteMessage compileExecuteMessage) {
        // 如果编译错误 直接退出
        if (compileExecuteMessage.getErrorMessage() != null && compileExecuteMessage.getErrorMessage().equals("编译错误")) {
            return null;
        }

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        System.out.println("编译后的文件的路径是" + userCodeParentPath);

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            try {
                // 构造命令，不传递参数
                String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp \"%s\" Main", userCodeParentPath);
                System.out.println("执行的代码是" + runCmd);

                // 使用 ProcessBuilder 创建一个新的进程
                ProcessBuilder processBuilder = new ProcessBuilder(runCmd.split(" "));
                processBuilder.directory(new File(userCodeParentPath)); // 设置工作目录
                processBuilder.redirectErrorStream(true); // 合并标准错误输出和标准输出

                // 启动进程
                Process runProcess = processBuilder.start();

                // 在每一个用例里面创建一个新的线程来处理超时
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT); // 等待 TIME_OUT 毫秒
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

                // 通过工具类获取输入输出
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行", inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);

            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }


    /**
     * 4、获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {

        // 编译错误 直接退出
        if(executeMessageList==null){
            return new ExecuteCodeResponse("编译错误",3,null);
        }

        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

}
