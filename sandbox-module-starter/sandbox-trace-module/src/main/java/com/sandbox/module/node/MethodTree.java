package com.sandbox.module.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class MethodTree {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // 根节点
    private final MethodNode root;

    // 当前节点
    private MethodNode current;

    public MethodTree(MethodInfo title) {
        this.root = new MethodNode(title).markBegin();
        this.current = root;
    }

    /**
     * 环检测
     * @return
     */
    private boolean isAncestor(MethodNode node, MethodNode potentialAncestor) {
        while (node != null) {
            if (node == potentialAncestor) {
                return true;
            }
            node = node.parent;
        }
        return false;
    }

    public MethodTree updateEnd(){
        current.markEnd();
        root.markEnd();
        return this;
    }

    public boolean isTop() {
        return current.isRoot();
    }

    /**
     * 创建一个分支节点
     *
     * @param data 节点数据
     * @return this
     */
    public MethodTree begin(MethodInfo data) {
//        current = new MethodTree.MethodNode(current, data);
//        current.markBegin();
        current = new MethodNode(current, data);
        current.markBegin();

        return this;
    }

    public MethodTree begin() {
        return begin(null);
    }

    public Object get() {
//        if (current.isRoot()) {
//            throw new IllegalStateException("current node is root.");
//        }
        return current.data;
    }

    public MethodTree set(MethodInfo data) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.data = data;
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public MethodTree end() {
        current.markEnd();
        if(!current.isRoot()){
            current = current.parent;
        }
        return this;
    }

    public MethodInfo getCurrentData(){
        return current.data;
    }
    public void setCurrentData(MethodInfo data){
        current.data = data;
    }

    public MethodNode getRoot() {
        return root;
    }

    public MethodNode getCurrent() {
        return current;
    }

    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, MethodNode node);

    }


    public MethodTreeDTO convertToDTO(MethodNode node) {
        if (node == null) {
            return null;
        }
        MethodTreeDTO dto = new MethodTreeDTO();
        dto.setDepth(node.depth);
        dto.setMethodInfo(node.data);
        dto.setBeginTimestamp(node.beginTimestamp);
        dto.setEndTimestamp(node.endTimestamp);
        // 使用迭代法代替递归法（适用于大树结构）
        Deque<MethodNode> stack = new ArrayDeque<>();
        Deque<MethodTreeDTO> dtoStack = new ArrayDeque<>();
        stack.push(node);
        dtoStack.push(dto);

        while (!stack.isEmpty()) {
            MethodNode current = stack.pop();
            MethodTreeDTO currentDTO = dtoStack.pop();
            for (MethodNode child : current.children) {
                MethodTreeDTO childDTO = new MethodTreeDTO();
                childDTO.setMethodInfo(child.data);
                childDTO.setBeginTimestamp(child.beginTimestamp);
                childDTO.setEndTimestamp(child.endTimestamp);
                childDTO.setDepth(child.depth);
                currentDTO.getChildren().add(childDTO);
                stack.push(child);
                dtoStack.push(childDTO);
            }
        }
        return dto;
    }

    public String getCurrentMethodNodeStr() {
        StringBuilder sb = new StringBuilder();
        printNode(root, 0, sb);
        return sb.toString();
    }

    private void printNode(MethodNode node, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) {
            sb.append("  "); // 缩进
        }
        sb.append(node.data == null ? "null" : node.data.toString());
        sb.append(" (start: ").append(node.beginTimestamp);
        sb.append(", end: ").append(node.endTimestamp).append(")\n");
        for (MethodNode child : node.children) {
            printNode(child, depth + 1, sb);
        }
    }



    private static class MethodNode {

        final MethodNode parent;

        /**
         * 节点数据
         */
        MethodInfo data;

        /**
         * 子节点
         */
        final List<MethodNode> children = new ArrayList<>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**s
         * 结束时间戳
         */
        private long endTimestamp;

        private final int depth;

        private MethodNode(MethodNode parent, MethodInfo data) {
            this.parent = parent;
            this.data = data;
            this.depth = (parent == null) ? 0 : parent.depth + 1;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        private MethodNode(MethodInfo data) {
            this.parent = null;
            this.data = data;
            this.depth = 0;
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
                return null == parent;
            }

        /**
         * 是否叶子节点
         *
         * @return true / false
         */
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

    }

}
