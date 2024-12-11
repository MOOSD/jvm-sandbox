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
    private String traceId;
    private String spanId;
    private Integer sort = 0;
    private String requestUri;
    private Long requestCreateTime;
    private Boolean send = false;
    private MethodNode current;

    // 方法详细信息列表
    private final Map<Integer, MethodInfo> baseInfo;

    // 构造方法，初始化根节点
    public MethodTree(MethodInfo title) {
        this.baseInfo = new HashMap<>();
        this.root = new MethodNode(title, this.sort++);
        this.current = root;
    }

    // 获取树的基础信息
    public Map<Integer, MethodInfo> getBaseInfo() {
        return baseInfo;
    }

    // 添加方法详细信息
    public void addBaseInfo(MethodInfo info) {
        baseInfo.put(this.getCurrentSort(), info);
    }

    // 获取指定排序值的 MethodInfo
    public MethodInfo getBaseInfo(Integer sort) {
        return baseInfo.get(sort);
    }

    // 获取当前节点的排序值
    public Integer getCurrentSort() {
        return this.current.getSort();
    }

    // 更新结束标志，标记根节点和当前节点结束
    public MethodTree updateEnd() {
        current.markEnd();
        root.markEnd();
        return this;
    }

    // 添加方法单元
    public void addMethodCell(String cell) {
        this.current.addMethodCell(cell);
    }

    // 判断当前节点是否为根节点
    public boolean isTop() {
        return current.isRoot();
    }

    // 创建一个新的分支节点
    public MethodTree begin(MethodInfo data) {
        current = new MethodNode(current, data, this.sort++);
        TraceIdModule.setSort(this.sort);
        current.markBegin();
        return this;
    }

    // 判断当前节点是否为开始节点
    public boolean getBegin() {
        return this.current.isBegin();
    }

    // 设置开始节点
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

    // 结束当前分支节点
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

    // 获取排序 RPC
    public List<Integer> getSortRpc() {
        return sortRpc;
    }

    // 设置排序 RPC
    public void setSortRpc(List<Integer> sortRpc) {
        this.sortRpc = sortRpc;
    }

    // 获取请求创建时间
    public Long getRequestCreateTime() {
        return requestCreateTime;
    }

    // 设置请求创建时间
    public void setRequestCreateTime(long requestCreateTime) {
        this.requestCreateTime = requestCreateTime;
    }

    public boolean getSend() {
        return send;
    }

    public void setSend(boolean b) {
        this.send = b;
    }

    // 用于回调的接口
    private interface Callback {
        void callback(int deep, boolean isLast, String prefix, MethodNode node);
    }

    // 将 MethodNode 转换为 DTO（数据传输对象）
    public MethodTreeDTO convertToDTO(MethodNode node, final MethodInfo[] methodInfoList, final Map<Integer, MethodInfo> methodInfoMap) {
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
        MethodInfo data;
        private final int sort;
        private boolean begin = false;
        final List<MethodNode> children = new ArrayList<>();
        private long beginTimestamp;
        private long endTimestamp;
        private final int depth;
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

        boolean isRoot() {
            return parent == null;
        }

        boolean isLeaf() {
            return children.isEmpty();
        }

        MethodNode markBegin() {
            beginTimestamp = currentTimeMillis();
            return this;
        }

        MethodNode markEnd() {
            endTimestamp = currentTimeMillis();
            return this;
        }

        public boolean isBegin() {
            return begin;
        }

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
