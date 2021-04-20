# Privpy java SDK



## 功能

对应中台v1.6版本

1. 作为Privpy 数据服务（DS）节点加入MPC计算，用户可以自定义数据读写.
2.  支持作为DS使用SS4协议加入密文运算，目前支持数据类型：String、Double、Integer32、Integer64.
3.  支持的MPC后台关于计算任务API调用封装.
4.  目前提供privpySdk-1.0-SNAPSHOT.jar方式使用本SDK



## 安装

1. 将privpySdk-1.0-SNAPSHOT.jar复制到自己的jar目录下，或任一目录下。
2. gradle 方式引用，在build.gradle

```java
dependencies {
    ...
    implementation files('/Your/jar/path/privpySdk-1.0-SNAPSHOT.jar')
    ...
}
```



## Quick start

项目入口为PrivpyApp，提供如下方法：

```java
/**
* app初始化, 包括申请用户证书、连接MPC集群.
* @throws AuthException 用户验证异常
*/
public void init(String dsId, String clusterAddress, String userName, String password,
                 String certPath, boolean useTls,String httpServer,String httpPort);

/**
* 获取任务管理器
* @return TaskManager
*/
public TaskManager getTaskManager();
/**
* 循环监听任务
*/
public void runLoop();
/**
* 停止服务
*/
public void stop();

```

init所需变量：

```java
String userName = "Username"; //你在MPC中的用户名，如果不清楚，请联系你的MPC集群管理员
String password = "你的密码";  //你在MPC中的用户密码，如果不清楚，请联系你的MPC集群管理员
String clusterAddress = "console.yourname.dev.tsingj.local:31391"; //你的MPC集群地址，如果不清楚请联系你的MPC集群管理员
String dsId = "00000000009"; //你的应用作为DS 的唯一ID，确保不能与MPC集群中其他DS ID重复
String certPath = "/your/cert/storage/path";
boolean useTls = true;       //使用TLS
String dsSecret = "test";    // 加密节点X.509证书的密码
String httpServer = true;       //httpServer
String httpPort = "test";    // httpPort
```



### 示例1 -主动监听MPC下发的任务，自动执行和监听Privpy回调中获动态取任务的执行状态

在自己的项目中引用privpySdk-1.0-SNAPSHOT.jar包, 在你代码的主循环下启动一个Privpy任务，实例代码如下:

```java
    /**
     * 测试主动监听任务
     */
    @Test
    public void testMonitor() throws InterruptedException {
        System.out.println("====testMonitor=====");
        PrivpyApp privpyApp = new PrivpyApp();

        try {
            privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1", "9000");

            //构建读写对象
            WriterCallback dataWriter = new BasicDataWriter();
            ReaderCallback dataReader = new BasicDataReader();

            privpyApp.getTaskManager().registerReaderCallback("cipher://00000000009/adult/adult1", dataReader);
            privpyApp.getTaskManager().registerWriterCallback(dataWriter);

            privpyApp.getTaskManager().registerDefaultTaskCallback(new DefaultTaskCallback());

            privpyApp.runLoop();

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            TimeUnit.SECONDS.sleep(100);
        }

    }
```

用户可以通过privpyApp.getTaskManager()获取任务管理器，再获取任务管理器后。

实现TaskCallback接口，实现registerDefaultTaskCallback()方法。

来监听任务的运行时状况，用户可以重写并且我们提供了默认的实现供用户参考：

```java
public class DefaultTaskCallback implements TaskCallback{
    @Override
    public boolean onVerify(String requestId, Task task) {
        System.out.println("success verify");
        return true;
    }

    @Override
    public void onStart(String requestId) {
        System.out.println("start task");
    }

    @Override
    public void onError(String requestId, Exception e) {
        System.out.println("task running err");
    }

    @Override
    public void onStatusChange(String requestId, TaskStatus status) {
        System.out.println("task status is:"+status);
    }
}
```

* onVerify 用法用量控制回调

 * onStart 任务开始回调
 * onError 任务错误回调
 * onStatusChange 状态变化回调



用户可以通过PrivpyApp.stop停止监听任务，清除资源

```java
public void testMonitor() throws InterruptedException {
    System.out.println("====testMonitor=====");
    PrivpyApp privpyApp = new PrivpyApp();
    try {
        privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1", "9000");
    		privpyApp.stop();

        while (true) {
            TimeUnit.SECONDS.sleep(100);
        }

}
```

用户可以通过PrivpyApp.stop停止监听任务，清除资源



### 示例2 -主动提交任务

```java
/**
 * 提交任务
 */
@Test
public void testsubmitTask() {
    System.out.println("====testsubmitTask=====");
    PrivpyApp privpyApp = new PrivpyApp() {
    };

    try {
        privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1", "9000");

        Task task = new Task();

        DataSource[] dataSources = new DataSource[2];
        DataSource dataSource = new DataSource();
        dataSource.setUrl("cipher://00000000009/movie/data1");
        dataSource.setVarName("data1");
        DataSource dataSource1 = new DataSource();
        dataSource1.setUrl("cipher://00000000009/movie/data2");
        dataSource1.setVarName("data2");
        dataSources[0] = dataSource;
        dataSources[1] = dataSource1;

        ResultDest[] resultDests = new ResultDest[2];
        ResultDest resultDest = new ResultDest();
        resultDest.setDestId("00000000009");
        resultDest.setUrl("cipher://00000000009/movie/result1");
        ResultDest resultDest1 = new ResultDest();
        resultDest1.setDestId("00000000009");
        resultDest1.setUrl("cipher://00000000009/movie/result2");
        resultDests[0] = resultDest;
        resultDests[1] = resultDest1;

        task.setName("testTask202011281");
        task.setCode("import privpy as pp\n" +
                "import privpy as pp\n" +
                "a = pp.ss('data1')\n" +
                "b = pp.ss('data2')\n" +
                "pp.reveal(a[:10],'result1')\n" +
                "pp.reveal(b[:10],'result2')\n");
        task.setBackend("ss4");

        TaskServerInfo taskServerInfo = new TaskServerInfo();
        taskServerInfo.setQuota(1000);
        task.setServerInfo(taskServerInfo);

        task.setDestinations(resultDests);
        task.setSources(dataSources);

        TaskManager taskManager  = privpyApp.getTaskManager();
        CreateTask createTask = taskManager.submitTask(task);
        System.out.println(createTask.getTaskId());
        Assert.assertEquals(RetCode.SUCCESS,createTask.getRetCode());
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println("====testsubmitTask=====");
}
```

#### 示例3 -主动删除任务

```java
/**
 * 测试主动删除任务
 */
@Test
public void testDeleteTask() throws AuthException, IOException {
    System.out.println("====testDeleteTask=====");
    PrivpyApp privpyApp = new PrivpyApp();
    privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1","9000");
    TaskManager taskManager = privpyApp.getTaskManager();
    RetCode retCode = taskManager.deleteTask("10");
    Assert.assertEquals(retCode,RetCode.SUCCESS);
    System.out.println("====testDeleteTask Finish=====");
}
```

#### 示例4 -主动杀死任务

```java
/**
 * 测试主动杀死任务
 */
@Test
public void testKillTask() throws AuthException, IOException {
    System.out.println("====testKillTask=====");
    PrivpyApp privpyApp = new PrivpyApp();
    privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1","9000");
    TaskManager taskManager = privpyApp.getTaskManager();
    RetCode retCode = taskManager.killTask("9");
    Assert.assertEquals(RetCode.SUCCESS,retCode);
    System.out.println("====testKillTask Finish=====");
}
```

#### 示例4 -修改任务

```java
/**
 * 测试修改任务
 */
@Test
public void modifyTask() {
    System.out.println("====testModifyTask=====");
    PrivpyApp privpyApp = new PrivpyApp();

    try {
        privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1","9000");

       Task task = new Task();
        TaskManager taskManager = privpyApp.getTaskManager();
        task.setDescription("a");
        RetCode retCode = taskManager.modifyTask(task);
        Assert.assertEquals(retCode,RetCode.SUCCESS);
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println("====testModifyTask Finish=====");
}
```

#### 示例4 -任务列表

```java
/**
 * 获取任务列表
 */
@Test
public void testGetTaskList() {
    System.out.println("====getTaskList====");
    PrivpyApp privpyApp = new PrivpyApp();

    try {
        privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1","9000");
        TaskManager taskManager = privpyApp.getTaskManager();
        TaskList taskList = taskManager.getTaskList(10,1);
        Assert.assertEquals(taskList.getRetCode(),RetCode.SUCCESS);
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println("====getTaskList Finish=====");
}
```



#### 示例5 -任务详情

```java
/**
 * 获取任务详情
 */
@Test
public void getTask() {
    System.out.println("====testGetTask====");
    PrivpyApp privpyApp = new PrivpyApp();

    try {
        privpyApp.init(dsId, clusterAddress, userName, password, certPath, useTls, "127.0.0.1","9000");
        TaskManager taskManager = privpyApp.getTaskManager();
        TaskResponse taskResponse = taskManager.getTask(taskId);
        System.out.println(taskResponse);
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println("====testTaskDetail Finish=====");
}
```