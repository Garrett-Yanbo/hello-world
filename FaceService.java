package com.tsingj.privpy.icbc;

import com.tsingj.privpy.sdk.PrivpyApp;
import com.tsingj.privpy.sdk.TaskManager;
import com.tsingj.privpy.sdk.exception.BusinessException;
import com.tsingj.privpy.sdk.exception.PrivpyPOJOException;
import com.tsingj.privpy.sdk.pojo.DataType;
import com.tsingj.privpy.sdk.pojo.Metadata;
import com.tsingj.privpy.sdk.pojo.PlainData;
import com.tsingj.privpy.sdk.pojo.Shape;
import com.tsingj.privpy.sdk.pojo.data.DataFloat64;
import com.tsingj.privpy.sdk.pojo.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class FaceService {

    private static final Logger logger = Logger.getLogger(FaceService.class.getName());

    private PrivpyApp privpyApp;
    private TaskManager taskManager;

    private String password = "1234qwer";
    private String userName = "admin_dsconsole";
    private String dsIdMain = "00000000009";
    private String dsIdSub = "00000000010";
    private String dsId = "00000000009";
    private String isMainDs = "false";
    private String clusterAddress = "console.test.dev.tsingj.local:31391";
    private String certPath = "/Users/test/cert";
    private String httpServer = "127.0.0.1";
    private String httpPort = "9000";
    private HashMap<String, String> paraHashMap;


    private StreamReader streamReader = new StreamReader();
    private StreamWriter streamWriter = new StreamWriter();

    private static FaceService singleton = null;


    RetCode retCode;

    public FaceService(String filepath) {
        this.privpyApp = new PrivpyApp();
        try {
            this.paraHashMap = this.GetAllProperties(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.paraHashMap.get("password")!=null){
            this.password = this.paraHashMap.get("password");
        }
        if (this.paraHashMap.get("userName")!=null){
            this.userName = this.paraHashMap.get("userName");
        }
        if (this.paraHashMap.get("dsIdMain")!=null){
            this.dsIdMain = this.paraHashMap.get("dsIdMain");
        }
        if (this.paraHashMap.get("dsIdSub")!=null){
            this.dsIdSub = this.paraHashMap.get("dsIdSub");
        }
        if (this.paraHashMap.get("clusterAddress")!=null){
            this.clusterAddress = this.paraHashMap.get("clusterAddress");
        }
        if (this.paraHashMap.get("certPath")!=null){
            this.certPath = this.paraHashMap.get("certPath");
        }
        if (this.paraHashMap.get("isMainDs")!=null){
            this.isMainDs = this.paraHashMap.get("isMainDs");
        }
        if (this.paraHashMap.get("httpServer")!=null){
            this.httpServer = this.paraHashMap.get("httpServer");
        }
        if (this.paraHashMap.get("httpPort")!=null){
            this.httpPort = this.paraHashMap.get("httpPort");
        }
    }

    public int init(){

        if("true".equals(this.isMainDs)){
            logger.info(String.format("sdk start as the main ds and dsId is %s!",dsIdMain));
            this.dsId = this.dsIdMain;
        }else {
            logger.info(String.format("sdk start as the slave ds and dsId is %s!",dsIdSub));
            this.dsId = this.dsIdSub;
        }

        logger.fine("test the log fine yangxing!");
        try {
            logger.info(String.format("sdk start and param is dsId:%s clusterAddress:%s userName:%s password:%s " +
                            "certPath:%s useTls:%s httpServer:%s httpPort:%s",dsId,clusterAddress,userName,password,certPath,true,this.httpServer,this.httpPort));
            this.retCode = this.privpyApp.init(this.dsId, this.clusterAddress, this.userName, this.password, this.certPath, true, this.httpServer, this.httpPort);
        } catch (BusinessException e) {
            e.printStackTrace();
            return 1;
        }
        if (RetCode.SUCCESS != retCode) {
            return 1;
        }
        this.taskManager = this.privpyApp.getTaskManager();
        try {
            String dataUrl = "cipher://" + dsId + "/face";
            logger.info(String.format("the ds data url is:%s",dataUrl));
            this.taskManager.registerReaderCallback(dataUrl, streamReader);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        try {
            this.taskManager.registerWriterCallback("default", streamWriter);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        this.privpyApp.runLoop();
        return 0;
    }

    public Map<String, Object> sendData(Map<String, Object> inPutData) {

        Long beginTime = System.currentTimeMillis();
        String libData = (String) inPutData.get("libData");
        String terminalData = (String) inPutData.get("terminalData");
        String taskCode = (String) inPutData.get("taskCode");
        Long timeOut = (Long) inPutData.get("timeOut");


        HashMap<String, Object> mapRes = new HashMap<>();
        mapRes.put("return_code", "0");
        mapRes.put("return_msg", "success");

        Boolean dataFormat = isDataFormatRight(inPutData);
        if (!dataFormat) {
            mapRes.put("return_code", "1");
            mapRes.put("return_msg", "data length wrong");
            return mapRes;
        }

        //transform the taskcode
        Double taskCodeInner = Double.valueOf(taskCode.hashCode());
        PlainData pd = null;
        Metadata metadata = new Metadata(DataType.Double,
                new Shape(new Integer[]{1, 517}),
                "face data size for length");
        try {
            pd = new PlainData(metadata);
        } catch (PrivpyPOJOException e) {
            e.printStackTrace();
        }

        //1、设置第一个数据类型
        DataFloat64 doubleData = new DataFloat64(taskCodeInner);
        pd.append(doubleData);

        String[] libDatas = libData.split(",");
        for(int i =0;i<libDatas.length;i++){
            DataFloat64 dataFloat64 = new DataFloat64(Double.valueOf(libDatas[i]));
            pd.append(dataFloat64);
        }

        String[] terminalDatas = terminalData.split(",");
        for(int i =0;i<terminalDatas.length;i++){
            DataFloat64 dataFloat64 = new DataFloat64(Double.valueOf(terminalDatas[i]));
            pd.append(dataFloat64);
        }

        if (null != pd) {
            try {
                //清理掉系统中无用的结果，对于当前1TPS的系统缓存清理机制，后续高并发情况需要考虑该缓存处理机制
                this.streamReader.clearCache();
                this.streamWriter.clearCache();
                this.streamReader.putData(pd);
            } catch (InterruptedException e) {
                e.printStackTrace();
                mapRes.put("return_code", "1");
                mapRes.put("return_msg", "data format error");
                return mapRes;
            }
        } else {
            mapRes.put("return_code", "1");
            mapRes.put("return_msg", "data format error");
            return mapRes;
        }

        //发送人脸任务
        Boolean sendTaskResult = false;
        //清理掉系统中无用的结果，对于当前1TPS的系统缓存清理机制，后续高并发情况需要考虑该缓存处理机制
        this.taskManager.getPrivpyClient().getDsIdSessionSet().clear();
        this.taskManager.getPrivpyClient().getTaskIdSessionMap().clear();

        if("true".equals(this.isMainDs)){
            sendTaskResult = this.taskSender(taskCode);
            if(sendTaskResult){
                this.taskManager.getPrivpyClient().getDsIdSessionSet().add(taskCode);
            }else {
                mapRes.put("return_code", "1");
                mapRes.put("return_msg", "send face task err!");
                return mapRes;
            }
        }else {
            //TODO 从节点在任务发送失败时，无法自主清理sessionCode值
            this.taskManager.getPrivpyClient().getDsIdSessionSet().add(taskCode);
        }

        //获取计算结果
        Float dataResult = null;
        try {
            dataResult = this.streamWriter.getData(taskCodeInner, timeOut);
            //清理掉系统中无用的结果，对于当前1TPS的系统缓存清理机制，后续高并发情况需要考虑该缓存处理机制
            this.streamWriter.clearCache();
        } catch (InterruptedException e) {
            e.printStackTrace();
            mapRes.put("return_code", "2");
            mapRes.put("return_msg", "system err");
            return mapRes;
        }

        if (null == dataResult) {
            mapRes.put("return_code", "1");
            mapRes.put("return_msg", "time out");
            return mapRes;
        }
        mapRes.put("return_data", dataResult);
        Long callingTime = System.currentTimeMillis() - beginTime;
        logger.info(String.format("the calling for %s used time for %s ",taskCode,callingTime));
        return mapRes;
    }


    private Boolean isDataFormatRight(Map<String, Object> inPutData) {

        String libData = (String) inPutData.get("libData");
        String terminalData = (String) inPutData.get("terminalData");
        String taskCode = (String) inPutData.get("taskCode");

        Long timeOut = (Long) inPutData.get("timeOut");

        if (libData != null && libData.length() != 0) {
            String[] faceData = libData.split(",");
            if (faceData.length != 258) {
                return false;
            }
        } else {
            return false;
        }

        if (terminalData != null && terminalData.length() != 0) {
            String[] faceData = terminalData.split(",");
            if (faceData.length != 258) {
                return false;
            }
        } else {
            return false;
        }

        if (taskCode == null){
            return false;
        }

        if (timeOut==null){
            return false;
        }
        return true;
    }

    public Map<String, Object> getData(Map<String, Object> inPutData) {

        Long timeout = (Long) inPutData.get("timeout");
        String taskCode = (String) inPutData.get("taskCode");

        HashMap<String, Object> mapRes = new HashMap<>();
        mapRes.put("return_code", "0");
        mapRes.put("return_msg", "success");
        mapRes.put("return_data", 0);

        Float dataResult = null;
        try {
            dataResult = this.streamWriter.getData(Double.valueOf(taskCode), timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
            mapRes.put("return_code", "2");
            mapRes.put("return_msg", "system err");
            return mapRes;
        }

        if (null == dataResult) {
            mapRes.put("return_code", "1");
            mapRes.put("return_msg", "time out");
            return mapRes;
        }

        mapRes.put("return_data", dataResult);
        return mapRes;
    }

    //读取Properties的全部信息
    public HashMap<String, String> GetAllProperties(String filePath) throws IOException {
        Properties pps = new Properties();
//        InputStream in = new BufferedInputStream(new FileInputStream(filePath));
        InputStream in = this.getClass().getResourceAsStream(filePath);
        pps.load(in);
        //得到配置文件的名字
        Enumeration en = pps.propertyNames();
        HashMap<String, String> hashMap = new HashMap<>();
        while (en.hasMoreElements()) {
            String strKey = (String) en.nextElement();
            String strValue = pps.getProperty(strKey);
            hashMap.put(strKey, strValue);
        }
        return hashMap;
    }

    private Boolean taskSender(String taskCode){
        Task task = new Task();
        DataSource[] ds = new DataSource[2];
        DataSource source1 = new DataSource();
        source1.setUrl("cipher://"+ dsIdMain+"/face");
        source1.setVarName("face1");
        ds[0] = source1;
        DataSource source2 = new DataSource();
        source2.setUrl("cipher://"+ dsIdSub+"/face");
        source2.setVarName("face2");
        ds[1] = source2;
        task.setSources(ds);
        ResultDest[] rs = new ResultDest[2];
        ResultDest result1 = new ResultDest();
        result1.setDestId(this.dsIdMain);
        result1.setUrl("result1");
        rs[0] = result1;
        ResultDest result2 = new ResultDest();
        result2.setDestId(this.dsIdSub);
        result2.setUrl("result2");
        rs[1] = result2;
        task.setDestinations(rs);

        task.setName("face_task");
        task.setCode(
                "import privpy as pp\n" +
                "import pnumpy as pnp\n" +
                "import numpy as np\n" +
                "import time\n" +
                "feature_len = 258\n" +
                "pp.engine_log(\"######### Get feature from ds\")\n" +
                "feature1 = pp.ss(\"face1\").flatten()\n" +
                "feature2 = pp.ss(\"face2\").flatten()\n" +
                "pp.engine_log(\"######### Get feature from ds finish\")\n" +
                "query_id_1 = round(pp.back2plain(feature1[0]))\n" +
                "query_id_2 = round(pp.back2plain(feature2[0]))\n" +
                "pp.engine_log(\"######### Get query id_1 %s\" % str(query_id_1))\n" +
                "pp.engine_log(\"######### Get query id_2 %s\" % str(query_id_2))\n" +
                "if (query_id_1 == query_id_2):\n" +
                "   pp.engine_log(\"######### Get equal query id %s\" % str(query_id_1))\n" +
                "   t1 = time.time()\n" +
                "   feature = feature1[1:] + feature2[1:]\n" +
                "   res = feature[0:feature_len].dot(feature[feature_len:])\n" +
                "   pp.engine_log(\"########## finish face match\")\n" +
                "   res1 = pp.sfixed(pnp.random.randint(100000))\n" +
                "   res2 = res - res1\n" +
                "   query_id = pp.sfixed(query_id_1)\n" +
                "   pp.reveal(pp.farr([query_id, res1]), \"result1\")\n" +
                "   pp.reveal(pp.farr([query_id, res2]), \"result2\")\n" +
                "   t2 = time.time()\n" +
                "   pp.engine_log(\"########## face match  consume:%f s\" % (t2 - t1))"
        );
        task.setBackend("ss4");
        TaskServerInfo taskServerInfo = new TaskServerInfo();
        taskServerInfo.setQuota(1);
        task.setServerInfo(taskServerInfo);

        HashMap<String, String> extraInfo = new HashMap<>();
        extraInfo.put("dsSessionId",taskCode);
        task.setExtraInfo(extraInfo);
        TaskManager taskManager  = this.privpyApp.getTaskManager();
        CreateTaskResult createTask = null;
        try {
            createTask = taskManager.submitTask(task);
        } catch (BusinessException e) {
            e.printStackTrace();
            return false;
        }
        taskManager.getPrivpyClient().getTaskIdSessionMap().put(createTask.getTaskId(),taskCode);
        taskManager.getPrivpyClient().getDsIdSessionSet().add(taskCode);

        ExecTaskResponse execTaskResponse = new ExecTaskResponse();
        try {
            execTaskResponse = taskManager.execTask(createTask.getTaskId());
        } catch (BusinessException e) {
            e.printStackTrace();
        }

        if(execTaskResponse.getRetCode()== RetCode.SUCCESS){
            taskManager.getPrivpyClient().getDsIdSessionSet().add(taskCode);
            return true;
        }
        return false;
    }
}
