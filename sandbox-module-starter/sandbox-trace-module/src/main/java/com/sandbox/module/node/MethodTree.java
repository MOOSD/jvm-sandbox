package com.sandbox.module.node;

import com.sandbox.module.dynamic.TraceIdModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.System.currentTimeMillis;

public class MethodTree {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 根节点
    private final MethodNode root;

    // 微服务外调id
    private List<Integer> sortRpc;
    // 树的基础信息
    // 树的唯一标识
    private String traceId;
    // 跨微服务调用深度
    private String spanId;
    // 单服务调用顺序
    private Integer sort = 0;
    // 请求url
    private String requestUri;

    // 是否已发送过
    private Boolean send = false;

    // 当前节点
    private MethodNode current;

    // 构造方法，初始化根节点
    public MethodTree(MethodInfo title) {
        this.baseInfo = new HashMap<>();
        this.root = new MethodNode(title, this.sort++).markBegin();
        this.current = root;
    }

    public Map<Integer, MethodInfo> getBaseInfo() {
        return baseInfo;
    }

    // 方法详细信息列表
    private final Map<Integer, MethodInfo> baseInfo;

    public void addBaseInfo(MethodInfo info){
        baseInfo.put(this.getCurrentSort(), info);
    }
    public MethodInfo getBaseInfo(Integer sort){
        return baseInfo.get(sort);
    }

    // 获取 send 状态
    public Boolean getSend() {
        return send;
    }

    // 设置 send 状态
    public void setSend(Boolean send) {
        this.send = send;
    }

    // 获取当前节点的 sort 值
    public Integer getCurrentSort(){
        return this.current.getSort();
    }

    // 更新结束标志，标记根节点和当前节点结束
    public MethodTree updateEnd() {
        current.markEnd();
        root.markEnd();
        return this;
    }

    public void addMethodCell(String cell){
        this.current.addMethodCell(cell);
    }

    // 判断当前节点是否为根节点
    public boolean isTop() {
        return current.isRoot();
    }

    /**
     * 创建一个新的分支节点
     * @param data 节点数据
     * @return this
     */
    public MethodTree begin(MethodInfo data) {
        current = new MethodNode(current, data, this.sort++);
        TraceIdModule.setSort(this.sort);
        current.markBegin();
        return this;
    }

    /**
     * 是否是开始节点
     * @return
     */
    public boolean getBegin() {
        return this.current.isBegin();
    }

    /**
     * 设置开始节点
     * @return
     */
    public void setBegin(boolean begin) {
        this.current.setBegin(begin);
    }

    // 无参数的 begin 方法，默认创建空节点
    public MethodTree begin() {
        return begin(null);
    }

    // 获取当前节点的数据
    public Object get() {
        return current.data;
    }

    // 设置当前节点的数据
    public MethodTree set(MethodInfo data) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.data = data;
        return this;
    }

    /**
     * 结束当前分支节点
     * @return this
     */
    public MethodTree end() {
        current.markEnd();
        if (!current.isRoot()) {
            current = current.parent;
        }
        return this;
    }

    // 获取当前节点的数据
    public MethodInfo getCurrentData() {
        return current.data;
    }

    // 设置当前节点的数据
    public void setCurrentData(MethodInfo data) {
        current.data = data;
    }

    // 获取根节点
    public MethodNode getRoot() {
        return root;
    }

    // 获取当前节点
    public MethodNode getCurrent() {
        return current;
    }

    // 获取树的排序值
    public Integer getSort() {
        return sort;
    }

    // 设置树的排序值
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    // 获取 traceId
    public String getTraceId() {
        return traceId;
    }

    // 设置 traceId
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    // 获取 spanId
    public String getSpanId() {
        return spanId;
    }

    // 设置 spanId
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    // 获取 requestUri
    public String getRequestUri() {
        return requestUri;
    }

    // 设置 requestUri
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public List<Integer> getSortRpc() {
        return sortRpc;
    }

    public void setSortRpc(List<Integer> sortRpc) {
        this.sortRpc = sortRpc;
    }

    // 用于回调的接口
    private interface Callback {
        void callback(int deep, boolean isLast, String prefix, MethodNode node);
    }

    // 将 MethodNode 转换为 DTO（数据传输对象）
    public MethodTreeDTO convertToDTO(MethodNode node,final MethodInfo[] methodInfoList ,final Map<Integer , MethodInfo> methodInfoMap) {
        if (node == null) {
            return null;
        }
        MethodTreeDTO dto = new MethodTreeDTO();
        Integer sort = node.sort;
        MethodInfo methodInfo = node.data;
        methodInfo.mergeInfo(methodInfoMap.get(sort));
        methodInfo.setSort(sort);
        methodInfoList[sort] = methodInfo;
        dto.setDepth(node.depth);
        dto.setClizzName(methodInfo.getClassName());
        dto.setMethodName(methodInfo.getMethodName());
        dto.setBeginTimestamp(node.beginTimestamp);
        dto.setEndTimestamp(node.endTimestamp);
        dto.setSort(sort);
        dto.setMethodCell(node.methodCells);

        // 使用迭代法代替递归法，适用于大树结构
        Deque<MethodNode> stack = new ArrayDeque<>();
        Deque<MethodTreeDTO> dtoStack = new ArrayDeque<>();
        stack.push(node);
        dtoStack.push(dto);

        // 迭代遍历树结构
        while (!stack.isEmpty()) {
            MethodNode current = stack.pop();
            MethodTreeDTO currentDTO = dtoStack.pop();

            // 避免 ConcurrentModificationException：将 children 复制到新集合中
            if (!current.children.isEmpty()) {
                List<MethodTreeDTO> children = new ArrayList<>(current.children.size());
                List<MethodNode> childNodes = new ArrayList<>(current.children);

                for (MethodNode child : childNodes) {
                    Integer childSort = child.sort;
                    MethodInfo childInfo = child.data;
                    childInfo.mergeInfo(methodInfoMap.get(childSort));
                    childInfo.setSort(childSort);
                    methodInfoList[childSort] = childInfo;

                    MethodTreeDTO childDTO = new MethodTreeDTO();
                    childDTO.setClizzName(childInfo.getClassName());
                    childDTO.setMethodName(childInfo.getMethodName());
                    childDTO.setBeginTimestamp(child.beginTimestamp);
                    childDTO.setEndTimestamp(child.endTimestamp);
                    childDTO.setDepth(child.depth);
                    childDTO.setSort(childSort);
                    childDTO.setMethodCell(child.methodCells);
                    children.add(childDTO);
                    stack.push(child);
                    dtoStack.push(childDTO);
                }
                currentDTO.setChildren(children);
            }
        }

        return dto;
    }

    // 获取当前节点的字符串表示
    public String getCurrentMethodNodeStr() {
        StringBuilder sb = new StringBuilder();
        printNode(root, 0, sb);
        return sb.toString();
    }

    // 遍历节点并打印树的结构
    private void printNode(MethodNode node, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(node.data == null ? "null" : node.data.toString());
        sb.append(" (start: ").append(node.beginTimestamp);
        sb.append(", end: ").append(node.endTimestamp).append(")\n");

        for (MethodNode child : node.children) {
            printNode(child, depth + 1, sb);
        }
    }

    /**
     * MethodNode 类，表示树的一个节点
     */
    private static class MethodNode {

        final MethodNode parent;
        MethodInfo data; // 节点数据
        private final int sort;
        private boolean begin = false; // 是否开始
        final List<MethodNode> children = new ArrayList<>(); // 子节点
        private long beginTimestamp; // 开始时间戳
        private long endTimestamp;   // 结束时间戳
        private final int depth;     // 节点的深度

        private List<String> methodCells = new LinkedList<>();
        // 构造方法：创建一个子节点
        private MethodNode(MethodNode parent, MethodInfo data, Integer sort) {
            this.parent = parent;
            this.data = data;
            this.sort = sort;
            this.depth = (parent == null) ? 0 : parent.depth + 1;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        // 构造方法：创建根节点
        private MethodNode(MethodInfo data, int sort) {
            this.sort = sort;
            this.parent = null;
            this.data = data;
            this.depth = 0;
        }

        public void addMethodCell(String methodCell) {
            methodCells.add(methodCell);
        }

        // 判断是否是根节点
        boolean isRoot() {
            return parent == null;
        }

        // 判断是否是叶子节点（没有子节点）
        boolean isLeaf() {
            return children.isEmpty();
        }

        // 标记节点为开始
        MethodNode markBegin() {
            beginTimestamp = currentTimeMillis();
            return this;
        }

        // 标记节点为结束
        MethodNode markEnd() {
            endTimestamp = currentTimeMillis();
            return this;
        }

        // 获取是否已开始
        public boolean isBegin() {
            return begin;
        }

        // 设置是否已开始
        public void setBegin(boolean begin) {
            this.begin = begin;
        }

        public Integer getSort() {
            return sort;
        }

        public List<String> getMethodCells() {
            return methodCells;
        }

        public void setMethodCells(List<String> methodCells) {
            this.methodCells = methodCells;
        }
    }
}
