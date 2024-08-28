package com.sandbox.module.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.module.node.MethodTreeDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author nicc
 * @version 1.0
 * @className MethodTreeUtil
 * @date 2024-08-23 10:18
 */
public class MethodTreeUtil {

    // 合并多个树（traceId 相同的所有链路的集合）
    public List<MethodTreeDTO> mergeMultipleTrees(List<MethodTreeDTO> trees) {
        if (trees == null || trees.isEmpty()) {
            return null;
        }


        // 获取链路树集合的spanId列表，由小到大排序
        List<String> spanList = trees.stream()
                .map(methodTreeDTO -> methodTreeDTO.getMethodInfo().getSpanId())
                .distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());

        // 由底端服务向上进行合并
        for (int i = spanList.size() - 1; i > 0; i--) {
            String spanA = spanList.get(i - 1);
            String spanB = spanList.get(i);

            List<MethodTreeDTO> methodTreeDTOsA = trees.stream().filter(methodTreeDTO -> methodTreeDTO.getMethodInfo().getSpanId().equals(spanA))
                    .collect(Collectors.toList());
            List<MethodTreeDTO> methodTreeDTOsB = trees.stream().filter(methodTreeDTO -> methodTreeDTO.getMethodInfo().getSpanId().equals(spanB))
                    .collect(Collectors.toList());
            mergeMethods(methodTreeDTOsA, methodTreeDTOsB);
        }


        String rootSpan = spanList.get(0);
        return trees.stream().filter(methodTreeDTO -> methodTreeDTO.getMethodInfo().getSpanId().equals(rootSpan))
                .collect(Collectors.toList());
    }


    // 合并相邻层级服务上调用链路
    public void mergeMethods(List<MethodTreeDTO> methodTreeDTOsA, List<MethodTreeDTO> methodTreeDTOsB){

        for (MethodTreeDTO rootB : methodTreeDTOsB) {
            for (MethodTreeDTO rootA : methodTreeDTOsA) {
                List<MethodTreeDTO> targetNodes = getNodeByMethodName(rootA, rootB.getMethodInfo().getMethodName());
                for (MethodTreeDTO targetNode : targetNodes) {
                    if(targetNode.getBeginTimestamp() <= rootB.getBeginTimestamp() &&
                            targetNode.getEndTimestamp() >= rootB.getEndTimestamp()){
                        targetNode.getChildren().add(rootB);
                    }
                }
            }
        }
    }


    // 获取指定方法名节点
    public List<MethodTreeDTO> getNodeByMethodName(MethodTreeDTO root, String methodName) {
        List<MethodTreeDTO> nodes = new ArrayList<>();
        findByMethodName(root, nodes, methodName);
        return nodes;
    }

    // 递归查找叶子节点
    private void findByMethodName(MethodTreeDTO node, List<MethodTreeDTO> nodes, String methodName) {
        if(node.getMethodInfo().getMethodName().equals(methodName)){
            nodes.add(node);
            return;
        }
        for (MethodTreeDTO child : node.getChildren()) {
            findByMethodName(child, nodes, methodName);
        }
    }




        // 获取所有叶子节点
    public List<MethodTreeDTO> getLeafNodes(MethodTreeDTO root) {
        List<MethodTreeDTO> leafNodes = new ArrayList<>();
        findLeafNodes(root, leafNodes);
        return leafNodes;
    }

    // 递归查找叶子节点
    private void findLeafNodes(MethodTreeDTO node, List<MethodTreeDTO> leafNodes) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            leafNodes.add(node);
            return;
        }
        for (MethodTreeDTO child : node.getChildren()) {
            findLeafNodes(child, leafNodes);
        }
    }



    // 测试
    public static void main(String[] args) throws Exception{

        String staticJson = "{\"methodInfo\":{\"className\":\"cn.newgrand.ck.controller.GitInfoController\",\"methodName\":\"rpcTest\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345997,\"children\":[{\"methodInfo\":{\"className\":\"javax.servlet.http.HttpServletRequest\",\"methodName\":\"getAttribute\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"javax.servlet.http.HttpServletRequest\",\"methodName\":\"getHeaders\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"javax.servlet.http.HttpServletRequest\",\"methodName\":\"setAttribute\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"org.springframework.web.context.request.RequestContextHolder\",\"methodName\":\"getRequestAttributes\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"org.springframework.web.context.request.ServletRequestAttributes\",\"methodName\":\"getRequest\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"cn.newgrand.ck.entity.request.MyRequest\",\"methodName\":\"getProjectId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"cn.newgrand.ck.entity.request.MyRequest\",\"methodName\":\"getMappingUrl\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345241,\"children\":[]},{\"methodInfo\":{\"className\":\"cn.newgrand.ck.service.GitInfoService\",\"methodName\":\"getBugKillerApiDefinition\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345997,\"children\":[{\"methodInfo\":{\"className\":\"cn.newgrand.ck.service.impl.GitInfoServiceImpl\",\"methodName\":\"getBugKillerApiDefinition\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345997,\"children\":[{\"methodInfo\":{\"className\":\"cn.newgrand.ck.api.rpc.ApiDefinitionFeignClient\",\"methodName\":\"getApiDefinitionByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345241,\"endTimestamp\":1724651345997,\"children\":[]}]}]},{\"methodInfo\":{\"className\":\"cn.newgrand.ck.api.base.ResultHolder\",\"methodName\":\"success\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0\"},\"beginTimestamp\":1724651345997,\"endTimestamp\":1724651345997,\"children\":[]}]}";
        String bugkillerJson = "{\"methodInfo\":{\"className\":\"io.metersphere.api.controller.ApiDefinitionController\",\"methodName\":\"getApiDefinitionByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345310,\"endTimestamp\":1724651345487,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.api.service.ApiDefinitionService\",\"methodName\":\"getApiDefinitionByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345310,\"endTimestamp\":1724651345487,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.api.service.ApiDefinitionService\",\"methodName\":\"getApiDefinitionByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.base.mapper.ApiDefinitionMapper\",\"methodName\":\"getByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"com.sun.proxy.$Proxy225\",\"methodName\":\"getByProjectIdAndPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"java.lang.reflect.InvocationHandler\",\"methodName\":\"invoke\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.UserDesensitizationInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.UserDesensitizationInterceptor\",\"methodName\":\"intercept\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Invocation\",\"methodName\":\"proceed\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"intercept\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Invocation\",\"methodName\":\"getMethod\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]},{\"methodInfo\":{\"className\":\"java.lang.reflect.Method\",\"methodName\":\"getName\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]},{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Invocation\",\"methodName\":\"getArgs\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]},{\"methodInfo\":{\"className\":\"java.lang.String\",\"methodName\":\"equals\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]},{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Invocation\",\"methodName\":\"proceed\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345403,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.UserDesensitizationInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.UserDesensitizationInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.UserDesensitizationInterceptor\",\"methodName\":\"plugin\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[{\"methodInfo\":{\"className\":\"org.apache.ibatis.plugin.Plugin\",\"methodName\":\"wrap\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345357,\"endTimestamp\":1724651345357,\"children\":[]}]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"<init>\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setProjectId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setName\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setMethod\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setProtocol\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setPath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setModulePath\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setEnvironmentId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setStatus\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setModuleId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setUserId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setCreateTime\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setUpdateTime\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setNum\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setTags\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setCreateUser\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setCaseTotal\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setOrder\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setVersionId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setRefId\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.base.domain.ApiDefinition\",\"methodName\":\"setLatest\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345403,\"endTimestamp\":1724651345403,\"children\":[]}]},{\"methodInfo\":{\"className\":\"java.util.ArrayList\",\"methodName\":\"<init>\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.ArrayList\",\"methodName\":\"iterator\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.Iterator\",\"methodName\":\"hasNext\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.Iterator\",\"methodName\":\"next\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"undo\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"undo\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"getConfig\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"io.metersphere.interceptor.MybatisInterceptor\",\"methodName\":\"getConfig\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[{\"methodInfo\":{\"className\":\"java.util.HashMap\",\"methodName\":\"<init>\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.lang.Object\",\"methodName\":\"getClass\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.lang.Class\",\"methodName\":\"getName\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.concurrent.ConcurrentHashMap\",\"methodName\":\"get\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.concurrent.ConcurrentHashMap\",\"methodName\":\"get\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]}]}]},{\"methodInfo\":{\"className\":\"org.apache.commons.collections.MapUtils\",\"methodName\":\"isEmpty\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]}]}]}]}]},{\"methodInfo\":{\"className\":\"java.util.ArrayList\",\"methodName\":\"<init>\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.ArrayList\",\"methodName\":\"iterator\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.Iterator\",\"methodName\":\"hasNext\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.Iterator\",\"methodName\":\"next\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.Iterator\",\"methodName\":\"hasNext\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]}]}]}]}]},{\"methodInfo\":{\"className\":\"java.util.Objects\",\"methodName\":\"nonNull\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.List\",\"methodName\":\"size\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]},{\"methodInfo\":{\"className\":\"java.util.List\",\"methodName\":\"get\",\"traceId\":\"1c2243e5-2617-4347-8f0d-bfdb83f92eb0\",\"spanId\":\"0.0\"},\"beginTimestamp\":1724651345404,\"endTimestamp\":1724651345404,\"children\":[]}]}]}]}";

            ObjectMapper objectMapper = new ObjectMapper();
            MethodTreeDTO staticTree = objectMapper.readValue(staticJson, MethodTreeDTO.class);
            MethodTreeDTO bugkillerTree = objectMapper.readValue(bugkillerJson, MethodTreeDTO.class);
            List<MethodTreeDTO> list = new ArrayList<>();

            list.add(staticTree);
            list.add(bugkillerTree);

            MethodTreeUtil methodTreeUtil = new MethodTreeUtil();

            List<MethodTreeDTO> trees = methodTreeUtil.mergeMultipleTrees(list);

        String json = objectMapper.writeValueAsString(list.get(0));

        System.out.println(trees);
        System.out.println(json);

    }
}
