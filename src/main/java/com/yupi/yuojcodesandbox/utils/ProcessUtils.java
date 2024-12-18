package com.yupi.yuojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 编译
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码： " + exitValue);
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 运行
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @param inputArgs
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName ,String inputArgs) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            System.out.println(opName);
            StopWatch runStopWatch = new StopWatch();
            runStopWatch.start();

            // 等待程序执行，获取错误码
//            executeMessage.setExitValue(exitValue);

            List<String> outputStrList = new ArrayList<>();

            OutputStream outputStream = runProcess.getOutputStream();
            InputStream inputStream = runProcess.getInputStream();

            System.out.println("拿到的输入流: "+outputStream.toString());
            System.out.println("拿到的输出流: "+inputStream.toString());

            System.out.println("传入的参数是" + inputArgs);

            // 向进程传递输入参数
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                writer.write(inputArgs);
                writer.newLine();  // 如果需要换行
                writer.flush();
            }catch ( Exception e){
                System.out.println("向进程传递输入参数 抛出异常");
                e.printStackTrace();
            }

            // 仅用于测试 StopWatch 的功能
            // todo 完善代码执行时间信息的获取
//            Thread.sleep(1000);

            int exitCode  = runProcess.waitFor();
            System.out.println("进程的退出码是"+exitCode);

            // 读取进程的输出（如果有的话）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("结果是+"+line);
                    outputStrList.add(line);
                }
            }catch ( Exception e){
                System.out.println("读取进程的输出 抛出异常");
                e.printStackTrace();
            }

            // 获取执行时间
            runStopWatch.stop();
            executeMessage.setTime(runStopWatch.getLastTaskTimeMillis());

            executeMessage.setMessage(StringUtils.join(outputStrList, "\n"));

            //
            // 获取进程的错误输出流
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.err.println("错误的输出流: " + errorLine);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

//    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName, String inputArgs) {
//        ExecuteMessage executeMessage = new ExecuteMessage();
//        try {
//            System.out.println(opName);
//            StopWatch runStopWatch = new StopWatch();
//            runStopWatch.start();
//
//            List<String> outputStrList = new ArrayList<>();
//
//            OutputStream outputStream = runProcess.getOutputStream();
//            InputStream inputStream = runProcess.getInputStream();
//            InputStream errorStream = runProcess.getErrorStream();  // 获取错误流
//
//            System.out.println("传入的参数是" + inputArgs);
//
//            // 向进程传递输入参数
//            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
//                writer.write(inputArgs);
//                writer.newLine();  // 如果需要换行
//                writer.flush();
//            } catch (Exception e) {
//                System.out.println("向进程传递输入参数 抛出异常");
//                e.printStackTrace();
//            }
//
//            // 启动线程读取输出流，避免阻塞
//            Thread outputThread = new Thread(() -> {
//                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        System.out.println("结果是: " + line);
//                        outputStrList.add(line);
//                    }
//                } catch (Exception e) {
//                    System.out.println("读取进程的输出 抛出异常");
//                    e.printStackTrace();
//                }
//            });
//            outputThread.start();
//
//            // 启动线程读取错误流，避免死锁
//            Thread errorThread = new Thread(() -> {
//                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {
//                    String errorLine;
//                    while ((errorLine = errorReader.readLine()) != null) {
//                        System.err.println("错误: " + errorLine);
//                    }
//                } catch (Exception e) {
//                    System.out.println("读取进程的错误流 抛出异常");
//                    e.printStackTrace();
//                }
//            });
//            errorThread.start();
//
//            // 等待输出流线程和错误流线程结束
//            outputThread.join();
//            errorThread.join();
//
//            // 获取执行时间
//            runStopWatch.stop();
//            executeMessage.setTime(runStopWatch.getLastTaskTimeMillis());
//
//            executeMessage.setMessage(String.join("\n", outputStrList));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return executeMessage;
//    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

}
